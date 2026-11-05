package com.example.productivity.data.tasks

import androidx.room.withTransaction
import com.example.productivity.data.AppDatabase
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class CircularDependencyException(message: String) : IllegalArgumentException(message)

/**
 * The single entry point for task data. Screens observe derived results and call these
 * functions to make changes; validation (non-blank titles, no dependency loops) lives here.
 */
class TaskRepository(
    private val db: AppDatabase,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val dao = db.taskDao()

    private fun today(): LocalDate = LocalDate.now(clock)
    private fun now(): Instant = clock.instant()

    /** Re-emits whenever any task, group or dependency changes. */
    val graph: Flow<TaskGraph> = combine(
        dao.observeTasks(),
        dao.observeGroups(),
        dao.observeTaskDependencies(),
        dao.observeGroupDependencies(),
        ::TaskGraph,
    )

    fun observeTasks(filter: TaskFilter): Flow<List<TaskStatus>> =
        graph.map { it.query(filter, today()) }

    fun observeTask(id: Long): Flow<TaskStatus?> =
        graph.map { graph -> graph.task(id)?.let { graph.statusOf(it, today()) } }

    fun observeGroups(): Flow<List<GroupSummary>> = graph.map { it.groupSummaries() }

    suspend fun getTask(id: Long): TaskEntity? = dao.getTask(id)

    // Tasks

    suspend fun createTask(
        title: String,
        groupId: Long? = null,
        startDate: LocalDate? = null,
        dueDate: LocalDate? = null,
    ): Long = dao.insertTask(
        // A new task has no dependencies of its own, so it can't close a loop.
        TaskEntity(
            title = cleanTitle(title),
            groupId = groupId,
            startDate = startDate,
            dueDate = dueDate,
            createdAt = now(),
        )
    )

    /**
     * Saves edits to a task. Rejects a move into a group if that would create a dependency
     * loop, and keeps [TaskEntity.completedAt] and the waiting fields consistent with the state.
     */
    suspend fun updateTask(task: TaskEntity) = db.withTransaction {
        val existing = requireNotNull(dao.getTask(task.id)) { "No task ${task.id}" }
        val updated = normalise(existing, task.copy(title = cleanTitle(task.title)))
        if (updated.groupId != existing.groupId && loadGraph().withTask(updated).hasCycle()) {
            throw CircularDependencyException("Moving this task into that group would make it depend on itself")
        }
        dao.updateTask(updated)
    }

    suspend fun setState(
        taskId: Long,
        state: TaskState,
        waitingNote: String? = null,
        checkBackDate: LocalDate? = null,
    ) = db.withTransaction {
        val existing = requireNotNull(dao.getTask(taskId)) { "No task $taskId" }
        dao.updateTask(
            normalise(existing, existing.copy(state = state, waitingNote = waitingNote, checkBackDate = checkBackDate))
        )
    }

    suspend fun deleteTask(id: Long) = dao.deleteTask(id)

    // Groups

    suspend fun createGroup(title: String): Long =
        dao.insertGroup(GroupEntity(title = cleanTitle(title), createdAt = now()))

    suspend fun renameGroup(id: Long, title: String) = dao.renameGroup(id, cleanTitle(title))

    /** Deletes the group but keeps its tasks, ungrouped. Dependencies on the group go with it. */
    suspend fun dismantleGroup(id: Long) = dao.deleteGroup(id)

    // Dependencies

    /** @throws CircularDependencyException if [target] already (transitively) depends on [taskId]. */
    suspend fun addDependency(taskId: Long, target: DependencyTarget) = db.withTransaction {
        if (target == DependencyTarget.OnTask(taskId)) {
            throw CircularDependencyException("A task can't depend on itself")
        }
        val graph = loadGraph()
        requireNotNull(graph.task(taskId)) { "No task $taskId" }
        when (target) {
            is DependencyTarget.OnTask -> requireNotNull(graph.task(target.taskId)) { "No task ${target.taskId}" }
            is DependencyTarget.OnGroup -> requireNotNull(graph.group(target.groupId)) { "No group ${target.groupId}" }
        }
        if (graph.withDependency(taskId, target).hasCycle()) {
            throw CircularDependencyException("That would create a loop of tasks waiting on each other")
        }
        when (target) {
            is DependencyTarget.OnTask -> dao.insertTaskDependency(TaskDependency(taskId, target.taskId))
            is DependencyTarget.OnGroup -> dao.insertGroupDependency(GroupDependency(taskId, target.groupId))
        }
    }

    suspend fun removeDependency(taskId: Long, target: DependencyTarget) = when (target) {
        is DependencyTarget.OnTask -> dao.deleteTaskDependency(TaskDependency(taskId, target.taskId))
        is DependencyTarget.OnGroup -> dao.deleteGroupDependency(GroupDependency(taskId, target.groupId))
    }

    // Helpers

    private suspend fun loadGraph() = TaskGraph(
        dao.getTasks(), dao.getGroups(), dao.getTaskDependencies(), dao.getGroupDependencies(),
    )

    private fun cleanTitle(title: String): String =
        title.trim().also { require(it.isNotEmpty()) { "Title can't be blank" } }

    private fun normalise(old: TaskEntity, new: TaskEntity): TaskEntity {
        val completedAt = when {
            new.state.isActive -> null
            old.state.isActive -> now() // just finished
            else -> old.completedAt     // already finished (e.g. done -> dropped)
        }
        return if (new.state == TaskState.Waiting) {
            new.copy(completedAt = completedAt)
        } else {
            new.copy(completedAt = completedAt, waitingNote = null, checkBackDate = null)
        }
    }
}
