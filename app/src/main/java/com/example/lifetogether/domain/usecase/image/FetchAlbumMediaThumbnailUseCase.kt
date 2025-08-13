package com.example.lifetogether.domain.usecase.image

import com.example.lifetogether.data.repository.LocalImageRepositoryImpl
import com.example.lifetogether.domain.callback.ByteArrayResultListener
import javax.inject.Inject

class FetchAlbumMediaThumbnailUseCase @Inject constructor(
    private val localImageRepositoryImpl: LocalImageRepositoryImpl,
) {
    suspend operator fun invoke(
        mediaId: String,
    ): ByteArrayResultListener {
        return localImageRepositoryImpl.getAlbumMediaThumbnail(mediaId)
    }
}
