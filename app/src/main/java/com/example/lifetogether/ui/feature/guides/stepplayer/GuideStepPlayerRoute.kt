package com.example.lifetogether.ui.feature.guides.stepplayer

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelStoreOwner
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun GuideStepPlayerRoute(
    appNavigator: AppNavigator? = null,
    guideId: String,
    viewModelStoreOwner: ViewModelStoreOwner? = null,
) {
    val guideStepPlayerViewModel: GuideStepPlayerViewModel = if (viewModelStoreOwner != null) {
        hiltViewModel(viewModelStoreOwner)
    } else {
        hiltViewModel()
    }
    val uiState by guideStepPlayerViewModel.uiState.collectAsStateWithLifecycle()

    CollectUiCommands(guideStepPlayerViewModel.uiCommands)

    LaunchedEffect(guideId) {
        guideStepPlayerViewModel.onEvent(GuideStepPlayerUiEvent.Initialize(guideId))
    }

    val navigateBack = {
        guideStepPlayerViewModel.flushPendingChanges()
        appNavigator?.navigateBack()
    }

    BackHandler {
        navigateBack()
    }

    GuideStepPlayerScreen(
        uiState = uiState,
        onUiEvent = guideStepPlayerViewModel::onEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                GuideStepPlayerNavigationEvent.NavigateBack -> navigateBack()
            }
        },
    )
}
