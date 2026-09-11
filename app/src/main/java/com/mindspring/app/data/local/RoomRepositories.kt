package com.mindspring.app.data.local

import androidx.room.withTransaction
import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.model.JournalEntry
import com.mindspring.app.data.model.LifeArea
import com.mindspring.app.data.model.MarkState
import com.mindspring.app.data.model.MoodEntry
import com.mindspring.app.data.model.Project
import com.mindspring.app.data.model.Task
import com.mindspring.app.data.model.TaskStatus
import com.mindspring.app.data.model.User
import com.mindspring.app.data.repository.AreaRepository
import com.mindspring.app.data.repository.AuthRepository
import com.mindspring.app.data.repository.AuthResult
import com.mindspring.app.data.repository.DataReset
import com.mindspring.app.data.repository.GratitudeRepository
import com.mindspring.app.data.repository.HabitRepository
import com.mindspring.app.data.repository.JournalRepository
import com.mindspring.app.data.repository.MoodRepository
import com.mindspring.app.data.repository.SettingsRepository
import com.mindspring.app.data.repository.TaskRepository
import com.mindspring.app.domain.PasswordHasher
import com.mindspring.app.domain.TaskLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

/** Resolves the signed-in account for every repository, so no query can read another user's rows. */
class Session(settings: SettingsRepository) {
    val userId: Flow<Long?> = settings.sessionUserId

    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T> scoped(empty: T, query: (Long) -> Flow<T>): Flow<T> =
        userId.flatMapLatest { id -> if (id == null) flowOf(empty) else query(id) }

    suspend fun id(): Long = userId.first() ?: throw IllegalStateException("No user is signed in")
}

/** The life areas a new account starts with. Renamed, recoloured or removed in Life Areas. */
object DefaultAreas {
    val names = listOf(
        "Health & Well-being",
        "Spirituality & Values",
        "Relationships",
        "Study & Research",
        "Career & Growth",
        "Creativity & Contribution",
        "Finance & Security",
        "Lifestyle & Home",
    )

    suspend fun seed(db: MindSpringDatabase, userId: Long): List<Long> =
        db.areas().insertAll(names.mapIndexed { i, n -> AreaEntity(0, userId, n, colorIndex = i, sortOrder = i) })
}

class RoomAuthRepository(
    private val db: MindSpringDatabase,
    private val settings: SettingsRepository,
    session: Session,
) : AuthRepository {

    override val currentUser: Flow<User?> =
        session.scoped<User?>(null) { id -> db.users().observe(id).map { it?.toModel() } }

    override suspend fun register(name: String, email: String, password: String): AuthResult {
        val key = email.trim().lowercase()
        if (key == DemoData.EMAIL || db.users().byEmail(key) != null) {
            return AuthResult.Error("An account with this email already exists.")
        }
        val hash = withContext(Dispatchers.Default) { PasswordHasher.hash(password) }
        val user = db.withTransaction {
            val id = db.users().insert(UserEntity(name = name.trim(), email = key, passwordHash = hash, memberSince = LocalDate.now()))
            DefaultAreas.seed(db, id)
            UserEntity(id, name.trim(), key, hash, LocalDate.now())
        }
        settings.setSessionUserId(user.id)
        return AuthResult.Success(user.toModel())
    }

    override suspend fun login(email: String, password: String): AuthResult {
        val account = db.users().byEmail(email.trim().lowercase())
        val ok = account != null && withContext(Dispatchers.Default) { PasswordHasher.verify(password, account.passwordHash) }
        if (!ok) return AuthResult.Error("Incorrect email or password.")
        settings.setSessionUserId(account!!.id)
        return AuthResult.Success(account.toModel())
    }

    override suspend fun loginDemo(): AuthResult {
        val existing = db.users().byEmail(DemoData.EMAIL)
        val user = existing ?: run {
            val hash = withContext(Dispatchers.Default) { PasswordHasher.hash(DemoData.PASSWORD) }
            db.withTransaction {
                val since = LocalDate.now().minusDays(DemoData.HISTORY_DAYS)
                val id = db.users().insert(UserEntity(name = DemoData.NAME, email = DemoData.EMAIL, passwordHash = hash, memberSince = since))
                DemoData.seed(db, id, LocalDate.now())
                UserEntity(id, DemoData.NAME, DemoData.EMAIL, hash, since)
            }
        }
        settings.setSessionUserId(user.id)
        return AuthResult.Success(user.toModel())
    }

    override suspend fun logout() = settings.setSessionUserId(null)

    override suspend fun updateName(name: String) {
        val id = settings.sessionUserId.first() ?: return
        db.users().rename(id, name.trim())
    }
}

class RoomAreaRepository(private val db: MindSpringDatabase, private val session: Session) : AreaRepository {
    override val areas = session.scoped(emptyList()) { id -> db.areas().observe(id).map { list -> list.map { it.toModel() } } }

    override suspend fun upsert(area: LifeArea): Long {
        val userId = session.id()
        val sort = if (area.id == 0L) (db.areas().all(userId).maxOfOrNull { it.sortOrder } ?: -1) + 1 else area.sortOrder
        val rowId = db.areas().upsert(AreaEntity.from(area.copy(name = area.name.trim(), sortOrder = sort), userId))
        return if (area.id != 0L) area.id else rowId
    }

    override suspend fun delete(id: Long) = db.areas().delete(session.id(), id)
}

class RoomHabitRepository(private val db: MindSpringDatabase, private val session: Session) : HabitRepository {
    override val habits = session.scoped(emptyList()) { id -> db.habits().observe(id).map { list -> list.map { it.toModel() } } }
    override val marks = session.scoped(emptyList()) { id -> db.habits().observeMarks(id).map { list -> list.map { it.toModel() } } }

