package com.example.lifetogether.ui.feature.lists.listDetails

sealed interface ListDetailsUiState {
    data object Loading : ListDetailsUiState

    data class Content(
        val listName: String = "",
        val isSelectionModeActive: Boolean = false,
        val selectedEntryIds: Set<String> = emptySet(),
        val isAllEntriesSelected: Boolean = false,
        val showActionSheet: Boolean = false,
        val showDeleteSelectedDialog: Boolean = false,
        val showAlertDialog: Boolean = false,
        val error: String = "",
    ) : ListDetailsUiState
}
