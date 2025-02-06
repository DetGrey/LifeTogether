package com.example.lifetogether.domain.usecase.image

import android.content.Context
import android.net.Uri
import com.example.lifetogether.data.repository.RemoteImageRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.model.sealed.ImageType
import javax.inject.Inject

class UploadImageUseCase @Inject constructor(
    private val remoteImageRepositoryImpl: RemoteImageRepositoryImpl,
) {
    suspend operator fun invoke(
        uri: Uri,
        imageType: ImageType,
        context: Context,
    ): ResultListener {
        println("UploadImageUseCase uri: $uri")
        val firebaseStorageResult = remoteImageRepositoryImpl.uploadImage(uri, imageType, context)
        println("UploadImageUseCase firebaseStorageResult: $firebaseStorageResult")
        when (firebaseStorageResult) {
            is StringResultListener.Success -> {
                val url = firebaseStorageResult.string
                println("UploadImageUseCase image download url: $url")
                val firestoreDeleteOldImageResult = remoteImageRepositoryImpl.deleteImage(imageType)

                val firestoreNewUrlResult = remoteImageRepositoryImpl.saveImageDownloadUri(url, imageType)

                if (firestoreDeleteOldImageResult is ResultListener.Failure && firestoreNewUrlResult is ResultListener.Success) {
                    return firestoreDeleteOldImageResult
                }
                return firestoreNewUrlResult
            }
            is StringResultListener.Failure -> {
                return ResultListener.Failure(firebaseStorageResult.message)
            }
        }
    }
}
