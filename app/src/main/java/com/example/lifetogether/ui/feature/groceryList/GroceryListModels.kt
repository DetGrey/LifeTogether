package com.example.lifetogether.ui.feature.groceryList

import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.util.UNCATEGORIZED_CATEGORY

sealed interface GroceryListUiState {
    data object Loading : GroceryListUiState

    data class Content(
        val showConfirmationDialog: Boolean = false,
        val groceryList: List<GroceryItem>,
        val completedItems: List<GroceryItem>,
        val categorizedItems: Map<Category, List<GroceryItem>>,
        val groceryCategories: List<Category>,
        val categoryExpandedStates: Map<String, Boolean>,
        val completedSectionExpanded: Boolean = false,
        val expectedTotalPrice: Float?,
        val newItemText: String = "",
        val newItemPrice: String = "",
        val newItemCategory: Category = UNCATEGORIZED_CATEGORY,
        val allGrocerySuggestions: List<GrocerySuggestion>,
        val currentGrocerySuggestions: List<GrocerySuggestion>,
    ) : GroceryListUiState
}

sealed interface GroceryListNavigationEvent {
    data object NavigateBack : GroceryListNavigationEvent
}

sealed interface GroceryListUiEvent {
    data class CategoryExpandedClicked(val categoryName: String) : GroceryListUiEvent
    data object CompletedSectionExpandedClicked : GroceryListUiEvent
    data class ItemCompletedToggled(val item: GroceryItem) : GroceryListUiEvent
    data class NotificationClicked(val item: GroceryItem) : GroceryListUiEvent
    data object DeleteCompletedClicked : GroceryListUiEvent
    data object DismissDeleteCompletedConfirmation : GroceryListUiEvent
    data object ConfirmDeleteCompletedConfirmation : GroceryListUiEvent
    data class NewItemTextChanged(val value: String) : GroceryListUiEvent
    data class NewItemPriceChanged(val value: String) : GroceryListUiEvent
    data class NewItemCategoryChanged(val value: Category?) : GroceryListUiEvent
    data object AddItemClicked : GroceryListUiEvent
    data class SuggestionClicked(val suggestion: GrocerySuggestion) : GroceryListUiEvent
}
