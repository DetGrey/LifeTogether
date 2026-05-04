package com.example.lifetogether.ui.feature.admin.groceryList.categories

import com.example.lifetogether.domain.model.Category

sealed interface AdminGroceryCategoriesUiState {
    data object Loading : AdminGroceryCategoriesUiState

    data class Content(
        val groceryCategories: List<Category>,
        val newCategory: String = "",
        val showDeleteCategoryConfirmationDialog: Boolean = false,
        val selectedCategory: Category? = null,
    ) : AdminGroceryCategoriesUiState
}

sealed interface AdminGroceryCategoriesUiEvent {
    data class NewCategoryChanged(val value: String) : AdminGroceryCategoriesUiEvent
    data object AddCategoryClicked : AdminGroceryCategoriesUiEvent
    data class DeleteCategoryClicked(val category: Category) : AdminGroceryCategoriesUiEvent
    data object DismissDeleteCategoryConfirmation : AdminGroceryCategoriesUiEvent
    data object ConfirmDeleteCategory : AdminGroceryCategoriesUiEvent
}

sealed interface AdminGroceryCategoriesNavigationEvent {
    data object NavigateBack : AdminGroceryCategoriesNavigationEvent
}
