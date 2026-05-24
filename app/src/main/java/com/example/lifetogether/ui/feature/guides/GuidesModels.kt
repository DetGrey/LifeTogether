package com.example.lifetogether.ui.feature.guides

import com.example.lifetogether.domain.model.guides.Guide

sealed interface GuidesUiState {
    data object Loading : GuidesUiState

    data class Content(
        val guides: List<Guide>,
        val dialog: GuidesDialogState? = null,
    ) : GuidesUiState {
        val isEmpty: Boolean get() = guides.isEmpty()
    }
}

sealed interface GuidesDialogState {
    data class ImportGuide(
        val isImporting: Boolean = false,
        val importSummary: String = "",
    ) : GuidesDialogState
}

sealed interface GuidesUiEvent {
    data object OpenImportDialog : GuidesUiEvent
    data object DismissDialog : GuidesUiEvent
    data class ImportGuidesFromJson(val json: String) : GuidesUiEvent
}

sealed interface GuidesNavigationEvent {
    data object NavigateBack : GuidesNavigationEvent
    data object NavigateToGuideEdit : GuidesNavigationEvent
    data class NavigateToGuideDetails(val guideId: String) : GuidesNavigationEvent
}
