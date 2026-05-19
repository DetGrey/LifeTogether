package com.example.lifetogether.ui.navigation

import androidx.navigation.NavDestination.Companion.hasRoute
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

    fun clearAndNavigate(route: AppRoute) {
        navController.navigate(
            route,
            navOptions {
                launchSingleTop = true
                popUpTo<LoadingNavRoute> {
                    inclusive = true
                }
            },
        )
    }

    fun navigateTopLevel(route: AppRoute) {
        when (route) {
            is HomeNavRoute -> popToHome()
            is ProfileNavRoute -> navigateProfileOrSettings(route)
            is SettingsNavRoute -> navigateProfileOrSettings(route)
            else -> {
                popToHome()
                navController.navigate(route, navOptions { launchSingleTop = true })
            }
        }
    }

    override fun navigateBack() {
        if (!navController.popBackStack()) {
            navController.navigate(HomeNavRoute, navOptions { launchSingleTop = true })
        }
    }

    private fun navigateProfileOrSettings(route: AppRoute) {
        when (route) {
            is ProfileNavRoute -> {
                if (isCurrentRoute<SettingsNavRoute>() && isPreviousRoute<ProfileNavRoute>()) {
                    navController.popBackStack()
                } else if (!isCurrentRoute<ProfileNavRoute>()) {
                    if (!isCurrentRoute<HomeNavRoute>() && !isCurrentRoute<SettingsNavRoute>()) {
                        popToHome()
                    }
                    navController.navigate(route, navOptions { launchSingleTop = true })
                }
            }

            is SettingsNavRoute -> {
                if (isCurrentRoute<ProfileNavRoute>() && isPreviousRoute<SettingsNavRoute>()) {
                    navController.popBackStack()
                } else if (!isCurrentRoute<SettingsNavRoute>()) {
                    if (!isCurrentRoute<HomeNavRoute>() && !isCurrentRoute<ProfileNavRoute>()) {
                        popToHome()
                    }
                    navController.navigate(route, navOptions { launchSingleTop = true })
                }
            }

            else -> Unit
        }
    }

    private fun popToHome() {
        if (!navController.popBackStack<HomeNavRoute>(inclusive = false)) {
            navController.navigate(HomeNavRoute, navOptions { launchSingleTop = true })
        }
    }

    private inline fun <reified T : Any> isCurrentRoute(): Boolean {
        return navController.currentBackStackEntry?.destination?.hasRoute(T::class) == true
    }

    private inline fun <reified T : Any> isPreviousRoute(): Boolean {
        return navController.previousBackStackEntry?.destination?.hasRoute(T::class) == true
    }
}
