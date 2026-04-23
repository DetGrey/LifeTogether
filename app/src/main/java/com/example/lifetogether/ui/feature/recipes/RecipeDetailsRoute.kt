package com.example.lifetogether.ui.feature.recipes

import androidx.compose.runtime.Composable
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun CreateRecipeRoute(
    appNavigator: AppNavigator,
) {
    RecipeDetailsScreen(appNavigator, null)
}

@Composable
fun RecipeDetailsRoute(
    recipeId: String,
    appNavigator: AppNavigator,
) {
    RecipeDetailsScreen(appNavigator, recipeId)
}
