package com.example.lifetogether.ui.feature.lists.listDetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun ListDetailsRoute(
    listId: String,
    appNavigator: AppNavigator,
) {
    FeatureSyncLifecycleBinding(keys = setOf(SyncKey.USER_LISTS, SyncKey.ROUTINE_LIST_ENTRIES))

    val viewModel: ListDetailsViewModel = hiltViewModel()
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    CollectUiCommands(viewModel.uiCommands)

    LaunchedEffect(listId) {
        if (listId.isNotBlank()) {
            viewModel.setUp(listId)
        }
    }

    ListDetailsScreen(
        screenState = screenState,
        onUiEvent = { event ->
            when (event) {
                ListDetailsUiEvent.ToggleActionSheet -> viewModel.toggleActionSheet()
                ListDetailsUiEvent.StartSelectionMode -> viewModel.startSelectionMode()
                is ListDetailsUiEvent.EnterSelectionMode -> viewModel.enterSelectionMode(event.entryId)
                ListDetailsUiEvent.ExitSelectionMode -> viewModel.exitSelectionMode()
                is ListDetailsUiEvent.ToggleEntrySelection -> viewModel.toggleEntrySelection(event.entryId)
                ListDetailsUiEvent.ToggleAllEntrySelection -> viewModel.toggleAllEntrySelection()
                ListDetailsUiEvent.RequestDeleteSelected -> viewModel.requestDeleteSelected()
                ListDetailsUiEvent.DismissDeleteSelectedDialog -> viewModel.dismissDeleteSelectedDialog()
                ListDetailsUiEvent.ConfirmDeleteSelected -> viewModel.confirmDeleteSelected()
                is ListDetailsUiEvent.CompleteEntry -> viewModel.completeEntry(event.entry)
            }
        },
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                ListDetailsNavigationEvent.NavigateBack -> appNavigator.navigateBack()
                ListDetailsNavigationEvent.NavigateToCreateEntry -> {
                    appNavigator.navigateToListEntryDetails(listId)
                }
                is ListDetailsNavigationEvent.NavigateToEntryDetails -> {
                    appNavigator.navigateToListEntryDetails(listId, navigationEvent.entryId)
                }
            }
        },
    )
}
