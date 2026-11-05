package com.example.productivity.ui.tasks

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The rule behind the date pickers' greyed-out days. */
class DateRangeTest {

    private val start = LocalDate.of(2026, 11, 5)
    private val due = LocalDate.of(2026, 11, 12)

    @Test
    fun `no limits allows anything`() {
        assertTrue(inRange(start, null, null))
    }

    @Test
    fun `a start date can be on or before the due date`() {
        assertTrue(inRange(due, null, due))
        assertTrue(inRange(due.minusDays(30), null, due))
        assertFalse(inRange(due.plusDays(1), null, due))
    }

    @Test
    fun `a due date can be on or after the start date`() {
        assertTrue(inRange(start, start, null))
        assertTrue(inRange(start.plusDays(30), start, null))
        assertFalse(inRange(start.minusDays(1), start, null))
    }
}
