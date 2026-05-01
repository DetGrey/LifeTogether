package com.example.lifetogether.ui.feature.guides.details

import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideSection

sealed interface GuideDetailsUiState {
    data object Loading : GuideDetailsUiState

    data class Content(
        val guide: Guide? = null,
        val sectionExpandedState: Map<String, Boolean> = emptyMap(),
        val selectedSectionAmountState: Map<String, Int> = emptyMap(),
        val canToggleAmountState: Map<String, Set<Int>> = emptyMap(),
        val isUpdatingVisibility: Boolean = false,
        val isStartingGuide: Boolean = false,
        val isDeletingGuide: Boolean = false,
    ) : GuideDetailsUiState
}

sealed interface GuideDetailsUiEvent {
    data object StartOrContinueClicked : GuideDetailsUiEvent
    data object ResetAllProgressClicked : GuideDetailsUiEvent
    data object ToggleVisibilityClicked : GuideDetailsUiEvent
    data object DeleteGuideClicked : GuideDetailsUiEvent
    data class ToggleSectionExpanded(val sectionKey: String) : GuideDetailsUiEvent
    data class SelectSectionAmount(val sectionKey: String, val amountIndex: Int) : GuideDetailsUiEvent
    data class ToggleStepCompletion(val stepId: String, val amountIndex: Int) : GuideDetailsUiEvent
}

sealed interface GuideDetailsNavigationEvent {
    data object NavigateBack : GuideDetailsNavigationEvent
}

sealed interface GuideDetailsCommand {
    data object NavigateToGuideStepPlayer : GuideDetailsCommand
    data object NavigateBack : GuideDetailsCommand
}

internal fun guideSectionKey(
    section: GuideSection,
    sectionIndex: Int,
): String = section.id.ifBlank { "section-$sectionIndex" }

internal fun reconcileSectionExpandedState(
    sections: List<GuideSection>,
    existingState: Map<String, Boolean>,
): Map<String, Boolean> {
    return buildMap {
        sections.forEachIndexed { sectionIndex, section ->
            val sectionKey = guideSectionKey(section, sectionIndex)
            put(sectionKey, existingState[sectionKey] ?: !section.completed)
        }
    }
}

internal fun defaultSectionAmountIndex(section: GuideSection): Int {
    val amount = section.amount.coerceAtLeast(1)
    val completedAmount = section.completedAmount.coerceIn(0, amount)
    return if (completedAmount >= amount) amount - 1 else completedAmount
}

internal fun reconcileSelectedSectionAmountState(
    sections: List<GuideSection>,
    existingState: Map<String, Int>,
): Map<String, Int> {
    return buildMap {
        sections.forEachIndexed { sectionIndex, section ->
            val sectionKey = guideSectionKey(section, sectionIndex)
            val amount = section.amount.coerceAtLeast(1)
            val fallbackIndex = defaultSectionAmountIndex(section)
            val selectedIndex = existingState[sectionKey]
                ?.coerceIn(0, amount - 1)
                ?: fallbackIndex
            put(sectionKey, selectedIndex)
        }
    }
}
