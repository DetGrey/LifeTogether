package com.example.lifetogether.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.example.lifetogether.ui.feature.admin.groceryList.categories.AdminGroceryCategoriesRoute
import com.example.lifetogether.ui.feature.admin.groceryList.suggestions.AdminGrocerySuggestionsRoute
import com.example.lifetogether.ui.feature.family.FamilyRoute
import com.example.lifetogether.ui.feature.gallery.AlbumDetailsRoute
import com.example.lifetogether.ui.feature.gallery.GalleryGraphObserverRoute
import com.example.lifetogether.ui.feature.gallery.GalleryScreenRoute
import com.example.lifetogether.ui.feature.gallery.MediaDetailsRoute
import com.example.lifetogether.ui.feature.groceryList.GroceryListRoute
import com.example.lifetogether.ui.feature.guides.GuidesRoute
import com.example.lifetogether.ui.feature.guides.create.GuideCreateRoute
import com.example.lifetogether.ui.feature.guides.details.GuideDetailsDestinationRoute
import com.example.lifetogether.ui.feature.guides.details.GuideStepPlayerDestinationRoute
import com.example.lifetogether.ui.feature.home.HomeRoute
import com.example.lifetogether.ui.feature.lists.ListsRoute
import com.example.lifetogether.ui.feature.lists.entryDetails.ListEntryDetailsRoute
import com.example.lifetogether.ui.feature.lists.listDetails.ListDetailsRoute
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
        startDestination = LoadingNavRoute,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(200))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(200))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(200))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(200))
        },
    ) {
        composable<AdminGroceryCategoriesNavRoute> { AdminGroceryCategoriesRoute(appNavigator) }
        composable<AdminGrocerySuggestionsNavRoute> { AdminGrocerySuggestionsRoute(appNavigator) }
        composable<LoadingNavRoute> { LoadingRoute(appNavigator) }
        composable<HomeNavRoute> { HomeRoute(appNavigator) }
        composable<ProfileNavRoute> { ProfileRoute(appNavigator) }
        composable<FamilyNavRoute> { FamilyRoute(appNavigator) }
        composable<SettingsNavRoute> { SettingsRoute(appNavigator) }
        composable<LoginNavRoute> { LoginRoute(appNavigator) }
        composable<SignupNavRoute> { SignupRoute(appNavigator) }
        composable<GroceryListNavRoute> { GroceryListRoute(appNavigator) }
        composable<RecipesNavRoute> { RecipesRoute(appNavigator) }
        composable<GuidesNavRoute> { GuidesRoute(appNavigator) }
        composable<GuideCreateNavRoute> { GuideCreateRoute(appNavigator) }

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

        composable<CreateRecipeNavRoute> { CreateRecipeRoute(appNavigator) }
        composable<RecipeDetailsNavRoute> { backStackEntry ->
            val recipeId = backStackEntry.toRoute<RecipeDetailsNavRoute>().recipeId
            RecipeDetailsRoute(recipeId = recipeId, appNavigator = appNavigator)
        }

        navigation<GalleryGraph>(startDestination = GalleryNavRoute::class) {
            composable<GalleryNavRoute> { GalleryScreenRoute(appNavigator) }
            composable<AlbumMediaNavRoute> { backStackEntry ->
                val albumId = backStackEntry.toRoute<AlbumMediaNavRoute>().albumId
                AlbumDetailsRoute(appNavigator, albumId)
            }
            composable<GalleryMediaNavRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<GalleryMediaNavRoute>()
                MediaDetailsRoute(appNavigator, route.albumId, route.initialIndex)
            }
        }

        composable<ListsNavRoute> { ListsRoute(appNavigator) }
        composable<ListDetailNavRoute> { backStackEntry ->
            val listId = backStackEntry.toRoute<ListDetailNavRoute>().listId
            ListDetailsRoute(listId = listId, appNavigator = appNavigator)
        }
        composable<ListEntryDetailsNavRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ListEntryDetailsNavRoute>()
            ListEntryDetailsRoute(listId = route.listId, entryId = route.entryId, appNavigator = appNavigator)
        }

        navigation<TipTrackerGraph>(startDestination = TipTrackerNavRoute::class) {
            composable<TipTrackerNavRoute> { backStackEntry ->
                val graphEntry = remember(backStackEntry) { navController.getBackStackEntry<TipTrackerGraph>() }
                TipTrackerRoute(viewModelStoreOwner = graphEntry, appNavigator = appNavigator)
            }
            composable<TipStatisticsNavRoute> { backStackEntry ->
                val graphEntry = remember(backStackEntry) { navController.getBackStackEntry<TipTrackerGraph>() }
                TipStatisticsRoute(viewModelStoreOwner = graphEntry, appNavigator = appNavigator)
            }
        }
    }
}
