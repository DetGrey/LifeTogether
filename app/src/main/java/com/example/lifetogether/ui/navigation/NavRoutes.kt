package com.example.lifetogether.ui.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute

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
@Serializable object GuideCreateNavRoute : AppRoute
@Serializable object CreateRecipeNavRoute : AppRoute
@Serializable data class RecipeDetailsNavRoute(val recipeId: String) : AppRoute
@Serializable data class GuideDetailsNavRoute(val guideId: String) : AppRoute
@Serializable data class GuideStepPlayerNavRoute(val guideId: String) : AppRoute
@Serializable object GalleryNavRoute : AppRoute
@Serializable data class AlbumMediaNavRoute(val albumId: String) : AppRoute
@Serializable data class GalleryMediaNavRoute(val albumId: String, val initialIndex: Int) : AppRoute
@Serializable object ListsNavRoute : AppRoute
@Serializable data class ListDetailNavRoute(val listId: String) : AppRoute
@Serializable data class ListEntryDetailsNavRoute(val listId: String, val entryId: String? = null) : AppRoute
@Serializable object TipTrackerNavRoute : AppRoute
@Serializable object TipStatisticsNavRoute : AppRoute

// Not AppRoute — used only for NavHost graph definitions and ViewModel scoping
@Serializable object GuideGraph
@Serializable object GalleryGraph
@Serializable object TipTrackerGraph
