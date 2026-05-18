package com.example.lifetogether.ui.feature.lists

import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.model.lists.ListType
import com.example.lifetogether.domain.model.lists.UserList

sealed interface ListsUiState {
    data object Loading : ListsUiState

    data class Content(
        val userLists: List<UserList>,
        val showCreateDialog: Boolean = false,
        val newListName: String = "",
        val newListType: ListType = ListType.ROUTINE,
        val newListVisibility: Visibility = Visibility.PRIVATE,
        val isSaving: Boolean = false,
        val isSelectionMode: Boolean = false,
        val selectedListIds: Set<String> = emptySet(),
        val isAllSelected: Boolean = false,
        val showActionSheet: Boolean = false,
        val showDeleteSelectedDialog: Boolean = false,
    ) : ListsUiState
}

sealed interface ListsUiEvent {
    data object CreateListClicked : ListsUiEvent
    data object CreateDialogDismissed : ListsUiEvent
    data class CreateListNameChanged(val value: String) : ListsUiEvent
    data class CreateListTypeChanged(val value: ListType) : ListsUiEvent
    data class CreateListVisibilityChanged(val value: Visibility) : ListsUiEvent
    data object ConfirmCreateListClicked : ListsUiEvent
    data object ToggleActionSheet : ListsUiEvent
    data object StartSelectionMode : ListsUiEvent
    data class EnterSelectionMode(val listId: String) : ListsUiEvent
    data object ExitSelectionMode : ListsUiEvent
    data class ToggleListSelection(val listId: String) : ListsUiEvent
    data object ToggleAllListSelection : ListsUiEvent
    data object RequestDeleteSelected : ListsUiEvent
    data object DismissDeleteSelectedDialog : ListsUiEvent
    data object ConfirmDeleteSelected : ListsUiEvent
}

sealed interface ListsNavigationEvent {
    data object NavigateBack : ListsNavigationEvent
    data class NavigateToListDetails(val listId: String) : ListsNavigationEvent
}

sealed interface ListsCommand {
    data class NavigateToListDetails(val listId: String) : ListsCommand
}
