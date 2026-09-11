package com.mindspring.app.domain

import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.model.HabitMark
import com.mindspring.app.data.model.JournalEntry
import com.mindspring.app.data.model.LifeArea
import com.mindspring.app.data.model.MarkState
import com.mindspring.app.data.model.Task
import com.mindspring.app.data.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class AnalyticsTest {
    private val today = LocalDate.of(2026, 9, 11)
    private val month = YearMonth.of(2026, 9)
    private val health = LifeArea(1, "Health", 0, 0)
    private val study = LifeArea(2, "Study", 3, 1)

    @Test fun habitMonthRollsUpByAreaAndPicksBestAndWorst() {
        val water = Habit(1, "Water", areaId = 1, subArea = "Physical", createdAt = today.withDayOfMonth(1))
        val read = Habit(2, "Read", areaId = 2, subArea = "Research", createdAt = today.withDayOfMonth(1))
        val marks = (1..10).map { HabitMark(1, today.withDayOfMonth(it), MarkState.Done) } +
            listOf(HabitMark(2, today.withDayOfMonth(1), MarkState.Done))
        val m = Analytics.habitMonth(listOf(water, read), marks, listOf(health, study), month, today)
        assertEquals(Tally(11, 20), m.tally)
        assertEquals("Water", m.best!!.habit.name)
        assertEquals("Read", m.needsAttention!!.habit.name)
        assertEquals(listOf("Health", "Study"), m.byArea.map { it.area!!.name })
        assertEquals(10, m.longest!!.count)
        assertEquals(11, m.dayScores.size)
    }

    @Test fun taskMonthMeasuresDelivery() {
        val tasks = listOf(
            Task(1, "early", due = today.withDayOfMonth(5), status = TaskStatus.Done, doneOn = today.withDayOfMonth(3), createdAt = today),
            Task(2, "late", due = today.withDayOfMonth(6), status = TaskStatus.Done, doneOn = today.withDayOfMonth(8), createdAt = today),
            Task(3, "open", due = today.minusDays(1), createdAt = today),
            Task(4, "soon", due = today.plusDays(2), createdAt = today),
            Task(5, "today", due = today, createdAt = today),
        )
        val t = Analytics.taskMonth(tasks, month, today)
        assertEquals(3, t.open)
        assertEquals(1, t.overdue)
        assertEquals(1, t.dueToday)
        assertEquals(1, t.dueNext7)
        assertEquals(2, t.completed)
        assertEquals(0.5f, t.onTimeRate!!, 0.001f)
        assertEquals(0f, t.avgDaysEarly!!, 0.001f)
        assertEquals(1, t.deadlines.first { it.first == today }.second)
    }

    @Test fun journalMonthCountsWrittenDays() {
        val entries = listOf(
            JournalEntry(today.withDayOfMonth(2), rating = 4, energy = 3, monologue = "one two three"),
            JournalEntry(today.withDayOfMonth(3), rating = 2, monologue = ""),
        )
        val j = Analytics.journalMonth(entries, month, today)
        assertEquals(1, j.written)
        assertEquals(11, j.elapsedDays)
        assertEquals(3f, j.avgRating!!, 0.001f)
        assertEquals(3f, j.avgEnergy!!, 0.001f)
        assertEquals(3f, j.avgWords!!, 0.001f)
        assertNull(Analytics.journalMonth(emptyList(), month, today).avgWords)
    }

    @Test fun subAreasJoinHabitsAndTasksIgnoringCase() {
        val habit = Habit(1, "Read", areaId = 2, subArea = "Research", createdAt = today.withDayOfMonth(1))
        val tasks = listOf(
            Task(1, "Paper", areaId = 2, subArea = "research ", due = today.plusDays(3), createdAt = today),
            Task(2, "Budget", areaId = null, subArea = "Money", createdAt = today),
        )
        val areas = Analytics.areas(listOf(health, study), listOf(habit), emptyList(), tasks, month, today)
        val studyRollup = areas.first { it.area?.id == 2L }
        assertEquals(1, studyRollup.subAreas.size)
        assertEquals(1, studyRollup.subAreas[0].activeHabits)
        assertEquals(1, studyRollup.subAreas[0].tasks.total)
        // Work filed under no area gets its own bucket at the end.
        assertNull(areas.last().area)
        assertEquals("Money", areas.last().subAreas.single().name)
        assertEquals("Habits only", Analytics.areas(listOf(study), listOf(habit), emptyList(), emptyList(), month, today)[0].subAreas[0].state)
    }
}
