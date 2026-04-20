package com.example.lifetogether.domain.usecase.gallery

import com.example.lifetogether.data.repository.ImageRepositoryImpl
import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.util.Constants
import javax.inject.Inject

class DeleteMediaUseCase @Inject constructor(
    private val remoteListRepository: RemoteListRepositoryImpl,
    private val imageRepositoryImpl: ImageRepositoryImpl,
) {
    suspend operator fun invoke(
        albumId: String,
        mediaList: List<GalleryMedia>,
        albumIsToBeDeleted: Boolean = false,
    ): Result<Unit, String> {
        if (mediaList.isEmpty()) return Result.Success(Unit)

        return try {
            // Attempt to delete media files
            val urlList = mediaList.mapNotNull { it.mediaUrl }
            val fileDeleteResult = imageRepositoryImpl.deleteMediaFiles(urlList)

            if (fileDeleteResult is Result.Failure) {
                return Result.Failure(fileDeleteResult.error)
            }

            // Attempt to delete associated media metadata
            val idsList = mediaList.mapNotNull { it.id }
            val dbDeleteResult = remoteListRepository.deleteItems(Constants.GALLERY_MEDIA_TABLE, idsList)

            if (dbDeleteResult is Result.Failure) {
                return Result.Failure(dbDeleteResult.error)
            }

            // Update album count (if necessary)
            if (!albumIsToBeDeleted) {
                val countUpdateResult = remoteListRepository.updateAlbumCount(albumId, -mediaList.size)
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
