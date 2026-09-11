package com.mindspring.app.ui.screens.calm

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mindspring.app.ui.components.BrandTopBar
import com.mindspring.app.ui.components.IconCircle
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.PrimaryButton
import com.mindspring.app.ui.components.SectionTitle
import com.mindspring.app.ui.components.appear
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme

@Composable
fun CalmScreen(
    userName: String,
    onBreathing: (technique: String) -> Unit,
    onGratitude: () -> Unit,
    onJournal: () -> Unit,
    onMoodHistory: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val c = MsTheme.colors
    Column(Modifier.fillMaxSize()) {
        BrandTopBar(userName, onAvatarClick = onOpenProfile)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(Dimens.screen),
            verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
        ) {
            Text("Mind", style = MaterialTheme.typography.headlineSmall, color = c.textPrimary)
            Text("Slow down, reflect, and notice what went well.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            Spacer(Modifier.height(Dimens.stackSm))

            MsCard(Modifier.fillMaxWidth().appear(0), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                BreathingArt(Modifier.fillMaxWidth().height(170.dp))
                Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(Dimens.stackSm)) {
                    Text(
                        "PRACTICE OF THE DAY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = c.textSecondary,
                        modifier = Modifier.clip(CircleShape).background(c.cardMuted).padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                    Text("Calm Breathing", style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                    Text(
                        "Breathe in for four, out for six. A longer exhale slows your heart rate and quiets a busy mind.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    PrimaryButton("Start Session", leadingIcon = Icons.Rounded.PlayArrow, onClick = { onBreathing(TECHNIQUE_CALM) })
                }
            }

            Spacer(Modifier.height(Dimens.stackSm))
            SectionTitle("Reflect")
            PracticeRow(Icons.Rounded.AutoStories, c.amber, c.onAmber, "Daily Reflection", "About 150 words on how today went", onJournal, Modifier.appear(1))
            PracticeRow(Icons.Rounded.EditNote, c.cardMuted, c.tealInk, "Gratitude Journal", "One good thing from today", onGratitude, Modifier.appear(2))
            PracticeRow(Icons.Rounded.History, c.cardMuted, c.tealInk, "Mood History", "Every check-in, with notes", onMoodHistory, Modifier.appear(3))
            Spacer(Modifier.height(Dimens.stackSm))
            SectionTitle("Breathe")
            PracticeRow(Icons.Rounded.Air, c.teal, c.onTeal, "Calm Breathing", "Slow, steady breaths · 1–5 min", { onBreathing(TECHNIQUE_CALM) }, Modifier.appear(4))
            PracticeRow(Icons.Rounded.CenterFocusStrong, c.teal, c.onTeal, "Box Breathing", "4-4-4-4 technique for focus", { onBreathing(TECHNIQUE_BOX) }, Modifier.appear(5))
            Spacer(Modifier.height(Dimens.stackSm))
        }
    }
}

@Composable
private fun PracticeRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MsTheme.colors
    MsCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCircle(icon, background = iconBg, tint = iconTint)
            Spacer(Modifier.width(Dimens.stackMd))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary)
        }
    }
}

/** Illustration for the featured session: slowly breathing rings on a teal gradient. */
@Composable
private fun BreathingArt(modifier: Modifier) {
    val c = MsTheme.colors
    val breathe by rememberInfiniteTransition(label = "art").animateFloat(
        initialValue = 0.85f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "artScale",
    )
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(c.teal, Color(0xFF0A312E)))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = this.center
            listOf(1f, 0.72f, 0.46f).forEachIndexed { i, f ->
                drawCircle(
                    color = c.amber.copy(alpha = 0.18f + i * 0.14f),
                    radius = size.minDimension * 0.5f * f * breathe,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
            drawCircle(c.amber.copy(alpha = 0.9f), radius = size.minDimension * 0.16f * breathe, center = center)
        }
        Icon(Icons.Rounded.SelfImprovement, contentDescription = null, tint = c.onAmber.copy(alpha = 0.6f), modifier = Modifier.size(28.dp))
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Timer, contentDescription = null, tint = Color(0xFF141D1B), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("3 min", style = MaterialTheme.typography.labelSmall, color = Color(0xFF141D1B))
        }
    }
}

const val TECHNIQUE_CALM = "calm"
const val TECHNIQUE_BOX = "box"
