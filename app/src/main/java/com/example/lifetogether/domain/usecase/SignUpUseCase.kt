package com.example.lifetogether.domain.usecase

import com.example.lifetogether.data.repository.UserRepositoryImpl
import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation

class SignUpUseCase {
    private val userRepository = UserRepositoryImpl()
    suspend operator fun invoke(
        user: User,
        userInformation: UserInformation,
    ): AuthResultListener {
//        if (user.password != confirmPassword) {
//            error = "Confirmed password does not match"
//            return
//        }
//
//        val userValidationUseCase = UserValidationUseCase()
//
//        val passwordResult = userValidationUseCase.passwordValidation(password)
//
//        if (!passwordResult) {
//            error = "Invalid password."
//            return
//        }
//
//        val emailResult = userValidationUseCase.emailValidation(email)
//
//        if (!emailResult) {
//            error = "Invalid email."
//            return
//        }

        return userRepository.signUp(user, userInformation)
    }
}
