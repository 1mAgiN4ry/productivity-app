package com.example.productivity.data.tasks

import java.time.LocalDate

enum class Grouping { All, Ungrouped, Grouped }

/** The filter sheet's "State" chips. [Blocked] is derived rather than a stored state. */
enum class StateFilter { Open, Waiting, Blocked, Done, Dropped }

enum class DateFilter {
    Overdue,
    DueToday,

    /** Shown as "Due in the next 7 days": today plus the following six days. */
    DueWithin7Days,
    HasStartDate,
    NoDate,
}

enum class SortOrder {
    /** Soonest due first; undated last. */
    DueDate,

    /** Newest first. */
    Created,

    /** Alphabetical by group, ungrouped last; due date within a group. */
    Group;

    fun comparator(groupsById: Map<Long, GroupEntity>): Comparator<TaskStatus> {
        val byDue = compareBy<TaskStatus, LocalDate?>(nullsLast()) { it.task.dueDate }
            .thenBy { it.task.createdAt }
            .thenBy { it.task.id }
        return when (this) {
            DueDate -> byDue
            Created -> compareByDescending<TaskStatus> { it.task.createdAt }.thenByDescending { it.task.id }
            Group -> compareBy<TaskStatus, String?>(nullsLast(String.CASE_INSENSITIVE_ORDER)) {
                it.task.groupId?.let { id -> groupsById[id]?.title }
            }.thenBy { it.task.groupId ?: Long.MAX_VALUE }.then(byDue)
        }
    }
}

/**
 * What the task list shows. Within a set of chips any one may match (OR); across sections all
 * must match (AND). An empty chip set doesn't restrict anything.
 */
data class TaskFilter(
    /** Defaults to [Grouping.Ungrouped]: the main list shows loose tasks plus a row per group. */
    val grouping: Grouping = Grouping.Ungrouped,
    val states: Set<StateFilter> = setOf(StateFilter.Open, StateFilter.Waiting),
    val dates: Set<DateFilter> = emptySet(),
    val sort: SortOrder = SortOrder.DueDate,
    /** The eye toggle: include blocked and upcoming tasks. */
    val showHidden: Boolean = false,
    /** Restrict to one group (the group view). Overrides [grouping]. */
    val groupId: Long? = null,
) {
    /** True if the sheet's choices differ from the defaults (ignores the eye and [groupId]). */
    val isCustomised: Boolean
        get() = copy(showHidden = false, groupId = null) != TaskFilter()

    /**
     * The sheet's choices as text, for saving, e.g. `grouping=Ungrouped;states=Open,Waiting;dates=;sort=DueDate`.
     * The eye and [groupId] aren't included.
     */
    fun encode(): String = listOf(
        "grouping=${grouping.name}",
        "states=${states.sorted().joinToString(",") { it.name }}",
        "dates=${dates.sorted().joinToString(",") { it.name }}",
        "sort=${sort.name}",
    ).joinToString(";")

    companion object {
        /** Reads [encode]'s text. Unknown or missing parts fall back to the defaults. */
        fun decode(text: String?): TaskFilter {
            val default = TaskFilter()
            if (text.isNullOrBlank()) return default
            val parts = text.split(";").mapNotNull {
                val i = it.indexOf('=')
                if (i < 0) null else it.substring(0, i) to it.substring(i + 1)
            }.toMap()

            fun names(key: String) = parts[key]?.split(",")?.filter { it.isNotEmpty() }
            return TaskFilter(
                grouping = parts["grouping"]?.let { v -> Grouping.entries.find { it.name == v } } ?: default.grouping,
                states = names("states")?.mapNotNull { v -> StateFilter.entries.find { it.name == v } }?.toSet()
                    ?: default.states,
                dates = names("dates")?.mapNotNull { v -> DateFilter.entries.find { it.name == v } }?.toSet()
                    ?: default.dates,
                sort = parts["sort"]?.let { v -> SortOrder.entries.find { it.name == v } } ?: default.sort,
            )
        }
    }

    fun matches(status: TaskStatus, today: LocalDate): Boolean {
        val task = status.task

        val inScope = when {
            groupId != null -> task.groupId == groupId
            grouping == Grouping.Ungrouped -> task.groupId == null
            grouping == Grouping.Grouped -> task.groupId != null
            else -> true
        }
        if (!inScope) return false

        // Picking the "Blocked" chip is itself a request to see blocked tasks.
        val explicitlyBlocked = StateFilter.Blocked in states && status.isBlocked
        if (status.isHidden && !showHidden && !explicitlyBlocked) return false

        val stateMatches = states.isEmpty() || states.any {
            when (it) {
                StateFilter.Open -> task.state == TaskState.Open
                StateFilter.Waiting -> task.state == TaskState.Waiting
                StateFilter.Blocked -> status.isBlocked
                StateFilter.Done -> task.state == TaskState.Done
                StateFilter.Dropped -> task.state == TaskState.Dropped
            }
        }
        if (!stateMatches) return false

        return dates.isEmpty() || dates.any {
            when (it) {
                DateFilter.Overdue ->
                    task.state.isActive && task.dueDate != null && task.dueDate.isBefore(today)
                DateFilter.DueToday -> task.dueDate == today
                DateFilter.DueWithin7Days ->
                    task.dueDate != null && !task.dueDate.isBefore(today) && task.dueDate.isBefore(today.plusDays(7))
                DateFilter.HasStartDate -> task.startDate != null
                DateFilter.NoDate -> task.startDate == null && task.dueDate == null
            }
        }
    }
}
