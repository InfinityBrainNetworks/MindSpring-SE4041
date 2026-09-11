package com.mindspring.app.domain

import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.model.HabitMark
import com.mindspring.app.data.model.JournalEntry
import com.mindspring.app.data.model.LifeArea
import com.mindspring.app.data.model.MarkState
import com.mindspring.app.data.model.MoodEntry
import com.mindspring.app.data.model.Project
import com.mindspring.app.data.model.Task
import com.mindspring.app.data.model.TaskStatus
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

fun List<HabitMark>.byHabit(): Map<Long, Map<LocalDate, MarkState>> =
    groupBy { it.habitId }.mapValues { (_, list) -> list.associate { it.date to it.state } }

data class HabitRate(val habit: Habit, val tally: Tally) {
    val rate: Float get() = tally.rate
}

data class AreaRate(val area: LifeArea?, val tally: Tally)

data class HabitStreak(val habit: Habit, val count: Int)

/** The Dashboard's "Habits - this month" block. */
data class HabitMonth(
    val tally: Tally,
    val activeHabits: Int,
    val longest: HabitStreak?,
    /** Habits that had anything to judge this month, best first. */
    val rates: List<HabitRate>,
    val best: HabitRate?,
    val needsAttention: HabitRate?,
    val byArea: List<AreaRate>,
    /** Each elapsed day of the month with its day score (null when nothing was due). */
    val dayScores: List<Pair<LocalDate, Float?>>,
)

/** The Dashboard's "Tasks and deadlines" block. Open counts are as of today; the rest are for the month. */
data class TaskMonth(
    val open: Int,
    val overdue: Int,
    val dueToday: Int,
    val dueNext7: Int,
    val completed: Int,
    val onTimeRate: Float?,
    val avgDaysEarly: Float?,
    /** Deadlines still open, per day of the month - the workbook's crunch-week row. */
    val deadlines: List<Pair<LocalDate, Int>>,
)

data class JournalMonth(
    val written: Int,
    val elapsedDays: Int,
    val avgRating: Float?,
    val avgEnergy: Float?,
    val avgWords: Float?,
)

/** One line of the month-by-month table. */
data class MonthRow(
    val month: YearMonth,
    val habitRate: Float?,
    val avgMood: Float?,
    val journalled: Int,
    val tasksDone: Int,
)

data class ProjectRollup(val project: Project, val tasks: TaskRollup)

data class SubAreaRollup(
    val name: String,
    val activeHabits: Int,
    val habitTally: Tally,
    val tasks: TaskRollup,
) {
    val state: String
        get() = when {
            tasks.total == 0 -> if (activeHabits > 0) "Habits only" else ""
            tasks.open == 0 -> "Tasks complete"
            tasks.overdue > 0 -> "${tasks.overdue} overdue"
            else -> "${tasks.open} open"
        }
}

data class AreaRollup(
    val area: LifeArea?,
    val activeHabits: Int,
    val habitTally: Tally,
    val tasks: TaskRollup,
    val subAreas: List<SubAreaRollup>,
)

/** Pure month-based analytics behind Insights, Projects and Life Areas. */
object Analytics {

    private fun YearMonth.range() = atDay(1)..atEndOfMonth()

