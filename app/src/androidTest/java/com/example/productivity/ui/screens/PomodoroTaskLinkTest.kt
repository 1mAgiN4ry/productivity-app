package com.example.productivity.ui.screens

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.productivity.data.AppDatabase
import com.example.productivity.data.tasks.TaskRepository
import com.example.productivity.ui.tasks.TaskChoice
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The timer's task field and its link to the Task List. Uses its own in-memory database. */
@RunWith(AndroidJUnit4::class)
class PomodoroTaskLinkTest {

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: AppDatabase
    private lateinit var repo: TaskRepository
    private val store = ViewModelStore()
    private lateinit var vm: PomodoroViewModel

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
        repo = TaskRepository(db)
        vm = onMain {
            val factory = viewModelFactory { initializer { PomodoroViewModel(app, repo) } }
            ViewModelProvider(store, factory)[PomodoroViewModel::class.java]
        }
    }

    @After
    fun tearDown() {
        onMain { store.clear() } // Stops the timer and the title-following before the database closes.
        db.close()
    }

    @Test
    fun startingOnATaskLinksItAndStartsAFocusSession() {
        val id = runBlocking { repo.createTask("Write the essay") }
        onMain {
            vm.selectMode(PomodoroMode.ShortBreak)
            vm.startOn(id)
        }
        assertEquals(PomodoroMode.Focus, vm.state.mode)
        assertEquals(TimerStatus.Running, vm.state.status)
        waitFor { vm.task == PomodoroTask("Write the essay", id) }

        // Renaming the task renames the field.
        runBlocking { repo.editTask(id) { it.copy(title = "Write the essay intro") } }
        waitFor { vm.task.title == "Write the essay intro" }

        // Typing unlinks it.
        onMain { vm.typeTask("Something else") }
        assertEquals(PomodoroTask("Something else"), vm.task)
    }

    @Test
    fun aSessionInProgressIsLeftAlone() {
        val id = runBlocking { repo.createTask("Write the essay") }
        onMain {
            vm.selectMode(PomodoroMode.LongBreak)
            vm.toggle() // start
            vm.toggle() // pause
        }
        val paused = vm.state

        onMain { vm.startOn(id) }
        assertEquals(paused, vm.state)
        waitFor { vm.task == PomodoroTask("Write the essay", id) }
    }

    @Test
    fun deletingTheLinkedTaskKeepsItsWords() {
        val id = runBlocking { repo.createTask("Write the essay") }
        onMain { vm.pickTask(TaskChoice(id, "Write the essay", null)) }
        assertEquals(TimerStatus.Ready, vm.state.status) // Picking alone doesn't start the timer.

        runBlocking { repo.deleteTask(id) }
        waitFor { vm.task.taskId == null }
        assertEquals("Write the essay", vm.task.title)
        assertNull(vm.task.taskId)
    }

    private fun <T> onMain(block: () -> T): T {
        var result: Result<T>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result = runCatching(block) }
        return result!!.getOrThrow()
    }

    private fun waitFor(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!onMain(condition)) {
            check(System.currentTimeMillis() < deadline) { "Timed out" }
            Thread.sleep(20)
        }
    }
}
