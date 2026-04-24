package com.example.lifetogether.ui.feature.profile

import com.example.lifetogether.domain.model.UserInformation

data class ProfileUiState(
    val userInformation: UserInformation? = null,
    val showConfirmationDialog: Boolean = false,
    val confirmationDialogType: ProfileConfirmationType? = null,
    val newName: String = "",
)

enum class ProfileConfirmationType {
    LOGOUT,
    NAME,
    PASSWORD,
}

sealed interface ProfileUiEvent {
    data object AddImageClicked : ProfileUiEvent
    data object ImageUploadDismissed : ProfileUiEvent
    data object ImageUploadConfirmed : ProfileUiEvent
    data object NameClicked : ProfileUiEvent
    data object PasswordClicked : ProfileUiEvent
    data object LogoutClicked : ProfileUiEvent
    data object DismissConfirmationDialog : ProfileUiEvent
    data object ConfirmConfirmationDialog : ProfileUiEvent
    data class NewNameChanged(val value: String) : ProfileUiEvent
}

sealed interface ProfileNavigationEvent {
    data object NavigateBack : ProfileNavigationEvent
    data object NavigateToSettings : ProfileNavigationEvent
}

sealed interface ProfileCommand {
    data object NavigateToHome : ProfileCommand
}
