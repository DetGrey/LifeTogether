package com.example.lifetogether.ui.feature.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.model.lists.ListType
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.button.SecondaryButton
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.sync.SyncUpdatingText
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun ListsScreen(
    uiState: ListsUiState,
    onUiEvent: (ListsUiEvent) -> Unit,
    onNavigationEvent: (ListsNavigationEvent) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(LifeTogetherTokens.spacing.small)
                .padding(bottom = LifeTogetherTokens.spacing.bottomInsetLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                ) {
                    TopBar(
                        leftIcon = Icon(
                            resId = R.drawable.ic_back_arrow,
                            description = "back arrow icon",
                        ),
                        onLeftClick = { onNavigationEvent(ListsNavigationEvent.NavigateBack) },
                        text = "Lists",
                    )

                    SyncUpdatingText(
                        keys = setOf(SyncKey.USER_LISTS, SyncKey.ROUTINE_LIST_ENTRIES),
                    )
                }
            }

            if (uiState.userLists.isEmpty()) {
                item {
                    TextDefault(text = "No lists yet. Tap + to create one.")
                }
            } else {
                items(uiState.userLists) { list ->
                    UserListCard(
                        list = list,
                        onClick = {
                            list.id?.let {
                                onNavigationEvent(ListsNavigationEvent.NavigateToListDetails(it))
                            }
                        },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = LifeTogetherTokens.spacing.xLarge, end = LifeTogetherTokens.spacing.xLarge),
            contentAlignment = Alignment.BottomEnd,
        ) {
            AddButton(onClick = { onUiEvent(ListsUiEvent.CreateListClicked) })
        }
    }

    if (uiState.showCreateDialog) {
        CreateListDialog(
            name = uiState.newListName,
            onNameChange = { onUiEvent(ListsUiEvent.CreateListNameChanged(it)) },
            type = uiState.newListType,
            onTypeChange = { onUiEvent(ListsUiEvent.CreateListTypeChanged(it)) },
            visibility = uiState.newListVisibility,
            onVisibilityChange = { onUiEvent(ListsUiEvent.CreateListVisibilityChanged(it)) },
            isSaving = uiState.isSaving,
            onDismiss = { onUiEvent(ListsUiEvent.CreateDialogDismissed) },
            onCreate = { onUiEvent(ListsUiEvent.ConfirmCreateListClicked) },
        )
    }
}

@Composable
private fun UserListCard(
    list: UserList,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.large)
            .clickable { onClick() }
            .padding(LifeTogetherTokens.spacing.medium),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = list.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = if (list.visibility == Visibility.FAMILY) "Family" else "Private",
                    style = MaterialTheme.typography.labelMedium,
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
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Text
//                    singleLine = true, //todo add these
//                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences
                )

                Column(verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall)) {
                    Text("Type", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small)) {
                        ListType.entries.forEach { listType -> //todo use TagOption instead
                            val selected = type == listType
                            if (selected) {
                                PrimaryButton(
                                    text = listType.name.lowercase().replaceFirstChar { it.uppercase() },
                                    onClick = {},
                                )
                            } else {
                                SecondaryButton(
                                    text = listType.name.lowercase().replaceFirstChar { it.uppercase() },
                                    onClick = { onTypeChange(listType) },
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall)) {
                    Text("Visibility", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small)) {
                        Visibility.entries.forEach { vis ->
                            val selected = visibility == vis
                            if (selected) {
                                PrimaryButton(
                                    text = vis.name.lowercase().replaceFirstChar { it.uppercase() },
                                    onClick = {},
                                )
                            } else {
                                SecondaryButton(
                                    text = vis.name.lowercase().replaceFirstChar { it.uppercase() },
                                    onClick = { onVisibilityChange(vis) },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isSaving) {
                CircularProgressIndicator()
            } else {
                PrimaryButton(
                    text = "Create",
                    onClick = onCreate,
                )
            }
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
fun UserListCardPreview() {
    LifeTogetherTheme {
        Column(
            modifier = Modifier.padding(LifeTogetherTokens.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
        ) {
            UserListCard(
                list = UserList(
                    id = "1",
                    itemName = "Morning Routines",
                    type = ListType.ROUTINE,
                    visibility = Visibility.FAMILY,
                ),
                onClick = {},
            )
            UserListCard(
                list = UserList(
                    id = "2",
                    itemName = "My Private Habits",
                    type = ListType.ROUTINE,
                    visibility = Visibility.PRIVATE,
                ),
                onClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateListDialogPreview() {
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
