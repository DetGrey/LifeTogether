package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.logic.AppErrors

import com.example.lifetogether.domain.result.AppError

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.core.net.toUri
import com.example.lifetogether.data.local.dao.GalleryMediaDao
import com.example.lifetogether.data.logic.generateImageThumbnailFromFile
import com.example.lifetogether.data.logic.generateVideoThumbnailFromFile
import com.example.lifetogether.data.model.GalleryMediaEntity
import com.example.lifetogether.di.IoDispatcher
import com.example.lifetogether.domain.model.enums.MediaType
import com.example.lifetogether.domain.model.gallery.MediaDownloadState
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.result.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaLocalDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val galleryMediaDao: GalleryMediaDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    companion object {
        private const val TAG = "MediaLocalDataSource"
    }

    data class PersistedDownloadResult(
        val mediaId: String,
        val error: AppError? = null,
    )

    suspend fun getAlbumMediaThumbnail(mediaId: String): ByteArray? {
        val existing = galleryMediaDao.getMediaThumbnail(mediaId)
        if (existing != null) return existing
        return regenerateAndPersistThumbnail(mediaId)
    }

    fun observeAlbumThumbnails(familyId: String): Flow<Map<String, ByteArray>> =
        galleryMediaDao.observeAlbumThumbnails(familyId).map { list ->
            list.associate { it.albumId to it.thumbnail }
        }

    suspend fun getAlbumThumbnail(albumId: String): ByteArray? {
        val existing = galleryMediaDao.getNewestMediaThumbnailByAlbumId(albumId)
        if (existing != null) return existing
        val latestMediaId = galleryMediaDao.getLatestMediaIdForAlbum(albumId)
        return latestMediaId?.let { regenerateAndPersistThumbnail(it) }
    }

    suspend fun getMediaItemsByIds(ids: List<String>): List<GalleryMediaEntity> =
        ids.mapNotNull { galleryMediaDao.getItemByIdDirect(it) }

    suspend fun deleteMediaItems(ids: List<String>) = galleryMediaDao.deleteItems(ids)

    suspend fun restoreMediaItems(entities: List<GalleryMediaEntity>) = galleryMediaDao.updateItems(entities)

    suspend fun getExistingGalleryMediaInfo(
        familyId: String,
    ): Map<String, GalleryMediaEntity> {
        val entities = galleryMediaDao.getItems(familyId).firstOrNull().orEmpty()
        return entities.associateBy { it.id }
    }

    suspend fun syncRemoteGalleryMetadata(
        familyId: String,
        remoteItems: List<GalleryMedia>,
    ): List<GalleryMedia> {
        val currentRoomItems = galleryMediaDao.getItems(familyId).firstOrNull() ?: emptyList()
        val currentItemsById = currentRoomItems.associateBy { it.id }
        val itemsNeedingDownload = mutableListOf<GalleryMedia>()
        val syncedEntities = remoteItems.map { mediaItem ->
            val existing = currentItemsById[mediaItem.id]
            val existingFileExists = existing?.mediaUri?.let { File(it).exists() } == true
            val remoteIsNewer = existing?.lastUpdated?.before(mediaItem.lastUpdated) == true
            val needsDownload = existing == null ||
                !existingFileExists ||
                existing.downloadState != MediaDownloadState.READY ||
                remoteIsNewer

            if (needsDownload) {
                itemsNeedingDownload.add(mediaItem)
            }

            val nextState = when {
                !needsDownload -> MediaDownloadState.READY
                existingFileExists -> MediaDownloadState.STALE
                else -> MediaDownloadState.PENDING
            }

            buildGalleryEntity(
                mediaItem = mediaItem,
                existing = existing,
                mediaUri = existing?.mediaUri.takeIf { existingFileExists },
                thumbnail = existing?.thumbnail.takeIf { existingFileExists && !remoteIsNewer },
                downloadState = nextState,
                lastDownloadAttempt = existing?.lastDownloadAttempt,
                lastDownloadError = if (needsDownload) existing?.lastDownloadError else null,
            )
        }

        val currentIdsInRoom = currentRoomItems.map { it.id }.toSet()
        val sourceIds = remoteItems.map { it.id }.toSet()
        val idsToDeleteFromRoom = currentIdsInRoom - sourceIds
        val itemsToDeleteFromStorage = currentRoomItems.filter { it.id in idsToDeleteFromRoom }

        itemsToDeleteFromStorage.forEach { itemToDelete ->
            deleteLocalMediaFile(itemToDelete.mediaUri)
        }

        if (idsToDeleteFromRoom.isNotEmpty()) {
            galleryMediaDao.deleteItems(idsToDeleteFromRoom.toList())
        }
        if (syncedEntities.isNotEmpty()) {
            galleryMediaDao.updateItems(syncedEntities)
        }

        return itemsNeedingDownload
    }

    suspend fun markMediaDownloadsPending(
        mediaIds: Collection<String>,
    ) {
        if (mediaIds.isEmpty()) return
        val entities = mediaIds.mapNotNull { galleryMediaDao.getItemByIdDirect(it) }.map { entity ->
            val hasLocalFile = entity.mediaUri?.let { File(it).exists() } == true
            entity.copy(
                downloadState = if (!hasLocalFile) {
                    MediaDownloadState.PENDING
                } else {
                    MediaDownloadState.STALE
                },
                lastDownloadError = null,
            )
        }
        if (entities.isNotEmpty()) {
            galleryMediaDao.updateItems(entities)
        }
    }

    suspend fun markMediaDownloadFailure(
        mediaId: String,
        error: AppError,
        attemptedAt: Date = Date(),
    ) {
        val entity = galleryMediaDao.getItemByIdDirect(mediaId) ?: return
        val hasLocalFile = entity.mediaUri?.let { File(it).exists() } == true
        galleryMediaDao.updateItems(
            listOf(
                entity.copy(
                    downloadState = if (!hasLocalFile) {
                        MediaDownloadState.FAILED
                    } else {
                        MediaDownloadState.STALE
                    },
                    lastDownloadAttempt = attemptedAt,
                    lastDownloadError = error.message,
                ),
            ),
        )
    }

    suspend fun persistDownloadedMedia(
        mediaItem: GalleryMedia,
        downloadedFile: File,
    ): PersistedDownloadResult {
        val existing = galleryMediaDao.getItemByIdDirect(mediaItem.id)
        val previousMediaUri = existing?.mediaUri
        val downloadedAt = Date()
        return try {
            val mediaStoragePathString = saveDownloadedMediaFile(downloadedFile, mediaItem)
                ?: return PersistedDownloadResult(
                    mediaId = mediaItem.id,
                    error = AppErrors.storage("Failed to save ${mediaItem.itemName} locally"),
                )

            val thumbnailBytes = when (mediaItem) {
                is GalleryImage -> generateImageThumbnailFromFile(downloadedFile)
                is GalleryVideo -> generateVideoThumbnailFromFile(downloadedFile)
            }

            val updatedEntity = buildGalleryEntity(
                mediaItem = mediaItem,
                existing = existing,
                mediaUri = mediaStoragePathString,
                thumbnail = thumbnailBytes,
                downloadState = MediaDownloadState.READY,
                lastDownloadAttempt = downloadedAt,
                lastDownloadError = null,
            )
            galleryMediaDao.updateItems(listOf(updatedEntity))

            if (!previousMediaUri.isNullOrBlank() && previousMediaUri != mediaStoragePathString) {
                deleteLocalMediaFile(previousMediaUri)
            }
            PersistedDownloadResult(mediaId = mediaItem.id)
        } catch (e: Exception) {
            PersistedDownloadResult(mediaId = mediaItem.id, error = AppErrors.fromThrowable(e))
        } finally {
            if (downloadedFile.exists()) {
                downloadedFile.delete()
            }
        }
    }

    fun getMediaFilesForDownload(
        mediaIds: List<String>,
        familyId: String,
    ): List<Pair<File, GalleryMedia?>>? {
        return try {
            val pairs: MutableList<Pair<File, GalleryMedia?>> = mutableListOf()
            val mediaItems = galleryMediaDao.getItemsByIds(familyId, mediaIds)
            mediaItems.forEach { mediaItem ->
                mediaItem.mediaUri?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        val domainMediaItem = when (mediaItem.mediaType) {
                            MediaType.IMAGE -> {
                                GalleryImage(
                                    id = mediaItem.id,
                                    familyId = mediaItem.familyId,
                                    itemName = mediaItem.itemName,
                                    lastUpdated = mediaItem.lastUpdated,
                                    albumId = mediaItem.albumId,
                                    dateCreated = mediaItem.dateCreated,
                                    mediaType = mediaItem.mediaType,
                                    mediaUrl = mediaItem.remoteMediaUrl,
                                    mediaUri = mediaItem.mediaUri.toUri(),
                                    downloadState = mediaItem.downloadState,
                                    lastDownloadAttempt = mediaItem.lastDownloadAttempt,
                                    lastDownloadError = mediaItem.lastDownloadError,
                                )
                            }

                            MediaType.VIDEO -> {
                                GalleryVideo(
                                    id = mediaItem.id,
                                    familyId = mediaItem.familyId,
                                    itemName = mediaItem.itemName,
                                    lastUpdated = mediaItem.lastUpdated,
                                    albumId = mediaItem.albumId,
                                    dateCreated = mediaItem.dateCreated,
                                    mediaType = mediaItem.mediaType,
                                    mediaUrl = mediaItem.remoteMediaUrl,
                                    mediaUri = mediaItem.mediaUri.toUri(),
                                    downloadState = mediaItem.downloadState,
                                    lastDownloadAttempt = mediaItem.lastDownloadAttempt,
                                    lastDownloadError = mediaItem.lastDownloadError,
                                    duration = mediaItem.videoDuration,
                                )
                            }
                        }
                        pairs.add(Pair(file, domainMediaItem))
                    }
                }
            }
            pairs
        } catch (e: Exception) {
            Log.e(TAG, "Error getting media files for download: ${e.message}", e)
            null
        }
    }

    fun copyMediaToGalleryFolder(
        mediaFile: File,
        mediaItem: GalleryMedia,
    ): Result<Unit, AppError> {
        val resolver = context.contentResolver
        var mediaStoreUri: Uri? = null

        return try {
            val (collectionUri, relativePath, mimeType) = when (mediaItem) {
                is GalleryImage -> Triple(
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    "Pictures/LifeTogether",
                    "image/jpeg",
                )

                is GalleryVideo -> Triple(
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    "Movies/LifeTogether",
                    "video/mp4",
                )
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, mediaItem.itemName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
                put(MediaStore.MediaColumns.DATE_TAKEN, mediaItem.dateCreated.time)

                if (mediaItem is GalleryVideo) {
                    mediaItem.duration?.let { put(MediaStore.Video.Media.DURATION, it) }
                }
            }

            mediaStoreUri = resolver.insert(collectionUri, contentValues)
                ?: return Result.Failure(AppErrors.storage("Failed to create MediaStore entry"))

            resolver.openOutputStream(mediaStoreUri)?.use { outputStream ->
                mediaFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return Result.Failure(AppErrors.storage("Failed to open output stream"))

            if (mediaItem is GalleryImage) {
                writeImageDateMetadata(mediaStoreUri, mediaItem.dateCreated)
            }

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(mediaStoreUri, contentValues, null, null)
            Result.Success(Unit)
        } catch (e: Exception) {
            mediaStoreUri?.let { uri ->
                try {
                    resolver.delete(uri, null, null)
                } catch (_: Exception) {
                }
            }
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    private fun writeImageDateMetadata(
        mediaStoreUri: Uri,
        dateCreated: Date,
    ) {
        try {
            context.contentResolver.openFileDescriptor(mediaStoreUri, "rw")?.use { fileDescriptor ->
                val exif = ExifInterface(fileDescriptor.fileDescriptor)
                val formattedDate = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(dateCreated)

                exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, formattedDate)
                exif.setAttribute(ExifInterface.TAG_DATETIME, formattedDate)
                exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, formattedDate)
                exif.saveAttributes()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write EXIF date metadata for gallery image: ${e.message}")
        }
    }

    private suspend fun regenerateAndPersistThumbnail(mediaId: String): ByteArray? {
        val entity = galleryMediaDao.getItemByIdDirect(mediaId) ?: return null
        val mediaUri = entity.mediaUri?.toUri() ?: return null

        val tempFile = withContext(ioDispatcher) {
            File.createTempFile("thumb_regen_", "tmp", context.cacheDir)
        }
        return try {
            context.contentResolver.openInputStream(mediaUri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return null

            val regenerated = when (entity.mediaType) {
                MediaType.IMAGE -> generateImageThumbnailFromFile(tempFile)
                MediaType.VIDEO -> generateVideoThumbnailFromFile(tempFile)
            }

            if (regenerated != null) {
                galleryMediaDao.updateItems(listOf(entity.copy(thumbnail = regenerated)))
            }
            regenerated
        } catch (e: Exception) {
            Log.e(TAG, "Failed to regenerate thumbnail for $mediaId: ${e.message}", e)
            null
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private fun saveDownloadedMediaFile(
        mediaFile: File,
        mediaItem: GalleryMedia,
    ): String? {
        return try {
            val mediaDir = File(context.filesDir, "media/${mediaItem.familyId}/${mediaItem.albumId}")
            if (!mediaDir.exists()) {
                mediaDir.mkdirs()
            }

            val fileName = "${mediaItem.id}_${mediaItem.itemName}"
            val destinationFile = File(mediaDir, fileName)
            mediaFile.inputStream().use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving ${mediaItem.itemName} to internal storage: ${e.message}", e)
            null
        }
    }

    private fun deleteLocalMediaFile(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }

    private fun buildGalleryEntity(
        mediaItem: GalleryMedia,
        existing: GalleryMediaEntity?,
        mediaUri: String?,
        thumbnail: ByteArray?,
        downloadState: MediaDownloadState,
        lastDownloadAttempt: Date?,
        lastDownloadError: String?,
    ): GalleryMediaEntity {
        return GalleryMediaEntity(
            id = mediaItem.id,
            mediaType = mediaItem.mediaType,
            familyId = mediaItem.familyId,
            itemName = mediaItem.itemName,
            lastUpdated = mediaItem.lastUpdated,
            albumId = mediaItem.albumId,
            dateCreated = mediaItem.dateCreated,
            mediaUri = mediaUri,
            remoteMediaUrl = mediaItem.mediaUrl,
            downloadState = downloadState,
            lastDownloadAttempt = lastDownloadAttempt,
            lastDownloadError = lastDownloadError,
            thumbnail = thumbnail,
            videoDuration = (mediaItem as? GalleryVideo)?.duration ?: existing?.videoDuration,
        )
    }
}
