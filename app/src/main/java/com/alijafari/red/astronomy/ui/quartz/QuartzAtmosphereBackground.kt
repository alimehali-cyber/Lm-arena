package com.alijafari.red.astronomy.ui.quartz

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The editorial silhouette backdrop: the one atmosphere every Quartz screen shares.
 *
 * A warm alabaster (or obsidian, or merlot nocturne) paper with a soft-focus human silhouette rising
 * through the centre — an obsidian head and shoulders above a burgundy torso bleed, with the cyan/
 * slate chromatic aberration that the reference photography shows on its left edge. It breathes on a
 * nine-second cycle so the surface never looks like a static gradient, and it is drawn entirely in
 * `drawCircle` calls: no bitmap, no blur filter, no external asset.
 *
 * [sunAltitudeDeg] only modulates the amount of warm light in the torso bleed, so the same backdrop
 * still answers to the real sky the rest of the app is tracking.
 */
@Composable
fun QuartzAtmosphereBackground(
    modifier: Modifier = Modifier,
    sunAltitudeDeg: Double = -18.0
) {
    val palette = QuartzTheme.palette
    val transition = rememberInfiniteTransition(label = "quartz_breath")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "quartz_breath_value"
    )

    val warmth = ((sunAltitudeDeg + 18.0) / 30.0).coerceIn(0.0, 1.0).toFloat()
    val bleedAlpha = 0.22f + 0.14f * warmth

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // 0. Editorial paper, lit from the top-left.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(palette.canvas, palette.canvasSecondary, palette.canvas)
            ),
            size = Size(w, h)
        )

        // 1. Head / shoulders mass — soft focus, breathing 3% of the width.
        val headCx = w * (0.52f + 0.03f * breath)
        val headCy = h * 0.24f
        val headR = w * 0.62f
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to palette.silhouetteCore,
                    0.42f to palette.silhouetteCore.copy(alpha = 0.62f),
                    0.72f to palette.silhouetteBleed.copy(alpha = 0.34f),
                    1.0f to Color.Transparent
                ),
                center = Offset(headCx, headCy),
                radius = headR
            ),
            radius = headR,
            center = Offset(headCx, headCy)
        )

        // 2. Torso bleed — burgundy, counter-breathing.
        val torsoCx = w * (0.46f - 0.03f * breath)
        val torsoCy = h * 0.70f
        val torsoR = w * 0.88f
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to palette.silhouetteCore.copy(alpha = 0.86f),
                    0.46f to palette.silhouetteBleed.copy(alpha = bleedAlpha),
                    0.80f to palette.silhouetteBleed.copy(alpha = bleedAlpha * 0.36f),
                    1.0f to Color.Transparent
                ),
                center = Offset(torsoCx, torsoCy),
                radius = torsoR
            ),
            radius = torsoR,
            center = Offset(torsoCx, torsoCy)
        )

        // 3. Chromatic aberration on the left edge (slate-cyan light wrap).
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.chromaticRim, Color.Transparent),
                center = Offset(w * 0.14f, h * 0.58f),
                radius = w * 0.46f
            ),
            radius = w * 0.46f,
            center = Offset(w * 0.14f, h * 0.58f)
        )

        // 4. A whisper of warm rose caught on the right rim of the torso.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.accentRose.copy(alpha = 0.10f + 0.06f * breath),
                    Color.Transparent
                ),
                center = Offset(w * 0.86f, h * 0.80f),
                radius = w * 0.52f
            ),
            radius = w * 0.52f,
            center = Offset(w * 0.86f, h * 0.80f)
        )

        // 5. Editorial vignette: the paper falls away at the top and bottom edges.
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to palette.canvas.copy(alpha = if (palette.isDark) 0.22f else 0.34f),
                0.18f to Color.Transparent,
                0.86f to Color.Transparent,
                1.0f to palette.canvas.copy(alpha = if (palette.isDark) 0.30f else 0.42f)
            )
        )

        // 6. A final, single hairline seam across the centre of the silhouette: the studio's
        //    calibration line, and the only straight edge the paper carries.
        drawLine(
            color = palette.hairline.copy(alpha = 0.5f),
            start = Offset(w * 0.10f, h * 0.5f),
            end = Offset(w * 0.90f, h * 0.5f),
            strokeWidth = 0.75.dp.toPx()
        )
    }
}

/** Small helper so call sites can put a 0.75 dp crosshair grid behind a hero panel if they want it. */
@Composable
fun QuartzCalibrationCrosshair(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val sw = 0.75.dp.toPx()
        drawLine(color, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), sw)
        drawLine(color, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), sw)
        drawCircle(
            color = color,
            radius = size.minDimension * 0.22f,
            center = Offset(size.width / 2f, size.height / 2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = sw)
        )
    }
}
