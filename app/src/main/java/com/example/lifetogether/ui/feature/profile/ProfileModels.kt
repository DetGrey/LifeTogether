package com.example.lifetogether.ui.feature.profile

import com.example.lifetogether.domain.model.UserInformation

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Content(
        val userInformation: UserInformation? = null,
        val showConfirmationDialog: Boolean = false,
        val showImageUploadDialog: Boolean = false,
        val confirmationDialogType: ProfileConfirmationType? = null,
        val newName: String = "",
    ) : ProfileUiState
}

enum class ProfileConfirmationType {
    LOGOUT,
    NAME,
}

sealed interface ProfileUiEvent {
    data object AddImageClicked : ProfileUiEvent
    data object ImageUploadDismissed : ProfileUiEvent
    data object ImageUploadConfirmed : ProfileUiEvent
    data object NameClicked : ProfileUiEvent
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
