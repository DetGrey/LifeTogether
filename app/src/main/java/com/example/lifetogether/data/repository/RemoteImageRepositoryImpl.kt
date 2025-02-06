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
    suspend fun uploadImage(
        uri: Uri,
        imageType: ImageType,
        context: Context,
    ): StringResultListener {
        return firebaseStorageDataSource.uploadPhoto(uri, imageType, context)
    }

    suspend fun deleteImage(
        imageType: ImageType,
    ): ResultListener {
        return when (val urlResult = firestoreDataSource.getImageUrl(imageType)) {
            is StringResultListener.Success -> {
                firebaseStorageDataSource.deleteImage(urlResult.string)
            }

            is StringResultListener.Failure -> {
                ResultListener.Failure(urlResult.message)
            }

            null -> ResultListener.Success // Means that there is no image to delete
        }
    }

    suspend fun saveImageDownloadUri(
        url: String,
        imageType: ImageType,
    ): ResultListener {
        return firestoreDataSource.saveImageDownloadUrl(url, imageType)
    }
}
