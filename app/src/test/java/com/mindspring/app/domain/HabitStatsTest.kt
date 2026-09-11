package com.mindspring.app.domain

import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.model.HabitFrequency
import com.mindspring.app.data.model.MarkState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitStatsTest {
    // Friday 11 September 2026.
    private val today = LocalDate.of(2026, 9, 11)
    private val start = LocalDate.of(2026, 8, 1)

    private fun habit(freq: HabitFrequency = HabitFrequency.Daily, created: LocalDate = start, id: Long = 1) =
        Habit(id = id, name = "h$id", frequency = freq, createdAt = created)

    private fun done(vararg days: Int) = days.associate { today.withDayOfMonth(it) to MarkState.Done }

    @Test fun rateCountsOnlyElapsedDays() {
        // Done 9 of the 10 finished days in September; today is still open and not counted.
        val marks = (1..10).filter { it != 4 }.associate { today.withDayOfMonth(it) to MarkState.Done }
        val t = HabitStats.tally(habit(), marks, today.withDayOfMonth(1), today.withDayOfMonth(30), today)
        assertEquals(Tally(9, 10), t)
    }

    @Test fun todayCountsOnceDone() {
        val t = HabitStats.tally(habit(), done(11), today, today, today)
        assertEquals(Tally(1, 1), t)
    }

    @Test fun skipLeavesTheDenominator() {
        val marks = done(8, 9, 10) + (today.withDayOfMonth(7) to MarkState.Skipped)
        val t = HabitStats.tally(habit(), marks, today.withDayOfMonth(7), today.withDayOfMonth(10), today)
        assertEquals(Tally(3, 3), t)
    }

    @Test fun weekdayHabitIgnoresWeekends() {
        // Mon 7 to Fri 11: four finished weekdays, today pending.
        val t = HabitStats.tally(habit(HabitFrequency.Weekdays), done(7, 8, 10), today.withDayOfMonth(5), today, today)
        assertEquals(Tally(3, 4), t)
    }

    @Test fun streakPassesOverSkipsAndPendingToday() {
        val marks = done(8, 10) + (today.withDayOfMonth(9) to MarkState.Skipped)
        assertEquals(2, HabitStats.currentStreak(habit(), marks, today))
    }

    @Test fun missedDayBreaksStreak() {
        assertEquals(1, HabitStats.currentStreak(habit(), done(8, 10), today))
    }

    @Test fun weeklyHabitIsJudgedPerWeek() {
        val h = habit(HabitFrequency.Weekly)
        // Done in the weeks of 24 Aug and 31 Aug, not yet in the current week (pending), not the week of 17 Aug.
        val marks = mapOf(LocalDate.of(2026, 8, 26) to MarkState.Done, LocalDate.of(2026, 9, 2) to MarkState.Done)
        assertEquals(2, HabitStats.currentStreak(h, marks, today))
        val sept = HabitStats.tally(h, marks, today.withDayOfMonth(1), today.withDayOfMonth(30), today)
        // Weeks overlapping September so far: 31 Aug (done) and 7 Sep (pending, not counted).
        assertEquals(Tally(1, 1), sept)
        assertTrue(HabitStats.belongsOn(h, today))
        assertFalse(HabitStats.unitDone(h, marks, today))
    }

    @Test fun monthlyHabitDoneOnceCoversTheMonth() {
        val h = habit(HabitFrequency.Monthly)
        val marks = done(3)
        assertTrue(HabitStats.unitDone(h, marks, today))
        assertEquals(Tally(1, 1), HabitStats.tally(h, marks, today.withDayOfMonth(1), today.withDayOfMonth(30), today))
    }

    @Test fun bestStreakFindsLongestRun() {
        val marks = done(1, 2, 3, 5, 6, 7, 8, 10)
        assertEquals(4, HabitStats.bestStreak(habit(created = today.withDayOfMonth(1)), marks, today))
    }

    @Test fun dayScoreUsesOnlyDayHabitsDueThatDay() {
        val daily = habit(id = 1)
        val weekday = habit(HabitFrequency.Weekdays, id = 2)
        val weekly = habit(HabitFrequency.Weekly, id = 3)
        val saturday = LocalDate.of(2026, 9, 5)
        val byHabit = mapOf(1L to mapOf(saturday to MarkState.Done), 3L to mapOf(saturday to MarkState.Done))
        // Only the daily habit is due on a Saturday, and it was done.
        assertEquals(1f, HabitStats.dayScore(listOf(daily, weekday, weekly), byHabit, saturday)!!, 0.001f)
        assertNull(HabitStats.dayScore(listOf(weekday), emptyMap(), saturday))
    }

    @Test fun nothingCountsBeforeCreation() {
        val h = habit(created = today.withDayOfMonth(9))
        val t = HabitStats.tally(h, done(9), today.withDayOfMonth(1), today, today)
        assertEquals(Tally(1, 2), t)
    }
}
