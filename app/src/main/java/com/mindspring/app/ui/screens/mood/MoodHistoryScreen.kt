package com.mindspring.app.ui.screens.mood

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.data.model.MoodEntry
import com.mindspring.app.data.model.Period
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.EmptyState
import com.mindspring.app.ui.components.LocalSnackbar
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.SegmentedTabs
import com.mindspring.app.ui.components.TagChip
import com.mindspring.app.ui.components.TealTopBar
import com.mindspring.app.ui.components.color
import com.mindspring.app.ui.theme.CardShape
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.util.Fmt
import kotlinx.coroutines.launch

@Composable
fun MoodHistoryScreen(onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val vm = appViewModel { MoodHistoryViewModel(it.moods) }
    val period by vm.period.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val c = MsTheme.colors
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        TealTopBar("Mood History", onNavigate = onBack)
        LazyColumn(
            contentPadding = PaddingValues(Dimens.screen),
            verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
        ) {
            item {
                SegmentedTabs(Period.entries, period, { vm.period.value = it }, { it.label })
                Spacer(Modifier.height(Dimens.stackSm))
                Text(
                    "Tap an entry to edit it. Swipe left to delete.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textTertiary,
                )
            }
            val list = entries
            if (list != null && list.isEmpty()) {
                item {
                    EmptyState(Icons.Rounded.History, "No check-ins yet", "Entries from this ${period.label.lowercase()} will appear here.")
                }
            }
            items(list.orEmpty(), key = { it.id }) { entry ->
                SwipeToDeleteCard(
                    entry = entry,
                    onClick = { onEdit(entry.id) },
                    onDelete = {
                        vm.delete(entry)
                        scope.launch {
                            val result = snackbar.showSnackbar("Entry deleted", actionLabel = "Undo", duration = SnackbarDuration.Short)
                            if (result == SnackbarResult.ActionPerformed) vm.restore(entry)
                        }
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

/** Swiping left reveals red, the only use of red in the product: destructive actions only. */
@Composable
private fun SwipeToDeleteCard(entry: MoodEntry, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val c = MsTheme.colors
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) onDelete()
    }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        modifier = modifier.clip(CardShape),
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().clip(CardShape).background(c.danger).padding(horizontal = Dimens.screen),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = if (c.isDark) c.canvas else Color.White)
            }
        },
    ) {
        MoodEntryCard(entry, onClick)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodEntryCard(entry: MoodEntry, onClick: () -> Unit) {
    val c = MsTheme.colors
    MsCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(entry.mood.color))
            Spacer(Modifier.width(12.dp))
            Text(entry.mood.label, style = MaterialTheme.typography.titleLarge, color = c.textPrimary, modifier = Modifier.weight(1f))
            Text(Fmt.relative(entry.loggedAt), style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
        }
        if (entry.feelings.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entry.feelings.forEach { TagChip(it) }
            }
        }
        if (entry.note.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                entry.note,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
