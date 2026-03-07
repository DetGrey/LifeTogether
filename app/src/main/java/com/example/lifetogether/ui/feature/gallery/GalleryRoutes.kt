package com.example.lifetogether.ui.feature.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.lifetogether.domain.observer.ObserverKey
import com.example.lifetogether.ui.common.observer.FeatureObserverLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.AppRoutes
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel

@Composable
fun GalleryGraphObserverRoute(
    navController: NavHostController,
    appSessionViewModel: AppSessionViewModel,
) {
    val userInformation by appSessionViewModel.userInformation.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val isInGalleryGraph = currentBackStackEntry
        ?.destination
        ?.hierarchy
        ?.any { destination -> destination.route == AppRoutes.GALLERY_GRAPH } == true

    if (isInGalleryGraph && !userInformation?.familyId.isNullOrBlank()) {
        FeatureObserverLifecycleBinding(
            appSessionViewModel = appSessionViewModel,
            keys = setOf(ObserverKey.GALLERY_ALBUMS, ObserverKey.GALLERY_MEDIA),
            uid = userInformation?.uid,
            familyId = userInformation?.familyId,
        )
    }
}

@Composable
fun GalleryScreenRoute(
    appNavigator: AppNavigator,
    appSessionViewModel: AppSessionViewModel,
) {
    GalleryScreen(appNavigator, appSessionViewModel)
}

@Composable
fun AlbumDetailsRoute(
    appNavigator: AppNavigator,
    appSessionViewModel: AppSessionViewModel,
    albumId: String,
) {
    AlbumDetailsScreen(appNavigator, appSessionViewModel, albumId)
}

@Composable
fun MediaDetailsRoute(
    appNavigator: AppNavigator,
    appSessionViewModel: AppSessionViewModel,
    albumId: String,
    initialIndex: Int,
) {
    MediaDetailsScreen(appNavigator, appSessionViewModel, albumId, initialIndex)
}
