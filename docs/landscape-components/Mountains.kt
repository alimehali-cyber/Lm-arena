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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/**
 * MOUNTAINS — every mountain silhouette drawn on the Home sky canvas.
 *
 * Provenance: extracted verbatim from
 * `app/src/main/java/com/alijafari/red/astronomy/ui/rendering/SceneGroundRenderer.kt`
 * (repo commit 716586ee67074e542485cbe5e7f6af9bcaabbdb0, file SHA-256 f8cdaac1…):
 *
 *  - `drawMountainLayer(...)`      lines 425–537
 *  - the range draws inside `drawMarvdashtLandscape(...)`
 *                                  lines 590–635   (far Mamasani folds + oak speckle)
 *                                  lines 661–812   (Dimeh-Mil massif: benched profile,
 *                                                   gullies, talus cones, varnish streaks,
 *                                                   the Tang-e Farakh gorge, oak speckle)
 *                                  lines 815–861   (piedmont foothills + alluvial fans)
 *  - `ridgePath` / `heightFor`     lines 2362–2399 (the stylised themes' ridges)
 *
 * Nothing here is new art: only the function seams are new. The locals that used to be
 * shared inside the single 1,490-line Marvdasht function are either recomputed from the
 * same palette formulas (`steppeTop`, `sand`, `rockLit`, `rockShade`) or passed in
 * (`composition`, `palette`, `lighting`, `framing`, `sunPosPx`).
 *
 * Draw order: mountains go down *before* the plain and the river, so call
 * [draw] first, then `GrassField.draw`, then `PulvarRiver.draw`.
 *
 * Depends on the repo's existing, unchanged types:
 * `SceneComposition`, `ScenePalette`, `LightingState`, `PersepolisFraming.Result`,
 * and `LandscapeProfile` (the noise + skyline maths).
 */
object LandscapeMountains {

    // Art-directed skyline envelopes (see LandscapeProfile): x in 0..1 across the canvas.
    // Far Mamasani folds: a complete encircling range, saddled behind Persepolis. The
    // Dimeh-Mil ridge (the Mil-e Azhdaha tower's own massif west of Nurabad): a
    // low shoulder behind the ruins and readout, swelling to the great crest at the right
    // that backs the Mil-e Azhdaha tower, then continuing off-frame.
    private val MAMASANI_FOLD_ENVELOPE = floatArrayOf(
        0f, 0.58f, 0.18f, 0.78f, 0.38f, 0.64f, 0.55f, 0.76f, 0.72f, 0.72f, 0.88f, 0.82f, 1f, 0.74f
    )

    // The Dimeh-Mil ridge behind Mil-e Azhdaha (Kuh-e Shirmard / Tang-e Kalleh massif,
    // west of Nurabad): a folded whaleback anticline — long gentle back-limb rising from
    // the left, hinge near x = 0.76, then the crest continuing high off-frame at the right.
    private val DIMEH_MIL_ENVELOPE = floatArrayOf(
        0f, 0.26f, 0.14f, 0.34f, 0.30f, 0.42f, 0.45f, 0.50f, 0.58f, 0.55f,
        0.68f, 0.70f, 0.76f, 0.85f, 0.86f, 0.76f, 1f, 0.60f
    )

    private val FOOTHILL_ENVELOPE = floatArrayOf(0f, 0.7f, 0.5f, 1f, 1f, 0.5f)

