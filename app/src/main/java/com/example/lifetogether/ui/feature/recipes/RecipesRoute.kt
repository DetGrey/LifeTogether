package com.example.lifetogether.ui.feature.recipes

import androidx.compose.runtime.Composable
import com.example.lifetogether.domain.observer.ObserverKey
import com.example.lifetogether.ui.common.observer.FeatureObserverLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun RecipesRoute(
    appNavigator: AppNavigator,
) {
    FeatureObserverLifecycleBinding(
        keys = setOf(ObserverKey.RECIPES),
    )
    RecipesScreen(appNavigator)
}
