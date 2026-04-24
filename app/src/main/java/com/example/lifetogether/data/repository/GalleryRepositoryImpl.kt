package com.example.lifetogether.data.repository

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.example.lifetogether.data.local.source.AlbumLocalDataSource
import com.example.lifetogether.data.local.source.MediaLocalDataSource
import com.example.lifetogether.data.model.AlbumEntity
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.enums.MediaType
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.repository.GalleryRepository
import com.example.lifetogether.domain.datasource.StorageDataSource
import com.example.lifetogether.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryRepositoryImpl @Inject constructor(
    private val albumLocalDataSource: AlbumLocalDataSource,
    private val mediaLocalDataSource: MediaLocalDataSource,
    private val firestoreDataSource: FirestoreDataSource,
    private val storageDataSource: StorageDataSource,
) : GalleryRepository {

    companion object {
        private const val TAG = "GalleryRepositoryImpl"
    }

    private val _thumbnailCache = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    override val thumbnailCache: StateFlow<Map<String, ByteArray>> = _thumbnailCache.asStateFlow()

    override fun observeAlbums(familyId: String): Flow<Result<List<Album>, String>> {
        return albumLocalDataSource.observeAlbums(familyId).map { entities ->
            try {
                Result.Success(entities.map { it.toModel() }.sortedBy { it.itemName })
            } catch (e: Exception) {
                Result.Failure(e.message ?: "Unknown mapping error")
            }
        }
    }

    override suspend fun saveAlbum(album: Album): Result<String, String> {
        return firestoreDataSource.saveItem(album, Constants.ALBUMS_TABLE)
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

    override suspend fun getAlbumMediaThumbnail(mediaId: String): Result<ByteArray, String> {
        Log.d(TAG, "getAlbumMediaThumbnail")
        val result = mediaLocalDataSource.getAlbumMediaThumbnail(mediaId)
        return if (result != null) {
            Result.Success(result)
        } else {
            Result.Failure("No thumbnail found")
        }
    }

    override suspend fun saveGalleryMediaMetaData(galleryMedia: List<GalleryMedia>): Result<Unit, String> {
        return when (val result = firestoreDataSource.saveGalleryMediaMetaData(galleryMedia)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(result.error)
        }
    }

    override suspend fun uploadVideo(uri: Uri, path: String, extension: String): Result<String, String> {
        return storageDataSource.uploadVideo(uri, path, extension)
    }

    override fun observeAlbumById(familyId: String, albumId: String): Flow<Result<Album, String>> {
        return albumLocalDataSource.observeAlbumById(familyId, albumId).map { entity ->
            try {
                if (entity != null) {
                    Result.Success(entity.toModel())
                } else {
                    Result.Failure("Album not found")
                }
            } catch (e: Exception) {
                Result.Failure(e.message ?: "Unknown error")
            }
        }
    }

    override fun observeAlbumMedia(familyId: String, albumId: String): Flow<Result<List<GalleryMedia>, String>> {
        return albumLocalDataSource.getAlbumMedia(familyId, albumId).map { entities ->
            try {
                val items: List<GalleryMedia> = entities.map { wrapper ->
                    val entity = wrapper.entity
                    when (entity.mediaType) {
                        MediaType.IMAGE -> GalleryImage(
                            id = entity.id,
                            familyId = entity.familyId,
                            itemName = entity.itemName,
                            lastUpdated = entity.lastUpdated,
                            albumId = entity.albumId,
                            dateCreated = entity.dateCreated,
                            mediaType = MediaType.IMAGE,
                            mediaUrl = null,
                            mediaUri = entity.mediaUri?.toUri(),
                        )
                        MediaType.VIDEO -> GalleryVideo(
                            id = entity.id,
                            familyId = entity.familyId,
                            itemName = entity.itemName,
                            lastUpdated = entity.lastUpdated,
                            albumId = entity.albumId,
                            dateCreated = entity.dateCreated,
                            mediaType = MediaType.VIDEO,
                            mediaUrl = null,
                            mediaUri = entity.mediaUri?.toUri(),
                            duration = entity.videoDuration,
                        )
                    }
                }
                Result.Success(items)
            } catch (e: Exception) {
                Result.Failure(e.message ?: "Unknown mapping error")
            }
        }
    }

    override suspend fun updateAlbum(album: Album): Result<Unit, String> {
        return firestoreDataSource.updateItem(album, Constants.ALBUMS_TABLE)
    }

    private fun AlbumEntity.toModel() = Album(
        id = id,
        familyId = familyId,
        itemName = itemName,
        lastUpdated = lastUpdated,
        count = count,
    )
}
