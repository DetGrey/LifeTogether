package com.example.lifetogether.ui.feature.lists

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.AppIcon
import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.model.lists.ListType
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.ui.common.ActionSheet
import com.example.lifetogether.ui.common.ActionSheetItem
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.button.SecondaryButton
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.list.SelectionModeBar
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.common.text.TextLabel
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import java.util.Date

@Composable
fun ListsScreen(
    uiState: ListsUiState,
    onUiEvent: (ListsUiEvent) -> Unit,
    onNavigationEvent: (ListsNavigationEvent) -> Unit,
) {
    val contentState = uiState as? ListsUiState.Content
    val isLoading = uiState is ListsUiState.Loading

    Scaffold(
        topBar = {
            AppTopBar(
                leftAppIcon = AppIcon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = { onNavigationEvent(ListsNavigationEvent.NavigateBack) },
                text = "Lists",
                rightAppIcon = if (contentState != null) AppIcon(
                    resId = R.drawable.ic_overflow_menu,
                    description = "overflow menu",
                ) else null,
                onRightClick = { onUiEvent(ListsUiEvent.ToggleActionSheet) },
            )
        },
        floatingActionButton = {
            if (!isLoading && contentState?.isSelectionMode != true) {
                AddButton(
                    onClick = { onUiEvent(ListsUiEvent.CreateListClicked) },
                )
            }
        },
    ) { padding ->
        AnimatedLoadingContent(
            isLoading = isLoading,
            label = "lists_loading_content",
            loadingContent = {
                Skeletons.ListDetail(modifier = Modifier.fillMaxSize())
            },
        ) {
            val content = contentState ?: return@AnimatedLoadingContent
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = LifeTogetherTokens.spacing.small),
            ) {
                AnimatedContent(
                    targetState = content.isSelectionMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "selection_mode_bar",
                ) { selectionActive ->
                    if (selectionActive) {
                        SelectionModeBar(
                            selectedCount = content.selectedListIds.size,
                            isAllSelected = content.isAllSelected,
                            onToggleAll = { onUiEvent(ListsUiEvent.ToggleAllListSelection) },
                            onCancel = { onUiEvent(ListsUiEvent.ExitSelectionMode) },
                        )
                    } else {
                        Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.medium))
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = LifeTogetherTokens.spacing.bottomInsetLarge),
                    verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
                ) {
                    if (content.userLists.isEmpty()) {
                        item {
                            TextDefault(text = "No lists yet. Tap + to create one.")
                        }
                    } else {
                        items(content.userLists, key = { it.id }) { list ->
                            val isSelected = content.selectedListIds.contains(list.id)
                            UserListCard(
                                list = list,
                                isSelected = isSelected,
                                onClick = {
                                    if (content.isSelectionMode) {
                                        onUiEvent(ListsUiEvent.ToggleListSelection(list.id))
                                    } else {
                                        onNavigationEvent(ListsNavigationEvent.NavigateToListDetails(list.id))
                                    }
                                },
                                onLongClick = {
                                    if (!content.isSelectionMode) {
                                        onUiEvent(ListsUiEvent.EnterSelectionMode(list.id))
                                    }
                                },
                            )
                        }
                    }
                }
            }

            if (content.showActionSheet) {
                val actions = if (content.isSelectionMode) {
                    listOf(
                        ActionSheetItem(
                            label = "Delete selected",
                            onClick = { onUiEvent(ListsUiEvent.RequestDeleteSelected) },
                            isDestructive = true,
                            isEnabled = content.selectedListIds.isNotEmpty(),
                        ),
                    )
                } else {
                    listOf(
                        ActionSheetItem(
                            label = "Select lists",
                            onClick = { onUiEvent(ListsUiEvent.StartSelectionMode) },
                            isEnabled = content.userLists.isNotEmpty(),
                        ),
                    )
                }
                ActionSheet(
                    onDismiss = { onUiEvent(ListsUiEvent.ToggleActionSheet) },
                    actionsList = actions,
                )
            }

            if (content.showDeleteSelectedDialog) {
                ConfirmationDialog(
                    dialogTitle = "Delete lists?",
                    dialogMessage = "The selected lists and all their entries will be permanently deleted.",
                    confirmButtonMessage = "Delete",
                    dismissButtonMessage = "Cancel",
                    onConfirm = { onUiEvent(ListsUiEvent.ConfirmDeleteSelected) },
                    onDismiss = { onUiEvent(ListsUiEvent.DismissDeleteSelectedDialog) },
                )
            }

            if (content.showCreateDialog) {
                CreateListDialog(
                    name = content.newListName,
                    onNameChange = { onUiEvent(ListsUiEvent.CreateListNameChanged(it)) },
                    type = content.newListType,
                    onTypeChange = { onUiEvent(ListsUiEvent.CreateListTypeChanged(it)) },
                    visibility = content.newListVisibility,
                    onVisibilityChange = { onUiEvent(ListsUiEvent.CreateListVisibilityChanged(it)) },
                    isSaving = content.isSaving,
                    onDismiss = { onUiEvent(ListsUiEvent.CreateDialogDismissed) },
                    onCreate = { onUiEvent(ListsUiEvent.ConfirmCreateListClicked) },
                )
            }
        }
    }
}

