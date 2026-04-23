package com.example.lifetogether.ui.feature.gallery

import com.example.lifetogether.ui.model.AlbumUiModel

data class GalleryUiState(
    val albums: List<AlbumUiModel> = emptyList(),
    val showNewAlbumDialog: Boolean = false,
    val newAlbumName: String = "",
)

sealed interface GalleryUiEvent {
    data object OpenNewAlbumDialog : GalleryUiEvent
    data object DismissNewAlbumDialog : GalleryUiEvent
    data class NewAlbumNameChanged(val name: String) : GalleryUiEvent
    data object CreateNewAlbum : GalleryUiEvent
}

sealed interface GalleryNavigationEvent {
    data object NavigateBack : GalleryNavigationEvent
    data class NavigateToAlbumMedia(val albumId: String) : GalleryNavigationEvent
}
