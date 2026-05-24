package com.example.lifetogether.ui.feature.recipes.details

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.logic.ingredientMatchesSuggestion
import com.example.lifetogether.domain.logic.toBitmap
import com.example.lifetogether.domain.model.Completable
import com.example.lifetogether.domain.model.enums.MeasureType
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.domain.model.recipe.Instruction
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.model.toggleCompleted
import com.example.lifetogether.domain.repository.GroceryRepository
import com.example.lifetogether.domain.repository.RecipeRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.domain.usecase.image.UploadImageUseCase
import com.example.lifetogether.ui.common.event.UiCommand
import com.example.lifetogether.ui.common.snackbar.SnackbarSeverity
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion

@HiltViewModel(assistedFactory = RecipeDetailsViewModel.Factory::class)
class RecipeDetailsViewModel @AssistedInject constructor(
    @Assisted val recipeId: String?,
    private val savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val recipeRepository: RecipeRepository,
    private val groceryRepository: GroceryRepository,
    private val uploadImageUseCase: UploadImageUseCase,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(recipeId: String?): RecipeDetailsViewModel
    }

    companion object {
        private const val PENDING_IMAGE_URI_ARG = "pendingRecipeImageUri"
    }

    private val _uiState = MutableStateFlow<RecipeDetailsUiState>(RecipeDetailsUiState.Loading)
    val uiState: StateFlow<RecipeDetailsUiState> = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    private val _commands = Channel<RecipeDetailsCommand>(Channel.BUFFERED)
    val commands: Flow<RecipeDetailsCommand> = _commands.receiveAsFlow()

    private var pendingRecipeId: String? = recipeId
    private var pendingImageUri: Uri? = savedStateHandle.get<String>(PENDING_IMAGE_URI_ARG)?.let(Uri::parse)
    private var currentFamilyId: String? = null
    private var originalRecipe: Recipe? = null
    private var observeRecipeJob: Job? = null
    private var latestSuggestions: List<GrocerySuggestion> = emptyList()

    init {
        if (pendingRecipeId == null) {
            _uiState.value = createContentState(editMode = true)
        }

        if (pendingImageUri != null) {
            restorePendingImagePreview()
        }

        viewModelScope.launch {
            groceryRepository.observeGrocerySuggestions().collect { result ->
                if (result is Result.Success) {
                    latestSuggestions = result.data
                    updateContent { it.copy(grocerySuggestions = result.data) }
                }
            }
        }

        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                currentFamilyId = (state as? SessionState.Authenticated)?.user?.familyId
                updateContent { content ->
                    content.copy(familyId = currentFamilyId)
                }

                val recipeId = pendingRecipeId
                val familyId = currentFamilyId
                if (!recipeId.isNullOrBlank() && !familyId.isNullOrBlank()) {
                    observeRecipe(recipeId = recipeId, familyId = familyId)
                }
            }
        }
    }

    fun onEvent(event: RecipeDetailsUiEvent) {
        when (event) {
            RecipeDetailsUiEvent.Editor.EditClicked -> toggleEditMode()
            is RecipeDetailsUiEvent.Editor.ItemNameChanged -> updateContent {
                it.copy(itemName = event.value)
            }
            is RecipeDetailsUiEvent.Editor.DescriptionChanged -> updateContent {
                it.copy(description = event.value)
            }
            is RecipeDetailsUiEvent.Editor.PreparationTimeChanged -> updateContent {
                it.copy(preparationTimeMin = event.value)
            }
            is RecipeDetailsUiEvent.Editor.ServingsChanged -> updateContentAndIngredientsByServings(event.value)
            is RecipeDetailsUiEvent.Editor.ServingsExpandedChanged -> updateContent {
                it.copy(servingsExpanded = event.value)
            }
            is RecipeDetailsUiEvent.Editor.TagsChanged -> updateContent {
                it.copy(tagsInput = event.value)
            }
            RecipeDetailsUiEvent.Editor.ToggleIngredientsExpanded -> toggleExpandedState("ingredients")
            RecipeDetailsUiEvent.Editor.ToggleInstructionsExpanded -> toggleExpandedState("instructions")
            is RecipeDetailsUiEvent.Editor.RecipeImageSelected -> setPendingRecipeImage(event.uri)

            is RecipeDetailsUiEvent.IngredientEvent.CompletedToggled -> toggleIngredientCompletion(event.ingredient)
            is RecipeDetailsUiEvent.InstructionEvent.CompletedToggled -> toggleInstructionCompletion(event.instruction)
            is RecipeDetailsUiEvent.IngredientEvent.EditClicked -> startEditingIngredient(event.ingredientId)
            is RecipeDetailsUiEvent.IngredientEvent.DeleteClicked -> requestIngredientDelete(event.ingredientId)
            is RecipeDetailsUiEvent.IngredientEvent.Moved -> moveIngredient(event.fromIndex, event.toIndex)
            is RecipeDetailsUiEvent.IngredientEvent.NameChanged -> updateIngredientDraft { it.copy(itemName = event.value) }
            is RecipeDetailsUiEvent.IngredientEvent.AmountChanged -> updateIngredientDraft { it.copy(amount = event.value) }
            is RecipeDetailsUiEvent.IngredientEvent.MeasureTypeChanged -> updateIngredientDraft { it.copy(measureType = event.value) }
            is RecipeDetailsUiEvent.InstructionEvent.TextChanged -> updateContent { it.copy(instructionDraft = event.value) }
            RecipeDetailsUiEvent.IngredientEvent.CancelEdit -> clearIngredientDraft()
            is RecipeDetailsUiEvent.IngredientEvent.AddClicked -> addIngredient(event.ingredient)

            is RecipeDetailsUiEvent.InstructionEvent.EditClicked -> startEditingInstruction(event.instructionId)
            is RecipeDetailsUiEvent.InstructionEvent.DeleteClicked -> requestInstructionDelete(event.instructionId)
            is RecipeDetailsUiEvent.InstructionEvent.Moved -> moveInstruction(event.fromIndex, event.toIndex)
            RecipeDetailsUiEvent.InstructionEvent.CancelEdit -> clearInstructionDraft()
            is RecipeDetailsUiEvent.InstructionEvent.AddClicked -> addInstruction(event.value)
            is RecipeDetailsUiEvent.IngredientEvent.AddToGroceryList -> addIngredientToGroceryList(event.ingredient)

            RecipeDetailsUiEvent.DialogEvent.DiscardClicked -> discardChanges()
            RecipeDetailsUiEvent.DialogEvent.DismissDiscardConfirmation -> dismissDiscardConfirmation()
            RecipeDetailsUiEvent.DialogEvent.ConfirmDiscardConfirmation -> performDiscardChanges()
            RecipeDetailsUiEvent.DialogEvent.DeleteClicked -> updateContent {
                if (it.recipeId.isNullOrBlank()) {
                    it
                } else {
                    it.copy(deleteConfirmationTarget = RecipeDeleteConfirmationTarget.Recipe)
                }
            }
            RecipeDetailsUiEvent.DialogEvent.DismissDeleteConfirmation -> updateContent {
                it.copy(deleteConfirmationTarget = null)
            }
            RecipeDetailsUiEvent.DialogEvent.ConfirmDeleteConfirmation -> confirmDelete()
            RecipeDetailsUiEvent.DialogEvent.SaveClicked -> saveRecipe()
        }
    }

    private fun observeRecipe(recipeId: String, familyId: String) {
        observeRecipeJob?.cancel()
        observeRecipeJob = viewModelScope.launch {
            recipeRepository.observeRecipeById(familyId, recipeId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        originalRecipe = result.data
                        _uiState.value = createContentState(
                            recipe = result.data,
                            familyId = familyId,
                            recipeId = recipeId,
                            editMode = false,
                        )
                    }

                    is Result.Failure -> {
                        if (result.error is AppError.NotFound) {
                            _commands.send(RecipeDetailsCommand.NavigateBack)
                        } else {
                            showError(result.error.toUserMessage())
                        }
                    }
                }
            }
        }
    }

    private fun createContentState(
        recipe: Recipe? = null,
        familyId: String? = currentFamilyId,
        recipeId: String? = pendingRecipeId,
        editMode: Boolean,
    ): RecipeDetailsUiState.Content {
        val isNewRecipe = recipe == null && recipeId == null
        val sourceRecipe = recipe ?: originalRecipe ?: blankRecipe(
            recipeId = recipeId.orEmpty(),
            familyId = familyId.orEmpty(),
        )

        val servings = sourceRecipe.servings.toString()
        val ingredients = sourceRecipe.ingredients

        return RecipeDetailsUiState.Content(
            recipeId = recipeId?.takeIf { it.isNotBlank() }
                ?: sourceRecipe.id.takeIf { it.isNotBlank() },
            familyId = familyId?.takeIf { it.isNotBlank() },
            itemName = sourceRecipe.itemName,
            description = sourceRecipe.description,
            ingredients = ingredients,
            instructions = sourceRecipe.instructions,
            preparationTimeMin = if (isNewRecipe) {
                ""
            } else {
                sourceRecipe.preparationTimeMin.toString()
            },
            favourite = sourceRecipe.favourite,
            recipeServings = sourceRecipe.servings,
            servings = if (recipe == null && recipeId == null) "" else servings,
            tagsInput = sourceRecipe.tags.joinToString(" "),
            tags = sourceRecipe.tags,
            ingredientDraft = RecipeIngredientDraftState(),
            instructionDraft = "",
            editingIngredientId = null,
            editingInstructionId = null,
            grocerySuggestions = latestSuggestions,
            editMode = editMode,
            isSaving = false,
            showDiscardConfirmationDialog = false,
            deleteConfirmationTarget = null,
            servingsExpanded = false,
            expandedStates = defaultExpandedStates(),
            ingredientsByServings = scaleIngredients(
                ingredients = ingredients,
                recipeServings = sourceRecipe.servings,
                selectedServings = sourceRecipe.servings.toDouble(),
            ),
        )
    }

    private fun defaultExpandedStates(): Map<String, Boolean> {
        return mapOf(
            "ingredients" to true,
            "instructions" to true,
        )
    }

    private fun toggleEditMode() {
        val content = contentState() ?: return
        if (content.recipeId.isNullOrBlank()) {
            return
        }

        if (content.editMode) {
            originalRecipe?.let { restoreRecipe(it) }
        } else {
            updateContent {
                it.copy(
                    editMode = true,
                    showDiscardConfirmationDialog = false,
                    editingIngredientId = null,
                    editingInstructionId = null,
                    ingredientDraft = RecipeIngredientDraftState(),
                    instructionDraft = "",
                )
            }
        }
    }

    private fun restoreRecipe(recipe: Recipe) {
        clearPendingRecipeImage()
        _uiState.value = createContentState(
            recipe = recipe,
            familyId = currentFamilyId,
            recipeId = pendingRecipeId,
            editMode = false,
        )
    }

    private fun toggleExpandedState(name: String) {
        updateContent { state ->
            val currentExpanded = state.expandedStates[name] ?: true
            state.copy(
                expandedStates = state.expandedStates.toMutableMap().apply {
                    put(name, !currentExpanded)
                },
            )
        }
    }

    private fun toggleIngredientCompletion(ingredient: Completable) {
        updateContent { state ->
            if (state.editMode) {
                val updatedIngredients = state.ingredients.toggleCompleted(ingredient.id)
                state.copy(
                    ingredients = updatedIngredients,
                    ingredientsByServings = scaleIngredients(
                        ingredients = updatedIngredients,
                        recipeServings = state.recipeServings,
                        selectedServings = state.servings.toDoubleOrNull() ?: state.recipeServings.toDouble(),
                    ),
                )
            } else {
                state.copy(
                    ingredientsByServings = state.ingredientsByServings.toggleCompleted(ingredient.id),
                )
            }
        }
    }

    private fun toggleInstructionCompletion(instruction: Completable) {
        updateContent {
            it.copy(
                instructions = it.instructions.toggleCompleted(instruction.id),
            )
        }
    }

    private fun addIngredient(ingredient: Ingredient) {
        updateContent { state ->
            if (!state.editMode) {
                state
            } else {
                val trimmedName = ingredient.itemName.trim()
                if (trimmedName.isBlank() || ingredient.amount < 0.0) {
                    state
                } else {
                    val sanitizedIngredient = ingredient.copy(itemName = trimmedName)
                    val updatedIngredients = if (state.editingIngredientId != null) {
                        state.ingredients.map { current ->
                            if (current.id == state.editingIngredientId) {
                                sanitizedIngredient.copy(
                                    id = current.id,
                                    sortOrder = current.sortOrder,
                                )
                            } else {
                                current
                            }
                        }
                    } else {
                        val nextSortOrder = (state.ingredients.maxOfOrNull { it.sortOrder } ?: -1) + 1
                        state.ingredients + sanitizedIngredient.copy(sortOrder = nextSortOrder)
                    }
                    state.copy(
                        ingredients = updatedIngredients,
                        ingredientsByServings = scaleIngredients(
                            ingredients = updatedIngredients,
                            recipeServings = state.recipeServings,
                            selectedServings = state.servings.toDoubleOrNull() ?: state.recipeServings.toDouble(),
                        ),
                        ingredientDraft = RecipeIngredientDraftState(),
                        editingIngredientId = null,
                    )
                }
            }
        }
    }

    private fun addInstruction(value: String) {
        updateContent { state ->
            if (!state.editMode) {
                state
            } else {
                val trimmedValue = value.trim()
                if (trimmedValue.isBlank()) {
                    return@updateContent state
                }
                val updatedInstructions = if (state.editingInstructionId != null) {
                    state.instructions.map { current ->
                        if (current.id == state.editingInstructionId) current.copy(itemName = trimmedValue) else current
                    }
                } else {
                    val nextSortOrder = (state.instructions.maxOfOrNull { it.sortOrder } ?: -1) + 1
                    state.instructions + Instruction(itemName = trimmedValue, sortOrder = nextSortOrder)
                }
                state.copy(
                    instructions = updatedInstructions,
                    instructionDraft = "",
                    editingInstructionId = null,
                )
            }
        }
    }

    private fun moveIngredient(fromIndex: Int, toIndex: Int) {
        updateContent { state ->
            if (!state.editMode) {
                state
            } else {
                val updatedIngredients = state.ingredients.moveAndReindexIngredients(fromIndex, toIndex)
                state.copy(
                    ingredients = updatedIngredients,
                    ingredientsByServings = scaleIngredients(
                        ingredients = updatedIngredients,
                        recipeServings = state.recipeServings,
                        selectedServings = state.servings.toDoubleOrNull() ?: state.recipeServings.toDouble(),
                    ),
                )
            }
        }
    }

    private fun moveInstruction(fromIndex: Int, toIndex: Int) {
        updateContent { state ->
            if (!state.editMode) {
                state
            } else {
                state.copy(
                    instructions = state.instructions.moveAndReindexInstructions(fromIndex, toIndex),
                )
            }
        }
    }

    private fun startEditingIngredient(ingredientId: String) {
        updateContent { state ->
            val ingredient = state.ingredients.firstOrNull { it.id == ingredientId } ?: return@updateContent state
            state.copy(
                editingIngredientId = ingredientId,
                editingInstructionId = null,
                ingredientDraft = RecipeIngredientDraftState(
                    itemName = ingredient.itemName,
                    amount = ingredient.amount.toString(),
                    measureType = ingredient.measureType,
                ),
                instructionDraft = "",
            )
        }
    }

    private fun startEditingInstruction(instructionId: String) {
        updateContent { state ->
            val instruction = state.instructions.firstOrNull { it.id == instructionId } ?: return@updateContent state
            state.copy(
                editingInstructionId = instructionId,
                editingIngredientId = null,
                instructionDraft = instruction.itemName,
                ingredientDraft = RecipeIngredientDraftState(),
            )
        }
    }

    private fun updateIngredientDraft(transform: (RecipeIngredientDraftState) -> RecipeIngredientDraftState) {
        updateContent { state ->
            state.copy(ingredientDraft = transform(state.ingredientDraft))
        }
    }

    private fun clearIngredientDraft() {
        updateContent { state ->
            state.copy(
                editingIngredientId = null,
                ingredientDraft = RecipeIngredientDraftState(),
            )
        }
    }

    private fun clearInstructionDraft() {
        updateContent { state ->
            state.copy(
                editingInstructionId = null,
                instructionDraft = "",
            )
        }
    }

    private fun updateContentAndIngredientsByServings(servings: String) {
        updateContent { state ->
            state.copy(
                servings = servings,
                ingredientsByServings = scaleIngredients(
                    ingredients = state.ingredients,
                    recipeServings = state.recipeServings,
                    selectedServings = servings.toDoubleOrNull() ?: state.recipeServings.toDouble(),
                ),
            )
        }
    }

    private fun saveRecipe() {
        val state = contentState() ?: return
        val trimmedName = state.itemName.trim()

        if (trimmedName.isEmpty()) {
            showError("Please write some text first")
            return
        }

        val familyId = state.familyId
        if (familyId.isNullOrBlank()) {
            showError("Please connect to a family first")
            return
        }

        val original = originalRecipe ?: blankRecipe(
            recipeId = state.recipeId.orEmpty(),
            familyId = familyId,
        )
        val trimmedDescription = state.description.trim()
        val sanitizedIngredients = state.ingredients
            .mapIndexedNotNull { index, ingredient ->
                val itemName = ingredient.itemName.trim()
                if (itemName.isBlank() || ingredient.amount < 0.0) {
                    null
                } else {
                    ingredient.copy(itemName = itemName, sortOrder = index)
                }
            }
        val sanitizedInstructions = state.instructions
            .mapIndexedNotNull { index, instruction ->
                val itemName = instruction.itemName.trim()
                if (itemName.isBlank()) {
                    null
                } else {
                    instruction.copy(itemName = itemName, sortOrder = index)
                }
            }
        val sanitizedTags = state.tagsInput
            .trim()
            .split(Regex("\\s+"))
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
        val recipe = Recipe(
            id = state.recipeId.takeIf { !it.isNullOrBlank() } ?: UUID.randomUUID().toString(),
            familyId = familyId,
            itemName = trimmedName,
            description = trimmedDescription,
            ingredients = sanitizedIngredients,
            instructions = sanitizedInstructions,
            preparationTimeMin = state.preparationTimeMin.toIntOrNull() ?: original.preparationTimeMin,
            favourite = state.favourite,
            servings = state.servings.toIntOrNull() ?: original.servings,
            tags = sanitizedTags.ifEmpty { original.tags },
            imageUrl = original.imageUrl,
        )

        updateContent { it.copy(isSaving = true) }
        viewModelScope.launch {
            when {
                state.recipeId.isNullOrBlank() -> {
                    when (val result = recipeRepository.saveRecipe(recipe)) {
                        is Result.Success -> {
                            val savedRecipeId = result.data
                            pendingRecipeId = savedRecipeId
                            originalRecipe = recipe.copy(
                                id = savedRecipeId,
                                lastUpdated = recipe.lastUpdated,
                            )
                            finishSaveAfterRecipeSaved(
                                recipeId = savedRecipeId,
                                familyId = familyId,
                                savedRecipe = recipe.copy(id = savedRecipeId),
                                navigateBack = true,
                            )
                        }

                        is Result.Failure -> {
                            updateContent { it.copy(isSaving = false) }
                            showError(result.error.toUserMessage())
                        }
                    }
                }

                else -> {
                    when (val result = recipeRepository.updateRecipe(recipe)) {
                        is Result.Success -> {
                            originalRecipe = recipe
                            finishSaveAfterRecipeSaved(
                                recipeId = recipe.id,
                                familyId = familyId,
                                savedRecipe = recipe,
                                navigateBack = false,
                            )
                        }

                        is Result.Failure -> {
                            updateContent { it.copy(isSaving = false) }
                            showError(result.error.toUserMessage())
                        }
                    }
                }
            }
        }
    }

    private fun discardChanges() {
        val content = contentState() ?: return
        if (hasUnsavedChanges(content)) {
            updateContent { it.copy(showDiscardConfirmationDialog = true) }
            return
        }

        performDiscardChanges()
    }

    private fun dismissDiscardConfirmation() {
        updateContent { it.copy(showDiscardConfirmationDialog = false) }
    }

    private fun performDiscardChanges() {
        val content = contentState() ?: return
        viewModelScope.launch {
            if (content.recipeId.isNullOrBlank()) {
                clearPendingRecipeImage()
                updateContent { it.copy(showDiscardConfirmationDialog = false) }
                _commands.send(RecipeDetailsCommand.NavigateBack)
                return@launch
            }

            originalRecipe?.let { restoreRecipe(it) } ?: run {
                clearPendingRecipeImage()
                updateContent {
                    it.copy(
                        editMode = false,
                        showDiscardConfirmationDialog = false,
                        editingIngredientId = null,
                        editingInstructionId = null,
                        ingredientDraft = RecipeIngredientDraftState(),
                        instructionDraft = "",
                    )
                }
            }
        }
    }

    private fun hasUnsavedChanges(content: RecipeDetailsUiState.Content): Boolean {
        val original = originalRecipe
        if (original == null) {
            return content.itemName.isNotBlank() ||
                content.description.isNotBlank() ||
                content.ingredients.isNotEmpty() ||
                content.instructions.isNotEmpty() ||
                content.preparationTimeMin.isNotBlank() ||
                content.favourite ||
                content.servings.isNotBlank() ||
                content.tagsInput.isNotBlank() ||
                isIngredientDraftDirty(content) ||
                isInstructionDraftDirty(content) ||
                pendingImageUri != null
        }

        return content.itemName != original.itemName ||
            content.description != original.description ||
            content.ingredients != original.ingredients ||
            content.instructions != original.instructions ||
            content.preparationTimeMin != original.preparationTimeMin.toString() ||
            content.favourite != original.favourite ||
            content.servings != original.servings.toString() ||
            content.tagsInput != original.tags.joinToString(" ") ||
            isIngredientDraftDirty(content) ||
            isInstructionDraftDirty(content) ||
            pendingImageUri != null
    }

    private fun isIngredientDraftDirty(content: RecipeDetailsUiState.Content): Boolean {
        val draft = content.ingredientDraft
        val editingId = content.editingIngredientId
        return if (editingId == null) {
            draft.itemName.isNotBlank() ||
                draft.amount.isNotBlank() ||
                draft.measureType != MeasureType.PIECE
        } else {
            val currentIngredient = content.ingredients.firstOrNull { it.id == editingId } ?: return draft.itemName.isNotBlank() ||
                draft.amount.isNotBlank() ||
                draft.measureType != MeasureType.PIECE
            draft.itemName != currentIngredient.itemName ||
                draft.amount != currentIngredient.amount.toString() ||
                draft.measureType != currentIngredient.measureType
        }
    }

    private fun isInstructionDraftDirty(content: RecipeDetailsUiState.Content): Boolean {
        val draft = content.instructionDraft
        val editingId = content.editingInstructionId
        return if (editingId == null) {
            draft.isNotBlank()
        } else {
            val currentInstruction = content.instructions.firstOrNull { it.id == editingId } ?: return draft.isNotBlank()
            draft != currentInstruction.itemName
        }
    }

    private fun requestIngredientDelete(ingredientId: String) {
        updateContent { state ->
            state.copy(
                deleteConfirmationTarget = RecipeDeleteConfirmationTarget.Ingredient(ingredientId),
            )
        }
    }

    private fun requestInstructionDelete(instructionId: String) {
        updateContent { state ->
            state.copy(
                deleteConfirmationTarget = RecipeDeleteConfirmationTarget.Instruction(instructionId),
            )
        }
    }

    private fun confirmDelete() {
        val target = contentState()?.deleteConfirmationTarget ?: return
        when (target) {
            RecipeDeleteConfirmationTarget.Recipe -> deleteRecipe()
            is RecipeDeleteConfirmationTarget.Ingredient -> deleteIngredient(target.ingredientId)
            is RecipeDeleteConfirmationTarget.Instruction -> deleteInstruction(target.instructionId)
        }
    }

    private fun deleteRecipe() {
        val recipeId = contentState()?.recipeId?.takeIf { it.isNotBlank() } ?: run {
            showError("Recipe not saved - no id")
            return
        }

        viewModelScope.launch {
            when (val result = recipeRepository.deleteRecipe(recipeId)) {
                is Result.Success -> Unit
                is Result.Failure -> showError(result.error.toUserMessage())
            }
            updateContent {
                it.copy(
                    deleteConfirmationTarget = null,
                )
            }
        }
    }

    private fun deleteIngredient(ingredientId: String) {
        updateContent { state ->
            val updatedIngredients = state.ingredients.filterNot { it.id == ingredientId }
            state.copy(
                ingredients = updatedIngredients,
                ingredientsByServings = scaleIngredients(
                    ingredients = updatedIngredients,
                    recipeServings = state.recipeServings,
                    selectedServings = state.servings.toDoubleOrNull() ?: state.recipeServings.toDouble(),
                ),
                editingIngredientId = if (state.editingIngredientId == ingredientId) null else state.editingIngredientId,
                ingredientDraft = if (state.editingIngredientId == ingredientId) RecipeIngredientDraftState() else state.ingredientDraft,
                deleteConfirmationTarget = null,
            )
        }
    }

    private fun deleteInstruction(instructionId: String) {
        updateContent { state ->
            val updatedInstructions = state.instructions.filterNot { it.id == instructionId }
            state.copy(
                instructions = updatedInstructions,
                editingInstructionId = if (state.editingInstructionId == instructionId) null else state.editingInstructionId,
                instructionDraft = if (state.editingInstructionId == instructionId) "" else state.instructionDraft,
                deleteConfirmationTarget = null,
            )
        }
    }

    private fun scaleIngredients(
        ingredients: List<Ingredient>,
        recipeServings: Int,
        selectedServings: Double,
    ): List<Ingredient> {
        if (recipeServings <= 0) {
            return ingredients
        }

        val multiplier = selectedServings / recipeServings.toDouble()
        return ingredients.map { ingredient ->
            ingredient.copy(amount = ingredient.amount * multiplier)
        }
    }

    private fun List<Ingredient>.moveAndReindexIngredients(fromIndex: Int, toIndex: Int): List<Ingredient> {
        if (isEmpty()) return this
        if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) {
            return mapIndexed { index, ingredient -> ingredient.copy(sortOrder = index) }
        }
        val mutable = toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return mutable.mapIndexed { index, ingredient -> ingredient.copy(sortOrder = index) }
    }

    private fun List<Instruction>.moveAndReindexInstructions(fromIndex: Int, toIndex: Int): List<Instruction> {
        if (isEmpty()) return this
        if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) {
            return mapIndexed { index, instruction -> instruction.copy(sortOrder = index) }
        }
        val mutable = toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return mutable.mapIndexed { index, instruction -> instruction.copy(sortOrder = index) }
    }

    private fun blankRecipe(recipeId: String, familyId: String): Recipe {
        return Recipe(
            id = recipeId,
            familyId = familyId,
            itemName = "",
            description = "",
            ingredients = emptyList(),
            instructions = emptyList(),
            preparationTimeMin = 0,
            favourite = false,
            servings = 1,
            tags = emptyList(),
        )
    }

    private fun setPendingRecipeImage(uri: Uri) {
        pendingImageUri = uri
        savedStateHandle[PENDING_IMAGE_URI_ARG] = uri.toString()

        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                uri.toBitmap(context.contentResolver)
            } ?: run {
                clearPendingRecipeImage()
                showError("Could not load the selected image")
                return@launch
            }

            if (pendingImageUri == uri) {
                updateContent { it.copy(localImageBitmap = bitmap) }
            }
        }
    }

    private fun restorePendingImagePreview() {
        val uri = pendingImageUri ?: return
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                uri.toBitmap(context.contentResolver)
            } ?: run {
                clearPendingRecipeImage()
                return@launch
            }

            if (pendingImageUri == uri) {
                updateContent { it.copy(localImageBitmap = bitmap) }
            }
        }
    }

    private fun clearPendingRecipeImage() {
        pendingImageUri = null
        savedStateHandle[PENDING_IMAGE_URI_ARG] = null
        updateContent { it.copy(localImageBitmap = null) }
    }

    private suspend fun uploadRecipeImage(
        recipeId: String,
        familyId: String,
        uri: Uri,
    ): Result<Unit, AppError> {
        return when (val result = uploadImageUseCase.invoke(
            uri = uri,
            imageType = ImageType.RecipeImage(familyId, recipeId),
            context = context,
        )) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(result.error)
        }
    }

    private suspend fun finishSaveAfterRecipeSaved(
        recipeId: String,
        familyId: String,
        savedRecipe: Recipe,
        navigateBack: Boolean,
    ) {
        val imageUri = pendingImageUri
        if (imageUri != null) {
            when (val uploadResult = uploadRecipeImage(
                recipeId = recipeId,
                familyId = familyId,
                uri = imageUri,
            )) {
                is Result.Success -> {
                    clearPendingRecipeImage()
                    if (navigateBack) {
                        _commands.send(RecipeDetailsCommand.NavigateBack)
                    } else {
                        updateContent { state ->
                            state.copy(
                                recipeId = recipeId,
                                familyId = familyId,
                                itemName = savedRecipe.itemName,
                                description = savedRecipe.description,
                                ingredients = savedRecipe.ingredients,
                                instructions = savedRecipe.instructions,
                                preparationTimeMin = savedRecipe.preparationTimeMin.toString(),
                                favourite = savedRecipe.favourite,
                                recipeServings = savedRecipe.servings,
                                servings = savedRecipe.servings.toString(),
                                tagsInput = savedRecipe.tags.joinToString(" "),
                                tags = savedRecipe.tags,
                                isSaving = false,
                                editMode = false,
                                showDiscardConfirmationDialog = false,
                                deleteConfirmationTarget = null,
                                servingsExpanded = false,
                                editingIngredientId = null,
                                editingInstructionId = null,
                                ingredientDraft = RecipeIngredientDraftState(),
                                instructionDraft = "",
                                ingredientsByServings = scaleIngredients(
                                    ingredients = savedRecipe.ingredients,
                                    recipeServings = savedRecipe.servings,
                                    selectedServings = savedRecipe.servings.toDouble(),
                                ),
                            )
                        }
                    }
                }

                is Result.Failure -> {
                    updateContent { it.copy(isSaving = false) }
                    showError(uploadResult.error.toUserMessage())
                }
            }
        } else {
            updateContent { state ->
                state.copy(
                    recipeId = recipeId,
                    familyId = familyId,
                    itemName = savedRecipe.itemName,
                    description = savedRecipe.description,
                    ingredients = savedRecipe.ingredients,
                    instructions = savedRecipe.instructions,
                    preparationTimeMin = savedRecipe.preparationTimeMin.toString(),
                    favourite = savedRecipe.favourite,
                    recipeServings = savedRecipe.servings,
                    servings = savedRecipe.servings.toString(),
                    tagsInput = savedRecipe.tags.joinToString(" "),
                    tags = savedRecipe.tags,
                    isSaving = false,
                    editMode = false,
                    showDiscardConfirmationDialog = false,
                    deleteConfirmationTarget = null,
                    servingsExpanded = false,
                    editingIngredientId = null,
                    editingInstructionId = null,
                    ingredientDraft = RecipeIngredientDraftState(),
                    instructionDraft = "",
                    ingredientsByServings = scaleIngredients(
                        ingredients = savedRecipe.ingredients,
                        recipeServings = savedRecipe.servings,
                        selectedServings = savedRecipe.servings.toDouble(),
                    ),
                )
            }
            if (navigateBack) {
                _commands.send(RecipeDetailsCommand.NavigateBack)
            }
        }
    }

    private fun contentState(): RecipeDetailsUiState.Content? {
        return _uiState.value as? RecipeDetailsUiState.Content
    }

    private fun updateContent(transform: (RecipeDetailsUiState.Content) -> RecipeDetailsUiState.Content) {
        _uiState.update { state ->
            (state as? RecipeDetailsUiState.Content)?.let(transform) ?: state
        }
    }

    private fun addIngredientToGroceryList(ingredient: Ingredient) {
        val familyId = currentFamilyId ?: return
        val content = contentState() ?: return
        val matchingSuggestion = content.grocerySuggestions.find { suggestion ->
            ingredientMatchesSuggestion(ingredient.itemName, suggestion.suggestionName)
        } ?: return

        viewModelScope.launch {
            val groceryResult = groceryRepository.observeGroceryItems(familyId).first()
            if (groceryResult is Result.Success) {
                val alreadyOnList = groceryResult.data
                    .filter { !it.completed }
                    .any { item ->
                        ingredientMatchesSuggestion(item.itemName, matchingSuggestion.suggestionName) &&
                            item.category == matchingSuggestion.category
                    }
                if (alreadyOnList) {
                    _uiCommands.send(
                        UiCommand.ShowSnackbar(
                            message = "${matchingSuggestion.suggestionName} is already on your grocery list",
                            severity = SnackbarSeverity.Info,
                        ),
                    )
                    return@launch
                }
            }

            val newItem = GroceryItem(
                id = UUID.randomUUID().toString(),
                familyId = familyId,
                itemName = matchingSuggestion.suggestionName,
                category = matchingSuggestion.category,
                approxPrice = matchingSuggestion.approxPrice,
            )
            when (val result = groceryRepository.saveGroceryItem(newItem)) {
                is Result.Success -> _uiCommands.send(
                    UiCommand.ShowSnackbar(
                        message = "${matchingSuggestion.suggestionName} added to grocery list",
                        severity = SnackbarSeverity.Info,
                    ),
                )
                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun showError(message: String) {
        viewModelScope.launch {
            _uiCommands.send(
                UiCommand.ShowSnackbar(
                    message = message,
                    withDismissAction = true,
                ),
            )
        }
    }
}
