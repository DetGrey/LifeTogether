package com.example.lifetogether.domain.logic

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDateToString(date: Date): String {
    val format = SimpleDateFormat("dd. MMMM yyyy", Locale.getDefault())
    return format.format(date)
}
