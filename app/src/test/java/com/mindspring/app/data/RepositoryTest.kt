package com.mindspring.app.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mindspring.app.MindSpringApp
import com.mindspring.app.TestApp
import com.mindspring.app.data.local.DefaultAreas
import com.mindspring.app.data.model.Habit
import com.mindspring.app.data.model.JournalEntry
import com.mindspring.app.data.model.MarkState
import com.mindspring.app.data.model.Project
import com.mindspring.app.data.model.Repeat
import com.mindspring.app.data.model.Task
import com.mindspring.app.data.model.TaskStatus
import com.mindspring.app.data.repository.AuthResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

/** The Room repositories end to end, on an in-memory database. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = TestApp::class)
class RepositoryTest {
    private val app get() = ApplicationProvider.getApplicationContext<MindSpringApp>().container
    private val today = LocalDate.now()

    @Test fun registerSeedsAreasAndSignsIn() = runBlocking {
        val result = app.auth.register("Nimal Perera", "Nimal@Example.com ", "secret1")
        assertTrue(result is AuthResult.Success)
        assertEquals("nimal@example.com", app.auth.currentUser.first()!!.email)
        assertEquals(DefaultAreas.names, app.areas.areas.first().map { it.name })
        // The same address, in any case, cannot register twice.
        assertTrue(app.auth.register("Other", "NIMAL@example.com", "secret1") is AuthResult.Error)
    }

    @Test fun loginChecksThePasswordHash() = runBlocking {
        app.auth.register("A", "a@b.co", "right-one")
        app.auth.logout()
        assertNull(app.auth.currentUser.first())
        assertTrue(app.auth.login("a@b.co", "wrong-one") is AuthResult.Error)
        assertTrue(app.auth.login("a@b.co", "right-one") is AuthResult.Success)
    }

    @Test fun eachAccountSeesOnlyItsOwnData() = runBlocking {
        app.auth.register("One", "one@x.co", "secret1")
        app.habits.upsert(Habit(name = "Mine", createdAt = today))
        app.auth.register("Two", "two@x.co", "secret1")
        assertTrue(app.habits.habits.first().isEmpty())
        app.auth.logout()
        app.auth.login("one@x.co", "secret1")
        assertEquals(listOf("Mine"), app.habits.habits.first().map { it.name })
    }

    @Test fun marksCanBeSetSkippedAndCleared() = runBlocking {
        app.auth.register("A", "m@x.co", "secret1")
        val id = app.habits.upsert(Habit(name = "Water", createdAt = today))
        app.habits.setMark(id, today, MarkState.Done)
        app.habits.setMark(id, today.minusDays(1), MarkState.Skipped)
        assertEquals(2, app.habits.marks.first().size)
        app.habits.setMark(id, today, null)
        assertEquals(MarkState.Skipped, app.habits.marks.first().single().state)
        // Deleting the habit takes its history with it.
        app.habits.delete(id)
        assertTrue(app.habits.marks.first().isEmpty())
    }

    @Test fun finishingARepeatingTaskSchedulesTheNextOne() = runBlocking {
        app.auth.register("A", "t@x.co", "secret1")
        val id = app.tasks.upsert(Task(title = "Tutoring", start = today, due = today, repeat = Repeat.Weekly, createdAt = today))
        app.tasks.setStatus(id, TaskStatus.Done, today)
        val all = app.tasks.tasks.first()
        assertEquals(2, all.size)
        assertEquals(today, all.first { it.id == id }.doneOn)
        assertEquals(today.plusWeeks(1), all.first { it.id != id }.due)
        // Reopening clears the done date again.
        app.tasks.setStatus(id, TaskStatus.InProgress, today)
        assertNull(app.tasks.task(id).first()!!.doneOn)
    }

    @Test fun deletingAProjectKeepsItsTasks() = runBlocking {
        app.auth.register("A", "p@x.co", "secret1")
        val project = app.tasks.upsertProject(Project(name = "Thesis", createdAt = today))
        val task = app.tasks.upsert(Task(title = "Outline", projectId = project, createdAt = today))
        app.tasks.deleteProject(project)
        assertNull(app.tasks.task(task).first()!!.projectId)
    }

    @Test fun backupRoundTripsIntoAFreshAccount() = runBlocking {
        app.auth.loginDemo()
        val before = Triple(app.habits.habits.first().size, app.tasks.tasks.first().size, app.journal.entries.first().size)
        val marks = app.habits.marks.first().size
        val alerts = app.tasks.tasks.first().mapNotNull { t -> t.alertAt?.let { t.title to (it to t.alertStyle) } }.toSet()
        val json = app.backup.export()

        app.auth.register("Fresh", "fresh@x.co", "secret1")
        assertTrue(app.habits.habits.first().isEmpty())
        val summary = app.backup.import(json)

        assertEquals(before, Triple(summary.habits, summary.tasks, summary.journal))
        assertEquals(before.first, app.habits.habits.first().size)
        assertEquals(marks, app.habits.marks.first().size)
        val projects = app.tasks.projects.first()
        assertEquals(4, projects.size)
        // Tasks still point at their (renumbered) projects.
        assertTrue(app.tasks.tasks.first().filter { it.projectId != null }.all { t -> projects.any { it.id == t.projectId } })
        // Alerts come back with their times and styles.
        assertEquals(2, alerts.size)
        assertEquals(alerts, app.tasks.tasks.first().mapNotNull { t -> t.alertAt?.let { t.title to (it to t.alertStyle) } }.toSet())
    }

    @Test fun journalEntryIsOnePerDay() = runBlocking {
        app.auth.register("A", "j@x.co", "secret1")
        app.journal.upsert(JournalEntry(today, rating = 3, monologue = "first"))
        app.journal.upsert(JournalEntry(today, rating = 4, monologue = "second draft"))
        val entries = app.journal.entries.first()
        assertEquals(1, entries.size)
        assertEquals(2, entries.single().words)
        assertNotNull(app.journal.entry(today).first())
    }

    @Test fun clearingDataKeepsTheAccountAndAreas() = runBlocking {
        app.auth.loginDemo()
        app.reset.clearUserData()
        assertTrue(app.habits.habits.first().isEmpty())
        assertTrue(app.tasks.tasks.first().isEmpty())
        assertNotNull(app.auth.currentUser.first())
        assertEquals(DefaultAreas.names.size, app.areas.areas.first().size)
    }
}
