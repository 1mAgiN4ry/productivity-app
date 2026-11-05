package com.example.productivity.ui.tasks

import com.example.productivity.data.tasks.Blocker
import com.example.productivity.data.tasks.Reminder
import com.example.productivity.data.tasks.group
import com.example.productivity.data.tasks.task
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TaskFormatTest {

    private val today = LocalDate.of(2026, 11, 5) // a Thursday
    private lateinit var previousLocale: Locale

    @Before
    fun pinLocale() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.UK)
    }

    @After
    fun restoreLocale() = Locale.setDefault(previousLocale)

    @Test
    fun `today is Today, other dates are the date`() {
        assertEquals("Today", TaskFormat.date(today, today))
        assertEquals("Fri 06 Nov", TaskFormat.date(today.plusDays(1), today))
        assertEquals("Wed 04 Nov", TaskFormat.date(today.minusDays(1), today))
    }

    @Test
    fun `dates in another year include the year`() {
        assertEquals("Tue 05 Jan 2027", TaskFormat.date(LocalDate.of(2027, 1, 5), today))
    }

    @Test
    fun reminders() {
        assertEquals("Due day · 09:00", TaskFormat.reminder(Reminder(0, LocalTime.of(9, 0))))
        assertEquals("1 day before · 18:30", TaskFormat.reminder(Reminder(1, LocalTime.of(18, 30))))
        assertEquals("3 days before · 07:05", TaskFormat.reminder(Reminder(3, LocalTime.of(7, 5))))
    }

    @Test
    fun `hidden summary omits zero counts`() {
        assertEquals("1 blocked · 1 upcoming", TaskFormat.hiddenSummary(1, 1))
        assertEquals("2 blocked", TaskFormat.hiddenSummary(2, 0))
        assertEquals("3 upcoming", TaskFormat.hiddenSummary(0, 3))
        assertNull(TaskFormat.hiddenSummary(0, 0))
    }

    @Test
    fun `blocked by names the first blocker and counts the rest`() {
        val parcel = Blocker.Task(task(1).copy(title = "Return parcel"))
        val home = Blocker.Group(group(2, "Home"))
        assertEquals("Blocked by Return parcel", TaskFormat.blockedBy(listOf(parcel)))
        assertEquals("Blocked by Return parcel +1", TaskFormat.blockedBy(listOf(parcel, home)))
    }
}
