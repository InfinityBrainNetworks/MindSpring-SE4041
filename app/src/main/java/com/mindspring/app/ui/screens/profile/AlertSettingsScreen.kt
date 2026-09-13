package com.mindspring.app.ui.screens.profile

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mindspring.app.AppContainer
import com.mindspring.app.data.model.AlertSounds
import com.mindspring.app.data.model.AlertStyle
import com.mindspring.app.reminders.AlertAccess
import com.mindspring.app.reminders.Alerts
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.GroupHeader
import com.mindspring.app.ui.components.IconCircle
import com.mindspring.app.ui.components.LocalSnackbar
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.SmallChip
import com.mindspring.app.ui.components.TealTopBar
import com.mindspring.app.ui.components.appear
import com.mindspring.app.ui.components.msSwitchColors
import com.mindspring.app.ui.preview.PreviewScreen
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlertSettingsViewModel(private val app: AppContainer) : ViewModel() {
    val sounds: StateFlow<AlertSounds?> = app.settings.alertSounds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun edit(change: (AlertSounds) -> AlertSounds) {
        val current = sounds.value ?: return
        viewModelScope.launch { app.settings.setAlertSounds(change(current)) }
    }

    fun setReminderTone(uri: String?) = edit { it.copy(reminderTone = uri) }
    fun setAlarmTone(uri: String?) = edit { it.copy(alarmTone = uri) }
    fun setVibrate(on: Boolean) = edit { it.copy(vibrate = on) }
    fun setSnooze(minutes: Int) = edit { it.copy(snoozeMinutes = minutes) }
}

