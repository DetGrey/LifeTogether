package com.example.lifetogether.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val familyRepository: FamilyRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                when (state) {
                    is SessionState.Authenticated -> updateUiState {
                        when (it) {
                            is SettingsUiState.Loading -> SettingsUiState.Content(
                                userInformation = state.user,
                            )

                            is SettingsUiState.Content -> it.copy(userInformation = state.user)
                        }
                    }

                    SessionState.Loading -> Unit
                    SessionState.Unauthenticated -> {
                        _uiState.value = SettingsUiState.Loading
                    }
                }
            }
        }
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.JoinFamilyClicked -> showJoinFamilyDialog()
            SettingsUiEvent.CreateNewFamilyClicked -> showCreateFamilyDialog()
            SettingsUiEvent.DismissDialog -> closeDialog()
            is SettingsUiEvent.FamilyIdChanged -> updateContent {
                it.copy(dialog = SettingsDialogState.JoinFamily(familyId = event.value))
            }
            SettingsUiEvent.ConfirmJoinFamily -> joinFamily()
            SettingsUiEvent.ConfirmCreateNewFamily -> createNewFamily()
        }
    }

    private fun showJoinFamilyDialog() {
        updateContent {
            it.copy(dialog = SettingsDialogState.JoinFamily())
        }
    }

    private fun showCreateFamilyDialog() {
        updateContent {
            it.copy(dialog = SettingsDialogState.CreateFamily)
        }
    }

    private fun closeDialog() {
        updateContent {
            it.copy(dialog = null)
        }
    }

    private fun joinFamily() {
        val state = _uiState.value as? SettingsUiState.Content ?: return
        val familyId = (state.dialog as? SettingsDialogState.JoinFamily)?.familyId ?: return
        if (familyId.isEmpty()) return
        val uid = state.userInformation.uid
        val name = state.userInformation.name

        viewModelScope.launch {
            when (val result = familyRepository.joinFamily(familyId, uid, name)) {
                is Result.Success -> closeDialog()
                is Result.Failure -> showError(result.error)
            }
        }
    }

    private fun createNewFamily() {
        val userInformation = (_uiState.value as? SettingsUiState.Content)?.userInformation ?: return
        val uid = userInformation.uid
        val name = userInformation.name

        viewModelScope.launch {
            when (val result = familyRepository.createNewFamily(uid, name)) {
                is Result.Success -> closeDialog()
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

    private fun updateContent(transform: (SettingsUiState.Content) -> SettingsUiState.Content) {
        updateUiState { state ->
            when (state) {
                is SettingsUiState.Loading -> state
                is SettingsUiState.Content -> transform(state)
            }
        }
    }
}
