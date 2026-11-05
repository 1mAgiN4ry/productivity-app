package com.example.productivity.data.tasks

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.productivity.data.AppDatabase
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskRepositoryTest {

    private val now = Instant.parse("2026-11-05T09:00:00Z")
    private val today = LocalDate.of(2026, 11, 5)
    private lateinit var db: AppDatabase
    private lateinit var repo: TaskRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        repo = TaskRepository(db, Clock.fixed(now, ZoneOffset.UTC))
    }

    @After
    fun tearDown() = db.close()

    private suspend fun snapshot() = repo.graph.first()

    @Test
    fun dismantlingAGroupKeepsItsTasksUngroupedAndDropsDependenciesOnIt() = runBlocking<Unit> {
        val group = repo.createGroup("App build")
        val member = repo.createTask("Set up Room database", groupId = group)
        val waiter = repo.createTask("Ship it")
        repo.addDependency(waiter, DependencyTarget.OnGroup(group))

        repo.dismantleGroup(group)

        val g = snapshot()
        assertNull(g.group(group))
        assertNull(g.task(member)!!.groupId)
        assertTrue(g.groupDependencies.isEmpty())
        assertFalse(g.statusOf(g.task(waiter)!!, today).isBlocked)
    }

    @Test
    fun deletingATaskRemovesDependenciesInBothDirections() = runBlocking<Unit> {
        val a = repo.createTask("A")
        val b = repo.createTask("B")
        val c = repo.createTask("C")
        repo.addDependency(a, DependencyTarget.OnTask(b))
        repo.addDependency(b, DependencyTarget.OnTask(c))

        repo.deleteTask(b)

        assertTrue(snapshot().taskDependencies.isEmpty())
    }

    @Test
    fun loopsAreRejectedAndNothingIsWritten() = runBlocking<Unit> {
        val group = repo.createGroup("G")
        val a = repo.createTask("A", groupId = group)
        val b = repo.createTask("B")
        repo.addDependency(b, DependencyTarget.OnTask(a))

        assertThrows(CircularDependencyException::class.java) {
            runBlocking { repo.addDependency(a, DependencyTarget.OnTask(b)) }
        }
        assertThrows(CircularDependencyException::class.java) {
            runBlocking { repo.addDependency(a, DependencyTarget.OnTask(a)) }
        }
        assertThrows(CircularDependencyException::class.java) {
            runBlocking { repo.addDependency(a, DependencyTarget.OnGroup(group)) }
        }
        val g = snapshot()
        assertEquals(listOf(TaskDependency(b, a)), g.taskDependencies)
        assertTrue(g.groupDependencies.isEmpty())
    }

    @Test
    fun movingATaskIntoAGroupItWaitsOnIsRejected() = runBlocking<Unit> {
        val group = repo.createGroup("G")
        repo.createTask("Member", groupId = group)
        val waiter = repo.createTask("Waiter")
        repo.addDependency(waiter, DependencyTarget.OnGroup(group))

        val task = repo.getTask(waiter)!!
        assertThrows(CircularDependencyException::class.java) {
            runBlocking { repo.updateTask(task.copy(groupId = group)) }
        }
        assertNull(repo.getTask(waiter)!!.groupId)
    }

    @Test
    fun stateChangesKeepCompletionAndWaitingFieldsConsistent() = runBlocking<Unit> {
        val id = repo.createTask("Return parcel")

        repo.setState(id, TaskState.Waiting, waitingNote = "label", checkBackDate = today.plusDays(2))
        repo.getTask(id)!!.let {
            assertEquals("label", it.waitingNote)
            assertEquals(today.plusDays(2), it.checkBackDate)
            assertNull(it.completedAt)
        }

        repo.setState(id, TaskState.Done)
        repo.getTask(id)!!.let {
            assertNull(it.waitingNote)
            assertNull(it.checkBackDate)
            assertEquals(now, it.completedAt)
        }

        repo.setState(id, TaskState.Open)
        assertNull(repo.getTask(id)!!.completedAt)
    }

    @Test
    fun datesTimesAndRemindersRoundTrip() = runBlocking<Unit> {
        val id = repo.createTask("Write data layer tests", startDate = today, dueDate = today.plusDays(8))
        val reminder = Reminder(daysBeforeDue = 1, time = LocalTime.of(9, 30))
        repo.updateTask(repo.getTask(id)!!.copy(reminder = reminder))

        val saved = repo.getTask(id)!!
        assertEquals(today, saved.startDate)
        assertEquals(today.plusDays(8), saved.dueDate)
        assertEquals(reminder, saved.reminder)
        assertEquals(now, saved.createdAt)
    }

    @Test
    fun titlesAreTrimmedAndMustNotBeBlank() = runBlocking<Unit> {
        val id = repo.createTask("  Renew bike lock  ")
        assertEquals("Renew bike lock", repo.getTask(id)!!.title)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repo.createTask("   ") }
        }
    }

    @Test
    fun observedListUpdatesWhenDataChanges() = runBlocking<Unit> {
        val tasks = repo.observeTasks(TaskFilter())
        assertTrue(tasks.first().isEmpty())

        val id = repo.createTask("Reply to landlord email", dueDate = today)
        val afterInsert = withTimeout(5_000) { tasks.first { it.isNotEmpty() } }
        assertEquals(listOf(id), afterInsert.map { it.task.id })

        repo.setState(id, TaskState.Done)
        withTimeout(5_000) { tasks.first { it.isEmpty() } }
    }

    @Test
    fun sampleDataLoadsAndMatchesTheMockup() = runBlocking<Unit> {
        SampleData.load(repo, today)

        val main = repo.observeTasks(TaskFilter()).first().map { it.task.title }
        assertEquals(
            listOf("Reply to landlord email", "Book dentist appointment", "Renew bike lock", "Return parcel"),
            main,
        )
        val hidden = repo.observeTasks(TaskFilter(showHidden = true)).first().filter { it.isHidden }
        assertEquals(setOf("Post the old desk", "Renew TV licence"), hidden.map { it.task.title }.toSet())

        val appBuild = repo.observeGroups().first().single { it.group.title == "App build" }
        assertEquals(5, appBuild.activeCount)
        assertEquals(2, appBuild.doneCount)
        assertNotNull(repo.observeTasks(TaskFilter(groupId = appBuild.group.id, showHidden = true)).first())
    }
}
