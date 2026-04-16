package com.example.lifetogether.ui.feature.lists.listDetails

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.observer.ObserverKey
import com.example.lifetogether.ui.common.ActionSheet
import com.example.lifetogether.ui.common.ActionSheetItem
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.list.CompletableBox
import com.example.lifetogether.ui.common.observer.ObserverUpdatingText
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailsScreen(
    listId: String,
    appNavigator: AppNavigator? = null,
) {
    val vm: ListDetailsViewModel = hiltViewModel()
    val screenState by vm.screenState.collectAsState()
    val uiState = screenState.uiState
    val entries = screenState.entries
    val imageBitmaps = screenState.imageBitmaps
    val contentState = uiState as? ListDetailsUiState.Content

    LaunchedEffect(listId) {
        if (listId.isNotBlank()) {
            vm.setUp(listId)
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                leftIcon = Icon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = { appNavigator?.navigateBack() },
                text = contentState?.listName?.ifBlank { "List" } ?: "List",
                rightIcon = Icon(
                    resId = R.drawable.ic_overflow_menu,
                    description = "overflow menu",
                ),
                onRightClick = { vm.toggleActionSheet(true) },
            )
        },
        floatingActionButton = {
            if (contentState?.isSelectionModeActive != true) {
                AddButton(onClick = { appNavigator?.navigateToListEntryDetails(listId) })
            }
        },
    ) { padding ->
        when (uiState) {
            is ListDetailsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is ListDetailsUiState.Content -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ObserverUpdatingText(
                        keys = setOf(ObserverKey.ROUTINE_LIST_ENTRIES),
                    )

                    if (uiState.isSelectionModeActive) {
                        SelectionModeBar(
                            selectedCount = uiState.selectedEntryIds.size,
                            isAllSelected = uiState.isAllEntriesSelected,
                            onToggleAll = { vm.toggleAllEntrySelection() },
                            onCancel = { vm.exitSelectionMode() },
                        )
                    } else {
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    if (entries.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "No entries yet. Tap + to add one.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(entries) { entry ->
                                ListEntryCard(
                                    entry = entry,
                                    bitmap = entry.id?.let { imageBitmaps[it] },
                                    isSelectionMode = uiState.isSelectionModeActive,
                                    isSelected = uiState.selectedEntryIds.contains(entry.id),
                                    onClick = {
                                        if (uiState.isSelectionModeActive) {
                                            entry.id?.let(vm::toggleEntrySelection)
                                        } else {
                                            entry.id?.let { appNavigator?.navigateToListEntryDetails(listId, it) }
                                        }
                                    },
                                    onLongClick = {
                                        if (!uiState.isSelectionModeActive) {
                                            entry.id?.let(vm::enterSelectionMode)
                                        }
                                    },
                                    onComplete = { vm.completeEntry(entry) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (contentState?.showActionSheet == true) {
        val actions = when (contentState.isSelectionModeActive) {
            true -> {
                listOf(
                    ActionSheetItem(
                        label = "Delete selected",
                        onClick = { vm.requestDeleteSelected() },
                        isDestructive = true,
                        isEnabled = contentState.selectedEntryIds.isNotEmpty(),
                    ),
                )
            }

            false -> {
                listOf(
                    ActionSheetItem(
                        label = "Select entries",
                        onClick = { vm.startSelectionMode() },
                        isEnabled = entries.any { it.id != null },
                    ),
                )
            }
        }

        ActionSheet(
            onDismiss = { vm.toggleActionSheet(false) },
            actionsList = actions,
        )
    }

    if (contentState?.showDeleteSelectedDialog == true) {
        ConfirmationDialog(
            onDismiss = { vm.dismissDeleteSelectedDialog() },
            onConfirm = { vm.confirmDeleteSelected() },
            dialogTitle = "Delete selected entries",
            dialogMessage = "Are you sure you want to delete the selected entries?",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Delete selected",
        )
    }

    if (contentState?.showAlertDialog == true) {
        LaunchedEffect(contentState.error) {
            vm.dismissAlert()
        }
        ErrorAlertDialog(contentState.error)
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
            .padding(top = 10.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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

@Composable
private fun ListEntryCard(
    entry: RoutineListEntry,
    bitmap: Bitmap?,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onComplete: () -> Unit,
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                shape = RoundedCornerShape(20.dp),
            )
            .background(
                color = MaterialTheme.colorScheme.onBackground,
                shape = RoundedCornerShape(20.dp),
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(14.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CompletableBox(
                    isCompleted = false,
                    onCompleteToggle = onComplete,
                    isEnabled = !isSelectionMode,
                )
                TextHeadingMedium(
                    text = entry.itemName,
                    maxLines = 1,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    TextDefault(
                        text = "Every ${entry.interval} ${entry.recurrenceUnit.value}",
                        color = Color.White,
                    )

                    entry.nextDate?.let { nextDate ->
                        TextDefault(
                            text = "Next: ${dateFormat.format(nextDate)}",
                            color = Color.White,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = RoundedCornerShape(16.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "entry image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListEntryCardDailyPreview() {
    LifeTogetherTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ListEntryCard(
                entry = RoutineListEntry(
                    itemName = "Water the plants",
                    recurrenceUnit = RecurrenceUnit.DAYS,
                    interval = 3,
                    nextDate = Date(),
                    completionCount = 7,
                ),
                bitmap = null,
                isSelectionMode = false,
                isSelected = false,
                onClick = {},
                onLongClick = {},
                onComplete = {},
            )
            ListEntryCard(
                entry = RoutineListEntry(
                    itemName = "Change bedsheets very long",
                    recurrenceUnit = RecurrenceUnit.WEEKS,
                    interval = 2,
                    weekdays = listOf(1, 4),
                    nextDate = null,
                    completionCount = 0,
                ),
                bitmap = null,
                isSelectionMode = true,
                isSelected = true,
                onClick = {},
                onLongClick = {},
                onComplete = {},
            )
        }
    }
}
