package com.example.lifetogether.domain.logic

import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

// Returns: 12. February 2026
fun Date.toFullDateString(): String =
    SimpleDateFormat("dd. MMMM yyyy", Locale.ENGLISH).format(this)

fun Date.toAbbreviatedDateString(): String =
    SimpleDateFormat("dd. MMM yyyy", Locale.ENGLISH).format(this)

// Returns: 09:50 12. February 2026
fun Date.toDateTimeString(): String =
    SimpleDateFormat("dd. MMMM yyyy HH:mm", Locale.ENGLISH).format(this)

// Returns: 12
fun Date.toDayOfMonthString(): String =
    SimpleDateFormat("dd", Locale.ENGLISH).format(this)

fun Date.daysSince(referenceDate: Date = Date()): Long {
    val start = toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val end = referenceDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    return ChronoUnit.DAYS.between(start, end).coerceAtLeast(0)
}

fun Date.daysTogetherText(referenceDate: Date = Date()): String {
    val days = daysSince(referenceDate)
    return if (days == 1L) {
        "1 day together"
    } else {
        "$days days together"
    }
}
