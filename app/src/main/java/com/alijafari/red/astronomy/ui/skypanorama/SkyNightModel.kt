package com.alijafari.red.astronomy.ui.skypanorama

import com.alijafari.red.astronomy.ar.core.SkyRenderModel
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * The physical night sky the panorama renders under the NASA galaxy plate. Pure Kotlin, no Android
 * or GL, so the whole model is unit-tested on the JVM.
 *
 * The plate is a photograph of the galaxy with a flat sky background, not a photograph of a night
 * sky: on its own it has no airglow, no light pollution, no zodiacal light, no moonlight and no
 * twilight, and it is identical at noon and at midnight. This model supplies everything else that a
 * real sky seen from the ground contains, in absolute photometric units, so the shader can add up
 * one physically consistent scene and tone-map it once:
 *
 * | Component | Source |
 * |---|---|
 * | Natural airglow | [SkyRenderModel.NATURAL_ZENITH_MAG_ARCSEC2], Krisciunas & Schaefer path term |
 * | Light pollution | Bortle zenith brightness ([SkyRenderModel.bortleZenithMagArcsec2]) minus natural |
 * | Zodiacal light + Gegenschein | ~22.9 and ~23.8 V mag/arcsec² peaks along the ecliptic |
 * | Moonlight | Krisciunas & Schaefer (1991) scattering function, scaled by the lunar phase |
 * | Twilight | Single-scattered sunlight from the solar depression angle |
 * | Galaxy peak | The plate's brightest texel mapped to ~18.9 V mag/arcsec² |
 * | Exposure | [SkyRenderModel.exposure] on the sky at the camera's own elevation |
 *
 * Every input is already computed for the frame (Sun and Moon altitude, Bortle class) or is a
 * physical constant, so nothing here invents a measurement ZIG does not have.
 */
object SkyNightModel {

    /** Natural zenith sky brightness (V mag/arcsec²) used for the airglow term. */
    const val AIRGLOW_ZENITH_MAG_ARCSEC2: Double = SkyRenderModel.NATURAL_ZENITH_MAG_ARCSEC2

    /** Zodiacal light at the ecliptic, 90° from the Sun (V mag/arcsec²), and its Gegenschein. */
    const val ZODIACAL_PEAK_MAG_ARCSEC2: Double = 23.0
    const val GEGENSCHEIN_MAG_ARCSEC2: Double = 23.8

    /**
     * Visual surface brightness of the brightest part of the Milky Way (the Sagittarius star
     * clouds), V mag/arcsec². The plate's brightest texel maps here after [GALAXY_GAMMA].
     */
    const val GALAXY_PEAK_MAG_ARCSEC2: Double = 18.9

    /**
     * The plate's equatorial background is a constant grey field (≈ 0.105 in sRGB), which is the
     * sky level of the star map, not the sky of a real night. It is subtracted before the plate is
     * used, and the residual is gamma-compressed onto the visual range of the galaxy.
     */
    const val PLATE_SKY_PEDESTAL: Double = 0.105
    const val GALAXY_GAMMA: Double = 0.90

    /**
     * Sky luminance 90° from a full Moon at the zenith, cd/m². This is the zenith sky brightness of
     * a full-moon night (~18 V mag/arcsec²) with the moon close to the zenith; the scattering
     * function then reshapes it over the sky and the illuminated fraction scales it.
     */
    val FULL_MOON_ZENITH_SKY: Double = SkyRenderModel.magArcsec2ToLuminance(18.2)

    /** Extinction coefficients (magnitudes/airmass) for starlight through the atmosphere. */
    const val K_EXTINCTION: Double = 0.26

    /**
     * How much of the Sun's twilight survives as scattered light, relative to a clear sky. ZIG has
     * no transparency measurement, so this is the clear-sky value.
     */
    const val TWILIGHT_TRANSPARENCY: Double = 1.0

    /** Everything the panorama shader needs from the sky for one instant. */
    data class Sky(
        /** Zenith extinction per channel (linear transmittance ratios turned into optical depth). */
        val tauR: Float,
        val tauG: Float,
        val tauB: Float,
        /** Natural airglow at the zenith, cd/m². */
        val airglow: Float,
        /** Artificial (Bortle excess) sky at the zenith, cd/m². */
        val lightPollution: Float,
        /** Photopic zenith transmittance, for the airglow airmass term. */
        val zenithTransmittance: Float,
        /** Sky luminance 90° from the Moon, cd/m², zero when the Moon is down. */
        val moonSky: Float,
        /** cd/m² for a unit galaxy texel. */
        val galaxyPeak: Float,
        val galaxyGamma: Float,
        val zodiacalPeak: Float,
        val gegenschein: Float,
        val twilightGain: Float,
        /** Exposure that maps the night at the camera's elevation onto the display. */
        val exposure: Float,
        /** 0 = daytime palette only, 1 = the physical night sky only. */
        val nightSkyWeight: Float,
        /** Quantised Moon inputs, for state equality and for tests. */
        val moonIllumination: Float,
        val moonAltitudeDeg: Float
    )

