package com.mindspring.app.ui.screens.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.model.HabitCategory
import com.mindspring.app.data.model.HabitIcon
import com.mindspring.app.data.repository.AuthRepository
import com.mindspring.app.data.repository.HabitRepository
import com.mindspring.app.domain.HabitStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

// ---------- Habits list ----------

data class HabitCardState(
    val habit: Habit,
    val doneToday: Boolean,
    val scheduledToday: Boolean,
    val weekDone: Int,
    val weekTarget: Int,
) {
    val weekMet: Boolean get() = weekDone >= weekTarget
}

class HabitsViewModel(private val repo: HabitRepository) : ViewModel() {
    val cards: StateFlow<List<HabitCardState>?> = combine(repo.habits, repo.completions) { habits, completions ->
        val today = LocalDate.now()
        val done = completions.groupBy({ it.habitId }, { it.date }).mapValues { it.value.toSet() }
        habits.map { habit ->
            val dates = done[habit.id].orEmpty()
            HabitCardState(
                habit = habit,
                doneToday = today in dates,
                scheduledToday = HabitStats.isScheduled(habit, today),
                weekDone = HabitStats.completedThisWeek(dates, today),
                weekTarget = habit.days.size,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setDone(habitId: Long, done: Boolean) {
        viewModelScope.launch { repo.setCompleted(habitId, LocalDate.now(), done) }
    }
}

// ---------- Add / edit ----------

data class HabitEditorState(
    val id: Long = 0,
    val name: String = "",
    val category: HabitCategory = HabitCategory.Health,
    val icon: HabitIcon = HabitIcon.Water,
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val reminderEnabled: Boolean = true,
    val reminderTime: LocalTime = LocalTime.of(8, 0),
    val createdAt: LocalDate = LocalDate.now(),
    val saved: Boolean = false,
) {
    val isEditing: Boolean get() = id != 0L
    val canSave: Boolean get() = name.isNotBlank() && days.isNotEmpty()
}

class HabitEditorViewModel(private val repo: HabitRepository, habitId: Long?) : ViewModel() {
    private val _state = MutableStateFlow(HabitEditorState())
    val state: StateFlow<HabitEditorState> = _state

    init {
        if (habitId != null) {
            viewModelScope.launch {
                repo.habit(habitId).first()?.let { h ->
                    _state.value = HabitEditorState(h.id, h.name, h.category, h.icon, h.days, h.reminderEnabled, h.reminderTime, h.createdAt)
                }
            }
        }
    }

    fun onName(v: String) = _state.update { it.copy(name = v.take(40)) }
    fun onCategory(v: HabitCategory) = _state.update { it.copy(category = v) }
    fun onIcon(v: HabitIcon) = _state.update { it.copy(icon = v) }
    fun onToggleDay(d: DayOfWeek) = _state.update { s -> s.copy(days = if (d in s.days) s.days - d else s.days + d) }
    fun onReminderEnabled(v: Boolean) = _state.update { it.copy(reminderEnabled = v) }
    fun onReminderTime(v: LocalTime) = _state.update { it.copy(reminderTime = v) }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            repo.upsert(Habit(s.id, s.name.trim(), s.category, s.icon, s.days, s.reminderEnabled, s.reminderTime, s.createdAt))
            _state.update { it.copy(saved = true) }
        }
    }
}

// ---------- Detail ----------

enum class DayCell { Done, Missed, Rest, Future, Before }

data class HabitDetailState(
    val habit: Habit,
    val streak: Int,
    val bestStreak: Int,
    val monthRate: Float,
    val totalDays: Int,
    val doneToday: Boolean,
    /** Five Monday-first weeks ending with the current week. */
    val grid: List<List<Pair<LocalDate, DayCell>>>,
    val streakDays: Set<LocalDate>,
)

class HabitDetailViewModel(private val repo: HabitRepository, private val habitId: Long) : ViewModel() {
    val state: StateFlow<HabitDetailState?> = combine(repo.habit(habitId), repo.completions) { habit, completions ->
        habit ?: return@combine null
        val today = LocalDate.now()
        val done = completions.filter { it.habitId == habitId }.map { it.date }.toSet()
        val streakDays = HabitStats.streakDates(habit, done, today)
        HabitDetailState(
            habit = habit,
            streak = streakDays.size,
            bestStreak = HabitStats.bestStreak(habit, done, today),
            monthRate = HabitStats.completionRate(habit, done, today.withDayOfMonth(1), today),
            totalDays = done.size,
            doneToday = today in done,
            grid = buildGrid(habit, done, today),
            streakDays = streakDays,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setDoneToday(done: Boolean) {
        viewModelScope.launch { repo.setCompleted(habitId, LocalDate.now(), done) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repo.delete(habitId)
            onDeleted()
        }
    }

    private fun buildGrid(habit: Habit, done: Set<LocalDate>, today: LocalDate): List<List<Pair<LocalDate, DayCell>>> {
        val thisMonday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val start = thisMonday.minusWeeks(4)
        return (0 until 5).map { week ->
            (0 until 7).map { d ->
                val day = start.plusWeeks(week.toLong()).plusDays(d.toLong())
                day to when {
                    day.isAfter(today) -> DayCell.Future
                    day in done -> DayCell.Done
                    day.isBefore(habit.createdAt) -> DayCell.Before
                    !HabitStats.isScheduled(habit, day) -> DayCell.Rest
                    day == today -> DayCell.Future
                    else -> DayCell.Missed
                }
            }
        }
    }
}

// ---------- Celebration ----------

data class CompletedState(val userFirstName: String, val habitName: String, val streak: Int)

class HabitCompletedViewModel(auth: AuthRepository, repo: HabitRepository, habitId: Long) : ViewModel() {
    val state: StateFlow<CompletedState?> = combine(auth.currentUser, repo.habit(habitId), repo.completions.map { list ->
        list.filter { it.habitId == habitId }.map { it.date }.toSet()
    }) { user, habit, done ->
        habit ?: return@combine null
        CompletedState(
            userFirstName = user?.name?.substringBefore(' ').orEmpty(),
            habitName = habit.name,
            streak = HabitStats.currentStreak(habit, done, LocalDate.now()),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
