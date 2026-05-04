package com.example.lifetogether.ui.feature.lists.listDetails

import android.graphics.Bitmap
import com.example.lifetogether.domain.model.lists.ListEntry
import com.example.lifetogether.domain.model.lists.ListType

sealed interface ListDetailsUiState {
    data object Loading : ListDetailsUiState

    data class Content(
        val listName: String,
        val listType: ListType,
        val entries: List<ListEntry>,
        val imageBitmaps: Map<String, Bitmap>,
        val isSelectionModeActive: Boolean,
        val selectedEntryIds: Set<String>,
        val isAllEntriesSelected: Boolean,
        val showActionSheet: Boolean,
        val showDeleteSelectedDialog: Boolean,
    ) : ListDetailsUiState
}

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
    data class ToggleEntryCompleted(val entryId: String) : ListDetailsUiEvent
}

sealed interface ListDetailsNavigationEvent {
    data object NavigateBack : ListDetailsNavigationEvent
    data object NavigateToCreateEntry : ListDetailsNavigationEvent
    data class NavigateToEntryDetails(val entryId: String) : ListDetailsNavigationEvent
}
