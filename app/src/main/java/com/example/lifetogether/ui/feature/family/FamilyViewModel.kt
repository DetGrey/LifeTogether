package com.example.lifetogether.ui.feature.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.family.FamilyMember
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.FamilyRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.ui.common.event.UiCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
class FamilyViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val familyRepository: FamilyRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FamilyUiState())
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    private val _commands = Channel<FamilyCommand>(Channel.BUFFERED)
    val commands: Flow<FamilyCommand> = _commands.receiveAsFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    private var familyInformationJob: Job? = null

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val authenticated = state as? SessionState.Authenticated
                val familyId = authenticated?.user?.familyId
                val uid = authenticated?.user?.uid
                val previousFamilyId = _uiState.value.familyId

                updateUiState {
                    it.copy(
                        familyId = familyId,
                        uid = uid,
                        familyInformation = if (familyId == previousFamilyId) {
                            it.familyInformation
                        } else {
                            null
                        },
                        showConfirmationDialog = false,
                        confirmationDialogType = null,
                        memberToRemove = null,
                    )
                }

                if (familyId != previousFamilyId) {
                    observeFamilyInformation(familyId)
                }
            }
        }
    }

    fun onEvent(event: FamilyUiEvent) {
        when (event) {
            FamilyUiEvent.AddMemberClicked -> showConfirmationDialog(FamilyConfirmationType.ADD_MEMBER)
            FamilyUiEvent.LeaveFamilyClicked -> showConfirmationDialog(FamilyConfirmationType.LEAVE_FAMILY)
            FamilyUiEvent.DeleteFamilyClicked -> showConfirmationDialog(FamilyConfirmationType.DELETE_FAMILY)
            is FamilyUiEvent.RemoveMemberClicked -> showRemoveMemberConfirmation(event.member)
            FamilyUiEvent.DismissConfirmationDialog -> closeConfirmationDialog()
            FamilyUiEvent.ConfirmConfirmationDialog -> confirmConfirmationDialog()
            FamilyUiEvent.AddImageClicked,
            FamilyUiEvent.ImageUploadDismissed,
            FamilyUiEvent.ImageUploadConfirmed -> Unit
        }
    }

    private fun observeFamilyInformation(familyId: String?) {
        familyInformationJob?.cancel()
        if (familyId == null) {
            updateUiState { it.copy(familyInformation = null) }
            return
        }

        familyInformationJob = viewModelScope.launch {
            familyRepository.observeFamilyInformation(familyId).collect { result ->
                when (result) {
                    is Result.Success -> updateUiState {
                        it.copy(familyInformation = result.data)
                    }

                    is Result.Failure -> {
                        updateUiState {
                            it.copy(familyInformation = null)
                        }
                        showError(result.error.toUserMessage())
                    }
                }
            }
        }
    }

    private fun showConfirmationDialog(type: FamilyConfirmationType) {
        updateUiState {
            it.copy(
                showConfirmationDialog = true,
                confirmationDialogType = type,
                memberToRemove = null,
            )
        }
    }

    private fun showRemoveMemberConfirmation(member: FamilyMember) {
        updateUiState {
            it.copy(
                showConfirmationDialog = true,
                confirmationDialogType = FamilyConfirmationType.REMOVE_MEMBER,
                memberToRemove = member,
            )
        }
    }

    private fun closeConfirmationDialog() {
        updateUiState {
            it.copy(
                showConfirmationDialog = false,
                confirmationDialogType = null,
                memberToRemove = null,
            )
        }
    }

    private fun confirmConfirmationDialog() {
        when (_uiState.value.confirmationDialogType) {
            FamilyConfirmationType.ADD_MEMBER -> copyFamilyId()
            FamilyConfirmationType.LEAVE_FAMILY -> leaveFamily()
            FamilyConfirmationType.REMOVE_MEMBER -> removeFamilyMember()
            FamilyConfirmationType.DELETE_FAMILY -> deleteFamily()
            null -> Unit
        }
    }

    private fun copyFamilyId() {
        val familyId = _uiState.value.familyId ?: return
        closeConfirmationDialog()
        viewModelScope.launch {
            _commands.send(FamilyCommand.CopyFamilyId(familyId))
        }
    }

    private fun leaveFamily() {
        val state = _uiState.value
        val familyId = state.familyId ?: return
        val uid = state.uid ?: return

        viewModelScope.launch {
            when (val result = familyRepository.leaveFamily(familyId, uid)) {
                is Result.Success -> {
                    closeConfirmationDialog()
                    _commands.send(FamilyCommand.NavigateBack)
                }

                is Result.Failure -> {
                    closeConfirmationDialog()
                    showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun removeFamilyMember() {
        val state = _uiState.value
        val familyId = state.familyId ?: return
        val memberUid = state.memberToRemove?.uid ?: return

        viewModelScope.launch {
            when (val result = familyRepository.leaveFamily(familyId, memberUid)) {
                is Result.Success -> closeConfirmationDialog()
                is Result.Failure -> {
                    closeConfirmationDialog()
                    showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun deleteFamily() {
        val familyId = _uiState.value.familyId ?: return

        viewModelScope.launch {
            when (val result = familyRepository.deleteFamily(familyId)) {
                is Result.Success -> {
                    closeConfirmationDialog()
                    _commands.send(FamilyCommand.NavigateBack)
                }

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

    private fun updateUiState(transform: (FamilyUiState) -> FamilyUiState) {
        _uiState.update(transform)
    }
}
