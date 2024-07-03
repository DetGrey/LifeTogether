package com.example.lifetogether.domain.usecase

import com.example.lifetogether.data.repository.LocalUserRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import javax.inject.Inject

data class CreateNewFamilyUseCase @Inject constructor(
    private val localUserRepositoryImpl: LocalUserRepositoryImpl,
) {
    suspend operator fun invoke(
        uid: String,
    ): ResultListener {
        println("CreateNewFamilyUseCase invoked")
        return localUserRepositoryImpl.createNewFamily(uid)
    }
}
