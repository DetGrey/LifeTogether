package com.example.lifetogether.domain.usecase.image

import com.example.lifetogether.domain.repository.GalleryRepository
import javax.inject.Inject

class FetchAlbumThumbnailUseCase @Inject constructor(
    private val galleryRepository: GalleryRepository,
) {
    suspend operator fun invoke(
        albumId: String,
    ) {
        galleryRepository.fetchAlbumThumbnail(albumId)
    }
}
