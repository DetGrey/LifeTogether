package com.example.lifetogether.ui.feature.settings

sealed interface SettingsNavigationEvent {
    data object NavigateBack : SettingsNavigationEvent
    data object NavigateToProfile : SettingsNavigationEvent
    data object NavigateToFamily : SettingsNavigationEvent
}
