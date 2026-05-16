package com.example.lifetogether.ui.feature.lists.listDetails

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.lists.ChecklistEntry
import com.example.lifetogether.domain.model.lists.NoteEntry
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.WishListPriority
import com.example.lifetogether.domain.model.lists.WishListEntry
import com.example.lifetogether.ui.common.ActionSheet
import com.example.lifetogether.ui.common.ActionSheetItem
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.add.AddNewString
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.list.CompletableBox
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.feature.lists.listDetails.content.CheckItemsSection
import com.example.lifetogether.ui.feature.lists.listDetails.content.NotesSection
import com.example.lifetogether.ui.feature.lists.listDetails.content.RoutinesSection
import com.example.lifetogether.ui.feature.lists.listDetails.content.WishesSection
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailsScreen(
    uiState: ListDetailsUiState,
    onUiEvent: (ListDetailsUiEvent) -> Unit,
    onNavigationEvent: (ListDetailsNavigationEvent) -> Unit,
) {
    val isLoading = uiState is ListDetailsUiState.Loading
    val contentState = uiState as? ListDetailsUiState.Content
    val listName = contentState?.listContent?.listName ?: "List"

    Scaffold(
        topBar = {
            AppTopBar(
                leftIcon = Icon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = { onNavigationEvent(ListDetailsNavigationEvent.NavigateBack) },
                text = listName,
                rightIcon = Icon(
                    resId = R.drawable.ic_overflow_menu,
                    description = "overflow menu",
                ),
                onRightClick = { onUiEvent(ListDetailsUiEvent.ToggleActionSheet) },
            )
        },
        floatingActionButton = {
            if (contentState?.isSelectionMode != true && contentState != null) {
                if (contentState.listContent !is ListDetailsListContent.CheckItems) {
                    AddButton {
                        onNavigationEvent(ListDetailsNavigationEvent.NavigateToCreateEntry)
                    }
                }
            }
        },
        bottomBar = {
            if (contentState?.isSelectionMode != true && contentState?.listContent is ListDetailsListContent.CheckItems) {
                AddNewString(
                    modifier = Modifier.padding(LifeTogetherTokens.spacing.medium),
                    label = "New checklist item",
                    textValue = contentState.checklistEditorState.draftName,
                    onTextChange = { value ->
                        onUiEvent(ListDetailsUiEvent.Checklist.NameChanged(value))
                    },
                    actionLabel = if (contentState.checklistEditorState.editingEntryId == null) "Add" else "Save",
                    onAddClick = {
                        onUiEvent(ListDetailsUiEvent.Checklist.ActionClicked)
                    },
                )
            }
        },
    ) { padding ->
        AnimatedLoadingContent(
            isLoading = isLoading,
            label = "list_details_loading_content",
            loadingContent = {
                Skeletons.ListDetail(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            },
        ) {
            val listContent = contentState?.listContent ?: return@AnimatedLoadingContent
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = LifeTogetherTokens.spacing.small),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedContent(
                    targetState = contentState.isSelectionMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "selection_mode_bar",
                ) { selectionActive ->
                    if (selectionActive) {
                        SelectionModeBar(
                            selectedCount = contentState.selectedEntryIds.size,
                            isAllSelected = contentState.isAllEntriesSelected,
                            onToggleAll = { onUiEvent(ListDetailsUiEvent.ToggleAllEntrySelection) },
                            onCancel = { onUiEvent(ListDetailsUiEvent.ExitSelectionMode) },
                        )
                    } else {
                        Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.medium))
                    }
                }

                if (listContent.entries.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "No entries yet. Tap + to add one.")
                    }
                } else {
                    val onEntryClick: (String) -> Unit = { entryId ->
                        if (contentState.isSelectionMode) {
                            onUiEvent(ListDetailsUiEvent.ToggleEntrySelection(entryId))
                        } else {
                            onNavigationEvent(ListDetailsNavigationEvent.NavigateToEntryDetails(entryId))
                        }
                    }
                    val onEntryLongClick: (String) -> Unit = { entryId ->
                        if (!contentState.isSelectionMode) {
                            onUiEvent(ListDetailsUiEvent.EnterSelectionMode(entryId))
                        }
                    }
                    val onEntryToggleComplete: (String) -> Unit = { entryId ->
                        onUiEvent(ListDetailsUiEvent.ToggleEntryCompleted(entryId))
                    }
                    when (listContent) {
                        is ListDetailsListContent.Routines -> RoutinesSection(
                            entries = listContent.entries,
                            imageBitmaps = listContent.imageBitmaps,
                            isSelectionMode = contentState.isSelectionMode,
                            selectedIds = contentState.selectedEntryIds,
                            onClick = onEntryClick,
                            onLongClick = onEntryLongClick,
                            onComplete = onEntryToggleComplete,
                        )
                        is ListDetailsListContent.Wishes -> WishesSection(
                            entries = listContent.entries,
                            isSelectionMode = contentState.isSelectionMode,
                            selectedIds = contentState.selectedEntryIds,
                            onClick = onEntryClick,
                            onLongClick = onEntryLongClick,
                            onComplete = onEntryToggleComplete,
                        )
                        is ListDetailsListContent.CheckItems -> CheckItemsSection(
                            entries = listContent.entries,
                            isSelectionMode = contentState.isSelectionMode,
                            selectedIds = contentState.selectedEntryIds,
                            onClick = { entryId -> onUiEvent(ListDetailsUiEvent.ToggleEntrySelection(entryId)) },
                            onLongClick = onEntryLongClick,
                            onComplete = onEntryToggleComplete,
                            onEdit = { entryId -> onUiEvent(ListDetailsUiEvent.Checklist.EditRequested(entryId)) },
                        )
                        is ListDetailsListContent.Notes -> NotesSection(
                            entries = listContent.entries,
                            selectedIds = contentState.selectedEntryIds,
                            onClick = onEntryClick,
                            onLongClick = onEntryLongClick,
                        )
                    }
                }
            }
        }
    }

    if (contentState?.showActionSheet == true) {
        val actions = when (contentState.isSelectionMode) {
            true -> {
                listOf(
                    ActionSheetItem(
                        label = "Delete selected",
                        onClick = { onUiEvent(ListDetailsUiEvent.RequestDeleteSelected) },
                        isDestructive = true,
                        isEnabled = contentState.selectedEntryIds.isNotEmpty(),
                    ),
                )
            }

            false -> {
                listOf(
                    ActionSheetItem(
                        label = "Select entries",
                        onClick = { onUiEvent(ListDetailsUiEvent.StartSelectionMode) },
                        isEnabled = contentState.listContent.entries.isNotEmpty(),
                    ),
                )
            }
        }

        ActionSheet(
            onDismiss = { onUiEvent(ListDetailsUiEvent.ToggleActionSheet) },
            actionsList = actions,
        )
    }

    if (contentState?.showDeleteSelectedDialog == true) {
        ConfirmationDialog(
            onDismiss = { onUiEvent(ListDetailsUiEvent.DismissDeleteSelectedDialog) },
            onConfirm = { onUiEvent(ListDetailsUiEvent.ConfirmDeleteSelected) },
            dialogTitle = "Delete selected entries",
            dialogMessage = "Are you sure you want to delete the selected entries?",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Delete selected",
        )
    }
}

