package com.example.lifetogether.ui.feature.tipTracker

import com.example.lifetogether.domain.model.TipItem
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.util.Date

data class TipTrackerStats(
    val weeklyTotal: Float = 0f,
    val monthlyTotal: Float = 0f,
    val yearlyTotal: Float = 0f,
    val total: Float = 0f,
    val weeklyAverage: Float = 0f,
    val monthlyAverage: Float = 0f,
    val yearlyAverage: Float = 0f,
    val totalAverage: Float = 0f,
    val highestTip: TipItem? = null,
    val bestMonth: Pair<String, Float>? = null,
)

data class TipTrackerCalendarState(
    val displayedDate: LocalDate = LocalDate.now(),
    val monthLabel: String = "",
    val summary: TipTrackerCalendarSummary = TipTrackerCalendarSummary(),
    val days: List<TipTrackerCalendarDay> = emptyList(),
    val gridHeight: Dp = 425.dp,
)

data class TipTrackerCalendarSummary(
    val totalText: String = "0",
    val averageText: String = "0",
)

data class TipTrackerCalendarDay(
    val label: String,
    val totalText: String? = null,
)

sealed interface TipTrackerUiState {
    data object Loading : TipTrackerUiState

    data class Content(
        val tips: List<TipItem>,
        val stats: TipTrackerStats,
        val groupedTips: Map<String, List<TipItem>>,
        val calendar: TipTrackerCalendarState,
        val selectedTip: TipItem? = null,
        val overviewOption: String = "Calendar",
        val timePeriod: String = "Week",
        val newItemAmount: String = "",
        val newItemDate: Date = Date(),
        val showConfirmationDialog: Boolean = false,
    ) : TipTrackerUiState
}

sealed interface TipTrackerUiEvent {
    data class OverviewOptionSelected(val value: String) : TipTrackerUiEvent
    data class TimePeriodSelected(val value: String) : TipTrackerUiEvent
    data class DeleteTipClicked(val tip: TipItem) : TipTrackerUiEvent
    data object DismissDeleteConfirmation : TipTrackerUiEvent
    data object ConfirmDeleteConfirmation : TipTrackerUiEvent
    data class NewItemAmountChanged(val value: String) : TipTrackerUiEvent
    data class NewItemDateChanged(val value: Date) : TipTrackerUiEvent
    data object AddItemClicked : TipTrackerUiEvent
    data object PreviousMonthClicked : TipTrackerUiEvent
    data object CurrentMonthClicked : TipTrackerUiEvent
    data object NextMonthClicked : TipTrackerUiEvent
}

sealed interface TipTrackerNavigationEvent {
    data object NavigateBack : TipTrackerNavigationEvent
    data object NavigateToStatistics : TipTrackerNavigationEvent
}