    /** Total zenith sky luminance (cd/m²) for a Bortle class: the number the eye adapts to. */
    fun zenithLuminance(bortle: Int): Double =
        SkyRenderModel.magArcsec2ToLuminance(SkyRenderModel.bortleZenithMagArcsec2(bortle))

    /** Natural zenith airglow luminance (cd/m²). */
    val naturalAirglow: Double = SkyRenderModel.magArcsec2ToLuminance(AIRGLOW_ZENITH_MAG_ARCSEC2)

    /** Artificial (light-pollution) zenith luminance (cd/m²) for a Bortle class. */
    fun artificialLuminance(bortle: Int): Double = max(0.0, zenithLuminance(bortle) - naturalAirglow)

    /** Galaxy peak luminance (cd/m²) the shader multiplies a unit plate texel by. */
    val galaxyPeakLuminance: Double = SkyRenderModel.magArcsec2ToLuminance(GALAXY_PEAK_MAG_ARCSEC2)

    /**
     * Luminance of the night sky the exposure is keyed on.
     *
     * The dark sky is what the eye adapts to, so the key is the *darkest* part of the sky that is
     * on screen — the zenith and a point 20° up, not the brighter horizon. Keying on the horizon
     * would expose the whole frame for the light dome and bury the galaxy. Twilight is not part of
     * the key: during twilight the sky is genuinely bright, and it must be allowed to look bright.
     */
    fun adaptationLuminance(airglow: Double, lightPollution: Double, moonSky: Double): Double {
        val zenith = airglow + lightPollution
        val lower = airglow * 1.18 + lightPollution * 2.4
        return max(min(zenith, lower) + moonSky * 0.35, 1e-7)
    }

    /**
     * Sky luminance 90° from the Moon, cd/m².
     *
     * Full Moon at the zenith gives [FULL_MOON_ZENITH_SKY]; the sky scales with the illuminated
     * fraction, is extinguished on the Moon's own path (the Moon is the light source), and is zero
     * once the Moon has set.
     */
    fun moonSkyLuminance(
        moonAltitudeDeg: Double,
        moonIlluminationPercent: Double,
        extinctionV: Double = 0.83
    ): Double {
        val above = smoothstep(-3.0, 5.0, moonAltitudeDeg)
        if (above <= 0.0) return 0.0
        val illum = (moonIlluminationPercent / 100.0).coerceIn(0.0, 1.0)
        if (illum <= 0.0) return 0.0
        // Airmass is 1 at the zenith by definition; the Kasten-Young fit returns 0.9997 there, which
        // would make the Moon's own path "amplify" its light by 0.005%. Clamped so the zenith value
        // is exactly [FULL_MOON_ZENITH_SKY].
        val airmassMoon = max(airmass(max(moonAltitudeDeg, -1.0)), 1.0)
        val transmission = extinctionV.pow(airmassMoon - 1.0)
        return FULL_MOON_ZENITH_SKY * (0.35 + 0.65 * illum) * above * transmission
    }

    /**
     * 0 in daylight and through civil twilight to 1 once the sky is fully dark, with the same
     * smoothstep shape the day/night palette blend uses so nothing steps.
     *
     * The physical night model itself fades out through twilight on its own (its twilight term
     * goes to zero as the Sun rises). This weight only decides how much of the *daytime palette*
     * is replaced, so it is fully replaced before the night terms vanish.
     */
    fun nightSkyWeight(sunAltDeg: Double): Float {
        if (sunAltDeg.isNaN()) return 1f
        val t = ((SkyPanoramaSkyModel.DAY_FULL_DEG + 2.0 - sunAltDeg) / 6.0).coerceIn(0.0, 1.0)
        return (t * t * (3.0 - 2.0 * t)).toFloat()
    }

