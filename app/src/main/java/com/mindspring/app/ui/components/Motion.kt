package com.mindspring.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mindspring.app.ui.theme.LocalAmbientMotion
import com.mindspring.app.ui.theme.MsTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class Blob(val x: Float, val y: Float, val radius: Float, val dx: Float, val dy: Float, val fx: Int, val fy: Int, val alpha: Float)

// Whole-number frequencies make every orbit close exactly once per cycle, so the loop is seamless.
private val Blobs = listOf(
    Blob(0.15f, 0.12f, 0.62f, 0.16f, 0.07f, 1, 2, 0.75f),
    Blob(0.92f, 0.30f, 0.55f, 0.10f, 0.12f, 2, 1, 0.55f),
    Blob(0.20f, 0.72f, 0.60f, 0.14f, 0.09f, 1, 1, 0.6f),
    Blob(0.85f, 0.92f, 0.58f, 0.12f, 0.08f, 1, 2, 0.65f),
)

/**
 * The calm backdrop behind every screen: four soft light sources drifting on slow closed loops
 * over the canvas colour. The animated value is read only while drawing, so the drift never
 * recomposes anything - it just redraws one layer. Holds still when ambient motion is off.
 */
@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    val c = MsTheme.colors
    val moving = LocalAmbientMotion.current
    val phase = if (moving) {
        rememberInfiniteTransition(label = "ambient").animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(48_000, easing = LinearEasing)),
            label = "ambientPhase",
        )
    } else null
    Canvas(modifier.fillMaxSize()) {
        drawRect(c.canvas)
        val t = phase?.value ?: 0.9f
        Blobs.forEachIndexed { i, b ->
            val center = Offset(
                size.width * (b.x + b.dx * sin(t * b.fx + i * 1.3f)),
                size.height * (b.y + b.dy * cos(t * b.fy + i * 0.7f)),
            )
            val radius = size.maxDimension * b.radius
            val color = c.ambient[i % c.ambient.size]
            drawCircle(
                brush = Brush.radialGradient(
                    0f to color.copy(alpha = b.alpha),
                    0.55f to color.copy(alpha = b.alpha * 0.35f),
                    1f to Color.Transparent,
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }
    }
}

/**
 * Fades and lifts content in when it first appears, staggered by [index] so a column of cards
 * arrives as a soft cascade. Runs once per placement, not on every recomposition.
 */
@Composable
fun Modifier.appear(index: Int = 0): Modifier {
    val progress = remember { Animatable(0f) }
    var played by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (played) {
            progress.snapTo(1f)
        } else {
            progress.animateTo(1f, tween(420, delayMillis = 40 * index.coerceAtMost(8), easing = FastOutSlowInEasing))
            played = true
        }
    }
    val lift = with(LocalDensity.current) { 14.dp.toPx() }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * lift
    }
}

/** A number that counts up or down to its new value instead of jumping. */
@Composable
fun AnimatedCount(
    value: Int,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    suffix: String = "",
    fontWeight: FontWeight? = null,
) {
    val shown by animateIntAsState(value, tween(700, easing = FastOutSlowInEasing), label = "count")
    Text("$shown$suffix", style = style, color = color, modifier = modifier, fontWeight = fontWeight)
}

/** Circular progress that sweeps to its value; [content] sits in the middle. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    stroke: Dp = 7.dp,
    color: Color = MsTheme.colors.tealInk,
    trackColor: Color = MsTheme.colors.cardMuted,
    content: @Composable () -> Unit = {},
) {
    val sweep by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            drawArc(trackColor, 0f, 360f, false, style = Stroke(w), topLeft = Offset(w / 2, w / 2), size = this.size.copy(this.size.width - w, this.size.height - w))
            if (sweep > 0f) {
                drawArc(color, -90f, 360f * sweep, false, style = Stroke(w, cap = StrokeCap.Round), topLeft = Offset(w / 2, w / 2), size = this.size.copy(this.size.width - w, this.size.height - w))
            }
        }
        content()
    }
}

/** What a tick button shows. */
enum class TickState { Empty, Done, Skipped, Off }

/**
 * The round tick used for habits and tasks. Completing it pops with a spring and fills; a
 * skipped day shows a dash; an off day is a faint outline that cannot be ticked.
 */
@Composable
fun TickButton(
    state: TickState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    doneColor: Color = MsTheme.colors.tealInk,
    onLongClick: (() -> Unit)? = null,
    contentDescription: String? = null,
) {
    val c = MsTheme.colors
    val scale by animateFloatAsState(
        if (state == TickState.Done) 1f else 0.94f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tickScale",
    )
    val fill by animateColorAsState(
        when (state) {
            TickState.Done -> doneColor
            TickState.Skipped -> c.cardMuted
            else -> Color.Transparent
        },
        label = "tickFill",
    )
    val border = when (state) {
        TickState.Empty -> c.textTertiary.copy(alpha = 0.6f)
        TickState.Off -> c.divider.copy(alpha = 0.6f)
        else -> Color.Transparent
    }
    Box(
        modifier
            .scale(scale)
            .size(size)
            .clip(CircleShape)
            .background(fill)
            .border(1.5.dp, border, CircleShape)
            .then(
                if (state == TickState.Off) Modifier
                else if (onLongClick != null) Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                else Modifier.clickable(onClick = onClick),
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            TickState.Done -> Icon(Icons.Rounded.Check, contentDescription ?: "Done", tint = if (c.isDark) Color(0xFF0D2623) else Color.White, modifier = Modifier.size(size * 0.62f))
            TickState.Skipped -> Icon(Icons.Rounded.Remove, contentDescription ?: "Skipped", tint = c.textSecondary, modifier = Modifier.size(size * 0.6f))
            else -> Unit
        }
    }
}
