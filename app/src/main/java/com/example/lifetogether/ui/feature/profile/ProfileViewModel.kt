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
                                isAdmin = state.isAdmin
                            )

                            is ProfileUiState.Content -> it.copy(
                                userInformation = state.user,
                            )
                        }
                    }
                    SessionState.Loading -> Unit
                    SessionState.Unauthenticated -> {
                        _commands.send(ProfileCommand.NavigateToLogin)
                    }
                }
            }
        }
    }

    fun onEvent(event: ProfileUiEvent) {
        when (event) {
            is ProfileUiEvent.ImageSelected -> uploadProfileImage(event.uri)
            ProfileUiEvent.NameClicked -> showNameDialog()
            ProfileUiEvent.LogoutClicked -> showLogoutDialog()
            ProfileUiEvent.DismissDialog -> closeDialog()
            ProfileUiEvent.ConfirmLogout -> logout()
            ProfileUiEvent.ConfirmChangeName -> changeName()
            is ProfileUiEvent.NameChanged -> updateContent {
                it.copy(dialog = ProfileDialogState.ChangeName(name = event.value))
            }
        }
    }

    private fun uploadProfileImage(uri: Uri) {
        viewModelScope.launch {
            when (val result = performProfileImageUpload(uri)) {
                is Result.Success -> Unit
                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private suspend fun performProfileImageUpload(uri: Uri): Result<Unit, AppError> {
        val uid = (uiState.value as? ProfileUiState.Content)?.userInformation?.uid
            ?: return Result.Failure(AppError.Validation("Missing user context"))
        return when (val result = uploadImageUseCase.invoke(
            uri = uri,
            imageType = ImageType.ProfileImage(uid),
            context = context,
        )) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(result.error)
        }
    }

    private fun showNameDialog() {
        updateContent {
            it.copy(dialog = ProfileDialogState.ChangeName(name = it.userInformation.name))
        }
    }

    private fun showLogoutDialog() {
        updateContent {
            it.copy(dialog = ProfileDialogState.Logout)
        }
    }

    private fun closeDialog() {
        updateContent {
            it.copy(dialog = null)
        }
    }

    private fun logout() {
        viewModelScope.launch {
            when (val result = sessionRepository.signOut()) {
                is Result.Success -> {
                    closeDialog()
                    _commands.send(ProfileCommand.NavigateToLogin)
                }

                is Result.Failure -> {
                    closeDialog()
                    showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun changeName() {
        val state = uiState.value as? ProfileUiState.Content ?: return
        val name = (state.dialog as? ProfileDialogState.ChangeName)?.name?.trim() ?: return
        if (name.isEmpty()) return

        val userInformation = state.userInformation
        val uid = userInformation.uid
        val familyId = userInformation.familyId

        viewModelScope.launch {
            when (val result = userRepository.changeName(uid, familyId, name)) {
                is Result.Success -> closeDialog()
                is Result.Failure -> {
                    closeDialog()
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
