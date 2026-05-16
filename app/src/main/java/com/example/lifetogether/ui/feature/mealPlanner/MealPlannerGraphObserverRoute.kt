package com.example.lifetogether.ui.feature.mealPlanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.di.rememberSessionRepository
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding

@Composable
fun MealPlannerGraphObserverRoute(
    navController: NavHostController,
) {
    val sessionRepository = rememberSessionRepository()
    val sessionState by sessionRepository.sessionState.collectAsStateWithLifecycle()
    val familyId = (sessionState as? SessionState.Authenticated)?.user?.familyId

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val isInMealPlannerGraph = currentBackStackEntry
        ?.destination
        ?.hierarchy
        ?.any { destination -> destination.hasRoute(com.example.lifetogether.ui.navigation.MealPlannerGraph::class) } == true

    if (isInMealPlannerGraph && !familyId.isNullOrBlank()) {
        FeatureSyncLifecycleBinding(
            keys = setOf(SyncKey.MEAL_PLANNER),
        )
    }
}
