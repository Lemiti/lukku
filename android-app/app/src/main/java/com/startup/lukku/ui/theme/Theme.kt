package com.startup.lukku.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RoosterRed   = Color(0xFFE53935)
private val DeepBlack    = Color(0xFF0A0A0A)
private val SurfaceGray  = Color(0xFF111111)

private val DarkColorScheme = darkColorScheme(
    primary        = RoosterRed,
    onPrimary      = Color.White,
    background     = DeepBlack,
    onBackground   = Color.White,
    surface        = SurfaceGray,
    onSurface      = Color.White,
)

@Composable
fun LukkuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content     = content
    )
}
