package com.example.lifetogether.ui.feature.tipTracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.TipStatisticsNavRoute

@Composable
fun TipTrackerRoute(
    viewModelStoreOwner: ViewModelStoreOwner,
    appNavigator: AppNavigator,
) {
    val viewModel: TipTrackerViewModel = hiltViewModel(viewModelStoreOwner)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CollectUiCommands(viewModel.uiCommands)

    TipTrackerScreen(
        uiState = uiState,
        onUiEvent = viewModel::onEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                TipTrackerNavigationEvent.NavigateBack -> appNavigator.navigateBack()
                TipTrackerNavigationEvent.NavigateToStatistics -> appNavigator.navigate(TipStatisticsNavRoute)
            }
        },
    )
}
