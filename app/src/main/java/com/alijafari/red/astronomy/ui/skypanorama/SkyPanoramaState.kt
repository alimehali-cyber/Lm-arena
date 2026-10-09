package com.alijafari.red.astronomy.ui.skypanorama

/**
 * Sky-state inputs for the panorama camera, derived from values the Home hero already computes:
 * the Local Apparent Sidereal Time (`TimeEngine.getLAST`) at the Time Machine / simulated instant,
 * and the observer's latitude from `UserLocation`.
 *
 * Equality is based on the quantized LST and latitude only. The renderer redraws when, and only
 * when, this value changes, so an unchanged sky never triggers a GL frame.
 */
data class SkyPanoramaState(
    val lstDeg: Double,
    val latitudeDeg: Double,
    /** Quantized Sun altitude of the hero, degrees. Drives the daytime palette and the night blend. */
    val sunAltDeg: Double = -90.0,
    /** Quantized moon-glow intensity from the existing lighting model, 0..1. */
    val moonGlow: Float = 0f
) {
    /** Camera basis in equatorial coordinates. Derived, so it does not affect equality. */
    val basis: SkyPanoramaMath.ViewBasis = SkyPanoramaMath.zenithBasis(lstDeg, latitudeDeg)

    /** Daytime palette and night blend weight. Derived, so it does not affect equality. */
    val sky: SkyPanoramaSkyModel.Sky = SkyPanoramaSkyModel.skyFor(sunAltDeg, moonGlow)

    companion object {
        /** Builds a quantized state from the hero's existing sky values. */
        fun fromSkyState(
            lastDeg: Double,
            latitudeDeg: Double,
            sunAltDeg: Double,
            moonGlowIntensity: Float
        ): SkyPanoramaState = SkyPanoramaState(
            lstDeg = SkyPanoramaMath.quantizeLst(lastDeg),
            latitudeDeg = latitudeDeg,
            sunAltDeg = SkyPanoramaSkyModel.quantizeSunAlt(sunAltDeg),
            moonGlow = SkyPanoramaSkyModel.quantizeMoonGlow(moonGlowIntensity)
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