@Composable
private fun SelectionModeBar(
    selectedCount: Int,
    isAllSelected: Boolean,
    onToggleAll: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = LifeTogetherTokens.spacing.small,
                bottom = LifeTogetherTokens.spacing.medium
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompletableBox(
                isCompleted = isAllSelected,
                onCompleteToggle = onToggleAll,
            )
            TextDefault(text = "All")
        }

        TextDefault(text = "$selectedCount selected")

        TextDefault(
            text = "Cancel",
            modifier = Modifier.combinedClickable(
                onClick = onCancel,
                onLongClick = onCancel,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingPreview() {
    LifeTogetherTheme {
        ListDetailsScreen(
            uiState = ListDetailsUiState.Loading,
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RoutinePreview() {
    LifeTogetherTheme {
        ListDetailsScreen(
            uiState = previewState(
                ListDetailsListContent.Routines(
                    listName = "House",
                    entries = listOf(
                        RoutineListEntry(
                            id = "routine-1",
                            familyId = "family-1",
                            listId = "list-1",
                            itemName = "Water avocado plants",
                            lastUpdated = Date(),
                            dateCreated = Date(),
                            recurrenceUnit = RecurrenceUnit.DAYS,
                            interval = 1,
                            weekdays = emptyList(),
                            nextDate = Date(),
                        ),
                    ),
                    imageBitmaps = emptyMap(),
                ),
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WishListPreview() {
    LifeTogetherTheme {
        ListDetailsScreen(
            uiState = previewState(
                ListDetailsListContent.Wishes(
                    listName = "Wishlist",
                    entries = listOf(
                        WishListEntry(
                            id = "wish-1",
                            familyId = "family-1",
                            listId = "list-1",
                            itemName = "Standing lamp",
                            lastUpdated = Date(),
                            dateCreated = Date(),
                            isPurchased = false,
                            url = "example.com/lamp",
                            price = 499.0,
                            currencyCode = "DKK",
                            priority = WishListPriority.URGENT,
                            notes = "Prefer warm light and a matte finish.",
                        ),
                        WishListEntry(
                            id = "wish-2",
                            familyId = "family-1",
                            listId = "list-1",
                            itemName = "Coffee grinder",
                            lastUpdated = Date(),
                            dateCreated = Date(),
                            isPurchased = true,
                            url = null,
                            price = 899.0,
                            currencyCode = "DKK",
                            priority = WishListPriority.PLANNED,
                            notes = "Already bought.",
                        ),
                    ),
                ),
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotesPreview() {
    LifeTogetherTheme {
        ListDetailsScreen(
            uiState = previewState(
                ListDetailsListContent.Notes(
                    listName = "Notes",
                    entries = listOf(
                        NoteEntry(
                            id = "note-1",
                            familyId = "family-1",
                            listId = "list-1",
                            itemName = "Weekend plan",
                            body = "Pick up groceries, clean the kitchen, and call mom after lunch. Do so much more this is a very long list",
                            lastUpdated = Date(),
                            dateCreated = Date(),
                        ),
                    ),
                ),
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChecklistPreview() {
    LifeTogetherTheme {
        ListDetailsScreen(
            uiState = previewState(
                ListDetailsListContent.CheckItems(
                    listName = "Packing list",
                    entries = listOf(
                        ChecklistEntry(
                            id = "check-1",
                            familyId = "family-1",
                            listId = "list-1",
                            itemName = "Passport",
                            isChecked = false,
                            lastUpdated = Date(),
                            dateCreated = Date(),
                        ),
                        ChecklistEntry(
                            id = "check-2",
                            familyId = "family-1",
                            listId = "list-1",
                            itemName = "Chargers",
                            isChecked = true,
                            lastUpdated = Date(),
                            dateCreated = Date(),
                        ),
                    ),
                ),
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}

private fun previewState(
    listContent: ListDetailsListContent,
) = ListDetailsUiState.Content(
    familyId = "family-1",
    listContent = listContent,
    selectedEntryIds = emptySet(),
    isSelectionMode = false,
    isAllEntriesSelected = false,
    showActionSheet = false,
    showDeleteSelectedDialog = false,
)
