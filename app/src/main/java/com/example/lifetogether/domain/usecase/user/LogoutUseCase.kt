package com.example.lifetogether.domain.usecase.user

import com.example.lifetogether.data.repository.RemoteUserRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val remoteUserRepositoryImpl: RemoteUserRepositoryImpl,
) {
    operator fun invoke(uid: String, familyId: String?): ResultListener {
        return remoteUserRepositoryImpl.logout(uid, familyId)
    }
}
