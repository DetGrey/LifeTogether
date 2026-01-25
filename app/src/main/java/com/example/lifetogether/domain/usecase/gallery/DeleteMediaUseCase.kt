package com.example.lifetogether.domain.usecase.gallery

import com.example.lifetogether.data.repository.RemoteImageRepositoryImpl
import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.util.Constants
import javax.inject.Inject

class DeleteMediaUseCase @Inject constructor(
    private val remoteListRepository: RemoteListRepositoryImpl,
    private val remoteImageRepositoryImpl: RemoteImageRepositoryImpl,
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
            val fileDeleteResult = remoteImageRepositoryImpl.deleteMediaFiles(urlList)

            if (fileDeleteResult !is ResultListener.Success) {
                return fileDeleteResult
            }

            // Attempt to delete associated media metadata
            val idsList = mediaList.mapNotNull { it.id }
            val dbDeleteResult = remoteListRepository.deleteItems(Constants.GALLERY_MEDIA_TABLE, idsList)

            if (dbDeleteResult !is ResultListener.Success) {
                return dbDeleteResult
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
