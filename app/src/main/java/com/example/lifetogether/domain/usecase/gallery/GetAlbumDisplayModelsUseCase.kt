package com.example.lifetogether.domain.usecase.gallery

import android.util.Log
import com.example.lifetogether.domain.repository.GalleryRepository
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.ui.model.AlbumUiModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import javax.inject.Inject

class GetAlbumDisplayModelsUseCase @Inject constructor(
    private val galleryRepository: GalleryRepository,
) {
    @OptIn(FlowPreview::class)
    operator fun invoke(familyId: String): Flow<Result<List<AlbumUiModel>, AppError>> {
        Log.d("GetAlbumDisplayModelsUseCase", "invoke")

        return combine(
            galleryRepository.observeAlbums(familyId),
            galleryRepository.thumbnailCache
        ) { albumsResult, thumbnailCache ->

            when (albumsResult) {
                is Result.Success -> {
                    val cachedCount = albumsResult.data.count { thumbnailCache.containsKey(it.id) }
                    Log.d(
                        "GetAlbumDisplayModelsUseCase",
                        "Merging lists. Albums: ${albumsResult.data.size}, Cached Thumbs: $cachedCount"
                    )

                    val albums = albumsResult.data.map { album ->
                        AlbumUiModel(
                            id = album.id ?: "",
                            familyId = album.familyId,
                            name = album.itemName,
                            lastUpdated = album.lastUpdated,
                            mediaCount = album.count,
                            thumbnail = thumbnailCache[album.id]
                        )
                    }
                    Result.Success(albums)
                }
                is Result.Failure -> albumsResult
            }
        }.debounce(100L) // Waits 100ms for updates to settle.
    }
}
