package com.example.lifetogether.ui.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.Completable
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.domain.model.recipe.Instruction
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.model.toggleCompleted
import com.example.lifetogether.domain.repository.RecipeRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.ui.common.event.UiCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class RecipeDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val recipeRepository: RecipeRepository,
) : ViewModel() {
    companion object {
        private const val RECIPE_ID_ARG = "recipeId"
    }

    private val _uiState = MutableStateFlow<RecipeDetailsUiState>(RecipeDetailsUiState.Loading)
    val uiState: StateFlow<RecipeDetailsUiState> = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    private val _commands = Channel<RecipeDetailsCommand>(Channel.BUFFERED)
    val commands: Flow<RecipeDetailsCommand> = _commands.receiveAsFlow()

    private var pendingRecipeId: String? = savedStateHandle[RECIPE_ID_ARG]
    private var currentFamilyId: String? = null
    private var originalRecipe: Recipe? = null
    private var observeRecipeJob: Job? = null

    init {
        if (pendingRecipeId == null) {
            _uiState.value = createContentState(editMode = true)
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
            RecipeDetailsUiEvent.EditClicked -> toggleEditMode()
            is RecipeDetailsUiEvent.ItemNameChanged -> updateContent {
                it.copy(itemName = event.value)
            }

            is RecipeDetailsUiEvent.DescriptionChanged -> updateContent {
                it.copy(description = event.value)
            }

            is RecipeDetailsUiEvent.PreparationTimeChanged -> updateContent {
                it.copy(preparationTimeMin = event.value)
            }

            is RecipeDetailsUiEvent.ServingsChanged -> updateContentAndIngredientsByServings(event.value)
            is RecipeDetailsUiEvent.ServingsExpandedChanged -> updateContent {
                it.copy(servingsExpanded = event.value)
            }

            is RecipeDetailsUiEvent.TagsChanged -> updateContent {
                it.copy(tagsInput = event.value)
            }

            RecipeDetailsUiEvent.ToggleIngredientsExpanded -> toggleExpandedState("ingredients")
            RecipeDetailsUiEvent.ToggleInstructionsExpanded -> toggleExpandedState("instructions")
            is RecipeDetailsUiEvent.IngredientCompletedToggled -> toggleIngredientCompletion(event.ingredient)
            is RecipeDetailsUiEvent.InstructionCompletedToggled -> toggleInstructionCompletion(event.instruction)
            is RecipeDetailsUiEvent.AddIngredientClicked -> addIngredient(event.ingredient)
            is RecipeDetailsUiEvent.AddInstructionClicked -> addInstruction(event.value)
            RecipeDetailsUiEvent.AddImageClicked -> openImageUploadDialog()
            RecipeDetailsUiEvent.ImageUploadDismissed,
            RecipeDetailsUiEvent.ImageUploadConfirmed -> updateContent {
                it.copy(showImageUploadDialog = false)
            }

            RecipeDetailsUiEvent.DeleteClicked -> updateContent {
                if (it.recipeId.isNullOrBlank()) {
                    it
                } else {
                    it.copy(showDeleteConfirmationDialog = true)
                }
            }

            RecipeDetailsUiEvent.DismissDeleteConfirmation -> updateContent {
                it.copy(showDeleteConfirmationDialog = false)
            }

            RecipeDetailsUiEvent.ConfirmDeleteConfirmation -> deleteRecipe()
            RecipeDetailsUiEvent.SaveClicked -> saveRecipe()
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

                    is Result.Failure -> showError(result.error.toUserMessage())
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
        val sourceRecipe = recipe ?: originalRecipe ?: Recipe(
            id = recipeId,
            familyId = familyId.orEmpty(),
        )

        val servings = sourceRecipe.servings.toString()
        val ingredients = sourceRecipe.ingredients

        return RecipeDetailsUiState.Content(
            recipeId = recipeId ?: sourceRecipe.id,
            familyId = familyId,
            itemName = sourceRecipe.itemName,
            description = sourceRecipe.description,
            ingredients = ingredients,
            instructions = sourceRecipe.instructions,
            preparationTimeMin = sourceRecipe.preparationTimeMin.toString(),
            favourite = sourceRecipe.favourite,
            recipeServings = sourceRecipe.servings,
            servings = if (recipe == null && recipeId == null) "" else servings,
            tagsInput = sourceRecipe.tags.joinToString(" "),
            tags = sourceRecipe.tags,
            editMode = editMode,
            showDeleteConfirmationDialog = false,
            showImageUploadDialog = false,
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
            updateContent { it.copy(editMode = true) }
        }
    }

    private fun restoreRecipe(recipe: Recipe) {
        _uiState.value = createContentState(
            recipe = recipe,
            familyId = currentFamilyId,
            recipeId = pendingRecipeId,
            editMode = false,
        )
    }

    private fun openImageUploadDialog() {
        val content = contentState() ?: return
        if (content.recipeId.isNullOrBlank() || content.familyId.isNullOrBlank()) {
            return
        }
        updateContent {
            it.copy(showImageUploadDialog = true)
        }
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
                val updatedIngredients = state.ingredients.toggleCompleted(ingredient.itemName)
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
                    ingredientsByServings = state.ingredientsByServings.toggleCompleted(ingredient.itemName),
                )
            }
        }
    }

    private fun toggleInstructionCompletion(instruction: Completable) {
        updateContent {
            it.copy(
                instructions = it.instructions.toggleCompleted(instruction.itemName),
            )
        }
    }

    private fun addIngredient(ingredient: Ingredient) {
        updateContent { state ->
            if (!state.editMode) {
                state
            } else {
                val updatedIngredients = state.ingredients + ingredient
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

    private fun addInstruction(value: String) {
        updateContent { state ->
            if (!state.editMode) {
                state
            } else {
                state.copy(
                    instructions = state.instructions + Instruction(itemName = value),
                )
            }
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

        if (state.itemName.isEmpty()) {
            showError("Please write some text first")
            return
        }

        val familyId = state.familyId
        if (familyId.isNullOrBlank()) {
            showError("Please connect to a family first")
            return
        }

        val original = originalRecipe ?: Recipe(
            id = state.recipeId,
            familyId = familyId,
        )
        val recipe = Recipe(
            id = state.recipeId?.takeIf { it.isNotBlank() },
            familyId = familyId,
            itemName = state.itemName,
            lastUpdated = Date(),
            description = state.description,
            ingredients = state.ingredients,
            instructions = state.instructions,
            preparationTimeMin = state.preparationTimeMin.toIntOrNull() ?: original.preparationTimeMin,
            favourite = state.favourite,
            servings = state.servings.toIntOrNull() ?: original.servings,
            tags = if (state.tagsInput.isNotBlank()) {
                state.tagsInput.lowercase().split(" ")
            } else {
                original.tags
            },
        )

        viewModelScope.launch {
            when {
                recipe.id.isNullOrBlank() -> {
                    when (val result = recipeRepository.saveRecipe(recipe)) {
                        is Result.Success -> _commands.send(RecipeDetailsCommand.NavigateBack)
                        is Result.Failure -> showError(result.error.toUserMessage())
                    }
                }
                else -> {
                    when (val result = recipeRepository.updateRecipe(recipe)) {
                        is Result.Success -> _commands.send(RecipeDetailsCommand.NavigateBack)
                        is Result.Failure -> showError(result.error.toUserMessage())
                    }
                }
            }
        }
    }

    private fun deleteRecipe() {
        val recipeId = contentState()?.recipeId?.takeIf { it.isNotBlank() } ?: run {
            showError("Recipe not saved - no id")
            return
        }

        viewModelScope.launch {
            when (val result = recipeRepository.deleteRecipe(recipeId)) {
                is Result.Success -> _commands.send(RecipeDetailsCommand.NavigateBack)
                is Result.Failure -> showError(result.error.toUserMessage())
            }
            updateContent { it.copy(showDeleteConfirmationDialog = false) }
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

    private fun contentState(): RecipeDetailsUiState.Content? {
        return _uiState.value as? RecipeDetailsUiState.Content
    }

    private fun updateContent(transform: (RecipeDetailsUiState.Content) -> RecipeDetailsUiState.Content) {
        _uiState.update { state ->
            (state as? RecipeDetailsUiState.Content)?.let(transform) ?: state
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
