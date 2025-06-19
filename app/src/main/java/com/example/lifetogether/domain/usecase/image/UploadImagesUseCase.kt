package com.example.lifetogether.domain.usecase.image

import android.content.Context
import com.example.lifetogether.data.repository.RemoteImageRepositoryImpl
import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.sealed.ImageType
import javax.inject.Inject

class UploadImagesUseCase @Inject constructor(
    private val remoteImageRepositoryImpl: RemoteImageRepositoryImpl,
    private val remoteListRepositoryImpl: RemoteListRepositoryImpl,
) {
    suspend operator fun invoke(
        imageType: ImageType,
        context: Context,
    ): ResultListener {
        when (imageType) {
            is ImageType.GalleryImage -> {
                val pairs = imageType.galleryImageUploadData
                println("UploadImagesUseCase uris: ${pairs.map { it.uri }}")

                val results = mutableListOf<ResultListener>()
                val imagesToUpload: MutableList<GalleryImage> = mutableListOf()

                for ((uri, image) in pairs) {
                    val firebaseStorageResult = remoteImageRepositoryImpl.uploadImage(uri, imageType, context)
                    println("UploadImagesUseCase firebaseStorageResult: $firebaseStorageResult")
                    when (firebaseStorageResult) {
                        is StringResultListener.Success -> {
                            val url = firebaseStorageResult.string
                            println("UploadImageUseCase image download url: $url")
                            imagesToUpload.add(image.copy(imageUrl = url))
                        }

                        is StringResultListener.Failure -> {
                            results.add(ResultListener.Failure(firebaseStorageResult.message))
                        }
                    }
                }

                val imageTypeUpdated = imageType.copy(galleryImageUploadData = listOf())
                val saveMetaDataResult = remoteImageRepositoryImpl.saveImagesMetaData(imageTypeUpdated, imagesToUpload)
                val updateAlbumCountResult = remoteListRepositoryImpl.updateAlbumCount(imageType.albumId, imagesToUpload.size)

                return saveMetaDataResult
            }
            else -> return ResultListener.Failure("Wrong ImageType")
        }
    }
}
