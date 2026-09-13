package com.mindspring.app.ui.preview

import com.mindspring.app.data.model.GratitudeEntry
import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.model.HabitFrequency
import com.mindspring.app.data.model.HabitIcon
import com.mindspring.app.data.model.JournalEntry
import com.mindspring.app.data.model.LifeArea
import com.mindspring.app.data.model.MarkState
import com.mindspring.app.data.model.Mood
import com.mindspring.app.data.model.MoodEntry
import com.mindspring.app.data.model.Priority
import com.mindspring.app.data.model.Project
import com.mindspring.app.data.model.Task
import com.mindspring.app.data.model.TaskStatus
import com.mindspring.app.domain.AreaRate
import com.mindspring.app.domain.AreaRollup
import com.mindspring.app.domain.HabitMonth
import com.mindspring.app.domain.HabitRate
import com.mindspring.app.domain.HabitStreak
import com.mindspring.app.domain.HabitUnit
import com.mindspring.app.domain.JournalMonth
import com.mindspring.app.domain.KeyInsight
import com.mindspring.app.domain.MonthRow
import com.mindspring.app.domain.ProjectRollup
import com.mindspring.app.domain.SubAreaRollup
import com.mindspring.app.domain.TaskMonth
import com.mindspring.app.domain.Tally
import com.mindspring.app.domain.TaskFlag
import com.mindspring.app.domain.TaskGroup
import com.mindspring.app.domain.TaskRollup
import com.mindspring.app.ui.screens.tasks.ProjectCard
import com.mindspring.app.ui.screens.tasks.TaskFilter
import com.mindspring.app.ui.screens.tasks.TaskSection
import com.mindspring.app.ui.screens.tasks.TasksState
import com.mindspring.app.ui.components.TickState
import com.mindspring.app.ui.screens.habits.AreaGroup
import com.mindspring.app.ui.screens.habits.HabitSummary
import com.mindspring.app.ui.screens.habits.HabitsState
import com.mindspring.app.ui.screens.habits.WeekCell
import com.mindspring.app.ui.screens.habits.WeekRow
import com.mindspring.app.ui.screens.insights.InsightsState
import com.mindspring.app.ui.screens.journal.JournalDay
import com.mindspring.app.ui.screens.journal.JournalListState
import com.mindspring.app.ui.screens.home.HomeUiState
import com.mindspring.app.ui.screens.home.StreakItem
import com.mindspring.app.ui.screens.home.TaskLine
import com.mindspring.app.ui.screens.home.TodayHabit
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

/**
 * Fixed sample data for the @Preview functions. Dates are pinned rather than taken from the clock
 * so a preview looks the same every time it is rendered, and so "overdue" and "due today" keep
 * meaning what their names say.
 */
val PreviewToday: LocalDate = LocalDate.of(2026, 3, 14)
val PreviewNow: LocalDateTime = PreviewToday.atTime(9, 41)

val PreviewHabits = listOf(
    Habit(
        id = 1,
        name = "Drink water",
        icon = HabitIcon.Water,
        frequency = HabitFrequency.Daily,
        target = "8 glasses",
        createdAt = PreviewToday.minusDays(96),
    ),
    Habit(
        id = 2,
        name = "Morning run",
        icon = HabitIcon.Run,
        frequency = HabitFrequency.Weekdays,
        target = "5 km",
        reminderEnabled = true,
        reminderTime = LocalTime.of(6, 30),
        createdAt = PreviewToday.minusDays(60),
    ),
    Habit(
        id = 3,
        name = "Read 20 pages",
        icon = HabitIcon.Book,
        frequency = HabitFrequency.Daily,
        createdAt = PreviewToday.minusDays(40),
    ),
    Habit(
        id = 4,
        name = "Meditate",
        icon = HabitIcon.Meditate,
        frequency = HabitFrequency.Weekly,
        createdAt = PreviewToday.minusDays(21),
    ),
)

val PreviewProjects = listOf(
    Project(id = 1, name = "Research Proposal", colorIndex = 0, createdAt = PreviewToday.minusDays(30)),
    Project(id = 2, name = "Flat move", colorIndex = 3, createdAt = PreviewToday.minusDays(12)),
)

