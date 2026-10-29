package com.alijafari.red.astronomy.ui.rendering.landscape

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.alijafari.red.astronomy.ui.rendering.LandscapeProfile
import com.alijafari.red.astronomy.ui.rendering.LightingState
import com.alijafari.red.astronomy.ui.rendering.PersepolisFraming
import com.alijafari.red.astronomy.ui.rendering.SceneComposition
import com.alijafari.red.astronomy.ui.rendering.ScenePalette
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/**
 * THE RIVER — the Pulvar, in the near foreground of the Home sky canvas.
 *
 * Provenance: extracted verbatim from
 * `app/src/main/java/com/alijafari/red/astronomy/ui/rendering/SceneGroundRenderer.kt`
 * (repo commit 716586ee67074e542485cbe5e7f6af9bcaabbdb0, file SHA-256 f8cdaac1…),
 * section 10 of `drawMarvdashtLandscape` — lines 902–1203.
 *
 * The centreline / half-width / bend functions are the *only* definition of where the water
 * is; callers get them through [Geometry], so the plain's grass mask
 * ([GrassField]) and the shrubs/trees use the same river the water is painted from. Nothing
 * here re-derives geometry.
 *
 * Draw order: after the mountains ([LandscapeMountains]) and after the plain/field
 * ([GrassField]); the trees and reeds are the only things that go over it.
 *
 * Depends on the repo's existing, unchanged types:
 * `SceneComposition`, `ScenePalette`, `LightingState`, `PersepolisFraming.Result`,
 * and `LandscapeProfile` (noise + the Cox–Munk glitter falloff).
 */
object PulvarRiver {

