package com.alijafari.red.astronomy.ui.aurora

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.alijafari.red.astronomy.domain.ThemeMode
import kotlin.math.PI
import kotlin.math.sin

// ============================================================================
// THE AURORA FIELD
// ============================================================================
//
// The ground every Aurora surface floats over, and the one piece of the language
// that is never still. It is drawn rather than composed: three curtains of light
// drifting across a starfield, on a single shared clock.
//
// Two rules from the Astryx specs govern it directly:
//
//  - Motion DR1 — motion carries meaning. The field's drift is the only motion
//    here that decorates rather than explains, so it is the slowest thing in the
//    system (one cycle per [AuroraMotion.AMBIENT_MS]) and it never moves content.
//  - Motion DR5 / reduced motion — the drift has an immediate form. Under a
//    reduced-motion preference the phase is pinned and the field simply *is*,
//    at a composed position, rather than being removed.
//
// It also obeys elevation-hierarchy DR2 without being asked: the field is the
// reference plane, so it is quieter than every surface on top of it and never
// borrows their rim or shadow.

private const val RIBBON_COUNT = 3
/** Columns sampled across the width. Enough to read as cloth, few enough to stay cheap. */
private const val RIBBON_COLUMNS = 56
private const val STAR_COUNT = 140

/** One deterministic point of light, resolved once per field and never re-randomised. */
private data class AuroraStar(val x: Float, val y: Float, val radiusPx: Float, val alpha: Float)

/**
 * A fixed, reproducible starfield.
 *
 * A freshly-seeded generator would re-randomise the sky on every recomposition,
 * so this uses a small linear congruential generator pinned to a constant seed:
 * the same sky every time, in every theme, on every device.
 */
private fun auroraStars(): List<AuroraStar> {
    val stars = ArrayList<AuroraStar>(STAR_COUNT)
    var state = 0x2F6E2B1
    fun next(): Float {
        state = (state * 1103515245 + 12345) and 0x7FFFFFFF
        return (state ushr 8) / 8388608f
    }
    repeat(STAR_COUNT) {
        val roll = next()
        stars.add(
            AuroraStar(
                x = next(),
                // Bias upward: a star near the bottom edge is just noise.
                y = next() * 0.82f,
                radiusPx = 0.6f + roll * 1.1f,
                alpha = 0.18f + roll * 0.62f
            )
        )
    }
    return stars
}

/**
 * The ambient field behind every Aurora screen.
 *
 * [sunAltitudeDeg] drives the field's intensity the way it drives a real aurora:
 * the curtains all but disappear in daylight, so a daytime screen gets a calm
 * gradient with the stars switched off rather than a night sky at noon.
 *
 * [isOpaqueCanvas] paints the base solid. Aurora's lenses are translucent by
 * definition, so when Live Sky is off there would otherwise be nothing behind
 * them but the theme's own background — which is a different, colder colour.
 */
@Composable
fun AuroraFieldBackground(
    themeMode: ThemeMode,
    isOpaqueCanvas: Boolean,
    sunAltitudeDeg: Double,
    modifier: Modifier = Modifier,
    phase: Float = rememberAuroraPhase()
) {
    val palette = AuroraTheme.palette
    val stars = remember { auroraStars() }

    // Night below -12 deg, full day above +6: the same bracket the sky renderer
    // uses to decide when the stars are worth drawing.
    val nightness = remember(sunAltitudeDeg) {
        val t = ((-6.0 - sunAltitudeDeg) / 18.0).coerceIn(0.0, 1.0)
        t.toFloat()
    }
    val starAlpha = (nightness * 1.15f).coerceIn(0f, 1f)
    // A daylight screen still deserves a trace of the aurora: enough to know
    // which language this is, not enough to look like night.
    val ribbonAlpha = (0.34f + nightness * 0.66f).coerceIn(0f, 1f)
    val baseAlpha = if (isOpaqueCanvas) 1f else 0.94f

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // 1. The base gradient: the sky the curtains hang in.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    palette.canvasTop.copy(alpha = palette.canvasTop.alpha * baseAlpha),
                    palette.canvasBottom.copy(alpha = palette.canvasBottom.alpha * baseAlpha)
                ),
                startY = 0f,
                endY = h
            )
        )

        // 2. Stars, only as night falls.
        if (starAlpha > 0.02f) {
            val ink = palette.starInk
            for (star in stars) {
                drawCircle(
                    color = ink.copy(alpha = ink.alpha * star.alpha * starAlpha),
                    radius = star.radiusPx,
                    center = Offset(star.x * w, star.y * h)
                )
            }
        }

        // 3. The horizon glow: the arc's own light spilling onto the lower sky.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, palette.horizonGlow.copy(alpha = palette.horizonGlow.alpha * ribbonAlpha)),
                startY = h * 0.55f,
                endY = h
            )
        )

        // 4. The curtains.
        drawAuroraCurtains(
            palette = palette,
            phase = phase,
            alpha = ribbonAlpha
        )
    }
}

