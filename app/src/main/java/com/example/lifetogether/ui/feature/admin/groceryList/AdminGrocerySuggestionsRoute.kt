package com.example.lifetogether.ui.feature.admin.groceryList

import androidx.compose.runtime.Composable
import com.example.lifetogether.domain.observer.ObserverKey
import com.example.lifetogether.ui.common.observer.FeatureObserverLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun AdminGrocerySuggestionsRoute(
    appNavigator: AppNavigator,
) {
    FeatureObserverLifecycleBinding(
        keys = setOf(ObserverKey.GROCERY_CATEGORIES, ObserverKey.GROCERY_SUGGESTIONS),
    )
    AdminGrocerySuggestionsScreen(appNavigator)
}
