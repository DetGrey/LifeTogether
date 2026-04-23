package com.example.lifetogether.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.enums.SettingsConfirmationTypes
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.FamilyRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.AppError
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

data class SettingsUiState(
    val userInformation: UserInformation? = null,
    val showConfirmationDialog: Boolean = false,
    val confirmationDialogType: SettingsConfirmationTypes? = null,
    val addedFamilyId: String = "",
)

sealed interface SettingsUiEvent {
    data object JoinFamilyClicked : SettingsUiEvent
    data object CreateNewFamilyClicked : SettingsUiEvent
    data object DismissConfirmationDialog : SettingsUiEvent
    data class AddedFamilyIdChanged(val value: String) : SettingsUiEvent
    data object ConfirmJoinFamily : SettingsUiEvent
    data object ConfirmCreateNewFamily : SettingsUiEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val familyRepository: FamilyRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

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

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.JoinFamilyClicked -> showJoinFamilyDialog()
            SettingsUiEvent.CreateNewFamilyClicked -> showCreateFamilyDialog()
            SettingsUiEvent.DismissConfirmationDialog -> closeConfirmationDialog()
            is SettingsUiEvent.AddedFamilyIdChanged -> updateUiState {
                it.copy(addedFamilyId = event.value)
            }
            SettingsUiEvent.ConfirmJoinFamily -> joinFamily()
            SettingsUiEvent.ConfirmCreateNewFamily -> createNewFamily()
        }
    }

    private fun showJoinFamilyDialog() {
        updateUiState {
            it.copy(
                showConfirmationDialog = true,
                confirmationDialogType = SettingsConfirmationTypes.JOIN_FAMILY,
                addedFamilyId = "",
            )
        }
    }

    private fun showCreateFamilyDialog() {
        updateUiState {
            it.copy(
                showConfirmationDialog = true,
                confirmationDialogType = SettingsConfirmationTypes.NEW_FAMILY,
                addedFamilyId = "",
            )
        }
    }

    private fun closeConfirmationDialog() {
        updateUiState {
            it.copy(
                showConfirmationDialog = false,
                confirmationDialogType = null,
                addedFamilyId = "",
            )
        }
    }

    private fun joinFamily() {
        val state = _uiState.value
        val addedFamilyId = state.addedFamilyId
        if (addedFamilyId.isEmpty()) return
        val userInformation = state.userInformation ?: return
        val uid = userInformation.uid ?: return
        val name = userInformation.name ?: return

        viewModelScope.launch {
            when (val result = familyRepository.joinFamily(addedFamilyId, uid, name)) {
                is Result.Success -> closeConfirmationDialog()
                is Result.Failure -> showError(result.error)
            }
        }
    }

    private fun createNewFamily() {
        val userInformation = _uiState.value.userInformation ?: return
        val uid = userInformation.uid ?: return
        val name = userInformation.name ?: return

        viewModelScope.launch {
            when (val result = familyRepository.createNewFamily(uid, name)) {
                is Result.Success -> closeConfirmationDialog()
                is Result.Failure -> showError(result.error)
            }
        }
    }

    private suspend fun showError(error: AppError) {
        _uiCommands.send(
            UiCommand.ShowSnackbar(
                message = error.toUserMessage(),
                withDismissAction = true,
            ),
        )
    }

    private fun updateUiState(transform: (SettingsUiState) -> SettingsUiState) {
        _uiState.update(transform)
    }
}
