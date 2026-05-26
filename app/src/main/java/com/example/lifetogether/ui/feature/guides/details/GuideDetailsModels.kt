package com.example.lifetogether.ui.feature.guides.details

import com.example.lifetogether.domain.logic.GuideLeafPointer
import com.example.lifetogether.domain.logic.GuideProgress
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.guides.GuideStep

sealed interface GuideDetailsUiState {
    data object Loading : GuideDetailsUiState

    data class Content(
        val guide: Guide?,
        val sectionExpandedState: Map<String, Boolean>,
        val selectedSectionPieceState: Map<String, Int>,
        val canTogglePieceState: Map<String, Set<Int>>,
        val jumpOptions: List<GuideJumpOption> = emptyList(),
        val selectedJumpOptionKey: String? = null,
        val isUpdatingVisibility: Boolean = false,
        val isStartingGuide: Boolean = false,
        val isDeletingGuide: Boolean = false,
        val isOwner: Boolean = false,
    ) : GuideDetailsUiState
}

sealed interface GuideDetailsUiEvent {
    data object StartOrContinueClicked : GuideDetailsUiEvent
    data object ResetAllProgressClicked : GuideDetailsUiEvent
    data object ToggleVisibilityClicked : GuideDetailsUiEvent
    data object DeleteGuideClicked : GuideDetailsUiEvent
    data object CompleteAndGoToSelectedStepClicked : GuideDetailsUiEvent
    data class SelectJumpOption(val optionKey: String) : GuideDetailsUiEvent
    data class ToggleSectionExpanded(val sectionKey: String) : GuideDetailsUiEvent
    data class SelectSectionPiece(val sectionKey: String, val pieceIndex: Int) : GuideDetailsUiEvent
    data class ToggleStepCompletion(val stepId: String, val pieceIndex: Int) : GuideDetailsUiEvent
}

sealed interface GuideDetailsNavigationEvent {
    data object NavigateBack : GuideDetailsNavigationEvent
    data object NavigateToEditGuide : GuideDetailsNavigationEvent
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

internal fun defaultSectionPieceIndex(section: GuideSection): Int {
    val pieces = section.pieces.coerceAtLeast(1)
    val completedPieces = section.completedPieces.coerceIn(0, pieces)
    return if (completedPieces >= pieces) pieces - 1 else completedPieces
}

internal fun reconcileSelectedSectionPieceState(
    sections: List<GuideSection>,
    existingState: Map<String, Int>,
): Map<String, Int> {
    return buildMap {
        sections.forEachIndexed { sectionIndex, section ->
            val sectionKey = guideSectionKey(section, sectionIndex)
            val pieces = section.pieces.coerceAtLeast(1)
            val fallbackIndex = defaultSectionPieceIndex(section)
            val selectedIndex = existingState[sectionKey]
                ?.coerceIn(0, pieces - 1)
                ?: fallbackIndex
            put(sectionKey, selectedIndex)
        }
    }
}

data class GuideJumpOption(
    val key: String,
    val label: String,
    val pointer: GuideLeafPointer,
)

internal fun buildGuideJumpOptions(guide: Guide): List<GuideJumpOption> {
    return GuideProgress.buildLeafPointers(guide.sections).mapNotNull { pointer ->
        val step = GuideProgress.getStepAtPointer(guide.sections, pointer) ?: return@mapNotNull null
        val section = guide.sections.getOrNull(pointer.sectionIndex) ?: return@mapNotNull null
        GuideJumpOption(
            key = guideJumpOptionKey(pointer),
            label = buildGuideJumpOptionLabel(
                section = section,
                sectionIndex = pointer.sectionIndex,
                step = step,
                pointer = pointer,
            ),
            pointer = pointer,
        )
    }
}

internal fun guideJumpOptionKey(pointer: GuideLeafPointer): String {
    val subStepIndex = pointer.subStepIndex ?: -1
    return "${pointer.sectionIndex}:${pointer.sectionPieceIndex}:${pointer.stepIndex}:$subStepIndex"
}

private fun buildGuideJumpOptionLabel(
    section: GuideSection,
    sectionIndex: Int,
    step: GuideStep,
    pointer: GuideLeafPointer,
): String {
    val sectionLabel = section.title.ifBlank { "Section ${sectionIndex + 1}" }
    val pieceLabel = if (section.pieces > 1) " • Piece ${pointer.sectionPieceIndex + 1}" else ""
    val stepLabel = when {
        step.title.isNotBlank() -> step.title
        step.name.isNotBlank() -> step.name
        step.content.isNotBlank() -> step.content.lineSequence().first().take(60)
        pointer.subStepIndex != null -> "Substep ${pointer.subStepIndex + 1}"
        else -> "Step ${pointer.stepIndex + 1}"
    }
    return "$sectionLabel$pieceLabel • $stepLabel"
}
