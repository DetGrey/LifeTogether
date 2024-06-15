package com.example.lifetogether.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.usecase.FetchUserInformationUseCase
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    var userInformation: UserInformation? by mutableStateOf(null)

    init {
        fetchUserInformation()
    }
    fun updateUserInformation(newUserInformation: UserInformation?) {
        println("New user information: $newUserInformation")
        this.userInformation = newUserInformation
    }

    private fun fetchUserInformation() {
        viewModelScope.launch {
            val fetchUserInformationUseCase = FetchUserInformationUseCase()
            val result = fetchUserInformationUseCase.invoke()
            if (result is AuthResultListener.Success) {
                updateUserInformation(result.userInformation)
            }
        }
    }
}
