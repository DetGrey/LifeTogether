package com.example.lifetogether.ui.feature.guides.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.GuideDetailsNavRoute

@Composable
fun GuideEditRoute(
    appNavigator: AppNavigator,
    guideId: String? = null,
) {
    val viewModel: GuideEditViewModel =
        hiltViewModel<GuideEditViewModel, GuideEditViewModel.Factory> { it.create(guideId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CollectUiCommands(viewModel.uiCommands)

    LaunchedEffect(viewModel) {
        viewModel.commands.collect { command ->
            when (command) {
                is GuideEditCommand.NavigateToGuideDetails -> {
                    appNavigator.navigateReplacing(GuideDetailsNavRoute(command.guideId))
                }
                GuideEditCommand.NavigateBack -> appNavigator.navigateBack()
            }
        }
    }

    GuideEditScreen(
        uiState = uiState,
        onUiEvent = viewModel::onEvent,
    )
}
