package com.example.lifetogether.ui.feature.tipTracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    appSessionViewModel: AppSessionViewModel,
) {
    val sharedEntry = remember(backStackEntry) {
        navController.getBackStackEntry(AppRoutes.TIP_TRACKER_GRAPH)
    }
    val viewModel: TipTrackerViewModel = hiltViewModel(sharedEntry)

    val userInformation by appSessionViewModel.userInformation.collectAsState()
    if (!userInformation?.familyId.isNullOrBlank()) {
        FeatureObserverLifecycleBinding(
            appSessionViewModel = appSessionViewModel,
            keys = setOf(ObserverKey.TIP_TRACKER),
            uid = userInformation?.uid,
            familyId = userInformation?.familyId,
        )
    }

    TipTrackerScreen(appNavigator, appSessionViewModel, viewModel)
}

@Composable
fun TipStatisticsRoute(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    appNavigator: AppNavigator,
    appSessionViewModel: AppSessionViewModel,
) {
    val sharedEntry = remember(backStackEntry) {
        navController.getBackStackEntry(AppRoutes.TIP_TRACKER_GRAPH)
    }
    val viewModel: TipTrackerViewModel = hiltViewModel(sharedEntry)

    TipStatisticsScreen(appNavigator, appSessionViewModel, viewModel)
}
