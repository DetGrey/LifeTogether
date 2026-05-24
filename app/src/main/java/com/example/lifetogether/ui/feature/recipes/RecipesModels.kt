package com.example.lifetogether.ui.feature.recipes

import com.example.lifetogether.domain.model.recipe.Recipe

sealed interface RecipesUiState {
    data object Loading : RecipesUiState

    data class Content(
        val recipes: List<Recipe>,
        val tagsList: List<String>,
        val selectedTag: String,
        val searchQuery: String = "",
        val isSearchActive: Boolean = false,
    ) : RecipesUiState
}

sealed interface RecipesUiEvent {
    data class TagSelected(val tag: String) : RecipesUiEvent
    data object SearchIconClicked : RecipesUiEvent
    data class SearchQueryChanged(val value: String) : RecipesUiEvent
    data object SearchClearClicked : RecipesUiEvent
    data object SearchCancelClicked : RecipesUiEvent
}

sealed interface RecipesNavigationEvent {
    data object NavigateBack : RecipesNavigationEvent
    data class NavigateToRecipeDetails(val recipeId: String) : RecipesNavigationEvent
    data object NavigateToCreateRecipe : RecipesNavigationEvent
}
