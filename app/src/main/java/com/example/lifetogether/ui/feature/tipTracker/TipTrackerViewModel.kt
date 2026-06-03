package com.example.lifetogether.ui.feature.tipTracker

import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TipTrackerViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val tipTrackerRepository: TipTrackerRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<TipTrackerUiState>(TipTrackerUiState.Loading)
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
                    observeTips()
                } else if (state is SessionState.Unauthenticated) {
                    familyId = null
                    tipsJob?.cancel()
                    tipsJob = null
                    _uiState.value = TipTrackerUiState.Loading
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
                it.copy(selectedTip = event.tip)
            }

            TipTrackerUiEvent.ConfirmDeleteConfirmation -> deleteItem()
            is TipTrackerUiEvent.NewItemAmountChanged -> updateContent {
                it.copy(newItemAmount = event.value)
            }

            is TipTrackerUiEvent.NewItemDateChanged -> updateNewItemDate(event.value)

            TipTrackerUiEvent.AddItemClicked -> addItemToList()
            TipTrackerUiEvent.PreviousMonthClicked -> updateCalendar { it.minusMonths(1) }

            TipTrackerUiEvent.CurrentMonthClicked -> updateCalendar { LocalDate.now() }

            TipTrackerUiEvent.NextMonthClicked -> updateCalendar { it.plusMonths(1) }
        }
    }

    private fun observeTips() {
        tipsJob?.cancel()
        val familyIdValue = familyId
        if (familyIdValue.isNullOrBlank()) {
            _uiState.value = TipTrackerUiState.Loading
            return
        }

        tipsJob = viewModelScope.launch {
            tipTrackerRepository.observeTips(familyIdValue).collect { result ->
                when (result) {
                    is Result.Success -> handleTipsSuccess(result.data)
                    is Result.Failure -> showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun handleTipsSuccess(tipItems: List<TipItem>) {
        val sortedTips = tipItems.sortedByDescending { it.date }
        val stats = TipStatisticsCalculator.calculateTipStats(sortedTips)

        _uiState.update { state ->
            when (state) {
                is TipTrackerUiState.Loading -> TipTrackerUiState.Content(
                    tips = sortedTips,
                    stats = stats,
                    calendar = buildCalendarState(
                        displayedDate = LocalDate.now(),
                        tips = sortedTips,
                    ),
                )

                is TipTrackerUiState.Content -> state.copy(
                    tips = sortedTips,
                    stats = stats,
                    calendar = buildCalendarState(
                        displayedDate = state.calendar.displayedDate,
                        tips = sortedTips,
                    ),
                )
            }
        }
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
            val dayLabel = "$day ${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}"
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
            monthLabel = displayedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)),
            summary = TipTrackerCalendarSummary(
                totalText = formatTipTotal(total),
                averageText = formatTipTotal(average),
            ),
            days = days,
            gridHeight = if (daysInMonth == 31) 475.dp else 425.dp,
        )
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

    private fun updateNewItemDate(date: Date) {
        if (date.toLocalDate().isAfter(LocalDate.now())) {
            showError("Cannot add tips for a future date")
            return
        }

        updateContent { it.copy(newItemDate = date) }
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
            id = UUID.randomUUID().toString(),
            familyId = familyIdValue,
            itemName = "Tip",
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
                is Result.Success -> updateContent { it.copy(selectedTip = null) }
                is Result.Failure -> {
                    updateContent { it.copy(selectedTip = null) }
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

    private fun Date.toLocalDate(): LocalDate {
        return toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }
}
