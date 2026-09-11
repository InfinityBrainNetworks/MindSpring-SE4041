package com.mindspring.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindspring.app.AppContainer
import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.model.JournalEntry
import com.mindspring.app.data.model.MarkState
import com.mindspring.app.data.model.MoodEntry
import com.mindspring.app.data.model.Task
import com.mindspring.app.data.model.TaskStatus
import com.mindspring.app.domain.Analytics
import com.mindspring.app.domain.HabitStats
import com.mindspring.app.domain.HabitUnit
import com.mindspring.app.domain.TaskFlag
import com.mindspring.app.domain.TaskLogic
import com.mindspring.app.domain.byHabit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** A habit on today's list. [doneOn] is set when a weekly or monthly habit was already done on another day. */
data class TodayHabit(
    val habit: Habit,
    val mark: MarkState?,
    val doneOn: LocalDate?,
    val streak: Int,
    val unit: HabitUnit,
    val areaColor: Int?,
) {
    val handled: Boolean get() = mark != null || doneOn != null
}

/** The habits that belong on [today]'s list, most frequent first (Daily, Weekdays... Monthly). */
fun todayHabits(
    habits: List<Habit>,
    byHabit: Map<Long, Map<LocalDate, MarkState>>,
    areaColors: Map<Long, Int>,
    today: LocalDate,
): List<TodayHabit> = habits.filter { HabitStats.belongsOn(it, today) }.map { h ->
    val hm = byHabit[h.id].orEmpty()
    val mark = hm[today]
    val doneEarlier = if (!h.frequency.isDayBased && mark == null) {
        hm.filter { (d, s) -> s == MarkState.Done && d in HabitStats.unitSpan(h, today) }.keys.maxOrNull()
    } else null
    TodayHabit(h, mark, doneEarlier, HabitStats.currentStreak(h, hm, today), HabitStats.unit(h), h.areaId?.let(areaColors::get))
}.sortedWith(compareBy<TodayHabit> { it.habit.frequency.ordinal }.thenBy { it.habit.id })

data class TaskLine(val task: Task, val flag: TaskFlag, val projectName: String?)

data class StreakItem(val name: String, val count: Int, val unit: HabitUnit)

data class HomeUiState(
    val loaded: Boolean = false,
    val firstName: String = "",
    val todayMood: MoodEntry? = null,
    val habits: List<TodayHabit> = emptyList(),
    val overdue: Int = 0,
    val dueToday: Int = 0,
    val dueWeek: Int = 0,
    val open: Int = 0,
    val upNext: List<TaskLine> = emptyList(),
    val journal: JournalEntry? = null,
    val streaks: List<StreakItem> = emptyList(),
) {
    /** Done out of what still counts today (skips excluded). */
    val doneCount: Int get() = habits.count { it.mark == MarkState.Done || (it.mark == null && it.doneOn != null) }
    val countable: Int get() = habits.count { it.mark != MarkState.Skipped }
    val progress: Float get() = if (countable == 0) 0f else doneCount.toFloat() / countable
}

class HomeViewModel(private val app: AppContainer) : ViewModel() {

    private val tasksAndProjects = combine(app.tasks.tasks, app.tasks.projects) { t, p -> t to p }
    private val habitData = combine(app.habits.habits, app.habits.marks, app.areas.areas) { h, m, a -> Triple(h, m, a) }

    val state: StateFlow<HomeUiState> = combine(
        app.auth.currentUser, habitData, tasksAndProjects, app.moods.entries, app.journal.entries,
    ) { user, (habits, marks, areas), (tasks, projects), moods, journal ->
        val today = LocalDate.now()
        val byHabit = marks.byHabit()
        val areaColors = areas.associate { it.id to it.colorIndex }
        val projectNames = projects.associate { it.id to it.name }

        val todayHabits = todayHabits(habits, byHabit, areaColors, today)

        val open = tasks.filter { it.status.isOpen }
        val taskStats = Analytics.taskMonth(tasks, YearMonth.from(today), today)

        HomeUiState(
            loaded = true,
            firstName = user?.name?.substringBefore(' ').orEmpty(),
            todayMood = moods.firstOrNull { it.loggedAt.toLocalDate() == today },
            habits = todayHabits,
            overdue = taskStats.overdue,
            dueToday = taskStats.dueToday,
            dueWeek = taskStats.dueNext7,
            open = taskStats.open,
            upNext = open.filter { it.start == null || !it.start.isAfter(today) || (it.due != null && !it.due.isAfter(today.plusDays(7))) }
                .sortedWith(TaskLogic.urgency)
                .take(4)
                .map { TaskLine(it, TaskLogic.flag(it, today), it.projectId?.let(projectNames::get)) },
            journal = journal.firstOrNull { it.date == today },
            streaks = habits.filter { it.active }
                .map { StreakItem(it.name, HabitStats.currentStreak(it, byHabit[it.id].orEmpty(), today), HabitStats.unit(it)) }
                .filter { it.count > 0 }
                .sortedByDescending { it.count }
                .take(3),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun toggleDone(item: TodayHabit) {
        viewModelScope.launch {
            app.habits.setMark(item.habit.id, LocalDate.now(), if (item.mark == MarkState.Done) null else MarkState.Done)
        }
    }

    fun toggleSkip(item: TodayHabit) {
        viewModelScope.launch {
            app.habits.setMark(item.habit.id, LocalDate.now(), if (item.mark == MarkState.Skipped) null else MarkState.Skipped)
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch { app.tasks.setStatus(task.id, TaskStatus.Done) }
    }

    fun reopenTask(task: Task) {
        viewModelScope.launch { app.tasks.setStatus(task.id, TaskStatus.InProgress) }
    }

    fun saveGratitude(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { app.gratitude.add(text) }
    }
}
