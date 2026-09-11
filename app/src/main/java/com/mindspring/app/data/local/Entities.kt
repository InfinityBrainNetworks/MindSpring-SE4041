package com.mindspring.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mindspring.app.data.model.GratitudeEntry
import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.model.HabitFrequency
import com.mindspring.app.data.model.HabitIcon
import com.mindspring.app.data.model.HabitMark
import com.mindspring.app.data.model.JournalEntry
import com.mindspring.app.data.model.LifeArea
import com.mindspring.app.data.model.MarkState
import com.mindspring.app.data.model.Mood
import com.mindspring.app.data.model.MoodEntry
import com.mindspring.app.data.model.Priority
import com.mindspring.app.data.model.Project
import com.mindspring.app.data.model.Repeat
import com.mindspring.app.data.model.Task
import com.mindspring.app.data.model.TaskStatus
import com.mindspring.app.data.model.User
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// Every row belongs to a user and is deleted with them (ON DELETE CASCADE). Enums are stored by
// name so reordering an enum in code can never reinterpret stored data.

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Stored lower-cased and trimmed; the unique index makes one account per address. */
    val email: String,
    /** PBKDF2 `iterations:salt:hash`, never the password itself. */
    val passwordHash: String,
    val memberSince: LocalDate,
) {
    fun toModel() = User(id, name, email, memberSince)
}

@Entity(
    tableName = "areas",
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId")],
)
data class AreaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val name: String,
    val colorIndex: Int,
    val sortOrder: Int,
) {
    fun toModel() = LifeArea(id, name, colorIndex, sortOrder)

    companion object {
        fun from(a: LifeArea, userId: Long) = AreaEntity(a.id, userId, a.name, a.colorIndex, a.sortOrder)
    }
}

