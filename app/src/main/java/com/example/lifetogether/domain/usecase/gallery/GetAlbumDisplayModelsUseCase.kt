package com.example.lifetogether.domain.usecase.gallery

import android.util.Log
import com.example.lifetogether.data.repository.ImageRepositoryImpl
import com.example.lifetogether.domain.listener.AlbumUiModelResultListener
import com.example.lifetogether.domain.listener.ListItemsResultListener
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.usecase.item.FetchListItemsUseCase
import com.example.lifetogether.ui.model.AlbumUiModel
import com.example.lifetogether.util.Constants
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import javax.inject.Inject

class GetAlbumDisplayModelsUseCase @Inject constructor(
    private val fetchListItemsUseCase: FetchListItemsUseCase,
    private val imageRepositoryImpl: ImageRepositoryImpl,
) {
    @OptIn(FlowPreview::class)
    operator fun invoke(familyId: String): Flow<AlbumUiModelResultListener> {
        Log.d("GetAlbumDisplayModelsUseCase", "invoke")

        return combine(
            fetchListItemsUseCase(familyId, Constants.ALBUMS_TABLE, Album::class),
            imageRepositoryImpl.thumbnailCache
        ) { listResult, thumbnailCache ->

            if (listResult is ListItemsResultListener.Success) {
                // Determine how many thumbnails we actually have (for debugging)
                val albumIds = listResult.listItems.filterIsInstance<Album>().map { it.id }
                val cachedCount = albumIds.count { thumbnailCache.containsKey(it) }
                Log.d(
                    "GetAlbumDisplayModelsUseCase",
                    "Merging lists. Albums: ${albumIds.size}, Cached Thumbs: $cachedCount"
                )

                val albums = listResult.listItems.filterIsInstance<Album>().map { album ->
                    AlbumUiModel(
                        id = album.id ?: "",
                        familyId = album.familyId,
                        name = album.itemName,
                        lastUpdated = album.lastUpdated,
                        mediaCount = album.count,
                        thumbnail = thumbnailCache[album.id]
                    )
                }
                AlbumUiModelResultListener.Success(albums)
            } else {
                val message =
                    (listResult as? ListItemsResultListener.Failure)?.message ?: "Unknown error"
                AlbumUiModelResultListener.Failure(message)
            }
        }.debounce(100L) // Waits 100ms for updates to settle.
    }
}
