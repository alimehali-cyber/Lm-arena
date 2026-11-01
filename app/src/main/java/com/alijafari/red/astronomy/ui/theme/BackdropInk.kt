package com.alijafari.red.astronomy.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import com.alijafari.red.astronomy.ui.backdrop.AppBackdropMode

/**
 * ============================================================================
 * INK FOR TEXT THAT HAS NO SURFACE UNDER IT
 * ============================================================================
 *
 * Almost every piece of text in this app sits on a surface the theme owns, so its contrast is the
 * theme's job. A handful do not: Home's feed titles are written straight onto whatever the user
 * chose to put behind the app — the theme background, the live sky, or one of the photographic
 * backdrops. Painting them in `textPrimary` is a guess about pixels nobody has seen, and on a bright
 * photo (or a midday sky in the light theme) that guess fails.
 *
 * So the backdrop's brightness is resolved once, at the root, from what the backdrop actually is
 * ([LocalBackdropLuminance]), and the text asks for the ink that wins on it ([backdropInkFor]).
 * That is a contrast decision made from a measured value, not from "the theme is dark, so the text
 * must be white".
 *
 * The halo is not a background: it is a text shadow in the *opposite* direction, a couple of pixels
 * wide, which buys contrast in the ambiguous middle of the range (a dusk sky at 0.5) where neither
 * pure black nor pure white has enough of it on its own. It is invisible on either end of the range,
 * which is why it can be left on unconditionally.
 */

/** Perceived brightness of whatever is behind the app's content: 0 = black, 1 = white. */
val LocalBackdropLuminance = staticCompositionLocalOf { 0f }

/**
 * The readable ink set for a backdrop of brightness [luminance], plus the halo that keeps the
 * middle of the range legible.
 */
@Immutable
data class BackdropInk(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    /** Text shadow: the opposite ink at low alpha. Zero alpha means "draw no shadow". */
    val halo: Color,
    /** True when the ink chosen is the dark one (so callers can tint a hairline the same way). */
    val isDarkInk: Boolean
)

/** The two inks every decision is made between. Both clear 4.5:1 against their own extreme. */
private val InkDark = Color(0xFF080A12)
private val InkLight = Color(0xFFF8FAFF)

/**
 * WCAG relative luminance of an sRGB colour (linearised, then 0.2126/0.7152/0.0722).
 *
 * Different from `inkedLuminance` on purpose: that one is the quick perceptual luma used to pick ink
 * for a surface the app owns, while this one is the quantity the contrast ratio is defined against.
 */
fun wcagLuminance(color: Color): Float {
    fun channel(c: Float): Float =
        if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    return 0.2126f * channel(color.red) + 0.7152f * channel(color.green) + 0.0722f * channel(color.blue)
}

/** WCAG contrast ratio between two colours, 1:1 … 21:1. */
fun wcagContrast(a: Color, b: Color): Float {
    val la = wcagLuminance(a)
    val lb = wcagLuminance(b)
    val hi = maxOf(la, lb)
    val lo = minOf(la, lb)
    return (hi + 0.05f) / (lo + 0.05f)
}

/** A neutral grey of brightness [luminance], which is all the contrast maths needs. */
private fun greyOf(luminance: Float): Color {
    val v = luminance.coerceIn(0f, 1f)
    return Color(v, v, v, 1f)
}

/**
 * The best ink for a backdrop of brightness [luminance].
 *
 * The choice is made by measured contrast ratio rather than by a threshold, so the crossover lands
 * where white and black actually swap places (≈0.18 in linear luminance) instead of wherever a
 * hand-picked constant happened to sit. The halo is the losing ink at an alpha that rises through
 * the middle of the range and vanishes at both ends, because that is exactly where it is needed.
 */
fun backdropInkFor(luminance: Float): BackdropInk {
    val backdrop = greyOf(luminance)
    val lightRatio = wcagContrast(InkLight, backdrop)
    val darkRatio = wcagContrast(InkDark, backdrop)
    val useDark = darkRatio >= lightRatio
    val ink = if (useDark) InkDark else InkLight
    val haloInk = if (useDark) Color.White else Color.Black

    // Needed most where the winner is weakest; gone once it is comfortably clear.
    val best = maxOf(lightRatio, darkRatio)
    val haloAlpha = ((7.0f - best) / 5.2f).coerceIn(0f, 1f) * 0.55f

    return BackdropInk(
        primary = ink,
        secondary = ink.copy(alpha = 0.82f),
        tertiary = ink.copy(alpha = 0.66f),
        halo = haloInk.copy(alpha = haloAlpha),
        isDarkInk = useDark
    )
}

/**
 * Brightness of the live-sky backdrop at this instant.
 *
 * The live sky is not a picture: it is drawn from the same ephemeris the rest of the app uses, so
 * its brightness is known rather than measured. A daylit sky is the brightest thing the app can put
 * behind text, astronomical night is the darkest, and twilight is the awkward middle — which is the
 * case the halo exists for.
 */
fun liveSkyLuminance(sunAltitudeDeg: Double, theme: SkyCanvasTheme = SkyCanvasTheme.REAL_SKY): Float {
    val day = sunAltitudeDeg
    val base = when {
        day >= 6.0 -> 0.62f          // full daylight: pale blue, the worst case
        day >= 0.0 -> 0.44f          // the Sun is up but low: warm and bright
        day >= -6.0 -> 0.26f         // civil twilight
        day >= -12.0 -> 0.14f        // nautical twilight
        day >= -18.0 -> 0.07f        // astronomical twilight
        else -> 0.04f                // night
    }
    return when (theme) {
        // A monochrome sky is drawn as paper by day and as ink at night; it is already near the ends.
        SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> if (day > 0.0) 0.78f else 0.06f
        SkyCanvasTheme.KIDS_WATERCOLOR -> (base + 0.10f).coerceAtMost(1f)
        SkyCanvasTheme.OBSERVATORY -> (base * 0.55f)
        SkyCanvasTheme.PAPERCRAFT_DIORAMA -> (base + 0.06f).coerceAtMost(1f)
        SkyCanvasTheme.ATMOSPHERIC_SKY, SkyCanvasTheme.REAL_SKY -> base
    }
}

/**
 * Brightness of the app backdrop, from what the backdrop *is*.
 *
 * [photoLuminance] is [AppBackdropLuminance]'s measured mean for the chosen photograph, so this
 * never has to guess about an image: the number was measured from the asset itself.
 */
fun appBackdropLuminance(
    mode: AppBackdropMode,
    themeBackground: Color,
    sunAltitudeDeg: Double,
    skyTheme: SkyCanvasTheme = SkyCanvasTheme.REAL_SKY
): Float = when (mode) {
    AppBackdropMode.NONE -> inkedLuminance(themeBackground)
    AppBackdropMode.LIVE_SKY -> liveSkyLuminance(sunAltitudeDeg, skyTheme)
    else -> measuredPhotoLuminance(mode)
}

/**
 * Measured mean brightness of each shipped backdrop photograph.
 *
 * Not an estimate: these are the mean sRGB luma of the assets themselves (`drawable-nodpi`), so a
 * photo the user picked can never surprise the ink. A new backdrop needs its number measured and
 * added here, which is exactly the moment to think about whether its text is readable.
 */
fun measuredPhotoLuminance(mode: AppBackdropMode): Float = when (mode) {
    AppBackdropMode.NGC_1929 -> 0.162f
    AppBackdropMode.CEPHEUS -> 0.202f
    AppBackdropMode.NONE -> 0f
    AppBackdropMode.LIVE_SKY -> 0f
}
