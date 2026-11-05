package com.example.productivity.ui.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.productivity.data.tasks.GroupSummary
import com.example.productivity.data.tasks.TaskState
import com.example.productivity.data.tasks.TaskStatus
import com.example.productivity.ui.theme.DmMono
import com.example.productivity.ui.theme.DmSans
import com.example.productivity.ui.theme.GruvBg0
import com.example.productivity.ui.theme.GruvBg2
import com.example.productivity.ui.theme.GruvBg3
import com.example.productivity.ui.theme.GruvFg0
import com.example.productivity.ui.theme.GruvFg3
import com.example.productivity.ui.theme.GruvFg4
import com.example.productivity.ui.theme.GruvYellow
import java.time.LocalDate

// Text styles from the mockup.

internal fun dmSans(size: Int, weight: FontWeight = FontWeight.Normal, color: Color = Color.Unspecified, lineHeight: Int? = null) =
    TextStyle(
        fontFamily = DmSans,
        fontWeight = weight,
        fontSize = size.sp,
        color = color,
        lineHeight = lineHeight?.sp ?: TextStyle.Default.lineHeight,
    )

internal fun dmMono(size: Int, color: Color = Color.Unspecified) =
    TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Normal, fontSize = size.sp, color = color)

// Modifiers

/** A line along the bottom edge, solid or dashed. */
internal fun Modifier.bottomRule(color: Color, dashed: Boolean = false, thickness: Dp = 1.dp) = drawBehind {
    val y = size.height - thickness.toPx() / 2
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = thickness.toPx(),
        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null,
    )
}

/** A dashed rounded outline, like the mockup's info boxes. */
internal fun Modifier.dashedBorder(color: Color, width: Dp = 2.dp, radius: Dp = 16.dp) = drawBehind {
    val stroke = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = size.copy(width = size.width - stroke, height = size.height - stroke),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        ),
    )
}

// Rows

/**
 * [groupName] is shown in the flat lists, where tasks from different groups sit together.
 * [onStart] adds the ▶ button that starts a pomodoro on the task (open and waiting tasks only).
 */
@Composable
internal fun TaskRow(
    status: TaskStatus,
    today: LocalDate,
    groupName: String? = null,
    onStart: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val task = status.task
    val finished = !task.state.isActive
    val showStart = onStart != null && !finished
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .bottomRule(GruvBg2)
            .padding(start = 24.dp, end = if (showStart) 10.dp else 24.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            when (task.state) {
                TaskState.Done -> Icon(TaskIcons.Check, contentDescription = "Done", tint = GruvFg4, modifier = Modifier.size(18.dp))
                TaskState.Dropped -> Box(Modifier.size(8.dp).border(1.5.dp, GruvFg4, CircleShape))
                else -> Box(Modifier.size(8.dp).background(GruvFg0, CircleShape))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = task.title,
                style = if (finished) {
                    dmSans(16, FontWeight.Medium, GruvFg4, lineHeight = 22).copy(textDecoration = TextDecoration.LineThrough)
                } else {
                    dmSans(16, FontWeight.Medium, lineHeight = 22)
                },
            )
            val waiting = task.waitingNote?.takeIf { task.state == TaskState.Waiting }
            if (task.dueDate != null || waiting != null || groupName != null || task.state == TaskState.Dropped) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (task.state == TaskState.Dropped) Text("Dropped", style = dmSans(13, color = GruvFg4))
                    task.dueDate?.let {
                        Text(TaskFormat.date(it, today), style = dmMono(13, if (finished) GruvFg4 else GruvYellow))
                    }
                    groupName?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f, fill = false),
                        ) {
                            Icon(TaskIcons.Layers, contentDescription = "Group", tint = GruvFg3, modifier = Modifier.size(13.dp))
                            Text(it, style = dmSans(13, color = GruvFg3), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    waiting?.let {
                        Text(
                            text = "Waiting · $it",
                            style = dmSans(13, color = GruvFg3),
                            modifier = Modifier
                                .border(1.dp, GruvFg3, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
        if (showStart) PlainIconButton(TaskIcons.Play, "Start pomodoro on this task", onStart!!, iconSize = 18.dp)
    }
}

/**
 * Swipe right to mark a task done. [onDone] is also offered to screen readers as an action,
 * since they can't swipe. Without [onDone] the row doesn't swipe.
 */
@Composable
internal fun SwipeToDone(onDone: (() -> Unit)?, resetKey: Any?, content: @Composable () -> Unit) {
    if (onDone == null) {
        content()
        return
    }
    val swipe = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.StartToEnd) onDone()
            it != SwipeToDismissBoxValue.EndToStart
        },
    )
    // If the row stays (e.g. the filter shows done tasks), slide it back once its state changes.
    LaunchedEffect(resetKey) {
        if (swipe.currentValue != SwipeToDismissBoxValue.Settled) swipe.snapTo(SwipeToDismissBoxValue.Settled)
    }
    SwipeToDismissBox(
        state = swipe,
        enableDismissFromEndToStart = false,
        modifier = Modifier.semantics {
            customActions = listOf(CustomAccessibilityAction("Mark done") { onDone(); true })
        },
        backgroundContent = {
            // Only while swiping, and silent: the row's "Mark done" action already says this.
            if (swipe.dismissDirection != SwipeToDismissBoxValue.StartToEnd) return@SwipeToDismissBox
            Row(
                modifier = Modifier.fillMaxSize().clearAndSetSemantics {}.background(GruvFg0).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(TaskIcons.Check, contentDescription = null, tint = GruvBg0, modifier = Modifier.size(22.dp))
                Text("Done", style = dmSans(16, FontWeight.Bold, GruvBg0))
            }
        },
    ) {
        Box(Modifier.background(GruvBg0)) { content() }
    }
}

