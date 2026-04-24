package com.example.lifetogether.ui.feature.groceryList

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun GroceryListRoute(
    appNavigator: AppNavigator,
) {
    val viewModel: GroceryListViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CollectUiCommands(viewModel.uiCommands)

    FeatureSyncLifecycleBinding(
        keys = setOf(
            SyncKey.GROCERY_LIST,
            SyncKey.GROCERY_CATEGORIES,
            SyncKey.GROCERY_SUGGESTIONS,
        ),
    )

    GroceryListScreen(
        uiState = uiState,
        onUiEvent = viewModel::onEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                GroceryListNavigationEvent.NavigateBack -> appNavigator.navigateBack()
            }
        },
    )
}
