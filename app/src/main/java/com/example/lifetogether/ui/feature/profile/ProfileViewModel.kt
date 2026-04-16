package com.example.lifetogether.ui.feature.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.usecase.user.ChangeNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val changeNameUseCase: ChangeNameUseCase,
) : ViewModel() {
    private val _userInformation = MutableStateFlow<UserInformation?>(null)
    val userInformation: StateFlow<UserInformation?> = _userInformation.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                _userInformation.value = (state as? SessionState.Authenticated)?.user
            }
        }
    }

    // ---------------------------------------------------------------- ERROR
    var showAlertDialog: Boolean by mutableStateOf(false)
    var error: String by mutableStateOf("")
    fun toggleAlertDialog() {
        viewModelScope.launch {
            delay(3000)
            showAlertDialog = false
            error = ""
        }
    }

    // ---------------------------------------------------------------- CONFIRMATION TYPES
    enum class ProfileConfirmationType {
        LOGOUT, NAME, PASSWORD
    }

    var confirmationDialogType: ProfileConfirmationType? by mutableStateOf(null)

    var showConfirmationDialog: Boolean by mutableStateOf(false)

    fun closeConfirmationDialog() {
        showConfirmationDialog = false
        newName = ""
    }

    // ---------------------------------------------------------------- LOGOUT
    fun logout(
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            val result = sessionRepository.signOut()
            if (result is ResultListener.Success) {
                println("ProfileViewModel: Logout successful")
                onSuccess()
            } else if (result is ResultListener.Failure) {
                error = result.message
                showAlertDialog = true
            }
        }
    }

    // ---------------------------------------------------------------- CHANGE NAME
    var newName: String by mutableStateOf("")

    fun changeName() {
        val name = newName
        if (name.isEmpty()) return

        val uid = _userInformation.value?.uid ?: return
        val familyId = _userInformation.value?.familyId

        viewModelScope.launch {
            val result = changeNameUseCase.invoke(uid, familyId, name)
            if (result is ResultListener.Success) {
                closeConfirmationDialog()
            } else if (result is ResultListener.Failure) {
                closeConfirmationDialog()
                error = result.message
                showAlertDialog = true
            }
        }
    }
}
