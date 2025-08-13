package com.example.lifetogether.domain.usecase.gallery

import com.example.lifetogether.data.repository.LocalListRepositoryImpl
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchAlbumMediaUseCase @Inject constructor(
    private val localListRepository: LocalListRepositoryImpl,
) {
    operator fun invoke(
        familyId: String,
        albumId: String,
    ): Flow<ListItemsResultListener<GalleryMedia>> {
        println("Inside FetchAlbumMediaUseCase and trying to fetch from local storage")
        // Return a flow that emits updates to the list items
        return localListRepository.fetchAlbumMedia(familyId, albumId)
    }
}
