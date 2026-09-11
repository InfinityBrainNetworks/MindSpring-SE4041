package com.mindspring.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.data.model.ThemeMode
import com.mindspring.app.reminders.AlertTarget
import com.mindspring.app.reminders.Alerts
import com.mindspring.app.ui.navigation.MindSpringRoot
import com.mindspring.app.ui.theme.MindSpringTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    /** A task or habit to open, when the app was launched from one of its alerts. */
    private val openTarget = MutableStateFlow<AlertTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // On a rotation the launch intent is still there; it was already opened.
        if (savedInstanceState == null) openTarget.value = targetFrom(intent)
        val settings = (application as MindSpringApp).container.settings
        // Respect the system "remove animations" setting as well as the in-app switch.
        val systemAnimations = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        setContent {
            val mode by settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val ambient by settings.ambientMotion.collectAsStateWithLifecycle(initialValue = true)
            val pending by openTarget.collectAsStateWithLifecycle()
            val dark = when (mode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            MindSpringTheme(darkTheme = dark, ambientMotion = ambient && systemAnimations) {
                MindSpringRoot(openTarget = pending, onOpened = { openTarget.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        targetFrom(intent)?.let { openTarget.value = it }
    }

    private fun targetFrom(intent: Intent?): AlertTarget? =
        if (intent?.action == Alerts.ACTION_OPEN) Alerts.targetFrom(intent)?.takeIf { it.id > 0 } else null
}
