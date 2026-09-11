package com.mindspring.app.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.data.model.Period
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.ActivityBars
import com.mindspring.app.ui.components.BrandTopBar
import com.mindspring.app.ui.components.IconCircle
import com.mindspring.app.ui.components.MoodTrendChart
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.MsProgressBar
import com.mindspring.app.ui.components.SectionTitle
import com.mindspring.app.ui.components.SegmentedTabs
import com.mindspring.app.ui.components.color
import com.mindspring.app.ui.components.icon
import com.mindspring.app.ui.theme.CardShape
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(userName: String, onOpenHistory: () -> Unit, onOpenProfile: () -> Unit) {
    val vm = appViewModel { InsightsViewModel(it.habits, it.moods) }
    val s by vm.state.collectAsStateWithLifecycle()
    val c = MsTheme.colors

    Column(Modifier.fillMaxSize().background(c.canvas)) {
        BrandTopBar(userName, onAvatarClick = onOpenProfile)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(Dimens.screen),
            verticalArrangement = Arrangement.spacedBy(Dimens.stackMd),
        ) {
            Text("Your Insights", style = MaterialTheme.typography.headlineSmall, color = c.textPrimary)
            Text("Here is a summary of your recent activity.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            SegmentedTabs(Period.entries, s.period, { vm.period.value = it }, { it.label }, Modifier.padding(vertical = Dimens.stackSm))

            KeyInsightCard(s)

            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Dimens.gutter)) {
                MsCard(Modifier.weight(1f).fillMaxHeight()) {
                    Text("CURRENT STREAK", style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                    Spacer(Modifier.height(Dimens.stackSm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = c.amber)
                        Spacer(Modifier.width(4.dp))
                        Text("${s.bestStreak} Days", style = MaterialTheme.typography.headlineSmall, color = c.textPrimary)
                    }
                    Text(
                        s.bestStreakHabit ?: "Complete a habit to begin",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textTertiary,
                    )
                }
                MsCard(Modifier.weight(1f).fillMaxHeight()) {
                    Text("AVERAGE MOOD", style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                    Spacer(Modifier.height(Dimens.stackSm))
                    val mood = s.averageMood
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (mood != null) {
                            Icon(mood.icon, contentDescription = null, tint = mood.color)
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(mood?.label ?: "–", style = MaterialTheme.typography.headlineSmall, color = c.tealInk)
                    }
                    Text("Based on the last ${s.period.days} days", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                }
            }

            MsCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.stackMd)) {
                SectionTitle("Mood Trend")
                if (s.trend.size < 2) {
                    Text(
                        "Check in on a couple of days to see your trend.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                    )
                } else {
                    MoodTrendChart(s.trend, s.trendLabels)
                }
            }

            MsCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.stackMd)) {
                SectionTitle("This Week") {
                    Text("Habits done per day", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                }
                ActivityBars(s.weekActivity, s.weekLabels, highlightIndex = s.weekActivity.lastIndex)
            }

            MsCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Habit Completion")
                if (s.rates.isEmpty()) {
                    Text("Add a habit to see completion rates.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                }
                s.rates.forEach { rate ->
                    Column {
                        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                            Text(rate.habit.name, style = MaterialTheme.typography.labelMedium, color = c.textPrimary, modifier = Modifier.weight(1f))
                            Text(
                                "${(rate.rate * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = c.tealInk,
                            )
                        }
                        MsProgressBar(rate.rate, trackColor = c.cardMuted)
                    }
                }
            }

            MsCard(Modifier.fillMaxWidth(), onClick = onOpenHistory) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconCircle(Icons.Rounded.History, size = 40.dp, iconSize = 22.dp)
                    Spacer(Modifier.width(Dimens.stackMd))
                    Column(Modifier.weight(1f)) {
                        Text("Mood History", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                        Text("Review, edit or delete past check-ins", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary)
                }
            }
            Spacer(Modifier.height(Dimens.stackSm))
        }
    }
}

/**
 * Opens with a sentence, not a chart. Amber appears as the outline and icon only: amber text on
 * the light canvas would fail contrast (design report, section 3.5).
 */
@Composable
private fun KeyInsightCard(s: InsightsState) {
    val c = MsTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(c.card)
            .border(1.5.dp, c.amber, CardShape)
            .padding(Dimens.card),
    ) {
        Icon(
            Icons.Rounded.Lightbulb,
            contentDescription = null,
            tint = c.amber.copy(alpha = 0.18f),
            modifier = Modifier.size(44.dp).align(Alignment.TopEnd),
        )
        Column(Modifier.padding(end = 40.dp), verticalArrangement = Arrangement.spacedBy(Dimens.stackSm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = c.amber, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("KEY INSIGHT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = c.textSecondary)
            }
            val insight = s.keyInsight
            if (insight == null) {
                Text(
                    "Keep logging your mood and habits. After a few days we'll show you which routines go with your better days.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.textPrimary,
                )
            } else {
                Text(
                    buildAnnotatedString {
                        append("You rate your mood ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.tealInk)) { append("${insight.percentHigher}% higher") }
                        append(" on days you complete ")
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(insight.habitName) }
                        append(".")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.textPrimary,
                )
            }
        }
    }
}
