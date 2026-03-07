package com.example.lifetogether.ui.feature.groceryList

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.lifetogether.domain.observer.ObserverKey
import com.example.lifetogether.ui.common.observer.FeatureObserverLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel

@Composable
fun GroceryListRoute(
    appNavigator: AppNavigator,
    appSessionViewModel: AppSessionViewModel,
) {
    val userInformation by appSessionViewModel.userInformation.collectAsState()
    if (!userInformation?.familyId.isNullOrBlank()) {
        FeatureObserverLifecycleBinding(
            appSessionViewModel = appSessionViewModel,
            keys = setOf(
                ObserverKey.GROCERY_LIST,
                ObserverKey.GROCERY_CATEGORIES,
                ObserverKey.GROCERY_SUGGESTIONS,
            ),
            uid = userInformation?.uid,
            familyId = userInformation?.familyId,
        )
    }

    GroceryListScreen(appNavigator, appSessionViewModel)
}
