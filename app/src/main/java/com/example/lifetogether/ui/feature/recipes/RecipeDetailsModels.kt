package com.example.lifetogether.ui.feature.recipes

import android.graphics.Bitmap
import android.net.Uri
import com.example.lifetogether.domain.model.Completable
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.model.enums.MeasureType
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
        val expandedStates: Map<String, Boolean>,
        val ingredientsByServings: List<Ingredient>,
        val grocerySuggestions: List<GrocerySuggestion> = emptyList(),
        val ingredientDraft: RecipeIngredientDraftState = RecipeIngredientDraftState(),
        val instructionDraft: String = "",
        val editingIngredientId: String? = null,
        val editingInstructionId: String? = null,
        val localImageBitmap: Bitmap? = null,
        val editMode: Boolean = false,
        val isSaving: Boolean = false,
        val showDiscardConfirmationDialog: Boolean = false,
        val showDeleteConfirmationDialog: Boolean = false,
        val showImageUploadDialog: Boolean = false,
        val servingsExpanded: Boolean = false,
    ) : RecipeDetailsUiState
}

data class RecipeIngredientDraftState(
    val itemName: String = "",
    val amount: String = "",
    val measureType: MeasureType = MeasureType.PIECE,
)

sealed interface RecipeDetailsUiEvent {
    sealed interface Editor : RecipeDetailsUiEvent {
        data object EditClicked : Editor
        data class ItemNameChanged(val value: String) : Editor
        data class DescriptionChanged(val value: String) : Editor
        data class PreparationTimeChanged(val value: String) : Editor
        data class ServingsChanged(val value: String) : Editor
        data class ServingsExpandedChanged(val value: Boolean) : Editor
        data class TagsChanged(val value: String) : Editor
        data object ToggleIngredientsExpanded : Editor
        data object ToggleInstructionsExpanded : Editor
        data class RecipeImageSelected(val uri: Uri) : Editor
    }

    sealed interface IngredientEvent : RecipeDetailsUiEvent {
        data class CompletedToggled(val ingredient: Completable) : IngredientEvent
        data class EditClicked(val ingredientId: String) : IngredientEvent
        data class Moved(val fromIndex: Int, val toIndex: Int) : IngredientEvent
        data class NameChanged(val value: String) : IngredientEvent
        data class AmountChanged(val value: String) : IngredientEvent
        data class MeasureTypeChanged(val value: MeasureType) : IngredientEvent
        data object CancelEdit : IngredientEvent
        data class AddClicked(val ingredient: Ingredient) : IngredientEvent
        data class AddToGroceryList(val ingredient: Ingredient) : IngredientEvent
    }

    sealed interface InstructionEvent : RecipeDetailsUiEvent {
        data class CompletedToggled(val instruction: Completable) : InstructionEvent
        data class EditClicked(val instructionId: String) : InstructionEvent
        data class Moved(val fromIndex: Int, val toIndex: Int) : InstructionEvent
        data class TextChanged(val value: String) : InstructionEvent
        data object CancelEdit : InstructionEvent
        data class AddClicked(val value: String) : InstructionEvent
    }

    sealed interface DialogEvent : RecipeDetailsUiEvent {
        data object DiscardClicked : DialogEvent
        data object DismissDiscardConfirmation : DialogEvent
        data object ConfirmDiscardConfirmation : DialogEvent
        data object DeleteClicked : DialogEvent
        data object DismissDeleteConfirmation : DialogEvent
        data object ConfirmDeleteConfirmation : DialogEvent
        data object SaveClicked : DialogEvent
    }
}

sealed interface RecipeDetailsNavigationEvent {
    data object NavigateBack : RecipeDetailsNavigationEvent
}

sealed interface RecipeDetailsCommand {
    data object NavigateBack : RecipeDetailsCommand
}