val PreviewTasks = listOf(
    Task(
        id = 1,
        title = "Draft the methodology chapter",
        notes = "Cover sampling and the analysis plan.",
        projectId = 1,
        due = PreviewToday.minusDays(2),
        priority = Priority.A,
        status = TaskStatus.InProgress,
        createdAt = PreviewToday.minusDays(9),
    ),
    Task(
        id = 2,
        title = "Email supervisor about the deadline",
        due = PreviewToday,
        priority = Priority.A,
        createdAt = PreviewToday.minusDays(3),
        alertAt = PreviewToday.atTime(16, 0),
    ),
    Task(
        id = 3,
        title = "Book the viva room",
        projectId = 1,
        due = PreviewToday.plusDays(3),
        priority = Priority.B,
        createdAt = PreviewToday.minusDays(1),
    ),
    Task(
        id = 4,
        title = "Pack the kitchen boxes",
        projectId = 2,
        priority = Priority.C,
        createdAt = PreviewToday.minusDays(5),
    ),
    Task(
        id = 5,
        title = "Renew the library loan",
        due = PreviewToday.minusDays(1),
        priority = Priority.B,
        status = TaskStatus.Done,
        doneOn = PreviewToday.minusDays(1),
        createdAt = PreviewToday.minusDays(6),
    ),
)

val PreviewMood = MoodEntry(
    id = 1,
    mood = Mood.Good,
    feelings = listOf("Focused", "Grateful"),
    note = "Got a clean run at the proposal this morning.",
    loggedAt = PreviewNow.minusHours(2),
)

val PreviewJournal = JournalEntry(
    date = PreviewToday,
    rating = 4,
    energy = 3,
    highlight = "Finally cracked the methodology outline.",
    monologue = "A slow start, then two solid hours on the proposal. The run helped more than " +
        "the coffee did. Tomorrow I want to get the sampling section down before lunch.",
    tomorrowTop = "Sampling section, before lunch",
    updatedAt = PreviewNow,
)

val PreviewTodayHabits = listOf(
    TodayHabit(PreviewHabits[0], MarkState.Done, PreviewToday, streak = 12, unit = HabitUnit.Day, areaColor = 1),
    TodayHabit(PreviewHabits[1], null, null, streak = 4, unit = HabitUnit.Day, areaColor = 0),
    TodayHabit(PreviewHabits[2], null, null, streak = 7, unit = HabitUnit.Day, areaColor = 2),
    TodayHabit(PreviewHabits[3], MarkState.Skipped, null, streak = 3, unit = HabitUnit.Week, areaColor = 4),
)

val PreviewTaskLines = listOf(
    TaskLine(PreviewTasks[0], TaskFlag.Overdue, "Research Proposal"),
    TaskLine(PreviewTasks[1], TaskFlag.DueToday, null),
    TaskLine(PreviewTasks[2], TaskFlag.DueSoon, "Research Proposal"),
)

val PreviewStreaks = listOf(
    StreakItem("Drink water", 12, HabitUnit.Day),
    StreakItem("Read 20 pages", 7, HabitUnit.Day),
    StreakItem("Meditate", 3, HabitUnit.Week),
)

/** A populated Today screen: mid-morning, some habits done, a couple of tasks pressing. */
val PreviewHomeState = HomeUiState(
    loaded = true,
    firstName = "Asan",
    todayMood = PreviewMood,
    habits = PreviewTodayHabits,
    overdue = 1,
    dueToday = 1,
    dueWeek = 3,
    open = 4,
    upNext = PreviewTaskLines,
    journal = PreviewJournal,
    streaks = PreviewStreaks,
)

/** The same screen for a brand new account, so the empty states can be checked too. */
val PreviewEmptyHomeState = HomeUiState(loaded = true, firstName = "Asan")

// --- Tasks -----------------------------------------------------------------------------------

val PreviewAreas = listOf(
    LifeArea(id = 1, name = "Health & Well-being", colorIndex = 0),
    LifeArea(id = 2, name = "Study", colorIndex = 1),
    LifeArea(id = 3, name = "Home", colorIndex = 3),
)

private fun rollup(total: Int, done: Int, overdue: Int, nextDue: LocalDate?) =
    TaskRollup(total = total, done = done, open = total - done, overdue = overdue, dropped = 0, nextDue = nextDue)

val PreviewProjectCards = listOf(
    ProjectCard(PreviewProjects[0], PreviewAreas[1], rollup(8, 3, 1, PreviewToday)),
    ProjectCard(PreviewProjects[1], PreviewAreas[2], rollup(5, 1, 0, PreviewToday.plusDays(6))),
)

