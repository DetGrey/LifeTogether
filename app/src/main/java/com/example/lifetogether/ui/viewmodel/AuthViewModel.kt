package com.example.lifetogether.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.enums.UpdateType
import com.example.lifetogether.domain.usecase.user.FetchUserInformationUseCase
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    // ---------------------------------------------- USER
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

    // ---------------------------------------------- ITEM COUNT
    var itemCount: Map<String, Int> by mutableStateOf(mapOf())

    fun updateItemCount(collection: String, updateType: UpdateType) {
        val currentCount = itemCount[collection] ?: 0
        val updatedCount = when (updateType) {
            UpdateType.ADD -> currentCount + 1
            UpdateType.SUBTRACT -> (currentCount - 1).coerceAtLeast(0) // Prevents negative counts
        }
        itemCount = itemCount.toMutableMap().apply {
            this[collection] = updatedCount
        }
    }
}
