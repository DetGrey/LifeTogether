package com.example.lifetogether.domain.usecase.family

import com.example.lifetogether.data.repository.RemoteUserRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import javax.inject.Inject

data class DeleteFamilyUseCase @Inject constructor(
    private val remoteUserRepositoryImpl: RemoteUserRepositoryImpl,
) {
    suspend operator fun invoke(
        familyId: String,
    ): ResultListener {
        println("DeleteFamilyUseCase invoked")
        return remoteUserRepositoryImpl.deleteFamily(familyId)
    }
}
