package com.example.lifetogether.ui.feature.lists.listDetails

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.lists.ChecklistEntry
import com.example.lifetogether.domain.model.lists.ListEntry
import com.example.lifetogether.domain.model.lists.ListType
import com.example.lifetogether.domain.model.lists.MealPlanEntry
import com.example.lifetogether.domain.model.lists.NoteEntry
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.WishListEntry
import com.example.lifetogether.ui.common.ActionSheet
import com.example.lifetogether.ui.common.ActionSheetItem
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.button.SecondaryButton
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.list.CompletableBox
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import kotlinx.coroutines.launch
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailsScreen(
    uiState: ListDetailsUiState,
    onUiEvent: (ListDetailsUiEvent) -> Unit,
    onNavigationEvent: (ListDetailsNavigationEvent) -> Unit,
) {
    val contentState = uiState as? ListDetailsUiState.Content
    val listType = contentState?.listType ?: ListType.ROUTINE
    val entries = contentState?.entries.orEmpty()
    val imageBitmaps = contentState?.imageBitmaps.orEmpty()
    var completedExpanded by rememberSaveable(listType) { mutableStateOf(false) }

    LaunchedEffect(listType) {
        completedExpanded = false
    }

    Scaffold(
        topBar = {
            AppTopBar(
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
            if (contentState?.isSelectionModeActive != true && contentState != null) {
                AddButton(onClick = {
                    onNavigationEvent(ListDetailsNavigationEvent.NavigateToCreateEntry)
                })
            }
        },
    ) { padding ->
        when (uiState) {
            is ListDetailsUiState.Loading -> {
                Skeletons.ListDetail(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }

            is ListDetailsUiState.Content -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = LifeTogetherTokens.spacing.small),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AnimatedContent(
                        targetState = uiState.isSelectionModeActive,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "selection_mode_bar",
                    ) { selectionActive ->
                        if (selectionActive) {
                            SelectionModeBar(
                                selectedCount = uiState.selectedEntryIds.size,
                                isAllSelected = uiState.isAllEntriesSelected,
                                onToggleAll = { onUiEvent(ListDetailsUiEvent.ToggleAllEntrySelection) },
                                onCancel = { onUiEvent(ListDetailsUiEvent.ExitSelectionMode) },
                            )
                        } else {
                            Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.medium))
                        }
                    }

                    if (entries.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "No entries yet. Tap + to add one.")
                        }
                    } else {
                        ListDetailsEntriesSection(
                            listType = listType,
                            entries = entries,
                            imageBitmaps = imageBitmaps,
                            isSelectionMode = uiState.isSelectionModeActive,
                            selectedIds = uiState.selectedEntryIds,
                            completedExpanded = completedExpanded,
                            onToggleCompletedExpanded = { completedExpanded = !completedExpanded },
                            onEntryClick = { entry ->
                                if (uiState.isSelectionModeActive) {
                                    onUiEvent(ListDetailsUiEvent.ToggleEntrySelection(entry.id))
                                } else {
                                    onNavigationEvent(ListDetailsNavigationEvent.NavigateToEntryDetails(entry.id))
                                }
                            },
                            onEntryLongClick = { entry ->
                                if (!uiState.isSelectionModeActive) {
                                    onUiEvent(ListDetailsUiEvent.EnterSelectionMode(entry.id))
                                }
                            },
                            onEntryToggleComplete = { entry ->
                                onUiEvent(ListDetailsUiEvent.ToggleEntryCompleted(entry.id))
                            },
                        )
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
                        isEnabled = entries.isNotEmpty(),
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
private fun CompletedSectionHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggle, onLongClick = onToggle),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextDefault(text = "Completed ($count)")
            Icon(
                painter = painterResource(id = if (expanded) R.drawable.ic_expanded else R.drawable.ic_expand),
                contentDescription = if (expanded) "collapse completed" else "expand completed",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = LifeTogetherTokens.spacing.xSmall),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun splitByCompletion(entries: List<ListEntry>): Pair<List<ListEntry>, List<ListEntry>> {
    val active = mutableListOf<ListEntry>()
    val completed = mutableListOf<ListEntry>()
    entries.forEach { entry ->
        val isCompleted = when (entry) {
            is ChecklistEntry -> entry.isChecked
            is WishListEntry -> entry.isPurchased
            else -> false
        }
        if (isCompleted) completed += entry else active += entry
    }
    return active to completed
}

