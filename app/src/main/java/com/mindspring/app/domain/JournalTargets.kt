package com.mindspring.app.domain

/** How long a day's monologue is against the ~150 word aim (the workbook's Status column). */
enum class JournalStatus(val label: String) {
    NotWritten("Not written"),
    TooShort("A little short"),
    Good("Good length"),
    TooLong("Running long"),
}

object JournalTargets {
    const val GOAL = 150
    const val LOW = 120
    const val HIGH = 180

    fun status(words: Int): JournalStatus = when {
        words == 0 -> JournalStatus.NotWritten
        words < LOW -> JournalStatus.TooShort
        words > HIGH -> JournalStatus.TooLong
        else -> JournalStatus.Good
    }
}
