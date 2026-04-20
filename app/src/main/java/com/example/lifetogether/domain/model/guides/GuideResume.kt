package com.example.lifetogether.domain.model.guides

import kotlinx.serialization.Serializable

@Serializable
data class GuideResume(
    val sectionIndex: Int = 0,
    val sectionAmountIndex: Int = 0,
    val stepIndex: Int = 0,
    val subStepIndex: Int? = null,
)
