package com.example.lifetogether.ui.feature.profile

sealed interface ProfileNavigationEvent {
    data object NavigateBack : ProfileNavigationEvent
    data object NavigateToSettings : ProfileNavigationEvent
}
