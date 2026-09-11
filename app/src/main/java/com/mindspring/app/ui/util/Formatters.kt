package com.mindspring.app.ui.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object Fmt {
    private val time = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val dayTime = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.getDefault())
    private val journal = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    private val monthYear = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
    private val monthFull = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val monthShort = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val shortDay = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
    private val longDay = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
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

    fun month(m: YearMonth): String = m.format(monthFull)

    fun monthShort(m: YearMonth): String = m.format(monthShort)

    fun dayMonth(d: LocalDate): String = d.format(dayMonth)

    /** "Fri 12 Sep". */
    fun shortDay(d: LocalDate): String = d.format(shortDay)

    /** "Friday, 12 September". */
    fun longDay(d: LocalDate): String = d.format(longDay)

    fun weekdayInitial(d: LocalDate): String = d.format(weekday).take(1)

    fun weekdayShort(d: LocalDate): String = d.format(weekday)

    /** "Today", "Tomorrow", "Yesterday" or "Fri 12 Sep". */
    fun friendlyDay(d: LocalDate, today: LocalDate = LocalDate.now()): String = when (d) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> shortDay(d)
    }

    /** "Today, 9:00 AM", "Tomorrow, 9:00 AM" or "Fri 18 Sep, 9:00 AM". */
    fun alert(dt: LocalDateTime, today: LocalDate = LocalDate.now()): String = "${friendlyDay(dt.toLocalDate(), today)}, ${time(dt.toLocalTime())}"

    /** "due in 3 days", "due today", "2 days late". */
    fun dueIn(due: LocalDate, today: LocalDate = LocalDate.now()): String {
        val days = ChronoUnit.DAYS.between(today, due)
        return when {
            days == 0L -> "due today"
            days == 1L -> "due tomorrow"
            days > 1 -> "due in $days days"
            days == -1L -> "1 day late"
            else -> "${abs(days)} days late"
        }
    }

    fun percent(rate: Float): String = "${(rate * 100).roundToInt()}%"

    fun countdown(totalSeconds: Long): String = "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)

    fun oneDecimal(v: Float): String = "%.1f".format(v)
}
