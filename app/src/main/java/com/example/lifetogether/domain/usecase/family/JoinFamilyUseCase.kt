package com.example.lifetogether.domain.usecase.family

import com.example.lifetogether.data.repository.UserRepositoryImpl
import com.example.lifetogether.domain.listener.ResultListener
import javax.inject.Inject

data class JoinFamilyUseCase @Inject constructor(
    private val userRepositoryImpl: UserRepositoryImpl
) {
    suspend operator fun invoke(
        familyId: String,
        uid: String,
        name: String,
    ): ResultListener {
        println("JoinFamilyUseCase invoked")
        return userRepositoryImpl.joinFamily(familyId, uid, name)
    }
}
