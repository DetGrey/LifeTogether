package com.example.lifetogether.ui.feature.mealPlanner

import com.example.lifetogether.domain.model.mealplanner.MealPlan

sealed interface MealPlannerUiState {
    data object Loading : MealPlannerUiState

    data class Content(
        val familyId: String,
        val mealPlans: List<MealPlan>,
        val recipePrepTimes: Map<String, Int>,
        val focusDate: String? = null,
    ) : MealPlannerUiState
}

sealed interface MealPlannerUiEvent {
    data object ClearFocusDate : MealPlannerUiEvent
}

sealed interface MealPlannerNavigationEvent {
    data object NavigateBack : MealPlannerNavigationEvent
    data class NavigateToCreateMealPlan(val defaultDate: String? = null) : MealPlannerNavigationEvent
    data class NavigateToMealPlanDetails(val mealPlanId: String) : MealPlannerNavigationEvent
}
