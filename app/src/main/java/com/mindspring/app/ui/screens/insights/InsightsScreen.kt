package com.mindspring.app.ui.screens.insights

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.ActivityBars
import com.mindspring.app.ui.components.BrandTopBar
import com.mindspring.app.ui.components.IconCircle
import com.mindspring.app.ui.components.MonthSwitcher
import com.mindspring.app.ui.components.MoodTrendChart
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.MsProgressBar
import com.mindspring.app.ui.components.SectionTitle
import com.mindspring.app.ui.components.SegmentedTabs
import com.mindspring.app.ui.components.appear
import com.mindspring.app.ui.components.color
import com.mindspring.app.ui.components.icon
import com.mindspring.app.domain.HabitStats
import com.mindspring.app.ui.preview.PreviewInsightsState
import com.mindspring.app.ui.preview.PreviewMonth
import com.mindspring.app.ui.preview.PreviewScreen
import com.mindspring.app.ui.theme.CardShape
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.theme.areaColor
import com.mindspring.app.ui.util.Fmt
import java.time.YearMonth

@Composable
fun InsightsScreen(
    userName: String,
    onOpenHistory: () -> Unit,
    onOpenAreas: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val vm = appViewModel { InsightsViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val month by vm.month.collectAsStateWithLifecycle()
    val section by vm.section.collectAsStateWithLifecycle()

    InsightsContent(
        userName = userName,
        state = state,
        month = month,
        section = section,
        onMonthChange = { vm.month.value = it },
        onSectionChange = { vm.section.value = it },
        onOpenHistory = onOpenHistory,
        onOpenAreas = onOpenAreas,
        onOpenJournal = onOpenJournal,
        onOpenProfile = onOpenProfile,
    )
}

/**
 * The screen as pure state and callbacks, so it renders in a @Preview without a ViewModel behind
 * it. [InsightsScreen] is the thin wrapper that supplies both from the app's data.
 */
@Composable
fun InsightsContent(
    userName: String,
    state: InsightsState?,
    month: YearMonth,
    section: InsightSection,
    onMonthChange: (YearMonth) -> Unit,
    onSectionChange: (InsightSection) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAreas: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val c = MsTheme.colors

    Column(Modifier.fillMaxSize()) {
        BrandTopBar(userName, onAvatarClick = onOpenProfile)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Dimens.screen, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
        ) {
            Column {
                Text("Your Insights", style = MaterialTheme.typography.headlineSmall, color = c.textPrimary)
                Text("How your habits, work and mood add up.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            }
            MonthSwitcher(month, onMonthChange)
            SegmentedTabs(InsightSection.entries, section, onSectionChange, { it.label })
            val s = state ?: return@Column
            AnimatedContent(section, transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(140)) }, label = "insightSection") { sec ->
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.stackMd)) {
                    when (sec) {
                        InsightSection.Overview -> Overview(s, onOpenAreas)
                        InsightSection.Habits -> HabitsSection(s)
                        InsightSection.Tasks -> TasksSection(s)
                        InsightSection.Mind -> MindSection(s, onOpenHistory, onOpenJournal)
                    }
                }
            }
            Spacer(Modifier.height(Dimens.stackSm))
        }
    }
}

// ---------------------------------------------------------------- Overview

