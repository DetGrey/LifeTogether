package com.example.lifetogether.domain.datasource

import com.example.lifetogether.domain.result.AppError

import android.content.Context
import android.net.Uri
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.sealed.ImageType
import java.io.File

interface StorageDataSource {
    suspend fun uploadPhoto(
        uri: Uri,
        imageType: ImageType,
        context: Context,
    ): Result<String, AppError>

    suspend fun fetchImageByteArray(url: String): Result<ByteArray, AppError>

    suspend fun deleteImage(url: String): Result<Unit, AppError>

    suspend fun deleteImages(urlList: List<String>): Result<Unit, AppError>

    suspend fun uploadVideo(
        uri: Uri,
        path: String,
        extension: String,
    ): Result<String, AppError>

    suspend fun downloadContentToTempFile(
        context: Context,
        storageUrl: String,
        desiredFileExtension: String,
    ): Result<File, AppError>
}
