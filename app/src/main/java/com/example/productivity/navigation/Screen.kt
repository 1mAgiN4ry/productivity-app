package com.example.productivity.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.productivity.ui.theme.GruvBg0
import com.example.productivity.ui.theme.PomodoroFocusRed

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val lightColor: Color,
    val darkColor: Color,
) {
    object Pomodoro : Screen(
        route = "pomodoro",
        title = "Pomodoro",
        icon = Icons.Filled.PlayArrow,
        // Unused while the screen is shown: MainActivity follows the selected PomodoroMode.
        lightColor = PomodoroFocusRed,
        darkColor = PomodoroFocusRed,
    )

    object TaskList : Screen(
        route = "tasks",
        title = "Task List",
        icon = Icons.Filled.Check,
        // Always dark Gruvbox, as in the Task List design.
        lightColor = GruvBg0,
        darkColor = GruvBg0,
    )

    object HabitTracker : Screen(
        route = "habits",
        title = "Habit Tracker",
        icon = Icons.Filled.Refresh,
        lightColor = Color(0xFFE8F5E9), // Emerald green tint
        darkColor = Color(0xFF1E3B2B),
    )

    object Settings : Screen(
        route = "settings",
        title = "Settings",
        icon = Icons.Filled.Settings,
        lightColor = Color(0xFFF3E5F5), // Soft purple tint
        darkColor = Color(0xFF311E3B),
    )

    companion object {
        val items get() = listOf(Pomodoro, TaskList, HabitTracker, Settings)
    }
}
