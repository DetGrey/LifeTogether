package com.example.lifetogether.ui.feature.admin.groceryList.suggestions

sealed interface AdminGrocerySuggestionsNavigationEvent {
    data object NavigateBack : AdminGrocerySuggestionsNavigationEvent
}