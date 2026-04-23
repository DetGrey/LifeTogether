package com.example.lifetogether.ui.feature.admin.groceryList.suggestions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun AdminGrocerySuggestionsRoute(
    appNavigator: AppNavigator,
) {
    val viewModel: AdminGrocerySuggestionsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CollectUiCommands(viewModel.commands)

    FeatureSyncLifecycleBinding(
        keys = setOf(SyncKey.GROCERY_CATEGORIES, SyncKey.GROCERY_SUGGESTIONS),
    )

    AdminGrocerySuggestionsScreen(
        uiState = uiState,
        onUiEvent = viewModel::onEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                AdminGrocerySuggestionsNavigationEvent.NavigateBack -> appNavigator.navigateBack()
            }
        },
    )
}
