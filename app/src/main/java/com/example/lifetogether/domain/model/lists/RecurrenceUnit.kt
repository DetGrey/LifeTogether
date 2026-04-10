package com.example.lifetogether.domain.model.lists

enum class RecurrenceUnit(val value: String) {
    DAYS("days"),
    WEEKS("weeks"),
    ;

    companion object {
        fun fromValue(value: String?): RecurrenceUnit {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: DAYS
        }
    }
}
