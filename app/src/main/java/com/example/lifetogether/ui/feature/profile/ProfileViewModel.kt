package com.example.lifetogether.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.repository.UserRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.ui.common.event.UiCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _commands = Channel<ProfileCommand>(Channel.BUFFERED)
    val commands: Flow<ProfileCommand> = _commands.receiveAsFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                updateUiState {
                    it.copy(
                        userInformation = (state as? SessionState.Authenticated)?.user,
                    )
                }
            }
        }
    }

    fun onEvent(event: ProfileUiEvent) {
        when (event) {
            ProfileUiEvent.AddImageClicked,
            ProfileUiEvent.ImageUploadDismissed,
            ProfileUiEvent.ImageUploadConfirmed -> Unit

            ProfileUiEvent.NameClicked -> showNameDialog()
            ProfileUiEvent.PasswordClicked -> showPasswordDialog()
            ProfileUiEvent.LogoutClicked -> showLogoutDialog()
            ProfileUiEvent.DismissConfirmationDialog -> closeConfirmationDialog()
            ProfileUiEvent.ConfirmConfirmationDialog -> confirmConfirmationDialog()
            is ProfileUiEvent.NewNameChanged -> updateUiState {
                it.copy(newName = event.value)
            }
        }
    }

    private fun showNameDialog() {
        updateUiState {
            it.copy(
                showConfirmationDialog = true,
                confirmationDialogType = ProfileConfirmationType.NAME,
                newName = it.userInformation?.name.orEmpty(),
            )
        }
    }

    private fun showPasswordDialog() {
        updateUiState {
            it.copy(
                showConfirmationDialog = true,
                confirmationDialogType = ProfileConfirmationType.PASSWORD,
                newName = "",
            )
        }
    }

    private fun showLogoutDialog() {
        updateUiState {
            it.copy(
                showConfirmationDialog = true,
                confirmationDialogType = ProfileConfirmationType.LOGOUT,
                newName = "",
            )
        }
    }

    private fun closeConfirmationDialog() {
        updateUiState {
            it.copy(
                showConfirmationDialog = false,
                confirmationDialogType = null,
                newName = "",
            )
        }
    }

    private fun confirmConfirmationDialog() {
        when (_uiState.value.confirmationDialogType) {
            ProfileConfirmationType.LOGOUT -> logout()
            ProfileConfirmationType.NAME -> changeName()
            ProfileConfirmationType.PASSWORD -> closeConfirmationDialog()
            null -> Unit
        }
    }

    private fun logout() {
        viewModelScope.launch {
            when (val result = sessionRepository.signOut()) {
                is Result.Success -> {
                    closeConfirmationDialog()
                    _commands.send(ProfileCommand.NavigateToHome)
                }

                is Result.Failure -> {
                    closeConfirmationDialog()
                    showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun changeName() {
        val state = _uiState.value
        val name = state.newName.trim()
        if (name.isEmpty()) return

        val userInformation = state.userInformation ?: return
        val uid = userInformation.uid ?: return
        val familyId = userInformation.familyId

        viewModelScope.launch {
            when (val result = userRepository.changeName(uid, familyId, name)) {
                is Result.Success -> closeConfirmationDialog()
                is Result.Failure -> {
                    closeConfirmationDialog()
                    showError(result.error.toUserMessage())
                }
            }
        }
    }

    private suspend fun showError(message: String) {
        _uiCommands.send(
            UiCommand.ShowSnackbar(
                message = message,
                withDismissAction = true,
            ),
        )
    }

    private fun updateUiState(transform: (ProfileUiState) -> ProfileUiState) {
        _uiState.update(transform)
    }
}
