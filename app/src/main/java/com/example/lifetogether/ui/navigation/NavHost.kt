package com.example.lifetogether.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.aca.ui.feature.login.LoginScreen
import com.example.lifetogether.ui.feature.home.HomeScreen

@Composable
fun NavHost(
    navController: NavHostController,
) {
    val appNavigator = AppNavigator(navController)

    androidx.navigation.compose.NavHost(
        navController = navController,
        startDestination = "home",
    ) {
        composable("home") {
            HomeScreen(appNavigator)
        }

        composable("login") {
            LoginScreen(appNavigator)
        }

        composable("profile") {
        }
    }
}
