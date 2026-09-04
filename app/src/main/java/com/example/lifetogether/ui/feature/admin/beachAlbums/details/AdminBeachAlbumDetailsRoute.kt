package com.example.lifetogether.ui.feature.admin.beachAlbums.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun AdminBeachAlbumDetailsRoute(
    appNavigator: AppNavigator,
    albumId: String,
    viewModel: AdminBeachAlbumDetailsViewModel = hiltViewModel<AdminBeachAlbumDetailsViewModel, AdminBeachAlbumDetailsViewModel.Factory> {
        it.create(albumId)
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CollectUiCommands(viewModel.uiCommands)

    LaunchedEffect(Unit) {
        viewModel.featureCommands.collect { command ->
            when (command) {
                is AdminBeachAlbumDetailsCommand.NavigateBack -> appNavigator.navigateBack()
            }
        }
    }

    AdminBeachAlbumDetailsScreen(
        uiState = uiState,
        onUiEvent = viewModel::onEvent,
        onNavigationEvent = { navEvent ->
            when (navEvent) {
                is AdminBeachAlbumDetailsNavigationEvent.NavigateBack -> appNavigator.navigateBack()
            }
        },
    )
}
