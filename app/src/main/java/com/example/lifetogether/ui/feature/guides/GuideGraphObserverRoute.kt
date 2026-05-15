package com.example.lifetogether.ui.feature.guides

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.GuideGraph

@Composable
fun GuideGraphObserverRoute(
    navController: NavHostController,
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val isInGuideGraph = currentBackStackEntry
        ?.destination
        ?.hierarchy
        ?.any { destination -> destination.hasRoute(GuideGraph::class) } == true

    if (isInGuideGraph) {
        FeatureSyncLifecycleBinding(keys = setOf(SyncKey.GUIDES))
    }
}
