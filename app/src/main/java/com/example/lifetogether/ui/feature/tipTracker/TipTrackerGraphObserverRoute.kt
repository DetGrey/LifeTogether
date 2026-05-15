package com.example.lifetogether.ui.feature.tipTracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.TipTrackerGraph

@Composable
fun TipTrackerGraphObserverRoute(
    navController: NavHostController,
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val isInTipTrackerGraph = currentBackStackEntry
        ?.destination
        ?.hierarchy
        ?.any { destination -> destination.hasRoute(TipTrackerGraph::class) } == true

    if (isInTipTrackerGraph) {
        FeatureSyncLifecycleBinding(keys = setOf(SyncKey.TIP_TRACKER))
    }
}
