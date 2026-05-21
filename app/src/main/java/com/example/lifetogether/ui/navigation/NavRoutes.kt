package com.example.lifetogether.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey

@Serializable object AdminGroceryCategoriesNavRoute : AppRoute
@Serializable object AdminGrocerySuggestionsNavRoute : AppRoute
@Serializable object LoadingNavRoute : AppRoute
@Serializable object HomeNavRoute : AppRoute
@Serializable object ProfileNavRoute : AppRoute
@Serializable object FamilyNavRoute : AppRoute
@Serializable object SettingsNavRoute : AppRoute
@Serializable object LoginNavRoute : AppRoute
@Serializable object SignupNavRoute : AppRoute
@Serializable object GroceryListNavRoute : AppRoute
@Serializable object RecipesNavRoute : AppRoute
@Serializable object GuidesNavRoute : AppRoute
@Serializable data class GuideEditNavRoute(val guideId: String? = null) : AppRoute
@Serializable data class RecipeDetailsNavRoute(val recipeId: String? = null) : AppRoute
@Serializable data class GuideDetailsNavRoute(val guideId: String) : AppRoute
@Serializable data class GuideStepPlayerNavRoute(val guideId: String) : AppRoute
@Serializable object GalleryNavRoute : AppRoute
@Serializable data class AlbumMediaNavRoute(val albumId: String) : AppRoute
@Serializable data class GalleryMediaNavRoute(
    val albumId: String,
    val initialIndex: Int
) : AppRoute
@Serializable object MealPlannerNavRoute : AppRoute
@Serializable data class MealPlanDetailsNavRoute(
    val mealPlanId: String? = null,
    val defaultDate: String? = null,
    val preselectedRecipeId: String? = null
) : AppRoute
@Serializable object ListsNavRoute : AppRoute
@Serializable data class ListDetailNavRoute(val listId: String) : AppRoute
@Serializable data class ListEntryDetailsNavRoute(val listId: String, val entryId: String? = null) : AppRoute
@Serializable object TipTrackerNavRoute : AppRoute
@Serializable object TipStatisticsNavRoute : AppRoute

// Stack marker for shared TipTrackerViewModel scope between TipTrackerRoute and TipStatisticsRoute.
// Pushed before TipTrackerNavRoute; auto-popped when all tip tracker routes leave the stack.
@Serializable object TipTrackerGraph : AppRoute
