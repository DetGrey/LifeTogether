package com.example.lifetogether.ui.feature.guides.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.GuideDetailsNavRoute

@Composable
fun GuideCreateRoute(
    appNavigator: AppNavigator,
) {
    val viewModel: GuideCreateViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CollectUiCommands(viewModel.uiCommands)

    LaunchedEffect(viewModel) {
        viewModel.commands.collect { command ->
            when (command) {
                is GuideCreateCommand.NavigateToGuideDetails -> {
                    appNavigator.navigate(GuideDetailsNavRoute(command.guideId))
                }
            }
        }
    }

    GuideCreateScreen(
        uiState = uiState,
        onUiEvent = viewModel::onEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                GuideCreateNavigationEvent.NavigateBack -> appNavigator.navigateBack()
            }
        },
    )
}
