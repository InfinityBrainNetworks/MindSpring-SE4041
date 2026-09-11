package com.mindspring.app.ui.components

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.mindspring.app.ui.theme.MsTheme

/** One app-wide snackbar host, provided by the root scaffold. */
val LocalSnackbar = staticCompositionLocalOf<SnackbarHostState> { error("No SnackbarHostState provided") }

@Composable
fun MsSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    val c = MsTheme.colors
    SnackbarHost(state, modifier) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = c.teal,
            contentColor = c.onTeal,
            actionColor = c.amber,
        )
    }
}
