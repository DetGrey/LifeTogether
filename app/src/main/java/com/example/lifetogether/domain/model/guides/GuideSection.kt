package com.example.lifetogether.domain.model.guides

import kotlinx.serialization.Serializable

@Serializable
data class GuideSection(
    val id: String = "",
    val orderNumber: Int = 0,
    val title: String = "",
    val subtitle: String? = null,
    val amount: Int = 1,
    val completedAmount: Int = 0,
    val completed: Boolean = false,
    val comment: String? = null,
    val steps: List<GuideStep> = emptyList(),
    val stepsProgressByAmount: List<List<GuideStep>> = emptyList(),
)
