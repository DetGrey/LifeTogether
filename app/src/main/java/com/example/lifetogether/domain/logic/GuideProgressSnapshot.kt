package com.example.lifetogether.domain.logic

import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.guides.GuideStep

object GuideProgressSnapshot {
    fun pointerKey(pointer: GuideLeafPointer): String {
        val subStepIndex = pointer.subStepIndex ?: -1
        return "${pointer.sectionIndex}:${pointer.sectionPieceIndex}:${pointer.stepIndex}:$subStepIndex"
    }

    fun completedPointerKeysFromSections(sections: List<GuideSection>): Set<String> {
        if (sections.isEmpty()) return emptySet()
        val allLeaves = GuideProgress.buildLeafPointers(sections)
        if (allLeaves.isEmpty()) return emptySet()

        return allLeaves
            .filter { pointer -> GuideProgress.isPointerCompleted(sections, pointer) }
            .mapTo(mutableSetOf(), ::pointerKey)
    }

    fun applyCompletedPointerKeys(
        sections: List<GuideSection>,
        completedPointerKeys: Set<String>,
    ): List<GuideSection> {
        return sections.mapIndexed { sectionIndex, section ->
            val pieces = section.pieces.coerceAtLeast(1)
            val updatedByPiece = (0 until pieces).map { pieceIndex ->
                val basePieceSteps = GuideProgress.sectionStepsForPiece(section, pieceIndex)
                applyStepCompletionForPiece(
                    sectionIndex = sectionIndex,
                    pieceIndex = pieceIndex,
                    steps = basePieceSteps,
                    completedPointerKeys = completedPointerKeys,
                )
            }

            GuideProgress.updateSectionCompletion(
                section.copy(
                    pieces = pieces,
                    completedPieces = 0,
                    completed = false,
                    steps = updatedByPiece.firstOrNull().orEmpty(),
                    stepsProgressByAmount = updatedByPiece,
                ),
            )
        }
    }

    private fun applyStepCompletionForPiece(
        sectionIndex: Int,
        pieceIndex: Int,
        steps: List<GuideStep>,
        completedPointerKeys: Set<String>,
    ): List<GuideStep> {
        return steps.mapIndexed { stepIndex, step ->
            if (step.subSteps.isNotEmpty()) {
                val updatedSubSteps = step.subSteps.mapIndexed { subStepIndex, subStep ->
                    val key = pointerKey(
                        GuideLeafPointer(
                            sectionIndex = sectionIndex,
                            sectionPieceIndex = pieceIndex,
                            stepIndex = stepIndex,
                            subStepIndex = subStepIndex,
                        ),
                    )
                    subStep.copy(completed = key in completedPointerKeys)
                }
                step.copy(
                    completed = updatedSubSteps.isNotEmpty() && updatedSubSteps.all { it.completed },
                    subSteps = updatedSubSteps,
                )
            } else {
                val key = pointerKey(
                    GuideLeafPointer(
                        sectionIndex = sectionIndex,
                        sectionPieceIndex = pieceIndex,
                        stepIndex = stepIndex,
                        subStepIndex = null,
                    ),
                )
                step.copy(completed = key in completedPointerKeys)
            }
        }
    }
}
