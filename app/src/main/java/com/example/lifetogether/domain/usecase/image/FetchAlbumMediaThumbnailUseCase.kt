package com.example.lifetogether.domain.usecase.image

import com.example.lifetogether.domain.listener.ByteArrayResultListener
import com.example.lifetogether.domain.repository.GalleryRepository
import javax.inject.Inject

class FetchAlbumMediaThumbnailUseCase @Inject constructor(
    private val galleryRepository: GalleryRepository,
) {
    suspend operator fun invoke(
        mediaId: String,
    ): ByteArrayResultListener {
        return galleryRepository.getAlbumMediaThumbnail(mediaId)
    }
}
