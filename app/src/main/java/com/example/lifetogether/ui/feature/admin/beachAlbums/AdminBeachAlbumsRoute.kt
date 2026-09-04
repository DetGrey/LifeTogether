package com.example.lifetogether.ui.feature.admin.beachAlbums

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.navigation.AdminBeachAlbumDetailsNavRoute
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun AdminBeachAlbumsRoute(
    appNavigator: AppNavigator,
    viewModel: AdminBeachAlbumsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CollectUiCommands(viewModel.uiCommands)

    AdminBeachAlbumsScreen(
        uiState = uiState,
        onUiEvent = viewModel::onEvent,
        onNavigationEvent = { navEvent ->
            when (navEvent) {
                is AdminBeachAlbumsNavigationEvent.NavigateBack -> appNavigator.navigateBack()
                is AdminBeachAlbumsNavigationEvent.NavigateToDetails -> {
                    appNavigator.navigate(AdminBeachAlbumDetailsNavRoute(navEvent.albumId))
                }
            }
        },
    )
}
