package com.mindspring.app.data.memory

import com.mindspring.app.data.model.GratitudeEntry
import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.model.HabitCompletion
import com.mindspring.app.data.model.MoodEntry
import com.mindspring.app.data.model.User
import com.mindspring.app.data.repository.AuthRepository
import com.mindspring.app.data.repository.AuthResult
import com.mindspring.app.data.repository.GratitudeRepository
import com.mindspring.app.data.repository.HabitRepository
import com.mindspring.app.data.repository.MoodRepository
import com.mindspring.app.data.repository.ReminderSettings
import com.mindspring.app.data.repository.SettingsRepository
import com.mindspring.app.domain.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// In-memory stand-ins used during the UI phase. Data lives only while the app process runs.

class InMemoryAuthRepository : AuthRepository {
    private data class Account(val user: User, val passwordHash: String)

    private val accounts = mutableMapOf<String, Account>()
    private val current = MutableStateFlow<User?>(null)
    private var nextId = 1L

    override val currentUser: Flow<User?> = current

    override suspend fun register(name: String, email: String, password: String): AuthResult =
        withContext(Dispatchers.Default) {
            val key = email.trim().lowercase()
            if (key in accounts || key == DEMO_EMAIL) return@withContext AuthResult.Error("An account with this email already exists.")
            val user = User(nextId++, name.trim(), key, LocalDate.now())
            accounts[key] = Account(user, PasswordHasher.hash(password))
            current.value = user
            AuthResult.Success(user)
        }

    override suspend fun login(email: String, password: String): AuthResult =
        withContext(Dispatchers.Default) {
            val key = email.trim().lowercase()
            if (key == DEMO_EMAIL && key !in accounts) {
                accounts[key] = Account(
                    User(nextId++, "Asan", DEMO_EMAIL, LocalDate.now().minusMonths(1)),
                    PasswordHasher.hash(DEMO_PASSWORD),
                )
            }
            val account = accounts[key]
            if (account == null || !PasswordHasher.verify(password, account.passwordHash)) {
                AuthResult.Error("Incorrect email or password.")
            } else {
                current.value = account.user
                AuthResult.Success(account.user)
            }
        }

    override suspend fun logout() {
        current.value = null
    }

    override suspend fun updateName(name: String) {
        val user = current.value ?: return
        val updated = user.copy(name = name.trim())
        accounts[user.email] = accounts.getValue(user.email).copy(user = updated)
        current.value = updated
    }

    override suspend fun deleteAccountData() {
        current.value?.let { accounts.remove(it.email) }
        current.value = null
    }

    companion object {
        const val DEMO_EMAIL = "asan@mindspring.app"
        const val DEMO_PASSWORD = "mindspring"
    }
}

class InMemoryHabitRepository(today: LocalDate = LocalDate.now()) : HabitRepository {
    private val habitState = MutableStateFlow(SampleData.habits(today))
    private val completionState = MutableStateFlow(SampleData.completions(today))
    private var nextId = (habitState.value.maxOfOrNull { it.id } ?: 0) + 1

    override val habits: Flow<List<Habit>> = habitState
    override val completions: Flow<List<HabitCompletion>> = completionState

    /** Exposed so the mood sample data can correlate with exercise days. */
    internal val completionSnapshot: List<HabitCompletion> get() = completionState.value

    override fun habit(id: Long): Flow<Habit?> = habitState.map { list -> list.find { it.id == id } }

    override suspend fun upsert(habit: Habit): Long {
        if (habit.id == 0L) {
            val created = habit.copy(id = nextId++)
            habitState.update { it + created }
            return created.id
        }
        habitState.update { list -> list.map { if (it.id == habit.id) habit else it } }
        return habit.id
    }

    override suspend fun delete(id: Long) {
        habitState.update { list -> list.filterNot { it.id == id } }
        completionState.update { list -> list.filterNot { it.habitId == id } }
    }

    override suspend fun setCompleted(habitId: Long, date: LocalDate, completed: Boolean) {
        val completion = HabitCompletion(habitId, date)
        completionState.update { list ->
            if (completed) (list + completion).distinct() else list - completion
        }
    }

    override suspend fun clear() {
        habitState.value = emptyList()
        completionState.value = emptyList()
    }
}

class InMemoryMoodRepository(seed: List<MoodEntry>) : MoodRepository {
    private val state = MutableStateFlow(seed)
    private var nextId = (seed.maxOfOrNull { it.id } ?: 0) + 1

    override val entries: Flow<List<MoodEntry>> = state.map { list -> list.sortedByDescending { it.loggedAt } }

    override fun entry(id: Long): Flow<MoodEntry?> = state.map { list -> list.find { it.id == id } }

    override suspend fun upsert(entry: MoodEntry): Long {
        if (entry.id == 0L) {
            val created = entry.copy(id = nextId++)
            state.update { it + created }
            return created.id
        }
        // Replace in place, or re-insert with the same id (used by "Undo" after a delete).
        state.update { list -> list.filterNot { it.id == entry.id } + entry }
        return entry.id
    }

    override suspend fun delete(id: Long) {
        state.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun clear() {
        state.value = emptyList()
    }
}

class InMemoryGratitudeRepository(today: LocalDate = LocalDate.now()) : GratitudeRepository {
    private val state = MutableStateFlow(SampleData.gratitude(today))
    private var nextId = (state.value.maxOfOrNull { it.id } ?: 0) + 1

    override val entries: Flow<List<GratitudeEntry>> = state.map { list -> list.sortedByDescending { it.createdAt } }

    override suspend fun add(text: String) {
        state.update { it + GratitudeEntry(nextId++, text.trim(), LocalDateTime.now()) }
    }

    override suspend fun delete(id: Long) {
        state.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun clear() {
        state.value = emptyList()
    }
}

class InMemorySettingsRepository : SettingsRepository {
    private val dark = MutableStateFlow<Boolean?>(null)
    private val onboarding = MutableStateFlow(false)
    private val reminder = MutableStateFlow(ReminderSettings(enabled = true, time = LocalTime.of(20, 0)))

    override val darkMode: StateFlow<Boolean?> = dark
    override val onboardingDone: StateFlow<Boolean> = onboarding
    override val checkInReminder: StateFlow<ReminderSettings> = reminder

    override suspend fun setDarkMode(enabled: Boolean?) {
        dark.value = enabled
    }

    override suspend fun setOnboardingDone() {
        onboarding.value = true
    }

    override suspend fun setCheckInReminder(settings: ReminderSettings) {
        reminder.value = settings
    }
}
