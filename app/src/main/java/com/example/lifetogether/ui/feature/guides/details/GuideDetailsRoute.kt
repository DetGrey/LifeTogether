package com.example.lifetogether.ui.feature.guides.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.GuideStepPlayerNavRoute

@Composable
fun GuideDetailsRoute(
    appNavigator: AppNavigator,
    viewModelStoreOwner: ViewModelStoreOwner,
) {
    val guideDetailsViewModel: GuideDetailsViewModel = hiltViewModel(viewModelStoreOwner)
    val uiState by guideDetailsViewModel.uiState.collectAsStateWithLifecycle()

    FeatureSyncLifecycleBinding(keys = setOf(SyncKey.GUIDES))

    CollectUiCommands(guideDetailsViewModel.uiCommands)

    LaunchedEffect(guideDetailsViewModel) {
        guideDetailsViewModel.commands.collect { command ->
            when (command) {
                GuideDetailsCommand.NavigateToGuideStepPlayer -> {
                    val guideId = (guideDetailsViewModel.uiState.value as? GuideDetailsUiState.Content)
                        ?.guide
                        ?.id
                        ?: return@collect
                    appNavigator.navigate(GuideStepPlayerNavRoute(guideId))
                }
                GuideDetailsCommand.NavigateBack -> {
                    appNavigator.navigateBack()
                }
            }
        }
    }

    GuideDetailsScreen(
        uiState = uiState,
        onUiEvent = guideDetailsViewModel::onEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                GuideDetailsNavigationEvent.NavigateBack -> {
                    guideDetailsViewModel.flushPendingChanges()
                    appNavigator.navigateBack()
                }
            }
        },
    )
}
