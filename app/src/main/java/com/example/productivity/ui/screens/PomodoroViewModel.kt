package com.example.productivity.ui.screens

import android.app.Application
import android.media.RingtoneManager
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    var state by mutableStateOf(PomodoroState())
        private set

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

    private fun now() = SystemClock.elapsedRealtime()
}
