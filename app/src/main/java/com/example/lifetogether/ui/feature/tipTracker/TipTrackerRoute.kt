package com.example.lifetogether.ui.feature.tipTracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.TipTrackerGraph

@Composable
fun TipTrackerRoute(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    appNavigator: AppNavigator,
) {
    val sharedEntry = remember(backStackEntry) {
        navController.getBackStackEntry<TipTrackerGraph>()
    }
    val viewModel: TipTrackerViewModel = hiltViewModel(sharedEntry)

    FeatureSyncLifecycleBinding(
        keys = setOf(SyncKey.TIP_TRACKER),
    )
    TipTrackerScreen(appNavigator, viewModel)
}

@Composable
fun TipStatisticsRoute(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    appNavigator: AppNavigator,
) {
    val sharedEntry = remember(backStackEntry) {
        navController.getBackStackEntry<TipTrackerGraph>()
    }
    val viewModel: TipTrackerViewModel = hiltViewModel(sharedEntry)

    TipStatisticsScreen(appNavigator, viewModel)
}
