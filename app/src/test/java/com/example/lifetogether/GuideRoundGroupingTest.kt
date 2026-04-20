package com.example.lifetogether

import com.example.lifetogether.domain.logic.GuideRoundGrouping
import com.example.lifetogether.domain.model.guides.GuideStep
import com.example.lifetogether.domain.model.guides.GuideStepType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GuideRoundGroupingTest {
    @Test
    fun `findRoundGroupContext groups consecutive rounds with same content`() {
        val steps = listOf(
            GuideStep(id = "1", type = GuideStepType.ROUND, name = "R6", content = "sc around", completed = true),
            GuideStep(id = "2", type = GuideStepType.ROUND, name = "R7", content = "sc around", completed = false),
            GuideStep(id = "3", type = GuideStepType.ROUND, name = "R8", content = "sc around", completed = true),
            GuideStep(id = "4", type = GuideStepType.ROUND, name = "R9", content = "dec around", completed = false),
        )

        val context = GuideRoundGrouping.findRoundGroupContext(steps, 1)

        assertNotNull(context)
        assertEquals(0, context?.startIndex)
        assertEquals(2, context?.endIndex)
        assertEquals(6..8, context?.range)
        assertEquals(2, context?.completedCount)
    }

    @Test
    fun `parseRoundPrefix extracts range and shared content`() {
        val parsed = GuideRoundGrouping.parseRoundPrefix("R7-10: sc around")

        assertNotNull(parsed)
        assertEquals(7..10, parsed?.first)
        assertEquals("sc around", parsed?.second)
    }
}
