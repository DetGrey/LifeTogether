package com.example.lifetogether.domain.logic

import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideResume
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.guides.GuideStep

data class GuideLeafPointer(
    val sectionIndex: Int,
    val sectionPieceIndex: Int = 0,
    val stepIndex: Int,
    val subStepIndex: Int? = null,
)

object GuideProgress {
    fun resetSectionsProgress(sections: List<GuideSection>): List<GuideSection> {
        return sections.map { section ->
            val normalizedSection = normalizeSection(section)
            val resetByPiece = normalizedSection.stepsProgressByAmount.map { pieceSteps ->
                setAllLeafCompletion(pieceSteps, completed = false)
            }
            updateSectionCompletion(
                normalizedSection.copy(
                    completedPieces = 0,
                    completed = false,
                    steps = resetByPiece.firstOrNull().orEmpty(),
                    stepsProgressByAmount = resetByPiece,
                ),
            )
        }
    }

    fun buildLeafPointers(sections: List<GuideSection>): List<GuideLeafPointer> {
        val pointers = mutableListOf<GuideLeafPointer>()
        sections.forEachIndexed { sectionIndex, section ->
            val normalizedSection = normalizeSection(section)
            val pieces = normalizedSection.pieces
            repeat(pieces) { pieceIndex ->
                sectionStepsForPiece(normalizedSection, pieceIndex).forEachIndexed { stepIndex, step ->
                    if (step.subSteps.isNotEmpty()) {
                        step.subSteps.forEachIndexed { subStepIndex, _ ->
                            pointers += GuideLeafPointer(
                                sectionIndex = sectionIndex,
                                sectionPieceIndex = pieceIndex,
                                stepIndex = stepIndex,
                                subStepIndex = subStepIndex,
                            )
                        }
                    } else {
                        pointers += GuideLeafPointer(
                            sectionIndex = sectionIndex,
                            sectionPieceIndex = pieceIndex,
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
            sectionPieceIndex = pointer.sectionPieceIndex,
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
        val pieces = section.pieces
        val normalizedPieceIndex = resume.sectionPieceIndex.coerceIn(0, pieces - 1)
        val pieceSteps = sectionStepsForPiece(section, normalizedPieceIndex)
        val step = pieceSteps.getOrNull(resume.stepIndex) ?: return null

        val normalizedSubStepIndex = resume.subStepIndex?.takeIf { step.subSteps.isNotEmpty() }
        if (normalizedSubStepIndex != null && step.subSteps.getOrNull(normalizedSubStepIndex) == null) {
            return null
        }

        return GuideLeafPointer(
            sectionIndex = resume.sectionIndex,
            sectionPieceIndex = normalizedPieceIndex,
            stepIndex = resume.stepIndex,
            subStepIndex = normalizedSubStepIndex,
        )
    }

    fun getStepAtPointer(
        sections: List<GuideSection>,
        pointer: GuideLeafPointer,
    ): GuideStep? {
        val section = sections.getOrNull(pointer.sectionIndex)?.let(::normalizeSection) ?: return null
        val pieces = section.pieces
        val normalizedPieceIndex = pointer.sectionPieceIndex.coerceIn(0, pieces - 1)
        val pieceSteps = sectionStepsForPiece(section, normalizedPieceIndex)
        val step = pieceSteps.getOrNull(pointer.stepIndex) ?: return null

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

    fun sectionStepsForPiece(
        section: GuideSection,
        pieceIndex: Int,
    ): List<GuideStep> {
        val normalizedSection = normalizeSection(section)
        if (normalizedSection.pieces <= 0) return emptyList()
        val normalizedPieceIndex = pieceIndex.coerceIn(0, normalizedSection.pieces - 1)
        return normalizedSection.stepsProgressByAmount.getOrElse(normalizedPieceIndex) { emptyList() }
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

    fun sectionPieceProgress(
        section: GuideSection,
        pieceIndex: Int,
    ): Pair<Int, Int> {
        val pieceSteps = sectionStepsForPiece(section, pieceIndex)
        val totalLeafSteps = countLeafSteps(pieceSteps)
        if (totalLeafSteps == 0) return 0 to 0

        val completedLeafSteps = countCompletedLeafSteps(pieceSteps).coerceAtMost(totalLeafSteps)

        return completedLeafSteps to totalLeafSteps
    }

    fun sectionPieceProgressPercent(
        section: GuideSection,
        pieceIndex: Int,
    ): Int {
        val (completed, total) = sectionPieceProgress(section, pieceIndex)
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
                applyCompletionToSectionPiece(section, pointer, completed)
            }
        }
    }

    fun updateSectionCompletion(section: GuideSection): GuideSection {
        return normalizeSection(section)
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
        val pieces = section.pieces
        val normalizedPieceIndex = pointer.sectionPieceIndex.coerceIn(0, pieces - 1)
        val pieceSteps = sectionStepsForPiece(section, normalizedPieceIndex)
        val step = pieceSteps.getOrNull(pointer.stepIndex) ?: return false
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
        val pieces = section.pieces
        val completedPieces = section.completedPieces
        val activePieceIndex = activePieceIndex(
            completedPieces = completedPieces,
            pieces = pieces,
        )
        return pointer.sectionPieceIndex == activePieceIndex
    }

    private fun applyCompletionToSectionPiece(
        section: GuideSection,
        pointer: GuideLeafPointer,
        completed: Boolean,
    ): GuideSection {
        val normalizedSection = normalizeSection(section)
        val pieces = normalizedSection.pieces
        val currentCompletedPieces = normalizedSection.completedPieces
        val activePieceIndex = activePieceIndex(
            completedPieces = currentCompletedPieces,
            pieces = pieces,
        )

        if (pointer.sectionPieceIndex != activePieceIndex) {
            return normalizedSection
        }

        val existingByPiece = normalizedSection.stepsProgressByAmount.toMutableList()
        val updatedSteps = applyCompletionToSteps(
            steps = existingByPiece[activePieceIndex],
            pointer = pointer,
            completed = completed,
        )
        existingByPiece[activePieceIndex] = updatedSteps

        return updateSectionCompletion(
            normalizedSection.copy(
                pieces = pieces,
                steps = updatedSteps,
                stepsProgressByAmount = existingByPiece,
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

    private fun normalizedPieces(section: GuideSection): Int {
        return section.pieces.coerceAtLeast(1)
    }

    private fun normalizeSection(section: GuideSection): GuideSection {
        val pieces = normalizedPieces(section)
        val rawCompletedPieces = section.completedPieces.coerceIn(0, pieces)
        val rawProgressByPiece = section.stepsProgressByAmount
        val baseSteps = when {
            section.steps.isNotEmpty() -> section.steps
            rawProgressByPiece.isNotEmpty() -> rawProgressByPiece.first()
            else -> emptyList()
        }

        val normalizedProgressByPiece = (0 until pieces).map { pieceIndex ->
            rawProgressByPiece.getOrNull(pieceIndex)
                ?: legacyFallbackStepsForPiece(
                    baseSteps = baseSteps,
                    completedPieces = rawCompletedPieces,
                    pieces = pieces,
                    pieceIndex = pieceIndex,
                )
        }
        val completedPieces = inferCompletedPieces(
            stepsByPiece = normalizedProgressByPiece,
            fallbackCompletedPieces = rawCompletedPieces,
        ).coerceIn(0, pieces)
        val activePieceIndex = activePieceIndex(
            completedPieces = completedPieces,
            pieces = pieces,
        )
        val activeSteps = normalizedProgressByPiece.getOrElse(activePieceIndex) { emptyList() }

        return section.copy(
            pieces = pieces,
            completedPieces = completedPieces,
            completed = completedPieces >= pieces,
            steps = activeSteps,
            stepsProgressByAmount = normalizedProgressByPiece,
        )
    }

    private fun legacyFallbackStepsForPiece(
        baseSteps: List<GuideStep>,
        completedPieces: Int,
        pieces: Int,
        pieceIndex: Int,
    ): List<GuideStep> {
        return when {
            completedPieces > pieceIndex -> setAllLeafCompletion(baseSteps, completed = true)
            completedPieces == pieceIndex && completedPieces < pieces -> baseSteps
            else -> setAllLeafCompletion(baseSteps, completed = false)
        }
    }

    private fun inferCompletedPieces(
        stepsByPiece: List<List<GuideStep>>,
        fallbackCompletedPieces: Int,
    ): Int {
        var completedPieces = 0
        while (completedPieces < stepsByPiece.size) {
            val pieceSteps = stepsByPiece[completedPieces]
            val isCompleted = if (countLeafSteps(pieceSteps) == 0) {
                completedPieces < fallbackCompletedPieces
            } else {
                areAllLeafStepsCompleted(pieceSteps)
            }
            if (!isCompleted) break
            completedPieces += 1
        }
        return completedPieces
    }

    private fun activePieceIndex(
        completedPieces: Int,
        pieces: Int,
    ): Int {
        return if (completedPieces >= pieces) {
            pieces - 1
        } else {
            completedPieces
        }.coerceAtLeast(0)
    }
}
