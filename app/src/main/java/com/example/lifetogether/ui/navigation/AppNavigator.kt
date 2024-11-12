package com.example.lifetogether.ui.navigation

import androidx.navigation.NavController

class AppNavigator(private val navController: NavController) : Navigator {
    override fun navigateToAdmin() {
        navController.navigate(AppRoutes.ADMIN_SCREEN)
    }
    override fun navigateToAdminGroceryList() {
        navController.navigate(AppRoutes.ADMIN_GROCERY_LIST_SCREEN)
    }
    override fun navigateToHome() {
        navController.navigate(AppRoutes.HOME_SCREEN)
    }

    override fun navigateToProfile() {
        navController.navigate(AppRoutes.PROFILE_SCREEN)
    }

    override fun navigateToSettings() {
        navController.navigate(AppRoutes.SETTINGS_SCREEN)
    }

    override fun navigateToGroceryList() {
        navController.navigate(AppRoutes.GROCERY_LIST_SCREEN)
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
