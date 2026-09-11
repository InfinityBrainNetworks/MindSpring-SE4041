package com.mindspring.app.ui.screens.tasks

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.AnimatedCount
import com.mindspring.app.ui.components.AreaChooser
import com.mindspring.app.ui.components.BarIconButton
import com.mindspring.app.ui.components.FieldLabel
import com.mindspring.app.ui.components.GhostButton
import com.mindspring.app.ui.components.GroupHeader
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.MsTextField
import com.mindspring.app.ui.components.PrimaryButton
import com.mindspring.app.ui.components.ProgressRing
import com.mindspring.app.ui.components.SecondaryButton
import com.mindspring.app.ui.components.SuggestionField
import com.mindspring.app.ui.components.TaskRow
import com.mindspring.app.ui.components.TealTopBar
import com.mindspring.app.ui.components.appear
import com.mindspring.app.ui.components.msSwitchColors
import com.mindspring.app.ui.theme.AreaPalette
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.theme.areaColor
import com.mindspring.app.ui.util.Fmt

@Composable
fun ProjectDetailScreen(
    projectId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onAddTask: () -> Unit,
) {
    val vm = appViewModel { ProjectDetailViewModel(it, projectId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val c = MsTheme.colors
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TealTopBar(
            title = state?.project?.name ?: "",
            onNavigate = onBack,
            actions = { BarIconButton(Icons.Rounded.Edit, "Edit project", onEdit) },
        )
        val s = state ?: return@Column
        val accent = areaColor(s.project.colorIndex)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = Dimens.screen, vertical = Dimens.stackLg - 8.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
        ) {
            MsCard(Modifier.fillMaxWidth().appear(0)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(s.rollup.progress, size = 84.dp, stroke = 9.dp, color = accent) {
                        Text(Fmt.percent(s.rollup.progress), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = c.textPrimary)
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.project.name, style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                        val where = listOfNotNull(s.area?.name, s.project.subArea.ifBlank { null }).joinToString(" · ")
                        if (where.isNotEmpty()) Text(where, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            when {
                                s.rollup.total == 0 -> "No tasks yet"
                                s.rollup.open == 0 -> "Complete. Every task is done."
                                else -> s.rollup.nextDue?.let { "Next deadline ${Fmt.friendlyDay(it)}" } ?: "No deadlines set"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (s.rollup.overdue > 0) c.danger else c.tealInk,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat("Tasks", s.rollup.total - s.rollup.dropped, c.textPrimary)
                    Stat("Done", s.rollup.done, c.tealInk)
                    Stat("Open", s.rollup.open, c.textPrimary)
                    Stat("Overdue", s.rollup.overdue, if (s.rollup.overdue > 0) c.danger else c.textTertiary)
                }
                if (s.project.notes.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(s.project.notes, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                }
            }

            MsCard(Modifier.fillMaxWidth().appear(1), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)) {
                GroupHeader("Open", s.open.size, modifier = Modifier.padding(horizontal = 8.dp))
                if (s.open.isEmpty()) {
                    Text("Nothing open.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, modifier = Modifier.padding(8.dp))
                }
                s.open.forEach { line -> TaskRow(line.task, line.flag, onToggle = { vm.toggle(line.task) }, onClick = { onOpenTask(line.task.id) }) }
                SecondaryButton("Add a task", onClick = onAddTask, leadingIcon = Icons.Rounded.Add, modifier = Modifier.padding(8.dp))
            }

            if (s.finished.isNotEmpty()) {
                MsCard(Modifier.fillMaxWidth().appear(2), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)) {
                    GroupHeader("Finished", s.finished.size, modifier = Modifier.padding(horizontal = 8.dp))
                    s.finished.forEach { line -> TaskRow(line.task, line.flag, onToggle = { vm.toggle(line.task) }, onClick = { onOpenTask(line.task.id) }) }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                GhostButton(if (s.project.archived) "Unarchive" else "Archive project", onClick = { vm.setArchived(!s.project.archived) })
                GhostButton("Delete project", onClick = { confirmDelete = true }, color = c.danger)
            }
            Spacer(Modifier.height(Dimens.stackMd))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this project?") },
            text = { Text("Its tasks are kept as standalone tasks. Archive it instead to hide it but keep the grouping.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(onBack) }) { Text("Delete", color = c.danger) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Stat(label: String, value: Int, color: Color) {
    val c = MsTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedCount(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
    }
}

@Composable
fun ProjectEditorScreen(projectId: Long?, onDone: () -> Unit) {
    val vm = appViewModel { ProjectEditorViewModel(it, projectId) }
    val s by vm.state.collectAsStateWithLifecycle()
    val areas by vm.areas.collectAsStateWithLifecycle()
    val subAreas by vm.subAreas.collectAsStateWithLifecycle()
    val c = MsTheme.colors

    LaunchedEffect(s.saved) { if (s.saved) onDone() }

    Column(Modifier.fillMaxSize()) {
        TealTopBar(title = if (s.isEditing) "Edit Project" else "New Project", navigationIcon = Icons.Rounded.Close, onNavigate = onDone)
        Column(
            Modifier.weight(1f).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Dimens.screen, vertical = Dimens.stackLg - 8.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.stackLg - 8.dp),
        ) {
            Column {
                FieldLabel("Project name")
                MsTextField(
                    value = s.name,
                    onValueChange = vm::onName,
                    placeholder = "e.g. Research Proposal",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            }
            Column {
                FieldLabel("Colour")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AreaPalette.indices.take(8).forEach { i ->
                        val selected = i == s.colorIndex % AreaPalette.size
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(areaColor(i))
                                .then(if (selected) Modifier.border(3.dp, c.textPrimary.copy(alpha = 0.7f), CircleShape) else Modifier)
                                .clickable { vm.onColor(i) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Column {
                FieldLabel("Life area")
                AreaChooser(areas, s.areaId, vm::onArea)
            }
            Column {
                FieldLabel("Sub-area (optional)")
                SuggestionField(s.subArea, vm::onSubArea, subAreas, placeholder = "e.g. Assignments")
            }
            Column {
                FieldLabel("Notes (optional)")
                MsTextField(value = s.notes, onValueChange = vm::onNotes, placeholder = "What does done look like?", singleLine = false, minLines = 3)
            }
            if (s.isEditing) {
                MsCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Archived", style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
                            Text("Hidden from the project list; tasks are kept.", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                        }
                        Switch(checked = s.archived, onCheckedChange = vm::onArchived, colors = msSwitchColors())
                    }
                }
            }
        }
        PrimaryButton(
            text = if (s.isEditing) "Save Changes" else "Create Project",
            onClick = vm::save,
            enabled = s.canSave,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Dimens.screen, vertical = Dimens.stackMd),
        )
    }
}
