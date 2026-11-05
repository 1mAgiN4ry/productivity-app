package com.example.productivity.data.tasks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskGraphTest {

    private fun TaskGraph.isBlocked(id: Long) = statusOf(task(id)!!, TODAY).isBlocked
    private fun TaskGraph.isUpcoming(id: Long) = statusOf(task(id)!!, TODAY).isUpcoming

    // Blocking by tasks

    @Test
    fun `open dependency blocks`() {
        val g = graph(listOf(task(1), task(2)), taskDeps = listOf(1L to 2L))
        assertTrue(g.isBlocked(1))
        assertFalse(g.isBlocked(2))
    }

    @Test
    fun `waiting dependency blocks`() {
        val g = graph(listOf(task(1), task(2, TaskState.Waiting)), taskDeps = listOf(1L to 2L))
        assertTrue(g.isBlocked(1))
    }

    @Test
    fun `done dependency unblocks`() {
        val g = graph(listOf(task(1), task(2, TaskState.Done)), taskDeps = listOf(1L to 2L))
        assertFalse(g.isBlocked(1))
    }

    @Test
    fun `dropped dependency unblocks`() {
        val g = graph(listOf(task(1), task(2, TaskState.Dropped)), taskDeps = listOf(1L to 2L))
        assertFalse(g.isBlocked(1))
    }

    @Test
    fun `finished tasks are never reported as blocked`() {
        val g = graph(listOf(task(1, TaskState.Done), task(2)), taskDeps = listOf(1L to 2L))
        assertFalse(g.isBlocked(1))
    }

    @Test
    fun `blockers lists only the unresolved dependencies`() {
        val g = graph(
            listOf(task(1), task(2), task(3, TaskState.Done)),
            taskDeps = listOf(1L to 2L, 1L to 3L),
        )
        assertEquals(listOf(Blocker.Task(g.task(2)!!)), g.blockersOf(g.task(1)!!))
    }

    // Groups

    @Test
    fun `group with an active task is not complete and blocks dependents`() {
        val g = graph(
            listOf(task(1), task(10, groupId = 5), task(11, TaskState.Done, groupId = 5)),
            groups = listOf(group(5)),
            groupDeps = listOf(1L to 5L),
        )
        assertFalse(g.isGroupComplete(5))
        assertEquals(listOf(Blocker.Group(g.group(5)!!)), g.blockersOf(g.task(1)!!))
    }

    @Test
    fun `group whose tasks are all done or dropped is complete`() {
        val g = graph(
            listOf(task(1), task(10, TaskState.Done, groupId = 5), task(11, TaskState.Dropped, groupId = 5)),
            groups = listOf(group(5)),
            groupDeps = listOf(1L to 5L),
        )
        assertTrue(g.isGroupComplete(5))
        assertFalse(g.isBlocked(1))
    }

    @Test
    fun `empty group is not complete and blocks dependents`() {
        val g = graph(listOf(task(1)), groups = listOf(group(5)), groupDeps = listOf(1L to 5L))
        assertFalse(g.isGroupComplete(5))
        assertTrue(g.isBlocked(1))
    }

    @Test
    fun `group summary counts active and done tasks`() {
        val g = graph(
            listOf(
                task(1, groupId = 5),
                task(2, TaskState.Waiting, groupId = 5),
                task(3, TaskState.Done, groupId = 5),
                task(4, TaskState.Dropped, groupId = 5),
                task(5), // not in the group
            ),
            groups = listOf(group(5)),
        )
        assertEquals(GroupSummary(g.group(5)!!, activeCount = 2, doneCount = 1, isComplete = false), g.summaryOf(g.group(5)!!))
    }

    // Upcoming

    @Test
    fun `task starting after today is upcoming`() {
        val g = graph(listOf(task(1, start = TODAY.plusDays(1)), task(2, start = TODAY), task(3)))
        assertTrue(g.isUpcoming(1))
        assertFalse(g.isUpcoming(2))
        assertFalse(g.isUpcoming(3))
    }

    @Test
    fun `finished task with a future start is not upcoming`() {
        val g = graph(listOf(task(1, TaskState.Done, start = TODAY.plusDays(3))))
        assertFalse(g.isUpcoming(1))
    }

    // Loops

    @Test
    fun `a chain of dependencies is not a loop`() {
        val g = graph(listOf(task(1), task(2), task(3)), taskDeps = listOf(1L to 2L, 2L to 3L))
        assertFalse(g.hasCycle())
    }

    @Test
    fun `two tasks depending on each other is a loop`() {
        val g = graph(listOf(task(1), task(2)), taskDeps = listOf(1L to 2L))
        assertTrue(g.withDependency(2, DependencyTarget.OnTask(1)).hasCycle())
    }

    @Test
    fun `a longer loop is detected`() {
        val g = graph(listOf(task(1), task(2), task(3)), taskDeps = listOf(1L to 2L, 2L to 3L))
        assertTrue(g.withDependency(3, DependencyTarget.OnTask(1)).hasCycle())
    }

    @Test
    fun `depending on your own group is a loop`() {
        val g = graph(listOf(task(1, groupId = 5)), groups = listOf(group(5)))
        assertTrue(g.withDependency(1, DependencyTarget.OnGroup(5)).hasCycle())
    }

    @Test
    fun `a loop through another group member is detected`() {
        // 1 waits on group 5; 2 is in group 5; making 2 wait on 1 closes the loop.
        val g = graph(
            listOf(task(1), task(2, groupId = 5)),
            groups = listOf(group(5)),
            groupDeps = listOf(1L to 5L),
        )
        assertTrue(g.withDependency(2, DependencyTarget.OnTask(1)).hasCycle())
    }

    @Test
    fun `moving a task into a group it depends on is a loop`() {
        val g = graph(listOf(task(1), task(2, groupId = 5)), groups = listOf(group(5)), groupDeps = listOf(1L to 5L))
        assertFalse(g.hasCycle())
        assertTrue(g.withTask(task(1, groupId = 5)).hasCycle())
    }

    @Test
    fun `hypothetical changes leave the original graph untouched`() {
        val g = graph(listOf(task(1), task(2)), taskDeps = listOf(1L to 2L))
        g.withDependency(2, DependencyTarget.OnTask(1))
        assertFalse(g.hasCycle())
        assertEquals(listOf(DependencyTarget.OnTask(2)), g.dependenciesOf(1))
    }
}
