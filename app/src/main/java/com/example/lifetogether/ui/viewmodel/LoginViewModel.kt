package com.example.lifetogether.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.usecase.user.LoginUseCase
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    var error: String by mutableStateOf("")

    // TEXT FIELDS
    var email: String by mutableStateOf("")
    var password: String by mutableStateOf("")

    fun onLoginClicked(
        onSuccess: (UserInformation) -> Unit,
    ) {
        error = ""

        viewModelScope.launch {
            val loginUseCase = LoginUseCase()
            val loginResult: AuthResultListener = loginUseCase.invoke(User(email, password))
            if (loginResult is AuthResultListener.Success) {
                onSuccess(loginResult.userInformation)
            } else if (loginResult is AuthResultListener.Failure) {
                error = loginResult.message
            }
        }
    }
}
