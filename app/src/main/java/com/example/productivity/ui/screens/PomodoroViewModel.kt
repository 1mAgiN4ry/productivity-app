package com.example.productivity.ui.screens

import android.app.Application
import android.media.RingtoneManager
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.productivity.ProductivityApp
import com.example.productivity.data.tasks.TaskRepository
import com.example.productivity.ui.tasks.TaskChoice
import com.example.productivity.ui.tasks.taskChoices
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The timer's task field: typed text, or a task from the Task List ([taskId] set). */
data class PomodoroTask(val title: String = "", val taskId: Long? = null)

class PomodoroViewModel(
    application: Application,
    private val tasks: TaskRepository,
) : AndroidViewModel(application) {

    /** Used by the default ViewModel factory. */
    constructor(application: Application) :
        this(application, (application as ProductivityApp).container.taskRepository)

    var state by mutableStateOf(PomodoroState())
        private set

    var task by mutableStateOf(PomodoroTask())
        private set

    /** Open tasks for the task field's picker. */
    val taskChoices: StateFlow<List<TaskChoice>> =
        combine(tasks.graph, tasks.today) { graph, today -> graph.taskChoices(today) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Keeps a linked task's title up to date. */
    private var following: Job? = null

    private var ticker: Job? = null

    /** Deadline on the monotonic clock, so the countdown can't drift or be skewed by clock changes. */
    private var endsAt = 0L

    /** Switching to a different mode resets the timer; re-picking the current one does nothing. */
    fun selectMode(mode: PomodoroMode) {
        if (mode == state.mode) return
        ticker?.cancel()
        state = PomodoroState(mode = mode, session = state.session)
    }

    /** Back to a full, stopped timer for the current mode and session. */
    fun reset() {
        ticker?.cancel()
        state = PomodoroState(mode = state.mode, session = state.session)
    }

    fun toggle() {
        when (state.status) {
            TimerStatus.Ready, TimerStatus.Paused -> start()
            TimerStatus.Running -> pause()
        }
    }

    private fun start() {
        endsAt = now() + state.remainingMillis
        state = state.copy(status = TimerStatus.Running)
        ticker = viewModelScope.launch {
            while (true) {
                val remaining = endsAt - now()
                if (remaining <= 0) {
                    finish()
                    break
                }
                state = state.copy(remainingMillis = remaining)
                // Wake up exactly when the displayed second changes.
                delay((remaining - 1) % 1000 + 1)
            }
        }
    }

    private fun pause() {
        ticker?.cancel()
        state = state.copy(
            status = TimerStatus.Paused,
            remainingMillis = (endsAt - now()).coerceAtLeast(0),
        )
    }

    private fun finish() {
        state = state.next()
        playChime()
    }

    private fun playChime() {
        val app = getApplication<Application>()
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return
        RingtoneManager.getRingtone(app, uri)?.play()
    }

    /** Typing in the field makes it plain text again, no longer linked to a task. */
    fun typeTask(text: String) {
        following?.cancel()
        task = PomodoroTask(text)
    }

    fun pickTask(choice: TaskChoice) = link(choice.id, choice.title)

    /**
     * From a ▶ in the Task List: links the task and, if the timer is idle, starts a focus
     * session on it. A session already running or paused is left alone; only the task changes.
     */
    fun startOn(taskId: Long) {
        link(taskId, title = if (task.taskId == taskId) task.title else "")
        if (state.status == TimerStatus.Ready) {
            selectMode(PomodoroMode.Focus)
            start()
        }
    }

    private fun link(taskId: Long, title: String) {
        following?.cancel()
        task = PomodoroTask(title, taskId)
        following = viewModelScope.launch {
            tasks.observeTask(taskId).collect { status ->
                task = if (status == null) {
                    // Deleted: keep the words, drop the link.
                    task.copy(taskId = null).also { following?.cancel() }
                } else {
                    task.copy(title = status.task.title)
                }
            }
        }
    }

    private fun now() = SystemClock.elapsedRealtime()
}
