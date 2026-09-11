package com.mindspring.app.domain

import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.model.HabitFrequency
import com.mindspring.app.data.model.MarkState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** Done out of possible over some span. Possible only counts time that has actually passed. */
data class Tally(val done: Int, val possible: Int) {
    val rate: Float get() = if (possible == 0) 0f else done.toFloat() / possible
    operator fun plus(other: Tally) = Tally(done + other.done, possible + other.possible)

    companion object {
        val Zero = Tally(0, 0)
    }
}

/** The calendar unit a habit is judged on. */
enum class HabitUnit(val singular: String, val plural: String) {
    Day("day", "days"),
    Week("week", "weeks"),
    Month("month", "months");

    fun count(n: Int): String = "$n ${if (n == 1) singular else plural}"
}

/**
 * Pure streak and completion maths for a single habit. `marks` maps a date to what was logged.
 *
 * Rules, taken from the Life Tracker workbook with two deliberate changes:
 *  - Day-based habits are judged on the weekdays they cover; Weekly and Monthly habits once per
 *    week (Monday to Sunday) or calendar month, done on whichever day suits.
 *  - A skip is excused: it leaves the rate's denominator and a streak passes over it. (The
 *    workbook let a dash reset the streak, which punished exactly the days it was meant to excuse.)
 *  - Only elapsed time counts. Today, or the current week or month, joins the denominator once
 *    it is done - so on the 11th a month rate reads out of 11 days, not 30.
 */
object HabitStats {

    fun unit(habit: Habit): HabitUnit = when (habit.frequency) {
        HabitFrequency.Weekly -> HabitUnit.Week
        HabitFrequency.Monthly -> HabitUnit.Month
        else -> HabitUnit.Day
    }

    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** True on the days a day-based habit is due. Weekly and Monthly habits are never "scheduled" on a day. */
    fun isScheduled(habit: Habit, date: LocalDate): Boolean =
        habit.frequency.isDayBased && date.dayOfWeek in habit.days && !date.isBefore(habit.createdAt)

    /**
     * Whether the habit belongs on the list for [date]: a day-based habit on its days; a Weekly or
     * Monthly one throughout its week or month, so it can be done on any of them.
     */
    fun belongsOn(habit: Habit, date: LocalDate): Boolean =
        habit.active && !date.isBefore(habit.createdAt) && (!habit.frequency.isDayBased || date.dayOfWeek in habit.days)

    /** The span of the unit containing [date]. */
    fun unitSpan(habit: Habit, date: LocalDate): ClosedRange<LocalDate> = when (unit(habit)) {
        HabitUnit.Day -> date..date
        HabitUnit.Week -> weekStart(date).let { it..it.plusDays(6) }
        HabitUnit.Month -> YearMonth.from(date).let { it.atDay(1)..it.atEndOfMonth() }
    }

    /** Whether the week or month containing [date] already holds a done mark (for a day habit: that day). */
    fun unitDone(habit: Habit, marks: Map<LocalDate, MarkState>, date: LocalDate): Boolean {
        val span = unitSpan(habit, date)
        return marks.any { (d, s) -> s == MarkState.Done && d in span }
    }

    private enum class UnitState { Done, Skipped, Missed, Pending, NotDue }

    /** One unit's outcome, looking only at marks inside it and time up to [today]. */
    private fun unitState(habit: Habit, marks: Map<LocalDate, MarkState>, span: ClosedRange<LocalDate>, today: LocalDate): UnitState {
        if (span.endInclusive.isBefore(habit.createdAt) || span.start.isAfter(today)) return UnitState.NotDue
        if (unit(habit) == HabitUnit.Day) {
            val day = span.start
            if (day.dayOfWeek !in habit.days) return UnitState.NotDue
            return when (marks[day]) {
                MarkState.Done -> UnitState.Done
                MarkState.Skipped -> UnitState.Skipped
                null -> if (day == today) UnitState.Pending else UnitState.Missed
            }
        }
        val inSpan = marks.filterKeys { it in span }
        return when {
            inSpan.values.any { it == MarkState.Done } -> UnitState.Done
            inSpan.values.any { it == MarkState.Skipped } -> UnitState.Skipped
            !span.endInclusive.isBefore(today) -> UnitState.Pending
            else -> UnitState.Missed
        }
    }

