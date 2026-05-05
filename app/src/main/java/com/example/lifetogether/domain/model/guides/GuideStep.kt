package com.example.lifetogether.domain.model.guides

import kotlinx.serialization.Serializable

@Serializable
data class GuideStep(
    val id: String,
    val name: String,
    val type: GuideStepType,
    val title: String,
    val content: String,
    val subSteps: List<GuideStep>,
    val completed: Boolean = false,
)
