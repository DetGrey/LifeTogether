package com.example.lifetogether.ui.feature.admin.groceryList

import androidx.compose.runtime.Composable
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun AdminGroceryCategoriesRoute(
    appNavigator: AppNavigator,
) {
    FeatureSyncLifecycleBinding(
        keys = setOf(SyncKey.GROCERY_CATEGORIES),
    )
    AdminGroceryCategoriesScreen(appNavigator)
}
