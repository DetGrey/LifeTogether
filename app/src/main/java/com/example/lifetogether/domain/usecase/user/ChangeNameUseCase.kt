package com.example.lifetogether.domain.usecase.user

import com.example.lifetogether.data.repository.RemoteUserRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import javax.inject.Inject

class ChangeNameUseCase @Inject constructor(
    private val remoteUserRepositoryImpl: RemoteUserRepositoryImpl,
) {
    suspend operator fun invoke(
        uid: String,
        familyId: String?,
        newName: String,
    ): ResultListener {
        return remoteUserRepositoryImpl.changeName(uid, familyId, newName)
    }
}
