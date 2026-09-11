package com.mindspring.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.data.model.MarkState
import com.mindspring.app.data.model.Mood
import com.mindspring.app.data.model.TaskStatus
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.AnimatedCount
import com.mindspring.app.ui.components.AvatarCircle
import com.mindspring.app.ui.components.GhostButton
import com.mindspring.app.ui.components.HabitRow
import com.mindspring.app.ui.components.LocalSnackbar
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.MsTextField
import com.mindspring.app.ui.components.OutlinePillButton
import com.mindspring.app.ui.components.PrimaryButton
import com.mindspring.app.ui.components.ProgressRing
import com.mindspring.app.ui.components.SectionTitle
import com.mindspring.app.ui.components.TaskRow
import com.mindspring.app.ui.components.TickState
import com.mindspring.app.ui.components.appear
import com.mindspring.app.ui.components.color
import com.mindspring.app.ui.components.emoji
import com.mindspring.app.domain.JournalTargets
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.util.Fmt
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun HomeScreen(
    onCheckIn: (rating: Int?) -> Unit,
    onEditMood: (entryId: Long) -> Unit,
    onOpenHabit: (Long) -> Unit,
    onAddHabit: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onOpenJournal: (LocalDate) -> Unit,
    onOpenProfile: () -> Unit,
) {
    val vm = appViewModel { HomeViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GreetingHeader(s, onOpenProfile)
        Column(
            Modifier.padding(horizontal = Dimens.screen, vertical = Dimens.stackLg - 8.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
        ) {
            MoodHeroCard(s, onCheckIn, onEditMood, Modifier.appear(0))
            DayAtAGlance(s, onOpenTasks, Modifier.appear(1))
            if (s.upNext.isNotEmpty()) UpNextCard(s, vm, onOpenTask, onOpenTasks, Modifier.appear(2))
            TodayHabitsCard(s, vm, onOpenHabit, onAddHabit, Modifier.appear(3))
            ReflectionCard(s, onOpenJournal, Modifier.appear(4))
            if (s.streaks.isNotEmpty()) StreaksCard(s.streaks, Modifier.appear(5))
            GratitudeCard(onSave = vm::saveGratitude, modifier = Modifier.appear(6))
            Spacer(Modifier.height(Dimens.stackSm))
        }
    }
}

@Composable
private fun GreetingHeader(s: HomeUiState, onAvatar: () -> Unit) {
    val c = MsTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(MsTheme.chrome)
            .statusBarsPadding()
            .padding(start = Dimens.screen, end = Dimens.screen, top = 16.dp, bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgressRing(
            progress = s.progress,
            size = 58.dp,
            stroke = 6.dp,
            color = c.amber,
            trackColor = Color.White.copy(alpha = 0.16f),
        ) {
            Text(
                if (s.countable == 0) "–" else "${s.doneCount}/${s.countable}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = c.onTeal,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (s.firstName.isBlank()) Fmt.greeting() else "${Fmt.greeting()}, ${s.firstName}",
                style = MaterialTheme.typography.titleLarge,
                color = c.onTeal,
            )
            Text(Fmt.longDay(LocalDate.now()), style = MaterialTheme.typography.bodySmall, color = c.onTealMuted)
        }
        AvatarCircle(
            s.firstName,
            size = 40.dp,
            background = Color.White.copy(alpha = 0.16f),
            contentColor = c.onTeal,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onAvatar),
        )
    }
}

