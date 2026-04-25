package com.example.lifetogether.ui.feature.guides.stepplayer

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelStoreOwner
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun GuideStepPlayerRoute(
    appNavigator: AppNavigator,
    viewModelStoreOwner: ViewModelStoreOwner,
) {
    val guideStepPlayerViewModel: GuideStepPlayerViewModel = hiltViewModel(viewModelStoreOwner)
    val uiState by guideStepPlayerViewModel.uiState.collectAsStateWithLifecycle()

    FeatureSyncLifecycleBinding(keys = setOf(SyncKey.GUIDES))

    CollectUiCommands(guideStepPlayerViewModel.uiCommands)

    val navigateBack = {
        guideStepPlayerViewModel.flushPendingChanges()
        appNavigator.navigateBack()
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
