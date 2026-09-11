package com.mindspring.app.ui.screens.habits

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.PrimaryButton
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme

/** The app's one moment of celebration: credits consistency, never perfection. */
@Composable
fun HabitCompletedScreen(habitId: Long, onDone: () -> Unit) {
    val vm = appViewModel { HabitCompletedViewModel(it.auth, it.habits, habitId) }
    val s by vm.state.collectAsStateWithLifecycle()
    val c = MsTheme.colors

    val pop = remember { Animatable(0.3f) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) { fade.animateTo(1f, tween(600, delayMillis = 250)) }
    val twinkle by rememberInfiniteTransition(label = "twinkle").animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "sparkle",
    )

    Box(Modifier.fillMaxSize().background(c.canvas)) {
        // Soft ambient glow behind the content.
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(listOf(c.amber.copy(alpha = 0.18f), Color.Transparent), center = Offset(size.width * 0.5f, size.height * 0.4f), radius = size.width * 0.8f),
                radius = size.width * 0.8f, center = Offset(size.width * 0.5f, size.height * 0.4f),
            )
            drawCircle(
                Brush.radialGradient(listOf(c.tealInk.copy(alpha = 0.12f), Color.Transparent), center = Offset(size.width * 0.9f, size.height * 0.8f), radius = size.width * 0.7f),
                radius = size.width * 0.7f, center = Offset(size.width * 0.9f, size.height * 0.8f),
            )
        }

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(Dimens.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }, contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(192.dp)
                        .shadow(24.dp, CircleShape, ambientColor = c.textPrimary.copy(alpha = 0.08f), spotColor = c.textPrimary.copy(alpha = 0.16f))
                        .clip(CircleShape)
                        .background(c.card),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = c.tealInk, modifier = Modifier.size(104.dp))
                }
                Icon(
                    Icons.Rounded.AutoAwesome, contentDescription = null, tint = c.amber,
                    modifier = Modifier.offset((-96).dp, (-88).dp).size(40.dp).graphicsLayer { scaleX = twinkle; scaleY = twinkle },
                )
                Icon(
                    Icons.Rounded.Star, contentDescription = null, tint = c.tealInk,
                    modifier = Modifier.offset(104.dp, (-40).dp).size(30.dp).graphicsLayer { scaleX = 2f - twinkle; scaleY = 2f - twinkle },
                )
                Icon(
                    Icons.Rounded.AutoAwesome, contentDescription = null, tint = c.amber.copy(alpha = 0.8f),
                    modifier = Modifier.offset((-72).dp, 96.dp).size(24.dp),
                )
            }

            Spacer(Modifier.height(Dimens.stackLg + Dimens.stackMd))
            Column(
                Modifier.graphicsLayer { alpha = fade.value; translationY = (1f - fade.value) * 40f },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val name = s?.userFirstName.orEmpty()
                Text(
                    if (name.isBlank()) "Awesome work!" else "Awesome work, $name!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = c.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Dimens.stackSm))
                Text(
                    "You completed ${s?.habitName ?: "your habit"}. You're one step closer to your goal. " +
                        "Consistency is the key to lasting vitality.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 340.dp),
                )
                val streak = s?.streak ?: 0
                if (streak > 0) {
                    Spacer(Modifier.height(Dimens.stackMd))
                    Row(
                        Modifier.clip(CircleShape).background(c.cardMuted).padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = c.amber, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "$streak day streak",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = c.textPrimary,
                        )
                    }
                }
                Spacer(Modifier.height(Dimens.stackLg))
                PrimaryButton("Done", onClick = onDone, modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp))
            }
        }
    }
}
