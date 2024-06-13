package com.example.aca.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.usecase.SignUpUseCase
import com.example.lifetogether.domain.usecase.UserValidationUseCase

class SignUpViewModel : ViewModel() {
    var error: String by mutableStateOf("")

    var email: String by mutableStateOf("")

    var password: String by mutableStateOf("")

    var confirmPassword: String by mutableStateOf("")

    fun onSignUpClicked() {
        error = ""

        if (password != confirmPassword) {
            error = "Confirmed password does not match"
            return
        }

        val userValidationUseCase = UserValidationUseCase()

        val passwordResult = userValidationUseCase.passwordValidation(password)

        if (!passwordResult) {
            error = "Invalid password."
            return
        }

        val emailResult = userValidationUseCase.emailValidation(email)

        if (!emailResult) {
            error = "Invalid email."
            return
        }

        // If no errors, proceed with sign up
        performSignUp()
    }

    private fun performSignUp() {
        // Handle the login logic here
        val signUpUseCase = SignUpUseCase()
        signUpUseCase.invoke(User(email, password))
    }
}
