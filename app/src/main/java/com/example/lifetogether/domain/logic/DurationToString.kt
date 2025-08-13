package com.example.lifetogether.domain.logic

import java.util.Locale

fun durationToString(duration: Long?): String {
    if (duration == null) {
        return ""
    }
    val seconds = duration / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.UK, "%02d:%02d", minutes, remainingSeconds)
}
