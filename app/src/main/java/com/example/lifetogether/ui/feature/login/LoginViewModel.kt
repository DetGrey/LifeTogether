package com.example.lifetogether.ui.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.domain.usecase.user.LoginUseCase
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
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _commands = Channel<LoginCommand>(Channel.BUFFERED)
    val commands: Flow<LoginCommand> = _commands.receiveAsFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.EmailChanged -> updateUiState { it.copy(email = event.value) }
            is LoginUiEvent.PasswordChanged -> updateUiState { it.copy(password = event.value) }
            LoginUiEvent.LoginClicked -> login()
        }
    }

    private fun login() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val loginResult = loginUseCase.invoke(User(state.email, state.password))) {
                is Result.Success -> _commands.send(LoginCommand.NavigateBackOnSuccess)
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _uiCommands.send(
                        UiCommand.ShowSnackbar(
                            message = loginResult.error.toUserMessage(),
                            withDismissAction = true,
                        ),
                    )
                }
            }
        }
    }

    private fun updateUiState(transform: (LoginUiState) -> LoginUiState) {
        _uiState.update(transform)
    }
}
