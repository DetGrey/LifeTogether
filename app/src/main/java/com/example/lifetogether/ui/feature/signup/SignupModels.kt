package com.example.lifetogether.ui.feature.signup

import java.util.Date

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

sealed interface SignupNavigationEvent {
    data object NavigateBack : SignupNavigationEvent
    data object LoginClicked : SignupNavigationEvent
}

sealed interface SignupCommand {
    data object NavigateToProfile : SignupCommand
}
