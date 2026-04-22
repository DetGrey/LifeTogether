package com.example.lifetogether.ui.feature.admin.groceryList

import com.example.lifetogether.domain.result.toUserMessage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.repository.GroceryRepository
import com.example.lifetogether.domain.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val ALERT_DISMISS_DELAY_MS = 3000L
private const val UNCATEGORIZED_NAME = "Uncategorized"
private val UNCATEGORIZED_CATEGORY = Category(
    emoji = "❓️",
    name = UNCATEGORIZED_NAME,
)

data class AdminGrocerySuggestionsUiState(
    val showDeleteCategoryConfirmationDialog: Boolean = false,
    val selectedSuggestion: GrocerySuggestion? = null,
    val showAlertDialog: Boolean = false,
    val error: String = "",
    val groceryCategories: List<Category> = emptyList(),
    val categoryExpandedStates: Set<String> = emptySet(),
    val grocerySuggestions: List<GrocerySuggestion> = emptyList(),
    val newSuggestionText: String = "",
    val newSuggestionPrice: String = "",
    val newSuggestionCategory: Category = UNCATEGORIZED_CATEGORY,
    val editingSuggestionId: String? = null,
    val isEditMode: Boolean = false,
)

@HiltViewModel
class AdminGrocerySuggestionsViewModel @Inject constructor(
    private val groceryRepository: GroceryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminGrocerySuggestionsUiState())
    val uiState: StateFlow<AdminGrocerySuggestionsUiState> = _uiState.asStateFlow()

    fun toggleAlertDialog() {
        viewModelScope.launch {
            delay(ALERT_DISMISS_DELAY_MS)
            updateUiState { state ->
                state.copy(
                    showAlertDialog = false,
                    error = "",
                )
            }
        }
    }

    // ---------------------------------------------------------------- SETUP/FETCH LIST
    fun setUpGrocerySuggestions() {
        observeCategories()
        observeGrocerySuggestions()
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
    fun toggleCategory(categoryName: String) {
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
        println("GroceryListViewModel before calling fetchGrocerySuggestionsUseCase")
        viewModelScope.launch {
            groceryRepository.observeGrocerySuggestions().collect { result ->
                println("GroceryListViewModel fetchGrocerySuggestionsUseCase result: $result")
                when (result) {
                    is Result.Success -> {
                        println("GroceryListViewModel categories updated: ${result.data}")
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
    fun onNewSuggestionTextChange(value: String) {
        updateUiState { state ->
            state.copy(newSuggestionText = value)
        }
    }

    fun onNewSuggestionPriceChange(value: String) {
        updateUiState { state ->
            state.copy(newSuggestionPrice = value)
        }
    }

    fun updateNewSuggestionCategory(category: Category?) {
        updateUiState { state ->
            state.copy(newSuggestionCategory = category ?: UNCATEGORIZED_CATEGORY)
        }
    }

    fun onDeleteSuggestionClick(suggestion: GrocerySuggestion) {
        updateUiState { state ->
            state.copy(
                selectedSuggestion = suggestion,
                showDeleteCategoryConfirmationDialog = true,
            )
        }
    }

    fun startEditingSuggestion(suggestion: GrocerySuggestion) {
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

    fun dismissDeleteSuggestionDialog() {
        updateUiState { state ->
            state.copy(
                showDeleteCategoryConfirmationDialog = false,
                selectedSuggestion = null,
            )
        }
    }

    // ---------------------------------------------------------------- ADD CATEGORY
    fun addNewGrocerySuggestion() {
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
                    println("Error: ${result.error.toUserMessage()}")
                    showError(result.error.toUserMessage())
                }
            }
        }
    }

    fun saveEditedGrocerySuggestion() {
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
                    println("Error: ${result.error.toUserMessage()}")
                    showError(result.error.toUserMessage())
                }
            }
        }
    }

    // ---------------------------------------------------------------- DELETE CATEGORY
    fun deleteCategory() {
        val selectedSuggestion = _uiState.value.selectedSuggestion ?: run {
            return
        }

        viewModelScope.launch {
            val result = groceryRepository.deleteGrocerySuggestion(selectedSuggestion)

            if (result is Result.Failure) {
                println("Error: ${result.error.toUserMessage()}")
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
        updateUiState { state ->
            state.copy(
                error = message,
                showAlertDialog = true,
            )
        }
    }

    private fun updateUiState(transform: (AdminGrocerySuggestionsUiState) -> AdminGrocerySuggestionsUiState) {
        _uiState.update(transform)
    }
}
