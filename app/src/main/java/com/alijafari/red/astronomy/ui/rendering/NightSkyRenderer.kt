package com.alijafari.red.astronomy.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.alijafari.red.astronomy.ui.skypanorama.SkyNightModel
import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * What the night sky looks like under the theme colours: how much airglow, how much light dome,
 * how much moonlight, and how visible the galaxy is in it.
 *
 * Compose-free and computed from the app's own values, so it can be unit-tested on the JVM. It is the
 * Canvas counterpart of [com.alijafari.red.astronomy.ui.skypanorama.SkyNightModel], which does the
 * same photometry for the GL panorama; this one turns the same numbers into the handful of colours
 * and alphas a Compose `DrawScope` can afford to paint.
 */
data class NightSkyPalette(
    /** 0 = daytime palette only, 1 = the night sky itself. */
    val strength: Float,
    /** Natural airglow at the zenith, as a theme colour. */
    val zenith: Color,
    /** The same airglow seen through the long path near the horizon. */
    val horizonGlow: Color,
    /** The local light dome: sodium/LED tinted, hugging the horizon. */
    val lightDome: Color,
    /** The moonlit wash on the sky, 0 none. */
    val moonGlow: Color,
    /** How much of the Milky Way survives this sky, 0..1. */
    val galaxyVisibility: Float,
    /** Alpha of the airglow at the zenith. */
    val zenithAlpha: Float,
    /** Alpha of the brighter airglow at the horizon. */
    val horizonAlpha: Float,
    /** Alpha of the light dome at the horizon. */
    val lightDomeAlpha: Float,
    /** Alpha of the moonlight wash where it is brightest (at the Moon). */
    val moonGlowAlpha: Float
) {
    companion object {
        /** Dayside default: everything off, the theme palette is drawn unchanged. */
        val DEFAULT = NightSkyPalette(
            strength = 0f,
            zenith = Color.Transparent,
            horizonGlow = Color.Transparent,
            lightDome = Color.Transparent,
            moonGlow = Color.Transparent,
            galaxyVisibility = 1f,
            zenithAlpha = 0f,
            horizonAlpha = 0f,
            lightDomeAlpha = 0f,
            moonGlowAlpha = 0f
        )

        /**
         * The night for a Bortle class and a Moon.
         *
         * The luminance anchors come from [SkyNightModel] (21.95 mag/arcsec² natural zenith, the
         * Bortle class' own zenith brightness, the full-moon sky). They are mapped onto the theme's
         * `sky` and `horizon` tones with an exponent rather than hard-coded colours, so every theme
         * keeps its own night: Real Sky stays blue-black, Monochrome stays grey, Observatory stays red.
         *
         * @param sunAltDeg Sun altitude, degrees. The night exists only below the start of civil
         *   twilight; it fades in from -5° to -14° and is gone above -5°.
         */
        fun forNight(
            sunAltDeg: Double,
            moonAltDeg: Double,
            moonIlluminationPercent: Double,
            bortle: Int,
            themeSkyTone: Color,
            themeHorizonTone: Color
        ): NightSkyPalette {
            val strength = SkyNightModel.smoothstep(-5.0, -14.0, sunAltDeg).toFloat()
            if (strength <= 0.001f) return DEFAULT

            val light = shares(moonAltDeg, moonIlluminationPercent, bortle)
            val pollution = light.pollution
            val moonlight = light.moonlight
            val moonWash = light.moonWash

            val zenithLum = light.zenithLevel
            val horizonLum = light.horizonLevel

            // A moonlit night is blue-grey, not merely "a dark sky with a glow in it": the whole
            // dome is lifted and desaturated towards the Moon's colour, most at the zenith where the
            // scattering is strongest.
            val zenith = mix(mix(themeSkyTone, RealSkyNight.airglowZenith, 0.55f), RealSkyNight.moonGlowTint, moonWash * 0.65f)
            val horizonGlow = mix(themeHorizonTone, RealSkyNight.airglowHorizon, 0.55f + 0.25f * moonlight)
            val lightDome = mix(RealSkyNight.lightDomeTint, horizonGlow, 0.35f)
            val moonGlow = RealSkyNight.moonGlowTint

            return NightSkyPalette(
                strength = strength,
                zenith = zenith,
                horizonGlow = horizonGlow,
                lightDome = lightDome,
                moonGlow = moonGlow,
                galaxyVisibility = light.galaxyVisibility * strength,
                // Zenith airglow is a very dark blue-green measured in the hundredths of a display
                // level; the Moon lifts it by up to ~0.2 of full scale, which is what a full Moon
                // does to a dark sky.
                zenithAlpha = (0.055f * zenithLum.coerceAtMost(2.0).toFloat() + 0.22f * moonWash)
                    .coerceIn(0f, 0.30f) * strength,
                horizonAlpha = (0.085f * horizonLum.coerceAtMost(2.4).toFloat() + 0.05f * pollution + 0.10f * moonWash)
                    .coerceIn(0f, 0.34f) * strength,
                lightDomeAlpha = (0.10f * pollution + 0.02f).coerceIn(0f, 0.22f) * strength,
                moonGlowAlpha = (0.10f * moonWash * (0.35f + 0.65f * moonlight)).coerceIn(0f, 0.16f) * strength
            )
        }

        /**
         * The relative light budget of one night: how much of the sky is artificial, how much is
         * moonlight, and how much brighter the Moon has made it. Shared by [forNight] and
         * [NightSkyRenderer.galaxyVisibility] so the Canvas night and the shadow it puts on the
         * Milky Way can never disagree.
         */
        data class LightShares(
            val pollution: Float,
            val moonlight: Float,
            val moonWash: Float,
            val zenithLevel: Double,
            val horizonLevel: Double,
            val galaxyVisibility: Float
        )

        internal fun shares(moonAltDeg: Double, moonIlluminationPercent: Double, bortle: Int): LightShares {
            val natural = SkyNightModel.naturalAirglow
            val artificial = SkyNightModel.artificialLuminance(bortle)
            val moonSky = SkyNightModel.moonSkyLuminance(moonAltDeg, moonIlluminationPercent)
            val dark = maxOf(natural + artificial, 1e-9)
            val total = maxOf(dark + moonSky, 1e-9)

            // Relative weight of each contribution, 0..1. These are the numbers that change what the
            // eye sees: a light-polluted sky is orange-brown and hides the galaxy, a moonlit sky is
            // blue-grey and washes it out.
            val pollution = (artificial / total).toFloat().coerceIn(0f, 1f)
            val moonlight = (moonSky / total).toFloat().coerceIn(0f, 1f)

            // How much brighter the Moon makes the sky than it would be without it, as a 0..1 wash.
            // A full Moon takes a dark zenith from ~22 to ~18 mag/arcsec² — a factor of 40 — and that
            // is unmistakable to a human observer, so it cannot be represented by the few-percent
            // share the Moon has of the total sky light. Measured in magnitudes of brightening (the
            // scale the eye responds to), a full Moon sits at 1 and a thin crescent well below it.
            val moonWash = (log10(total / dark) / 1.4).coerceIn(0.0, 1.0).toFloat()

            return LightShares(
                pollution = pollution,
                moonlight = moonlight,
                moonWash = moonWash,
                zenithLevel = (natural + artificial + moonSky * 0.45) / total,
                horizonLevel = (natural * 1.55 + artificial * 2.9 + moonSky * 0.85) / total,
                // The galaxy survives only a dark, moonless, unpolluted sky: it dims with the light
                // pollution that raises the sky's surface brightness and with the moonlight that caps
                // the eye's adaptation.
                galaxyVisibility = ((1f - pollution * 0.95f) * (1f - moonWash * 0.90f)).coerceIn(0f, 1f)
            )
        }

        private fun mix(a: Color, b: Color, t: Float): Color {
            val f = t.coerceIn(0f, 1f)
            return Color(
                red = a.red + (b.red - a.red) * f,
                green = a.green + (b.green - a.green) * f,
                blue = a.blue + (b.blue - a.blue) * f,
                alpha = 1f
            )
        }
    }
}

