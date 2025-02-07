package com.example.lifetogether.domain.usecase.user

import com.example.lifetogether.data.repository.RemoteUserRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import javax.inject.Inject

class StoreFcmTokenUseCase @Inject constructor(
    private val remoteUserRepositoryImpl: RemoteUserRepositoryImpl,
) {
    suspend operator fun invoke(
        uid: String,
        familyId: String,
    ): ResultListener {
        return remoteUserRepositoryImpl.storeFcmToken(uid, familyId)
    }
}
