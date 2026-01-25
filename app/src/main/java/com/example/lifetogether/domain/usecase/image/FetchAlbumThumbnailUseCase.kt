package com.example.lifetogether.domain.usecase.image

import com.example.lifetogether.data.repository.LocalImageRepositoryImpl
import javax.inject.Inject

class FetchAlbumThumbnailUseCase @Inject constructor(
    private val localImageRepositoryImpl: LocalImageRepositoryImpl,
) {
    suspend operator fun invoke(
        albumId: String,
    ) {
        localImageRepositoryImpl.getAlbumThumbnail(albumId)
    }
}
