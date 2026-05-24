package com.example.lifetogether.data.repository

import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.data.logic.AppErrorThrowable
import com.example.lifetogether.data.logic.appResultOf
import com.example.lifetogether.domain.result.AppError
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.core.net.toUri
import com.example.lifetogether.data.local.source.AlbumLocalDataSource
import com.example.lifetogether.data.local.source.MediaLocalDataSource
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.data.model.AlbumEntity
import com.example.lifetogether.data.model.GalleryMediaEntity
import com.example.lifetogether.data.remote.GalleryFirestoreDataSource
import com.example.lifetogether.data.repository.internal.stampNow
import com.example.lifetogether.di.IoDispatcher
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.SaveProgress
import com.example.lifetogether.domain.model.enums.MediaType
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.model.gallery.MediaDownloadState
import com.example.lifetogether.domain.repository.GalleryRepository
import com.example.lifetogether.domain.worker.GalleryMediaRetryWorker
import com.example.lifetogether.domain.datasource.StorageDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
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
import java.util.Date
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class GalleryRepositoryImpl @Inject constructor(
    private val albumLocalDataSource: AlbumLocalDataSource,
    private val mediaLocalDataSource: MediaLocalDataSource,
    private val galleryFirestoreDataSource: GalleryFirestoreDataSource,
    private val storageDataSource: StorageDataSource,
    @param:ApplicationContext private val appContext: Context,
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

    private data class GalleryDownloadAttempt(
        val media: GalleryMedia,
        val file: File? = null,
        val error: AppError? = null,
    )

    override fun observeAlbums(familyId: String): Flow<Result<List<Album>, AppError>> {
        return albumLocalDataSource.observeAlbums(familyId).map { entities ->
            appResultOf { entities.map { it.toModel() }.sortedBy { it.itemName } }
        }
    }

    override fun observeAlbumThumbnails(familyId: String): Flow<Map<String, ByteArray>> =
        mediaLocalDataSource.observeAlbumThumbnails(familyId)

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
        val stampedAlbum = album.stampNow()
        albumLocalDataSource.upsertAlbum(stampedAlbum.toEntity())
        return when (val result = galleryFirestoreDataSource.saveAlbum(stampedAlbum)) {
            is Result.Success -> Result.Success(stampedAlbum.id)
            is Result.Failure -> {
                albumLocalDataSource.deleteAlbum(stampedAlbum.id)
                Result.Failure(result.error)
            }
        }
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
        val stampedMedia = galleryMedia.stampNow()
        val entities = stampedMedia.map { it.toPendingEntity() }
        mediaLocalDataSource.restoreMediaItems(entities)
        return when (val result = galleryFirestoreDataSource.saveGalleryMediaMetaData(stampedMedia)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                mediaLocalDataSource.deleteMediaItems(stampedMedia.map { it.id })
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun uploadVideo(uri: Uri, path: String, extension: String): Result<String, AppError> {
        return storageDataSource.uploadVideo(uri, path, extension)
    }

    override suspend fun deleteAlbum(albumId: String): Result<Unit, AppError> {
        val oldEntity = albumLocalDataSource.getAlbumOnce(albumId)
        albumLocalDataSource.deleteAlbum(albumId)
        return when (val result = galleryFirestoreDataSource.deleteAlbum(albumId)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                if (oldEntity != null) albumLocalDataSource.upsertAlbum(oldEntity)
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun deleteGalleryMedia(mediaIds: List<String>): Result<Unit, AppError> {
        val oldEntities = mediaLocalDataSource.getMediaItemsByIds(mediaIds)
        mediaLocalDataSource.deleteMediaItems(mediaIds)
        return when (val result = galleryFirestoreDataSource.deleteGalleryMedia(mediaIds)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                if (oldEntities.isNotEmpty()) mediaLocalDataSource.restoreMediaItems(oldEntities)
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun updateAlbumCount(albumId: String, count: Int): Result<Unit, AppError> {
        val now = Date()
        val oldEntity = albumLocalDataSource.getAlbumOnce(albumId)
        if (oldEntity != null) {
            albumLocalDataSource.upsertAlbum(oldEntity.copy(count = oldEntity.count + count, lastUpdated = now))
        }
        return when (val result = galleryFirestoreDataSource.updateAlbumCount(albumId, count, now)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                if (oldEntity != null) albumLocalDataSource.upsertAlbum(oldEntity)
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun moveMediaToAlbum(
        mediaIdList: Set<String>,
        newAlbumId: String,
        oldAlbumId: String,
    ): Result<Unit, AppError> {
        val now = Date()
        val oldMediaEntities = mediaLocalDataSource.getMediaItemsByIds(mediaIdList.toList())
        val oldNewAlbum = albumLocalDataSource.getAlbumOnce(newAlbumId)
        val oldOldAlbum = albumLocalDataSource.getAlbumOnce(oldAlbumId)

        mediaLocalDataSource.restoreMediaItems(oldMediaEntities.map { it.copy(albumId = newAlbumId, lastUpdated = now) })
        if (oldNewAlbum != null) {
            albumLocalDataSource.upsertAlbum(oldNewAlbum.copy(count = oldNewAlbum.count + mediaIdList.size, lastUpdated = now))
        }
        if (oldOldAlbum != null) {
            albumLocalDataSource.upsertAlbum(oldOldAlbum.copy(count = maxOf(0, oldOldAlbum.count - mediaIdList.size), lastUpdated = now))
        }

        return when (val result = galleryFirestoreDataSource.moveMediaToAlbum(mediaIdList, newAlbumId, oldAlbumId, now)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                mediaLocalDataSource.restoreMediaItems(oldMediaEntities)
                if (oldNewAlbum != null) albumLocalDataSource.upsertAlbum(oldNewAlbum)
                if (oldOldAlbum != null) albumLocalDataSource.upsertAlbum(oldOldAlbum)
                Result.Failure(result.error)
            }
        }
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

                    val itemsToDownload = mediaLocalDataSource
                        .syncRemoteGalleryMetadata(familyId, result.data.items)
                    val syncResult = downloadGalleryMediaItems(itemsToDownload, context, familyId)
                    emit(syncResult)
                }

                is Result.Failure -> emit(Result.Failure(result.error))
            }
        }
    }

    override suspend fun retryGalleryMediaDownloads(
        mediaIds: List<String>,
        familyId: String,
    ): Result<Unit, AppError> {
        if (mediaIds.isEmpty()) {
            return Result.Success(Unit)
        }

        val existingMedia = mediaLocalDataSource.getExistingGalleryMediaInfo(familyId)
        val itemsToRetry = mediaIds.mapNotNull { mediaId ->
            existingMedia[mediaId]?.toModel()
        }
        if (itemsToRetry.isEmpty()) {
            return Result.Failure(AppErrors.notFound("No retryable media found"))
        }

        mediaLocalDataSource.markMediaDownloadsPending(itemsToRetry.map { it.id })
        return downloadGalleryMediaItems(itemsToRetry, appContext, familyId)
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
        val stampedAlbum = album.stampNow()
        val oldEntity = albumLocalDataSource.getAlbumOnce(album.id)
        albumLocalDataSource.upsertAlbum(stampedAlbum.toEntity())
        return when (val result = galleryFirestoreDataSource.updateAlbum(stampedAlbum)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                if (oldEntity != null) albumLocalDataSource.upsertAlbum(oldEntity)
                else albumLocalDataSource.deleteAlbum(stampedAlbum.id)
                Result.Failure(result.error)
            }
        }
    }

    private fun GalleryMedia.toPendingEntity() = GalleryMediaEntity(
        id = id,
        mediaType = mediaType,
        familyId = familyId,
        itemName = itemName,
        lastUpdated = lastUpdated,
        albumId = albumId,
        dateCreated = dateCreated,
        remoteMediaUrl = mediaUrl,
        downloadState = MediaDownloadState.PENDING,
        videoDuration = (this as? GalleryVideo)?.duration,
    )

    private fun Album.toEntity() = AlbumEntity(
        id = id,
        familyId = familyId,
        itemName = itemName,
        lastUpdated = lastUpdated,
        count = count,
    )

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
                mediaUrl = remoteMediaUrl,
                mediaUri = mediaUri?.toUri(),
                downloadState = downloadState,
                lastDownloadAttempt = lastDownloadAttempt,
                lastDownloadError = lastDownloadError,
            )

            MediaType.VIDEO -> GalleryVideo(
                id = id,
                familyId = familyId,
                itemName = itemName,
                lastUpdated = lastUpdated,
                albumId = albumId,
                dateCreated = dateCreated,
                mediaType = MediaType.VIDEO,
                mediaUrl = remoteMediaUrl,
                mediaUri = mediaUri?.toUri(),
                downloadState = downloadState,
                lastDownloadAttempt = lastDownloadAttempt,
                lastDownloadError = lastDownloadError,
                duration = videoDuration,
            )
        }

    private suspend fun downloadGalleryMediaItems(
        itemsToDownload: List<GalleryMedia>,
        context: Context,
        familyId: String,
    ): Result<Unit, AppError> {
        if (itemsToDownload.isEmpty()) {
            return Result.Success(Unit)
        }

        mediaLocalDataSource.markMediaDownloadsPending(itemsToDownload.map { it.id })
        val failedItems = mutableMapOf<String, AppError>()
        var remainingItems = itemsToDownload
        var round = 1

        while (remainingItems.isNotEmpty() && round <= MAX_DOWNLOAD_ROUNDS) {
            val roundFailures = mutableMapOf<String, AppError>()

            remainingItems.chunked(BATCH_SIZE).forEach { batch ->
                val attempts = downloadBatch(batch, context)
                attempts.forEach { attempt ->
                    if (attempt.file != null) {
                        val persisted = mediaLocalDataSource.persistDownloadedMedia(attempt.media, attempt.file)
                        if (persisted.error != null) {
                            roundFailures[attempt.media.id] = persisted.error
                        }
                    } else if (attempt.error != null) {
                        roundFailures[attempt.media.id] = attempt.error
                    }
                }
            }

            if (roundFailures.isEmpty()) {
                remainingItems = emptyList()
            } else {
                failedItems.putAll(roundFailures)
                remainingItems = remainingItems.filter { it.id in roundFailures.keys }
                if (remainingItems.isNotEmpty() && round < MAX_DOWNLOAD_ROUNDS) {
                    mediaLocalDataSource.markMediaDownloadsPending(remainingItems.map { it.id })
                    delay(ROUND_RETRY_DELAY.milliseconds)
                }
            }
            round += 1
        }

        if (remainingItems.isNotEmpty()) {
            remainingItems.forEach { media ->
                val error = failedItems[media.id] ?: AppErrors.storage("Failed to download ${media.itemName}")
                mediaLocalDataSource.markMediaDownloadFailure(media.id, error, Date())
            }
            val failedIds = remainingItems.map { it.id }
            enqueueRetryWork(familyId, failedIds)
            Log.w(TAG, "${failedIds.size} gallery media items failed to sync: $failedIds")
            return Result.Failure(
                AppErrors.storage(
                    "Gallery sync incomplete. ${failedIds.size} media item(s) failed to download.",
                ),
            )
        }

        return Result.Success(Unit)
    }

    private suspend fun downloadBatch(
        batch: List<GalleryMedia>,
        context: Context,
    ): List<GalleryDownloadAttempt> = coroutineScope {
        val semaphore = Semaphore(MAX_CONCURRENT_DOWNLOADS)
        val downloadTasks: List<Deferred<GalleryDownloadAttempt>> = batch.map { galleryMedia ->
            async(ioDispatcher) {
                semaphore.acquire()
                try {
                    downloadSingleMedia(galleryMedia, context)
                } finally {
                    semaphore.release()
                }
            }
        }
        downloadTasks.awaitAll()
    }

    private suspend fun downloadSingleMedia(
        galleryMedia: GalleryMedia,
        context: Context,
    ): GalleryDownloadAttempt {
        val url = galleryMedia.mediaUrl
            ?: return GalleryDownloadAttempt(
                media = galleryMedia,
                error = AppErrors.notFound("Missing remote media URL"),
            )

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
                    delay((BASE_RETRY_DELAY * (attempt + 1)).milliseconds)
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                    delay((BASE_RETRY_DELAY * (attempt + 1)).milliseconds)
                }
            }
        }

        return when (downloadResult) {
            is Result.Success -> GalleryDownloadAttempt(media = galleryMedia, file = downloadResult.data)
            is Result.Failure -> {
                Log.d(TAG, "Failed to download ${galleryMedia.itemName}: ${downloadResult.error}")
                GalleryDownloadAttempt(media = galleryMedia, error = downloadResult.error)
            }
            else -> {
                val error = AppErrors.fromThrowable(lastException ?: IllegalStateException("Unknown media download error"))
                Log.d(TAG, "Failed to download ${galleryMedia.itemName}: ${error.message}")
                GalleryDownloadAttempt(media = galleryMedia, error = error)
            }
        }
    }

    private fun enqueueRetryWork(
        familyId: String,
        mediaIds: List<String>,
    ) {
        if (mediaIds.isEmpty()) return

        val request = OneTimeWorkRequestBuilder<GalleryMediaRetryWorker>()
            .setInputData(GalleryMediaRetryWorker.createInputData(familyId, mediaIds))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
            .addTag("${GalleryMediaRetryWorker.WORK_NAME_PREFIX}-$familyId")
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            "${GalleryMediaRetryWorker.WORK_NAME_PREFIX}-$familyId",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
