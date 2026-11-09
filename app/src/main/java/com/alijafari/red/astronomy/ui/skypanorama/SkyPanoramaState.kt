package com.alijafari.red.astronomy.ui.skypanorama

import com.alijafari.red.astronomy.ui.rendering.SkySceneFrame

/**
 * Camera framing for the panorama. The texture and the shader are identical for every framing; only the view
 * direction and the vertical field of view differ.
 *
 *  - [HOME_ZENITH] is the Home hero's camera: it looks at the zenith, with the Home hero's 60° field of view.
 *    Its output must not change.
 *  - [APP_BACKDROP] is the full-screen Live Sky backdrop's camera. It looks [BACKDROP_CENTER_ALT_DEG] above the
 *    equator-facing horizon (south in the north, north in the south, as Home does) with a wide field of view,
 *    so the Sun, Moon, planets and stars near the horizon fall inside the picture, and the bottom of the screen
 *    is still photographic sky with no landscape.
 */
/** Home hero vertical field of view, degrees. */
private const val HOME_FOV_Y_DEG: Float = 60f

/** Backdrop vertical field of view, degrees. At 40° altitude this spans about -12° to +92° on the centre column. */
private const val BACKDROP_FOV_Y_DEG: Float = 104f

/** Altitude of the backdrop camera's view centre, degrees. */
private const val BACKDROP_CENTER_ALT_DEG: Double = 40.0

enum class SkyPanoramaFraming(
    val fovYDeg: Float,
    /** Altitude of the point the camera looks at, degrees. The night sky is exposed for this view. */
    val centreAltitudeDeg: Double
) {
    HOME_ZENITH(fovYDeg = HOME_FOV_Y_DEG, centreAltitudeDeg = 90.0),
    APP_BACKDROP(fovYDeg = BACKDROP_FOV_Y_DEG, centreAltitudeDeg = BACKDROP_CENTER_ALT_DEG);

    /** Camera basis in equatorial coordinates for this framing at a local sidereal time and latitude. */
    fun basis(lstDeg: Double, latitudeDeg: Double): SkyPanoramaMath.ViewBasis = when (this) {
        HOME_ZENITH -> SkyPanoramaMath.zenithBasis(lstDeg, latitudeDeg)
        APP_BACKDROP -> SkyPanoramaMath.basisForForwardAndUp(
            forward = SkyPanoramaMath.directionFromHorizontal(
                azimuthDeg = equatorFacingAzimuthDeg(latitudeDeg),
                altitudeDeg = BACKDROP_CENTER_ALT_DEG,
                latitudeDeg = latitudeDeg,
                lstDeg = lstDeg
            ),
            // Keep the zenith at the top of the screen, so the horizon-level view is upright in both hemispheres.
            upHint = SkyPanoramaMath.directionFromHorizontal(
                azimuthDeg = 0.0,
                altitudeDeg = 90.0,
                latitudeDeg = latitudeDeg,
                lstDeg = lstDeg
            )
        )
    }

    companion object {
        /** Same rule as [com.alijafari.red.astronomy.ui.rendering.HeroSkyProjection]: south in the north, north in the south. */
        fun equatorFacingAzimuthDeg(latitudeDeg: Double): Double = if (latitudeDeg >= 0.0) 180.0 else 0.0
    }
}

/**
 * Sky-state inputs for the panorama camera, derived from values the Home hero already computes:
 * the Local Apparent Sidereal Time (`TimeEngine.getLAST`) at the Time Machine / simulated instant,
 * and the observer's latitude from `UserLocation`.
 *
 * Equality is based on the quantized LST, latitude, sky values and framing. The renderer redraws when, and only
 * when, this value changes, so an unchanged sky never triggers a GL frame.
 */
