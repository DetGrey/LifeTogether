package com.example.lifetogether.ui.feature.recipes

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.AppRoutes
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel

@Composable
fun CreateRecipeRoute(
    appNavigator: AppNavigator,
    appSessionViewModel: AppSessionViewModel,
) {
    RecipeDetailsScreen(appNavigator, appSessionViewModel, null)
}

@Composable
fun RecipeDetailsRoute(
    backStackEntry: NavBackStackEntry,
    appNavigator: AppNavigator,
    appSessionViewModel: AppSessionViewModel,
) {
    val recipeId = backStackEntry.arguments?.getString(AppRoutes.RECIPE_ID_ARG)
    RecipeDetailsScreen(appNavigator, appSessionViewModel, recipeId)
}
