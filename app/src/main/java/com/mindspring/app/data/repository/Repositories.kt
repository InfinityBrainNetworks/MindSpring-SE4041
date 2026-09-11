package com.mindspring.app.data.repository

import com.mindspring.app.data.model.GratitudeEntry
import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.model.HabitCompletion
import com.mindspring.app.data.model.MoodEntry
import com.mindspring.app.data.model.User
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

// Screens depend only on these interfaces. The UI phase uses in-memory implementations;
// the database phase swaps in Room-backed ones without touching any screen.

sealed interface AuthResult {
    data class Success(val user: User) : AuthResult
    data class Error(val message: String) : AuthResult
}

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun register(name: String, email: String, password: String): AuthResult
    suspend fun login(email: String, password: String): AuthResult
    suspend fun logout()
    suspend fun updateName(name: String)
    suspend fun deleteAccountData()
}

interface HabitRepository {
    val habits: Flow<List<Habit>>
    val completions: Flow<List<HabitCompletion>>
    fun habit(id: Long): Flow<Habit?>
    suspend fun upsert(habit: Habit): Long
    suspend fun delete(id: Long)
    suspend fun setCompleted(habitId: Long, date: LocalDate, completed: Boolean)
    suspend fun clear()
}

interface MoodRepository {
    val entries: Flow<List<MoodEntry>>
    fun entry(id: Long): Flow<MoodEntry?>
    suspend fun upsert(entry: MoodEntry): Long
    suspend fun delete(id: Long)
    suspend fun clear()
}

interface GratitudeRepository {
    val entries: Flow<List<GratitudeEntry>>
    suspend fun add(text: String)
    suspend fun delete(id: Long)
    suspend fun clear()
}

data class ReminderSettings(val enabled: Boolean, val time: LocalTime)

interface SettingsRepository {
    /** null means "follow the system setting". */
    val darkMode: Flow<Boolean?>
    val onboardingDone: Flow<Boolean>
    val checkInReminder: Flow<ReminderSettings>
    suspend fun setDarkMode(enabled: Boolean?)
    suspend fun setOnboardingDone()
    suspend fun setCheckInReminder(settings: ReminderSettings)
}
