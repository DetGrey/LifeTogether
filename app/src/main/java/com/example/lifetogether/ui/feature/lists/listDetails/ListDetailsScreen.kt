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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.ActionSheet
import com.example.lifetogether.ui.common.ActionSheetItem
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.list.CompletableBox
import com.example.lifetogether.ui.common.sync.SyncUpdatingText
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailsScreen(
    screenState: ListDetailsScreenState,
    onUiEvent: (ListDetailsUiEvent) -> Unit,
    onNavigationEvent: (ListDetailsNavigationEvent) -> Unit,
) {
    val uiState = screenState.uiState
    val entries = screenState.entries
    val imageBitmaps = screenState.imageBitmaps
    val contentState = uiState as? ListDetailsUiState.Content

    Scaffold(
        topBar = {
            TopBar(
                leftIcon = Icon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = { onNavigationEvent(ListDetailsNavigationEvent.NavigateBack) },
                text = contentState?.listName?.ifBlank { "List" } ?: "List",
                rightIcon = Icon(
                    resId = R.drawable.ic_overflow_menu,
                    description = "overflow menu",
                ),
                onRightClick = { onUiEvent(ListDetailsUiEvent.ToggleActionSheet) },
            )
        },
        floatingActionButton = {
            if (contentState?.isSelectionModeActive != true) {
                AddButton(onClick = {
                    onNavigationEvent(ListDetailsNavigationEvent.NavigateToCreateEntry)
                })
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
                        .padding(horizontal = LifeTogetherTokens.spacing.small),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SyncUpdatingText(keys = setOf(SyncKey.ROUTINE_LIST_ENTRIES))

                    if (uiState.isSelectionModeActive) {
                        SelectionModeBar(
                            selectedCount = uiState.selectedEntryIds.size,
                            isAllSelected = uiState.isAllEntriesSelected,
                            onToggleAll = { onUiEvent(ListDetailsUiEvent.ToggleAllEntrySelection) },
                            onCancel = { onUiEvent(ListDetailsUiEvent.ExitSelectionMode) },
                        )
                    } else {
                        Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.medium))
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
                                .padding(bottom = LifeTogetherTokens.spacing.small),
                            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
                        ) {
                            items(entries) { entry ->
                                ListEntryCard(
                                    entry = entry,
                                    bitmap = entry.id?.let { imageBitmaps[it] },
                                    isSelectionMode = uiState.isSelectionModeActive,
                                    isSelected = uiState.selectedEntryIds.contains(entry.id),
                                    onClick = {
                                        if (uiState.isSelectionModeActive) {
                                            entry.id?.let { onUiEvent(ListDetailsUiEvent.ToggleEntrySelection(it)) }
                                        } else {
                                            entry.id?.let {
                                                onNavigationEvent(ListDetailsNavigationEvent.NavigateToEntryDetails(it))
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        if (!uiState.isSelectionModeActive) {
                                            entry.id?.let { onUiEvent(ListDetailsUiEvent.EnterSelectionMode(it)) }
                                        }
                                    },
                                    onComplete = { onUiEvent(ListDetailsUiEvent.CompleteEntry(entry)) },
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
                        isEnabled = entries.any { it.id != null },
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
            .padding(top = LifeTogetherTokens.spacing.small, bottom = LifeTogetherTokens.spacing.medium),
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
                shape = MaterialTheme.shapes.large,
            )
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.large,
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(LifeTogetherTokens.spacing.medium),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
            ) {
                CompletableBox(
                    isCompleted = false,
                    onCompleteToggle = onComplete,
                    isEnabled = !isSelectionMode,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    tint = MaterialTheme.colorScheme.primaryContainer,
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
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )

                    entry.nextDate?.let { nextDate ->
                        TextDefault(
                            text = "Next: ${dateFormat.format(nextDate)}",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(LifeTogetherTokens.sizing.avatarMedium)
                        .background(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = MaterialTheme.shapes.medium,
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
fun ListEntryScreenLoadingPreview() {
    LifeTogetherTheme {
        ListDetailsScreen(
            screenState = ListDetailsScreenState(),
            onUiEvent = {},
            onNavigationEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ListEntryScreenPreview() {
    LifeTogetherTheme {
        ListDetailsScreen(
            screenState = ListDetailsScreenState(
                uiState = ListDetailsUiState.Content("Name"),
                entries = listOf(RoutineListEntry(
                    itemName = "Water avocado plants"
                ))
            ),
            onUiEvent = {},
            onNavigationEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ListEntryCardDailyPreview() {
    LifeTogetherTheme {
        Column(
            modifier = Modifier.padding(LifeTogetherTokens.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
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
