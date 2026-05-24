package com.example.lifetogether.ui.feature.tipTracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.AppIcon
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.ui.feature.tipTracker.components.AddNewTipItem
import com.example.lifetogether.ui.feature.tipTracker.components.TipCard
import com.example.lifetogether.ui.feature.tipTracker.components.TipsCalendar
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import java.util.Date

@Composable
fun TipTrackerScreen(
    uiState: TipTrackerUiState,
    onUiEvent: (TipTrackerUiEvent) -> Unit,
    onNavigationEvent: (TipTrackerNavigationEvent) -> Unit,
) {
    var showDeleteTipDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                leftAppIcon = AppIcon(
                    resId = R.drawable.ic_back,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    onNavigationEvent(TipTrackerNavigationEvent.NavigateBack)
                },
                text = "Tip Tracker",
                rightAppIcon = AppIcon(
                    resId = R.drawable.ic_statistics,
                    description = "statistics icon",
                ),
                onRightClick = {
                    onNavigationEvent(TipTrackerNavigationEvent.NavigateToStatistics)
                },
            )
        },
    ) { padding ->
        AnimatedLoadingContent(
            isLoading = uiState is TipTrackerUiState.Loading,
            label = "tip_tracker_loading",
            loadingContent = {
                Skeletons.GridCollection(
                    modifier = Modifier.fillMaxSize(),
                )
            },
        ) {
            val content = uiState as? TipTrackerUiState.Content ?: return@AnimatedLoadingContent

            if (content.tips.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(LifeTogetherTokens.spacing.small),
                    contentPadding = PaddingValues(bottom = LifeTogetherTokens.spacing.bottomInsetMedium),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                ) {
                    item {
                        TagOptionRow(
                            options = listOf("Calendar", "List"),
                            selectedOption = content.overviewOption,
                            onSelectedOptionChange = {
                                onUiEvent(TipTrackerUiEvent.OverviewOptionSelected(it))
                            },
                            center = true,
                        )
                    }

                    when (content.overviewOption) {
                        "Calendar" -> {
                            item {
                                TipsCalendar(
                                    calendar = content.calendar,
                                    onPreviousMonthClick = {
                                        onUiEvent(TipTrackerUiEvent.PreviousMonthClicked)
                                    },
                                    onCurrentMonthClick = {
                                        onUiEvent(TipTrackerUiEvent.CurrentMonthClicked)
                                    },
                                    onNextMonthClick = {
                                        onUiEvent(TipTrackerUiEvent.NextMonthClicked)
                                    },
                                )
                            }
                        }

                        "List" -> {
                            items(content.tips, key = { it.id }) { tip ->
                                TipCard(
                                    tip = tip,
                                    onDeleteClick = {
                                        onUiEvent(TipTrackerUiEvent.DeleteTipClicked(tip))
                                        showDeleteTipDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(LifeTogetherTokens.spacing.small)
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                AddNewTipItem(
                    textValue = content.newItemAmount,
                    onTextChange = { onUiEvent(TipTrackerUiEvent.NewItemAmountChanged(it)) },
                    onAddClick = {
                        onUiEvent(TipTrackerUiEvent.AddItemClicked)
                    },
                    dateValue = content.newItemDate,
                    onDateChange = {
                        onUiEvent(TipTrackerUiEvent.NewItemDateChanged(it))
                    },
                )
            }

            if (showDeleteTipDialog) {
                ConfirmationDialog(
                    onDismiss = { showDeleteTipDialog = false },
                    onConfirm = {
                        showDeleteTipDialog = false
                        onUiEvent(TipTrackerUiEvent.ConfirmDeleteConfirmation)
                    },
                    dialogTitle = "Delete tip",
                    dialogMessage = "Are you sure you want to delete this tip?",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Delete",
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TipTrackerScreenPreview() {
    LifeTogetherTheme {
        TipTrackerScreen(
            uiState = TipTrackerUiState.Content(
                tips = listOf(
                    TipItem(
                        id = "tip-1",
                        familyId = "family-1",
                        itemName = "Tip",
                        amount = 120f,
                        date = Date(),
                    ),
                ),
                stats = TipTrackerStats(),
                calendar = TipTrackerCalendarState(
                    monthLabel = "April",
                    days = listOf(TipTrackerCalendarDay("1 Fri", "100"), TipTrackerCalendarDay("2 Sat")),
                ),
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TipTrackerScreenListPreview() {
    LifeTogetherTheme {
        TipTrackerScreen(
            uiState = TipTrackerUiState.Content(
                tips = listOf(
                    TipItem(
                        id = "tip-1",
                        familyId = "family-1",
                        itemName = "Tip",
                        amount = 120f,
                        date = Date(),
                    ),
                ),
                stats = TipTrackerStats(),
                overviewOption = "List",
                calendar = TipTrackerCalendarState(),
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
