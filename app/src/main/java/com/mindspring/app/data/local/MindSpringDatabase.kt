package com.mindspring.app.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Dates are stored as epoch days and date-times as epoch seconds (in UTC, as a plain clock
 * reading), so SQLite can sort and range-filter them as integers.
 */
class Converters {
    @TypeConverter fun dateToLong(d: LocalDate?): Long? = d?.toEpochDay()
    @TypeConverter fun longToDate(v: Long?): LocalDate? = v?.let(LocalDate::ofEpochDay)
    @TypeConverter fun dateTimeToLong(d: LocalDateTime?): Long? = d?.toEpochSecond(ZoneOffset.UTC)
    @TypeConverter fun longToDateTime(v: Long?): LocalDateTime? = v?.let { LocalDateTime.ofEpochSecond(it, 0, ZoneOffset.UTC) }
}

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun byEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    fun observe(id: Long): Flow<UserEntity?>

    @Query("UPDATE users SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface AreaDao {
    @Query("SELECT * FROM areas WHERE userId = :userId ORDER BY sortOrder, id")
    fun observe(userId: Long): Flow<List<AreaEntity>>

    @Query("SELECT * FROM areas WHERE userId = :userId ORDER BY sortOrder, id")
    suspend fun all(userId: Long): List<AreaEntity>

    @Upsert suspend fun upsert(area: AreaEntity): Long
    @Insert suspend fun insertAll(areas: List<AreaEntity>): List<Long>

    @Query("DELETE FROM areas WHERE id = :id AND userId = :userId")
    suspend fun delete(userId: Long, id: Long)

    @Query("DELETE FROM areas WHERE userId = :userId")
    suspend fun clear(userId: Long)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE userId = :userId ORDER BY archived, name COLLATE NOCASE")
    fun observe(userId: Long): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE userId = :userId")
    suspend fun all(userId: Long): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id AND userId = :userId")
    fun observeOne(userId: Long, id: Long): Flow<ProjectEntity?>

    @Upsert suspend fun upsert(project: ProjectEntity): Long

    @Query("DELETE FROM projects WHERE id = :id AND userId = :userId")
    suspend fun delete(userId: Long, id: Long)

    @Query("DELETE FROM projects WHERE userId = :userId")
    suspend fun clear(userId: Long)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE userId = :userId ORDER BY id")
    fun observe(userId: Long): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE userId = :userId ORDER BY id")
    suspend fun all(userId: Long): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id AND userId = :userId")
    fun observeOne(userId: Long, id: Long): Flow<HabitEntity?>

    @Upsert suspend fun upsert(habit: HabitEntity): Long

    @Query("DELETE FROM habits WHERE id = :id AND userId = :userId")
    suspend fun delete(userId: Long, id: Long)

    @Query("DELETE FROM habits WHERE userId = :userId")
    suspend fun clear(userId: Long)

    @Query("SELECT m.* FROM habit_marks m JOIN habits h ON h.id = m.habitId WHERE h.userId = :userId")
    fun observeMarks(userId: Long): Flow<List<HabitMarkEntity>>

    @Query("SELECT m.* FROM habit_marks m JOIN habits h ON h.id = m.habitId WHERE h.userId = :userId")
    suspend fun allMarks(userId: Long): List<HabitMarkEntity>

    @Upsert suspend fun upsertMark(mark: HabitMarkEntity)
    @Upsert suspend fun upsertMarks(marks: List<HabitMarkEntity>)

    @Query("DELETE FROM habit_marks WHERE habitId = :habitId AND date = :date")
    suspend fun deleteMark(habitId: Long, date: LocalDate)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY id")
    fun observe(userId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY id")
    suspend fun all(userId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id AND userId = :userId")
    fun observeOne(userId: Long, id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id AND userId = :userId")
    suspend fun one(userId: Long, id: Long): TaskEntity?

    @Upsert suspend fun upsert(task: TaskEntity): Long

    @Query("DELETE FROM tasks WHERE id = :id AND userId = :userId")
    suspend fun delete(userId: Long, id: Long)

    @Query("DELETE FROM tasks WHERE userId = :userId")
    suspend fun clear(userId: Long)
}

@Dao
interface MoodDao {
    @Query("SELECT * FROM mood_entries WHERE userId = :userId ORDER BY loggedAt DESC")
    fun observe(userId: Long): Flow<List<MoodEntity>>

    @Query("SELECT * FROM mood_entries WHERE userId = :userId ORDER BY loggedAt DESC")
    suspend fun all(userId: Long): List<MoodEntity>

    @Query("SELECT * FROM mood_entries WHERE id = :id AND userId = :userId")
    fun observeOne(userId: Long, id: Long): Flow<MoodEntity?>

    @Upsert suspend fun upsert(entry: MoodEntity): Long

    @Query("DELETE FROM mood_entries WHERE id = :id AND userId = :userId")
    suspend fun delete(userId: Long, id: Long)

    @Query("DELETE FROM mood_entries WHERE userId = :userId")
    suspend fun clear(userId: Long)
}

@Dao
interface GratitudeDao {
    @Query("SELECT * FROM gratitude_entries WHERE userId = :userId ORDER BY createdAt DESC")
    fun observe(userId: Long): Flow<List<GratitudeEntity>>

    @Query("SELECT * FROM gratitude_entries WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun all(userId: Long): List<GratitudeEntity>

    @Upsert suspend fun upsert(entry: GratitudeEntity): Long

    @Query("DELETE FROM gratitude_entries WHERE id = :id AND userId = :userId")
    suspend fun delete(userId: Long, id: Long)

    @Query("DELETE FROM gratitude_entries WHERE userId = :userId")
    suspend fun clear(userId: Long)
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY date DESC")
    fun observe(userId: Long): Flow<List<JournalEntity>>

    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY date DESC")
    suspend fun all(userId: Long): List<JournalEntity>

    @Query("SELECT * FROM journal_entries WHERE userId = :userId AND date = :date")
    fun observeOne(userId: Long, date: LocalDate): Flow<JournalEntity?>

    @Upsert suspend fun upsert(entry: JournalEntity)

    @Query("DELETE FROM journal_entries WHERE userId = :userId AND date = :date")
    suspend fun delete(userId: Long, date: LocalDate)

    @Query("DELETE FROM journal_entries WHERE userId = :userId")
    suspend fun clear(userId: Long)
}

@Database(
    entities = [
        UserEntity::class, AreaEntity::class, ProjectEntity::class, HabitEntity::class, HabitMarkEntity::class,
        TaskEntity::class, MoodEntity::class, GratitudeEntity::class, JournalEntity::class,
    ],
    version = 3,
    exportSchema = true,
    // Room derives the ALTER TABLEs from the saved schemas.
    // 2: tasks gain an alert time and style. 3: habit reminders gain a style.
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
@TypeConverters(Converters::class)
abstract class MindSpringDatabase : RoomDatabase() {
    abstract fun users(): UserDao
    abstract fun areas(): AreaDao
    abstract fun projects(): ProjectDao
    abstract fun habits(): HabitDao
    abstract fun tasks(): TaskDao
    abstract fun moods(): MoodDao
    abstract fun gratitude(): GratitudeDao
    abstract fun journal(): JournalDao

    companion object {
        fun build(context: Context): MindSpringDatabase =
            Room.databaseBuilder(context, MindSpringDatabase::class.java, "mindspring.db").build()

        /** For tests: a fresh database that lives only in memory. */
        fun inMemory(context: Context): MindSpringDatabase =
            Room.inMemoryDatabaseBuilder(context, MindSpringDatabase::class.java).allowMainThreadQueries().build()
    }
}
