package com.mindspring.app.ui.screens.tasks

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.data.model.Task
import com.mindspring.app.domain.TaskGroup
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.BrandTopBar
import com.mindspring.app.ui.components.EmptyState
import com.mindspring.app.ui.components.GroupHeader
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.MsProgressBar
import com.mindspring.app.ui.components.Pill
import com.mindspring.app.ui.components.SegmentedTabs
import com.mindspring.app.ui.components.TaskRow
import com.mindspring.app.ui.components.Tint
import com.mindspring.app.ui.components.appear
import com.mindspring.app.ui.preview.PreviewScreen
import com.mindspring.app.ui.preview.PreviewTasksState
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.theme.areaColor
import com.mindspring.app.ui.util.Fmt

@Composable
fun TasksScreen(
    userName: String,
    onOpenTask: (Long) -> Unit,
    onAddTask: () -> Unit,
    onOpenProject: (Long) -> Unit,
    onAddProject: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val vm = appViewModel { TasksViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    val view by vm.view.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()

    TasksContent(
        userName = userName,
        s = s,
        view = view,
        filter = filter,
        onViewChange = { vm.view.value = it },
        onFilterChange = { vm.filter.value = it },
        onToggleTask = vm::toggle,
        onOpenTask = onOpenTask,
        onAddTask = onAddTask,
        onOpenProject = onOpenProject,
        onAddProject = onAddProject,
        onOpenProfile = onOpenProfile,
    )
}

/**
 * The screen as pure state and callbacks, so it renders in a @Preview without a ViewModel behind
 * it. [TasksScreen] is the thin wrapper that supplies both from the app's data.
 */
@Composable
fun TasksContent(
    userName: String,
    s: TasksState,
    view: TasksView,
    filter: TaskFilter,
    onViewChange: (TasksView) -> Unit,
    onFilterChange: (TaskFilter) -> Unit,
    onToggleTask: (Task) -> Unit,
    onOpenTask: (Long) -> Unit,
    onAddTask: () -> Unit,
    onOpenProject: (Long) -> Unit,
    onAddProject: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val c = MsTheme.colors

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BrandTopBar(userName, onAvatarClick = onOpenProfile)
            Column(Modifier.padding(start = Dimens.screen, end = Dimens.screen, top = 20.dp, bottom = 8.dp)) {
                Text("Tasks", style = MaterialTheme.typography.headlineSmall, color = c.textPrimary)
                Text(
                    if (view == TasksView.Tasks) "One-off work with a deadline, most urgent first." else "Deliverables that several tasks add up to.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
                Spacer(Modifier.height(12.dp))
                SegmentedTabs(TasksView.entries, view, onViewChange, { it.label })
            }
            AnimatedContent(
                targetState = view,
                transitionSpec = {
                    val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (fadeIn(tween(240)) + slideInHorizontally(tween(280)) { it / 8 * dir }) togetherWith fadeOut(tween(160))
                },
                label = "tasksView",
                modifier = Modifier.weight(1f),
            ) { v ->
                when (v) {
                    TasksView.Tasks -> TaskList(s, filter, onFilterChange, onToggleTask, onOpenTask)
                    TasksView.Projects -> ProjectList(s, onOpenProject)
                }
            }
        }
        FloatingActionButton(
            onClick = if (view == TasksView.Tasks) onAddTask else onAddProject,
            containerColor = c.amber,
            contentColor = c.onAmber,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(6.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(Dimens.screen),
        ) {
            Icon(if (view == TasksView.Tasks) Icons.Rounded.Add else Icons.Rounded.CreateNewFolder, contentDescription = if (view == TasksView.Tasks) "Add task" else "New project")
        }
    }
}

@Composable
private fun TaskList(
    s: TasksState,
    filter: TaskFilter,
    onFilterChange: (TaskFilter) -> Unit,
    onToggleTask: (Task) -> Unit,
    onOpenTask: (Long) -> Unit,
) {
    val c = MsTheme.colors
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Dimens.screen, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TaskFilter.entries.forEach { f ->
                FilterChip(f, s.counts[f] ?: 0, selected = f == filter) { onFilterChange(f) }
            }
        }
        if (s.loaded && s.sections.isEmpty()) {
            Column(Modifier.padding(Dimens.screen)) {
                EmptyState(
                    Icons.Rounded.TaskAlt,
                    when (filter) {
                        TaskFilter.Open -> "Nothing open"
                        TaskFilter.Done -> "Nothing finished yet"
                        else -> "All clear"
                    },
                    if (filter == TaskFilter.Open) "Tap + to add a task with a deadline." else "Nothing here right now. Enjoy it.",
                )
            }
            return
        }
        LazyColumn(
            contentPadding = PaddingValues(start = Dimens.screen, end = Dimens.screen, top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
        ) {
            items(s.sections, key = { "${filter.name}-${it.group.name}" }) { section ->
                MsCard(Modifier.fillMaxWidth().animateItem().appear(section.group.ordinal), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)) {
                    GroupHeader(
                        section.group.label,
                        section.lines.size,
                        color = if (section.group == TaskGroup.Overdue) c.danger else c.textSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    section.lines.forEach { line ->
                        TaskRow(
                            task = line.task,
                            flag = line.flag,
                            projectName = line.projectName,
                            onToggle = { onToggleTask(line.task) },
                            onClick = { onOpenTask(line.task.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(filter: TaskFilter, count: Int, selected: Boolean, onClick: () -> Unit) {
    val c = MsTheme.colors
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (selected) c.teal else c.card)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(filter.label, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = if (selected) c.onTeal else c.textPrimary)
        Spacer(Modifier.width(6.dp))
        val alert = filter == TaskFilter.Overdue && count > 0
        Text(
            "$count",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = when {
                selected -> c.onTeal
                alert -> c.danger
                else -> c.textTertiary
            },
            modifier = Modifier
                .clip(CircleShape)
                .background(if (selected) c.onTeal.copy(alpha = 0.18f) else if (alert) c.dangerSoft else c.cardMuted)
                .padding(horizontal = 7.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun ProjectList(s: TasksState, onOpenProject: (Long) -> Unit) {
    val c = MsTheme.colors
    var showArchived by rememberSaveable { mutableStateOf(false) }
    if (s.loaded && s.projects.isEmpty() && s.archived.isEmpty()) {
        Column(Modifier.padding(Dimens.screen)) {
            EmptyState(Icons.Rounded.CreateNewFolder, "No projects yet", "A project groups the tasks that add up to one thing, like an assignment. Tap + to start one.")
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = Dimens.screen, end = Dimens.screen, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
    ) {
        items(s.projects, key = { it.project.id }) { card ->
            ProjectCardView(card, Modifier.animateItem()) { onOpenProject(card.project.id) }
        }
        if (s.archived.isNotEmpty()) {
            item(key = "archived") {
                Text(
                    if (showArchived) "Archived" else "Show ${s.archived.size} archived",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.tealInk,
                    modifier = Modifier.clip(CircleShape).clickable { showArchived = !showArchived }.padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
            if (showArchived) {
                items(s.archived, key = { it.project.id }) { card ->
                    ProjectCardView(card, Modifier.animateItem()) { onOpenProject(card.project.id) }
                }
            }
        }
    }
}

/** The Projects sheet as a card: done, open, overdue, a progress bar and the next deadline. */
@Composable
fun ProjectCardView(card: ProjectCard, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = MsTheme.colors
    val r = card.rollup
    val accent = areaColor(card.project.colorIndex)
    MsCard(modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(0.dp)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(accent))
            Column(Modifier.padding(start = 16.dp, end = 18.dp, top = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(card.project.name, style = MaterialTheme.typography.titleMedium, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val where = listOfNotNull(card.area?.name, card.project.subArea.ifBlank { null }).joinToString(" · ")
                        if (where.isNotEmpty()) Text(where, style = MaterialTheme.typography.labelSmall, color = c.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    when {
                        r.total == 0 -> Pill("No tasks", Tint(c.cardMuted, c.textTertiary))
                        r.open == 0 -> Pill("Complete", Tint(c.cardSubtle, c.tealInk))
                        r.overdue > 0 -> Pill("${r.overdue} overdue", Tint(c.dangerSoft, c.danger))
                        else -> Pill("${r.open} open", Tint(c.cardMuted, c.textSecondary))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MsProgressBar(r.progress, Modifier.weight(1f), color = accent, height = 7.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(Fmt.percent(r.progress), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = c.textPrimary)
                }
                Row {
                    Text("${r.done} of ${r.total - r.dropped} done", style = MaterialTheme.typography.labelSmall, color = c.textSecondary, modifier = Modifier.weight(1f))
                    r.nextDue?.let { Text("Next: ${Fmt.friendlyDay(it)}", style = MaterialTheme.typography.labelSmall, color = c.textSecondary) }
                }
            }
        }
    }
}

// --- Previews -------------------------------------------------------------------------------

@Preview(name = "Tasks", showBackground = true, widthDp = 393, heightDp = 830)
@Composable
private fun TasksScreenPreview() = PreviewScreen {
    TasksContent(
        userName = "Asan", s = PreviewTasksState, view = TasksView.Tasks, filter = TaskFilter.Open,
        onViewChange = {}, onFilterChange = {}, onToggleTask = {}, onOpenTask = {}, onAddTask = {},
        onOpenProject = {}, onAddProject = {}, onOpenProfile = {},
    )
}

@Preview(name = "Tasks · dark", showBackground = true, widthDp = 393, heightDp = 830)
@Composable
private fun TasksScreenDarkPreview() = PreviewScreen(dark = true) {
    TasksContent(
        userName = "Asan", s = PreviewTasksState, view = TasksView.Tasks, filter = TaskFilter.Open,
        onViewChange = {}, onFilterChange = {}, onToggleTask = {}, onOpenTask = {}, onAddTask = {},
        onOpenProject = {}, onAddProject = {}, onOpenProfile = {},
    )
}

@Preview(name = "Projects", showBackground = true, widthDp = 393, heightDp = 830)
@Composable
private fun ProjectsViewPreview() = PreviewScreen {
    TasksContent(
        userName = "Asan", s = PreviewTasksState, view = TasksView.Projects, filter = TaskFilter.Open,
        onViewChange = {}, onFilterChange = {}, onToggleTask = {}, onOpenTask = {}, onAddTask = {},
        onOpenProject = {}, onAddProject = {}, onOpenProfile = {},
    )
}
