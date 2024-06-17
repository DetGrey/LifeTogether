package com.example.lifetogether.domain.usecase.user

import com.example.lifetogether.data.repository.UserRepositoryImpl
import com.example.lifetogether.domain.callback.AuthResultListener

class FetchUserInformationUseCase {
    private val userRepository = UserRepositoryImpl()
    suspend operator fun invoke(): AuthResultListener {
        return userRepository.getCurrentUser()
    }
}
