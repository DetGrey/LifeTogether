package com.example.lifetogether.ui.feature.tipTracker.statistics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.feature.tipTracker.TipTrackerNavigationEvent
import com.example.lifetogether.ui.feature.tipTracker.TipTrackerViewModel
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.AppRoutes

@Composable
fun TipStatisticsRoute(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    appNavigator: AppNavigator,
) {
    val sharedEntry = remember(backStackEntry) {
        navController.getBackStackEntry(AppRoutes.TIP_TRACKER_GRAPH)
    }
    val viewModel: TipTrackerViewModel = hiltViewModel(sharedEntry)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CollectUiCommands(viewModel.uiCommands)

    FeatureSyncLifecycleBinding(keys = setOf(SyncKey.TIP_TRACKER))

    TipStatisticsScreen(
        uiState = uiState,
        onUiEvent = viewModel::onEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                TipTrackerNavigationEvent.NavigateBack -> appNavigator.navigateBack()
                TipTrackerNavigationEvent.NavigateToStatistics -> appNavigator.navigateToTipStatistics()
            }
        },
    )
}