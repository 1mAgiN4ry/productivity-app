package com.example.productivity.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.productivity.data.tasks.CircularDependencyException
import com.example.productivity.data.tasks.DeletedTask
import com.example.productivity.data.tasks.GroupEntity
import com.example.productivity.data.tasks.GroupSummary
import com.example.productivity.data.tasks.Grouping
import com.example.productivity.data.tasks.InvalidDatesException
import com.example.productivity.data.tasks.TaskEntity
import com.example.productivity.data.tasks.TaskFilter
import com.example.productivity.data.tasks.TaskFilterStore
import com.example.productivity.data.tasks.TaskRepository
import com.example.productivity.data.tasks.TaskState
import com.example.productivity.data.tasks.TaskStatus
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskListUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    /** Tasks to show, in order (hidden ones included only when [showHidden]). */
    val tasks: List<TaskStatus> = emptyList(),
    /** All view only: a row per group, after the tasks. */
    val groups: List<GroupSummary> = emptyList(),
    /** Group view only. Null once the group has been dismantled. */
    val group: GroupSummary? = null,
    val showHidden: Boolean = false,
    val hiddenBlocked: Int = 0,
    val hiddenUpcoming: Int = 0,
    /** Every group, for choosing one when creating a task. */
    val allGroups: List<GroupEntity> = emptyList(),
    /** Group view only: open tasks elsewhere that could be added to this group. */
    val candidates: List<GroupCandidate> = emptyList(),
    /** All view only: the filter sheet's choices. */
    val filter: TaskFilter = TaskFilter(),
    /** Group names by id, for labelling tasks in the flat (All / Grouped) lists. Empty otherwise. */
    val groupLabels: Map<Long, String> = emptyMap(),
)

/** How many tasks, and group rows, a filter would list. */
data class FilterPreview(val tasks: Int, val groups: Int)

data class GroupCandidate(
    val taskId: Long,
    val title: String,
    /** Where it is now, e.g. "No group" or "In Uni · waiting". */
    val detail: String,
)

/** Backs both the All view ([groupId] null: loose tasks plus group rows) and a Group view. */
class TaskListViewModel(
    private val repo: TaskRepository,
    private val undo: TaskUndo,
    private val writes: CoroutineScope,
    private val groupId: Long?,
    /** The All view's saved filter. The group view always shows its open and waiting tasks. */
    private val filters: TaskFilterStore = TaskFilterStore(null),
) : ViewModel() {

    private val showHidden = MutableStateFlow(false)

    private val chosenFilter: Flow<TaskFilter> =
        if (groupId == null) filters.filter else flowOf(TaskFilter(groupId = groupId))

    // Kept for previewing a draft filter's count while the sheet is open.
    private val latestGraph = repo.graph.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val latestToday = repo.today.stateIn(viewModelScope, SharingStarted.Eagerly, LocalDate.now())

    val state: StateFlow<TaskListUiState> =
        combine(repo.graph, repo.today, showHidden, chosenFilter) { graph, today, show, filter ->
            val shown = graph.query(filter.copy(showHidden = show), today)
            // What the eye link counts: everything hidden while it's on; otherwise what it would add.
            val shownIds = shown.mapTo(HashSet()) { it.task.id }
            val hidden = graph.query(filter.copy(showHidden = true), today)
                .filter { it.isHidden && (show || it.task.id !in shownIds) }
            val flat = groupId == null && filter.grouping != Grouping.Ungrouped
            TaskListUiState(
                loading = false,
                today = today,
                tasks = shown,
                groups = if (groupId == null && !flat) graph.groupSummaries() else emptyList(),
                group = groupId?.let(graph::group)?.let(graph::summaryOf),
                showHidden = show,
                hiddenBlocked = hidden.count { it.isBlocked },
                hiddenUpcoming = hidden.count { !it.isBlocked },
                filter = filter,
                groupLabels = if (flat) graph.groups.associate { it.id to it.title } else emptyMap(),
                allGroups = graph.groups.sortedBy { it.title.lowercase() },
                candidates = if (groupId == null) emptyList() else graph.tasks
                    .filter { it.state.isActive && it.groupId != groupId }
                    .sortedWith(
                        compareBy<TaskEntity, String?>(nullsFirst()) {
                            it.groupId?.let(graph::group)?.title?.lowercase()
                        }.thenBy { it.title.lowercase() }
                    )
                    .map { task ->
                        val where = task.groupId?.let(graph::group)?.let { "In ${it.title}" } ?: "No group"
                        val waiting = if (task.state == TaskState.Waiting) " · waiting" else ""
                        GroupCandidate(task.id, task.title, where + waiting)
                    },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskListUiState())

    val pendingUndo: StateFlow<DeletedTask?> = undo.pending

    fun toggleHidden() = showHidden.update { !it }

    /** What [draft] would show right now, for the sheet's "Show N tasks" button. */
    fun previewFor(draft: TaskFilter): FilterPreview {
        val graph = latestGraph.value ?: return FilterPreview(0, 0)
        val tasks = graph.query(draft.copy(showHidden = showHidden.value, groupId = groupId), latestToday.value).size
        // The Ungrouped list also has a row per group.
        val groups = if (groupId == null && draft.grouping == Grouping.Ungrouped) graph.groups.size else 0
        return FilterPreview(tasks, groups)
    }

    fun applyFilter(filter: TaskFilter) = filters.set(filter)

    fun clearFilter() = filters.set(TaskFilter())

    private val _messages = Channel<String>(Channel.BUFFERED)

    /** One-off messages for a snackbar, e.g. tasks that couldn't be added to this group. */
    val messages: Flow<String> = _messages.receiveAsFlow()

    /** In a group view the task always goes into this group, whatever [group] says. */
    fun createTask(title: String, group: Long?, startDate: LocalDate?, dueDate: LocalDate?) {
        writes.launch {
            try {
                repo.createTask(title, groupId = groupId ?: group, startDate = startDate, dueDate = dueDate)
            } catch (e: InvalidDatesException) {
                _messages.send("${e.message}.")
            }
        }
    }

    /** Moves existing tasks into this group, skipping (and reporting) any that would create a loop. */
    fun addExisting(taskIds: Set<Long>) {
        val target = groupId ?: return
        writes.launch {
            val refused = mutableListOf<String>()
            for (id in taskIds) {
                try {
                    repo.editTask(id) { it.copy(groupId = target) }
                } catch (e: CircularDependencyException) {
                    repo.getTask(id)?.let { refused += it.title }
                }
            }
            if (refused.isNotEmpty()) {
                val names = refused.joinToString(", ") { "“$it”" }
                val them = if (refused.size == 1) "it" else "them"
                _messages.send("Couldn't add $names: that would make $them wait on this group's own tasks.")
            }
        }
    }

    fun createGroup(title: String) {
        writes.launch { repo.createGroup(title) }
    }

    fun renameGroup(title: String) {
        val id = groupId ?: return
        writes.launch { repo.renameGroup(id, title) }
    }

    fun dismantleGroup() {
        val id = groupId ?: return
        writes.launch { repo.dismantleGroup(id) }
    }

    fun undoDelete(deleted: DeletedTask) {
        undo.clear(deleted)
        writes.launch { repo.restoreTask(deleted) }
    }

    fun dismissUndo(deleted: DeletedTask) = undo.clear(deleted)
}
