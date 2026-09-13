package com.mindspring.app.ui.screens.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mindspring.app.AppContainer
import com.mindspring.app.data.model.ThemeMode
import com.mindspring.app.data.model.User
import com.mindspring.app.data.repository.ReminderSettings
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.AvatarCircle
import com.mindspring.app.ui.components.LocalSnackbar
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.MsTextField
import com.mindspring.app.ui.components.SegmentedTabs
import com.mindspring.app.ui.components.TealTopBar
import com.mindspring.app.ui.components.TimePickerDialog
import com.mindspring.app.ui.components.appear
import com.mindspring.app.ui.components.msSwitchColors
import com.mindspring.app.ui.preview.PreviewScreen
import com.mindspring.app.ui.preview.PreviewToday
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.util.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

class ProfileViewModel(private val app: AppContainer) : ViewModel() {
    val user: StateFlow<User?> = app.auth.currentUser.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val theme: StateFlow<ThemeMode> = app.settings.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.System)
    val ambient: StateFlow<Boolean> = app.settings.ambientMotion.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val checkIn: StateFlow<ReminderSettings> = app.settings.checkInReminder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderSettings(true, LocalTime.of(20, 0)))
    val digest: StateFlow<ReminderSettings> = app.settings.taskDigest
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderSettings(true, LocalTime.of(8, 0)))

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { app.settings.setThemeMode(mode) }
    fun setAmbient(on: Boolean) = viewModelScope.launch { app.settings.setAmbientMotion(on) }
    fun setReminders(checkIn: ReminderSettings, digest: ReminderSettings) = viewModelScope.launch {
        app.settings.setCheckInReminder(checkIn)
        app.settings.setTaskDigest(digest)
    }
    fun rename(name: String) = viewModelScope.launch { if (name.isNotBlank()) app.auth.updateName(name) }
    fun logout(then: () -> Unit) = viewModelScope.launch { app.auth.logout(); then() }

    /** Wipes habits, tasks, projects, moods and journal entries. The account and its areas are kept. */
    fun clearAll(then: () -> Unit) = viewModelScope.launch {
        app.reset.clearUserData()
        then()
    }

    fun export(context: android.content.Context, uri: Uri, then: (String) -> Unit) = viewModelScope.launch {
        val message = runCatching {
            val json = app.backup.export()
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
            }
            "Backup saved"
        }.getOrElse { "Couldn't save the backup" }
        then(message)
    }

    fun import(context: android.content.Context, uri: Uri, then: (String) -> Unit) = viewModelScope.launch {
        val message = runCatching {
            val json = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            }
            val s = app.backup.import(json)
            "Restored ${s.habits} habits, ${s.tasks} tasks and ${s.journal} journal entries"
        }.getOrElse { it.message?.takeIf { m -> m.contains("MindSpring") } ?: "That file couldn't be read as a backup" }
        then(message)
    }
}

@Composable
fun ProfileScreen(onBack: () -> Unit, onOpenAreas: () -> Unit, onOpenAlerts: () -> Unit, onLoggedOut: () -> Unit) {
    val vm = appViewModel { ProfileViewModel(it) }
    val user by vm.user.collectAsStateWithLifecycle()
    val theme by vm.theme.collectAsStateWithLifecycle()
    val ambient by vm.ambient.collectAsStateWithLifecycle()
    val checkIn by vm.checkIn.collectAsStateWithLifecycle()
    val digest by vm.digest.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
    var pendingImport by rememberSaveable { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.export(context, uri, toast)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri.toString()
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) toast("Reminders need notification permission to appear")
    }
    val askForNotifications = {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    ProfileContent(
        user = user,
        theme = theme,
        ambient = ambient,
        checkIn = checkIn,
        digest = digest,
        onSetTheme = { vm.setTheme(it) },
        onSetAmbient = { vm.setAmbient(it) },
        onRename = { vm.rename(it) },
        onSetReminders = { a, b ->
            vm.setReminders(a, b)
            if (a.enabled || b.enabled) askForNotifications()
        },
        onExport = { exportLauncher.launch("mindspring-backup-${LocalDate.now()}.json") },
        onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
        onLogout = { vm.logout(onLoggedOut) },
        onClearAll = { vm.clearAll { toast("All data cleared") } },
        onBack = onBack,
        onOpenAreas = onOpenAreas,
        onOpenAlerts = onOpenAlerts,
    )

    pendingImport?.let { uri ->
        ConfirmDialog(
            title = "Restore this backup?",
            message = "Everything currently in your account is replaced by the backup's contents.",
            confirm = "Restore",
            destructive = true,
            onDismiss = { pendingImport = null },
        ) {
            pendingImport = null
            vm.import(context, Uri.parse(uri), toast)
        }
    }
}

/**
 * The screen as pure state and callbacks, so it renders in a @Preview without a ViewModel behind
 * it. [ProfileScreen] is the wrapper that supplies both, and also owns the system pickers for
 * backups and the notification permission, which need a running activity.
 */
