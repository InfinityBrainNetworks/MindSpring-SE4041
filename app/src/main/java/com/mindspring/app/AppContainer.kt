package com.mindspring.app

import android.content.Context
import com.mindspring.app.data.local.Backup
import com.mindspring.app.data.local.DataStoreSettings
import com.mindspring.app.data.local.MindSpringDatabase
import com.mindspring.app.data.local.RoomAreaRepository
import com.mindspring.app.data.local.RoomAuthRepository
import com.mindspring.app.data.local.RoomDataReset
import com.mindspring.app.data.local.RoomGratitudeRepository
import com.mindspring.app.data.local.RoomHabitRepository
import com.mindspring.app.data.local.RoomJournalRepository
import com.mindspring.app.data.local.RoomMoodRepository
import com.mindspring.app.data.local.RoomTaskRepository
import com.mindspring.app.data.local.Session
import com.mindspring.app.data.repository.AreaRepository
import com.mindspring.app.data.repository.AuthRepository
import com.mindspring.app.data.repository.DataReset
import com.mindspring.app.data.repository.GratitudeRepository
import com.mindspring.app.data.repository.HabitRepository
import com.mindspring.app.data.repository.JournalRepository
import com.mindspring.app.data.repository.MoodRepository
import com.mindspring.app.data.repository.SettingsRepository
import com.mindspring.app.data.repository.TaskRepository

/**
 * Manual dependency injection: the one place that decides which implementations run. Everything
 * is stored on the device - a Room (SQLite) database for the user's data and DataStore for
 * settings - and survives restarts until the app is uninstalled.
 */
class AppContainer(context: Context, val db: MindSpringDatabase = MindSpringDatabase.build(context)) {
    val settings: SettingsRepository = DataStoreSettings(context)
    private val session = Session(settings)

    val auth: AuthRepository = RoomAuthRepository(db, settings, session)
    val areas: AreaRepository = RoomAreaRepository(db, session)
    val habits: HabitRepository = RoomHabitRepository(db, session)
    val tasks: TaskRepository = RoomTaskRepository(db, session)
    val moods: MoodRepository = RoomMoodRepository(db, session)
    val gratitude: GratitudeRepository = RoomGratitudeRepository(db, session)
    val journal: JournalRepository = RoomJournalRepository(db, session)
    val reset: DataReset = RoomDataReset(db, session)
    val backup = Backup(db, session)
}
