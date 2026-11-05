package com.example.productivity.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.productivity.data.tasks.DateFilter
import com.example.productivity.data.tasks.Grouping
import com.example.productivity.data.tasks.SortOrder
import com.example.productivity.data.tasks.StateFilter
import com.example.productivity.data.tasks.TaskFilter
import com.example.productivity.ui.theme.GruvBg2
import com.example.productivity.ui.theme.GruvFg3

/** Survives rotation as the same text the filter is saved as. */
private val DraftSaver = Saver<TaskFilter, String>(save = { it.encode() }, restore = { TaskFilter.decode(it) })

/**
 * The All view's Filter sheet. Changes are a draft until "Show N tasks"; dismissing the sheet
 * discards them. [preview] says what a draft would list.
 */
@Composable
internal fun FilterSheet(
    current: TaskFilter,
    preview: (TaskFilter) -> FilterPreview,
    onApply: (TaskFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by rememberSaveable(stateSaver = DraftSaver) { mutableStateOf(current) }
    TaskSheet(onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Filter", style = dmSans(22, FontWeight.Bold), modifier = Modifier.weight(1f))
                Text(
                    text = "Clear all",
                    style = dmSans(14, FontWeight.Medium, GruvFg3).copy(textDecoration = TextDecoration.Underline),
                    modifier = Modifier
                        .clickable(role = Role.Button) { draft = TaskFilter() }
                        .padding(horizontal = 4.dp, vertical = 12.dp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Section("Grouping") {
                    SegmentedControl(Grouping.entries, draft.grouping, ::groupingLabel, { draft = draft.copy(grouping = it) }, height = 38.dp)
                }
                Section("State") {
                    ToggleChips(StateFilter.entries, draft.states, ::stateLabel) {
                        draft = draft.copy(states = draft.states.toggle(it))
                    }
                }
                Section("Dates") {
                    ToggleChips(DateFilter.entries, draft.dates, ::dateLabel) {
                        draft = draft.copy(dates = draft.dates.toggle(it))
                    }
                }
                Section("Sort by") {
                    SegmentedControl(SortOrder.entries, draft.sort, ::sortLabel, { draft = draft.copy(sort = it) }, height = 38.dp)
                }
            }

            PrimaryButton(
                text = showLabel(preview(draft)),
                onClick = {
                    onApply(draft)
                    onDismiss()
                },
                modifier = Modifier
                    .topRule(GruvBg2)
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 28.dp),
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = dmSans(13, FontWeight.Bold, GruvFg3))
        content()
    }
}

/** "Show 1 task", "Show 4 tasks", or "Show 0 tasks & 3 groups" when group rows are listed too. */
internal fun showLabel(p: FilterPreview): String {
    val tasks = if (p.tasks == 1) "1 task" else "${p.tasks} tasks"
    val groups = when (p.groups) {
        0 -> ""
        1 -> " & 1 group"
        else -> " & ${p.groups} groups"
    }
    return "Show $tasks$groups"
}

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item

private fun groupingLabel(g: Grouping) = when (g) {
    Grouping.All -> "All"
    Grouping.Ungrouped -> "Ungrouped"
    Grouping.Grouped -> "Grouped"
}

private fun stateLabel(s: StateFilter) = when (s) {
    StateFilter.Open -> "Open"
    StateFilter.Waiting -> "Waiting"
    StateFilter.Blocked -> "Blocked"
    StateFilter.Done -> "Done"
    StateFilter.Dropped -> "Dropped"
}

private fun dateLabel(d: DateFilter) = when (d) {
    DateFilter.Overdue -> "Overdue"
    DateFilter.DueToday -> "Due today"
    DateFilter.DueWithin7Days -> "Due in the next 7 days"
    DateFilter.HasStartDate -> "Has start date"
    DateFilter.NoDate -> "No date"
}

private fun sortLabel(s: SortOrder) = when (s) {
    SortOrder.DueDate -> "Due date"
    SortOrder.Created -> "Created"
    SortOrder.Group -> "Group"
}

/** A 1dp line along the top edge, above the sheet's button. */
private fun Modifier.topRule(color: Color) = drawBehind {
    drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
}