    override fun habit(id: Long): Flow<Habit?> =
        session.scoped<Habit?>(null) { user -> db.habits().observeOne(user, id).map { it?.toModel() } }

    override suspend fun upsert(habit: Habit): Long {
        val rowId = db.habits().upsert(HabitEntity.from(habit, session.id()))
        return if (habit.id != 0L) habit.id else rowId
    }

    override suspend fun delete(id: Long) = db.habits().delete(session.id(), id)

    override suspend fun setMark(habitId: Long, date: LocalDate, state: MarkState?) {
        if (state == null) db.habits().deleteMark(habitId, date)
        else db.habits().upsertMark(HabitMarkEntity(habitId, date, state.name))
    }
}

class RoomTaskRepository(private val db: MindSpringDatabase, private val session: Session) : TaskRepository {
    override val tasks = session.scoped(emptyList()) { id -> db.tasks().observe(id).map { list -> list.map { it.toModel() } } }
    override val projects = session.scoped(emptyList()) { id -> db.projects().observe(id).map { list -> list.map { it.toModel() } } }

    override fun task(id: Long): Flow<Task?> =
        session.scoped<Task?>(null) { user -> db.tasks().observeOne(user, id).map { it?.toModel() } }

    override fun project(id: Long): Flow<Project?> =
        session.scoped<Project?>(null) { user -> db.projects().observeOne(user, id).map { it?.toModel() } }

    override suspend fun upsert(task: Task): Long {
        val userId = session.id()
        return db.withTransaction {
            val before = if (task.id != 0L) db.tasks().one(userId, task.id)?.toModel() else null
            val rowId = db.tasks().upsert(TaskEntity.from(task, userId))
            // Saving a repeating task as Done from the editor must roll it forward too.
            if (before != null && before.status != TaskStatus.Done && task.status == TaskStatus.Done) {
                TaskLogic.nextOccurrence(task, task.doneOn ?: LocalDate.now())?.let { db.tasks().upsert(TaskEntity.from(it, userId)) }
            }
            if (task.id != 0L) task.id else rowId
        }
    }

    override suspend fun setStatus(id: Long, status: TaskStatus, today: LocalDate) {
        val userId = session.id()
        db.withTransaction {
            val task = db.tasks().one(userId, id)?.toModel() ?: return@withTransaction
            if (task.status == status) return@withTransaction
            val doneOn = if (status == TaskStatus.Done) today else null
            db.tasks().upsert(TaskEntity.from(task.copy(status = status, doneOn = doneOn), userId))
            if (status == TaskStatus.Done) {
                TaskLogic.nextOccurrence(task, today)?.let { db.tasks().upsert(TaskEntity.from(it, userId)) }
            }
        }
    }

    override suspend fun delete(id: Long) = db.tasks().delete(session.id(), id)

    override suspend fun upsertProject(project: Project): Long {
        val rowId = db.projects().upsert(ProjectEntity.from(project, session.id()))
        return if (project.id != 0L) project.id else rowId
    }

    override suspend fun deleteProject(id: Long) = db.projects().delete(session.id(), id)
}

class RoomMoodRepository(private val db: MindSpringDatabase, private val session: Session) : MoodRepository {
    override val entries = session.scoped(emptyList()) { id -> db.moods().observe(id).map { list -> list.map { it.toModel() } } }

    override fun entry(id: Long): Flow<MoodEntry?> =
        session.scoped<MoodEntry?>(null) { user -> db.moods().observeOne(user, id).map { it?.toModel() } }

    /** Also used by Undo after a delete: an entry keeps its id, so it is re-inserted as it was. */
    override suspend fun upsert(entry: MoodEntry): Long {
        val rowId = db.moods().upsert(MoodEntity.from(entry, session.id()))
        return if (entry.id != 0L) entry.id else rowId
    }

    override suspend fun delete(id: Long) = db.moods().delete(session.id(), id)
}

class RoomGratitudeRepository(private val db: MindSpringDatabase, private val session: Session) : GratitudeRepository {
    override val entries = session.scoped(emptyList()) { id -> db.gratitude().observe(id).map { list -> list.map { it.toModel() } } }

    override suspend fun add(text: String) {
        db.gratitude().upsert(GratitudeEntity(userId = session.id(), text = text.trim(), createdAt = LocalDateTime.now()))
    }

    override suspend fun delete(id: Long) = db.gratitude().delete(session.id(), id)
}

class RoomJournalRepository(private val db: MindSpringDatabase, private val session: Session) : JournalRepository {
    override val entries = session.scoped(emptyList()) { id -> db.journal().observe(id).map { list -> list.map { it.toModel() } } }

    override fun entry(date: LocalDate): Flow<JournalEntry?> =
        session.scoped<JournalEntry?>(null) { user -> db.journal().observeOne(user, date).map { it?.toModel() } }

    override suspend fun upsert(entry: JournalEntry) =
        db.journal().upsert(JournalEntity.from(entry.copy(updatedAt = LocalDateTime.now()), session.id()))

    override suspend fun delete(date: LocalDate) = db.journal().delete(session.id(), date)
}

/** Clears everything the user has logged but keeps the account and its life areas. */
class RoomDataReset(private val db: MindSpringDatabase, private val session: Session) : DataReset {
    override suspend fun clearUserData() {
        val id = session.id()
        db.withTransaction {
            db.tasks().clear(id)
            db.projects().clear(id)
            db.habits().clear(id)
            db.moods().clear(id)
            db.gratitude().clear(id)
            db.journal().clear(id)
        }
    }
}
