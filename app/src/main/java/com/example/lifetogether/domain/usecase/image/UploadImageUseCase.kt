package com.example.lifetogether.domain.usecase.image

import com.example.lifetogether.domain.result.AppError

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.repository.ImageRepository
import com.example.lifetogether.domain.result.Result
import javax.inject.Inject

class UploadImageUseCase @Inject constructor(
    private val imageRepository: ImageRepository,
) {
    private companion object {
        const val TAG = "UploadImageUseCase"
    }

    suspend operator fun invoke(
        uri: Uri,
        imageType: ImageType,
        context: Context,
    ): Result<Unit, AppError> {
        Log.d(TAG, "invoke")
        val firebaseStorageResult = imageRepository.uploadImage(uri, imageType, context)
        Log.d(TAG, "uploadImage completed")
        when (firebaseStorageResult) {
            is Result.Success -> {
                val url = firebaseStorageResult.data
                Log.d(TAG, "image upload returned url; updating references")
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
