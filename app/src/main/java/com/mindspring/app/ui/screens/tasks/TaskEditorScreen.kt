package com.mindspring.app.ui.screens.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.data.model.AlertStyle
import com.mindspring.app.data.model.LifeArea
import com.mindspring.app.data.model.Project
import com.mindspring.app.data.model.Priority
import com.mindspring.app.data.model.Repeat
import com.mindspring.app.data.model.TaskStatus
import com.mindspring.app.domain.TaskLogic
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.AlertStylePicker
import com.mindspring.app.ui.components.AreaChooser
import com.mindspring.app.ui.components.BarIconButton
import com.mindspring.app.ui.components.DateField
import com.mindspring.app.ui.components.DotChip
import com.mindspring.app.ui.components.FieldLabel
import com.mindspring.app.ui.components.FlagPill
import com.mindspring.app.ui.components.MsDatePicker
import com.mindspring.app.ui.components.MsTextField
import com.mindspring.app.ui.components.PrimaryButton
import com.mindspring.app.ui.components.SegmentedTabs
import com.mindspring.app.ui.components.SmallChip
import com.mindspring.app.ui.components.SuggestionField
import com.mindspring.app.ui.components.TealTopBar
import com.mindspring.app.ui.components.TimePickerDialog
import com.mindspring.app.ui.components.rememberAskForNotifications
import com.mindspring.app.ui.components.msSwitchColors
import com.mindspring.app.ui.preview.PreviewAreas
import com.mindspring.app.ui.preview.PreviewProjects
import com.mindspring.app.ui.preview.PreviewScreen
import com.mindspring.app.ui.preview.PreviewToday
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.InputShape
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.theme.areaColor
import com.mindspring.app.ui.util.Fmt
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskEditorScreen(taskId: Long?, projectId: Long?, onDone: () -> Unit) {
    val vm = appViewModel { TaskEditorViewModel(it, taskId, projectId) }
    val s by vm.state.collectAsStateWithLifecycle()
    val areas by vm.areas.collectAsStateWithLifecycle()
    val projects by vm.projects.collectAsStateWithLifecycle()
    val subAreas by vm.subAreas.collectAsStateWithLifecycle()

    LaunchedEffect(s.saved) { if (s.saved) onDone() }

    TaskEditorContent(s, areas, projects, subAreas, vm, onDone)
}

