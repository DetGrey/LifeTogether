package com.example.lifetogether.ui.feature.admin.beachAlbums.details

import android.net.Uri
import com.example.lifetogether.domain.model.beach.BeachAlbum
import com.example.lifetogether.domain.model.beach.BeachMedia

sealed interface AdminBeachAlbumDetailsUiState {
    data object Loading : AdminBeachAlbumDetailsUiState

    data class Content(
        val album: BeachAlbum,
        val mediaList: List<BeachMedia> = emptyList(),
        val selectedMediaIds: Set<String> = emptySet(),
        val isSelectionMode: Boolean = false,
        val showMoreActions: Boolean = false,
        val isRefreshing: Boolean = false,
        val isSyncing: Boolean = false,
        val isPartialLoad: Boolean = false,
        val retryingMediaIds: Set<String> = emptySet(),
        val dialogState: BeachAlbumDetailsDialogState? = null,
        val uploadProgress: UploadProgress? = null,
    ) : AdminBeachAlbumDetailsUiState
}

data class UploadProgress(
    val current: Int,
    val total: Int,
)

sealed interface BeachAlbumDetailsDialogState {
    data object QrCode : BeachAlbumDetailsDialogState
    data class RenameAlbum(val nameDraft: String) : BeachAlbumDetailsDialogState
    data object DeleteAlbum : BeachAlbumDetailsDialogState
    data object DeleteSelectedMedia : BeachAlbumDetailsDialogState
}

sealed interface AdminBeachAlbumDetailsUiEvent {
    data object RetryFetchBeachMedia : AdminBeachAlbumDetailsUiEvent
    data class RetryMediaDownload(val mediaId: String) : AdminBeachAlbumDetailsUiEvent
    data object ToggleSelectionMode : AdminBeachAlbumDetailsUiEvent
    data object ToggleMoreActions : AdminBeachAlbumDetailsUiEvent
    data object OpenQrDialog : AdminBeachAlbumDetailsUiEvent
    data object OpenRenameAlbumDialog : AdminBeachAlbumDetailsUiEvent
    data class RenameDraftChanged(val value: String) : AdminBeachAlbumDetailsUiEvent
    data object ConfirmRenameAlbum : AdminBeachAlbumDetailsUiEvent
    data object OpenDeleteAlbumDialog : AdminBeachAlbumDetailsUiEvent
    data object ConfirmDeleteAlbum : AdminBeachAlbumDetailsUiEvent
    data class MediaClicked(val mediaId: String) : AdminBeachAlbumDetailsUiEvent
    data class MediaLongClicked(val mediaId: String) : AdminBeachAlbumDetailsUiEvent
    data object ToggleSelectAll : AdminBeachAlbumDetailsUiEvent
    data object ClearSelection : AdminBeachAlbumDetailsUiEvent
    data object OpenDeleteMediaDialog : AdminBeachAlbumDetailsUiEvent
    data object ConfirmDeleteSelectedMedia : AdminBeachAlbumDetailsUiEvent
    data object DismissDialog : AdminBeachAlbumDetailsUiEvent
    data class UploadPhotos(val uris: List<Uri>) : AdminBeachAlbumDetailsUiEvent
}

sealed interface AdminBeachAlbumDetailsNavigationEvent {
    data object NavigateBack : AdminBeachAlbumDetailsNavigationEvent
}

sealed interface AdminBeachAlbumDetailsCommand {
    data object NavigateBack : AdminBeachAlbumDetailsCommand
}
