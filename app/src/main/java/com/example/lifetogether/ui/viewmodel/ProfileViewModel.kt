package com.example.lifetogether.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.data.repository.LocalUserRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.usecase.user.ChangeNameUseCase
import com.example.lifetogether.domain.usecase.user.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val localUserRepositoryImpl: LocalUserRepositoryImpl,
    private val logoutUseCase: LogoutUseCase,
    private val changeNameUseCase: ChangeNameUseCase,
) : ViewModel() {
    private val _uid = MutableStateFlow<String?>(null)
    val uid: StateFlow<String?> = _uid.asStateFlow()

    init {
        viewModelScope.launch {
            localUserRepositoryImpl.userInformation
                .map { it?.uid }
                .distinctUntilChanged()
                .collect { newUid ->
                    _uid.value = newUid
                }
        }
    }

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
            val result = logoutUseCase.invoke()
            if (result is ResultListener.Success) {
                onSuccess()
            } else if (result is ResultListener.Failure) {
                // TODO
            }
        }
    }
    fun changeName() {
        val name = newName

        uid.value?.let { uid ->
            viewModelScope.launch {
                val result = changeNameUseCase.invoke(uid, name)
                if (result is ResultListener.Success) {
                    closeConfirmationDialog()
                } else if (result is ResultListener.Failure) {
                    // TODO
                }
            }
        }
    }
}
