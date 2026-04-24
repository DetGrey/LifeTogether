package com.example.lifetogether.ui.feature.admin.groceryList.categories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun AdminGroceryCategoriesRoute(
    appNavigator: AppNavigator,
) {
    val viewModel: AdminGroceryCategoriesViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CollectUiCommands(viewModel.uiCommands)

    FeatureSyncLifecycleBinding(keys = setOf(SyncKey.GROCERY_CATEGORIES))

    AdminGroceryCategoriesScreen(
        uiState = uiState,
        onUiEvent = viewModel::onEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                AdminGroceryCategoriesNavigationEvent.NavigateBack -> appNavigator.navigateBack()
            }
        },
    )
}
