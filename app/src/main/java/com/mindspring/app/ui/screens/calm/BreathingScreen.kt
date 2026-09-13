package com.mindspring.app.ui.screens.calm

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mindspring.app.ui.components.PrimaryButton
import com.mindspring.app.ui.preview.PreviewScreen
import com.mindspring.app.ui.theme.Amber
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.util.Fmt

private enum class Phase(val label: String) { In("Breathe in"), HoldFull("Hold"), Out("Breathe out"), HoldEmpty("Hold") }

private data class Technique(val title: String, val subtitle: String, val steps: List<Pair<Phase, Int>>) {
    val cycleMs: Long = steps.sumOf { it.second } * 1000L
}

private val CalmTechnique = Technique("Calm Breathing", "In for 4, hold for 2, out for 6", listOf(Phase.In to 4, Phase.HoldFull to 2, Phase.Out to 6))
private val BoxTechnique = Technique("Box Breathing", "4-4-4-4 technique for focus", listOf(Phase.In to 4, Phase.HoldFull to 4, Phase.Out to 4, Phase.HoldEmpty to 4))

private val OnDark = Color(0xFFE9F3EF)

/**
 * Immersive breathing session. The orb's size is the pacing mechanism itself: it grows on the
 * inhale, rests on holds and shrinks on the exhale. Navigation chrome is hidden.
 */
@Composable
fun BreathingScreen(technique: String, onClose: () -> Unit) {
    val t = if (technique == TECHNIQUE_BOX) BoxTechnique else CalmTechnique
    var minutes by rememberSaveable { mutableIntStateOf(3) }
    var elapsed by rememberSaveable { mutableLongStateOf(0L) }
    var running by rememberSaveable { mutableStateOf(true) }
    val totalMs = minutes * 60_000L
    val finished = elapsed >= totalMs

    LaunchedEffect(running, minutes) {
        if (!running) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (elapsed < totalMs) {
            withFrameMillis { now ->
                elapsed = (elapsed + (now - last)).coerceAtMost(totalMs)
                last = now
            }
        }
    }

    // Where we are inside the current breath cycle.
    var inCycle = elapsed % t.cycleMs
    var phase = t.steps.first().first
    var phaseProgress = 0f
    for ((p, secs) in t.steps) {
        val len = secs * 1000L
        if (inCycle < len) {
            phase = p
            phaseProgress = inCycle.toFloat() / len
            break
        }
        inCycle -= len
    }
    val eased = FastOutSlowInEasing.transform(phaseProgress)
    val orb = when (phase) {
        Phase.In -> 0.55f + 0.45f * eased
        Phase.HoldFull -> 1f
        Phase.Out -> 1f - 0.45f * eased
        Phase.HoldEmpty -> 0.55f
    }
    val remaining = ((totalMs - elapsed + 999) / 1000)

    val restart = { elapsed = 0L; running = true }
    val setMinutes = { m: Int -> minutes = m; elapsed = 0L; running = true }

    val background = if (t === BoxTechnique) {
        Brush.verticalGradient(listOf(Color(0xFF16201E), Color(0xFF0F1715)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF10514C), Color(0xFF0D403C), Color(0xFF0A312E)))
    }

    Column(
        Modifier.fillMaxSize().background(background).statusBarsPadding().navigationBarsPadding().padding(Dimens.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose, modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = OnDark)
            }
            Spacer(Modifier.weight(1f))
            listOf(1, 3, 5).forEach { m ->
                DurationChip("$m min", selected = m == minutes, onClick = { setMinutes(m) })
            }
        }

        Spacer(Modifier.height(Dimens.stackLg))
        Text(t.title, style = MaterialTheme.typography.headlineMedium, color = OnDark)
        Text(t.subtitle, style = MaterialTheme.typography.bodyMedium, color = OnDark.copy(alpha = 0.6f))

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (finished) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.SelfImprovement, contentDescription = null, tint = Amber, modifier = Modifier.size(72.dp))
                    Spacer(Modifier.height(Dimens.stackMd))
                    Text("Session complete", style = MaterialTheme.typography.headlineSmall, color = OnDark)
                    Text(
                        "Well done. Take that calm with you.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnDark.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
            } else if (t === BoxTechnique) {
                Orb(orb, boxStyle = true)
                PhaseText(phase, big = false)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PhaseText(phase, big = true)
                    Spacer(Modifier.height(Dimens.stackMd))
                    Orb(orb, boxStyle = false)
                }
            }
        }

        Spacer(Modifier.height(Dimens.stackSm))
        Text(Fmt.countdown(remaining), style = MaterialTheme.typography.titleLarge, color = OnDark)
        Spacer(Modifier.height(Dimens.stackLg))

        when {
            finished -> PrimaryButton("Done", onClick = onClose, modifier = Modifier.fillMaxWidth())
            t === BoxTechnique -> Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gutter), verticalAlignment = Alignment.CenterVertically) {
                RoundControl(Icons.Rounded.Replay, "Restart", 60, Color.Transparent, onClick = restart)
                RoundControl(
                    if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (running) "Pause" else "Resume", 76, Color(0xFF2E7D74),
                ) { running = !running }
                RoundControl(Icons.Rounded.Close, "End session", 60, Color.Transparent, onClick = onClose)
            }
            else -> Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gutter)) {
                OutlinedButton(
                    onClick = { running = !running },
                    border = BorderStroke(1.dp, OnDark.copy(alpha = 0.4f)),
                    shape = CircleShape,
                ) { Text(if (running) "Pause" else "Resume", color = OnDark, style = MaterialTheme.typography.labelLarge) }
                OutlinedButton(
                    onClick = onClose,
                    border = BorderStroke(1.dp, OnDark.copy(alpha = 0.4f)),
                    shape = CircleShape,
                ) { Text("End session", color = OnDark, style = MaterialTheme.typography.labelLarge) }
            }
        }
        Spacer(Modifier.height(Dimens.stackSm))
    }
}