/** What the phone allows right now; re-read whenever the screen comes back from Android settings. */
data class AlertPermissions(val notifications: Boolean, val exact: Boolean, val fullScreen: Boolean, val battery: Boolean) {
    companion object {
        fun read(context: Context) = AlertPermissions(
            notifications = AlertAccess.notificationsAllowed(context),
            exact = AlertAccess.exactAllowed(context),
            fullScreen = AlertAccess.fullScreenAllowed(context),
            battery = context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlertSettingsScreen(onBack: () -> Unit) {
    val vm = appViewModel { AlertSettingsViewModel(it) }
    val sounds by vm.sounds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }

    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val access = remember(refresh) { AlertPermissions.read(context) }

    val open: (Intent) -> Unit = { intent ->
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(AlertAccess.notificationSettings(context))
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refresh++
        // Once refused twice Android stops asking, so send the user to the switch instead.
        if (!granted) open(AlertAccess.notificationSettings(context))
    }
    val allowNotifications = {
        if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else open(AlertAccess.notificationSettings(context))
    }

    val reminderPicker = rememberLauncherForActivityResult(remember { PickTone(RingtoneManager.TYPE_NOTIFICATION, "Reminder tone") }) { picked ->
        if (picked != null) vm.setReminderTone(picked.uri)
    }
    val alarmPicker = rememberLauncherForActivityResult(remember { PickTone(RingtoneManager.TYPE_ALARM, "Alarm tone") }) { picked ->
        if (picked != null) vm.setAlarmTone(picked.uri)
    }

    val tryIt: (AlertStyle) -> Unit = { style ->
        if (!access.notifications) {
            allowNotifications()
        } else {
            // Long enough to lock the phone and see the lock screen view.
            Alerts.scheduleTest(context, style, seconds = 10)
            toast("The test ${style.label.lowercase()} arrives in 10 seconds. Lock your phone to see it fill the screen.")
        }
    }

    AlertSettingsContent(
        sounds = sounds,
        access = access,
        onTry = tryIt,
        onPickReminderTone = { reminderPicker.launch(sounds?.reminderTone) },
        onPickAlarmTone = { alarmPicker.launch(sounds?.alarmTone) },
        onVibrate = vm::setVibrate,
        onSnooze = vm::setSnooze,
        onAllowNotifications = allowNotifications,
        onOpenSoundSettings = { open(AlertAccess.notificationSettings(context)) },
        onOpenExactSettings = { open(AlertAccess.exactSettings(context)) },
        onOpenFullScreenSettings = { open(AlertAccess.fullScreenSettings(context)) },
        onOpenBatterySettings = { open(batteryIntent(context)) },
        onBack = onBack,
    )
}

/**
 * The screen as pure state and callbacks, so it renders in a @Preview without a ViewModel behind
 * it. [AlertSettingsScreen] is the wrapper that supplies both, and owns the tone pickers, the
 * permission request and the jumps into Android settings, which need a running activity.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlertSettingsContent(
    sounds: AlertSounds?,
    access: AlertPermissions,
    onTry: (AlertStyle) -> Unit,
    onPickReminderTone: () -> Unit,
    onPickAlarmTone: () -> Unit,
    onVibrate: (Boolean) -> Unit,
    onSnooze: (Int) -> Unit,
    onAllowNotifications: () -> Unit,
    onOpenSoundSettings: () -> Unit,
    onOpenExactSettings: () -> Unit,
    onOpenFullScreenSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onBack: () -> Unit,
) {
    val c = MsTheme.colors

    Column(Modifier.fillMaxSize()) {
        TealTopBar("Alerts & Sounds", onNavigate = onBack)
        Column(
            Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(Dimens.screen),
            verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
        ) {
            Text(
                "Set alerts on tasks in the task editor and on habits in the habit editor, and pick how each gets your attention.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                modifier = Modifier.appear(0),
            )
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).appear(1), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StyleTile(AlertStyle.Reminder, Icons.Rounded.NotificationsActive, Modifier.weight(1f)) { onTry(AlertStyle.Reminder) }
                StyleTile(AlertStyle.Alarm, Icons.Rounded.Alarm, Modifier.weight(1f)) { onTry(AlertStyle.Alarm) }
            }

            GroupHeader("Sounds")
            MsCard(Modifier.fillMaxWidth().appear(2), contentPadding = PaddingValues(vertical = 4.dp)) {
                val s = sounds
                ToneRow(Icons.Outlined.MusicNote, "Reminder tone", "Reminders and daily nudges", s?.reminderTone, onPickReminderTone)
                Divider()
                ToneRow(Icons.Rounded.Alarm, "Alarm tone", "Alarms on tasks and habits", s?.alarmTone, onPickAlarmTone)
                Divider()
                Row(Modifier.fillMaxWidth().padding(horizontal = Dimens.card, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Vibration, contentDescription = null, tint = c.tealInk)
                    Spacer(Modifier.width(Dimens.stackMd))
                    Text("Vibrate", style = MaterialTheme.typography.bodyLarge, color = c.textPrimary, modifier = Modifier.weight(1f))
                    Switch(checked = s?.vibrate ?: true, onCheckedChange = onVibrate, colors = msSwitchColors())
                }
                Divider()
                Column(Modifier.fillMaxWidth().padding(horizontal = Dimens.card, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Snooze, contentDescription = null, tint = c.tealInk)
                        Spacer(Modifier.width(Dimens.stackMd))
                        Text("Snooze for", style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
                    }
                    FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AlertSounds.SnoozeChoices.forEach { m ->
                            SmallChip("$m min", selected = s?.snoozeMinutes == m, onClick = { onSnooze(m) })
                        }
                    }
                }
                Divider()
                LinkRow(Icons.Outlined.Tune, "More sound options", "Volume, lock screen and Do Not Disturb", onOpenSoundSettings)
            }

            GroupHeader("Permissions")
            MsCard(Modifier.fillMaxWidth().appear(3), contentPadding = PaddingValues(vertical = 4.dp)) {
                AccessRow(Icons.Outlined.Notifications, "Notifications", "Needed for every reminder and alarm", access.notifications, onFix = onAllowNotifications)
                Divider()
                AccessRow(Icons.Outlined.Schedule, "On-time alerts", "Ring at the exact minute, not a little later", access.exact) {
                    onOpenExactSettings()
                }
                Divider()
                AccessRow(Icons.Outlined.LockClock, "Lock screen alerts", "Let alerts wake the phone and fill the lock screen", access.fullScreen) {
                    onOpenFullScreenSettings()
                }
                Divider()
                AccessRow(Icons.Outlined.BatteryChargingFull, "Run in the background", "Recommended on phones that close apps to save battery", access.battery, optional = true) {
                    onOpenBatterySettings()
                }
            }

            Text(
                "Alerts ring even when MindSpring is closed. Finishing a task, or ticking a habit for the day, silences its alert.",
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun StyleTile(style: AlertStyle, icon: ImageVector, modifier: Modifier, onTry: () -> Unit) {
    val c = MsTheme.colors
    MsCard(modifier.fillMaxHeight(), contentPadding = PaddingValues(16.dp)) {
        IconCircle(
            icon,
            size = 40.dp,
            iconSize = 22.dp,
            background = if (style == AlertStyle.Alarm) c.amber else c.cardMuted,
            tint = if (style == AlertStyle.Alarm) c.onAmber else c.tealInk,
        )
        Spacer(Modifier.height(10.dp))
        Text(style.label, style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
        Spacer(Modifier.height(2.dp))
        Text(style.description, style = MaterialTheme.typography.labelSmall, color = c.textSecondary, modifier = Modifier.weight(1f))
        Spacer(Modifier.height(12.dp))
        SmallChip("Try it", selected = false, onClick = onTry)
    }
}

@Composable
private fun ToneRow(icon: ImageVector, title: String, use: String, tone: String?, onClick: () -> Unit) {
    LinkRow(icon, title, "${toneName(tone)} · $use", onClick)
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, detail: String, onClick: () -> Unit) {
    val c = MsTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Dimens.card, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.tealInk)
        Spacer(Modifier.width(Dimens.stackMd))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = c.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary)
    }
}

@Composable
private fun AccessRow(icon: ImageVector, title: String, detail: String, granted: Boolean, optional: Boolean = false, onFix: () -> Unit) {
    val c = MsTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(enabled = !granted, onClick = onFix).padding(horizontal = Dimens.card, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.tealInk)
        Spacer(Modifier.width(Dimens.stackMd))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
        }
        Spacer(Modifier.width(8.dp))
        if (granted) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = "Allowed", tint = c.tealInk, modifier = Modifier.size(22.dp))
        } else {
            Text(
                "Allow",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (optional) c.tealInk else c.onAmber,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (optional) c.cardMuted else c.amber)
                    .clickable(onClick = onFix)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(horizontal = Dimens.card), color = MsTheme.colors.divider)

/** The tone's name as the phone's sound picker shows it. */
@Composable
private fun toneName(uri: String?): String {
    val context = LocalContext.current
    return remember(uri) {
        if (uri == null) "Silent"
        else runCatching { RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context) }.getOrNull() ?: "Default"
    }
}

// Asks directly; for an app that rings alarms this is an accepted use of the request.
@SuppressLint("BatteryLife")
private fun batteryIntent(context: Context): Intent =
    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))

