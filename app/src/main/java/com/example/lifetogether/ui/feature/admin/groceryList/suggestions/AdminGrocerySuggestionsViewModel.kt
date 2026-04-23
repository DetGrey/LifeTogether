package com.example.lifetogether.ui.feature.admin.groceryList.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.repository.GroceryRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.ui.common.event.UiCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminGrocerySuggestionsViewModel @Inject constructor(
    private val groceryRepository: GroceryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminGrocerySuggestionsUiState())
    val uiState: StateFlow<AdminGrocerySuggestionsUiState> = _uiState.asStateFlow()
    private val _commands = Channel<UiCommand>(Channel.BUFFERED)
    val commands: Flow<UiCommand> = _commands.receiveAsFlow()

    init {
        observeCategories()
        observeGrocerySuggestions()
    }

    fun onEvent(event: AdminGrocerySuggestionsUiEvent) {
        when (event) {
            is AdminGrocerySuggestionsUiEvent.ToggleCategory -> toggleCategory(event.categoryName)
            is AdminGrocerySuggestionsUiEvent.StartEditingSuggestion -> startEditingSuggestion(event.suggestion)
            is AdminGrocerySuggestionsUiEvent.ClickDeleteSuggestion -> onDeleteSuggestionClick(event.suggestion)
            AdminGrocerySuggestionsUiEvent.DismissDeleteSuggestionDialog -> dismissDeleteSuggestionDialog()
            AdminGrocerySuggestionsUiEvent.ConfirmDeleteSuggestion -> deleteCategory()
            is AdminGrocerySuggestionsUiEvent.NewSuggestionTextChanged -> onNewSuggestionTextChange(event.value)
            is AdminGrocerySuggestionsUiEvent.NewSuggestionPriceChanged -> onNewSuggestionPriceChange(event.value)
            is AdminGrocerySuggestionsUiEvent.NewSuggestionCategoryChanged -> updateNewSuggestionCategory(event.category)
            AdminGrocerySuggestionsUiEvent.ClickAddSuggestion -> addNewGrocerySuggestion()
            AdminGrocerySuggestionsUiEvent.ClickSaveSuggestion -> saveEditedGrocerySuggestion()
        }
    }

    // ---------------------------------------------------------------- CATEGORIES
    private fun observeCategories() {
        viewModelScope.launch {
            groceryRepository.observeCategories().collect { result ->
                when (result) {
                    is Result.Success -> {
                        updateUiState { state ->
                            state.copy(
                                groceryCategories = result.data
                                    .filterNot { it.name == UNCATEGORIZED_NAME }
                                    .sortedBy { it.name }
                                    .let { listOf(UNCATEGORIZED_CATEGORY) + it },
                            )
                        }
                    }

                    is Result.Failure -> {
                        updateUiState { state ->
                            state.copy(groceryCategories = emptyList())
                        }
                        showError(result.error.toUserMessage())
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- EXPANDED STATES
    private fun toggleCategory(categoryName: String) {
        updateUiState { state ->
            val currentSet = state.categoryExpandedStates
            val newSet = if (currentSet.contains(categoryName)) {
                currentSet - categoryName
            } else {
                currentSet + categoryName
            }
            state.copy(categoryExpandedStates = newSet)
        }
    }

    // ---------------------------------------------------------------- GROCERY SUGGESTIONS
    private fun observeGrocerySuggestions() {
        viewModelScope.launch {
            groceryRepository.observeGrocerySuggestions().collect { result ->
                when (result) {
                    is Result.Success -> {
                        updateUiState { state ->
                            state.copy(
                                grocerySuggestions = result.data
                                    .sortedBy { it.category?.name }
                            )
                        }
                    }

                    is Result.Failure -> {
                        updateUiState { state ->
                            state.copy(grocerySuggestions = emptyList())
                        }
                        showError(result.error.toUserMessage())
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- NEW ITEM
    private fun onNewSuggestionTextChange(value: String) {
        updateUiState { state ->
            state.copy(newSuggestionText = value)
        }
    }

    private fun onNewSuggestionPriceChange(value: String) {
        updateUiState { state ->
            state.copy(newSuggestionPrice = value)
        }
    }

    private fun updateNewSuggestionCategory(category: Category?) {
        updateUiState { state ->
            state.copy(newSuggestionCategory = category ?: UNCATEGORIZED_CATEGORY)
        }
    }

    private fun onDeleteSuggestionClick(suggestion: GrocerySuggestion) {
        updateUiState { state ->
            state.copy(
                selectedSuggestion = suggestion,
                showDeleteCategoryConfirmationDialog = true,
            )
        }
    }

    private fun startEditingSuggestion(suggestion: GrocerySuggestion) {
        updateUiState { state ->
            state.copy(
                editingSuggestionId = suggestion.id,
                isEditMode = true,
                newSuggestionText = suggestion.suggestionName,
                newSuggestionPrice = suggestion.approxPrice?.toString().orEmpty(),
                newSuggestionCategory = suggestion.category ?: UNCATEGORIZED_CATEGORY,
            )
        }
    }

    private fun clearSuggestionDraft() {
        updateUiState { state ->
            state.copy(
                editingSuggestionId = null,
                isEditMode = false,
                newSuggestionCategory = UNCATEGORIZED_CATEGORY,
                newSuggestionText = "",
                newSuggestionPrice = "",
            )
        }
    }

    private fun dismissDeleteSuggestionDialog() {
        updateUiState { state ->
            state.copy(
                showDeleteCategoryConfirmationDialog = false,
                selectedSuggestion = null,
            )
        }
    }

    // ---------------------------------------------------------------- ADD CATEGORY
    private fun addNewGrocerySuggestion() {
        val state = _uiState.value

        if (state.newSuggestionText.isEmpty()) {
            showError("Please enter a suggestion first")
            return
        }

        val price = state.newSuggestionPrice.toFloatOrNull()

        val grocerySuggestion = GrocerySuggestion(
            category = state.newSuggestionCategory,
            suggestionName = state.newSuggestionText,
            approxPrice = price,
        )

        viewModelScope.launch {
            when (val result = groceryRepository.saveGrocerySuggestion(grocerySuggestion)) {
                is Result.Success -> clearSuggestionDraft()
                is Result.Failure -> {
                    showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun saveEditedGrocerySuggestion() {
        val state = _uiState.value
        if (!state.isEditMode) {
            return
        }

        if (state.newSuggestionText.isEmpty()) {
            showError("Please enter a suggestion first")
            return
        }

        val editingSuggestionId = state.editingSuggestionId
        if (editingSuggestionId.isNullOrBlank()) {
            showError("Missing suggestion id")
            return
        }

        val updatedSuggestion = GrocerySuggestion(
            id = editingSuggestionId,
            suggestionName = state.newSuggestionText,
            category = state.newSuggestionCategory,
            approxPrice = state.newSuggestionPrice.toFloatOrNull(),
        )

        viewModelScope.launch {
            when (val result = groceryRepository.updateGrocerySuggestion(updatedSuggestion)) {
                is Result.Success -> clearSuggestionDraft()
                is Result.Failure -> {
                    showError(result.error.toUserMessage())
                }
            }
        }
    }

    // ---------------------------------------------------------------- DELETE CATEGORY
    private fun deleteCategory() {
        val selectedSuggestion = _uiState.value.selectedSuggestion ?: run {
            return
        }

        viewModelScope.launch {
            val result = groceryRepository.deleteGrocerySuggestion(selectedSuggestion)

            if (result is Result.Failure) {
                showError(result.error.toUserMessage())
            }

            updateUiState { state ->
                state.copy(
                    selectedSuggestion = null,
                    showDeleteCategoryConfirmationDialog = false,
                )
            }
        }
    }

    private fun showError(message: String) {
        viewModelScope.launch {
            _commands.send(
                UiCommand.ShowSnackbar(
                    message = message,
                    withDismissAction = true,
                ),
            )
        }
    }

    private fun updateUiState(transform: (AdminGrocerySuggestionsUiState) -> AdminGrocerySuggestionsUiState) {
        _uiState.update(transform)
    }
}
