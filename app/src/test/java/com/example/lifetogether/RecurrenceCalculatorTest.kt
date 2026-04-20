package com.example.lifetogether

import com.example.lifetogether.domain.logic.RecurrenceCalculator
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class RecurrenceCalculatorTest {

    private fun makeEntry(
        recurrenceUnit: RecurrenceUnit = RecurrenceUnit.DAYS,
        interval: Int = 1,
        weekdays: List<Int> = emptyList(),
    ) = RoutineListEntry(
        id = "test",
        familyId = "fam",
        listId = "list1",
        itemName = "Test",
        recurrenceUnit = recurrenceUnit,
        interval = interval,
        weekdays = weekdays,
    )

    private fun daysBetween(a: Date, b: Date): Long =
        (b.time - a.time) / (24L * 60 * 60 * 1000)

    // ----------------------------------------------------------------- DAYS

    @Test
    fun `nextDate daily 1 is always 1 day after from`() {
        val now = Date()
        val entry = makeEntry(RecurrenceUnit.DAYS, interval = 1)
        val next = RecurrenceCalculator.nextDate(entry, from = now)
        assertTrue("next must be after now", next.after(now))
        assertEquals(1L, daysBetween(now, next))
    }

    @Test
    fun `nextDate daily 7 is always 7 days after from`() {
        val now = Date()
        val entry = makeEntry(RecurrenceUnit.DAYS, interval = 7)
        val next = RecurrenceCalculator.nextDate(entry, from = now)
        assertEquals(7L, daysBetween(now, next))
    }

    @Test
    fun `nextDate daily is always exactly interval days ahead regardless of when from is`() {
        val entry = makeEntry(RecurrenceUnit.DAYS, interval = 3)
        listOf(Date(), addDays(Date(), -10), addDays(Date(), 5)).forEach { from ->
            val next = RecurrenceCalculator.nextDate(entry, from = from)
            assertEquals("expected 3 days ahead of $from", 3L, daysBetween(from, next))
        }
    }

    @Test
    fun `nextDate daily completion late still gives interval days from completion`() {
        // Complete 2 days late — next should still be interval days from completion, not "catch up"
        val entry = makeEntry(RecurrenceUnit.DAYS, interval = 7)
        val completedLate = addDays(Date(), -2) // pretend we completed 2 days ago
        val next = RecurrenceCalculator.nextDate(entry, from = completedLate)
        assertEquals(7L, daysBetween(completedLate, next))
    }

    // ----------------------------------------------------------------- WEEKS

    @Test
    fun `nextDate weekly finds next matching weekday after from`() {
        val now = Date()
        val entry = makeEntry(RecurrenceUnit.WEEKS, interval = 1, weekdays = listOf(1, 3, 5)) // Mon, Wed, Fri
        val next = RecurrenceCalculator.nextDate(entry, from = now)
        assertTrue("next must be after now", next.after(now))
        val cal = Calendar.getInstance()
        cal.time = next
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val mapped = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
        assertTrue("day must be Mon, Wed, or Fri", mapped in listOf(1, 3, 5))
    }

    @Test
    fun `nextDate weekly always returns a day strictly after from`() {
        // Even if today is a matching weekday, next should be the NEXT occurrence, not today
        val now = Date()
        val entry = makeEntry(RecurrenceUnit.WEEKS, interval = 1, weekdays = listOf(1, 2, 3, 4, 5, 6, 7))
        val next = RecurrenceCalculator.nextDate(entry, from = now)
        assertTrue("next must be strictly after now", next.after(now))
        assertEquals(1L, daysBetween(now, next))
    }

    @Test
    fun `nextDate weekly with empty weekdays returns from unchanged`() {
        val now = Date()
        val entry = makeEntry(RecurrenceUnit.WEEKS, interval = 1, weekdays = emptyList())
        val next = RecurrenceCalculator.nextDate(entry, from = now)
        assertEquals(now, next)
    }

    @Test
    fun `nextDate weekly result is within interval weeks window`() {
        val now = Date()
        val entry = makeEntry(RecurrenceUnit.WEEKS, interval = 2, weekdays = listOf(1, 5)) // Mon, Fri
        val next = RecurrenceCalculator.nextDate(entry, from = now)
        val daysAhead = daysBetween(now, next)
        assertTrue("next must be within 2 weeks + 1 day", daysAhead in 1..15)
    }

    // ----------------------------------------------------------------- COMPLETION

    @Test
    fun `applyCompletion increments count and sets lastCompletedAt`() {
        val entry = makeEntry(RecurrenceUnit.DAYS, interval = 1)
        val completedAt = Date()
        val updated = RecurrenceCalculator.applyCompletion(entry, completedAt)
        assertEquals(1, updated.completionCount)
        assertEquals(completedAt, updated.lastCompletedAt)
    }

    @Test
    fun `applyCompletion stacks on existing count`() {
        val base = makeEntry(RecurrenceUnit.DAYS, interval = 1).copy(completionCount = 4)
        val updated = RecurrenceCalculator.applyCompletion(base, Date())
        assertEquals(5, updated.completionCount)
    }

    @Test
    fun `applyCompletion sets nextDate to interval days after completion`() {
        val now = Date()
        val entry = makeEntry(RecurrenceUnit.DAYS, interval = 5)
        val updated = RecurrenceCalculator.applyCompletion(entry, now)
        assertEquals(5L, daysBetween(now, updated.nextDate!!))
    }

    @Test
    fun `applyCompletion late completion still gives interval days from completion not from original due date`() {
        val entry = makeEntry(RecurrenceUnit.DAYS, interval = 7)
        val completedLate = addDays(Date(), -3) // completed 3 days after it was due
        val updated = RecurrenceCalculator.applyCompletion(entry, completedLate)
        assertEquals(7L, daysBetween(completedLate, updated.nextDate!!))
    }

    @Test
    fun `applyCompletion updates lastUpdated`() {
        val entry = makeEntry(RecurrenceUnit.DAYS, interval = 1)
        val completedAt = Date()
        val updated = RecurrenceCalculator.applyCompletion(entry, completedAt)
        assertEquals(completedAt, updated.lastUpdated)
    }

    // ----------------------------------------------------------------- VISIBILITY CONSTANTS

    @Test
    fun `generic visibility constants have expected values`() {
        assertEquals("family", Constants.VISIBILITY_FAMILY)
        assertEquals("private", Constants.VISIBILITY_PRIVATE)
    }

    // ----------------------------------------------------------------- HELPERS

    private fun addDays(base: Date, n: Int): Date {
        val cal = Calendar.getInstance()
        cal.time = base
        cal.add(Calendar.DAY_OF_YEAR, n)
        return cal.time
    }
}
