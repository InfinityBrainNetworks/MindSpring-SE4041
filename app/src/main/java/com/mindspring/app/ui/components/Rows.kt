package com.mindspring.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.model.Repeat
import com.mindspring.app.data.model.Task
import com.mindspring.app.data.model.TaskStatus
import com.mindspring.app.domain.HabitUnit
import com.mindspring.app.domain.TaskFlag
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.theme.areaColor
import com.mindspring.app.ui.util.Fmt
import java.time.LocalDate

/**
 * One habit on a list. Tap the tick to complete, long-press it to skip (the workbook's dash);
 * tap the row to open the habit. Rows are compact so a long list of habits stays scannable.
 */
@Composable
fun HabitRow(
    habit: Habit,
    tick: TickState,
    onTick: () -> Unit,
    onSkip: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    streak: Int = 0,
    unit: HabitUnit = HabitUnit.Day,
    areaColorIndex: Int? = null,
    note: String? = null,
) {
    val c = MsTheme.colors
    val done = tick == TickState.Done
    val bg by animateColorAsState(if (done) c.cardSubtle.copy(alpha = 0.7f) else Color.Transparent, label = "habitRowBg")
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onSkip)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(areaColorIndex?.let { areaColor(it).copy(alpha = 0.18f) } ?: c.cardMuted), contentAlignment = Alignment.Center) {
            Icon(habit.icon.vector, contentDescription = null, tint = areaColorIndex?.let { areaColor(it) } ?: c.tealInk, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                habit.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (done) c.textSecondary else c.textPrimary,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = buildList {
                note?.let(::add)
                if (habit.target.isNotBlank()) add(habit.target)
                if (!habit.frequency.isDayBased) add(habit.frequency.label)
            }
            if (meta.isNotEmpty() || streak > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (meta.isNotEmpty()) {
                        Text(meta.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = c.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    }
                    if (streak > 0) {
                        if (meta.isNotEmpty()) Spacer(Modifier.width(8.dp))
                        Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = c.amber, modifier = Modifier.size(13.dp))
                        Text(
                            if (unit == HabitUnit.Day) "$streak" else "$streak ${unit.singular.take(2)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textSecondary,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        TickButton(tick, onClick = onTick, onLongClick = onSkip, contentDescription = if (done) "Mark ${habit.name} as not done" else "Mark ${habit.name} as done")
    }
}

/**
 * One task on a list: tick to finish, then title, and a meta line with priority, project and
 * deadline; the flag sits at the end so the eye can run down the urgency column.
 */
@Composable
fun TaskRow(
    task: Task,
    flag: TaskFlag,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    projectName: String? = null,
    today: LocalDate = LocalDate.now(),
) {
    val c = MsTheme.colors
    val finished = !task.status.isOpen
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TickButton(
            when (task.status) {
                TaskStatus.Done -> TickState.Done
                TaskStatus.Dropped -> TickState.Skipped
                else -> TickState.Empty
            },
            onClick = onToggle,
            size = 28.dp,
            contentDescription = if (task.status == TaskStatus.Done) "Reopen ${task.title}" else "Complete ${task.title}",
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (finished) c.textTertiary else c.textPrimary,
                textDecoration = if (finished) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                PriorityBadge(task.priority, size = 18.dp)
                // The date leads: it is what the eye scans for, and must survive truncation.
                val meta = buildList {
                    when {
                        task.status == TaskStatus.Done && task.doneOn != null -> add("done ${Fmt.friendlyDay(task.doneOn, today)}")
                        task.due != null && task.status.isOpen -> add(Fmt.friendlyDay(task.due, today))
                        task.due != null -> add("due ${Fmt.shortDay(task.due)}")
                    }
                    if (task.status == TaskStatus.InProgress || task.status == TaskStatus.Blocked) add(task.status.label)
                    projectName?.let(::add)
                }
                if (meta.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Text(meta.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = c.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                }
                if (task.repeat != Repeat.None) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Rounded.Repeat, contentDescription = task.repeat.label, tint = c.textTertiary, modifier = Modifier.size(13.dp))
                }
            }
        }
        if (flag != TaskFlag.Open && flag != TaskFlag.NoDeadline) {
            Spacer(Modifier.width(8.dp))
            FlagPill(flag)
        }
    }
}

/** Thin label above a group of rows. */
@Composable
fun GroupHeader(text: String, count: Int? = null, color: Color = MsTheme.colors.textSecondary, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color)
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text("$count", style = MaterialTheme.typography.labelSmall, color = MsTheme.colors.textTertiary)
        }
        Spacer(Modifier.height(1.dp))
    }
}
