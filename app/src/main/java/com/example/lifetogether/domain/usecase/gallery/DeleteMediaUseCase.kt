package com.example.lifetogether.domain.usecase.gallery

import com.example.lifetogether.data.logic.AppErrorThrowable
import com.example.lifetogether.data.logic.appResultOfSuspend

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.repository.ImageRepository
import com.example.lifetogether.domain.repository.GalleryRepository
import com.example.lifetogether.domain.result.Result
import javax.inject.Inject

class DeleteMediaUseCase @Inject constructor(
    private val galleryRepository: GalleryRepository,
    private val imageRepository: ImageRepository,
) {
    suspend operator fun invoke(
        albumId: String,
        mediaList: List<GalleryMedia>,
        albumIsToBeDeleted: Boolean = false,
    ): Result<Unit, AppError> {
        return appResultOfSuspend {
            if (mediaList.isEmpty()) return@appResultOfSuspend

            // Attempt to delete media files
            val urlList = mediaList.mapNotNull { it.mediaUrl }
            val fileDeleteResult = imageRepository.deleteMediaFiles(urlList)

            if (fileDeleteResult is Result.Failure) {
                throw AppErrorThrowable(fileDeleteResult.error)
            }

            // Attempt to delete associated media metadata
            val idsList = mediaList.mapNotNull { it.id }
            val dbDeleteResult = galleryRepository.deleteGalleryMedia(idsList)

            if (dbDeleteResult is Result.Failure) {
                throw AppErrorThrowable(dbDeleteResult.error)
            }

            // Update album count (if necessary)
            if (!albumIsToBeDeleted) {
                val countUpdateResult = galleryRepository.updateAlbumCount(albumId, -mediaList.size)
                if (countUpdateResult is Result.Failure) {
                    throw AppErrorThrowable(countUpdateResult.error)
                }
            }
        }
    }
}
