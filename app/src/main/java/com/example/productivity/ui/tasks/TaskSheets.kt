package com.example.productivity.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.productivity.data.tasks.GroupEntity
import com.example.productivity.ui.theme.GruvBg0Hard
import com.example.productivity.ui.theme.GruvBg1
import com.example.productivity.ui.theme.GruvBg2
import com.example.productivity.ui.theme.GruvFg0
import com.example.productivity.ui.theme.GruvFg3
import com.example.productivity.ui.theme.GruvFg4
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = GruvBg1,
        contentColor = GruvFg0,
        scrimColor = GruvBg0Hard.copy(alpha = 0.7f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = GruvFg4) },
    ) {
        content()
    }
}

/** The group picker for a task: "No group" plus every group. */
@Composable
internal fun GroupPickerSheet(groups: List<GroupEntity>, selectedId: Long?, onPick: (Long?) -> Unit, onDismiss: () -> Unit) {
    TaskSheet(onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            Text("Group", style = dmSans(22, FontWeight.Bold), modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            val options = listOf<Pair<Long?, String>>(null to "No group") + groups.map { it.id to it.title }
            options.forEach { (id, title) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable(role = Role.RadioButton) {
                            onPick(id)
                            onDismiss()
                        }
                        .bottomRule(GruvBg2)
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = dmSans(16, FontWeight.Medium, if (id == null) GruvFg3 else GruvFg0),
                        modifier = Modifier.weight(1f),
                    )
                    if (id == selectedId) {
                        Icon(TaskIcons.Check, contentDescription = "Selected", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

/** A dialog with one text field, e.g. renaming a group. */
@Composable
internal fun TextInputDialog(
    heading: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(heading) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text); onDismiss() }, enabled = text.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = GruvBg1,
    )
}

@Composable
internal fun ConfirmDialog(heading: String, body: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(heading) },
        text = { Text(body, color = GruvFg3) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = GruvBg1,
    )
}

/** Setting a task to Waiting: a required note and an optional check-back date. */
@Composable
internal fun WaitingDialog(
    initialNote: String,
    initialCheckBack: LocalDate?,
    today: LocalDate,
    onSave: (String, LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    var note by rememberSaveable { mutableStateOf(initialNote) }
    var checkBack by rememberSaveable { mutableStateOf(initialCheckBack) }
    var picking by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Waiting on…") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("e.g. a reply from Sam", color = GruvFg4) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button) { picking = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Check back", style = dmSans(16, FontWeight.Medium), modifier = Modifier.weight(1f))
                    Text(
                        text = checkBack?.let { TaskFormat.date(it, today) } ?: "Optional",
                        style = if (checkBack != null) dmMono(15) else dmSans(15, color = GruvFg3),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(note, checkBack); onDismiss() }, enabled = note.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = GruvBg1,
    )
    if (picking) {
        TaskDatePicker(
            heading = "Check back",
            initial = checkBack,
            onPick = { checkBack = it },
            onDismiss = { picking = false },
            minDate = today,
        )
    }
}

/**
 * Android's date picker, Gruvbox-themed, with "Remove" to clear an existing date. Dates outside
 * [minDate]..[maxDate] are greyed out and can't be picked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskDatePicker(
    heading: String,
    initial: LocalDate?,
    onPick: (LocalDate?) -> Unit,
    onDismiss: () -> Unit,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
) {
    val selectable = remember(minDate, maxDate) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                inRange(utcTimeMillis.toDate(), minDate, maxDate)

            override fun isSelectableYear(year: Int): Boolean =
                (minDate == null || year >= minDate.year) && (maxDate == null || year <= maxDate.year)
        }
    }
    // Open on the chosen date, else today, else the nearest allowed month.
    val shown = initial ?: LocalDate.now().let { today ->
        when {
            minDate != null && today.isBefore(minDate) -> minDate
            maxDate != null && today.isAfter(maxDate) -> maxDate
            else -> today
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.toUtcMillis(),
        initialDisplayedMonthMillis = shown.withDayOfMonth(1).toUtcMillis(),
        selectableDates = selectable,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onPick(it.toDate()) }
                    onDismiss()
                },
                enabled = state.selectedDateMillis != null,
            ) { Text("OK") }
        },
        dismissButton = {
            Row {
                if (initial != null) {
                    TextButton(onClick = { onPick(null); onDismiss() }) { Text("Remove") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    ) {
        DatePicker(
            state = state,
            title = {
                Text(
                    text = heading,
                    style = dmSans(14, FontWeight.Medium, GruvFg3),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
        )
    }
}

// The picker works in UTC-midnight milliseconds.
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.toDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

internal fun inRange(date: LocalDate, min: LocalDate?, max: LocalDate?): Boolean =
    (min == null || !date.isBefore(min)) && (max == null || !date.isAfter(max))

