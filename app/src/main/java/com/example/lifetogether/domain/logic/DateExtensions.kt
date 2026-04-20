package com.example.lifetogether.domain.logic

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Returns: 12. February 2026
fun Date.toFullDateString(): String =
    SimpleDateFormat("dd. MMMM yyyy", Locale.getDefault()).format(this)

fun Date.toAbbreviatedDateString(): String =
    SimpleDateFormat("dd. MMM yyyy", Locale.getDefault()).format(this)

// Returns: 09:50 12. February 2026
fun Date.toDateTimeString(): String =
    SimpleDateFormat("dd. MMMM yyyy HH:mm", Locale.getDefault()).format(this)

// Returns: 12
fun Date.toDayOfMonthString(): String =
    SimpleDateFormat("dd", Locale.getDefault()).format(this)
