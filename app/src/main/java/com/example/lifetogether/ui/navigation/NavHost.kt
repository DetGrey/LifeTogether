package com.example.lifetogether.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.lifetogether.ui.feature.groceryList.GroceryListScreen
import com.example.lifetogether.ui.feature.home.HomeScreen
import com.example.lifetogether.ui.feature.login.LoginScreen
import com.example.lifetogether.ui.feature.profile.ProfileScreen
import com.example.lifetogether.ui.feature.settings.SettingsScreen
import com.example.lifetogether.ui.feature.signup.SignupScreen
import com.example.lifetogether.ui.viewmodel.AuthViewModel

@Composable
fun NavHost(
    navController: NavHostController,
) {
    val appNavigator = AppNavigator(navController)
    val authViewModel: AuthViewModel = viewModel()

    androidx.navigation.compose.NavHost(
        navController = navController,
        startDestination = "home",
    ) {
        composable("home") {
            HomeScreen(appNavigator, authViewModel)
        }

        composable("profile") {
            ProfileScreen(appNavigator, authViewModel)
        }

        composable("settings") {
            SettingsScreen(appNavigator, authViewModel)
        }

        composable("grocery") {
            GroceryListScreen(appNavigator, authViewModel)
        }

        composable("login") {
            LoginScreen(appNavigator, authViewModel)
        }
        composable("signup") {
            SignupScreen(appNavigator, authViewModel)
        }
    }
}
