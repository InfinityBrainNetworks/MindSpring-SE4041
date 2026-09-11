package com.mindspring.app.data.repository

import com.mindspring.app.data.model.AlertSounds
import com.mindspring.app.data.model.GratitudeEntry
import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.model.HabitMark
import com.mindspring.app.data.model.JournalEntry
import com.mindspring.app.data.model.LifeArea
import com.mindspring.app.data.model.MarkState
import com.mindspring.app.data.model.MoodEntry
import com.mindspring.app.data.model.Project
import com.mindspring.app.data.model.Task
import com.mindspring.app.data.model.TaskStatus
import com.mindspring.app.data.model.ThemeMode
import com.mindspring.app.data.model.User
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

// Screens depend only on these interfaces. Every repository is scoped to the signed-in user:
// flows emit that user's rows (or nothing when signed out) and writes land on their account.

sealed interface AuthResult {
    data class Success(val user: User) : AuthResult
    data class Error(val message: String) : AuthResult
}

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun register(name: String, email: String, password: String): AuthResult
    suspend fun login(email: String, password: String): AuthResult
    /** Signs in to the sample account, creating it with six weeks of history the first time. */
    suspend fun loginDemo(): AuthResult
    suspend fun logout()
    suspend fun updateName(name: String)
}

interface AreaRepository {
    val areas: Flow<List<LifeArea>>
    suspend fun upsert(area: LifeArea): Long
    /** Habits, tasks and projects in the area are kept, just no longer filed under it. */
    suspend fun delete(id: Long)
}

interface HabitRepository {
    val habits: Flow<List<Habit>>
    val marks: Flow<List<HabitMark>>
    fun habit(id: Long): Flow<Habit?>
    suspend fun upsert(habit: Habit): Long
    suspend fun delete(id: Long)
    /** Records done or skipped for a day; null clears the day back to blank. */
    suspend fun setMark(habitId: Long, date: LocalDate, state: MarkState?)
}

interface TaskRepository {
    val tasks: Flow<List<Task>>
    val projects: Flow<List<Project>>
    fun task(id: Long): Flow<Task?>
    fun project(id: Long): Flow<Project?>
    suspend fun upsert(task: Task): Long
    /**
     * Changes status, keeping Done-on in step: finishing stamps [today], reopening clears it.
     * Finishing a repeating task also creates its next occurrence.
     */
    suspend fun setStatus(id: Long, status: TaskStatus, today: LocalDate = LocalDate.now())
    suspend fun delete(id: Long)
    suspend fun upsertProject(project: Project): Long
    /** The project's tasks are kept as standalone tasks. */
    suspend fun deleteProject(id: Long)
}

interface MoodRepository {
    val entries: Flow<List<MoodEntry>>
    fun entry(id: Long): Flow<MoodEntry?>
    suspend fun upsert(entry: MoodEntry): Long
    suspend fun delete(id: Long)
}

interface GratitudeRepository {
    val entries: Flow<List<GratitudeEntry>>
    suspend fun add(text: String)
    suspend fun delete(id: Long)
}

interface JournalRepository {
    val entries: Flow<List<JournalEntry>>
    fun entry(date: LocalDate): Flow<JournalEntry?>
    suspend fun upsert(entry: JournalEntry)
    suspend fun delete(date: LocalDate)
}

data class ReminderSettings(val enabled: Boolean, val time: LocalTime)

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    /** The slowly drifting background gradient; off also calms other decorative motion. */
    val ambientMotion: Flow<Boolean>
    val onboardingDone: Flow<Boolean>
    /** Evening nudge to check in and write the day's reflection. */
    val checkInReminder: Flow<ReminderSettings>
    /** Morning summary of what is due and overdue. */
    val taskDigest: Flow<ReminderSettings>
    val sessionUserId: Flow<Long?>
    /** Tones, vibration and snooze length for reminders and task alarms. */
    val alertSounds: Flow<AlertSounds>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAlertSounds(sounds: AlertSounds)
    suspend fun setAmbientMotion(enabled: Boolean)
    suspend fun setOnboardingDone()
    suspend fun setCheckInReminder(settings: ReminderSettings)
    suspend fun setTaskDigest(settings: ReminderSettings)
    suspend fun setSessionUserId(id: Long?)
}

/** Wipes the signed-in user's own data (not the account) - the "Clear all data" setting. */
interface DataReset {
    suspend fun clearUserData()
}
