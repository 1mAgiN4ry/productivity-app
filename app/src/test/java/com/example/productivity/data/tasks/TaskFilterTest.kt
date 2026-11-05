package com.example.productivity.data.tasks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskFilterTest {

    private fun TaskGraph.ids(filter: TaskFilter) = query(filter, TODAY).map { it.task.id }

    @Test
    fun `default shows loose open and waiting tasks only`() {
        val g = graph(
            listOf(
                task(1),
                task(2, TaskState.Waiting),
                task(3, TaskState.Done),
                task(4, TaskState.Dropped),
                task(5, groupId = 9),
                task(6, start = TODAY.plusDays(2)), // upcoming
                task(7), // blocked by 1
            ),
            groups = listOf(group(9)),
            taskDeps = listOf(7L to 1L),
        )
        assertEquals(listOf(1L, 2L), g.ids(TaskFilter()))
    }

    @Test
    fun `show hidden adds blocked and upcoming tasks in place`() {
        val g = graph(
            listOf(task(1), task(6, start = TODAY.plusDays(2)), task(7)),
            taskDeps = listOf(7L to 1L),
        )
        assertEquals(listOf(1L, 6L, 7L), g.ids(TaskFilter(showHidden = true)))
    }

    @Test
    fun `the blocked chip shows blocked tasks without the eye toggle`() {
        val g = graph(listOf(task(1), task(7)), taskDeps = listOf(7L to 1L))
        assertEquals(listOf(7L), g.ids(TaskFilter(states = setOf(StateFilter.Blocked))))
    }

    @Test
    fun `state chips combine with OR`() {
        val g = graph(listOf(task(1), task(2, TaskState.Done), task(3, TaskState.Dropped)))
        assertEquals(listOf(2L, 3L), g.ids(TaskFilter(states = setOf(StateFilter.Done, StateFilter.Dropped))))
    }

    @Test
    fun `grouping and group scope`() {
        val g = graph(
            listOf(task(1), task(2, groupId = 8), task(3, groupId = 9)),
            groups = listOf(group(8), group(9)),
        )
        assertEquals(listOf(1L, 2L, 3L), g.ids(TaskFilter(grouping = Grouping.All)))
        assertEquals(listOf(2L, 3L), g.ids(TaskFilter(grouping = Grouping.Grouped)))
        assertEquals(listOf(3L), g.ids(TaskFilter(groupId = 9)))
    }

    @Test
    fun `date chips`() {
        val g = graph(
            listOf(
                task(1, due = TODAY.minusDays(1)),                    // overdue
                task(2, TaskState.Done, due = TODAY.minusDays(1)),    // finished, so not overdue
                task(3, due = TODAY),                                 // due today
                task(4, start = TODAY.minusDays(3)),                  // has start date
                task(5),                                              // no date
            )
        )
        val any = setOf(StateFilter.Open, StateFilter.Done)
        assertEquals(listOf(1L), g.ids(TaskFilter(states = any, dates = setOf(DateFilter.Overdue))))
        assertEquals(listOf(3L), g.ids(TaskFilter(states = any, dates = setOf(DateFilter.DueToday))))
        assertEquals(listOf(4L), g.ids(TaskFilter(states = any, dates = setOf(DateFilter.HasStartDate))))
        assertEquals(listOf(5L), g.ids(TaskFilter(states = any, dates = setOf(DateFilter.NoDate))))
        assertEquals(
            listOf(1L, 3L),
            g.ids(TaskFilter(states = any, dates = setOf(DateFilter.Overdue, DateFilter.DueToday))),
        )
    }

    @Test
    fun `due in the next 7 days covers today through six days ahead`() {
        val g = graph(
            listOf(
                task(1, due = TODAY.minusDays(1)), // overdue: not included
                task(2, due = TODAY),
                task(3, due = TODAY.plusDays(6)),
                task(4, due = TODAY.plusDays(7)),  // a week out: not included
                task(5),
            )
        )
        assertEquals(listOf(2L, 3L), g.ids(TaskFilter(dates = setOf(DateFilter.DueWithin7Days))))
    }

    @Test
    fun `sort by due date puts undated last`() {
        val g = graph(
            listOf(task(1), task(2, due = TODAY.plusDays(5)), task(3, due = TODAY), task(4))
        )
        assertEquals(listOf(3L, 2L, 1L, 4L), g.ids(TaskFilter(sort = SortOrder.DueDate)))
    }

    @Test
    fun `sort by created puts newest first`() {
        val g = graph(listOf(task(1, createdSecond = 10), task(2, createdSecond = 30), task(3, createdSecond = 20)))
        assertEquals(listOf(2L, 3L, 1L), g.ids(TaskFilter(sort = SortOrder.Created)))
    }

    @Test
    fun `sort by group is alphabetical with ungrouped last`() {
        val g = graph(
            listOf(task(1), task(2, groupId = 8), task(3, groupId = 9), task(4, groupId = 8, due = TODAY)),
            groups = listOf(group(8, "Uni"), group(9, "App build")),
        )
        assertEquals(
            listOf(3L, 4L, 2L, 1L),
            g.ids(TaskFilter(grouping = Grouping.All, sort = SortOrder.Group)),
        )
    }

    @Test
    fun `a filter survives being saved as text`() {
        val f = TaskFilter(
            grouping = Grouping.Grouped,
            states = setOf(StateFilter.Done, StateFilter.Blocked),
            dates = setOf(DateFilter.Overdue, DateFilter.DueWithin7Days),
            sort = SortOrder.Group,
        )
        assertEquals(f, TaskFilter.decode(f.encode()))
        // Empty chip sets mean "anything" and must stay empty rather than revert to the defaults.
        val empty = TaskFilter(states = emptySet(), dates = emptySet())
        assertEquals(empty, TaskFilter.decode(empty.encode()))
    }

    @Test
    fun `unreadable saved text falls back to the defaults`() {
        assertEquals(TaskFilter(), TaskFilter.decode(null))
        assertEquals(TaskFilter(), TaskFilter.decode("rubbish"))
        assertEquals(
            TaskFilter(sort = SortOrder.Created),
            TaskFilter.decode("grouping=Sideways;states=Open,Waiting,Bogus;sort=Created"),
        )
    }

    @Test
    fun `only the sheet's choices count as customising`() {
        assertFalse(TaskFilter().isCustomised)
        assertFalse(TaskFilter(showHidden = true, groupId = 3).isCustomised)
        assertTrue(TaskFilter(sort = SortOrder.Created).isCustomised)
        assertTrue(TaskFilter(states = emptySet()).isCustomised)
    }
}
