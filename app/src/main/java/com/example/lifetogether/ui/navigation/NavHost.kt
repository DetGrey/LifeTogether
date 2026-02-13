package com.example.lifetogether.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.lifetogether.ui.feature.admin.groceryList.AdminGroceryCategoriesScreen
import com.example.lifetogether.ui.feature.admin.groceryList.AdminGrocerySuggestionsScreen
import com.example.lifetogether.ui.feature.family.FamilyScreen
import com.example.lifetogether.ui.feature.gallery.AlbumDetailsScreen
import com.example.lifetogether.ui.feature.gallery.MediaDetailsScreen
import com.example.lifetogether.ui.feature.gallery.GalleryScreen
import com.example.lifetogether.ui.feature.groceryList.GroceryListScreen
import com.example.lifetogether.ui.feature.home.HomeScreen
import com.example.lifetogether.ui.feature.loading.LoadingScreen
import com.example.lifetogether.ui.feature.login.LoginScreen
import com.example.lifetogether.ui.feature.profile.ProfileScreen
import com.example.lifetogether.ui.feature.recipes.RecipeDetailsScreen
import com.example.lifetogether.ui.feature.recipes.RecipesScreen
import com.example.lifetogether.ui.feature.settings.SettingsScreen
import com.example.lifetogether.ui.feature.signup.SignupScreen
import com.example.lifetogether.ui.feature.tipTracker.TipStatisticsScreen
import com.example.lifetogether.ui.feature.tipTracker.TipTrackerScreen
import com.example.lifetogether.ui.feature.tipTracker.TipTrackerViewModel
import com.example.lifetogether.ui.viewmodel.FirebaseViewModel

@Composable
fun NavHost(
    navController: NavHostController,
    firebaseViewModel: FirebaseViewModel,
) {
    val appNavigator = AppNavigator(navController)

    androidx.navigation.compose.NavHost(
        navController = navController,
        startDestination = AppRoutes.LOADING_SCREEN,
    ) {
        composable(AppRoutes.ADMIN_GROCERY_CATEGORIES_SCREEN) {
            AdminGroceryCategoriesScreen(appNavigator, firebaseViewModel)
        }
        composable(AppRoutes.ADMIN_GROCERY_SUGGESTIONS_SCREEN) {
            AdminGrocerySuggestionsScreen(appNavigator, firebaseViewModel)
        }

        composable(AppRoutes.LOADING_SCREEN) {
            LoadingScreen(appNavigator, firebaseViewModel)
        }

        composable(AppRoutes.HOME_SCREEN) {
            HomeScreen(appNavigator, firebaseViewModel)
        }

        composable(AppRoutes.PROFILE_SCREEN) {
            ProfileScreen(appNavigator, firebaseViewModel)
        }

        composable(AppRoutes.FAMILY_SCREEN) {
            FamilyScreen(appNavigator, firebaseViewModel)
        }

        composable(AppRoutes.SETTINGS_SCREEN) {
            SettingsScreen(appNavigator, firebaseViewModel)
        }

        composable(AppRoutes.LOGIN_SCREEN) {
            LoginScreen(appNavigator)
        }
        composable(AppRoutes.SIGNUP_SCREEN) {
            SignupScreen(appNavigator)
        }

        composable(AppRoutes.GROCERY_LIST_SCREEN) {
            GroceryListScreen(appNavigator, firebaseViewModel)
        }

        composable(AppRoutes.RECIPES_SCREEN) {
            RecipesScreen(appNavigator, firebaseViewModel)
        }

        composable(AppRoutes.CREATE_RECIPE_SCREEN) {
            RecipeDetailsScreen(appNavigator, firebaseViewModel, null)
        }

        composable(
            route = "${AppRoutes.RECIPE_DETAILS_SCREEN}/{${AppRoutes.RECIPE_ID_ARG}}",
            arguments = listOf(navArgument(AppRoutes.RECIPE_ID_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString(AppRoutes.RECIPE_ID_ARG)
            RecipeDetailsScreen(appNavigator, firebaseViewModel, recipeId)
        }

        composable(AppRoutes.GALLERY_SCREEN) {
            GalleryScreen(appNavigator, firebaseViewModel)
        }

        composable(
            route = "${AppRoutes.ALBUM_MEDIA_SCREEN}/{${AppRoutes.ALBUM_MEDIA_ID_ARG}}",
            arguments = listOf(navArgument(AppRoutes.ALBUM_MEDIA_ID_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString(AppRoutes.ALBUM_MEDIA_ID_ARG)
                ?: return@composable
            AlbumDetailsScreen(appNavigator, firebaseViewModel, albumId)
        }

        composable(
            route = "${AppRoutes.GALLERY_MEDIA_SCREEN}/{${AppRoutes.GALLERY_MEDIA_ALBUM_ARG}}/{${AppRoutes.GALLERY_MEDIA_INDEX_ARG}}",
            arguments = listOf(
                navArgument(AppRoutes.GALLERY_MEDIA_ALBUM_ARG) { type = NavType.StringType },
                navArgument(AppRoutes.GALLERY_MEDIA_INDEX_ARG) { type = NavType.IntType }
            ),
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString(AppRoutes.GALLERY_MEDIA_ALBUM_ARG)
                ?: return@composable
            val initialIndex = backStackEntry.arguments?.getInt(AppRoutes.GALLERY_MEDIA_INDEX_ARG)
                ?: 0
            MediaDetailsScreen(appNavigator, firebaseViewModel, albumId, initialIndex)
        }

        // Nested graph
        navigation(startDestination = AppRoutes.TIP_TRACKER_SCREEN, route = AppRoutes.TIP_TRACKER_GRAPH) {

            composable(AppRoutes.TIP_TRACKER_SCREEN) { backStackEntry ->
                // Get the ViewModel scoped to "AppRoutes.TIP_TRACKER_GRAPH"
                val sharedEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AppRoutes.TIP_TRACKER_GRAPH)
                }
                val viewModel: TipTrackerViewModel = hiltViewModel(sharedEntry)

                TipTrackerScreen(appNavigator, firebaseViewModel, viewModel)
            }

            composable(AppRoutes.TIP_STATISTICS_SCREEN) { backStackEntry ->
                // Get the SAME ViewModel scoped to "AppRoutes.TIP_TRACKER_GRAPH"
                val sharedEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AppRoutes.TIP_TRACKER_GRAPH)
                }
                val viewModel: TipTrackerViewModel = hiltViewModel(sharedEntry)

                TipStatisticsScreen(appNavigator, firebaseViewModel, viewModel)
            }
        }
    }
}
