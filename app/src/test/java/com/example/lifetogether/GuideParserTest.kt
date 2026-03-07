package com.example.lifetogether

import com.example.lifetogether.domain.logic.GuideParser
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.guides.GuideStep
import com.example.lifetogether.domain.model.guides.GuideStepType
import com.example.lifetogether.domain.model.guides.GuideVisibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class GuideParserTest {
    @Test
    fun `parseGuideMap normalizes completed keys and subsection object steps`() {
        val rawGuide = mapOf(
            "id" to "guide-1",
            "familyId" to "family-a",
            "ownerUid" to "owner-a",
            "name" to "My guide",
            "sections" to listOf(
                mapOf(
                    "id" to "section-1",
                    "orderNumber" to 1,
                    "title" to "Section 1",
                    "completed" to true,
                    "steps" to listOf(
                        mapOf(
                            "id" to "step-1",
                            "type" to "subsection",
                            "title" to "Nose",
                            "steps" to mapOf(
                                "id" to "leaf-1",
                                "type" to "numbered",
                                "content" to "Build the nose",
                            ),
                        ),
                    ),
                ),
                mapOf(
                    "id" to "section-2",
                    "orderNumber" to 2,
                    "title" to "Section 2",
                    "isCompleted" to true,
                    "steps" to emptyList<Map<String, Any?>>(),
                ),
            ),
        )

        val guide = GuideParser.parseGuideMap(rawGuide)

        assertEquals("My guide", guide.itemName)
        assertEquals(2, guide.sections.size)
        assertTrue(guide.sections[0].completed)
        assertTrue(guide.sections[1].completed)

        val firstStep = guide.sections[0].steps.first()
        assertEquals(GuideStepType.SUBSECTION, firstStep.type)
        assertEquals(1, firstStep.subSteps.size)
        assertEquals("Build the nose", firstStep.subSteps.first().content)
        assertEquals(GuideStepType.NUMBERED, firstStep.subSteps.first().type)
        assertTrue(firstStep.subSteps.first().completed)
    }

    @Test
    fun `parseJsonGuides keeps guide id empty and regenerates nested ids`() {
        val json =
            """
            {
              "id": "original-guide-id",
              "name": "Imported guide",
              "sections": [
                {
                  "id": "original-section-id",
                  "orderNumber": 1,
                  "title": "Head",
                  "steps": [
                    {
                      "id": "original-step-id",
                      "type": "numbered",
                      "content": "Do this"
                    }
                  ]
                }
              ]
            }
            """.trimIndent()

        val guides = GuideParser.parseJsonGuides(
            json = json,
            familyId = "family-import",
            ownerUid = "user-import",
        )

        assertEquals(1, guides.size)

        val importedGuide = guides.first()
        assertNull(importedGuide.id)
        assertEquals("family-import", importedGuide.familyId)
        assertEquals("user-import", importedGuide.ownerUid)
        assertEquals(GuideVisibility.PRIVATE, importedGuide.visibility)

        val importedSection = importedGuide.sections.first()
        assertNotEquals("original-section-id", importedSection.id)
        assertNotEquals("original-step-id", importedSection.steps.first().id)
        assertTrue(importedSection.id.isNotBlank())
        assertTrue(importedSection.steps.first().id.isNotBlank())
    }

    @Test
    fun `guideToFirestoreMap serializes step enum as lowercase type string`() {
        val guide = Guide(
            id = "guide-id",
            familyId = "family-a",
            ownerUid = "owner-a",
            itemName = "Typed guide",
            sections = listOf(
                GuideSection(
                    id = "section-id",
                    title = "Section",
                    steps = listOf(
                        GuideStep(
                            id = "step-id",
                            type = GuideStepType.ROUND,
                            name = "R1",
                            content = "sc 3",
                        ),
                    ),
                ),
            ),
        )

        val map = GuideParser.guideToFirestoreMap(guide)
        assertFalse(map.containsKey("id"))
        @Suppress("UNCHECKED_CAST")
        val sections = map["sections"] as List<Map<String, Any?>>
        @Suppress("UNCHECKED_CAST")
        val steps = sections.first()["steps"] as List<Map<String, Any?>>

        assertEquals("round", steps.first()["type"])
    }

    @Test
    fun `parseGuideMap expands round range shorthand into individual round steps`() {
        val rawGuide = mapOf(
            "id" to "guide-1",
            "familyId" to "family-a",
            "ownerUid" to "owner-a",
            "name" to "Range guide",
            "sections" to listOf(
                mapOf(
                    "id" to "section-1",
                    "orderNumber" to 1,
                    "title" to "Section 1",
                    "steps" to listOf(
                        mapOf(
                            "id" to "step-1",
                            "type" to "round",
                            "name" to "R7-10",
                            "content" to "sc around",
                        ),
                    ),
                ),
            ),
        )

        val parsed = GuideParser.parseGuideMap(rawGuide)
        val parsedSteps = parsed.sections.first().steps

        assertEquals(4, parsedSteps.size)
        assertEquals(listOf("R7", "R8", "R9", "R10"), parsedSteps.map { it.name })
        assertTrue(parsedSteps.all { it.content == "sc around" })
    }

    @Test
    fun `parseGuideMap handles section amount and completedAmount`() {
        val rawGuide = mapOf(
            "id" to "guide-amount",
            "familyId" to "family-a",
            "ownerUid" to "owner-a",
            "name" to "Amount guide",
            "sections" to listOf(
                mapOf(
                    "id" to "section-ears",
                    "orderNumber" to 1,
                    "title" to "Ears",
                    "amount" to 2,
                    "completedAmount" to 1,
                    "completed" to false,
                    "steps" to listOf(
                        mapOf(
                            "id" to "step-1",
                            "type" to "round",
                            "name" to "R1",
                            "content" to "sc around",
                            "completed" to false,
                        ),
                    ),
                ),
            ),
        )

        val parsed = GuideParser.parseGuideMap(rawGuide)
        val section = parsed.sections.first()

        assertEquals(2, section.amount)
        assertEquals(1, section.completedAmount)
        assertFalse(section.completed)
        assertEquals(2, section.stepsProgressByAmount.size)
        assertTrue(section.stepsProgressByAmount[0].all { it.completed })
        assertTrue(section.stepsProgressByAmount[1].all { !it.completed })

        val firestoreMap = GuideParser.guideToFirestoreMap(parsed)
        @Suppress("UNCHECKED_CAST")
        val sections = firestoreMap["sections"] as List<Map<String, Any?>>
        assertEquals(2, sections.first()["amount"])
        assertFalse(sections.first().containsKey("completedAmount"))
        assertFalse(sections.first().containsKey("completed"))
        assertFalse(sections.first().containsKey("stepsProgressByAmount"))
        @Suppress("UNCHECKED_CAST")
        val storedSteps = sections.first()["steps"] as List<Map<String, Any?>>
        assertFalse(storedSteps.first().containsKey("completed"))
    }

    @Test
    fun `parseGuideMap accepts stepsProgressByAmount as indexed map`() {
        val rawGuide = mapOf(
            "name" to "Map progress guide",
            "sections" to listOf(
                mapOf(
                    "title" to "Tail",
                    "amount" to 2,
                    "steps" to listOf(
                        mapOf(
                            "id" to "step-fallback",
                            "type" to "round",
                            "name" to "R1",
                            "content" to "fallback",
                        ),
                    ),
                    "stepsProgressByAmount" to mapOf(
                        "0" to listOf(
                            mapOf(
                                "id" to "step-0",
                                "type" to "round",
                                "name" to "R1",
                                "content" to "part0",
                                "completed" to true,
                            ),
                        ),
                        "1" to listOf(
                            mapOf(
                                "id" to "step-1",
                                "type" to "round",
                                "name" to "R1",
                                "content" to "part1",
                                "completed" to false,
                            ),
                        ),
                    ),
                ),
            ),
        )

        val parsed = GuideParser.parseGuideMap(rawGuide)
        val section = parsed.sections.first()

        assertEquals(2, section.amount)
        assertEquals(2, section.stepsProgressByAmount.size)
        assertEquals("part0", section.stepsProgressByAmount[0].first().content)
        assertEquals("part1", section.stepsProgressByAmount[1].first().content)
        assertEquals(1, section.completedAmount)
        assertFalse(section.completed)
    }

    @Test
    fun `parseFirestoreGuide ignores embedded progress hints`() {
        val rawGuide = mapOf(
            "familyId" to "family-a",
            "ownerUid" to "owner-a",
            "name" to "Remote guide",
            "started" to true,
            "resume" to mapOf(
                "sectionIndex" to 0,
                "sectionAmountIndex" to 0,
                "stepIndex" to 0,
            ),
            "sections" to listOf(
                mapOf(
                    "id" to "section-1",
                    "title" to "Section",
                    "amount" to 2,
                    "completedAmount" to 1,
                    "completed" to true,
                    "steps" to listOf(
                        mapOf(
                            "id" to "step-1",
                            "type" to "round",
                            "content" to "sc around",
                            "completed" to true,
                        ),
                    ),
                    "stepsProgressByAmount" to mapOf(
                        "0" to listOf(
                            mapOf(
                                "id" to "step-1",
                                "type" to "round",
                                "content" to "sc around",
                                "completed" to true,
                            ),
                        ),
                        "1" to listOf(
                            mapOf(
                                "id" to "step-2",
                                "type" to "round",
                                "content" to "sc around again",
                                "completed" to true,
                            ),
                        ),
                    ),
                ),
            ),
        )

        val parsed = GuideParser.parseFirestoreGuide("guide-1", rawGuide)
        val section = parsed.sections.first()

        assertFalse(parsed.started)
        assertNull(parsed.resume)
        assertEquals(0, section.completedAmount)
        assertFalse(section.completed)
        assertTrue(section.steps.all { !it.completed })
        assertEquals(2, section.stepsProgressByAmount.size)
        assertTrue(section.stepsProgressByAmount.all { amountSteps ->
            amountSteps.all { !it.completed }
        })
    }
}
