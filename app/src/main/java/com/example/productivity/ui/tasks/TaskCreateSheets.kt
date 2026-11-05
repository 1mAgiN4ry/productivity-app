package com.example.productivity.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.productivity.data.tasks.GroupEntity
import com.example.productivity.ui.theme.GruvFg0
import com.example.productivity.ui.theme.GruvFg3
import com.example.productivity.ui.theme.GruvFg4
import com.example.productivity.ui.theme.GruvYellow
import java.time.LocalDate

private enum class NewKind(val label: String) { Task("Task"), Group("Group") }

private enum class AddMode(val label: String) { New("New task"), Existing("Existing") }

private enum class DateField { Start, Due }

/** The All view's "+ New": a task (with its group and dates) or a group. */
@Composable
internal fun NewSheet(
    groups: List<GroupEntity>,
    today: LocalDate,
    onCreateTask: (title: String, groupId: Long?, start: LocalDate?, due: LocalDate?) -> Unit,
    onCreateGroup: (title: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var kind by rememberSaveable { mutableStateOf(NewKind.Task) }
    TaskSheet(onDismiss) {
        SheetBody {
            Text("New", style = dmSans(22, FontWeight.Bold))
            SegmentedControl(NewKind.entries, kind, NewKind::label, { kind = it }, height = 38.dp)
            when (kind) {
                NewKind.Task -> TaskForm(groups, initialGroupId = null, today) { title, group, start, due ->
                    onCreateTask(title, group, start, due)
                    onDismiss()
                }
                NewKind.Group -> TitleForm("e.g. App build", "Create group") {
                    onCreateGroup(it)
                    onDismiss()
                }
            }
        }
    }
}

/** The group view's "+ Add task": a new task in this group, or existing tasks moved into it. */
@Composable
internal fun AddToGroupSheet(
    groupTitle: String,
    candidates: List<GroupCandidate>,
    today: LocalDate,
    onCreateTask: (title: String, start: LocalDate?, due: LocalDate?) -> Unit,
    onAddExisting: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(AddMode.New) }
    TaskSheet(onDismiss) {
        SheetBody {
            Text("Add to $groupTitle", style = dmSans(22, FontWeight.Bold))
            SegmentedControl(AddMode.entries, mode, AddMode::label, { mode = it }, height = 38.dp)
            when (mode) {
                AddMode.New -> TaskForm(groups = null, initialGroupId = null, today) { title, _, start, due ->
                    onCreateTask(title, start, due)
                    onDismiss()
                }
                AddMode.Existing -> ExistingTaskPicker(candidates) {
                    onAddExisting(it)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun SheetBody(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        content()
    }
}

/** Title, then (if [groups] isn't null) a group, then optional start and due dates. */
@Composable
private fun TaskForm(
    groups: List<GroupEntity>?,
    initialGroupId: Long?,
    today: LocalDate,
    onCreate: (title: String, groupId: Long?, start: LocalDate?, due: LocalDate?) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var groupId by rememberSaveable { mutableStateOf(initialGroupId) }
    var start by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var due by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var picking by rememberSaveable { mutableStateOf<DateField?>(null) }
    val create = { if (title.isNotBlank()) onCreate(title, groupId, start, due) }

    TitleField(title, { title = it }, "What needs doing?", onDone = create)

    Column {
        if (!groups.isNullOrEmpty()) {
            DropdownField(
                label = "Group",
                options = listOf<Pair<Long?, String>>(null to "No group") + groups.map { it.id to it.title },
                selected = groupId,
                onSelect = { groupId = it },
            )
        }
        PropertyRow("Start date", onClick = { picking = DateField.Start }) {
            start?.let { Text(TaskFormat.date(it, today), style = dmMono(15, GruvFg0)) } ?: AddValue()
        }
        PropertyRow("Due date", onClick = { picking = DateField.Due }) {
            due?.let { Text(TaskFormat.date(it, today), style = dmMono(15, GruvYellow)) } ?: AddValue()
        }
    }

    PrimaryButton("Create task", onClick = create, enabled = title.isNotBlank())

    when (picking) {
        DateField.Start -> TaskDatePicker("Start date", start, { start = it }, { picking = null }, maxDate = due)
        DateField.Due -> TaskDatePicker("Due date", due, { due = it }, { picking = null }, minDate = start)
        null -> Unit
    }
}

/** Just a title and a button, e.g. for a new group. */
@Composable
private fun TitleForm(placeholder: String, buttonLabel: String, onCreate: (String) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    val create = { if (title.isNotBlank()) onCreate(title) }
    TitleField(title, { title = it }, placeholder, onDone = create)
    PrimaryButton(buttonLabel, onClick = create, enabled = title.isNotBlank())
}

@Composable
private fun TitleField(value: String, onChange: (String) -> Unit, placeholder: String, onDone: () -> Unit) {
    val focus = remember { FocusRequester() }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, color = GruvFg4) },
        singleLine = true,
        textStyle = dmSans(16, FontWeight.Medium),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
    )
    // After the field above is in place, so the request can't come too early.
    LaunchedEffect(Unit) { focus.requestFocus() }
}

@Composable
private fun ExistingTaskPicker(candidates: List<GroupCandidate>, onAdd: (Set<Long>) -> Unit) {
    var picked by rememberSaveable { mutableStateOf(setOf<Long>()) }
    if (candidates.isEmpty()) {
        Text("There are no open tasks outside this group.", style = dmSans(15, color = GruvFg3, lineHeight = 22))
        return
    }
    Column {
        candidates.forEach { c ->
            TickRow(c.title, c.detail, checked = c.taskId in picked) {
                picked = if (c.taskId in picked) picked - c.taskId else picked + c.taskId
            }
        }
    }
    val label = when (picked.size) {
        0 -> "Add tasks"
        1 -> "Add 1 task"
        else -> "Add ${picked.size} tasks"
    }
    PrimaryButton(label, onClick = { onAdd(picked) }, enabled = picked.isNotEmpty())
}
