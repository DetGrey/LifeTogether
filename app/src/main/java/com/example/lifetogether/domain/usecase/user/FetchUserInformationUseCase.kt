package com.example.lifetogether.domain.usecase.user

import com.example.lifetogether.data.repository.LocalUserRepositoryImpl
import com.example.lifetogether.domain.callback.AuthResultListener
import javax.inject.Inject

class FetchUserInformationUseCase @Inject constructor(
    private val userRepository: LocalUserRepositoryImpl,
) {
    suspend operator fun invoke(): AuthResultListener {
        println("FetchUserInformationUseCase invoked")
        return userRepository.getCurrentUser()
    }
}
