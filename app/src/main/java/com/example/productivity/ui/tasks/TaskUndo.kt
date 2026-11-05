package com.example.productivity.ui.tasks

import com.example.productivity.data.tasks.DeletedTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hands a just-deleted task from the detail screen (which closes) to whichever list screen
 * comes back into view, so that screen can offer "Undo".
 */
class TaskUndo {
    private val _pending = MutableStateFlow<DeletedTask?>(null)
    val pending: StateFlow<DeletedTask?> = _pending.asStateFlow()

    fun offer(deleted: DeletedTask) {
        _pending.value = deleted
    }

    /** Takes the pending deletion, if it's still [deleted] (so two screens can't both claim it). */
    fun clear(deleted: DeletedTask) {
        _pending.compareAndSet(deleted, null)
    }
}
