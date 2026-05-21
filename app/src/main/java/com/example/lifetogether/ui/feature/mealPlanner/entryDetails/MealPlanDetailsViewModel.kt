package com.example.lifetogether.ui.feature.mealPlanner.entryDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.repository.RecipeRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.ui.common.event.UiCommand
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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

@HiltViewModel(assistedFactory = MealPlanDetailsViewModel.Factory::class)
class MealPlanDetailsViewModel @AssistedInject constructor(
    @Assisted("mealPlanId") val mealPlanId: String?,
    @Assisted("defaultDate") val defaultDate: String?,
    @Assisted("preselectedRecipeId") val preselectedRecipeId: String?,
    private val loader: MealPlanDetailsLoader,
    private val saver: MealPlanDetailsSaver,
    private val recipeRepository: RecipeRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("mealPlanId") mealPlanId: String?,
            @Assisted("defaultDate") defaultDate: String?,
            @Assisted("preselectedRecipeId") preselectedRecipeId: String?,
        ): MealPlanDetailsViewModel
    }

    private val _familyId = MutableStateFlow<String?>(null)
    val familyId = _familyId.asStateFlow()

    private var observeRecipesJob: Job? = null
    private var observedRecipesFamilyId: String? = null
    private var allRecipeSearchItems: List<RecipeSearchItem> = emptyList()
    private var originalForm: MealPlanFormState? = null

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
                    is MealPlanDetailsLoadState.Content -> showContent(state.form, state.isNewEntry)
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
            is MealPlanDetailsUiEvent.Meal.NotesChanged -> updateForm { reduce(it, event) }
        }
    }

    private fun confirmDiscard() {
        if (mealPlanId == null) {
            viewModelScope.launch { _commands.send(MealPlanDetailsCommand.NavigateBack) }
            return
        }
        val original = originalForm ?: return
        updateContent {
            it.copy(
                form = original,
                mealRecipeSearchState = buildMealRecipeSearchState(form = original, currentState = it.mealRecipeSearchState),
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
        val form = content.form
        val activeFamilyId = _familyId.value ?: return showError("Missing family context")
        val now = Date()

        updateContent { it.copy(isSaving = true) }

        viewModelScope.launch {
            val result = saver.save(
                form = form,
                mealRecipeSearchState = content.mealRecipeSearchState,
                mealPlanId = mealPlanId,
                familyId = activeFamilyId,
                now = now,
            )

            updateContent { it.copy(isSaving = false) }

            when (result) {
                is Result.Success -> {
                    originalForm = currentContentState()?.form
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
        originalForm = null
        _uiState.value = MealPlanDetailsUiState.Loading
    }

    private fun showContent(form: MealPlanFormState, isNewEntry: Boolean) {
        val effectiveForm = if (isNewEntry) {
            form.copy(
                date = if (form.date.isBlank() && !defaultDate.isNullOrBlank()) defaultDate else form.date,
                recipeId = if (!preselectedRecipeId.isNullOrBlank()) preselectedRecipeId else form.recipeId,
            )
        } else form

        originalForm = effectiveForm
        _uiState.update { current ->
            when (current) {
                is MealPlanDetailsUiState.Content -> {
                    observeRecipes()
                    current.copy(
                        form = effectiveForm,
                        mealRecipeSearchState = buildMealRecipeSearchState(effectiveForm, current.mealRecipeSearchState),
                    )
                }
                MealPlanDetailsUiState.Loading -> {
                    observeRecipes()
                    MealPlanDetailsUiState.Content(
                        form = effectiveForm,
                        mealRecipeSearchState = buildMealRecipeSearchState(effectiveForm, null),
                        isEditing = isNewEntry,
                        showDiscardDialog = false,
                        showDeleteDialog = false,
                        isSaving = false,
                    )
                }
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
        updateMealContent { form, state ->
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
            form to state.copy(
                query = trimmed,
                selectedRecipeSearchItem = selectedRecipe,
                suggestions = suggestions,
            )
        }
    }

    private fun selectRecipe(recipe: RecipeSearchItem) {
        updateMealContent { form, _ ->
            form.copy(recipeId = recipe.id, customMealName = null) to MealRecipeSearchState(
                mode = MealSearchMode.RECIPE,
                query = recipe.itemName,
                isSearchFocused = false,
                selectedRecipeSearchItem = recipe,
                suggestions = emptyList(),
            )
        }
    }

    private fun updateMealMode(mode: MealSearchMode) {
        updateMealContent { form, state ->
            val updatedForm = form.copy(
                recipeId = if (mode == MealSearchMode.RECIPE) form.recipeId else null,
                customMealName = if (mode == MealSearchMode.CUSTOM) form.customMealName else null,
            )
            val updatedState = if (mode == MealSearchMode.RECIPE) {
                buildMealRecipeSearchState(updatedForm, state.copy(mode = mode))
            } else {
                state.copy(
                    mode = mode,
                    query = "",
                    selectedRecipeSearchItem = null,
                    suggestions = emptyList(),
                    isSearchFocused = false,
                )
            }
            updatedForm to updatedState
        }
    }

    private fun updateMealCustomName(value: String) {
        updateForm { form -> form.copy(customMealName = value, recipeId = null) }
    }

    private fun updateForm(block: (MealPlanFormState) -> MealPlanFormState) {
        _uiState.update { current ->
            if (current is MealPlanDetailsUiState.Content) {
                current.copy(form = block(current.form))
            } else {
                current
            }
        }
        updateMealSearchStateFromCurrentContent()
    }

    private fun updateMealContent(
        block: (MealPlanFormState, MealRecipeSearchState) -> Pair<MealPlanFormState, MealRecipeSearchState>,
    ) {
        _uiState.update { current ->
            if (current is MealPlanDetailsUiState.Content) {
                val (updatedForm, updatedSearchState) = block(current.form, current.mealRecipeSearchState)
                current.copy(form = updatedForm, mealRecipeSearchState = updatedSearchState)
            } else {
                current
            }
        }
    }

    private fun reduce(form: MealPlanFormState, event: MealPlanDetailsUiEvent): MealPlanFormState {
        return when (event) {
            is MealPlanDetailsUiEvent.Meal.DateChanged -> form.copy(date = event.value)
            is MealPlanDetailsUiEvent.Meal.MealTypeChanged -> form.copy(
                mealType = com.example.lifetogether.domain.model.lists.MealType.fromDisplayName(event.value) ?: form.mealType,
            )
            is MealPlanDetailsUiEvent.Meal.NotesChanged -> form.copy(notes = event.value)
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
            MealPlanDetailsUiEvent.SaveClicked -> form
        }
    }

    private fun buildMealRecipeSearchState(
        form: MealPlanFormState,
        currentState: MealRecipeSearchState?,
    ): MealRecipeSearchState {
        val selectedRecipeId = form.recipeId?.takeIf { it.isNotBlank() }
        val selectedRecipe = selectedRecipeId?.let { id -> allRecipeSearchItems.firstOrNull { it.id == id } }
        val mode = currentState?.mode ?: when {
            !form.customMealName.isNullOrBlank() -> MealSearchMode.CUSTOM
            selectedRecipeId != null -> MealSearchMode.RECIPE
            else -> MealSearchMode.RECIPE
        }
        val query = currentState?.query?.takeIf { it.isNotBlank() } ?: selectedRecipe?.itemName.orEmpty()
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
        if (current is MealPlanDetailsUiState.Content) {
            _uiState.update {
                current.copy(
                    mealRecipeSearchState = buildMealRecipeSearchState(
                        form = current.form,
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
        val original = originalForm ?: return false
        return content.form != original
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
