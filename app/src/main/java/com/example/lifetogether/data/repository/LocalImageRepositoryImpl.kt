package com.example.lifetogether.data.repository

import android.util.Log
import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.domain.listener.ByteArrayResultListener
import com.example.lifetogether.domain.model.sealed.ImageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalImageRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource,
) {
    companion object {
        private const val TAG = "LocalImageRepositoryImpl"
    }
    fun getImageByteArray(imageType: ImageType): Flow<ByteArrayResultListener> {
        Log.d(TAG, "getImageByteArray")
        return localDataSource.getImageByteArray(imageType).map { byteArray ->
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
        val result = localDataSource.getAlbumMediaThumbnail(mediaId)
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

        val result = localDataSource.getAlbumThumbnail(albumId)
        if (result != null) {
            _thumbnailCache.update { currentMap ->
                currentMap + (albumId to result)
            }
            Log.d(TAG, "Cache updated for album: $albumId. New size: ${_thumbnailCache.value.size}")
        }
    }
}
