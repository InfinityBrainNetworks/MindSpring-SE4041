package com.mindspring.app.ui.screens.habits

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
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
import com.mindspring.app.data.model.HabitCategory
import com.mindspring.app.data.model.HabitIcon
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.DayToggle
import com.mindspring.app.ui.components.FieldLabel
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.MsTextField
import com.mindspring.app.ui.components.PrimaryButton
import com.mindspring.app.ui.components.SelectChip
import com.mindspring.app.ui.components.TealTopBar
import com.mindspring.app.ui.components.msSwitchColors
import com.mindspring.app.ui.components.vector
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.util.Fmt
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
fun HabitEditorScreen(habitId: Long?, onDone: () -> Unit) {
    val vm = appViewModel { HabitEditorViewModel(it.habits, habitId) }
    val s by vm.state.collectAsStateWithLifecycle()
    val c = MsTheme.colors
    var pickTime by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(s.saved) { if (s.saved) onDone() }

    Column(Modifier.fillMaxSize().background(c.canvas)) {
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
                .padding(horizontal = Dimens.screen, vertical = Dimens.stackLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.stackLg),
        ) {
            Column {
                FieldLabel("Habit Name")
                MsTextField(
                    value = s.name,
                    onValueChange = vm::onName,
                    placeholder = "e.g., Drink Water",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }

            Column {
                FieldLabel("Category")
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HabitCategory.entries.forEach { category ->
                        SelectChip(
                            text = category.label,
                            selected = category == s.category,
                            onClick = { vm.onCategory(category) },
                            selectedColor = c.amber,
                            selectedContent = c.onAmber,
                        )
                    }
                }
            }

            Column {
                FieldLabel("Icon")
                MsCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HabitIcon.entries.chunked(5).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            row.forEach { icon ->
                                val selected = icon == s.icon
                                val bg by animateColorAsState(if (selected) c.teal else Color.Transparent, label = "iconBg")
                                Box(
                                    Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(bg)
                                        .clickable { vm.onIcon(icon) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(icon.vector, contentDescription = icon.name, tint = if (selected) c.onTeal else c.tealInk)
                                }
                            }
                        }
                    }
                }
            }

            Column {
                FieldLabel("Frequency")
                MsCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DayOfWeek.entries.forEach { day ->
                            DayToggle(
                                letter = day.name.take(1),
                                selected = day in s.days,
                                onClick = { vm.onToggleDay(day) },
                                modifier = Modifier.weight(1f, fill = false).size(40.dp).aspectRatio(1f),
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        when (s.days.size) {
                            0 -> "Pick at least one day."
                            7 -> "Every day"
                            else -> "${s.days.size} days a week"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (s.days.isEmpty()) c.danger else c.textSecondary,
                    )
                }
            }

            Column {
                FieldLabel("Reminder")
                MsCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.small)
                                .clickable(enabled = s.reminderEnabled) { pickTime = true }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = c.tealInk)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    Fmt.time(s.reminderTime),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (s.reminderEnabled) c.textPrimary else c.textTertiary,
                                )
                                Text(
                                    if (s.reminderEnabled) "Tap to change time" else "Reminder off",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.textTertiary,
                                )
                            }
                        }
                        Switch(checked = s.reminderEnabled, onCheckedChange = vm::onReminderEnabled, colors = msSwitchColors())
                    }
                }
            }
        }
        PrimaryButton(
            text = if (s.isEditing) "Save Changes" else "Save Habit",
            onClick = vm::save,
            enabled = s.canSave,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.screen, vertical = Dimens.stackMd),
        )
    }

    if (pickTime) {
        TimePickerDialog(initial = s.reminderTime, onDismiss = { pickTime = false }) {
            vm.onReminderTime(it)
            pickTime = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val c = MsTheme.colors
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Reminder time") },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = c.cardMuted,
                    selectorColor = c.teal,
                    timeSelectorSelectedContainerColor = c.amber,
                    timeSelectorSelectedContentColor = c.onAmber,
                    timeSelectorUnselectedContainerColor = c.cardMuted,
                    periodSelectorSelectedContainerColor = c.amber,
                    periodSelectorSelectedContentColor = c.onAmber,
                ),
            )
        },
    )
}
