package com.mindspring.app.ui.screens.areas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mindspring.app.AppContainer
import com.mindspring.app.data.model.LifeArea
import com.mindspring.app.domain.Analytics
import com.mindspring.app.domain.AreaRollup
import com.mindspring.app.domain.SubAreaRollup
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.BarIconButton
import com.mindspring.app.ui.components.MonthSwitcher
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.MsProgressBar
import com.mindspring.app.ui.components.MsTextField
import com.mindspring.app.ui.components.Pill
import com.mindspring.app.ui.components.ProgressRing
import com.mindspring.app.ui.components.TealTopBar
import com.mindspring.app.ui.components.Tint
import com.mindspring.app.ui.components.appear
import com.mindspring.app.ui.theme.AreaPalette
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.theme.areaColor
import com.mindspring.app.ui.util.Fmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class LifeAreasViewModel(private val app: AppContainer) : ViewModel() {
    val month = MutableStateFlow(YearMonth.now())

    val rollups: StateFlow<List<AreaRollup>?> = combine(
        app.areas.areas, app.habits.habits, app.habits.marks, app.tasks.tasks, month,
    ) { areas, habits, marks, tasks, m ->
        Analytics.areas(areas, habits, marks, tasks, m, LocalDate.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(area: LifeArea) = viewModelScope.launch { app.areas.upsert(area) }
    fun delete(id: Long) = viewModelScope.launch { app.areas.delete(id) }
}

@Composable
fun LifeAreasScreen(onBack: () -> Unit) {
    val vm = appViewModel { LifeAreasViewModel(it) }
    val rollups by vm.rollups.collectAsStateWithLifecycle()
    val month by vm.month.collectAsStateWithLifecycle()
    val c = MsTheme.colors
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TealTopBar("Life Areas", onNavigate = onBack, actions = { BarIconButton(Icons.Rounded.Add, "Add a life area") { adding = true } })
        LazyColumn(
            contentPadding = PaddingValues(Dimens.screen),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "intro") {
                Column {
                    Text(
                        "Where your habits and tasks live. Habit rates are for the month below; task counts are as of today.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    MonthSwitcher(month, { vm.month.value = it })
                }
            }
            items(rollups.orEmpty(), key = { it.area?.id ?: -1L }) { r ->
                AreaCard(r, Modifier.animateItem().appear((r.area?.sortOrder ?: 9)), onEdit = { editing = r.area?.id })
            }
        }
    }

    val areaToEdit = rollups?.firstOrNull { it.area?.id == editing }?.area
    if (areaToEdit != null) {
        AreaDialog(
            initial = areaToEdit,
            onDismiss = { editing = null },
            onSave = { vm.save(it); editing = null },
            onDelete = { vm.delete(areaToEdit.id); editing = null },
        )
    }
    if (adding) {
        AreaDialog(
            initial = LifeArea(name = "", colorIndex = rollups?.size ?: 0),
            onDismiss = { adding = false },
            onSave = { vm.save(it); adding = false },
            onDelete = null,
        )
    }
}

@Composable
private fun AreaCard(r: AreaRollup, modifier: Modifier, onEdit: () -> Unit) {
    val c = MsTheme.colors
    var open by rememberSaveable(r.area?.id) { mutableStateOf(false) }
    val accent = r.area?.let { areaColor(it.colorIndex) } ?: c.textTertiary
    val turn by animateFloatAsState(if (open) 180f else 0f, label = "chevron")
    MsCard(modifier.fillMaxWidth().animateContentSize(), onClick = { open = !open }, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(r.habitTally.rate, size = 48.dp, stroke = 5.dp, color = accent) {
                Text(if (r.habitTally.possible == 0) "–" else Fmt.percent(r.habitTally.rate), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = c.textPrimary)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(r.area?.name ?: "Not filed under an area", style = MaterialTheme.typography.titleMedium, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    buildList {
                        add("${r.activeHabits} habit${if (r.activeHabits == 1) "" else "s"}")
                        if (r.tasks.total > 0) add("${r.tasks.done}/${r.tasks.total - r.tasks.dropped} tasks done")
                        if (r.tasks.overdue > 0) add("${r.tasks.overdue} overdue")
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (r.tasks.overdue > 0) c.danger else c.textSecondary,
                )
            }
            if (r.area != null) {
                IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Edit ${r.area.name}", tint = c.textTertiary, modifier = Modifier.size(18.dp)) }
            }
            Icon(Icons.Rounded.ExpandMore, contentDescription = if (open) "Collapse" else "Expand", tint = c.textTertiary, modifier = Modifier.rotate(turn))
        }
        if (r.tasks.total > 0) {
            Spacer(Modifier.height(10.dp))
            MsProgressBar(r.tasks.progress, color = accent.copy(alpha = 0.8f), height = 5.dp)
        }
        AnimatedVisibility(open) {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (r.subAreas.isEmpty()) {
                    Text("No sub-areas yet. Add one on a habit or task.", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
                }
                r.subAreas.forEach { SubAreaRow(it, accent) }
            }
        }
    }
}

@Composable
private fun SubAreaRow(s: SubAreaRollup, accent: Color) {
    val c = MsTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.cardSubtle.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(s.name, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                buildList {
                    if (s.activeHabits > 0) add("${s.activeHabits} habit${if (s.activeHabits == 1) "" else "s"} · ${if (s.habitTally.possible == 0) "–" else Fmt.percent(s.habitTally.rate)}")
                    if (s.tasks.total > 0) add("${s.tasks.done}/${s.tasks.total - s.tasks.dropped} tasks")
                    s.tasks.nextDue?.let { add("next ${Fmt.friendlyDay(it)}") }
                }.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary,
            )
        }
        if (s.state.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Pill(
                s.state,
                when {
                    s.tasks.overdue > 0 -> Tint(c.dangerSoft, c.danger)
                    s.state == "Tasks complete" -> Tint(c.cardSubtle, c.tealInk)
                    else -> Tint(c.cardMuted, c.textSecondary)
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AreaDialog(initial: LifeArea, onDismiss: () -> Unit, onSave: (LifeArea) -> Unit, onDelete: (() -> Unit)?) {
    val c = MsTheme.colors
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var color by rememberSaveable { mutableIntStateOf(initial.colorIndex % AreaPalette.size) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "New life area" else "Edit life area") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MsTextField(name, { name = it.take(40) }, label = "Name")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AreaPalette.indices.forEach { i ->
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(areaColor(i))
                                .then(if (i == color) Modifier.border(3.dp, c.textPrimary.copy(alpha = 0.7f), CircleShape) else Modifier)
                                .clickable { color = i },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (i == color) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                if (onDelete != null) {
                    Text(
                        if (confirmDelete) "Tap again to remove. Its habits and tasks are kept, just unfiled." else "Remove this area",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.danger,
                        modifier = Modifier.clip(CircleShape).clickable { if (confirmDelete) onDelete() else confirmDelete = true }.padding(vertical = 6.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(initial.copy(name = name.trim(), colorIndex = color)) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
