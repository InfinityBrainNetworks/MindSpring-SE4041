package com.mindspring.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindspring.app.domain.groupKey
import com.mindspring.app.ui.theme.InputShape
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.util.Fmt
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * A date that can be empty: shows the chosen day (or [emptyText]) and opens a calendar; quick
 * chips cover the common picks so most dates are one tap.
 */
@Composable
fun DateField(
    value: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier,
    quick: List<Pair<String, LocalDate>> = emptyList(),
) {
    val c = MsTheme.colors
    var open by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(InputShape)
                .background(c.card)
                .clickable { open = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Event, contentDescription = null, tint = c.tealInk, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                value?.let { d ->
                    val friendly = Fmt.friendlyDay(d)
                    if (friendly == Fmt.shortDay(d)) friendly else "$friendly · ${Fmt.shortDay(d)}"
                } ?: emptyText,
                style = MaterialTheme.typography.bodyLarge,
                color = if (value == null) c.textTertiary else c.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (value != null) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Clear date",
                    tint = c.textTertiary,
                    modifier = Modifier.size(20.dp).clip(CircleShape).clickable { onChange(null) },
                )
            }
        }
        if (quick.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quick.forEach { (label, date) ->
                    SmallChip(label, selected = value == date, onClick = { onChange(date) })
                }
            }
        }
    }
    if (open) {
        MsDatePicker(initial = value ?: LocalDate.now(), onDismiss = { open = false }) { onChange(it); open = false }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MsDatePicker(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    val c = MsTheme.colors
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } ?: onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(
            state = state,
            colors = DatePickerDefaults.colors(
                selectedDayContainerColor = c.teal,
                selectedDayContentColor = c.onTeal,
                todayDateBorderColor = c.amber,
                todayContentColor = c.tealInk,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(initial: LocalTime, onDismiss: () -> Unit, title: String = "Reminder time", onConfirm: (LocalTime) -> Unit) {
    val c = MsTheme.colors
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(title) },
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

/** Compact selectable chip for quick picks and filters. */
@Composable
fun SmallChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = MsTheme.colors
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        color = if (selected) c.onTeal else c.tealInk,
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) c.teal else c.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

/** "‹  September 2026  ›" with the title sliding in the direction of travel. */
@Composable
fun MonthSwitcher(month: YearMonth, onChange: (YearMonth) -> Unit, modifier: Modifier = Modifier, max: YearMonth = YearMonth.now()) {
    val c = MsTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(month.minusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month", tint = c.tealInk)
        }
        AnimatedContent(
            targetState = month,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(260)) { it / 3 * dir } + fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(tween(200)) { -it / 3 * dir } + fadeOut(tween(200)))
            },
            modifier = Modifier.weight(1f),
            label = "month",
        ) { m ->
            Text(Fmt.month(m), style = MaterialTheme.typography.titleMedium, color = c.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        IconButton(onClick = { onChange(month.plusMonths(1)) }, enabled = month < max) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Next month",
                tint = if (month < max) c.tealInk else c.textTertiary.copy(alpha = 0.4f),
            )
        }
    }
}

/**
 * Free text with the labels already in use offered underneath, so a sub-area or project is typed
 * once and picked after that - the workbook's self-growing dropdown.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuggestionField(
    value: String,
    onChange: (String) -> Unit,
    suggestions: List<String>,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val shown = suggestions
        .distinctBy { groupKey(it) }
        .filter { groupKey(it) != groupKey(value) && (value.isBlank() || groupKey(it).contains(groupKey(value))) }
        .take(8)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MsTextField(value = value, onValueChange = { onChange(it.take(40)) }, placeholder = placeholder)
        if (shown.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                shown.forEach { SmallChip(it, selected = false, onClick = { onChange(it) }) }
            }
        }
    }
}

/** Life-area picker: a wrap of chips, each with its colour, plus "None". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AreaChooser(
    areas: List<com.mindspring.app.data.model.LifeArea>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        areas.forEach { area ->
            DotChip(area.name, com.mindspring.app.ui.theme.areaColor(area.colorIndex), selected = area.id == selectedId, onClick = { onSelect(area.id) })
        }
        DotChip("None", MsTheme.colors.textTertiary, selected = selectedId == null, onClick = { onSelect(null) })
    }
}

@Composable
fun DotChip(text: String, dot: androidx.compose.ui.graphics.Color, selected: Boolean, onClick: () -> Unit) {
    val c = MsTheme.colors
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (selected) dot.copy(alpha = if (c.isDark) 0.32f else 0.2f) else c.card)
            .then(if (selected) Modifier.border(1.5.dp, dot, CircleShape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = c.textPrimary,
        )
    }
}