    /**
     * The whole mountain stack of the photographic Home scene, back to front.
     *
     * @param sunPosPx the Sun's projected screen position this frame, or null when it is far
     * below the horizon — it decides which side of every ridge is lit (`sunSign`).
     */
    fun draw(
        drawScope: DrawScope,
        composition: SceneComposition,
        palette: ScenePalette,
        lighting: LightingState,
        framing: PersepolisFraming.Result,
        sunPosPx: Offset?
    ) {
        val width = composition.widthPx
        val height = composition.heightPx
        val groundTopY = composition.groundTopY
        val toeY = framing.toeYPx
        val day = lighting.ambientBrightness.coerceIn(0f, 1f)
        val moonGlow = lighting.moonGlowIntensity.coerceIn(0f, 1f)
        val lightLevel = maxOf(day, moonGlow * 0.35f)
        val sunSign = if (sunPosPx != null && sunPosPx.x >= width * 0.5f) 1f else -1f
        val atmosphere = palette.haze
        val sand = lerp(Color(0xFF292216), Color(0xFFBEA06E), day)

        val rockLit = lerp(Color(0xFF161C28), Color(0xFFA68B6E), lightLevel)
        val rockShade = lerp(Color(0xFF0B1018), Color(0xFF6E5A4C), lightLevel)

        // The plain's top tone, recomputed here because the alluvial fans are mixed from
        // it (the plain fill itself lives in GrassField.kt, and this is its exact formula).
        val steppeTop = lerp(palette.groundTop, Color(0xFF5F7F45), 0.62f * day)

        // 1. Far Zagros range: the encircling mountains, deep in aerial perspective. Its
        //    strips fill down to the plain so no sky sliver survives under the range.
        val farBase = groundTopY + height * 0.006f
        drawMountainLayer(
            drawScope = drawScope, width = width, sceneHeight = height, baseY = farBase,
            heights = LandscapeProfile.mountainHeights(
                190, height * 0.095f, MAMASANI_FOLD_ENVELOPE, seed = 11,
                benchAmp = 0.08f, benchStep = 0.16f
            ),
            // Opaque and only lightly hazed: at 0.72 alpha washed 55% toward a
            // near-white haze, this range plus the sky bleeding through it was the
            // glowing white band across the daytime scene at the readout's date line.
            lit = lerp(palette.farRidge, rockLit, 0.35f).copy(alpha = 1f),
            shade = lerp(palette.farRidge, rockShade, 0.35f).copy(alpha = 1f),
            haze = atmosphere, hazeTop = 0.12f, hazeBase = 0.25f,
            sunSign = sunSign, facetStrength = 0.32f, strataAmp = 0f,
            microSeed = 3, crestAlpha = 0.05f + 0.10f * lightLevel, capAmount = 0.25f,
            scrub = lerp(Color(0xFF0D130D), Color(0xFF46543C), lightLevel), scrubAmt = 0.35f
        )
        // Far oak-speckle: the folded Mamasani hills carry open oak woodland; at this
        // range it is only a grain in the slope tone, denser toward the feet.
        run {
            val farHeights = LandscapeProfile.mountainHeights(
                190, height * 0.095f, MAMASANI_FOLD_ENVELOPE, seed = 11,
                benchAmp = 0.08f, benchStep = 0.16f
            )
            val speck = Path()
            for (k in 0 until 130) {
                val x = width * LandscapeProfile.hash01(401, k * 13 + 3)
                val top = farBase - LandscapeProfile.heightAt(farHeights, (x / width).coerceIn(0f, 1f))
                val hh = farBase - top
                if (hh < height * 0.03f) continue
                val y = top + hh * (0.35f + 0.60f * LandscapeProfile.hash01(403, k * 7 + 1))
                val r = height * (0.0010f + 0.0012f * LandscapeProfile.hash01(405, k * 5 + 2))
                speck.addOval(
                    androidx.compose.ui.geometry.Rect(x - r, y - r * 0.7f, x + r, y + r * 0.7f)
                )
            }
            drawScope.drawPath(
                speck,
                lerp(Color(0xFF0A0F0A), Color(0xFF3E4C34), lightLevel).copy(alpha = 0.10f + 0.16f * lightLevel)
            )
        }

        // (No horizon mist shelf here either — with the hazy far range behind it, the old
        // shelf stacked into a white band across the canvas at the readout's day/date line.
        // The range's own haze dissolve supplies the aerial perspective by itself.)

        // 4. The Dimeh-Mil massif: the rocky, relatively green limestone ridge the
        //    Mil-e Azhdaha tower stands against west of Nurabad (Kuh-e Shirmard /
        //    Tang-e Kalleh). Nearer, paler and more strongly faceted than the far
        //    folds, with folded strata, gullies — and the wide gorge (Tang-e Farakh)
        //    cut through its crest, the spring at its mouth feeding the green below.
        val massifBase = toeY - height * 0.004f
        val massifHeights = LandscapeProfile.mountainHeights(
            210, height * 0.145f, DIMEH_MIL_ENVELOPE, seed = 29,
            benchAmp = 0.16f, benchStep = 0.13f,
            notches = floatArrayOf(
                0.70f, 0.034f, 0.42f, // Tang-e Farakh: the wide gorge through the crest
                0.34f, 0.020f, 0.24f  // a lesser cleft on the back-limb
            )
        )
        drawMountainLayer(
            drawScope = drawScope, width = width, sceneHeight = height, baseY = massifBase,
            heights = massifHeights,
            lit = lerp(rockLit, Color(0xFFC4AC84), 0.25f * lightLevel), // white-veined limestone
            shade = rockShade,
            haze = atmosphere, hazeTop = 0.05f, hazeBase = 0.14f,
            sunSign = sunSign, facetStrength = 0.78f, strataAmp = 0.06f,
            microSeed = 5, crestAlpha = 0.10f + 0.18f * lightLevel,
            strataDip = 0.9f, hingeX = width * 0.76f, capAmount = 0.45f,
            scrub = lerp(Color(0xFF0D140D), Color(0xFF4C5E3A), lightLevel), scrubAmt = 0.5f
        )
        // Erosion gullies: dark seams from crest to fan apex with a faint lit lip.
        fun massifTopAt(x: Float): Float =
            massifBase - LandscapeProfile.heightAt(massifHeights, (x / width).coerceIn(0f, 1f))
        val gullyDark = rockShade.scaled(0.55f).copy(alpha = 0.10f + 0.22f * lightLevel)
        val gullyLip = rockLit.copy(alpha = 0.12f * lightLevel)
        val gullyWidth = (height * 0.0016f).coerceIn(1f, 3f)
        for (k in 0 until 11) {
            val p = 0.35f + 0.63f * LandscapeProfile.hash01(317, k * 13 + 4)
            val x0 = p * width
            val top0 = massifTopAt(x0)
            if (massifBase - top0 < height * 0.04f) continue
            val drift = (LandscapeProfile.hash01(173, k * 7 + 9) - 0.5f) * width * 0.05f
            val dark = Path()
            val lip = Path()
            for (s in 0..8) {
                val t = s / 8f
                val x = x0 + drift * t + sin(t * 5f + k * 1.7f) * height * 0.004f * (1f - t)
                val y = maxOf(top0 + (massifBase - top0) * t, massifTopAt(x) + 2f)
                if (s == 0) {
                    dark.moveTo(x, y)
                    lip.moveTo(x + gullyWidth, y)
                } else {
                    dark.lineTo(x, y)
                    lip.lineTo(x + gullyWidth, y)
                }
            }
            drawScope.drawPath(dark, gullyDark, style = Stroke(width = gullyWidth))
            drawScope.drawPath(lip, gullyLip, style = Stroke(width = gullyWidth * 0.6f))
        }
        // Talus cones under the steepest faces: scree resting at ~35 degrees of repose,
        // paler and warmer than the bedrock above it.
        run {
            val nIdx = massifHeights.size - 1
            val stepX = width / nIdx
            val chosenX = ArrayList<Float>()
            val order = (4 until nIdx - 3).sortedByDescending { i ->
                abs(massifHeights[i + 3] - massifHeights[i - 3])
            }
            for (i in order) {
                val h = massifHeights[i]
                val x = stepX * i
                if (h < height * 0.07f) continue
                if (chosenX.any { abs(it - x) < width * 0.09f }) continue
                chosenX.add(x)
                val apexY = massifBase - h * 0.38f
                val halfW = h * 0.38f * 1.43f
                val footY = massifBase + height * 0.012f
                val cone = Path().apply {
                    moveTo(x, apexY)
                    lineTo(x - halfW, footY)
                    lineTo(x + halfW, footY)
                    close()
                }
                drawScope.drawPath(cone, sand.copy(alpha = 0.05f + 0.11f * lightLevel))
                for (r in listOf(-0.5f, 0f, 0.5f)) {
                    drawScope.drawLine(
                        color = sand.copy(alpha = 0.04f + 0.06f * lightLevel),
                        start = Offset(x, apexY),
                        end = Offset(x + halfW * r, footY),
                        strokeWidth = 1f,
                        cap = StrokeCap.Round
                    )
                }
                if (chosenX.size >= 5) break
            }
        }
        // Desert-varnish streaks: dark rain-washed runnels on the steepest cliffs only.
        run {
            val nIdx = massifHeights.size - 1
            val stepX = width / nIdx
            val streakColor = rockShade.scaled(0.45f).copy(alpha = 0.10f + 0.12f * lightLevel)
            var drawn = 0
            for (i in 3 until nIdx - 2) {
                val slope = abs(massifHeights[i + 2] - massifHeights[i - 2]) /
                    (stepX * 4f).coerceAtLeast(1f)
                if (slope < 0.55f) continue
                if (LandscapeProfile.hash01(331, i * 7 + 3) > 0.45f) continue
                val x = stepX * i + (LandscapeProfile.hash01(337, i * 5 + 1) - 0.5f) * stepX
                val y0 = massifTopAt(x) + height * 0.006f
                drawScope.drawLine(
                    color = streakColor,
                    start = Offset(x, y0),
                    end = Offset(
                        x + height * 0.002f,
                        y0 + height * (0.016f + 0.020f * LandscapeProfile.hash01(339, i * 3 + 2))
                    ),
                    strokeWidth = (height * 0.0016f).coerceIn(1f, 3f),
                    cap = StrokeCap.Round
                )
                if (++drawn >= 14) break
            }
        }
        // The gorge (Tang-e Farakh): a shadowed cleft where the notch cuts the crest,
        // and the Dimeh-Mil spring greening its mouth — the tower's valley is watered.
        run {
            val gx = width * 0.70f
            val gTop = massifTopAt(gx)
            val gorge = Path().apply {
                moveTo(gx - width * 0.016f, gTop - height * 0.002f)
                lineTo(gx + width * 0.016f, gTop - height * 0.002f)
                lineTo(gx + width * 0.004f, massifBase - height * 0.010f)
                lineTo(gx - width * 0.004f, massifBase - height * 0.010f)
                close()
            }
            drawScope.drawPath(gorge, rockShade.scaled(0.5f).copy(alpha = 0.28f + 0.20f * lightLevel))
            drawSoftPatch(
                drawScope, gx, massifBase + height * 0.004f, width * 0.07f, height * 0.011f,
                lerp(Color(0xFF0E1A0E), Color(0xFF55763E), lightLevel),
                peakAlpha = 0.08f + 0.16f * lightLevel, layers = 8
            )
        }
        // Near oak-speckle on the massif: open Quercus brantii woodland grain.
        run {
            val speck = Path()
            for (k in 0 until 320) {
                val x = width * LandscapeProfile.hash01(409, k * 13 + 3)
                val top = massifTopAt(x)
                val hh = massifBase - top
                if (hh < height * 0.045f) continue
                val y = top + hh * (0.30f + 0.66f * LandscapeProfile.hash01(411, k * 7 + 1))
                val r = height * (0.0012f + 0.0016f * LandscapeProfile.hash01(413, k * 5 + 2))
                speck.addOval(
                    androidx.compose.ui.geometry.Rect(x - r, y - r * 0.7f, x + r, y + r * 0.7f)
                )
            }
            drawScope.drawPath(
                speck,
                lerp(Color(0xFF0A0F0A), Color(0xFF42532F), lightLevel).copy(alpha = 0.14f + 0.20f * lightLevel)
            )
        }
        // (No haze-pooling wash at the massif foot: another full-width pale rect, and
        // one more contributor to the horizontal white band. Banned like the rest.)

        // 5. Piedmont foothills: one low eroded undulation in front of the massif foot.
        val footBase = toeY + height * 0.028f
        val footHeights = LandscapeProfile.foothillHeights(80, height * 0.021f, FOOTHILL_ENVELOPE, seed = 77)
        val foothill = Path().apply {
            val n = footHeights.size - 1
            moveTo(0f, footBase - footHeights[0])
            for (i in 1..n) lineTo(width * i / n, footBase - footHeights[i])
            lineTo(width, footBase)
            lineTo(0f, footBase)
            close()
        }
        drawScope.drawPath(
            path = foothill,
            brush = Brush.verticalGradient(
                0.0f to lerp(palette.midRidge, Color(0xFF66763F), 0.5f * day),
                1.0f to lerp(palette.midRidge.scaled(0.70f), Color(0xFF3F4E2A), 0.5f * day),
                startY = footBase - height * 0.021f,
                endY = footBase
            )
        )

        // 6. Alluvial fans: pale gravel skirts where the gullies debouch onto the plain.
        val fanColor = lerp(steppeTop, Color(0xFFB29066), 0.55f * day)
            .copy(alpha = 0.14f + 0.12f * day)
        for ((p, spread) in listOf(0.56f to 0.075f, 0.63f to 0.095f, 0.82f to 0.08f, 0.93f to 0.07f)) {
            val ax = p * width
            val ay = massifBase - 2f
            val by = toeY + height * 0.036f
            val fan = Path().apply {
                moveTo(ax, ay)
                for (j in 0..6) {
                    val t = j / 6f
                    lineTo(
                        ax + width * spread * (t * 2f - 1f),
                        by + height * 0.007f * sin(PI.toFloat() * t)
                    )
                }
                close()
            }
            drawScope.drawPath(fan, fanColor)
            for (r in listOf(-0.55f, 0f, 0.55f)) {
                drawScope.drawLine(
                    color = sand.copy(alpha = 0.08f * lightLevel),
                    start = Offset(ax, ay),
                    end = Offset(ax + width * spread * r, by),
                    strokeWidth = 1f,
                    cap = StrokeCap.Round
                )
            }
        }
    }

