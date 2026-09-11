package com.mindspring.app.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.MsProgressBar
import com.mindspring.app.ui.components.OutlinePillButton
import com.mindspring.app.ui.components.PrimaryButton
import com.mindspring.app.ui.components.TealTopBar
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme
import java.time.DayOfWeek
import kotlin.math.roundToInt

@Composable
fun HabitDetailScreen(
    habitId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCompleted: () -> Unit,
) {
    val vm = appViewModel { HabitDetailViewModel(it.habits, habitId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val c = MsTheme.colors
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(c.canvas)) {
        TealTopBar(title = state?.habit?.name ?: "", onNavigate = onBack)
        val s = state ?: return@Column
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = Dimens.screen, vertical = Dimens.stackLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
        ) {
            StreakHero(s)

            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Dimens.gutter)) {
                StatTile("Completion", Icons.Rounded.DonutLarge, c.tealInk, Modifier.weight(1f).fillMaxHeight()) {
                    Text("${(s.monthRate * 100).roundToInt()}%", style = MaterialTheme.typography.headlineMedium, color = c.tealInk, fontWeight = FontWeight.Bold)
                    Text("This month", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    MsProgressBar(s.monthRate)
                }
                StatTile("Best Streak", Icons.Rounded.EmojiEvents, c.amber, Modifier.weight(1f).fillMaxHeight()) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${s.bestStreak}", style = MaterialTheme.typography.headlineMedium, color = c.textPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Text("days", style = MaterialTheme.typography.titleMedium, color = c.textTertiary, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Text("Personal record", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                }
            }
            StatTile("Total Days", Icons.Rounded.CalendarMonth, c.tealInk, Modifier.fillMaxWidth()) {
                Text("${s.totalDays}", style = MaterialTheme.typography.headlineMedium, color = c.textPrimary, fontWeight = FontWeight.Bold)
                Text("Completed since you started", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
            }

            ActivityCard(s)

            Spacer(Modifier.height(Dimens.stackSm))
            if (s.doneToday) {
                OutlinePillButton(
                    "Completed today · tap to undo",
                    leadingIcon = Icons.Rounded.CheckCircle,
                    onClick = { vm.setDoneToday(false) },
                    contentColor = c.tealInk,
                    borderColor = c.tealInk.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                )
            } else {
                PrimaryButton(
                    "Complete for Today",
                    leadingIcon = Icons.Rounded.CheckCircle,
                    onClick = { vm.setDoneToday(true); onCompleted() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gutter)) {
                OutlinePillButton(
                    "Edit Habit",
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
            Spacer(Modifier.height(Dimens.stackMd))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this habit?") },
            text = { Text("This removes the habit and its whole completion history. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete(onBack) }) {
                    Text("Delete", color = c.danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StreakHero(s: HabitDetailState) {
    val c = MsTheme.colors
    MsCard(Modifier.fillMaxWidth(), color = c.hero, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(s.habit.name, style = MaterialTheme.typography.headlineMedium, color = c.onTeal.copy(alpha = 0.85f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(Dimens.stackMd))
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(4.dp, c.amber.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = if (s.streak > 0) c.amber else c.onTealMuted,
                modifier = Modifier.size(60.dp),
            )
        }
        Spacer(Modifier.height(Dimens.stackSm))
        Text(
            if (s.streak == 1) "1 Day Streak" else "${s.streak} Day Streak",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = c.amber,
        )
        Text(
            when {
                s.streak == 0 -> "Complete today to start a streak"
                s.streak >= s.bestStreak && s.streak > 1 -> "Your best run yet. Keep it burning!"
                else -> "Keep it burning!"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = c.onTealMuted,
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
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
private fun ActivityCard(s: HabitDetailState) {
    val c = MsTheme.colors
    // A missed day is a mid-tone, never red: missing a day is not a failure state.
    val missedColor = c.tealInk.copy(alpha = if (c.isDark) 0.3f else 0.32f)
    MsCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                Text(
                    it.name.take(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
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
                        DayCell.Rest -> base.background(c.cardSubtle)
                        DayCell.Future -> base.border(1.dp, c.divider, shape)
                        DayCell.Before -> base
                    }
                    Box(styled, contentAlignment = Alignment.Center) {
                        if (date in s.streakDays) Box(Modifier.size(7.dp).clip(CircleShape).background(c.amber))
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Legend(c.tealInk, "Done")
            Legend(missedColor, "Missed")
            Legend(c.cardSubtle, "Rest day")
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
