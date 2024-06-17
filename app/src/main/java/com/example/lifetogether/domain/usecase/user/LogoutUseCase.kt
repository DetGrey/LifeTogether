package com.example.lifetogether.domain.usecase.user

import com.example.lifetogether.data.repository.UserRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener

class LogoutUseCase {
    private val userRepository = UserRepositoryImpl()
    suspend operator fun invoke(): ResultListener {
        return userRepository.logout()
    }
}
