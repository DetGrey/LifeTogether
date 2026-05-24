package com.example.lifetogether.ui.feature.gallery

import com.example.lifetogether.ui.model.AlbumUiModel

sealed interface GalleryUiState {
    data object Loading : GalleryUiState

    data class Content(
        val albums: List<AlbumUiModel>,
        val dialog: GalleryDialogState? = null,
    ) : GalleryUiState
}

sealed interface GalleryDialogState {
    data class NewAlbum(val name: String = "") : GalleryDialogState
}

sealed interface GalleryUiEvent {
    data object OpenNewAlbumDialog : GalleryUiEvent
    data object DismissDialog : GalleryUiEvent
    data class NewAlbumNameChanged(val name: String) : GalleryUiEvent
    data object CreateNewAlbum : GalleryUiEvent
}

sealed interface GalleryNavigationEvent {
    data object NavigateBack : GalleryNavigationEvent
    data class NavigateToAlbumMedia(val albumId: String) : GalleryNavigationEvent
}

sealed interface GalleryCommand {
    data class NavigateToAlbumMedia(val albumId: String) : GalleryCommand
}
