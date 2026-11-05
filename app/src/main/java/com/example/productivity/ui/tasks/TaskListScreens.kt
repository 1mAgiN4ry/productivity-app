package com.example.productivity.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.productivity.data.tasks.Blocker
import com.example.productivity.data.tasks.TaskStatus
import com.example.productivity.ui.theme.GruvBg0
import com.example.productivity.ui.theme.GruvFg3

/** Opens whatever is blocking a task: the first blocker, task or group. */
private fun openBlocker(status: TaskStatus, onOpenTask: (Long) -> Unit, onOpenGroup: (Long) -> Unit) {
    when (val blocker = status.blockers.firstOrNull()) {
        is Blocker.Task -> onOpenTask(blocker.task.id)
        is Blocker.Group -> onOpenGroup(blocker.group.id)
        null -> onOpenTask(status.task.id)
    }
}

/** Shows "Deleted “…” · Undo" when coming back from deleting a task. */
@Composable
private fun UndoDeleteEffect(vm: TaskListViewModel, host: SnackbarHostState) {
    val pending by vm.pendingUndo.collectAsState()
    LaunchedEffect(pending) {
        val deleted = pending ?: return@LaunchedEffect
        val result = host.showSnackbar(
            message = "Deleted “${deleted.task.title}”",
            actionLabel = "Undo",
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) vm.undoDelete(deleted) else vm.dismissUndo(deleted)
    }
}

@Composable
fun AllTasksScreen(
    vm: TaskListViewModel,
    onOpenTask: (Long) -> Unit,
    onOpenGroup: (Long) -> Unit,
) {
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var creating by rememberSaveable { mutableStateOf(false) }
    UndoDeleteEffect(vm, snackbar)
    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }

    Box(Modifier.fillMaxSize().background(GruvBg0)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Tasks", style = dmSans(28, FontWeight.Bold), modifier = Modifier.weight(1f))
                RoundIconToggle(
                    icon = TaskIcons.Eye,
                    description = "Show blocked and upcoming tasks",
                    pressed = state.showHidden,
                    onClick = vm::toggleHidden,
                )
            }

            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                val nothingAtAll = state.tasks.isEmpty() && state.groups.isEmpty() &&
                    state.hiddenBlocked + state.hiddenUpcoming == 0
                if (!state.loading && nothingAtAll) {
                    item { EmptyMessage("Nothing here yet. Tap New to add a task or a group.") }
                }
                items(state.tasks, key = { "task-${it.task.id}" }) { status ->
                    if (status.isHidden) {
                        HiddenTaskRow(
                            status, state.today,
                            onClick = { onOpenTask(status.task.id) },
                            onBlockerClick = { openBlocker(status, onOpenTask, onOpenGroup) },
                        )
                    } else {
                        TaskRow(status, state.today) { onOpenTask(status.task.id) }
                    }
                }
                items(state.groups, key = { "group-${it.group.id}" }) { summary ->
                    GroupRow(summary) { onOpenGroup(summary.group.id) }
                }
                item {
                    HiddenTasksLink(state.showHidden, state.hiddenBlocked, state.hiddenUpcoming, vm::toggleHidden)
                }
            }

            PrimaryButton(
                text = "New",
                icon = TaskIcons.Plus,
                onClick = { creating = true },
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp),
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 84.dp)) { TaskSnackbar(it) }
    }

    if (creating) {
        NewSheet(
            groups = state.allGroups,
            today = state.today,
            onCreateTask = vm::createTask,
            onCreateGroup = vm::createGroup,
            onDismiss = { creating = false },
        )
    }
}

@Composable
fun GroupScreen(
    vm: TaskListViewModel,
    onBack: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onOpenGroup: (Long) -> Unit,
) {
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var menuOpen by remember { mutableStateOf(false) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var dismantling by rememberSaveable { mutableStateOf(false) }
    UndoDeleteEffect(vm, snackbar)
    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }

    val group = state.group
    if (!state.loading && group == null) {
        // Dismantled (here or elsewhere): nothing left to show.
        LaunchedEffect(Unit) { onBack() }
    }

    Box(Modifier.fillMaxSize().background(GruvBg0)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 20.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlainIconButton(TaskIcons.ChevronLeft, "Back to all tasks", onBack)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = group?.group?.title.orEmpty(),
                        style = dmSans(26, FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (group != null) {
                        Text(TaskFormat.groupSummary(group.activeCount, group.doneCount), style = dmMono(13, GruvFg3))
                    }
                }
                RoundIconToggle(
                    icon = TaskIcons.Eye,
                    description = "Show blocked and upcoming tasks",
                    pressed = state.showHidden,
                    onClick = vm::toggleHidden,
                )
                Box {
                    PlainIconButton(TaskIcons.More, "Group options", { menuOpen = true })
                    TaskMenu(
                        expanded = menuOpen,
                        onDismiss = { menuOpen = false },
                        items = listOf(
                            "Rename" to { renaming = true },
                            "Dismantle" to { dismantling = true },
                        ),
                    )
                }
            }

            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                if (!state.loading && state.tasks.isEmpty() && state.hiddenBlocked + state.hiddenUpcoming == 0) {
                    item { EmptyMessage("No open tasks in this group.") }
                }
                items(state.tasks, key = { it.task.id }) { status ->
                    if (status.isHidden) {
                        HiddenTaskRow(
                            status, state.today,
                            onClick = { onOpenTask(status.task.id) },
                            onBlockerClick = { openBlocker(status, onOpenTask, onOpenGroup) },
                        )
                    } else {
                        TaskRow(status, state.today) { onOpenTask(status.task.id) }
                    }
                }
                item {
                    HiddenTasksLink(state.showHidden, state.hiddenBlocked, state.hiddenUpcoming, vm::toggleHidden)
                }
                item {
                    InfoNote(
                        text = "This group completes on its own once none of its tasks are open.",
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp),
                    )
                }
            }

            PrimaryButton(
                text = "Add task",
                icon = TaskIcons.Plus,
                onClick = { creating = true },
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp),
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 84.dp)) { TaskSnackbar(it) }
    }

    if (creating && group != null) {
        AddToGroupSheet(
            groupTitle = group.group.title,
            candidates = state.candidates,
            today = state.today,
            onCreateTask = { title, start, due -> vm.createTask(title, group.group.id, start, due) },
            onAddExisting = vm::addExisting,
            onDismiss = { creating = false },
        )
    }
    if (renaming && group != null) {
        TextInputDialog("Rename group", group.group.title, "Rename", vm::renameGroup) { renaming = false }
    }
    if (dismantling && group != null) {
        val single = group.activeCount + group.doneCount == 1
        val tasks = if (single) "Its task stays, but becomes ungrouped." else "Its tasks stay, but become ungrouped."
        ConfirmDialog(
            heading = "Dismantle “${group.group.title}”?",
            body = "$tasks Anything waiting on this group stops waiting.",
            confirmLabel = "Dismantle",
            onConfirm = vm::dismantleGroup,
        ) { dismantling = false }
    }
}
