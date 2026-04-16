package com.example.lifetogether.domain.usecase.image

import com.example.lifetogether.data.repository.ImageRepositoryImpl
import com.example.lifetogether.domain.listener.ByteArrayResultListener
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FetchImageByteArrayUseCase @Inject constructor(
    private val imageRepositoryImpl: ImageRepositoryImpl,
) {
    operator fun invoke(
        imageType: ImageType,
    ): Flow<ByteArrayResultListener> {
        println("FetchImageByteArray invoked imageType: $imageType")
        return imageRepositoryImpl.getImageByteArray(imageType).map {
            when (it) {
                is Result.Success -> ByteArrayResultListener.Success(it.data)
                is Result.Failure -> ByteArrayResultListener.Failure(it.error)
            }
        }
    }
}
