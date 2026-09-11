package com.mindspring.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.data.model.Mood
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.GhostButton
import com.mindspring.app.ui.components.LocalSnackbar
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.MsProgressBar
import com.mindspring.app.ui.components.MsTextField
import com.mindspring.app.ui.components.OutlinePillButton
import com.mindspring.app.ui.components.PrimaryButton
import com.mindspring.app.ui.components.SectionTitle
import com.mindspring.app.ui.components.color
import com.mindspring.app.ui.components.emoji
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.util.Fmt
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onCheckIn: (rating: Int?) -> Unit,
    onEditMood: (entryId: Long) -> Unit,
    onAddHabit: () -> Unit,
    onOpenReminders: () -> Unit,
) {
    val vm = appViewModel { HomeViewModel(it.auth, it.habits, it.moods, it.gratitude) }
    val s by vm.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(MsTheme.colors.canvas)
            .verticalScroll(rememberScrollState()),
    ) {
        GreetingHeader(s.firstName, onOpenReminders)
        Column(
            Modifier.padding(horizontal = Dimens.screen, vertical = Dimens.stackLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
        ) {
            MoodHeroCard(s, onCheckIn, onEditMood)
            TodayHabitsCard(s, onToggle = vm::setDone, onAddHabit = onAddHabit)
            StreaksCard(s.streaks)
            GratitudeCard(onSave = vm::saveGratitude)
        }
    }
}

@Composable
private fun GreetingHeader(firstName: String, onBell: () -> Unit) {
    val c = MsTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            .background(c.teal)
            .statusBarsPadding()
            .padding(horizontal = Dimens.screen, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (firstName.isBlank()) Fmt.greeting() else "${Fmt.greeting()}, $firstName",
                style = MaterialTheme.typography.titleLarge,
                color = c.onTeal,
            )
            Text(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                style = MaterialTheme.typography.bodySmall,
                color = c.onTealMuted,
            )
        }
        IconButton(
            onClick = onBell,
            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.18f)),
        ) {
            Icon(Icons.Outlined.Notifications, contentDescription = "Reminders", tint = c.onTeal)
        }
    }
}

@Composable
private fun MoodHeroCard(s: HomeUiState, onCheckIn: (Int?) -> Unit, onEditMood: (Long) -> Unit) {
    val c = MsTheme.colors
    MsCard(
        Modifier.fillMaxWidth(),
        color = c.hero,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
    ) {
        val today = s.todayMood
        if (today == null) {
            Text("How are you feeling today?", style = MaterialTheme.typography.titleLarge, color = c.onTeal, textAlign = TextAlign.Center)
            Row(
                Modifier.fillMaxWidth().widthIn(max = 320.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
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

@Composable
private fun TodayHabitsCard(s: HomeUiState, onToggle: (Long, Boolean) -> Unit, onAddHabit: () -> Unit) {
    val c = MsTheme.colors
    MsCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Today's Habits") {
            if (s.habits.isNotEmpty()) {
                Text("${s.doneCount} of ${s.habits.size} complete", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
            }
        }
        if (s.habits.isEmpty()) {
            Text("No habits scheduled for today.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            GhostButton("Add a habit", onClick = onAddHabit)
            return@MsCard
        }
        MsProgressBar(s.doneCount.toFloat() / s.habits.size)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            s.habits.forEach { item ->
                val bg by animateColorAsState(if (item.done) c.cardSubtle else Color.Transparent, label = "habitRow")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .clickable { onToggle(item.habit.id, !item.done) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (item.done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = if (item.done) "Completed" else "Not completed",
                        tint = if (item.done) c.tealInk else c.textTertiary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        item.habit.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (item.done) c.textSecondary else c.textPrimary,
                        textDecoration = if (item.done) TextDecoration.LineThrough else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun StreaksCard(streaks: List<StreakItem>) {
    val c = MsTheme.colors
    MsCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Current Streaks")
        if (streaks.isEmpty()) {
            Text("Complete a habit to start your first streak.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        }
        streaks.forEach { streak ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = c.amber)
                Spacer(Modifier.width(8.dp))
                Text(streak.name, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary, modifier = Modifier.weight(1f))
                Text(
                    "${streak.days} ${if (streak.days == 1) "day" else "days"}",
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
private fun GratitudeCard(onSave: (String) -> Unit) {
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    MsCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Gratitude")
        MsTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = "One thing I'm thankful for today...",
            singleLine = false,
            minLines = 3,
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
