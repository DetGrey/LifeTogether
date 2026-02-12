package com.example.lifetogether.domain.logic

import java.util.Locale

fun Long?.durationToString(): String {
    if (this == null) {
        return ""
    }
    val seconds = this / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.UK, "%02d:%02d", minutes, remainingSeconds)
}
