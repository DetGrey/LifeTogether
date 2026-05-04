package com.example.lifetogether.ui.feature.gallery

import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.ui.model.MenuAction

sealed interface MediaDetailsUiState {
    data object Loading : MediaDetailsUiState

    data class Content(
        val mediaList: List<GalleryMedia>,
        val currentIndex: Int,
        val showOverflowMenu: Boolean = false,
        val showOverflowMenuActionDialog: Boolean = false,
        val overflowMenuAction: MenuAction.MediaDetailsActions? = null,
        val actionDialogText: String = "",
        val offsetY: Float = 0f,
    ) : MediaDetailsUiState
}

sealed interface MediaDetailsUiEvent {
    data class VerticalDrag(val dragAmount: Float, val totalHeight: Int) : MediaDetailsUiEvent
    data class DragEnd(val totalHeight: Int) : MediaDetailsUiEvent
    data object ToggleOverflowMenu : MediaDetailsUiEvent
    data class StartOverflowAction(val action: MenuAction.MediaDetailsActions) : MediaDetailsUiEvent
    data object DismissOverflowMenuActionDialog : MediaDetailsUiEvent
    data class DownloadMedia(val index: Int? = null) : MediaDetailsUiEvent
    data class DeleteMedia(val index: Int? = null) : MediaDetailsUiEvent
}

sealed interface MediaDetailsNavigationEvent {
    data object NavigateBack : MediaDetailsNavigationEvent
}