    fun habitMonth(
        habits: List<Habit>,
        marks: List<HabitMark>,
        areas: List<LifeArea>,
        month: YearMonth,
        today: LocalDate,
    ): HabitMonth {
        val byHabit = marks.byHabit()
        val from = month.atDay(1)
        val to = month.atEndOfMonth()
        val active = habits.filter { it.active }
        val rates = active.map { h -> HabitRate(h, HabitStats.tally(h, byHabit[h.id].orEmpty(), from, to, today)) }
            .filter { it.tally.possible > 0 }
            .sortedWith(compareByDescending<HabitRate> { it.rate }.thenBy { it.habit.name })
        val total = rates.fold(Tally.Zero) { acc, r -> acc + r.tally }
        val areaById = areas.associateBy { it.id }
        val byArea = rates.groupBy { it.habit.areaId?.let(areaById::get) }
            .map { (area, list) -> AreaRate(area, list.fold(Tally.Zero) { acc, r -> acc + r.tally }) }
            .sortedBy { it.area?.sortOrder ?: Int.MAX_VALUE }
        // "Longest streak" is the live one, so it only means something for the current month.
        val longest = if (month == YearMonth.from(today)) {
            active.map { HabitStreak(it, HabitStats.currentStreak(it, byHabit[it.id].orEmpty(), today)) }
                .filter { it.count > 0 }
                .maxByOrNull { it.count }
        } else null
        val lastDay = minOf(to, today)
        val days = if (lastDay.isBefore(from)) emptyList() else generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(lastDay) }.toList()
        return HabitMonth(
            tally = total,
            activeHabits = active.size,
            longest = longest,
            rates = rates,
            best = rates.firstOrNull(),
            needsAttention = rates.lastOrNull()?.takeIf { rates.size > 1 && it.rate < 1f },
            byArea = byArea,
            dayScores = days.map { it to HabitStats.dayScore(active, byHabit, it) },
        )
    }

    fun taskMonth(tasks: List<Task>, month: YearMonth, today: LocalDate): TaskMonth {
        val open = tasks.filter { it.status.isOpen }
        val range = month.range()
        val finished = tasks.filter { it.status == TaskStatus.Done && it.doneOn != null && it.doneOn in range }
        val dated = finished.filter { it.due != null }
        return TaskMonth(
            open = open.size,
            overdue = open.count { TaskLogic.isOverdue(it, today) },
            dueToday = open.count { it.due == today },
            dueNext7 = open.count { it.due != null && it.due.isAfter(today) && !it.due.isAfter(today.plusDays(7)) },
            completed = finished.size,
            onTimeRate = if (dated.isEmpty()) null else dated.count { !it.doneOn!!.isAfter(it.due) }.toFloat() / dated.size,
            avgDaysEarly = if (dated.isEmpty()) null else dated.map { ChronoUnit.DAYS.between(it.doneOn, it.due).toFloat() }.average().toFloat(),
            deadlines = generateSequence(range.start) { it.plusDays(1) }.takeWhile { !it.isAfter(range.endInclusive) }
                .map { day -> day to open.count { it.due == day } }
                .toList(),
        )
    }

    fun journalMonth(entries: List<JournalEntry>, month: YearMonth, today: LocalDate): JournalMonth {
        val range = month.range()
        val inMonth = entries.filter { it.date in range }
        val written = inMonth.filter { it.words > 0 }
        val lastDay = minOf(range.endInclusive, today)
        val elapsed = if (lastDay.isBefore(range.start)) 0 else ChronoUnit.DAYS.between(range.start, lastDay).toInt() + 1
        return JournalMonth(
            written = written.size,
            elapsedDays = elapsed,
            avgRating = inMonth.mapNotNull { it.rating }.averageOrNull(),
            avgEnergy = inMonth.mapNotNull { it.energy }.averageOrNull(),
            avgWords = written.map { it.words }.averageOrNull(),
        )
    }

    fun monthRows(
        habits: List<Habit>,
        marks: List<HabitMark>,
        tasks: List<Task>,
        moods: List<MoodEntry>,
        journal: List<JournalEntry>,
        months: List<YearMonth>,
        today: LocalDate,
    ): List<MonthRow> {
        val byHabit = marks.byHabit()
        val active = habits.filter { it.active }
        return months.map { m ->
            val range = m.range()
            val tally = active.fold(Tally.Zero) { acc, h -> acc + HabitStats.tally(h, byHabit[h.id].orEmpty(), range.start, range.endInclusive, today) }
            MonthRow(
                month = m,
                habitRate = if (tally.possible == 0) null else tally.rate,
                avgMood = InsightEngine.averageMood(moods, range.start, range.endInclusive),
                journalled = journal.count { it.date in range && it.words > 0 },
                tasksDone = tasks.count { it.status == TaskStatus.Done && it.doneOn != null && it.doneOn in range },
            )
        }
    }

    fun projects(projects: List<Project>, tasks: List<Task>, today: LocalDate): List<ProjectRollup> {
        val byProject = tasks.groupBy { it.projectId }
        return projects.map { ProjectRollup(it, TaskLogic.rollup(byProject[it.id].orEmpty(), today)) }
    }

    /**
     * The Sub-areas sheet, nested under life areas. A sub-area gathers habits and tasks that share
     * its (trimmed, case-insensitive) name within an area; habit rates are for [month].
     */
    fun areas(
        areas: List<LifeArea>,
        habits: List<Habit>,
        marks: List<HabitMark>,
        tasks: List<Task>,
        month: YearMonth,
        today: LocalDate,
    ): List<AreaRollup> {
        val byHabit = marks.byHabit()
        val range = month.range()
        val active = habits.filter { it.active }
        val tallies = active.associate { it.id to HabitStats.tally(it, byHabit[it.id].orEmpty(), range.start, range.endInclusive, today) }
        val areaIds = areas.map { it.id }.toSet()
        fun areaOf(id: Long?) = id?.takeIf { it in areaIds }

        // Every area in order, then a bucket for work not filed under any area.
        val buckets: List<LifeArea?> = areas.sortedBy { it.sortOrder } + listOf<LifeArea?>(null)
        return buckets.mapNotNull { area ->
            val aHabits = active.filter { areaOf(it.areaId) == area?.id }
            val aTasks = tasks.filter { areaOf(it.areaId) == area?.id }
            if (area == null && aHabits.isEmpty() && aTasks.isEmpty()) return@mapNotNull null
            val names = linkedMapOf<String, String>()
            (aHabits.map { it.subArea } + aTasks.map { it.subArea }).filter { it.isNotBlank() }.forEach { names.putIfAbsent(groupKey(it), it.trim()) }
            val subs = names.map { (key, name) ->
                val sh = aHabits.filter { groupKey(it.subArea) == key }
                SubAreaRollup(
                    name = name,
                    activeHabits = sh.size,
                    habitTally = sh.fold(Tally.Zero) { acc, h -> acc + tallies.getValue(h.id) },
                    tasks = TaskLogic.rollup(aTasks.filter { groupKey(it.subArea) == key }, today),
                )
            }.sortedBy { it.name.lowercase() }
            AreaRollup(
                area = area,
                activeHabits = aHabits.size,
                habitTally = aHabits.fold(Tally.Zero) { acc, h -> acc + tallies.getValue(h.id) },
                tasks = TaskLogic.rollup(aTasks, today),
                subAreas = subs,
            )
        }
    }

    private fun List<Int>.averageOrNull(): Float? = if (isEmpty()) null else average().toFloat()
}
