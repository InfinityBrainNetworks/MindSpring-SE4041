package com.mindspring.app.ui.screens.habits

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.data.model.HabitFrequency
import com.mindspring.app.data.model.MarkState
import com.mindspring.app.domain.HabitUnit
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.BrandTopBar
import com.mindspring.app.ui.components.EmptyState
import com.mindspring.app.ui.components.FrequencyPill
import com.mindspring.app.ui.components.GroupHeader
import com.mindspring.app.ui.components.HabitRow
import com.mindspring.app.ui.components.LocalSnackbar
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.MsProgressBar
import com.mindspring.app.ui.components.ProgressRing
import com.mindspring.app.ui.components.SegmentedTabs
import com.mindspring.app.ui.components.TickState
import com.mindspring.app.ui.components.appear
import com.mindspring.app.ui.components.tint
import com.mindspring.app.ui.components.vector
import com.mindspring.app.ui.screens.home.TodayHabit
import com.mindspring.app.ui.preview.PreviewHabitsState
import com.mindspring.app.ui.preview.PreviewScreen
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.theme.areaColor
import com.mindspring.app.ui.util.Fmt
import kotlinx.coroutines.launch

@Composable
fun HabitsScreen(
    userName: String,
    onOpenHabit: (Long) -> Unit,
    onAddHabit: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val vm = appViewModel { HabitsViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    val view by vm.view.collectAsStateWithLifecycle()
    val weekOffset by vm.weekOffset.collectAsStateWithLifecycle()

    HabitsContent(
        userName = userName,
        s = s,
        view = view,
        weekOffset = weekOffset,
        onViewChange = { vm.view.value = it },
        onWeekOffsetChange = { vm.weekOffset.value = it },
        onToggleDone = vm::toggleDone,
        onToggleSkip = vm::toggleSkip,
        onCycleCell = vm::cycle,
        onOpenHabit = onOpenHabit,
        onAddHabit = onAddHabit,
        onOpenProfile = onOpenProfile,
    )
}

/**
 * The screen as pure state and callbacks, so it renders in a @Preview without a ViewModel behind
 * it. [HabitsScreen] is the thin wrapper that supplies both from the app's data.
 */
@Composable
fun HabitsContent(
    userName: String,
    s: HabitsState,
    view: HabitsView,
    weekOffset: Long,
    onViewChange: (HabitsView) -> Unit,
    onWeekOffsetChange: (Long) -> Unit,
    onToggleDone: (TodayHabit) -> Unit,
    onToggleSkip: (TodayHabit) -> Unit,
    onCycleCell: (Long, WeekCell) -> Unit,
    onOpenHabit: (Long) -> Unit,
    onAddHabit: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val c = MsTheme.colors

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BrandTopBar(userName, onAvatarClick = onOpenProfile)
            Column(Modifier.padding(start = Dimens.screen, end = Dimens.screen, top = 20.dp, bottom = 8.dp)) {
                Text("Habits", style = MaterialTheme.typography.headlineSmall, color = c.textPrimary)
                Text(
                    when (view) {
                        HabitsView.Today -> "Tick what you did. Long-press to skip a day."
                        HabitsView.Week -> "Tap a day: done, then skipped, then clear."
                        HabitsView.All -> "Every habit, grouped by life area."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
                Spacer(Modifier.height(12.dp))
                SegmentedTabs(HabitsView.entries, view, onViewChange, { it.label })
            }
            AnimatedContent(
                targetState = view,
                transitionSpec = {
                    val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (fadeIn(tween(240)) + slideInHorizontally(tween(280)) { it / 8 * dir }) togetherWith fadeOut(tween(160))
                },
                label = "habitsView",
                modifier = Modifier.weight(1f),
            ) { v ->
                when (v) {
                    HabitsView.Today -> TodayView(s, onToggleDone, onToggleSkip, onOpenHabit, onAddHabit)
                    HabitsView.Week -> WeekView(s, weekOffset, onWeekOffsetChange, onCycleCell)
                    HabitsView.All -> AllView(s, onOpenHabit)
                }
            }
        }
        FloatingActionButton(
            onClick = onAddHabit,
            containerColor = c.amber,
            contentColor = c.onAmber,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(6.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(Dimens.screen),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add habit")
        }
    }
}

@Composable
private fun TodayView(
    s: HabitsState,
    onToggleDone: (TodayHabit) -> Unit,
    onToggleSkip: (TodayHabit) -> Unit,
    onOpenHabit: (Long) -> Unit,
    onAddHabit: () -> Unit,
) {
    val c = MsTheme.colors
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    if (s.loaded && s.today.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(Dimens.screen)) {
            EmptyState(Icons.Rounded.EventAvailable, "Nothing planned today", "Tap + to create a habit, or check the Week view.")
        }
        return
    }
    val groups = listOf(
        "Today" to s.today.filter { it.habit.frequency.isDayBased },
        "This week" to s.today.filter { it.habit.frequency == HabitFrequency.Weekly },
        "This month" to s.today.filter { it.habit.frequency == HabitFrequency.Monthly },
    ).filter { it.second.isNotEmpty() }
    val done = s.today.count { it.mark == MarkState.Done || (it.mark == null && it.doneOn != null) }
    val countable = s.today.count { it.mark != MarkState.Skipped }

    LazyColumn(
        contentPadding = PaddingValues(start = Dimens.screen, end = Dimens.screen, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
    ) {
        item(key = "summary") {
            MsCard(Modifier.fillMaxWidth().appear(0)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(if (countable == 0) 0f else done.toFloat() / countable, size = 56.dp) {
                        Text(Fmt.percent(if (countable == 0) 0f else done.toFloat() / countable), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = c.textPrimary)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("$done of $countable done", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                        Text(
                            if (done == countable && countable > 0) "Everything handled. Well done." else "${countable - done} still open today",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary,
                        )
                    }
                }
            }
        }
        groups.forEachIndexed { gi, (title, list) ->
            item(key = "group-$title") {
                MsCard(Modifier.fillMaxWidth().appear(gi + 1), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)) {
                    GroupHeader(title, list.size, modifier = Modifier.padding(horizontal = 6.dp))
                    list.forEach { item ->
                        HabitRow(
                            habit = item.habit,
                            tick = item.tick(),
                            onTick = {
                                if (item.mark == MarkState.Skipped) onToggleSkip(item) else onToggleDone(item)
                            },
                            onSkip = {
                                onToggleSkip(item)
                                if (item.mark != MarkState.Skipped) scope.launch { snackbar.showSnackbar("Skipped ${item.habit.name} for today") }
                            },
                            onClick = { onOpenHabit(item.habit.id) },
                            streak = item.streak,
                            unit = item.unit,
                            areaColorIndex = item.areaColor,
                            note = item.doneOn?.let { "Done ${Fmt.weekdayShort(it)}" },
                        )
                    }
                }
            }
        }
    }
}

private fun TodayHabit.tick(): TickState = when {
    mark == MarkState.Done -> TickState.Done
    mark == MarkState.Skipped -> TickState.Skipped
    doneOn != null -> TickState.Done
    else -> TickState.Empty
}

/** The workbook's month grid, one week at a time so every cell is big enough to tap. */
@Composable
private fun WeekView(
    s: HabitsState,
    offset: Long,
    onOffsetChange: (Long) -> Unit,
    onCycleCell: (Long, WeekCell) -> Unit,
) {
    val c = MsTheme.colors
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = Dimens.screen, end = Dimens.screen, top = 4.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onOffsetChange(offset - 1) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous week", tint = c.tealInk)
            }
            Text(
                "${Fmt.dayMonth(s.weekStart)} – ${Fmt.dayMonth(s.weekStart.plusDays(6))}" + if (offset == 0L) " · this week" else "",
                style = MaterialTheme.typography.titleMedium,
                color = c.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onOffsetChange(offset + 1) }, enabled = offset < 0) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next week", tint = if (offset < 0) c.tealInk else c.textTertiary.copy(alpha = 0.4f))
            }
        }

        MsCard(Modifier.fillMaxWidth().appear(0), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)) {
            val days = (0L..6L).map { s.weekStart.plusDays(it) }
            val todayIndex = days.indexOf(java.time.LocalDate.now())
            // Header: weekday letters and dates; today in amber.
            Row(verticalAlignment = Alignment.Bottom) {
                days.forEachIndexed { i, d ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Fmt.weekdayInitial(d), style = MaterialTheme.typography.labelSmall, color = if (i == todayIndex) c.amber else c.textTertiary, fontWeight = if (i == todayIndex) FontWeight.Bold else null)
                        Text("${d.dayOfMonth}", style = MaterialTheme.typography.labelMedium, color = if (i == todayIndex) c.amber else c.textSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            if (s.week.isEmpty()) {
                Text("No habits in this week.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, modifier = Modifier.padding(vertical = 12.dp))
            }
            var lastFrequency: HabitFrequency? = null
            s.week.forEach { row ->
                if (row.habit.frequency != lastFrequency) {
                    lastFrequency = row.habit.frequency
                    val t = row.habit.frequency.tint(c)
                    Text(
                        row.habit.frequency.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = t.ink,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp).clip(RoundedCornerShape(50)).background(t.fill).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                // The full name on its own line, then the week beneath it, so nothing is cut short.
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(row.habit.icon.vector, contentDescription = null, tint = row.areaColor?.let { areaColor(it) } ?: c.tealInk, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(row.habit.name, style = MaterialTheme.typography.labelMedium, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (row.habit.target.isNotBlank()) {
                        Text("  ·  ${row.habit.target}", style = MaterialTheme.typography.labelSmall, color = c.textTertiary, maxLines = 1)
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    row.cells.forEach { cell ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            GridCell(cell, anyDay = !row.habit.frequency.isDayBased) { onCycleCell(row.habit.id, cell) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            // Footer rows: the day score, and the deadlines landing on each day.
            Text("DAY SCORE", style = MaterialTheme.typography.labelSmall, color = c.textSecondary, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            FooterRow(s.dayScores.map { score ->
                score?.let { Fmt.percent(it) to scoreColor(it) }
            })
            Text("TASK DEADLINES", style = MaterialTheme.typography.labelSmall, color = c.textSecondary, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            FooterRow(s.deadlines.map { n -> if (n == 0) null else "$n" to c.amber })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            LegendDot(TickState.Done, "Done")
            LegendDot(TickState.Skipped, "Skipped")
            LegendDot(TickState.Off, "Not due")
        }
    }
}

private val CELL = 34.dp

@Composable
private fun GridCell(cell: WeekCell, anyDay: Boolean, onClick: () -> Unit) {
    val c = MsTheme.colors
    val scale by animateFloatAsState(if (cell.tick == TickState.Done) 1f else 0.9f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "cell")
    val fill by animateColorAsState(
        when (cell.tick) {
            TickState.Done -> c.tealInk
            TickState.Skipped -> c.cardMuted
            else -> Color.Transparent
        },
        label = "cellFill",
    )
    Box(Modifier.width(CELL).height(CELL), contentAlignment = Alignment.Center) {
        if (cell.isToday) Box(Modifier.size(CELL - 2.dp).clip(RoundedCornerShape(10.dp)).background(c.amber.copy(alpha = 0.14f)))
        Box(
            Modifier
                .scale(scale)
                .size(26.dp)
                .clip(CircleShape)
                .background(fill)
                .then(
                    when (cell.tick) {
                        // A weekly or monthly habit may go on any of these days, so blanks are
                        // drawn faintly: they are options, not misses.
                        TickState.Empty -> Modifier.border(1.5.dp, c.textTertiary.copy(alpha = if (anyDay) 0.18f else 0.45f), CircleShape)
                        else -> Modifier
                    },
                )
                .then(if (cell.enabled) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            when (cell.tick) {
                TickState.Done -> Icon(Icons.Rounded.Check, contentDescription = "Done", tint = if (c.isDark) Color(0xFF0D2623) else Color.White, modifier = Modifier.size(16.dp))
                TickState.Skipped -> Icon(Icons.Rounded.Remove, contentDescription = "Skipped", tint = c.textSecondary, modifier = Modifier.size(16.dp))
                TickState.Off -> Box(Modifier.size(5.dp).clip(CircleShape).background(c.divider))
                TickState.Empty -> Unit
            }
        }
    }
}

@Composable
private fun FooterRow(values: List<Pair<String, Color>?>) {
    val c = MsTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        values.forEach { v ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (v != null) {
                    Text(v.first, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = v.second, maxLines = 1)
                } else {
                    Text("·", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                }
            }
        }
    }
}

@Composable
private fun scoreColor(score: Float): Color {
    val c = MsTheme.colors
    return when {
        score >= 0.75f -> c.tealInk
        score >= 0.4f -> if (c.isDark) Color(0xFFF2CB86) else Color(0xFF8A5A00)
        else -> c.danger
    }
}

@Composable
private fun LegendDot(tick: TickState, label: String) {
    val c = MsTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(14.dp).clip(CircleShape).background(
                when (tick) {
                    TickState.Done -> c.tealInk
                    TickState.Skipped -> c.cardMuted
                    else -> c.divider
                },
            ),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
    }
}

@Composable
private fun AllView(s: HabitsState, onOpenHabit: (Long) -> Unit) {
    val c = MsTheme.colors
    var showRetired by rememberSaveable { mutableStateOf(false) }
    if (s.loaded && s.groups.isEmpty() && s.retired.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(Dimens.screen)) {
            EmptyState(Icons.Rounded.EventAvailable, "No habits yet", "Tap + to create your first habit.")
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = Dimens.screen, end = Dimens.screen, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
    ) {
        items(s.groups, key = { "area-${it.area?.id ?: 0}" }) { group ->
            MsCard(Modifier.fillMaxWidth().animateItem(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)) {
                Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(group.area?.let { areaColor(it.colorIndex) } ?: c.textTertiary))
                    Spacer(Modifier.width(8.dp))
                    Text(group.area?.name ?: "No area", style = MaterialTheme.typography.titleMedium, color = c.textPrimary, modifier = Modifier.weight(1f))
                    if (group.tally.possible > 0) {
                        Text("${Fmt.percent(group.tally.rate)} this month", style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                    }
                }
                if (group.tally.possible > 0) {
                    MsProgressBar(group.tally.rate, Modifier.padding(horizontal = 6.dp, vertical = 8.dp), color = group.area?.let { areaColor(it.colorIndex) } ?: c.tealInk, height = 5.dp)
                }
                group.habits.forEach { SummaryRow(it, onOpenHabit) }
            }
        }
        if (s.retired.isNotEmpty()) {
            item(key = "retired") {
                MsCard(Modifier.fillMaxWidth(), onClick = { showRetired = !showRetired }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)) {
                    Text(
                        "Retired habits (${s.retired.size})" + if (showRetired) "" else " · tap to show",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.textSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                    if (showRetired) s.retired.forEach { SummaryRow(it, onOpenHabit) }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(item: HabitSummary, onOpenHabit: (Long) -> Unit) {
    val c = MsTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onOpenHabit(item.habit.id) }.padding(horizontal = 6.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(item.habit.icon.vector, contentDescription = null, tint = item.areaColor?.let { areaColor(it) } ?: c.tealInk, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.habit.name, style = MaterialTheme.typography.bodyLarge, color = if (item.habit.active) c.textPrimary else c.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                FrequencyPill(item.habit.frequency)
                val meta = listOfNotNull(item.habit.subArea.ifBlank { null }, item.habit.target.ifBlank { null }).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Text(meta, style = MaterialTheme.typography.labelSmall, color = c.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(if (item.month.possible == 0) "–" else Fmt.percent(item.month.rate), style = MaterialTheme.typography.labelLarge, color = c.tealInk, fontWeight = FontWeight.Bold)
            if (item.streak > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = c.amber, modifier = Modifier.size(12.dp))
                    Text(if (item.unit == HabitUnit.Day) "${item.streak}" else item.unit.count(item.streak), style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                }
            }
        }
    }
}

// --- Previews -------------------------------------------------------------------------------

@Composable
private fun HabitsPreview(dark: Boolean = false, view: HabitsView) = PreviewScreen(dark) {
    CompositionLocalProvider(LocalSnackbar provides SnackbarHostState()) {
        HabitsContent(
            userName = "Asan", s = PreviewHabitsState, view = view, weekOffset = 0L,
            onViewChange = {}, onWeekOffsetChange = {}, onToggleDone = {}, onToggleSkip = {},
            onCycleCell = { _, _ -> }, onOpenHabit = {}, onAddHabit = {}, onOpenProfile = {},
        )
    }
}

@Preview(name = "Habits · today", showBackground = true, widthDp = 393, heightDp = 830)
@Composable
private fun HabitsTodayPreview() = HabitsPreview(view = HabitsView.Today)

@Preview(name = "Habits · week grid", showBackground = true, widthDp = 393, heightDp = 830)
@Composable
private fun HabitsWeekPreview() = HabitsPreview(view = HabitsView.Week)

@Preview(name = "Habits · all", showBackground = true, widthDp = 393, heightDp = 830)
@Composable
private fun HabitsAllPreview() = HabitsPreview(view = HabitsView.All)

@Preview(name = "Habits · today dark", showBackground = true, widthDp = 393, heightDp = 830)
@Composable
private fun HabitsTodayDarkPreview() = HabitsPreview(dark = true, view = HabitsView.Today)
