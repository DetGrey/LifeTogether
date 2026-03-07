package com.example.lifetogether.domain.logic

import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideResume
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.guides.GuideStep

data class GuideLeafPointer(
    val sectionIndex: Int,
    val sectionAmountIndex: Int = 0,
    val stepIndex: Int,
    val subStepIndex: Int? = null,
)

object GuideProgress {
    fun buildLeafPointers(sections: List<GuideSection>): List<GuideLeafPointer> {
        val pointers = mutableListOf<GuideLeafPointer>()
        sections.forEachIndexed { sectionIndex, section ->
            val amount = normalizedAmount(section)
            repeat(amount) { amountIndex ->
                section.steps.forEachIndexed { stepIndex, step ->
                    if (step.subSteps.isNotEmpty()) {
                        step.subSteps.forEachIndexed { subStepIndex, _ ->
                            pointers += GuideLeafPointer(
                                sectionIndex = sectionIndex,
                                sectionAmountIndex = amountIndex,
                                stepIndex = stepIndex,
                                subStepIndex = subStepIndex,
                            )
                        }
                    } else {
                        pointers += GuideLeafPointer(
                            sectionIndex = sectionIndex,
                            sectionAmountIndex = amountIndex,
                            stepIndex = stepIndex,
                            subStepIndex = null,
                        )
                    }
                }
            }
        }
        return pointers
    }

    fun resumeFromPointer(pointer: GuideLeafPointer): GuideResume {
        return GuideResume(
            sectionIndex = pointer.sectionIndex,
            sectionAmountIndex = pointer.sectionAmountIndex,
            stepIndex = pointer.stepIndex,
            subStepIndex = pointer.subStepIndex,
        )
    }

    fun pointerFromResume(
        resume: GuideResume?,
        sections: List<GuideSection>,
    ): GuideLeafPointer? {
        if (resume == null) return null
        val section = sections.getOrNull(resume.sectionIndex) ?: return null
        val amount = normalizedAmount(section)
        val normalizedAmountIndex = resume.sectionAmountIndex.coerceIn(0, amount - 1)
        val step = section.steps.getOrNull(resume.stepIndex) ?: return null

        val normalizedSubStepIndex = resume.subStepIndex?.takeIf { step.subSteps.isNotEmpty() }
        if (normalizedSubStepIndex != null && step.subSteps.getOrNull(normalizedSubStepIndex) == null) {
            return null
        }

        return GuideLeafPointer(
            sectionIndex = resume.sectionIndex,
            sectionAmountIndex = normalizedAmountIndex,
            stepIndex = resume.stepIndex,
            subStepIndex = normalizedSubStepIndex,
        )
    }

    fun getStepAtPointer(
        sections: List<GuideSection>,
        pointer: GuideLeafPointer,
    ): GuideStep? {
        val section = sections.getOrNull(pointer.sectionIndex) ?: return null
        val step = section.steps.getOrNull(pointer.stepIndex) ?: return null

        return if (pointer.subStepIndex != null) {
            step.subSteps.getOrNull(pointer.subStepIndex)
        } else {
            step
        }
    }

    fun firstIncompletePointer(sections: List<GuideSection>): GuideLeafPointer? {
        return buildLeafPointers(sections).firstOrNull { pointer ->
            !isPointerCompleted(sections, pointer)
        }
    }

    fun sectionProgress(section: GuideSection): Pair<Int, Int> {
        val amount = normalizedAmount(section)
        val completedAmount = normalizedCompletedAmount(section)
        val leafStepsPerAmount = countLeafSteps(section.steps)

        if (leafStepsPerAmount == 0) return 0 to 0

        val totalLeafSteps = leafStepsPerAmount * amount
        if (completedAmount >= amount) return totalLeafSteps to totalLeafSteps

        val currentPassCompleted = countCompletedLeafSteps(section.steps)
        val completedLeafSteps = (leafStepsPerAmount * completedAmount + currentPassCompleted)
            .coerceAtMost(totalLeafSteps)

        return completedLeafSteps to totalLeafSteps
    }

    fun progressPercent(section: GuideSection): Int {
        val (completed, total) = sectionProgress(section)
        if (total == 0) return 0
        return ((completed.toFloat() / total.toFloat()) * 100f).toInt()
    }

    fun applyLeafCompletion(
        sections: List<GuideSection>,
        pointer: GuideLeafPointer,
        completed: Boolean,
    ): List<GuideSection> {
        return sections.mapIndexed { sectionIndex, section ->
            if (sectionIndex != pointer.sectionIndex) {
                updateSectionCompletion(section)
            } else {
                applyCompletionToSectionAmount(section, pointer, completed)
            }
        }
    }

    fun updateSectionCompletion(section: GuideSection): GuideSection {
        val amount = normalizedAmount(section)
        val completedAmount = if (amount == 1) {
            if (areAllLeafStepsCompleted(section.steps)) 1 else 0
        } else {
            normalizedCompletedAmount(section)
        }

        return section.copy(
            amount = amount,
            completedAmount = completedAmount,
            completed = completedAmount >= amount,
        )
    }

    fun updateGuideResume(guide: Guide): GuideResume? {
        return firstIncompletePointer(guide.sections)?.let { resumeFromPointer(it) }
    }

