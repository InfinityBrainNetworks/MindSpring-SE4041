package com.mindspring.app.ui.screens.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindspring.app.AppContainer
import com.mindspring.app.data.model.AlertStyle
import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.model.HabitFrequency
import com.mindspring.app.data.model.HabitIcon
import com.mindspring.app.data.model.LifeArea
import com.mindspring.app.data.model.MarkState
import com.mindspring.app.domain.HabitStats
import com.mindspring.app.domain.HabitUnit
import com.mindspring.app.domain.Tally
import com.mindspring.app.domain.byHabit
import com.mindspring.app.ui.components.TickState
import com.mindspring.app.ui.screens.home.TodayHabit
import com.mindspring.app.ui.screens.home.todayHabits
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

// ---------- Habits tab ----------

data class WeekCell(val date: LocalDate, val tick: TickState, val enabled: Boolean, val isToday: Boolean)

data class WeekRow(val habit: Habit, val cells: List<WeekCell>, val areaColor: Int?)

data class HabitSummary(val habit: Habit, val month: Tally, val streak: Int, val unit: HabitUnit, val areaColor: Int?)

data class AreaGroup(val area: LifeArea?, val tally: Tally, val habits: List<HabitSummary>)

data class HabitsState(
    val loaded: Boolean = false,
    val today: List<TodayHabit> = emptyList(),
    val weekStart: LocalDate = HabitStats.weekStart(LocalDate.now()),
    val week: List<WeekRow> = emptyList(),
    /** Share of day-based habits done on each day of the shown week (null: nothing was due). */
    val dayScores: List<Float?> = emptyList(),
    /** Open task deadlines on each day of the shown week. */
    val deadlines: List<Int> = emptyList(),
    val groups: List<AreaGroup> = emptyList(),
    val retired: List<HabitSummary> = emptyList(),
)

enum class HabitsView(val label: String) { Today("Today"), Week("Week"), All("All") }

class HabitsViewModel(private val app: AppContainer) : ViewModel() {
    val view = MutableStateFlow(HabitsView.Today)
    val weekOffset = MutableStateFlow(0L)

