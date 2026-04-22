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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.model.lists.ListType
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.sync.SyncUpdatingText
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun ListsScreen(
    appNavigator: AppNavigator? = null,
) {
    val viewModel: ListsViewModel = hiltViewModel()
    val userLists by viewModel.userLists.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TopBar(
                        leftIcon = Icon(
                            resId = R.drawable.ic_back_arrow,
                            description = "back arrow icon",
                        ),
                        onLeftClick = { appNavigator?.navigateBack() },
                        text = "Lists",
                    )

                    SyncUpdatingText(
                        keys = setOf(SyncKey.USER_LISTS, SyncKey.ROUTINE_LIST_ENTRIES),
                    )
                }
            }

            if (userLists.isEmpty()) {
                item {
                    Text(text = "No lists yet. Tap + to create one.")
                }
            } else {
                items(userLists) { list ->
                    UserListCard(
                        list = list,
                        onClick = { list.id?.let { appNavigator?.navigateToListDetail(it) } },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 30.dp, end = 30.dp),
            contentAlignment = Alignment.BottomEnd,
        ) {
            AddButton(onClick = { viewModel.openCreateDialog() })
        }
    }

    if (viewModel.showAlertDialog) {
        LaunchedEffect(viewModel.error) {
            viewModel.dismissAlert()
        }
        ErrorAlertDialog(viewModel.error)
    }

    if (viewModel.showCreateDialog) {
        CreateListDialog(
            name = viewModel.newListName,
            onNameChange = { viewModel.newListName = it },
            type = viewModel.newListType,
            onTypeChange = { viewModel.newListType = it },
            visibility = viewModel.newListVisibility,
            onVisibilityChange = { viewModel.newListVisibility = it },
            isSaving = viewModel.isSaving,
            onDismiss = { viewModel.showCreateDialog = false },
            onCreate = {
                viewModel.createList { newId ->
                    appNavigator?.navigateToListDetail(newId)
                }
            },
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
            .background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = list.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.background,
                )
                Text(
                    text = if (list.visibility == Visibility.FAMILY) "Family" else "Private",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.background,
                )
            }
            Text(
                text = list.itemName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.background,
                fontWeight = FontWeight.Bold,
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
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Type", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ListType.entries.forEach { listType ->
                            val selected = type == listType
                            if (selected) {
                                Button(onClick = {}) {
                                    Text(listType.name.lowercase().replaceFirstChar { it.uppercase() })
                                }
                            } else {
                                OutlinedButton(onClick = { onTypeChange(listType) }) {
                                    Text(listType.name.lowercase().replaceFirstChar { it.uppercase() })
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Visibility", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Visibility.entries.forEach { vis ->
                            val selected = visibility == vis
                            if (selected) {
                                Button(onClick = {}) {
                                    Text(vis.name.lowercase().replaceFirstChar { it.uppercase() })
                                }
                            } else {
                                OutlinedButton(onClick = { onVisibilityChange(vis) }) {
                                    Text(vis.name.lowercase().replaceFirstChar { it.uppercase() })
                                }
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
                Button(onClick = onCreate) { Text("Create") }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Preview(showBackground = true)
@Composable
fun UserListCardPreview() {
    LifeTogetherTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
