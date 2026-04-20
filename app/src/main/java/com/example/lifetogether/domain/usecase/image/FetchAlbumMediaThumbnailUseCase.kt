package com.example.lifetogether.domain.usecase.image

import com.example.lifetogether.domain.repository.GalleryRepository
import com.example.lifetogether.domain.result.Result
import javax.inject.Inject

class FetchAlbumMediaThumbnailUseCase @Inject constructor(
    private val galleryRepository: GalleryRepository,
) {
    suspend operator fun invoke(
        mediaId: String,
    ): Result<ByteArray, String> {
        return galleryRepository.getAlbumMediaThumbnail(mediaId)
    }
}
