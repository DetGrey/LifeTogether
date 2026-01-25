package com.example.lifetogether.domain.usecase.observers

import android.content.Context
import android.util.Log
import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.listener.ListItemsResultListener
import com.example.lifetogether.domain.repository.StorageRepository
import com.example.lifetogether.domain.listener.TempFileDownloadResult
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import java.io.File
import javax.inject.Inject
import kotlin.let

class ObserveGalleryMediaUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val storageRepository: StorageRepository,
    private val localDataSource: LocalDataSource,
) {
    companion object {
        private const val TAG = "ObserveGalleryMedia"
        // Limit concurrent downloads to prevent ANR and memory exhaustion
        private const val MAX_CONCURRENT_DOWNLOADS = 2
        // Batch size for Room database updates to prevent blocking main thread
        private const val BATCH_SIZE = 10
        // Retry attempts for failed individual downloads
        private const val MAX_RETRY_ATTEMPTS = 3
        // Base delay for exponential backoff (ms)
        private const val BASE_RETRY_DELAY = 500L
        // How many rounds to retry failed items (in addition to per-item retries)
        private const val MAX_DOWNLOAD_ROUNDS = 10
        // Delay between rounds (ms)
        private const val ROUND_RETRY_DELAY = 2000L
    }

    suspend operator fun invoke(
        familyId: String,
        context: Context,
    ) {
        Log.d(TAG, "invoked")
        firestoreDataSource.galleryMediaSnapshotListener(familyId).collect { result ->
            Log.d(TAG, "galleryMediaSnapshotListener().collect result: $result")
            when (result) {
                is ListItemsResultListener.Success -> {
                    // Only allow empty list to delete local files if data is from server (not cache)
                    // This prevents offline/cached empty responses from deleting local data
                    // But allows legitimate deletions when confirmed by the server
                    if (result.listItems.isEmpty() && result.isFromCache) {
                        Log.d(TAG, "galleryMediaSnapshotListener().collect result: is empty from cache - ignoring to preserve local cache")
                        // Don't delete anything - cached empty responses are unreliable
                        return@collect
                    }
                    
                    if (result.listItems.isEmpty()) {
                        Log.d(TAG, "galleryMediaSnapshotListener().collect result: is empty from server - processing deletions")
                    }

                    // Get existing media from local database to avoid re-downloading
                    val existingMediaMap = localDataSource.getExistingGalleryMediaInfo(familyId)

                    // Filter items that need downloading (not already in local storage)
                    val itemsToDownload = result.listItems.filter { galleryMedia ->
                        val mediaId = galleryMedia.id
                        val existingMediaUri = existingMediaMap[mediaId]?.first
                        existingMediaUri == null // Only download if not already locally cached
                    }

                    if (itemsToDownload.isEmpty()) {
                        Log.d(TAG, "All ${result.listItems.size} items already exist locally")
                        // Still need to call updateGalleryMedia with completeSourceList to handle deletions
                        localDataSource.updateGalleryMedia(
                            familyId = familyId,
                            items = emptyList(), // No new items to download
                            completeSourceList = result.listItems // Pass complete list for deletion detection
                        )
                        return@collect
                    }

                    Log.d(TAG, "Need to download ${itemsToDownload.size} items out of ${result.listItems.size}")
                    
                    // Track items that fail after all retries for detection/logging
                    val failedItems = mutableSetOf<String>()

                    // Process downloads in batches with limited concurrency to prevent ANR/OOM
                    val allDownloadedItems: MutableList<Pair<GalleryMedia, File>> = mutableListOf()

                    var remainingItems = itemsToDownload
                    var round = 1

                    while (remainingItems.isNotEmpty() && round <= MAX_DOWNLOAD_ROUNDS) {
                        Log.d(TAG, "Download round $round: ${remainingItems.size} items remaining")

                        val roundFailedItems = mutableSetOf<String>()

                        remainingItems.chunked(BATCH_SIZE).forEachIndexed { batchIndex, batch ->
                            Log.d(TAG, "Processing batch ${batchIndex + 1} (round $round) with ${batch.size} items")

                            val batchResults: MutableList<Pair<GalleryMedia, File>> = mutableListOf()

                            coroutineScope {
                                // Use semaphore to limit concurrent downloads to MAX_CONCURRENT_DOWNLOADS
                                val semaphore = Semaphore(MAX_CONCURRENT_DOWNLOADS)

                                val downloadTasks: List<Deferred<Pair<GalleryMedia, File>?>> =
                                    batch.map { galleryMedia ->
                                        async(Dispatchers.IO) {
                                            semaphore.acquire()
                                            try {
                                                galleryMedia.mediaUrl?.let { url ->
                                                    val fallbackExtension =
                                                        if (galleryMedia is GalleryImage) "jpeg" else "mp4"
                                                    val extension = "." + galleryMedia.itemName.substringAfterLast(
                                                        '.',
                                                        fallbackExtension
                                                    )

                                                    // Retry logic with exponential backoff
                                                    var downloadResult: TempFileDownloadResult? = null
                                                    var lastException: Exception? = null

                                                    repeat(MAX_RETRY_ATTEMPTS) { attempt ->
                                                        if (downloadResult is TempFileDownloadResult.Success) {
                                                            return@repeat // Exit early if successful
                                                        }

                                                        try {
                                                            downloadResult = storageRepository.downloadContentToTempFile(
                                                                context,
                                                                url,
                                                                extension
                                                            )

                                                            if (downloadResult is TempFileDownloadResult.Success) {
                                                                if (attempt > 0) {
                                                                    Log.d(TAG, "Downloaded on retry ${attempt + 1}: ${galleryMedia.itemName}")
                                                                } else {
                                                                    Log.d(TAG, "Downloaded: ${galleryMedia.itemName}")
                                                                }
                                                            } else if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                                                                // Exponential backoff before retry
                                                                val delayMs = BASE_RETRY_DELAY * (attempt + 1)
                                                                Log.d(TAG, "Download failed for ${galleryMedia.itemName}, retrying in ${delayMs}ms (attempt ${attempt + 1}/${MAX_RETRY_ATTEMPTS})")
                                                                delay(delayMs)
                                                            }
                                                        } catch (e: Exception) {
                                                            lastException = e
                                                            if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                                                                val delayMs = BASE_RETRY_DELAY * (attempt + 1)
                                                                Log.d(TAG, "Download exception for ${galleryMedia.itemName}: ${e.message}, retrying in ${delayMs}ms (attempt ${attempt + 1}/${MAX_RETRY_ATTEMPTS})")
                                                                delay(delayMs)
                                                            }
                                                        }
                                                    }

                                                    if (downloadResult is TempFileDownloadResult.Success) {
                                                        Pair(
                                                            galleryMedia,
                                                            downloadResult.downloadedFile
                                                        )
                                                    } else {
                                                        roundFailedItems.add(galleryMedia.id ?: "unknown")
                                                        if (downloadResult is TempFileDownloadResult.Failure) {
                                                            Log.d(TAG, "Failed to download ${galleryMedia.itemName} after $MAX_RETRY_ATTEMPTS attempts: ${downloadResult.message}")
                                                        } else if (lastException != null) {
                                                            Log.d(TAG, "Failed to download ${galleryMedia.itemName} after $MAX_RETRY_ATTEMPTS attempts: ${lastException.message}")
                                                        }
                                                        null
                                                    }
                                                }
                                            } finally {
                                                semaphore.release()
                                            }
                                        }
                                    }

                                val successfulDownloads = downloadTasks.awaitAll().filterNotNull()
                                batchResults.addAll(successfulDownloads)
                            }

                            // Update database after each batch to avoid blocking main thread for too long
                            if (batchResults.isNotEmpty()) {
                                Log.d(TAG, "Saving batch ${batchIndex + 1} (round $round) with ${batchResults.size} items to database")
                                // Don't pass completeSourceList here - we'll do it at the end to handle deletions once
                                localDataSource.updateGalleryMedia(familyId, batchResults, completeSourceList = null)
                                allDownloadedItems.addAll(batchResults)
                            }
                        }

                        // Prepare for next round if needed
                        if (roundFailedItems.isNotEmpty()) {
                            Log.d(TAG, "Round $round completed with ${roundFailedItems.size} failed items: $roundFailedItems")
                            // Track permanently failed items (those that fail in last round)
                            if (round >= MAX_DOWNLOAD_ROUNDS) {
                                failedItems.addAll(roundFailedItems)
                            }
                            // Filter to remaining items by id
                            remainingItems = remainingItems.filter { media ->
                                roundFailedItems.contains(media.id)
                            }
                        } else {
                            remainingItems = emptyList()
                        }

                        if (remainingItems.isNotEmpty() && round < MAX_DOWNLOAD_ROUNDS) {
                            Log.d(TAG, "Scheduling next download round after ${ROUND_RETRY_DELAY}ms for ${remainingItems.size} remaining items")
                            delay(ROUND_RETRY_DELAY)
                        }

                        round += 1
                    }

                    Log.d(TAG, "Completed downloading ${allDownloadedItems.size} items")
                    
                    // Final update with complete source list to handle deletions
                    // This ensures items deleted from Firestore are also deleted locally
                    Log.d(TAG, "Performing final sync to detect and remove deleted items")
                    localDataSource.updateGalleryMedia(
                        familyId = familyId,
                        items = emptyList(), // No new items to add
                        completeSourceList = result.listItems // Pass complete list for deletion detection
                    )
                    
                    // Detect partial downloads and log warning
                    if (failedItems.isNotEmpty()) {
                        Log.w(TAG, "${failedItems.size} items failed to download after $MAX_DOWNLOAD_ROUNDS rounds: $failedItems")
                    }
                    
                    if (remainingItems.isNotEmpty()) {
                        Log.w(TAG, "Download reached max rounds ($MAX_DOWNLOAD_ROUNDS) with ${remainingItems.size} items still failing")
                        failedItems.addAll(remainingItems.mapNotNull { it.id })
                    }
                    
                    val expectedCount = result.listItems.size
                    val actualCount = allDownloadedItems.size + existingMediaMap.size
                    if (actualCount < expectedCount) {
                        Log.w(TAG, "Partial media load - Expected $expectedCount items, got $actualCount (${failedItems.size} failed, ${existingMediaMap.size} already cached)")
                    }
                }

                is ListItemsResultListener.Failure -> {
                    // Handle failure
                    Log.e(TAG, "failure: ${result.message}")
                }
            }
        }
    }
}
