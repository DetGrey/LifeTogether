package com.example.lifetogether.ui.feature.profile

import android.net.Uri
import com.example.lifetogether.domain.model.UserInformation

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Content(
        val userInformation: UserInformation,
        val dialog: ProfileDialogState? = null,
    ) : ProfileUiState
}

sealed interface ProfileDialogState {
    data class ChangeName(val name: String = "") : ProfileDialogState
    data object Logout : ProfileDialogState
}

sealed interface ProfileUiEvent {
    data class ImageSelected(val uri: Uri) : ProfileUiEvent
    data object NameClicked : ProfileUiEvent
    data object LogoutClicked : ProfileUiEvent
    data object DismissDialog : ProfileUiEvent
    data object ConfirmLogout : ProfileUiEvent
    data object ConfirmChangeName : ProfileUiEvent
    data class NameChanged(val value: String) : ProfileUiEvent
}

sealed interface ProfileNavigationEvent {
    data object NavigateBack : ProfileNavigationEvent
    data object NavigateToSettings : ProfileNavigationEvent
}

sealed interface ProfileCommand {
    data object NavigateToLogin : ProfileCommand
}
