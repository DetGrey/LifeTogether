package com.example.lifetogether.domain.model.guides

import kotlinx.serialization.Serializable

@Serializable
data class GuideStep(
    val id: String = "",
    val name: String = "",
    val type: GuideStepType = GuideStepType.NUMBERED,
    val title: String = "",
    val content: String = "",
    val completed: Boolean = false,
    val subSteps: List<GuideStep> = emptyList(),
)
