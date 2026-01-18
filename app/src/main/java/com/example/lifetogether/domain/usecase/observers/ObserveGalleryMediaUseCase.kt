package com.example.lifetogether.domain.usecase.observers

import android.content.Context
import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirebaseStorageDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.TempFileDownloadResult
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import javax.inject.Inject
import kotlin.let

class ObserveGalleryMediaUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val firebaseStorageDataSource: FirebaseStorageDataSource,
    private val localDataSource: LocalDataSource,
) {
    suspend operator fun invoke(
        familyId: String,
        context: Context,
    ) {
        println("ObserveGalleryMediaUseCase invoked")
        firestoreDataSource.galleryMediaSnapshotListener(familyId).collect { result ->
            println("galleryMediaSnapshotListener().collect result: $result")
            when (result) {
                is ListItemsResultListener.Success -> {
                    if (result.listItems.isEmpty()) {
                        println("galleryMediaSnapshotListener().collect result: is empty")
                        localDataSource.deleteFamilyGalleryMedia(familyId)
                    } else {
                        // Get existing media from local database to avoid re-downloading
                        val existingMediaMap = localDataSource.getExistingGalleryMediaInfo(familyId)

                        val tempFileDownloadSuccessResults: MutableList<Pair<GalleryMedia, File>> = mutableListOf()

                        coroutineScope { // For concurrent downloads
                            // Explicitly type the list of Deferred objects
                            val downloadTasks: List<Deferred<Pair<GalleryMedia, File>?>> =
                                result.listItems.map { galleryMedia ->

                                    async(Dispatchers.IO) { // Ensure download happens on IO dispatcher
                                        galleryMedia.mediaUrl?.let { url ->
                                            val mediaId = galleryMedia.id
                                            val existingMediaUri = existingMediaMap[mediaId]?.first

                                            // Only download if media doesn't exist locally (no mediaUri stored)
                                            if (existingMediaUri != null) {
                                                println("ObserveGalleryMediaUseCase: Skipping download for ${galleryMedia.itemName} - already exists locally")
                                                return@async null // Media already exists, skip download
                                            }

                                            val fallbackExtension = if (galleryMedia is GalleryImage) "jpeg" else "mp4"
                                            val extension = "." + galleryMedia.itemName.substringAfterLast('.', fallbackExtension)

                                            val downloadResult = firebaseStorageDataSource.downloadContentToTempFile(context, url, extension)
                                            if (downloadResult is TempFileDownloadResult.Success) {
                                                Pair(galleryMedia, downloadResult.downloadedFile)
                                            } else {
                                                if (downloadResult is TempFileDownloadResult.Failure) {
                                                    println("ObserveGalleryMediaUseCase: Failed to download ${galleryMedia.itemName}: ${downloadResult.message}")
                                                } else {
                                                    println("ObserveGalleryMediaUseCase: Unknown error downloading ${galleryMedia.itemName}")
                                                }
                                                null // Indicate failure for this item
                                            }
                                        }
                                        // If mediaUrl is null, this async block implicitly returns null (as per Pair<GalleryMedia, File>?)
                                    }
                                }
                            // awaitAll() will return List<Pair<GalleryMedia, File>?>
                            // filterNotNull() will then correctly change it to List<Pair<GalleryMedia, File>>
                            val successfulDownloads = downloadTasks.awaitAll().filterNotNull()
                            tempFileDownloadSuccessResults.addAll(successfulDownloads)
                        }

                        if (tempFileDownloadSuccessResults.isNotEmpty()) {
                            localDataSource.updateGalleryMedia(familyId, tempFileDownloadSuccessResults)
                        }
                    }
                }
                is ListItemsResultListener.Failure -> {
                    // Handle failure
                    println("ObserveGalleryMediaUseCase failure: ${result.message}")
                }
            }
        }
    }
}
