package com.example.productivity.ui.tasks

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Stroke icons traced from the Task List mockup's SVGs (24x24 viewBox). */
object TaskIcons {
    val Eye = strokeIcon(
        "Eye", 2f,
        "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z",
        "M15 12a3 3 0 1 1-6 0a3 3 0 1 1 6 0z",
    )
    val Layers = strokeIcon("Layers", 2f, "M12 3l9 5-9 5-9-5z", "M3 13l9 5 9-5")
    val ChevronRight = strokeIcon("ChevronRight", 2.2f, "M9 6l6 6-6 6")
    val ChevronDown = strokeIcon("ChevronDown", 2.2f, "M6 9l6 6 6-6")
    val ChevronLeft = strokeIcon("ChevronLeft", 2.2f, "M15 6l-6 6 6 6")
    val Plus = strokeIcon("Plus", 2.6f, "M12 5v14M5 12h14")
    val PlusThin = strokeIcon("PlusThin", 2.4f, "M12 5v14M5 12h14")
    val Lock = strokeIcon(
        "Lock", 2f,
        "M7 11h10a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z",
        "M8 11V7a4 4 0 0 1 8 0v4",
    )
    val Calendar = strokeIcon(
        "Calendar", 2f,
        "M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z",
        "M16 3v4M8 3v4M3 10h18",
    )
    val Filter = strokeIcon("Filter", 2.2f, "M4 6h16M7 12h10M10 18h4")
    val More = strokeIcon("More", 3f, "M12 5h.01M12 12h.01M12 19h.01")
    /** Filled, unlike the others. */
    val Play = filledIcon("Play", "M7 4.5l13 7.5-13 7.5z")
    val Check = strokeIcon("Check", 2.4f, "M5 12.5l4.5 4.5L19 7.5")
}

private fun strokeIcon(name: String, strokeWidth: Float, vararg paths: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        paths.forEach { path ->
            addPath(
                pathData = addPathNodes(path),
                stroke = SolidColor(Color.Black), // replaced by Icon's tint
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

private fun filledIcon(name: String, path: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(path),
        fill = SolidColor(Color.Black), // replaced by Icon's tint
    ).build()
