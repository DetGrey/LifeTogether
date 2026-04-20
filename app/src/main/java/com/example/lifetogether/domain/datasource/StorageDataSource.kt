package com.example.lifetogether.domain.datasource

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
    ): Result<String, String>

    suspend fun fetchImageByteArray(url: String): Result<ByteArray, String>

    suspend fun deleteImage(url: String): Result<Unit, String>

    suspend fun deleteImages(urlList: List<String>): Result<Unit, String>

    suspend fun uploadVideo(
        uri: Uri,
        path: String,
        extension: String,
    ): Result<String, String>

    suspend fun downloadContentToTempFile(
        context: Context,
        storageUrl: String,
        desiredFileExtension: String,
    ): Result<File, String>
}
