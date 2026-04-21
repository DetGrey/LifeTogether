package com.example.lifetogether.data.local.source

import com.example.lifetogether.domain.result.AppError

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import com.example.lifetogether.data.local.dao.GalleryMediaDao
import com.example.lifetogether.data.logic.generateImageThumbnailFromFile
import com.example.lifetogether.data.logic.generateVideoThumbnailFromFile
import com.example.lifetogether.data.model.GalleryMediaEntity
import com.example.lifetogether.di.IoDispatcher
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.enums.MediaType
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
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

    suspend fun getAlbumMediaThumbnail(mediaId: String): ByteArray? {
        val existing = galleryMediaDao.getMediaThumbnail(mediaId)
        if (existing != null) return existing
        return regenerateAndPersistThumbnail(mediaId)
    }

    suspend fun getAlbumThumbnail(albumId: String): ByteArray? {
        val existing = galleryMediaDao.getNewestMediaThumbnailByAlbumId(albumId)
        if (existing != null) return existing
        val latestMediaId = galleryMediaDao.getLatestMediaIdForAlbum(albumId)
        return latestMediaId?.let { regenerateAndPersistThumbnail(it) }
    }

    suspend fun getExistingGalleryMediaInfo(
        familyId: String,
    ): Map<String, Pair<String?, String?>> {
        val entities = galleryMediaDao.getExistingMediaIdsWithUris(familyId)
        return entities.associate { entity -> entity.id to Pair(entity.mediaUri, null) }
    }

    suspend fun updateGalleryMedia(
        familyId: String,
        items: List<Pair<GalleryMedia, File>>,
        completeSourceList: List<GalleryMedia>? = null,
    ) {
        if (items.isEmpty() && completeSourceList == null) return

        val currentRoomItems = galleryMediaDao.getItems(familyId).firstOrNull() ?: emptyList()
        val currentMediaStoreUrisMap = currentRoomItems.associateBy({ it.id }, { it.mediaUri })
        val entityList = mutableListOf<GalleryMediaEntity>()

        if (items.isEmpty() && completeSourceList != null) {
            val currentItemsById = currentRoomItems.associateBy { it.id }
            completeSourceList.forEach { mediaItem ->
                val existing = mediaItem.id?.let { currentItemsById[it] } ?: return@forEach
                entityList.add(
                    existing.copy(
                        albumId = mediaItem.albumId,
                        itemName = mediaItem.itemName,
                        lastUpdated = mediaItem.lastUpdated,
                    ),
                )
            }
        }

        for ((mediaItem, downloadedFile) in items) {
            val mediaStoragePathString: String? =
                currentMediaStoreUrisMap[mediaItem.id] ?: saveMediaFileToInternalStorage(downloadedFile, mediaItem)

            if (mediaStoragePathString == null && currentMediaStoreUrisMap[mediaItem.id] == null) {
                continue
            }

            val thumbnailBytes = when (mediaItem) {
                is GalleryImage -> generateImageThumbnailFromFile(downloadedFile)
                is GalleryVideo -> generateVideoThumbnailFromFile(downloadedFile)
            }

            entityList.add(
                GalleryMediaEntity(
                    id = mediaItem.id!!,
                    mediaType = mediaItem.mediaType,
                    familyId = mediaItem.familyId,
                    itemName = mediaItem.itemName,
                    lastUpdated = mediaItem.lastUpdated,
                    albumId = mediaItem.albumId,
                    dateCreated = mediaItem.dateCreated,
                    mediaUri = mediaStoragePathString,
                    thumbnail = thumbnailBytes,
                    videoDuration = (mediaItem as? GalleryVideo)?.duration,
                ),
            )

            if (downloadedFile.exists()) {
                downloadedFile.delete()
            }
        }

        val itemsToUpdateOrInsert = entityList.filter { newEntity ->
            currentRoomItems.none { currentEntity ->
                newEntity.id == currentEntity.id &&
                    newEntity.mediaUri == currentEntity.mediaUri &&
                    newEntity.itemName == currentEntity.itemName &&
                    newEntity.albumId == currentEntity.albumId &&
                    newEntity.lastUpdated == currentEntity.lastUpdated &&
                    (newEntity.thumbnail?.contentEquals(currentEntity.thumbnail ?: byteArrayOf())
                        ?: (currentEntity.thumbnail == null))
            }
        }

        if (completeSourceList != null) {
            val currentIdsInRoom = currentRoomItems.map { it.id }.toSet()
            val sourceIds = completeSourceList.mapNotNull { it.id }.toSet()
            val idsToDeleteFromRoom = currentIdsInRoom - sourceIds
            val itemsToDeleteFromStorage = currentRoomItems.filter { it.id in idsToDeleteFromRoom }

            itemsToDeleteFromStorage.forEach { itemToDelete ->
                itemToDelete.mediaUri?.let { uriString ->
                    try {
                        val file = File(uriString)
                        if (file.exists()) file.delete()
                    } catch (_: Exception) {
                    }
                }
            }

            if (idsToDeleteFromRoom.isNotEmpty()) {
                galleryMediaDao.deleteItems(idsToDeleteFromRoom.toList())
            }
        }

        if (itemsToUpdateOrInsert.isNotEmpty()) {
            galleryMediaDao.updateItems(itemsToUpdateOrInsert)
        }
    }

    fun getMediaFilesForDownload(
        mediaIds: List<String>,
        familyId: String,
    ): List<Pair<File, GalleryMedia?>>? {
        return try {
            val pairs: MutableList<Pair<File, GalleryMedia?>> = mutableListOf()
            val mediaItems = galleryMediaDao.getItemsByIds(familyId, mediaIds)
            mediaItems?.forEach { mediaItem ->
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

                mediaItem.dateCreated?.time?.let {
                    put(MediaStore.MediaColumns.DATE_TAKEN, it)
                }
                if (mediaItem is GalleryVideo) {
                    mediaItem.duration?.let { put(MediaStore.Video.Media.DURATION, it) }
                }
            }

            mediaStoreUri = resolver.insert(collectionUri, contentValues)
                ?: return Result.Failure("Failed to create MediaStore entry")

            resolver.openOutputStream(mediaStoreUri)?.use { outputStream ->
                mediaFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return Result.Failure("Failed to open output stream")

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(mediaStoreUri, contentValues, null, null)
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving media: ${e.message}", e)
            mediaStoreUri?.let { uri ->
                try {
                    resolver.delete(uri, null, null)
                } catch (_: Exception) {
                }
            }
            Result.Failure("Failed to save file: ${e.message}")
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

    private fun saveMediaFileToInternalStorage(
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
}
