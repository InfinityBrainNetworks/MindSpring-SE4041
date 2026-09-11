package com.mindspring.app.data.local

import androidx.room.withTransaction
import com.mindspring.app.data.model.AlertStyle
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

/** What an import brought in, for the confirmation message. */
data class ImportSummary(val habits: Int, val tasks: Int, val moods: Int, val journal: Int)

/**
 * Exports the signed-in user's data as one JSON document and restores it again. The data lives
 * only on this phone and is lost on uninstall, so this file is the user's backup.
 *
 * Row ids are written as they are and remapped on import, so a backup can be restored into any
 * account, including a fresh one after a reinstall.
 */
class Backup(private val db: MindSpringDatabase, private val session: Session) {

    suspend fun export(): String {
        val userId = session.id()
        val root = JSONObject()
            .put("app", "MindSpring")
            .put("format", FORMAT)
            .put("exportedAt", LocalDateTime.now().toString())

        root.put("areas", JSONArray(db.areas().all(userId).map { a ->
            JSONObject().put("id", a.id).put("name", a.name).put("color", a.colorIndex).put("sort", a.sortOrder)
        }))
        root.put("projects", JSONArray(db.projects().all(userId).map { p ->
            JSONObject().put("id", p.id).put("name", p.name).putOpt("areaId", p.areaId).put("subArea", p.subArea)
                .put("color", p.colorIndex).put("notes", p.notes).put("archived", p.archived).put("createdAt", p.createdAt.toString())
        }))
        root.put("habits", JSONArray(db.habits().all(userId).map { h ->
            JSONObject().put("id", h.id).put("name", h.name).putOpt("areaId", h.areaId).put("subArea", h.subArea)
                .put("icon", h.icon).put("frequency", h.frequency).put("daysMask", h.daysMask).put("target", h.target)
                .put("reminderEnabled", h.reminderEnabled).put("reminderMinute", h.reminderMinute)
                .put("active", h.active).put("createdAt", h.createdAt.toString()).put("reminderStyle", h.reminderStyle)
        }))
        root.put("marks", JSONArray(db.habits().allMarks(userId).map { m ->
            JSONObject().put("habitId", m.habitId).put("date", m.date.toString()).put("state", m.state)
        }))
        root.put("tasks", JSONArray(db.tasks().all(userId).map { t ->
            JSONObject().put("id", t.id).put("title", t.title).put("notes", t.notes).putOpt("areaId", t.areaId)
                .put("subArea", t.subArea).putOpt("projectId", t.projectId)
                .putOpt("start", t.startDate?.toString()).putOpt("due", t.dueDate?.toString())
                .put("priority", t.priority).put("status", t.status).putOpt("doneOn", t.doneOn?.toString())
                .put("repeat", t.repeat).put("createdAt", t.createdAt.toString())
                .putOpt("alertAt", t.alertAt?.toString()).put("alertStyle", t.alertStyle)
        }))
        root.put("moods", JSONArray(db.moods().all(userId).map { m ->
            JSONObject().put("rating", m.rating).put("feelings", m.feelings).put("note", m.note).put("loggedAt", m.loggedAt.toString())
        }))
        root.put("gratitude", JSONArray(db.gratitude().all(userId).map { g ->
            JSONObject().put("text", g.text).put("createdAt", g.createdAt.toString())
        }))
        root.put("journal", JSONArray(db.journal().all(userId).map { j ->
            JSONObject().put("date", j.date.toString()).putOpt("rating", j.rating).putOpt("energy", j.energy)
                .put("highlight", j.highlight).put("monologue", j.monologue).put("tomorrowTop", j.tomorrowTop)
                .put("updatedAt", j.updatedAt.toString())
        }))
        return root.toString(2)
    }