    val state: StateFlow<HabitsState> = combine(
        app.habits.habits, app.habits.marks, app.areas.areas, app.tasks.tasks, weekOffset,
    ) { habits, marks, areas, tasks, offset ->
        val today = LocalDate.now()
        val byHabit = marks.byHabit()
        val areaColors = areas.associate { it.id to it.colorIndex }
        val monday = HabitStats.weekStart(today).plusWeeks(offset)
        val days = (0L..6L).map { monday.plusDays(it) }
        val active = habits.filter { it.active }
            .sortedWith(compareBy<Habit> { it.frequency.ordinal }.thenBy { it.id })

        val week = active.filter { !it.createdAt.isAfter(days.last()) }.map { h ->
            val hm = byHabit[h.id].orEmpty()
            WeekRow(
                habit = h,
                cells = days.map { d ->
                    val allowed = !d.isBefore(h.createdAt) && !d.isAfter(today) && (!h.frequency.isDayBased || d.dayOfWeek in h.days)
                    val tick = when (hm[d]) {
                        MarkState.Done -> TickState.Done
                        MarkState.Skipped -> TickState.Skipped
                        null -> if (allowed) TickState.Empty else TickState.Off
                    }
                    WeekCell(d, tick, allowed || hm[d] != null, d == today)
                },
                areaColor = h.areaId?.let(areaColors::get),
            )
        }

        val month = YearMonth.from(today)
        fun summary(h: Habit): HabitSummary {
            val hm = byHabit[h.id].orEmpty()
            return HabitSummary(
                habit = h,
                month = HabitStats.tally(h, hm, month.atDay(1), month.atEndOfMonth(), today),
                streak = HabitStats.currentStreak(h, hm, today),
                unit = HabitStats.unit(h),
                areaColor = h.areaId?.let(areaColors::get),
            )
        }
        val areaById = areas.associateBy { it.id }
        val groups = active.map(::summary)
            .groupBy { it.habit.areaId?.let(areaById::get) }
            .map { (area, list) -> AreaGroup(area, list.fold(Tally.Zero) { acc, s -> acc + s.month }, list) }
            .sortedBy { it.area?.sortOrder ?: Int.MAX_VALUE }

        val openTasks = tasks.filter { it.status.isOpen }
        HabitsState(
            loaded = true,
            today = todayHabits(habits, byHabit, areaColors, today),
            weekStart = monday,
            week = week,
            dayScores = days.map { if (it.isAfter(today)) null else HabitStats.dayScore(active, byHabit, it) },
            deadlines = days.map { d -> openTasks.count { it.due == d } },
            groups = groups,
            retired = habits.filter { !it.active }.map(::summary),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitsState())

    fun toggleDone(item: TodayHabit) = mark(item.habit.id, LocalDate.now(), if (item.mark == MarkState.Done) null else MarkState.Done)

    fun toggleSkip(item: TodayHabit) = mark(item.habit.id, LocalDate.now(), if (item.mark == MarkState.Skipped) null else MarkState.Skipped)

    /** The grid's tap: blank → done → skipped → blank, like typing x, then -, then clearing. */
    fun cycle(habitId: Long, cell: WeekCell) {
        val next = when (cell.tick) {
            TickState.Done -> MarkState.Skipped
            TickState.Skipped -> null
            else -> MarkState.Done
        }
        mark(habitId, cell.date, next)
    }

    private fun mark(habitId: Long, date: LocalDate, state: MarkState?) {
        viewModelScope.launch { app.habits.setMark(habitId, date, state) }
    }
}

// ---------- Add / edit ----------

data class HabitEditorState(
    val id: Long = 0,
    val name: String = "",
    val areaId: Long? = null,
    val subArea: String = "",
    val icon: HabitIcon = HabitIcon.Spa,
    val frequency: HabitFrequency = HabitFrequency.Daily,
    val customDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val target: String = "",
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = LocalTime.of(8, 0),
    val active: Boolean = true,
    val createdAt: LocalDate = LocalDate.now(),
    val reminderStyle: AlertStyle = AlertStyle.Reminder,
    val saved: Boolean = false,
) {
    val isEditing: Boolean get() = id != 0L
    val canSave: Boolean get() = name.isNotBlank() && (frequency != HabitFrequency.Custom || customDays.isNotEmpty())

    fun toHabit() = Habit(
        id, name.trim(), areaId, subArea.trim(), icon, frequency, customDays, target.trim(), reminderEnabled, reminderTime, active, createdAt,
        reminderStyle,
    )
}

/**
 * Everything the habit editor can do, named apart from the ViewModel that implements it, so the
 * editor composable can be rendered in a @Preview against a no-op stand-in.
 */
interface HabitEditorActions {
    fun onName(v: String)
    fun onArea(v: Long?)
    fun onSubArea(v: String)
    fun onIcon(v: HabitIcon)
    fun onFrequency(v: HabitFrequency)
    fun onToggleDay(d: DayOfWeek)
    fun onTarget(v: String)
    fun onReminderEnabled(v: Boolean)
    fun onReminderTime(v: LocalTime)
    fun onReminderStyle(v: AlertStyle)
    fun onActive(v: Boolean)
    fun save()

    /** Does nothing; for previews, where there is no data layer to write to. */
    object None : HabitEditorActions {
        override fun onName(v: String) = Unit
        override fun onArea(v: Long?) = Unit
        override fun onSubArea(v: String) = Unit
        override fun onIcon(v: HabitIcon) = Unit
        override fun onFrequency(v: HabitFrequency) = Unit
        override fun onToggleDay(d: DayOfWeek) = Unit
        override fun onTarget(v: String) = Unit
        override fun onReminderEnabled(v: Boolean) = Unit
        override fun onReminderTime(v: LocalTime) = Unit
        override fun onReminderStyle(v: AlertStyle) = Unit
        override fun onActive(v: Boolean) = Unit
        override fun save() = Unit
    }
}

class HabitEditorViewModel(private val app: AppContainer, habitId: Long?) : ViewModel(), HabitEditorActions {
    private val _state = MutableStateFlow(HabitEditorState())
    val state: StateFlow<HabitEditorState> = _state

    val areas: StateFlow<List<LifeArea>> = app.areas.areas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Sub-areas already used on habits and tasks, most used first. */
    val subAreas: StateFlow<List<String>> = combine(app.habits.habits, app.tasks.tasks) { h, t ->
        (h.map { it.subArea } + t.map { it.subArea }).filter { it.isNotBlank() }
            .groupingBy { it.trim() }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (habitId != null) {
            viewModelScope.launch {
                app.habits.habit(habitId).first()?.let { h ->
                    _state.value = HabitEditorState(
                        h.id, h.name, h.areaId, h.subArea, h.icon, h.frequency, h.customDays, h.target,
                        h.reminderEnabled, h.reminderTime, h.active, h.createdAt, h.reminderStyle,
                    )
                }
            }
        }
    }

