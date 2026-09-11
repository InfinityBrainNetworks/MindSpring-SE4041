package com.mindspring.app.domain

import com.mindspring.app.data.model.Habit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Pure streak and completion maths for a single habit. `done` is the set of completed dates. */
object HabitStats {

    fun isScheduled(habit: Habit, date: LocalDate): Boolean =
        date.dayOfWeek in habit.days && !date.isBefore(habit.createdAt)

    /**
     * Consecutive scheduled days completed, counting back from today. A scheduled day that is
     * still in progress (today, not yet ticked) does not break the streak. Rest days are skipped.
     */
    fun currentStreak(habit: Habit, done: Set<LocalDate>, today: LocalDate): Int =
        streakDates(habit, done, today).size

    /** The scheduled, completed dates that make up the current streak. */
    fun streakDates(habit: Habit, done: Set<LocalDate>, today: LocalDate): Set<LocalDate> {
        if (habit.days.isEmpty()) return emptySet()
        var day = if (isScheduled(habit, today) && today !in done) today.minusDays(1) else today
        val dates = mutableSetOf<LocalDate>()
        while (!day.isBefore(habit.createdAt)) {
            when {
                !isScheduled(habit, day) -> Unit
                day in done -> dates += day
                else -> break
            }
            day = day.minusDays(1)
        }
        return dates
    }

    fun bestStreak(habit: Habit, done: Set<LocalDate>, today: LocalDate): Int {
        var best = 0
        var run = 0
        var day = habit.createdAt
        while (!day.isAfter(today)) {
            if (isScheduled(habit, day)) {
                if (day in done) {
                    run++
                    best = maxOf(best, run)
                } else if (day != today) {
                    run = 0
                }
            }
            day = day.plusDays(1)
        }
        return best
    }

    /** Share of scheduled days in [from, to] that were completed, from 0 to 1. */
    fun completionRate(habit: Habit, done: Set<LocalDate>, from: LocalDate, to: LocalDate): Float {
        var scheduled = 0
        var completed = 0
        var day = maxOf(from, habit.createdAt)
        while (!day.isAfter(to)) {
            if (isScheduled(habit, day)) {
                scheduled++
                if (day in done) completed++
            }
            day = day.plusDays(1)
        }
        return if (scheduled == 0) 0f else completed.toFloat() / scheduled
    }

    /** Completions in the Monday-to-Sunday week containing [today]. */
    fun completedThisWeek(done: Set<LocalDate>, today: LocalDate): Int {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return done.count { !it.isBefore(monday) && !it.isAfter(today) }
    }
}
