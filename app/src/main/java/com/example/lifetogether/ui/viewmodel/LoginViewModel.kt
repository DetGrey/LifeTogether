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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {
    var error: String by mutableStateOf("")

    // TEXT FIELDS
    var email: String by mutableStateOf("")
    var password: String by mutableStateOf("")

    fun onLoginClicked(
        onSuccess: (UserInformation) -> Unit,
    ) {
        println("Login clicked")
        error = ""

        viewModelScope.launch {
            val loginResult: AuthResultListener = loginUseCase.invoke(User(email, password))
            if (loginResult is AuthResultListener.Success) {
                println("LoginViewModel: Login successful")
                onSuccess(loginResult.userInformation)
            } else if (loginResult is AuthResultListener.Failure) {
                error = loginResult.message
            }
        }
    }
}