    /**
     * The two stylised ridges, for the themes that are not photographic (flat illustration is
     * those themes' whole point): a distant ridge under the horizon and a nearer, darker one
     * anchored on the ground band. Same arithmetic as the shipped stylised path.
     *
     * @param papercraft when true each ridge is also outlined with [ScenePalette.cutEdge], the
     * hard cardstock edge the diorama theme wants.
     */
    fun drawStylisedRidges(
        drawScope: DrawScope,
        composition: SceneComposition,
        palette: ScenePalette,
        papercraft: Boolean
    ) {
        val width = composition.widthPx
        val height = composition.heightPx
        val horizonY = composition.horizonY

        // 1. Distant ridge, just under the horizon.
        val farRidge = ridgePath(
            width = width,
            baselineY = horizonY + height * 0.012f,
            amplitude = composition.farRidgeAmplitude,
            phase = 0.6f,
            waves = 3.0f,
            detail = 7.0f
        )
        drawScope.drawPath(path = farRidge, color = palette.farRidge)
        if (papercraft) {
            drawScope.drawPath(path = farRidge, color = palette.cutEdge, style = Stroke(width = 1.4f))
        }

        // 3. Mid ridge, closer and darker, anchored on the ground band.
        val midRidge = ridgePath(
            width = width,
            baselineY = composition.groundTopY,
            amplitude = composition.midRidgeAmplitude,
            phase = 2.1f,
            waves = 2.0f,
            detail = 9.0f
        )
        drawScope.drawPath(path = midRidge, color = palette.midRidge)
        if (papercraft) {
            drawScope.drawPath(path = midRidge, color = palette.cutEdge, style = Stroke(width = 1.4f))
        }
    }

