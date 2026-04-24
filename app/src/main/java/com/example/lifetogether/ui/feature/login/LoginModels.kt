package com.example.lifetogether.ui.feature.login

data class LoginUiState(
    val email: String = "",
    val password: String = "",
)

sealed interface LoginUiEvent {
    data class EmailChanged(val value: String) : LoginUiEvent
    data class PasswordChanged(val value: String) : LoginUiEvent
    data object LoginClicked : LoginUiEvent
}

sealed interface LoginNavigationEvent {
    data object NavigateBack : LoginNavigationEvent
    data object SignUpClicked : LoginNavigationEvent
}

sealed interface LoginCommand {
    data object NavigateBackOnSuccess : LoginCommand
}