@Composable
private fun PhaseText(phase: Phase, big: Boolean) {
    AnimatedContent(phase, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "phase") { p ->
        Text(
            p.label,
            style = if (big) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleLarge,
            color = if (big) OnDark else Color.White,
        )
    }
}

/** Concentric rings around a glowing orb, all scaled by the breath. */
@Composable
private fun Orb(scale: Float, boxStyle: Boolean) {
    Canvas(Modifier.size(280.dp)) {
        val r = size.minDimension / 2
        if (boxStyle) {
            drawCircle(Color(0xFF2E7D74).copy(alpha = 0.5f), radius = r * 0.9f, style = Stroke(2.dp.toPx()))
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFF2E7D74).copy(alpha = 0.55f), Color.Transparent), radius = r * 0.9f * scale),
                radius = r * 0.9f * scale,
            )
        } else {
            listOf(1f to 0.2f, 0.75f to 0.4f, 0.5f to 0.6f).forEach { (f, a) ->
                drawCircle(Amber.copy(alpha = a), radius = r * f * (0.85f + 0.15f * scale), style = Stroke(1.5.dp.toPx()))
            }
            drawCircle(Brush.radialGradient(listOf(Amber.copy(alpha = 0.35f), Color.Transparent), radius = r * 0.6f * scale), radius = r * 0.6f * scale)
            drawCircle(Amber, radius = r * 0.36f * scale)
        }
    }
}

@Composable
private fun DurationChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) Color.White.copy(alpha = 0.2f) else Color.Transparent,
        border = if (selected) null else BorderStroke(1.dp, OnDark.copy(alpha = 0.3f)),
        modifier = Modifier.padding(start = 8.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = OnDark, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}

@Composable
private fun RoundControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    sizeDp: Int,
    container: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        border = if (container == Color.Transparent) BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)) else null,
        modifier = Modifier.size(sizeDp.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size((sizeDp * 0.42f).dp))
        }
    }
}

// --- Previews -------------------------------------------------------------------------------

@Preview(name = "Breathing · box", showBackground = true, widthDp = 393, heightDp = 830)
@Composable
private fun BreathingScreenPreview() = PreviewScreen {
    BreathingScreen(technique = TECHNIQUE_BOX, onClose = {})
}

@Preview(name = "Breathing · calm", showBackground = true, widthDp = 393, heightDp = 830)
@Composable
private fun BreathingCalmPreview() = PreviewScreen(dark = true) {
    BreathingScreen(technique = TECHNIQUE_CALM, onClose = {})
}
