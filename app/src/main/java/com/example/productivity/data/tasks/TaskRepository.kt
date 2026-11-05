package com.example.productivity.data.tasks

import androidx.room.withTransaction
import com.example.productivity.data.AppDatabase
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class CircularDependencyException(message: String) : IllegalArgumentException(message)

/** Everything removed with a task, so the deletion can be undone. */
data class DeletedTask(
    val task: TaskEntity,
    val taskDependencies: List<TaskDependency>,
    val groupDependencies: List<GroupDependency>,
)

/**
 * The single entry point for task data. Screens observe derived results and call these
 * functions to make changes; validation (non-blank titles, no dependency loops) lives here.
 */
class TaskRepository(
    private val db: AppDatabase,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val dao = db.taskDao()

    private fun now(): Instant = clock.instant()

    /** Re-emits whenever any task, group or dependency changes. */
    val graph: Flow<TaskGraph> = combine(
        dao.observeTasks(),
        dao.observeGroups(),
        dao.observeTaskDependencies(),
        dao.observeGroupDependencies(),
        ::TaskGraph,
    )

    /** The current date, re-emitted just after each midnight so "Today" and "Overdue" roll over. */
    val today: Flow<LocalDate> = flow {
        while (true) {
            val now = ZonedDateTime.now(clock)
            emit(now.toLocalDate())
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay(Duration.between(now, nextMidnight).toMillis() + 1_000)
        }
    }.distinctUntilChanged()

    fun observeTasks(filter: TaskFilter): Flow<List<TaskStatus>> =
        combine(graph, today) { graph, today -> graph.query(filter, today) }

    fun observeTask(id: Long): Flow<TaskStatus?> =
        combine(graph, today) { graph, today -> graph.task(id)?.let { graph.statusOf(it, today) } }

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
     * Applies [change] to the current stored version of a task, so concurrent edits to different
     * fields don't overwrite each other. Rejects a move into a group if that would create a
     * dependency loop, and keeps [TaskEntity.completedAt] and the waiting fields consistent.
     */
    suspend fun editTask(id: Long, change: (TaskEntity) -> TaskEntity) = db.withTransaction {
        val existing = requireNotNull(dao.getTask(id)) { "No task $id" }
        val edited = change(existing)
        val updated = normalise(
            existing,
            edited.copy(id = existing.id, title = cleanTitle(edited.title), createdAt = existing.createdAt),
        )
        if (updated.groupId != existing.groupId && loadGraph().withTask(updated).hasCycle()) {
            throw CircularDependencyException("Moving it into that group would make it wait on itself")
        }
        dao.updateTask(updated)
    }

    suspend fun updateTask(task: TaskEntity) = editTask(task.id) { task }

    suspend fun setState(
        taskId: Long,
        state: TaskState,
        waitingNote: String? = null,
        checkBackDate: LocalDate? = null,
    ) = editTask(taskId) { it.copy(state = state, waitingNote = waitingNote, checkBackDate = checkBackDate) }

    /** Deletes a task and its dependency links, returning what's needed to [restoreTask] it. */
    suspend fun deleteTask(id: Long): DeletedTask? = db.withTransaction {
        val task = dao.getTask(id) ?: return@withTransaction null
        val deleted = DeletedTask(
            task = task,
            taskDependencies = dao.getTaskDependencies().filter { it.taskId == id || it.dependsOnTaskId == id },
            groupDependencies = dao.getGroupDependencies().filter { it.taskId == id },
        )
        dao.deleteTask(id)
        deleted
    }

    /**
     * Puts a deleted task back with its original id. Links to things deleted since, or that
     * would now form a loop, are skipped; if its group is gone it comes back ungrouped.
     */
    suspend fun restoreTask(deleted: DeletedTask) = db.withTransaction {
        val groupIds = dao.getGroups().mapTo(HashSet()) { it.id }
        val task = deleted.task.let { if (it.groupId != null && it.groupId !in groupIds) it.copy(groupId = null) else it }
        dao.insertTask(task)
        var graph = loadGraph()
        for (dep in deleted.taskDependencies) {
            if (graph.task(dep.taskId) == null || graph.task(dep.dependsOnTaskId) == null) continue
            val next = graph.withDependency(dep.taskId, DependencyTarget.OnTask(dep.dependsOnTaskId))
            if (next.hasCycle()) continue
            dao.insertTaskDependency(dep)
            graph = next
        }
        for (dep in deleted.groupDependencies) {
            if (dep.dependsOnGroupId !in groupIds) continue
            val next = graph.withDependency(dep.taskId, DependencyTarget.OnGroup(dep.dependsOnGroupId))
            if (next.hasCycle()) continue
            dao.insertGroupDependency(dep)
            graph = next
        }
    }

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
