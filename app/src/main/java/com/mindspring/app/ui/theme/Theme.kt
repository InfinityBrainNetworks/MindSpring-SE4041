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
import androidx.compose.ui.graphics.Brush
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

/** Whether decorative motion (the drifting background, idle pulses) runs. Off in tests and when the user turns it off. */
val LocalAmbientMotion = staticCompositionLocalOf { true }

object MsTheme {
    val colors: MsColors
        @Composable @ReadOnlyComposable get() = LocalMsColors.current

    /** The teal chrome: a gentle diagonal from deep to bright teal. */
    val chrome: Brush
        @Composable @ReadOnlyComposable get() = Brush.linearGradient(listOf(TealDeep, Teal, TealBright))
}

private fun MsColors.toMaterial() = if (isDark) {
    darkColorScheme(
        primary = tealInk, onPrimary = Color(0xFF00201D),
        primaryContainer = teal, onPrimaryContainer = onTeal,
        secondary = amber, onSecondary = onAmber,
        background = canvas, onBackground = textPrimary,
        surface = Color(0xFF1E2826), onSurface = textPrimary,
        surfaceVariant = cardMuted, onSurfaceVariant = textSecondary,
        surfaceContainerLowest = Color(0xFF1E2826), surfaceContainerLow = cardSubtle,
        surfaceContainer = Color(0xFF26312E), surfaceContainerHigh = Color(0xFF26312E), surfaceContainerHighest = cardMuted,
        outline = textTertiary, outlineVariant = divider,
        error = danger, onError = Color(0xFF690005),
    )
} else {
    lightColorScheme(
        primary = tealInk, onPrimary = onTeal,
        primaryContainer = Color(0xFFB2EEE7), onPrimaryContainer = Color(0xFF00201D),
        secondary = amber, onSecondary = onAmber,
        background = canvas, onBackground = textPrimary,
        surface = Color.White, onSurface = textPrimary,
        surfaceVariant = cardMuted, onSurfaceVariant = textSecondary,
        surfaceContainerLowest = Color.White, surfaceContainerLow = cardSubtle,
        surfaceContainer = Color.White, surfaceContainerHigh = Color(0xFFF7F9F6), surfaceContainerHighest = cardMuted,
        outline = textTertiary, outlineVariant = Color(0xFFBFC8C6),
        error = danger, onError = Color.White,
    )
}

@Composable
fun MindSpringTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    ambientMotion: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkMsColors else LightMsColors
    CompositionLocalProvider(LocalMsColors provides colors, LocalAmbientMotion provides ambientMotion) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = MsTypography,
            shapes = MsShapes,
            content = content,
        )
    }
}
