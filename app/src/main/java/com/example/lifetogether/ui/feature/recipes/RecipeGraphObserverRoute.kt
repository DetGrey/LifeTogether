package com.example.lifetogether.ui.feature.recipes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.di.rememberSessionRepository
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.RecipeGraph

@Composable
fun RecipeGraphObserverRoute(
    navController: NavHostController,
) {
    val sessionRepository = rememberSessionRepository()
    val sessionState by sessionRepository.sessionState.collectAsStateWithLifecycle()
    val familyId = (sessionState as? SessionState.Authenticated)?.user?.familyId

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val isInRecipeGraph = currentBackStackEntry
        ?.destination
        ?.hierarchy
        ?.any { destination -> destination.hasRoute(RecipeGraph::class) } == true

    if (isInRecipeGraph && !familyId.isNullOrBlank()) {
        FeatureSyncLifecycleBinding(keys = setOf(SyncKey.RECIPES))
    }
}
