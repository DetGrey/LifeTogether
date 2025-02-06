package com.example.lifetogether.domain.usecase.family

import com.example.lifetogether.data.repository.RemoteUserRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import javax.inject.Inject

data class JoinFamilyUseCase @Inject constructor(
    private val remoteUserRepositoryImpl: RemoteUserRepositoryImpl,
) {
    suspend operator fun invoke(
        familyId: String,
        uid: String,
        name: String,
    ): ResultListener {
        println("JoinFamilyUseCase invoked")
        return remoteUserRepositoryImpl.joinFamily(familyId, uid, name)
    }
}
