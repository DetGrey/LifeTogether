package com.example.lifetogether.ui.feature.recipes

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.AppRoutes
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel

@Composable
fun CreateRecipeRoute(
    appNavigator: AppNavigator,
) {
    // TODO [Issue #3]: remove bridge after RecipeDetailsScreen migrates off AppSessionViewModel
    val appSessionViewModel: AppSessionViewModel = hiltViewModel()
    RecipeDetailsScreen(appNavigator, appSessionViewModel, null)
}

@Composable
fun RecipeDetailsRoute(
    backStackEntry: NavBackStackEntry,
    appNavigator: AppNavigator,
) {
    val recipeId = backStackEntry.arguments?.getString(AppRoutes.RECIPE_ID_ARG)
    // TODO [Issue #3]: remove bridge after RecipeDetailsScreen migrates off AppSessionViewModel
    val appSessionViewModel: AppSessionViewModel = hiltViewModel()
    RecipeDetailsScreen(appNavigator, appSessionViewModel, recipeId)
}