    /** Replaces everything the signed-in user has with the backup's contents. */
    suspend fun import(json: String): ImportSummary {
        val root = JSONObject(json)
        require(root.optString("app") == "MindSpring") { "This file is not a MindSpring backup." }
        val userId = session.id()

        return db.withTransaction {
            db.tasks().clear(userId)
            db.projects().clear(userId)
            db.habits().clear(userId)
            db.moods().clear(userId)
            db.gratitude().clear(userId)
            db.journal().clear(userId)
            db.areas().clear(userId)

            val areaIds = mutableMapOf<Long, Long>()
            root.objects("areas").forEach { o ->
                areaIds[o.getLong("id")] = db.areas().upsert(AreaEntity(0, userId, o.getString("name"), o.optInt("color"), o.optInt("sort")))
            }
            fun area(o: JSONObject) = o.optLongOrNull("areaId")?.let(areaIds::get)

            val projectIds = mutableMapOf<Long, Long>()
            root.objects("projects").forEach { o ->
                projectIds[o.getLong("id")] = db.projects().upsert(
                    ProjectEntity(
                        0, userId, o.getString("name"), area(o), o.optString("subArea"), o.optInt("color"),
                        o.optString("notes"), o.optBoolean("archived"), LocalDate.parse(o.getString("createdAt")),
                    ),
                )
            }

            val habitIds = mutableMapOf<Long, Long>()
            root.objects("habits").forEach { o ->
                habitIds[o.getLong("id")] = db.habits().upsert(
                    HabitEntity(
                        0, userId, o.getString("name"), area(o), o.optString("subArea"), o.optString("icon"),
                        o.optString("frequency"), o.optInt("daysMask", 127), o.optString("target"),
                        o.optBoolean("reminderEnabled"), o.optInt("reminderMinute", 480), o.optBoolean("active", true),
                        LocalDate.parse(o.getString("createdAt")), o.optString("reminderStyle", AlertStyle.Reminder.name),
                    ),
                )
            }
            db.habits().upsertMarks(root.objects("marks").mapNotNull { o ->
                habitIds[o.getLong("habitId")]?.let { HabitMarkEntity(it, LocalDate.parse(o.getString("date")), o.getString("state")) }
            })

            val tasks = root.objects("tasks")
            tasks.forEach { o ->
                db.tasks().upsert(
                    TaskEntity(
                        0, userId, o.getString("title"), o.optString("notes"), area(o), o.optString("subArea"),
                        o.optLongOrNull("projectId")?.let(projectIds::get),
                        o.optDate("start"), o.optDate("due"), o.optString("priority"), o.optString("status"),
                        o.optDate("doneOn"), o.optString("repeat"), LocalDate.parse(o.getString("createdAt")),
                        // Older backups have no alert fields; they restore without alerts.
                        if (o.isNull("alertAt")) null else LocalDateTime.parse(o.getString("alertAt")),
                        o.optString("alertStyle", AlertStyle.Reminder.name),
                    ),
                )
            }
            val moods = root.objects("moods")
            moods.forEach { o ->
                db.moods().upsert(MoodEntity(0, userId, o.getInt("rating"), o.optString("feelings"), o.optString("note"), LocalDateTime.parse(o.getString("loggedAt"))))
            }
            root.objects("gratitude").forEach { o ->
                db.gratitude().upsert(GratitudeEntity(0, userId, o.getString("text"), LocalDateTime.parse(o.getString("createdAt"))))
            }
            val journal = root.objects("journal")
            journal.forEach { o ->
                db.journal().upsert(
                    JournalEntity(
                        userId, LocalDate.parse(o.getString("date")), o.optIntOrNull("rating"), o.optIntOrNull("energy"),
                        o.optString("highlight"), o.optString("monologue"), o.optString("tomorrowTop"),
                        LocalDateTime.parse(o.getString("updatedAt")),
                    ),
                )
            }
            ImportSummary(habitIds.size, tasks.size, moods.size, journal.size)
        }
    }

    private fun JSONObject.objects(key: String): List<JSONObject> {
        val array = optJSONArray(key) ?: return emptyList()
        return (0 until array.length()).map { array.getJSONObject(it) }
    }

    private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key)) null else optLong(key)
    private fun JSONObject.optIntOrNull(key: String): Int? = if (isNull(key)) null else optInt(key)
    private fun JSONObject.optDate(key: String): LocalDate? = if (isNull(key)) null else LocalDate.parse(getString(key))

    companion object {
        const val FORMAT = 1
    }
}
