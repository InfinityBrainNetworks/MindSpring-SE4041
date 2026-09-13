package com.mindspring.app.ui.screens.journal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.data.model.Mood
import com.mindspring.app.domain.JournalStatus
import com.mindspring.app.domain.JournalTargets
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.AnimatedCount
import com.mindspring.app.ui.components.FieldLabel
import com.mindspring.app.ui.components.MoodFaceButton
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.MsProgressBar
import com.mindspring.app.ui.components.MsTextField
import com.mindspring.app.ui.components.OutlinePillButton
import com.mindspring.app.ui.components.Pill
import com.mindspring.app.ui.components.PrimaryButton
import com.mindspring.app.ui.components.TealTopBar
import com.mindspring.app.ui.components.Tint
import com.mindspring.app.ui.components.appear
import com.mindspring.app.ui.components.color
import com.mindspring.app.ui.components.icon
import com.mindspring.app.ui.preview.PreviewJournalListState
import com.mindspring.app.ui.preview.PreviewScreen
import com.mindspring.app.ui.preview.PreviewToday
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsColors
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.util.Fmt
import java.time.LocalDate

private fun JournalStatus.tint(c: MsColors): Tint = when (this) {
    JournalStatus.Good -> Tint(c.cardSubtle, c.tealInk)
    JournalStatus.TooShort, JournalStatus.TooLong -> Tint(c.warnSoft, if (c.isDark) Color(0xFFF2CB86) else Color(0xFF8A5A00))
    JournalStatus.NotWritten -> Tint(c.cardMuted, c.textTertiary)
}

