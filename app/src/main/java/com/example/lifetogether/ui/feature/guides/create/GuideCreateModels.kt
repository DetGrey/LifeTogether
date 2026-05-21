package com.example.lifetogether.ui.feature.guides.create

import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.guides.GuideStepType

data class GuideCreateUiState(
    val title: String = "",
    val description: String = "",
    val visibility: Visibility = Visibility.PRIVATE,
    val sections: List<GuideSection> = emptyList(),
    val stepDrafts: Map<String, String> = emptyMap(),
    val stepTypeDrafts: Map<String, GuideStepType> = emptyMap(),
    val isSaving: Boolean = false,
)

sealed interface GuideCreateUiEvent {
    data class TitleChanged(val value: String) : GuideCreateUiEvent
    data class DescriptionChanged(val value: String) : GuideCreateUiEvent
    data class VisibilityChanged(val value: Visibility) : GuideCreateUiEvent
    data class AddSectionRequested(val title: String, val amount: Int) : GuideCreateUiEvent
    data class DeleteSectionRequested(val sectionId: String) : GuideCreateUiEvent
    data class SectionMoved(val fromIndex: Int, val toIndex: Int) : GuideCreateUiEvent
    data class AddStepRequested(
        val sectionId: String,
        val content: String,
        val type: GuideStepType,
    ) : GuideCreateUiEvent
    data class DeleteStepRequested(val sectionId: String, val stepId: String) : GuideCreateUiEvent
    data class StepMoved(val sectionId: String, val fromIndex: Int, val toIndex: Int) : GuideCreateUiEvent
    data class StepDraftChanged(val sectionId: String, val value: String) : GuideCreateUiEvent
    data class StepTypeDraftChanged(val sectionId: String, val type: GuideStepType) : GuideCreateUiEvent
    data object SaveClicked : GuideCreateUiEvent
}

sealed interface GuideCreateNavigationEvent {
    data object NavigateBack : GuideCreateNavigationEvent
}

sealed interface GuideCreateCommand {
    data class NavigateToGuideDetails(val guideId: String) : GuideCreateCommand
}
