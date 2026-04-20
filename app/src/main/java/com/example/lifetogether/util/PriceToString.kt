package com.example.lifetogether.util

import java.util.Locale

fun Float.priceToString(
    toPreciseFormat: Boolean = false,
): String {
    val decimals = if (toPreciseFormat) "%.2f" else "%.0f"
    return "~" + String.format(Locale.US, decimals, this) + "kr."
}
