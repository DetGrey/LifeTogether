package com.example.lifetogether.ui.feature.guides.stepplayer

import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideStep

sealed interface GuideStepPlayerUiState {
    data object Loading : GuideStepPlayerUiState

    data class Content(
        val guide: Guide?,
        val currentStep: GuideStep?,
        val nextStep: GuideStep?,
        val currentRoundGroupLabel: String,
        val currentRoundGroupMeta: String,
        val currentStepNumber: Int,
        val totalSteps: Int,
        val sectionTitle: String,
        val sectionSubtitle: String,
        val currentPartLabel: String,
        val currentPartProgressPercent: Int,
        val currentPartProgressText: String,
        val currentStepCompleted: Boolean = false,
        val canToggleCurrentStep: Boolean = false,
        val canGoPrevious: Boolean = false,
        val canGoNext: Boolean = false,
    ) : GuideStepPlayerUiState
}

sealed interface GuideStepPlayerUiEvent {
    data object PreviousClicked : GuideStepPlayerUiEvent
    data object CompleteCurrentAndGoNextClicked : GuideStepPlayerUiEvent
    data object ToggleCurrentStepCompletionClicked : GuideStepPlayerUiEvent
}

sealed interface GuideStepPlayerNavigationEvent {
    data object NavigateBack : GuideStepPlayerNavigationEvent
}
