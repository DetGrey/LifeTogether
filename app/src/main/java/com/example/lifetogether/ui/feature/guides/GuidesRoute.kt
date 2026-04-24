package com.example.lifetogether.ui.feature.guides

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.GuideCreateNavRoute
import com.example.lifetogether.ui.navigation.GuideDetailsNavRoute

@Composable
fun GuidesRoute(
    appNavigator: AppNavigator,
) {
    val viewModel: GuidesViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CollectUiCommands(viewModel.uiCommands)

    FeatureSyncLifecycleBinding(keys = setOf(SyncKey.GUIDES))
    GuidesScreen(
        uiState = uiState,
        onUiEvent = viewModel::onEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                GuidesNavigationEvent.NavigateBack -> appNavigator.navigateBack()
                GuidesNavigationEvent.NavigateToGuideCreate -> appNavigator.navigate(GuideCreateNavRoute)
                is GuidesNavigationEvent.NavigateToGuideDetails -> {
                    appNavigator.navigate(GuideDetailsNavRoute(navigationEvent.guideId))
                }
            }
        },
    )
}
