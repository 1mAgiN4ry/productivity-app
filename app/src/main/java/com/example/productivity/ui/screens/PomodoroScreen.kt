package com.example.productivity.ui.screens

import com.example.productivity.ui.tasks.TaskChoice
import com.example.productivity.ui.tasks.TaskPickerSheet
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.productivity.ui.theme.DmMono
import com.example.productivity.ui.theme.DmSans
import com.example.productivity.ui.theme.PomodoroInk

/**
 * Pomodoro "timer ready" screen, built from the design reference.
 *
 * The screen does not paint its own background: the hosting Scaffold fills it with
 * [PomodoroMode.color] so the navbar and system bars change colour along with it.
 */
@Composable
fun PomodoroScreen(
    state: PomodoroState,
    onModeChange: (PomodoroMode) -> Unit,
    onStartPause: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    taskTitle: String = "",
    onTaskTyped: (String) -> Unit = {},
    taskChoices: List<TaskChoice> = emptyList(),
    linkedTaskId: Long? = null,
    onTaskPicked: (TaskChoice) -> Unit = {},
) {
    val mode = state.mode
    // Same 300ms tween as the Scaffold background, so the "cut-out" colours stay in sync.
    val modeColor by animateColorAsState(
        targetValue = mode.color,
        animationSpec = tween(durationMillis = 300),
        label = "PomodoroModeColor"
    )
    var picking by rememberSaveable { mutableStateOf(false) }

    CompositionLocalProvider(LocalContentColor provides PomodoroInk) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .imePadding()
                // The design's 64px top / 48px bottom include the status bar and home
                // indicator; on Android the Scaffold insets already account for those.
                .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ModePicker(
                mode = mode,
                modeColor = modeColor,
                onModeChange = onModeChange,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds(),
                // Sit above centre: of the free space, 20% goes above the dial and 80% below
                // the button, leaving room for the task area and keeping proportions on any
                // screen height.
                contentAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.6f),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(56.dp),
                ) {
                    TimerDial(state = state)
                    Box(contentAlignment = Alignment.TopCenter) {
                        StartButton(
                            label = when (state.status) {
                                TimerStatus.Ready -> "Start"
                                TimerStatus.Running -> "Pause"
                                TimerStatus.Paused -> "Resume"
                            },
                            modeColor = modeColor,
                            onClick = onStartPause,
                        )
                        // Offset below the button rather than laid out after it, so showing it
                        // doesn't shift the dial and button away from the design's positions.
                        ResetLink(
                            visible = state.status == TimerStatus.Paused,
                            onClick = onReset,
                            modifier = Modifier.offset(y = 72.dp + 12.dp),
                        )
                    }
                }
            }
            TaskField(
                value = taskTitle,
                onValueChange = onTaskTyped,
                onPickTask = { picking = true },
            )
        }
    }

    if (picking) {
        TaskPickerSheet(
            choices = taskChoices,
            selectedId = linkedTaskId,
            onPick = onTaskPicked,
            onDismiss = { picking = false },
        )
    }
}

private val PickerTextStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
)

@Composable
private fun ModePicker(
    mode: PomodoroMode,
    modeColor: Color,
    onModeChange: (PomodoroMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier.size(width = 220.dp, height = 48.dp)) {
        PickerSurface(modeColor) {
            PickerRow(
                modifier = Modifier.clickable(
                    role = Role.DropdownList,
                    onClickLabel = "Change timer mode",
                ) { expanded = true },
            ) {
                Text(mode.label, style = PickerTextStyle)
                Icon(
                    imageVector = ChevronDownIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // The open list is a popup laid exactly over the closed pill, so it draws above the
        // dial and closes on any tap outside it (including the navbar) or on Back.
        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                PickerSurface(modeColor, Modifier.width(220.dp)) {
                    PomodoroMode.entries.forEach { option ->
                        val selected = option == mode
                        PickerRow(
                            modifier = Modifier.selectable(
                                selected = selected,
                                role = Role.Tab,
                            ) {
                                onModeChange(option)
                                expanded = false
                            },
                            background = if (selected) PomodoroInk else Color.Transparent,
                            contentColor = if (selected) modeColor else PomodoroInk,
                        ) {
                            Text(option.label, style = PickerTextStyle)
                        }
                    }
                }
            }
        }
    }
}

/** The picker's outlined pill; used for both the closed button and the open list. */
@Composable
private fun PickerSurface(
    modeColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    CompositionLocalProvider(LocalContentColor provides PomodoroInk) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .background(modeColor)
                .border(2.dp, PomodoroInk, shape)
                .padding(2.dp),
            content = content,
        )
    }
}

