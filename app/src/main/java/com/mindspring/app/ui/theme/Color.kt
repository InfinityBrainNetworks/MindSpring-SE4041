package com.mindspring.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Brand palette (see the design report, section 3.1).
val Teal = Color(0xFF10514C)
val Amber = Color(0xFFE8A33D)
val OffWhite = Color(0xFFF4F6F1)
val NearBlack = Color(0xFF1B2422)

/**
 * Semantic colours for the 60-30-10 system. The Material colour scheme is derived from these,
 * but screens read these names directly so every surface has one obvious role.
 */
@Immutable
data class MsColors(
    val canvas: Color,        // ~60%: screen background
    val card: Color,          // raised content cards
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
    val isDark: Boolean,
)

val LightMsColors = MsColors(
    canvas = OffWhite,
    card = Color.White,
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
    textTertiary = Color(0xFF707977),
    divider = Color(0xFFDBE5E1),
    danger = Color(0xFFBA1A1A),
    isDark = false,
)

// Dark mode re-assigns roles rather than inverting: near-black canvas, cards one step lighter,
// teal chrome kept, amber at full strength (design report, section 3.4).
val DarkMsColors = MsColors(
    canvas = NearBlack,
    card = Color(0xFF26312E),
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
