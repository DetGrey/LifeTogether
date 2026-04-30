package com.example.lifetogether.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
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
import com.example.lifetogether.ui.feature.guides.details.GuideDetailsRoute
import com.example.lifetogether.ui.feature.guides.stepplayer.GuideStepPlayerRoute
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
                val guideGraphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<GuideGraph>()
                }

                GuideDetailsRoute(
                    appNavigator = appNavigator,
                    viewModelStoreOwner = guideGraphEntry,
                )
            }

            composable<GuideStepPlayerNavRoute> { backStackEntry ->
                val guideGraphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<GuideGraph>()
                }

                GuideStepPlayerRoute(
                    appNavigator = appNavigator,
                    viewModelStoreOwner = guideGraphEntry,
                )
            }
        }

        composable<CreateRecipeNavRoute> { CreateRecipeRoute(appNavigator) }
        composable<RecipeDetailsNavRoute> {
            RecipeDetailsRoute(appNavigator = appNavigator)
        }

        navigation<GalleryGraph>(startDestination = GalleryNavRoute::class) {
            composable<GalleryNavRoute> { GalleryScreenRoute(appNavigator) }
            composable<AlbumMediaNavRoute> { AlbumDetailsRoute(appNavigator) }
            composable<GalleryMediaNavRoute> { MediaDetailsRoute(appNavigator) }
        }

        composable<ListsNavRoute> { ListsRoute(appNavigator) }
        composable<ListDetailNavRoute> { ListDetailsRoute(appNavigator = appNavigator) }
        composable<ListEntryDetailsNavRoute> { ListEntryDetailsRoute(appNavigator = appNavigator) }

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
