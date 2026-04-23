package com.example.lifetogether.ui.feature.tipTracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.logic.toFullDateString
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.repository.TipTrackerRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.ui.common.event.UiCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TipTrackerViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val tipTrackerRepository: TipTrackerRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<TipTrackerUiState>(TipTrackerUiState.Content())
    val uiState: StateFlow<TipTrackerUiState> = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    private var familyId: String? = null
    private var tipsJob: Job? = null

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val newFamilyId = (state as? SessionState.Authenticated)?.user?.familyId
                if (newFamilyId != familyId) {
                    familyId = newFamilyId
                    observeTips(newFamilyId)
                } else if (state is SessionState.Unauthenticated) {
                    familyId = null
                    observeTips(null)
                }
            }
        }
    }

    fun onEvent(event: TipTrackerUiEvent) {
        when (event) {
            is TipTrackerUiEvent.OverviewOptionSelected -> updateContent {
                it.copy(overviewOption = event.value)
            }

            is TipTrackerUiEvent.TimePeriodSelected -> updateContent {
                it.copy(timePeriod = event.value)
            }

            is TipTrackerUiEvent.DeleteTipClicked -> updateContent {
                it.copy(
                    selectedTip = event.tip,
                    showConfirmationDialog = true,
                )
            }

            TipTrackerUiEvent.DismissDeleteConfirmation -> updateContent {
                it.copy(
                    selectedTip = null,
                    showConfirmationDialog = false,
                )
            }

            TipTrackerUiEvent.ConfirmDeleteConfirmation -> deleteItem()
            is TipTrackerUiEvent.NewItemAmountChanged -> updateContent {
                it.copy(newItemAmount = event.value)
            }

            is TipTrackerUiEvent.NewItemDateChanged -> updateContent {
                it.copy(newItemDate = event.value)
            }

            TipTrackerUiEvent.AddItemClicked -> addItemToList()
            TipTrackerUiEvent.PreviousMonthClicked -> updateCalendar { it.minusMonths(1) }

            TipTrackerUiEvent.CurrentMonthClicked -> updateCalendar { LocalDate.now() }

            TipTrackerUiEvent.NextMonthClicked -> updateCalendar { it.plusMonths(1) }
        }
    }

    private fun observeTips(familyId: String?) {
        tipsJob?.cancel()
        if (familyId.isNullOrBlank()) {
            updateContent { TipTrackerUiState.Content() }
            return
        }

        tipsJob = viewModelScope.launch {
            tipTrackerRepository.observeTips(familyId).collect { result ->
                when (result) {
                    is Result.Success -> handleTipsSuccess(result.data)
                    is Result.Failure -> showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun handleTipsSuccess(tipItems: List<TipItem>) {
        val sortedTips = tipItems.sortedByDescending { it.date }
        val stats = calculateStats(sortedTips)
        val groupedTips = sortedTips.groupBy { it.date.toFullDateString() }

        updateContent {
            it.copy(
                tips = sortedTips,
                stats = stats,
                groupedTips = groupedTips,
                calendar = buildCalendarState(
                    displayedDate = it.calendar.displayedDate,
                    tips = sortedTips,
                ),
            )
        }
    }

    private enum class Period { WEEK, MONTH, YEAR, ALL }

    private fun calculateStats(tips: List<TipItem>): TipTrackerStats {
        return TipTrackerStats(
            weeklyTotal = calculateTotal(tips, Period.WEEK),
            monthlyTotal = calculateTotal(tips, Period.MONTH),
            yearlyTotal = calculateTotal(tips, Period.YEAR),
            total = calculateTotal(tips, Period.ALL),
            weeklyAverage = calculateAverage(tips, Period.WEEK),
            monthlyAverage = calculateAverage(tips, Period.MONTH),
            yearlyAverage = calculateAverage(tips, Period.YEAR),
            totalAverage = calculateAverage(tips, Period.ALL),
            highestTip = tips.maxByOrNull { it.amount },
            bestMonth = calculateBestMonth(tips),
        )
    }

    private fun calculateTotal(tips: List<TipItem>, period: Period): Float {
        val filteredTips = filterTipsByPeriod(tips, period)
        return filteredTips.sumOf { it.amount.toDouble() }.toFloat()
    }

    private fun calculateAverage(tips: List<TipItem>, period: Period): Float {
        val filteredTips = filterTipsByPeriod(tips, period)
        return if (filteredTips.isNotEmpty()) {
            val average = filteredTips.sumOf { it.amount.toDouble() } / filteredTips.size
            String.format(Locale.US, "%.2f", average).toFloat()
        } else {
            0f
        }
    }

    private fun filterTipsByPeriod(tips: List<TipItem>, period: Period): List<TipItem> {
        if (period == Period.ALL) return tips

        val today = LocalDate.now()
        val startDate = when (period) {
            Period.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            Period.MONTH -> today.with(TemporalAdjusters.firstDayOfMonth())
            Period.YEAR -> today.with(TemporalAdjusters.firstDayOfYear())
            else -> LocalDate.MIN
        }

        return tips.filter { tip ->
            val tipDate = tip.date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            !tipDate.isBefore(startDate)
        }
    }

    private fun calculateBestMonth(tips: List<TipItem>): Pair<String, Float>? {
        if (tips.isEmpty()) return null

        val zoneId = ZoneId.systemDefault()
        val monthlyTotals = tips.groupBy { tip ->
            val localDate = tip.date.toInstant().atZone(zoneId).toLocalDate()
            YearMonth.from(localDate)
        }.mapValues { entry ->
            entry.value.sumOf { it.amount.toDouble() }
        }

        val bestEntry = monthlyTotals.maxByOrNull { it.value } ?: return null
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
        return bestEntry.key.format(formatter) to bestEntry.value.toFloat()
    }

    private fun buildCalendarState(
        displayedDate: LocalDate,
        tips: List<TipItem>,
    ): TipTrackerCalendarState {
        val zoneId = ZoneId.systemDefault()
        val tipDates = tips.map { tip ->
            tip.date.toInstant().atZone(zoneId).toLocalDate()
        }
        val monthlyTips = tips.filterIndexed { index, _ ->
            val tipDate = tipDates[index]
            tipDate.month == displayedDate.month && tipDate.year == displayedDate.year
        }
        val daysInMonth = displayedDate.lengthOfMonth()
        val days = (1..daysInMonth).map { day ->
            val date = displayedDate.withDayOfMonth(day)
            val dayLabel = "$day ${date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())}"
            val total = tips.filterIndexed { index, _ -> tipDates[index] == date }
                .sumOf { it.amount.toDouble() }
                .toFloat()
            TipTrackerCalendarDay(
                label = dayLabel,
                totalText = if (total > 0f) formatTipTotal(total) else null,
            )
        }
        val total = monthlyTips.sumOf { it.amount.toDouble() }.toFloat()
        val average = if (monthlyTips.isNotEmpty()) total / monthlyTips.size else 0f

        return TipTrackerCalendarState(
            displayedDate = displayedDate,
            monthLabel = displayedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)),
            summary = TipTrackerCalendarSummary(
                totalText = formatTipTotal(total),
                averageText = formatTipTotal(average),
            ),
            days = days,
            gridHeight = if (daysInMonth == 31) 475.dp else 425.dp,
        )
    }

    private fun formatTipTotal(total: Float): String {
        return total
            .toBigDecimal()
            .setScale(2, java.math.RoundingMode.HALF_DOWN)
            .stripTrailingZeros()
            .toPlainString()
    }

    private fun updateCalendar(transform: (LocalDate) -> LocalDate) {
        updateContent { content ->
            content.copy(
                calendar = buildCalendarState(
                    displayedDate = transform(content.calendar.displayedDate),
                    tips = content.tips,
                ),
            )
        }
    }

    private fun addItemToList() {
        val currentState = contentState()
        val amount = currentState.newItemAmount.trim()

        if (amount.isEmpty()) {
            showError("Please write some text first")
            return
        }

        val parsedAmount = amount.toFloatOrNull()
        if (parsedAmount == null) {
            showError("Please enter a valid amount")
            return
        }

        val familyIdValue = familyId
        if (familyIdValue == null) {
            showError("Please connect to a family first")
            return
        }

        val tipItem = TipItem(
            familyId = familyIdValue,
            lastUpdated = Date(System.currentTimeMillis()),
            amount = parsedAmount,
            date = currentState.newItemDate,
        )

        viewModelScope.launch {
            when (val result = tipTrackerRepository.saveTip(tipItem)) {
                is Result.Success -> {
                    updateContent {
                        it.copy(
                            newItemAmount = "",
                            newItemDate = Date(),
                        )
                    }
                }

                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun deleteItem() {
        val tipId = contentState().selectedTip?.id ?: return

        viewModelScope.launch {
            when (val result = tipTrackerRepository.deleteTip(tipId)) {
                is Result.Success -> updateContent {
                    it.copy(
                        selectedTip = null,
                        showConfirmationDialog = false,
                    )
                }

                is Result.Failure -> {
                    updateContent {
                        it.copy(
                            selectedTip = null,
                            showConfirmationDialog = false,
                        )
                    }
                    showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun showError(message: String) {
        viewModelScope.launch {
            _uiCommands.send(
                UiCommand.ShowSnackbar(
                    message = message,
                    withDismissAction = true,
                ),
            )
        }
    }

    private fun contentState(): TipTrackerUiState.Content {
        return _uiState.value as TipTrackerUiState.Content
    }

    private fun updateContent(transform: (TipTrackerUiState.Content) -> TipTrackerUiState.Content) {
        _uiState.update { state ->
            (state as? TipTrackerUiState.Content)?.let(transform) ?: state
        }
    }
}
