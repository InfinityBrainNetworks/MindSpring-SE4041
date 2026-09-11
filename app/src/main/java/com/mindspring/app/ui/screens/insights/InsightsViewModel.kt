package com.mindspring.app.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindspring.app.data.model.Mood
import com.mindspring.app.data.model.Period
import com.mindspring.app.data.repository.HabitRepository
import com.mindspring.app.data.repository.MoodRepository
import com.mindspring.app.domain.HabitRate
import com.mindspring.app.domain.HabitStats
import com.mindspring.app.domain.InsightEngine
import com.mindspring.app.domain.KeyInsight
import com.mindspring.app.ui.util.Fmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class InsightsState(
    val period: Period = Period.Week,
    val keyInsight: KeyInsight? = null,
    val trend: List<Float> = emptyList(),
    val trendLabels: List<String> = emptyList(),
    val rates: List<HabitRate> = emptyList(),
    val averageMood: Mood? = null,
    val bestStreak: Int = 0,
    val bestStreakHabit: String? = null,
    val weekActivity: List<Float> = emptyList(),
    val weekLabels: List<String> = emptyList(),
)

class InsightsViewModel(habits: HabitRepository, moods: MoodRepository) : ViewModel() {
    val period = MutableStateFlow(Period.Week)

    val state: StateFlow<InsightsState> = combine(period, habits.habits, habits.completions, moods.entries) { p, hs, cs, ms ->
        val today = LocalDate.now()
        val from = today.minusDays(p.days - 1)
        val done = cs.groupBy({ it.habitId }, { it.date }).mapValues { it.value.toSet() }

        val daily = InsightEngine.dailyMood(ms, from, today)
        // A year of daily points is unreadable on a phone, so the Year view averages by month.
        val (trend, trendDates) = if (p == Period.Year) {
            daily.groupBy { YearMonth.from(it.first) }.toSortedMap()
                .map { (month, days) -> days.map { it.second }.average().toFloat() to month.atDay(1) }
                .unzip()
        } else {
            daily.map { it.second } to daily.map { it.first }
        }
        val labelFormat = DateTimeFormatter.ofPattern(if (p == Period.Year) "MMM" else "d MMM")
        val labels = when {
            trendDates.size >= 3 -> listOf(trendDates.first(), trendDates[trendDates.size / 2], trendDates.last())
            else -> trendDates
        }.map { it.format(labelFormat) }

        val streaks = hs.map { it to HabitStats.currentStreak(it, done[it.id].orEmpty(), today) }
        val best = streaks.maxByOrNull { it.second }

        val week = (6 downTo 0).map { today.minusDays(it.toLong()) }

        InsightsState(
            period = p,
            keyInsight = InsightEngine.keyInsight(hs, cs, ms, from, today),
            trend = trend,
            trendLabels = labels,
            rates = InsightEngine.completionRates(hs, cs, from, today),
            averageMood = InsightEngine.averageMood(ms, from, today)?.let { Mood.fromRating(it.roundToInt()) },
            bestStreak = best?.second ?: 0,
            bestStreakHabit = best?.first?.name?.takeIf { (best.second) > 0 },
            weekActivity = week.map { day -> cs.count { it.date == day }.toFloat() },
            weekLabels = week.map { Fmt.weekdayInitial(it) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsState())
}
