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
import androidx.compose.ui.tooling.preview.Preview
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
import com.mindspring.app.ui.preview.PreviewScreen
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

    LaunchedEffect(s.saved) { if (s.saved) onDone() }

    MoodCheckInContent(
        s = s,
        onMood = vm::onMood,
        onToggleFeeling = vm::onToggleFeeling,
        onNote = vm::onNote,
        onSave = vm::save,
        onDone = onDone,
    )
}

/**
 * The screen as pure state and callbacks, so it renders in a @Preview without a ViewModel behind
 * it. [MoodCheckInScreen] is the thin wrapper that supplies both from the app's data.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodCheckInContent(
    s: CheckInState,
    onMood: (Mood) -> Unit,
    onToggleFeeling: (String) -> Unit,
    onNote: (String) -> Unit,
    onSave: () -> Unit,
    onDone: () -> Unit,
) {
    val c = MsTheme.colors

    Column(Modifier.fillMaxSize()) {
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
                        MoodFaceButton(mood, selected = s.mood == mood, onClick = { onMood(mood) })
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
                        SelectChip(tag, selected = tag in s.feelings, onClick = { onToggleFeeling(tag) })
                    }
                }
            }

            MsCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.stackMd)) {
                Text("Add a note (optional)", style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                MsTextField(
                    value = s.note,
                    onValueChange = onNote,
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
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.screen, vertical = Dimens.stackMd),
        )
    }
}

// --- Previews -------------------------------------------------------------------------------

@Preview(name = "Mood check-in", showBackground = true, widthDp = 393, heightDp = 900)
@Composable
private fun MoodCheckInPreview() = PreviewScreen {
    MoodCheckInContent(
        s = CheckInState(mood = Mood.Good, feelings = setOf("Focused", "Grateful"), note = "Got a clean run at the proposal this morning."),
        onMood = {}, onToggleFeeling = {}, onNote = {}, onSave = {}, onDone = {},
    )
}

@Preview(name = "Mood check-in · empty", showBackground = true, widthDp = 393, heightDp = 900)
@Composable
private fun MoodCheckInEmptyPreview() = PreviewScreen {
    MoodCheckInContent(s = CheckInState(), onMood = {}, onToggleFeeling = {}, onNote = {}, onSave = {}, onDone = {})
}

@Preview(name = "Mood check-in · dark", showBackground = true, widthDp = 393, heightDp = 900)
@Composable
private fun MoodCheckInDarkPreview() = PreviewScreen(dark = true) {
    MoodCheckInContent(
        s = CheckInState(mood = Mood.Great, feelings = setOf("Calm")),
        onMood = {}, onToggleFeeling = {}, onNote = {}, onSave = {}, onDone = {},
    )
}