    /**
     * One mountain range as sun-faceted vertical strips over a [LandscapeProfile] skyline.
     *
     * Each strip is a trapezoid from the crest line down to [baseY], filled with a vertical
     * gradient: slope-facing facet colour at the top (lit/shade chosen by the profile's wide-
     * window slope against the Sun's side, so whole faces light coherently instead of
     * striping), darkening and dissolving into [haze] toward the foot — aerial perspective
     * doing the depth work. A whisper of altitude banding ([strataAmp]) suggests folded
     * limestone bedding without drawing a single hard contour, and a hairline crest
     * catch-light separates the skyline from the sky at glancing light.
     */
    private fun drawMountainLayer(
        drawScope: DrawScope,
        width: Float,
        sceneHeight: Float,
        baseY: Float,
        heights: FloatArray,
        lit: Color,
        shade: Color,
        haze: Color,
        hazeTop: Float,
        hazeBase: Float,
        sunSign: Float,
        facetStrength: Float,
        strataAmp: Float,
        microSeed: Int,
        crestAlpha: Float,
        strataDip: Float = 0f,
        hingeX: Float = 0f,
        capAmount: Float = 0f,
        scrub: Color = Color.Transparent,
        scrubAmt: Float = 0f
    ) {
        val n = heights.size - 1
        if (n < 2 || width <= 0f) return
        val step = width / n
        for (i in 0 until n) {
            val h0 = heights[i]
            val h1 = heights[i + 1]
            if (h0 < 1f && h1 < 1f) continue
            val x0 = step * i
            val x1 = step * (i + 1)
            val top0 = baseY - h0
            val top1 = baseY - h1
            val lo = heights[(i - 2).coerceAtLeast(0)]
            val hi = heights[(i + 3).coerceAtMost(n)]
            val slope = (hi - lo) / (step * 4f).coerceAtLeast(1f)
            val facing = (-slope * 1.05f * sunSign).coerceIn(-1f, 1f)
            val micro = 0.95f + 0.10f * LandscapeProfile.hash01(microSeed, i * 37 + 11)
            val facet = lerp(shade, lit, 0.5f + 0.5f * facing * facetStrength).scaled(micro)

            fun colorAt(frac: Float): Color {
                var c = lerp(facet, facet.scaled(0.80f), frac)
                c = lerp(c, haze, (hazeTop + (hazeBase - hazeTop) * frac).coerceIn(0f, 1f))
                if (capAmount > 0f) {
                    // Pale hard-limestone caprock hugging the crest (Asmari-type beds).
                    val cap = (1f - frac * 3.3f).coerceIn(0f, 1f)
                    if (cap > 0f) c = lerp(c, c.scaled(1.28f), cap * capAmount)
                }
                if (scrubAmt > 0f) {
                    // Oak-scrub slopes (Zagros Quercus brantii woodland clothes the
                    // lower two-thirds of these hills): a broken green-grey wash that
                    // strengthens toward the foot and breaks up per facet.
                    val vegBand = ((frac - 0.38f) / 0.62f).coerceIn(0f, 1f)
                    val vegNoise = 0.35f + 0.65f * LandscapeProfile.hash01(microSeed + 9, i * 13 + 5)
                    c = lerp(c, scrub, vegBand * scrubAmt * vegNoise)
                }
                if (strataAmp > 0f) {
                    val yTop = (top0 + top1) / 2f
                    val y = yTop + (baseY - yTop) * frac
                    // Beds dip away from the fold hinge in a chevron (fold-and-thrust Zagros),
                    // not flat contour lines: the phase field tilts with |x - hinge|.
                    val xMid = (x0 + x1) / 2f
                    val k = 1f + strataAmp *
                        sin((((baseY - y) + strataDip * abs(xMid - hingeX)) / sceneHeight) * 105f +
                            1.7f * sin(i * 0.11f))
                    c = c.scaled(k)
                }
                return c
            }

            val strip = Path().apply {
                moveTo(x0, top0)
                lineTo(x1, top1)
                lineTo(x1, baseY)
                lineTo(x0, baseY)
                close()
            }
            drawScope.drawPath(
                path = strip,
                brush = Brush.verticalGradient(
                    0.0f to colorAt(0f),
                    0.38f to colorAt(0.38f),
                    0.72f to colorAt(0.72f),
                    1.0f to colorAt(1f),
                    startY = minOf(top0, top1),
                    endY = baseY
                )
            )
        }
        if (crestAlpha > 0.01f) {
            val crest = Path()
            var penDown = false
            for (i in heights.indices) {
                val h = heights[i]
                val x = step * i
                val y = baseY - h
                if (h > sceneHeight * 0.012f) {
                    if (penDown) crest.lineTo(x, y) else {
                        crest.moveTo(x, y)
                        penDown = true
                    }
                } else {
                    penDown = false
                }
            }
            drawScope.drawPath(
                path = crest,
                color = lerp(lit, Color.White, 0.35f).copy(alpha = crestAlpha),
                style = Stroke(width = (sceneHeight * 0.0012f).coerceIn(0.8f, 2f))
            )
        }
    }

