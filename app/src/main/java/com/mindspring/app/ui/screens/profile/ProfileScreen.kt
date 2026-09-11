package com.mindspring.app.ui.screens.profile

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mindspring.app.AppContainer
import com.mindspring.app.data.model.User
import com.mindspring.app.data.repository.ReminderSettings
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.AvatarCircle
import com.mindspring.app.ui.components.BrandTopBar
import com.mindspring.app.ui.components.LocalSnackbar
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.MsTextField
import com.mindspring.app.ui.components.msSwitchColors
import com.mindspring.app.ui.screens.habits.TimePickerDialog
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.util.Fmt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

class ProfileViewModel(private val app: AppContainer) : ViewModel() {
    val user: StateFlow<User?> = app.auth.currentUser.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val darkMode: StateFlow<Boolean?> = app.settings.darkMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val reminder: StateFlow<ReminderSettings> = app.settings.checkInReminder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderSettings(true, LocalTime.of(20, 0)))

    fun setDarkMode(enabled: Boolean) = viewModelScope.launch { app.settings.setDarkMode(enabled) }
    fun setReminder(settings: ReminderSettings) = viewModelScope.launch { app.settings.setCheckInReminder(settings) }
    fun rename(name: String) = viewModelScope.launch { if (name.isNotBlank()) app.auth.updateName(name) }
    fun logout(then: () -> Unit) = viewModelScope.launch { app.auth.logout(); then() }

    /** Wipes habits, mood entries and journal entries. The account itself is kept. */
    fun clearAll(then: () -> Unit) = viewModelScope.launch {
        app.habits.clear()
        app.moods.clear()
        app.gratitude.clear()
        then()
    }
}

@Composable
fun ProfileScreen(systemDark: Boolean, onLoggedOut: () -> Unit) {
    val vm = appViewModel { ProfileViewModel(it) }
    val user by vm.user.collectAsStateWithLifecycle()
    val darkPref by vm.darkMode.collectAsStateWithLifecycle()
    val reminder by vm.reminder.collectAsStateWithLifecycle()
    val c = MsTheme.colors
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()

    var dialog by rememberSaveable { mutableStateOf<ProfileDialog?>(null) }
    val name = user?.name.orEmpty()

    Column(Modifier.fillMaxSize().background(c.canvas)) {
        BrandTopBar(name, onAvatarClick = { dialog = ProfileDialog.EditName })
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(Dimens.screen),
            verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
        ) {
            Column(Modifier.fillMaxWidth().padding(vertical = Dimens.stackMd), horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    AvatarCircle(
                        name,
                        size = 96.dp,
                        background = c.hero,
                        contentColor = c.onTeal,
                        modifier = Modifier.border(4.dp, c.card, CircleShape),
                    )
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(c.amber)
                            .clickable { dialog = ProfileDialog.EditName },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit name", tint = c.onAmber, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(Dimens.stackMd))
                Text(name, style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                user?.let {
                    Text("Member since ${Fmt.monthYear(it.memberSince)}", style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
                }
            }

            MsCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
                SettingsRow(Icons.Outlined.Person, "Edit Profile") { dialog = ProfileDialog.EditName }
                RowDivider()
                SettingsRow(
                    Icons.Outlined.Notifications,
                    "Reminders",
                    detail = if (reminder.enabled) "Daily at ${Fmt.time(reminder.time)}" else "Off",
                ) { dialog = ProfileDialog.Reminders }
                RowDivider()
                val dark = darkPref ?: systemDark
                SettingsRow(
                    Icons.Outlined.DarkMode,
                    "Dark Mode",
                    trailing = { Switch(checked = dark, onCheckedChange = { vm.setDarkMode(it) }, colors = msSwitchColors()) },
                ) { vm.setDarkMode(!dark) }
                RowDivider()
                SettingsRow(Icons.Outlined.Download, "Export Data") {
                    scope.launch { snackbar.showSnackbar("Export arrives with the database step") }
                }
                RowDivider()
                SettingsRow(Icons.Outlined.Info, "About") { dialog = ProfileDialog.About }
            }

            MsCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
                SettingsRow(Icons.AutoMirrored.Rounded.Logout, "Log Out", chevron = false) { dialog = ProfileDialog.Logout }
            }

            // The one destructive control in settings: isolated in its own card and rendered in red.
            MsCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
                SettingsRow(Icons.Rounded.DeleteForever, "Clear All Data", tint = c.danger, chevron = false) { dialog = ProfileDialog.Clear }
            }

            Text(
                "MindSpring is a self-reflection tool and not medical advice.",
                style = MaterialTheme.typography.labelSmall,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.stackMd),
            )
        }
    }

    when (dialog) {
        ProfileDialog.EditName -> EditNameDialog(name, onDismiss = { dialog = null }) { vm.rename(it); dialog = null }
        ProfileDialog.Reminders -> RemindersDialog(reminder, onDismiss = { dialog = null }) { vm.setReminder(it); dialog = null }
        ProfileDialog.About -> AlertDialog(
            onDismissRequest = { dialog = null },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Close") } },
            title = { Text("About MindSpring") },
            text = {
                Text(
                    "Version 1.0\n\nMindSpring connects your daily habits with how you feel, and shows you which routines " +
                        "go with your better days. Everything stays on this device.\n\n" +
                        "MindSpring is a self-reflection tool, not a medical product. If you are struggling, please reach out " +
                        "to a qualified professional or a local support service.",
                )
            },
        )
        ProfileDialog.Logout -> ConfirmDialog(
            title = "Log out?",
            message = "Your data stays on this device. You can sign back in at any time.",
            confirm = "Log Out",
            onDismiss = { dialog = null },
        ) { dialog = null; vm.logout(onLoggedOut) }
        ProfileDialog.Clear -> ConfirmDialog(
            title = "Clear all data?",
            message = "This permanently deletes every habit, mood check-in and journal entry on this device. Your account is kept.",
            confirm = "Clear Data",
            destructive = true,
            onDismiss = { dialog = null },
        ) {
            dialog = null
            vm.clearAll { scope.launch { snackbar.showSnackbar("All data cleared") } }
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
    trailing: (@Composable () -> Unit)? = null,
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
        when {
            trailing != null -> trailing()
            chevron -> Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary)
        }
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
private fun RemindersDialog(current: ReminderSettings, onDismiss: () -> Unit, onSave: (ReminderSettings) -> Unit) {
    val c = MsTheme.colors
    var enabled by rememberSaveable { mutableStateOf(current.enabled) }
    var time by rememberSaveable { mutableStateOf(current.time) }
    var picking by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily check-in reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.stackMd)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Remind me to check in", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { enabled = it }, colors = msSwitchColors())
                }
                Text(
                    Fmt.time(time),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (enabled) c.tealInk else c.textTertiary,
                    modifier = Modifier.clip(MaterialTheme.shapes.small).clickable(enabled = enabled) { picking = true }.padding(4.dp),
                )
                Text(
                    "Habit reminders are set on each habit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(ReminderSettings(enabled, time)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (picking) {
        TimePickerDialog(time, onDismiss = { picking = false }) { time = it; picking = false }
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
