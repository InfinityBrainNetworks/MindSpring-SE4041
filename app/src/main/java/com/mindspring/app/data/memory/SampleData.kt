package com.mindspring.app.data.memory

import com.mindspring.app.data.model.GratitudeEntry
import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.model.HabitCategory
import com.mindspring.app.data.model.HabitCompletion
import com.mindspring.app.data.model.HabitIcon
import com.mindspring.app.data.model.Mood
import com.mindspring.app.data.model.MoodEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Deterministic demo history (about six weeks) so every screen has realistic content during the
 * UI phase. Exercise days are given a mood lift so the Insights correlation has something to find.
 */
internal object SampleData {
    private const val HISTORY_DAYS = 45L

    fun habits(today: LocalDate): List<Habit> {
        val start = today.minusDays(HISTORY_DAYS)
        val everyDay = DayOfWeek.entries.toSet()
        return listOf(
            Habit(1, "Study Physics", HabitCategory.Study, HabitIcon.Book, everyDay, true, LocalTime.of(18, 0), start),
            Habit(2, "Drink Water", HabitCategory.Health, HabitIcon.Water, everyDay, true, LocalTime.of(9, 0), start),
            Habit(3, "Morning Meditation", HabitCategory.Mindfulness, HabitIcon.Meditate, everyDay, true, LocalTime.of(7, 0), start),
            Habit(
                4, "Exercise", HabitCategory.Fitness, HabitIcon.Run,
                setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
                false, LocalTime.of(17, 30), start,
            ),
        )
    }

    fun completions(today: LocalDate): List<HabitCompletion> {
        val rnd = Random(42)
        val out = mutableListOf<HabitCompletion>()
        for (offset in HISTORY_DAYS downTo 1) {
            val day = today.minusDays(offset)
            if (rnd.nextFloat() < 0.68f) out += HabitCompletion(1, day)
            if (offset <= 6 || rnd.nextFloat() < 0.8f) out += HabitCompletion(2, day)
            if (offset <= 12 || rnd.nextFloat() < 0.55f) out += HabitCompletion(3, day)
            if (day.dayOfWeek in EXERCISE_DAYS && rnd.nextFloat() < 0.75f) out += HabitCompletion(4, day)
        }
        // Today: two of four done, matching the dashboard in the design.
        out += HabitCompletion(1, today)
        out += HabitCompletion(2, today)
        return out
    }

    fun moods(today: LocalDate, completions: List<HabitCompletion>): List<MoodEntry> {
        val rnd = Random(7)
        val exercised = completions.filter { it.habitId == 4L }.map { it.date }.toSet()
        val out = mutableListOf<MoodEntry>()
        var id = 1L
        for (offset in HISTORY_DAYS downTo 1) {
            // The last week is always filled so the Week views have something to show.
            if (offset > 7 && rnd.nextFloat() > 0.85f) continue
            val day = today.minusDays(offset)
            val base = if (day in exercised) 4.2f else 3.3f
            val rating = (base + rnd.nextFloat() * 1.6f - 0.8f).roundToInt().coerceIn(1, 5)
            val mood = Mood.fromRating(rating)
            val feelings = FEELINGS_BY_MOOD.getValue(mood).shuffled(rnd).take(1 + rnd.nextInt(2))
            val note = NOTES_BY_MOOD.getValue(mood).random(rnd)
            val time = LocalTime.of(8 + rnd.nextInt(13), rnd.nextInt(4) * 15)
            out += MoodEntry(id++, mood, feelings, note, day.atTime(time))
        }
        return out
    }

    fun gratitude(today: LocalDate): List<GratitudeEntry> = listOf(
        GratitudeEntry(1, "The morning walk was surprisingly peaceful. The fresh air set a good tone for the day.", today.minusDays(1).atTime(21, 10)),
        GratitudeEntry(2, "I finished the lab report ahead of the deadline. Relieved and proud of the effort.", today.minusDays(2).atTime(20, 45)),
        GratitudeEntry(3, "Had dinner with an old friend. It was great to catch up and laugh about old times.", today.minusDays(3).atTime(22, 0)),
    )

    private val EXERCISE_DAYS = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)

    private val FEELINGS_BY_MOOD = mapOf(
        Mood.Awful to listOf("Stressed", "Anxious", "Tired", "Lonely"),
        Mood.Bad to listOf("Tired", "Stressed", "Anxious"),
        Mood.Okay to listOf("Tired", "Calm", "Focused"),
        Mood.Good to listOf("Calm", "Focused", "Motivated", "Grateful"),
        Mood.Great to listOf("Motivated", "Grateful", "Focused", "Calm"),
    )

    private val NOTES_BY_MOOD = mapOf(
        Mood.Awful to listOf("Couldn't sleep and the day dragged. Need to reset tomorrow.", ""),
        Mood.Bad to listOf("Feeling overwhelmed by the upcoming midterms.", "Skipped lunch and felt drained all afternoon."),
        Mood.Okay to listOf("Just feeling a bit drained today. Need an early night.", "An ordinary day, nothing special.", ""),
        Mood.Good to listOf("Quiet evening by the window. The rain helped me unwind.", "Finished the group project meeting. It went better than expected."),
        Mood.Great to listOf("Felt really focused this morning and got through my entire reading list.", "Great study session with friends. Feeling confident about the exam."),
    )
}