    fun defaultPointerForGuide(guide: Guide): GuideLeafPointer? {
        val allLeaves = buildLeafPointers(guide.sections)
        if (allLeaves.isEmpty()) return null

        if (!guide.started) return allLeaves.first()

        val pointerFromResume = pointerFromResume(guide.resume, guide.sections)
        if (pointerFromResume != null) return pointerFromResume

        return firstIncompletePointer(guide.sections) ?: allLeaves.last()
    }

    fun isPointerCompleted(
        sections: List<GuideSection>,
        pointer: GuideLeafPointer,
    ): Boolean {
        val section = sections.getOrNull(pointer.sectionIndex) ?: return false
        val amount = normalizedAmount(section)
        val completedAmount = normalizedCompletedAmount(section)
        val normalizedAmountIndex = pointer.sectionAmountIndex.coerceIn(0, amount - 1)

        if (normalizedAmountIndex < completedAmount) return true
        if (normalizedAmountIndex > completedAmount) return false

        return getStepAtPointer(sections, pointer)?.completed == true
    }

    fun canTogglePointer(
        sections: List<GuideSection>,
        pointer: GuideLeafPointer,
    ): Boolean {
        val section = sections.getOrNull(pointer.sectionIndex) ?: return false
        val amount = normalizedAmount(section)
        val completedAmount = normalizedCompletedAmount(section)
        val activeAmountIndex = if (completedAmount >= amount) amount - 1 else completedAmount
        return pointer.sectionAmountIndex == activeAmountIndex
    }

    private fun applyCompletionToSectionAmount(
        section: GuideSection,
        pointer: GuideLeafPointer,
        completed: Boolean,
    ): GuideSection {
        val amount = normalizedAmount(section)
        val currentCompletedAmount = normalizedCompletedAmount(section)
        val activeAmountIndex = if (currentCompletedAmount >= amount) amount - 1 else currentCompletedAmount

        if (pointer.sectionAmountIndex != activeAmountIndex) {
            return updateSectionCompletion(section.copy(amount = amount))
        }

        val updatedSteps = applyCompletionToSteps(
            steps = section.steps,
            pointer = pointer,
            completed = completed,
        )
        val passCompleted = areAllLeafStepsCompleted(updatedSteps)

        var nextCompletedAmount = currentCompletedAmount
        var nextSteps = updatedSteps

        if (currentCompletedAmount < amount) {
            if (passCompleted) {
                nextCompletedAmount = (currentCompletedAmount + 1).coerceAtMost(amount)
                if (nextCompletedAmount < amount) {
                    nextSteps = setAllLeafCompletion(updatedSteps, completed = false)
                }
            }
        } else if (!passCompleted) {
            nextCompletedAmount = (amount - 1).coerceAtLeast(0)
        }

        return updateSectionCompletion(
            section.copy(
                amount = amount,
                completedAmount = nextCompletedAmount,
                steps = nextSteps,
            ),
        )
    }

    private fun applyCompletionToSteps(
        steps: List<GuideStep>,
        pointer: GuideLeafPointer,
        completed: Boolean,
    ): List<GuideStep> {
        return steps.mapIndexed { stepIndex, step ->
            if (stepIndex != pointer.stepIndex) {
                step
            } else if (pointer.subStepIndex != null) {
                val updatedSubSteps = step.subSteps.mapIndexed { subStepIndex, subStep ->
                    if (subStepIndex == pointer.subStepIndex) {
                        subStep.copy(completed = completed)
                    } else {
                        subStep
                    }
                }
                step.copy(
                    completed = updatedSubSteps.isNotEmpty() && updatedSubSteps.all { it.completed },
                    subSteps = updatedSubSteps,
                )
            } else {
                step.copy(completed = completed)
            }
        }
    }

    private fun setAllLeafCompletion(
        steps: List<GuideStep>,
        completed: Boolean,
    ): List<GuideStep> {
        return steps.map { step ->
            if (step.subSteps.isNotEmpty()) {
                val updatedSubSteps = setAllLeafCompletion(step.subSteps, completed)
                step.copy(
                    completed = updatedSubSteps.isNotEmpty() && updatedSubSteps.all { it.completed },
                    subSteps = updatedSubSteps,
                )
            } else {
                step.copy(completed = completed)
            }
        }
    }

    private fun areAllLeafStepsCompleted(steps: List<GuideStep>): Boolean {
        if (steps.isEmpty()) return false
        return steps.all { step ->
            if (step.subSteps.isNotEmpty()) {
                step.subSteps.isNotEmpty() && step.subSteps.all { it.completed }
            } else {
                step.completed
            }
        }
    }

    private fun countLeafSteps(steps: List<GuideStep>): Int {
        return steps.sumOf { step ->
            if (step.subSteps.isNotEmpty()) {
                step.subSteps.size
            } else {
                1
            }
        }
    }

    private fun countCompletedLeafSteps(steps: List<GuideStep>): Int {
        return steps.sumOf { step ->
            if (step.subSteps.isNotEmpty()) {
                step.subSteps.count { it.completed }
            } else if (step.completed) {
                1
            } else {
                0
            }
        }
    }

    private fun normalizedAmount(section: GuideSection): Int {
        return section.amount.coerceAtLeast(1)
    }

    private fun normalizedCompletedAmount(section: GuideSection): Int {
        return section.completedAmount.coerceIn(0, normalizedAmount(section))
    }
}
