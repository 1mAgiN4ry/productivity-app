package com.example.productivity.data.tasks

import java.time.Instant
import java.time.LocalDate

val TODAY: LocalDate = LocalDate.of(2026, 11, 5)
private val EPOCH: Instant = Instant.parse("2026-11-01T10:00:00Z")

fun task(
    id: Long,
    state: TaskState = TaskState.Open,
    groupId: Long? = null,
    start: LocalDate? = null,
    due: LocalDate? = null,
    createdSecond: Long = id,
) = TaskEntity(
    id = id,
    title = "Task $id",
    state = state,
    groupId = groupId,
    startDate = start,
    dueDate = due,
    createdAt = EPOCH.plusSeconds(createdSecond),
)

fun group(id: Long, title: String = "Group $id") =
    GroupEntity(id = id, title = title, createdAt = EPOCH.plusSeconds(id))

fun graph(
    tasks: List<TaskEntity>,
    groups: List<GroupEntity> = emptyList(),
    taskDeps: List<Pair<Long, Long>> = emptyList(),
    groupDeps: List<Pair<Long, Long>> = emptyList(),
) = TaskGraph(
    tasks = tasks,
    groups = groups,
    taskDependencies = taskDeps.map { (task, on) -> TaskDependency(task, on) },
    groupDependencies = groupDeps.map { (task, on) -> GroupDependency(task, on) },
)