@Composable
private fun PickerRow(
    modifier: Modifier = Modifier,
    background: Color = Color.Transparent,
    contentColor: Color = PomodoroInk,
    content: @Composable RowScope.() -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(background)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            content = content,
        )
    }
}

@Composable
private fun TimerDial(
    state: PomodoroState,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            // Geometry is specified on a 300-unit artboard; scale it in case the dial
            // is squeezed on narrow screens.
            val unit = size.minDimension / 300f
            val ringRadius = 140f * unit
            val ring = Stroke(width = 14f * unit)

            drawCircle(
                color = PomodoroInk.copy(alpha = 0.22f),
                radius = ringRadius,
                center = center,
                style = ring,
            )

            // One segment per minute, separated by gaps of 0.4% of the circumference,
            // with a gap centred at 12 o'clock. As time runs down the ring empties
            // anticlockwise back towards the top.
            val segments = state.mode.durationMinutes
            val step = 360f / segments
            val gap = 360f * 0.004f
            val filledTo = state.progress * 360f
            val arcTopLeft = Offset(center.x - ringRadius, center.y - ringRadius)
            val arcSize = Size(ringRadius * 2, ringRadius * 2)
            repeat(segments) { i ->
                val segmentStart = i * step + gap / 2
                val segmentEnd = (i + 1) * step - gap / 2
                val sweep = minOf(segmentEnd, filledTo) - segmentStart
                if (sweep <= 0f) return@repeat
                drawArc(
                    color = PomodoroInk,
                    startAngle = -90f + segmentStart,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = ring,
                )
            }

            drawCircle(
                color = PomodoroInk,
                radius = 118f * unit,
                center = center,
                style = Stroke(width = 2f * unit),
            )
        }
        Column(
            modifier = Modifier.padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(36.dp),
        ) {
            Text(
                text = formatTime(state.remainingSeconds),
                style = TextStyle(
                    fontFamily = DmMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 72.sp,
                    lineHeight = 72.sp,
                    letterSpacing = (-0.02).em,
                ),
            )
            SessionDots(current = state.session, total = SESSIONS_PER_CYCLE)
        }
    }
}

@Composable
private fun SessionDots(current: Int, total: Int) {
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "Session $current of $total"
        },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(total) { i ->
            val dot = Modifier.size(10.dp)
            Box(
                if (i < current) dot.background(PomodoroInk, CircleShape)
                else dot.border(2.dp, PomodoroInk, CircleShape)
            )
        }
    }
}

@Composable
private fun StartButton(label: String, modeColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 280.dp, height = 72.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(PomodoroInk)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = modeColor,
            style = TextStyle(
                fontFamily = DmSans,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
            ),
        )
    }
}

@Composable
private fun ResetLink(visible: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Reset",
                style = TextStyle(
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
            )
        }
    }
}

private val TaskTextStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
)

@Composable
private fun TaskField(
    value: String,
    onValueChange: (String) -> Unit,
    onPickTask: () -> Unit,
) {
    Column(Modifier.width(280.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Task" },
                singleLine = true,
                textStyle = TaskTextStyle.copy(color = PomodoroInk),
                cursorBrush = SolidColor(PomodoroInk),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = "Add or pick a task",
                                style = TaskTextStyle,
                                color = PomodoroInk,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(role = Role.Button, onClick = onPickTask),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = TaskListIcon,
                    contentDescription = "Choose an existing task",
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(PomodoroInk.copy(alpha = 0.7f))
        )
    }
}

private fun formatTime(totalSeconds: Long): String =
    "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)

// Stroke icons copied from the design's SVGs (24x24 viewBox).
private val ChevronDownIcon: ImageVector = strokeIcon(
    name = "ChevronDown",
    pathData = "M6 9l6 6 6-6",
    strokeWidth = 2.5f,
)

private val TaskListIcon: ImageVector = strokeIcon(
    name = "TaskList",
    pathData = "M9 6h11M9 12h11M9 18h11M4.5 6h.01M4.5 12h.01M4.5 18h.01",
    strokeWidth = 2f,
)

private fun strokeIcon(name: String, pathData: String, strokeWidth: Float): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(pathData),
        stroke = SolidColor(Color.Black), // replaced by Icon's tint
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()

@Preview(widthDp = 390, heightDp = 760)
@Composable
private fun PomodoroScreenPreview() {
    Box(Modifier.background(PomodoroMode.Focus.color)) {
        PomodoroScreen(state = PomodoroState(), onModeChange = {}, onStartPause = {}, onReset = {})
    }
}
