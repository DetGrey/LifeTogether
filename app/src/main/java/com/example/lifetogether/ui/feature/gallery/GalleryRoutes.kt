package com.example.lifetogether.ui.feature.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.navigation.AlbumMediaNavRoute
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.GalleryMediaNavRoute

@Composable
fun GalleryScreenRoute(
    appNavigator: AppNavigator,
) {
    val viewModel: GalleryViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CollectUiCommands(viewModel.uiCommands)

    LaunchedEffect(viewModel) {
        viewModel.commands.collect { command ->
            when (command) {
                is GalleryCommand.NavigateToAlbumMedia -> {
                    appNavigator.navigate(AlbumMediaNavRoute(command.albumId))
                }
            }
        }
    }

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
    albumId: String,
) {
    val viewModel: AlbumDetailsViewModel =
        hiltViewModel<AlbumDetailsViewModel, AlbumDetailsViewModel.Factory> { it.create(albumId) }
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
                    val albumId = (uiState as? AlbumDetailsUiState.Content)?.album?.id ?: return@AlbumDetailsScreen
                    appNavigator.navigate(GalleryMediaNavRoute(albumId, navigationEvent.initialIndex))
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
    val viewModel: MediaDetailsViewModel =
        hiltViewModel<MediaDetailsViewModel, MediaDetailsViewModel.Factory> {
            it.create(albumId, initialIndex)
        }
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
