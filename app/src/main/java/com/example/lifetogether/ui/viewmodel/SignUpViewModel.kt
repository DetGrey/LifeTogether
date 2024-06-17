package com.example.lifetogether.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.usecase.user.SignUpUseCase
import kotlinx.coroutines.launch
import java.util.Date

class SignUpViewModel : ViewModel() {
    var error: String by mutableStateOf("")

    // TEXT FIELDS
    var name: String by mutableStateOf("")
    var email: String by mutableStateOf("")
    var birthday: Date? by mutableStateOf(null)
    var birthdayExpanded: Boolean by mutableStateOf(false)
    var password: String by mutableStateOf("")
    var confirmPassword: String by mutableStateOf("")

    fun onSignUpClicked(
        onSuccess: (UserInformation) -> Unit,
    ) {
        error = ""

        val userInformation = UserInformation(
            name = name,
            email = email,
            birthday = birthday,
        )

        viewModelScope.launch {
            val signUpUseCase = SignUpUseCase()
            val loginResult: AuthResultListener = signUpUseCase.invoke(User(email, password), userInformation)
            if (loginResult is AuthResultListener.Success) {
                onSuccess(loginResult.userInformation)
            } else if (loginResult is AuthResultListener.Failure) {
                error = loginResult.message
            }
        }
    }
}
