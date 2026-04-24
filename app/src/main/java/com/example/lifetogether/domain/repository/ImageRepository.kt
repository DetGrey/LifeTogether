package com.example.lifetogether.domain.repository

import android.content.Context
import android.net.Uri
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.sealed.ImageType
import kotlinx.coroutines.flow.Flow

interface ImageRepository {
    fun getImageByteArray(imageType: ImageType): Flow<Result<ByteArray, String>>
    suspend fun uploadImage(uri: Uri,
        imageType: ImageType,
        context: Context,
    ): Result<String, String>
    suspend fun deleteImage(imageType: ImageType): Result<Unit, String>
    suspend fun deleteMediaFiles(
        urlList: List<String>,
    ): Result<Unit, String>
    suspend fun saveImageDownloadUrl(
        url: String,
        imageType: ImageType,
    ): Result<Unit, String>
}
