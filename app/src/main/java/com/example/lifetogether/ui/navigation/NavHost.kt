package com.example.lifetogether.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.lifetogether.ui.common.sync.RouteSyncBinding
import com.example.lifetogether.ui.feature.admin.groceryList.categories.AdminGroceryCategoriesRoute
import com.example.lifetogether.ui.feature.admin.groceryList.suggestions.AdminGrocerySuggestionsRoute
import com.example.lifetogether.ui.feature.family.FamilyRoute
import com.example.lifetogether.ui.feature.gallery.AlbumDetailsRoute
import com.example.lifetogether.ui.feature.gallery.GalleryScreenRoute
import com.example.lifetogether.ui.feature.gallery.MediaDetailsRoute
import com.example.lifetogether.ui.feature.groceryList.GroceryListRoute
import com.example.lifetogether.ui.feature.guides.GuidesRoute
import com.example.lifetogether.ui.feature.guides.edit.GuideEditRoute
import com.example.lifetogether.ui.feature.guides.details.GuideDetailsRoute
import com.example.lifetogether.ui.feature.guides.stepplayer.GuideStepPlayerRoute
import com.example.lifetogether.ui.feature.home.HomeRoute
import com.example.lifetogether.ui.feature.lists.ListsRoute
import com.example.lifetogether.ui.feature.lists.entryDetails.ListEntryDetailsRoute
import com.example.lifetogether.ui.feature.lists.listDetails.ListDetailsRoute
import com.example.lifetogether.ui.feature.loading.LoadingRoute
import com.example.lifetogether.ui.feature.login.LoginRoute
import com.example.lifetogether.ui.feature.mealPlanner.MealPlannerRoute
import com.example.lifetogether.ui.feature.mealPlanner.entryDetails.MealPlanDetailsRoute
import com.example.lifetogether.ui.feature.settings.notifications.NotificationsRoute
import com.example.lifetogether.ui.feature.profile.ProfileRoute
import com.example.lifetogether.ui.feature.recipes.RecipesRoute
import com.example.lifetogether.ui.feature.recipes.details.RecipeDetailsRoute
import com.example.lifetogether.ui.feature.settings.SettingsRoute
import com.example.lifetogether.ui.feature.signup.SignupRoute
import com.example.lifetogether.ui.feature.tipTracker.TipTrackerRoute
import com.example.lifetogether.ui.feature.tipTracker.statistics.TipStatisticsRoute

private const val RouteTransitionDurationMillis = 450
private const val RouteTransitionFadeInitialAlpha = 0.92f

