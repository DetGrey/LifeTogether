package com.example.lifetogether.data.repository

import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.data.logic.AppErrorThrowable
import com.example.lifetogether.data.logic.appResultOf
import com.example.lifetogether.domain.result.AppError
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.example.lifetogether.data.local.source.AlbumLocalDataSource
import com.example.lifetogether.data.local.source.MediaLocalDataSource
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.data.model.AlbumEntity
import com.example.lifetogether.data.model.GalleryMediaEntity
import com.example.lifetogether.data.remote.GalleryFirestoreDataSource
import com.example.lifetogether.di.IoDispatcher
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.SaveProgress
import com.example.lifetogether.domain.model.enums.MediaType
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.repository.GalleryRepository
import com.example.lifetogether.domain.datasource.StorageDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryRepositoryImpl @Inject constructor(
    private val albumLocalDataSource: AlbumLocalDataSource,
    private val mediaLocalDataSource: MediaLocalDataSource,
    private val galleryFirestoreDataSource: GalleryFirestoreDataSource,
    private val storageDataSource: StorageDataSource,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GalleryRepository {

    companion object {
        private const val TAG = "GalleryRepositoryImpl"
        private const val MAX_CONCURRENT_DOWNLOADS = 2
        private const val BATCH_SIZE = 10
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BASE_RETRY_DELAY = 500L
        private const val MAX_DOWNLOAD_ROUNDS = 10
        private const val ROUND_RETRY_DELAY = 2000L
    }

    private val _thumbnailCache = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    override val thumbnailCache: StateFlow<Map<String, ByteArray>> = _thumbnailCache.asStateFlow()

    override fun observeAlbums(familyId: String): Flow<Result<List<Album>, AppError>> {
        return albumLocalDataSource.observeAlbums(familyId).map { entities ->
            appResultOf { entities.map { it.toModel() }.sortedBy { it.itemName } }
        }
    }

    override fun syncAlbumsFromRemote(familyId: String): Flow<Result<Unit, AppError>> {
        return galleryFirestoreDataSource.albumsSnapshotListener(familyId).map { result ->
            when (result) {
                is Result.Success -> appResultOfSuspend {
                    if (result.data.items.isEmpty()) {
                        albumLocalDataSource.deleteFamilyAlbums(familyId)
                    } else {
                        albumLocalDataSource.updateAlbums(result.data.items)
                    }
                }

                is Result.Failure -> Result.Failure(result.error)
            }
        }
    }

    override suspend fun saveAlbum(album: Album): Result<String, AppError> {
        return galleryFirestoreDataSource.saveAlbum(album)
    }

    override suspend fun fetchAlbumThumbnail(albumId: String) {
        Log.d(TAG, "fetchAlbumThumbnail")
        if (_thumbnailCache.value.containsKey(albumId)) return

        val result = mediaLocalDataSource.getAlbumThumbnail(albumId)
        if (result != null) {
            _thumbnailCache.update { currentMap ->
                currentMap + (albumId to result)
            }
            Log.d(TAG, "Cache updated for album: $albumId. New size: ${_thumbnailCache.value.size}")
        }
    }

    override suspend fun getAlbumMediaThumbnail(mediaId: String): Result<ByteArray, AppError> {
        Log.d(TAG, "getAlbumMediaThumbnail")
        val result = mediaLocalDataSource.getAlbumMediaThumbnail(mediaId)
        return if (result != null) {
            Result.Success(result)
        } else {
            Result.Failure(AppErrors.notFound("No thumbnail found"))
        }
    }

    override suspend fun saveGalleryMediaMetaData(galleryMedia: List<GalleryMedia>): Result<Unit, AppError> {
        return when (val result = galleryFirestoreDataSource.saveGalleryMediaMetaData(galleryMedia)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(result.error)
        }
    }

    override suspend fun uploadVideo(uri: Uri, path: String, extension: String): Result<String, AppError> {
        return storageDataSource.uploadVideo(uri, path, extension)
    }

    override suspend fun deleteAlbum(albumId: String): Result<Unit, AppError> {
        return galleryFirestoreDataSource.deleteAlbum(albumId)
    }

    override suspend fun deleteGalleryMedia(mediaIds: List<String>): Result<Unit, AppError> {
        return galleryFirestoreDataSource.deleteGalleryMedia(mediaIds)
    }

    override suspend fun updateAlbumCount(albumId: String, count: Int): Result<Unit, AppError> {
        return galleryFirestoreDataSource.updateAlbumCount(albumId, count)
    }

    override suspend fun moveMediaToAlbum(
        mediaIdList: Set<String>,
        newAlbumId: String,
        oldAlbumId: String,
    ): Result<Unit, AppError> {
        return galleryFirestoreDataSource.moveMediaToAlbum(mediaIdList, newAlbumId, oldAlbumId)
    }

    override fun observeAlbumById(familyId: String, albumId: String): Flow<Result<Album, AppError>> {
        return albumLocalDataSource.observeAlbumById(familyId, albumId).map { entity ->
            appResultOf {
                entity?.toModel() ?: throw AppErrorThrowable(AppErrors.notFound("Album not found"))
            }
        }
    }

    override fun observeAlbumMedia(familyId: String, albumId: String): Flow<Result<List<GalleryMedia>, AppError>> {
        return albumLocalDataSource.getAlbumMedia(familyId, albumId).map { entities ->
            appResultOf {
                entities.map { it.toModel() }
            }
        }
    }

    override fun syncGalleryMediaFromRemote(
        familyId: String,
        context: Context,
    ): Flow<Result<Unit, AppError>> = flow {
        galleryFirestoreDataSource.galleryMediaSnapshotListener(familyId).collect { result ->
            when (result) {
                is Result.Success -> {
                    if (result.data.items.isEmpty() && result.data.isFromCache) {
                        return@collect
                    }

                    val existingMediaMap = mediaLocalDataSource.getExistingGalleryMediaInfo(familyId)
                    val itemsToDownload = result.data.items.filter { galleryMedia ->
                        val mediaId = galleryMedia.id
                        val existingMediaUri = existingMediaMap[mediaId]?.first
                        existingMediaUri == null
                    }

                    if (itemsToDownload.isEmpty()) {
                        mediaLocalDataSource.updateGalleryMedia(
                            familyId = familyId,
                            items = emptyList(),
                            completeSourceList = result.data.items,
                        )
                        emit(Result.Success(Unit))
                        return@collect
                    }

                    val failedItems = mutableSetOf<String>()
                    val allDownloadedItems: MutableList<Pair<GalleryMedia, File>> = mutableListOf()
                    var remainingItems = itemsToDownload
                    var round = 1

                    while (remainingItems.isNotEmpty() && round <= MAX_DOWNLOAD_ROUNDS) {
                        val roundFailedItems = mutableSetOf<String>()
                        remainingItems.chunked(BATCH_SIZE).forEach { batch ->
                            val batchResults: MutableList<Pair<GalleryMedia, File>> = mutableListOf()
                            coroutineScope {
                                val semaphore = Semaphore(MAX_CONCURRENT_DOWNLOADS)
                                val downloadTasks: List<Deferred<Pair<GalleryMedia, File>?>> = batch.map { galleryMedia ->
                                    async(ioDispatcher) {
                                        semaphore.acquire()
                                        try {
                                            galleryMedia.mediaUrl?.let { url ->
                                                val fallbackExtension = if (galleryMedia is GalleryImage) "jpeg" else "mp4"
                                                val extension = "." + galleryMedia.itemName.substringAfterLast('.', fallbackExtension)
                                                var downloadResult: Result<File, AppError>? = null
                                                var lastException: Exception? = null

                                                repeat(MAX_RETRY_ATTEMPTS) { attempt ->
                                                    if (downloadResult is Result.Success) return@repeat
                                                    try {
                                                        downloadResult = storageDataSource.downloadContentToTempFile(
                                                            context,
                                                            url,
                                                            extension,
                                                        )
                                                        if (downloadResult is Result.Failure && attempt < MAX_RETRY_ATTEMPTS - 1) {
                                                            delay(BASE_RETRY_DELAY * (attempt + 1))
                                                        }
                                                    } catch (e: Exception) {
                                                        lastException = e
                                                        if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                                                            delay(BASE_RETRY_DELAY * (attempt + 1))
                                                        }
                                                    }
                                                }

                                                if (downloadResult is Result.Success) {
                                                    Pair(galleryMedia, downloadResult.data)
                                                } else {
                                                    roundFailedItems.add(galleryMedia.id ?: "unknown")
                                                    if (downloadResult is Result.Failure) {
                                                        Log.d(TAG, "Failed to download ${galleryMedia.itemName}: ${downloadResult.error}")
                                                    } else if (lastException != null) {
                                                        Log.d(TAG, "Failed to download ${galleryMedia.itemName}: ${lastException.message}")
                                                    }
                                                    null
                                                }
                                            }
                                        } finally {
                                            semaphore.release()
                                        }
                                    }
                                }
                                batchResults.addAll(downloadTasks.awaitAll().filterNotNull())
                            }

                            if (batchResults.isNotEmpty()) {
                                mediaLocalDataSource.updateGalleryMedia(familyId, batchResults, completeSourceList = null)
                                allDownloadedItems.addAll(batchResults)
                            }
                        }

                        if (roundFailedItems.isNotEmpty()) {
                            if (round >= MAX_DOWNLOAD_ROUNDS) failedItems.addAll(roundFailedItems)
                            remainingItems = remainingItems.filter { media -> roundFailedItems.contains(media.id) }
                        } else {
                            remainingItems = emptyList()
                        }

                        if (remainingItems.isNotEmpty() && round < MAX_DOWNLOAD_ROUNDS) {
                            delay(ROUND_RETRY_DELAY)
                        }
                        round += 1
                    }

                    mediaLocalDataSource.updateGalleryMedia(
                        familyId = familyId,
                        items = emptyList(),
                        completeSourceList = result.data.items,
                    )

                    if (remainingItems.isNotEmpty()) {
                        failedItems.addAll(remainingItems.mapNotNull { it.id })
                    }
                    if (failedItems.isNotEmpty()) {
                        Log.w(TAG, "${failedItems.size} gallery media items failed to sync: $failedItems")
                    }
                    emit(Result.Success(Unit))
                }

                is Result.Failure -> emit(Result.Failure(result.error))
            }
        }
    }

    override fun downloadMediaToGallery(
        mediaIds: List<String>,
        familyId: String,
    ): Flow<SaveProgress> = flow {
        if (mediaIds.isEmpty()) {
            emit(SaveProgress.Finished(0, 0))
            return@flow
        }
        try {
            val items = mediaLocalDataSource.getMediaFilesForDownload(mediaIds, familyId)
            if (items.isNullOrEmpty()) {
                emit(SaveProgress.Error("No media items found"))
                return@flow
            }

            var successCount = 0
            var failureCount = 0
            items.forEachIndexed { index, (file, mediaItem) ->
                emit(SaveProgress.Loading(current = index + 1, total = items.size))
                if (mediaItem == null) {
                    failureCount++
                    return@forEachIndexed
                }
                when (mediaLocalDataSource.copyMediaToGalleryFolder(file, mediaItem)) {
                    is Result.Success -> successCount++
                    is Result.Failure -> failureCount++
                }
            }
            emit(SaveProgress.Finished(successCount, failureCount))
        } catch (e: Exception) {
            emit(SaveProgress.Error("Unexpected error: ${e.message}"))
        }
    }

    override suspend fun updateAlbum(album: Album): Result<Unit, AppError> {
        return galleryFirestoreDataSource.updateAlbum(album)
    }

    private fun AlbumEntity.toModel() = Album(
        id = id,
        familyId = familyId,
        itemName = itemName,
        lastUpdated = lastUpdated,
        count = count,
    )

    private fun GalleryMediaEntity.toModel(): GalleryMedia =
        when (mediaType) {
            MediaType.IMAGE -> GalleryImage(
                id = id,
                familyId = familyId,
                itemName = itemName,
                lastUpdated = lastUpdated,
                albumId = albumId,
                dateCreated = dateCreated,
                mediaType = MediaType.IMAGE,
                mediaUrl = null,
                mediaUri = mediaUri?.toUri(),
            )

            MediaType.VIDEO -> GalleryVideo(
                id = id,
                familyId = familyId,
                itemName = itemName,
                lastUpdated = lastUpdated,
                albumId = albumId,
                dateCreated = dateCreated,
                mediaType = MediaType.VIDEO,
                mediaUrl = null,
                mediaUri = mediaUri?.toUri(),
                duration = videoDuration,
            )
        }
}
