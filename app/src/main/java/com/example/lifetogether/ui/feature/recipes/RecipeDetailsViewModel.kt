package com.example.lifetogether.ui.feature.recipes

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.logic.toBitmap
import com.example.lifetogether.domain.model.Completable
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.domain.model.recipe.Instruction
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.model.toggleCompleted
import com.example.lifetogether.domain.repository.RecipeRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.domain.usecase.image.UploadImageUseCase
import com.example.lifetogether.ui.common.event.UiCommand
import com.example.lifetogether.ui.navigation.RecipeDetailsNavRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import androidx.navigation.toRoute

@HiltViewModel
class RecipeDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val recipeRepository: RecipeRepository,
    private val uploadImageUseCase: UploadImageUseCase,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    companion object {
        private const val PENDING_IMAGE_URI_ARG = "pendingRecipeImageUri"
    }

    private val recipeRoute = savedStateHandle.toRoute<RecipeDetailsNavRoute>()

    private val _uiState = MutableStateFlow<RecipeDetailsUiState>(RecipeDetailsUiState.Loading)
    val uiState: StateFlow<RecipeDetailsUiState> = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    private val _commands = Channel<RecipeDetailsCommand>(Channel.BUFFERED)
    val commands: Flow<RecipeDetailsCommand> = _commands.receiveAsFlow()

    private var pendingRecipeId: String? = recipeRoute.recipeId
    private var pendingImageUri: Uri? = savedStateHandle.get<String>(PENDING_IMAGE_URI_ARG)?.let(Uri::parse)
    private var currentFamilyId: String? = null
    private var originalRecipe: Recipe? = null
    private var observeRecipeJob: Job? = null

    init {
        if (pendingRecipeId == null) {
            _uiState.value = createContentState(editMode = true)
        }

        if (pendingImageUri != null) {
            restorePendingImagePreview()
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
            is RecipeDetailsUiEvent.RecipeImageSelected -> setPendingRecipeImage(event.uri)
            RecipeDetailsUiEvent.DiscardClicked -> discardChanges()

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
            editMode = editMode,
            isSaving = false,
            showDeleteConfirmationDialog = false,
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
                val trimmedName = ingredient.itemName.trim()
                if (trimmedName.isBlank() || ingredient.amount < 0.0) {
                    state
                } else {
                    val sanitizedIngredient = ingredient.copy(itemName = trimmedName)
                    val updatedIngredients = state.ingredients + sanitizedIngredient
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
                state.copy(
                    instructions = state.instructions + Instruction(itemName = trimmedValue),
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
            .mapNotNull { ingredient ->
                val itemName = ingredient.itemName.trim()
                if (itemName.isBlank() || ingredient.amount <= 0.0) {
                    null
                } else {
                    ingredient.copy(itemName = itemName)
                }
            }
        val sanitizedInstructions = state.instructions
            .mapNotNull { instruction ->
                val itemName = instruction.itemName.trim()
                if (itemName.isBlank()) {
                    null
                } else {
                    instruction.copy(itemName = itemName)
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
            lastUpdated = Date(),
            description = trimmedDescription,
            ingredients = sanitizedIngredients,
            instructions = sanitizedInstructions,
            preparationTimeMin = state.preparationTimeMin.toIntOrNull() ?: original.preparationTimeMin,
            favourite = state.favourite,
            servings = state.servings.toIntOrNull() ?: original.servings,
            tags = sanitizedTags.ifEmpty { original.tags },
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
        viewModelScope.launch {
            if (content.recipeId.isNullOrBlank()) {
                _commands.send(RecipeDetailsCommand.NavigateBack)
                return@launch
            }

            originalRecipe?.let { restoreRecipe(it) } ?: run {
                clearPendingRecipeImage()
                updateContent {
                    it.copy(editMode = false)
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

    private fun blankRecipe(recipeId: String, familyId: String): Recipe {
        return Recipe(
            id = recipeId,
            familyId = familyId,
            itemName = "",
            lastUpdated = Date(),
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
                                showDeleteConfirmationDialog = false,
                                servingsExpanded = false,
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
                    showDeleteConfirmationDialog = false,
                    servingsExpanded = false,
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
