package com.example.productivity.ui.tasks

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.productivity.data.AppDatabase
import com.example.productivity.data.tasks.DependencyTarget
import com.example.productivity.data.tasks.TaskRepository
import com.example.productivity.data.tasks.TaskState
import com.example.productivity.ui.theme.GruvboxTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class TaskScreensTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: TaskRepository
    private val undo = TaskUndo()
    private val writes = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // The outer rule: the database is closed only after the screen under test is gone, so a
    // screen's last reads can't hit a closed database.
    @get:Rule(order = 0)
    val database = object : ExternalResource() {
        override fun before() {
            db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
            repo = TaskRepository(db)
        }

        override fun after() {
            writes.cancel()
            db.close()
        }
    }

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private fun showAllTasks(onOpenTask: (Long) -> Unit = {}, onOpenGroup: (Long) -> Unit = {}) {
        val vm = TaskListViewModel(repo, undo, writes, groupId = null)
        compose.setContent { GruvboxTheme { AllTasksScreen(vm, onOpenTask, onOpenGroup) } }
    }

    private fun showDetail(taskId: Long) {
        val vm = TaskDetailViewModel(repo, undo, writes, taskId)
        compose.setContent { GruvboxTheme { TaskDetailScreen(vm, onBack = {}, onOpenTask = {}, onOpenGroup = {}) } }
    }

    @Test
    fun creatingATaskShowsItInTheList() {
        showAllTasks()
        compose.onNodeWithText("New").performClick()
        compose.onNode(hasText("What needs doing?")).performTextInput("Water the plants")
        compose.onNodeWithText("Create task").performClick()

        compose.waitUntilAtLeastOneExists(hasText("Water the plants"), timeoutMillis = 5_000)
    }

    @Test
    fun aNewTaskCanBePutInAGroupStraightAway() {
        val groupId = runBlocking { repo.createGroup("Uni") }
        showAllTasks()
        compose.waitUntilAtLeastOneExists(hasText("Uni"), timeoutMillis = 5_000)

        compose.onNodeWithText("New").performClick()
        compose.onNode(hasText("What needs doing?")).performTextInput("Submit coursework")
        compose.onNodeWithText("No group").performClick()
        compose.onAllNodesWithText("Uni").filterToOne(hasAnyAncestor(isPopup())).performClick()
        compose.onNodeWithText("Create task").performClick()

        compose.waitUntil(5_000) {
            runBlocking { repo.graph.first().tasks.singleOrNull()?.groupId } == groupId
        }
    }

    @Test
    fun anExistingTaskCanBeAddedToAGroup() {
        val (groupId, taskId) = runBlocking { repo.createGroup("Home") to repo.createTask("Fix the bathroom tap") }
        val vm = TaskListViewModel(repo, undo, writes, groupId)
        compose.setContent { GruvboxTheme { GroupScreen(vm, onBack = {}, onOpenTask = {}, onOpenGroup = {}) } }

        compose.waitUntilAtLeastOneExists(hasText("Add task"), timeoutMillis = 5_000)
        compose.onNodeWithText("Add task").performClick()
        compose.onNodeWithText("Existing").performClick()
        compose.onNodeWithText("Fix the bathroom tap").performClick()
        compose.onNodeWithText("Add 1 task").performClick()

        compose.waitUntil(5_000) { runBlocking { repo.getTask(taskId)?.groupId } == groupId }
    }

    @Test
    fun tappingAGroupOpensIt() {
        val groupId = runBlocking { repo.createGroup("App build") }
        var opened: Long? = null
        showAllTasks(onOpenGroup = { opened = it })

        compose.waitUntilAtLeastOneExists(hasText("App build"), timeoutMillis = 5_000)
        compose.onNodeWithText("App build").performClick()
        assertEquals(groupId, opened)
    }

    @Test
    fun theEyeToggleRevealsBlockedTasks() {
        runBlocking {
            val parcel = repo.createTask("Return parcel")
            val desk = repo.createTask("Post the old desk")
            repo.addDependency(desk, DependencyTarget.OnTask(parcel))
        }
        showAllTasks()

        compose.waitUntilAtLeastOneExists(hasText("1 blocked"), timeoutMillis = 5_000)
        compose.onNodeWithText("Post the old desk").assertDoesNotExist()
        compose.onNodeWithContentDescription("Show blocked and upcoming tasks").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Blocked by Return parcel"), timeoutMillis = 5_000)
    }

    @Test
    fun editingTheTitleSavesIt() {
        val id = runBlocking { repo.createTask("Renew bike lock") }
        showDetail(id)

        compose.waitUntilAtLeastOneExists(hasContentDescription("Task title"), timeoutMillis = 5_000)
        compose.onNodeWithContentDescription("Task title").performTextReplacement("Renew bike lock and lights")
        compose.waitUntil(5_000) { runBlocking { repo.getTask(id)?.title } == "Renew bike lock and lights" }
    }

    @Test
    fun markingDoneSavesTheState() {
        val id = runBlocking { repo.createTask("Reply to landlord email") }
        showDetail(id)

        compose.waitUntilAtLeastOneExists(hasText("Done"), timeoutMillis = 5_000)
        compose.onNodeWithText("Done").performClick()
        compose.waitUntil(5_000) { runBlocking { repo.getTask(id)?.state } == TaskState.Done }
    }

}
