package com.example.lifetogether.ui.feature.guides.edit

import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.guides.GuideStepType

sealed interface GuideEditUiState {
    data object Loading : GuideEditUiState

    data class Content(
        val title: String = "",
        val description: String = "",
        val visibility: Visibility = Visibility.PRIVATE,
        val sections: List<GuideSection> = emptyList(),
        val stepDrafts: Map<String, String> = emptyMap(),
        val stepTypeDrafts: Map<String, GuideStepType> = emptyMap(),
        val isSaving: Boolean = false,
        val isEditMode: Boolean = false,
        val showDiscardDialog: Boolean = false,
    ) : GuideEditUiState
}

sealed interface GuideEditUiEvent {
    data class TitleChanged(val value: String) : GuideEditUiEvent
    data class DescriptionChanged(val value: String) : GuideEditUiEvent
    data class VisibilityChanged(val value: Visibility) : GuideEditUiEvent
    data class AddSectionRequested(val title: String, val amount: Int) : GuideEditUiEvent
    data class DeleteSectionRequested(val sectionId: String) : GuideEditUiEvent
    data class SectionMoved(val fromIndex: Int, val toIndex: Int) : GuideEditUiEvent
    data class AddStepRequested(
        val sectionId: String,
        val content: String,
        val type: GuideStepType,
    ) : GuideEditUiEvent
    data class DeleteStepRequested(val sectionId: String, val stepId: String) : GuideEditUiEvent
    data class StepMoved(val sectionId: String, val fromIndex: Int, val toIndex: Int) : GuideEditUiEvent
    data class StepDraftChanged(val sectionId: String, val value: String) : GuideEditUiEvent
    data class StepTypeDraftChanged(val sectionId: String, val type: GuideStepType) : GuideEditUiEvent
    data object SaveClicked : GuideEditUiEvent
    data object DiscardClicked : GuideEditUiEvent
    data object DismissDiscardDialog : GuideEditUiEvent
    data object ConfirmDiscard : GuideEditUiEvent
}

sealed interface GuideEditCommand {
    data class NavigateToGuideDetails(val guideId: String) : GuideEditCommand
    data object NavigateBack : GuideEditCommand
}
