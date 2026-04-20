package com.example.lifetogether.ui.feature.lists.entryDetails

sealed interface EntryDetailsUiState {
    data object Loading : EntryDetailsUiState
    data class Content(
        val isEditing: Boolean = false,
        val showDiscardDialog: Boolean = false,
        val isSaving: Boolean = false,
        val showAlertDialog: Boolean = false,
        val error: String = "",
    ) : EntryDetailsUiState
}