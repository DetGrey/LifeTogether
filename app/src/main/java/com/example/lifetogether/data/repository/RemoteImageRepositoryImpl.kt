package com.example.lifetogether.data.repository

import android.content.Context
import android.net.Uri
import com.example.lifetogether.data.remote.FirebaseStorageDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.repository.ImageRepository
import javax.inject.Inject

class RemoteImageRepositoryImpl @Inject constructor(
    private val firebaseStorageDataSource: FirebaseStorageDataSource,
    private val firestoreDataSource: FirestoreDataSource,
) : ImageRepository {
    override suspend fun uploadImage(
        uri: Uri,
        imageType: ImageType,
        context: Context,
    ): StringResultListener {
        return firebaseStorageDataSource.uploadPhoto(uri, imageType, context)
    }

    override suspend fun saveImageDownloadUri(
        url: String,
        imageType: ImageType,
    ): ResultListener {
        return firestoreDataSource.saveImageDownloadUrl(url, imageType)
    }
}
