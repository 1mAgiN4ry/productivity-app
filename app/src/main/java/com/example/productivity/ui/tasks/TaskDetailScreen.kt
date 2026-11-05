package com.example.productivity.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.productivity.data.tasks.Blocker
import com.example.productivity.data.tasks.DependencyTarget
import com.example.productivity.data.tasks.TaskState
import com.example.productivity.ui.theme.GruvBg0
import com.example.productivity.ui.theme.GruvBg2
import com.example.productivity.ui.theme.GruvFg0
import com.example.productivity.ui.theme.GruvFg3
import com.example.productivity.ui.theme.GruvFg4
import com.example.productivity.ui.theme.GruvRed
import com.example.productivity.ui.theme.GruvYellow
import kotlinx.coroutines.launch

private enum class DetailDialog { Waiting, Group, StartDate, DueDate }

@Composable
fun TaskDetailScreen(
    vm: TaskDetailViewModel,
    onBack: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onOpenGroup: (Long) -> Unit,
) {
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var dialog by rememberSaveable { mutableStateOf<DetailDialog?>(null) }
    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }

    val status = state.status
    if (status == null) {
        Box(Modifier.fillMaxSize().background(GruvBg0))
        // Deleted (here or elsewhere): leave.
        if (!state.loading) LaunchedEffect(Unit) { onBack() }
        return
    }
    val task = status.task
    val today = state.today

    Box(Modifier.fillMaxSize().background(GruvBg0)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(start = 14.dp, end = 14.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlainIconButton(TaskIcons.ChevronLeft, "Back to tasks", onBack)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TitleField(initial = task.title, taskId = task.id, onChange = vm::rename)
                    state.groupTitle?.let { Text(it, style = dmSans(14, color = GruvFg3)) }
                }

                if (status.isBlocked) BlockedBanner(status.blockers)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("State")
                    SegmentedControl(
                        options = TaskState.entries,
                        selected = task.state,
                        label = TaskFormat::state,
                        onSelect = { picked ->
                            if (picked == TaskState.Waiting) dialog = DetailDialog.Waiting else vm.setState(picked)
                        },
                    )
                    val waitingLine = if (task.state == TaskState.Waiting && task.waitingNote != null) {
                        "Waiting on ${task.waitingNote}" +
                            (task.checkBackDate?.let { " · check back ${TaskFormat.date(it, today)}" } ?: "")
                    } else {
                        "Waiting asks for a note and an optional check-back date."
                    }
                    Text(waitingLine, style = dmSans(12, color = GruvFg4))
                }

                Column {
                    DetailRow("Group", onClick = { dialog = DetailDialog.Group }) {
                        Text(state.groupTitle ?: "None", style = dmSans(16, FontWeight.Medium, GruvFg3))
                    }
                    DetailRow("Start date", onClick = { dialog = DetailDialog.StartDate }) {
                        task.startDate?.let { Text(TaskFormat.date(it, today), style = dmMono(15, GruvFg0)) } ?: AddValue()
                    }
                    DetailRow("Due date", onClick = { dialog = DetailDialog.DueDate }) {
                        task.dueDate?.let { Text(TaskFormat.date(it, today), style = dmMono(15, GruvYellow)) } ?: AddValue()
                    }
                    // Read-only until reminders are built.
                    DetailRow("Reminder", onClick = null) {
                        Text(task.reminder?.let(TaskFormat::reminder) ?: "None", style = dmMono(15, GruvFg3))
                    }
                }

                if (state.dependencies.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionLabel("Depends on")
                        state.dependencies.forEach { dep ->
                            DependencyRow(dep) {
                                when (val target = dep.target) {
                                    is DependencyTarget.OnTask -> onOpenTask(target.taskId)
                                    is DependencyTarget.OnGroup -> onOpenGroup(target.groupId)
                                }
                            }
                        }
                    }
                }

                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Delete task",
                        style = dmSans(14, FontWeight.Bold, GruvRed),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .clickable(role = Role.Button) {
                                scope.launch {
                                    vm.delete()
                                    onBack()
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) { TaskSnackbar(it) }
    }

    when (dialog) {
        DetailDialog.Waiting -> WaitingDialog(
            initialNote = task.waitingNote.orEmpty(),
            initialCheckBack = task.checkBackDate,
            today = today,
            onSave = vm::setWaiting,
            onDismiss = { dialog = null },
        )
        DetailDialog.Group -> GroupPickerSheet(state.groups, task.groupId, vm::moveToGroup) { dialog = null }
        DetailDialog.StartDate -> TaskDatePicker(task.startDate, vm::setStartDate) { dialog = null }
        DetailDialog.DueDate -> TaskDatePicker(task.dueDate, vm::setDueDate) { dialog = null }
        null -> Unit
    }
}

/** Editable in place; saves as you type. Local state so saved values don't fight the cursor. */
@Composable
private fun TitleField(initial: String, taskId: Long, onChange: (String) -> Unit) {
    var text by rememberSaveable(taskId) { mutableStateOf(initial) }
    val focus = LocalFocusManager.current
    BasicTextField(
        value = text,
        onValueChange = {
            text = it
            onChange(it)
        },
        textStyle = dmSans(24, FontWeight.Bold, GruvFg0),
        cursorBrush = SolidColor(GruvFg0),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Task title" }
            .bottomRule(GruvFg0, thickness = 2.dp)
            .padding(bottom = 8.dp),
    )
}

@Composable
private fun BlockedBanner(blockers: List<Blocker>) {
    val names = blockers.map(TaskFormat::blockerName)
    val single = blockers.singleOrNull()
    val ending = when {
        single is Blocker.Group -> " is complete."
        single != null -> " is done."
        else -> " are done."
    }
    val text = buildAnnotatedString {
        append("Blocked until ")
        names.forEachIndexed { i, name ->
            if (i > 0) append(if (i == names.lastIndex) " and " else ", ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = GruvFg0)) { append(name) }
        }
        append(ending)
        append(" This clears on its own.")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dashedBorder(GruvFg4)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(TaskIcons.Lock, contentDescription = null, tint = GruvFg3, modifier = Modifier.padding(top = 1.dp).size(18.dp))
        Text(text, style = dmSans(14, color = GruvFg3, lineHeight = 20))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = dmSans(13, FontWeight.Bold, GruvFg3))
}

@Composable
private fun DetailRow(label: String, onClick: (() -> Unit)?, value: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .bottomRule(GruvBg2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = dmSans(16, FontWeight.Medium), modifier = Modifier.weight(1f))
        value()
    }
}

@Composable
private fun AddValue() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(TaskIcons.PlusThin, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text("Add", style = dmSans(16, FontWeight.Medium))
    }
}

@Composable
private fun DependencyRow(dep: DependencyItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .bottomRule(GruvBg2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (dep.target) {
            is DependencyTarget.OnTask -> Box(Modifier.size(20.dp).border(2.dp, GruvFg0, CircleShape))
            is DependencyTarget.OnGroup -> Icon(TaskIcons.Layers, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(dep.title, style = dmSans(16, FontWeight.Medium))
            Text(dep.detail, style = dmSans(12, color = GruvFg3))
        }
    }
}
