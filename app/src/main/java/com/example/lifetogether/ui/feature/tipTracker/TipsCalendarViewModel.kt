package com.example.lifetogether.ui.feature.tipTracker

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
    private val today = LocalDate.now()

    val daysInMonth: Int = today.lengthOfMonth()
    val days: List<Int> = (1..daysInMonth).toList()

    fun getDate(day: Int): LocalDate = today.withDayOfMonth(day)

    fun getDateKey(date: LocalDate): String = formatter.format(date)

    fun getDayOfWeekLabel(date: LocalDate): String {
        return date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }

    fun getTipTotal(dateKey: String, filteredTips: Map<String, List<TipItem>>): Float {
        return filteredTips[dateKey]?.sumOf { it.amount.toDouble() }?.toFloat() ?: 0f
    }

    fun getGridHeight(): Dp = if (daysInMonth == 31) 475.dp else 425.dp
}
