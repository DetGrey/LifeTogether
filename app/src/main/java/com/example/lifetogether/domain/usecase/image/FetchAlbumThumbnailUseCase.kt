package com.example.lifetogether.domain.usecase.image

import com.example.lifetogether.data.repository.ImageRepositoryImpl
import javax.inject.Inject

class FetchAlbumThumbnailUseCase @Inject constructor(
    private val imageRepositoryImpl: ImageRepositoryImpl,
) {
    suspend operator fun invoke(
        albumId: String,
    ) {
        imageRepositoryImpl.getAlbumThumbnail(albumId)
    }
}