@Composable
private fun ListDetailsEntriesSection(
    listType: ListType,
    entries: List<ListEntry>,
    imageBitmaps: Map<String, Bitmap>,
    isSelectionMode: Boolean,
    selectedIds: Set<String>,
    completedExpanded: Boolean,
    onToggleCompletedExpanded: () -> Unit,
    onEntryClick: (ListEntry) -> Unit,
    onEntryLongClick: (ListEntry) -> Unit,
    onEntryToggleComplete: (ListEntry) -> Unit,
) {
    when (listType) {
        ListType.MEAL_PLANNER -> MealPlannerListSection(
            entries = entries.filterIsInstance<MealPlanEntry>(),
            isSelectionMode = isSelectionMode,
            selectedIds = selectedIds,
            imageBitmaps = imageBitmaps,
            onEntryClick = { onEntryClick(it) },
            onEntryLongClick = { onEntryLongClick(it) },
            onEntryToggleComplete = { onEntryToggleComplete(it) },
        )

        ListType.WISH_LIST,
        ListType.CHECKLIST -> CompletionGroupedListSection(
            entries = entries,
            completedExpanded = completedExpanded,
            onToggleCompletedExpanded = onToggleCompletedExpanded,
            imageBitmaps = imageBitmaps,
            isSelectionMode = isSelectionMode,
            selectedIds = selectedIds,
            onEntryClick = onEntryClick,
            onEntryLongClick = onEntryLongClick,
            onEntryToggleComplete = onEntryToggleComplete,
        )

        else -> FlatListSection(
            entries = entries,
            imageBitmaps = imageBitmaps,
            isSelectionMode = isSelectionMode,
            selectedIds = selectedIds,
            onEntryClick = onEntryClick,
            onEntryLongClick = onEntryLongClick,
            onEntryToggleComplete = onEntryToggleComplete,
        )
    }
}

@Composable
private fun FlatListSection(
    entries: List<ListEntry>,
    imageBitmaps: Map<String, Bitmap>,
    isSelectionMode: Boolean,
    selectedIds: Set<String>,
    onEntryClick: (ListEntry) -> Unit,
    onEntryLongClick: (ListEntry) -> Unit,
    onEntryToggleComplete: (ListEntry) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = LifeTogetherTokens.spacing.small),
        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
    ) {
        items(entries) { entry ->
            ListEntryCard(
                entry = entry,
                bitmap = (entry as? RoutineListEntry)?.id?.let { imageBitmaps[it] },
                isSelectionMode = isSelectionMode,
                isSelected = selectedIds.contains(entry.id),
                onClick = { onEntryClick(entry) },
                onLongClick = { onEntryLongClick(entry) },
                onComplete = { onEntryToggleComplete(entry) },
            )
        }
    }
}

