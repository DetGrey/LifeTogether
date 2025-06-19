package com.example.lifetogether.domain.logic

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDateToDayString(date: Date): String {
    val format = SimpleDateFormat("dd", Locale.getDefault())
    return format.format(date)
}
