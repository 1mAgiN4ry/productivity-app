package com.example.productivity.data.tasks

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * The state a person sets on a task. "Blocked" and "upcoming" are not here: they are
 * derived from dependencies and the start date (see [TaskGraph]).
 */
enum class TaskState {
    Open, Waiting, Done, Dropped;

    /** Still needs doing: counts against a group's completion and blocks dependents. */
    val isActive: Boolean get() = this == Open || this == Waiting
}

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Instant,
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        // Dismantling a group keeps its tasks, ungrouped.
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("groupId")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val state: TaskState = TaskState.Open,
    /** What the task is waiting on; only meaningful while [state] is [TaskState.Waiting]. */
    val waitingNote: String? = null,
    val checkBackDate: LocalDate? = null,
    val groupId: Long? = null,
    /** Before this date the task is "upcoming" and hidden by default. */
    val startDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    @Embedded(prefix = "reminder_") val reminder: Reminder? = null,
    val createdAt: Instant,
    /** When the task last became done or dropped. */
    val completedAt: Instant? = null,
)

/** A reminder relative to the due date, e.g. "due day · 09:00" is `Reminder(0, 09:00)`. */
data class Reminder(
    val daysBeforeDue: Int,
    val time: LocalTime,
)

/** [taskId] can't proceed until [dependsOnTaskId] is done or dropped. */
@Entity(
    tableName = "task_depends_on_task",
    primaryKeys = ["taskId", "dependsOnTaskId"],
    foreignKeys = [
        ForeignKey(TaskEntity::class, ["id"], ["taskId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(TaskEntity::class, ["id"], ["dependsOnTaskId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("dependsOnTaskId")],
)
data class TaskDependency(
    val taskId: Long,
    val dependsOnTaskId: Long,
)

/** [taskId] can't proceed until group [dependsOnGroupId] is complete. */
@Entity(
    tableName = "task_depends_on_group",
    primaryKeys = ["taskId", "dependsOnGroupId"],
    foreignKeys = [
        ForeignKey(TaskEntity::class, ["id"], ["taskId"], onDelete = ForeignKey.CASCADE),
        // A dismantled group no longer exists to wait on.
        ForeignKey(GroupEntity::class, ["id"], ["dependsOnGroupId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("dependsOnGroupId")],
)
data class GroupDependency(
    val taskId: Long,
    val dependsOnGroupId: Long,
)
