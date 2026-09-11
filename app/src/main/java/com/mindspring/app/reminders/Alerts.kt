package com.mindspring.app.reminders

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mindspring.app.MainActivity
import com.mindspring.app.MindSpringApp
import com.mindspring.app.R
import com.mindspring.app.data.local.enumOr
import com.mindspring.app.data.model.AlertStyle
import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.model.HabitMark
import com.mindspring.app.data.model.MarkState
import com.mindspring.app.data.model.Task
import com.mindspring.app.data.model.TaskStatus
import com.mindspring.app.domain.HabitStats
import com.mindspring.app.ui.util.Fmt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** What an alert belongs to. */
enum class AlertKind(val path: String, private val notificationBase: Int) {
    Task("task", 50_000),
    Habit("habit", 70_000);

    fun notificationId(id: Long): Int = notificationBase + id.toInt()
}

data class AlertTarget(val kind: AlertKind, val id: Long) {
    val key: String get() = "${kind.path}:$id"
    val notificationId: Int get() = kind.notificationId(id)

    companion object {
        /** The sample alert sent from settings, which belongs to nothing. */
        val Test = AlertTarget(AlertKind.Task, 0)

        /** Reads [key]; a bare number is a task, as stored before habits had alerts. */
        fun parse(key: String): AlertTarget? {
            val parts = key.split(':')
            return when (parts.size) {
                1 -> parts[0].toLongOrNull()?.let { AlertTarget(AlertKind.Task, it) }
                2 -> {
                    val kind = AlertKind.entries.firstOrNull { it.path == parts[0] } ?: return null
                    parts[1].toLongOrNull()?.let { AlertTarget(kind, it) }
                }
                else -> null
            }
        }
    }
}

/** What an alert shows: a title, and a line on when it is due or what the habit asks for. */
data class AlertInfo(
    val target: AlertTarget,
    val title: String,
    val detail: String,
    val style: AlertStyle,
    val at: LocalDateTime,
) {
    val isTest: Boolean get() = target == AlertTarget.Test

    companion object {
        fun sample(style: AlertStyle, at: LocalDateTime = LocalDateTime.now()) = AlertInfo(
            AlertTarget.Test,
            if (style == AlertStyle.Alarm) "This is an alarm" else "This is a reminder",
            "Snooze it or mark it done right from here",
            style,
            at,
        )
    }
}

/**
 * Exact-time alerts on AlarmManager, for tasks and habits. An open task with an alert still to
 * come holds one alarm; a habit with its reminder on holds one for its next reminder, and moves on
 * to the following one each time it goes off. [sync] adds and removes alarms to match the data,
 * and runs again after a reboot, an app update or a clock change, since Android forgets alarms then.
 *
 * Both styles are high-priority notifications with a full-screen intent: in use, the phone shows
 * them as a banner dropping over whatever is on screen; locked or asleep, it opens [AlarmActivity]
 * over the lock screen. A reminder sounds once; an alarm's sound repeats until the user answers it
 * or [RING_TIMEOUT] passes.
 */
object Alerts {
    const val ACTION_FIRE = "com.mindspring.app.alert.FIRE"
    const val ACTION_DONE = "com.mindspring.app.alert.DONE"
    const val ACTION_SNOOZE = "com.mindspring.app.alert.SNOOZE"
    const val ACTION_STOP = "com.mindspring.app.alert.STOP"
    const val ACTION_OPEN = "com.mindspring.app.alert.OPEN"
    private const val EXTRA_KIND = "kind"
    private const val EXTRA_ID = "id"
    private const val EXTRA_STYLE = "style"

    val RING_TIMEOUT: Duration = Duration.ofMinutes(10)

    private const val PREFS = "task_alerts"
    private const val KEY_SCHEDULED = "scheduled"

    // ---------- scheduling ----------

