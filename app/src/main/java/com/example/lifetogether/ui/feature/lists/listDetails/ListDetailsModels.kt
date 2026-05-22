package com.example.lifetogether.ui.feature.lists.listDetails

import android.graphics.Bitmap
import com.example.lifetogether.domain.model.lists.ChecklistEntry
import com.example.lifetogether.domain.model.lists.ListEntry
import com.example.lifetogether.domain.model.lists.ListType
import com.example.lifetogether.domain.model.lists.NoteEntry
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.WishListEntry

sealed interface ListDetailsUiState {
    data object Loading : ListDetailsUiState

    data class Content(
        val familyId: String,
        val listContent: ListDetailsListContent,
        val selectedEntryIds: Set<String>,
        val checklistEditorState: ChecklistEditorState = ChecklistEditorState(),
        val isSelectionMode: Boolean = false,
        val isAllEntriesSelected: Boolean = false,
        val showActionSheet: Boolean = false,
        val dialog: ListDetailsDialogState? = null,
    ) : ListDetailsUiState
}

sealed interface ListDetailsDialogState {
    data class RenameList(val name: String = "") : ListDetailsDialogState
}

sealed interface ListDetailsListContent {
    val listName: String
    val listType: ListType
    val entries: List<ListEntry>

    data class Routines(
        override val listName: String,
        override val entries: List<RoutineListEntry>,
        val imageBitmaps: Map<String, Bitmap>,
    ) : ListDetailsListContent {
        override val listType: ListType = ListType.ROUTINE
    }
    data class Wishes(
        override val listName: String,
        override val entries: List<WishListEntry>,
    ) : ListDetailsListContent {
        override val listType: ListType = ListType.WISH_LIST
    }
    data class Notes(
        override val listName: String,
        override val entries: List<NoteEntry>,
    ) : ListDetailsListContent {
        override val listType: ListType = ListType.NOTES
    }
    data class CheckItems(
        override val listName: String,
        override val entries: List<ChecklistEntry>,
    ) : ListDetailsListContent {
        override val listType: ListType = ListType.CHECKLIST
    }
}

sealed interface ListDetailsUiEvent {
    data object ToggleActionSheet : ListDetailsUiEvent
    data object StartSelectionMode : ListDetailsUiEvent
    data class EnterSelectionMode(val entryId: String) : ListDetailsUiEvent
    data object ExitSelectionMode : ListDetailsUiEvent
    data class ToggleEntrySelection(val entryId: String) : ListDetailsUiEvent
    data object ToggleAllEntrySelection : ListDetailsUiEvent
    data object ConfirmDeleteSelected : ListDetailsUiEvent
    data object RequestRenameList : ListDetailsUiEvent
    data object DismissDialog : ListDetailsUiEvent
    data class RenameListNameChanged(val value: String) : ListDetailsUiEvent
    data object ConfirmRenameList : ListDetailsUiEvent
    data class ToggleEntryCompleted(val entryId: String) : ListDetailsUiEvent
    sealed interface Checklist : ListDetailsUiEvent {
        data class EditRequested(val entryId: String) : Checklist
        data class NameChanged(val value: String) : Checklist
        data object ActionClicked : Checklist
    }
}

sealed interface ListDetailsNavigationEvent {
    data object NavigateBack : ListDetailsNavigationEvent
    data object NavigateToCreateEntry : ListDetailsNavigationEvent
    data class NavigateToEntryDetails(val entryId: String) : ListDetailsNavigationEvent
}

data class ChecklistEditorState(
    val editingEntryId: String? = null,
    val draftName: String = "",
)