    /**
     * The river's resolved geometry for one canvas.
     *
     * All of it is a pure function of the canvas size and the framing, so it is cheap to
     * build per frame and identical for every component that asks.
     */
    class Geometry(
        val width: Float,
        val height: Float,
        /** Pixel row the water's centreline meanders around. */
        val riverY: Float,
        /** The ribbon is drawn past both canvas edges by this much, so no cap is ever visible. */
        val riverMargin: Float
    ) {
        /** Horizontal fraction along the ribbon, 0..1, including the off-canvas margins. */
        fun p(x: Float): Float = ((x + riverMargin) / (width + 2f * riverMargin)).coerceIn(0f, 1f)

        /** Centreline of the water at canvas column [x]: two meanders plus fBm swerve, drifting down-right. */
        fun centerY(x: Float): Float {
            val p = p(x)
            return riverY + height * 0.017f * sin(p * 4.6f + 0.7f) +
                height * 0.008f * LandscapeProfile.fbm(p * 3.1f, 41, octaves = 3) + p * height * 0.014f
        }

        /** Half-width of the water at column [x] (a function of depth down the frame), pooled in the bends. */
        fun halfW(x: Float, y: Float): Float {
            val p = p(x)
            val depth = ((y / height - 0.86f) / 0.12f).coerceIn(0f, 1f)
            val pool = 1f + 0.45f * maxOf(0f, sin(p * 4.6f + 2.0f))
            // ~3.8x the original stream (doubled twice): a broad river whose near edge can
            // run out of the frame's bottom-right pools — the viewer stands in the shallows.
            return height * (0.021f + 0.025f * depth) * pool
        }

        // Meander phase: +1 bulges toward the viewer, -1 away. Drives the thalweg offset
        // (deep water hugs the outer bank) and the gravel point bars (inner bank).
        fun bend(x: Float): Float = sin(p(x) * 4.6f + 0.7f)

        /** The closed water ribbon at a fraction [scale] of the true half-width. */
        fun ribbonPath(scale: Float, offset: Float = 0f): Path = Path().apply {
            val steps = 28
            for (i in 0..steps) {
                val x = -riverMargin + (width + 2f * riverMargin) * i / steps
                val yc = centerY(x)
                val y = yc + bend(x) * offset * halfW(x, yc) - halfW(x, yc) * scale
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            for (i in steps downTo 0) {
                val x = -riverMargin + (width + 2f * riverMargin) * i / steps
                val yc = centerY(x)
                lineTo(x, yc + bend(x) * offset * halfW(x, yc) + halfW(x, yc) * scale)
            }
            close()
        }

        /** One open bank line: `sign = -1` far bank, `sign = +1` near bank. */
        fun bankPath(sign: Float): Path = Path().apply {
            val steps = 28
            for (i in 0..steps) {
                val x = -riverMargin + (width + 2f * riverMargin) * i / steps
                val y = centerY(x) + sign * halfW(x, centerY(x))
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }

        /** One ribbon edge at a fractional half-width (inside the true bank line). */
        fun edgePath(scale: Float, offset: Float = 0f): Path = Path().apply {
            val steps = 28
            for (i in 0..steps) {
                val x = -riverMargin + (width + 2f * riverMargin) * i / steps
                val yc = centerY(x)
                val y = yc + bend(x) * offset * halfW(x, yc) - halfW(x, yc) * scale
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
    }

    /**
     * Builds the river's geometry for this canvas. The centreline sits a little lower than
     * the Mil slope's front step so the far edge of the wet-bank fringe still clears it at
     * every meander.
     */
    fun geometryFor(composition: SceneComposition, framing: PersepolisFraming.Result): Geometry {
        val width = composition.widthPx
        val height = composition.heightPx
        val riverY = (framing.milSlopeFrontYPx + height * 0.062f).coerceIn(height * 0.895f, height * 0.945f)
        return Geometry(width = width, height = height, riverY = riverY, riverMargin = width * 0.02f)
    }

    /**
     * Paints the whole river, wettest to deepest, then its surface work.
     *
     * @param geometry the shared river geometry, from [geometryFor]. Pass the same instance the
     * plain's field mask was built with.
     * @param sunPosPx the Sun's projected screen position, or null when it is far below the
     * horizon — it aims the glitter column and the sheen.
     * @param moonPosPx the Moon's projected screen position, for the night glitter.
     * @param palette accepted for call-site symmetry with [LandscapeMountains] and
     * [GrassField]; the water mixes only from the lighting tones and the fixed art values,
     * exactly as the shipped section did.
     */
    fun draw(
        drawScope: DrawScope,
        composition: SceneComposition,
        palette: ScenePalette,
        lighting: LightingState,
        sunPosPx: Offset?,
        moonPosPx: Offset?,
        geometry: Geometry
    ) {
        val width = composition.widthPx
        val height = composition.heightPx
        val day = lighting.ambientBrightness.coerceIn(0f, 1f)
        val moonGlow = lighting.moonGlowIntensity.coerceIn(0f, 1f)
        val lightLevel = maxOf(day, moonGlow * 0.35f)
        val sunAlt = lighting.sunAltitudeDeg.toFloat()
        val sand = lerp(Color(0xFF292216), Color(0xFFBEA06E), day)
        // The foreground canopy tone the bank shadows and the tree reflections are mixed
        // from (the trees themselves live in the vegetation layer).
        val canopy = lerp(Color(0xFF0D140D), Color(0xFF415232), lightLevel)
        val canopyDark = canopy.scaled(0.70f)

        val g = geometry
        val riverY = g.riverY
        val riverMargin = g.riverMargin

        fun centerY(x: Float): Float = g.centerY(x)
        fun halfW(x: Float, y: Float): Float = g.halfW(x, y)
        fun bend(x: Float): Float = g.bend(x)
        fun ribbonPath(scale: Float, offset: Float = 0f): Path = g.ribbonPath(scale, offset)
        fun bankPath(sign: Float): Path = g.bankPath(sign)
        fun edgePath(scale: Float, offset: Float = 0f): Path = g.edgePath(scale, offset)

        fun oval(cx: Float, cy: Float, w: Float, h: Float, color: Color) {
            drawScope.drawOval(
                color = color,
                topLeft = Offset(cx - w / 2f, cy - h / 2f),
                size = androidx.compose.ui.geometry.Size(w, h)
            )
        }

        val deepWater = lerp(Color(0xFF0A141C), Color(0xFF2E4A52), day)
        val shallowWater = lerp(Color(0xFF0E1814), Color(0xFF4A5A4C), day) // bed through thin water
        val wetBank = lerp(Color(0xFF0C0E08), Color(0xFF4C4434), day)      // damp darkened shore
        val reflection = lerp(lerp(lighting.horizonTone, lighting.skyTone, 0.35f), deepWater, 0.22f)

        // Layered water, wettest to deepest: damp bank fringe, shallow bed showing at the
        // edges, the sky's reflection, then the deep thalweg hugging each outer bank.
        drawScope.drawPath(ribbonPath(1.18f), wetBank.copy(alpha = 0.55f + 0.30f * day))
        drawScope.drawPath(ribbonPath(1f), shallowWater)
        // Fresnel-graded reflection (Schlick, F0 ≈ 0.02 in spirit): the far water is
        // seen at a grazing angle and mirrors the sky almost fully; the near water is
        // seen steeply, so the bed and deep water show through and the sky's share
        // falls. A flat 0.90 reflection read as a plastic sheet; the vertical grade
        // is what makes the same ribbon read as a surface lying in the light.
        drawScope.drawPath(
            ribbonPath(0.84f),
            brush = Brush.verticalGradient(
                0.0f to reflection.copy(alpha = 0.96f),
                0.55f to reflection.copy(alpha = 0.82f),
                1.0f to lerp(reflection, deepWater, 0.55f).copy(alpha = 0.60f),
                startY = riverY - height * 0.045f,
                endY = riverY + height * 0.085f
            )
        )
        drawScope.drawPath(ribbonPath(0.22f, offset = 0.45f), deepWater.copy(alpha = 0.60f))
        // Far-bank waterline: at grazing incidence the waterline itself catches the
        // sky — a hairline of reflected light just inside the far edge, separating
        // the water from the bank shadow line drawn later on the true edge.
        drawScope.drawPath(
            edgePath(0.93f),
            reflection.copy(alpha = 0.16f + 0.26f * lightLevel),
            style = Stroke(width = (height * 0.0012f).coerceIn(0.8f, 2f), cap = StrokeCap.Round)
        )
        for (i in 0 until 8) { // dark slow water breaking the reflection
            val x = width * LandscapeProfile.hash01(97, i * 11 + 4)
            val yc = centerY(x)
            drawSoftPatch(drawScope, x, yc, width * 0.07f, halfW(x, yc) * 1.0f, deepWater, 0.18f, layers = 8)
        }
        // Pale gravel point bars inside the bends, where the current drops its load.
        for (i in 0..28) {
            val x = -riverMargin + (width + 2f * riverMargin) * i / 28f
            val b = bend(x)
            if (abs(b) <= 0.72f) continue
            val yc = centerY(x)
            drawSoftPatch(
                drawScope, x, yc - b * halfW(x, yc) * 0.82f,
                width * 0.07f, halfW(x, yc) * 0.5f, sand,
                peakAlpha = 0.10f + 0.20f * day, layers = 8
            )
        }
        // Flow ripples: small sky-coloured dashes riding the current.
        val rippleColor = lerp(lighting.skyTone, Color.White, 0.25f)
        for (i in 0 until 90) {
            val x = width * LandscapeProfile.hash01(179, i * 17 + 3)
            val yc = centerY(x)
            val y = yc + (LandscapeProfile.hash01(73, i * 7 + 5) - 0.5f) * 1.7f * halfW(x, yc)
            val len = height * (0.004f + 0.010f * LandscapeProfile.hash01(39, i * 3 + 1))
            drawScope.drawLine(
                color = rippleColor.copy(alpha = 0.05f + 0.13f * lightLevel),
                start = Offset(x, y),
                end = Offset(x + len, y + len * 0.10f),
                strokeWidth = (height * 0.0014f).coerceIn(0.8f, 2.5f),
                cap = StrokeCap.Round
            )
        }
        // Submerged pebbles freckling the near shallows.
        for (i in 0 until 26) {
            val x = width * LandscapeProfile.hash01(231, i * 19 + 5)
            val yc = centerY(x)
            val y = yc + halfW(x, yc) * (0.62f + 0.3f * LandscapeProfile.hash01(47, i * 3 + 2))
            val r = height * (0.0012f + 0.0018f * LandscapeProfile.hash01(63, i * 5 + 1))
            oval(x, y, r * 2f, r, sand.copy(alpha = 0.10f + 0.18f * day))
        }
        drawScope.drawPath(
            bankPath(-1f),
            canopyDark.copy(alpha = 0.45f),
            style = Stroke(width = (height * 0.0016f).coerceIn(1f, 3f))
        )
        drawScope.drawPath(
            bankPath(1f),
            sand.copy(alpha = 0.40f),
            style = Stroke(width = (height * 0.0016f).coerceIn(1f, 3f))
        )
        val riparian = lerp(canopy, Color(0xFF5A7244), 0.4f * day)
        for (i in 0 until 12) { // greener grass along the far bank
            val x = width * (i + LandscapeProfile.hash01(51, i * 3 + 1)) / 12f
            val yc = centerY(x)
            drawSoftPatch(
                drawScope, x, yc - halfW(x, yc) - height * 0.004f,
                width * 0.13f, height * 0.016f, riparian, peakAlpha = 0.06f + 0.14f * day, layers = 10
            )
        }
        // Glitter: broken dashes concentrated in the Sun's (or Moon's) reflection column.
        val glint: Triple<Float, Float, Color>? = when {
            sunPosPx != null && sunAlt > -2f -> Triple(
                sunPosPx.x.coerceIn(0f, width),
                (1f - sunAlt.coerceIn(0f, 38f) / 38f) * 0.9f,
                lerp(Color(0xFFFFE9C4), lighting.horizonTone, 0.35f)
            )
            moonPosPx != null -> Triple(
                moonPosPx.x.coerceIn(0f, width),
                0.45f * (lighting.moonIlluminationPercent.toFloat() / 100f).coerceIn(0f, 1f),
                Color(0xFFDCE8FF)
            )
            else -> null
        }
        if (glint != null && glint.second > 0.01f) {
            val (glintX, glintStrength, glintColor) = glint
            // Broad Fresnel sheen across the reflection column, under the sparkles.
            val sheenY = centerY(glintX)
            drawSoftPatch(
                drawScope, glintX, sheenY, width * 0.52f, halfW(glintX, sheenY) * 1.7f,
                glintColor, peakAlpha = 0.10f * glintStrength, layers = 10
            )
            drawSoftPatch(
                drawScope, glintX, sheenY, width * 0.30f, halfW(glintX, sheenY) * 1.5f,
                glintColor, peakAlpha = 0.20f * glintStrength, layers = 10
            )
            // Cox–Munk glitter: a sparkle exists only where a ripple facet happens to
            // mirror the Sun/Moon to the viewer, so sparkle *density, brightness and
            // size* all follow the 2D Gaussian falloff in slope space around the
            // specular point (the sheen's centre) — dense, long and bright at the
            // heart, sparse and short at the fringes. The old 1D column rained
            // uniform dashes at any height in the ribbon; this is a point process
            // thinned by the facet probability itself.
            for (i in 0 until 72) {
                val x = width * LandscapeProfile.hash01(137, i * 7 + 2)
                val dxS = (x - glintX) / (width * 0.17f)
                if (abs(dxS) > 2.6f) continue // beyond ~2.6σ there are no facets to catch
                val yc = centerY(x)
                val hw = halfW(x, yc)
                val yOff = (LandscapeProfile.hash01(79, i * 5 + 3) - 0.5f) * 1.7f
                val y = yc + yOff * hw * 0.82f
                val glitter = LandscapeProfile.sunGlitter(dxS, yOff / 0.55f)
                if (LandscapeProfile.hash01(193, i * 11 + 8) > 0.10f + 0.90f * glitter) continue
                val alpha = (glintStrength * glitter *
                    (0.30f + 0.70f * LandscapeProfile.hash01(31, i * 3 + 1))).coerceIn(0f, 0.92f)
                if (alpha < 0.02f) continue
                val len = height * (0.005f + 0.020f * LandscapeProfile.hash01(57, i * 9 + 5)) *
                    (0.35f + 0.85f * glitter)
                drawScope.drawLine(
                    color = glintColor.copy(alpha = alpha),
                    start = Offset(x, y),
                    end = Offset(x + len, y),
                    strokeWidth = ((height * 0.002f) * (0.7f + 0.6f * glitter)).coerceIn(1f, 3.5f),
                    cap = StrokeCap.Round
                )
                // Rare star sparkles at the specular heart.
                if (glitter > 0.72f && LandscapeProfile.hash01(297, i * 3 + 1) > 0.62f) {
                    val r = height * (0.002f + 0.002f * LandscapeProfile.hash01(41, i * 7 + 2))
                    drawScope.drawLine(
                        color = glintColor.copy(alpha = alpha * 0.5f),
                        start = Offset(x + len / 2f, y - r),
                        end = Offset(x + len / 2f, y + r),
                        strokeWidth = (height * 0.0014f).coerceIn(0.8f, 2.5f),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        // Broken reflections of the two foreground trees in the near water: ripple-
        // torn horizontal slices. Deeper slices sit on water that has travelled
        // farther from the tree, so their wobble grows, their dashes split, and
        // hash-driven gaps open in them — a reflection is a signal the surface
        // keeps interrupting, not a stack of neat bars.
        for ((txFrac, thFrac) in listOf(0.115f to 0.068f, 0.875f to 0.058f)) {
            val tx = width * txFrac
            val th = height * thFrac
            val yc = centerY(tx)
            val hw = halfW(tx, yc)
            val bot = yc + hw
            val seedBase = (txFrac * 1000f).toInt()
            for (j in 0 until 7) {
                val y = bot - (j + 0.5f) * (th * 0.55f / 7f)
                if (y < yc - hw) break
                // Ripple gaps: some slices are simply missing, more often deeper.
                if (j > 0 && LandscapeProfile.hash01(277, seedBase + j * 7) < 0.10f + 0.05f * j) continue
                val wdt = th * 1.1f * (0.55f - 0.045f * j)
                val wobble = (LandscapeProfile.hash01(77, seedBase + j * 7) - 0.5f) *
                    height * 0.006f * (0.6f + 0.35f * j)
                val alpha = 0.30f * (1f - j / 7f) * (0.4f + 0.6f * lightLevel)
                val strokeW = (height * 0.002f).coerceIn(1f, 3.5f)
                drawScope.drawLine(
                    color = canopyDark.copy(alpha = alpha),
                    start = Offset(tx + wobble - wdt / 2f, y),
                    end = Offset(tx + wobble + wdt / 2f, y),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                // A second, shorter fragment torn sideways off the deeper slices.
                if (j >= 2 && LandscapeProfile.hash01(281, seedBase + j * 11) > 0.45f) {
                    val fragW = wdt * (0.30f + 0.20f * LandscapeProfile.hash01(283, seedBase + j * 13))
                    val fragX = tx + wobble * 1.7f +
                        (LandscapeProfile.hash01(293, seedBase + j * 17) - 0.5f) * wdt * 0.5f
                    drawScope.drawLine(
                        color = canopyDark.copy(alpha = alpha * 0.7f),
                        start = Offset(fragX - fragW / 2f, y + th * 0.028f),
                        end = Offset(fragX + fragW / 2f, y + th * 0.028f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        // Reed clumps (Phragmites) at the banks: grouped curved stems, feathery plumes.
        val plumeColor = lerp(Color(0xFF201A10), Color(0xFFB2966E), day)
        for (ci in 0 until 7) {
            val pp = 0.08f + 0.84f * (ci + LandscapeProfile.hash01(93, ci * 3 + 1)) / 7f
            val x0 = -riverMargin + (width + 2f * riverMargin) * pp
            val yc = centerY(x0)
            val side = if (ci % 2 == 0) -1f else 1f
            val by = yc + side * halfW(x0, yc) * 1.02f
            for (si in 0 until 8) {
                val x = x0 + (LandscapeProfile.hash01(31, ci * 31 + si * 7) - 0.5f) * width * 0.018f
                val bh = height * (0.014f + 0.012f * LandscapeProfile.hash01(17, ci * 17 + si * 3))
                val lean = (LandscapeProfile.hash01(13, ci * 13 + si * 5) - 0.5f) * bh * 0.35f
                val stem = Path()
                for ((k, t) in listOf(0f, 0.33f, 0.66f, 1f).withIndex()) {
                    val px = x + lean * t.pow(1.6f)
                    val py = by - bh * t
                    if (k == 0) stem.moveTo(px, py) else stem.lineTo(px, py)
                }
                drawScope.drawPath(
                    stem, riparian.copy(alpha = 0.45f + 0.25f * day),
                    style = Stroke(width = (height * 0.0014f).coerceIn(0.8f, 2.5f))
                )
                if (si % 3 == 0) {
                    oval(
                        x + lean, by - bh - height * 0.0015f,
                        height * 0.0028f, height * 0.006f,
                        plumeColor.copy(alpha = 0.35f + 0.30f * day)
                    )
                }
            }
        }
    }
}

/** This colour at [factor]× brightness (alpha unchanged). */
private fun Color.scaled(factor: Float): Color = Color(
    red = (red * factor).coerceIn(0f, 1f),
    green = (green * factor).coerceIn(0f, 1f),
    blue = (blue * factor).coerceIn(0f, 1f),
    alpha = alpha
)

/**
 * One soft elliptical patch (single colour) with the many-equal-alpha-shell falloff the
 * grounding drifts use: source-over accumulation reaches [peakAlpha] at the centre in
 * steps too small to read as contour rings.
 */
private fun drawSoftPatch(
    drawScope: DrawScope,
    cx: Float,
    cy: Float,
    patchWidth: Float,
    patchHeight: Float,
    color: Color,
    peakAlpha: Float,
    layers: Int = 10
) {
    val safeLayers = layers.coerceAtLeast(2)
    val shellAlpha = 1f - (1f - peakAlpha.coerceIn(0f, 0.95f)).pow(1f / safeLayers)
    for (layer in 0 until safeLayers) {
        val t = layer / (safeLayers - 1).toFloat()
        val scale = 1f - 0.92f * t
        val w = patchWidth * scale
        val h = patchHeight * scale
        drawScope.drawOval(
            color = color.copy(alpha = shellAlpha * color.alpha),
            topLeft = Offset(cx - w / 2f, cy - h / 2f),
            size = androidx.compose.ui.geometry.Size(w, h)
        )
    }
}
