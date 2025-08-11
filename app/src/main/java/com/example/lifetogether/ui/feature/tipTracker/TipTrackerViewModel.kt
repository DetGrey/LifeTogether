package com.example.lifetogether.ui.feature.tipTracker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class TipTrackerViewModel @Inject constructor(
    private val saveItemUseCase: SaveItemUseCase,
    private val fetchListItemsUseCase: FetchListItemsUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
) : ViewModel() {
    var showConfirmationDialog: Boolean by mutableStateOf(false)

    // ---------------------------------------------------------------- ERROR
    var showAlertDialog: Boolean by mutableStateOf(false)
    var error: String by mutableStateOf("")
    fun toggleAlertDialog() {
        viewModelScope.launch {
            delay(3000)
            showAlertDialog = false
            error = ""
        }
    }

    // ---------------------------------------------------------------- UID
    private var familyIdIsSet = false
    var familyId: String? = null

    // ---------------------------------------------------------------- SETUP/FETCH LIST
    private val _tips = MutableStateFlow<List<TipItem>>(emptyList())
    val tips: StateFlow<List<TipItem>> = _tips.asStateFlow()

    fun setUpTipTracker(addedFamilyId: String) {
        if (!familyIdIsSet) {
            println("TipTrackerViewModel setting familyId")
            familyId = addedFamilyId
            // Use the UID here (e.g., fetch grocery list items)
            viewModelScope.launch {
                fetchListItemsUseCase(familyId!!, Constants.TIP_TRACKER_TABLE, TipItem::class).collect { result ->
                    println("fetchListItemsUseCase result: $result")
                    when (result) {
                        is ListItemsResultListener.Success -> {
                            // Filter and map the result.listItems to only include TipItem instances
                            println("Items found: ${result.listItems}")
                            val tipItems = result.listItems.filterIsInstance<TipItem>()
                            if (tipItems.isNotEmpty()) {
                                println("_tipTracker old value: ${_tips.value}")
                                _tips.value = tipItems.sortedByDescending { it.date }
                                println("tipTracker new value: ${tips.value}")

                                weeklyTotal = getTotalForLastDays(tipItems, 7)
                                monthlyTotal = getTotalForLastDays(tipItems, 30)
                                yearlyTotal = getTotalForLastDays(tipItems, 365)
                                total = getTotalForLastDays(tipItems, null)
                                weeklyAverage = getAverageForLastDays(tipItems, 7)
                                monthlyAverage = getAverageForLastDays(tipItems, 30)
                                yearlyAverage = getAverageForLastDays(tipItems, 365)
                                totalAverage = getAverageForLastDays(tipItems, null)

                                groupedTips = tips.value.groupBy { formatDateToString(it.date) }
                            }
                        }

                        is ListItemsResultListener.Failure -> {
                            // Handle failure, e.g., show an error message
                            println("Error: ${result.message}")
                            error = result.message
                            showAlertDialog = true
                        }
                    }
                }
            }
            familyIdIsSet = true
        }
    }

    // ---------------------------------------------------------------- STATS
    var timePeriod: String by mutableStateOf("Week")

    var weeklyTotal: Float by mutableFloatStateOf(0f)
    var monthlyTotal: Float by mutableFloatStateOf(0f)
    var yearlyTotal: Float by mutableFloatStateOf(0f)
    var total: Float by mutableFloatStateOf(0f)

    private fun getTotalForLastDays(tips: List<TipItem>, days: Int?): Float {
        return if (days != null) {
            val cutoff = Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong()))
            tips.filter { it.date.after(cutoff) }.sumOf { it.amount.toDouble() }.toFloat()
        } else {
            tips.sumOf { it.amount.toDouble() }.toFloat()
        }
    }

    var weeklyAverage: Float by mutableFloatStateOf(0f)
    var monthlyAverage: Float by mutableFloatStateOf(0f)
    var yearlyAverage: Float by mutableFloatStateOf(0f)
    var totalAverage: Float by mutableFloatStateOf(0f)

    private fun getAverageForLastDays(tips: List<TipItem>, days: Int?): Float {
        val filteredTips = if (days != null) {
            val cutoff = Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong()))
            tips.filter { it.date.after(cutoff) }
        } else {
            tips
        }

        return if (filteredTips.isNotEmpty()) {
            val average = filteredTips.sumOf { it.amount.toDouble() } / filteredTips.size
            String.format(Locale.US, "%.2f", average).toFloat()
        } else {
            0f
        }
    }

    // ---------------------------------------------------------------- TIP OVERVIEW
    var overviewOption: String by mutableStateOf("Calendar")
    var groupedTips: Map<String, List<TipItem>> by mutableStateOf(mapOf())
    var selectedTip: TipItem? by mutableStateOf(null)

    // ---------------------------------------------------------------- NEW ITEM
    var newItemAmount: String by mutableStateOf("")
    var newItemDate: Date by mutableStateOf(Date())

    // ---------------------------------------------------------------- ADD NEW ITEM
    // USE CASES
    fun addItemToList(
        onSuccess: () -> Unit,
    ) {
        println("TipTrackerViewModel addItemToList()")

        if (newItemAmount.isEmpty()) {
            error = "Please write some text first"
            showAlertDialog = true
            return
        }

        val tipItem = familyId?.let {
            TipItem(
                familyId = it,
                lastUpdated = Date(System.currentTimeMillis()),
                amount = newItemAmount.toFloat(),
                date = newItemDate,
            )
        }
        if (tipItem == null) {
            error = "Please connect to a family first"
            showAlertDialog = true
            return
        }

        viewModelScope.launch {
            val result: StringResultListener = saveItemUseCase.invoke(tipItem, Constants.TIP_TRACKER_TABLE)
            if (result is StringResultListener.Success) {
                newItemAmount = ""
                newItemDate = Date()
                onSuccess()
            } else if (result is StringResultListener.Failure) {
                println("Error: ${result.message}")
                error = result.message
                showAlertDialog = true
            }
        }
    }

    fun deleteItem() {
        selectedTip?.id?.let { tipId ->
            viewModelScope.launch {
                val result: ResultListener =
                    deleteItemUseCase.invoke(tipId, Constants.TIP_TRACKER_TABLE)
                if (result is ResultListener.Success) {
                    showConfirmationDialog = false
                } else if (result is ResultListener.Failure) {
                    println("Error: ${result.message}")
                    showConfirmationDialog = false
                    error = result.message
                }
            }
        }
    }
}
