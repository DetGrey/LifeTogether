package com.example.lifetogether.ui.feature.lists

import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.model.lists.ListType
import com.example.lifetogether.domain.model.lists.UserList

data class ListsUiState(
    val userLists: List<UserList> = emptyList(),
    val showCreateDialog: Boolean = false,
    val newListName: String = "",
    val newListType: ListType = ListType.ROUTINE,
    val newListVisibility: Visibility = Visibility.PRIVATE,
    val isSaving: Boolean = false,
)

sealed interface ListsUiEvent {
    data object CreateListClicked : ListsUiEvent
    data object CreateDialogDismissed : ListsUiEvent
    data class CreateListNameChanged(val value: String) : ListsUiEvent
    data class CreateListTypeChanged(val value: ListType) : ListsUiEvent
    data class CreateListVisibilityChanged(val value: Visibility) : ListsUiEvent
    data object ConfirmCreateListClicked : ListsUiEvent
}

sealed interface ListsNavigationEvent {
    data object NavigateBack : ListsNavigationEvent
    data class NavigateToListDetails(val listId: String) : ListsNavigationEvent
}

sealed interface ListsCommand {
    data class NavigateToListDetails(val listId: String) : ListsCommand
}
