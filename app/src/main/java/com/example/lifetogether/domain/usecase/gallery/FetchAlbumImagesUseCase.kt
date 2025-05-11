package com.example.lifetogether.domain.usecase.gallery

import com.example.lifetogether.data.repository.LocalListRepositoryImpl
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.model.gallery.GalleryImage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchAlbumImagesUseCase @Inject constructor(
    private val localListRepository: LocalListRepositoryImpl,
) {
    operator fun invoke(
        familyId: String,
        albumId: String,
    ): Flow<ListItemsResultListener<GalleryImage>> {
        println("Inside FetchAlbumImagesUseCase and trying to fetch from local storage")
        // Return a flow that emits updates to the list items
        return localListRepository.fetchAlbumImages(familyId, albumId)
    }
}
