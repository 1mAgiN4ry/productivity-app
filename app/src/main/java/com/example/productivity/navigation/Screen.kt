package com.example.productivity.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

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
        lightColor = Color(0xFFFFEBEE), // Warm red tint
        darkColor = Color(0xFF3B1E1E),
    )

    object TaskList : Screen(
        route = "tasks",
        title = "Task List",
        icon = Icons.Filled.Check,
        lightColor = Color(0xFFE3F2FD), // Cool blue tint
        darkColor = Color(0xFF1E2E3B),
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
