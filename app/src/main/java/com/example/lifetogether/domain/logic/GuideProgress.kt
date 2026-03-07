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
    fun resetSectionsProgress(sections: List<GuideSection>): List<GuideSection> {
        return sections.map { section ->
            val normalizedSection = normalizeSection(section)
            val resetByAmount = normalizedSection.stepsProgressByAmount.map { amountSteps ->
                setAllLeafCompletion(amountSteps, completed = false)
            }
            updateSectionCompletion(
                normalizedSection.copy(
                    completedAmount = 0,
                    completed = false,
                    steps = resetByAmount.firstOrNull().orEmpty(),
                    stepsProgressByAmount = resetByAmount,
                ),
            )
        }
    }

    fun buildLeafPointers(sections: List<GuideSection>): List<GuideLeafPointer> {
        val pointers = mutableListOf<GuideLeafPointer>()
        sections.forEachIndexed { sectionIndex, section ->
            val normalizedSection = normalizeSection(section)
            val amount = normalizedSection.amount
            repeat(amount) { amountIndex ->
                sectionStepsForAmount(normalizedSection, amountIndex).forEachIndexed { stepIndex, step ->
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
        val section = sections.getOrNull(resume.sectionIndex)?.let(::normalizeSection) ?: return null
        val amount = section.amount
        val normalizedAmountIndex = resume.sectionAmountIndex.coerceIn(0, amount - 1)
        val amountSteps = sectionStepsForAmount(section, normalizedAmountIndex)
        val step = amountSteps.getOrNull(resume.stepIndex) ?: return null

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
        val section = sections.getOrNull(pointer.sectionIndex)?.let(::normalizeSection) ?: return null
        val amount = section.amount
        val normalizedAmountIndex = pointer.sectionAmountIndex.coerceIn(0, amount - 1)
        val amountSteps = sectionStepsForAmount(section, normalizedAmountIndex)
        val step = amountSteps.getOrNull(pointer.stepIndex) ?: return null

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

    fun sectionStepsForAmount(
        section: GuideSection,
        amountIndex: Int,
    ): List<GuideStep> {
        val normalizedSection = normalizeSection(section)
        if (normalizedSection.amount <= 0) return emptyList()
        val normalizedAmountIndex = amountIndex.coerceIn(0, normalizedSection.amount - 1)
        return normalizedSection.stepsProgressByAmount.getOrElse(normalizedAmountIndex) { emptyList() }
    }

    fun sectionProgress(section: GuideSection): Pair<Int, Int> {
        val normalizedSection = normalizeSection(section)
        if (normalizedSection.stepsProgressByAmount.isEmpty()) return 0 to 0

        val totalLeafSteps = normalizedSection.stepsProgressByAmount
            .sumOf(::countLeafSteps)
        if (totalLeafSteps == 0) return 0 to 0

        val completedLeafSteps = normalizedSection.stepsProgressByAmount
            .sumOf(::countCompletedLeafSteps)
            .coerceAtMost(totalLeafSteps)

        return completedLeafSteps to totalLeafSteps
    }

    fun progressPercent(section: GuideSection): Int {
        val (completed, total) = sectionProgress(section)
        if (total == 0) return 0
        return ((completed.toFloat() / total.toFloat()) * 100f).toInt()
    }

    fun sectionAmountProgress(
        section: GuideSection,
        amountIndex: Int,
    ): Pair<Int, Int> {
        val amountSteps = sectionStepsForAmount(section, amountIndex)
        val totalLeafSteps = countLeafSteps(amountSteps)
        if (totalLeafSteps == 0) return 0 to 0

        val completedLeafSteps = countCompletedLeafSteps(amountSteps).coerceAtMost(totalLeafSteps)

        return completedLeafSteps to totalLeafSteps
    }

    fun sectionAmountProgressPercent(
        section: GuideSection,
        amountIndex: Int,
    ): Int {
        val (completed, total) = sectionAmountProgress(section, amountIndex)
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
        return normalizeSection(section)
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
        val section = sections.getOrNull(pointer.sectionIndex)?.let(::normalizeSection) ?: return false
        val amount = section.amount
        val normalizedAmountIndex = pointer.sectionAmountIndex.coerceIn(0, amount - 1)
        val amountSteps = sectionStepsForAmount(section, normalizedAmountIndex)
        val step = amountSteps.getOrNull(pointer.stepIndex) ?: return false
        return if (pointer.subStepIndex != null) {
            step.subSteps.getOrNull(pointer.subStepIndex)?.completed == true
        } else {
            step.completed
        }
    }

    fun canTogglePointer(
        sections: List<GuideSection>,
        pointer: GuideLeafPointer,
    ): Boolean {
        val section = sections.getOrNull(pointer.sectionIndex)?.let(::normalizeSection) ?: return false
        val amount = section.amount
        val completedAmount = section.completedAmount
        val activeAmountIndex = activeAmountIndex(
            completedAmount = completedAmount,
            amount = amount,
        )
        return pointer.sectionAmountIndex == activeAmountIndex
    }

    private fun applyCompletionToSectionAmount(
        section: GuideSection,
        pointer: GuideLeafPointer,
        completed: Boolean,
    ): GuideSection {
        val normalizedSection = normalizeSection(section)
        val amount = normalizedSection.amount
        val currentCompletedAmount = normalizedSection.completedAmount
        val activeAmountIndex = activeAmountIndex(
            completedAmount = currentCompletedAmount,
            amount = amount,
        )

        if (pointer.sectionAmountIndex != activeAmountIndex) {
            return normalizedSection
        }

        val existingByAmount = normalizedSection.stepsProgressByAmount.toMutableList()
        val updatedSteps = applyCompletionToSteps(
            steps = existingByAmount[activeAmountIndex],
            pointer = pointer,
            completed = completed,
        )
        existingByAmount[activeAmountIndex] = updatedSteps

        return updateSectionCompletion(
            normalizedSection.copy(
                amount = amount,
                steps = updatedSteps,
                stepsProgressByAmount = existingByAmount,
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
                step.subSteps.all { it.completed }
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

    private fun normalizeSection(section: GuideSection): GuideSection {
        val amount = normalizedAmount(section)
        val rawCompletedAmount = section.completedAmount.coerceIn(0, amount)
        val rawProgressByAmount = section.stepsProgressByAmount
        val baseSteps = when {
            section.steps.isNotEmpty() -> section.steps
            rawProgressByAmount.isNotEmpty() -> rawProgressByAmount.first()
            else -> emptyList()
        }

        val normalizedProgressByAmount = (0 until amount).map { amountIndex ->
            rawProgressByAmount.getOrNull(amountIndex)
                ?: legacyFallbackStepsForAmount(
                    baseSteps = baseSteps,
                    completedAmount = rawCompletedAmount,
                    amount = amount,
                    amountIndex = amountIndex,
                )
        }
        val completedAmount = inferCompletedAmount(
            stepsByAmount = normalizedProgressByAmount,
            fallbackCompletedAmount = rawCompletedAmount,
        ).coerceIn(0, amount)
        val activeAmountIndex = activeAmountIndex(
            completedAmount = completedAmount,
            amount = amount,
        )
        val activeSteps = normalizedProgressByAmount.getOrElse(activeAmountIndex) { emptyList() }

        return section.copy(
            amount = amount,
            completedAmount = completedAmount,
            completed = completedAmount >= amount,
            steps = activeSteps,
            stepsProgressByAmount = normalizedProgressByAmount,
        )
    }

    private fun legacyFallbackStepsForAmount(
        baseSteps: List<GuideStep>,
        completedAmount: Int,
        amount: Int,
        amountIndex: Int,
    ): List<GuideStep> {
        return when {
            completedAmount > amountIndex -> setAllLeafCompletion(baseSteps, completed = true)
            completedAmount == amountIndex && completedAmount < amount -> baseSteps
            else -> setAllLeafCompletion(baseSteps, completed = false)
        }
    }

    private fun inferCompletedAmount(
        stepsByAmount: List<List<GuideStep>>,
        fallbackCompletedAmount: Int,
    ): Int {
        var completedAmount = 0
        while (completedAmount < stepsByAmount.size) {
            val amountSteps = stepsByAmount[completedAmount]
            val isCompleted = if (countLeafSteps(amountSteps) == 0) {
                completedAmount < fallbackCompletedAmount
            } else {
                areAllLeafStepsCompleted(amountSteps)
            }
            if (!isCompleted) break
            completedAmount += 1
        }
        return completedAmount
    }

    private fun activeAmountIndex(
        completedAmount: Int,
        amount: Int,
    ): Int {
        return if (completedAmount >= amount) {
            amount - 1
        } else {
            completedAmount
        }.coerceAtLeast(0)
    }
}
