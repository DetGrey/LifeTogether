package com.example.lifetogether.ui.feature.groceryList

import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion

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
