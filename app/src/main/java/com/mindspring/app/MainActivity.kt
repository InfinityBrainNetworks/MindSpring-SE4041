package com.mindspring.app

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.data.model.ThemeMode
import com.mindspring.app.ui.navigation.MindSpringRoot
import com.mindspring.app.ui.theme.MindSpringTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as MindSpringApp).container.settings
        // Respect the system "remove animations" setting as well as the in-app switch.
        val systemAnimations = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        setContent {
            val mode by settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val ambient by settings.ambientMotion.collectAsStateWithLifecycle(initialValue = true)
            val dark = when (mode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            MindSpringTheme(darkTheme = dark, ambientMotion = ambient && systemAnimations) {
                MindSpringRoot()
            }
        }
    }
}
