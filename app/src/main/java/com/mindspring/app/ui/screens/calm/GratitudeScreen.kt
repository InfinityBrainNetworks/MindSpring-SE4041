package com.mindspring.app.ui.screens.calm

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mindspring.app.data.model.GratitudeEntry
import com.mindspring.app.data.repository.GratitudeRepository
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.EmptyState
import com.mindspring.app.ui.components.GhostButton
import com.mindspring.app.ui.components.LocalSnackbar
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.MsTextField
import com.mindspring.app.ui.components.PrimaryButton
import com.mindspring.app.ui.components.SectionTitle
import com.mindspring.app.ui.components.TealTopBar
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.util.Fmt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GratitudeViewModel(private val repo: GratitudeRepository) : ViewModel() {
    val entries: StateFlow<List<GratitudeEntry>?> =
        repo.entries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun add(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { repo.add(text) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }
}

/** Replaces the intimidating blank page with one specific prompt. */
@Composable
fun GratitudeScreen(onBack: () -> Unit) {
    val vm = appViewModel { GratitudeViewModel(it.gratitude) }
    val entries by vm.entries.collectAsStateWithLifecycle()
    val c = MsTheme.colors
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf<Long?>(null) }

    Column(Modifier.fillMaxSize().background(c.canvas)) {
        TealTopBar("Gratitude Journal", onNavigate = onBack)
        LazyColumn(
            modifier = Modifier.imePadding(),
            contentPadding = PaddingValues(Dimens.screen),
            verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
        ) {
            item {
                MsCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.stackSm)) {
                    Text("Name one thing that went well today", style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                    Text(
                        "Take a moment to reflect on a positive aspect of your day, no matter how small.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    MsTextField(
                        value = text,
                        onValueChange = { text = it.take(500) },
                        placeholder = "Today I am grateful for...",
                        singleLine = false,
                        minLines = 4,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        PrimaryButton(
                            "Save Entry",
                            leadingIcon = Icons.Rounded.Save,
                            enabled = text.isNotBlank(),
                            onClick = {
                                vm.add(text)
                                text = ""
                                scope.launch { snackbar.showSnackbar("Entry saved") }
                            },
                        )
                    }
                }
            }
            item {
                Spacer(Modifier.height(Dimens.stackSm))
                SectionTitle("Past Entries")
            }
            val list = entries
            if (list != null && list.isEmpty()) {
                item {
                    EmptyState(Icons.Rounded.VolunteerActivism, "Your journal is empty", "Entries you save will collect here.")
                }
            }
            items(list.orEmpty(), key = { it.id }) { entry ->
                val open = expanded == entry.id
                MsCard(
                    Modifier.fillMaxWidth().animateContentSize().animateItem(),
                    onClick = { expanded = if (open) null else entry.id },
                    verticalArrangement = Arrangement.spacedBy(Dimens.stackSm),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Event, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(Fmt.journalDate(entry.createdAt), style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                    }
                    Text(
                        entry.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textPrimary,
                        maxLines = if (open) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (open) {
                        GhostButton("Delete entry", onClick = { vm.delete(entry.id) }, color = c.danger, modifier = Modifier.align(Alignment.End))
                    }
                }
            }
        }
    }
}
