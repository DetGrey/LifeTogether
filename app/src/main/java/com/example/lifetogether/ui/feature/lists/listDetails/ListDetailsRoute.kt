package com.example.lifetogether.ui.feature.lists.listDetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.ListEntryDetailsNavRoute

@Composable
fun ListDetailsRoute(
    appNavigator: AppNavigator,
) {
    FeatureSyncLifecycleBinding(
        keys = setOf(
            SyncKey.USER_LISTS,
            SyncKey.ROUTINE_LIST_ENTRIES,
            SyncKey.WISH_LIST_ENTRIES,
            SyncKey.NOTE_ENTRIES,
            SyncKey.CHECKLIST_ENTRIES,
            SyncKey.MEAL_PLAN_ENTRIES,
        ),
    )

    val viewModel: ListDetailsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listId = viewModel.listId

    CollectUiCommands(viewModel.uiCommands)

    ListDetailsScreen(
        uiState = uiState,
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
                is ListDetailsUiEvent.ToggleEntryCompleted -> viewModel.toggleEntryCompleted(event.entryId)
            }
        },
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
