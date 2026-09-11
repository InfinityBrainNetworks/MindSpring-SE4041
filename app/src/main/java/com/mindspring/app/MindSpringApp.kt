package com.mindspring.app

import android.app.Application
import androidx.work.Configuration
import com.mindspring.app.reminders.ReminderSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

open class MindSpringApp : Application(), Configuration.Provider {
    lateinit var container: AppContainer
        private set

    /** Lives as long as the process; used for work that outlasts any screen. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = createContainer()
        startBackgroundWork()
    }

    protected open fun createContainer(): AppContainer = AppContainer(this)

    /** Keeps scheduled reminders in step with settings and habits. Tests turn this off. */
    protected open fun startBackgroundWork() {
        ReminderSync.start(this, container, appScope)
    }

    // WorkManager starts on first use with this configuration instead of at process start.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}
