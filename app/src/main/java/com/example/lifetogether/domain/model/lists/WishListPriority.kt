package com.example.lifetogether.domain.model.lists

enum class WishListPriority(val value: String) {
    URGENT("urgent"),
    PLANNED("planned"),
    SOMEDAY("someday"),
    ;

    companion object {
        fun fromValue(value: String?): WishListPriority {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: PLANNED
        }
    }
}
