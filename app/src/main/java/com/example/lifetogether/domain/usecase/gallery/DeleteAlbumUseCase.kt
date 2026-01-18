package com.example.lifetogether.domain.usecase.gallery

import com.example.lifetogether.data.repository.RemoteImageRepositoryImpl
import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.util.Constants
import javax.inject.Inject

class DeleteAlbumUseCase @Inject constructor(
    private val remoteListRepository: RemoteListRepositoryImpl,
    private val remoteImageRepositoryImpl: RemoteImageRepositoryImpl,
) {
    suspend operator fun invoke(
        albumId: String,
        albumMedia: List<GalleryMedia>,
    ): ResultListener {
        try {
            // Step 0: Skip media deletion if empty album
            if (albumMedia.isEmpty()) {
                return remoteListRepository.deleteItem(albumId, Constants.ALBUMS_TABLE)
            }

            // Step 1: Attempt to delete media files
            val urlList = albumMedia.mapNotNull { it.mediaUrl }
            var result = remoteImageRepositoryImpl.deleteMediaFiles(urlList)

            if (result is ResultListener.Success) {
                // Step 2: Attempt to delete associated media metadata
                val idsList = albumMedia.mapNotNull { it.id }
                result = remoteListRepository.deleteItems(Constants.ALBUMS_TABLE, idsList)

                if (result is ResultListener.Success) {
                    // Step 3: If media deletion was successful, attempt to delete the album item itself
                    result = remoteListRepository.deleteItem(albumId, Constants.ALBUMS_TABLE)
                }
            }
            return result
        } catch (e: Exception) {
            return ResultListener.Failure(e.message ?: "Unknown error occurred")
        }
    }
}
