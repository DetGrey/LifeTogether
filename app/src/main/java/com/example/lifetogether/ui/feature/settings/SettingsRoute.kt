package com.example.lifetogether.ui.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun SettingsRoute(
    appNavigator: AppNavigator,
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CollectUiCommands(viewModel.uiCommands)

    SettingsScreen(
        uiState = uiState,
        onUiEvent = viewModel::onEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                SettingsNavigationEvent.NavigateBack -> appNavigator.navigateBack()
                SettingsNavigationEvent.NavigateToProfile -> appNavigator.navigateToProfile()
                SettingsNavigationEvent.NavigateToFamily -> appNavigator.navigateToFamily()
            }
        },
    )
}
