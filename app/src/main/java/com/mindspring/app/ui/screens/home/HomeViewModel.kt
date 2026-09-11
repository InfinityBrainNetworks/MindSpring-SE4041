package com.mindspring.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.model.MoodEntry
import com.mindspring.app.data.repository.AuthRepository
import com.mindspring.app.data.repository.GratitudeRepository
import com.mindspring.app.data.repository.HabitRepository
import com.mindspring.app.data.repository.MoodRepository
import com.mindspring.app.domain.HabitStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayHabit(val habit: Habit, val done: Boolean)
data class StreakItem(val name: String, val days: Int)

data class HomeUiState(
    val firstName: String = "",
    val todayMood: MoodEntry? = null,
    val habits: List<TodayHabit> = emptyList(),
    val streaks: List<StreakItem> = emptyList(),
) {
    val doneCount: Int get() = habits.count { it.done }
}

class HomeViewModel(
    auth: AuthRepository,
    private val habitRepo: HabitRepository,
    moods: MoodRepository,
    private val gratitude: GratitudeRepository,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        auth.currentUser, habitRepo.habits, habitRepo.completions, moods.entries,
    ) { user, habits, completions, entries ->
        val today = LocalDate.now()
        val done = completions.groupBy({ it.habitId }, { it.date }).mapValues { it.value.toSet() }
        HomeUiState(
            firstName = user?.name?.substringBefore(' ').orEmpty(),
            todayMood = entries.firstOrNull { it.loggedAt.toLocalDate() == today },
            habits = habits
                .filter { HabitStats.isScheduled(it, today) || today in done[it.id].orEmpty() }
                .map { TodayHabit(it, today in done[it.id].orEmpty()) },
            streaks = habits
                .map { StreakItem(it.name, HabitStats.currentStreak(it, done[it.id].orEmpty(), today)) }
                .filter { it.days > 0 }
                .sortedByDescending { it.days }
                .take(3),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setDone(habitId: Long, done: Boolean) {
        viewModelScope.launch { habitRepo.setCompleted(habitId, LocalDate.now(), done) }
    }

    fun saveGratitude(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { gratitude.add(text) }
    }
}
