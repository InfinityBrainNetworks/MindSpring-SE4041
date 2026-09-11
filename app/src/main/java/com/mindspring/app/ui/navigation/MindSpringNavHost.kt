package com.mindspring.app.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
import com.mindspring.app.ui.components.AmbientBackground
import com.mindspring.app.ui.components.LocalSnackbar
import com.mindspring.app.ui.components.MainTab
import com.mindspring.app.ui.components.MsBottomBar
import com.mindspring.app.ui.components.MsSnackbarHost
import com.mindspring.app.ui.components.SystemBarIcons
import com.mindspring.app.ui.screens.areas.LifeAreasScreen
import com.mindspring.app.ui.screens.auth.LoginScreen
import com.mindspring.app.ui.screens.auth.RegisterScreen
import com.mindspring.app.ui.screens.calm.BreathingScreen
import com.mindspring.app.ui.screens.calm.CalmScreen
import com.mindspring.app.ui.screens.calm.GratitudeScreen
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
import com.mindspring.app.ui.screens.profile.ProfileScreen
import com.mindspring.app.ui.screens.tasks.ProjectDetailScreen
import com.mindspring.app.ui.screens.tasks.ProjectEditorScreen
import com.mindspring.app.ui.screens.tasks.TaskEditorScreen
import com.mindspring.app.ui.screens.tasks.TasksScreen
import com.mindspring.app.ui.theme.MsTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.time.LocalDate

object Routes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val HABITS = "habits"
    const val TASKS = "tasks"
    const val INSIGHTS = "insights"
    const val CALM = "calm"
    const val PROFILE = "profile"
    const val AREAS = "areas"
    const val HABIT_EDITOR = "habit-editor?id={id}"
    const val HABIT_DETAIL = "habit-detail/{id}"
    const val HABIT_DONE = "habit-done/{id}"
    const val TASK_EDITOR = "task-editor?id={id}&project={project}"
    const val PROJECT_DETAIL = "project/{id}"
    const val PROJECT_EDITOR = "project-editor?id={id}"
    const val MOOD_CHECKIN = "mood-checkin?id={id}&rating={rating}"
    const val MOOD_HISTORY = "mood-history"
    const val BREATHING = "breathing/{technique}"
    const val GRATITUDE = "gratitude"
    const val JOURNAL = "journal"
    const val JOURNAL_ENTRY = "journal-entry/{day}"

    fun habitEditor(id: Long? = null) = if (id == null) "habit-editor" else "habit-editor?id=$id"
    fun habitDetail(id: Long) = "habit-detail/$id"
    fun habitDone(id: Long) = "habit-done/$id"
    fun taskEditor(id: Long? = null, project: Long? = null) = "task-editor?id=${id ?: -1}&project=${project ?: -1}"
    fun projectDetail(id: Long) = "project/$id"
    fun projectEditor(id: Long? = null) = if (id == null) "project-editor" else "project-editor?id=$id"
    fun moodCheckIn(id: Long? = null, rating: Int? = null) = "mood-checkin?id=${id ?: -1}&rating=${rating ?: -1}"
    fun breathing(technique: String) = "breathing/$technique"
    fun journalEntry(date: LocalDate) = "journal-entry/${date.toEpochDay()}"
}

private val MainTab.route: String
    get() = when (this) {
        MainTab.Today -> Routes.HOME
        MainTab.Habits -> Routes.HABITS
        MainTab.Tasks -> Routes.TASKS
        MainTab.Insights -> Routes.INSIGHTS
        MainTab.Mind -> Routes.CALM
    }

private val TabRoutes = MainTab.entries.map { it.route }.toSet()

/** Which tab is highlighted, whether the bottom bar shows, and whether the top edge is light. */
private data class Chrome(val tab: MainTab?, val bottomBar: Boolean, val lightTop: Boolean)

private fun chromeFor(route: String?): Chrome = when (route) {
    Routes.HOME -> Chrome(MainTab.Today, true, false)
    Routes.HABITS -> Chrome(MainTab.Habits, true, false)
    Routes.TASKS -> Chrome(MainTab.Tasks, true, false)
    Routes.INSIGHTS, Routes.AREAS -> Chrome(MainTab.Insights, true, false)
    Routes.CALM, Routes.GRATITUDE, Routes.MOOD_HISTORY, Routes.JOURNAL -> Chrome(MainTab.Mind, true, false)
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

// Tab to tab is a quiet cross-fade; going deeper rises gently into place; coming back sinks away.
private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch() =
    initialState.destination.route in TabRoutes && targetState.destination.route in TabRoutes

private val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    if (isTabSwitch()) fadeIn(tween(260))
    else fadeIn(tween(280)) + slideInVertically(tween(320)) { it / 18 } + scaleIn(tween(320), initialScale = 0.985f)
}
private val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { fadeOut(tween(180)) }
private val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { fadeIn(tween(260)) }
private val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(200)) + slideOutVertically(tween(260)) { it / 18 }
}

