package com.mindspring.app.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindspring.app.AppContainer
import com.mindspring.app.data.model.AlertStyle
import com.mindspring.app.data.model.LifeArea
import com.mindspring.app.data.model.Priority
import com.mindspring.app.data.model.Project
import com.mindspring.app.data.model.Repeat
import com.mindspring.app.data.model.Task
import com.mindspring.app.data.model.TaskStatus
import com.mindspring.app.domain.TaskGroup
import com.mindspring.app.domain.TaskLogic
import com.mindspring.app.domain.TaskRollup
import com.mindspring.app.ui.screens.home.TaskLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

// ---------- Tasks tab ----------

enum class TasksView(val label: String) { Tasks("Tasks"), Projects("Projects") }

enum class TaskFilter(val label: String) { Open("Open"), Overdue("Overdue"), Today("Today"), Week("Next 7 days"), Done("Finished") }

data class TaskSection(val group: TaskGroup, val lines: List<TaskLine>)

data class ProjectCard(val project: Project, val area: LifeArea?, val rollup: TaskRollup)

data class TasksState(
    val loaded: Boolean = false,
    val sections: List<TaskSection> = emptyList(),
    val counts: Map<TaskFilter, Int> = emptyMap(),
    val projects: List<ProjectCard> = emptyList(),
    val archived: List<ProjectCard> = emptyList(),
)

class TasksViewModel(private val app: AppContainer) : ViewModel() {
    val view = MutableStateFlow(TasksView.Tasks)
    val filter = MutableStateFlow(TaskFilter.Open)

    val state: StateFlow<TasksState> = combine(app.tasks.tasks, app.tasks.projects, app.areas.areas, filter) { tasks, projects, areas, f ->
        val today = LocalDate.now()
        val projectNames = projects.associate { it.id to it.name }
        val open = tasks.filter { it.status.isOpen }
        fun line(t: Task) = TaskLine(t, TaskLogic.flag(t, today), t.projectId?.let(projectNames::get))

        val picked = when (f) {
            TaskFilter.Open -> open
            TaskFilter.Overdue -> open.filter { TaskLogic.isOverdue(it, today) }
            TaskFilter.Today -> open.filter { it.due == today }
            TaskFilter.Week -> open.filter { it.due != null && !it.due.isBefore(today) && !it.due.isAfter(today.plusDays(7)) }
            TaskFilter.Done -> tasks.filter { !it.status.isOpen }
        }
        val sections = if (f == TaskFilter.Done) {
            listOf(TaskSection(TaskGroup.Finished, picked.sortedWith(TaskLogic.recentlyFinished).map(::line))).filter { it.lines.isNotEmpty() }
        } else {
            picked.sortedWith(TaskLogic.urgency).groupBy { TaskLogic.group(it, today) }
                .toSortedMap(compareBy { it.ordinal })
                .map { (g, list) -> TaskSection(g, list.map(::line)) }
        }

        val areaById = areas.associateBy { it.id }
        val byProject = tasks.groupBy { it.projectId }
        val cards = projects.map { p -> ProjectCard(p, p.areaId?.let(areaById::get), TaskLogic.rollup(byProject[p.id].orEmpty(), today)) }
            .sortedWith(compareBy<ProjectCard> { it.rollup.open == 0 && it.rollup.total > 0 }.thenBy { it.rollup.nextDue ?: LocalDate.MAX }.thenBy { it.project.name.lowercase() })

        TasksState(
            loaded = true,
            sections = sections,
            counts = mapOf(
                TaskFilter.Open to open.size,
                TaskFilter.Overdue to open.count { TaskLogic.isOverdue(it, today) },
                TaskFilter.Today to open.count { it.due == today },
                TaskFilter.Week to open.count { it.due != null && !it.due.isBefore(today) && !it.due.isAfter(today.plusDays(7)) },
                TaskFilter.Done to tasks.count { !it.status.isOpen },
            ),
            projects = cards.filter { !it.project.archived },
            archived = cards.filter { it.project.archived },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksState())

    fun toggle(task: Task) {
        viewModelScope.launch { app.tasks.setStatus(task.id, if (task.status.isOpen) TaskStatus.Done else TaskStatus.NotStarted) }
    }
}

// ---------- Task editor ----------

data class TaskEditorState(
    val id: Long = 0,
    val title: String = "",
    val notes: String = "",
    val areaId: Long? = null,
    val subArea: String = "",
    val projectId: Long? = null,
    val start: LocalDate? = null,
    val due: LocalDate? = null,
    val priority: Priority = Priority.B,
    val status: TaskStatus = TaskStatus.NotStarted,
    val doneOn: LocalDate? = null,
    val repeat: Repeat = Repeat.None,
    val createdAt: LocalDate = LocalDate.now(),
    val alertAt: LocalDateTime? = null,
    val alertStyle: AlertStyle = AlertStyle.Reminder,
    val saved: Boolean = false,
    val showErrors: Boolean = false,
) {
    val isEditing: Boolean get() = id != 0L
    val dateError: String? get() = if (start != null && due != null && due.isBefore(start)) "The deadline is before the start date." else null
    val titleError: String? get() = if (title.isBlank()) "Give the task a name." else null
    val canSave: Boolean get() = titleError == null && dateError == null

    /** An alert set for a time already gone will not ring; say so rather than refuse to save. */
    fun alertPassed(now: LocalDateTime = LocalDateTime.now()): Boolean = alertAt != null && status.isOpen && !alertAt.isAfter(now)

    fun toTask() = Task(
        id, title.trim(), notes.trim(), areaId, subArea.trim(), projectId, start, due, priority, status, doneOn, repeat, createdAt,
        alertAt, alertStyle,
    )
}

class TaskEditorViewModel(private val app: AppContainer, taskId: Long?, projectId: Long?) : ViewModel() {
    private val _state = MutableStateFlow(TaskEditorState(projectId = projectId))
    val state: StateFlow<TaskEditorState> = _state

