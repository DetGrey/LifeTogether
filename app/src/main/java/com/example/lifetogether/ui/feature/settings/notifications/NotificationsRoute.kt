package com.example.lifetogether.ui.feature.settings.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun NotificationsRoute(appNavigator: AppNavigator) {
    val viewModel: NotificationsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NotificationsScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent,
        onNavigationEvent = { event ->
            when (event) {
                NotificationsNavigationEvent.NavigateBack -> appNavigator.navigateBack()
            }
        },
    )
}
