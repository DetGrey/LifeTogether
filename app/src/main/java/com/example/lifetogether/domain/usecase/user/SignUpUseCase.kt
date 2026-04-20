package com.example.lifetogether.domain.usecase.user

import com.example.lifetogether.data.repository.UserRepositoryImpl
import com.example.lifetogether.domain.listener.AuthResultListener
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val userRepositoryImpl: UserRepositoryImpl,
) {
    suspend operator fun invoke(
        user: User,
        userInformation: UserInformation,
    ): AuthResultListener {
        // TODO sign up validation is needed to fit with the validation when logging in
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
        println("SignUpUseCase invoked")
        return userRepositoryImpl.signUp(user, userInformation)
    }
}