@Composable
private fun Overview(s: InsightsState, onOpenAreas: () -> Unit) {
    val c = MsTheme.colors
    KeyInsightCard(s, Modifier.appear(0))
    TileRow(Modifier.appear(1)) {
        Tile("Habits", if (s.habits.tally.possible == 0) "–" else Fmt.percent(s.habits.tally.rate), "completion", c.tealInk, Modifier.weight(1f).fillMaxHeight())
        Tile("On time", s.tasks.onTimeRate?.let { Fmt.percent(it) } ?: "–", "tasks delivered", c.tealInk, Modifier.weight(1f).fillMaxHeight())
    }
    TileRow(Modifier.appear(2)) {
        Tile("Mood", s.averageMood?.label ?: "–", "average", s.averageMood?.color ?: c.textPrimary, Modifier.weight(1f).fillMaxHeight())
        Tile("Journal", "${s.journal.written}/${s.journal.elapsedDays}", "days written", c.textPrimary, Modifier.weight(1f).fillMaxHeight())
    }
    MsCard(Modifier.fillMaxWidth().appear(3), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Month by month") { Text("Habit completion", style = MaterialTheme.typography.labelSmall, color = c.textTertiary) }
        ActivityBars(
            values = s.rows.map { it.habitRate ?: 0f },
            labels = s.rows.map { Fmt.monthShort(it.month) },
            highlightIndex = s.rows.lastIndex,
            height = 120.dp,
        )
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            listOf("Month", "Habits", "Mood", "Journal", "Tasks").forEachIndexed { i, h ->
                Text(h, style = MaterialTheme.typography.labelSmall, color = c.textTertiary, modifier = Modifier.weight(if (i == 0) 1.2f else 1f))
            }
        }
        s.rows.reversed().forEach { r ->
            Row(Modifier.fillMaxWidth()) {
                Text(Fmt.month(r.month).substringBefore(' ').take(3) + " " + r.month.year % 100, style = MaterialTheme.typography.bodySmall, color = c.textPrimary, modifier = Modifier.weight(1.2f))
                Text(r.habitRate?.let { Fmt.percent(it) } ?: "–", style = MaterialTheme.typography.bodySmall, color = c.tealInk, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(r.avgMood?.let { Fmt.oneDecimal(it) } ?: "–", style = MaterialTheme.typography.bodySmall, color = c.textSecondary, modifier = Modifier.weight(1f))
                Text("${r.journalled}", style = MaterialTheme.typography.bodySmall, color = c.textSecondary, modifier = Modifier.weight(1f))
                Text("${r.tasksDone}", style = MaterialTheme.typography.bodySmall, color = c.textSecondary, modifier = Modifier.weight(1f))
            }
        }
    }
    LinkCard(Icons.Rounded.Category, "Life areas", "Habit rates and tasks per area and sub-area", onOpenAreas, Modifier.appear(4))
}

/**
 * Opens with a sentence, not a chart. Amber appears as the outline and icon only: amber text on
 * the light canvas would fail contrast (design report, section 3.5).
 */
