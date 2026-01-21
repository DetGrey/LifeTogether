package com.example.lifetogether.domain.repository

import android.content.Context
import android.net.Uri
import com.example.lifetogether.domain.callback.ByteArrayResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.callback.TempFileDownloadResult
import com.example.lifetogether.domain.model.sealed.ImageType

interface StorageRepository {
    suspend fun uploadPhoto(
        uri: Uri,
        imageType: ImageType,
        context: Context,
    ): StringResultListener

    suspend fun fetchImageByteArray(url: String): ByteArrayResultListener

    suspend fun deleteImage(url: String): ResultListener

    suspend fun deleteImages(urlList: List<String>): ResultListener

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
