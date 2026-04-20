package com.example.lifetogether.domain.usecase.user

import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.repository.UserRepository
import com.example.lifetogether.domain.result.Result
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(user: User): Result<UserInformation, String> {
        println("LoginUseCase invoked")
        // TODO Handle the login logic and validation here
//        val userValidationUseCase = UserValidationUseCase()
//
//        val passwordResult = userValidationUseCase.passwordValidation(user.password)
//        if (!passwordResult) {
//            return Result.Failure("Invalid password.")
//        }
//
//        val emailResult = userValidationUseCase.emailValidation(user.email)
//        if (!emailResult) {
//            return Result.Failure("Invalid email.")
//        }

        return userRepository.login(user)
    }
}