@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(UserEntity::class, ["id"], ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(AreaEntity::class, ["id"], ["areaId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("userId"), Index("areaId")],
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val name: String,
    val areaId: Long?,
    val subArea: String,
    val colorIndex: Int,
    val notes: String,
    val archived: Boolean,
    val createdAt: LocalDate,
) {
    fun toModel() = Project(id, name, areaId, subArea, colorIndex, notes, archived, createdAt)

    companion object {
        fun from(p: Project, userId: Long) =
            ProjectEntity(p.id, userId, p.name.trim(), p.areaId, p.subArea.trim(), p.colorIndex, p.notes, p.archived, p.createdAt)
    }
}

@Entity(
    tableName = "habits",
    foreignKeys = [
        ForeignKey(UserEntity::class, ["id"], ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(AreaEntity::class, ["id"], ["areaId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("userId"), Index("areaId")],
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val name: String,
    val areaId: Long?,
    val subArea: String,
    val icon: String,
    val frequency: String,
    /** Bit n set = DayOfWeek with value n+1 (Monday = bit 0). */
    val daysMask: Int,
    val target: String,
    val reminderEnabled: Boolean,
    /** Minutes after midnight. */
    val reminderMinute: Int,
    val active: Boolean,
    val createdAt: LocalDate,
) {
    fun toModel() = Habit(
        id = id,
        name = name,
        areaId = areaId,
        subArea = subArea,
        icon = enumOr(icon, HabitIcon.Spa),
        frequency = enumOr(frequency, HabitFrequency.Daily),
        customDays = daysFromMask(daysMask),
        target = target,
        reminderEnabled = reminderEnabled,
        reminderTime = LocalTime.of(reminderMinute / 60, reminderMinute % 60),
        active = active,
        createdAt = createdAt,
    )

    companion object {
        fun from(h: Habit, userId: Long) = HabitEntity(
            id = h.id,
            userId = userId,
            name = h.name.trim(),
            areaId = h.areaId,
            subArea = h.subArea.trim(),
            icon = h.icon.name,
            frequency = h.frequency.name,
            daysMask = maskFromDays(h.customDays),
            target = h.target.trim(),
            reminderEnabled = h.reminderEnabled,
            reminderMinute = h.reminderTime.hour * 60 + h.reminderTime.minute,
            active = h.active,
            createdAt = h.createdAt,
        )
    }
}

@Entity(
    tableName = "habit_marks",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [ForeignKey(HabitEntity::class, ["id"], ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("date")],
)
data class HabitMarkEntity(
    val habitId: Long,
    val date: LocalDate,
    val state: String,
) {
    fun toModel() = HabitMark(habitId, date, enumOr(state, MarkState.Done))
}

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(UserEntity::class, ["id"], ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(AreaEntity::class, ["id"], ["areaId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(ProjectEntity::class, ["id"], ["projectId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("userId"), Index("areaId"), Index("projectId"), Index("dueDate")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val title: String,
    val notes: String,
    val areaId: Long?,
    val subArea: String,
    val projectId: Long?,
    val startDate: LocalDate?,
    val dueDate: LocalDate?,
    val priority: String,
    val status: String,
    val doneOn: LocalDate?,
    @ColumnInfo(name = "repeatRule") val repeat: String,
    val createdAt: LocalDate,
) {
    fun toModel() = Task(
        id = id,
        title = title,
        notes = notes,
        areaId = areaId,
        subArea = subArea,
        projectId = projectId,
        start = startDate,
        due = dueDate,
        priority = enumOr(priority, Priority.B),
        status = enumOr(status, TaskStatus.NotStarted),
        doneOn = doneOn,
        repeat = enumOr(repeat, Repeat.None),
        createdAt = createdAt,
    )

    companion object {
        fun from(t: Task, userId: Long) = TaskEntity(
            id = t.id,
            userId = userId,
            title = t.title.trim(),
            notes = t.notes.trim(),
            areaId = t.areaId,
            subArea = t.subArea.trim(),
            projectId = t.projectId,
            startDate = t.start,
            dueDate = t.due,
            priority = t.priority.name,
            status = t.status.name,
            doneOn = t.doneOn,
            repeat = t.repeat.name,
            createdAt = t.createdAt,
        )
    }
}

@Entity(
    tableName = "mood_entries",
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId"), Index("loggedAt")],
)
data class MoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val rating: Int,
    /** Feeling tags joined with '|'; tags are fixed words that never contain it. */
    val feelings: String,
    val note: String,
    val loggedAt: LocalDateTime,
) {
    fun toModel() = MoodEntry(id, Mood.fromRating(rating), feelings.split('|').filter { it.isNotBlank() }, note, loggedAt)

    companion object {
        fun from(e: MoodEntry, userId: Long) =
            MoodEntity(e.id, userId, e.mood.rating, e.feelings.joinToString("|"), e.note, e.loggedAt)
    }
}

@Entity(
    tableName = "gratitude_entries",
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId")],
)
data class GratitudeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val text: String,
    val createdAt: LocalDateTime,
) {
    fun toModel() = GratitudeEntry(id, text, createdAt)
}

@Entity(
    tableName = "journal_entries",
    primaryKeys = ["userId", "date"],
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["userId"], onDelete = ForeignKey.CASCADE)],
)
data class JournalEntity(
    val userId: Long,
    val date: LocalDate,
    val rating: Int?,
    val energy: Int?,
    val highlight: String,
    val monologue: String,
    val tomorrowTop: String,
    val updatedAt: LocalDateTime,
) {
    fun toModel() = JournalEntry(date, rating, energy, highlight, monologue, tomorrowTop, updatedAt)

    companion object {
        fun from(e: JournalEntry, userId: Long) =
            JournalEntity(userId, e.date, e.rating, e.energy, e.highlight.trim(), e.monologue.trim(), e.tomorrowTop.trim(), e.updatedAt)
    }
}

internal inline fun <reified E : Enum<E>> enumOr(name: String, fallback: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: fallback

internal fun maskFromDays(days: Set<DayOfWeek>): Int = days.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }

internal fun daysFromMask(mask: Int): Set<DayOfWeek> = DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()
