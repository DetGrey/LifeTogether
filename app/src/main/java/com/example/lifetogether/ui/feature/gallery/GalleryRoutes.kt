package com.example.lifetogether.ui.feature.gallery

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
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
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.GalleryGraph
import com.example.lifetogether.ui.viewmodel.RootCoordinatorViewModel
import com.example.lifetogether.ui.viewmodel.ImageViewModel

@Composable
fun GalleryGraphObserverRoute(
    navController: NavHostController,
) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    val rootCoordinator: RootCoordinatorViewModel = hiltViewModel(activity)
    val sessionState by rootCoordinator.sessionState.collectAsStateWithLifecycle()
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
                    appNavigator.navigateToAlbumMedia(navigationEvent.albumId)
                }
            }
        },
    )
}

@Composable
fun AlbumDetailsRoute(
    appNavigator: AppNavigator,
    albumId: String,
) {
    val viewModel: AlbumDetailsViewModel = hiltViewModel()
    val imageViewModel: ImageViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CollectUiCommands(viewModel.uiCommands)

    LaunchedEffect(albumId) {
        viewModel.setUp(albumId)
    }

    LaunchedEffect(viewModel.commands) {
        viewModel.commands.collect { command ->
            when (command) {
                AlbumDetailsCommand.NavigateBack -> appNavigator.navigateBack()
            }
        }
    }

    AlbumDetailsScreen(
        uiState = uiState,
        showImageUploadDialog = imageViewModel.showImageUploadDialog,
        onUiEvent = { event ->
            when (event) {
                AlbumDetailsUiEvent.RequestImageUpload -> imageViewModel.showImageUploadDialog = true
                AlbumDetailsUiEvent.DismissImageUploadDialog,
                AlbumDetailsUiEvent.ConfirmImageUploadDialog -> imageViewModel.showImageUploadDialog = false
                else -> viewModel.onEvent(event)
            }
        },
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                AlbumDetailsNavigationEvent.NavigateBack -> appNavigator.navigateBack()
                is AlbumDetailsNavigationEvent.NavigateToMediaDetails -> {
                    appNavigator.navigateToGalleryMedia(albumId, navigationEvent.initialIndex)
                }
            }
        },
    )
}

@Composable
fun MediaDetailsRoute(
    appNavigator: AppNavigator,
    albumId: String,
    initialIndex: Int,
) {
    val viewModel: MediaDetailsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CollectUiCommands(viewModel.uiCommands)

    LaunchedEffect(albumId, initialIndex) {
        viewModel.setUp(albumId, initialIndex)
    }

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