data class SkyPanoramaState(
    val lstDeg: Double,
    val latitudeDeg: Double,
    /** Quantized Sun altitude of the hero, degrees. Drives the daytime palette and the night blend. */
    val sunAltDeg: Double = -90.0,
    /** Quantized moon-glow intensity from the existing lighting model, 0..1. */
    val moonGlow: Float = 0f,
    /** Camera framing. Home keeps [SkyPanoramaFraming.HOME_ZENITH]; the Live Sky backdrop uses its own. */
    val framing: SkyPanoramaFraming = SkyPanoramaFraming.HOME_ZENITH,
    /** Quantised apparent altitude of the Moon, degrees. Drives the moonlight term of the night sky. */
    val moonAltDeg: Double = -90.0,
    /** Quantised illuminated fraction of the lunar disc, 0..1. Drives the moonlight term. */
    val moonIllum: Float = 0f,
    /** Apparent azimuth of the Sun, degrees from north through east. */
    val sunAzDeg: Double = 0.0,
    /** Apparent azimuth of the Moon, degrees from north through east. */
    val moonAzDeg: Double = 0.0,
    /** The observer's Bortle class, 1..9. Drives the light-pollution term of the night sky. */
    val bortle: Int = 3
) {
    /** Camera basis in equatorial coordinates. Derived from the framing, so it does not affect equality. */
    val basis: SkyPanoramaMath.ViewBasis = framing.basis(lstDeg, latitudeDeg)

    /** The observer's zenith, north point and east point in equatorial coordinates. */
    val zenithDir: DoubleArray = SkyPanoramaMath.directionFromHorizontal(0.0, 90.0, latitudeDeg, lstDeg)
    val northDir: DoubleArray = SkyPanoramaMath.directionFromHorizontal(0.0, 0.0, latitudeDeg, lstDeg)
    val eastDir: DoubleArray = SkyPanoramaMath.directionFromHorizontal(90.0, 0.0, latitudeDeg, lstDeg)

    /** Daytime palette and night blend weight. Derived, so it does not affect equality. */
    val sky: SkyPanoramaSkyModel.Sky = SkyPanoramaSkyModel.skyFor(sunAltDeg, moonGlow)

    /**
     * Absolute photometry of this night: airglow, light pollution, zodiacal light, moonlight,
     * twilight and the exposure they share. Derived, so it does not affect equality; it changes
     * exactly when the quantised inputs above change.
     */
    val night: SkyNightModel.Sky = SkyNightModel.skyFor(
        sunAltDeg = sunAltDeg,
        moonAltDeg = moonAltDeg,
        moonIlluminationPercent = moonIllum * 100.0,
        bortle = bortle,
        cameraAltDeg = framing.centreAltitudeDeg,
        extinctionV = SkyPanoramaSkyModel.EXTINCTION_V
    )

    companion object {
        /** Builds a quantized state from the hero's existing sky values. */
        fun fromSkyState(
            lastDeg: Double,
            latitudeDeg: Double,
            sunAltDeg: Double,
            moonGlowIntensity: Float,
            framing: SkyPanoramaFraming = SkyPanoramaFraming.HOME_ZENITH,
            moonAltDeg: Double = -90.0,
            moonIlluminationPercent: Double = 0.0,
            bortle: Int = DEFAULT_BORTLE,
            sunAzimuthDeg: Double = 0.0,
            moonAzimuthDeg: Double = 0.0
        ): SkyPanoramaState = SkyPanoramaState(
            lstDeg = SkyPanoramaMath.quantizeLst(lastDeg),
            latitudeDeg = latitudeDeg,
            sunAltDeg = SkyPanoramaSkyModel.quantizeSunAlt(sunAltDeg),
            moonGlow = SkyPanoramaSkyModel.quantizeMoonGlow(moonGlowIntensity),
            framing = framing,
            moonAltDeg = SkyPanoramaSkyModel.quantizeMoonAlt(moonAltDeg),
            moonIllum = SkyPanoramaSkyModel.quantizeMoonIllum(moonIlluminationPercent),
            sunAzDeg = sunAzDeg,
            moonAzDeg = moonAzDeg,
            bortle = bortle.coerceIn(1, 9)
        )

        /** Bortle class used when a caller does not supply one (the app's own default). */
        const val DEFAULT_BORTLE: Int = 3

        /**
         * The panorama inputs for one computed sky frame. The Home hero and the Live Sky backdrop both call this, so
         * the same frame always yields the same panorama state for the same framing.
         */
        fun fromSceneFrame(
            frame: SkySceneFrame,
            framing: SkyPanoramaFraming = SkyPanoramaFraming.HOME_ZENITH
        ): SkyPanoramaState = fromSkyState(
            lastDeg = frame.lastDeg,
            latitudeDeg = frame.latitudeDeg,
            sunAltDeg = frame.sunHoriz.altitudeDeg,
            moonGlowIntensity = frame.lightingState.moonGlowIntensity,
            framing = framing,
            moonAltDeg = frame.moonData.altitudeDeg,
            moonIlluminationPercent = frame.moonData.illuminationPercent,
            bortle = frame.bortle,
            sunAzimuthDeg = frame.sunHoriz.azimuthDeg,
            moonAzimuthDeg = frame.moonData.azimuthDeg
        )
    }
}

/** Lifecycle of the panorama layer as seen by the Compose hero. */
enum class SkyPanoramaStatus {
    /** Surface or texture not yet presented. The legacy Sky Canvas stays visible underneath. */
    LOADING,

    /** At least one panorama frame has been presented. The legacy sky background is skipped. */
    READY,

    /** Texture or GL setup failed. The legacy Sky Canvas stays visible. */
    UNAVAILABLE
}
