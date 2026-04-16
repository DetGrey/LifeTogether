package com.example.lifetogether.domain.usecase.family

import com.example.lifetogether.data.repository.UserRepositoryImpl
import com.example.lifetogether.domain.listener.ResultListener
import javax.inject.Inject

data class LeaveFamilyUseCase @Inject constructor(
    private val userRepositoryImpl: UserRepositoryImpl
) {
    suspend operator fun invoke(
        familyId: String,
        uid: String,
    ): ResultListener {
        println("LeaveFamilyUseCase invoked")
        return userRepositoryImpl.leaveFamily(familyId, uid)
    }
}
