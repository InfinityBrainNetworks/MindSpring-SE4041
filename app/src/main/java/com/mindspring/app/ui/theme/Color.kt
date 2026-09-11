package com.mindspring.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Brand palette (see the design report, section 3.1).
val Teal = Color(0xFF10514C)
val TealDeep = Color(0xFF0B3F3B)
val TealBright = Color(0xFF1A6B62)
val Amber = Color(0xFFE8A33D)
val OffWhite = Color(0xFFF4F6F1)
val NearBlack = Color(0xFF1B2422)

/**
 * Semantic colours for the 60-30-10 system. The Material colour scheme is derived from these,
 * but screens read these names directly so every surface has one obvious role.
 */
@Immutable
data class MsColors(
    val canvas: Color,        // ~60%: screen background, beneath the ambient gradient
    val card: Color,          // raised content cards (slightly translucent: the gradient glows through)
    val cardBorder: Color,    // hairline that gives the glass card its edge
    val cardMuted: Color,     // chips, progress tracks, inactive fills
    val cardSubtle: Color,    // tinted rows (e.g. completed habits)
    val teal: Color,          // ~30%: app bars, bottom navigation
    val onTeal: Color,
    val onTealMuted: Color,
    val tealInk: Color,       // teal used as ink on the canvas: checks, progress, headings
    val hero: Color,          // hero / streak cards
    val amber: Color,         // ~10%: the one primary action, streaks, highlights
    val onAmber: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
    val danger: Color,
    val dangerSoft: Color,
    val warnSoft: Color,
    /** Soft light sources that drift behind every screen. */
    val ambient: List<Color>,
    val isDark: Boolean,
)

val LightMsColors = MsColors(
    canvas = OffWhite,
    card = Color.White.copy(alpha = 0.84f),
    cardBorder = Color.White.copy(alpha = 0.9f),
    cardMuted = Color(0xFFE0EAE7),
    cardSubtle = Color(0xFFECF6F2),
    teal = Teal,
    onTeal = Color.White,
    onTealMuted = Color(0xB3EAF2EF),
    tealInk = Teal,
    hero = Teal,
    amber = Amber,
    onAmber = NearBlack,
    textPrimary = Color(0xFF141D1B),
    textSecondary = Color(0xFF404947),
    textTertiary = Color(0xFF6A7371),
    divider = Color(0xFFDBE5E1),
    danger = Color(0xFFB3261E),
    dangerSoft = Color(0xFFF9DEDC),
    warnSoft = Color(0xFFFCE8C3),
    ambient = listOf(Color(0xFFBFE3D7), Color(0xFFF7D9AE), Color(0xFFD5E6C8), Color(0xFFB9DDE0)),
    isDark = false,
)

// Dark mode re-assigns roles rather than inverting: near-black canvas, cards one step lighter,
// teal chrome kept, amber at full strength (design report, section 3.4).
val DarkMsColors = MsColors(
    canvas = Color(0xFF141C1A),
    card = Color(0xFF26312E).copy(alpha = 0.86f),
    cardBorder = Color.White.copy(alpha = 0.06f),
    cardMuted = Color(0xFF33403C),
    cardSubtle = Color(0xFF2C3835),
    teal = Teal,
    onTeal = Color.White,
    onTealMuted = Color(0xB3EAF2EF),
    tealInk = Color(0xFF7CC4B9),
    hero = Color(0xFF1D5550),
    amber = Amber,
    onAmber = NearBlack,
    textPrimary = Color(0xFFE9F3EF),
    textSecondary = Color(0xFFB5C2BE),
    textTertiary = Color(0xFF8A9894),
    divider = Color(0xFF3A4744),
    danger = Color(0xFFFFB4AB),
    dangerSoft = Color(0xFF4A2522),
    warnSoft = Color(0xFF45371E),
    ambient = listOf(Color(0xFF1C5049), Color(0xFF4A3818), Color(0xFF173C42), Color(0xFF27402B)),
    isDark = true,
)

/** Five-point mood scale colours, from the Stitch mood selector. */
object MoodColors {
    val Awful = Color(0xFFC25E5E)
    val Bad = Color(0xFFE28C65)
    val Okay = Color(0xFFF3C363)
    val Good = Color(0xFF8EBAA3)
    val Great = Color(0xFF4E8F7C)
}

/**
 * Colours for life areas and projects: muted, calm hues that read on both the light and the
 * dark canvas. Areas store an index into this list.
 */
val AreaPalette = listOf(
    Color(0xFF3E9A84), // sea green
    Color(0xFF8A7BC8), // lavender
    Color(0xFFD97B8C), // rose
    Color(0xFF4A8CC0), // sky
    Color(0xFFE0A03C), // amber
    Color(0xFFE07E58), // coral
    Color(0xFF6FA35A), // leaf
    Color(0xFFA78B6B), // sand
    Color(0xFF4FA9B3), // lagoon
    Color(0xFFB8739E), // orchid
)

fun areaColor(index: Int): Color = AreaPalette[Math.floorMod(index, AreaPalette.size)]
