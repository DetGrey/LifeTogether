package com.example.lifetogether.domain.usecase.family

import com.example.lifetogether.data.repository.UserRepositoryImpl
import com.example.lifetogether.domain.listener.ResultListener
import javax.inject.Inject

data class DeleteFamilyUseCase @Inject constructor(
    private val userRepositoryImpl: UserRepositoryImpl,
) {
    suspend operator fun invoke(
        familyId: String,
    ): ResultListener {
        println("DeleteFamilyUseCase invoked")
        return userRepositoryImpl.deleteFamily(familyId)
    }
}
