package com.mindspring.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mindspring.app.ui.theme.CardShape
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme
import com.mindspring.app.ui.theme.NearBlack

/**
 * Level-1 content card: 20dp radius, a frosted surface the ambient gradient glows through, and a
 * bright hairline edge that lifts it off the canvas (DESIGN.md "Elevation & Depth"). A soft
 * shadow drawn only outside the edge keeps depth without greying the translucent face.
 * Clickable cards scale to 98% while pressed, as the design system specifies.
 */
@Composable
fun MsCard(
    modifier: Modifier = Modifier,
    color: Color = MsTheme.colors.card,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(Dimens.card),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = MsTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "cardPress")
    val glass = color.alpha < 1f
    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(if (c.isDark) Modifier else Modifier.softShadow(CardShape))
            .clip(CardShape)
            .background(color)
            .then(if (glass) Modifier.border(1.dp, c.cardBorder, CardShape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(interaction, indication = null, onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

/**
 * A soft drop shadow painted only outside [shape], so translucent surfaces keep a clean face.
 * (An elevation shadow would show through the frosted card and grey it.)
 */
fun Modifier.softShadow(
    shape: Shape,
    color: Color = NearBlack.copy(alpha = 0.10f),
    blur: Dp = 22.dp,
    offsetY: Dp = 8.dp,
): Modifier = drawBehind {
    val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawBehind)) }
    clipPath(path, ClipOp.Difference) {
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                this.color = android.graphics.Color.TRANSPARENT
                setShadowLayer(blur.toPx(), 0f, offsetY.toPx(), color.toArgb())
            }
            canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
        }
    }
}

/** Tinted circular container for an icon, used for habit categories and settings rows. */
@Composable
fun IconCircle(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    background: Color = MsTheme.colors.cardMuted,
    tint: Color = MsTheme.colors.tealInk,
    iconSize: Dp = 24.dp,
) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Initials avatar; the app stores no photos, so identity is shown as a monogram. */
@Composable
fun AvatarCircle(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    background: Color = MsTheme.colors.cardMuted,
    contentColor: Color = MsTheme.colors.tealInk,
) {
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials.ifEmpty { "?" },
            color = contentColor,
            style = if (size >= 64.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Small read-only pill, e.g. feeling tags on a mood history card. */
@Composable
fun TagChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MsTheme.colors.textPrimary,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MsTheme.colors.cardMuted)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MsTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/** Uppercase caption used above form fields and stat tiles. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MsTheme.colors.textSecondary,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = Dimens.stackLg, horizontal = Dimens.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconCircle(icon, size = 72.dp, iconSize = 36.dp)
        Spacer(Modifier.height(Dimens.stackMd))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MsTheme.colors.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MsTheme.colors.textSecondary, textAlign = TextAlign.Center)
    }
}
