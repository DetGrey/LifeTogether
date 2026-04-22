package com.example.lifetogether.ui.feature.recipes

import androidx.compose.runtime.Composable
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun RecipesRoute(
    appNavigator: AppNavigator,
) {
    FeatureSyncLifecycleBinding(
        keys = setOf(SyncKey.RECIPES),
    )
    RecipesScreen(appNavigator)
}
