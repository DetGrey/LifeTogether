package com.example.lifetogether.ui.feature.mealPlanner.entryDetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.repository.RecipeRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.ui.common.event.UiCommand
import com.example.lifetogether.ui.navigation.MealPlanDetailsNavRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import androidx.navigation.toRoute

@HiltViewModel
class MealPlanDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val loader: MealPlanDetailsLoader,
    private val saver: MealPlanDetailsSaver,
    private val recipeRepository: RecipeRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<MealPlanDetailsNavRoute>()
    val mealPlanId: String? = route.mealPlanId
    private val defaultDate: String? = route.defaultDate

    private val _familyId = MutableStateFlow<String?>(null)
    val familyId = _familyId.asStateFlow()

    private var observeRecipesJob: Job? = null
    private var observedRecipesFamilyId: String? = null
    private var allRecipeSearchItems: List<RecipeSearchItem> = emptyList()
    private var originalDetails: MealPlanDetailsContent? = null

    private val _uiState = MutableStateFlow<MealPlanDetailsUiState>(MealPlanDetailsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    private val _commands = Channel<MealPlanDetailsCommand>(Channel.BUFFERED)
    val commands: Flow<MealPlanDetailsCommand> = _commands.receiveAsFlow()

    init {
        viewModelScope.launch {
            loader.observe(mealPlanId).collect { snapshot ->
                _familyId.value = snapshot.familyId
                when (val state = snapshot.state) {
                    MealPlanDetailsLoadState.Loading -> resetLoadingState()
                    is MealPlanDetailsLoadState.Content -> showContent(state.details, state.isNewEntry)
                    is MealPlanDetailsLoadState.Error -> showError(state.message)
                }
            }
        }
    }

    fun onUiEvent(event: MealPlanDetailsUiEvent) {
        when (event) {
            MealPlanDetailsUiEvent.EnterEditMode -> updateContent {
                it.copy(
                    isEditing = true,
                    showDiscardDialog = false,
                )
            }
            MealPlanDetailsUiEvent.RequestCancelEdit -> requestCancelEdit()
            MealPlanDetailsUiEvent.ConfirmDiscard -> confirmDiscard()
            MealPlanDetailsUiEvent.DismissDiscardDialog -> updateContent { it.copy(showDiscardDialog = false) }
            MealPlanDetailsUiEvent.RequestDeleteMealPlan -> updateContent { it.copy(showDeleteDialog = true) }
            MealPlanDetailsUiEvent.ConfirmDeleteMealPlan -> deleteMealPlan()
            MealPlanDetailsUiEvent.DismissDeleteDialog -> updateContent { it.copy(showDeleteDialog = false) }
            MealPlanDetailsUiEvent.SaveClicked -> saveMealPlan()
            is MealPlanDetailsUiEvent.Meal.RecipeQueryChanged -> updateMealRecipeQuery(event.value)
            is MealPlanDetailsUiEvent.Meal.RecipeSearchFocusedChanged -> updateMealContent { details, state ->
                details to state.copy(isSearchFocused = event.value)
            }
            is MealPlanDetailsUiEvent.Meal.RecipeSelected -> selectRecipe(event.recipe)
            is MealPlanDetailsUiEvent.Meal.RecipeModeChanged -> updateMealMode(event.mode)
            is MealPlanDetailsUiEvent.Meal.CustomMealNameChanged -> updateMealCustomName(event.value)
            is MealPlanDetailsUiEvent.Meal.DateChanged,
            is MealPlanDetailsUiEvent.Meal.MealTypeChanged,
            is MealPlanDetailsUiEvent.Meal.NotesChanged -> updateCurrentDetails { reduce(it, event) }
        }
    }

    private fun confirmDiscard() {
        if (mealPlanId == null) {
            viewModelScope.launch { _commands.send(MealPlanDetailsCommand.NavigateBack) }
            return
        }
        val original = originalDetails as? MealPlanDetailsContent.Meal ?: return
        updateContent {
            val restoredSearchState = buildMealRecipeSearchState(
                details = original,
                currentState = it.mealRecipeSearchState,
            )
            it.copy(
                details = original,
                mealRecipeSearchState = restoredSearchState,
                isEditing = false,
                showDiscardDialog = false,
                showDeleteDialog = false,
                isSaving = false,
            )
        }
    }

    private fun requestCancelEdit() {
        val content = currentContentState() ?: return
        if (hasUnsavedChanges(content)) {
            updateContent { it.copy(showDiscardDialog = true) }
        } else {
            confirmDiscard()
        }
    }

    private fun saveMealPlan() {
        val content = currentContentState() ?: return showError("Entry is not ready yet")
        val details = content.details as? MealPlanDetailsContent.Meal ?: return showError("Entry is not ready yet")
        val activeFamilyId = _familyId.value ?: return showError("Missing family context")
        val now = Date()

        updateContent { it.copy(isSaving = true) }

        viewModelScope.launch {
            val result = saver.save(
                details = details,
                mealRecipeSearchState = content.mealRecipeSearchState,
                mealPlanId = mealPlanId,
                familyId = activeFamilyId,
                now = now,
            )

            updateContent { it.copy(isSaving = false) }

            when (result) {
                is Result.Success -> {
                    originalDetails = currentContentState()?.details
                    if (mealPlanId != null) {
                        updateContent { it.copy(isEditing = false) }
                    }
                    _commands.send(MealPlanDetailsCommand.NavigateBack)
                }

                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun resetLoadingState() {
        originalDetails = null
        _uiState.value = MealPlanDetailsUiState.Loading
    }

    private fun showContent(details: MealPlanDetailsContent, isNewEntry: Boolean) {
        val effectiveDetails = if (isNewEntry && !defaultDate.isNullOrBlank()) {
            val meal = details as? MealPlanDetailsContent.Meal
            if (meal != null && meal.form.date.isBlank()) {
                meal.copy(form = meal.form.copy(date = defaultDate))
            } else details
        } else details
        originalDetails = effectiveDetails
        _uiState.update { current ->
            when (current) {
                is MealPlanDetailsUiState.Content -> {
                    var mealRecipeSearchState = MealRecipeSearchState()
                    if (current.details is MealPlanDetailsContent.Meal) {
                        observeRecipes()
                        mealRecipeSearchState = buildMealRecipeSearchState(
                            details = current.details,
                            currentState = current.mealRecipeSearchState,
                        )
                    }
                    current.copy(
                        details = effectiveDetails,
                        mealRecipeSearchState = mealRecipeSearchState,
                    )
                }
                MealPlanDetailsUiState.Loading -> MealPlanDetailsUiState.Content(
                    details = effectiveDetails,
                    mealRecipeSearchState = when (effectiveDetails) {
                        is MealPlanDetailsContent.Meal -> {
                            observeRecipes()
                            buildMealRecipeSearchState(effectiveDetails, null)
                        }
                    },
                    isEditing = isNewEntry,
                    showDiscardDialog = false,
                    showDeleteDialog = false,
                    isSaving = false,
                )
            }
        }
    }

    private fun deleteMealPlan() {
        val mealPlanIdValue = mealPlanId ?: return
        viewModelScope.launch {
            when (val result = saver.deleteMealPlan(mealPlanIdValue)) {
                is Result.Success -> _commands.send(MealPlanDetailsCommand.NavigateBack)
                is Result.Failure -> {
                    updateContent { it.copy(showDeleteDialog = false) }
                    showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun observeRecipes() {
        val familyIdValue = _familyId.value
        if (familyIdValue.isNullOrBlank()) {
            observedRecipesFamilyId = null
            observeRecipesJob?.cancel()
            allRecipeSearchItems = emptyList()
            updateMealSearchStateFromCurrentContent()
            return
        }

        if (observedRecipesFamilyId == familyIdValue && observeRecipesJob?.isActive == true) {
            return
        }

        observedRecipesFamilyId = familyIdValue
        observeRecipesJob?.cancel()
        observeRecipesJob = viewModelScope.launch {
            recipeRepository.observeRecipes(familyIdValue).collect { result ->
                allRecipeSearchItems = when (result) {
                    is Result.Success -> result.data.map { recipe ->
                        RecipeSearchItem(
                            id = recipe.id,
                            itemName = recipe.itemName,
                            preparationTimeMin = recipe.preparationTimeMin,
                        )
                    }
                    is Result.Failure -> {
                        showError(result.error.toUserMessage())
                        emptyList()
                    }
                }
                updateMealSearchStateFromCurrentContent()
            }
        }
    }

    private fun updateMealRecipeQuery(value: String) {
        updateMealContent { details, state ->
            val trimmed = value.trimStart()
            val selectedRecipe = if (trimmed.isBlank()) {
                null
            } else if (state.selectedRecipeSearchItem?.itemName.equals(trimmed, ignoreCase = true)) {
                state.selectedRecipeSearchItem
            } else {
                null
            }

            val suggestions = searchMealRecipeSuggestions(
                query = trimmed,
                suggestions = allRecipeSearchItems,
                selectedRecipeId = selectedRecipe?.id,
            )

            details to state.copy(
                query = trimmed,
                selectedRecipeSearchItem = selectedRecipe,
                suggestions = suggestions,
            )
        }
    }

    private fun selectRecipe(recipe: RecipeSearchItem) {
        updateMealContent { details, _ ->
            details.copy(
                form = details.form.copy(
                    recipeId = recipe.id,
                    customMealName = null,
                ),
            ) to MealRecipeSearchState(
                mode = MealSearchMode.RECIPE,
                query = recipe.itemName,
                isSearchFocused = false,
                selectedRecipeSearchItem = recipe,
                suggestions = emptyList(),
            )
        }
    }

    private fun updateMealMode(mode: MealSearchMode) {
        updateMealContent { details, state ->
            val updatedDetails = details.copy(
                form = details.form.copy(
                    recipeId = if (mode == MealSearchMode.RECIPE) details.form.recipeId else null,
                    customMealName = if (mode == MealSearchMode.CUSTOM) details.form.customMealName else null,
                ),
            )
            val updatedState = if (mode == MealSearchMode.RECIPE) {
                buildMealRecipeSearchState(updatedDetails, state.copy(mode = mode))
            } else {
                state.copy(
                    mode = mode,
                    query = "",
                    selectedRecipeSearchItem = null,
                    suggestions = emptyList(),
                    isSearchFocused = false,
                )
            }
            updatedDetails to updatedState
        }
    }

    private fun updateMealCustomName(value: String) {
        updateCurrentDetails { details ->
            val meal = details as? MealPlanDetailsContent.Meal ?: return@updateCurrentDetails details
            meal.copy(
                form = meal.form.copy(
                    customMealName = value,
                    recipeId = null,
                ),
            )
        }
    }

    private fun updateCurrentDetails(block: (MealPlanDetailsContent) -> MealPlanDetailsContent) {
        _uiState.update { current ->
            if (current is MealPlanDetailsUiState.Content) {
                current.copy(details = block(current.details))
            } else {
                current
            }
        }
        updateMealSearchStateFromCurrentContent()
    }

    private fun updateMealContent(
        block: (MealPlanDetailsContent.Meal, MealRecipeSearchState) -> Pair<MealPlanDetailsContent, MealRecipeSearchState>,
    ) {
        _uiState.update { current ->
            if (current is MealPlanDetailsUiState.Content) {
                val meal = current.details as? MealPlanDetailsContent.Meal ?: return@update current
                val (updatedDetails, updatedSearchState) = block(meal, current.mealRecipeSearchState)
                current.copy(
                    details = updatedDetails,
                    mealRecipeSearchState = updatedSearchState,
                )
            } else {
                current
            }
        }
    }

    private fun reduce(
        details: MealPlanDetailsContent,
        event: MealPlanDetailsUiEvent,
    ): MealPlanDetailsContent {
        val meal = details as? MealPlanDetailsContent.Meal ?: return details
        return when (event) {
            is MealPlanDetailsUiEvent.Meal.DateChanged -> meal.copy(
                form = meal.form.copy(date = event.value),
            )
            is MealPlanDetailsUiEvent.Meal.MealTypeChanged -> meal.copy(
                form = meal.form.copy(
                    mealType = com.example.lifetogether.domain.model.lists.MealType.fromDisplayName(event.value)
                        ?: meal.form.mealType,
                ),
            )
            is MealPlanDetailsUiEvent.Meal.NotesChanged -> meal.copy(
                form = meal.form.copy(notes = event.value),
            )
            is MealPlanDetailsUiEvent.Meal.RecipeQueryChanged,
            is MealPlanDetailsUiEvent.Meal.RecipeSearchFocusedChanged,
            is MealPlanDetailsUiEvent.Meal.RecipeSelected,
            is MealPlanDetailsUiEvent.Meal.RecipeModeChanged,
            is MealPlanDetailsUiEvent.Meal.CustomMealNameChanged,
            MealPlanDetailsUiEvent.EnterEditMode,
            MealPlanDetailsUiEvent.RequestCancelEdit,
            MealPlanDetailsUiEvent.ConfirmDiscard,
            MealPlanDetailsUiEvent.DismissDiscardDialog,
            MealPlanDetailsUiEvent.RequestDeleteMealPlan,
            MealPlanDetailsUiEvent.ConfirmDeleteMealPlan,
            MealPlanDetailsUiEvent.DismissDeleteDialog,
            MealPlanDetailsUiEvent.SaveClicked -> meal
        }
    }

    private fun buildMealRecipeSearchState(
        details: MealPlanDetailsContent.Meal,
        currentState: MealRecipeSearchState?,
    ): MealRecipeSearchState {
        val selectedRecipeId = details.form.recipeId?.takeIf { it.isNotBlank() }
        val selectedRecipe = selectedRecipeId?.let { recipeId ->
            allRecipeSearchItems.firstOrNull { it.id == recipeId }
        }
        val mode = currentState?.mode ?: when {
            !details.form.customMealName.isNullOrBlank() -> MealSearchMode.CUSTOM
            selectedRecipeId != null -> MealSearchMode.RECIPE
            else -> MealSearchMode.RECIPE
        }
        val query = currentState?.query?.takeIf { it.isNotBlank() }
            ?: selectedRecipe?.itemName.orEmpty()
        val selectedRecipeItem = selectedRecipe ?: currentState?.selectedRecipeSearchItem
        val suggestions = if (mode == MealSearchMode.RECIPE) {
            searchMealRecipeSuggestions(
                query = query,
                suggestions = allRecipeSearchItems,
                selectedRecipeId = selectedRecipeItem?.id,
            )
        } else {
            emptyList()
        }
        return MealRecipeSearchState(
            mode = mode,
            query = query,
            isSearchFocused = currentState?.isSearchFocused ?: false,
            selectedRecipeSearchItem = selectedRecipeItem,
            suggestions = suggestions,
        )
    }

    private fun updateMealSearchStateFromCurrentContent() {
        val current = _uiState.value
        if (current is MealPlanDetailsUiState.Content && current.details is MealPlanDetailsContent.Meal) {
            _uiState.update {
                current.copy(
                    mealRecipeSearchState = buildMealRecipeSearchState(
                        details = current.details,
                        currentState = current.mealRecipeSearchState,
                    ),
                )
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

    private fun currentContentState(): MealPlanDetailsUiState.Content? {
        return _uiState.value as? MealPlanDetailsUiState.Content
    }

    private fun hasUnsavedChanges(content: MealPlanDetailsUiState.Content): Boolean {
        val original = originalDetails ?: return false
        return content.details != original
    }

    private fun updateContent(block: (MealPlanDetailsUiState.Content) -> MealPlanDetailsUiState.Content) {
        _uiState.update { current ->
            if (current is MealPlanDetailsUiState.Content) {
                block(current)
            } else {
                current
            }
        }
    }
}
