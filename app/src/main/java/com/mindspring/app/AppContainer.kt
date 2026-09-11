package com.mindspring.app

import com.mindspring.app.data.memory.InMemoryAuthRepository
import com.mindspring.app.data.memory.InMemoryGratitudeRepository
import com.mindspring.app.data.memory.InMemoryHabitRepository
import com.mindspring.app.data.memory.InMemoryMoodRepository
import com.mindspring.app.data.memory.InMemorySettingsRepository
import com.mindspring.app.data.memory.SampleData
import com.mindspring.app.data.repository.AuthRepository
import com.mindspring.app.data.repository.GratitudeRepository
import com.mindspring.app.data.repository.HabitRepository
import com.mindspring.app.data.repository.MoodRepository
import com.mindspring.app.data.repository.SettingsRepository
import java.time.LocalDate

/** Manual dependency injection: one place that decides which repository implementations run. */
class AppContainer {
    private val habitStore = InMemoryHabitRepository()

    val auth: AuthRepository = InMemoryAuthRepository()
    val settings: SettingsRepository = InMemorySettingsRepository()
    val habits: HabitRepository = habitStore
    val moods: MoodRepository = InMemoryMoodRepository(SampleData.moods(LocalDate.now(), habitStore.completionSnapshot))
    val gratitude: GratitudeRepository = InMemoryGratitudeRepository()
}
