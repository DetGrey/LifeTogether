package com.example.lifetogether.ui.feature.guides.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.feature.guides.stepplayer.GuideStepPlayerRoute
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.GuideGraph

@Composable
fun GuideDetailsDestinationRoute(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    guideId: String,
    appNavigator: AppNavigator,
) {
    FeatureSyncLifecycleBinding(
        keys = setOf(SyncKey.GUIDES),
    )

    val guideGraphEntry = remember(backStackEntry) {
        navController.getBackStackEntry<GuideGraph>()
    }

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
    guideId: String,
    appNavigator: AppNavigator,
) {
    FeatureSyncLifecycleBinding(
        keys = setOf(SyncKey.GUIDES),
    )

    val guideGraphEntry = remember(backStackEntry) {
        navController.getBackStackEntry<GuideGraph>()
    }

    GuideStepPlayerRoute(
        appNavigator = appNavigator,
        guideId = guideId,
        viewModelStoreOwner = guideGraphEntry,
    )
}
