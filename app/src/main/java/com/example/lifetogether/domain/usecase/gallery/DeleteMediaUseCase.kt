package com.example.lifetogether.domain.usecase.gallery

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
        if (mediaList.isEmpty()) return Result.Success(Unit)

        return try {
            // Attempt to delete media files
            val urlList = mediaList.mapNotNull { it.mediaUrl }
            val fileDeleteResult = imageRepository.deleteMediaFiles(urlList)

            if (fileDeleteResult is Result.Failure) {
                return Result.Failure(fileDeleteResult.error)
            }

            // Attempt to delete associated media metadata
            val idsList = mediaList.mapNotNull { it.id }
            val dbDeleteResult = galleryRepository.deleteGalleryMedia(idsList)

            if (dbDeleteResult is Result.Failure) {
                return Result.Failure(dbDeleteResult.error)
            }

            // Update album count (if necessary)
            if (!albumIsToBeDeleted) {
                val countUpdateResult = galleryRepository.updateAlbumCount(albumId, -mediaList.size)
                if (countUpdateResult !is Result.Success) {
                    return countUpdateResult
                }
            }
            Result.Success(Unit)

        } catch (e: Exception) {
            Result.Failure(e.message ?: "Unknown error occurred")
        }
    }
}