/**
 * The screen as pure state plus an actions object, so it renders in a @Preview without a ViewModel
 * behind it. [TaskEditorScreen] is the thin wrapper that supplies both from the app's data.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskEditorContent(
    s: TaskEditorState,
    areas: List<LifeArea>,
    projects: List<Project>,
    subAreas: List<String>,
    actions: TaskEditorActions,
    onDone: () -> Unit,
) {
    val c = MsTheme.colors
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val today = LocalDate.now()

    Column(Modifier.fillMaxSize()) {
        TealTopBar(
            title = if (s.isEditing) "Task ${s.toTask().code}" else "New Task",
            navigationIcon = Icons.Rounded.Close,
            onNavigate = onDone,
            actions = { if (s.isEditing) BarIconButton(Icons.Rounded.DeleteOutline, "Delete task") { confirmDelete = true } },
        )
        Column(
            Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screen, vertical = Dimens.stackLg - 8.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.stackLg - 8.dp),
        ) {
            Column {
                FieldLabel("Task")
                MsTextField(
                    value = s.title,
                    onValueChange = actions::onTitle,
                    placeholder = "e.g. Submit the final APK",
                    singleLine = false,
                    isError = s.showErrors && s.titleError != null,
                    errorText = s.titleError,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                if (s.isEditing) {
                    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        FlagPill(TaskLogic.flag(s.toTask(), today))
                        TaskLogic.window(s.toTask())?.let {
                            Spacer(Modifier.width(8.dp))
                            Text(if (it == 1L) "Single-day task" else "$it-day window", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                        }
                    }
                }
            }

            Column {
                FieldLabel("Deadline")
                DateField(
                    value = s.due,
                    onChange = actions::onDue,
                    emptyText = "No deadline",
                    quick = listOf(
                        "Today" to today,
                        "Tomorrow" to today.plusDays(1),
                        "In 3 days" to today.plusDays(3),
                        "Next Monday" to today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)),
                        "In 2 weeks" to today.plusWeeks(2),
                    ),
                )
            }

            Column {
                FieldLabel("Can start from (optional)")
                DateField(value = s.start, onChange = actions::onStart, emptyText = "Any time", quick = listOf("Today" to today, "Tomorrow" to today.plusDays(1)))
                s.dateError?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = c.danger, modifier = Modifier.padding(start = 4.dp, top = 6.dp)) }
            }

            AlertSection(s, actions)

            Column {
                FieldLabel("Priority")
                SegmentedTabs(Priority.entries, s.priority, actions::onPriority, { "${it.short} · ${it.label}" })
            }

            Column {
                FieldLabel("Status")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskStatus.entries.forEach { st -> SmallChip(st.label, selected = st == s.status, onClick = { actions.onStatus(st) }) }
                }
                AnimatedVisibility(s.status == TaskStatus.Done) {
                    Column(Modifier.padding(top = 12.dp)) {
                        FieldLabel("Done on")
                        DateField(value = s.doneOn, onChange = actions::onDoneOn, emptyText = "Pick the day you finished")
                    }
                }
            }

            Column {
                FieldLabel("Project")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DotChip("None", c.textTertiary, selected = s.projectId == null, onClick = { actions.onProject(null) })
                    projects.filter { !it.archived || it.id == s.projectId }.forEach { p ->
                        DotChip(p.name, areaColor(p.colorIndex), selected = p.id == s.projectId, onClick = { actions.onProject(p) })
                    }
                }
                if (projects.isEmpty()) {
                    Text("Create projects from the Projects tab to group related tasks.", style = MaterialTheme.typography.labelSmall, color = c.textTertiary, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
                }
            }

            Column {
                FieldLabel("Repeat")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Repeat.entries.forEach { r -> SmallChip(r.label, selected = r == s.repeat, onClick = { actions.onRepeat(r) }) }
                }
                if (s.repeat != Repeat.None) {
                    Text(
                        "Finishing it schedules the next one automatically.",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textTertiary,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                    )
                }
            }

            Column {
                FieldLabel("Life area")
                AreaChooser(areas, s.areaId, actions::onArea)
            }

            Column {
                FieldLabel("Sub-area (optional)")
                SuggestionField(s.subArea, actions::onSubArea, subAreas, placeholder = "e.g. Assignments")
            }

            Column {
                FieldLabel("Notes (optional)")
                MsTextField(value = s.notes, onValueChange = actions::onNotes, placeholder = "Anything worth remembering", singleLine = false, minLines = 3)
            }
        }
        PrimaryButton(
            text = if (s.isEditing) "Save Changes" else "Add Task",
            onClick = actions::save,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Dimens.screen, vertical = Dimens.stackMd),
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this task?") },
            text = { Text("To keep it on record without counting it as a failure, set its status to Dropped instead.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; actions.delete(onDone) }) { Text("Delete", color = c.danger) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/**
 * An optional alert at a date and time, as a reminder banner or a ringing alarm. Quick picks cover
 * the usual choices; the pills open the full date and time pickers.
 */
