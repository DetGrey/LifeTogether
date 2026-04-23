package com.example.lifetogether.ui.feature.admin.groceryList.categories

import com.example.lifetogether.domain.model.Category

data class AdminGroceryCategoriesUiState(
    val groceryCategories: List<Category> = emptyList(),
    val newCategory: String = "",
    val showDeleteCategoryConfirmationDialog: Boolean = false,
    val selectedCategory: Category? = null,
)

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
