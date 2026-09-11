package com.mindspring.app.reminders

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mindspring.app.MindSpringApp
import com.mindspring.app.TestApp
import com.mindspring.app.data.model.AlertSounds
import com.mindspring.app.data.model.AlertStyle
import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.model.HabitMark
import com.mindspring.app.data.model.MarkState
import com.mindspring.app.data.model.Task
import com.mindspring.app.data.model.TaskStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = TestApp::class)
class AlertsTest {
    private val app get() = ApplicationProvider.getApplicationContext<MindSpringApp>()
    private val alarms get() = shadowOf(app.getSystemService(AlarmManager::class.java))
    private val notifications get() = shadowOf(app.getSystemService(NotificationManager::class.java))
    private val today = LocalDate.now()
    private val now = LocalDateTime.now()

    @Before fun allowExactAlarms() = ShadowAlarmManager.setCanScheduleExactAlarms(true)

    private fun task(id: Long, alert: LocalDateTime?, style: AlertStyle = AlertStyle.Reminder, status: TaskStatus = TaskStatus.NotStarted) =
        Task(id, "t$id", status = status, createdAt = today, alertAt = alert, alertStyle = style)

    private fun LocalDateTime.millis() = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test fun syncSchedulesOnlyUpcomingAlertsOnOpenTasks() {
        val tasks = listOf(
            task(1, now.plusHours(1)),
            task(2, now.plusHours(2), AlertStyle.Alarm),
            task(3, now.plusHours(1), status = TaskStatus.Done),
            task(4, now.minusHours(1)),
            task(5, null),
        )
        Alerts.sync(app, tasks, emptyList(), emptyList(), now)
        assertEquals(2, alarms.scheduledAlarms.size)
        // The alarm-style one is an alarm clock, which Android shows in the status bar.
        assertEquals(now.plusHours(2).millis(), app.getSystemService(AlarmManager::class.java).nextAlarmClock!!.triggerTime)

        // Removing a task's alert (or the task) cancels its alarm.
        Alerts.sync(app, tasks.take(1), emptyList(), emptyList(), now)
        assertEquals(1, alarms.scheduledAlarms.size)
        Alerts.sync(app, emptyList(), emptyList(), emptyList(), now)
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }

    @Test fun habitReminderIsScheduledForItsNextOpenDay() {
        val morning = today.atTime(7, 0)
        val habit = Habit(1, "Stretch", reminderEnabled = true, reminderTime = LocalTime.of(8, 0), createdAt = today.minusDays(10))
        Alerts.sync(app, emptyList(), listOf(habit), emptyList(), morning)
        assertEquals(today.atTime(8, 0).millis(), alarms.scheduledAlarms.single().triggerAtMs)

        // Ticked early: today's reminder moves to tomorrow.
        Alerts.sync(app, emptyList(), listOf(habit), listOf(HabitMark(1, today, MarkState.Done)), morning)
        assertEquals(today.plusDays(1).atTime(8, 0).millis(), alarms.scheduledAlarms.single().triggerAtMs)

        // Turning the reminder off cancels it.
        Alerts.sync(app, emptyList(), listOf(habit.copy(reminderEnabled = false)), emptyList(), morning)
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }

