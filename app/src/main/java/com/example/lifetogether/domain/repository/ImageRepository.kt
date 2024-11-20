package com.example.lifetogether.domain.repository

import android.content.Context
import android.net.Uri
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.model.sealed.ImageType

interface ImageRepository {
    suspend fun uploadImage(uri: Uri, imageType: ImageType, context: Context): StringResultListener
    suspend fun saveImageDownloadUri(url: String, imageType: ImageType): ResultListener
}
