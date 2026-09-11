package com.mindspring.app.ui.screens.habits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.data.model.AlertStyle
import com.mindspring.app.data.model.HabitFrequency
import com.mindspring.app.data.model.HabitIcon
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.AlertStylePicker
import com.mindspring.app.ui.components.AreaChooser
import com.mindspring.app.ui.components.DayToggle
import com.mindspring.app.ui.components.FieldLabel
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.MsTextField
import com.mindspring.app.ui.components.PrimaryButton
import com.mindspring.app.ui.components.SelectChip
import com.mindspring.app.ui.components.SuggestionField
import com.mindspring.app.ui.components.TealTopBar
import com.mindspring.app.ui.components.TimePickerDialog
import com.mindspring.app.ui.components.msSwitchColors
import com.mindspring.app.ui.components.rememberAskForNotifications
import com.mindspring.app.ui.components.tint
import com.mindspring.app.ui.components.vector
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.util.Fmt
import java.time.DayOfWeek

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitEditorScreen(habitId: Long?, onDone: () -> Unit) {
    val vm = appViewModel { HabitEditorViewModel(it, habitId) }
    val s by vm.state.collectAsStateWithLifecycle()
    val areas by vm.areas.collectAsStateWithLifecycle()
    val subAreas by vm.subAreas.collectAsStateWithLifecycle()
    val c = MsTheme.colors
    var pickTime by rememberSaveable { mutableStateOf(false) }
    val askForNotifications = rememberAskForNotifications()

    LaunchedEffect(s.saved) { if (s.saved) onDone() }

    Column(Modifier.fillMaxSize()) {
        TealTopBar(
            title = if (s.isEditing) "Edit Habit" else "Add Habit",
            navigationIcon = Icons.Rounded.Close,
            onNavigate = onDone,
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
                FieldLabel("Habit")
                MsTextField(
                    value = s.name,
                    onValueChange = vm::onName,
                    placeholder = "e.g. Drink 2.5 L water",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }

            Column {
                FieldLabel("How often")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HabitFrequency.entries.forEach { f ->
                        val t = f.tint(c)
                        SelectChip(f.label, selected = f == s.frequency, onClick = { vm.onFrequency(f) }, selectedColor = t.ink, selectedContent = if (c.isDark) Color(0xFF10201D) else Color.White)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    when (s.frequency) {
                        HabitFrequency.Daily -> "Every day."
                        HabitFrequency.Weekdays -> "Monday to Friday. Weekends are days off."
                        HabitFrequency.Weekends -> "Saturday and Sunday only."
                        HabitFrequency.Custom -> when (s.customDays.size) {
                            0 -> "Pick at least one day."
                            else -> "${s.customDays.size} days a week, on the days you pick."
                        }
                        HabitFrequency.Weekly -> "Once a week, on any day that suits."
                        HabitFrequency.Monthly -> "Once a month, on any day that suits."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (s.frequency == HabitFrequency.Custom && s.customDays.isEmpty()) c.danger else c.textSecondary,
                    modifier = Modifier.padding(start = 4.dp),
                )
                AnimatedVisibility(s.frequency == HabitFrequency.Custom) {
                    MsCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DayOfWeek.entries.forEach { day ->
                                DayToggle(
                                    letter = day.name.take(1),
                                    selected = day in s.customDays,
                                    onClick = { vm.onToggleDay(day) },
                                    modifier = Modifier.weight(1f, fill = false).size(40.dp).aspectRatio(1f),
                                )
                            }
                        }
                    }
                }
            }

            Column {
                FieldLabel("Target (optional)")
                MsTextField(value = s.target, onValueChange = vm::onTarget, placeholder = "e.g. 8k steps, 20 min, 3 lines")
            }

            Column {
                FieldLabel("Reminder")
                MsCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            Modifier.weight(1f).clip(MaterialTheme.shapes.small).clickable(enabled = s.reminderEnabled) { pickTime = true }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (s.reminderEnabled && s.reminderStyle == AlertStyle.Alarm) Icons.Outlined.Alarm else Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = if (s.reminderEnabled) c.tealInk else c.textTertiary,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(Fmt.time(s.reminderTime), style = MaterialTheme.typography.bodyLarge, color = if (s.reminderEnabled) c.textPrimary else c.textTertiary)
                                Text(
                                    if (s.reminderEnabled) "Tap to change the time" else "Reminder off",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.textTertiary,
                                )
                            }
                        }
                        Switch(
                            checked = s.reminderEnabled,
                            onCheckedChange = { on ->
                                if (on) askForNotifications()
                                vm.onReminderEnabled(on)
                            },
                            colors = msSwitchColors(),
                        )
                    }
                    AnimatedVisibility(s.reminderEnabled) {
                        AlertStylePicker(s.reminderStyle, vm::onReminderStyle, Modifier.padding(top = 14.dp))
                    }
                }
                if (s.reminderEnabled) {
                    Text(
                        "Goes off on the days this habit is due, and stays quiet once it's ticked or skipped for the day.",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textTertiary,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    )
                }
            }

            Column {
                FieldLabel("Life area")
                AreaChooser(areas, s.areaId, vm::onArea)
            }

            Column {
                FieldLabel("Sub-area (optional)")
                SuggestionField(s.subArea, vm::onSubArea, subAreas, placeholder = "e.g. Physical Health")
            }

            Column {
                FieldLabel("Icon")
                MsCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HabitIcon.entries.chunked(6).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            row.forEach { icon ->
                                val selected = icon == s.icon
                                val bg by animateColorAsState(if (selected) c.teal else Color.Transparent, label = "iconBg")
                                Box(
                                    Modifier.size(44.dp).clip(CircleShape).background(bg).clickable { vm.onIcon(icon) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(icon.vector, contentDescription = icon.name, tint = if (selected) c.onTeal else c.tealInk)
                                }
                            }
                            repeat(6 - row.size) { Spacer(Modifier.size(44.dp)) }
                        }
                    }
                }
            }

            if (s.isEditing) {
                MsCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Active", style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
                            Text(
                                "Turn off to retire it. History is kept and it stops counting.",
                                style = MaterialTheme.typography.labelSmall,
                                color = c.textTertiary,
                            )
                        }
                        Switch(checked = s.active, onCheckedChange = vm::onActive, colors = msSwitchColors())
                    }
                }
            }
        }
        PrimaryButton(
            text = if (s.isEditing) "Save Changes" else "Save Habit",
            onClick = vm::save,
            enabled = s.canSave,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Dimens.screen, vertical = Dimens.stackMd),
        )
    }

    if (pickTime) {
        TimePickerDialog(initial = s.reminderTime, onDismiss = { pickTime = false }) {
            vm.onReminderTime(it)
            pickTime = false
        }
    }
}