val PreviewTasksState = TasksState(
    loaded = true,
    sections = listOf(
        TaskSection(TaskGroup.Overdue, listOf(PreviewTaskLines[0])),
        TaskSection(TaskGroup.Today, listOf(PreviewTaskLines[1])),
        TaskSection(
            TaskGroup.Week,
            listOf(PreviewTaskLines[2], TaskLine(PreviewTasks[3], TaskFlag.NoDeadline, "Flat move")),
        ),
    ),
    counts = mapOf(
        TaskFilter.Open to 4,
        TaskFilter.Overdue to 1,
        TaskFilter.Today to 1,
        TaskFilter.Week to 3,
        TaskFilter.Done to 1,
    ),
    projects = PreviewProjectCards,
)

// --- Habits ----------------------------------------------------------------------------------

/** The Monday of the sample week, so the grid lines up with [PreviewToday] (a Saturday). */
val PreviewWeekStart: LocalDate = PreviewToday.minusDays(5)

private fun week(vararg ticks: TickState) = ticks.mapIndexed { i, tick ->
    val date = PreviewWeekStart.plusDays(i.toLong())
    WeekCell(date = date, tick = tick, enabled = !date.isAfter(PreviewToday), isToday = date == PreviewToday)
}

val PreviewHabitsState = HabitsState(
    loaded = true,
    today = PreviewTodayHabits,
    weekStart = PreviewWeekStart,
    week = listOf(
        WeekRow(
            PreviewHabits[0],
            week(TickState.Done, TickState.Done, TickState.Done, TickState.Done, TickState.Done, TickState.Done, TickState.Empty),
            areaColor = 1,
        ),
        WeekRow(
            PreviewHabits[1],
            week(TickState.Done, TickState.Empty, TickState.Done, TickState.Skipped, TickState.Done, TickState.Off, TickState.Off),
            areaColor = 0,
        ),
        WeekRow(
            PreviewHabits[2],
            week(TickState.Done, TickState.Done, TickState.Empty, TickState.Done, TickState.Empty, TickState.Empty, TickState.Empty),
            areaColor = 2,
        ),
    ),
    dayScores = listOf(1f, 0.66f, 0.66f, 0.66f, 0.66f, 0.5f, null),
    deadlines = listOf(0, 1, 0, 0, 2, 1, 0),
    groups = listOf(
        AreaGroup(
            PreviewAreas[0],
            Tally(38, 44),
            listOf(
                HabitSummary(PreviewHabits[0], Tally(26, 28), streak = 12, unit = HabitUnit.Day, areaColor = 1),
                HabitSummary(PreviewHabits[1], Tally(12, 16), streak = 4, unit = HabitUnit.Day, areaColor = 0),
            ),
        ),
        AreaGroup(
            PreviewAreas[1],
            Tally(21, 28),
            listOf(HabitSummary(PreviewHabits[2], Tally(21, 28), streak = 7, unit = HabitUnit.Day, areaColor = 2)),
        ),
    ),
)

// --- Insights --------------------------------------------------------------------------------

val PreviewMonth: YearMonth = YearMonth.from(PreviewToday)

private val previewDayScores: List<Pair<LocalDate, Float?>> =
    (1..PreviewToday.dayOfMonth).map { day ->
        val date = PreviewMonth.atDay(day)
        date to when (day % 5) {
            0 -> null
            1 -> 1f
            2 -> 0.75f
            3 -> 0.5f
            else -> 0.9f
        }
    }

val PreviewInsightsState = InsightsState(
    month = PreviewMonth,
    keyInsight = KeyInsight(habitName = "Morning run", percentHigher = 23),
    habits = HabitMonth(
        tally = Tally(96, 118),
        activeHabits = 4,
        longest = HabitStreak(PreviewHabits[0], 12),
        rates = listOf(
            HabitRate(PreviewHabits[0], Tally(13, 14)),
            HabitRate(PreviewHabits[2], Tally(11, 14)),
            HabitRate(PreviewHabits[1], Tally(6, 10)),
        ),
        best = HabitRate(PreviewHabits[0], Tally(13, 14)),
        needsAttention = HabitRate(PreviewHabits[1], Tally(6, 10)),
        byArea = listOf(
            AreaRate(PreviewAreas[0], Tally(19, 24)),
            AreaRate(PreviewAreas[1], Tally(11, 14)),
        ),
        dayScores = previewDayScores,
    ),
    tasks = TaskMonth(
        open = 4,
        overdue = 1,
        dueToday = 1,
        dueNext7 = 3,
        completed = 9,
        onTimeRate = 0.78f,
        avgDaysEarly = 1.4f,
        deadlines = previewDayScores.map { (date, _) -> date to (date.dayOfMonth % 4).coerceAtMost(2) },
    ),
    journal = JournalMonth(written = 11, elapsedDays = 14, avgRating = 3.8f, avgEnergy = 3.2f, avgWords = 142f),
    averageMood = Mood.Good,
    averageMoodValue = 3.8f,
    trend = listOf(3f, 4f, 2f, 4f, 5f, 4f, 4f),
    trendLabels = listOf("M", "T", "W", "T", "F", "S", "S"),
    rows = listOf(
        MonthRow(PreviewMonth.minusMonths(3), 0.62f, 3.2f, 8, 6),
        MonthRow(PreviewMonth.minusMonths(2), 0.71f, 3.5f, 12, 9),
        MonthRow(PreviewMonth.minusMonths(1), 0.68f, 3.4f, 10, 11),
        MonthRow(PreviewMonth, 0.81f, 3.8f, 11, 9),
    ),
    projects = listOf(
        ProjectRollup(PreviewProjects[0], rollup(8, 3, 1, PreviewToday)),
        ProjectRollup(PreviewProjects[1], rollup(5, 1, 0, PreviewToday.plusDays(6))),
    ),
    today = PreviewToday,
)

