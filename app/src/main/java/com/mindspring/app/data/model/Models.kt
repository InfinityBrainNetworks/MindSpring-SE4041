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

/**
 * A standing part of life (Health, Relationships, Finance...). Areas are a short, user-editable
 * list; sub-areas and projects hang off them and are invented freely.
 */
data class LifeArea(
    val id: Long = 0,
    val name: String,
    val colorIndex: Int,
    val sortOrder: Int = 0,
)

/** Icon keys are stored, not drawables, so the data layer stays free of UI types. */
enum class HabitIcon { Water, Run, Book, Meditate, Sleep, Spa, Heart, Flower, Food, School, Work, Code, Money, Home, People, Pray, Walk, Write }

/**
 * How often a habit is meant to happen. The first four are judged day by day on the days they
 * cover; Weekly and Monthly can be done on any day and are judged once per week or month.
 */
enum class HabitFrequency(val label: String) {
    Daily("Daily"),
    Weekdays("Weekdays"),
    Weekends("Weekends"),
    Custom("Custom"),
    Weekly("Weekly"),
    Monthly("Monthly");

    val isDayBased: Boolean get() = this != Weekly && this != Monthly
}

val Weekdays: Set<DayOfWeek> = DayOfWeek.entries.filter { it.value <= 5 }.toSet()
val Weekend: Set<DayOfWeek> = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

data class Habit(
    val id: Long = 0,
    val name: String,
    val areaId: Long? = null,
    val subArea: String = "",
    val icon: HabitIcon = HabitIcon.Spa,
    val frequency: HabitFrequency = HabitFrequency.Daily,
    /** Only read for [HabitFrequency.Custom]; the other frequencies imply their own days. */
    val customDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val target: String = "",
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = LocalTime.of(8, 0),
    /** Retired habits keep their history but stop appearing and stop counting. */
    val active: Boolean = true,
    val createdAt: LocalDate,
) {
    /** The weekdays a day-based habit is due on. Weekly and Monthly habits can be done any day. */
    val days: Set<DayOfWeek>
        get() = when (frequency) {
            HabitFrequency.Daily, HabitFrequency.Weekly, HabitFrequency.Monthly -> DayOfWeek.entries.toSet()
            HabitFrequency.Weekdays -> Weekdays
            HabitFrequency.Weekends -> Weekend
            HabitFrequency.Custom -> customDays
        }
}

/**
 * What was recorded for a habit on a day. Skipped is the spreadsheet's dash: the habit genuinely
 * did not apply (travel, illness), so the day neither counts against the rate nor breaks a streak.
 */
enum class MarkState { Done, Skipped }

data class HabitMark(val habitId: Long, val date: LocalDate, val state: MarkState)

enum class Priority(val label: String, val short: String) {
    A("Must", "A"),
    B("Should", "B"),
    C("Optional", "C"),
}

enum class TaskStatus(val label: String) {
    NotStarted("Not started"),
    InProgress("In progress"),
    Blocked("Blocked"),
    Done("Done"),
    Dropped("Dropped");

    val isOpen: Boolean get() = this != Done && this != Dropped
}

/** A repeating task spawns its next occurrence when it is completed. */
enum class Repeat(val label: String) { None("Does not repeat"), Daily("Every day"), Weekly("Every week"), Monthly("Every month") }

data class Task(
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val areaId: Long? = null,
    val subArea: String = "",
    val projectId: Long? = null,
    /** First day the task can be worked on. Null means "from now". */
    val start: LocalDate? = null,
    /** Hard deadline. Null means no deadline: never overdue. */
    val due: LocalDate? = null,
    val priority: Priority = Priority.B,
    val status: TaskStatus = TaskStatus.NotStarted,
    /** The real completion date; drives the on-time rate. */
    val doneOn: LocalDate? = null,
    val repeat: Repeat = Repeat.None,
    val createdAt: LocalDate,
) {
    /** The spreadsheet's task ID, derived from the row's database id so it travels with the task. */
    val code: String get() = "T%03d".format(id)
}

/** One deliverable that several tasks add up to, e.g. "Research Proposal". */
data class Project(
    val id: Long = 0,
    val name: String,
    val areaId: Long? = null,
    val subArea: String = "",
    val colorIndex: Int = 0,
    val notes: String = "",
    val archived: Boolean = false,
    val createdAt: LocalDate,
)

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

/**
 * The end-of-day reflection: one per date. Rating and energy are optional one-tap scores; the
 * monologue is the ~150 word free write the spreadsheet's Journal sheet was built around.
 */
data class JournalEntry(
    val date: LocalDate,
    val rating: Int? = null,
    val energy: Int? = null,
    val highlight: String = "",
    val monologue: String = "",
    val tomorrowTop: String = "",
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    val words: Int get() = countWords(monologue)
}

fun countWords(text: String): Int = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

/** Feeling tags offered on the mood check-in (design report, section 4.6). */
val FeelingTags = listOf("Calm", "Anxious", "Tired", "Motivated", "Stressed", "Grateful", "Lonely", "Focused")

enum class Period(val label: String, val days: Long) {
    Week("Week", 7),
    Month("Month", 30),
    Year("Year", 365),
}

enum class ThemeMode(val label: String) { System("System"), Light("Light"), Dark("Dark") }
