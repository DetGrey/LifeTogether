package com.example.lifetogether.ui.feature.recipes

import com.example.lifetogether.domain.model.recipe.Recipe

data class RecipesUiState(
    val recipes: List<Recipe> = emptyList(),
    val tagsList: List<String> = listOf("All"),
    val selectedTag: String = "All",
)

sealed interface RecipesUiEvent {
    data class TagSelected(val tag: String) : RecipesUiEvent
}

sealed interface RecipesNavigationEvent {
    data object NavigateBack : RecipesNavigationEvent
    data class NavigateToRecipeDetails(val recipeId: String? = null) : RecipesNavigationEvent
}
