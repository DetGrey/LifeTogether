package com.example.lifetogether.domain.usecase.family

import com.example.lifetogether.data.repository.RemoteUserRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import javax.inject.Inject

data class CreateNewFamilyUseCase @Inject constructor(
    private val remoteUserRepositoryImpl: RemoteUserRepositoryImpl,
) {
    suspend operator fun invoke(
        uid: String,
        name: String,
    ): ResultListener {
        println("CreateNewFamilyUseCase invoked")
        return remoteUserRepositoryImpl.createNewFamily(uid, name)
    }
}
