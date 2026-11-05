package com.example.productivity.ui.tasks

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.productivity.AppContainer
import com.example.productivity.ProductivityApp
import com.example.productivity.ui.theme.GruvboxTheme

object TaskRoutes {
    /** The Task List tab's nested graph; also its navbar route. */
    const val GRAPH = "tasks"
    const val ALL = "tasks/all"
    const val GROUP = "tasks/group/{groupId}"
    const val DETAIL = "tasks/task/{taskId}"

    fun group(id: Long) = "tasks/group/$id"
    fun task(id: Long) = "tasks/task/$id"
}

fun NavGraphBuilder.taskGraph(nav: NavController) {
    navigation(startDestination = TaskRoutes.ALL, route = TaskRoutes.GRAPH) {
        composable(TaskRoutes.ALL) {
            val c = container()
            val vm = viewModel { TaskListViewModel(c.taskRepository, c.taskUndo, c.appScope, groupId = null, c.taskFilter) }
            GruvboxTheme {
                AllTasksScreen(
                    vm = vm,
                    onOpenTask = { nav.navigate(TaskRoutes.task(it)) },
                    onOpenGroup = { nav.navigate(TaskRoutes.group(it)) },
                )
            }
        }
        composable(
            TaskRoutes.GROUP,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType }),
        ) { entry ->
            val c = container()
            val groupId = entry.arguments!!.getLong("groupId")
            val vm = viewModel { TaskListViewModel(c.taskRepository, c.taskUndo, c.appScope, groupId) }
            GruvboxTheme {
                GroupScreen(
                    vm = vm,
                    onBack = nav.backFrom(entry),
                    onOpenTask = { nav.navigate(TaskRoutes.task(it)) },
                    onOpenGroup = { if (it != groupId) nav.navigate(TaskRoutes.group(it)) },
                )
            }
        }
        composable(
            TaskRoutes.DETAIL,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
        ) { entry ->
            val c = container()
            val taskId = entry.arguments!!.getLong("taskId")
            val vm = viewModel { TaskDetailViewModel(c.taskRepository, c.taskUndo, c.appScope, taskId) }
            GruvboxTheme {
                TaskDetailScreen(
                    vm = vm,
                    onBack = nav.backFrom(entry),
                    onOpenTask = { if (it != taskId) nav.navigate(TaskRoutes.task(it)) },
                    onOpenGroup = { nav.navigate(TaskRoutes.group(it)) },
                )
            }
        }
    }
}

/**
 * Pops [entry] only while it's the screen in front, so a double tap, or a screen leaving
 * itself after a deletion it triggered, can't pop the screen underneath too.
 */
private fun NavController.backFrom(entry: NavBackStackEntry): () -> Unit = {
    if (entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) popBackStack()
}

@Composable
private fun container(): AppContainer =
    (LocalContext.current.applicationContext as ProductivityApp).container
