package com.example.lifetogether.domain.usecase.image

import com.example.lifetogether.data.repository.LocalImageRepositoryImpl
import com.example.lifetogether.domain.callback.ByteArrayResultListener
import com.example.lifetogether.domain.model.sealed.ImageType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchImageByteArrayUseCase @Inject constructor(
    private val localImageRepositoryImpl: LocalImageRepositoryImpl,
) {
    operator fun invoke(
        imageType: ImageType,
    ): Flow<ByteArrayResultListener> {
        println("FetchImageByteArray invoked imageType: $imageType")
        return localImageRepositoryImpl.getImageByteArray(imageType)
    }
}
