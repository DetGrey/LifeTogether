package com.example.lifetogether.ui.feature.groceryList

import androidx.compose.runtime.Composable
import com.example.lifetogether.domain.observer.ObserverKey
import com.example.lifetogether.ui.common.observer.FeatureObserverLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun GroceryListRoute(
    appNavigator: AppNavigator,
) {
    FeatureObserverLifecycleBinding(
        keys = setOf(
            ObserverKey.GROCERY_LIST,
            ObserverKey.GROCERY_CATEGORIES,
            ObserverKey.GROCERY_SUGGESTIONS,
        ),
    )
    GroceryListScreen(appNavigator)
}
