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
    val topMonths: List<TipTotalSummary> = emptyList(),
    val topDays: List<TipTotalSummary> = emptyList(),
    val bestWeekdays: List<TipTotalSummary> = emptyList(),
    val monthlyTrend: List<TipTotalSummary> = emptyList(),
    val periodComparisons: Map<TipStatisticsPeriod, TipPeriodComparison> = emptyMap(),
)

data class TipTotalSummary(
    val label: String,
    val total: Float,
)

data class TipPeriodComparison(
    val currentTotal: Float,
    val previousTotal: Float,
    val difference: Float,
    val differencePercent: Float?,
)

enum class TipStatisticsPeriod(val displayName: String) {
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
    ALL("All"),
}

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
        val calendar: TipTrackerCalendarState,
        val selectedTip: TipItem? = null,
        val overviewOption: TipTrackerOverviewOption = TipTrackerOverviewOption.CALENDAR,
        val timePeriod: TipStatisticsPeriod = TipStatisticsPeriod.WEEK,
        val newItemAmount: String = "",
        val newItemDate: Date = Date(),
    ) : TipTrackerUiState
}

enum class TipTrackerOverviewOption(val displayName: String) {
    CALENDAR("Calendar"),
    LIST("List"),

}

sealed interface TipTrackerUiEvent {
    data class OverviewOptionSelected(val value: TipTrackerOverviewOption) : TipTrackerUiEvent
    data class TimePeriodSelected(val value: TipStatisticsPeriod) : TipTrackerUiEvent
    data class DeleteTipClicked(val tip: TipItem) : TipTrackerUiEvent
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
