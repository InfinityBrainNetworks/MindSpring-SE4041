package com.mindspring.app.ui.screens.alarm

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindspring.app.data.model.AlertStyle
import com.mindspring.app.reminders.AlertInfo
import com.mindspring.app.reminders.AlertKind
import com.mindspring.app.reminders.AlertTarget
import com.mindspring.app.ui.components.AmbientBackground
import com.mindspring.app.ui.components.GhostButton
import com.mindspring.app.ui.components.OutlinePillButton
import com.mindspring.app.ui.components.PrimaryButton
import com.mindspring.app.ui.preview.PreviewScreen
import com.mindspring.app.ui.preview.PreviewToday
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.LocalAmbientMotion
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.util.Fmt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * The full-screen alarm, shown over the lock screen: the time, the task, and the ways to answer.
 * Like the phone's own clock, it is dismissed with a swipe rather than a tap, so a stray touch in
 * a pocket cannot silence it. [alert] is null while the task loads, or if it has since been removed.
 */
@Composable
fun AlarmScreen(
    alert: AlertInfo?,
    snoozeMinutes: Int,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
    onOpen: () -> Unit,
    now: LocalDateTime? = null,
) {
    val c = MsTheme.colors
    var clock by remember { mutableStateOf(now ?: LocalDateTime.now()) }
    if (now == null) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(1_000)
                clock = LocalDateTime.now()
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AmbientBackground()
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = Dimens.screen, vertical = Dimens.stackMd),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.5f))
            val what = if (alert?.target?.kind == AlertKind.Habit) "HABIT" else "TASK"
            Text(
                if (alert?.style == AlertStyle.Reminder) "$what REMINDER" else "$what ALARM",
                style = MaterialTheme.typography.labelMedium,
                color = c.tealInk,
                letterSpacing = 2.sp,
            )
            Text(
                Fmt.time(clock.toLocalTime()),
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 64.sp, lineHeight = 72.sp),
                fontWeight = FontWeight.Light,
                color = c.textPrimary,
            )
            Text(Fmt.longDay(clock.toLocalDate()), style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)

            Spacer(Modifier.weight(0.6f))
            Text(
                alert?.title ?: "Alarm",
                style = MaterialTheme.typography.headlineSmall,
                color = c.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                alert?.detail ?: "Nothing is left to remind you about here.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
            if (alert != null && !alert.isTest) {
                GhostButton(if (alert.target.kind == AlertKind.Habit) "Open habit" else "Open task", onClick = onOpen)
            }

            Spacer(Modifier.weight(1f))
            if (alert != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinePillButton(
                        "Snooze $snoozeMinutes min",
                        onClick = onSnooze,
                        containerColor = c.card,
                        borderColor = c.cardBorder,
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    )
                    PrimaryButton("Done", onClick = onDone, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
            SwipeToDismiss(onDismiss = onStop)
        }
    }
}

/**
 * A handle that is dragged outwards to dismiss, as on the phone's clock: a disc grows from it
 * with the drag and dismisses once it reaches the outer ring; let go early and it springs back.
 * Screen readers get a plain "Dismiss" action instead of the gesture.
 */
@Composable
private fun SwipeToDismiss(onDismiss: () -> Unit) {
    val c = MsTheme.colors
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val reachPx = with(LocalDensity.current) { 80.dp.toPx() }
    val pull = remember { Animatable(0f) }
    var dismissed by remember { mutableStateOf(false) }
    var drag by remember { mutableStateOf(Offset.Zero) }

    val moving = LocalAmbientMotion.current
    val pulse = if (moving) {
        rememberInfiniteTransition(label = "handle").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2_000, easing = LinearEasing), RepeatMode.Restart),
            label = "handlePulse",
        )
    } else null

    fun dismiss() {
        if (dismissed) return
        dismissed = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onDismiss()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(236.dp)
                .semantics {
                    contentDescription = "Swipe to dismiss"
                    onClick(label = "Dismiss") { dismiss(); true }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { drag = Offset.Zero },
                        onDragEnd = {
                            if (pull.value >= 1f) dismiss() else scope.launch { pull.animateTo(0f, spring(dampingRatio = 0.6f)) }
                        },
                        onDragCancel = { scope.launch { pull.animateTo(0f, spring()) } },
                    ) { change, amount ->
                        change.consume()
                        drag += amount
                        scope.launch {
                            pull.snapTo((drag.getDistance() / reachPx).coerceIn(0f, 1f))
                            if (pull.value >= 1f) dismiss()
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val handle = 38.dp.toPx()
                val outer = handle + reachPx
                // The boundary to reach.
                drawCircle(c.textTertiary.copy(alpha = 0.25f + 0.35f * pull.value), radius = outer, style = Stroke(1.5.dp.toPx()))
                // Idle pulse, inviting the swipe; it gives way while dragging.
                val p = pulse?.value
                if (p != null && pull.value == 0f) {
                    drawCircle(c.tealInk.copy(alpha = (1f - p) * 0.45f), radius = handle + reachPx * 0.8f * p, style = Stroke(2.dp.toPx()))
                }
                // The disc that follows the finger outwards.
                if (pull.value > 0f) drawCircle(c.tealInk.copy(alpha = 0.14f + 0.2f * pull.value), radius = handle + reachPx * pull.value)
            }
            Box(
                Modifier.size(76.dp).scale(1f + 0.08f * pull.value).clip(CircleShape).background(c.card).border(1.dp, c.cardBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, tint = c.textPrimary, modifier = Modifier.size(34.dp))
            }
        }
        Text("Swipe to dismiss", style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
    }
}

// --- Previews -------------------------------------------------------------------------------

private val previewAlert = AlertInfo(
    target = AlertTarget(AlertKind.Task, 5),
    title = "Submit the final APK",
    detail = "Due today · Mobile App Assignment",
    style = AlertStyle.Alarm,
    at = PreviewToday.atTime(19, 30),
)

@Preview(name = "Alarm", showBackground = true, widthDp = 393, heightDp = 830)
@Composable
private fun AlarmScreenPreview() = PreviewScreen {
    AlarmScreen(previewAlert, snoozeMinutes = 10, onDone = {}, onSnooze = {}, onStop = {}, onOpen = {}, now = PreviewToday.atTime(19, 30))
}

@Preview(name = "Alarm · dark", showBackground = true, widthDp = 393, heightDp = 830)
@Composable
private fun AlarmScreenDarkPreview() = PreviewScreen(dark = true) {
    AlarmScreen(previewAlert, snoozeMinutes = 10, onDone = {}, onSnooze = {}, onStop = {}, onOpen = {}, now = PreviewToday.atTime(19, 30))
}

@Preview(name = "Reminder on lock screen", showBackground = true, widthDp = 393, heightDp = 830)
@Composable
private fun ReminderScreenPreview() = PreviewScreen {
    AlarmScreen(
        previewAlert.copy(style = AlertStyle.Reminder, title = "Room database and repositories", detail = "Due tomorrow · Mobile App Assignment"),
        snoozeMinutes = 10, onDone = {}, onSnooze = {}, onStop = {}, onOpen = {}, now = PreviewToday.atTime(9, 0),
    )
}
