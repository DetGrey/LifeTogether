package com.example.lifetogether.ui.feature.admin.groceryList

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.domain.observer.ObserverKey
import com.example.lifetogether.ui.common.observer.FeatureObserverLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel

@Composable
fun AdminGroceryCategoriesRoute(
    appNavigator: AppNavigator,
) {
    // TODO [Issue #3]: remove bridge after AdminGroceryCategoriesScreen migrates off AppSessionViewModel
    val appSessionViewModel: AppSessionViewModel = hiltViewModel()
    FeatureObserverLifecycleBinding(
        keys = setOf(ObserverKey.GROCERY_CATEGORIES),
    )
    AdminGroceryCategoriesScreen(appNavigator, appSessionViewModel)
}
