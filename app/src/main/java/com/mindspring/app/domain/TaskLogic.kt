package com.mindspring.app.domain

import com.mindspring.app.data.model.Repeat
import com.mindspring.app.data.model.Task
import com.mindspring.app.data.model.TaskStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** The workbook's Flag column: one word for where a task stands today. */
enum class TaskFlag(val label: String) {
    Overdue("Overdue"),
    DueToday("Due today"),
    DueSoon("Due soon"),
    Upcoming("Upcoming"),
    Open("Open"),
    NoDeadline("No deadline"),
    DoneOnTime("Done on time"),
    DoneLate("Done late"),
    Done("Done"),
    Dropped("Dropped"),
}

/** Sections of the task list, most urgent first. */
enum class TaskGroup(val label: String) {
    Overdue("Overdue"),
    Today("Today"),
    Tomorrow("Tomorrow"),
    Week("Next 7 days"),
    Later("Later"),
    NoDeadline("No deadline"),
    Finished("Completed"),
}

/** Counts for a set of tasks - a project, a sub-area or everything. Dropped tasks are not failures. */
data class TaskRollup(
    val total: Int,
    val done: Int,
    val open: Int,
    val overdue: Int,
    val dropped: Int,
    val nextDue: LocalDate?,
) {
    /** Done out of everything that was not dropped. */
    val progress: Float get() = (total - dropped).let { if (it <= 0) 0f else done.toFloat() / it }

    val state: String
        get() = when {
            total == 0 -> ""
            open == 0 -> "Complete"
            overdue > 0 -> "$overdue overdue"
            else -> "$open open"
        }

    companion object {
        val Empty = TaskRollup(0, 0, 0, 0, 0, null)
    }
}

object TaskLogic {
    /** A deadline this many days away or fewer reads as "due soon". */
    const val SOON_DAYS = 3L

    fun flag(task: Task, today: LocalDate): TaskFlag = when {
        task.status == TaskStatus.Dropped -> TaskFlag.Dropped
        task.status == TaskStatus.Done -> when {
            task.doneOn == null || task.due == null -> TaskFlag.Done
            !task.doneOn.isAfter(task.due) -> TaskFlag.DoneOnTime
            else -> TaskFlag.DoneLate
        }
        task.due == null -> TaskFlag.NoDeadline
        task.due.isBefore(today) -> TaskFlag.Overdue
        task.due == today -> TaskFlag.DueToday
        !task.due.isAfter(today.plusDays(SOON_DAYS)) -> TaskFlag.DueSoon
        task.start != null && task.start.isAfter(today) -> TaskFlag.Upcoming
        else -> TaskFlag.Open
    }

    fun isOverdue(task: Task, today: LocalDate): Boolean = task.status.isOpen && task.due != null && task.due.isBefore(today)

    /** Days until the deadline; negative when overdue; null for finished or undated tasks. */
    fun daysLeft(task: Task, today: LocalDate): Long? =
        if (!task.status.isOpen || task.due == null) null else ChronoUnit.DAYS.between(today, task.due)

    /** Length of the start-to-due window in days (a single-day task is 1). */
    fun window(task: Task): Long? = when {
        task.due == null -> null
        task.start == null -> 1
        else -> ChronoUnit.DAYS.between(task.start, task.due) + 1
    }

    fun group(task: Task, today: LocalDate): TaskGroup {
        if (!task.status.isOpen) return TaskGroup.Finished
        val due = task.due ?: return TaskGroup.NoDeadline
        return when {
            due.isBefore(today) -> TaskGroup.Overdue
            due == today -> TaskGroup.Today
            due == today.plusDays(1) -> TaskGroup.Tomorrow
            !due.isAfter(today.plusDays(7)) -> TaskGroup.Week
            else -> TaskGroup.Later
        }
    }

    /** Most urgent first: earliest deadline, undated last, then priority. */
    val urgency: Comparator<Task> = compareBy<Task>({ it.due ?: LocalDate.MAX }, { it.priority.ordinal }, { it.id })

    /** Recently finished first. */
    val recentlyFinished: Comparator<Task> = compareByDescending<Task> { it.doneOn ?: it.due ?: it.createdAt }.thenByDescending { it.id }

    /**
     * The next occurrence of a repeating task, or null. Dates move forward by the repeat period
     * until the task lands today or later, so finishing a weekly class late skips the missed slot
     * instead of creating one already overdue. An alert moves with the dates, keeping its time of day.
     */
    fun nextOccurrence(task: Task, today: LocalDate): Task? {
        if (task.repeat == Repeat.None) return null
        fun step(d: LocalDate): LocalDate = when (task.repeat) {
            Repeat.Daily -> d.plusDays(1)
            Repeat.Weekly -> d.plusWeeks(1)
            Repeat.Monthly -> d.plusMonths(1)
            Repeat.None -> d
        }
        var start = task.start
        var due = task.due
        var alert = task.alertAt
        if (start == null && due == null) due = today
        do {
            start = start?.let(::step)
            due = due?.let(::step)
            alert = alert?.let { step(it.toLocalDate()).atTime(it.toLocalTime()) }
        } while ((due ?: start)!!.isBefore(today))
        return task.copy(id = 0, start = start, due = due, status = TaskStatus.NotStarted, doneOn = null, createdAt = today, alertAt = alert)
    }

    fun rollup(tasks: List<Task>, today: LocalDate): TaskRollup {
        if (tasks.isEmpty()) return TaskRollup.Empty
        val open = tasks.filter { it.status.isOpen }
        return TaskRollup(
            total = tasks.size,
            done = tasks.count { it.status == TaskStatus.Done },
            open = open.size,
            overdue = open.count { isOverdue(it, today) },
            dropped = tasks.count { it.status == TaskStatus.Dropped },
            nextDue = open.mapNotNull { it.due }.minOrNull(),
        )
    }
}

/** Sub-areas and project labels group on a trimmed, case-insensitive key, so "Research " and "research" stay one. */
fun groupKey(label: String): String = label.trim().lowercase()
