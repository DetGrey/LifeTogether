package com.example.lifetogether.data.repository

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.domain.callback.ByteArrayResultListener
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.repository.ImageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalImageRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource,
) : ImageRepository {
    fun getImageByteArray(imageType: ImageType): Flow<ByteArrayResultListener> {
        println("LocalUserRepositoryImpl getImageByteArray")
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
        println("LocalUserRepositoryImpl getAlbumMediaThumbnail")
        val result = localDataSource.getAlbumMediaThumbnail(mediaId)
        return if (result != null) {
            ByteArrayResultListener.Success(result)
        } else {
            ByteArrayResultListener.Failure("No thumbnail found")
        }
    }

    suspend fun getAlbumThumbnail(
        albumId: String,
    ): ByteArrayResultListener {
        println("LocalUserRepositoryImpl getAlbumThumbnail")
        val result = localDataSource.getAlbumThumbnail(albumId)
        return if (result != null) {
            ByteArrayResultListener.Success(result)
        } else {
            ByteArrayResultListener.Failure("No thumbnail found")
        }
    }
}
