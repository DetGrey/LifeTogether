package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.result.AppError

import android.content.Context
import android.net.Uri
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.sealed.ImageType
import kotlinx.coroutines.flow.Flow

interface ImageRepository {
    fun observeImageByteArray(imageType: ImageType): Flow<Result<ByteArray, AppError>>
    suspend fun uploadImage(uri: Uri,
        imageType: ImageType,
        context: Context,
    ): Result<String, AppError>
    suspend fun deleteImage(imageType: ImageType): Result<Unit, AppError>
    suspend fun deleteMediaFiles(
        urlList: List<String>,
    ): Result<Unit, AppError>
    suspend fun saveImageDownloadUrl(
        url: String,
        imageType: ImageType,
    ): Result<Unit, AppError>
}
