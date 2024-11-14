package com.example.lifetogether.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.lifetogether.ui.feature.admin.AdminScreen
import com.example.lifetogether.ui.feature.admin.groceryList.AdminGroceryCategoriesScreen
import com.example.lifetogether.ui.feature.admin.groceryList.AdminGrocerySuggestionsScreen
import com.example.lifetogether.ui.feature.groceryList.GroceryListScreen
import com.example.lifetogether.ui.feature.home.HomeScreen
import com.example.lifetogether.ui.feature.login.LoginScreen
import com.example.lifetogether.ui.feature.profile.ProfileScreen
import com.example.lifetogether.ui.feature.recipes.RecipeDetailsScreen
import com.example.lifetogether.ui.feature.recipes.RecipesScreen
import com.example.lifetogether.ui.feature.settings.SettingsScreen
import com.example.lifetogether.ui.feature.signup.SignupScreen
import com.example.lifetogether.ui.viewmodel.AuthViewModel

@Composable
fun NavHost(
    navController: NavHostController,
) {
    val appNavigator = AppNavigator(navController)
    val authViewModel: AuthViewModel = hiltViewModel()

    val userInformation = authViewModel.userInformation.collectAsState(initial = null)
    when (val familyId = userInformation.value?.familyId) {
        is String -> {
            authViewModel.observeFirestoreFamilyData(familyId)
        }
    }

    androidx.navigation.compose.NavHost(
        navController = navController,
        startDestination = AppRoutes.HOME_SCREEN,
    ) {
        composable(AppRoutes.ADMIN_SCREEN) {
            AdminScreen(appNavigator, authViewModel)
        }
        composable(AppRoutes.ADMIN_GROCERY_CATEGORIES_SCREEN) {
            AdminGroceryCategoriesScreen(appNavigator, authViewModel)
        }
        composable(AppRoutes.ADMIN_GROCERY_SUGGESTIONS_SCREEN) {
            AdminGrocerySuggestionsScreen(appNavigator, authViewModel)
        }
        composable(AppRoutes.HOME_SCREEN) {
            HomeScreen(appNavigator, authViewModel)
        }

        composable(AppRoutes.PROFILE_SCREEN) {
            ProfileScreen(appNavigator, authViewModel)
        }

        composable(AppRoutes.SETTINGS_SCREEN) {
            SettingsScreen(appNavigator, authViewModel)
        }

        composable(AppRoutes.LOGIN_SCREEN) {
            LoginScreen(appNavigator, authViewModel)
        }
        composable(AppRoutes.SIGNUP_SCREEN) {
            SignupScreen(appNavigator, authViewModel)
        }

        composable(AppRoutes.GROCERY_LIST_SCREEN) {
            GroceryListScreen(appNavigator, authViewModel)
        }

        composable(AppRoutes.RECIPES_SCREEN) {
            RecipesScreen(appNavigator, authViewModel)
        }

        composable(AppRoutes.CREATE_RECIPE_SCREEN) {
            RecipeDetailsScreen(appNavigator, authViewModel, null)
        }

        composable(
            route = "${AppRoutes.RECIPE_DETAILS_SCREEN}/{${AppRoutes.RECIPE_ID_ARG}}",
            arguments = listOf(navArgument(AppRoutes.RECIPE_ID_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString(AppRoutes.RECIPE_ID_ARG)
            RecipeDetailsScreen(appNavigator, authViewModel, recipeId)
        }
    }
}
