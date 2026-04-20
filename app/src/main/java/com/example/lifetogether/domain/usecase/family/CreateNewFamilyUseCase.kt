package com.example.lifetogether.domain.usecase.family

import com.example.lifetogether.data.repository.UserRepositoryImpl
import com.example.lifetogether.domain.listener.ResultListener
import javax.inject.Inject

data class CreateNewFamilyUseCase @Inject constructor(
    private val userRepositoryImpl: UserRepositoryImpl
) {
    suspend operator fun invoke(
        uid: String,
        name: String,
    ): ResultListener {
        println("CreateNewFamilyUseCase invoked")
        return userRepositoryImpl.createNewFamily(uid, name)
    }
}