@Composable
private fun AlertSection(s: TaskEditorState, actions: TaskEditorActions) {
    val c = MsTheme.colors
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var pickTime by rememberSaveable { mutableStateOf(false) }
    val askForNotifications = rememberAskForNotifications()

    val now = LocalDateTime.now()
    val today = now.toLocalDate()
    val alertAt = s.alertAt
    val on = alertAt != null

    Column {
        FieldLabel("Alert")
        Row(
            Modifier
                .fillMaxWidth()
                .clip(InputShape)
                .background(c.card)
                .clickable {
                    if (!on) askForNotifications()
                    actions.onAlertEnabled(!on)
                }
                .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (!on) Icons.Rounded.NotificationsNone else if (s.alertStyle == AlertStyle.Alarm) Icons.Rounded.Alarm else Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = if (on) c.tealInk else c.textTertiary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    alertAt?.let { Fmt.alert(it, today) } ?: "No alert",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (on) c.textPrimary else c.textTertiary,
                )
                if (on) Text(s.alertStyle.label, style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
            }
            Switch(
                checked = on,
                onCheckedChange = { enable ->
                    if (enable) askForNotifications()
                    actions.onAlertEnabled(enable)
                },
                colors = msSwitchColors(),
            )
        }

        AnimatedVisibility(on) {
            val at = alertAt ?: return@AnimatedVisibility
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickerPill(Icons.Rounded.Event, Fmt.friendlyDay(at.toLocalDate(), today), Modifier.weight(1f)) { pickDate = true }
                    PickerPill(Icons.Rounded.Schedule, Fmt.time(at.toLocalTime()), Modifier.weight(1f)) { pickTime = true }
                }
                val quick = buildList {
                    add("In 1 hour" to now.truncatedTo(ChronoUnit.MINUTES).plusHours(1))
                    if (now.hour < 19) add("Tonight 8 PM" to today.atTime(20, 0))
                    add("Tomorrow 9 AM" to today.plusDays(1).atTime(9, 0))
                    s.due?.minusDays(1)?.takeIf { it.isAfter(today.plusDays(1)) }?.let { add("Day before deadline" to it.atTime(9, 0)) }
                    s.due?.takeIf { it.isAfter(today.plusDays(1)) }?.let { add("Deadline 9 AM" to it.atTime(9, 0)) }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quick.forEach { (label, time) -> SmallChip(label, selected = at == time, onClick = { actions.onAlertAt(time) }) }
                }
                AlertStylePicker(s.alertStyle, actions::onAlertStyle, passed = s.alertPassed(now))
            }
        }
    }

    if (pickDate) {
        MsDatePicker(initial = alertAt?.toLocalDate() ?: today, onDismiss = { pickDate = false }) { actions.onAlertDate(it); pickDate = false }
    }
    if (pickTime) {
        TimePickerDialog(alertAt?.toLocalTime() ?: TaskEditorViewModel.DEFAULT_ALERT_TIME, onDismiss = { pickTime = false }, title = "Alert time") {
            actions.onAlertTime(it)
            pickTime = false
        }
    }
}

@Composable
private fun PickerPill(icon: ImageVector, text: String, modifier: Modifier, onClick: () -> Unit) {
    val c = MsTheme.colors
    Row(
        modifier.clip(InputShape).background(c.card).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.tealInk, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
    }
}

// --- Previews -------------------------------------------------------------------------------

private val previewEditorState = TaskEditorState(
    id = 1,
    title = "Draft the methodology chapter",
    notes = "Cover sampling and the analysis plan.",
    areaId = 2,
    subArea = "Dissertation",
    projectId = 1,
    due = PreviewToday.plusDays(2),
    priority = Priority.A,
    status = TaskStatus.InProgress,
    createdAt = PreviewToday.minusDays(9),
    alertAt = PreviewToday.plusDays(2).atTime(9, 0),
)

@Preview(name = "Task editor", showBackground = true, widthDp = 393, heightDp = 1200)
@Composable
private fun TaskEditorPreview() = PreviewScreen {
    TaskEditorContent(
        s = previewEditorState, areas = PreviewAreas, projects = PreviewProjects,
        subAreas = listOf("Dissertation", "Assignments", "Reading"),
        actions = TaskEditorActions.None, onDone = {},
    )
}

@Preview(name = "Task editor · new task", showBackground = true, widthDp = 393, heightDp = 1200)
@Composable
private fun TaskEditorNewPreview() = PreviewScreen {
    TaskEditorContent(
        s = TaskEditorState(), areas = PreviewAreas, projects = PreviewProjects,
        subAreas = listOf("Dissertation", "Assignments"),
        actions = TaskEditorActions.None, onDone = {},
    )
}

@Preview(name = "Task editor · dark", showBackground = true, widthDp = 393, heightDp = 1200)
@Composable
private fun TaskEditorDarkPreview() = PreviewScreen(dark = true) {
    TaskEditorContent(
        s = previewEditorState, areas = PreviewAreas, projects = PreviewProjects,
        subAreas = listOf("Dissertation", "Assignments"),
        actions = TaskEditorActions.None, onDone = {},
    )
}
