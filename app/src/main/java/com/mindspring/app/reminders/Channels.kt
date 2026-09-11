package com.mindspring.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import com.mindspring.app.data.model.AlertSounds

/**
 * The app's notification channels. Android fixes a channel's sound once the channel exists, so a
 * new tone needs a new channel: each id carries a short fingerprint of its sound settings, and
 * channels left over from earlier settings are deleted.
 */
object Channels {
    data class Ids(val daily: String, val task: String, val alarm: String)

    private val Ours = listOf("daily-", "task-", "alarm-")

    /** The single channel used before tones could be chosen. */
    private const val LEGACY = "reminders"

    private val AlarmPattern = longArrayOf(0, 700, 500, 700, 500)

    fun ensure(context: Context, sounds: AlertSounds): Ids {
        val nm = context.getSystemService(NotificationManager::class.java)
        val reminderKey = fingerprint(sounds.reminderTone, sounds.vibrate)
        val ids = Ids(daily = "daily-$reminderKey", task = "task-$reminderKey", alarm = "alarm-${fingerprint(sounds.alarmTone, sounds.vibrate)}")

        val notificationAudio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        // Alarm usage plays on the alarm volume, like the clock app, so it is heard when media is muted.
        val alarmAudio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        nm.createNotificationChannels(
            listOf(
                NotificationChannel(ids.daily, "Daily reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "The evening check-in and the morning task summary"
                    sound(sounds.reminderTone, notificationAudio, sounds.vibrate)
                },
                NotificationChannel(ids.task, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Task alerts and habit reminders, shown at the top of the screen or on the lock screen"
                    sound(sounds.reminderTone, notificationAudio, sounds.vibrate)
                },
                NotificationChannel(ids.alarm, "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Task and habit alerts that keep ringing until answered"
                    sound(sounds.alarmTone, alarmAudio, sounds.vibrate, AlarmPattern)
                },
            ),
        )
        val keep = setOf(ids.daily, ids.task, ids.alarm)
        nm.notificationChannels
            .filter { ch -> ch.id !in keep && (ch.id == LEGACY || Ours.any { ch.id.startsWith(it) }) }
            .forEach { nm.deleteNotificationChannel(it.id) }
        return ids
    }

    private fun NotificationChannel.sound(tone: String?, audio: AudioAttributes, vibrate: Boolean, pattern: LongArray? = null) {
        if (tone == null) setSound(null, null) else setSound(Uri.parse(tone), audio)
        enableVibration(vibrate)
        if (vibrate && pattern != null) vibrationPattern = pattern
    }

    private fun fingerprint(tone: String?, vibrate: Boolean): String =
        "${tone ?: "silent"}|$vibrate".hashCode().toUInt().toString(36)
}
