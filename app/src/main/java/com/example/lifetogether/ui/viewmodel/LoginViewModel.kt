package com.example.aca.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.usecase.LoginUseCase
import com.example.lifetogether.domain.usecase.UserValidationUseCase

class LoginViewModel : ViewModel() {
    var error: String by mutableStateOf("")

    var email: String by mutableStateOf("")

    var password: String by mutableStateOf("")

    fun onLoginClicked() {
        error = ""
        val userValidationUseCase = UserValidationUseCase()

        val passwordResult = userValidationUseCase.passwordValidation(password)
        val emailResult = userValidationUseCase.emailValidation(email)

        if (!passwordResult) {
            error = "Invalid password."
            return
        }

        if (!emailResult) {
            error = "Invalid email."
            return
        }

        // If no errors, proceed with login
        performLogin()
    }

    private fun performLogin() {
        // Handle the login logic here
        val loginUseCase = LoginUseCase()
        loginUseCase.invoke(User(email, password))
    }
}
