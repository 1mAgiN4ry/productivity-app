package com.example.productivity

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.productivity.navigation.Screen
import com.example.productivity.ui.screens.HabitTrackerScreen
import com.example.productivity.ui.screens.PomodoroViewModel
import com.example.productivity.ui.screens.PomodoroScreen
import com.example.productivity.ui.screens.SettingsScreen
import com.example.productivity.ui.tasks.TaskRoutes
import com.example.productivity.ui.tasks.taskGraph
import com.example.productivity.ui.theme.GruvBg0
import com.example.productivity.ui.theme.GruvFg0
import com.example.productivity.ui.theme.GruvFg3
import com.example.productivity.ui.theme.PomodoroInk
import com.example.productivity.ui.theme.ProductivityAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProductivityAppTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val destination = navBackStackEntry?.destination

    val isDark = isSystemInDarkTheme()
    // A tab can hold several screens (Task List: list, group, detail), so match on the hierarchy.
    val activeScreen = Screen.items.firstOrNull { screen ->
        destination?.hierarchy?.any { it.route == screen.route } == true
    } ?: Screen.Pomodoro
    val onTasks = activeScreen == Screen.TaskList
    // Task detail is full-screen, as in the design.
    val showNavbar = destination?.route != TaskRoutes.DETAIL

    // Activity-scoped so the timer keeps running across tab switches and rotation, and so
    // the navbar and system bars can follow the current Pomodoro mode.
    val pomodoro: PomodoroViewModel = viewModel()
    val onPomodoro = activeScreen == Screen.Pomodoro

    val targetColor = when {
        onPomodoro -> pomodoro.state.mode.color
        isDark -> activeScreen.darkColor
        else -> activeScreen.lightColor
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300),
        label = "NavbarColorAnimation"
    )

    // Pomodoro's saturated modes and the dark task screens need light status/nav bar icons.
    val view = LocalView.current
    val lightSystemBars = !onPomodoro && !onTasks && !isDark
    if (!view.isInEditMode) {
        LaunchedEffect(lightSystemBars) {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = lightSystemBars
                isAppearanceLightNavigationBars = lightSystemBars
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedColor),
        containerColor = animatedColor,
        bottomBar = {
            if (showNavbar) NavigationBar(
                containerColor = animatedColor,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                // On Pomodoro, mirror the design's mode picker: cream indicator, mode-coloured icon.
                val itemColors = if (onPomodoro) {
                    NavigationBarItemDefaults.colors(
                        indicatorColor = PomodoroInk,
                        selectedIconColor = animatedColor,
                        selectedTextColor = PomodoroInk,
                        unselectedIconColor = PomodoroInk.copy(alpha = 0.7f),
                        unselectedTextColor = PomodoroInk.copy(alpha = 0.7f)
                    )
                } else if (onTasks) {
                    // Gruvbox, as in the Task List design.
                    NavigationBarItemDefaults.colors(
                        indicatorColor = GruvFg0,
                        selectedIconColor = GruvBg0,
                        selectedTextColor = GruvFg0,
                        unselectedIconColor = GruvFg3,
                        unselectedTextColor = GruvFg3
                    )
                } else {
                    NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Screen.items.forEach { screen ->
                    val isSelected = activeScreen == screen
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = isSelected,
                        onClick = {
                            if (isSelected) {
                                // Re-tapping the Task List tab returns to its main list.
                                if (screen == Screen.TaskList) {
                                    navController.popBackStack(TaskRoutes.ALL, inclusive = false)
                                }
                            } else {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = itemColors
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Pomodoro.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Lets imePadding() inside screens ignore the space the navbar already takes.
                .consumeWindowInsets(innerPadding)
        ) {
            composable(Screen.Pomodoro.route) {
                val choices by pomodoro.taskChoices.collectAsState()
                PomodoroScreen(
                    state = pomodoro.state,
                    onModeChange = pomodoro::selectMode,
                    onStartPause = pomodoro::toggle,
                    onReset = pomodoro::reset,
                    taskTitle = pomodoro.task.title,
                    onTaskTyped = pomodoro::typeTask,
                    taskChoices = choices,
                    linkedTaskId = pomodoro.task.taskId,
                    onTaskPicked = pomodoro::pickTask,
                )
            }
            taskGraph(navController, onStartPomodoro = { taskId ->
                pomodoro.startOn(taskId)
                navController.navigate(Screen.Pomodoro.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            })
            composable(Screen.HabitTracker.route) {
                HabitTrackerScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