/** The colours of a real dark night, as sRGB. The airglow tints are the dark-sky values the eye sees. */
private object RealSkyNight {
    /** 557.7 nm OI airglow seen from a dark site: a very dark blue-green, not black. */
    val airglowZenith = Color(0xFF101A24)

    /** The same airglow along the long horizon path, plus scattered starlight. */
    val airglowHorizon = Color(0xFF1B2732)

    /** High-pressure sodium / white LED light dome. */
    val lightDomeTint = Color(0xFF3A2E1E)

    /** Moonlight scattering on the atmosphere: a cool blue-grey wash. */
    val moonGlowTint = Color(0xFF2A3A52)
}

/**
 * Paints the part of the night sky that is not stars or the galaxy.
 *
 * A real night is never one flat colour. It is the natural airglow, brightest towards the horizon
 * because the line of sight passes through more of the emitting layer; the local light dome, hugging
 * the same horizon; and, when the Moon is up, a blue-grey wash strongest around it. The old renderer
 * drew a single vertical gradient and one flat band, which is why the night read as a gradient rather
 * than as a sky. These three are painted instead, each with its own shape and each fading into the
 * other with no seams.
 */
object NightSkyRenderer {

    /**
     * The zenith-to-horizon night, over the palette gradient the caller has already drawn.
     *
     * The airglow is confined to the sky above [horizonY]. The light dome and the Moon's wash are
     * centred on the horizon and on the Moon, so a little of them extends below it — which is
     * correct: a light dome is emitted from the ground, and the ground is drawn over it afterwards.
     */
    fun drawSkyGlow(
        drawScope: DrawScope,
        horizonY: Float,
        palette: NightSkyPalette,
        moonPosPx: Offset? = null
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val skyHeight = horizonY.coerceAtLeast(1f)
        if (palette.strength <= 0.004f) return

        // 1. Airglow, zenith to horizon. A real night brightens slowly, then quickly in the last ~15
        //    degrees, because the line of sight crosses more of the emitting layer (airmass). The
        //    stops below are that curve, not a straight line.
        drawScope.drawRect(
            brush = Brush.verticalGradient(
                0.00f to withAlpha(palette.zenith, palette.zenithAlpha),
                0.45f to withAlpha(palette.zenith, palette.zenithAlpha * 1.35f),
                0.72f to withAlpha(palette.horizonGlow, palette.horizonAlpha * 0.62f),
                0.90f to withAlpha(palette.horizonGlow, palette.horizonAlpha),
                1.00f to withAlpha(palette.horizonGlow, palette.horizonAlpha * 0.92f),
                startY = 0f,
                endY = skyHeight
            ),
            topLeft = Offset.Zero,
            size = Size(width, skyHeight)
        )

        // 2. The light dome. It sits on the horizon and is wider than it is tall, so it is painted as
        //    a soft ellipse across the bottom of the sky, not a band of constant colour.
        if (palette.lightDomeAlpha > 0.004f) {
            val domeHeight = (skyHeight * 0.30f).coerceAtLeast(24f)
            val centre = Offset(width * 0.5f, horizonY + domeHeight * 0.25f)
            val radius = (width * 0.95f).coerceAtLeast(domeHeight * 2.2f)
            drawScope.drawCircle(
                brush = Brush.radialGradient(
                    0.0f to withAlpha(palette.lightDome, palette.lightDomeAlpha),
                    0.55f to withAlpha(palette.lightDome, palette.lightDomeAlpha * 0.45f),
                    1.0f to Color.Transparent,
                    center = centre,
                    radius = radius
                ),
                radius = radius,
                center = centre
            )
        }

        // 3. Moonlight: a cool wash centred on the Moon that reaches across the sky. It follows the
        //    Moon's screen position when the caller has one, and sits at the top otherwise.
        if (palette.moonGlowAlpha > 0.004f) {
            val centre = moonPosPx ?: Offset(width * 0.5f, height * 0.06f)
            val radius = (width * 1.15f).coerceAtLeast(height * 0.55f)
            drawScope.drawCircle(
                brush = Brush.radialGradient(
                    0.0f to withAlpha(palette.moonGlow, palette.moonGlowAlpha),
                    0.35f to withAlpha(palette.moonGlow, palette.moonGlowAlpha * 0.60f),
                    0.70f to withAlpha(palette.moonGlow, palette.moonGlowAlpha * 0.22f),
                    1.0f to Color.Transparent,
                    center = centre,
                    radius = radius
                ),
                radius = radius,
                center = centre
            )
        }
    }

    /** The same colour at a different opacity, keeping its channels exactly. */
    private fun withAlpha(color: Color, alpha: Float): Color =
        Color(color.red, color.green, color.blue, alpha.coerceIn(0f, 1f))

    /**
     * The galaxy's own visibility under this sky, as a 0..1 multiplier the Milky Way renderer uses.
     * Kept as a function so callers can ask without building a whole palette.
     */
    fun galaxyVisibility(sunAltDeg: Double, moonAltDeg: Double, moonIlluminationPercent: Double, bortle: Int): Float {
        val light = NightSkyPalette.shares(moonAltDeg, moonIlluminationPercent, bortle)
        val strength = SkyNightModel.smoothstep(-5.0, -14.0, sunAltDeg).toFloat()
        return light.galaxyVisibility * strength
    }

    /** Number of sky-transparency steps the airglow has, used by the tests to pin the curve. */
    fun airglowStops(): Int = 5

    /** Alpha of the airglow at the horizon for a palette, quantised to 8-bit display. */
    internal fun horizonAlphaBytes(palette: NightSkyPalette): Int = (palette.horizonAlpha * 255f).roundToInt()
}
