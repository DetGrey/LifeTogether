package com.example.lifetogether.ui.feature.traveller

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.navigation.AlbumMediaNavRoute
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun TravellerRoute(
    filterAlbumId: String?,
    appNavigator: AppNavigator,
) {
    val viewModel: TravellerViewModel =
        hiltViewModel<TravellerViewModel, TravellerViewModel.Factory> { it.create(filterAlbumId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CollectUiCommands(viewModel.uiCommands)

    LaunchedEffect(viewModel.commands) {
        viewModel.commands.collect { command ->
            when (command) {
                is TravellerCommand.NavigateToAlbum -> appNavigator.navigate(AlbumMediaNavRoute(command.albumId))
            }
        }
    }

    TravellerScreen(
        uiState = uiState,
        onUiEvent = viewModel::onEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                TravellerNavigationEvent.NavigateBack -> appNavigator.navigateBack()
            }
        },
    )
}