/** The result of the phone's sound picker: a tone, or null for Silent. A cancelled pick gives no result. */
private data class PickedTone(val uri: String?)

private class PickTone(private val type: Int, private val title: String) : ActivityResultContract<String?, PickedTone?>() {
    override fun createIntent(context: Context, input: String?): Intent =
        Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, type)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, title)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(type))
            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, input?.let(Uri::parse))

    override fun parseResult(resultCode: Int, intent: Intent?): PickedTone? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        return PickedTone(IntentCompat.getParcelableExtra(intent, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)?.toString())
    }
}

// --- Previews -------------------------------------------------------------------------------

@Composable
private fun AlertSettingsPreviewBody(access: AlertPermissions) = AlertSettingsContent(
    sounds = AlertSounds(reminderTone = null, alarmTone = null, vibrate = true, snoozeMinutes = 10),
    access = access,
    onTry = {}, onPickReminderTone = {}, onPickAlarmTone = {}, onVibrate = {}, onSnooze = {},
    onAllowNotifications = {}, onOpenSoundSettings = {}, onOpenExactSettings = {},
    onOpenFullScreenSettings = {}, onOpenBatterySettings = {}, onBack = {},
)

@Preview(name = "Alert settings", showBackground = true, widthDp = 393, heightDp = 1300)
@Composable
private fun AlertSettingsPreview() = PreviewScreen {
    AlertSettingsPreviewBody(AlertPermissions(notifications = true, exact = true, fullScreen = true, battery = true))
}

@Preview(name = "Alert settings · permissions missing", showBackground = true, widthDp = 393, heightDp = 1300)
@Composable
private fun AlertSettingsMissingPreview() = PreviewScreen {
    AlertSettingsPreviewBody(AlertPermissions(notifications = false, exact = false, fullScreen = true, battery = false))
}

@Preview(name = "Alert settings · dark", showBackground = true, widthDp = 393, heightDp = 1300)
@Composable
private fun AlertSettingsDarkPreview() = PreviewScreen(dark = true) {
    AlertSettingsPreviewBody(AlertPermissions(notifications = true, exact = true, fullScreen = true, battery = true))
}
