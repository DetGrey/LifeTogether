package com.example.lifetogether.ui.feature.settings

import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.enums.SettingsConfirmationTypes

data class SettingsUiState(
    val userInformation: UserInformation? = null,
    val showConfirmationDialog: Boolean = false,
    val confirmationDialogType: SettingsConfirmationTypes? = null,
    val addedFamilyId: String = "",
)

sealed interface SettingsUiEvent {
    data object JoinFamilyClicked : SettingsUiEvent
    data object CreateNewFamilyClicked : SettingsUiEvent
    data object DismissConfirmationDialog : SettingsUiEvent
    data class AddedFamilyIdChanged(val value: String) : SettingsUiEvent
    data object ConfirmJoinFamily : SettingsUiEvent
    data object ConfirmCreateNewFamily : SettingsUiEvent
}

sealed interface SettingsNavigationEvent {
    data object NavigateBack : SettingsNavigationEvent
    data object NavigateToProfile : SettingsNavigationEvent
    data object NavigateToFamily : SettingsNavigationEvent
}
