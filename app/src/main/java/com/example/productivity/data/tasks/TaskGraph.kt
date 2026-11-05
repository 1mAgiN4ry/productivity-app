package com.example.productivity.data.tasks

import java.time.LocalDate

/** Something a task can depend on. */
sealed interface DependencyTarget {
    data class OnTask(val taskId: Long) : DependencyTarget
    data class OnGroup(val groupId: Long) : DependencyTarget
}

/** An unresolved dependency that is currently blocking a task. */
sealed interface Blocker {
    data class Task(val task: TaskEntity) : Blocker
    data class Group(val group: GroupEntity) : Blocker
}

/** A task together with everything derived about it. */
data class TaskStatus(
    val task: TaskEntity,
    val blockers: List<Blocker>,
    /** Active, with a start date after today. */
    val isUpcoming: Boolean,
) {
    val isBlocked: Boolean get() = blockers.isNotEmpty()

    /** Hidden from the list unless "show blocked & upcoming" is on. */
    val isHidden: Boolean get() = isBlocked || isUpcoming
}

data class GroupSummary(
    val group: GroupEntity,
    /** Open or waiting (including blocked/upcoming ones). */
    val activeCount: Int,
    val doneCount: Int,
    val isComplete: Boolean,
)

/**
 * An immutable snapshot of all tasks, groups and dependencies, with the rules that derive
 * status from them. Kept free of Android and Room so it can be unit tested directly.
 *
 * Rules:
 * - A dependency is resolved when the task is done or dropped, or the group is complete.
 * - A group is complete when it has at least one task and none of them are active.
 *   An empty group is not complete.
 * - Only active tasks are reported as blocked or upcoming.
 * - Dependencies may not form a loop, including loops through group membership
 *   (e.g. a task depending on its own group).
 */
class TaskGraph(
    val tasks: List<TaskEntity>,
    val groups: List<GroupEntity>,
    val taskDependencies: List<TaskDependency>,
    val groupDependencies: List<GroupDependency>,
) {
    private val tasksById = tasks.associateBy { it.id }
    private val groupsById = groups.associateBy { it.id }
    private val tasksByGroup = tasks.filter { it.groupId != null }.groupBy { it.groupId!! }
    private val taskDepsByTask = taskDependencies.groupBy({ it.taskId }, { it.dependsOnTaskId })
    private val groupDepsByTask = groupDependencies.groupBy({ it.taskId }, { it.dependsOnGroupId })

    fun task(id: Long): TaskEntity? = tasksById[id]

    fun group(id: Long): GroupEntity? = groupsById[id]

    fun tasksIn(groupId: Long): List<TaskEntity> = tasksByGroup[groupId].orEmpty()

    fun isGroupComplete(groupId: Long): Boolean {
        val members = tasksIn(groupId)
        return members.isNotEmpty() && members.none { it.state.isActive }
    }

    fun summaryOf(group: GroupEntity): GroupSummary {
        val members = tasksIn(group.id)
        return GroupSummary(
            group = group,
            activeCount = members.count { it.state.isActive },
            doneCount = members.count { it.state == TaskState.Done },
            isComplete = isGroupComplete(group.id),
        )
    }

    fun groupSummaries(): List<GroupSummary> = groups.sortedBy { it.createdAt }.map(::summaryOf)

    fun dependenciesOf(taskId: Long): List<DependencyTarget> =
        taskDepsByTask[taskId].orEmpty().map { DependencyTarget.OnTask(it) } +
            groupDepsByTask[taskId].orEmpty().map { DependencyTarget.OnGroup(it) }

    fun blockersOf(task: TaskEntity): List<Blocker> {
        if (!task.state.isActive) return emptyList()
        val onTasks = taskDepsByTask[task.id].orEmpty()
            .mapNotNull { tasksById[it] }
            .filter { it.state.isActive }
            .map { Blocker.Task(it) }
        val onGroups = groupDepsByTask[task.id].orEmpty()
            .filter { !isGroupComplete(it) }
            .mapNotNull { groupsById[it] }
            .map { Blocker.Group(it) }
        return onTasks + onGroups
    }

    fun statusOf(task: TaskEntity, today: LocalDate): TaskStatus = TaskStatus(
        task = task,
        blockers = blockersOf(task),
        isUpcoming = task.state.isActive && task.startDate?.isAfter(today) == true,
    )

    fun query(filter: TaskFilter, today: LocalDate): List<TaskStatus> =
        tasks.asSequence()
            .map { statusOf(it, today) }
            .filter { filter.matches(it, today) }
            .sortedWith(filter.sort.comparator(groupsById))
            .toList()

    // Hypothetical changes, used to validate an edit before writing it.

    fun withDependency(taskId: Long, target: DependencyTarget): TaskGraph = when (target) {
        is DependencyTarget.OnTask -> TaskGraph(
            tasks, groups, taskDependencies + TaskDependency(taskId, target.taskId), groupDependencies,
        )
        is DependencyTarget.OnGroup -> TaskGraph(
            tasks, groups, taskDependencies, groupDependencies + GroupDependency(taskId, target.groupId),
        )
    }

    fun withTask(task: TaskEntity): TaskGraph =
        TaskGraph(tasks.filter { it.id != task.id } + task, groups, taskDependencies, groupDependencies)

    /**
     * True if any task (transitively) depends on itself. Edges run from a task to each task it
     * depends on, and from a task to every member of each group it depends on.
     */
    fun hasCycle(): Boolean {
        val visiting = HashSet<Long>()
        val done = HashSet<Long>()

        fun successors(taskId: Long): List<Long> =
            taskDepsByTask[taskId].orEmpty() +
                groupDepsByTask[taskId].orEmpty().flatMap { groupId -> tasksIn(groupId).map { it.id } }

        fun visit(taskId: Long): Boolean {
            if (taskId in done) return false
            if (!visiting.add(taskId)) return true // back on the current path: a loop
            val looped = successors(taskId).any(::visit)
            visiting.remove(taskId)
            done.add(taskId)
            return looped
        }

        return tasks.any { visit(it.id) }
    }
}
