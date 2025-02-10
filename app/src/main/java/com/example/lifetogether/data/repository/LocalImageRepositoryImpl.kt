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
}
