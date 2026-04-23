package com.example.lifetogether.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.lifetogether.ui.feature.admin.groceryList.categories.AdminGroceryCategoriesRoute
import com.example.lifetogether.ui.feature.admin.groceryList.suggestions.AdminGrocerySuggestionsRoute
import com.example.lifetogether.ui.feature.family.FamilyRoute
import com.example.lifetogether.ui.feature.gallery.AlbumDetailsRoute
import com.example.lifetogether.ui.feature.gallery.GalleryGraphObserverRoute
import com.example.lifetogether.ui.feature.gallery.GalleryScreenRoute
import com.example.lifetogether.ui.feature.gallery.MediaDetailsRoute
import com.example.lifetogether.ui.feature.guides.GuidesRoute
import com.example.lifetogether.ui.feature.lists.listDetails.ListDetailsRoute
import com.example.lifetogether.ui.feature.lists.entryDetails.ListEntryDetailsRoute
import com.example.lifetogether.ui.feature.lists.ListsRoute
import com.example.lifetogether.ui.feature.guides.create.GuideCreateScreen
import com.example.lifetogether.ui.feature.guides.details.GuideDetailsDestinationRoute
import com.example.lifetogether.ui.feature.guides.details.GuideStepPlayerDestinationRoute
import com.example.lifetogether.ui.feature.groceryList.GroceryListRoute
import com.example.lifetogether.ui.feature.home.HomeRoute
import com.example.lifetogether.ui.feature.loading.LoadingRoute
import com.example.lifetogether.ui.feature.login.LoginRoute
import com.example.lifetogether.ui.feature.profile.ProfileRoute
import com.example.lifetogether.ui.feature.recipes.CreateRecipeRoute
import com.example.lifetogether.ui.feature.recipes.RecipeDetailsRoute
import com.example.lifetogether.ui.feature.recipes.RecipesRoute
import com.example.lifetogether.ui.feature.settings.SettingsRoute
import com.example.lifetogether.ui.feature.signup.SignupRoute
import com.example.lifetogether.ui.feature.tipTracker.TipTrackerRoute
import com.example.lifetogether.ui.feature.tipTracker.statistics.TipStatisticsRoute