    /** Units overlapping [from, to], oldest first. */
    private fun units(habit: Habit, from: LocalDate, to: LocalDate): Sequence<ClosedRange<LocalDate>> {
        if (to.isBefore(from)) return emptySequence()
        val first = unitSpan(habit, from)
        return generateSequence(first) { prev -> unitSpan(habit, prev.endInclusive.plusDays(1)) }
            .takeWhile { !it.start.isAfter(to) }
    }

    fun tally(habit: Habit, marks: Map<LocalDate, MarkState>, from: LocalDate, to: LocalDate, today: LocalDate): Tally {
        var done = 0
        var possible = 0
        val start = maxOf(from, habit.createdAt)
        val end = minOf(to, today)
        for (span in units(habit, start, end)) {
            when (unitState(habit, marks, span, today)) {
                UnitState.Done -> { done++; possible++ }
                UnitState.Missed -> possible++
                else -> Unit
            }
        }
        return Tally(done, possible)
    }

    fun completionRate(habit: Habit, marks: Map<LocalDate, MarkState>, from: LocalDate, to: LocalDate, today: LocalDate): Float =
        tally(habit, marks, from, to, today).rate

    /**
     * Consecutive done units counting back from today. The current unit, while still pending,
     * does not break the streak; skipped units are passed over without adding to it.
     */
    fun currentStreak(habit: Habit, marks: Map<LocalDate, MarkState>, today: LocalDate): Int =
        streakUnits(habit, marks, today).size

    /** The done dates inside the units that make up the current streak (for the amber dots). */
    fun streakDates(habit: Habit, marks: Map<LocalDate, MarkState>, today: LocalDate): Set<LocalDate> =
        streakUnits(habit, marks, today).flatMap { span -> marks.filter { (d, s) -> s == MarkState.Done && d in span }.keys }.toSet()

    private fun streakUnits(habit: Habit, marks: Map<LocalDate, MarkState>, today: LocalDate): List<ClosedRange<LocalDate>> {
        val out = mutableListOf<ClosedRange<LocalDate>>()
        var span = unitSpan(habit, today)
        while (!span.endInclusive.isBefore(habit.createdAt)) {
            when (unitState(habit, marks, span, today)) {
                UnitState.Done -> out += span
                UnitState.Missed -> break
                else -> Unit // pending, skipped or not due: pass over
            }
            span = unitSpan(habit, span.start.minusDays(1))
        }
        return out
    }

    fun bestStreak(habit: Habit, marks: Map<LocalDate, MarkState>, today: LocalDate): Int {
        var best = 0
        var run = 0
        for (span in units(habit, habit.createdAt, today)) {
            when (unitState(habit, marks, span, today)) {
                UnitState.Done -> { run++; best = maxOf(best, run) }
                UnitState.Missed -> run = 0
                else -> Unit
            }
        }
        return best
    }

    /** Progress inside the current week: done days of scheduled days, or 0/1 for a Weekly habit. */
    fun weekProgress(habit: Habit, marks: Map<LocalDate, MarkState>, today: LocalDate): Pair<Int, Int> {
        val monday = weekStart(today)
        val week = (0L..6L).map { monday.plusDays(it) }
        return when (unit(habit)) {
            HabitUnit.Day -> {
                val due = week.filter { it.dayOfWeek in habit.days && !it.isBefore(habit.createdAt) && marks[it] != MarkState.Skipped }
                due.count { marks[it] == MarkState.Done } to due.size
            }
            HabitUnit.Week -> (if (unitDone(habit, marks, today)) 1 else 0) to 1
            HabitUnit.Month -> (if (unitDone(habit, marks, today)) 1 else 0) to 1
        }
    }

    /**
     * The workbook's day score: of the day-based habits due on [date], the share done. Weekly and
     * Monthly habits are left out, since no single day is "theirs". Null when nothing was due.
     */
    fun dayScore(habits: List<Habit>, marksByHabit: Map<Long, Map<LocalDate, MarkState>>, date: LocalDate): Float? {
        var done = 0
        var possible = 0
        habits.filter { it.active && isScheduled(it, date) }.forEach { h ->
            when (marksByHabit[h.id]?.get(date)) {
                MarkState.Done -> { done++; possible++ }
                MarkState.Skipped -> Unit
                null -> possible++
            }
        }
        return if (possible == 0) null else done.toFloat() / possible
    }
}
