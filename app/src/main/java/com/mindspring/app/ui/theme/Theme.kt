package com.mindspring.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Spacing from DESIGN.md: an 8dp grid with a generous 24dp screen gutter. */
object Dimens {
    val screen = 24.dp
    val card = 20.dp
    val gutter = 16.dp
    val stackSm = 8.dp
    val stackMd = 16.dp
    val stackLg = 32.dp
}

val CardShape = RoundedCornerShape(20.dp)
val InputShape = RoundedCornerShape(8.dp)

private val MsShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = InputShape,
    medium = RoundedCornerShape(12.dp),
    large = CardShape,
    extraLarge = RoundedCornerShape(28.dp),
)

private val LocalMsColors = staticCompositionLocalOf { LightMsColors }

object MsTheme {
    val colors: MsColors
        @Composable @ReadOnlyComposable get() = LocalMsColors.current
}

private fun MsColors.toMaterial() = if (isDark) {
    darkColorScheme(
        primary = tealInk, onPrimary = Color(0xFF00201D),
        primaryContainer = teal, onPrimaryContainer = onTeal,
        secondary = amber, onSecondary = onAmber,
        background = canvas, onBackground = textPrimary,
        surface = canvas, onSurface = textPrimary,
        surfaceVariant = cardMuted, onSurfaceVariant = textSecondary,
        surfaceContainerLowest = card, surfaceContainerLow = cardSubtle,
        surfaceContainer = card, surfaceContainerHigh = card, surfaceContainerHighest = cardMuted,
        outline = textTertiary, outlineVariant = divider,
        error = danger, onError = Color(0xFF690005),
    )
} else {
    lightColorScheme(
        primary = tealInk, onPrimary = onTeal,
        primaryContainer = Color(0xFFB2EEE7), onPrimaryContainer = Color(0xFF00201D),
        secondary = amber, onSecondary = onAmber,
        background = canvas, onBackground = textPrimary,
        surface = canvas, onSurface = textPrimary,
        surfaceVariant = cardMuted, onSurfaceVariant = textSecondary,
        surfaceContainerLowest = card, surfaceContainerLow = cardSubtle,
        surfaceContainer = card, surfaceContainerHigh = card, surfaceContainerHighest = cardMuted,
        outline = textTertiary, outlineVariant = Color(0xFFBFC8C6),
        error = danger, onError = Color.White,
    )
}

@Composable
fun MindSpringTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkMsColors else LightMsColors
    CompositionLocalProvider(LocalMsColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = MsTypography,
            shapes = MsShapes,
            content = content,
        )
    }
}
