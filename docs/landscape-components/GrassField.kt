package com.alijafari.red.astronomy.ui.rendering.landscape

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.alijafari.red.astronomy.ui.rendering.LandscapeProfile
import com.alijafari.red.astronomy.ui.rendering.LightingState
import com.alijafari.red.astronomy.ui.rendering.PersepolisFraming
import com.alijafari.red.astronomy.ui.rendering.SceneComposition
import com.alijafari.red.astronomy.ui.rendering.ScenePalette
import kotlin.math.abs

/**
 * THE GREEN FLOOR — the valley plain, its cultivation and the uniform grass field.
 *
 * There is no "grass sprite" in this landscape: the green floor is a **field**, one textured
 * green mass painted as eight bucketed path fills plus a lush ribbon along the near bank,
 * never individual blades. (The tufts at the bottom of this file belong to the *stylised*
 * themes only, which are flat illustration by design.)
 *
 * Provenance: extracted verbatim from
 * `app/src/main/java/com/alijafari/red/astronomy/ui/rendering/SceneGroundRenderer.kt`
 * (repo commit 716586ee67074e542485cbe5e7f6af9bcaabbdb0, file SHA-256 f8cdaac1…):
 *
 *  - the plain's vertical gradient        lines 636–651  (§3 of `drawMarvdashtLandscape`)
 *  - the cultivation mosaic               lines 863–880  (§7)
 *  - the irrigation canal + tree line     lines 882–899  (§8)
 *  - the field cell layout (perspective rows of overlapping tone cells, the
 *    near-bank lush ribbon, `grassAllowed`, `moistureAt`) and the bake-once
 *    cache                             lines 1823–1919 (§11 of `buildVegetation`)
 *  - the field replay (eight bucket fills + the ribbon)  lines 1926–1941
 *  - the stylised grass line (`drawGrass`, `pseudoRandom`) lines 2397–2461
 *
 * CALL ORDER — the plain is one object painted in three passes, because the mountains and
 * the river sit between them:
 *
 * ```
 * GrassField.drawPlainFloor(...)   // 1. behind everything: the green gradient floor
 * LandscapeMountains.draw(...)     // 2. the ranges and the massif stand on it
 * GrassField.drawCultivation(...)  // 3. plots and the canal cross the plain's middle
 * PulvarRiver.draw(...)            // 4. the water
 * GrassField.drawField(...)        // 5. the near field + lush bank, in front of the water
 * ```
 *
 * Depends on the repo's existing, unchanged types:
 * `SceneComposition`, `ScenePalette`, `LightingState`, `PersepolisFraming.Result`,
 * `LandscapeProfile` (noise), and [PulvarRiver.Geometry] for the water mask.
 */
object GrassField {

    /** One canvas's baked field: eight tone buckets and the near-bank lush ribbon. */
    private class FieldGeometry(
        val key: FieldKey,
        val fieldBuckets: List<Path>,
        val nearBankFieldP: Path
    )

    /** Everything the field layout depends on. A new key means a rebuild. */
    private data class FieldKey(
        val width: Float,
        val height: Float,
        val toeY: Float,
        val platformLeftX: Float,
        val platformRightX: Float,
        val milSlopeLeftX: Float,
        val milSlopeFrontY: Float,
        val riverY: Float
    )

    /** Last built layout; replaced whenever the key changes. Main-thread only. */
    private var cache: FieldGeometry? = null

    // ---------------------------------------------------------------------------------
    // 1. The plain — drawn behind the mountains.
    // ---------------------------------------------------------------------------------

    /**
     * The plain: a lively cool-green field floor, darkening toward the viewer.
     *
     * (The old dry olive-straw base read yellowy and half-dead under the new green field; the
     * whole floor now lives in the same cool green family.)
     */
    fun drawPlainFloor(
        drawScope: DrawScope,
        composition: SceneComposition,
        palette: ScenePalette,
        lighting: LightingState
    ) {
        val width = composition.widthPx
        val height = composition.heightPx
        val groundTopY = composition.groundTopY
        val day = lighting.ambientBrightness.coerceIn(0f, 1f)

        val steppeTop = lerp(palette.groundTop, Color(0xFF5F7F45), 0.62f * day)
        val fieldBottom = lerp(palette.groundBottom, Color(0xFF3C6034), 0.58f * day)
        drawScope.drawRect(
            brush = Brush.verticalGradient(
                0.0f to steppeTop,
                0.38f to lerp(steppeTop, fieldBottom, 0.45f),
                1.0f to fieldBottom,
                startY = groundTopY,
                endY = height
            ),
            topLeft = Offset(0f, groundTopY),
            size = androidx.compose.ui.geometry.Size(width, (height - groundTopY).coerceAtLeast(1f))
        )
        // (No horizon catch-light wash on the plain's top edge: its hard straight top
        // edge at groundTopY was the white band visible across the scene at the
        // readout's day/date line, strongest while scrubbing through daytime.)
    }

