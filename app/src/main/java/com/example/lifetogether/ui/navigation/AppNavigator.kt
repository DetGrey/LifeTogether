package com.example.lifetogether.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.navOptions

class AppNavigator(private val navController: NavController) : Navigator {

    override fun navigate(route: AppRoute) {
        val options = navOptions {
            launchSingleTop = true
            if (route is GalleryNavRoute) {
                popUpTo<GalleryNavRoute> { inclusive = false }
            }
        }
        navController.navigate(route, options)
    }

    override fun navigateBack() {
        if (!navController.popBackStack()) {
            navController.navigate(HomeNavRoute, navOptions { launchSingleTop = true })
        }
    }
}
