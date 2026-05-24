package com.example.lifetogether.domain.model.notification

import android.annotation.SuppressLint
import com.example.lifetogether.domain.model.lists.MealType

data class MealNotificationPreferences(
    val masterEnabled: Boolean = false,
    val onboardingShown: Boolean = false,
    val breakfastEnabled: Boolean = true,
    val lunchEnabled: Boolean = true,
    val dinnerEnabled: Boolean = true,
    val snackEnabled: Boolean = true,
    val breakfastHour: Int = 9,
    val breakfastMinute: Int = 0,
    val lunchHour: Int = 12,
    val lunchMinute: Int = 0,
    val dinnerHour: Int = 18,
    val dinnerMinute: Int = 0,
    val snackHour: Int = 14,
    val snackMinute: Int = 0,
)

fun MealNotificationPreferences.isEnabledFor(mealType: MealType): Boolean = masterEnabled && when (mealType) {
    MealType.BREAKFAST -> breakfastEnabled
    MealType.LUNCH -> lunchEnabled
    MealType.DINNER -> dinnerEnabled
    MealType.SNACK -> snackEnabled
    MealType.OTHER -> false
}

fun MealNotificationPreferences.typeEnabledFor(mealType: MealType): Boolean = when (mealType) {
    MealType.BREAKFAST -> breakfastEnabled
    MealType.LUNCH -> lunchEnabled
    MealType.DINNER -> dinnerEnabled
    MealType.SNACK -> snackEnabled
    MealType.OTHER -> false
}

fun MealNotificationPreferences.timeFor(mealType: MealType): Pair<Int, Int> = when (mealType) {
    MealType.BREAKFAST -> Pair(breakfastHour, breakfastMinute)
    MealType.LUNCH -> Pair(lunchHour, lunchMinute)
    MealType.DINNER -> Pair(dinnerHour, dinnerMinute)
    MealType.SNACK -> Pair(snackHour, snackMinute)
    MealType.OTHER -> Pair(0, 0)
}

@SuppressLint("DefaultLocale")
fun MealNotificationPreferences.formatTimeFor(mealType: MealType): String {
    val (h, m) = timeFor(mealType)
    return String.format("%02d:%02d", h, m)
}