@Composable
fun NavHost(
    navController: NavHostController,
) {
    val appNavigator = AppNavigator(navController)
    GalleryGraphObserverRoute(navController = navController)

    androidx.navigation.compose.NavHost(
        navController = navController,
        startDestination = AppRoutes.LOADING_SCREEN,
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
        composable(AppRoutes.ADMIN_GROCERY_CATEGORIES_SCREEN) {
            AdminGroceryCategoriesRoute(appNavigator)
        }
        composable(AppRoutes.ADMIN_GROCERY_SUGGESTIONS_SCREEN) {
            AdminGrocerySuggestionsRoute(appNavigator)
        }

        composable(AppRoutes.LOADING_SCREEN) {
            LoadingRoute(appNavigator)
        }

        composable(AppRoutes.HOME_SCREEN) {
            HomeRoute(appNavigator)
        }

        composable(AppRoutes.PROFILE_SCREEN) {
            ProfileRoute(appNavigator)
        }

        composable(AppRoutes.FAMILY_SCREEN) {
            FamilyRoute(appNavigator)
        }

        composable(AppRoutes.SETTINGS_SCREEN) {
            SettingsRoute(appNavigator)
        }

        composable(AppRoutes.LOGIN_SCREEN) {
            LoginRoute(appNavigator)
        }
        composable(AppRoutes.SIGNUP_SCREEN) {
            SignupRoute(appNavigator)
        }

        composable(AppRoutes.GROCERY_LIST_SCREEN) {
            GroceryListRoute(appNavigator)
        }

        composable(AppRoutes.RECIPES_SCREEN) {
            RecipesRoute(appNavigator)
        }

        composable(AppRoutes.GUIDES_SCREEN) {
            GuidesRoute(appNavigator)
        }

        composable(AppRoutes.GUIDE_CREATE_SCREEN) {
            GuideCreateScreen(appNavigator)
        }

        navigation(
            route = AppRoutes.GUIDE_GRAPH_ROUTE,
            arguments = listOf(navArgument(AppRoutes.GUIDE_ID_ARG) { type = NavType.StringType }),
            startDestination = AppRoutes.GUIDE_DETAILS_SCREEN,
        ) {
            composable(AppRoutes.GUIDE_DETAILS_SCREEN) { backStackEntry ->
                GuideDetailsDestinationRoute(
                    navController = navController,
                    backStackEntry = backStackEntry,
                    appNavigator = appNavigator,
                )
            }

            composable(AppRoutes.GUIDE_STEP_PLAYER_SCREEN) { backStackEntry ->
                GuideStepPlayerDestinationRoute(
                    navController = navController,
                    backStackEntry = backStackEntry,
                    appNavigator = appNavigator,
                )
            }
        }

        composable(AppRoutes.CREATE_RECIPE_SCREEN) {
            CreateRecipeRoute(appNavigator)
        }

        composable(
            route = "${AppRoutes.RECIPE_DETAILS_SCREEN}/{${AppRoutes.RECIPE_ID_ARG}}",
            arguments = listOf(navArgument(AppRoutes.RECIPE_ID_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            RecipeDetailsRoute(backStackEntry, appNavigator)
        }

        navigation(
            route = AppRoutes.GALLERY_GRAPH,
            startDestination = AppRoutes.GALLERY_SCREEN,
        ) {
            composable(AppRoutes.GALLERY_SCREEN) {
                GalleryScreenRoute(appNavigator)
            }

            composable(
                route = "${AppRoutes.ALBUM_MEDIA_SCREEN}/{${AppRoutes.ALBUM_MEDIA_ID_ARG}}",
                arguments = listOf(navArgument(AppRoutes.ALBUM_MEDIA_ID_ARG) { type = NavType.StringType }),
            ) { backStackEntry ->
                val albumId = backStackEntry.arguments?.getString(AppRoutes.ALBUM_MEDIA_ID_ARG)
                    ?: return@composable
                AlbumDetailsRoute(appNavigator, albumId)
            }

            composable(
                route = "${AppRoutes.GALLERY_MEDIA_SCREEN}/{${AppRoutes.GALLERY_MEDIA_ALBUM_ARG}}/{${AppRoutes.GALLERY_MEDIA_INDEX_ARG}}",
                arguments = listOf(
                    navArgument(AppRoutes.GALLERY_MEDIA_ALBUM_ARG) { type = NavType.StringType },
                    navArgument(AppRoutes.GALLERY_MEDIA_INDEX_ARG) { type = NavType.IntType },
                ),
            ) { backStackEntry ->
                val albumId = backStackEntry.arguments?.getString(AppRoutes.GALLERY_MEDIA_ALBUM_ARG)
                    ?: return@composable
                val initialIndex = backStackEntry.arguments?.getInt(AppRoutes.GALLERY_MEDIA_INDEX_ARG)
                    ?: 0
                MediaDetailsRoute(appNavigator, albumId, initialIndex)
            }
        }

        composable(AppRoutes.LISTS_SCREEN) {
            ListsRoute(appNavigator)
        }

        composable(
            route = "${AppRoutes.LIST_DETAIL_SCREEN}/{${AppRoutes.LIST_ID_ARG}}",
            arguments = listOf(navArgument(AppRoutes.LIST_ID_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString(AppRoutes.LIST_ID_ARG) ?: return@composable
            ListDetailsRoute(
                listId = listId,
                appNavigator = appNavigator,
            )
        }

        composable(
            route = "${AppRoutes.LIST_ENTRY_DETAILS_SCREEN}/{${AppRoutes.LIST_ID_ARG}}",
            arguments = listOf(navArgument(AppRoutes.LIST_ID_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString(AppRoutes.LIST_ID_ARG) ?: return@composable
            ListEntryDetailsRoute(
                listId = listId,
                entryId = null,
                appNavigator = appNavigator,
            )
        }

        composable(
            route = "${AppRoutes.LIST_ENTRY_DETAILS_SCREEN}/{${AppRoutes.LIST_ID_ARG}}/{${AppRoutes.LIST_ENTRY_ID_ARG}}",
            arguments = listOf(
                navArgument(AppRoutes.LIST_ID_ARG) { type = NavType.StringType },
                navArgument(AppRoutes.LIST_ENTRY_ID_ARG) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString(AppRoutes.LIST_ID_ARG) ?: return@composable
            val entryId = backStackEntry.arguments?.getString(AppRoutes.LIST_ENTRY_ID_ARG)
            ListEntryDetailsRoute(
                listId = listId,
                entryId = entryId,
                appNavigator = appNavigator,
            )
        }

        // Nested graph
        navigation(startDestination = AppRoutes.TIP_TRACKER_SCREEN, route = AppRoutes.TIP_TRACKER_GRAPH) {

            composable(AppRoutes.TIP_TRACKER_SCREEN) { backStackEntry ->
                TipTrackerRoute(
                    navController = navController,
                    backStackEntry = backStackEntry,
                    appNavigator = appNavigator,
                )
            }

            composable(AppRoutes.TIP_STATISTICS_SCREEN) { backStackEntry ->
                TipStatisticsRoute(
                    navController = navController,
                    backStackEntry = backStackEntry,
                    appNavigator = appNavigator,
                )
            }
        }
    }
}
