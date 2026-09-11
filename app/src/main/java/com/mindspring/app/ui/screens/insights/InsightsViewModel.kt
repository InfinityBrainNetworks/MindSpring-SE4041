package com.mindspring.app.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindspring.app.AppContainer
import com.mindspring.app.data.model.Mood
import com.mindspring.app.domain.Analytics
import com.mindspring.app.domain.HabitMonth
import com.mindspring.app.domain.InsightEngine
import com.mindspring.app.domain.JournalMonth
import com.mindspring.app.domain.KeyInsight
import com.mindspring.app.domain.MonthRow
import com.mindspring.app.domain.ProjectRollup
import com.mindspring.app.domain.TaskMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

enum class InsightSection(val label: String) { Overview("Overview"), Habits("Habits"), Tasks("Tasks"), Mind("Mind") }

data class InsightsState(
    val month: YearMonth,
    val keyInsight: KeyInsight?,
    val habits: HabitMonth,
    val tasks: TaskMonth,
    val journal: JournalMonth,
    val averageMood: Mood?,
    val averageMoodValue: Float?,
    val trend: List<Float>,
    val trendLabels: List<String>,
    val rows: List<MonthRow>,
    val projects: List<ProjectRollup>,
    val today: LocalDate,
)

class InsightsViewModel(app: AppContainer) : ViewModel() {
    val month = MutableStateFlow(YearMonth.now())
    val section = MutableStateFlow(InsightSection.Overview)

    private val habitData = combine(app.habits.habits, app.habits.marks, app.areas.areas) { h, m, a -> Triple(h, m, a) }
    private val taskData = combine(app.tasks.tasks, app.tasks.projects) { t, p -> t to p }
    private val mindData = combine(app.moods.entries, app.journal.entries) { m, j -> m to j }

    val state: StateFlow<InsightsState?> = combine(habitData, taskData, mindData, month) { (habits, marks, areas), (tasks, projects), (moods, journal), m ->
        val today = LocalDate.now()
        val from = m.atDay(1)
        val to = minOf(m.atEndOfMonth(), today)
        val daily = InsightEngine.dailyMood(moods, from, to)
        // Early in a month there is too little to compare, so the current month looks back 30 days.
        val insightFrom = if (m == YearMonth.from(today)) minOf(from, today.minusDays(29)) else from
        val avg = InsightEngine.averageMood(moods, from, to)
        InsightsState(
            month = m,
            keyInsight = InsightEngine.keyInsight(habits.filter { it.active }, marks, moods, insightFrom, to),
            habits = Analytics.habitMonth(habits, marks, areas, m, today),
            tasks = Analytics.taskMonth(tasks, m, today),
            journal = Analytics.journalMonth(journal, m, today),
            averageMood = avg?.let { Mood.fromRating(it.roundToInt()) },
            averageMoodValue = avg,
            trend = daily.map { it.second },
            trendLabels = daily.map { it.first }.let { d -> if (d.size >= 3) listOf(d.first(), d[d.size / 2], d.last()) else d }
                .map { "${it.dayOfMonth} ${it.month.name.take(3).lowercase().replaceFirstChar(Char::uppercase)}" },
            rows = Analytics.monthRows(habits, marks, tasks, moods, journal, (5L downTo 0L).map { m.minusMonths(it) }, today),
            projects = Analytics.projects(projects.filter { !it.archived }, tasks, today)
                .filter { it.tasks.total > 0 }
                .sortedWith(compareBy({ it.tasks.open == 0 }, { it.tasks.nextDue ?: LocalDate.MAX })),
            today = today,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