    // ---------------------------------------------------------------------------------
    // 2. Cultivation — drawn after the mountains, before the river.
    // ---------------------------------------------------------------------------------

    /**
     * The plain's land use: vast soft plots, then the irrigation canal and its tree line.
     *
     * Cultivation mosaic: vast soft plots, irrigated green against fallow straw, kept
     * feather-edged and low-contrast so the plain reads as land use, not shapes. Marvdasht
     * has been farmed since Cyrus, and a green thread through dry steppe is its most
     * characteristic mark.
     */
    fun drawCultivation(
        drawScope: DrawScope,
        composition: SceneComposition,
        palette: ScenePalette,
        lighting: LightingState,
        framing: PersepolisFraming.Result
    ) {
        val width = composition.widthPx
        val height = composition.heightPx
        val toeY = framing.toeYPx
        val day = lighting.ambientBrightness.coerceIn(0f, 1f)
        val moonGlow = lighting.moonGlowIntensity.coerceIn(0f, 1f)
        val lightLevel = maxOf(day, moonGlow * 0.35f)

        fun oval(cx: Float, cy: Float, w: Float, h: Float, color: Color) {
            drawScope.drawOval(
                color = color,
                topLeft = Offset(cx - w / 2f, cy - h / 2f),
                size = androidx.compose.ui.geometry.Size(w, h)
            )
        }

        // 7. Cultivation mosaic.
        val irrigated = lerp(palette.groundTop, Color(0xFF4E7A38), 0.55f * day)
            .copy(alpha = 0.20f + 0.16f * day)
        val fallow = lerp(palette.groundTop, Color(0xFF8E9A5E), 0.5f * day)
            .copy(alpha = 0.08f + 0.08f * day)
        for ((cx, cy, w, h, green) in listOf(
            listOf(0.16f, 0.875f, 0.55f, 0.040f, 1f), listOf(0.62f, 0.855f, 0.62f, 0.034f, 0f),
            listOf(0.40f, 0.935f, 0.70f, 0.050f, 1f), listOf(0.85f, 0.920f, 0.55f, 0.044f, 0f),
            listOf(0.08f, 0.960f, 0.50f, 0.046f, 0f)
        )) {
            oval(width * cx, height * cy, width * w, height * h, if (green > 0.5f) irrigated else fallow)
        }

        // 8. An irrigation canal and its tree line crossing the mid-foreground.
        val canopy = lerp(Color(0xFF0D140D), Color(0xFF415232), lightLevel)
        val canopyDark = canopy.scaled(0.70f)
        val canalY = toeY + height * 0.048f
        drawScope.drawLine(
            color = lerp(palette.groundTop, Color(0xFF33402A), 0.5f * day).copy(alpha = 0.55f),
            start = Offset(width * 0.015f, canalY - height * 0.004f),
            end = Offset(width * 0.985f, canalY + height * 0.006f),
            strokeWidth = (height * 0.0022f).coerceIn(1f, 4f),
            cap = StrokeCap.Round
        )
        for (i in 0 until 26) {
            val x = width * (0.03f + 0.94f * (i + LandscapeProfile.hash01(77, i * 3 + 1)) / 26f)
            val y = canalY - height * 0.004f + (x / width) * height * 0.010f
            val r = height * (0.0025f + 0.0035f * LandscapeProfile.hash01(33, i * 5 + 2))
            oval(x, y, r * 2f, r * 1.4f, canopyDark.copy(alpha = 0.55f))
        }
    }

    // ---------------------------------------------------------------------------------
    // 3. The grass field itself — drawn after the river.
    // ---------------------------------------------------------------------------------

    /**
     * The uniform grass field: eight bucketed fills for the whole valley floor, then the
     * near-bank lush ribbon, so tree shadows, trunks and crowns all stand *in* it. Cool lively
     * greens, deep in shade to sunlit at the bucket tops; at night the same buckets collapse
     * toward the dark ground tone.
     */
    fun drawField(
        drawScope: DrawScope,
        composition: SceneComposition,
        lighting: LightingState,
        framing: PersepolisFraming.Result,
        river: PulvarRiver.Geometry
    ) {
        val day = lighting.ambientBrightness.coerceIn(0f, 1f)

        val veg = fieldGeometry(composition, framing, river)

        val fieldDeep = lerp(Color(0xFF0A1610), Color(0xFF38663A), day)
        val fieldLit = lerp(Color(0xFF101F14), Color(0xFF74A460), day)
        for (i in veg.fieldBuckets.indices) {
            val t = (i + 0.5f) / veg.fieldBuckets.size
            drawScope.drawPath(veg.fieldBuckets[i], lerp(fieldDeep, fieldLit, t).copy(alpha = 0.94f))
        }
        drawScope.drawPath(
            veg.nearBankFieldP,
            lerp(lerp(Color(0xFF0A1610), Color(0xFF3E7038), day), fieldLit, 0.25f).copy(alpha = 0.95f)
        )
    }

