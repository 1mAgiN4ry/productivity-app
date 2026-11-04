package com.example.productivity.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class PomodoroStateTest {

    @Test
    fun `focus is followed by a short break in the same session`() {
        val next = PomodoroState(PomodoroMode.Focus, session = 2).next()
        assertEquals(PomodoroState(PomodoroMode.ShortBreak, session = 2), next)
    }

    @Test
    fun `last focus session is followed by a long break`() {
        val next = PomodoroState(PomodoroMode.Focus, session = SESSIONS_PER_CYCLE).next()
        assertEquals(PomodoroMode.LongBreak, next.mode)
    }

    @Test
    fun `short break advances to the next focus session`() {
        val next = PomodoroState(PomodoroMode.ShortBreak, session = 2).next()
        assertEquals(PomodoroState(PomodoroMode.Focus, session = 3), next)
    }

    @Test
    fun `long break starts a new cycle`() {
        val next = PomodoroState(PomodoroMode.LongBreak, session = SESSIONS_PER_CYCLE).next()
        assertEquals(PomodoroState(PomodoroMode.Focus, session = 1), next)
    }

    @Test
    fun `next state is ready with a full timer`() {
        val next = PomodoroState(PomodoroMode.Focus, status = TimerStatus.Running, remainingMillis = 0).next()
        assertEquals(TimerStatus.Ready, next.status)
        assertEquals(PomodoroMode.ShortBreak.durationMillis, next.remainingMillis)
        assertEquals(1f, next.progress)
    }

    @Test
    fun `remaining seconds round up`() {
        assertEquals(1500L, PomodoroState(remainingMillis = 1_499_001).remainingSeconds)
        assertEquals(1499L, PomodoroState(remainingMillis = 1_499_000).remainingSeconds)
        assertEquals(1L, PomodoroState(remainingMillis = 1).remainingSeconds)
        assertEquals(0L, PomodoroState(remainingMillis = 0).remainingSeconds)
    }
}
