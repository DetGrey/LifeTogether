package com.example.lifetogether.domain.usecase.image

import com.example.lifetogether.data.repository.ImageRepositoryImpl
import com.example.lifetogether.domain.listener.ByteArrayResultListener
import javax.inject.Inject

class FetchAlbumMediaThumbnailUseCase @Inject constructor(
    private val imageRepositoryImpl: ImageRepositoryImpl,
) {
    suspend operator fun invoke(
        mediaId: String,
    ): ByteArrayResultListener {
        return imageRepositoryImpl.getAlbumMediaThumbnail(mediaId)
    }
}
