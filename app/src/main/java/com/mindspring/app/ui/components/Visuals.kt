package com.mindspring.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.SentimentNeutral
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.SentimentVeryDissatisfied
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mindspring.app.R
import com.mindspring.app.data.model.HabitIcon
import com.mindspring.app.data.model.Mood
import com.mindspring.app.ui.theme.MoodColors
import com.mindspring.app.ui.theme.MsTheme

val Mood.color: Color
    get() = when (this) {
        Mood.Awful -> MoodColors.Awful
        Mood.Bad -> MoodColors.Bad
        Mood.Okay -> MoodColors.Okay
        Mood.Good -> MoodColors.Good
        Mood.Great -> MoodColors.Great
    }

val Mood.icon: ImageVector
    get() = when (this) {
        Mood.Awful -> Icons.Rounded.SentimentVeryDissatisfied
        Mood.Bad -> Icons.Rounded.SentimentDissatisfied
        Mood.Okay -> Icons.Rounded.SentimentNeutral
        Mood.Good -> Icons.Rounded.SentimentSatisfied
        Mood.Great -> Icons.Rounded.SentimentVerySatisfied
    }

val Mood.emoji: String
    get() = when (this) {
        Mood.Awful -> "😫"
        Mood.Bad -> "😕"
        Mood.Okay -> "😐"
        Mood.Good -> "🙂"
        Mood.Great -> "😄"
    }

val HabitIcon.vector: ImageVector
    get() = when (this) {
        HabitIcon.Water -> Icons.Rounded.WaterDrop
        HabitIcon.Run -> Icons.AutoMirrored.Rounded.DirectionsRun
        HabitIcon.Book -> Icons.AutoMirrored.Rounded.MenuBook
        HabitIcon.Meditate -> Icons.Rounded.SelfImprovement
        HabitIcon.Sleep -> Icons.Rounded.Bed
        HabitIcon.Spa -> Icons.Rounded.Spa
        HabitIcon.Heart -> Icons.Rounded.Favorite
        HabitIcon.Flower -> Icons.Rounded.LocalFlorist
        HabitIcon.Food -> Icons.Rounded.Restaurant
        HabitIcon.School -> Icons.Rounded.School
    }

/**
 * Mood face selector button. When selected it grows slightly, takes its mood-scale colour and
 * gains an amber ring (DESIGN.md "Mood Selector").
 */
@Composable
fun MoodFaceButton(
    mood: Mood,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val c = MsTheme.colors
    val scale by animateFloatAsState(if (selected) 1.12f else 1f, label = "moodScale")
    val bg by animateColorAsState(if (selected) mood.color else c.cardMuted, label = "moodBg")
    val tint = if (selected) (if (mood == Mood.Okay) c.onAmber else Color.White) else c.textSecondary
    Box(
        modifier = modifier
            .scale(scale)
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .then(if (selected) Modifier.border(2.dp, c.amber, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(mood.icon, contentDescription = mood.label, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Two-tone "MindSpring" wordmark with the sprout mark, readable in both themes. */
@Composable
fun Wordmark(
    modifier: Modifier = Modifier,
    mindColor: Color = MsTheme.colors.tealInk,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    markSize: Dp = 26.dp,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_sprout), contentDescription = null, modifier = Modifier.size(markSize))
        Box(Modifier.width(6.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = mindColor)) { append("Mind") }
                withStyle(SpanStyle(color = MsTheme.colors.amber)) { append("Spring") }
            },
            style = style,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Seven-dot week strip: filled dots are completed days; amber when the week target is met. */
@Composable
fun WeekDots(target: Int, completed: Int, modifier: Modifier = Modifier) {
    val c = MsTheme.colors
    val met = target > 0 && completed >= target
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(target.coerceAtLeast(1)) { i ->
            val color = when {
                i >= completed -> c.cardMuted
                met -> c.amber
                else -> c.tealInk
            }
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        }
    }
}

/** Rounded linear progress bar; teal for completion, amber for streak-style progress. */
@Composable
fun MsProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MsTheme.colors.tealInk,
    trackColor: Color = MsTheme.colors.textTertiary.copy(alpha = 0.15f),
    height: Dp = 8.dp,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress")
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(trackColor)) {
        Box(Modifier.fillMaxWidth(animated).height(height).clip(CircleShape).background(color))
    }
}
