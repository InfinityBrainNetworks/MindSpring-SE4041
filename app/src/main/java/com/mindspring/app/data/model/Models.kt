package com.mindspring.app.data.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val memberSince: LocalDate,
)

enum class HabitCategory(val label: String) {
    Health("Health"),
    Sleep("Sleep"),
    Study("Study"),
    Fitness("Fitness"),
    Mindfulness("Mindfulness"),
    Social("Social"),
}

/** Icon keys are stored, not drawables, so the data layer stays free of UI types. */
enum class HabitIcon { Water, Run, Book, Meditate, Sleep, Spa, Heart, Flower, Food, School }

data class Habit(
    val id: Long = 0,
    val name: String,
    val category: HabitCategory,
    val icon: HabitIcon,
    val days: Set<DayOfWeek>,
    val reminderEnabled: Boolean,
    val reminderTime: LocalTime,
    val createdAt: LocalDate,
)

data class HabitCompletion(val habitId: Long, val date: LocalDate)

enum class Mood(val rating: Int, val label: String) {
    Awful(1, "Awful"),
    Bad(2, "Bad"),
    Okay(3, "Okay"),
    Good(4, "Good"),
    Great(5, "Great");

    companion object {
        fun fromRating(rating: Int): Mood = entries.first { it.rating == rating.coerceIn(1, 5) }
    }
}

data class MoodEntry(
    val id: Long = 0,
    val mood: Mood,
    val feelings: List<String>,
    val note: String,
    val loggedAt: LocalDateTime,
)

data class GratitudeEntry(
    val id: Long = 0,
    val text: String,
    val createdAt: LocalDateTime,
)

/** Feeling tags offered on the mood check-in (design report, section 4.6). */
val FeelingTags = listOf("Calm", "Anxious", "Tired", "Motivated", "Stressed", "Grateful", "Lonely", "Focused")

enum class Period(val label: String, val days: Long) {
    Week("Week", 7),
    Month("Month", 30),
    Year("Year", 365),
}
