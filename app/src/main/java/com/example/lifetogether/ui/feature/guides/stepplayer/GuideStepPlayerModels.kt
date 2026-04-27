package com.example.lifetogether.ui.feature.guides.stepplayer

import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideStep

data class GuideStepPlayerUiState(
    val guide: Guide? = null,
    val currentStep: GuideStep? = null,
    val nextStep: GuideStep? = null,
    val currentStepCompleted: Boolean = false,
    val canToggleCurrentStep: Boolean = false,
    val currentRoundGroupLabel: String = "",
    val currentRoundGroupMeta: String = "",
    val currentStepNumber: Int = 0,
    val totalSteps: Int = 0,
    val sectionTitle: String = "",
    val sectionSubtitle: String = "",
    val currentPartLabel: String = "",
    val currentPartProgressPercent: Int = 0,
    val currentPartProgressText: String = "0 / 0",
    val canGoPrevious: Boolean = false,
    val canGoNext: Boolean = false,
)

sealed interface GuideStepPlayerUiEvent {
    data object PreviousClicked : GuideStepPlayerUiEvent
    data object CompleteCurrentAndGoNextClicked : GuideStepPlayerUiEvent
    data object ToggleCurrentStepCompletionClicked : GuideStepPlayerUiEvent
}

sealed interface GuideStepPlayerNavigationEvent {
    data object NavigateBack : GuideStepPlayerNavigationEvent
}
