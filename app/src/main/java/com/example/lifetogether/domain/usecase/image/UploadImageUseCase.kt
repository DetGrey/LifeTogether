package com.example.lifetogether.domain.usecase.image

import android.content.Context
import android.net.Uri
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.repository.ImageRepository
import com.example.lifetogether.domain.result.Result
import javax.inject.Inject

class UploadImageUseCase @Inject constructor(
    private val imageRepository: ImageRepository,
) {
    suspend operator fun invoke(
        uri: Uri,
        imageType: ImageType,
        context: Context,
    ): Result<Unit, String> {
        println("UploadImageUseCase uri: $uri")
        val firebaseStorageResult = imageRepository.uploadImage(uri, imageType, context)
        println("UploadImageUseCase firebaseStorageResult: $firebaseStorageResult")
        when (firebaseStorageResult) {
            is Result.Success -> {
                val url = firebaseStorageResult.data
                println("UploadImageUseCase image download url: $url")
                val firestoreDeleteOldImageResult = imageRepository.deleteImage(imageType)

                val firestoreNewUrlResult = imageRepository.saveImageDownloadUrl(url, imageType)

                if (firestoreDeleteOldImageResult is Result.Failure && firestoreNewUrlResult is Result.Success) {
                    return firestoreDeleteOldImageResult
                }
                return firestoreNewUrlResult
            }
            is Result.Failure -> {
                return Result.Failure(firebaseStorageResult.error)
            }
        }
    }
}
