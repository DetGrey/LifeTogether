package com.example.lifetogether.domain.usecase.gallery

import com.example.lifetogether.data.repository.LocalListRepositoryImpl
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FetchAlbumMediaUseCase @Inject constructor(
    private val localListRepository: LocalListRepositoryImpl,
) {
    operator fun invoke(
        familyId: String,
        albumId: String,
    ): Flow<ListItemsResultListener<GalleryMedia>> {
        println("Inside FetchAlbumMediaUseCase and trying to fetch from local storage")
        // Return a flow that emits updates to the list items, sorted by dateCreated descending
        return localListRepository.fetchAlbumMedia(familyId, albumId)
            .map { result ->
                when (result) {
                    is ListItemsResultListener.Success -> {
                        val sortedItems = result.listItems.sortedByDescending { it.dateCreated }
                        ListItemsResultListener.Success(sortedItems)
                    }
                    is ListItemsResultListener.Failure -> result
                }
            }
    }
}
