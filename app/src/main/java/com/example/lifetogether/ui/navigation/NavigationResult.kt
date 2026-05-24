package com.example.lifetogether.ui.navigation

sealed interface NavigationResult {
    data class MealPlannerFocusDate(val date: String) : NavigationResult
}
