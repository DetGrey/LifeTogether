package com.example.lifetogether.ui.feature.lists

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.UserListGraph

@Composable
fun UserListGraphObserverRoute(
    navController: NavHostController,
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val isInUserListGraph = currentBackStackEntry
        ?.destination
        ?.hierarchy
        ?.any { destination -> destination.hasRoute(UserListGraph::class) } == true

    if (isInUserListGraph) {
        FeatureSyncLifecycleBinding(
            keys = setOf(
                SyncKey.USER_LISTS,
                SyncKey.ROUTINE_LIST_ENTRIES,
                SyncKey.WISH_LIST_ENTRIES,
                SyncKey.NOTE_ENTRIES,
                SyncKey.CHECKLIST_ENTRIES,
                SyncKey.MEAL_PLAN_ENTRIES,
            ),
        )
    }
}
