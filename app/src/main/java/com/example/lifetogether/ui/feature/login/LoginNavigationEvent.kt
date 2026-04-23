package com.example.lifetogether.ui.feature.login

sealed interface LoginNavigationEvent {
    data object NavigateBack : LoginNavigationEvent
    data object SignUpClicked : LoginNavigationEvent
}