    val areas: StateFlow<List<LifeArea>> = app.areas.areas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val projects: StateFlow<List<Project>> = app.tasks.projects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val subAreas: StateFlow<List<String>> = combine(app.habits.habits, app.tasks.tasks) { h, t ->
        (t.map { it.subArea } + h.map { it.subArea }).filter { it.isNotBlank() }
            .groupingBy { it.trim() }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            if (taskId != null) {
                app.tasks.task(taskId).first()?.let { t ->
                    _state.value = TaskEditorState(
                        t.id, t.title, t.notes, t.areaId, t.subArea, t.projectId, t.start, t.due,
                        t.priority, t.status, t.doneOn, t.repeat, t.createdAt, t.alertAt, t.alertStyle,
                    )
                }
            } else if (projectId != null) {
                app.tasks.project(projectId).first()?.let { p -> _state.update { it.copy(areaId = p.areaId, subArea = p.subArea) } }
            }
        }
    }

    fun onTitle(v: String) = _state.update { it.copy(title = v.take(120)) }
    fun onNotes(v: String) = _state.update { it.copy(notes = v.take(1000)) }
    fun onArea(v: Long?) = _state.update { it.copy(areaId = v) }
    fun onSubArea(v: String) = _state.update { it.copy(subArea = v) }
    fun onStart(v: LocalDate?) = _state.update { it.copy(start = v) }
    fun onDue(v: LocalDate?) = _state.update { it.copy(due = v) }
    fun onPriority(v: Priority) = _state.update { it.copy(priority = v) }
    fun onRepeat(v: Repeat) = _state.update { it.copy(repeat = v) }
    fun onDoneOn(v: LocalDate?) = _state.update { it.copy(doneOn = v) }

    fun onAlertEnabled(on: Boolean) = _state.update { s ->
        s.copy(alertAt = if (on) s.alertAt ?: defaultAlert(s.due) else null)
    }

    fun onAlertDate(d: LocalDate) = _state.update { s -> s.copy(alertAt = d.atTime(s.alertAt?.toLocalTime() ?: DEFAULT_ALERT_TIME)) }
    fun onAlertTime(t: LocalTime) = _state.update { s -> s.copy(alertAt = (s.alertAt?.toLocalDate() ?: LocalDate.now()).atTime(t)) }
    fun onAlertAt(at: LocalDateTime) = _state.update { it.copy(alertAt = at) }
    fun onAlertStyle(v: AlertStyle) = _state.update { it.copy(alertStyle = v) }

    fun onStatus(v: TaskStatus) = _state.update {
        it.copy(status = v, doneOn = if (v == TaskStatus.Done) it.doneOn ?: LocalDate.now() else null)
    }

    /** Picking a project files the task where the project lives, unless it was filed already. */
    fun onProject(project: Project?) = _state.update { s ->
        if (project == null) s.copy(projectId = null)
        else s.copy(
            projectId = project.id,
            areaId = s.areaId ?: project.areaId,
            subArea = s.subArea.ifBlank { project.subArea },
        )
    }

