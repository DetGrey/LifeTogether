package com.example.lifetogether.domain.datasource

import android.content.Context
import android.net.Uri
import com.example.lifetogether.domain.listener.ByteArrayResultListener
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.listener.StringResultListener
import com.example.lifetogether.domain.listener.TempFileDownloadResult
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.result.Result

interface StorageDataSource {
    suspend fun uploadPhoto(
        uri: Uri,
        imageType: ImageType,
        context: Context,
    ): StringResultListener

    suspend fun fetchImageByteArray(url: String): ByteArrayResultListener

    suspend fun deleteImage(url: String): ResultListener

    suspend fun deleteImages(urlList: List<String>): Result<Unit, String>

    suspend fun uploadVideo(
        uri: Uri,
        path: String,
        extension: String,
    ): StringResultListener

    suspend fun downloadContentToTempFile(
        context: Context,
        storageUrl: String,
        desiredFileExtension: String,
    ): TempFileDownloadResult
}