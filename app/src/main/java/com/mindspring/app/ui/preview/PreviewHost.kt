package com.mindspring.app.ui.preview

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.mindspring.app.ui.theme.MindSpringTheme
import com.mindspring.app.ui.theme.MsTheme

/**
 * Wraps a @Preview in the app's theme on the app's canvas, so a screen previews the way it looks
 * when the app runs. Each screen keeps its own @Preview functions at the bottom of its own file:
 * open the screen, use the Split view, and its preview is right there.
 *
 * Ambient motion is off by default so a preview settles into a still frame instead of animating
 * forever; interactive mode (the pointer icon on the preview) brings the motion and taps back.
 */
@Composable
fun PreviewScreen(dark: Boolean = false, content: @Composable () -> Unit) {
    MindSpringTheme(darkTheme = dark, ambientMotion = false) {
        Surface(color = MsTheme.colors.canvas, content = content)
    }
}
