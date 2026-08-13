package com.alijafari.red.astronomy.astro_engine

object CoordinateEngine {
    data class Equatorial(val raDeg: Double, val decDeg: Double)
    data class Horizontal(val azimuthDeg: Double, val altitudeDeg: Double) {
        val azimuthCompassNameEn: String
            get() {
                val a = (azimuthDeg % 360.0 + 360.0) % 360.0
                return when {
                    a < 22.5 || a >= 337.5 -> "North"
                    a < 67.5 -> "Northeast"
                    a < 112.5 -> "East"
                    a < 157.5 -> "Southeast"
                    a < 202.5 -> "South"
                    a < 247.5 -> "Southwest"
                    a < 292.5 -> "West"
                    else -> "Northwest"
                }
            }

        val azimuthCompassNameFa: String
            get() {
                val a = (azimuthDeg % 360.0 + 360.0) % 360.0
                return when {
                    a < 22.5 || a >= 337.5 -> "شمال"
                    a < 67.5 -> "شمال‌شرق"
                    a < 112.5 -> "شرق"
                    a < 157.5 -> "جنوب‌شرق"
                    a < 202.5 -> "جنوب"
                    a < 247.5 -> "جنوب‌غرب"
                    a < 292.5 -> "غرب"
                    else -> "شمال‌غرب"
                }
            }
    }
    data class Galactic(val lDeg: Double, val bDeg: Double)
    data class Nutation(
        val deltaPsiDeg: Double,
        val deltaEpsDeg: Double,
        val meanObliquityDeg: Double,
        val trueObliquityDeg: Double
    )
    data class RiseSetTransit(
        val riseTimeStr: String,
        val transitTimeStr: String,
        val setTimeStr: String,
        val isCircumpolar: Boolean = false,
        val neverRises: Boolean = false
    )

    fun calculateNutation(jd: Double) = CoordinateEngineLegacy.calculateNutation(jd)
    fun equatorialToHorizontal(equatorial: Equatorial, lastDeg: Double, latitudeDeg: Double, observerElevationM: Double = 0.0): Horizontal {
        val legacyHoriz = CoordinateEngineLegacy.equatorialToHorizontal(
            CoordinateEngineLegacy.Equatorial(equatorial.raDeg, equatorial.decDeg),
            lastDeg, latitudeDeg, observerElevationM
        )
        return Horizontal(legacyHoriz.azimuthDeg, legacyHoriz.altitudeDeg)
    }
    fun horizontalToEquatorial(horizontal: Horizontal, lastDeg: Double, latitudeDeg: Double): Equatorial {
        val legacyEq = CoordinateEngineLegacy.horizontalToEquatorial(
            CoordinateEngineLegacy.Horizontal(horizontal.azimuthDeg, horizontal.altitudeDeg),
            lastDeg, latitudeDeg
        )
        return Equatorial(legacyEq.raDeg, legacyEq.decDeg)
    }
    fun geocentricToTopocentric(geocentric: Equatorial, geocentricDistanceKm: Double, lastDeg: Double, latitudeDeg: Double, elevationM: Double = 0.0): Equatorial {
        val legacyEq = CoordinateEngineLegacy.geocentricToTopocentric(
            CoordinateEngineLegacy.Equatorial(geocentric.raDeg, geocentric.decDeg),
            geocentricDistanceKm, lastDeg, latitudeDeg, elevationM
        )
        return Equatorial(legacyEq.raDeg, legacyEq.decDeg)
    }
    fun equatorialToGalactic(eq: Equatorial): Galactic {
        val legacyGal = CoordinateEngineLegacy.equatorialToGalactic(CoordinateEngineLegacy.Equatorial(eq.raDeg, eq.decDeg))
        return Galactic(legacyGal.lDeg, legacyGal.bDeg)
    }
    fun galacticToEquatorial(gal: Galactic): Equatorial {
        val legacyEq = CoordinateEngineLegacy.galacticToEquatorial(CoordinateEngineLegacy.Galactic(gal.lDeg, gal.bDeg))
        return Equatorial(legacyEq.raDeg, legacyEq.decDeg)
    }
    fun formatRA(raDeg: Double) = CoordinateEngineLegacy.formatRA(raDeg)
    fun formatDec(decDeg: Double) = CoordinateEngineLegacy.formatDec(decDeg)
    fun calculateRiseSetTransit(raDeg: Double, decDeg: Double, latDeg: Double, lonDeg: Double, jd: Double = TimeEngine.getJulianDate(), isFa: Boolean = true): RiseSetTransit {
        val legacyRst = CoordinateEngineLegacy.calculateRiseSetTransit(raDeg, decDeg, latDeg, lonDeg, jd, isFa)
        return RiseSetTransit(legacyRst.riseTimeStr, legacyRst.transitTimeStr, legacyRst.setTimeStr, legacyRst.isCircumpolar, legacyRst.neverRises)
    }
    fun calculateAngularSeparationDeg(ra1: Double, dec1: Double, ra2: Double, dec2: Double) = CoordinateEngineLegacy.calculateAngularSeparationDeg(ra1, dec1, ra2, dec2)
}