    /**
     * The whole sky for one instant.
     *
     * @param sunAltDeg apparent altitude of the Sun, degrees.
     * @param moonAltDeg apparent altitude of the Moon, degrees.
     * @param moonIlluminationPercent illuminated fraction of the lunar disc, 0..100.
     * @param bortle the observer's Bortle class, 1..9.
     * @param cameraAltDeg altitude of the point the camera is looking at; the exposure is keyed on
     *   the dark sky at that elevation, so the same night is exposed the same way in every framing.
     * @param extinctionV zenith transmittance in V (0.83 is a clear sea-level night: k ≈ 0.20).
     */
    fun skyFor(
        sunAltDeg: Double,
        moonAltDeg: Double,
        moonIlluminationPercent: Double,
        bortle: Int,
        cameraAltDeg: Double = 40.0,
        extinctionV: Double = 0.83
    ): Sky {
        val tz = extinctionV.coerceIn(0.05, 1.0)
        val tauV = -kotlin.math.ln(tz)
        // Rayleigh extinction is strongly wavelength dependent: blue is lost far faster than red,
        // which is why the galaxy reddens towards the horizon.
        val tauR = 0.34 * tauV
        val tauG = 0.62 * tauV
        val tauB = 1.02 * tauV

        val natural = naturalAirglow
        val artificial = artificialLuminance(bortle)
        val moonSky = moonSkyLuminance(moonAltDeg, moonIlluminationPercent, tz)
        val adaptation = adaptationLuminance(natural, artificial, moonSky)
        // Exposure is normalised by the sky at the camera's elevation: the eye adapts to what fills
        // the frame. It uses the same dark-sky model as everything else, with the light dome over
        // the horizon included.
        val airmassCamera = airmass(cameraAltDeg)
        val glareAtCamera = natural * airmassCamera * tz.pow(airmassCamera - 1.0) +
            artificial * (0.40 + 2.5 * exp(-max(cameraAltDeg, 0.4) / 7.5) * 1.28)
        val keyLuminance = max(0.65 * adaptation + 0.35 * max(glareAtCamera, 1e-7), 1e-7)

        return Sky(
            tauR = tauR.toFloat(),
            tauG = tauG.toFloat(),
            tauB = tauB.toFloat(),
            airglow = natural.toFloat(),
            lightPollution = artificial.toFloat(),
            zenithTransmittance = tz.toFloat(),
            moonSky = moonSky.toFloat(),
            galaxyPeak = galaxyPeakLuminance.toFloat(),
            galaxyGamma = GALAXY_GAMMA.toFloat(),
            zodiacalPeak = SkyRenderModel.magArcsec2ToLuminance(ZODIACAL_PEAK_MAG_ARCSEC2).toFloat(),
            gegenschein = SkyRenderModel.magArcsec2ToLuminance(GEGENSCHEIN_MAG_ARCSEC2).toFloat(),
            twilightGain = TWILIGHT_TRANSPARENCY.toFloat(),
            exposure = SkyRenderModel.exposure(keyLuminance).toFloat(),
            nightSkyWeight = nightSkyWeight(sunAltDeg),
            moonIllumination = (moonIlluminationPercent / 100.0).coerceIn(0.0, 1.0).toFloat(),
            moonAltitudeDeg = moonAltitudeDeg.toFloat()
        )
    }

    /** Kasten & Young (1989) relative airmass. Mirrors the shader and [SkyRenderModel]. */
    fun airmass(altitudeDeg: Double): Double {
        val h = max(altitudeDeg, -0.9)
        return 1.0 / (sin(Math.toRadians(h)) + 0.50572 * (h + 6.07995).pow(-1.6364))
    }

    /** Krisciunas & Schaefer (1991) lunar scattering function, normalised to f(0) = 1. */
    fun moonScattering(rhoDeg: Double): Double {
        val c = kotlin.math.cos(Math.toRadians(rhoDeg))
        val f = 1.0e5 * (1.06 + c * c) + 1.0e6 * exp(-rhoDeg / 40.0)
        val f0 = 1.0e5 * 2.06 + 1.0e6
        return f / f0
    }

    /**
     * V-band sky brightness at an altitude and azimuth for a sky, V mag/arcsec².
     *
     * This is the number a Sky Quality Meter would read: the airglow path term plus the local light
     * dome. It exists so the model can be checked against published values, and so the UI could
     * show it later.
     */
    fun skyBrightnessMagArcsec2(altitudeDeg: Double, airglow: Double, lightPollution: Double, tz: Double): Double {
        val x = airmass(altitudeDeg)
        val glow = airglow * x * tz.pow(x - 1.0)
        val dome = lightPollution * (0.40 + 2.5 * exp(-max(altitudeDeg, 0.4) / 7.5) * 1.28)
        return SkyRenderModel.luminanceToMagArcsec2(max(glow + dome, 1e-12))
    }

    /** Smoothstep, for the altitude gates the shader uses. */
    fun smoothstep(edge0: Double, edge1: Double, x: Double): Double {
        if (edge1 == edge0) return if (x < edge0) 0.0 else 1.0
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0.0, 1.0)
        return t * t * (3.0 - 2.0 * t)
    }

    /** log10 helper kept for symmetry with the photometric code above. */
    fun magFromLuminance(luminance: Double): Double = -2.5 * log10(max(luminance, 1e-12) / SkyRenderModel.MAG_ARCSEC2_ZERO_POINT)

    /** The achromatic zenith extinction the sky model applies, magnitudes/airmass. */
    fun zenithExtinctionMagnitudes(extinctionV: Double): Double = -2.5 * log10(extinctionV.coerceIn(0.01, 1.0))
}
