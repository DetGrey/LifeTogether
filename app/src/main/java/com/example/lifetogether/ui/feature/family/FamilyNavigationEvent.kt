package com.example.lifetogether.ui.feature.family

sealed interface FamilyNavigationEvent {
    data object NavigateBack : FamilyNavigationEvent
}
