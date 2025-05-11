package com.example.lifetogether.domain.usecase.image

import com.example.lifetogether.data.repository.LocalImageRepositoryImpl
import com.example.lifetogether.domain.callback.ByteArrayResultListener
import javax.inject.Inject

class FetchAlbumImageByteArrayListUseCase @Inject constructor(
    private val localImageRepositoryImpl: LocalImageRepositoryImpl,
) {
    suspend operator fun invoke(
        imageId: String,
    ): ByteArrayResultListener {
        return localImageRepositoryImpl.getAlbumImageThumbnail(imageId)
    }
}