@Composable
private fun UserListCard(
    list: UserList,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                shape = MaterialTheme.shapes.large,
            )
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.large)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(LifeTogetherTokens.spacing.medium),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextLabel(
                    text = list.type.displayName,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                TextLabel(
                    text = if (list.visibility == Visibility.FAMILY) "Family" else "Private",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            TextHeadingMedium(
                text = list.itemName,
            )
        }
    }
}


@Composable
private fun CreateListDialog(
    name: String,
    onNameChange: (String) -> Unit,
    type: ListType,
    onTypeChange: (ListType) -> Unit,
    visibility: Visibility,
    onVisibilityChange: (Visibility) -> Unit,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create new list") },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium)) {
                CustomTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = "Name",
                    capitalization = true,
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Text
//                    singleLine = true, //todo add these
//                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences
                )

                Column(verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall)) {
                    Text("Type", style = MaterialTheme.typography.labelLarge)
                    TagOptionRow(
                        options = ListType.entries.map { it.name.lowercase() }, //todo add visual name
                        selectedOption = type.name.lowercase(),
                        onSelectedOptionChange = { new ->
                            ListType.entries.find { it.name.lowercase() == new }?.let { onTypeChange(it) }
                        }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall)) {
                    Text("Visibility", style = MaterialTheme.typography.labelLarge)
                    TagOptionRow(
                        options = Visibility.entries.map { it.name.lowercase() }, //todo add visual name
                        selectedOption = visibility.name.lowercase(),
                        onSelectedOptionChange = { new ->
                            Visibility.entries.find { it.name.lowercase() == new }?.let { onVisibilityChange(it) }
                        }
                    )
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Create",
                onClick = onCreate,
                loading = isSaving,
            )
        },
        dismissButton = {
            SecondaryButton(
                text = "Cancel",
                onClick = onDismiss,
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun UserListCardPreview() {
    LifeTogetherTheme {
        Column(
            modifier = Modifier.padding(LifeTogetherTokens.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
        ) {
            UserListCard(
                list = UserList(
                    id = "1",
                    familyId = "family-1",
                    itemName = "Weekly meals",
                    lastUpdated = Date(),
                    dateCreated = Date(),
                    type = ListType.ROUTINE,
                    visibility = Visibility.FAMILY,
                    ownerUid = "user-1",
                ),
                isSelected = false,
                onClick = {},
                onLongClick = {},
            )
            UserListCard(
                list = UserList(
                    id = "2",
                    familyId = "family-1",
                    itemName = "My Private Habits",
                    lastUpdated = Date(),
                    dateCreated = Date(),
                    type = ListType.ROUTINE,
                    visibility = Visibility.PRIVATE,
                    ownerUid = "user-1",
                ),
                isSelected = true,
                onClick = {},
                onLongClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateListDialogPreview() {
    LifeTogetherTheme {
        CreateListDialog(
            name = "Morning Routines",
            onNameChange = {},
            type = ListType.ROUTINE,
            onTypeChange = {},
            visibility = Visibility.FAMILY,
            onVisibilityChange = {},
            isSaving = false,
            onDismiss = {},
            onCreate = {},
        )
    }
}