@Composable
fun ProfileContent(
    user: User?,
    theme: ThemeMode,
    ambient: Boolean,
    checkIn: ReminderSettings,
    digest: ReminderSettings,
    onSetTheme: (ThemeMode) -> Unit,
    onSetAmbient: (Boolean) -> Unit,
    onRename: (String) -> Unit,
    onSetReminders: (checkIn: ReminderSettings, digest: ReminderSettings) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onLogout: () -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
    onOpenAreas: () -> Unit,
    onOpenAlerts: () -> Unit,
) {
    val c = MsTheme.colors
    var dialog by rememberSaveable { mutableStateOf<ProfileDialog?>(null) }
    val name = user?.name.orEmpty()

    Column(Modifier.fillMaxSize()) {
        TealTopBar("Profile & Settings", onNavigate = onBack)
        Column(
            Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(Dimens.screen),
            verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
        ) {
            Column(Modifier.fillMaxWidth().padding(vertical = Dimens.stackSm).appear(0), horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    AvatarCircle(name, size = 96.dp, background = c.hero, contentColor = c.onTeal, modifier = Modifier.border(4.dp, c.cardBorder, CircleShape))
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(32.dp).clip(CircleShape).background(c.amber).clickable { dialog = ProfileDialog.EditName },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit name", tint = c.onAmber, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(Dimens.stackMd))
                Text(name, style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                user?.let {
                    Text("${it.email} · since ${Fmt.monthYear(it.memberSince)}", style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
                }
            }

            MsCard(Modifier.fillMaxWidth().appear(1), contentPadding = PaddingValues(vertical = 4.dp)) {
                SettingsRow(Icons.Outlined.Person, "Edit profile") { dialog = ProfileDialog.EditName }
                RowDivider()
                SettingsRow(Icons.Outlined.Category, "Life areas", detail = "Rename, recolour or add areas", onClick = onOpenAreas)
                RowDivider()
                SettingsRow(
                    Icons.Outlined.Notifications,
                    "Reminders",
                    detail = listOfNotNull(
                        if (checkIn.enabled) "Check-in ${Fmt.time(checkIn.time)}" else null,
                        if (digest.enabled) "Tasks ${Fmt.time(digest.time)}" else null,
                    ).joinToString(" · ").ifEmpty { "Off" },
                ) { dialog = ProfileDialog.Reminders }
                RowDivider()
                SettingsRow(Icons.Outlined.Alarm, "Alerts & sounds", detail = "Task alarms, tones, vibration and snooze", onClick = onOpenAlerts)
            }

            MsCard(Modifier.fillMaxWidth().appear(2), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Palette, contentDescription = null, tint = c.tealInk)
                    Spacer(Modifier.width(Dimens.stackMd))
                    Text("Appearance", style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
                }
                SegmentedTabs(ThemeMode.entries, theme, onSetTheme, { it.label })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = c.tealInk)
                    Spacer(Modifier.width(Dimens.stackMd))
                    Column(Modifier.weight(1f)) {
                        Text("Ambient motion", style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
                        Text("The slowly drifting background", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                    }
                    Switch(checked = ambient, onCheckedChange = onSetAmbient, colors = msSwitchColors())
                }
            }

            MsCard(Modifier.fillMaxWidth().appear(3), contentPadding = PaddingValues(vertical = 4.dp)) {
                SettingsRow(Icons.Outlined.CloudUpload, "Export backup", detail = "Save everything to a file") {
                    onExport()
                }
                RowDivider()
                SettingsRow(Icons.Outlined.CloudDownload, "Import backup", detail = "Restore from a backup file") {
                    onImport()
                }
                RowDivider()
                SettingsRow(Icons.Outlined.Info, "About") { dialog = ProfileDialog.About }
            }

            MsCard(Modifier.fillMaxWidth().appear(4), contentPadding = PaddingValues(vertical = 4.dp)) {
                SettingsRow(Icons.AutoMirrored.Rounded.Logout, "Log out", chevron = false) { dialog = ProfileDialog.Logout }
            }

            // The one destructive control in settings: isolated in its own card and rendered in red.
            MsCard(Modifier.fillMaxWidth().appear(5), contentPadding = PaddingValues(vertical = 4.dp)) {
                SettingsRow(Icons.Rounded.DeleteForever, "Clear all data", tint = c.danger, chevron = false) { dialog = ProfileDialog.Clear }
            }

            Text(
                "Your data lives only on this phone and is removed if the app is uninstalled. Export a backup to keep it.\n" +
                    "MindSpring is a self-reflection tool and not medical advice.",
                style = MaterialTheme.typography.labelSmall,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.stackMd),
            )
        }
    }

    when (dialog) {
        ProfileDialog.EditName -> EditNameDialog(name, onDismiss = { dialog = null }) { onRename(it); dialog = null }
        ProfileDialog.Reminders -> RemindersDialog(checkIn, digest, onDismiss = { dialog = null }) { a, b ->
            onSetReminders(a, b)
            dialog = null
        }
        ProfileDialog.About -> AlertDialog(
            onDismissRequest = { dialog = null },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Close") } },
            title = { Text("About MindSpring") },
            text = {
                Text(
                    "Version 2.1\n\nMindSpring brings your habits, tasks and moods together and shows you which routines " +
                        "go with your better days. Everything stays on this device.\n\n" +
                        "MindSpring is a self-reflection tool, not a medical product. If you are struggling, please reach out " +
                        "to a qualified professional or a local support service.",
                )
            },
        )
        ProfileDialog.Logout -> ConfirmDialog(
            title = "Log out?",
            message = "Your data stays on this device. You can sign back in at any time.",
            confirm = "Log out",
            onDismiss = { dialog = null },
        ) { dialog = null; onLogout() }
        ProfileDialog.Clear -> ConfirmDialog(
            title = "Clear all data?",
            message = "This permanently deletes every habit, task, project, mood check-in and journal entry on this account. Your account and life areas are kept.",
            confirm = "Clear data",
            destructive = true,
            onDismiss = { dialog = null },
        ) {
            dialog = null
            onClearAll()
        }
        null -> Unit
    }
}

