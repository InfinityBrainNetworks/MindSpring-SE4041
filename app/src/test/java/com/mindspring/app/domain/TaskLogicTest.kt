package com.mindspring.app.domain

import com.mindspring.app.data.model.Priority
import com.mindspring.app.data.model.Repeat
import com.mindspring.app.data.model.Task
import com.mindspring.app.data.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TaskLogicTest {
    private val today = LocalDate.of(2026, 9, 11)

    private fun task(
        due: LocalDate? = null,
        start: LocalDate? = null,
        status: TaskStatus = TaskStatus.NotStarted,
        doneOn: LocalDate? = null,
        priority: Priority = Priority.B,
        repeat: Repeat = Repeat.None,
        id: Long = 1,
    ) = Task(id = id, title = "t$id", start = start, due = due, status = status, doneOn = doneOn, priority = priority, repeat = repeat, createdAt = today)

    @Test fun flagsFollowTheWorkbook() {
        assertEquals(TaskFlag.Overdue, TaskLogic.flag(task(due = today.minusDays(1)), today))
        assertEquals(TaskFlag.DueToday, TaskLogic.flag(task(due = today), today))
        assertEquals(TaskFlag.DueSoon, TaskLogic.flag(task(due = today.plusDays(3)), today))
        assertEquals(TaskFlag.Upcoming, TaskLogic.flag(task(start = today.plusDays(5), due = today.plusDays(9)), today))
        assertEquals(TaskFlag.Open, TaskLogic.flag(task(due = today.plusDays(9)), today))
        assertEquals(TaskFlag.NoDeadline, TaskLogic.flag(task(), today))
        assertEquals(TaskFlag.Dropped, TaskLogic.flag(task(due = today.minusDays(4), status = TaskStatus.Dropped), today))
    }

    @Test fun deliveryIsJudgedOnTheDoneOnDate() {
        val due = today.minusDays(2)
        assertEquals(TaskFlag.DoneOnTime, TaskLogic.flag(task(due = due, status = TaskStatus.Done, doneOn = due), today))
        assertEquals(TaskFlag.DoneLate, TaskLogic.flag(task(due = due, status = TaskStatus.Done, doneOn = due.plusDays(1)), today))
        assertEquals(TaskFlag.Done, TaskLogic.flag(task(status = TaskStatus.Done, doneOn = today), today))
    }

    @Test fun windowAndDaysLeft() {
        assertEquals(1L, TaskLogic.window(task(due = today)))
        assertEquals(7L, TaskLogic.window(task(start = today, due = today.plusDays(6))))
        assertNull(TaskLogic.window(task()))
        assertEquals(-3L, TaskLogic.daysLeft(task(due = today.minusDays(3)), today))
        assertNull(TaskLogic.daysLeft(task(due = today, status = TaskStatus.Done), today))
    }

    @Test fun urgencyPutsEarliestFirstAndUndatedLast() {
        val list = listOf(
            task(id = 1),
            task(due = today.plusDays(2), priority = Priority.C, id = 2),
            task(due = today.plusDays(2), priority = Priority.A, id = 3),
            task(due = today.minusDays(1), id = 4),
        )
        assertEquals(listOf(4L, 3L, 2L, 1L), list.sortedWith(TaskLogic.urgency).map { it.id })
    }

    @Test fun weeklyRepeatMovesToTheNextSlot() {
        val t = task(start = today, due = today, repeat = Repeat.Weekly, status = TaskStatus.Done, doneOn = today)
        val next = TaskLogic.nextOccurrence(t, today)!!
        assertEquals(today.plusWeeks(1), next.due)
        assertEquals(today.plusWeeks(1), next.start)
        assertEquals(TaskStatus.NotStarted, next.status)
        assertNull(next.doneOn)
        assertEquals(0L, next.id)
    }

    @Test fun lateRepeatSkipsSlotsAlreadyPast() {
        val t = task(due = today.minusDays(15), repeat = Repeat.Weekly)
        assertEquals(today.plusDays(6), TaskLogic.nextOccurrence(t, today)!!.due)
        assertNull(TaskLogic.nextOccurrence(task(due = today), today))
    }

    @Test fun rollupIgnoresDroppedWork() {
        val tasks = listOf(
            task(status = TaskStatus.Done, id = 1),
            task(status = TaskStatus.Dropped, id = 2),
            task(due = today.minusDays(1), id = 3),
            task(due = today.plusDays(4), id = 4),
        )
        val r = TaskLogic.rollup(tasks, today)
        assertEquals(4, r.total)
        assertEquals(1, r.done)
        assertEquals(2, r.open)
        assertEquals(1, r.overdue)
        assertEquals(1f / 3, r.progress, 0.001f)
        assertEquals(today.minusDays(1), r.nextDue)
        assertEquals("1 overdue", r.state)
    }

    @Test fun groupKeyTrimsAndIgnoresCase() {
        assertEquals(groupKey("Research"), groupKey("  research "))
    }
}
