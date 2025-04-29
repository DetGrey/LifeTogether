package com.example.lifetogether.domain.usecase.image

import android.content.Context
import android.net.Uri
import com.example.lifetogether.data.repository.RemoteImageRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.model.sealed.ImageType
import javax.inject.Inject

class UploadImagesUseCase @Inject constructor(
    private val remoteImageRepositoryImpl: RemoteImageRepositoryImpl,
) {
    suspend operator fun invoke(
        uris: List<Uri>,
        imageType: ImageType,
        context: Context,
    ): ResultListener {
        println("UploadImageUseCase uri: $uris")
        val results = mutableListOf<ResultListener>()

        for (uri in uris) {
            val firebaseStorageResult = remoteImageRepositoryImpl.uploadImage(uri, imageType, context)
            println("UploadImageUseCase firebaseStorageResult: $firebaseStorageResult")
            when (firebaseStorageResult) {
                is StringResultListener.Success -> {
                    val url = firebaseStorageResult.string
                    println("UploadImageUseCase image download url: $url")
                    val firestoreNewUrlResult =
                        when (imageType) {
                            is ImageType.GalleryImage -> {
                                val images = imageType.galleryImages.map { it.second.copy(imageUrl = url) }
                                remoteImageRepositoryImpl.saveImagesMetaData(imageType, images)
                            }
                            else -> remoteImageRepositoryImpl.saveImageDownloadUri(url, imageType)
                        }

                    results.add(firestoreNewUrlResult)
                }

                is StringResultListener.Failure -> {
                    results.add(ResultListener.Failure(firebaseStorageResult.message))
                }
            }
        }
        return if (results.all { it is ResultListener.Success }) {
            ResultListener.Success
        } else {
            val failures = results.filterIsInstance<ResultListener.Failure>()
            println("UploadImageUseCase failure results: $failures")
            ResultListener.Failure("Could not upload images - ${failures[0]}")
        }
    }
}
