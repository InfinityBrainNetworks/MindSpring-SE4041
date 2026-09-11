package com.mindspring.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.ui.navigation.MindSpringRoot
import com.mindspring.app.ui.theme.MindSpringTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as MindSpringApp).container.settings
        setContent {
            val darkPref by settings.darkMode.collectAsStateWithLifecycle(initialValue = null)
            MindSpringTheme(darkTheme = darkPref ?: isSystemInDarkTheme()) {
                MindSpringRoot()
            }
        }
    }
}
