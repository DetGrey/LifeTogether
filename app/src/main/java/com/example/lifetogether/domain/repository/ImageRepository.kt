package com.example.lifetogether.domain.repository

import android.content.Context
import android.net.Uri
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.listener.StringResultListener
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface ImageRepository {
    fun getImageByteArray(imageType: ImageType): Flow<Result<ByteArray, String>>
    suspend fun uploadImage(uri: Uri,
        imageType: ImageType,
        context: Context,
    ): StringResultListener
    suspend fun deleteImage(imageType: ImageType): ResultListener
    suspend fun deleteMediaFiles(
        urlList: List<String>,
    ): Result<Unit, String>
    suspend fun saveImageDownloadUrl(
        url: String,
        imageType: ImageType,
    ): ResultListener
}
