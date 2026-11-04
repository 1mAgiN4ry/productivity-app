package com.example.productivity.ui.screens

import androidx.compose.ui.graphics.Color
import com.example.productivity.ui.theme.PomodoroFocusRed
import com.example.productivity.ui.theme.PomodoroLongBreakBlue
import com.example.productivity.ui.theme.PomodoroShortBreakGreen

enum class PomodoroMode(
    val label: String,
    val durationMinutes: Int,
    val color: Color,
) {
    Focus("Focus", 25, PomodoroFocusRed),
    ShortBreak("Short break", 5, PomodoroShortBreakGreen),
    LongBreak("Long break", 15, PomodoroLongBreakBlue);

    val durationMillis: Long get() = durationMinutes * 60_000L
}
