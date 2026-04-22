package com.example.lifetogether.ui.feature.groceryList

import androidx.compose.runtime.Composable
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun GroceryListRoute(
    appNavigator: AppNavigator,
) {
    FeatureSyncLifecycleBinding(
        keys = setOf(
            SyncKey.GROCERY_LIST,
            SyncKey.GROCERY_CATEGORIES,
            SyncKey.GROCERY_SUGGESTIONS,
        ),
    )
    GroceryListScreen(appNavigator)
}
