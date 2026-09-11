package com.mindspring.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mindspring.app.AppContainer
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.LocalSnackbar
import com.mindspring.app.ui.components.MainTab
import com.mindspring.app.ui.components.MsBottomBar
import com.mindspring.app.ui.components.MsSnackbarHost
import com.mindspring.app.ui.components.SystemBarIcons
import com.mindspring.app.ui.screens.auth.LoginScreen
import com.mindspring.app.ui.screens.auth.RegisterScreen
import com.mindspring.app.ui.screens.calm.BreathingScreen
import com.mindspring.app.ui.screens.calm.CalmScreen
import com.mindspring.app.ui.screens.calm.GratitudeScreen
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
import com.mindspring.app.ui.theme.MsTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

object Routes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val HABITS = "habits"
    const val INSIGHTS = "insights"
    const val CALM = "calm"
    const val PROFILE = "profile"
    const val HABIT_EDITOR = "habit-editor?id={id}"
    const val HABIT_DETAIL = "habit-detail/{id}"
    const val HABIT_DONE = "habit-done/{id}"
    const val MOOD_CHECKIN = "mood-checkin?id={id}&rating={rating}"
    const val MOOD_HISTORY = "mood-history"
    const val BREATHING = "breathing/{technique}"
    const val GRATITUDE = "gratitude"

    fun habitEditor(id: Long? = null) = if (id == null) "habit-editor" else "habit-editor?id=$id"
    fun habitDetail(id: Long) = "habit-detail/$id"
    fun habitDone(id: Long) = "habit-done/$id"
    fun moodCheckIn(id: Long? = null, rating: Int? = null) =
        "mood-checkin?id=${id ?: -1}&rating=${rating ?: -1}"
    fun breathing(technique: String) = "breathing/$technique"
}

private val MainTab.route: String
    get() = when (this) {
        MainTab.Home -> Routes.HOME
        MainTab.Habits -> Routes.HABITS
        MainTab.Insights -> Routes.INSIGHTS
        MainTab.Calm -> Routes.CALM
        MainTab.Profile -> Routes.PROFILE
    }

/** Which tab is highlighted, whether the bottom bar shows, and whether the top edge is light. */
private data class Chrome(val tab: MainTab?, val bottomBar: Boolean, val lightTop: Boolean)

private fun chromeFor(route: String?): Chrome = when (route) {
    Routes.HOME -> Chrome(MainTab.Home, true, false)
    Routes.HABITS -> Chrome(MainTab.Habits, true, false)
    Routes.INSIGHTS, Routes.MOOD_HISTORY -> Chrome(MainTab.Insights, true, false)
    Routes.CALM, Routes.GRATITUDE -> Chrome(MainTab.Calm, true, false)
    Routes.PROFILE -> Chrome(MainTab.Profile, true, false)
    Routes.ONBOARDING, Routes.LOGIN, Routes.REGISTER, Routes.MOOD_CHECKIN, Routes.HABIT_DONE -> Chrome(null, false, true)
    else -> Chrome(null, false, false)
}

