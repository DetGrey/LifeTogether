package com.example.lifetogether.ui.feature.tipTracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.example.lifetogether.domain.observer.ObserverKey
import com.example.lifetogether.ui.common.observer.FeatureObserverLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.AppRoutes
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel

@Composable
fun TipTrackerRoute(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    appNavigator: AppNavigator,
) {
    val sharedEntry = remember(backStackEntry) {
        navController.getBackStackEntry(AppRoutes.TIP_TRACKER_GRAPH)
    }
    val viewModel: TipTrackerViewModel = hiltViewModel(sharedEntry)

    // TODO [Issue #3]: remove bridge after TipTrackerScreen migrates off AppSessionViewModel
    val appSessionViewModel: AppSessionViewModel = hiltViewModel()
    FeatureObserverLifecycleBinding(
        keys = setOf(ObserverKey.TIP_TRACKER),
    )
    TipTrackerScreen(appNavigator, appSessionViewModel, viewModel)
}

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

    // TODO [Issue #3]: remove bridge after TipStatisticsScreen migrates off AppSessionViewModel
    val appSessionViewModel: AppSessionViewModel = hiltViewModel()
    TipStatisticsScreen(appNavigator, appSessionViewModel, viewModel)
}
