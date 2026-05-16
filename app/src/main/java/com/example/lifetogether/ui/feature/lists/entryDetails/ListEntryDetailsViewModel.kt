package com.example.lifetogether.ui.feature.lists.entryDetails

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.logic.toBitmap
import com.example.lifetogether.domain.repository.RecipeRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.ui.common.event.UiCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ListEntryDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentLoader: ListEntryDetailsLoader,
    private val formReducer: ListEntryDetailsFormReducer,
    private val entryDetailsSaver: ListEntryDetailsSaver,
    private val recipeRepository: RecipeRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    companion object {
        val WEEKDAYS: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    }

    private val listId: String = checkNotNull(savedStateHandle["listId"])
    val entryId: String? = savedStateHandle["entryId"]

    private val _familyId = MutableStateFlow<String?>(null)
    val familyId = _familyId.asStateFlow()

    private var observeRecipesJob: Job? = null
    private var observedRecipesFamilyId: String? = null
    private var allRecipeSearchItems: List<RecipeSearchItem> = emptyList()
    private var originalDetails: EntryDetailsContent? = null
    private var isDeletingMealPlanEntry = false

    private val _uiState = MutableStateFlow<EntryDetailsUiState>(EntryDetailsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    private val _commands = Channel<ListEntryDetailsCommand>(Channel.BUFFERED)
    val commands: Flow<ListEntryDetailsCommand> = _commands.receiveAsFlow()

    init {
        viewModelScope.launch {
            contentLoader.observe(listId, entryId).collect { snapshot ->
                if (isDeletingMealPlanEntry) {
                    return@collect
                }
                _familyId.value = snapshot.familyId
                when (val state = snapshot.state) {
                    ListEntryDetailsLoadState.Loading -> resetLoadingState()
                    is ListEntryDetailsLoadState.Content -> showContent(state.details, state.isNewEntry)
                    is ListEntryDetailsLoadState.Error -> showError(state.message)
                }
            }
        }
    }

    fun onUiEvent(event: ListEntryDetailsUiEvent) {
        when (event) {
            ListEntryDetailsUiEvent.EnterEditMode -> updateContent { it.copy(isEditing = true) }
            ListEntryDetailsUiEvent.RequestCancelEdit -> updateContent { it.copy(showDiscardDialog = true) }
            ListEntryDetailsUiEvent.ConfirmDiscard -> confirmDiscard()
            ListEntryDetailsUiEvent.DismissDiscardDialog -> updateContent { it.copy(showDiscardDialog = false) }
            ListEntryDetailsUiEvent.RequestDeleteEntry -> updateContent { it.copy(showDeleteDialog = true) }
            ListEntryDetailsUiEvent.ConfirmDeleteEntry -> deleteMealPlanEntry()
            ListEntryDetailsUiEvent.DismissDeleteDialog -> updateContent { it.copy(showDeleteDialog = false) }
            ListEntryDetailsUiEvent.SaveClicked -> saveEntry()
            is ListEntryDetailsUiEvent.Routine.ImageSelected -> onImageSelected(event.uri)
            is ListEntryDetailsUiEvent.Meal.RecipeQueryChanged -> updateMealRecipeQuery(event.value)
            is ListEntryDetailsUiEvent.Meal.RecipeSearchFocusedChanged -> updateMealContent { details, state ->
                details to state.copy(isSearchFocused = event.value)
            }
            is ListEntryDetailsUiEvent.Meal.RecipeSelected -> selectRecipe(event.recipe)
            is ListEntryDetailsUiEvent.Meal.RecipeModeChanged -> updateMealMode(event.mode)
            is ListEntryDetailsUiEvent.Meal.CustomMealNameChanged -> updateMealCustomName(event.value)
            is ListEntryDetailsUiEvent.Meal.DateChanged,
            is ListEntryDetailsUiEvent.Meal.MealTypeChanged,
            is ListEntryDetailsUiEvent.Meal.NotesChanged,
            is ListEntryDetailsUiEvent.NameChanged,
            is ListEntryDetailsUiEvent.Routine.RecurrenceUnitChanged,
            is ListEntryDetailsUiEvent.Routine.IntervalChanged,
            is ListEntryDetailsUiEvent.Routine.SelectedWeekdaysChanged,
            is ListEntryDetailsUiEvent.Wish.PurchasedChanged,
            is ListEntryDetailsUiEvent.Wish.UrlChanged,
            is ListEntryDetailsUiEvent.Wish.PriceChanged,
            is ListEntryDetailsUiEvent.Wish.CurrencyCodeChanged,
            is ListEntryDetailsUiEvent.Wish.PriorityChanged,
            is ListEntryDetailsUiEvent.Wish.NotesChanged,
            is ListEntryDetailsUiEvent.Note.BodyChanged -> updateCurrentDetails { formReducer.reduce(it, event) }
        }
    }

    fun confirmDiscard() {
        val original = originalDetails ?: return
        updateContent {
            val restoredSearchState = if (original is EntryDetailsContent.Meal) {
                buildMealRecipeSearchState(
                    details = original,
                    currentState = it.mealRecipeSearchState,
                )
            } else {
                MealRecipeSearchState()
            }

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

    fun saveEntry() {
        val content = currentContentState() ?: return showError("Entry is not ready yet")
        val activeFamilyId = _familyId.value ?: return showError("Missing family context")
        val now = Date()

        updateContent { it.copy(isSaving = true) }

        viewModelScope.launch {
            val result = entryDetailsSaver.save(
                details = content.details,
                mealRecipeSearchState = content.mealRecipeSearchState,
                entryId = entryId,
                familyId = activeFamilyId,
                listId = listId,
                now = now,
                context = context,
            )

            updateContent { it.copy(isSaving = false) }

            when (result) {
                is Result.Success -> {
                    originalDetails = currentContentState()?.details
                    if (entryId != null) {
                        updateContent { it.copy(isEditing = false) }
                    }
                    viewModelScope.launch {
                        _commands.send(ListEntryDetailsCommand.NavigateBack)
                    }
                }

                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun resetLoadingState() {
        originalDetails = null
        _uiState.value = EntryDetailsUiState.Loading
    }

    private fun showContent(details: EntryDetailsContent, isNewEntry: Boolean) {
        originalDetails = details
        _uiState.update { current ->
            when (current) {
                is EntryDetailsUiState.Content -> {
                    var mealRecipeSearchState = MealRecipeSearchState()
                    if (current.details is EntryDetailsContent.Meal) {
                        observeRecipes()
                        mealRecipeSearchState = buildMealRecipeSearchState(
                            details = current.details,
                            currentState = current.mealRecipeSearchState,
                        )
                    }
                    current.copy(
                        details = details,
                        mealRecipeSearchState = mealRecipeSearchState,
                    )
                }
                is EntryDetailsUiState.Loading -> EntryDetailsUiState.Content(
                    details = details,
                    mealRecipeSearchState = when (details) {
                        is EntryDetailsContent.Meal -> {
                            observeRecipes()
                            buildMealRecipeSearchState(details, null)
                        }
                        else -> MealRecipeSearchState()
                    },
                    isEditing = isNewEntry,
                    showDiscardDialog = false,
                    showDeleteDialog = false,
                    isSaving = false,
                )
            }
        }
    }

    private fun onImageSelected(uri: Uri) {
        val bitmap = uri.toBitmap(context.contentResolver)
        updateCurrentDetails { details ->
            when (details) {
                is EntryDetailsContent.Routine -> details.copy(
                    form = details.form.copy(
                        pendingImageUri = uri,
                        pendingImageBitmap = bitmap,
                    ),
                )

                else -> details
            }
        }

        val shouldUploadImmediately = entryId != null && _familyId.value != null
        if (!shouldUploadImmediately) {
            return
        }

        viewModelScope.launch {
            val familyIdValue = _familyId.value
            val existingEntryId = entryId
            if (familyIdValue.isNullOrBlank() || existingEntryId.isBlank()) {
                return@launch
            }

            when (val result = entryDetailsSaver.uploadRoutineImage(uri, familyIdValue, existingEntryId, context)) {
                is Result.Success -> Unit
                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun observeRecipes() {
        val familyId = familyId.value
        if (familyId.isNullOrBlank()) {
            observedRecipesFamilyId = null
            observeRecipesJob?.cancel()
            allRecipeSearchItems = emptyList()
            updateMealSearchStateFromCurrentContent()
            return
        }

        if (observedRecipesFamilyId == familyId && observeRecipesJob?.isActive == true) {
            return
        }

        observedRecipesFamilyId = familyId
        observeRecipesJob?.cancel()

        observeRecipesJob = viewModelScope.launch {
            recipeRepository.observeRecipes(familyId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val mappedItems = result.data.map { recipe ->
                            RecipeSearchItem(
                                id = recipe.id,
                                itemName = recipe.itemName,
                                preparationTimeMin = recipe.preparationTimeMin,
                            )
                        }
                        if (mappedItems == allRecipeSearchItems) {
                            return@collect
                        }
                        allRecipeSearchItems = mappedItems
                        updateMealSearchStateFromCurrentContent()
                    }

                    is Result.Failure -> {
                        allRecipeSearchItems = emptyList()
                        updateMealSearchStateFromCurrentContent()
                        showError(result.error.toUserMessage())
                    }
                }
            }
        }
    }

    private fun deleteMealPlanEntry() {
        val currentState = currentContentState() ?: return
        val entryIdValue = entryId ?: return
        if (currentState.details !is EntryDetailsContent.Meal) {
            return
        }

        isDeletingMealPlanEntry = true
        updateContent { it.copy(isSaving = true, showDeleteDialog = false) }

        viewModelScope.launch {
            when (val result = entryDetailsSaver.deleteMealPlanEntry(entryIdValue)) {
                is Result.Success -> {
                    _commands.send(ListEntryDetailsCommand.NavigateBack)
                }

                is Result.Failure -> {
                    isDeletingMealPlanEntry = false
                    updateContent { it.copy(isSaving = false) }
                    showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun updateMealRecipeQuery(value: String) {
        updateMealContent { details, searchState ->
            val selectedRecipe = searchState.selectedRecipeSearchItem
            val shouldClearSelection = selectedRecipe != null && value != selectedRecipe.itemName
            val updatedSearchState = searchState.copy(
                mode = MealSearchMode.RECIPE,
                query = value,
                isSearchFocused = true,
                selectedRecipeSearchItem = if (shouldClearSelection) null else selectedRecipe,
                suggestions = searchMealRecipeSuggestions(
                    query = value,
                    suggestions = allRecipeSearchItems,
                    selectedRecipeId = if (shouldClearSelection) null else selectedRecipe?.id,
                ),
            )
            details.copy(
                form = details.form.copy(
                    name = value,
                    recipeId = if (shouldClearSelection) "" else details.form.recipeId,
                ),
            ) to updatedSearchState
        }
    }

    private fun updateMealMode(mode: MealSearchMode) {
        updateMealContent { details, searchState ->
            val selectedRecipe = searchState.selectedRecipeSearchItem
            val isRecipeMode = mode == MealSearchMode.RECIPE
            val query = if (isRecipeMode) {
                searchState.query.ifBlank { selectedRecipe?.itemName.orEmpty() }
            } else {
                searchState.query
            }
            val updatedSearchState = searchState.copy(
                mode = mode,
                query = query,
                suggestions = if (isRecipeMode) {
                    searchMealRecipeSuggestions(
                        query = query,
                        suggestions = allRecipeSearchItems,
                        selectedRecipeId = selectedRecipe?.id,
                    )
                } else {
                    emptyList()
                },
            )
            details.copy(
                form = details.form.copy(
                    name = if (isRecipeMode) query else details.form.customMealName,
                ),
            ) to updatedSearchState
        }
    }

    private fun updateMealCustomName(value: String) {
        updateMealContent { details, searchState ->
            details.copy(
                form = details.form.copy(
                    name = value,
                    customMealName = value,
                ),
            ) to searchState
        }
    }

    private fun selectRecipe(recipe: RecipeSearchItem) {
        updateMealContent { details, searchState ->
            details.copy(
                form = details.form.copy(
                    name = recipe.itemName,
                    recipeId = recipe.id,
                ),
            ) to searchState.copy(
                mode = MealSearchMode.RECIPE,
                query = recipe.itemName,
                isSearchFocused = false,
                selectedRecipeSearchItem = recipe,
                suggestions = emptyList(),
            )
        }
    }

    private fun updateMealSearchStateFromCurrentContent() {
        val content = currentContentState() ?: return
        val mealDetails = content.details as? EntryDetailsContent.Meal ?: return
        updateContent { state ->
            state.copy(
                mealRecipeSearchState = buildMealRecipeSearchState(
                    details = mealDetails,
                    currentState = state.mealRecipeSearchState,
                )
            )
        }
    }

    private fun buildMealRecipeSearchState(
        details: EntryDetailsContent.Meal,
        currentState: MealRecipeSearchState?,
    ): MealRecipeSearchState {
        val existingRecipe = details.form.recipeId.takeIf { it.isNotBlank() }
            ?.let { recipeId -> allRecipeSearchItems.firstOrNull { it.id == recipeId } }
        val currentSelectedRecipe = currentState?.selectedRecipeSearchItem
            ?.let { selectedRecipe -> allRecipeSearchItems.firstOrNull { it.id == selectedRecipe.id } }
        val isRecipeMode = currentState?.mode ?: if (details.form.customMealName.isBlank()) {
            MealSearchMode.RECIPE
        } else {
            MealSearchMode.CUSTOM
        }
        val isRecipeModeSelected = isRecipeMode == MealSearchMode.RECIPE
        val selectedRecipeSearchItem = existingRecipe ?: currentSelectedRecipe
        val query = when {
            isRecipeModeSelected && currentState?.query?.isNotBlank() == true -> currentState.query
            isRecipeModeSelected && selectedRecipeSearchItem != null -> selectedRecipeSearchItem.itemName
            isRecipeModeSelected -> details.form.name
            else -> currentState?.query.orEmpty()
        }

        return MealRecipeSearchState(
            mode = isRecipeMode,
            query = query,
            isSearchFocused = currentState?.isSearchFocused ?: false,
            selectedRecipeSearchItem = selectedRecipeSearchItem,
            suggestions = searchMealRecipeSuggestions(
                query = query,
                suggestions = allRecipeSearchItems,
                selectedRecipeId = selectedRecipeSearchItem?.id,
            ),
        )
    }

    private fun updateMealContent(
        transform: (EntryDetailsContent.Meal, MealRecipeSearchState) -> Pair<EntryDetailsContent.Meal, MealRecipeSearchState>,
    ) {
        updateContent { state ->
            val meal = state.details as? EntryDetailsContent.Meal ?: return@updateContent state
            val (updatedMeal, updatedSearchState) = transform(meal, state.mealRecipeSearchState)
            state.copy(
                details = updatedMeal,
                mealRecipeSearchState = updatedSearchState,
            )
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

    private fun currentContentState(): EntryDetailsUiState.Content? {
        return _uiState.value as? EntryDetailsUiState.Content
    }

    private fun updateContent(block: (EntryDetailsUiState.Content) -> EntryDetailsUiState.Content) {
        _uiState.update { state ->
            if (state is EntryDetailsUiState.Content) block(state) else state
        }
    }

    private fun updateCurrentDetails(block: (EntryDetailsContent) -> EntryDetailsContent) {
        updateContent { state ->
            state.copy(details = block(state.details))
        }
    }
}
