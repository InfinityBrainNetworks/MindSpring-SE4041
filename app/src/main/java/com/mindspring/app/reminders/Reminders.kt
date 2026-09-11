package com.mindspring.app.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mindspring.app.AppContainer
import com.mindspring.app.MainActivity
import com.mindspring.app.MindSpringApp
import com.mindspring.app.R
import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.repository.ReminderSettings
import com.mindspring.app.domain.HabitStats
import com.mindspring.app.domain.TaskLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Daily reminders on WorkManager: an evening check-in, a morning task digest, and one per habit
 * that has a reminder time. Each is a daily periodic job; the worker checks, when it fires, whether
 * there is still anything to say (a habit already ticked stays quiet).
 */
object ReminderSync {
    private const val CHANNEL = "reminders"
    private const val PREFS = "reminder_schedule"

    @OptIn(FlowPreview::class)
    fun start(context: Context, container: AppContainer, scope: CoroutineScope) {
        createChannel(context)
        scope.launch {
            combine(
                container.settings.sessionUserId,
                container.settings.checkInReminder,
                container.settings.taskDigest,
                container.habits.habits,
            ) { user, checkIn, digest, habits -> Plan(user != null, checkIn, digest, habits) }
                .debounce(750)
                .collect { apply(context, it) }
        }
    }

    private data class Plan(val signedIn: Boolean, val checkIn: ReminderSettings, val digest: ReminderSettings, val habits: List<Habit>)

    private fun apply(context: Context, plan: Plan) {
        val wm = WorkManager.getInstance(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wanted = mutableMapOf<String, Pair<LocalTime, Map<String, Any>>>()
        if (plan.signedIn) {
            if (plan.checkIn.enabled) wanted["checkin"] = plan.checkIn.time to mapOf(KEY_KIND to KIND_CHECKIN)
            if (plan.digest.enabled) wanted["digest"] = plan.digest.time to mapOf(KEY_KIND to KIND_DIGEST)
            plan.habits.filter { it.active && it.reminderEnabled }.forEach {
                wanted["habit-${it.id}"] = it.reminderTime to mapOf(KEY_KIND to KIND_HABIT, KEY_HABIT to it.id)
            }
        }
        // Drop jobs that are no longer wanted.
        prefs.all.keys.filter { it !in wanted }.forEach { name ->
            wm.cancelUniqueWork(name)
            prefs.edit().remove(name).apply()
        }
        // (Re)schedule only what changed, so opening the app does not keep resetting the clock.
        wanted.forEach { (name, spec) ->
            val (time, data) = spec
            val signature = time.toString()
            if (prefs.getString(name, null) == signature) return@forEach
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayUntil(time), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(*data.map { it.key to it.value }.toTypedArray()))
                .build()
            wm.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
            prefs.edit().putString(name, signature).apply()
        }
    }

    private fun delayUntil(time: LocalTime, now: LocalDateTime = LocalDateTime.now()): Long {
        var next = now.toLocalDate().atTime(time)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis()
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Check-ins, habit reminders and the morning task summary"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    internal fun notify(context: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF10514C.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    internal const val KEY_KIND = "kind"
    internal const val KEY_HABIT = "habitId"
    internal const val KIND_CHECKIN = "checkin"
    internal const val KIND_DIGEST = "digest"
    internal const val KIND_HABIT = "habit"
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as MindSpringApp).container
        container.settings.sessionUserId.first() ?: return Result.success()
        val today = LocalDate.now()
        when (inputData.getString(ReminderSync.KEY_KIND)) {
            ReminderSync.KIND_CHECKIN -> {
                val checkedIn = container.moods.entries.first().any { it.loggedAt.toLocalDate() == today }
                val wrote = container.journal.entry(today).first()?.let { it.words > 0 } == true
                if (!checkedIn || !wrote) {
                    ReminderSync.notify(
                        applicationContext, 1,
                        "How was your day?",
                        if (!checkedIn) "Take a moment to check in and note how you feel." else "A few lines in tonight's reflection will round off the day.",
                    )
                }
            }
            ReminderSync.KIND_DIGEST -> {
                val open = container.tasks.tasks.first().filter { it.status.isOpen }
                val overdue = open.count { TaskLogic.isOverdue(it, today) }
                val dueToday = open.count { it.due == today }
                val habitsToday = container.habits.habits.first().count { HabitStats.isScheduled(it, today) && it.active }
                val parts = buildList {
                    if (dueToday > 0) add("$dueToday due today")
                    if (overdue > 0) add("$overdue overdue")
                    if (habitsToday > 0) add("$habitsToday habits planned")
                }
                if (parts.isNotEmpty()) ReminderSync.notify(applicationContext, 2, "Your day ahead", parts.joinToString(" · "))
            }
            ReminderSync.KIND_HABIT -> {
                val habitId = inputData.getLong(ReminderSync.KEY_HABIT, -1)
                val habit = container.habits.habit(habitId).first() ?: return Result.success()
                val marks = container.habits.marks.first().filter { it.habitId == habitId }.associate { it.date to it.state }
                // Done, or deliberately skipped, today - or a weekly/monthly habit already done this period.
                val handled = marks[today] != null || (!habit.frequency.isDayBased && HabitStats.unitDone(habit, marks, today))
                if (habit.active && habit.reminderEnabled && HabitStats.belongsOn(habit, today) && !handled) {
                    val detail = habit.target.ifBlank { "A small step still counts." }
                    ReminderSync.notify(applicationContext, (1000 + habitId).toInt(), "Time for ${habit.name}", detail)
                }
            }
        }
        return Result.success()
    }
}
