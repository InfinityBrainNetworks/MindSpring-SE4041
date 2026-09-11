package com.mindspring.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mindspring.app.data.model.HabitFrequency
import com.mindspring.app.data.model.Priority
import com.mindspring.app.domain.TaskFlag
import com.mindspring.app.ui.theme.MsColors
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.theme.areaColor

/** A fill and an ink colour that belong together. */
data class Tint(val fill: Color, val ink: Color)

/**
 * One colour per habit frequency, as on the workbook's Habits sheet and month grids: green for
 * daily, blue for weekdays, amber for weekends, violet for weekly, rose for monthly.
 */
fun HabitFrequency.tint(c: MsColors): Tint = when (this) {
    HabitFrequency.Daily -> if (c.isDark) Tint(Color(0xFF23402C), Color(0xFF9FD8AE)) else Tint(Color(0xFFDFF0E1), Color(0xFF1E6B3A))
    HabitFrequency.Weekdays -> if (c.isDark) Tint(Color(0xFF203648), Color(0xFF9CC7EA)) else Tint(Color(0xFFDCE9F3), Color(0xFF1F5C8B))
    HabitFrequency.Weekends -> if (c.isDark) Tint(Color(0xFF45371E), Color(0xFFF2CB86)) else Tint(Color(0xFFFBE7C6), Color(0xFF8A5A00))
    HabitFrequency.Custom -> if (c.isDark) Tint(Color(0xFF26403D), Color(0xFF9ED3CA)) else Tint(Color(0xFFD7EEEA), Color(0xFF10514C))
    HabitFrequency.Weekly -> if (c.isDark) Tint(Color(0xFF342D4A), Color(0xFFC6B8F0)) else Tint(Color(0xFFE7E1F5), Color(0xFF584A87))
    HabitFrequency.Monthly -> if (c.isDark) Tint(Color(0xFF462A37), Color(0xFFEDB0C8)) else Tint(Color(0xFFF5E2EA), Color(0xFF8A3B5A))
}

fun TaskFlag.tint(c: MsColors): Tint = when (this) {
    TaskFlag.Overdue -> Tint(c.dangerSoft, c.danger)
    TaskFlag.DueToday -> Tint(c.warnSoft, if (c.isDark) Color(0xFFF2CB86) else Color(0xFF8A5A00))
    TaskFlag.DueSoon -> Tint(c.warnSoft.copy(alpha = 0.6f), if (c.isDark) Color(0xFFF2CB86) else Color(0xFF8A5A00))
    TaskFlag.DoneOnTime, TaskFlag.Done -> Tint(c.cardSubtle, c.tealInk)
    TaskFlag.DoneLate -> Tint(c.cardSubtle, if (c.isDark) Color(0xFFF2CB86) else Color(0xFF8A5A00))
    TaskFlag.Dropped -> Tint(c.cardMuted, c.textTertiary)
    TaskFlag.Upcoming, TaskFlag.Open, TaskFlag.NoDeadline -> Tint(c.cardMuted.copy(alpha = 0.7f), c.textSecondary)
}

fun Priority.tint(c: MsColors): Tint = when (this) {
    Priority.A -> Tint(c.amber, c.onAmber)
    Priority.B -> Tint(c.cardMuted, c.tealInk)
    Priority.C -> Tint(Color.Transparent, c.textTertiary)
}

/** Small rounded label: flags, frequencies and similar one-word states. */
@Composable
fun Pill(text: String, tint: Tint, modifier: Modifier = Modifier, bold: Boolean = true) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium,
        color = tint.ink,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(tint.fill)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun FlagPill(flag: TaskFlag, modifier: Modifier = Modifier) = Pill(flag.label, flag.tint(MsTheme.colors), modifier)

@Composable
fun FrequencyPill(frequency: HabitFrequency, modifier: Modifier = Modifier) =
    Pill(frequency.label, frequency.tint(MsTheme.colors), modifier)

/** A/B/C in a small circle: amber for must, teal for should, outline for optional. */
@Composable
fun PriorityBadge(priority: Priority, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    val c = MsTheme.colors
    val t = priority.tint(c)
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(t.fill)
            .then(if (priority == Priority.C) Modifier.border(1.dp, c.textTertiary.copy(alpha = 0.5f), CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(priority.short, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = t.ink)
    }
}

/** Coloured dot plus name, for a life area or project. */
@Composable
fun AreaTag(name: String, colorIndex: Int, modifier: Modifier = Modifier, dot: Dp = 8.dp) {
    val c = MsTheme.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(dot).clip(CircleShape).background(areaColor(colorIndex)))
        Spacer(Modifier.width(6.dp))
        Text(name, style = MaterialTheme.typography.labelSmall, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
