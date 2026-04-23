package com.example.lifetogether.ui.feature.signup

sealed interface SignupNavigationEvent {
    data object NavigateBack : SignupNavigationEvent
    data object LoginClicked : SignupNavigationEvent
}
