package com.example.lifetogether.ui.feature.family

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.model.family.FamilyMember
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.FamilyRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.domain.usecase.image.UploadImageUseCase
import com.example.lifetogether.ui.common.event.UiCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val uploadImageUseCase: UploadImageUseCase,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow<FamilyUiState>(FamilyUiState.Loading)
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
                val previousFamilyId = (_uiState.value as? FamilyUiState.Content)?.familyId

                updateUiState {
                    when (it) {
                        is FamilyUiState.Loading -> {
                            if (familyId != null) {
                                FamilyUiState.Content(
                                    familyId = familyId,
                                    uid = uid,
                                    familyInformation = null,
                                )
                            } else {
                                FamilyUiState.Loading
                            }
                        }

                        is FamilyUiState.Content -> it.copy(
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
                            showImageUploadDialog = false,
                        )
                    }
                }

                if (familyId != previousFamilyId) {
                    observeFamilyInformation(familyId)
                }
                if (state is SessionState.Unauthenticated) {
                    familyInformationJob?.cancel()
                    familyInformationJob = null
                    _uiState.value = FamilyUiState.Loading
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
            FamilyUiEvent.AddImageClicked -> updateContent { it.copy(showImageUploadDialog = true) }
            FamilyUiEvent.ImageUploadDismissed,
            FamilyUiEvent.ImageUploadConfirmed -> updateContent { it.copy(showImageUploadDialog = false) }
        }
    }

    suspend fun uploadFamilyImage(uri: Uri): Result<Unit, AppError> {
        val familyId = (uiState.value as? FamilyUiState.Content)?.familyId
            ?: return Result.Failure(AppError.Validation("Missing family context"))
        return uploadImageUseCase.invoke(
            uri = uri,
            imageType = ImageType.FamilyImage(familyId),
            context = context,
        )
    }

    private fun observeFamilyInformation(familyId: String?) {
        familyInformationJob?.cancel()
        if (familyId == null) {
            updateContent { it.copy(familyInformation = null) }
            return
        }

        familyInformationJob = viewModelScope.launch {
            familyRepository.observeFamilyInformation(familyId).collect { result ->
                when (result) {
                    is Result.Success -> updateContent {
                        it.copy(familyInformation = result.data)
                    }

                    is Result.Failure -> {
                        updateContent {
                            it.copy(familyInformation = null)
                        }
                        showError(result.error.toUserMessage())
                    }
                }
            }
        }
    }

    private fun showConfirmationDialog(type: FamilyConfirmationType) {
        updateContent {
            it.copy(
                showConfirmationDialog = true,
                confirmationDialogType = type,
                memberToRemove = null,
            )
        }
    }

    private fun showRemoveMemberConfirmation(member: FamilyMember) {
        updateContent {
            it.copy(
                showConfirmationDialog = true,
                confirmationDialogType = FamilyConfirmationType.REMOVE_MEMBER,
                memberToRemove = member,
            )
        }
    }

    private fun closeConfirmationDialog() {
        updateContent {
            it.copy(
                showConfirmationDialog = false,
                confirmationDialogType = null,
                memberToRemove = null,
            )
        }
    }

    private fun confirmConfirmationDialog() {
        when ((uiState.value as? FamilyUiState.Content)?.confirmationDialogType) {
            FamilyConfirmationType.ADD_MEMBER -> copyFamilyId()
            FamilyConfirmationType.LEAVE_FAMILY -> leaveFamily()
            FamilyConfirmationType.REMOVE_MEMBER -> removeFamilyMember()
            FamilyConfirmationType.DELETE_FAMILY -> deleteFamily()
            null -> Unit
        }
    }

    private fun copyFamilyId() {
        val familyId = (uiState.value as? FamilyUiState.Content)?.familyId ?: return
        closeConfirmationDialog()
        viewModelScope.launch {
            _commands.send(FamilyCommand.CopyFamilyId(familyId))
        }
    }

    private fun leaveFamily() {
        val state = _uiState.value as? FamilyUiState.Content ?: return
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
        val state = _uiState.value as? FamilyUiState.Content ?: return
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
        val familyId = (uiState.value as? FamilyUiState.Content)?.familyId ?: return

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

    private fun updateContent(transform: (FamilyUiState.Content) -> FamilyUiState.Content) {
        updateUiState { state ->
            when (state) {
                is FamilyUiState.Loading -> state
                is FamilyUiState.Content -> transform(state)
            }
        }
    }
}
