package com.example.lifetogether.ui.feature.gallery

import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.ui.model.AlbumUiModel

sealed interface AlbumDetailsUiState {
    data object Loading : AlbumDetailsUiState

    data class Content(
        val album: Album?,
        val media: List<GalleryMedia>,
        val groupedMedia: List<Pair<String, List<GalleryMedia>>>,
        val thumbnails: Map<String, ByteArray>,
        val showOverflowMenu: Boolean = false,
        val showImageUploadDialog: Boolean = false,
        val dialog: AlbumDetailsDialogState? = null,
        val isPartialLoad: Boolean = false,
        val isRefreshing: Boolean = false,
        val isSelectionModeActive: Boolean = false,
        val selectedMedia: Set<String>,
        val isAllMediaSelected: Boolean = false,
        val albums: List<AlbumUiModel>,
        val familyId: String?,
        val isSyncing: Boolean = false,
        val retryingMediaIds: Set<String> = emptySet(),
    ) : AlbumDetailsUiState
}

sealed interface AlbumDetailsDialogState {
    data class RenameAlbum(val name: String = "") : AlbumDetailsDialogState
    data class MoveSelectedMedia(val targetAlbumId: String = "") : AlbumDetailsDialogState
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
    data object RequestRenameAlbum : AlbumDetailsUiEvent
    data object RequestMoveSelectedMedia : AlbumDetailsUiEvent
    data object DismissDialog : AlbumDetailsUiEvent
    data class RenameAlbumNameChanged(val text: String) : AlbumDetailsUiEvent
    data object ConfirmRenameAlbum : AlbumDetailsUiEvent
    data object ConfirmDeleteAlbum : AlbumDetailsUiEvent
    data object DownloadSelectedMedia : AlbumDetailsUiEvent
    data object ConfirmDeleteSelectedMedia : AlbumDetailsUiEvent
    data class MoveSelectedMediaToAlbum(val albumId: String) : AlbumDetailsUiEvent
    data object ConfirmMoveSelectedMedia : AlbumDetailsUiEvent
    data class RetryMediaDownload(val mediaId: String) : AlbumDetailsUiEvent
}

sealed interface AlbumDetailsNavigationEvent {
    data object NavigateBack : AlbumDetailsNavigationEvent
    data class NavigateToMediaDetails(val initialIndex: Int) : AlbumDetailsNavigationEvent
}

sealed interface AlbumDetailsCommand {
    data object NavigateBack : AlbumDetailsCommand
}
