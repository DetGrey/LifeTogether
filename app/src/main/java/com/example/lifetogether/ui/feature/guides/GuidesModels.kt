package com.example.lifetogether.ui.feature.guides

import com.example.lifetogether.domain.model.guides.Guide

sealed interface GuidesUiState {
    data object Loading : GuidesUiState

    data class Content(
        val guides: List<Guide> = emptyList(),
        val showAddOptionsDialog: Boolean = false,
        val showImportDialog: Boolean = false,
        val isImporting: Boolean = false,
        val importSummary: String = "",
    ) : GuidesUiState {
        val isEmpty: Boolean get() = guides.isEmpty()
    }
}

sealed interface GuidesUiEvent {
    data object OpenAddOptionsDialog : GuidesUiEvent
    data object CloseAddOptionsDialog : GuidesUiEvent
    data object OpenImportDialog : GuidesUiEvent
    data object CloseImportDialog : GuidesUiEvent
    data class ImportGuidesFromJson(val json: String) : GuidesUiEvent
}

sealed interface GuidesNavigationEvent {
    data object NavigateBack : GuidesNavigationEvent
    data object NavigateToGuideCreate : GuidesNavigationEvent
    data class NavigateToGuideDetails(val guideId: String) : GuidesNavigationEvent
}
