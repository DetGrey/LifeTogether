package com.example.lifetogether.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.usecase.ChangeNameUseCase
import com.example.lifetogether.domain.usecase.LogoutUseCase
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    enum class ConfirmationType {
        LOGOUT, NAME, PASSWORD
    }

    var newName: String by mutableStateOf("")

    var confirmationDialogType: ConfirmationType? by mutableStateOf(null)

    var showConfirmationDialog: Boolean by mutableStateOf(false)

    fun closeConfirmationDialog() {
        showConfirmationDialog = false
        newName = ""
    }

    fun logout(
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            val logoutUseCase = LogoutUseCase()
            val result = logoutUseCase.invoke()
            if (result is ResultListener.Success) {
                onSuccess()
            } else if (result is ResultListener.Failure) {
                // TODO
            }
        }
    }
    fun changeName(
        uid: String,
        onSuccess: (String) -> Unit,
    ) {
        val name = newName

        viewModelScope.launch {
            val changeNameUseCase = ChangeNameUseCase()
            val result = changeNameUseCase.invoke(uid, name)
            if (result is ResultListener.Success) {
                onSuccess(name)
            } else if (result is ResultListener.Failure) {
                // TODO
            }
        }
    }
}
