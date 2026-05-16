package com.example.lifetogether.ui.feature.mealPlanner.entryDetails

import com.example.lifetogether.domain.model.lists.MealType
import com.example.lifetogether.domain.model.mealplanner.MealPlan

sealed interface MealPlanDetailsUiState {
    data object Loading : MealPlanDetailsUiState

    data class Content(
        val details: MealPlanDetailsContent,
        val mealRecipeSearchState: MealRecipeSearchState = MealRecipeSearchState(),
        val isEditing: Boolean = false,
        val showDiscardDialog: Boolean = false,
        val showDeleteDialog: Boolean = false,
        val isSaving: Boolean = false,
    ) : MealPlanDetailsUiState
}

sealed interface MealPlanDetailsContent {
    data class Meal(
        val form: MealPlanFormState,
    ) : MealPlanDetailsContent {
        companion object {
            fun blank(): Meal {
                return Meal(form = MealPlanFormState())
            }

            fun from(mealPlan: MealPlan): Meal {
                return Meal(
                    form = MealPlanFormState(
                        name = mealPlan.itemName,
                        date = mealPlan.date,
                        recipeId = mealPlan.recipeId.orEmpty(),
                        customMealName = mealPlan.customMealName.orEmpty(),
                        mealType = mealPlan.mealType,
                        notes = mealPlan.notes,
                    ),
                )
            }
        }
    }
}

data class RecipeSearchItem(
    val id: String,
    val itemName: String,
    val preparationTimeMin: Int,
)

enum class MealSearchMode {
    RECIPE,
    CUSTOM,
}

data class MealRecipeSearchState(
    val mode: MealSearchMode = MealSearchMode.RECIPE,
    val query: String = "",
    val isSearchFocused: Boolean = false,
    val selectedRecipeSearchItem: RecipeSearchItem? = null,
    val suggestions: List<RecipeSearchItem> = emptyList(),
)

data class MealPlanFormState(
    val name: String = "",
    val date: String = "",
    val recipeId: String = "",
    val customMealName: String = "",
    val mealType: MealType = MealType.DINNER,
    val notes: String = "",
)

sealed interface MealPlanDetailsUiEvent {
    data object EnterEditMode : MealPlanDetailsUiEvent
    data object RequestCancelEdit : MealPlanDetailsUiEvent
    data object ConfirmDiscard : MealPlanDetailsUiEvent
    data object DismissDiscardDialog : MealPlanDetailsUiEvent
    data object RequestDeleteMealPlan : MealPlanDetailsUiEvent
    data object ConfirmDeleteMealPlan : MealPlanDetailsUiEvent
    data object DismissDeleteDialog : MealPlanDetailsUiEvent
    data object SaveClicked : MealPlanDetailsUiEvent

    sealed interface Meal : MealPlanDetailsUiEvent {
        data class DateChanged(val value: String) : Meal
        data class RecipeQueryChanged(val value: String) : Meal
        data class RecipeSearchFocusedChanged(val value: Boolean) : Meal
        data class RecipeSelected(val recipe: RecipeSearchItem) : Meal
        data class RecipeModeChanged(val mode: MealSearchMode) : Meal
        data class CustomMealNameChanged(val value: String) : Meal
        data class MealTypeChanged(val value: String) : Meal
        data class NotesChanged(val value: String) : Meal
    }
}

sealed interface MealPlanDetailsNavigationEvent {
    data object NavigateBack : MealPlanDetailsNavigationEvent
    data class NavigateToRecipeDetails(val recipeId: String) : MealPlanDetailsNavigationEvent
}

sealed interface MealPlanDetailsCommand {
    data object NavigateBack : MealPlanDetailsCommand
}
