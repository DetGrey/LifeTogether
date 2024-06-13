package com.example.lifetogether.domain.usecase

import com.example.lifetogether.data.repository.UserRepositoryImpl
import com.example.lifetogether.domain.model.User

class LoginUseCase {
    private val userRepository = UserRepositoryImpl()
    operator fun invoke(user: User) {
        userRepository.login(user)
    }
}
