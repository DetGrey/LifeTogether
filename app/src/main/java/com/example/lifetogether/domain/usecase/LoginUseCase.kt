package com.example.lifetogether.domain.usecase

import com.example.lifetogether.data.repository.UserRepositoryImpl
import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.model.User

class LoginUseCase {
    private val userRepository = UserRepositoryImpl()
    suspend operator fun invoke(user: User): AuthResultListener {
        // Handle the login logic and validation here
        val userValidationUseCase = UserValidationUseCase()

        val passwordResult = userValidationUseCase.passwordValidation(user.password)
        if (!passwordResult) {
            return AuthResultListener.Failure("Invalid password.")
        }

        val emailResult = userValidationUseCase.emailValidation(user.email)
        if (!emailResult) {
            return AuthResultListener.Failure("Invalid email.")
        }

        return userRepository.login(user)
    }
}