private enum class ProfileDialog { EditName, Reminders, About, Logout, Clear }

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    detail: String? = null,
    tint: androidx.compose.ui.graphics.Color = MsTheme.colors.tealInk,
    chevron: Boolean = true,
    onClick: () -> Unit,
) {
    val c = MsTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Dimens.card, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(Dimens.stackMd))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (tint == c.danger) c.danger else c.textPrimary,
                fontWeight = if (tint == c.danger) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (detail != null) Text(detail, style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
        }
        if (chevron) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary)
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(Modifier.padding(horizontal = Dimens.card), color = MsTheme.colors.divider)
}

@Composable
private fun EditNameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile") },
        text = { MsTextField(value, { value = it.take(40) }, label = "Name") },
        confirmButton = { TextButton(onClick = { onSave(value) }, enabled = value.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RemindersDialog(
    checkIn: ReminderSettings,
    digest: ReminderSettings,
    onDismiss: () -> Unit,
    onSave: (ReminderSettings, ReminderSettings) -> Unit,
) {
    var checkInOn by rememberSaveable { mutableStateOf(checkIn.enabled) }
    var checkInAt by rememberSaveable { mutableStateOf(checkIn.time) }
    var digestOn by rememberSaveable { mutableStateOf(digest.enabled) }
    var digestAt by rememberSaveable { mutableStateOf(digest.time) }
    var picking by rememberSaveable { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminders") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.stackMd)) {
                ReminderLine("Evening check-in", "Mood and tonight's reflection", checkInOn, { checkInOn = it }, checkInAt) { picking = 0 }
                ReminderLine("Morning summary", "What's due and overdue today", digestOn, { digestOn = it }, digestAt) { picking = 1 }
                Text("Habit reminders are set on each habit.", style = MaterialTheme.typography.bodySmall, color = MsTheme.colors.textSecondary)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(ReminderSettings(checkInOn, checkInAt), ReminderSettings(digestOn, digestAt)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    when (picking) {
        0 -> TimePickerDialog(checkInAt, onDismiss = { picking = null }) { checkInAt = it; picking = null }
        1 -> TimePickerDialog(digestAt, onDismiss = { picking = null }) { digestAt = it; picking = null }
    }
}

@Composable
private fun ReminderLine(title: String, detail: String, enabled: Boolean, onEnabled: (Boolean) -> Unit, time: LocalTime, onPick: () -> Unit) {
    val c = MsTheme.colors
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(detail, style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
            }
            Switch(checked = enabled, onCheckedChange = onEnabled, colors = msSwitchColors())
        }
        Text(
            Fmt.time(time),
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) c.tealInk else c.textTertiary,
            modifier = Modifier.clip(MaterialTheme.shapes.small).clickable(enabled = enabled, onClick = onPick).padding(4.dp),
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    destructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirm, color = if (destructive) MsTheme.colors.danger else MsTheme.colors.tealInk)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// --- Previews -------------------------------------------------------------------------------

@Composable
private fun ProfilePreviewBody() = ProfileContent(
    user = User(id = 1, name = "Asan Perera", email = "asan@example.com", memberSince = PreviewToday.minusMonths(6)),
    theme = ThemeMode.System,
    ambient = true,
    checkIn = ReminderSettings(true, LocalTime.of(20, 0)),
    digest = ReminderSettings(true, LocalTime.of(8, 0)),
    onSetTheme = {}, onSetAmbient = {}, onRename = {}, onSetReminders = { _, _ -> },
    onExport = {}, onImport = {}, onLogout = {}, onClearAll = {},
    onBack = {}, onOpenAreas = {}, onOpenAlerts = {},
)

@Preview(name = "Profile", showBackground = true, widthDp = 393, heightDp = 1200)
@Composable
private fun ProfilePreview() = PreviewScreen { ProfilePreviewBody() }

@Preview(name = "Profile · dark", showBackground = true, widthDp = 393, heightDp = 1200)
@Composable
private fun ProfileDarkPreview() = PreviewScreen(dark = true) { ProfilePreviewBody() }
