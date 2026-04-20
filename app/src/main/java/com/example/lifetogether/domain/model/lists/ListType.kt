package com.example.lifetogether.domain.model.lists

enum class ListType(val value: String) {
    ROUTINE("routine"),
    ;

    companion object {
        fun fromValue(value: String?): ListType {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: ROUTINE
        }
    }
}
