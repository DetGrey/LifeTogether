package com.example.lifetogether.ui.feature.tipTracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.logic.formatDateToString
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
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
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
                TipItem::class
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
        val groupedTips = sortedTips.groupBy { formatDateToString(it.date) }

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

    private fun calculateStats(tips: List<TipItem>): TipStats {
        return TipStats(
            weeklyTotal = calculateTotal(tips, 7),
            monthlyTotal = calculateTotal(tips, 30),
            yearlyTotal = calculateTotal(tips, 365),
            total = calculateTotal(tips, null),
            weeklyAverage = calculateAverage(tips, 7),
            monthlyAverage = calculateAverage(tips, 30),
            yearlyAverage = calculateAverage(tips, 365),
            totalAverage = calculateAverage(tips, null),
        )
    }

    private fun calculateTotal(tips: List<TipItem>, days: Int?): Float {
        val filteredTips = filterTipsByDays(tips, days)
        return filteredTips.sumOf { it.amount.toDouble() }.toFloat()
    }

    private fun calculateAverage(tips: List<TipItem>, days: Int?): Float {
        val filteredTips = filterTipsByDays(tips, days)
        return if (filteredTips.isNotEmpty()) {
            val average = filteredTips.sumOf { it.amount.toDouble() } / filteredTips.size
            String.format(Locale.US, "%.2f", average).toFloat()
        } else {
            0f
        }
    }

    private fun filterTipsByDays(tips: List<TipItem>, days: Int?): List<TipItem> {
        return if (days != null) {
            val cutoff = Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong()))
            tips.filter { it.date.after(cutoff) }
        } else {
            tips
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
