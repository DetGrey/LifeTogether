package com.example.lifetogether.ui.feature.admin.groceryList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.listener.CategoriesListener
import com.example.lifetogether.domain.listener.GrocerySuggestionsListener
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.usecase.item.DeleteGrocerySuggestionUseCase
import com.example.lifetogether.domain.usecase.item.FetchCategoriesUseCase
import com.example.lifetogether.domain.usecase.item.FetchGrocerySuggestionsUseCase
import com.example.lifetogether.domain.usecase.item.SaveGrocerySuggestionUseCase
import com.example.lifetogether.domain.usecase.item.UpdateGrocerySuggestionUseCase
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
    private val fetchCategoriesUseCase: FetchCategoriesUseCase,
    private val fetchGrocerySuggestionsUseCase: FetchGrocerySuggestionsUseCase,
    private val deleteGrocerySuggestionUseCase: DeleteGrocerySuggestionUseCase,
    private val saveGrocerySuggestionUseCase: SaveGrocerySuggestionUseCase,
    private val updateGrocerySuggestionUseCase: UpdateGrocerySuggestionUseCase,
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
        fetchCategories()
        fetchGrocerySuggestions()
    }

    // ---------------------------------------------------------------- CATEGORIES
    private fun fetchCategories() {
        println("GroceryListViewModel before calling fetchCategoriesUseCase")
        viewModelScope.launch {
            fetchCategoriesUseCase().collect { result ->
                println("GroceryListViewModel fetchCategoriesUseCase result: $result")
                when (result) {
                    is CategoriesListener.Success -> {
                        println("GroceryListViewModel categories updated: ${result.listItems}")
                        updateUiState { state ->
                            state.copy(
                                groceryCategories = result.listItems
                                    .filterNot { it.name == UNCATEGORIZED_NAME }
                                    .sortedBy { it.name }
                                    .let { listOf(UNCATEGORIZED_CATEGORY) + it },
                            )
                        }
                    }

                    is CategoriesListener.Failure -> {
                        updateUiState { state ->
                            state.copy(groceryCategories = emptyList())
                        }
                        showError(result.message)
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
    private fun fetchGrocerySuggestions() {
        println("GroceryListViewModel before calling fetchGrocerySuggestionsUseCase")
        viewModelScope.launch {
            fetchGrocerySuggestionsUseCase().collect { result ->
                println("GroceryListViewModel fetchGrocerySuggestionsUseCase result: $result")
                when (result) {
                    is GrocerySuggestionsListener.Success -> {
                        println("GroceryListViewModel categories updated: ${result.listItems}")
                        updateUiState { state ->
                            state.copy(
                                grocerySuggestions = result.listItems
                                    .sortedBy { it.category?.name }
                            )
                        }
                    }

                    is GrocerySuggestionsListener.Failure -> {
                        updateUiState { state ->
                            state.copy(grocerySuggestions = emptyList())
                        }
                        showError(result.message)
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
            val result: ResultListener = saveGrocerySuggestionUseCase.invoke(grocerySuggestion)
            if (result is ResultListener.Success) {
                clearSuggestionDraft()
            } else if (result is ResultListener.Failure) {
                println("Error: ${result.message}")
                showError(result.message)
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
            when (val result = updateGrocerySuggestionUseCase.invoke(updatedSuggestion)) {
                is ResultListener.Success -> clearSuggestionDraft()
                is ResultListener.Failure -> {
                    println("Error: ${result.message}")
                    showError(result.message)
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
            val result: ResultListener = deleteGrocerySuggestionUseCase.invoke(selectedSuggestion)

            if (result is ResultListener.Failure) {
                println("Error: ${result.message}")
                showError(result.message)
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
