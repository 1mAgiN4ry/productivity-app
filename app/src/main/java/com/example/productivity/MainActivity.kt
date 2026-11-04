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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.example.productivity.ui.screens.TaskListScreen
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
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Pomodoro.route

    val isDark = isSystemInDarkTheme()
    val activeScreen = Screen.items.firstOrNull { it.route == currentRoute } ?: Screen.Pomodoro

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

    // The Pomodoro modes are saturated colours, so they need light status/nav bar icons.
    val view = LocalView.current
    val lightSystemBars = !onPomodoro && !isDark
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
            NavigationBar(
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
                    val isSelected = currentRoute == screen.route
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
                            if (currentRoute != screen.route) {
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
                PomodoroScreen(
                    state = pomodoro.state,
                    onModeChange = pomodoro::selectMode,
                    onStartPause = pomodoro::toggle,
                    onReset = pomodoro::reset
                )
            }
            composable(Screen.TaskList.route) {
                TaskListScreen()
            }
            composable(Screen.HabitTracker.route) {
                HabitTrackerScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
