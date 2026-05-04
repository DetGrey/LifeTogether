package com.example.lifetogether.ui.feature.gallery

import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.ui.model.AlbumUiModel
import com.example.lifetogether.ui.model.MenuAction

sealed interface AlbumDetailsUiState {
    data object Loading : AlbumDetailsUiState

    data class Content(
        val album: Album?,
        val media: List<GalleryMedia>,
        val groupedMedia: List<Pair<String, List<GalleryMedia>>>,
        val thumbnails: Map<String, ByteArray>,
        val showOverflowMenu: Boolean = false,
        val showOverflowMenuActionDialog: Boolean = false,
        val showImageUploadDialog: Boolean = false,
        val overflowMenuAction: MenuAction?,
        val actionDialogText: String,
        val isPartialLoad: Boolean = false,
        val isRefreshing: Boolean = false,
        val isSelectionModeActive: Boolean = false,
        val selectedMedia: Set<String>,
        val isAllMediaSelected: Boolean = false,
        val albums: List<AlbumUiModel>,
        val familyId: String?,
        val isSyncing: Boolean = false,
    ) : AlbumDetailsUiState
}

sealed interface AlbumDetailsUiEvent {
    data object RetryFetchAlbumMedia : AlbumDetailsUiEvent
    data object ToggleOverflowMenu : AlbumDetailsUiEvent
    data object ToggleSelectionMode : AlbumDetailsUiEvent
    data object ToggleAllMediaSelection : AlbumDetailsUiEvent
    data class ToggleMediaSelection(val mediaId: String?) : AlbumDetailsUiEvent
    data class EnterSelectionMode(val mediaId: String?) : AlbumDetailsUiEvent
    data object RequestImageUpload : AlbumDetailsUiEvent
    data object DismissImageUploadDialog : AlbumDetailsUiEvent
    data object ConfirmImageUploadDialog : AlbumDetailsUiEvent
    data class StartOverflowAction(val action: MenuAction) : AlbumDetailsUiEvent
    data object DismissOverflowMenuActionDialog : AlbumDetailsUiEvent
    data class SetActionDialogText(val text: String) : AlbumDetailsUiEvent
    data object ConfirmRenameAlbum : AlbumDetailsUiEvent
    data object ConfirmDeleteAlbum : AlbumDetailsUiEvent
    data object DownloadSelectedMedia : AlbumDetailsUiEvent
    data object ConfirmDeleteSelectedMedia : AlbumDetailsUiEvent
    data class MoveSelectedMediaToAlbum(val albumId: String) : AlbumDetailsUiEvent
    data object ConfirmMoveSelectedMedia : AlbumDetailsUiEvent
}

sealed interface AlbumDetailsNavigationEvent {
    data object NavigateBack : AlbumDetailsNavigationEvent
    data class NavigateToMediaDetails(val initialIndex: Int) : AlbumDetailsNavigationEvent
}

sealed interface AlbumDetailsCommand {
    data object NavigateBack : AlbumDetailsCommand
}