    override fun onName(v: String) = _state.update { it.copy(name = v.take(60)) }
    override fun onArea(v: Long?) = _state.update { it.copy(areaId = v) }
    override fun onSubArea(v: String) = _state.update { it.copy(subArea = v) }
    override fun onIcon(v: HabitIcon) = _state.update { it.copy(icon = v) }
    override fun onFrequency(v: HabitFrequency) = _state.update { it.copy(frequency = v) }
    override fun onToggleDay(d: DayOfWeek) = _state.update { s -> s.copy(customDays = if (d in s.customDays) s.customDays - d else s.customDays + d) }
    override fun onTarget(v: String) = _state.update { it.copy(target = v.take(24)) }
    override fun onReminderEnabled(v: Boolean) = _state.update { it.copy(reminderEnabled = v) }
    override fun onReminderTime(v: LocalTime) = _state.update { it.copy(reminderTime = v) }
    override fun onReminderStyle(v: AlertStyle) = _state.update { it.copy(reminderStyle = v) }
    override fun onActive(v: Boolean) = _state.update { it.copy(active = v) }

    override fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            app.habits.upsert(s.toHabit())
            _state.update { it.copy(saved = true) }
        }
    }
}

// ---------- Detail ----------

enum class DayCell { Done, Skipped, Missed, Rest, Open, Future, Before }

data class HabitDetailState(
    val habit: Habit,
    val area: LifeArea?,
    val unit: HabitUnit,
    val streak: Int,
    val bestStreak: Int,
    val month: Tally,
    val totalDone: Int,
    val todayMark: MarkState?,
    val belongsToday: Boolean,
    /** Five Monday-first weeks ending with the current week. */
    val grid: List<List<Pair<LocalDate, DayCell>>>,
    val streakDays: Set<LocalDate>,
)

class HabitDetailViewModel(private val app: AppContainer, private val habitId: Long) : ViewModel() {
    val state: StateFlow<HabitDetailState?> = combine(app.habits.habit(habitId), app.habits.marks, app.areas.areas) { habit, marks, areas ->
        habit ?: return@combine null
        val today = LocalDate.now()
        val hm = marks.filter { it.habitId == habitId }.associate { it.date to it.state }
        val month = YearMonth.from(today)
        HabitDetailState(
            habit = habit,
            area = areas.firstOrNull { it.id == habit.areaId },
            unit = HabitStats.unit(habit),
            streak = HabitStats.currentStreak(habit, hm, today),
            bestStreak = HabitStats.bestStreak(habit, hm, today),
            month = HabitStats.tally(habit, hm, month.atDay(1), month.atEndOfMonth(), today),
            totalDone = hm.count { it.value == MarkState.Done },
            todayMark = hm[today],
            belongsToday = HabitStats.belongsOn(habit, today),
            grid = buildGrid(habit, hm, today),
            streakDays = HabitStats.streakDates(habit, hm, today),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setToday(state: MarkState?) {
        viewModelScope.launch { app.habits.setMark(habitId, LocalDate.now(), state) }
    }

    fun setActive(active: Boolean) {
        val habit = state.value?.habit ?: return
        viewModelScope.launch { app.habits.upsert(habit.copy(active = active)) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            app.habits.delete(habitId)
            onDeleted()
        }
    }

    private fun buildGrid(habit: Habit, marks: Map<LocalDate, MarkState>, today: LocalDate): List<List<Pair<LocalDate, DayCell>>> {
        val start = HabitStats.weekStart(today).minusWeeks(4)
        return (0 until 5).map { week ->
            (0 until 7).map { d ->
                val day = start.plusWeeks(week.toLong()).plusDays(d.toLong())
                day to when {
                    day.isAfter(today) -> DayCell.Future
                    marks[day] == MarkState.Done -> DayCell.Done
                    marks[day] == MarkState.Skipped -> DayCell.Skipped
                    day.isBefore(habit.createdAt) -> DayCell.Before
                    !habit.frequency.isDayBased -> DayCell.Open
                    day.dayOfWeek !in habit.days -> DayCell.Rest
                    day == today -> DayCell.Future
                    else -> DayCell.Missed
                }
            }
        }
    }
}

// ---------- Celebration ----------

data class CompletedState(val userFirstName: String, val habitName: String, val streak: Int, val unit: HabitUnit)

class HabitCompletedViewModel(app: AppContainer, habitId: Long) : ViewModel() {
    val state: StateFlow<CompletedState?> = combine(app.auth.currentUser, app.habits.habit(habitId), app.habits.marks) { user, habit, marks ->
        habit ?: return@combine null
        val hm = marks.filter { it.habitId == habitId }.associate { it.date to it.state }
        CompletedState(
            userFirstName = user?.name?.substringBefore(' ').orEmpty(),
            habitName = habit.name,
            streak = HabitStats.currentStreak(habit, hm, LocalDate.now()),
            unit = HabitStats.unit(habit),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
