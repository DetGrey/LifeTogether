package com.example.lifetogether.domain.usecase.user

import com.example.lifetogether.data.repository.RemoteUserRepositoryImpl
import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.model.User
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val remoteUserRepositoryImpl: RemoteUserRepositoryImpl,
) {
    suspend operator fun invoke(user: User): AuthResultListener {
        println("LoginUseCase invoked")
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

        return remoteUserRepositoryImpl.login(user)
    }
}