@Composable
fun NavHost(deepLinkRoutes: List<AppRoute>? = null) {
    val backStack = rememberNavBackStack(LoadingNavRoute)
    val appNavigator = remember(backStack) { AppNavigator(backStack) }

    // ViewModelStore map for graph-scoped entries (TipTrackerGraph).
    // Stores are keyed by the graph marker route object and cleared when that
    // marker is no longer in the back stack.
    val graphStores = remember { HashMap<NavKey, ViewModelStore>() }
    SideEffect {
        val staleKeys = graphStores.keys.filter { key -> !backStack.contains(key) }
        staleKeys.forEach { key -> graphStores.remove(key)?.clear() }
    }

    // Sync activation driven directly by the current top route
    val currentRoute = backStack.lastOrNull() as? AppRoute
    RouteSyncBinding(route = currentRoute)

    val enterTransition = slideInHorizontally(
        animationSpec = tween(RouteTransitionDurationMillis, easing = FastOutSlowInEasing),
        initialOffsetX = { it },
    ) + fadeIn(
        animationSpec = tween(RouteTransitionDurationMillis, easing = FastOutSlowInEasing),
        initialAlpha = RouteTransitionFadeInitialAlpha,
    )
    val exitTransition = slideOutHorizontally(
        animationSpec = tween(RouteTransitionDurationMillis, easing = FastOutSlowInEasing),
        targetOffsetX = { -it },
    ) + fadeOut(
        animationSpec = tween(RouteTransitionDurationMillis, easing = FastOutSlowInEasing),
        targetAlpha = RouteTransitionFadeInitialAlpha,
    )
    val popEnterTransition = slideInHorizontally(
        animationSpec = tween(RouteTransitionDurationMillis, easing = FastOutSlowInEasing),
        initialOffsetX = { -it },
    ) + fadeIn(
        animationSpec = tween(RouteTransitionDurationMillis, easing = FastOutSlowInEasing),
        initialAlpha = RouteTransitionFadeInitialAlpha,
    )
    val popExitTransition = slideOutHorizontally(
        animationSpec = tween(RouteTransitionDurationMillis, easing = FastOutSlowInEasing),
        targetOffsetX = { it },
    ) + fadeOut(
        animationSpec = tween(RouteTransitionDurationMillis, easing = FastOutSlowInEasing),
        targetAlpha = RouteTransitionFadeInitialAlpha,
    )

    NavDisplay(
        backStack = backStack,
        onBack = { appNavigator.navigateBack() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = { enterTransition togetherWith exitTransition },
        popTransitionSpec = { popEnterTransition togetherWith popExitTransition },
        predictivePopTransitionSpec = { popEnterTransition togetherWith popExitTransition },
        entryProvider = entryProvider {
            // ─── Auth / loading ────────────────────────────────────────────
            entry<LoadingNavRoute> { LoadingRoute(appNavigator, deepLinkRoutes) }
            entry<LoginNavRoute> { LoginRoute(appNavigator) }
            entry<SignupNavRoute> { SignupRoute(appNavigator) }

            // ─── Top-level ─────────────────────────────────────────────────
            entry<HomeNavRoute> { HomeRoute(appNavigator) }
            entry<ProfileNavRoute> { ProfileRoute(appNavigator) }
            entry<FamilyNavRoute> { FamilyRoute(appNavigator) }

            // ─── Settings ───────────────────────────────────────────────────
            entry<SettingsNavRoute> { SettingsRoute(appNavigator) }
            entry<NotificationsNavRoute> { NotificationsRoute(appNavigator) }

            // ─── Admin ─────────────────────────────────────────────────────
            entry<AdminGroceryCategoriesNavRoute> { AdminGroceryCategoriesRoute(appNavigator) }
            entry<AdminGrocerySuggestionsNavRoute> { AdminGrocerySuggestionsRoute(appNavigator) }

            // ─── Grocery ───────────────────────────────────────────────────
            entry<GroceryListNavRoute> { GroceryListRoute(appNavigator) }

            // ─── Recipes ───────────────────────────────────────────────────
            entry<RecipesNavRoute> { RecipesRoute(appNavigator) }
            entry<RecipeDetailsNavRoute> { key ->
                RecipeDetailsRoute(appNavigator = appNavigator, recipeId = key.recipeId)
            }

            // ─── Guides ────────────────────────────────────────────────────
            entry<GuidesNavRoute> { GuidesRoute(appNavigator) }
            entry<GuideEditNavRoute> { key ->
                GuideEditRoute(appNavigator = appNavigator, guideId = key.guideId)
            }
            entry<GuideDetailsNavRoute> { key ->
                GuideDetailsRoute(appNavigator = appNavigator, guideId = key.guideId)
            }
            entry<GuideStepPlayerNavRoute> { key ->
                GuideStepPlayerRoute(appNavigator = appNavigator, guideId = key.guideId)
            }

            // ─── Gallery ───────────────────────────────────────────────────
            entry<GalleryNavRoute> { GalleryScreenRoute(appNavigator) }
            entry<AlbumMediaNavRoute> { key ->
                AlbumDetailsRoute(appNavigator = appNavigator, albumId = key.albumId)
            }
            entry<GalleryMediaNavRoute> { key ->
                MediaDetailsRoute(
                    appNavigator = appNavigator,
                    albumId = key.albumId,
                    initialIndex = key.initialIndex,
                )
            }

            // ─── Meal planner ──────────────────────────────────────────────
            entry<MealPlannerNavRoute> { MealPlannerRoute(appNavigator) }
            entry<MealPlanDetailsNavRoute> { key ->
                MealPlanDetailsRoute(appNavigator = appNavigator, routeKey = key)
            }

            // ─── Lists ─────────────────────────────────────────────────────
            entry<ListsNavRoute> { ListsRoute(appNavigator) }
            entry<ListDetailNavRoute> { key ->
                ListDetailsRoute(appNavigator = appNavigator, listId = key.listId)
            }
            entry<ListEntryDetailsNavRoute> { key ->
                ListEntryDetailsRoute(
                    appNavigator = appNavigator,
                    listId = key.listId,
                    entryId = key.entryId,
                )
            }

            // ─── Tip tracker (shared-scoped via TipTrackerGraph marker) ────
            entry<TipTrackerGraph> { /* invisible scope anchor — no UI */ }
            entry<TipTrackerNavRoute> { _ ->
                val graphOwner = tipTrackerGraphOwner(backStack, graphStores)
                TipTrackerRoute(viewModelStoreOwner = graphOwner, appNavigator = appNavigator)
            }
            entry<TipStatisticsNavRoute> { _ ->
                val graphOwner = tipTrackerGraphOwner(backStack, graphStores)
                TipStatisticsRoute(viewModelStoreOwner = graphOwner, appNavigator = appNavigator)
            }
        },
    )
}

@Composable
private fun tipTrackerGraphOwner(
    backStack: NavBackStack<NavKey>,
    graphStores: HashMap<NavKey, ViewModelStore>,
): ViewModelStoreOwner {
    val graphKey = backStack.firstOrNull { it is TipTrackerGraph } ?: TipTrackerGraph
    val store = remember(graphKey) { graphStores.getOrPut(graphKey) { ViewModelStore() } }
    val parentOwner = checkNotNull(LocalViewModelStoreOwner.current) as HasDefaultViewModelProviderFactory
    return remember(store, parentOwner) {
        object : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
            override val viewModelStore: ViewModelStore = store
            override val defaultViewModelProviderFactory = parentOwner.defaultViewModelProviderFactory
            override val defaultViewModelCreationExtras = parentOwner.defaultViewModelCreationExtras
        }
    }
}
