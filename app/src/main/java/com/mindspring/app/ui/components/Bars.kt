package com.mindspring.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme

/** Teal app bar with a centred title and optional navigation icon (back or close). */
@Composable
fun TealTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = Icons.AutoMirrored.Rounded.ArrowBack,
    onNavigate: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = MsTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MsTheme.chrome)
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 8.dp),
    ) {
        if (navigationIcon != null && onNavigate != null) {
            IconButton(onClick = onNavigate, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(navigationIcon, contentDescription = "Back", tint = c.onTeal)
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = c.onTeal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 56.dp),
        )
        Row(modifier = Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Teal bar for the main tabs: the two-tone wordmark and the user's avatar (which opens Profile). */
@Composable
fun BrandTopBar(userName: String, onAvatarClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = MsTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MsTheme.chrome)
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = Dimens.screen),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Wordmark(mindColor = c.onTeal, modifier = Modifier.weight(1f))
        AvatarCircle(
            name = userName,
            size = 36.dp,
            background = c.onTeal.copy(alpha = 0.18f),
            contentColor = c.onTeal,
            modifier = Modifier.clickable(onClick = onAvatarClick),
        )
    }
}

/** Light bar for focused tasks such as the mood check-in; the ambient backdrop shows through. */
@Composable
fun TaskTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    val c = MsTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 8.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = c.textPrimary, modifier = Modifier.align(Alignment.Center))
        Row(modifier = Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

enum class MainTab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Today("Today", Icons.Outlined.WbSunny, Icons.Rounded.WbSunny),
    Habits("Habits", Icons.Outlined.CheckCircle, Icons.Rounded.CheckCircle),
    Tasks("Tasks", Icons.Outlined.TaskAlt, Icons.Rounded.TaskAlt),
    Insights("Insights", Icons.Outlined.Insights, Icons.Rounded.Insights),
    Mind("Mind", Icons.Outlined.SelfImprovement, Icons.Rounded.SelfImprovement),
}

/**
 * Teal navigation bar. The active tab is signalled twice, by a filled icon and by amber, so state
 * never depends on colour alone; an amber pill glides to the chosen tab and the icon pops.
 */
@Composable
fun MsBottomBar(selected: MainTab?, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    val c = MsTheme.colors
    val tabs = MainTab.entries
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(MsTheme.chrome)
            .navigationBarsPadding()
            .height(72.dp),
    ) {
        val slot = maxWidth / tabs.size
        val index = selected?.ordinal ?: -1
        val indicatorX by animateDpAsState(
            slot * index.coerceAtLeast(0) + (slot - 40.dp) / 2,
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "navIndicator",
        )
        if (index >= 0) {
            Box(
                Modifier
                    .offset(x = indicatorX, y = 6.dp)
                    .width(40.dp)
                    .height(3.dp)
                    .background(c.amber, RoundedCornerShape(2.dp)),
            )
        }
        Row(Modifier.fillMaxWidth().fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEach { tab ->
                val active = tab == selected
                val color by animateColorAsState(if (active) c.amber else c.onTealMuted, label = "navColor")
                val pop by animateFloatAsState(
                    if (active) 1.12f else 1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "navPop",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(remember { MutableInteractionSource() }, indication = null) { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Spacer(Modifier.height(4.dp))
                    Icon(
                        if (active) tab.selectedIcon else tab.icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp).scale(pop),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/** Small circular icon button for use on the teal bars. */
@Composable
fun BarIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val c = MsTheme.colors
    IconButton(onClick = onClick) {
        Box(Modifier.size(36.dp).background(c.onTeal.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = c.onTeal, modifier = Modifier.size(20.dp))
        }
    }
}
