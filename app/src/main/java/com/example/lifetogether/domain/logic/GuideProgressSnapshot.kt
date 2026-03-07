package com.example.lifetogether.domain.logic

import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.guides.GuideStep

object GuideProgressSnapshot {
    fun pointerKey(pointer: GuideLeafPointer): String {
        val subStepIndex = pointer.subStepIndex ?: -1
        return "${pointer.sectionIndex}:${pointer.sectionAmountIndex}:${pointer.stepIndex}:$subStepIndex"
    }

    fun completedPointerKeysFromSections(sections: List<GuideSection>): Set<String> {
        if (sections.isEmpty()) return emptySet()
        val allLeaves = GuideProgress.buildLeafPointers(sections)
        if (allLeaves.isEmpty()) return emptySet()

        val leavesBySectionAmount = allLeaves.groupBy { it.sectionIndex to it.sectionAmountIndex }
        val completedKeys = mutableSetOf<String>()

        sections.forEachIndexed { sectionIndex, section ->
            val amount = section.amount.coerceAtLeast(1)
            val completedAmount = section.completedAmount.coerceIn(0, amount)

            (0 until completedAmount).forEach { amountIndex ->
                leavesBySectionAmount[sectionIndex to amountIndex].orEmpty()
                    .forEach { completedKeys += pointerKey(it) }
            }

            if (completedAmount < amount) {
                leavesBySectionAmount[sectionIndex to completedAmount].orEmpty().forEach { pointer ->
                    if (GuideProgress.getStepAtPointer(sections, pointer)?.completed == true) {
                        completedKeys += pointerKey(pointer)
                    }
                }
            }
        }

        return completedKeys
    }

    fun applyCompletedPointerKeys(
        sections: List<GuideSection>,
        completedPointerKeys: Set<String>,
    ): List<GuideSection> {
        if (sections.isEmpty()) return sections
        val allLeaves = GuideProgress.buildLeafPointers(sections)
        if (allLeaves.isEmpty()) return sections

        val leavesBySectionAmount = allLeaves.groupBy { it.sectionIndex to it.sectionAmountIndex }

        return sections.mapIndexed { sectionIndex, section ->
            val amount = section.amount.coerceAtLeast(1)
            var completedAmount = 0
            while (completedAmount < amount) {
                val leavesForAmount = leavesBySectionAmount[sectionIndex to completedAmount].orEmpty()
                if (leavesForAmount.isEmpty()) break
                val isComplete = leavesForAmount.all { pointerKey(it) in completedPointerKeys }
                if (!isComplete) break
                completedAmount += 1
            }

            val activeAmountIndex = if (completedAmount >= amount) amount - 1 else completedAmount
            val updatedSteps = applyStepCompletionForAmount(
                sectionIndex = sectionIndex,
                amountIndex = activeAmountIndex.coerceAtLeast(0),
                steps = section.steps,
                completedPointerKeys = completedPointerKeys,
            )

            section.copy(
                amount = amount,
                completedAmount = completedAmount.coerceIn(0, amount),
                completed = completedAmount >= amount,
                steps = updatedSteps,
            )
        }
    }

    private fun applyStepCompletionForAmount(
        sectionIndex: Int,
        amountIndex: Int,
        steps: List<GuideStep>,
        completedPointerKeys: Set<String>,
    ): List<GuideStep> {
        return steps.mapIndexed { stepIndex, step ->
            if (step.subSteps.isNotEmpty()) {
                val updatedSubSteps = step.subSteps.mapIndexed { subStepIndex, subStep ->
                    val key = pointerKey(
                        GuideLeafPointer(
                            sectionIndex = sectionIndex,
                            sectionAmountIndex = amountIndex,
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
                        sectionAmountIndex = amountIndex,
                        stepIndex = stepIndex,
                        subStepIndex = null,
                    ),
                )
                step.copy(completed = key in completedPointerKeys)
            }
        }
    }
}
