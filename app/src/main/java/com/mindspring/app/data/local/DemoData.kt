package com.mindspring.app.data.local

import com.mindspring.app.data.model.AlertStyle
import com.mindspring.app.data.model.HabitFrequency
import com.mindspring.app.data.model.HabitIcon
import com.mindspring.app.data.model.MarkState
import com.mindspring.app.data.model.Mood
import com.mindspring.app.data.model.Priority
import com.mindspring.app.data.model.Repeat
import com.mindspring.app.data.model.TaskStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * The sample account behind "Use the demo account": about six weeks of habits, tasks, moods and
 * journal entries, generated from fixed seeds so every run (and every screenshot) looks the same.
 * Workout days carry a mood lift so the Insights correlation has something real to find.
 */
object DemoData {
    const val EMAIL = "asan@mindspring.app"
    const val PASSWORD = "mindspring"
    const val NAME = "Asan"
    const val HISTORY_DAYS = 45L

    private data class H(
        val area: Int, val sub: String, val name: String, val freq: HabitFrequency, val target: String,
        val icon: HabitIcon, val odds: Float, val days: Set<DayOfWeek> = emptySet(), val reminder: LocalTime? = null,
    )

    private val workoutDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)

    private val habits = listOf(
        H(0, "Physical Health", "Drink 2.5 L water", HabitFrequency.Daily, "2.5 L", HabitIcon.Water, 0.82f, reminder = LocalTime.of(9, 0)),
        H(0, "Physical Health", "Walk 8,000 steps", HabitFrequency.Daily, "8k steps", HabitIcon.Walk, 0.6f),
        H(0, "Mental Well-being", "10 min meditation", HabitFrequency.Daily, "10 min", HabitIcon.Meditate, 0.62f, reminder = LocalTime.of(7, 0)),
        H(0, "Sleep", "Lights out by 11:30 PM", HabitFrequency.Daily, "7h sleep", HabitIcon.Sleep, 0.55f),
        H(0, "Physical Health", "Workout", HabitFrequency.Custom, "45 min", HabitIcon.Run, 0.75f, days = workoutDays),
        H(3, "Studies", "Deep study block", HabitFrequency.Weekdays, "90 min", HabitIcon.School, 0.7f, reminder = LocalTime.of(18, 0)),
        H(3, "Research", "Read one paper", HabitFrequency.Weekdays, "1 paper", HabitIcon.Book, 0.45f),
        H(1, "Character", "Gratitude, three lines", HabitFrequency.Daily, "3 lines", HabitIcon.Heart, 0.58f),
        H(2, "Family", "Time with family", HabitFrequency.Daily, "20 min", HabitIcon.People, 0.7f),
        H(2, "Social Life", "Reach out to a friend", HabitFrequency.Weekly, "1x/week", HabitIcon.People, 0.25f),
        H(6, "Money Management", "Weekly budget review", HabitFrequency.Weekly, "1x/week", HabitIcon.Money, 0.2f),
        H(7, "Recreation", "Hobby time, no phone", HabitFrequency.Weekends, "1 hour", HabitIcon.Flower, 0.6f),
        H(4, "Planning", "Review goals and direction", HabitFrequency.Monthly, "1x/month", HabitIcon.Write, 0.06f),
    )

    suspend fun seed(db: MindSpringDatabase, userId: Long, today: LocalDate) {
        val areaIds = DefaultAreas.seed(db, userId)
        val start = today.minusDays(HISTORY_DAYS)
        val rnd = Random(42)

        // ---- habits and their marks
        val workoutDone = mutableSetOf<LocalDate>()
        habits.forEachIndexed { index, h ->
            val id = db.habits().upsert(
                HabitEntity(
                    userId = userId, name = h.name, areaId = areaIds[h.area], subArea = h.sub, icon = h.icon.name,
                    frequency = h.freq.name, daysMask = maskFromDays(h.days.ifEmpty { DayOfWeek.entries.toSet() }),
                    target = h.target, reminderEnabled = h.reminder != null,
                    reminderMinute = (h.reminder ?: LocalTime.of(8, 0)).let { it.hour * 60 + it.minute },
                    active = true, createdAt = start,
                ),
            )
            val days = when (h.freq) {
                HabitFrequency.Weekdays -> DayOfWeek.entries.filter { it.value <= 5 }.toSet()
                HabitFrequency.Weekends -> setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                HabitFrequency.Custom -> h.days
                else -> DayOfWeek.entries.toSet()
            }
            val marks = mutableListOf<HabitMarkEntity>()
            for (offset in HISTORY_DAYS downTo 1) {
                val day = today.minusDays(offset)
                if (day.dayOfWeek !in days) continue
                val roll = rnd.nextFloat()
                // Recent days lean better, so streaks are running.
                val odds = if (offset <= 5 && index < 3) 1f else h.odds
                when {
                    roll < odds -> marks += HabitMarkEntity(id, day, MarkState.Done.name)
                    roll > 0.97f && h.freq.isDayBased -> marks += HabitMarkEntity(id, day, MarkState.Skipped.name)
                }
            }
            // Today: a couple already done, matching the dashboard in the design.
            if (index == 0 || index == 5) marks += HabitMarkEntity(id, today, MarkState.Done.name)
            db.habits().upsertMarks(marks)
            if (h.name == "Workout") workoutDone += marks.filter { it.state == MarkState.Done.name }.map { it.date }
        }

        // ---- projects and tasks, dated relative to today
        fun project(name: String, area: Int, sub: String, color: Int) =
            ProjectEntity(userId = userId, name = name, areaId = areaIds[area], subArea = sub, colorIndex = color, notes = "", archived = false, createdAt = start)
        val app = db.projects().upsert(project("Mobile App Assignment", 3, "Assignments", 3))
        val proposal = db.projects().upsert(project("Research Proposal", 3, "Research", 1))
        val portfolio = db.projects().upsert(project("Portfolio Website", 4, "Technical Growth", 4))
        val reels = db.projects().upsert(project("Kids Cartoon Reels", 5, "Content Creation", 5))

        fun d(offset: Long) = today.plusDays(offset)
        fun task(
            title: String, area: Int, sub: String, project: Long?, start: Long?, due: Long?, pri: Priority,
            status: TaskStatus = TaskStatus.NotStarted, doneOn: Long? = null, repeat: Repeat = Repeat.None, notes: String = "",
            alert: LocalTime? = null, alertStyle: AlertStyle = AlertStyle.Reminder,
        ) = TaskEntity(
            userId = userId, title = title, notes = notes, areaId = areaIds[area], subArea = sub, projectId = project,
            startDate = start?.let(::d), dueDate = due?.let(::d), priority = pri.name, status = status.name,
            doneOn = doneOn?.let(::d), repeat = repeat.name, createdAt = d(-(HISTORY_DAYS - 5)),
            // Alerts sit on the due day, so they are always still to come.
            alertAt = alert?.let { due?.let(::d)?.atTime(it) }, alertStyle = alertStyle.name,
        )
        listOf(
            task("Finalise the UI screens", 3, "Assignments", app, -12, -5, Priority.A, TaskStatus.Done, doneOn = -5),
            task("Room database and repositories", 3, "Assignments", app, -4, 1, Priority.A, TaskStatus.InProgress, alert = LocalTime.of(9, 0)),
            task("Write unit and UI tests", 3, "Assignments", app, 0, 4, Priority.A),
            task("Record the demo video", 3, "Assignments", app, 5, 6, Priority.B),
            task(
                "Submit the final APK", 3, "Assignments", app, 7, 7, Priority.A, notes = "Upload to the course page before midnight.",
                alert = LocalTime.of(19, 30), alertStyle = AlertStyle.Alarm,
            ),
            task("Draft the research proposal", 3, "Research", proposal, -15, -6, Priority.A, TaskStatus.Done, doneOn = -4),
            task("Literature review notes", 3, "Research", proposal, -8, -1, Priority.A, TaskStatus.InProgress),
            task("Rework methodology after feedback", 3, "Research", proposal, 2, 10, Priority.B),
            task("Pick a template", 4, "Technical Growth", portfolio, -20, -14, Priority.C, TaskStatus.Done, doneOn = -16),
            task("Write two case studies", 4, "Technical Growth", portfolio, 3, 14, Priority.C),
            task("Deploy the site", 4, "Technical Growth", portfolio, null, null, Priority.C),
            task("Check AI animation compatibility", 5, "Content Creation", reels, -10, -9, Priority.B, TaskStatus.Done, doneOn = -8),
            task("Reel music", 5, "Content Creation", reels, -6, -3, Priority.C, TaskStatus.Dropped),
            task("3D video reel 01", 5, "Content Creation", reels, -2, 2, Priority.B, TaskStatus.InProgress),
            task("Renew laptop insurance", 6, "Protection", null, 0, 0, Priority.B),
            task("Weekly tutoring class", 4, "Teaching", null, 3, 3, Priority.A, repeat = Repeat.Weekly),
            task("Pay the phone bill", 6, "Money Management", null, 12, 12, Priority.B, repeat = Repeat.Monthly),
            task("Call the bank about the card", 6, "Money Management", null, null, null, Priority.C),
            task("Book a dentist appointment", 0, "Physical Health", null, -30, -28, Priority.B, TaskStatus.Done, doneOn = -28),
            task("Return library books", 3, "Studies", null, -35, -33, Priority.C, TaskStatus.Done, doneOn = -34),
            task("Group meeting slides", 3, "Assignments", null, -26, -24, Priority.A, TaskStatus.Done, doneOn = -25),
        ).forEach { db.tasks().upsert(it) }

        // ---- moods: fuller in the last week, with a lift on workout days
        val moodRnd = Random(7)
        for (offset in HISTORY_DAYS downTo 1) {
            if (offset > 7 && moodRnd.nextFloat() > 0.85f) continue
            val day = today.minusDays(offset)
            val base = if (day in workoutDone) 4.2f else 3.3f
            val rating = (base + moodRnd.nextFloat() * 1.6f - 0.8f).roundToInt().coerceIn(1, 5)
            val mood = Mood.fromRating(rating)
            val feelings = FEELINGS.getValue(mood).shuffled(moodRnd).take(1 + moodRnd.nextInt(2))
            val time = LocalTime.of(8 + moodRnd.nextInt(13), moodRnd.nextInt(4) * 15)
            db.moods().upsert(MoodEntity(0, userId, rating, feelings.joinToString("|"), NOTES.getValue(mood).random(moodRnd), day.atTime(time)))
        }

        // ---- gratitude
        listOf(
            1L to "The morning walk was surprisingly peaceful. The fresh air set a good tone for the day.",
            2L to "I finished the lab report ahead of the deadline. Relieved and proud of the effort.",
            3L to "Had dinner with an old friend. It was great to catch up and laugh about old times.",
        ).forEach { (ago, text) -> db.gratitude().upsert(GratitudeEntity(0, userId, text, today.minusDays(ago).atTime(21, 10))) }

        // ---- daily reflections for most recent days
        val jRnd = Random(11)
        for (offset in 24L downTo 1) {
            if (jRnd.nextFloat() > 0.72f) continue
            val words = 90 + jRnd.nextInt(110)
            db.journal().upsert(
                JournalEntity(
                    userId = userId,
                    date = today.minusDays(offset),
                    rating = if (jRnd.nextFloat() < 0.8f) 2 + jRnd.nextInt(4) else null,
                    energy = if (jRnd.nextFloat() < 0.7f) 2 + jRnd.nextInt(4) else null,
                    highlight = HIGHLIGHTS.random(jRnd),
                    monologue = monologue(words, jRnd),
                    tomorrowTop = TOMORROW.random(jRnd),
                    updatedAt = today.minusDays(offset).atTime(22, 0),
                ),
            )
        }
    }

    private fun monologue(words: Int, rnd: Random): String {
        // Each sentence at most once, so an entry never repeats itself.
        val out = StringBuilder()
        var count = 0
        for (s in SENTENCES.shuffled(rnd)) {
            if (count >= words) break
            out.append(s).append(' ')
            count += s.split(' ').size
        }
        return out.toString().trim()
    }

    private val FEELINGS = mapOf(
        Mood.Awful to listOf("Stressed", "Anxious", "Tired", "Lonely"),
        Mood.Bad to listOf("Tired", "Stressed", "Anxious"),
        Mood.Okay to listOf("Tired", "Calm", "Focused"),
        Mood.Good to listOf("Calm", "Focused", "Motivated", "Grateful"),
        Mood.Great to listOf("Motivated", "Grateful", "Focused", "Calm"),
    )

    private val NOTES = mapOf(
        Mood.Awful to listOf("Couldn't sleep and the day dragged. Need to reset tomorrow.", ""),
        Mood.Bad to listOf("Feeling overwhelmed by the upcoming deadlines.", "Skipped lunch and felt drained all afternoon."),
        Mood.Okay to listOf("Just feeling a bit drained today. Need an early night.", "An ordinary day, nothing special.", ""),
        Mood.Good to listOf("Quiet evening by the window. The rain helped me unwind.", "The group meeting went better than expected."),
        Mood.Great to listOf("Felt really focused this morning and got through my whole list.", "Great study session with friends. Feeling confident."),
    )

    private val HIGHLIGHTS = listOf(
        "Finished the database layer", "Long walk after lunch", "Dinner with the family", "A focused two-hour study block",
        "Helped a friend debug their project", "Morning workout felt easy", "Good feedback from the supervisor", "Quiet reading hour",
    )

    private val TOMORROW = listOf(
        "Write the test cases", "Send the proposal draft", "Start the demo video", "Clear the inbox before 10",
        "Finish the literature notes", "Plan the week on Sunday evening",
    )

    private val SENTENCES = listOf(
        "Today started slowly but I found my rhythm after the first coffee.",
        "I kept coming back to the idea that small steps compound over a week.",
        "The study block in the afternoon was the most productive part of the day.",
        "I noticed that I feel calmer on days I get outside early.",
        "There was a moment of stress around the deadline, but writing the plan down helped.",
        "I want to protect my evenings a little more and keep the phone away after nine.",
        "Talking to family over dinner reminded me what actually matters.",
        "Not everything went to plan, and that is fine.",
        "I am proud that I showed up even when I did not feel like it.",
        "Tomorrow I will start with the hardest task before checking messages.",
        "My energy dipped after lunch, so a short walk might be worth trying.",
        "I am learning that consistency beats intensity for me.",
        "The group chat was noisy today, so I muted it and got more done in an hour than all morning.",
        "Lunch outside in the sun did more for my mood than I expected.",
        "I caught myself scrolling late again and put the phone in the other room.",
        "A short call with a friend reminded me that I am not the only one feeling stretched this month.",
        "Writing this down is already making the day feel lighter.",
        "I finally cleared the small task I had been avoiding, and it took ten minutes.",
        "The evening was quiet, and I read a few chapters before bed.",
        "Next week looks busy, so I want to plan it properly on Sunday.",
    )
}