/** The Journal sheet: one row per day, the last two weeks always shown so a missing day is visible. */
@Composable
fun JournalListScreen(onBack: () -> Unit, onOpenDay: (LocalDate) -> Unit) {
    val vm = appViewModel { JournalListViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()

    JournalListContent(s = s, onBack = onBack, onOpenDay = onOpenDay)
}

/**
 * The screen as pure state and callbacks, so it renders in a @Preview without a ViewModel behind
 * it. [JournalListScreen] is the thin wrapper that supplies both from the app's data.
 */
@Composable
fun JournalListContent(s: JournalListState, onBack: () -> Unit, onOpenDay: (LocalDate) -> Unit) {
    val c = MsTheme.colors

    Column(Modifier.fillMaxSize()) {
        TealTopBar("Daily Reflection", onNavigate = onBack)
        LazyColumn(
            contentPadding = PaddingValues(Dimens.screen),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "summary") {
                val m = s.month
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).appear(0), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryTile("Writing streak", Modifier.weight(1f).fillMaxHeight()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = c.amber, modifier = Modifier.size(20.dp))
                            AnimatedCount(s.streak, style = MaterialTheme.typography.headlineSmall, color = c.textPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    SummaryTile("This month", Modifier.weight(1f).fillMaxHeight()) {
                        Text("${m?.written ?: 0}/${m?.elapsedDays ?: 0}", style = MaterialTheme.typography.headlineSmall, color = c.textPrimary, fontWeight = FontWeight.Bold)
                    }
                    SummaryTile("Avg words", Modifier.weight(1f).fillMaxHeight()) {
                        Text(m?.avgWords?.let { "${it.toInt()}" } ?: "–", style = MaterialTheme.typography.headlineSmall, color = c.tealInk, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item(key = "intro") {
                Text(
                    "About ${JournalTargets.GOAL} words, honest rather than polished. Rating and energy are optional.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            items(s.days, key = { it.date.toString() }) { day ->
                DayRow(day.date, day.entry?.words ?: 0, day.entry?.rating, day.entry?.monologue.orEmpty(), Modifier.animateItem()) { onOpenDay(day.date) }
            }
            if (s.older.isNotEmpty()) {
                item(key = "older") {
                    Text("EARLIER", style = MaterialTheme.typography.labelSmall, color = c.textSecondary, modifier = Modifier.padding(start = 4.dp, top = 12.dp))
                }
                items(s.older, key = { "old-${it.date}" }) { e ->
                    DayRow(e.date, e.words, e.rating, e.monologue, Modifier.animateItem()) { onOpenDay(e.date) }
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(label: String, modifier: Modifier, value: @Composable () -> Unit) {
    val c = MsTheme.colors
    MsCard(modifier, contentPadding = PaddingValues(14.dp)) {
        value()
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
    }
}

@Composable
private fun DayRow(date: LocalDate, words: Int, rating: Int?, text: String, modifier: Modifier, onClick: () -> Unit) {
    val c = MsTheme.colors
    val status = JournalTargets.status(words)
    MsCard(modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Fmt.weekdayShort(date).uppercase(), style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                Text("${date.dayOfMonth}", style = MaterialTheme.typography.titleLarge, color = if (date == LocalDate.now()) c.amber else c.textPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(status.label, status.tint(c))
                    if (words > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text("$words words", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                    }
                }
                Text(
                    text.ifBlank { if (date == LocalDate.now()) "Tap to write today's reflection" else "Nothing written" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (text.isBlank()) c.textTertiary else c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (rating != null) {
                val mood = Mood.fromRating(rating)
                Spacer(Modifier.width(8.dp))
                Icon(mood.icon, contentDescription = mood.label, tint = mood.color, modifier = Modifier.size(24.dp))
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary)
        }
    }
}

@Composable
fun JournalEntryScreen(date: LocalDate, onDone: () -> Unit) {
    val vm = appViewModel(key = "journal-$date") { JournalEditorViewModel(it, date) }
    val s by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(s.saved) { if (s.saved) onDone() }

    JournalEntryContent(
        date = date,
        s = s,
        onRating = vm::onRating,
        onEnergy = vm::onEnergy,
        onHighlight = vm::onHighlight,
        onMonologue = vm::onMonologue,
        onTomorrow = vm::onTomorrow,
        onAddTomorrowAsTask = vm::addTomorrowAsTask,
        onSave = vm::save,
        onDone = onDone,
    )
}

/**
 * The screen as pure state and callbacks, so it renders in a @Preview without a ViewModel behind
 * it. [JournalEntryScreen] is the thin wrapper that supplies both from the app's data.
 */
@Composable
fun JournalEntryContent(
    date: LocalDate,
    s: JournalEditorState,
    onRating: (Int) -> Unit,
    onEnergy: (Int) -> Unit,
    onHighlight: (String) -> Unit,
    onMonologue: (String) -> Unit,
    onTomorrow: (String) -> Unit,
    onAddTomorrowAsTask: () -> Unit,
    onSave: () -> Unit,
    onDone: () -> Unit,
) {
    val c = MsTheme.colors

    Column(Modifier.fillMaxSize()) {
        TealTopBar(if (date == LocalDate.now()) "Tonight's Reflection" else Fmt.shortDay(date), onNavigate = onDone)
        Column(
            Modifier.weight(1f).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Dimens.screen, vertical = Dimens.stackLg - 8.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.stackLg - 8.dp),
        ) {
            Text(Fmt.longDay(date), style = MaterialTheme.typography.titleMedium, color = c.textSecondary)

            Column {
                FieldLabel("How was the day?")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Mood.entries.forEach { m -> MoodFaceButton(m, selected = s.rating == m.rating, onClick = { onRating(m.rating) }, size = 52.dp) }
                }
            }

            Column {
                FieldLabel("Energy")
                EnergyBar(s.energy, onEnergy)
            }

            Column {
                FieldLabel("Highlight of the day")
                MsTextField(
                    value = s.highlight,
                    onValueChange = onHighlight,
                    placeholder = "The best part, in a line",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }

            Column {
                FieldLabel("Monologue")
                MsTextField(
                    value = s.monologue,
                    onValueChange = onMonologue,
                    placeholder = "What happened, how it felt, what you noticed. No one else reads this.",
                    singleLine = false,
                    minLines = 8,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Spacer(Modifier.height(10.dp))
                WordMeter(s.words)
            }

            Column {
                FieldLabel("Tomorrow's #1")
                MsTextField(
                    value = s.tomorrowTop,
                    onValueChange = onTomorrow,
                    placeholder = "The one thing that would make tomorrow a win",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                if (s.tomorrowTop.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    OutlinePillButton(
                        if (s.taskAdded) "Added to tomorrow's tasks" else "Add as a task for tomorrow",
                        leadingIcon = if (s.taskAdded) Icons.Rounded.CheckCircle else Icons.Rounded.AddTask,
                        onClick = onAddTomorrowAsTask,
                        contentColor = c.tealInk,
                        borderColor = c.tealInk.copy(alpha = 0.4f),
                    )
                }
            }
        }
        PrimaryButton(
            "Save Reflection",
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Dimens.screen, vertical = Dimens.stackMd),
        )
    }
}

/** Five bolts; tap one to set the level, tap it again to clear. */
@Composable
private fun EnergyBar(value: Int?, onChange: (Int) -> Unit) {
    val c = MsTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        (1..5).forEach { level ->
            val on = value != null && level <= value
            val bg by animateColorAsState(if (on) c.amber else c.card, label = "energy")
            val pop by animateFloatAsState(if (on) 1f else 0.92f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "energyPop")
            Box(
                Modifier.scale(pop).size(46.dp).clip(RoundedCornerShape(14.dp)).background(bg).clickable { onChange(level) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Bolt, contentDescription = "Energy $level of 5", tint = if (on) c.onAmber else c.textTertiary)
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(
            when (value) {
                null -> "Optional"
                1 -> "Drained"
                2 -> "Low"
                3 -> "Steady"
                4 -> "Good"
                else -> "Full"
            },
            style = MaterialTheme.typography.labelMedium,
            color = c.textSecondary,
        )
    }
}

/** Live word count against the ~150 word aim, with the 120-180 band read as a good length. */
@Composable
private fun WordMeter(words: Int) {
    val c = MsTheme.colors
    val status = JournalTargets.status(words)
    val color = when (status) {
        JournalStatus.Good -> c.tealInk
        JournalStatus.NotWritten -> c.textTertiary
        else -> c.amber
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        MsProgressBar(words / JournalTargets.GOAL.toFloat(), Modifier.weight(1f), color = color, height = 6.dp)
        Spacer(Modifier.width(12.dp))
        Text("$words / ${JournalTargets.GOAL}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
    Text(
        when (status) {
            JournalStatus.NotWritten -> "Aim for about ${JournalTargets.GOAL} words."
            JournalStatus.TooShort -> "${JournalTargets.LOW - words} more words reaches a good length."
            JournalStatus.Good -> "A good length."
            JournalStatus.TooLong -> "Running long. That's fine if it helps."
        },
        style = MaterialTheme.typography.labelSmall,
        color = c.textTertiary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

// --- Previews -------------------------------------------------------------------------------

@Preview(name = "Journal list", showBackground = true, widthDp = 393, heightDp = 900)
@Composable
private fun JournalListPreview() = PreviewScreen {
    JournalListContent(s = PreviewJournalListState, onBack = {}, onOpenDay = {})
}

@Preview(name = "Journal list · dark", showBackground = true, widthDp = 393, heightDp = 900)
@Composable
private fun JournalListDarkPreview() = PreviewScreen(dark = true) {
    JournalListContent(s = PreviewJournalListState, onBack = {}, onOpenDay = {})
}

private val previewJournalEditor = JournalEditorState(
    date = PreviewToday,
    rating = 4,
    energy = 3,
    highlight = "Finally cracked the methodology outline.",
    monologue = "A slow start, then two solid hours on the proposal. The run helped more than the " +
        "coffee did. Tomorrow I want to get the sampling section down before lunch.",
    tomorrowTop = "Sampling section, before lunch",
    exists = true,
)

@Preview(name = "Journal entry", showBackground = true, widthDp = 393, heightDp = 1100)
@Composable
private fun JournalEntryPreview() = PreviewScreen {
    JournalEntryContent(
        date = PreviewToday, s = previewJournalEditor, onRating = {}, onEnergy = {}, onHighlight = {},
        onMonologue = {}, onTomorrow = {}, onAddTomorrowAsTask = {}, onSave = {}, onDone = {},
    )
}

@Preview(name = "Journal entry · blank", showBackground = true, widthDp = 393, heightDp = 1100)
@Composable
private fun JournalEntryBlankPreview() = PreviewScreen {
    JournalEntryContent(
        date = PreviewToday, s = JournalEditorState(date = PreviewToday), onRating = {}, onEnergy = {},
        onHighlight = {}, onMonologue = {}, onTomorrow = {}, onAddTomorrowAsTask = {}, onSave = {}, onDone = {},
    )
}
