package com.example.lifetogether.ui.feature.admin.beachAlbums

import com.example.lifetogether.domain.model.beach.BeachAlbum

sealed interface AdminBeachAlbumsUiState {
    data object Loading : AdminBeachAlbumsUiState

    data class Content(
        val albums: List<BeachAlbum> = emptyList(),
        val thumbnails: Map<String, ByteArray> = emptyMap(),
        val dialogState: BeachAlbumsDialogState? = null,
        val isRefreshing: Boolean = false,
        val isSyncing: Boolean = false,
    ) : AdminBeachAlbumsUiState
}

sealed interface BeachAlbumsDialogState {
    data class CreateAlbum(val nameDraft: String = "") : BeachAlbumsDialogState
}

sealed interface AdminBeachAlbumsUiEvent {
    data object OpenCreateDialog : AdminBeachAlbumsUiEvent
    data class NameDraftChanged(val value: String) : AdminBeachAlbumsUiEvent
    data object ConfirmCreateAlbum : AdminBeachAlbumsUiEvent
    data object DismissDialog : AdminBeachAlbumsUiEvent
    data object RefreshAlbums : AdminBeachAlbumsUiEvent
}

sealed interface AdminBeachAlbumsNavigationEvent {
    data object NavigateBack : AdminBeachAlbumsNavigationEvent
    data class NavigateToDetails(val albumId: String) : AdminBeachAlbumsNavigationEvent
}

sealed interface AdminBeachAlbumsCommand {
    data class ShowSnackbar(val message: String) : AdminBeachAlbumsCommand
}
