package com.example.lifetogether.ui.feature.guides

import com.example.lifetogether.domain.model.guides.Guide

data class GuidesUiState(
    val guides: List<Guide> = emptyList(),
    val showAddOptionsDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val isImporting: Boolean = false,
    val importSummary: String = "",
) {
    val isEmpty: Boolean get() = guides.isEmpty()
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
