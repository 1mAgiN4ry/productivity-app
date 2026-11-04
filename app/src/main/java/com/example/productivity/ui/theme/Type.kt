package com.example.productivity.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.productivity.R

val DmSans = FontFamily(
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_bold, FontWeight.Bold),
)

val DmMono = FontFamily(
    Font(R.font.dm_mono_medium, FontWeight.Medium),
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
