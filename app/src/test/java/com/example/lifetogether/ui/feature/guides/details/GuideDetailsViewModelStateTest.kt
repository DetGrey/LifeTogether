package com.example.lifetogether.ui.feature.guides.details

import com.example.lifetogether.domain.model.guides.GuideSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideDetailsViewModelStateTest {
    @Test
    fun `reconcileSectionExpandedState collapses completed sections by default`() {
        val sections = listOf(
            GuideSection(id = "complete", title = "Done", completed = true),
            GuideSection(id = "active", title = "Active", completed = false),
        )

        val expandedState = reconcileSectionExpandedState(
            sections = sections,
            existingState = emptyMap(),
        )

        assertFalse(expandedState.getValue("complete"))
        assertTrue(expandedState.getValue("active"))
    }

    @Test
    fun `reconcileSectionExpandedState preserves existing toggles for matching sections`() {
        val sections = listOf(
            GuideSection(id = "complete", title = "Done", completed = true),
            GuideSection(id = "", title = "Untitled", completed = false),
        )

        val expandedState = reconcileSectionExpandedState(
            sections = sections,
            existingState = mapOf(
                "complete" to true,
                "section-1" to false,
                "removed" to true,
            ),
        )

        assertEquals(
            mapOf(
                "complete" to true,
                "section-1" to false,
            ),
            expandedState,
        )
    }

    @Test
    fun `reconcileSelectedSectionAmountState defaults to active amount and preserves existing values`() {
        val sections = listOf(
            GuideSection(id = "first", title = "First", amount = 3, completedAmount = 1),
            GuideSection(id = "", title = "Second", amount = 2, completedAmount = 2),
        )

        val selectedState = reconcileSelectedSectionAmountState(
            sections = sections,
            existingState = mapOf(
                "first" to 2,
                "section-1" to 99,
            ),
        )

        assertEquals(
            mapOf(
                "first" to 2,
                "section-1" to 1,
            ),
            selectedState,
        )
    }
}
