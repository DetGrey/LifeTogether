package com.example.lifetogether.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.example.lifetogether.ui.feature.admin.groceryList.AdminGroceryCategoriesRoute
import com.example.lifetogether.ui.feature.admin.groceryList.AdminGrocerySuggestionsRoute
import com.example.lifetogether.ui.feature.family.FamilyScreen
import com.example.lifetogether.ui.feature.gallery.AlbumDetailsRoute
import com.example.lifetogether.ui.feature.gallery.GalleryGraphObserverRoute
import com.example.lifetogether.ui.feature.gallery.GalleryScreenRoute
import com.example.lifetogether.ui.feature.gallery.MediaDetailsRoute
import com.example.lifetogether.ui.feature.guides.GuidesRoute
import com.example.lifetogether.ui.feature.guides.create.GuideCreateScreen
import com.example.lifetogether.ui.feature.guides.details.GuideDetailsDestinationRoute
import com.example.lifetogether.ui.feature.guides.details.GuideStepPlayerDestinationRoute
import com.example.lifetogether.ui.feature.groceryList.GroceryListRoute
import com.example.lifetogether.ui.feature.home.HomeScreen
import com.example.lifetogether.ui.feature.lists.ListsRoute
import com.example.lifetogether.ui.feature.lists.entryDetails.ListEntryDetailsRoute
import com.example.lifetogether.ui.feature.lists.listDetails.ListDetailsRoute
import com.example.lifetogether.ui.feature.loading.LoadingScreen
import com.example.lifetogether.ui.feature.login.LoginScreen
import com.example.lifetogether.ui.feature.profile.ProfileScreen
import com.example.lifetogether.ui.feature.recipes.CreateRecipeRoute
import com.example.lifetogether.ui.feature.recipes.RecipeDetailsRoute
import com.example.lifetogether.ui.feature.recipes.RecipesRoute
import com.example.lifetogether.ui.feature.settings.SettingsScreen
import com.example.lifetogether.ui.feature.signup.SignupScreen
import com.example.lifetogether.ui.feature.tipTracker.TipStatisticsRoute
import com.example.lifetogether.ui.feature.tipTracker.TipTrackerRoute

@Composable
fun NavHost(
    navController: NavHostController,
) {
    val appNavigator = AppNavigator(navController)
    GalleryGraphObserverRoute(navController = navController)

    androidx.navigation.compose.NavHost(
        navController = navController,
        startDestination = LoadingNavRoute,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(200)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(200)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(200)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(200)
            )
        },
    ) {
        composable<AdminGroceryCategoriesNavRoute> {
            AdminGroceryCategoriesRoute(appNavigator)
        }
        composable<AdminGrocerySuggestionsNavRoute> {
            AdminGrocerySuggestionsRoute(appNavigator)
        }

        composable<LoadingNavRoute> {
            LoadingScreen(appNavigator)
        }

        composable<HomeNavRoute> {
            HomeScreen(appNavigator)
        }

        composable<ProfileNavRoute> {
            ProfileScreen(appNavigator)
        }

        composable<FamilyNavRoute> {
            FamilyScreen(appNavigator)
        }

        composable<SettingsNavRoute> {
            SettingsScreen(appNavigator)
        }

        composable<LoginNavRoute> {
            LoginScreen(appNavigator)
        }

        composable<SignupNavRoute> {
            SignupScreen(appNavigator)
        }

        composable<GroceryListNavRoute> {
            GroceryListRoute(appNavigator)
        }

        composable<RecipesNavRoute> {
            RecipesRoute(appNavigator)
        }

        composable<GuidesNavRoute> {
            GuidesRoute(appNavigator)
        }

        composable<GuideCreateNavRoute> {
            GuideCreateScreen(appNavigator)
        }

        navigation<GuideGraph>(startDestination = GuideDetailsNavRoute::class) {
            composable<GuideDetailsNavRoute> { backStackEntry ->
                val guideId = backStackEntry.toRoute<GuideDetailsNavRoute>().guideId
                GuideDetailsDestinationRoute(
                    navController = navController,
                    backStackEntry = backStackEntry,
                    guideId = guideId,
                    appNavigator = appNavigator,
                )
            }
            composable<GuideStepPlayerNavRoute> { backStackEntry ->
                val guideId = backStackEntry.toRoute<GuideStepPlayerNavRoute>().guideId
                GuideStepPlayerDestinationRoute(
                    navController = navController,
                    backStackEntry = backStackEntry,
                    guideId = guideId,
                    appNavigator = appNavigator,
                )
            }
        }

        composable<CreateRecipeNavRoute> {
            CreateRecipeRoute(appNavigator)
        }

        composable<RecipeDetailsNavRoute> { backStackEntry ->
            val recipeId = backStackEntry.toRoute<RecipeDetailsNavRoute>().recipeId
            RecipeDetailsRoute(recipeId = recipeId, appNavigator = appNavigator)
        }

        navigation<GalleryGraph>(startDestination = GalleryNavRoute::class) {
            composable<GalleryNavRoute> {
                GalleryScreenRoute(appNavigator)
            }
            composable<AlbumMediaNavRoute> { backStackEntry ->
                val albumId = backStackEntry.toRoute<AlbumMediaNavRoute>().albumId
                AlbumDetailsRoute(appNavigator, albumId)
            }
            composable<GalleryMediaNavRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<GalleryMediaNavRoute>()
                MediaDetailsRoute(appNavigator, route.albumId, route.initialIndex)
            }
        }

        composable<ListsNavRoute> {
            ListsRoute(appNavigator)
        }

        composable<ListDetailNavRoute> { backStackEntry ->
            val listId = backStackEntry.toRoute<ListDetailNavRoute>().listId
            ListDetailsRoute(
                listId = listId,
                appNavigator = appNavigator,
            )
        }

        composable<ListEntryDetailsNavRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ListEntryDetailsNavRoute>()
            ListEntryDetailsRoute(
                listId = route.listId,
                entryId = route.entryId,
                appNavigator = appNavigator,
            )
        }

        navigation<TipTrackerGraph>(startDestination = TipTrackerNavRoute::class) {
            composable<TipTrackerNavRoute> {
                TipTrackerRoute(
                    navController = navController,
                    appNavigator = appNavigator,
                )
            }
            composable<TipStatisticsNavRoute> {
                TipStatisticsRoute(
                    navController = navController,
                    appNavigator = appNavigator,
                )
            }
        }
    }
}
