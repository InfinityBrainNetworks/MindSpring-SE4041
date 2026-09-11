package com.mindspring.app.reminders

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.MindSpringApp
import com.mindspring.app.data.model.AlertStyle
import com.mindspring.app.data.model.ThemeMode
import com.mindspring.app.ui.screens.alarm.AlarmScreen
import com.mindspring.app.ui.theme.MindSpringTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Shown by an alert's full-screen intent: over the lock screen, turning the screen on. The sound
 * itself belongs to the notification, so answering here cancels that notification.
 */
class AlarmActivity : ComponentActivity() {
    private data class Request(val target: AlertTarget, val testStyle: AlertStyle?)

    private var request by mutableStateOf(Request(AlertTarget.Test, null))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge()
        request = read(intent)
        val app = application as MindSpringApp
        val settings = app.container.settings

        setContent {
            val mode by settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val ambient by settings.ambientMotion.collectAsStateWithLifecycle(initialValue = true)
            val sounds by settings.alertSounds.collectAsStateWithLifecycle(initialValue = null)
            val dark = when (mode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            val r = request
            val alert by produceState(initialValue = null as AlertInfo?, r) { value = Alerts.info(app, r.target, r.testStyle) }
            // The notification stops ringing on its own after the timeout; so does this screen.
            LaunchedEffect(r) {
                delay(Alerts.RING_TIMEOUT.toMillis())
                finish()
            }
            MindSpringTheme(darkTheme = dark, ambientMotion = ambient) {
                AlarmScreen(
                    alert = alert,
                    snoozeMinutes = sounds?.snoozeMinutes ?: 10,
                    onDone = { answer(Alerts.ACTION_DONE) },
                    onSnooze = { answer(Alerts.ACTION_SNOOZE) },
                    onStop = { answer(Alerts.ACTION_STOP) },
                    onOpen = {
                        // Opening the app itself asks for the phone to be unlocked first.
                        startActivity(Alerts.openIntent(this, r.target))
                        answer(Alerts.ACTION_STOP)
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        request = read(intent)
    }

    private fun read(intent: Intent) = Request(Alerts.targetFrom(intent) ?: AlertTarget.Test, Alerts.testStyleFrom(intent))

    private fun answer(action: String) {
        val app = application as MindSpringApp
        val target = request.target
        app.appScope.launch { Alerts.respond(app, action, target) }
        finish()
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
