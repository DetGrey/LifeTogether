package com.example.lifetogether.ui.feature.tipTracker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.example.lifetogether.domain.model.TipItem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TipsCalendarViewModel @Inject constructor() : ViewModel() {
    private val formatter = DateTimeFormatter.ofPattern("dd. MMMM yyyy")
    private var currentDisplayedDate by mutableStateOf(LocalDate.now())

    val daysInMonth: Int
        get() = getDaysInMonth(currentDisplayedDate)

    val days: List<Int>
        get() = (1..daysInMonth).toList()

    fun getDate(day: Int): LocalDate = currentDisplayedDate.withDayOfMonth(day)

    fun getDaysInMonth(date: LocalDate): Int = date.lengthOfMonth()

    fun getDateKey(date: LocalDate): String = formatter.format(date)

    fun getDayOfWeekLabel(date: LocalDate): String {
        return date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }

    fun getTipTotal(dateKey: String, filteredTips: Map<String, List<TipItem>>): Float {
        return filteredTips[dateKey]?.sumOf { it.amount.toDouble() }?.toFloat() ?: 0f
    }

    fun selectPreviousMonth() {
        currentDisplayedDate = currentDisplayedDate.minusMonths(1)
    }

    fun selectCurrentMonth() {
        currentDisplayedDate = LocalDate.now()
    }

    fun selectNextMonth() {
        currentDisplayedDate = currentDisplayedDate.plusMonths(1)
    }

    fun getCurrentMonthYearDisplay(): String {
        val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        return currentDisplayedDate.format(monthYearFormatter)
    }

    fun getGridHeight(): Dp = if (daysInMonth == 31) 475.dp else 425.dp

    fun formatTipTotal(total: Float): String {
        return if (total == total.toInt().toFloat()) { // Check if it's a whole number
            total.toInt().toString()
        } else {
            total.toString()
        }
    }
}