    @Test fun bothStylesFillTheLockScreenButOnlyTheAlarmKeepsRinging() = runBlocking {
        app.container.auth.register("A", "alarm@x.co", "secret1")
        val tasks = app.container.tasks
        val alarm = tasks.upsert(Task(title = "Submit the APK", due = today, createdAt = today, alertAt = now.plusMinutes(1), alertStyle = AlertStyle.Alarm))
        val reminder = tasks.upsert(Task(title = "Call home", createdAt = today, alertAt = now.plusMinutes(1)))

        Alerts.fire(app, AlertTarget(AlertKind.Task, alarm), null)
        Alerts.fire(app, AlertTarget(AlertKind.Task, reminder), null)

        val ringing = notifications.getNotification(AlertKind.Task.notificationId(alarm))
        assertEquals("Submit the APK", ringing.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Due today", ringing.extras.getString(Notification.EXTRA_TEXT))
        assertNotNull(ringing.fullScreenIntent)
        assertTrue(ringing.flags and Notification.FLAG_INSISTENT != 0)
        assertEquals(3, ringing.actions.size)

        val banner = notifications.getNotification(AlertKind.Task.notificationId(reminder))
        assertNotNull(banner.fullScreenIntent)
        assertEquals(0, banner.flags and Notification.FLAG_INSISTENT)
        assertEquals(0, banner.flags and Notification.FLAG_ONGOING_EVENT)
        assertEquals(listOf("Snooze 10 min", "Done"), banner.actions.map { it.title.toString() })
    }

    @Test fun finishedTaskStaysQuiet() = runBlocking {
        app.container.auth.register("A", "quiet@x.co", "secret1")
        val id = app.container.tasks.upsert(Task(title = "Old", status = TaskStatus.Done, createdAt = today, alertAt = now.plusMinutes(1)))
        Alerts.fire(app, AlertTarget(AlertKind.Task, id), null)
        assertNull(notifications.getNotification(AlertKind.Task.notificationId(id)))
    }

    @Test fun snoozeMovesTheTaskAlertAndDoneFinishesTheTask() = runBlocking {
        app.container.auth.register("A", "snooze@x.co", "secret1")
        val tasks = app.container.tasks
        val id = tasks.upsert(Task(title = "Stretch", createdAt = today, alertAt = now))
        val target = AlertTarget(AlertKind.Task, id)

        Alerts.respond(app, Alerts.ACTION_SNOOZE, target)
        val snoozed = tasks.task(id).first()!!.alertAt!!
        val minutes = Duration.between(now, snoozed).toMinutes()
        assertTrue("snoozed by $minutes minutes", minutes in 9..10)
        assertEquals(1, alarms.scheduledAlarms.size)

        Alerts.respond(app, Alerts.ACTION_DONE, target)
        assertEquals(TaskStatus.Done, tasks.task(id).first()!!.status)
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }

    @Test fun habitAlarmRingsUntilTickedAndDoneTicksIt() = runBlocking {
        app.container.auth.register("A", "habit@x.co", "secret1")
        val habits = app.container.habits
        // A reminder time just passed, as when the alarm goes off.
        val id = habits.upsert(
            Habit(
                name = "Meditate", target = "10 min", reminderEnabled = true, reminderTime = now.toLocalTime().minusSeconds(1),
                reminderStyle = AlertStyle.Alarm, createdAt = today.minusDays(3),
            ),
        )
        val target = AlertTarget(AlertKind.Habit, id)

        Alerts.fire(app, target, null)
        val ringing = notifications.getNotification(AlertKind.Habit.notificationId(id))
        assertEquals("Meditate", ringing.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("10 min", ringing.extras.getString(Notification.EXTRA_TEXT))
        assertTrue(ringing.flags and Notification.FLAG_INSISTENT != 0)
        // Having gone off, the next reminder is lined up for tomorrow.
        assertTrue(alarms.scheduledAlarms.single().triggerAtMs > now.millis())

        // Snooze adds a one-off alarm alongside the daily one.
        Alerts.respond(app, Alerts.ACTION_SNOOZE, target)
        assertEquals(2, alarms.scheduledAlarms.size)

        Alerts.respond(app, Alerts.ACTION_DONE, target)
        assertEquals(MarkState.Done, habits.marks.first().single { it.habitId == id && it.date == today }.state)
        assertNull(notifications.getNotification(AlertKind.Habit.notificationId(id)))

        // Once ticked, a late (snoozed) alarm stays quiet.
        Alerts.fire(app, target, null)
        assertNull(notifications.getNotification(AlertKind.Habit.notificationId(id)))
    }

    @Test fun targetsReadBackIncludingTheOldFormat() {
        assertEquals(AlertTarget(AlertKind.Habit, 4), AlertTarget.parse(AlertTarget(AlertKind.Habit, 4).key))
        assertEquals(AlertTarget(AlertKind.Task, 7), AlertTarget.parse("7"))
        assertNull(AlertTarget.parse("nonsense:1"))
    }

    @Test fun channelsFollowTheChosenTones() {
        val nm = app.getSystemService(NotificationManager::class.java)
        val first = Channels.ensure(app, AlertSounds("content://tone/a", "content://tone/b"))
        val second = Channels.ensure(app, AlertSounds("content://tone/c", "content://tone/b"))
        // A new reminder tone makes new reminder channels; the alarm channel is untouched.
        assertTrue(first.task != second.task)
        assertEquals(first.alarm, second.alarm)
        assertEquals(setOf(second.daily, second.task, second.alarm), nm.notificationChannels.map { it.id }.toSet())
        assertNull(nm.getNotificationChannel(Channels.ensure(app, AlertSounds(null, null)).alarm).sound)
    }
}
