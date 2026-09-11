package com.mindspring.app.ui.screens.habits

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.BrandTopBar
import com.mindspring.app.ui.components.EmptyState
import com.mindspring.app.ui.components.IconCircle
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.WeekDots
import com.mindspring.app.ui.components.vector
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme

@Composable
fun HabitsListScreen(
    userName: String,
    onOpenHabit: (Long) -> Unit,
    onAddHabit: () -> Unit,
    onCompleted: (Long) -> Unit,
    onOpenProfile: () -> Unit,
) {
    val vm = appViewModel { HabitsViewModel(it.habits) }
    val cards by vm.cards.collectAsStateWithLifecycle()
    val c = MsTheme.colors

    Box(Modifier.fillMaxSize().background(c.canvas)) {
        Column(Modifier.fillMaxSize()) {
            BrandTopBar(userName, onAvatarClick = onOpenProfile)
            LazyColumn(
                contentPadding = PaddingValues(start = Dimens.screen, end = Dimens.screen, top = Dimens.stackLg, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
            ) {
                item {
                    Text("Habits", style = MaterialTheme.typography.headlineSmall, color = c.textPrimary)
                    Text(
                        "Tap a card for details, or the button to complete it today.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                }
                val list = cards
                if (list != null && list.isEmpty()) {
                    item {
                        EmptyState(Icons.Rounded.EventAvailable, "No habits yet", "Tap the + button to create your first habit.")
                    }
                }
                items(list.orEmpty(), key = { it.habit.id }) { card ->
                    HabitCard(
                        card,
                        onClick = { onOpenHabit(card.habit.id) },
                        onAction = {
                            vm.setDone(card.habit.id, !card.doneToday)
                            if (!card.doneToday) onCompleted(card.habit.id)
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = onAddHabit,
            containerColor = c.amber,
            contentColor = c.onAmber,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(6.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(Dimens.screen),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add habit")
        }
    }
}

@Composable
private fun HabitCard(card: HabitCardState, onClick: () -> Unit, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val c = MsTheme.colors
    MsCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCircle(card.habit.icon.vector)
            Spacer(Modifier.width(Dimens.stackMd))
            Column(Modifier.weight(1f)) {
                Text(card.habit.name, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Text(
                    "${card.weekDone} of ${card.weekTarget} this week",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(Dimens.stackMd))
        Row(verticalAlignment = Alignment.CenterVertically) {
            WeekDots(card.weekTarget, card.weekDone, Modifier.weight(1f))
            CompleteButton(card, onAction)
        }
    }
}

/**
 * The card's single action. Teal = complete today; amber = done today (tap to undo);
 * grey double-check = done today and the weekly target is met; outline = rest day.
 */
@Composable
private fun CompleteButton(card: HabitCardState, onClick: () -> Unit) {
    val c = MsTheme.colors
    val (bg, fg) = when {
        card.doneToday && card.weekMet -> c.cardMuted to c.textTertiary
        card.doneToday -> c.amber to c.onAmber
        card.scheduledToday -> c.teal to c.onTeal
        else -> Color.Transparent to c.tealInk
    }
    val animatedBg by animateColorAsState(bg, label = "completeBg")
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(animatedBg)
            .then(if (!card.doneToday && !card.scheduledToday) Modifier.border(1.dp, c.divider, CircleShape) else Modifier),
    ) {
        Icon(
            if (card.doneToday && card.weekMet) Icons.Rounded.DoneAll else Icons.Rounded.Check,
            contentDescription = if (card.doneToday) "Mark as not done" else "Mark as done",
            tint = fg,
        )
    }
}
