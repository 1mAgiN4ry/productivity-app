package com.example.productivity.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.productivity.data.tasks.DeletedTask
import com.example.productivity.data.tasks.GroupSummary
import com.example.productivity.data.tasks.Grouping
import com.example.productivity.data.tasks.TaskFilter
import com.example.productivity.data.tasks.TaskRepository
import com.example.productivity.data.tasks.TaskStatus
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
)

/** Backs both the All view ([groupId] null: loose tasks plus group rows) and a Group view. */
class TaskListViewModel(
    private val repo: TaskRepository,
    private val undo: TaskUndo,
    private val writes: CoroutineScope,
    private val groupId: Long?,
) : ViewModel() {

    private val showHidden = MutableStateFlow(false)

    val state: StateFlow<TaskListUiState> =
        combine(repo.graph, repo.today, showHidden) { graph, today, show ->
            // Fetch with hidden tasks included, then decide what to show and what to count.
            val all = graph.query(TaskFilter(grouping = Grouping.Ungrouped, groupId = groupId, showHidden = true), today)
            val hidden = all.filter { it.isHidden }
            TaskListUiState(
                loading = false,
                today = today,
                tasks = if (show) all else all.filterNot { it.isHidden },
                groups = if (groupId == null) graph.groupSummaries() else emptyList(),
                group = groupId?.let(graph::group)?.let(graph::summaryOf),
                showHidden = show,
                hiddenBlocked = hidden.count { it.isBlocked },
                hiddenUpcoming = hidden.count { !it.isBlocked },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskListUiState())

    val pendingUndo: StateFlow<DeletedTask?> = undo.pending

    fun toggleHidden() = showHidden.update { !it }

    fun createTask(title: String) {
        writes.launch { repo.createTask(title, groupId = groupId) }
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
