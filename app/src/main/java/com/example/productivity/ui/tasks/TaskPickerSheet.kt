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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.productivity.data.tasks.TaskGraph
import com.example.productivity.data.tasks.TaskFilter
import com.example.productivity.data.tasks.Grouping
import com.example.productivity.ui.theme.GruvBg2
import com.example.productivity.ui.theme.GruvFg3
import com.example.productivity.ui.theme.GruvboxTheme
import java.time.LocalDate

/** A task offered by [TaskPickerSheet], e.g. "Write the essay" / "Uni · Today". */
data class TaskChoice(val id: Long, val title: String, val detail: String?)

/** The open tasks you could work on now (not blocked or upcoming), soonest due first. */
fun TaskGraph.taskChoices(today: LocalDate): List<TaskChoice> =
    query(TaskFilter(grouping = Grouping.All), today).map { status ->
        val task = status.task
        val detail = listOfNotNull(
            task.groupId?.let(::group)?.title,
            task.dueDate?.let { TaskFormat.date(it, today) },
        ).joinToString(" · ").ifEmpty { null }
        TaskChoice(task.id, task.title, detail)
    }

/** Picks a task for the Pomodoro timer. Gruvbox-styled, like the Task List it comes from. */
@Composable
fun TaskPickerSheet(
    choices: List<TaskChoice>,
    selectedId: Long?,
    onPick: (TaskChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    GruvboxTheme {
        TaskSheet(onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
            ) {
                Text(
                    text = "Pick a task",
                    style = dmSans(22, FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                if (choices.isEmpty()) {
                    EmptyMessage("No open tasks right now. Add some in the Task List, or type one in.")
                }
                choices.forEach { choice ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable(role = Role.Button) {
                                onPick(choice)
                                onDismiss()
                            }
                            .bottomRule(GruvBg2)
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(choice.title, style = dmSans(16, FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            choice.detail?.let {
                                Text(it, style = dmSans(13, color = GruvFg3), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (choice.id == selectedId) {
                            Icon(TaskIcons.Check, contentDescription = "Selected", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
