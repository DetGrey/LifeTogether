package com.example.lifetogether.ui.common.sync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.di.rememberSessionRepository
import com.example.lifetogether.ui.navigation.AlbumMediaNavRoute
import com.example.lifetogether.ui.navigation.AppRoute
import com.example.lifetogether.ui.navigation.GalleryMediaNavRoute
import com.example.lifetogether.ui.navigation.GalleryNavRoute
import com.example.lifetogether.ui.navigation.GuideCreateNavRoute
import com.example.lifetogether.ui.navigation.GuideDetailsNavRoute
import com.example.lifetogether.ui.navigation.GuidesNavRoute
import com.example.lifetogether.ui.navigation.GuideStepPlayerNavRoute
import com.example.lifetogether.ui.navigation.ListDetailNavRoute
import com.example.lifetogether.ui.navigation.ListEntryDetailsNavRoute
import com.example.lifetogether.ui.navigation.ListsNavRoute
import com.example.lifetogether.ui.navigation.MealPlanDetailsNavRoute
import com.example.lifetogether.ui.navigation.MealPlannerNavRoute
import com.example.lifetogether.ui.navigation.RecipeDetailsNavRoute
import com.example.lifetogether.ui.navigation.RecipesNavRoute
import com.example.lifetogether.ui.navigation.TipStatisticsNavRoute
import com.example.lifetogether.ui.navigation.TipTrackerGraph
import com.example.lifetogether.ui.navigation.TipTrackerNavRoute

fun AppRoute.activeSyncKeys(): Set<SyncKey> = when (this) {
    is RecipesNavRoute, is RecipeDetailsNavRoute -> setOf(SyncKey.RECIPES)
    is GalleryNavRoute, is AlbumMediaNavRoute, is GalleryMediaNavRoute -> setOf(SyncKey.GALLERY_ALBUMS, SyncKey.GALLERY_MEDIA)
    is MealPlannerNavRoute, is MealPlanDetailsNavRoute -> setOf(SyncKey.MEAL_PLANNER)
    is ListsNavRoute, is ListDetailNavRoute, is ListEntryDetailsNavRoute -> setOf(
        SyncKey.USER_LISTS,
        SyncKey.ROUTINE_LIST_ENTRIES,
        SyncKey.WISH_LIST_ENTRIES,
        SyncKey.NOTE_ENTRIES,
        SyncKey.CHECKLIST_ENTRIES,
    )
    is GuidesNavRoute, is GuideCreateNavRoute, is GuideDetailsNavRoute, is GuideStepPlayerNavRoute -> setOf(SyncKey.GUIDES)
    is TipTrackerNavRoute, is TipStatisticsNavRoute, is TipTrackerGraph -> setOf(SyncKey.TIP_TRACKER)
    else -> emptySet()
}

@Composable
fun RouteSyncBinding(route: AppRoute?) {
    val sessionRepository = rememberSessionRepository()
    val sessionState by sessionRepository.sessionState.collectAsStateWithLifecycle()
    val familyId = (sessionState as? SessionState.Authenticated)?.user?.familyId

    val syncKeys = route?.activeSyncKeys() ?: emptySet()
    if (syncKeys.isNotEmpty() && !familyId.isNullOrBlank()) {
        FeatureSyncLifecycleBinding(keys = syncKeys)
    }
}