// --- Mood ------------------------------------------------------------------------------------

val PreviewMoodHistory = listOf(
    PreviewMood,
    MoodEntry(2, Mood.Okay, listOf("Tired", "Stressed"), "Long lab session, not much left in the tank.", PreviewNow.minusDays(1)),
    MoodEntry(3, Mood.Great, listOf("Motivated", "Calm"), "", PreviewNow.minusDays(2)),
    MoodEntry(4, Mood.Bad, listOf("Anxious"), "Deadline nerves.", PreviewNow.minusDays(3)),
    MoodEntry(5, Mood.Good, listOf("Focused"), "", PreviewNow.minusDays(4)),
)

val PreviewGratitude = listOf(
    GratitudeEntry(1, "The library stayed open late, which saved the afternoon.", PreviewNow.minusHours(3)),
    GratitudeEntry(2, "Ammi's call came right when I needed it.", PreviewNow.minusDays(1)),
    GratitudeEntry(3, "Finished the run without stopping for the first time.", PreviewNow.minusDays(2)),
)

// --- Life areas ------------------------------------------------------------------------------

val PreviewAreaRollups = listOf(
    AreaRollup(
        area = PreviewAreas[0],
        activeHabits = 2,
        habitTally = Tally(19, 24),
        tasks = rollup(6, 4, 0, PreviewToday.plusDays(2)),
        subAreas = listOf(
            SubAreaRollup("Fitness", activeHabits = 1, habitTally = Tally(12, 16), tasks = rollup(2, 2, 0, null)),
            SubAreaRollup("Sleep", activeHabits = 1, habitTally = Tally(7, 8), tasks = rollup(0, 0, 0, null)),
        ),
    ),
    AreaRollup(
        area = PreviewAreas[1],
        activeHabits = 1,
        habitTally = Tally(11, 14),
        tasks = rollup(8, 3, 1, PreviewToday),
        subAreas = listOf(
            SubAreaRollup("Dissertation", activeHabits = 1, habitTally = Tally(11, 14), tasks = rollup(8, 3, 1, PreviewToday)),
        ),
    ),
    AreaRollup(
        area = PreviewAreas[2],
        activeHabits = 0,
        habitTally = Tally(0, 0),
        tasks = rollup(5, 1, 0, PreviewToday.plusDays(6)),
        subAreas = emptyList(),
    ),
)

// --- Journal ---------------------------------------------------------------------------------

val PreviewJournalListState = JournalListState(
    loaded = true,
    days = (0L..13L).map { back ->
        val date = PreviewToday.minusDays(back)
        JournalDay(
            date = date,
            entry = when {
                back % 4 == 3L -> null
                back == 0L -> PreviewJournal
                else -> PreviewJournal.copy(
                    date = date,
                    rating = ((back % 5) + 1).toInt(),
                    monologue = "Day ${date.dayOfMonth}. " + PreviewJournal.monologue,
                )
            },
        )
    },
    older = listOf(
        PreviewJournal.copy(date = PreviewToday.minusDays(20), rating = 3),
        PreviewJournal.copy(date = PreviewToday.minusDays(24), rating = 5),
    ),
    month = JournalMonth(written = 11, elapsedDays = 14, avgRating = 3.8f, avgEnergy = 3.2f, avgWords = 142f),
    streak = 5,
)
