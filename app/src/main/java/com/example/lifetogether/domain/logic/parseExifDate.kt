package com.example.lifetogether.domain.logic

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun parseExifDate(dateString: String? = null, timestamp: Long? = null): Date? {
    return try {
        when {
            // If a string is provided, parse it
            !dateString.isNullOrBlank() -> {
                val dateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                dateFormat.parse(dateString)
            }

            // If a timestamp is provided, convert it to Date
            timestamp != null -> Date(timestamp)

            else -> null // Return null if both are missing
        }
    } catch (e: Exception) {
        null // Handle parsing errors gracefully
    }
}
