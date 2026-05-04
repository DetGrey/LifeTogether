package com.example.lifetogether.ui.feature.recipes

import com.example.lifetogether.domain.model.Completable
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.domain.model.recipe.Instruction

sealed interface RecipeDetailsUiState {
    data object Loading : RecipeDetailsUiState

    data class Content(
        val recipeId: String?,
        val familyId: String?,
        val itemName: String,
        val description: String,
        val ingredients: List<Ingredient>,
        val instructions: List<Instruction>,
        val preparationTimeMin: String,
        val favourite: Boolean,
        val recipeServings: Int,
        val servings: String,
        val tagsInput: String,
        val tags: List<String>,
        val editMode: Boolean,
        val isSaving: Boolean,
        val showDeleteConfirmationDialog: Boolean,
        val showImageUploadDialog: Boolean,
        val servingsExpanded: Boolean,
        val expandedStates: Map<String, Boolean>,
        val ingredientsByServings: List<Ingredient>,
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
