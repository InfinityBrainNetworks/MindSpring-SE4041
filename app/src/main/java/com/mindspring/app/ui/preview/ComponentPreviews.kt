package com.mindspring.app.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mindspring.app.data.model.HabitFrequency
import com.mindspring.app.data.model.Mood
import com.mindspring.app.data.model.Priority
import com.mindspring.app.domain.TaskFlag
import com.mindspring.app.ui.components.AreaTag
import com.mindspring.app.ui.components.AvatarCircle
import com.mindspring.app.ui.components.BrandTopBar
import com.mindspring.app.ui.components.DayToggle
import com.mindspring.app.ui.components.EmptyState
import com.mindspring.app.ui.components.FieldLabel
import com.mindspring.app.ui.components.FlagPill
import com.mindspring.app.ui.components.FrequencyPill
import com.mindspring.app.ui.components.GhostButton
import com.mindspring.app.ui.components.GroupHeader
import com.mindspring.app.ui.components.HabitRow
import com.mindspring.app.ui.components.IconCircle
import com.mindspring.app.ui.components.MainTab
import com.mindspring.app.ui.components.MoodFaceButton
import com.mindspring.app.ui.components.MoodTrendChart
import com.mindspring.app.ui.components.MsBottomBar
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.MsProgressBar
import com.mindspring.app.ui.components.MsTextField
import com.mindspring.app.ui.components.OutlinePillButton
import com.mindspring.app.ui.components.PrimaryButton
import com.mindspring.app.ui.components.PriorityBadge
import com.mindspring.app.ui.components.SecondaryButton
import com.mindspring.app.ui.components.SectionTitle
import com.mindspring.app.ui.components.SelectChip
import com.mindspring.app.ui.components.TagChip
import com.mindspring.app.ui.components.TaskRow
import com.mindspring.app.ui.components.TaskTopBar
import com.mindspring.app.ui.components.TealTopBar
import com.mindspring.app.ui.components.TickState
import com.mindspring.app.ui.components.WeekDots
import com.mindspring.app.ui.components.Wordmark
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MindSpringTheme
import com.mindspring.app.ui.theme.MsTheme

/**
 * Live previews of the design system, one @Preview per group. Open this file in Android Studio
 * and use the Split or Design view on the right to see and edit them.
 *
 * Ambient motion is switched off so a preview renders a still frame rather than an animation that
 * never settles; run a preview in interactive mode (the pointer icon on the preview) to get the
 * taps, ripples and transitions back.
 */
@Composable
private fun Gallery(dark: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    MindSpringTheme(darkTheme = dark, ambientMotion = false) {
        Surface(color = MsTheme.colors.canvas) {
            Column(
                Modifier.fillMaxWidth().padding(Dimens.gutter),
                verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
                content = content,
            )
        }
    }
}

@Preview(name = "Buttons", showBackground = true, widthDp = 393)
@Composable
private fun ButtonsPreview() = Gallery {
    SectionTitle("Buttons")
    PrimaryButton("Save habit", onClick = {}, modifier = Modifier.fillMaxWidth())
    PrimaryButton("Add task", onClick = {}, modifier = Modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.Add)
    PrimaryButton("Continue", onClick = {}, modifier = Modifier.fillMaxWidth(), trailingIcon = Icons.Rounded.ArrowForward)
    PrimaryButton("Disabled", onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false)
    SecondaryButton("Cancel", onClick = {}, modifier = Modifier.fillMaxWidth())
    OutlinePillButton("This week", onClick = {})
    GhostButton("Skip", onClick = {})
}

@Preview(name = "Buttons · dark", showBackground = true, widthDp = 393)
@Composable
private fun ButtonsDarkPreview() = Gallery(dark = true) {
    SectionTitle("Buttons")
    PrimaryButton("Save habit", onClick = {}, modifier = Modifier.fillMaxWidth())
    SecondaryButton("Cancel", onClick = {}, modifier = Modifier.fillMaxWidth())
    OutlinePillButton("This week", onClick = {})
    GhostButton("Skip", onClick = {})
}

@Preview(name = "Pills and badges", showBackground = true, widthDp = 393)
@Composable
private fun PillsPreview() = Gallery {
    SectionTitle("Task flags")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskFlag.entries.forEach { FlagPill(it) }
    }
    SectionTitle("Habit frequencies")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HabitFrequency.entries.forEach { FrequencyPill(it) }
    }
    SectionTitle("Priority")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Priority.entries.forEach { PriorityBadge(it) }
    }
    SectionTitle("Life areas")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Health", "Study", "Finance", "Relationships", "Home").forEachIndexed { i, name ->
            AreaTag(name, colorIndex = i)
        }
    }
}

@Preview(name = "Habit rows", showBackground = true, widthDp = 393)
@Composable
private fun HabitRowsPreview() = Gallery {
    GroupHeader("Today", count = 4)
    HabitRow(
        habit = PreviewHabits[0], tick = TickState.Done, onTick = {}, onSkip = {}, onClick = {},
        streak = 12, areaColorIndex = 1,
    )
    HabitRow(
        habit = PreviewHabits[1], tick = TickState.Empty, onTick = {}, onSkip = {}, onClick = {},
        streak = 4, areaColorIndex = 0, note = "5 km before work",
    )
    HabitRow(
        habit = PreviewHabits[3], tick = TickState.Skipped, onTick = {}, onSkip = {}, onClick = {},
        streak = 3, areaColorIndex = 4,
    )
    HabitRow(
        habit = PreviewHabits[2], tick = TickState.Off, onTick = {}, onSkip = {}, onClick = {},
        areaColorIndex = 2,
    )
}

