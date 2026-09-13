package com.mindspring.app.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.data.model.MarkState
import com.mindspring.app.domain.HabitStats
import com.mindspring.app.domain.HabitUnit
import com.mindspring.app.domain.Tally
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.AnimatedCount
import com.mindspring.app.ui.components.FrequencyPill
import com.mindspring.app.ui.components.GhostButton
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.MsProgressBar
import com.mindspring.app.ui.components.OutlinePillButton
import com.mindspring.app.ui.components.Pill
import com.mindspring.app.ui.components.PrimaryButton
import com.mindspring.app.ui.components.TealTopBar
import com.mindspring.app.ui.components.Tint
import com.mindspring.app.ui.components.appear
import com.mindspring.app.ui.preview.PreviewAreas
import com.mindspring.app.ui.preview.PreviewHabits
import com.mindspring.app.ui.preview.PreviewScreen
import com.mindspring.app.ui.preview.PreviewToday
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.theme.areaColor
import com.mindspring.app.ui.util.Fmt
import java.time.DayOfWeek
import kotlin.math.roundToInt

@Composable
fun HabitDetailScreen(
    habitId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCompleted: () -> Unit,
) {
    val vm = appViewModel { HabitDetailViewModel(it, habitId) }
    val state by vm.state.collectAsStateWithLifecycle()

    HabitDetailContent(
        state = state,
        onSetToday = vm::setToday,
        onSetActive = vm::setActive,
        onDelete = vm::delete,
        onBack = onBack,
        onEdit = onEdit,
        onCompleted = onCompleted,
    )
}

/**
 * The screen as pure state and callbacks, so it renders in a @Preview without a ViewModel behind
 * it. [HabitDetailScreen] is the thin wrapper that supplies both from the app's data.
 */
