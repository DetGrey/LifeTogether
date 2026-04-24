package com.example.lifetogether.ui.feature.guides.create

import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.guides.GuideStepType

data class GuideCreateUiState(
    val title: String = "",
    val description: String = "",
    val visibility: Visibility = Visibility.PRIVATE,
    val sections: List<GuideSection> = emptyList(),
    val isSaving: Boolean = false,
)

sealed interface GuideCreateUiEvent {
    data class TitleChanged(val value: String) : GuideCreateUiEvent
    data class DescriptionChanged(val value: String) : GuideCreateUiEvent
    data class VisibilityChanged(val value: Visibility) : GuideCreateUiEvent
    data class AddSectionRequested(val title: String, val amount: Int) : GuideCreateUiEvent
    data class AddStepRequested(
        val sectionId: String,
        val content: String,
        val type: GuideStepType,
    ) : GuideCreateUiEvent
    data object SaveClicked : GuideCreateUiEvent
}

sealed interface GuideCreateNavigationEvent {
    data object NavigateBack : GuideCreateNavigationEvent
}

sealed interface GuideCreateCommand {
    data class NavigateToGuideDetails(val guideId: String) : GuideCreateCommand
}
