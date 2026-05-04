package com.example.lifetogether.ui.feature.recipes

import com.example.lifetogether.domain.model.recipe.Recipe

sealed interface RecipesUiState {
    data object Loading : RecipesUiState

    data class Content(
        val recipes: List<Recipe>,
        val tagsList: List<String>,
        val selectedTag: String,
    ) : RecipesUiState
}

sealed interface RecipesUiEvent {
    data class TagSelected(val tag: String) : RecipesUiEvent
}

sealed interface RecipesNavigationEvent {
    data object NavigateBack : RecipesNavigationEvent
    data class NavigateToRecipeDetails(val recipeId: String) : RecipesNavigationEvent
    data object NavigateToCreateRecipe : RecipesNavigationEvent
}
