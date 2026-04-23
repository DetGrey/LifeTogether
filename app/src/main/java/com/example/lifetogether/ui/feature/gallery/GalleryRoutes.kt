package com.example.lifetogether.ui.feature.gallery

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.GalleryGraph
import com.example.lifetogether.ui.viewmodel.RootCoordinatorViewModel

@Composable
fun GalleryGraphObserverRoute(
    navController: NavHostController,
) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    val rootCoordinator: RootCoordinatorViewModel = hiltViewModel(activity)
    val sessionState by rootCoordinator.sessionState.collectAsState()
    val familyId = (sessionState as? SessionState.Authenticated)?.user?.familyId

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val isInGalleryGraph = currentBackStackEntry
        ?.destination
        ?.hierarchy
        ?.any { destination -> destination.hasRoute(GalleryGraph::class) } == true

    if (isInGalleryGraph && !familyId.isNullOrBlank()) {
        FeatureSyncLifecycleBinding(
            keys = setOf(SyncKey.GALLERY_ALBUMS, SyncKey.GALLERY_MEDIA),
        )
    }
}

@Composable
fun GalleryScreenRoute(
    appNavigator: AppNavigator,
) {
    GalleryScreen(appNavigator)
}

@Composable
fun AlbumDetailsRoute(
    appNavigator: AppNavigator,
    albumId: String,
) {
    AlbumDetailsScreen(appNavigator, albumId)
}

@Composable
fun MediaDetailsRoute(
    appNavigator: AppNavigator,
    albumId: String,
    initialIndex: Int,
) {
    MediaDetailsScreen(appNavigator, albumId, initialIndex)
}