@Preview(name = "Task rows", showBackground = true, widthDp = 393)
@Composable
private fun TaskRowsPreview() = Gallery {
    GroupHeader("Up next", count = 3)
    PreviewTaskLines.forEach { line ->
        TaskRow(
            task = line.task, flag = line.flag, onToggle = {}, onClick = {},
            projectName = line.projectName, today = PreviewToday,
        )
    }
    GroupHeader("Done")
    TaskRow(
        task = PreviewTasks[4], flag = TaskFlag.DoneOnTime, onToggle = {}, onClick = {},
        today = PreviewToday,
    )
}

@Preview(name = "Cards and surfaces", showBackground = true, widthDp = 393)
@Composable
private fun SurfacesPreview() = Gallery {
    SectionTitle("Card")
    MsCard {
        SectionTitle("Reflection")
        FieldLabel("Highlight")
        TagChip("4 / 5")
    }
    SectionTitle("Circles")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconCircle(Icons.Rounded.EditNote)
        AvatarCircle("Asan Perera")
        AvatarCircle("Maya", size = 48.dp)
    }
    SectionTitle("Empty state")
    MsCard {
        EmptyState(
            icon = Icons.Rounded.EditNote,
            title = "No tasks yet",
            message = "Add your first task and it will show up here.",
        )
    }
}

@Preview(name = "Inputs", showBackground = true, widthDp = 393)
@Composable
private fun InputsPreview() = Gallery {
    var text by remember { mutableStateOf("Draft the methodology chapter") }
    var password by remember { mutableStateOf("hunter2") }
    var selected by remember { mutableStateOf("Weekdays") }
    var days by remember { mutableStateOf(setOf(0, 1, 2)) }

    SectionTitle("Text fields")
    MsTextField(text, { text = it }, label = "Task title", modifier = Modifier.fillMaxWidth())
    MsTextField(
        "", {}, label = "Email", placeholder = "you@example.com",
        isError = true, errorText = "Enter a valid email address", modifier = Modifier.fillMaxWidth(),
    )
    MsTextField(password, { password = it }, label = "Password", isPassword = true, modifier = Modifier.fillMaxWidth())

    SectionTitle("Chips")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Daily", "Weekdays", "Weekends", "Weekly").forEach {
            SelectChip(it, selected = selected == it, onClick = { selected = it })
        }
    }

    SectionTitle("Day toggles")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, letter ->
            DayToggle(letter, selected = days.contains(i), onClick = {
                days = if (days.contains(i)) days - i else days + i
            })
        }
    }
}

@Preview(name = "Mood and progress", showBackground = true, widthDp = 393)
@Composable
private fun VisualsPreview() = Gallery {
    var mood by remember { mutableStateOf(Mood.Good) }
    SectionTitle("Mood faces")
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Mood.entries.forEach { MoodFaceButton(it, selected = mood == it, onClick = { mood = it }) }
    }
    SectionTitle("Week dots")
    WeekDots(target = 5, completed = 3)
    WeekDots(target = 5, completed = 5)
    SectionTitle("Progress")
    MsProgressBar(progress = 0.25f)
    MsProgressBar(progress = 0.7f)
    MsProgressBar(progress = 1f)
    SectionTitle("Wordmark")
    Wordmark()
}

@Preview(name = "Charts", showBackground = true, widthDp = 393)
@Composable
private fun ChartsPreview() = Gallery {
    SectionTitle("Mood trend")
    MoodTrendChart(
        values = listOf(3f, 4f, 2f, 4f, 5f, 4f, 4f),
        labels = listOf("M", "T", "W", "T", "F", "S", "S"),
    )
}

@Preview(name = "Top bars", showBackground = true, widthDp = 393)
@Composable
private fun TopBarsPreview() = MindSpringTheme(ambientMotion = false) {
    Surface(color = MsTheme.colors.canvas) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.stackMd)) {
            BrandTopBar(userName = "Asan", onAvatarClick = {})
            TealTopBar(title = "Insights", onNavigate = {})
            TaskTopBar(title = "Edit task", onBack = {}, actions = {
                IconCircle(Icons.Rounded.MoreVert, size = 32.dp, iconSize = 18.dp)
            })
        }
    }
}

@Preview(name = "Bottom bar", showBackground = true, widthDp = 393)
@Composable
private fun BottomBarPreview() = MindSpringTheme(ambientMotion = false) {
    var tab by remember { mutableStateOf(MainTab.Today) }
    Surface(color = MsTheme.colors.canvas) {
        MsBottomBar(selected = tab, onSelect = { tab = it })
    }
}

@Preview(name = "Bottom bar · dark", showBackground = true, widthDp = 393)
@Composable
private fun BottomBarDarkPreview() = MindSpringTheme(darkTheme = true, ambientMotion = false) {
    var tab by remember { mutableStateOf(MainTab.Insights) }
    Surface(color = MsTheme.colors.canvas) {
        MsBottomBar(selected = tab, onSelect = { tab = it })
    }
}
