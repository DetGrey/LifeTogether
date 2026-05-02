package com.example.lifetogether.ui.feature.recipes

import com.example.lifetogether.domain.model.Completable
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.domain.model.recipe.Instruction

sealed interface RecipeDetailsUiState {
    data object Loading : RecipeDetailsUiState

    data class Content(
        val recipeId: String? = null,
        val familyId: String? = null,
        val itemName: String = "",
        val description: String = "",
        val ingredients: List<Ingredient> = emptyList(),
        val instructions: List<Instruction> = emptyList(),
        val preparationTimeMin: String = "",
        val favourite: Boolean = false,
        val recipeServings: Int = 1,
        val servings: String = "",
        val tagsInput: String = "",
        val tags: List<String> = emptyList(),
        val editMode: Boolean = false,
        val isSaving: Boolean = false,
        val showDeleteConfirmationDialog: Boolean = false,
        val showImageUploadDialog: Boolean = false,
        val servingsExpanded: Boolean = false,
        val expandedStates: Map<String, Boolean> = mapOf(
            "ingredients" to true,
            "instructions" to true,
        ),
        val ingredientsByServings: List<Ingredient> = emptyList(),
    ) : RecipeDetailsUiState
}

sealed interface RecipeDetailsUiEvent {
    data object EditClicked : RecipeDetailsUiEvent
    data class ItemNameChanged(val value: String) : RecipeDetailsUiEvent
    data class DescriptionChanged(val value: String) : RecipeDetailsUiEvent
    data class PreparationTimeChanged(val value: String) : RecipeDetailsUiEvent
    data class ServingsChanged(val value: String) : RecipeDetailsUiEvent
    data class ServingsExpandedChanged(val value: Boolean) : RecipeDetailsUiEvent
    data class TagsChanged(val value: String) : RecipeDetailsUiEvent
    data object ToggleIngredientsExpanded : RecipeDetailsUiEvent
    data object ToggleInstructionsExpanded : RecipeDetailsUiEvent
    data class IngredientCompletedToggled(val ingredient: Completable) : RecipeDetailsUiEvent
    data class InstructionCompletedToggled(val instruction: Completable) : RecipeDetailsUiEvent
    data class AddIngredientClicked(val ingredient: Ingredient) : RecipeDetailsUiEvent
    data class AddInstructionClicked(val value: String) : RecipeDetailsUiEvent
    data object AddImageClicked : RecipeDetailsUiEvent
    data object ImageUploadDismissed : RecipeDetailsUiEvent
    data object ImageUploadConfirmed : RecipeDetailsUiEvent
    data object DeleteClicked : RecipeDetailsUiEvent
    data object DismissDeleteConfirmation : RecipeDetailsUiEvent
    data object ConfirmDeleteConfirmation : RecipeDetailsUiEvent
    data object SaveClicked : RecipeDetailsUiEvent
}

sealed interface RecipeDetailsNavigationEvent {
    data object NavigateBack : RecipeDetailsNavigationEvent
}

sealed interface RecipeDetailsCommand {
    data object NavigateBack : RecipeDetailsCommand
}
