package com.mindspring.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.mindspring.app.data.model.AlertStyle
import com.mindspring.app.reminders.AlertInfo
import com.mindspring.app.reminders.AlertKind
import com.mindspring.app.reminders.AlertTarget
import com.mindspring.app.ui.components.AmbientBackground
import com.mindspring.app.ui.components.LocalSnackbar
import com.mindspring.app.ui.components.MainTab
import com.mindspring.app.ui.components.MsBottomBar
import com.mindspring.app.ui.screens.alarm.AlarmScreen
import com.mindspring.app.ui.screens.areas.LifeAreasScreen
import com.mindspring.app.ui.screens.auth.LoginScreen
import com.mindspring.app.ui.screens.auth.RegisterScreen
import com.mindspring.app.ui.screens.calm.BreathingScreen
import com.mindspring.app.ui.screens.calm.CalmScreen
import com.mindspring.app.ui.screens.calm.GratitudeScreen
import com.mindspring.app.ui.screens.calm.TECHNIQUE_BOX
import com.mindspring.app.ui.screens.calm.TECHNIQUE_CALM
import com.mindspring.app.ui.screens.habits.HabitCompletedScreen
import com.mindspring.app.ui.screens.habits.HabitDetailScreen
import com.mindspring.app.ui.screens.habits.HabitEditorScreen
import com.mindspring.app.ui.screens.habits.HabitsScreen
import com.mindspring.app.ui.screens.home.HomeScreen
import com.mindspring.app.ui.screens.insights.InsightsScreen
import com.mindspring.app.ui.screens.journal.JournalEntryScreen
import com.mindspring.app.ui.screens.journal.JournalListScreen
import com.mindspring.app.ui.screens.mood.MoodCheckInScreen
import com.mindspring.app.ui.screens.mood.MoodHistoryScreen
import com.mindspring.app.ui.screens.onboarding.OnboardingScreen
import com.mindspring.app.ui.screens.profile.AlertSettingsScreen
import com.mindspring.app.ui.screens.profile.ProfileScreen
import com.mindspring.app.ui.screens.tasks.ProjectDetailScreen
import com.mindspring.app.ui.screens.tasks.ProjectEditorScreen
import com.mindspring.app.ui.screens.tasks.TaskEditorScreen
import com.mindspring.app.ui.screens.tasks.TasksScreen
import com.mindspring.app.ui.theme.MindSpringTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Renders every screen with the demo account to PNG files under app/build/screenshots, without
 * an emulator. Useful for design review and for the report's screenshot section.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w393dp-h1200dp-xxhdpi", application = TestApp::class)
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<MindSpringApp>()

    @Before
    fun signIn() = runBlocking {
        app.container.auth.loginDemo()
        Unit
    }

    /** Room and DataStore deliver on background threads; give them a moment, then let Compose settle. */
    private fun settle() {
        repeat(4) {
            Thread.sleep(250)
            compose.waitForIdle()
        }
    }

    private fun setScreen(dark: Boolean, tab: MainTab?, content: @Composable () -> Unit) {
        compose.setContent {
            MindSpringTheme(darkTheme = dark, ambientMotion = false) {
                CompositionLocalProvider(LocalSnackbar provides remember { SnackbarHostState() }) {
                    Box(Modifier.fillMaxSize()) {
                        AmbientBackground()
                        if (tab == null) {
                            content()
                        } else {
                            Scaffold(
                                containerColor = Color.Transparent,
                                contentWindowInsets = WindowInsets(0),
                                bottomBar = { MsBottomBar(tab, onSelect = {}) },
                            ) { padding -> Box(Modifier.padding(padding)) { content() } }
                        }
                    }
                }
            }
        }
        settle()
    }

    private fun capture(name: String) = compose.onRoot().captureRoboImage("build/screenshots/$name.png")

    private fun shoot(name: String, dark: Boolean = false, tab: MainTab? = null, content: @Composable () -> Unit) {
        setScreen(dark, tab, content)
        capture(name)
    }

    /** Opens a segment or filter by its label, then captures. */
    private fun shootAfterTap(name: String, label: String, dark: Boolean = false, tab: MainTab? = null, content: @Composable () -> Unit) {
        setScreen(dark, tab, content)
        compose.onAllNodesWithText(label).onFirst().performClick()
        settle()
        capture(name)
    }

    @Test fun onboarding() = shoot("01_onboarding") { OnboardingScreen(onFinish = {}) }
    @Test fun login() = shoot("02_login") { LoginScreen(onSignedIn = {}, onCreateAccount = {}) }
    @Test fun register() = shoot("03_register") { RegisterScreen(onSignedIn = {}, onBack = {}) }

    @Test fun today() = shoot("04_today", tab = MainTab.Today) { HomeScreen({}, {}, {}, {}, {}, {}, {}, {}) }
    @Test fun todayDark() = shoot("04_today_dark", dark = true, tab = MainTab.Today) { HomeScreen({}, {}, {}, {}, {}, {}, {}, {}) }

    @Test fun habitsToday() = shoot("05_habits_today", tab = MainTab.Habits) { HabitsScreen("Asan", {}, {}, {}) }
    @Test fun habitsWeek() = shootAfterTap("05_habits_week", "Week", tab = MainTab.Habits) { HabitsScreen("Asan", {}, {}, {}) }
    @Test fun habitsWeekDark() = shootAfterTap("05_habits_week_dark", "Week", dark = true, tab = MainTab.Habits) { HabitsScreen("Asan", {}, {}, {}) }
    @Test fun habitsAll() = shootAfterTap("05_habits_all", "All", tab = MainTab.Habits) { HabitsScreen("Asan", {}, {}, {}) }
    @Test fun addHabit() = shoot("06_add_habit") { HabitEditorScreen(habitId = null, onDone = {}) }
    @Test fun habitDetail() = shoot("07_habit_detail") { HabitDetailScreen(3, {}, {}, {}) }
    @Test fun habitDone() = shoot("08_habit_completed") { HabitCompletedScreen(1, onDone = {}) }

    @Test fun tasks() = shoot("09_tasks", tab = MainTab.Tasks) { TasksScreen("Asan", {}, {}, {}, {}, {}) }
    @Test fun tasksDark() = shoot("09_tasks_dark", dark = true, tab = MainTab.Tasks) { TasksScreen("Asan", {}, {}, {}, {}, {}) }
    @Test fun tasksFinished() = shootAfterTap("09_tasks_finished", "Finished", tab = MainTab.Tasks) { TasksScreen("Asan", {}, {}, {}, {}, {}) }
    @Test fun projects() = shootAfterTap("10_projects", "Projects", tab = MainTab.Tasks) { TasksScreen("Asan", {}, {}, {}, {}, {}) }
    @Test fun taskEditor() = shoot("11_task_editor") { TaskEditorScreen(taskId = 2, projectId = null, onDone = {}) }
    @Test fun projectDetail() = shoot("12_project_detail") { ProjectDetailScreen(1, {}, {}, {}, {}) }
    @Test fun projectEditor() = shoot("13_project_editor") { ProjectEditorScreen(projectId = null, onDone = {}) }

    @Test fun checkIn() = shoot("14_mood_checkin") { MoodCheckInScreen(entryId = null, initialRating = 4, onDone = {}) }
    @Test fun history() = shoot("15_mood_history", tab = MainTab.Mind) { MoodHistoryScreen({}, {}) }

    @Test fun insights() = shoot("16_insights", tab = MainTab.Insights) { InsightsScreen("Asan", {}, {}, {}, {}) }
    @Test fun insightsDark() = shoot("16_insights_dark", dark = true, tab = MainTab.Insights) { InsightsScreen("Asan", {}, {}, {}, {}) }
    @Test fun insightsHabits() = shootAfterTap("17_insights_habits", "Habits", tab = MainTab.Insights) { InsightsScreen("Asan", {}, {}, {}, {}) }
    @Test fun insightsTasks() = shootAfterTap("18_insights_tasks", "Tasks", tab = MainTab.Insights) { InsightsScreen("Asan", {}, {}, {}, {}) }
    @Test fun insightsMind() = shootAfterTap("19_insights_mind", "Mind", tab = MainTab.Insights) { InsightsScreen("Asan", {}, {}, {}, {}) }
    @Test fun lifeAreas() = shootAfterTap("20_life_areas", "Health & Well-being", tab = MainTab.Insights) { LifeAreasScreen(onBack = {}) }

    @Test fun mind() = shoot("21_mind", tab = MainTab.Mind) { CalmScreen("Asan", {}, {}, {}, {}, {}) }
    @Test fun journal() = shoot("22_journal", tab = MainTab.Mind) { JournalListScreen({}, {}) }
    @Test fun journalEntry() = shoot("23_journal_entry") { JournalEntryScreen(LocalDate.now().minusDays(1), onDone = {}) }
    @Test fun gratitude() = shoot("24_gratitude", tab = MainTab.Mind) { GratitudeScreen(onBack = {}) }
    @Test fun profile() = shoot("25_profile") { ProfileScreen({}, {}, {}, {}) }
    @Test fun profileDark() = shoot("25_profile_dark", dark = true) { ProfileScreen({}, {}, {}, {}) }
    @Test fun alertSettings() = shoot("28_alert_settings") { AlertSettingsScreen(onBack = {}) }
    @Test fun alertSettingsDark() = shoot("28_alert_settings_dark", dark = true) { AlertSettingsScreen(onBack = {}) }

    // Task 2 carries a demo alert at 9 AM tomorrow, so the editor opens with the alert section filled in.
    @Test fun taskEditorAlarm() {
        setScreen(dark = false, tab = null) { TaskEditorScreen(taskId = 2, projectId = null, onDone = {}) }
        compose.onAllNodesWithText("Alarm").onFirst().performClick()
        settle()
        compose.onRoot().captureRoboImage("build/screenshots/11_task_editor_alarm.png")
    }

    private val sampleAlarm = AlertInfo(AlertTarget(AlertKind.Task, 5), "Submit the final APK", "Due today · Mobile App Assignment", AlertStyle.Alarm, LocalDate.now().atTime(19, 30))

    @Test fun alarm() = shoot("29_alarm") {
        AlarmScreen(sampleAlarm, 10, {}, {}, {}, {}, now = LocalDate.now().atTime(19, 30))
    }

    @Test fun alarmDark() = shoot("29_alarm_dark", dark = true) {
        AlarmScreen(sampleAlarm, 10, {}, {}, {}, {}, now = LocalDate.now().atTime(19, 30))
    }

    @Test fun reminderOnLockScreen() = shoot("29_reminder_lock_screen") {
        AlarmScreen(sampleAlarm.copy(style = AlertStyle.Reminder, title = "Room database and repositories", detail = "Due tomorrow · Mobile App Assignment"), 10, {}, {}, {}, {}, now = LocalDate.now().atTime(9, 0))
    }

    @Test fun habitAlarmOnLockScreen() = shoot("29_habit_alarm_dark", dark = true) {
        AlarmScreen(
            AlertInfo(AlertTarget(AlertKind.Habit, 3), "Morning meditation", "10 min", AlertStyle.Alarm, LocalDate.now().atTime(6, 30)),
            10, {}, {}, {}, {}, now = LocalDate.now().atTime(6, 30),
        )
    }

    // Habit 1 has a demo reminder; switching it to Alarm shows the choice in the editor.
    @Test fun habitEditorAlarm() {
        setScreen(dark = false, tab = null) { HabitEditorScreen(habitId = 1, onDone = {}) }
        compose.onAllNodesWithText("Alarm").onFirst().performClick()
        settle()
        compose.onRoot().captureRoboImage("build/screenshots/06_habit_editor_alarm.png")
    }

    @Test fun alarmIsDismissedBySwipeNotTap() {
        var stopped = false
        setScreen(dark = false, tab = null) {
            AlarmScreen(sampleAlarm, 10, {}, {}, { stopped = true }, {}, now = LocalDate.now().atTime(19, 30))
        }
        val handle = compose.onNodeWithContentDescription("Swipe to dismiss")
        handle.performTouchInput { click(center) }
        compose.waitForIdle()
        assertFalse("a tap must not dismiss", stopped)
        handle.performTouchInput { swipe(center, center + Offset(0f, -400f), durationMillis = 400) }
        compose.waitForIdle()
        assertTrue("a swipe dismisses", stopped)
    }

    @Test fun breathingCalm() {
        compose.mainClock.autoAdvance = false
        shootAfter("26_breathing_calm", 2_500) { BreathingScreen(TECHNIQUE_CALM, onClose = {}) }
    }

    @Test fun breathingBox() {
        compose.mainClock.autoAdvance = false
        shootAfter("27_breathing_box", 6_000) { BreathingScreen(TECHNIQUE_BOX, onClose = {}) }
    }

    /** For screens driven by a frame clock: advance a fixed time, then capture. */
    private fun shootAfter(name: String, millis: Long, content: @Composable () -> Unit) {
        compose.setContent { MindSpringTheme(ambientMotion = false) { content() } }
        compose.mainClock.advanceTimeBy(millis)
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }
}
