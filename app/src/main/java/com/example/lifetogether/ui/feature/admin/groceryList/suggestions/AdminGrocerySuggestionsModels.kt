package com.example.lifetogether.ui.feature.admin.groceryList.suggestions

import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.util.UNCATEGORIZED_CATEGORY

sealed interface AdminGrocerySuggestionsUiState {
    data object Loading : AdminGrocerySuggestionsUiState

    data class Content(
        val groceryCategories: List<Category>,
        val grocerySuggestions: List<GrocerySuggestion>,
        val showDeleteCategoryConfirmationDialog: Boolean = false,
        val selectedSuggestion: GrocerySuggestion? = null,
        val categoryExpandedStates: Set<String> = emptySet(),
        val newSuggestionText: String = "",
        val newSuggestionPrice: String = "",
        val newSuggestionCategory: Category = UNCATEGORIZED_CATEGORY,
        val editingSuggestionId: String? = null,
    ) : AdminGrocerySuggestionsUiState
}

sealed interface AdminGrocerySuggestionsUiEvent {
    data class ToggleCategory(val categoryName: String) : AdminGrocerySuggestionsUiEvent
    data class StartEditingSuggestion(val suggestion: GrocerySuggestion) : AdminGrocerySuggestionsUiEvent
    data class ClickDeleteSuggestion(val suggestion: GrocerySuggestion) : AdminGrocerySuggestionsUiEvent
    data object DismissDeleteSuggestionDialog : AdminGrocerySuggestionsUiEvent
    data object ConfirmDeleteSuggestion : AdminGrocerySuggestionsUiEvent
    data class NewSuggestionTextChanged(val value: String) : AdminGrocerySuggestionsUiEvent
    data class NewSuggestionPriceChanged(val value: String) : AdminGrocerySuggestionsUiEvent
    data class NewSuggestionCategoryChanged(val category: Category?) : AdminGrocerySuggestionsUiEvent
    data object ClickAddSuggestion : AdminGrocerySuggestionsUiEvent
    data object ClickSaveSuggestion : AdminGrocerySuggestionsUiEvent
}

sealed interface AdminGrocerySuggestionsNavigationEvent {
    data object NavigateBack : AdminGrocerySuggestionsNavigationEvent
}
