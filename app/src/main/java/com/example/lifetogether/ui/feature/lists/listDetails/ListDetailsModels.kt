package com.example.lifetogether.ui.feature.lists.listDetails

import android.graphics.Bitmap
import com.example.lifetogether.domain.model.lists.RoutineListEntry

sealed interface ListDetailsUiState {
    data object Loading : ListDetailsUiState

    data class Content(
        val listName: String = "",
        val isSelectionModeActive: Boolean = false,
        val selectedEntryIds: Set<String> = emptySet(),
        val isAllEntriesSelected: Boolean = false,
        val showActionSheet: Boolean = false,
        val showDeleteSelectedDialog: Boolean = false,
    ) : ListDetailsUiState
}

data class ListDetailsScreenState(
    val uiState: ListDetailsUiState = ListDetailsUiState.Loading,
    val entries: List<RoutineListEntry> = emptyList(),
    val imageBitmaps: Map<String, Bitmap> = emptyMap(),
)

sealed interface ListDetailsUiEvent {
    data object ToggleActionSheet : ListDetailsUiEvent
    data object StartSelectionMode : ListDetailsUiEvent
    data class EnterSelectionMode(val entryId: String) : ListDetailsUiEvent
    data object ExitSelectionMode : ListDetailsUiEvent
    data class ToggleEntrySelection(val entryId: String) : ListDetailsUiEvent
    data object ToggleAllEntrySelection : ListDetailsUiEvent
    data object RequestDeleteSelected : ListDetailsUiEvent
    data object DismissDeleteSelectedDialog : ListDetailsUiEvent
    data object ConfirmDeleteSelected : ListDetailsUiEvent
    data class CompleteEntry(val entry: RoutineListEntry) : ListDetailsUiEvent
}

sealed interface ListDetailsNavigationEvent {
    data object NavigateBack : ListDetailsNavigationEvent
    data object NavigateToCreateEntry : ListDetailsNavigationEvent
    data class NavigateToEntryDetails(val entryId: String) : ListDetailsNavigationEvent
}