    fun save() {
        val s = _state.value
        if (!s.canSave) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            app.tasks.upsert(s.toTask())
            _state.update { it.copy(saved = true) }
        }
    }

    fun delete(then: () -> Unit) {
        val id = _state.value.id
        if (id == 0L) return then()
        viewModelScope.launch {
            app.tasks.delete(id)
            then()
        }
    }

    companion object {
        val DEFAULT_ALERT_TIME: LocalTime = LocalTime.of(9, 0)

        /** 9 AM on the deadline (or today); if that has passed, the next whole hour. */
        fun defaultAlert(due: LocalDate?, now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
            val onDay = (due ?: now.toLocalDate()).atTime(DEFAULT_ALERT_TIME)
            return if (onDay.isAfter(now)) onDay else now.truncatedTo(ChronoUnit.HOURS).plusHours(1)
        }
    }
}

// ---------- Projects ----------

data class ProjectEditorState(
    val id: Long = 0,
    val name: String = "",
    val areaId: Long? = null,
    val subArea: String = "",
    val colorIndex: Int = 0,
    val notes: String = "",
    val archived: Boolean = false,
    val createdAt: LocalDate = LocalDate.now(),
    val saved: Boolean = false,
) {
    val isEditing: Boolean get() = id != 0L
    val canSave: Boolean get() = name.isNotBlank()
}

class ProjectEditorViewModel(private val app: AppContainer, projectId: Long?) : ViewModel() {
    private val _state = MutableStateFlow(ProjectEditorState())
    val state: StateFlow<ProjectEditorState> = _state
    val areas: StateFlow<List<LifeArea>> = app.areas.areas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val subAreas: StateFlow<List<String>> = combine(app.habits.habits, app.tasks.tasks) { h, t ->
        (t.map { it.subArea } + h.map { it.subArea }).filter { it.isNotBlank() }.distinctBy { it.trim().lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            if (projectId != null) {
                app.tasks.project(projectId).first()?.let { p ->
                    _state.value = ProjectEditorState(p.id, p.name, p.areaId, p.subArea, p.colorIndex, p.notes, p.archived, p.createdAt)
                }
            } else {
                // A new project takes the next colour in the palette so neighbours differ.
                val count = app.tasks.projects.first().size
                _state.update { it.copy(colorIndex = count) }
            }
        }
    }

    fun onName(v: String) = _state.update { it.copy(name = v.take(60)) }
    fun onArea(v: Long?) = _state.update { it.copy(areaId = v) }
    fun onSubArea(v: String) = _state.update { it.copy(subArea = v) }
    fun onColor(v: Int) = _state.update { it.copy(colorIndex = v) }
    fun onNotes(v: String) = _state.update { it.copy(notes = v.take(500)) }
    fun onArchived(v: Boolean) = _state.update { it.copy(archived = v) }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            app.tasks.upsertProject(Project(s.id, s.name.trim(), s.areaId, s.subArea.trim(), s.colorIndex, s.notes.trim(), s.archived, s.createdAt))
            _state.update { it.copy(saved = true) }
        }
    }
}

data class ProjectDetailState(
    val project: Project,
    val area: LifeArea?,
    val rollup: TaskRollup,
    val open: List<TaskLine>,
    val finished: List<TaskLine>,
)

class ProjectDetailViewModel(private val app: AppContainer, private val projectId: Long) : ViewModel() {
    val state: StateFlow<ProjectDetailState?> = combine(app.tasks.project(projectId), app.tasks.tasks, app.areas.areas) { project, tasks, areas ->
        project ?: return@combine null
        val today = LocalDate.now()
        val mine = tasks.filter { it.projectId == projectId }
        fun line(t: Task) = TaskLine(t, TaskLogic.flag(t, today), null)
        ProjectDetailState(
            project = project,
            area = areas.firstOrNull { it.id == project.areaId },
            rollup = TaskLogic.rollup(mine, today),
            open = mine.filter { it.status.isOpen }.sortedWith(TaskLogic.urgency).map(::line),
            finished = mine.filter { !it.status.isOpen }.sortedWith(TaskLogic.recentlyFinished).map(::line),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggle(task: Task) {
        viewModelScope.launch { app.tasks.setStatus(task.id, if (task.status.isOpen) TaskStatus.Done else TaskStatus.NotStarted) }
    }

    fun setArchived(archived: Boolean) {
        val p = state.value?.project ?: return
        viewModelScope.launch { app.tasks.upsertProject(p.copy(archived = archived)) }
    }

    fun delete(then: () -> Unit) {
        viewModelScope.launch {
            app.tasks.deleteProject(projectId)
            then()
        }
    }
}
