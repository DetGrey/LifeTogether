package com.example.lifetogether.ui.feature.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.repository.UserRepository
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.domain.usecase.image.UploadImageUseCase
import com.example.lifetogether.ui.common.event.UiCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val uploadImageUseCase: UploadImageUseCase,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _commands = Channel<ProfileCommand>(Channel.BUFFERED)
    val commands: Flow<ProfileCommand> = _commands.receiveAsFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                when (state) {
                    is SessionState.Authenticated -> updateUiState {
                        when (it) {
                            is ProfileUiState.Loading -> ProfileUiState.Content(
                                userInformation = state.user,
                                showConfirmationDialog = false,
                                showImageUploadDialog = false,
                                confirmationDialogType = null,
                                newName = "",
                            )

                            is ProfileUiState.Content -> it.copy(
                                userInformation = state.user,
                                showImageUploadDialog = false,
                            )
                        }
                    }

                    SessionState.Loading -> Unit
                    SessionState.Unauthenticated -> {
                        _uiState.value = ProfileUiState.Loading
                    }
                }
            }
        }
    }

    fun onEvent(event: ProfileUiEvent) {
        when (event) {
            ProfileUiEvent.AddImageClicked -> updateContent { it.copy(showImageUploadDialog = true) }
            ProfileUiEvent.ImageUploadDismissed,
            ProfileUiEvent.ImageUploadConfirmed -> updateContent { it.copy(showImageUploadDialog = false) }

            ProfileUiEvent.NameClicked -> showNameDialog()
            ProfileUiEvent.LogoutClicked -> showLogoutDialog()
            ProfileUiEvent.DismissConfirmationDialog -> closeConfirmationDialog()
            ProfileUiEvent.ConfirmConfirmationDialog -> confirmConfirmationDialog()
            is ProfileUiEvent.NewNameChanged -> updateContent {
                it.copy(newName = event.value)
            }
        }
    }

    suspend fun uploadProfileImage(uri: Uri): Result<Unit, AppError> {
        val uid = (uiState.value as? ProfileUiState.Content)?.userInformation?.uid
            ?: return Result.Failure(AppError.Validation("Missing user context"))
        return uploadImageUseCase.invoke(
            uri = uri,
            imageType = ImageType.ProfileImage(uid),
            context = context,
        )
    }

    private fun showNameDialog() {
        updateContent {
            it.copy(
                showConfirmationDialog = true,
                confirmationDialogType = ProfileConfirmationType.NAME,
                newName = it.userInformation?.name.orEmpty(),
            )
        }
    }

    private fun showLogoutDialog() {
        updateContent {
            it.copy(
                showConfirmationDialog = true,
                confirmationDialogType = ProfileConfirmationType.LOGOUT,
                newName = "",
            )
        }
    }

    private fun closeConfirmationDialog() {
        updateContent {
            it.copy(
                showConfirmationDialog = false,
                confirmationDialogType = null,
                newName = "",
            )
        }
    }

    private fun confirmConfirmationDialog() {
        when ((uiState.value as? ProfileUiState.Content)?.confirmationDialogType) {
            ProfileConfirmationType.LOGOUT -> logout()
            ProfileConfirmationType.NAME -> changeName()
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
        val state = uiState.value as? ProfileUiState.Content ?: return
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

    private fun updateContent(transform: (ProfileUiState.Content) -> ProfileUiState.Content) {
        updateUiState { state ->
            when (state) {
                is ProfileUiState.Loading -> state
                is ProfileUiState.Content -> transform(state)
            }
        }
    }
}
