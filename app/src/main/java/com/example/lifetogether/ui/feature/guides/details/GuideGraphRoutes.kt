package com.example.lifetogether.ui.feature.guides.details

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    appSessionViewModel: AppSessionViewModel,
) {
    val userInformation by appSessionViewModel.userInformation.collectAsState()
    if (!userInformation?.familyId.isNullOrBlank() && !userInformation?.uid.isNullOrBlank()) {
        FeatureObserverLifecycleBinding(
            appSessionViewModel = appSessionViewModel,
            keys = setOf(ObserverKey.GUIDES),
            uid = userInformation?.uid,
            familyId = userInformation?.familyId,
        )
    }

    val guideGraphEntry = remember(backStackEntry) {
        navController.getBackStackEntry(AppRoutes.GUIDE_GRAPH_ROUTE)
    }
    val guideId = guideGraphEntry.arguments?.getString(AppRoutes.GUIDE_ID_ARG)
        ?.let(Uri::decode)
        ?: return

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
    appSessionViewModel: AppSessionViewModel,
) {
    val userInformation by appSessionViewModel.userInformation.collectAsState()
    if (!userInformation?.familyId.isNullOrBlank() && !userInformation?.uid.isNullOrBlank()) {
        FeatureObserverLifecycleBinding(
            appSessionViewModel = appSessionViewModel,
            keys = setOf(ObserverKey.GUIDES),
            uid = userInformation?.uid,
            familyId = userInformation?.familyId,
        )
    }

    val guideGraphEntry = remember(backStackEntry) {
        navController.getBackStackEntry(AppRoutes.GUIDE_GRAPH_ROUTE)
    }
    val guideId = guideGraphEntry.arguments?.getString(AppRoutes.GUIDE_ID_ARG)
        ?.let(Uri::decode)
        ?: return

    GuideStepPlayerRoute(
        appNavigator = appNavigator,
        appSessionViewModel = appSessionViewModel,
        guideId = guideId,
        viewModelStoreOwner = guideGraphEntry,
    )
}
