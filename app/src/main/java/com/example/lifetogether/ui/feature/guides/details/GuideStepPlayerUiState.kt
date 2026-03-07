package com.example.lifetogether.ui.feature.guides.details

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
    val sectionAmountProgressText: String = "",
    val sectionProgressPercent: Int = 0,
    val sectionProgressText: String = "0 / 0",
    val canGoPrevious: Boolean = false,
    val canGoNext: Boolean = false,
    val showAlertDialog: Boolean = false,
    val error: String = "",
)