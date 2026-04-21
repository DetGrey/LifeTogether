package com.example.lifetogether.domain.usecase.gallery

import com.example.lifetogether.data.logic.AppErrors

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.repository.GalleryRepository
import javax.inject.Inject

class DeleteAlbumUseCase @Inject constructor(
    private val galleryRepository: GalleryRepository,
    private val deleteMediaUseCase: DeleteMediaUseCase,
) {
    suspend operator fun invoke(
        albumId: String,
        albumMedia: List<GalleryMedia>,
    ): Result<Unit, AppError> {
        try {
            // Step 1: Skip media deletion if empty album
            if (albumMedia.isEmpty()) {
                return deleteAlbum(albumId)
            }
            // Step 2: Delete media files and associated metadata
            return when (val result = deleteMediaUseCase.invoke(albumId, albumMedia, true)) {
                is Result.Success -> {
                    // Step 3: If media deletion was successful, attempt to delete the album item itself
                    deleteAlbum(albumId)
                }
                is Result.Failure -> result
            }
        } catch (e: Exception) {
            return Result.Failure(AppErrors.fromThrowable(e))
        }
    }
    private suspend fun deleteAlbum(albumId: String): Result<Unit, AppError> {
        return galleryRepository.deleteAlbum(albumId)
    }

}
