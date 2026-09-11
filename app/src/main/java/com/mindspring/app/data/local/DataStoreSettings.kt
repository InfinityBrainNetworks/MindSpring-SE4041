package com.mindspring.app.data.local

import android.content.Context
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mindspring.app.data.model.AlertSounds
import com.mindspring.app.data.model.ThemeMode
import com.mindspring.app.data.repository.ReminderSettings
import com.mindspring.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalTime

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Small key-value settings, plus which account is signed in on this device. */
class DataStoreSettings(context: Context) : SettingsRepository {
    private val store = context.applicationContext.settingsStore

    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val ambient = booleanPreferencesKey("ambient_motion")
        val onboarding = booleanPreferencesKey("onboarding_done")
        val checkInOn = booleanPreferencesKey("checkin_enabled")
        val checkInAt = intPreferencesKey("checkin_minute")
        val digestOn = booleanPreferencesKey("digest_enabled")
        val digestAt = intPreferencesKey("digest_minute")
        val session = longPreferencesKey("session_user_id")
        val reminderTone = stringPreferencesKey("reminder_tone")
        val alarmTone = stringPreferencesKey("alarm_tone")
        val vibrate = booleanPreferencesKey("alert_vibrate")
        val snooze = intPreferencesKey("snooze_minutes")
    }

    private fun <T> read(block: (Preferences) -> T): Flow<T> = store.data.map(block).distinctUntilChanged()

    override val themeMode = read { p -> ThemeMode.entries.firstOrNull { it.name == p[Keys.theme] } ?: ThemeMode.System }
    override val ambientMotion = read { it[Keys.ambient] ?: true }
    override val onboardingDone = read { it[Keys.onboarding] ?: false }
    override val checkInReminder = read { ReminderSettings(it[Keys.checkInOn] ?: true, minuteToTime(it[Keys.checkInAt] ?: (20 * 60))) }
    override val taskDigest = read { ReminderSettings(it[Keys.digestOn] ?: true, minuteToTime(it[Keys.digestAt] ?: (8 * 60))) }
    override val sessionUserId: Flow<Long?> = read { it[Keys.session] }

    // A tone that was never picked is the phone's default; an empty string records "Silent".
    override val alertSounds = read {
        AlertSounds(
            reminderTone = (it[Keys.reminderTone] ?: Settings.System.DEFAULT_NOTIFICATION_URI.toString()).ifEmpty { null },
            alarmTone = (it[Keys.alarmTone] ?: Settings.System.DEFAULT_ALARM_ALERT_URI.toString()).ifEmpty { null },
            vibrate = it[Keys.vibrate] ?: true,
            snoozeMinutes = it[Keys.snooze] ?: 10,
        )
    }

    override suspend fun setAlertSounds(sounds: AlertSounds) {
        store.edit {
            it[Keys.reminderTone] = sounds.reminderTone.orEmpty()
            it[Keys.alarmTone] = sounds.alarmTone.orEmpty()
            it[Keys.vibrate] = sounds.vibrate
            it[Keys.snooze] = sounds.snoozeMinutes
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) { store.edit { it[Keys.theme] = mode.name } }
    override suspend fun setAmbientMotion(enabled: Boolean) { store.edit { it[Keys.ambient] = enabled } }
    override suspend fun setOnboardingDone() { store.edit { it[Keys.onboarding] = true } }

    override suspend fun setCheckInReminder(settings: ReminderSettings) {
        store.edit {
            it[Keys.checkInOn] = settings.enabled
            it[Keys.checkInAt] = settings.time.hour * 60 + settings.time.minute
        }
    }

    override suspend fun setTaskDigest(settings: ReminderSettings) {
        store.edit {
            it[Keys.digestOn] = settings.enabled
            it[Keys.digestAt] = settings.time.hour * 60 + settings.time.minute
        }
    }

    override suspend fun setSessionUserId(id: Long?) {
        store.edit { if (id == null) it.remove(Keys.session) else it[Keys.session] = id }
    }

    private fun minuteToTime(m: Int) = LocalTime.of((m / 60) % 24, m % 60)
}
