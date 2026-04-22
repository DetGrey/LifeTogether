package com.example.lifetogether.ui.feature.guides.details

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.feature.guides.stepplayer.GuideStepPlayerRoute
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.AppRoutes

@Composable
fun GuideDetailsDestinationRoute(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    appNavigator: AppNavigator,
) {
    FeatureSyncLifecycleBinding(
        keys = setOf(SyncKey.GUIDES),
    )

    val guideGraphEntry = remember(backStackEntry) {
        navController.getBackStackEntry(AppRoutes.GUIDE_GRAPH_ROUTE)
    }
    val guideId = guideGraphEntry.arguments?.getString(AppRoutes.GUIDE_ID_ARG)
        ?.let(Uri::decode)
        ?: return

    GuideDetailsRoute(
        appNavigator = appNavigator,
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
    FeatureSyncLifecycleBinding(
        keys = setOf(SyncKey.GUIDES),
    )

    val guideGraphEntry = remember(backStackEntry) {
        navController.getBackStackEntry(AppRoutes.GUIDE_GRAPH_ROUTE)
    }
    val guideId = guideGraphEntry.arguments?.getString(AppRoutes.GUIDE_ID_ARG)
        ?.let(Uri::decode)
        ?: return

    GuideStepPlayerRoute(
        appNavigator = appNavigator,
        guideId = guideId,
        viewModelStoreOwner = guideGraphEntry,
    )
}
