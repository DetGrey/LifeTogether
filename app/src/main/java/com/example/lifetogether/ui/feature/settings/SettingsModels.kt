package com.example.lifetogether.ui.feature.settings

import com.example.lifetogether.domain.model.UserInformation

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Content(
        val userInformation: UserInformation,
        val dialog: SettingsDialogState? = null,
    ) : SettingsUiState
}

sealed interface SettingsDialogState {
    data class JoinFamily(val familyId: String = "") : SettingsDialogState
    data object CreateFamily : SettingsDialogState
}

sealed interface SettingsUiEvent {
    data object JoinFamilyClicked : SettingsUiEvent
    data object CreateNewFamilyClicked : SettingsUiEvent
    data object DismissDialog : SettingsUiEvent
    data class FamilyIdChanged(val value: String) : SettingsUiEvent
    data object ConfirmJoinFamily : SettingsUiEvent
    data object ConfirmCreateNewFamily : SettingsUiEvent
}

sealed interface SettingsNavigationEvent {
    data object NavigateBack : SettingsNavigationEvent
    data object NavigateToProfile : SettingsNavigationEvent
    data object NavigateToFamily : SettingsNavigationEvent
}