@Composable
private fun KeyInsightCard(s: InsightsState, modifier: Modifier) {
    val c = MsTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(c.card)
            .border(1.5.dp, c.amber, CardShape)
            .padding(Dimens.card),
    ) {
        Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = c.amber.copy(alpha = 0.18f), modifier = Modifier.size(44.dp).align(Alignment.TopEnd))
        Column(Modifier.padding(end = 40.dp), verticalArrangement = Arrangement.spacedBy(Dimens.stackSm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = c.amber, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("KEY INSIGHT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = c.textSecondary)
            }
            val insight = s.keyInsight
            if (insight == null) {
                Text(
                    "Keep logging your mood and habits. After a few days we'll show you which routines go with your better days.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.textPrimary,
                )
            } else {
                Text(
                    buildAnnotatedString {
                        append("You rate your mood ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.tealInk)) { append("${insight.percentHigher}% higher") }
                        append(" on days you complete ")
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(insight.habitName) }
                        append(".")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.textPrimary,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Habits

@Composable
private fun HabitsSection(s: InsightsState) {
    val c = MsTheme.colors
    val h = s.habits
    var showAll by rememberSaveable { mutableStateOf(false) }
    TileRow(Modifier.appear(0)) {
        Tile("Completion", if (h.tally.possible == 0) "–" else Fmt.percent(h.tally.rate), "${h.tally.done} of ${h.tally.possible} logged", c.tealInk, Modifier.weight(1f).fillMaxHeight())
        Tile(
            "Longest streak",
            h.longest?.let { "${it.count}" } ?: "–",
            h.longest?.let { "${HabitStats.unit(it.habit).plural} · ${it.habit.name}" } ?: "${h.activeHabits} active habits",
            c.textPrimary,
            Modifier.weight(1f).fillMaxHeight(),
            icon = if (h.longest != null) Icons.Rounded.LocalFireDepartment else null,
        )
    }
    if (h.best != null || h.needsAttention != null) {
        MsCard(Modifier.fillMaxWidth().appear(1), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            h.best?.let { NamedRate("Going well", it.habit.name, it.rate, c.tealInk) }
            h.needsAttention?.let { NamedRate("Needs attention", it.habit.name, it.rate, c.amber) }
        }
    }
    if (h.byArea.isNotEmpty()) {
        MsCard(Modifier.fillMaxWidth().appear(2), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("By life area")
            h.byArea.forEach { a ->
                RateBar(a.area?.name ?: "No area", a.tally.rate, a.area?.let { areaColor(it.colorIndex) } ?: c.textTertiary)
            }
        }
    }
    if (h.dayScores.isNotEmpty()) {
        MsCard(Modifier.fillMaxWidth().appear(3), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Day score") { Text("Share of the day's habits done", style = MaterialTheme.typography.labelSmall, color = c.textTertiary) }
            ActivityBars(
                values = h.dayScores.map { it.second ?: 0f },
                labels = h.dayScores.mapIndexed { i, (d, _) -> if (i % 7 == 0 || i == h.dayScores.lastIndex) "${d.dayOfMonth}" else "" },
                highlightIndex = h.dayScores.indexOfFirst { it.first == s.today },
                height = 110.dp,
            )
        }
    }
    MsCard(Modifier.fillMaxWidth().appear(4), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Each habit")
        if (h.rates.isEmpty()) Text("Nothing to score yet this month.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        (if (showAll) h.rates else h.rates.take(8)).forEach { r ->
            RateBar(r.habit.name, r.rate, c.tealInk, detail = "${r.tally.done}/${r.tally.possible}")
        }
        if (h.rates.size > 8) {
            Text(
                if (showAll) "Show fewer" else "Show all ${h.rates.size}",
                style = MaterialTheme.typography.labelLarge,
                color = c.tealInk,
                modifier = Modifier.clip(CircleShape).clickable { showAll = !showAll }.padding(vertical = 4.dp),
            )
        }
    }
}

// ---------------------------------------------------------------- Tasks

@Composable
private fun TasksSection(s: InsightsState) {
    val c = MsTheme.colors
    val t = s.tasks
    TileRow(Modifier.appear(0)) {
        Tile("Open", "${t.open}", "right now", c.textPrimary, Modifier.weight(1f).fillMaxHeight())
        Tile("Overdue", "${t.overdue}", "past deadline", if (t.overdue > 0) c.danger else c.textPrimary, Modifier.weight(1f).fillMaxHeight())
        Tile("Next 7 days", "${t.dueNext7 + t.dueToday}", "due soon", c.tealInk, Modifier.weight(1f).fillMaxHeight())
    }
    TileRow(Modifier.appear(1)) {
        Tile("Completed", "${t.completed}", "this month", c.tealInk, Modifier.weight(1f).fillMaxHeight())
        Tile("On time", t.onTimeRate?.let { Fmt.percent(it) } ?: "–", "of dated tasks", c.textPrimary, Modifier.weight(1f).fillMaxHeight())
        Tile(
            "Avg timing",
            t.avgDaysEarly?.let { if (it >= 0) Fmt.oneDecimal(it) else Fmt.oneDecimal(-it) } ?: "–",
            t.avgDaysEarly?.let { if (it >= 0) "days early" else "days late" } ?: "days early",
            c.textPrimary,
            Modifier.weight(1f).fillMaxHeight(),
        )
    }
    MsCard(Modifier.fillMaxWidth().appear(2), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Deadlines") { Text("Open tasks due each day", style = MaterialTheme.typography.labelSmall, color = c.textTertiary) }
        if (t.deadlines.all { it.second == 0 }) {
            Text("No open deadlines this month.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        } else {
            ActivityBars(
                values = t.deadlines.map { it.second.toFloat() },
                labels = t.deadlines.mapIndexed { i, (d, _) -> if (i % 7 == 0 || i == t.deadlines.lastIndex) "${d.dayOfMonth}" else "" },
                highlightIndex = t.deadlines.indexOfFirst { it.first == s.today },
                height = 100.dp,
            )
            Text("Tall bars are crunch days. Spread the work before them.", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
        }
    }
    if (s.projects.isNotEmpty()) {
        MsCard(Modifier.fillMaxWidth().appear(3), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Projects")
            s.projects.forEach { p ->
                RateBar(
                    p.project.name,
                    p.tasks.progress,
                    areaColor(p.project.colorIndex),
                    detail = when {
                        p.tasks.overdue > 0 -> "${p.tasks.overdue} overdue"
                        p.tasks.open == 0 -> "complete"
                        else -> "${p.tasks.done}/${p.tasks.total - p.tasks.dropped}"
                    },
                    detailColor = if (p.tasks.overdue > 0) c.danger else null,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Mind

@Composable
private fun MindSection(s: InsightsState, onOpenHistory: () -> Unit, onOpenJournal: () -> Unit) {
    val c = MsTheme.colors
    val j = s.journal
    TileRow(Modifier.appear(0)) {
        Tile("Average mood", s.averageMood?.label ?: "–", s.averageMoodValue?.let { "${Fmt.oneDecimal(it)} of 5" } ?: "no check-ins", s.averageMood?.color ?: c.textPrimary, Modifier.weight(1f).fillMaxHeight(), icon = s.averageMood?.icon)
        Tile("Journal", "${j.written}/${j.elapsedDays}", "days written", c.tealInk, Modifier.weight(1f).fillMaxHeight())
    }
    TileRow(Modifier.appear(1)) {
        Tile("Day rating", j.avgRating?.let { Fmt.oneDecimal(it) } ?: "–", "average of 5", c.textPrimary, Modifier.weight(1f).fillMaxHeight())
        Tile("Energy", j.avgEnergy?.let { Fmt.oneDecimal(it) } ?: "–", "average of 5", c.textPrimary, Modifier.weight(1f).fillMaxHeight())
        Tile("Words", j.avgWords?.let { "${it.toInt()}" } ?: "–", "per entry", c.textPrimary, Modifier.weight(1f).fillMaxHeight())
    }
    MsCard(Modifier.fillMaxWidth().appear(2), verticalArrangement = Arrangement.spacedBy(Dimens.stackMd)) {
        SectionTitle("Mood trend")
        if (s.trend.size < 2) {
            Text("Check in on a couple of days to see your trend.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        } else {
            MoodTrendChart(s.trend, s.trendLabels)
        }
    }
    LinkCard(Icons.Rounded.History, "Mood history", "Review, edit or delete past check-ins", onOpenHistory, Modifier.appear(3))
    LinkCard(Icons.Rounded.AutoStories, "Daily reflection", "Your journal, one entry per day", onOpenJournal, Modifier.appear(4))
}

// ---------------------------------------------------------------- pieces

@Composable
private fun TileRow(modifier: Modifier, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable
private fun Tile(label: String, value: String, caption: String, valueColor: Color, modifier: Modifier, icon: ImageVector? = null) {
    val c = MsTheme.colors
    MsCard(modifier, contentPadding = PaddingValues(14.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.textSecondary, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = if (icon == Icons.Rounded.LocalFireDepartment) c.amber else valueColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(value, style = MaterialTheme.typography.titleLarge, color = valueColor, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Text(caption, style = MaterialTheme.typography.labelSmall, color = c.textTertiary, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun NamedRate(label: String, name: String, rate: Float, color: Color) {
    val c = MsTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
            Text(name, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(Fmt.percent(rate), style = MaterialTheme.typography.titleMedium, color = c.textPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RateBar(name: String, rate: Float, color: Color, detail: String? = null, detailColor: Color? = null) {
    val c = MsTheme.colors
    Column {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.labelMedium, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.labelSmall, color = detailColor ?: c.textTertiary)
                Spacer(Modifier.width(8.dp))
            }
            Text(Fmt.percent(rate), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = c.tealInk)
        }
        MsProgressBar(rate, color = color, trackColor = c.cardMuted)
    }
}

@Composable
private fun LinkCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier) {
    val c = MsTheme.colors
    MsCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCircle(icon, size = 40.dp, iconSize = 22.dp)
            Spacer(Modifier.width(Dimens.stackMd))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary)
        }
    }
}

// --- Previews -------------------------------------------------------------------------------

@Composable
private fun InsightsPreview(dark: Boolean = false, section: InsightSection) = PreviewScreen(dark) {
    InsightsContent(
        userName = "Asan", state = PreviewInsightsState, month = PreviewMonth, section = section,
        onMonthChange = {}, onSectionChange = {}, onOpenHistory = {}, onOpenAreas = {},
        onOpenJournal = {}, onOpenProfile = {},
    )
}

@Preview(name = "Insights · overview", showBackground = true, widthDp = 393, heightDp = 1000)
@Composable
private fun InsightsOverviewPreview() = InsightsPreview(section = InsightSection.Overview)

@Preview(name = "Insights · habits", showBackground = true, widthDp = 393, heightDp = 1000)
@Composable
private fun InsightsHabitsPreview() = InsightsPreview(section = InsightSection.Habits)

@Preview(name = "Insights · tasks", showBackground = true, widthDp = 393, heightDp = 1000)
@Composable
private fun InsightsTasksPreview() = InsightsPreview(section = InsightSection.Tasks)

@Preview(name = "Insights · mind", showBackground = true, widthDp = 393, heightDp = 1000)
@Composable
private fun InsightsMindPreview() = InsightsPreview(section = InsightSection.Mind)

@Preview(name = "Insights · overview dark", showBackground = true, widthDp = 393, heightDp = 1000)
@Composable
private fun InsightsOverviewDarkPreview() = InsightsPreview(dark = true, section = InsightSection.Overview)