@Composable
fun MindSpringRoot() {
    val vm = appViewModel { RootViewModel(it) }
    val start by vm.startRoute.collectAsStateWithLifecycle()
    val userName by vm.userName.collectAsStateWithLifecycle()
    val c = MsTheme.colors

    Box(Modifier.fillMaxSize()) {
        // One backdrop for the whole app, so it keeps drifting smoothly across navigation.
        AmbientBackground()

        val startRoute = start ?: return@Box

        val nav = rememberNavController()
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route
        val chrome = chromeFor(route)
        val snackbar = remember { SnackbarHostState() }

        SystemBarIcons(
            darkStatusIcons = !c.isDark && chrome.lightTop,
            darkNavigationIcons = !c.isDark && !chrome.bottomBar && route != Routes.BREATHING,
        )
        NotificationPermissionOnce(ask = route == Routes.HOME)

        val toTab: (MainTab) -> Unit = { tab ->
            nav.navigate(tab.route) {
                popUpTo(Routes.HOME) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        val openProfile = { nav.navigate(Routes.PROFILE) }
        val enterApp: () -> Unit = { nav.navigate(Routes.HOME) { popUpTo(nav.graph.id) { inclusive = true } } }
        val back: () -> Unit = { nav.navigateUp() }
        val openHabit: (Long) -> Unit = { nav.navigate(Routes.habitDetail(it)) }
        val openTask: (Long) -> Unit = { nav.navigate(Routes.taskEditor(id = it)) }
        val openJournalDay: (LocalDate) -> Unit = { nav.navigate(Routes.journalEntry(it)) }

        CompositionLocalProvider(LocalSnackbar provides snackbar) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0),
                snackbarHost = { MsSnackbarHost(snackbar) },
                bottomBar = { if (chrome.bottomBar) MsBottomBar(chrome.tab, onSelect = toTab) },
            ) { padding ->
                NavHost(
                    navController = nav,
                    startDestination = startRoute,
                    modifier = Modifier.padding(padding),
                    enterTransition = enter,
                    exitTransition = exit,
                    popEnterTransition = popEnter,
                    popExitTransition = popExit,
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
                            onOpenHabit = openHabit,
                            onAddHabit = { nav.navigate(Routes.habitEditor()) },
                            onOpenTasks = { toTab(MainTab.Tasks) },
                            onOpenTask = openTask,
                            onOpenJournal = openJournalDay,
                            onOpenProfile = openProfile,
                        )
                    }
                    composable(Routes.HABITS) {
                        HabitsScreen(
                            userName = userName,
                            onOpenHabit = openHabit,
                            onAddHabit = { nav.navigate(Routes.habitEditor()) },
                            onOpenProfile = openProfile,
                        )
                    }
                    composable(Routes.TASKS) {
                        TasksScreen(
                            userName = userName,
                            onOpenTask = openTask,
                            onAddTask = { nav.navigate(Routes.taskEditor()) },
                            onOpenProject = { nav.navigate(Routes.projectDetail(it)) },
                            onAddProject = { nav.navigate(Routes.projectEditor()) },
                            onOpenProfile = openProfile,
                        )
                    }
                    composable(Routes.INSIGHTS) {
                        InsightsScreen(
                            userName = userName,
                            onOpenHistory = { nav.navigate(Routes.MOOD_HISTORY) },
                            onOpenAreas = { nav.navigate(Routes.AREAS) },
                            onOpenJournal = { nav.navigate(Routes.JOURNAL) },
                            onOpenProfile = openProfile,
                        )
                    }
                    composable(Routes.CALM) {
                        CalmScreen(
                            userName = userName,
                            onBreathing = { nav.navigate(Routes.breathing(it)) },
                            onGratitude = { nav.navigate(Routes.GRATITUDE) },
                            onJournal = { nav.navigate(Routes.JOURNAL) },
                            onMoodHistory = { nav.navigate(Routes.MOOD_HISTORY) },
                            onOpenProfile = openProfile,
                        )
                    }
                    composable(Routes.PROFILE) {
                        ProfileScreen(
                            onBack = back,
                            onOpenAreas = { nav.navigate(Routes.AREAS) },
                            onLoggedOut = { nav.navigate(Routes.LOGIN) { popUpTo(nav.graph.id) { inclusive = true } } },
                        )
                    }
                    composable(Routes.AREAS) { LifeAreasScreen(onBack = back) }

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

                    composable(Routes.TASK_EDITOR, arguments = listOf(longArg("id"), longArg("project"))) { e ->
                        TaskEditorScreen(taskId = e.optionalLong("id"), projectId = e.optionalLong("project"), onDone = back)
                    }
                    composable(Routes.PROJECT_DETAIL, arguments = listOf(longArg("id"))) { e ->
                        val id = e.optionalLong("id") ?: return@composable
                        ProjectDetailScreen(
                            projectId = id,
                            onBack = back,
                            onEdit = { nav.navigate(Routes.projectEditor(id)) },
                            onOpenTask = openTask,
                            onAddTask = { nav.navigate(Routes.taskEditor(project = id)) },
                        )
                    }
                    composable(Routes.PROJECT_EDITOR, arguments = listOf(longArg("id"))) { e ->
                        ProjectEditorScreen(projectId = e.optionalLong("id"), onDone = back)
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
                    composable(Routes.GRATITUDE) { GratitudeScreen(onBack = back) }
                    composable(Routes.JOURNAL) { JournalListScreen(onBack = back, onOpenDay = openJournalDay) }
                    composable(Routes.JOURNAL_ENTRY, arguments = listOf(navArgument("day") { type = NavType.LongType })) { e ->
                        val day = e.arguments?.getLong("day") ?: LocalDate.now().toEpochDay()
                        JournalEntryScreen(date = LocalDate.ofEpochDay(day), onDone = back)
                    }
                }
            }
        }
    }
}

/** Reminders need permission on Android 13+; ask once per launch, on the home screen, not at sign-in. */
@Composable
private fun NotificationPermissionOnce(ask: Boolean) {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(ask) {
        if (ask && !asked && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private fun longArg(name: String) = navArgument(name) { type = NavType.LongType; defaultValue = -1L }

private fun NavBackStackEntry.optionalLong(name: String): Long? = arguments?.getLong(name)?.takeIf { it > 0 }
