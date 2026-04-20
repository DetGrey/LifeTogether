package com.example.lifetogether.domain.usecase.gallery

import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.util.Constants
import javax.inject.Inject

class DeleteAlbumUseCase @Inject constructor(
    private val remoteListRepository: RemoteListRepositoryImpl,
    private val deleteMediaUseCase: DeleteMediaUseCase,
) {
    suspend operator fun invoke(
        albumId: String,
        albumMedia: List<GalleryMedia>,
    ): ResultListener {
        try {
            // Step 1: Skip media deletion if empty album
            if (albumMedia.isEmpty()) {
                return deleteAlbum(albumId)
            }
            // Step 2: Delete media files and associated metadata
            return when (val result = deleteMediaUseCase.invoke(albumId, albumMedia, true)) {
                is ResultListener.Success -> {
                    // Step 3: If media deletion was successful, attempt to delete the album item itself
                    deleteAlbum(albumId)
                }
                is ResultListener.Failure -> result
            }
        } catch (e: Exception) {
            return ResultListener.Failure(e.message ?: "Unknown error occurred")
        }
    }
    private suspend fun deleteAlbum(albumId: String): ResultListener {
        return remoteListRepository.deleteItem(albumId, Constants.ALBUMS_TABLE)
    }

}
