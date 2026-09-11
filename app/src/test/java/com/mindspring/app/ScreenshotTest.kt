package com.mindspring.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.mindspring.app.data.memory.InMemoryAuthRepository
import com.mindspring.app.ui.components.LocalSnackbar
import com.mindspring.app.ui.components.MainTab
import com.mindspring.app.ui.components.MsBottomBar
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
import com.mindspring.app.ui.screens.habits.HabitsListScreen
import com.mindspring.app.ui.screens.home.HomeScreen
import com.mindspring.app.ui.screens.insights.InsightsScreen
import com.mindspring.app.ui.screens.mood.MoodCheckInScreen
import com.mindspring.app.ui.screens.mood.MoodHistoryScreen
import com.mindspring.app.ui.screens.onboarding.OnboardingScreen
import com.mindspring.app.ui.screens.profile.ProfileScreen
import com.mindspring.app.ui.theme.MindSpringTheme
import com.mindspring.app.ui.theme.MsTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders every screen with the sample data to PNG files under app/build/screenshots, without
 * needing an emulator. Useful for design review and for the report's screenshot section.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w393dp-h1200dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<MindSpringApp>()

    @Before
    fun signIn() = runBlocking {
        app.container.auth.login(InMemoryAuthRepository.DEMO_EMAIL, InMemoryAuthRepository.DEMO_PASSWORD)
        Unit
    }

    private fun shoot(name: String, dark: Boolean = false, tab: MainTab? = null, content: @Composable () -> Unit) {
        compose.setContent {
            MindSpringTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalSnackbar provides remember { SnackbarHostState() }) {
                    if (tab == null) {
                        content()
                    } else {
                        Scaffold(
                            containerColor = MsTheme.colors.canvas,
                            contentWindowInsets = WindowInsets(0),
                            bottomBar = { MsBottomBar(tab, onSelect = {}) },
                        ) { padding -> Box(Modifier.padding(padding)) { content() } }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    @Test fun onboarding() = shoot("01_onboarding") { OnboardingScreen(onFinish = {}) }
    @Test fun login() = shoot("02_login") { LoginScreen(onSignedIn = {}, onCreateAccount = {}) }
    @Test fun register() = shoot("03_register") { RegisterScreen(onSignedIn = {}, onBack = {}) }
    @Test fun home() = shoot("04_home", tab = MainTab.Home) { HomeScreen({}, {}, {}, {}) }
    @Test fun homeDark() = shoot("04_home_dark", dark = true, tab = MainTab.Home) { HomeScreen({}, {}, {}, {}) }
    @Test fun habits() = shoot("05_habits", tab = MainTab.Habits) { HabitsListScreen("Asan", {}, {}, {}, {}) }
    @Test fun addHabit() = shoot("06_add_habit") { HabitEditorScreen(habitId = null, onDone = {}) }
    @Test fun habitDetail() = shoot("07_habit_detail") { HabitDetailScreen(3, {}, {}, {}) }
    @Test fun habitDone() = shoot("08_habit_completed") { HabitCompletedScreen(1, onDone = {}) }
    @Test fun checkIn() = shoot("09_mood_checkin") { MoodCheckInScreen(entryId = null, initialRating = 4, onDone = {}) }
    @Test fun history() = shoot("10_mood_history", tab = MainTab.Insights) { MoodHistoryScreen({}, {}) }
    @Test fun insights() = shoot("11_insights", tab = MainTab.Insights) { InsightsScreen("Asan", {}, {}) }
    @Test fun insightsDark() = shoot("11_insights_dark", dark = true, tab = MainTab.Insights) { InsightsScreen("Asan", {}, {}) }
    @Test fun calm() = shoot("12_calm", tab = MainTab.Calm) { CalmScreen("Asan", {}, {}, {}) }
    @Test fun gratitude() = shoot("15_gratitude", tab = MainTab.Calm) { GratitudeScreen(onBack = {}) }
    @Test fun profile() = shoot("16_profile", tab = MainTab.Profile) { ProfileScreen(systemDark = false, onLoggedOut = {}) }

    @Test fun breathingCalm() {
        compose.mainClock.autoAdvance = false
        shootAfter("13_breathing_calm", 2_500) { BreathingScreen(TECHNIQUE_CALM, onClose = {}) }
    }

    @Test fun breathingBox() {
        compose.mainClock.autoAdvance = false
        shootAfter("14_breathing_box", 6_000) { BreathingScreen(TECHNIQUE_BOX, onClose = {}) }
    }

    /** For screens driven by a frame clock: advance a fixed time, then capture. */
    private fun shootAfter(name: String, millis: Long, content: @Composable () -> Unit) {
        compose.setContent { MindSpringTheme { content() } }
        compose.mainClock.advanceTimeBy(millis)
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }
}
