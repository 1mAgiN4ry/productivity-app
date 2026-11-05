package com.example.productivity.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle

private val GruvboxDarkScheme = darkColorScheme(
    primary = GruvFg0,
    onPrimary = GruvBg0,
    primaryContainer = GruvFg0,
    onPrimaryContainer = GruvBg0,
    inversePrimary = GruvBg0,
    secondary = GruvFg3,
    onSecondary = GruvBg0,
    secondaryContainer = GruvBg2,
    onSecondaryContainer = GruvFg0,
    tertiary = GruvYellow,
    onTertiary = GruvBg0,
    background = GruvBg0,
    onBackground = GruvFg0,
    surface = GruvBg0,
    onSurface = GruvFg0,
    surfaceVariant = GruvBg1,
    onSurfaceVariant = GruvFg3,
    surfaceTint = GruvBg0,
    surfaceBright = GruvBg2,
    surfaceDim = GruvBg0,
    surfaceContainerLowest = GruvBg0Hard,
    surfaceContainerLow = GruvBg1,
    surfaceContainer = GruvBg1,
    surfaceContainerHigh = GruvBg1,
    surfaceContainerHighest = GruvBg2,
    inverseSurface = GruvFg0,
    inverseOnSurface = GruvBg0,
    outline = GruvFg4,
    outlineVariant = GruvBg2,
    error = GruvRed,
    onError = GruvBg0,
    scrim = GruvBg0Hard,
)

private val GruvboxTypography: Typography = Typography().let { base ->
    fun TextStyle.dm() = copy(fontFamily = DmSans)
    Typography(
        displayLarge = base.displayLarge.dm(),
        displayMedium = base.displayMedium.dm(),
        displaySmall = base.displaySmall.dm(),
        headlineLarge = base.headlineLarge.dm(),
        headlineMedium = base.headlineMedium.dm(),
        headlineSmall = base.headlineSmall.dm(),
        titleLarge = base.titleLarge.dm(),
        titleMedium = base.titleMedium.dm(),
        titleSmall = base.titleSmall.dm(),
        bodyLarge = base.bodyLarge.dm(),
        bodyMedium = base.bodyMedium.dm(),
        bodySmall = base.bodySmall.dm(),
        labelLarge = base.labelLarge.dm(),
        labelMedium = base.labelMedium.dm(),
        labelSmall = base.labelSmall.dm(),
    )
}

/**
 * Dark Gruvbox, always (regardless of the system setting), for the task screens. Standard
 * Material components inside (date pickers, dialogs, sheets, menus, snackbars) pick it up.
 */
@Composable
fun GruvboxTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GruvboxDarkScheme, typography = GruvboxTypography) {
        CompositionLocalProvider(LocalContentColor provides GruvFg0, content = content)
    }
}