class RootViewModel(private val app: AppContainer) : ViewModel() {
    /** Decided once at launch; later sign-in/out is handled by explicit navigation. */
    val startRoute: StateFlow<String?> = combine(app.settings.onboardingDone, app.auth.currentUser) { onboarded, user ->
        when {
            !onboarded -> Routes.ONBOARDING
            user == null -> Routes.LOGIN
            else -> Routes.HOME
        }
    }.take(1).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val userName: StateFlow<String> = app.auth.currentUser.map { it?.name.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun completeOnboarding() {
        viewModelScope.launch { app.settings.setOnboardingDone() }
    }
}

@Composable
fun MindSpringRoot() {
    val vm = appViewModel { RootViewModel(it) }
    val start by vm.startRoute.collectAsStateWithLifecycle()
    val userName by vm.userName.collectAsStateWithLifecycle()
    val c = MsTheme.colors
    val systemDark = isSystemInDarkTheme()

    val startRoute = start
    if (startRoute == null) {
        Box(Modifier.fillMaxSize().background(c.canvas))
        return
    }

    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val chrome = chromeFor(route)
    val snackbar = remember { SnackbarHostState() }

    SystemBarIcons(
        darkStatusIcons = !c.isDark && chrome.lightTop,
        darkNavigationIcons = !c.isDark && !chrome.bottomBar && route != Routes.BREATHING,
    )

    val toTab: (MainTab) -> Unit = { tab ->
        nav.navigate(tab.route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val openProfile = { toTab(MainTab.Profile) }
    val enterApp: () -> Unit = { nav.navigate(Routes.HOME) { popUpTo(nav.graph.id) { inclusive = true } } }
    val back: () -> Unit = { nav.navigateUp() }

    CompositionLocalProvider(LocalSnackbar provides snackbar) {
        Scaffold(
            containerColor = c.canvas,
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { MsSnackbarHost(snackbar) },
            bottomBar = { if (chrome.bottomBar) MsBottomBar(chrome.tab, onSelect = toTab) },
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = startRoute,
                modifier = Modifier.padding(padding),
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(180)) },
            ) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(onFinish = {
                        vm.completeOnboarding()
                        nav.navigate(Routes.LOGIN) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    })
                }
                composable(Routes.LOGIN) {
                    LoginScreen(onSignedIn = enterApp, onCreateAccount = { nav.navigate(Routes.REGISTER) })
                }
                composable(Routes.REGISTER) {
                    RegisterScreen(onSignedIn = enterApp, onBack = back)
                }

                composable(Routes.HOME) {
                    HomeScreen(
                        onCheckIn = { rating -> nav.navigate(Routes.moodCheckIn(rating = rating)) },
                        onEditMood = { id -> nav.navigate(Routes.moodCheckIn(id = id)) },
                        onAddHabit = { nav.navigate(Routes.habitEditor()) },
                        onOpenReminders = openProfile,
                    )
                }
                composable(Routes.HABITS) {
                    HabitsListScreen(
                        userName = userName,
                        onOpenHabit = { nav.navigate(Routes.habitDetail(it)) },
                        onAddHabit = { nav.navigate(Routes.habitEditor()) },
                        onCompleted = { nav.navigate(Routes.habitDone(it)) },
                        onOpenProfile = openProfile,
                    )
                }
                composable(Routes.INSIGHTS) {
                    InsightsScreen(userName, onOpenHistory = { nav.navigate(Routes.MOOD_HISTORY) }, onOpenProfile = openProfile)
                }
                composable(Routes.CALM) {
                    CalmScreen(
                        userName = userName,
                        onBreathing = { nav.navigate(Routes.breathing(it)) },
                        onGratitude = { nav.navigate(Routes.GRATITUDE) },
                        onOpenProfile = openProfile,
                    )
                }
                composable(Routes.PROFILE) {
                    ProfileScreen(
                        systemDark = systemDark,
                        onLoggedOut = { nav.navigate(Routes.LOGIN) { popUpTo(nav.graph.id) { inclusive = true } } },
                    )
                }

                composable(Routes.HABIT_EDITOR, arguments = listOf(longArg("id"))) { e ->
                    HabitEditorScreen(habitId = e.optionalLong("id"), onDone = back)
                }
                composable(Routes.HABIT_DETAIL, arguments = listOf(longArg("id"))) { e ->
                    val id = e.optionalLong("id") ?: return@composable
                    HabitDetailScreen(
                        habitId = id,
                        onBack = back,
                        onEdit = { nav.navigate(Routes.habitEditor(id)) },
                        onCompleted = { nav.navigate(Routes.habitDone(id)) },
                    )
                }
                composable(Routes.HABIT_DONE, arguments = listOf(longArg("id"))) { e ->
                    HabitCompletedScreen(habitId = e.optionalLong("id") ?: -1, onDone = back)
                }
                composable(
                    Routes.MOOD_CHECKIN,
                    arguments = listOf(longArg("id"), navArgument("rating") { type = NavType.IntType; defaultValue = -1 }),
                ) { e ->
                    val rating = e.arguments?.getInt("rating")?.takeIf { it > 0 }
                    MoodCheckInScreen(entryId = e.optionalLong("id"), initialRating = rating, onDone = back)
                }
                composable(Routes.MOOD_HISTORY) {
                    MoodHistoryScreen(onBack = back, onEdit = { nav.navigate(Routes.moodCheckIn(id = it)) })
                }
                composable(Routes.BREATHING, arguments = listOf(navArgument("technique") { type = NavType.StringType })) { e ->
                    BreathingScreen(technique = e.arguments?.getString("technique").orEmpty(), onClose = back)
                }
                composable(Routes.GRATITUDE) {
                    GratitudeScreen(onBack = back)
                }
            }
        }
    }
}

private fun longArg(name: String) = navArgument(name) { type = NavType.LongType; defaultValue = -1L }

private fun NavBackStackEntry.optionalLong(name: String): Long? = arguments?.getLong(name)?.takeIf { it > 0 }
