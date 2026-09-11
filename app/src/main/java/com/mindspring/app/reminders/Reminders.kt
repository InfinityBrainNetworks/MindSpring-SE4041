package com.mindspring.app.reminders

import android.Manifest
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
 * Daily nudges on WorkManager: an evening check-in and a morning task digest. Each is a daily
 * periodic job; the worker checks, when it fires, whether there is still anything to say.
 *
 * Also keeps the exact-time task alerts and habit reminders ([Alerts]) and the notification
 * channels in step with the data and the chosen tones.
 */
object ReminderSync {
    private const val PREFS = "reminder_schedule"

    @OptIn(FlowPreview::class)
    fun start(context: Context, container: AppContainer, scope: CoroutineScope) {
        scope.launch {
            container.settings.alertSounds.collect { Channels.ensure(context, it) }
        }
        scope.launch {
            // The data is scoped to the signed-in user, so signing out empties it and cancels every alert.
            combine(container.tasks.tasks, container.habits.habits, container.habits.marks, ::Triple)
                .debounce(500)
                .collect { (tasks, habits, marks) -> Alerts.sync(context, tasks, habits, marks) }
        }
        scope.launch {
            combine(
                container.settings.sessionUserId,
                container.settings.checkInReminder,
                container.settings.taskDigest,
            ) { user, checkIn, digest -> Plan(user != null, checkIn, digest) }
                .debounce(750)
                .collect { apply(context, it) }
        }
    }

    private data class Plan(val signedIn: Boolean, val checkIn: ReminderSettings, val digest: ReminderSettings)

    private fun apply(context: Context, plan: Plan) {
        val wm = WorkManager.getInstance(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wanted = mutableMapOf<String, Pair<LocalTime, Map<String, Any>>>()
        if (plan.signedIn) {
            if (plan.checkIn.enabled) wanted["checkin"] = plan.checkIn.time to mapOf(KEY_KIND to KIND_CHECKIN)
            if (plan.digest.enabled) wanted["digest"] = plan.digest.time to mapOf(KEY_KIND to KIND_DIGEST)
        }
        // Drop jobs that are no longer wanted, including the per-habit jobs of earlier versions.
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

    internal fun notify(context: Context, channel: String, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channel)
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
    internal const val KIND_CHECKIN = "checkin"
    internal const val KIND_DIGEST = "digest"
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as MindSpringApp).container
        container.settings.sessionUserId.first() ?: return Result.success()
        val today = LocalDate.now()
        val channel = Channels.ensure(applicationContext, container.settings.alertSounds.first()).daily
        when (inputData.getString(ReminderSync.KEY_KIND)) {
            ReminderSync.KIND_CHECKIN -> {
                val checkedIn = container.moods.entries.first().any { it.loggedAt.toLocalDate() == today }
                val wrote = container.journal.entry(today).first()?.let { it.words > 0 } == true
                if (!checkedIn || !wrote) {
                    ReminderSync.notify(
                        applicationContext, channel, 1,
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
                if (parts.isNotEmpty()) ReminderSync.notify(applicationContext, channel, 2, "Your day ahead", parts.joinToString(" · "))
            }
        }
        return Result.success()
    }
}
