package com.mindspring.app.ui.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object Fmt {
    private val time = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val dayTime = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.getDefault())
    private val journal = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    private val monthYear = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val weekday = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())

    fun greeting(now: LocalTime = LocalTime.now()): String = when (now.hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    fun time(t: LocalTime): String = t.format(time)

    /** "Today, 9:30 AM", "Yesterday, 8:00 PM" or "Aug 12, 9:00 AM". */
    fun relative(dt: LocalDateTime, today: LocalDate = LocalDate.now()): String = when (dt.toLocalDate()) {
        today -> "Today, ${dt.format(time)}"
        today.minusDays(1) -> "Yesterday, ${dt.format(time)}"
        else -> dt.format(dayTime)
    }

    fun journalDate(dt: LocalDateTime): String = dt.format(journal).uppercase(Locale.getDefault())

    fun monthYear(d: LocalDate): String = d.format(monthYear)

    fun dayMonth(d: LocalDate): String = d.format(dayMonth)

    fun weekdayInitial(d: LocalDate): String = d.format(weekday).take(1)

    fun countdown(totalSeconds: Long): String = "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
