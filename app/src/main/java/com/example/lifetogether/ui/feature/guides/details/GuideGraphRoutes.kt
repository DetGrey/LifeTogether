package com.example.lifetogether.ui.feature.guides.details

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.example.lifetogether.domain.observer.ObserverKey
import com.example.lifetogether.ui.common.observer.FeatureObserverLifecycleBinding
import com.example.lifetogether.ui.feature.guides.stepplayer.GuideStepPlayerRoute
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.AppRoutes
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel

@Composable
fun GuideDetailsDestinationRoute(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    appNavigator: AppNavigator,
) {
    FeatureObserverLifecycleBinding(
        keys = setOf(ObserverKey.GUIDES),
    )

    val guideGraphEntry = remember(backStackEntry) {
        navController.getBackStackEntry(AppRoutes.GUIDE_GRAPH_ROUTE)
    }
    val guideId = guideGraphEntry.arguments?.getString(AppRoutes.GUIDE_ID_ARG)
        ?.let(Uri::decode)
        ?: return

    // TODO [Issue #3]: remove bridge after GuideDetailsRoute migrates off AppSessionViewModel
    val appSessionViewModel: AppSessionViewModel = hiltViewModel()
    GuideDetailsRoute(
        appNavigator = appNavigator,
        appSessionViewModel = appSessionViewModel,
        guideId = guideId,
        viewModelStoreOwner = guideGraphEntry,
    )
}

@Composable
fun GuideStepPlayerDestinationRoute(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    appNavigator: AppNavigator,
) {
    FeatureObserverLifecycleBinding(
        keys = setOf(ObserverKey.GUIDES),
    )

    val guideGraphEntry = remember(backStackEntry) {
        navController.getBackStackEntry(AppRoutes.GUIDE_GRAPH_ROUTE)
    }
    val guideId = guideGraphEntry.arguments?.getString(AppRoutes.GUIDE_ID_ARG)
        ?.let(Uri::decode)
        ?: return

    // TODO [Issue #3]: remove bridge after GuideStepPlayerRoute migrates off AppSessionViewModel
    val appSessionViewModel: AppSessionViewModel = hiltViewModel()
    GuideStepPlayerRoute(
        appNavigator = appNavigator,
        appSessionViewModel = appSessionViewModel,
        guideId = guideId,
        viewModelStoreOwner = guideGraphEntry,
    )
}
