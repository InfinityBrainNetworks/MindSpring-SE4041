package com.mindspring.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mindspring.app.TestApp
import com.mindspring.app.data.local.MindSpringDatabase
import com.mindspring.app.data.model.AlertStyle
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * A phone that installed version 1 has a database in the version 1 shape. Build exactly that from
 * the exported schema, put a task in it, and check the current app opens it with the task intact.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = TestApp::class)
class MigrationTest {

    @Test fun version1DatabaseUpgradesWithItsTasks() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.getDatabasePath("upgrade-test.db").apply { parentFile?.mkdirs(); delete() }
        val schema = JSONObject(File("schemas/com.mindspring.app.data.local.MindSpringDatabase/1.json").readText()).getJSONObject("database")

        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.execSQL("INSERT INTO users (id, name, email, passwordHash, memberSince) VALUES (1, 'A', 'a@b.co', 'x', 20000)")
            db.execSQL(
                "INSERT INTO tasks (id, userId, title, notes, subArea, priority, status, repeatRule, createdAt) " +
                    "VALUES (7, 1, 'Written by version 1', '', '', 'A', 'NotStarted', 'None', 20000)",
            )
            db.version = 1
        }

        val room = Room.databaseBuilder(context, MindSpringDatabase::class.java, file.absolutePath).allowMainThreadQueries().build()
        val task = room.tasks().one(1, 7)!!.toModel()
        assertEquals("Written by version 1", task.title)
        assertNull(task.alertAt)
        assertEquals(AlertStyle.Reminder, task.alertStyle)
        room.close()
    }
}
