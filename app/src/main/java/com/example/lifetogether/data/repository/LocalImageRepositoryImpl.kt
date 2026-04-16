package com.example.lifetogether.data.repository

import android.util.Log
import com.example.lifetogether.data.local.source.MediaLocalDataSource
import com.example.lifetogether.data.local.source.RecipeLocalDataSource
import com.example.lifetogether.data.local.source.RoutineListEntryLocalDataSource
import com.example.lifetogether.data.local.source.UserLocalDataSource
import com.example.lifetogether.domain.listener.ByteArrayResultListener
import com.example.lifetogether.domain.model.sealed.ImageType
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
class LocalImageRepositoryImpl @Inject constructor(
    private val userLocalDataSource: UserLocalDataSource,
    private val recipeLocalDataSource: RecipeLocalDataSource,
    private val routineListEntryLocalDataSource: RoutineListEntryLocalDataSource,
    private val mediaLocalDataSource: MediaLocalDataSource,
) {
    companion object {
        private const val TAG = "LocalImageRepositoryImpl"
    }
    fun getImageByteArray(imageType: ImageType): Flow<ByteArrayResultListener> {
        Log.d(TAG, "getImageByteArray")
        val byteArrayFlow = when (imageType) {
            is ImageType.ProfileImage -> userLocalDataSource.getProfileImageByteArray(imageType.uid)
            is ImageType.FamilyImage -> userLocalDataSource.getFamilyImageByteArray(imageType.familyId)
            is ImageType.RecipeImage -> recipeLocalDataSource.getImageByteArray(
                familyId = imageType.familyId,
                recipeId = imageType.recipeId,
            )
            is ImageType.RoutineListEntryImage -> routineListEntryLocalDataSource.getImageByteArray(imageType.entryId)
            is ImageType.GalleryMedia -> flowOf(null)
        }
        return byteArrayFlow.map { byteArray ->
            try {
                if (byteArray != null) {
                    ByteArrayResultListener.Success(byteArray)
                } else {
                    ByteArrayResultListener.Failure("No ByteArray found")
                }
            } catch (e: Exception) {
                ByteArrayResultListener.Failure(e.message ?: "Unknown error")
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
}
