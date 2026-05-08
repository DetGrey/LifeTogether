package com.example.lifetogether.ui.feature.lists.listDetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.ListEntryDetailsNavRoute

@Composable
fun ListDetailsRoute(
    appNavigator: AppNavigator,
) {
    val viewModel: ListDetailsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listId = viewModel.listId

    CollectUiCommands(viewModel.uiCommands)

    ListDetailsScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                ListDetailsNavigationEvent.NavigateBack -> appNavigator.navigateBack()
                ListDetailsNavigationEvent.NavigateToCreateEntry -> {
                    appNavigator.navigate(ListEntryDetailsNavRoute(listId))
                }
                is ListDetailsNavigationEvent.NavigateToEntryDetails -> {
                    appNavigator.navigate(ListEntryDetailsNavRoute(listId, navigationEvent.entryId))
                }
            }
        },
    )
}