@Composable
private fun MoodHeroCard(s: HomeUiState, onCheckIn: (Int?) -> Unit, onEditMood: (Long) -> Unit, modifier: Modifier) {
    val c = MsTheme.colors
    MsCard(
        modifier.fillMaxWidth(),
        color = c.hero,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
    ) {
        val today = s.todayMood
        if (today == null) {
            Text("How are you feeling today?", style = MaterialTheme.typography.titleLarge, color = c.onTeal, textAlign = TextAlign.Center)
            Row(Modifier.fillMaxWidth().widthIn(max = 320.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Mood.entries.forEach { mood ->
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(mood.color.copy(alpha = 0.28f))
                            .clickable { onCheckIn(mood.rating) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(mood.emoji, fontSize = 26.sp)
                    }
                }
            }
            PrimaryButton("Check in", onClick = { onCheckIn(null) }, modifier = Modifier.fillMaxWidth())
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(today.mood.color.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center,
                ) { Text(today.mood.emoji, fontSize = 34.sp) }
                Spacer(Modifier.width(Dimens.stackMd))
                Column(Modifier.weight(1f)) {
                    Text("Today you're feeling", style = MaterialTheme.typography.labelMedium, color = c.onTealMuted)
                    Text(today.mood.label, style = MaterialTheme.typography.headlineSmall, color = c.onTeal)
                    if (today.feelings.isNotEmpty()) {
                        Text(today.feelings.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = c.onTealMuted)
                    }
                }
            }
            OutlinePillButton(
                "Update check-in",
                onClick = { onEditMood(today.id) },
                contentColor = c.onTeal,
                borderColor = c.onTeal.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The workbook's Today KPIs: what is overdue, due today, due this week and open overall. */
@Composable
private fun DayAtAGlance(s: HomeUiState, onOpenTasks: () -> Unit, modifier: Modifier) {
    val c = MsTheme.colors
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlanceTile("Overdue", s.overdue, if (s.overdue > 0) c.danger else c.textPrimary, onOpenTasks, Modifier.weight(1f).fillMaxHeight())
        GlanceTile("Today", s.dueToday, if (s.dueToday > 0) c.amber else c.textPrimary, onOpenTasks, Modifier.weight(1f).fillMaxHeight())
        GlanceTile("7 days", s.dueWeek, c.tealInk, onOpenTasks, Modifier.weight(1f).fillMaxHeight())
        GlanceTile("Open", s.open, c.textPrimary, onOpenTasks, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun GlanceTile(label: String, value: Int, color: Color, onClick: () -> Unit, modifier: Modifier) {
    val c = MsTheme.colors
    MsCard(modifier, onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedCount(value, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.textSecondary, maxLines = 1)
    }
}

@Composable
private fun UpNextCard(s: HomeUiState, vm: HomeViewModel, onOpenTask: (Long) -> Unit, onOpenTasks: () -> Unit, modifier: Modifier) {
    val c = MsTheme.colors
    MsCard(modifier.fillMaxWidth().animateContentSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 8.dp)) {
        SectionTitle("Up next", Modifier.padding(horizontal = 8.dp)) {
            Row(
                Modifier.clip(CircleShape).clickable(onClick = onOpenTasks).padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("All tasks", style = MaterialTheme.typography.labelMedium, color = c.tealInk)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.tealInk, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        s.upNext.forEach { line ->
            TaskRow(
                task = line.task,
                flag = line.flag,
                projectName = line.projectName,
                onToggle = { if (line.task.status == TaskStatus.Done) vm.reopenTask(line.task) else vm.completeTask(line.task) },
                onClick = { onOpenTask(line.task.id) },
            )
        }
    }
}

@Composable
private fun TodayHabitsCard(s: HomeUiState, vm: HomeViewModel, onOpenHabit: (Long) -> Unit, onAddHabit: () -> Unit, modifier: Modifier) {
    val c = MsTheme.colors
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var showHandled by rememberSaveable { mutableStateOf(false) }
    MsCard(modifier.fillMaxWidth().animateContentSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 10.dp)) {
        SectionTitle("Today's habits", Modifier.padding(horizontal = 8.dp)) {
            if (s.habits.isNotEmpty()) {
                Text("${s.doneCount} of ${s.countable} done", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
            }
        }
        if (s.habits.isEmpty()) {
            Text(
                if (s.loaded) "Nothing planned today. Add a habit to build a routine." else "",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                modifier = Modifier.padding(8.dp),
            )
            GhostButton("Add a habit", onClick = onAddHabit)
            return@MsCard
        }
        Spacer(Modifier.height(4.dp))
        val (handled, pending) = s.habits.partition { it.handled }
        pending.forEach { item ->
            HabitRow(
                habit = item.habit,
                tick = TickState.Empty,
                onTick = { vm.toggleDone(item) },
                onSkip = {
                    vm.toggleSkip(item)
                    scope.launch { snackbar.showSnackbar("Skipped ${item.habit.name} for today") }
                },
                onClick = { onOpenHabit(item.habit.id) },
                streak = item.streak,
                unit = item.unit,
                areaColorIndex = item.areaColor,
            )
        }
        if (pending.isEmpty()) {
            Text(
                "All done for today. Nicely handled.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.tealInk,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
        if (handled.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { showHandled = !showHandled }.padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${handled.size} done or skipped",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                Icon(if (showHandled) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null, tint = c.textTertiary)
            }
            AnimatedVisibility(showHandled) {
                Column {
                    handled.forEach { item ->
                        HabitRow(
                            habit = item.habit,
                            tick = when {
                                item.mark == MarkState.Skipped -> TickState.Skipped
                                else -> TickState.Done
                            },
                            onTick = { if (item.mark == MarkState.Skipped) vm.toggleSkip(item) else if (item.mark == MarkState.Done) vm.toggleDone(item) else onOpenHabit(item.habit.id) },
                            onSkip = { vm.toggleSkip(item) },
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
        Text(
            "Tip: long-press a habit to skip it for today.",
            style = MaterialTheme.typography.labelSmall,
            color = c.textTertiary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ReflectionCard(s: HomeUiState, onOpenJournal: (LocalDate) -> Unit, modifier: Modifier) {
    val c = MsTheme.colors
    val entry = s.journal
    MsCard(modifier.fillMaxWidth(), onClick = { onOpenJournal(LocalDate.now()) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(c.amber.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.AutoStories, contentDescription = null, tint = if (c.isDark) c.amber else Color(0xFF8A5A00))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Tonight's reflection", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                val words = entry?.words ?: 0
                Text(
                    when {
                        words == 0 -> "About ${JournalTargets.GOAL} words on how today went."
                        else -> "$words words · ${JournalTargets.status(words).label}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary)
        }
    }
}

@Composable
private fun StreaksCard(streaks: List<StreakItem>, modifier: Modifier) {
    val c = MsTheme.colors
    MsCard(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Current streaks")
        streaks.forEach { streak ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = c.amber)
                Spacer(Modifier.width(8.dp))
                Text(streak.name, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary, modifier = Modifier.weight(1f))
                Text(
                    streak.unit.count(streak.count),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = c.textSecondary,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(c.cardMuted).padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun GratitudeCard(onSave: (String) -> Unit, modifier: Modifier) {
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    MsCard(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Gratitude")
        MsTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = "One thing I'm thankful for today...",
            singleLine = false,
            minLines = 2,
        )
        AnimatedVisibility(visible = text.isNotBlank(), modifier = Modifier.align(Alignment.End)) {
            OutlinePillButton(
                "Save",
                leadingIcon = Icons.Rounded.Save,
                onClick = {
                    onSave(text)
                    text = ""
                    scope.launch { snackbar.showSnackbar("Saved to your gratitude journal") }
                },
                contentColor = MsTheme.colors.onAmber,
                borderColor = Color.Transparent,
                containerColor = MsTheme.colors.amber,
            )
        }
    }
}