    // ---------------------------------------------------------------------------------
    // The bake: perspective rows of overlapping tone cells.
    // ---------------------------------------------------------------------------------

    private fun fieldGeometry(
        composition: SceneComposition,
        framing: PersepolisFraming.Result,
        river: PulvarRiver.Geometry
    ): FieldGeometry {
        val width = composition.widthPx
        val height = composition.heightPx
        val toeY = framing.toeYPx
        val key = FieldKey(
            width = width, height = height, toeY = toeY,
            platformLeftX = framing.platformLeftXPx,
            platformRightX = framing.platformRightXPx,
            milSlopeLeftX = framing.milSlopeLeftXPx,
            milSlopeFrontY = framing.milSlopeFrontYPx,
            riverY = river.riverY
        )
        cache?.let { if (it.key == key) return it }

        val fieldBuckets = List(8) { Path() }
        val nearBankFieldP = Path()

        fun addOvalTo(path: Path, cx: Float, cy: Float, w: Float, h: Float) {
            path.addOval(
                androidx.compose.ui.geometry.Rect(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
            )
        }
        fun centerY(x: Float): Float = river.centerY(x)
        fun halfW(x: Float, y: Float): Float = river.halfW(x, y)

        val canalY = toeY + height * 0.048f
        fun canalYAt(x: Float): Float = canalY - height * 0.004f + (x / width) * height * 0.010f

        val persepolisApronL = framing.platformLeftXPx - width * 0.030f
        val persepolisApronR = framing.platformRightXPx + width * 0.105f
        val slopeApronL = framing.milSlopeLeftXPx - width * 0.065f
        fun inApron(x: Float, y: Float): Boolean =
            (x in persepolisApronL..persepolisApronR && y < toeY + height * 0.095f) ||
                (x > slopeApronL && y < framing.milSlopeFrontYPx + height * 0.095f)

        fun grassAllowed(x: Float, y: Float): Boolean {
            if (inApron(x, y)) return false
            val yc = centerY(x)
            return abs(y - yc) >= halfW(x, yc) * 1.10f
        }
        fun moistureAt(x: Float, y: Float, depth: Float): Float {
            var m = 0.5f + 0.5f * LandscapeProfile.fbm(x / width * 4.2f + depth * 1.7f, 91, octaves = 3)
            val yc = centerY(x)
            m += (1f - abs(y - yc) / (halfW(x, yc) * 2.6f + height * 0.008f)).coerceIn(0f, 1f) * 0.50f
            m += (1f - abs(y - canalYAt(x)) / (height * 0.018f)).coerceIn(0f, 1f) * 0.35f
            return m.coerceIn(0f, 1f)
        }
        // The near-bank lush ribbon: one filled band hugging the water's near
        // edge — the same field, deepened by the river's moisture.
        run {
            val steps = 28
            for (i in 0..steps) {
                val x = -width * 0.02f + (width * 1.04f) * i / steps
                val yc = centerY(x)
                val y = yc + halfW(x, yc) * 1.03f
                if (i == 0) nearBankFieldP.moveTo(x, y) else nearBankFieldP.lineTo(x, y)
            }
            for (i in steps downTo 0) {
                val x = -width * 0.02f + (width * 1.04f) * i / steps
                val yc = centerY(x)
                nearBankFieldP.lineTo(x, yc + halfW(x, yc) * 1.58f)
            }
            nearBankFieldP.close()
        }
        // The field itself: perspective rows of overlapping tone cells.
        val fieldTop = toeY + height * 0.008f // the plain below the foothill skirt
        var rowY = fieldTop
        var row = 0
        while (rowY < height && row < 90) {
            val depth = ((rowY - fieldTop) / (height - fieldTop)).coerceIn(0f, 1f)
            val rowH = height * (0.005f + 0.009f * depth)
            val cellW = width * (0.020f + 0.024f * depth)
            var x = -cellW * 0.5f + cellW * LandscapeProfile.hash01(2001, row * 7 + 1)
            var col = 0
            while (x < width + cellW) {
                val cx = x + (LandscapeProfile.hash01(2003, row * 131 + col * 17) - 0.5f) * cellW * 0.35f
                val cy = rowY + (LandscapeProfile.hash01(2005, row * 137 + col * 19) - 0.5f) * rowH * 0.5f
                col++
                x += cellW
                if (!grassAllowed(cx, cy)) continue
                if (cx > framing.milSlopeLeftXPx - width * 0.065f &&
                    cy < framing.milSlopeFrontYPx + height * 0.14f) continue // Mil slope front stays stone
                if (abs(cy - canalYAt(cx)) < height * 0.0065f) continue // the canal thread stays open
                val moist = moistureAt(cx, cy, depth)
                val patch = 0.5f + 0.5f * LandscapeProfile.fbm(
                    cx / width * 4.6f + depth * 2.3f, 733, octaves = 3
                )
                // Tone: patch albedo x moisture, contrast compressed with
                // distance (the mipmap lesson), never dipping into straw.
                var tone = (0.18f + 0.52f * patch + 0.38f * moist).coerceIn(0f, 1f)
                tone = 0.5f + (tone - 0.5f) * 0.60f * (1f - 0.45f * (1f - depth))
                val bucket = (tone * (fieldBuckets.size - 1)).toInt().coerceIn(0, fieldBuckets.size - 1)
                addOvalTo(fieldBuckets[bucket], cx, cy, cellW * 1.60f, rowH * 1.05f)
            }
            rowY += rowH * 0.85f
            row++
        }

        return FieldGeometry(key, fieldBuckets, nearBankFieldP).also { cache = it }
    }

    // ---------------------------------------------------------------------------------
    // The stylised themes' grass line (flat illustration, not the photographic field).
    // ---------------------------------------------------------------------------------

    /**
     * Grass tufts along the top of the ground band. Positions are deterministic, so the line
     * does not shimmer when the scene redraws on the next clock tick. Papercraft keeps hard
     * cardstock edges instead of strokes, and does not call this.
     */
    fun drawGrassLine(
        drawScope: DrawScope,
        composition: SceneComposition,
        color: Color,
        brightness: Float
    ) {
        val width = composition.widthPx
        val height = composition.heightPx
        val tufts = 96
        val baseAlpha = 0.30f + 0.30f * brightness

        for (i in 0 until tufts) {
            val r1 = pseudoRandom(i * 2_654_435_761.toInt())
            val r2 = pseudoRandom(i * 40_503 + 17)
            val r3 = pseudoRandom(i * 12_289 + 91)
            val x = width * ((i + r1 * 0.9f) / tufts)
            // Tufts run from the grass line down into the first few percent of the ground, thinning as
            // they go, so the strip under the ridge is not a flat band of colour.
            val depth = r2 * height * 0.11f
            val y = composition.groundTopY + depth
            val bladeHeight = height * (0.008f + r3 * 0.014f) * (1f - depth / (height * 0.13f)).coerceIn(0.4f, 1f)
            val lean = (r1 - 0.5f) * bladeHeight * 0.45f
            val alpha = baseAlpha * (0.55f + 0.45f * r2) * (1f - depth / (height * 0.22f)).coerceIn(0.30f, 1f)

            drawScope.drawLine(
                color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
                start = Offset(x, y),
                end = Offset(x + lean, y - bladeHeight),
                strokeWidth = 1.4f + r3 * 1.1f,
                cap = StrokeCap.Round
            )
            // A second, shorter blade on most tufts gives the line some body.
            if (r3 > 0.35f) {
                drawScope.drawLine(
                    color = color.copy(alpha = (alpha * 0.75f).coerceIn(0f, 1f)),
                    start = Offset(x + 1.6f, y),
                    end = Offset(x + lean * 1.8f - 1.2f, y - bladeHeight * 0.68f),
                    strokeWidth = 1.1f,
                    cap = StrokeCap.Round
                )
            }
        }
    }

    /**
     * Small deterministic 0..1 value.
     *
     * The mask clears the sign bit, so the value is in `0 .. 2^31-1`; dividing by `2^31` is what puts it
     * in `0 .. 1`. The divisor was `2^24-1` once, which mapped the same value to `0 .. 128` — the grass
     * line was then laid out over 128 screen widths, and 95 of the 96 tufts were drawn outside the
     * canvas (only one of them was ever visible). Any change here changes the silhouette of the ground,
     * so it is worth re-reading this comment before "tidying" the constant.
     */
    private fun pseudoRandom(seed: Int): Float {
        var x = seed * 1_103_515_245 + 12_345
        x = x xor (x ushr 13)
        x *= 0x5bd1e995
        x = x xor (x ushr 15)
        return (x and 0x7FFFFFFF) / 2_147_483_648f
    }
}

/** This colour at [factor]× brightness (alpha unchanged). */
private fun Color.scaled(factor: Float): Color = Color(
    red = (red * factor).coerceIn(0f, 1f),
    green = (green * factor).coerceIn(0f, 1f),
    blue = (blue * factor).coerceIn(0f, 1f),
    alpha = alpha
)
