package com.example.lifetogether.domain.usecase.image

import com.example.lifetogether.data.repository.LocalUserRepositoryImpl
import com.example.lifetogether.domain.callback.ByteArrayResultListener
import com.example.lifetogether.domain.model.sealed.ImageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class FetchImageByteArrayUseCase @Inject constructor(
    private val localUserRepositoryImpl: LocalUserRepositoryImpl,
) {
    operator fun invoke(
        imageType: ImageType,
    ): Flow<ByteArrayResultListener> {
        println("FetchImageByteArray invoked imageType: $imageType")

        return when (imageType) {
            is ImageType.ProfileImage -> {
                localUserRepositoryImpl.getImageByteArray(imageType.uid)
            }

            is ImageType.FamilyImage -> {
                flowOf(ByteArrayResultListener.Failure("Not implemented")) // TODO
            }
        }
    }
}
