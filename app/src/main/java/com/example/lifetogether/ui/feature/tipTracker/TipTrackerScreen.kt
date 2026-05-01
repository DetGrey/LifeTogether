package com.example.lifetogether.ui.feature.tipTracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.ui.feature.tipTracker.components.AddNewTipItem
import com.example.lifetogether.ui.feature.tipTracker.components.TipsCalendar
import com.example.lifetogether.ui.feature.tipTracker.components.TipsList
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import java.util.Date

@Composable
fun TipTrackerScreen(
    uiState: TipTrackerUiState,
    onUiEvent: (TipTrackerUiEvent) -> Unit,
    onNavigationEvent: (TipTrackerNavigationEvent) -> Unit,
) {
    val content = uiState as TipTrackerUiState.Content
    Scaffold(
        topBar = {
            TopBar(
                leftIcon = Icon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    onNavigationEvent(TipTrackerNavigationEvent.NavigateBack)
                },
                text = "Tip Tracker",
                rightIcon = Icon(
                    resId = R.drawable.ic_statistics,
                    description = "back arrow icon",
                ),
                onRightClick = {
                    onNavigationEvent(TipTrackerNavigationEvent.NavigateToStatistics)
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(LifeTogetherTokens.spacing.small)
                .padding(bottom = LifeTogetherTokens.spacing.bottomInsetMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xLarge),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small)) {
                    if (content.tips.isNotEmpty()) {
                        TagOptionRow(
                            options = listOf("Calendar", "List"),
                            selectedOption = content.overviewOption,
                            onSelectedOptionChange = {
                                onUiEvent(TipTrackerUiEvent.OverviewOptionSelected(it))
                            },
                            center = true,
                        )
                        when (content.overviewOption) {
                            "Calendar" -> {
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

                            "List" -> {
                                TipsList(
                                    content.groupedTips,
                                    onDeleteClick = {
                                        onUiEvent(TipTrackerUiEvent.DeleteTipClicked(it))
                                    },
                                )
                            }
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

        if (content.showConfirmationDialog && content.selectedTip != null) {
            ConfirmationDialog(
                onDismiss = { onUiEvent(TipTrackerUiEvent.DismissDeleteConfirmation) },
                onConfirm = {
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

@Preview(showBackground = true)
@Composable
private fun TipTrackerScreenPreview() {
    LifeTogetherTheme {
        TipTrackerScreen(
            uiState = TipTrackerUiState.Content(
                tips = listOf(
                    TipItem(amount = 120f, date = Date()),
                ),
                groupedTips = mapOf("01. January 2026" to listOf(TipItem(amount = 120f, date = Date()))),
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
                    TipItem(amount = 120f, date = Date()),
                ),
                groupedTips = mapOf("01. January 2026" to listOf(TipItem(amount = 120f, date = Date()))),
                overviewOption = "List"
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
