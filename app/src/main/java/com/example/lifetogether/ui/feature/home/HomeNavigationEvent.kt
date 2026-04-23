package com.example.lifetogether.ui.feature.home

sealed interface HomeNavigationEvent {
    data object ProfileClicked : HomeNavigationEvent
    data object SettingsClicked : HomeNavigationEvent
    data object StatusCardClicked : HomeNavigationEvent
    data class TileClicked(val tile: HomeTile) : HomeNavigationEvent
}
