package com.example.lifetogether.ui.navigation

import android.net.Uri
import androidx.navigation.NavController

class AppNavigator(private val navController: NavController) : Navigator {

    override fun navigateToAdminGroceryCategories() {
        navController.navigate(AppRoutes.ADMIN_GROCERY_CATEGORIES_SCREEN)
    }

    override fun navigateToAdminGrocerySuggestions() {
        navController.navigate(AppRoutes.ADMIN_GROCERY_SUGGESTIONS_SCREEN)
    }
    override fun navigateToHome() {
        navController.navigate(AppRoutes.HOME_SCREEN)
    }

    override fun navigateToProfile() {
        navController.navigate(AppRoutes.PROFILE_SCREEN)
    }

    override fun navigateToFamily() {
        navController.navigate(AppRoutes.FAMILY_SCREEN)
    }

    override fun navigateToSettings() {
        navController.navigate(AppRoutes.SETTINGS_SCREEN)
    }

    override fun navigateToLogin() {
        navController.navigate(AppRoutes.LOGIN_SCREEN)
    }

    override fun navigateToSignUp() {
        navController.navigate(AppRoutes.SIGNUP_SCREEN)
    }

    override fun navigateBack() {
        if (!navController.popBackStack()) {
            navController.navigate(AppRoutes.HOME_SCREEN)
        }
    }

    override fun navigateToGroceryList() {
        navController.navigate(AppRoutes.GROCERY_LIST_SCREEN)
    }

    override fun navigateToRecipes() {
        navController.navigate(AppRoutes.RECIPES_SCREEN)
    }

    override fun navigateToRecipeDetails(recipeId: String?) {
        val route = recipeId?.let {
            "${AppRoutes.RECIPE_DETAILS_SCREEN}/$it"
        } ?: AppRoutes.CREATE_RECIPE_SCREEN
        navController.navigate(route)
    }

    override fun navigateToGuides() {
        navController.navigate(AppRoutes.GUIDES_SCREEN)
    }

    override fun navigateToGuideDetails(guideId: String) {
        val route = "${AppRoutes.GUIDE_GRAPH}/${Uri.encode(guideId)}"
        navController.navigate(route)
    }

    override fun navigateToGuideStepPlayer() {
        navController.navigate(AppRoutes.GUIDE_STEP_PLAYER_SCREEN)
    }

    override fun navigateToGuideCreate() {
        navController.navigate(AppRoutes.GUIDE_CREATE_SCREEN)
    }

    override fun navigateToGallery() {
        navController.navigate(AppRoutes.GALLERY_SCREEN) {
            launchSingleTop = true
            // Clear any album or media screens when returning to gallery
            popUpTo(AppRoutes.GALLERY_SCREEN) {
                inclusive = false
            }
        }
    }

    override fun navigateToAlbumMedia(albumId: String) {
        val route = "${AppRoutes.ALBUM_MEDIA_SCREEN}/$albumId"
        navController.navigate(route) {
            // Clear any existing album/media screens from back stack
            popUpTo(AppRoutes.GALLERY_SCREEN) {
                inclusive = false
            }
        }
    }

    override fun navigateToGalleryMedia(albumId: String, initialIndex: Int) {
        val route = "${AppRoutes.GALLERY_MEDIA_SCREEN}/$albumId/$initialIndex"
        navController.navigate(route)
    }

    override fun navigateToTipTracker() {
        navController.navigate(AppRoutes.TIP_TRACKER_SCREEN)
    }

    override fun navigateToTipStatistics() {
        navController.navigate(AppRoutes.TIP_STATISTICS_SCREEN)
    }
}

/*
// ViewModel using the navigator
class SomeViewModel(private val navigator: AppNavigator) : ViewModel() {
    fun onLoginSelected() {
        navigator.navigateToLogin()
    }

    fun onSignUpSelected() {
        navigator.navigateToSignUp()
    }
    // ... other ViewModel logic
}
 */
