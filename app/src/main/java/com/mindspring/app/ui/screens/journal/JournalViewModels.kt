package com.mindspring.app.ui.screens.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindspring.app.AppContainer
import com.mindspring.app.data.model.JournalEntry
import com.mindspring.app.data.model.Priority
import com.mindspring.app.data.model.Task
import com.mindspring.app.data.model.countWords
import com.mindspring.app.domain.Analytics
import com.mindspring.app.domain.JournalMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class JournalDay(val date: LocalDate, val entry: JournalEntry?)

data class JournalListState(
    val loaded: Boolean = false,
    val days: List<JournalDay> = emptyList(),
    val older: List<JournalEntry> = emptyList(),
    val month: JournalMonth? = null,
    /** Consecutive days written, counting back from today (or yesterday, if today is still blank). */
    val streak: Int = 0,
)

class JournalListViewModel(app: AppContainer) : ViewModel() {
    val state: StateFlow<JournalListState> = app.journal.entries.map { entries ->
        val today = LocalDate.now()
        val byDate = entries.associateBy { it.date }
        val recent = (0L until RECENT_DAYS).map { today.minusDays(it) }
        var streak = 0
        var day = if (byDate[today]?.words?.let { it > 0 } == true) today else today.minusDays(1)
        while (byDate[day]?.words?.let { it > 0 } == true) {
            streak++
            day = day.minusDays(1)
        }
        JournalListState(
            loaded = true,
            days = recent.map { JournalDay(it, byDate[it]) },
            older = entries.filter { it.date.isBefore(recent.last()) },
            month = Analytics.journalMonth(entries, YearMonth.from(today), today),
            streak = streak,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalListState())

    companion object {
        const val RECENT_DAYS = 14L
    }
}

data class JournalEditorState(
    val date: LocalDate,
    val rating: Int? = null,
    val energy: Int? = null,
    val highlight: String = "",
    val monologue: String = "",
    val tomorrowTop: String = "",
    val exists: Boolean = false,
    val saved: Boolean = false,
    val taskAdded: Boolean = false,
) {
    val words: Int get() = countWords(monologue)
    val isEmpty: Boolean get() = rating == null && energy == null && highlight.isBlank() && monologue.isBlank() && tomorrowTop.isBlank()
}

class JournalEditorViewModel(private val app: AppContainer, date: LocalDate) : ViewModel() {
    private val _state = MutableStateFlow(JournalEditorState(date))
    val state: StateFlow<JournalEditorState> = _state

    init {
        viewModelScope.launch {
            val existing = app.journal.entry(date).first()
            if (existing != null) {
                _state.value = JournalEditorState(date, existing.rating, existing.energy, existing.highlight, existing.monologue, existing.tomorrowTop, exists = true)
            } else if (date == LocalDate.now()) {
                // Start the day's rating from today's mood check-in, if there was one.
                val mood = app.moods.entries.first().filter { it.loggedAt.toLocalDate() == date }.map { it.mood.rating }
                if (mood.isNotEmpty()) _state.update { it.copy(rating = Math.round(mood.average()).toInt()) }
            }
        }
    }

    fun onRating(v: Int) = _state.update { it.copy(rating = if (it.rating == v) null else v) }
    fun onEnergy(v: Int) = _state.update { it.copy(energy = if (it.energy == v) null else v) }
    fun onHighlight(v: String) = _state.update { it.copy(highlight = v.take(120)) }
    fun onMonologue(v: String) = _state.update { it.copy(monologue = v.take(4000)) }
    fun onTomorrow(v: String) = _state.update { it.copy(tomorrowTop = v.take(120), taskAdded = false) }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            if (s.isEmpty) app.journal.delete(s.date)
            else app.journal.upsert(JournalEntry(s.date, s.rating, s.energy, s.highlight, s.monologue, s.tomorrowTop))
            _state.update { it.copy(saved = true) }
        }
    }

    /** Turns "Tomorrow's #1" into a must-do task due the next day. */
    fun addTomorrowAsTask() {
        val s = _state.value
        if (s.tomorrowTop.isBlank() || s.taskAdded) return
        viewModelScope.launch {
            val due = s.date.plusDays(1)
            app.tasks.upsert(Task(title = s.tomorrowTop.trim(), start = due, due = due, priority = Priority.A, createdAt = LocalDate.now()))
            _state.update { it.copy(taskAdded = true) }
        }
    }
}
