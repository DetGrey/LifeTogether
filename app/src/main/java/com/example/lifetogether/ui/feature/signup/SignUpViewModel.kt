package com.example.lifetogether.ui.feature.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.domain.usecase.user.SignUpUseCase
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
import java.util.Date
import javax.inject.Inject

data class SignupUiState(
    val name: String = "",
    val email: String = "",
    val birthday: Date? = null,
    val showBirthdayPicker: Boolean = false,
    val password: String = "",
    val confirmPassword: String = "",
)

sealed interface SignupUiEvent {
    data class NameChanged(val value: String) : SignupUiEvent
    data class EmailChanged(val value: String) : SignupUiEvent
    data object BirthdayClicked : SignupUiEvent
    data object BirthdayDismissed : SignupUiEvent
    data class BirthdaySelected(val value: Date) : SignupUiEvent
    data class PasswordChanged(val value: String) : SignupUiEvent
    data class ConfirmPasswordChanged(val value: String) : SignupUiEvent
    data object SignUpClicked : SignupUiEvent
}

sealed interface SignupCommand {
    data object NavigateToProfile : SignupCommand
}

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    private val _commands = Channel<SignupCommand>(Channel.BUFFERED)
    val commands: Flow<SignupCommand> = _commands.receiveAsFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    fun onEvent(event: SignupUiEvent) {
        when (event) {
            is SignupUiEvent.NameChanged -> updateUiState { it.copy(name = event.value) }
            is SignupUiEvent.EmailChanged -> updateUiState { it.copy(email = event.value) }
            SignupUiEvent.BirthdayClicked -> updateUiState { it.copy(showBirthdayPicker = true) }
            SignupUiEvent.BirthdayDismissed -> updateUiState { it.copy(showBirthdayPicker = false) }
            is SignupUiEvent.BirthdaySelected -> updateUiState {
                it.copy(
                    birthday = event.value,
                    showBirthdayPicker = false,
                )
            }
            is SignupUiEvent.PasswordChanged -> updateUiState { it.copy(password = event.value) }
            is SignupUiEvent.ConfirmPasswordChanged -> updateUiState { it.copy(confirmPassword = event.value) }
            SignupUiEvent.SignUpClicked -> signUp()
        }
    }

    private fun signUp() {
        val state = _uiState.value
        val userInformation = UserInformation(
            name = state.name,
            email = state.email,
            birthday = state.birthday,
        ) //todo there should be a check to make sure they are not null either here or in usecase

        viewModelScope.launch {
            when (val result = signUpUseCase.invoke(User(state.email, state.password), userInformation)) {
                is Result.Success -> _commands.send(SignupCommand.NavigateToProfile)
                is Result.Failure -> _uiCommands.send(
                    UiCommand.ShowSnackbar(
                        message = result.error.toUserMessage(),
                        withDismissAction = true,
                    ),
                )
            }
        }
    }

    private fun updateUiState(transform: (SignupUiState) -> SignupUiState) {
        _uiState.update(transform)
    }
}
