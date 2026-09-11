package com.mindspring.app.ui.screens.mood

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.data.model.FeelingTags
import com.mindspring.app.data.model.Mood
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.MoodFaceButton
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.MsTextField
import com.mindspring.app.ui.components.PrimaryButton
import com.mindspring.app.ui.components.SelectChip
import com.mindspring.app.ui.components.TaskTopBar
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme

/**
 * Three progressively optional steps: one tap for the face, optional feeling chips, optional note.
 * A complete entry takes two taps (face, then Save).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodCheckInScreen(entryId: Long?, initialRating: Int?, onDone: () -> Unit) {
    val vm = appViewModel { MoodCheckInViewModel(it.moods, entryId, initialRating) }
    val s by vm.state.collectAsStateWithLifecycle()
    val c = MsTheme.colors

    LaunchedEffect(s.saved) { if (s.saved) onDone() }

    Column(Modifier.fillMaxSize().background(c.canvas)) {
        TaskTopBar(if (s.isEditing) "Edit Check-in" else "Mood Check-in", onBack = onDone)
        Column(
            Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screen, vertical = Dimens.stackLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.stackLg),
        ) {
            MsCard(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("How was your day?", style = MaterialTheme.typography.headlineMedium, color = c.textPrimary, textAlign = TextAlign.Center)
                Row(
                    Modifier.fillMaxWidth().widthIn(max = 360.dp).padding(top = 24.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Mood.entries.forEach { mood ->
                        MoodFaceButton(mood, selected = s.mood == mood, onClick = { vm.onMood(mood) })
                    }
                }
                AnimatedContent(s.mood, label = "moodLabel") { mood ->
                    Text(
                        mood?.label ?: "Tap a face",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (mood == null) c.textTertiary else c.tealInk,
                    )
                }
            }

            MsCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.stackMd)) {
                Text("What feelings stood out?", style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeelingTags.forEach { tag ->
                        SelectChip(tag, selected = tag in s.feelings, onClick = { vm.onToggleFeeling(tag) })
                    }
                }
            }

            MsCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.stackMd)) {
                Text("Add a note (optional)", style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                MsTextField(
                    value = s.note,
                    onValueChange = vm::onNote,
                    placeholder = "Jot down some thoughts about today...",
                    singleLine = false,
                    minLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }
        }
        PrimaryButton(
            text = "Save Entry",
            trailingIcon = Icons.Rounded.Check,
            enabled = s.mood != null,
            onClick = vm::save,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.screen, vertical = Dimens.stackMd),
        )
    }
}
