package com.example.lifetogether.domain.model.lists

enum class MealType(val displayName: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack"),
    OTHER("Other");

    companion object {
        fun fromValue(value: String?): MealType? {
            return entries.find { it.displayName == value || it.name == value }
        }

        fun fromDisplayName(displayName: String): MealType? {
            return entries.find { it.displayName == displayName }
        }
    }
}
