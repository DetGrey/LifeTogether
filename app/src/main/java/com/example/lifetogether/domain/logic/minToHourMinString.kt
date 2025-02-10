package com.example.lifetogether.domain.logic

fun minToHourMinString(allMinutes: Int): String {
    if (allMinutes <= 0) {
        return ""
    }

    if (allMinutes < 60) {
        return "⏳ ${allMinutes}min"
    }

    val hours = allMinutes / 60
    val min = allMinutes % 60

    if (min == 0) {
        return "⏳ ${hours}h"
    }

    return "⏳ ${hours}h ${min}min"
}