    /** A smoothed polyline ridge filled down to `baselineY + amplitude * 2`, closed. */
    private fun ridgePath(
        width: Float,
        baselineY: Float,
        amplitude: Float,
        phase: Float,
        waves: Float,
        detail: Float,
        points: Int = 36
    ): Path = Path().apply {
        val step = width / points
        var prevX = 0f
        var prevY = baselineY - heightFor(0f, waves, detail, phase, amplitude)
        moveTo(0f, prevY)
        for (i in 1..points) {
            val x = step * i
            val y = baselineY - heightFor(i / points.toFloat(), waves, detail, phase, amplitude)
            val midX = (prevX + x) / 2f
            val midY = (prevY + y) / 2f
            quadraticTo(prevX, prevY, midX, midY)
            prevX = x
            prevY = y
        }
        lineTo(width, prevY)
        lineTo(width, baselineY + amplitude * 2f)
        lineTo(0f, baselineY + amplitude * 2f)
        close()
    }

    /** Ridge height at [progress] in 0..1, always non-negative. */
    private fun heightFor(progress: Float, waves: Float, detail: Float, phase: Float, amplitude: Float): Float {
        val a = sin(progress * PI.toFloat() * 2f * waves + phase)
        val b = sin(progress * PI.toFloat() * 2f * detail + phase * 1.7f)
        return ((a * 0.65f + b * 0.35f) * 0.5f + 0.5f) * amplitude
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
