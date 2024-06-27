package com.example.lifetogether.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.enums.UpdateType
import com.example.lifetogether.domain.usecase.observers.ObserveAuthStateUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveCategoriesUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveGroceryListUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveUserInformationUseCase
import com.example.lifetogether.domain.usecase.user.FetchUserInformationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val fetchUserInformationUseCase: FetchUserInformationUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val observeGroceryListUseCase: ObserveGroceryListUseCase,
    private val observeCategoriesUseCase: ObserveCategoriesUseCase,
    private val observeUserInformationUseCase: ObserveUserInformationUseCase,
) : ViewModel() {
    // ---------------------------------------------- USER
    // Create a StateFlow to hold user information
    private val _userInformation = MutableStateFlow<UserInformation?>(null)
    val userInformation: StateFlow<UserInformation?> = _userInformation.asStateFlow()

    init {
        // Observe changes to user information
        viewModelScope.launch {
            observeAuthStateUseCase().collect { result ->
                println("AuthViewModel authstate: $result")
                when (result) {
                    is AuthResultListener.Success -> {
                        result.userInformation.uid?.let { uid ->
                            observeFirestore()
                            fetchUserInformation(uid)
                        }
                    }
                    is AuthResultListener.Failure -> {
                        _userInformation.value = null
                    }
                }
            }
        }

        viewModelScope.launch {
            observeCategoriesUseCase()
        }
    }

    private suspend fun observeFirestore() {
        userInformation.value?.uid?.let { uid ->
            viewModelScope.launch {
                observeUserInformationUseCase.invoke(uid)
            }

            viewModelScope.launch {
                observeGroceryListUseCase.invoke(uid)
            }
        }
    }

    private suspend fun fetchUserInformation(uid: String) {
        println("AuthViewModel before calling fetchUserInformationUseCase")
        fetchUserInformationUseCase(uid = uid).collect { result ->
            println("AuthViewModel fetchUserInformationUseCase result: $result")
            when (result) {
                is AuthResultListener.Success -> {
                    _userInformation.value = result.userInformation
                }

                is AuthResultListener.Failure -> {
                    _userInformation.value = null
                    // Handle failure, e.g., show an error message
                }
            }
        }
    }

    fun loggedOut() {
        _userInformation.value = null
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