/**
 * Three curtains of light, each a sine-modulated band sampled column by column.
 *
 * Sampling columns and drawing each as a short vertical stroke is what gives the
 * ribbon its cloth-like fall: the top edge is a continuous curve, and the body
 * fades downward with no visible seam between samples.
 */
private fun DrawScope.drawAuroraCurtains(
    palette: AuroraPalette,
    phase: Float,
    alpha: Float
) {
    val w = size.width
    val h = size.height
    val columnWidth = w / RIBBON_COLUMNS
    val twoPi = (2.0 * PI).toFloat()

    val ribbonColors = listOf(palette.ribbonPrimary, palette.ribbonSecondary, palette.ribbonTertiary)

    for (ribbon in 0 until RIBBON_COUNT) {
        val color = ribbonColors[ribbon]
        // Each curtain hangs at its own height and drifts at its own rate, so
        // the three never line up into a single band.
        val baseY = h * (0.16f + 0.13f * ribbon)
        val amplitude = h * (0.055f + 0.022f * ribbon)
        val bandHeight = h * (0.30f + 0.09f * ribbon)
        val driftSpeed = 1f + ribbon * 0.55f
        val waveLength = 1.35f + ribbon * 0.5f
        val strength = (0.62f - ribbon * 0.15f).coerceAtLeast(0.18f)

        for (column in 0..RIBBON_COLUMNS) {
            val t = column / RIBBON_COLUMNS.toFloat()
            val x = t * w

            // Two out-of-phase sines plus the shared clock: no column repeats
            // its neighbour, and the whole cloth travels together.
            val wave = sin(twoPi * (t * waveLength + phase * driftSpeed)) * 0.62f +
                sin(twoPi * (t * waveLength * 2.3f - phase * driftSpeed * 0.7f)) * 0.38f
            // A second, slower modulation gives the curtain its vertical rays.
            val ray = 0.72f + 0.28f * sin(twoPi * (t * 7.0f + phase * driftSpeed * 1.4f))

            val topY = baseY + wave * amplitude
            val bottomY = topY + bandHeight

            val columnAlpha = alpha * strength * ray
            if (columnAlpha <= 0.004f) continue

            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        color.copy(alpha = color.alpha * columnAlpha * 0.85f),
                        color.copy(alpha = color.alpha * columnAlpha * 0.22f),
                        Color.Transparent
                    ),
                    startY = topY,
                    endY = bottomY
                ),
                start = Offset(x, topY),
                end = Offset(x, bottomY),
                // A hair of overlap keeps the columns from showing as a comb.
                strokeWidth = columnWidth * 1.35f
            )
        }
    }
}

/**
 * A compact field for use *inside* a card: the same three curtains, cropped and
 * dimmed, so a tile can carry the language without competing with the screen's
 * own field.
 */
@Composable
fun AuroraFieldVignette(
    modifier: Modifier = Modifier,
    phase: Float = rememberAuroraPhase(),
    intensity: Float = 0.5f
) {
    val palette = AuroraTheme.palette
    Canvas(modifier = modifier) {
        drawAuroraCurtains(
            palette = palette,
            phase = phase,
            alpha = intensity.coerceIn(0f, 1f) * 0.55f
        )
    }
}