@Composable
private fun CompletionGroupedListSection(
    entries: List<ListEntry>,
    completedExpanded: Boolean,
    onToggleCompletedExpanded: () -> Unit,
    imageBitmaps: Map<String, Bitmap>,
    isSelectionMode: Boolean,
    selectedIds: Set<String>,
    onEntryClick: (ListEntry) -> Unit,
    onEntryLongClick: (ListEntry) -> Unit,
    onEntryToggleComplete: (ListEntry) -> Unit,
) {
    val (activeEntries, completedEntries) = splitByCompletion(entries)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = LifeTogetherTokens.spacing.small),
        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
    ) {
        items(activeEntries) { entry ->
            ListEntryCard(
                entry = entry,
                bitmap = (entry as? RoutineListEntry)?.id?.let { imageBitmaps[it] },
                isSelectionMode = isSelectionMode,
                isSelected = selectedIds.contains(entry.id),
                onClick = { onEntryClick(entry) },
                onLongClick = { onEntryLongClick(entry) },
                onComplete = { onEntryToggleComplete(entry) },
            )
        }

        if (completedEntries.isNotEmpty()) {
            item {
                CompletedSectionHeader(
                    count = completedEntries.size,
                    expanded = completedExpanded,
                    onToggle = onToggleCompletedExpanded,
                )
            }
            item {
                AnimatedVisibility(
                    visible = completedExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
                    ) {
                        completedEntries.forEach { entry ->
                            ListEntryCard(
                                entry = entry,
                                bitmap = (entry as? RoutineListEntry)?.id?.let { imageBitmaps[it] },
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedIds.contains(entry.id),
                                onClick = { onEntryClick(entry) },
                                onLongClick = { onEntryLongClick(entry) },
                                onComplete = { onEntryToggleComplete(entry) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealPlannerListSection(
    entries: List<MealPlanEntry>,
    isSelectionMode: Boolean,
    selectedIds: Set<String>,
    imageBitmaps: Map<String, Bitmap>,
    onEntryClick: (MealPlanEntry) -> Unit,
    onEntryLongClick: (MealPlanEntry) -> Unit,
    onEntryToggleComplete: (MealPlanEntry) -> Unit,
) {
    MealPlannerWeekPager(
        entries = entries,
        isSelectionMode = isSelectionMode,
        selectedIds = selectedIds,
        imageBitmaps = imageBitmaps,
        onEntryClick = onEntryClick,
        onEntryLongClick = onEntryLongClick,
        onEntryToggleComplete = onEntryToggleComplete,
    )
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
    entry: ListEntry,
    bitmap: Bitmap?,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onComplete: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val isCompletable = entry is RoutineListEntry || entry is ChecklistEntry || entry is WishListEntry
    val isCompleted = when (entry) {
        is ChecklistEntry -> entry.isChecked
        is WishListEntry -> entry.isPurchased
        else -> false
    }
    val subtitle = when (entry) {
        is RoutineListEntry -> {
            val next = " | Next: ${dateFormat.format(entry.nextDate)}"
            "Every ${entry.interval} ${entry.recurrenceUnit.value}$next"
        }

        is WishListEntry -> {
            val price = entry.estimatedPriceMinor?.let { "$it ${entry.currencyCode.orEmpty()}" } ?: "No price"
            val url = entry.url?.takeIf { it.isNotBlank() }?.let { " | $it" }.orEmpty()
            "$price$url"
        }

        is NoteEntry -> entry.markdownBody.take(60)
        is ChecklistEntry -> if (entry.isChecked) "Completed" else "Pending"
        is MealPlanEntry -> "${entry.date} | ${entry.customMealName ?: entry.recipeId.orEmpty()}"
        else -> ""
    }
    val wishNotesSnippet = (entry as? WishListEntry)?.notes?.takeIf { it.isNotBlank() }?.take(80)
    val wishUrl = (entry as? WishListEntry)?.url?.takeIf { it.isNotBlank() }

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
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
            ) {
                CompletableBox(
                    isCompleted = isCompleted,
                    onCompleteToggle = onComplete,
                    isEnabled = !isSelectionMode && isCompletable,
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
                Column(modifier = Modifier.padding(top = LifeTogetherTokens.spacing.xSmall)) {
                    if (subtitle.isNotBlank()) {
                        TextDefault(
                            text = subtitle,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }

                    if (!wishUrl.isNullOrBlank()) {
                        TextDefault(
                            text = "Open link",
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    val candidate = if (wishUrl.startsWith("http://") || wishUrl.startsWith("https://")) {
                                        wishUrl
                                    } else {
                                        "https://$wishUrl"
                                    }
                                    runCatching { uriHandler.openUri(candidate) }
                                },
                                onLongClick = {},
                            ),
                        )
                    }

                    if (!wishNotesSnippet.isNullOrBlank()) {
                        TextDefault(
                            text = wishNotesSnippet,
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
                    if (entry is RoutineListEntry && bitmap != null) {
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

@Composable
private fun MealPlannerWeekPager(
    entries: List<MealPlanEntry>,
    isSelectionMode: Boolean,
    selectedIds: Set<String>,
    imageBitmaps: Map<String, Bitmap>,
    onEntryClick: (MealPlanEntry) -> Unit,
    onEntryLongClick: (MealPlanEntry) -> Unit,
    onEntryToggleComplete: (MealPlanEntry) -> Unit,
) {
    val parsedEntries = remember(entries) {
        entries.mapNotNull { entry ->
            parseMealDate(entry.date)?.let { it to entry }
        }
    }
    val entryByDate = remember(parsedEntries) { parsedEntries.toMap() }
    val today = remember { LocalDate.now() }
    val currentWeekStart = remember(today) { startOfWeek(today) }
    val weekOffsets = remember(parsedEntries, currentWeekStart) {
        parsedEntries.map { pair -> java.time.temporal.ChronoUnit.WEEKS.between(currentWeekStart, startOfWeek(pair.first)).toInt() }
    }
    val minOffset = remember(weekOffsets) { minOf(weekOffsets.minOrNull() ?: 0, -8) }
    val maxOffset = remember(weekOffsets) { maxOf(weekOffsets.maxOrNull() ?: 0, 8) }
    val pageCount = (maxOffset - minOffset + 1).coerceAtLeast(1)
    val currentWeekPage = -minOffset
    val pagerState = rememberPagerState(initialPage = currentWeekPage) { pageCount }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextDefault(text = "Week view")
            SecondaryButton(
                text = "Current week",
                onClick = {
                    coroutineScope.launch { pagerState.animateScrollToPage(currentWeekPage) }
                },
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val weekStart = currentWeekStart.plusWeeks((page - currentWeekPage).toLong())
            val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
            ) {
                items(weekDays) { day ->
                    val dayEntry = entryByDate[day]
                    val label = "${day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${day.dayOfMonth}"
                    if (dayEntry == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.large,
                                )
                                .padding(LifeTogetherTokens.spacing.medium),
                        ) {
                            TextDefault(text = "$label - No meal planned")
                        }
                    } else {
                        ListEntryCard(
                            entry = dayEntry,
                            bitmap = imageBitmaps[dayEntry.id],
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedIds.contains(dayEntry.id),
                            onClick = { onEntryClick(dayEntry) },
                            onLongClick = { onEntryLongClick(dayEntry) },
                            onComplete = { onEntryToggleComplete(dayEntry) },
                        )
                    }
                }
            }
        }
    }
}

private fun parseMealDate(value: String): LocalDate? {
    return try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun startOfWeek(date: LocalDate): LocalDate {
    var d = date
    while (d.dayOfWeek != DayOfWeek.MONDAY) {
        d = d.minusDays(1)
    }
    return d
}

@Preview(showBackground = true)
@Composable
fun ListEntryScreenLoadingPreview() {
    LifeTogetherTheme {
        ListDetailsScreen(
            uiState = ListDetailsUiState.Loading,
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
            uiState = ListDetailsUiState.Content(
                listName = "Name",
                listType = ListType.ROUTINE,
                entries = listOf(
                    RoutineListEntry(
                        id = "routine-1",
                        familyId = "family-1",
                        listId = "list-1",
                        itemName = "Water avocado plants",
                        lastUpdated = Date(),
                        dateCreated = Date(),
                        nextDate = Date(),
                    ),
                ),
                imageBitmaps = emptyMap(),
                isSelectionModeActive = false,
                selectedEntryIds = emptySet(),
                isAllEntriesSelected = false,
                showActionSheet = false,
                showDeleteSelectedDialog = false,
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
                    id = "132",
                    familyId = "family-1",
                    listId = "list-1",
                    itemName = "Water the plants",
                    lastUpdated = Date(),
                    dateCreated = Date(),
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
                    id = "459382",
                    familyId = "family-1",
                    listId = "list-1",
                    itemName = "Change bedsheets very long",
                    lastUpdated = Date(),
                    dateCreated = Date(),
                    recurrenceUnit = RecurrenceUnit.WEEKS,
                    interval = 2,
                    weekdays = listOf(1, 4),
                    nextDate = Date(),
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