/** A blocked or upcoming task, shown muted with a dashed divider when hidden tasks are on. */
@Composable
internal fun HiddenTaskRow(
    status: TaskStatus,
    today: LocalDate,
    onClick: () -> Unit,
    onBlockerClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .bottomRule(GruvBg3, dashed = true)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = if (status.isBlocked) TaskIcons.Lock else TaskIcons.Calendar,
            contentDescription = if (status.isBlocked) "Blocked" else "Upcoming",
            tint = GruvFg4,
            modifier = Modifier.size(22.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(status.task.title, style = dmSans(16, color = GruvFg4, lineHeight = 22))
            if (status.isBlocked) {
                Text(
                    text = TaskFormat.blockedBy(status.blockers),
                    style = dmSans(13, color = GruvFg3).copy(textDecoration = TextDecoration.Underline),
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = onBlockerClick)
                        .padding(vertical = 2.dp),
                )
            } else {
                status.task.startDate?.let {
                    Text("Starts ${TaskFormat.date(it, today)}", style = dmMono(13, GruvFg4))
                }
            }
        }
    }
}

@Composable
internal fun GroupRow(summary: GroupSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(onClick = onClick)
            .bottomRule(GruvBg2)
            .padding(start = 24.dp, end = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(TaskIcons.Layers, contentDescription = null, modifier = Modifier.size(22.dp))
        Text(
            text = summary.group.title,
            style = dmSans(16, FontWeight.Bold),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(TaskIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

// Buttons

/** The 44dp round outlined button; filled when [pressed]. */
@Composable
internal fun RoundIconToggle(
    icon: ImageVector,
    description: String,
    pressed: Boolean,
    onClick: () -> Unit,
    iconSize: Dp = 20.dp,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (pressed) GruvFg0 else Color.Transparent)
            .border(2.dp, GruvFg0, CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Switch
                contentDescription = description
                stateDescription = if (pressed) "On" else "Off"
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (pressed) GruvBg0 else GruvFg0, modifier = Modifier.size(iconSize))
    }
}

@Composable
internal fun PlainIconButton(icon: ImageVector, description: String, onClick: () -> Unit, iconSize: Dp = 22.dp) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = null, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize))
    }
}

/** The outlined pill with an icon (the header's "Filter"); filled when [active]. */
@Composable
internal fun PillButton(text: String, icon: ImageVector, active: Boolean, activeDescription: String, onClick: () -> Unit) {
    val content = if (active) GruvBg0 else GruvFg0
    Row(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (active) GruvFg0 else Color.Transparent)
            .border(2.dp, GruvFg0, RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { if (active) stateDescription = activeDescription }
            .padding(start = 14.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Text(text, style = dmSans(14, FontWeight.Bold, content))
    }
}

/** Multi-select pills (the filter sheet's State and Dates). They wrap onto more lines as needed. */
@OptIn(ExperimentalLayoutApi::class) // FlowRow, stable from Compose 1.8
@Composable
internal fun <T> ToggleChips(options: List<T>, selected: Set<T>, label: (T) -> String, onToggle: (T) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val on = option in selected
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (on) GruvFg0 else Color.Transparent)
                    .border(2.dp, GruvFg0, RoundedCornerShape(18.dp))
                    .toggleable(value = on, role = Role.Checkbox, onValueChange = { onToggle(option) })
                    .padding(start = if (on) 10.dp else 14.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (on) Icon(TaskIcons.Check, contentDescription = null, tint = GruvBg0, modifier = Modifier.size(14.dp))
                Text(label(option), style = dmSans(14, FontWeight.Bold, if (on) GruvBg0 else GruvFg0))
            }
        }
    }
}

/** The full-width cream "New" button. */
@Composable
internal fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (enabled) GruvFg0 else GruvBg3)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = GruvBg0, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = dmSans(18, FontWeight.Bold, color = GruvBg0))
    }
}

