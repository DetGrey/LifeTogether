package com.example.lifetogether.domain.usecase.user

import com.example.lifetogether.domain.result.AppError

import android.util.Log
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.repository.UserRepository
import com.example.lifetogether.domain.result.Result
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    private companion object {
        const val TAG = "SignUpUseCase"
    }

    suspend operator fun invoke(
        user: User,
        userInformation: UserInformation,
    ): Result<UserInformation, AppError> {
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
        Log.d(TAG, "invoke")
        return userRepository.signUp(user, userInformation)
    }
}