    /**
     * Schedules every upcoming task alert and each habit's next reminder, and cancels any
     * scheduled alarm that is no longer wanted.
     */
    fun sync(context: Context, tasks: List<Task>, habits: List<Habit>, marks: List<HabitMark>, now: LocalDateTime = LocalDateTime.now()) {
        val marksByHabit = marks.groupBy { it.habitId }.mapValues { (_, list) -> list.associate { it.date to it.state } }
        val wanted = buildMap {
            tasks.forEach { t -> t.upcomingAlert(now)?.let { put(AlertTarget(AlertKind.Task, t.id), it to t.alertStyle) } }
            habits.forEach { h ->
                HabitStats.nextReminder(h, marksByHabit[h.id].orEmpty(), now)?.let { put(AlertTarget(AlertKind.Habit, h.id), it to h.reminderStyle) }
            }
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val before = prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty().mapNotNull(AlertTarget::parse)
        (before - wanted.keys).forEach { cancel(context, it) }
        // Setting an alarm with the same PendingIntent replaces it, so rescheduling everything is safe.
        wanted.forEach { (target, v) -> schedule(context, target, v.first.toEpochMillis(), v.second) }
        prefs.edit().putStringSet(KEY_SCHEDULED, wanted.keys.map { it.key }.toSet()).apply()
    }

    /** Re-reads the signed-in user's data and syncs; used where it is not already at hand. */
    suspend fun resync(app: MindSpringApp) {
        val c = app.container
        sync(app, c.tasks.tasks.first(), c.habits.habits.first(), c.habits.marks.first())
    }

    /** A sample alert a few seconds from now, so the user can see and hear what they chose. */
    fun scheduleTest(context: Context, style: AlertStyle, seconds: Long) =
        schedule(context, AlertTarget.Test, System.currentTimeMillis() + seconds * 1000, style, test = true)

    private fun schedule(context: Context, target: AlertTarget, trigger: Long, style: AlertStyle, test: Boolean = false, snooze: Boolean = false) {
        val am = context.getSystemService(AlarmManager::class.java)
        val fire = broadcast(context, ACTION_FIRE, target, snooze) { if (test) putExtra(EXTRA_STYLE, style.name) }
        try {
            when {
                !AlertAccess.exactAllowed(context) -> am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, fire)
                // An alarm clock alarm is the most punctual kind and shows the alarm icon in the status bar.
                style == AlertStyle.Alarm -> am.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, open(context, target)), fire)
                else -> am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, fire)
            }
        } catch (_: SecurityException) {
            // Exact alarms were revoked between the check and the call: fall back to an approximate one.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, fire)
        }
    }

    private fun cancel(context: Context, target: AlertTarget) {
        context.getSystemService(AlarmManager::class.java).cancel(broadcast(context, ACTION_FIRE, target))
    }

    // ---------- firing ----------

    /** What to show, or null when there is nothing left to remind about (finished, ticked, removed). */
    suspend fun info(app: MindSpringApp, target: AlertTarget, testStyle: AlertStyle?, now: LocalDateTime = LocalDateTime.now()): AlertInfo? {
        if (testStyle != null) return AlertInfo.sample(testStyle)
        return when (target.kind) {
            AlertKind.Task -> {
                val task = app.container.tasks.task(target.id).first() ?: return null
                val at = task.alertAt ?: return null
                if (!task.status.isOpen) return null
                val project = task.projectId?.let { id -> app.container.tasks.projects.first().firstOrNull { it.id == id }?.name }
                AlertInfo(target, task.title, taskDetail(task, project), task.alertStyle, at)
            }
            AlertKind.Habit -> {
                val habits = app.container.habits
                val habit = habits.habit(target.id).first() ?: return null
                val today = now.toLocalDate()
                val marks = habits.marks.first().filter { it.habitId == habit.id }.associate { it.date to it.state }
                // Done, or deliberately skipped, today - or a weekly/monthly habit already done this period.
                val handled = marks[today] != null || (!habit.frequency.isDayBased && HabitStats.unitDone(habit, marks, today))
                if (!habit.active || !habit.reminderEnabled || !HabitStats.belongsOn(habit, today) || handled) return null
                AlertInfo(target, habit.name, habitDetail(habit), habit.reminderStyle, today.atTime(habit.reminderTime))
            }
        }
    }

    fun taskDetail(task: Task, project: String?, today: LocalDate = LocalDate.now()): String = listOfNotNull(
        task.due?.let { Fmt.dueIn(it, today).replaceFirstChar(Char::uppercase) } ?: "No deadline",
        project,
    ).joinToString(" · ")

    fun habitDetail(habit: Habit): String = listOfNotNull(
        habit.target.ifBlank { "A small step still counts." },
        habit.frequency.takeIf { !it.isDayBased }?.let { "${it.label} habit" },
    ).joinToString(" · ")

    suspend fun fire(app: MindSpringApp, target: AlertTarget, testStyle: AlertStyle?) {
        info(app, target, testStyle)?.let { alert ->
            val sounds = app.container.settings.alertSounds.first()
            val channels = Channels.ensure(app, sounds)
            if (AlertAccess.notificationsAllowed(app)) {
                NotificationManagerCompat.from(app).notify(target.notificationId, build(app, alert, channels, sounds.snoozeMinutes))
            }
        }
        // A habit's alarm is spent once it goes off; line up its next reminder.
        if (testStyle == null) resync(app)
    }

    private fun build(context: Context, a: AlertInfo, channels: Channels.Ids, snoozeMinutes: Int): Notification {
        val alarm = a.style == AlertStyle.Alarm
        val builder = NotificationCompat.Builder(context, if (alarm) channels.alarm else channels.task)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF10514C.toInt())
            .setContentTitle(a.title)
            .setContentText(a.detail)
            .setWhen(a.at.toEpochMillis())
            .setShowWhen(true)
            .setPriority(if (alarm) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (alarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, "Snooze $snoozeMinutes min", broadcast(context, ACTION_SNOOZE, a.target))
            .addAction(0, "Done", broadcast(context, ACTION_DONE, a.target))
            // On a locked or sleeping phone both styles take over the screen; in use, Android shows a banner instead.
            .setFullScreenIntent(alarmScreen(context, a), true)
        if (!alarm) {
            return builder.setContentIntent(open(context, a.target)).setAutoCancel(true).build()
        }
        return builder
            .setContentIntent(alarmScreen(context, a))
            .setOngoing(true)
            .setTimeoutAfter(RING_TIMEOUT.toMillis())
            .addAction(0, "Stop", broadcast(context, ACTION_STOP, a.target))
            .build()
            // The tone repeats until the notification is answered or times out, like an alarm clock.
            .apply { flags = flags or Notification.FLAG_INSISTENT }
    }

    // ---------- answering ----------

    /** Done, snooze or stop - from a notification button or the full-screen alert. */
    suspend fun respond(app: MindSpringApp, action: String, target: AlertTarget) {
        NotificationManagerCompat.from(app).cancel(target.notificationId)
        if (target == AlertTarget.Test) return
        val minutes = app.container.settings.alertSounds.first().snoozeMinutes.toLong()
        val later = LocalDateTime.now().plusMinutes(minutes).truncatedTo(ChronoUnit.MINUTES)
        when (target.kind) {
            AlertKind.Task -> {
                val tasks = app.container.tasks
                when (action) {
                    ACTION_DONE -> tasks.setStatus(target.id, TaskStatus.Done)
                    ACTION_SNOOZE -> tasks.task(target.id).first()?.let { tasks.upsert(it.copy(alertAt = later)) }
                }
            }
            AlertKind.Habit -> {
                val habits = app.container.habits
                when (action) {
                    ACTION_DONE -> habits.setMark(target.id, LocalDate.now(), MarkState.Done)
                    // A habit's reminder time stays as set; the snooze is its own one-off alarm.
                    ACTION_SNOOZE -> habits.habit(target.id).first()?.let {
                        schedule(app, target, later.toEpochMillis(), it.reminderStyle, snooze = true)
                    }
                }
            }
        }
        // The app may not be running to notice the change, so update the alarms here.
        resync(app)
    }

    // ---------- intents ----------

    private fun uri(target: AlertTarget, snooze: Boolean = false) =
        Uri.parse("mindspring://${target.kind.path}/${target.id}" + if (snooze) "/snooze" else "")

    private fun Intent.put(target: AlertTarget): Intent = putExtra(EXTRA_KIND, target.kind.name).putExtra(EXTRA_ID, target.id)

    fun targetFrom(intent: Intent): AlertTarget? {
        val id = intent.getLongExtra(EXTRA_ID, -1).takeIf { it >= 0 } ?: return null
        return AlertTarget(enumOr(intent.getStringExtra(EXTRA_KIND).orEmpty(), AlertKind.Task), id)
    }

    fun testStyleFrom(intent: Intent): AlertStyle? = intent.getStringExtra(EXTRA_STYLE)?.let { enumOr(it, AlertStyle.Reminder) }

    private fun broadcast(context: Context, action: String, target: AlertTarget, snooze: Boolean = false, extras: Intent.() -> Unit = {}): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            target.notificationId,
            // The data URI keeps each alert's intents distinct from every other's.
            Intent(context, AlertReceiver::class.java).setAction(action).setData(uri(target, snooze)).put(target).apply(extras),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Opens the app on the task or habit. */
    fun openIntent(context: Context, target: AlertTarget): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN)
            .setData(uri(target))
            .put(target)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    private fun open(context: Context, target: AlertTarget): PendingIntent =
        PendingIntent.getActivity(context, target.notificationId, openIntent(context, target), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun alarmScreen(context: Context, a: AlertInfo): PendingIntent =
        PendingIntent.getActivity(
            context,
            a.target.notificationId,
            Intent(context, AlarmActivity::class.java)
                .setData(uri(a.target))
                .put(a.target)
                .apply { if (a.isTest) putExtra(EXTRA_STYLE, a.style.name) }
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun LocalDateTime.toEpochMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

/** What Android lets the app do, and where to send the user to change it. */
object AlertAccess {
    fun notificationsAllowed(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** On time to the minute; otherwise Android may hold an alert back to save battery. */
    fun exactAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    /** Alerts may take over a locked screen. */
    fun fullScreenAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    fun notificationSettings(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun exactSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= 31) Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        else notificationSettings(context)

    fun fullScreenSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= 34) Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
        else notificationSettings(context)
}

/** Fires alerts and answers their buttons. */
class AlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as MindSpringApp
        val action = intent.action ?: return
        val target = Alerts.targetFrom(intent) ?: return
        val pending = goAsync()
        app.appScope.launch {
            try {
                if (action == Alerts.ACTION_FIRE) Alerts.fire(app, target, Alerts.testStyleFrom(intent))
                else Alerts.respond(app, action, target)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Puts the alarms back after Android has cleared them: a reboot, an update, or a clock change. */
class AlertRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in Triggers) return
        val app = context.applicationContext as MindSpringApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                Alerts.resync(app)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val Triggers = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
