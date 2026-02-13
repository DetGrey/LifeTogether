package com.example.lifetogether.ui.feature.tipTracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.listener.ListItemsResultListener
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.listener.StringResultListener
import com.example.lifetogether.domain.logic.toFullDateString
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.domain.usecase.item.DeleteItemUseCase
import com.example.lifetogether.domain.usecase.item.FetchListItemsUseCase
import com.example.lifetogether.domain.usecase.item.SaveItemUseCase
import com.example.lifetogether.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// Data classes for state management
data class TipStats(
    val weeklyTotal: Float = 0f,
    val monthlyTotal: Float = 0f,
    val yearlyTotal: Float = 0f,
    val total: Float = 0f,

    val weeklyAverage: Float = 0f,
    val monthlyAverage: Float = 0f,
    val yearlyAverage: Float = 0f,
    val totalAverage: Float = 0f,

    val highestTip: TipItem? = null,
    val bestMonth: Pair<String,Float>? = null,
)

data class TipTrackerUiState(
    val tips: List<TipItem> = emptyList(),
    val stats: TipStats = TipStats(),
    val groupedTips: Map<String, List<TipItem>> = emptyMap(),
    val selectedTip: TipItem? = null,
    val timePeriod: String = "Week",
    val overviewOption: String = "Calendar",
    val newItemAmount: String = "",
    val newItemDate: Date = Date(),
    val showConfirmationDialog: Boolean = false,
    val showAlertDialog: Boolean = false,
    val error: String = "",
    val isInitialized: Boolean = false,
)

@HiltViewModel
class TipTrackerViewModel @Inject constructor(
    private val saveItemUseCase: SaveItemUseCase,
    private val fetchListItemsUseCase: FetchListItemsUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TipTrackerUiState())
    val uiState: StateFlow<TipTrackerUiState> = _uiState.asStateFlow()

    private var familyId: String? = null

    fun setUpTipTracker(addedFamilyId: String) {
        if (_uiState.value.isInitialized) return

        familyId = addedFamilyId
        viewModelScope.launch {
            fetchListItemsUseCase(
                familyId!!,
                Constants.TIP_TRACKER_TABLE,
                TipItem::class,
            ).collect { result ->
                when (result) {
                    is ListItemsResultListener.Success -> {
                        handleTipsSuccess(result.listItems.filterIsInstance<TipItem>())
                    }

                    is ListItemsResultListener.Failure -> {
                        handleTipsError(result.message)
                    }
                }
            }
        }
    }

    private fun handleTipsSuccess(tipItems: List<TipItem>) {
        if (tipItems.isEmpty()) return

        val sortedTips = tipItems.sortedByDescending { it.date }
        val stats = calculateStats(sortedTips)
        val groupedTips = sortedTips.groupBy { it.date.toFullDateString() }

        _uiState.update {
            it.copy(
                tips = sortedTips,
                stats = stats,
                groupedTips = groupedTips,
                isInitialized = true,
            )
        }
    }

    private fun handleTipsError(message: String) {
        _uiState.update {
            it.copy(
                error = message,
                showAlertDialog = true,
            )
        }
    }

    private enum class Period { WEEK, MONTH, YEAR, ALL }

    private fun calculateStats(tips: List<TipItem>): TipStats {
        return TipStats(
            weeklyTotal = calculateTotal(tips, Period.WEEK),
            monthlyTotal = calculateTotal(tips, Period.MONTH),
            yearlyTotal = calculateTotal(tips, Period.YEAR),
            total = calculateTotal(tips, Period.ALL),

            weeklyAverage = calculateAverage(tips, Period.WEEK),
            monthlyAverage = calculateAverage(tips, Period.MONTH),
            yearlyAverage = calculateAverage(tips, Period.YEAR),
            totalAverage = calculateAverage(tips, Period.ALL),

            highestTip = tips.maxByOrNull { it.amount },
            bestMonth = calculateBestMonth(tips)
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
            else -> LocalDate.MIN // Should not happen given the check above
        }

        return tips.filter { tip ->
            val tipDate = tip.date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            // Check if tip is ON or AFTER the start date
            !tipDate.isBefore(startDate)
        }
    }

    private fun calculateBestMonth(tips: List<TipItem>): Pair<String, Float>? {
        if (tips.isEmpty()) return null

        val zoneId = ZoneId.systemDefault()

        // 2. Group by Year-Month key (e.g., "2024-01")
        // We use a Map<YearMonth, Double>
        val monthlyTotals = tips.groupBy { tip ->
            // Convert Date -> LocalDate -> YearMonth
            val localDate = tip.date.toInstant().atZone(zoneId).toLocalDate()
            YearMonth.from(localDate)
        }.mapValues { entry ->
            // Sum the amounts for this specific month
            entry.value.sumOf { it.amount.toDouble() }
        }

        // 3. Find the entry with the max value
        val bestEntry = monthlyTotals.maxByOrNull { it.value }

        return if (bestEntry != null) {
            // Format the key (YearMonth) into a nice string "MMMM yyyy"
            val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
            Pair(
                bestEntry.key.format(formatter),
                bestEntry.value.toFloat()
            )
        } else {
            null
        }
    }

    fun setTimePeriod(period: String) {
        _uiState.update { it.copy(timePeriod = period) }
    }

    fun setOverviewOption(option: String) {
        _uiState.update { it.copy(overviewOption = option) }
    }

    fun setSelectedTip(tip: TipItem?) {
        _uiState.update { it.copy(selectedTip = tip) }
    }

    fun setNewItemAmount(amount: String) {
        _uiState.update { it.copy(newItemAmount = amount) }
    }

    fun setNewItemDate(date: Date) {
        _uiState.update { it.copy(newItemDate = date) }
    }

    fun setShowConfirmationDialog(show: Boolean) {
        _uiState.update { it.copy(showConfirmationDialog = show) }
    }

    fun dismissAlert() {
        viewModelScope.launch {
            delay(3000)
            _uiState.update {
                it.copy(
                    showAlertDialog = false,
                    error = "",
                )
            }
        }
    }

    fun addItemToList(onSuccess: () -> Unit) {
        val currentState = _uiState.value

        if (currentState.newItemAmount.isEmpty()) {
            showError("Please write some text first")
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
            amount = currentState.newItemAmount.toFloat(),
            date = currentState.newItemDate,
        )

        viewModelScope.launch {
            val result = saveItemUseCase.invoke(tipItem, Constants.TIP_TRACKER_TABLE)
            when (result) {
                is StringResultListener.Success -> {
                    _uiState.update {
                        it.copy(
                            newItemAmount = "",
                            newItemDate = Date(),
                        )
                    }
                    onSuccess()
                }

                is StringResultListener.Failure -> {
                    showError(result.message)
                }
            }
        }
    }

    fun deleteItem() {
        val tipId = _uiState.value.selectedTip?.id ?: return

        viewModelScope.launch {
            val result = deleteItemUseCase.invoke(tipId, Constants.TIP_TRACKER_TABLE)
            when (result) {
                is ResultListener.Success -> {
                    _uiState.update {
                        it.copy(showConfirmationDialog = false)
                    }
                }

                is ResultListener.Failure -> {
                    _uiState.update {
                        it.copy(showConfirmationDialog = false)
                    }
                    showError(result.message)
                }
            }
        }
    }

    private fun showError(message: String) {
        _uiState.update {
            it.copy(
                error = message,
                showAlertDialog = true,
            )
        }
    }
}
