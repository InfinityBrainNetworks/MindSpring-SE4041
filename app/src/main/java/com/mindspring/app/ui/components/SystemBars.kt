package com.mindspring.app.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Sets whether the status and navigation bar icons are dark. Screens with a teal bar behind the
 * system bars need light icons; light canvases need dark ones.
 */
@Composable
fun SystemBarIcons(darkStatusIcons: Boolean, darkNavigationIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = darkStatusIcons
            isAppearanceLightNavigationBars = darkNavigationIcons
        }
    }
}
