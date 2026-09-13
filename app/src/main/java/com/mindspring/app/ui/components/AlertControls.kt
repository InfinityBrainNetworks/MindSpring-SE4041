package com.mindspring.app.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.mindspring.app.data.model.AlertStyle
import com.mindspring.app.reminders.AlertAccess
import com.mindspring.app.ui.theme.InputShape
import com.mindspring.app.ui.theme.MsTheme

/**
 * Reminder or Alarm, a line on what the choice does, and a note if the phone would stop the
 * alert showing (notifications off, lock screen alerts off, or a time already gone).
 */
@Composable
fun AlertStylePicker(style: AlertStyle, onChange: (AlertStyle) -> Unit, modifier: Modifier = Modifier, passed: Boolean = false) {
    val c = MsTheme.colors
    val context = LocalContext.current
    // An IDE preview has no real notification settings to read, so assume the happy path there.
    val inPreview = LocalInspectionMode.current
    var refresh by remember { mutableIntStateOf(0) }
    if (!inPreview) LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val notificationsOn = remember(refresh) { inPreview || AlertAccess.notificationsAllowed(context) }
    val lockScreenOn = remember(refresh) { inPreview || AlertAccess.fullScreenAllowed(context) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SegmentedTabs(AlertStyle.entries, style, onChange, { it.label })
        Text(style.description, style = MaterialTheme.typography.labelSmall, color = c.textTertiary, modifier = Modifier.padding(horizontal = 4.dp))
        when {
            passed -> AlertWarning("This time has already passed, so the alert won't ring.")
            !notificationsOn -> AlertWarning("Notifications are off for MindSpring, so alerts can't appear.", "Turn on") {
                context.startActivity(AlertAccess.notificationSettings(context))
            }
            !lockScreenOn -> AlertWarning("Lock screen alerts are off, so this shows only as a banner.", "Turn on") {
                context.startActivity(AlertAccess.fullScreenSettings(context))
            }
        }
    }
}

/** Asks for notification permission (Android 13+) if it has not been given; call when an alert is switched on. */
@Composable
fun rememberAskForNotifications(): () -> Unit {
    // No activity backs an IDE preview, so there is no result registry to launch a request from.
    if (LocalInspectionMode.current) return {}
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return {
        if (Build.VERSION.SDK_INT >= 33 && !AlertAccess.notificationsAllowed(context)) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
private fun AlertWarning(text: String, action: String? = null, onAction: () -> Unit = {}) {
    val c = MsTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(InputShape).background(c.warnSoft).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = c.textPrimary, modifier = Modifier.weight(1f))
        if (action != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                action,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = c.tealInk,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onAction).padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}
