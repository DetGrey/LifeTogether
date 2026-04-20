package com.example.lifetogether

import com.example.lifetogether.domain.logic.GuideProgress
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.guides.GuideStep
import com.example.lifetogether.domain.model.guides.GuideStepType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideProgressTest {
    @Test
    fun `sectionProgress counts leaf subsection steps`() {
        val section = GuideSection(
            id = "section-1",
            title = "Section",
            steps = listOf(
                GuideStep(id = "step-1", content = "A", completed = true),
                GuideStep(
                    id = "step-2",
                    type = GuideStepType.SUBSECTION,
                    title = "Sub",
                    completed = false,
                    subSteps = listOf(
                        GuideStep(id = "sub-1", content = "B", completed = true),
                        GuideStep(id = "sub-2", content = "C", completed = false),
                    ),
                ),
            ),
        )

        val (completedLeafSteps, totalLeafSteps) = GuideProgress.sectionProgress(section)

        assertEquals(2, completedLeafSteps)
        assertEquals(3, totalLeafSteps)
        assertEquals(66, GuideProgress.progressPercent(section))
    }

    @Test
    fun `sectionAmountProgress only reflects current part for repeated section`() {
        val section = GuideSection(
            id = "section-1",
            title = "Section",
            amount = 3,
            completedAmount = 1,
            steps = listOf(
                GuideStep(id = "step-1", content = "A", completed = true),
                GuideStep(id = "step-2", content = "B", completed = false),
            ),
        )

        assertEquals(2 to 2, GuideProgress.sectionAmountProgress(section, amountIndex = 0))
        assertEquals(1 to 2, GuideProgress.sectionAmountProgress(section, amountIndex = 1))
        assertEquals(0 to 2, GuideProgress.sectionAmountProgress(section, amountIndex = 2))
        assertEquals(50, GuideProgress.sectionAmountProgressPercent(section, amountIndex = 1))
    }

    @Test
    fun `repeated section keeps separate step completion for each amount`() {
        val section = GuideSection(
            id = "section-repeat",
            title = "Ears",
            amount = 2,
            completedAmount = 0,
            steps = listOf(
                GuideStep(id = "step-1", content = "A", completed = true),
                GuideStep(id = "step-2", content = "B", completed = true),
            ),
        )

        val normalized = GuideProgress.updateSectionCompletion(section)
        assertEquals(1, normalized.completedAmount)
        assertEquals(2 to 2, GuideProgress.sectionAmountProgress(normalized, amountIndex = 0))
        assertEquals(0 to 2, GuideProgress.sectionAmountProgress(normalized, amountIndex = 1))

        val secondAmountPointer = GuideProgress.buildLeafPointers(listOf(normalized))
            .first { it.sectionAmountIndex == 1 && it.stepIndex == 0 }
        val updated = GuideProgress.applyLeafCompletion(
            sections = listOf(normalized),
            pointer = secondAmountPointer,
            completed = true,
        ).first()

        assertEquals(1, updated.completedAmount)
        assertEquals(2 to 2, GuideProgress.sectionAmountProgress(updated, amountIndex = 0))
        assertEquals(1 to 2, GuideProgress.sectionAmountProgress(updated, amountIndex = 1))
    }

    @Test
    fun `updateGuideResume returns first incomplete leaf pointer`() {
        val guide = Guide(
            familyId = "family",
            itemName = "Guide",
            sections = listOf(
                GuideSection(
                    id = "section-1",
                    title = "Section 1",
                    steps = listOf(
                        GuideStep(id = "step-1", content = "A", completed = true),
                        GuideStep(id = "step-2", content = "B", completed = false),
                    ),
                ),
            ),
        )

        val resume = GuideProgress.updateGuideResume(guide)

        assertNotNull(resume)
        assertEquals(0, resume?.sectionIndex)
        assertEquals(0, resume?.sectionAmountIndex)
        assertEquals(1, resume?.stepIndex)
    }

    @Test
    fun `applyLeafCompletion updates leaf and section completion`() {
        val section = GuideSection(
            id = "section-1",
            title = "Section 1",
            completed = false,
            steps = listOf(
                GuideStep(id = "step-1", content = "A", completed = false),
            ),
        )

        val pointer = GuideProgress.buildLeafPointers(listOf(section)).first()

        val updatedSections = GuideProgress.applyLeafCompletion(
            sections = listOf(section),
            pointer = pointer,
            completed = true,
        )

        assertTrue(updatedSections.first().steps.first().completed)
        assertTrue(updatedSections.first().completed)

        val revertedSections = GuideProgress.applyLeafCompletion(
            sections = updatedSections,
            pointer = pointer,
            completed = false,
        )

        assertFalse(revertedSections.first().steps.first().completed)
        assertFalse(revertedSections.first().completed)
    }

    @Test
    fun `applyLeafCompletion advances repeated sections and resets next amount`() {
        val repeatedSection = GuideSection(
            id = "section-repeat",
            title = "Ears",
            amount = 2,
            completedAmount = 0,
            completed = false,
            steps = listOf(
                GuideStep(id = "step-1", content = "sc around", completed = false),
            ),
        )

        val passOnePointer = GuideProgress.buildLeafPointers(listOf(repeatedSection)).first()

        val afterFirstPass = GuideProgress.applyLeafCompletion(
            sections = listOf(repeatedSection),
            pointer = passOnePointer,
            completed = true,
        ).first()

        assertEquals(2, afterFirstPass.amount)
        assertEquals(1, afterFirstPass.completedAmount)
        assertFalse(afterFirstPass.completed)
        assertFalse(afterFirstPass.steps.first().completed)
        assertEquals(1 to 2, GuideProgress.sectionProgress(afterFirstPass))

        val secondPassPointer = GuideProgress.firstIncompletePointer(listOf(afterFirstPass))
        assertEquals(1, secondPassPointer?.sectionAmountIndex)

        val afterSecondPass = GuideProgress.applyLeafCompletion(
            sections = listOf(afterFirstPass),
            pointer = secondPassPointer!!,
            completed = true,
        ).first()

        assertEquals(2, afterSecondPass.completedAmount)
        assertTrue(afterSecondPass.completed)
        assertTrue(afterSecondPass.steps.first().completed)
        assertEquals(2 to 2, GuideProgress.sectionProgress(afterSecondPass))
    }

    @Test
    fun `resetSectionsProgress clears repeated section and nested substep completion`() {
        val sections = listOf(
            GuideSection(
                id = "section-repeat",
                title = "Rounds",
                amount = 3,
                completedAmount = 2,
                completed = false,
                steps = listOf(
                    GuideStep(id = "step-1", content = "A", completed = true),
                    GuideStep(
                        id = "step-2",
                        type = GuideStepType.SUBSECTION,
                        title = "Sub",
                        completed = true,
                        subSteps = listOf(
                            GuideStep(id = "sub-1", content = "B", completed = true),
                            GuideStep(id = "sub-2", content = "C", completed = true),
                        ),
                    ),
                ),
            ),
            GuideSection(
                id = "section-done",
                title = "Done",
                completed = true,
                steps = listOf(
                    GuideStep(id = "step-3", content = "D", completed = true),
                ),
            ),
        )

        val resetSections = GuideProgress.resetSectionsProgress(sections)

        assertEquals(0, resetSections[0].completedAmount)
        assertFalse(resetSections[0].completed)
        assertFalse(resetSections[0].steps[0].completed)
        assertFalse(resetSections[0].steps[1].completed)
        assertFalse(resetSections[0].steps[1].subSteps[0].completed)
        assertFalse(resetSections[0].steps[1].subSteps[1].completed)
        assertFalse(resetSections[1].completed)
        assertFalse(resetSections[1].steps[0].completed)
    }
}
