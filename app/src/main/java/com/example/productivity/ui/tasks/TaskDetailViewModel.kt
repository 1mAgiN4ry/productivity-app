package com.example.productivity.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.productivity.data.tasks.CircularDependencyException
import com.example.productivity.data.tasks.DependencyTarget
import com.example.productivity.data.tasks.GroupEntity
import com.example.productivity.data.tasks.InvalidDatesException
import com.example.productivity.data.tasks.TaskEntity
import com.example.productivity.data.tasks.TaskRepository
import com.example.productivity.data.tasks.TaskState
import com.example.productivity.data.tasks.TaskStatus
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DependencyItem(
    val target: DependencyTarget,
    val title: String,
    /** E.g. "Task · open", "Group · complete". */
    val detail: String,
)

data class TaskDetailUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    /** Null once loaded means the task no longer exists. */
    val status: TaskStatus? = null,
    val groupTitle: String? = null,
    val groups: List<GroupEntity> = emptyList(),
    val dependencies: List<DependencyItem> = emptyList(),
)

class TaskDetailViewModel(
    private val repo: TaskRepository,
    private val undo: TaskUndo,
    private val writes: CoroutineScope,
    private val taskId: Long,
) : ViewModel() {

    val state: StateFlow<TaskDetailUiState> =
        combine(repo.graph, repo.today) { graph, today ->
            val task = graph.task(taskId)
            TaskDetailUiState(
                loading = false,
                today = today,
                status = task?.let { graph.statusOf(it, today) },
                groupTitle = task?.groupId?.let(graph::group)?.title,
                groups = graph.groups.sortedBy { it.title.lowercase() },
                dependencies = graph.dependenciesOf(taskId).mapNotNull { target ->
                    when (target) {
                        is DependencyTarget.OnTask -> graph.task(target.taskId)?.let {
                            DependencyItem(target, it.title, "Task · ${TaskFormat.state(it.state).lowercase()}")
                        }
                        is DependencyTarget.OnGroup -> graph.group(target.groupId)?.let {
                            val done = if (graph.isGroupComplete(it.id)) "complete" else "open"
                            DependencyItem(target, it.title, "Group · $done")
                        }
                    }
                },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskDetailUiState())

    private val _messages = Channel<String>(Channel.BUFFERED)

    /** One-off messages to show in a snackbar, e.g. a rejected group move. */
    val messages: Flow<String> = _messages.receiveAsFlow()

    /** Saved on every change; blank titles are skipped (the last non-blank one stays). */
    fun rename(title: String) {
        if (title.isBlank()) return
        writes.launch { repo.editTask(taskId) { it.copy(title = title) } }
    }

    fun setState(state: TaskState) {
        writes.launch { repo.setState(taskId, state) }
    }

    fun setWaiting(note: String, checkBack: LocalDate?) {
        writes.launch { repo.setState(taskId, TaskState.Waiting, note.trim(), checkBack) }
    }

    fun moveToGroup(groupId: Long?) {
        writes.launch {
            try {
                repo.editTask(taskId) { it.copy(groupId = groupId) }
            } catch (e: CircularDependencyException) {
                _messages.send("Can't move it there. ${e.message}.")
            }
        }
    }

    fun setStartDate(date: LocalDate?) = editDates { it.copy(startDate = date) }

    fun setDueDate(date: LocalDate?) = editDates { it.copy(dueDate = date) }

    // The pickers already prevent a start after the due date; this covers anything that slips by.
    private fun editDates(change: (TaskEntity) -> TaskEntity) {
        writes.launch {
            try {
                repo.editTask(taskId, change)
            } catch (e: InvalidDatesException) {
                _messages.send("${e.message}.")
            }
        }
    }

    /** Deletes the task and hands it to [TaskUndo]; returns once it's gone. */
    suspend fun delete() {
        repo.deleteTask(taskId)?.let(undo::offer)
    }
}
