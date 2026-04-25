package com.example.lifetogether.ui.feature.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.di.rememberSessionRepository
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.AlbumMediaNavRoute
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.GalleryMediaNavRoute
import com.example.lifetogether.ui.navigation.GalleryGraph

@Composable
fun GalleryGraphObserverRoute(
    navController: NavHostController,
) {
    val sessionRepository = rememberSessionRepository()
    val sessionState by sessionRepository.sessionState.collectAsStateWithLifecycle()
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
    val viewModel: GalleryViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CollectUiCommands(viewModel.uiCommands)

    GalleryScreen(
        uiState = uiState,
        onUiEvent = viewModel::onEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                GalleryNavigationEvent.NavigateBack -> appNavigator.navigateBack()
                is GalleryNavigationEvent.NavigateToAlbumMedia -> {
                    appNavigator.navigate(AlbumMediaNavRoute(navigationEvent.albumId))
                }
            }
        },
    )
}

@Composable
fun AlbumDetailsRoute(
    appNavigator: AppNavigator,
) {
    val viewModel: AlbumDetailsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CollectUiCommands(viewModel.uiCommands)

    LaunchedEffect(viewModel.commands) {
        viewModel.commands.collect { command ->
            when (command) {
                AlbumDetailsCommand.NavigateBack -> appNavigator.navigateBack()
            }
        }
    }

    AlbumDetailsScreen(
        uiState = uiState,
        onImageUpload = viewModel::uploadGalleryMediaItems,
        onUiEvent = viewModel::onUiEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                AlbumDetailsNavigationEvent.NavigateBack -> appNavigator.navigateBack()
                is AlbumDetailsNavigationEvent.NavigateToMediaDetails -> {
                    val albumId = uiState.album?.id ?: return@AlbumDetailsScreen
                    appNavigator.navigate(GalleryMediaNavRoute(albumId, navigationEvent.initialIndex))
                }
            }
        },
    )
}

@Composable
fun MediaDetailsRoute(
    appNavigator: AppNavigator,
) {
    val viewModel: MediaDetailsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CollectUiCommands(viewModel.uiCommands)

    MediaDetailsScreen(
        uiState = uiState,
        onUiEvent = viewModel::onEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                MediaDetailsNavigationEvent.NavigateBack -> appNavigator.navigateBack()
            }
        },
    )
}
