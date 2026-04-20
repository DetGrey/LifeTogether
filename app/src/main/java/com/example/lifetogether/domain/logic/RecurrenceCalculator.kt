package com.example.lifetogether.domain.logic

import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import java.util.Calendar
import java.util.Date

object RecurrenceCalculator {

    /**
     * Calculate the next due date for [entry] starting from [from] (defaults to now).
     *
     * - DAYS: advance the anchor by multiples of [interval] days until a future date.
     * - WEEKS: find the next matching weekday within the [interval]-week window starting from [from].
     */
    fun nextDate(entry: RoutineListEntry, from: Date = Date()): Date {
        return when (entry.recurrenceUnit) {
            RecurrenceUnit.DAYS -> nextDailyDate(entry.interval, from)
            RecurrenceUnit.WEEKS -> nextWeeklyDate(entry.interval, entry.weekdays, from)
        }
    }

    private fun nextDailyDate(intervalDays: Int, from: Date): Date {
        if (intervalDays <= 0) return from
        val cal = Calendar.getInstance()
        cal.time = from
        cal.add(Calendar.DAY_OF_YEAR, intervalDays)
        return cal.time
    }

    private fun nextWeeklyDate(intervalWeeks: Int, weekdays: List<Int>, from: Date): Date {
        if (weekdays.isEmpty() || intervalWeeks <= 0) return from

        val cal = Calendar.getInstance()
        cal.time = from
        // Start checking from the next day
        cal.add(Calendar.DAY_OF_YEAR, 1)

        repeat(intervalWeeks * 7 + 1) {
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            // Calendar: 1=Sun, 2=Mon…7=Sat → map to 1=Mon…7=Sun
            val mapped = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
            if (mapped in weekdays) return cal.time
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.time
    }

    /**
     * Apply a completion event to [entry]: advance nextDate, record lastCompletedAt, increment counter.
     */
    fun applyCompletion(entry: RoutineListEntry, completedAt: Date = Date()): RoutineListEntry {
        val newNextDate = nextDate(entry, from = completedAt)
        return entry.copy(
            lastCompletedAt = completedAt,
            completionCount = entry.completionCount + 1,
            nextDate = newNextDate,
            lastUpdated = completedAt,
        )
    }
}