@Composable
fun HabitDetailContent(
    state: HabitDetailState?,
    onSetToday: (MarkState?) -> Unit,
    onSetActive: (Boolean) -> Unit,
    onDelete: (onDeleted: () -> Unit) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCompleted: () -> Unit,
) {
    val c = MsTheme.colors
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TealTopBar(title = state?.habit?.name ?: "", onNavigate = onBack)
        val s = state ?: return@Column
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = Dimens.screen, vertical = Dimens.stackLg - 8.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
        ) {
            StreakHero(s, Modifier.appear(0))

            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).appear(1), horizontalArrangement = Arrangement.spacedBy(Dimens.gutter)) {
                StatTile("This month", Icons.Rounded.DonutLarge, c.tealInk, Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedCount((s.month.rate * 100).roundToInt(), style = MaterialTheme.typography.headlineMedium, color = c.tealInk, suffix = "%", fontWeight = FontWeight.Bold)
                    Text(
                        if (s.month.possible == 0) "Nothing due yet" else "${s.month.done} of ${s.month.possible} ${s.unit.plural}",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textTertiary,
                    )
                    Spacer(Modifier.height(12.dp))
                    MsProgressBar(s.month.rate)
                }
                StatTile("Best streak", Icons.Rounded.EmojiEvents, c.amber, Modifier.weight(1f).fillMaxHeight()) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        AnimatedCount(s.bestStreak, style = MaterialTheme.typography.headlineMedium, color = c.textPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Text(if (s.bestStreak == 1) s.unit.singular else s.unit.plural, style = MaterialTheme.typography.titleMedium, color = c.textTertiary, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Text("Personal record", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                }
            }
            StatTile("Total", Icons.Rounded.CalendarMonth, c.tealInk, Modifier.fillMaxWidth().appear(2)) {
                AnimatedCount(s.totalDone, style = MaterialTheme.typography.headlineMedium, color = c.textPrimary, fontWeight = FontWeight.Bold)
                Text("Times done since ${Fmt.dayMonth(s.habit.createdAt)}", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
            }

            ActivityCard(s, Modifier.appear(3))

            Spacer(Modifier.height(Dimens.stackSm))
            if (s.habit.active && s.belongsToday) {
                when (s.todayMark) {
                    MarkState.Done -> OutlinePillButton(
                        "Completed today · tap to undo",
                        leadingIcon = Icons.Rounded.CheckCircle,
                        onClick = { onSetToday(null) },
                        contentColor = c.tealInk,
                        borderColor = c.tealInk.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    )
                    MarkState.Skipped -> OutlinePillButton(
                        "Skipped today · tap to undo",
                        onClick = { onSetToday(null) },
                        contentColor = c.textSecondary,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    )
                    null -> {
                        PrimaryButton(
                            "Complete for Today",
                            leadingIcon = Icons.Rounded.CheckCircle,
                            onClick = { onSetToday(MarkState.Done); onCompleted() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        GhostButton(
                            "Skip today (doesn't count against you)",
                            onClick = { onSetToday(MarkState.Skipped) },
                            color = c.textSecondary,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gutter)) {
                OutlinePillButton(
                    "Edit",
                    leadingIcon = Icons.Rounded.Edit,
                    onClick = onEdit,
                    containerColor = c.cardMuted,
                    borderColor = Color.Transparent,
                    modifier = Modifier.weight(1f),
                )
                OutlinePillButton(
                    "Delete",
                    leadingIcon = Icons.Rounded.Delete,
                    onClick = { confirmDelete = true },
                    contentColor = c.danger,
                    borderColor = c.danger.copy(alpha = 0.35f),
                    modifier = Modifier.weight(1f),
                )
            }
            GhostButton(
                if (s.habit.active) "Retire this habit (keeps its history)" else "Bring this habit back",
                onClick = { onSetActive(!s.habit.active) },
                color = c.tealInk,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(Dimens.stackMd))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this habit?") },
            text = { Text("This removes the habit and its whole history. To stop tracking it but keep the history, retire it instead.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(onBack) }) { Text("Delete", color = c.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StreakHero(s: HabitDetailState, modifier: Modifier) {
    val c = MsTheme.colors
    MsCard(modifier.fillMaxWidth(), color = c.hero, horizontalAlignment = Alignment.CenterHorizontally) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FrequencyPill(s.habit.frequency)
            s.area?.let { Pill(it.name, Tint(areaColor(it.colorIndex).copy(alpha = 0.3f), Color.White)) }
            if (s.habit.subArea.isNotBlank()) Pill(s.habit.subArea, Tint(Color.White.copy(alpha = 0.12f), c.onTeal))
            if (s.habit.target.isNotBlank()) Pill(s.habit.target, Tint(Color.White.copy(alpha = 0.12f), c.onTeal))
            if (!s.habit.active) Pill("Retired", Tint(Color.White.copy(alpha = 0.2f), c.onTeal))
        }
        Spacer(Modifier.height(Dimens.stackMd))
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).border(4.dp, c.amber.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = if (s.streak > 0) c.amber else c.onTealMuted, modifier = Modifier.size(60.dp))
        }
        Spacer(Modifier.height(Dimens.stackSm))
        Text(
            "${s.streak} ${(if (s.streak == 1) s.unit.singular else s.unit.plural).replaceFirstChar { it.uppercase() }} Streak",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = c.amber,
        )
        Text(
            when {
                s.streak == 0 -> "Complete it to start a streak"
                s.streak >= s.bestStreak && s.streak > 1 -> "Your best run yet. Keep it going!"
                else -> "Keep it going!"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = c.onTealMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatTile(label: String, icon: ImageVector, iconTint: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = MsTheme.colors
    MsCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = c.textSecondary)
        }
        Spacer(Modifier.height(Dimens.stackMd))
        content()
    }
}

/** Last five weeks, Monday-first. Amber dots mark the days in the current streak. */
@Composable
private fun ActivityCard(s: HabitDetailState, modifier: Modifier) {
    val c = MsTheme.colors
    // A missed day is a mid-tone, never red: missing a day is not a failure state.
    val missedColor = c.tealInk.copy(alpha = 0.32f)
    MsCard(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Activity", style = MaterialTheme.typography.titleLarge, color = c.textPrimary, modifier = Modifier.weight(1f))
            Text(
                "Last 5 weeks",
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary,
                modifier = Modifier.clip(CircleShape).background(c.cardMuted).padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DayOfWeek.entries.forEach {
                Text(it.name.take(1), style = MaterialTheme.typography.labelSmall, color = c.textTertiary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        s.grid.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { (date, cell) ->
                    val shape = RoundedCornerShape(6.dp)
                    val base = Modifier.weight(1f).aspectRatio(1f).clip(shape)
                    val styled = when (cell) {
                        DayCell.Done -> base.background(c.tealInk)
                        DayCell.Missed -> base.background(missedColor)
                        DayCell.Skipped -> base.background(c.cardMuted).border(1.dp, c.textTertiary.copy(alpha = 0.35f), shape)
                        DayCell.Rest, DayCell.Open -> base.background(c.cardSubtle)
                        DayCell.Future -> base.border(1.dp, c.divider, shape)
                        DayCell.Before -> base
                    }
                    Box(styled, contentAlignment = Alignment.Center) {
                        if (date in s.streakDays) Box(Modifier.size(7.dp).clip(CircleShape).background(c.amber))
                        if (cell == DayCell.Skipped) Text("–", style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Legend(c.tealInk, "Done")
            if (s.habit.frequency.isDayBased) Legend(missedColor, "Missed")
            Legend(c.cardMuted, "Skipped")
            Legend(c.cardSubtle, if (s.habit.frequency.isDayBased) "Day off" else "Any day")
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MsTheme.colors.textTertiary)
    }
}

// --- Previews -------------------------------------------------------------------------------

/** Five Monday-first weeks ending with the sample week: mostly done, a skip and a miss for texture. */
private val previewDetailState: HabitDetailState = run {
    val firstMonday = HabitStats.weekStart(PreviewToday).minusWeeks(4)
    val grid = (0L until 5L).map { w ->
        (0L until 7L).map { d ->
            val date = firstMonday.plusWeeks(w).plusDays(d)
            date to when {
                date.isAfter(PreviewToday) -> DayCell.Future
                date == PreviewToday -> DayCell.Open
                (w * 7 + d) % 11 == 4L -> DayCell.Missed
                (w * 7 + d) % 9 == 2L -> DayCell.Skipped
                else -> DayCell.Done
            }
        }
    }
    HabitDetailState(
        habit = PreviewHabits[1],
        area = PreviewAreas[0],
        unit = HabitUnit.Day,
        streak = 12,
        bestStreak = 21,
        month = Tally(10, 13),
        totalDone = 48,
        todayMark = null,
        belongsToday = true,
        grid = grid,
        streakDays = (1L..12L).map { PreviewToday.minusDays(it) }.toSet(),
    )
}

@Preview(name = "Habit detail", showBackground = true, widthDp = 393, heightDp = 1250)
@Composable
private fun HabitDetailPreview() = PreviewScreen {
    HabitDetailContent(
        state = previewDetailState, onSetToday = {}, onSetActive = {}, onDelete = {},
        onBack = {}, onEdit = {}, onCompleted = {},
    )
}

@Preview(name = "Habit detail · done today", showBackground = true, widthDp = 393, heightDp = 1250)
@Composable
private fun HabitDetailDonePreview() = PreviewScreen {
    HabitDetailContent(
        state = previewDetailState.copy(todayMark = MarkState.Done, streak = 13),
        onSetToday = {}, onSetActive = {}, onDelete = {}, onBack = {}, onEdit = {}, onCompleted = {},
    )
}

@Preview(name = "Habit detail · dark", showBackground = true, widthDp = 393, heightDp = 1250)
@Composable
private fun HabitDetailDarkPreview() = PreviewScreen(dark = true) {
    HabitDetailContent(
        state = previewDetailState, onSetToday = {}, onSetActive = {}, onDelete = {},
        onBack = {}, onEdit = {}, onCompleted = {},
    )
}
