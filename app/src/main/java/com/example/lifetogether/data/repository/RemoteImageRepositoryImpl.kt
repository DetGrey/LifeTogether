package com.example.lifetogether.data.repository

import android.content.Context
import android.net.Uri
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.repository.StorageRepository
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.listener.StringResultListener
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.sealed.ImageType
import javax.inject.Inject

class RemoteImageRepositoryImpl @Inject constructor(
    private val storageRepository: StorageRepository,
    private val firestoreDataSource: FirestoreDataSource,
) {
    suspend fun uploadImage(
        uri: Uri,
        imageType: ImageType,
        context: Context,
    ): StringResultListener {
        return storageRepository.uploadPhoto(uri, imageType, context)
    }

    suspend fun deleteImage(
        imageType: ImageType,
    ): ResultListener {
        return when (val urlResult = firestoreDataSource.getImageUrl(imageType)) {
            is StringResultListener.Success -> {
                storageRepository.deleteImage(urlResult.string)
            }

            is StringResultListener.Failure -> {
                ResultListener.Failure(urlResult.message)
            }

            null -> ResultListener.Success // Means that there is no image to delete
        }
    }

    suspend fun deleteMediaFiles(
        urlList: List<String>,
    ): ResultListener {
        return storageRepository.deleteImages(urlList)
    }

    suspend fun saveImageDownloadUrl(
        url: String,
        imageType: ImageType,
    ): ResultListener {
        return firestoreDataSource.saveImageDownloadUrl(url, imageType)
    }

    suspend fun saveGalleryMediaMetaData(
        galleryMedia: List<GalleryMedia>,
    ): ResultListener {
        return firestoreDataSource.saveGalleryMediaMetaData(galleryMedia)
    }

    // ------------------------------------------------------------------------------- VIDEOS
    suspend fun uploadVideo(
        uri: Uri,
        path: String,
        extension: String,
    ): StringResultListener {
        return storageRepository.uploadVideo(uri, path, extension)
    }
}
