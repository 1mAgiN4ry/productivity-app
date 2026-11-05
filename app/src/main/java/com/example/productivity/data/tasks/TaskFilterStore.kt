package com.example.productivity.data.tasks

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The All view's filter, remembered across app restarts. Pass null [prefs] to keep it in memory
 * only (tests).
 */
class TaskFilterStore(private val prefs: SharedPreferences?) {

    private val _filter = MutableStateFlow(TaskFilter.decode(prefs?.getString(KEY, null)))
    val filter: StateFlow<TaskFilter> = _filter.asStateFlow()

    fun set(filter: TaskFilter) {
        val choices = filter.copy(showHidden = false, groupId = null)
        _filter.value = choices
        prefs?.edit()?.putString(KEY, choices.encode())?.apply()
    }

    private companion object {
        const val KEY = "all_view_filter"
    }
}
