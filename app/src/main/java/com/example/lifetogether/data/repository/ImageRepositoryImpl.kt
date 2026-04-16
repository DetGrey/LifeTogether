package com.example.lifetogether.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.lifetogether.data.local.source.MediaLocalDataSource
import com.example.lifetogether.data.local.source.RecipeLocalDataSource
import com.example.lifetogether.data.local.source.UserListLocalDataSource
import com.example.lifetogether.data.local.source.UserLocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.listener.ByteArrayResultListener
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.listener.StringResultListener
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.repository.StorageRepository
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageRepositoryImpl @Inject constructor(
    private val userLocalDataSource: UserLocalDataSource,
    private val recipeLocalDataSource: RecipeLocalDataSource,
    private val userListLocalDataSource: UserListLocalDataSource,
    private val mediaLocalDataSource: MediaLocalDataSource,
    private val storageRepository: StorageRepository,
    private val firestoreDataSource: FirestoreDataSource,
) {
    companion object {
        private const val TAG = "LocalImageRepositoryImpl"
    }
    fun getImageByteArray(imageType: ImageType): Flow<Result<ByteArray, String>> {
        Log.d(TAG, "getImageByteArray")
        val byteArrayFlow = when (imageType) {
            is ImageType.ProfileImage -> userLocalDataSource.getProfileImageByteArray(imageType.uid)
            is ImageType.FamilyImage -> userLocalDataSource.getFamilyImageByteArray(imageType.familyId)
            is ImageType.RecipeImage -> recipeLocalDataSource.getImageByteArray(
                familyId = imageType.familyId,
                recipeId = imageType.recipeId,
            )
            is ImageType.RoutineListEntryImage -> userListLocalDataSource.observeRoutineImageByteArray(imageType.entryId)
            is ImageType.GalleryMedia -> flowOf(null)
        }
        return byteArrayFlow.map { byteArray ->
            try {
                if (byteArray != null) {
                    Result.Success(byteArray)
                } else {
                    Result.Failure("No ByteArray found")
                }
            } catch (e: Exception) {
                Result.Failure(e.message ?: "Unknown error")
            }
        }
    }

    suspend fun getAlbumMediaThumbnail(
        mediaId: String,
    ): ByteArrayResultListener {
        Log.d(TAG, "getAlbumMediaThumbnail")
        val result = mediaLocalDataSource.getAlbumMediaThumbnail(mediaId)
        return if (result != null) {
            ByteArrayResultListener.Success(result)
        } else {
            ByteArrayResultListener.Failure("No thumbnail found")
        }
    }

    private val _thumbnailCache = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    val thumbnailCache: StateFlow<Map<String, ByteArray>> = _thumbnailCache.asStateFlow()
    suspend fun getAlbumThumbnail(albumId: String) {
        Log.d(TAG, "getAlbumThumbnail")
        // Check if it's already there to avoid unnecessary DB hits
        if (_thumbnailCache.value.containsKey(albumId)) return

        val result = mediaLocalDataSource.getAlbumThumbnail(albumId)
        if (result != null) {
            _thumbnailCache.update { currentMap ->
                currentMap + (albumId to result)
            }
            Log.d(TAG, "Cache updated for album: $albumId. New size: ${_thumbnailCache.value.size}")
        }
    }
    // ------------------- REMOTE
    suspend fun uploadImage(
        uri: Uri,
        imageType: ImageType,
        context: Context,
    ): StringResultListener {
        return storageRepository.uploadPhoto(uri, imageType, context)
    }

    suspend fun deleteImage(
        imageType: ImageType,
    ): ResultListener {
        return when (val urlResult = firestoreDataSource.getImageUrl(imageType)) {
            is StringResultListener.Success -> {
                storageRepository.deleteImage(urlResult.string)
            }

            is StringResultListener.Failure -> {
                ResultListener.Failure(urlResult.message)
            }

            null -> ResultListener.Success // Means that there is no image to delete
        }
    }

    suspend fun deleteMediaFiles(
        urlList: List<String>,
    ): Result<Unit, String> {
        return storageRepository.deleteImages(urlList)
    }

    suspend fun saveImageDownloadUrl(
        url: String,
        imageType: ImageType,
    ): ResultListener {
        return firestoreDataSource.saveImageDownloadUrl(url, imageType)
    }

    suspend fun saveGalleryMediaMetaData(
        galleryMedia: List<GalleryMedia>,
    ): ResultListener {
        return firestoreDataSource.saveGalleryMediaMetaData(galleryMedia)
    }

    // ------------------------------------------------------------------------------- VIDEOS
    suspend fun uploadVideo(
        uri: Uri,
        path: String,
        extension: String,
    ): StringResultListener {
        return storageRepository.uploadVideo(uri, path, extension)
    }
}