/** The muted link under the list ("1 blocked · 1 upcoming" / "Hide blocked & upcoming"). */
@Composable
internal fun HiddenTasksLink(showHidden: Boolean, blocked: Int, upcoming: Int, onClick: () -> Unit) {
    val text = if (showHidden) "Hide blocked & upcoming" else TaskFormat.hiddenSummary(blocked, upcoming) ?: return
    if (showHidden && blocked + upcoming == 0) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(44.dp)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!showHidden) {
            Icon(TaskIcons.Eye, contentDescription = null, tint = GruvFg3, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = dmSans(14, FontWeight.Medium, GruvFg3))
    }
}

/**
 * The outlined segmented control (Grouping, Sort by, task State). [onSelect] gets the tapped
 * option, even if it's already selected.
 */
@Composable
internal fun <T> SegmentedControl(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(2.dp, GruvFg0), RoundedCornerShape(24.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .clip(RoundedCornerShape(height / 2))
                    .background(if (isSelected) GruvFg0 else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(option) }
                    .semantics { this.selected = isSelected },
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option), style = dmSans(14, FontWeight.Bold, if (isSelected) GruvBg0 else GruvFg0))
            }
        }
    }
}

/** The mockup's dashed info box. */
@Composable
internal fun InfoNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = dmSans(13, color = GruvFg3, lineHeight = 19),
        modifier = modifier
            .fillMaxWidth()
            .dashedBorder(GruvBg3)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}

@Composable
internal fun EmptyMessage(text: String) {
    Text(
        text = text,
        style = dmSans(15, color = GruvFg3, lineHeight = 22),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
    )
}

/** Gruvbox-styled snackbar: cream, with the action in bold. */
@Composable
internal fun TaskSnackbar(data: SnackbarData) {
    Snackbar(
        snackbarData = data,
        containerColor = GruvFg0,
        contentColor = GruvBg0,
        actionColor = GruvBg0,
        shape = RoundedCornerShape(16.dp),
    )
}

/** A label with a value on the right, e.g. a due date. Tappable unless [onClick] is null. */
@Composable
internal fun PropertyRow(label: String, onClick: (() -> Unit)?, value: @Composable () -> Unit) {
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

/** The "+ Add" value shown for an empty date. */
@Composable
internal fun AddValue() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(TaskIcons.PlusThin, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text("Add", style = dmSans(16, FontWeight.Medium))
    }
}

/**
 * A property row whose value opens a dropdown of [options]. The first option is the "none" choice,
 * shown dimmed. Long names are cut short with "…", and a long list scrolls.
 */
@Composable
internal fun <K> DropdownField(label: String, options: List<Pair<K, String>>, selected: K, onSelect: (K) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == selected } ?: options.first()
    val isNone = current == options.first()
    PropertyRow(label, onClick = { open = true }) {
        Box(Modifier.widthIn(max = 220.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = current.second,
                    style = dmSans(16, FontWeight.Medium, if (isNone) GruvFg3 else GruvFg0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(TaskIcons.ChevronDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                modifier = Modifier.heightIn(max = 320.dp).widthIn(min = 200.dp, max = 280.dp),
                shape = RoundedCornerShape(20.dp),
                containerColor = GruvBg0,
                border = BorderStroke(2.dp, GruvFg0),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
            ) {
                options.forEachIndexed { i, (key, name) ->
                    val on = key == selected
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = name,
                                style = dmSans(16, FontWeight.Bold, if (i == 0) GruvFg3 else GruvFg0),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingIcon = if (on) {
                            { Icon(TaskIcons.Check, contentDescription = "Selected", modifier = Modifier.size(18.dp)) }
                        } else {
                            null
                        },
                        onClick = {
                            open = false
                            onSelect(key)
                        },
                        contentPadding = PaddingValues(horizontal = 20.dp),
                    )
                }
            }
        }
    }
}

/** A row with a round tick box, for picking several items. */
@Composable
internal fun TickRow(title: String, detail: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .bottomRule(GruvBg2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) GruvFg0 else Color.Transparent)
                .border(2.dp, GruvFg0, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(TaskIcons.Check, contentDescription = null, tint = GruvBg0, modifier = Modifier.size(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = dmSans(16, FontWeight.Medium))
            Text(detail, style = dmSans(12, color = GruvFg3))
        }
    }
}

/** A dropdown menu in the app's style: dark surface, cream outline, rounded like the pills. */
@Composable
internal fun TaskMenu(expanded: Boolean, onDismiss: () -> Unit, items: List<Pair<String, () -> Unit>>) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = GruvBg0,
        border = BorderStroke(2.dp, GruvFg0),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        items.forEach { (label, action) ->
            DropdownMenuItem(
                text = { Text(label, style = dmSans(16, FontWeight.Bold)) },
                onClick = {
                    onDismiss()
                    action()
                },
                contentPadding = PaddingValues(horizontal = 20.dp),
            )
        }
    }
}
