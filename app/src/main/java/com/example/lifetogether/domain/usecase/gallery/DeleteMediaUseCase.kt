package com.example.lifetogether.domain.usecase.gallery

import com.example.lifetogether.data.repository.ImageRepositoryImpl
import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.result.Result
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
    ): ResultListener {
        if (mediaList.isEmpty()) return ResultListener.Success

        return try {
            // Attempt to delete media files
            val urlList = mediaList.mapNotNull { it.mediaUrl }
            val fileDeleteResult = imageRepositoryImpl.deleteMediaFiles(urlList)

            if (fileDeleteResult is Result.Failure) {
                return ResultListener.Failure(fileDeleteResult.error)
            }

            // Attempt to delete associated media metadata
            val idsList = mediaList.mapNotNull { it.id }
            val dbDeleteResult = remoteListRepository.deleteItems(Constants.GALLERY_MEDIA_TABLE, idsList)

            if (dbDeleteResult is Result.Failure) {
                return ResultListener.Failure(dbDeleteResult.error)
            }

            // Update album count (if necessary)
            if (!albumIsToBeDeleted) {
                val countUpdateResult = remoteListRepository.updateAlbumCount(albumId, -mediaList.size)
                if (countUpdateResult !is ResultListener.Success) {
                    return countUpdateResult
                }
            }
            ResultListener.Success

        } catch (e: Exception) {
            ResultListener.Failure(e.message ?: "Unknown error occurred")
        }
    }
}
