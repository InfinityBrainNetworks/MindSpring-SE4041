package com.mindspring.app.domain

import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.model.HabitMark
import com.mindspring.app.data.model.MarkState
import com.mindspring.app.data.model.MoodEntry
import java.time.LocalDate
import kotlin.math.roundToInt

/** A plain-language correlation such as "mood is 32% higher on days you exercise". */
data class KeyInsight(val habitName: String, val percentHigher: Int)

/** Pure mood analytics, and the link between mood and habits. */
object InsightEngine {

    /** Average mood rating per calendar day, oldest first; days without entries are skipped. */
    fun dailyMood(entries: List<MoodEntry>, from: LocalDate, to: LocalDate): List<Pair<LocalDate, Float>> =
        entries
            .filter { val d = it.loggedAt.toLocalDate(); !d.isBefore(from) && !d.isAfter(to) }
            .groupBy { it.loggedAt.toLocalDate() }
            .map { (day, list) -> day to list.map { it.mood.rating }.average().toFloat() }
            .sortedBy { it.first }

    fun averageMood(entries: List<MoodEntry>, from: LocalDate, to: LocalDate): Float? =
        dailyMood(entries, from, to).map { it.second }.takeIf { it.isNotEmpty() }?.average()?.toFloat()

    /**
     * For each habit, compares the average mood on days it was done against days it was not.
     * Returns the strongest positive difference, or null when there is not yet enough data.
     */
    fun keyInsight(
        habits: List<Habit>,
        marks: List<HabitMark>,
        entries: List<MoodEntry>,
        from: LocalDate,
        to: LocalDate,
        minDaysEachSide: Int = 2,
    ): KeyInsight? {
        val moodByDay = dailyMood(entries, from, to).toMap()
        if (moodByDay.size < minDaysEachSide * 2) return null
        val doneByHabit = marks.filter { it.state == MarkState.Done }
            .groupBy({ it.habitId }, { it.date })
            .mapValues { it.value.toSet() }

        return habits.mapNotNull { habit ->
            val done = doneByHabit[habit.id].orEmpty()
            val (withHabit, without) = moodByDay.entries.partition { it.key in done }
            if (withHabit.size < minDaysEachSide || without.size < minDaysEachSide) return@mapNotNull null
            val avgWith = withHabit.map { it.value }.average()
            val avgWithout = without.map { it.value }.average()
            val percent = ((avgWith - avgWithout) / avgWithout * 100).roundToInt()
            if (percent >= 5) KeyInsight(habit.name, percent) else null
        }.maxByOrNull { it.percentHigher }
    }
}
