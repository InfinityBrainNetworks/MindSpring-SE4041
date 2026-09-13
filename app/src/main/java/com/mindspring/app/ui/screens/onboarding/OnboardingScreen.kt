package com.mindspring.app.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mindspring.app.R
import com.mindspring.app.ui.components.GhostButton
import com.mindspring.app.ui.components.PrimaryButton
import com.mindspring.app.ui.preview.PreviewScreen
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme
import kotlinx.coroutines.launch

private data class Slide(val title: String, val body: String, val art: @Composable () -> Painter, val tinted: Boolean)

private val slides = listOf(
    // Privacy leads, because trust decides whether people record anything honest.
    Slide(
        "Your Private Sanctuary",
        "A safe space for your mind, entirely offline. Your thoughts remain with you.",
        { painterResource(R.drawable.ic_sprout) },
        tinted = false,
    ),
    Slide(
        "Build Better Habits",
        "Establish routines that nourish your well-being through gentle, daily guidance.",
        { rememberVectorPainter(Icons.Rounded.TaskAlt) },
        tinted = true,
    ),
    Slide(
        "Find Your Center",
        "Take a moment each day to reflect, breathe, and reconnect with yourself.",
        { rememberVectorPainter(Icons.Rounded.SelfImprovement) },
        tinted = true,
    ),
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val c = MsTheme.colors
    val pager = rememberPagerState { slides.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == slides.lastIndex

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Dimens.stackSm, vertical = Dimens.stackSm), horizontalArrangement = Arrangement.End) {
            GhostButton("Skip", onClick = onFinish)
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val slide = slides[page]
            Column(
                Modifier.fillMaxSize().padding(horizontal = Dimens.screen),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(256.dp)
                        .shadow(12.dp, CircleShape, ambientColor = c.textPrimary.copy(alpha = 0.05f), spotColor = c.textPrimary.copy(alpha = 0.08f))
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(c.card, c.cardSubtle)))
                        .border(1.dp, c.divider, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    val painter = slide.art()
                    if (slide.tinted) {
                        Icon(painter, contentDescription = null, tint = c.tealInk, modifier = Modifier.size(120.dp))
                    } else {
                        Image(painter, contentDescription = null, modifier = Modifier.size(140.dp))
                    }
                }
                Spacer(Modifier.height(Dimens.stackLg))
                Text(
                    slide.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = c.tealInk,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Dimens.stackSm))
                Text(
                    slide.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 340.dp),
                )
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(horizontal = Dimens.screen, vertical = Dimens.stackLg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(slides.size) { i ->
                    val active = i == pager.currentPage
                    val width by animateDpAsState(if (active) 24.dp else 8.dp, label = "dotWidth")
                    val color by animateColorAsState(if (active) c.amber else c.divider, label = "dotColor")
                    Box(Modifier.height(8.dp).size(width, 8.dp).clip(CircleShape).background(color))
                }
            }
            Spacer(Modifier.height(Dimens.stackLg))
            PrimaryButton(
                text = if (last) "Get Started" else "Next",
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                onClick = { if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp),
            )
        }
    }
}

// --- Previews -------------------------------------------------------------------------------

@Preview(name = "Onboarding", showBackground = true, widthDp = 393, heightDp = 830)
@Composable
private fun OnboardingScreenPreview() = PreviewScreen {
    OnboardingScreen(onFinish = {})
}

@Preview(name = "Onboarding · dark", showBackground = true, widthDp = 393, heightDp = 830)
@Composable
private fun OnboardingScreenDarkPreview() = PreviewScreen(dark = true) {
    OnboardingScreen(onFinish = {})
}
