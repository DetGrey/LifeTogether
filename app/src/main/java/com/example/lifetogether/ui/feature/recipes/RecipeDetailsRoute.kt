package com.example.lifetogether.ui.feature.recipes

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.AppRoutes

@Composable
fun CreateRecipeRoute(
    appNavigator: AppNavigator,
) {
    RecipeDetailsScreen(appNavigator, null)
}

@Composable
fun RecipeDetailsRoute(
    backStackEntry: NavBackStackEntry,
    appNavigator: AppNavigator,
) {
    val recipeId = backStackEntry.arguments?.getString(AppRoutes.RECIPE_ID_ARG)
    RecipeDetailsScreen(appNavigator, recipeId)
}
