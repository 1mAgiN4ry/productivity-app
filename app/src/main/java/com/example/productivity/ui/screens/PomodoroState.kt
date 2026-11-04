package com.example.productivity.ui.screens

const val SESSIONS_PER_CYCLE = 4

enum class TimerStatus { Ready, Running, Paused }

data class PomodoroState(
    val mode: PomodoroMode = PomodoroMode.Focus,
    /** 1-based focus session within the current cycle of [SESSIONS_PER_CYCLE]. */
    val session: Int = 1,
    val status: TimerStatus = TimerStatus.Ready,
    val remainingMillis: Long = mode.durationMillis,
) {
    /** 1.0 when the timer is full, 0.0 when it has run out. */
    val progress: Float
        get() = (remainingMillis.toFloat() / mode.durationMillis).coerceIn(0f, 1f)

    /** Whole seconds left, rounded up so the display shows 25:00 until a full second has passed. */
    val remainingSeconds: Long
        get() = (remainingMillis + 999) / 1000

    /**
     * The ready state that follows this interval finishing:
     * focus -> short break (or long break after the last session), break -> next focus.
     */
    fun next(): PomodoroState = when (mode) {
        PomodoroMode.Focus ->
            if (session >= SESSIONS_PER_CYCLE) PomodoroState(PomodoroMode.LongBreak, session)
            else PomodoroState(PomodoroMode.ShortBreak, session)
        PomodoroMode.ShortBreak -> PomodoroState(PomodoroMode.Focus, session + 1)
        PomodoroMode.LongBreak -> PomodoroState(PomodoroMode.Focus, 1)
    }
}
