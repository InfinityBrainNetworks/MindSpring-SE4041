package com.mindspring.app.ui.screens.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindspring.app.data.model.Mood
import com.mindspring.app.data.model.MoodEntry
import com.mindspring.app.data.model.Period
import com.mindspring.app.data.repository.MoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

data class CheckInState(
    val entryId: Long = 0,
    val mood: Mood? = null,
    val feelings: Set<String> = emptySet(),
    val note: String = "",
    val loggedAt: LocalDateTime? = null,
    val saved: Boolean = false,
) {
    val isEditing: Boolean get() = entryId != 0L
}

class MoodCheckInViewModel(
    private val repo: MoodRepository,
    entryId: Long?,
    initialRating: Int?,
) : ViewModel() {
    private val _state = MutableStateFlow(CheckInState(mood = initialRating?.let(Mood::fromRating)))
    val state: StateFlow<CheckInState> = _state

    init {
        if (entryId != null) {
            viewModelScope.launch {
                repo.entry(entryId).first()?.let { e ->
                    _state.value = CheckInState(e.id, e.mood, e.feelings.toSet(), e.note, e.loggedAt)
                }
            }
        }
    }

    fun onMood(m: Mood) = _state.update { it.copy(mood = m) }
    fun onToggleFeeling(f: String) = _state.update { s -> s.copy(feelings = if (f in s.feelings) s.feelings - f else s.feelings + f) }
    fun onNote(v: String) = _state.update { it.copy(note = v.take(500)) }

    fun save() {
        val s = _state.value
        val mood = s.mood ?: return
        viewModelScope.launch {
            repo.upsert(MoodEntry(s.entryId, mood, s.feelings.toList(), s.note.trim(), s.loggedAt ?: LocalDateTime.now()))
            _state.update { it.copy(saved = true) }
        }
    }
}

class MoodHistoryViewModel(private val repo: MoodRepository) : ViewModel() {
    val period = MutableStateFlow(Period.Week)

    val entries: StateFlow<List<MoodEntry>?> = combine(repo.entries, period) { list, p ->
        val from = LocalDate.now().minusDays(p.days - 1)
        list.filter { !it.loggedAt.toLocalDate().isBefore(from) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(entry: MoodEntry) {
        viewModelScope.launch { repo.delete(entry.id) }
    }

    fun restore(entry: MoodEntry) {
        viewModelScope.launch { repo.upsert(entry) }
    }
}
