package com.mindspring.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mindspring.app.ui.theme.MsTheme

/**
 * Smooth mood trend line on a 1-5 scale with a dashed average baseline and amber data points.
 * The line draws itself in when the data changes.
 */
@Composable
fun MoodTrendChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
) {
    val c = MsTheme.colors
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(values) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(900))
    }
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            if (values.isEmpty()) return@Canvas
            val padX = 8.dp.toPx()
            val padY = 12.dp.toPx()
            val w = size.width - padX * 2
            val h = size.height - padY * 2
            fun x(i: Int) = padX + if (values.size == 1) w / 2 else w * i / (values.size - 1)
            fun y(v: Float) = padY + h * (1f - (v - 1f) / 4f)

            // Faint guides for ratings 1..5
            for (r in 1..5) {
                drawLine(c.divider.copy(alpha = 0.6f), Offset(padX, y(r.toFloat())), Offset(padX + w, y(r.toFloat())), 1f)
            }
            val avg = values.average().toFloat()
            drawLine(
                color = c.textTertiary.copy(alpha = 0.6f),
                start = Offset(padX, y(avg)),
                end = Offset(padX + w, y(avg)),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
            )

            val points = values.mapIndexed { i, v -> Offset(x(i), y(v)) }
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val p0 = points[i - 1]
                    val p1 = points[i]
                    val midX = (p0.x + p1.x) / 2
                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                }
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val partial = Path()
            measure.getSegment(0f, measure.length * reveal.value, partial, true)
            drawPath(partial, c.tealInk, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

            val dotEvery = if (points.size > 14) points.size / 7 else 1
            points.forEachIndexed { i, p ->
                val visible = points.size == 1 || i.toFloat() / (points.size - 1) <= reveal.value
                if (visible && (i % dotEvery == 0 || i == points.lastIndex)) {
                    drawCircle(c.card, radius = 6.dp.toPx(), center = p)
                    drawCircle(c.amber, radius = 4.5.dp.toPx(), center = p)
                }
            }
        }
        if (labels.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                labels.forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = c.textTertiary) }
            }
        }
    }
}

/** Vertical bar chart where only the highlighted bar is amber (dark Insights design). */
@Composable
fun ActivityBars(
    values: List<Float>,
    labels: List<String>,
    highlightIndex: Int,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
) {
    val c = MsTheme.colors
    val grow = remember { Animatable(0f) }
    LaunchedEffect(values) {
        grow.snapTo(0f)
        grow.animateTo(1f, tween(700))
    }
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            if (values.isEmpty()) return@Canvas
            val max = values.max().coerceAtLeast(0.01f)
            val slot = size.width / values.size
            val barW = slot * 0.55f
            values.forEachIndexed { i, v ->
                val barH = (size.height * (v / max) * grow.value).coerceAtLeast(4.dp.toPx())
                val left = slot * i + (slot - barW) / 2
                drawRoundRect(
                    color = if (i == highlightIndex) c.amber else c.tealInk.copy(alpha = if (c.isDark) 0.45f else 0.8f),
                    topLeft = Offset(left, size.height - barH),
                    size = androidx.compose.ui.geometry.Size(barW, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                )
            }
        }
        if (values.size <= 12) {
            Row(Modifier.fillMaxWidth()) {
                labels.forEachIndexed { i, label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (i == highlightIndex) c.amber else c.textTertiary,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        } else {
            // A month of bars leaves each slot too narrow for a label, so the few labels given
            // are drawn centred under their bar, free to spill into the empty slots beside it.
            val measurer = rememberTextMeasurer()
            val style = MaterialTheme.typography.labelSmall
            Canvas(Modifier.fillMaxWidth().height(18.dp)) {
                val slot = size.width / values.size
                labels.forEachIndexed { i, label ->
                    if (label.isEmpty()) return@forEachIndexed
                    val layout = measurer.measure(label, style.copy(color = if (i == highlightIndex) c.amber else c.textTertiary))
                    val x = (slot * i + slot / 2 - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
                    drawText(layout, topLeft = Offset(x, 2.dp.toPx()))
                }
            }
        }
    }
}
