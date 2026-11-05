package com.example.productivity.ui.tasks

import com.example.productivity.data.tasks.Blocker
import com.example.productivity.data.tasks.Reminder
import com.example.productivity.data.tasks.TaskState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formatting shared by the task screens. Pure functions, unit tested. */
object TaskFormat {

    // Built per call so a change of the phone's language is picked up.
    private fun pattern(p: String) = DateTimeFormatter.ofPattern(p, Locale.getDefault())

    /** "Today", otherwise e.g. "Thu 05 Nov" (with the year when it isn't this year). */
    fun date(date: LocalDate, today: LocalDate): String = when {
        date == today -> "Today"
        date.year == today.year -> date.format(pattern("EEE dd MMM"))
        else -> date.format(pattern("EEE dd MMM yyyy"))
    }

    /** E.g. "Due day · 09:00", "1 day before · 18:30". */
    fun reminder(reminder: Reminder): String {
        val day = when (reminder.daysBeforeDue) {
            0 -> "Due day"
            1 -> "1 day before"
            else -> "${reminder.daysBeforeDue} days before"
        }
        return "$day · ${reminder.time.format(pattern("HH:mm"))}"
    }

    fun state(state: TaskState): String = when (state) {
        TaskState.Open -> "Open"
        TaskState.Waiting -> "Waiting"
        TaskState.Done -> "Done"
        TaskState.Dropped -> "Dropped"
    }

    fun blockerName(blocker: Blocker): String = when (blocker) {
        is Blocker.Task -> blocker.task.title
        is Blocker.Group -> blocker.group.title
    }

    /** The link under a blocked row: "Blocked by Return parcel" or "Blocked by Return parcel +2". */
    fun blockedBy(blockers: List<Blocker>): String {
        val first = blockerName(blockers.first())
        return if (blockers.size == 1) "Blocked by $first" else "Blocked by $first +${blockers.size - 1}"
    }

    /** Footer link text, e.g. "1 blocked · 1 upcoming", or null when nothing is hidden. */
    fun hiddenSummary(blocked: Int, upcoming: Int): String? =
        listOfNotNull(
            blocked.takeIf { it > 0 }?.let { "$it blocked" },
            upcoming.takeIf { it > 0 }?.let { "$it upcoming" },
        ).joinToString(" · ").ifEmpty { null }

    /** Group header summary, e.g. "4 open · 2 done". */
    fun groupSummary(active: Int, done: Int): String = "$active open · $done done"
}
