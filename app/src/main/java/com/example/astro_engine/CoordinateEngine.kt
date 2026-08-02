package com.example.astro_engine

import kotlin.math.*

object CoordinateEngine {

    data class Equatorial(val raDeg: Double, val decDeg: Double)
    data class Horizontal(val azimuthDeg: Double, val altitudeDeg: Double)

    /**
     * Converts Equatorial coordinates (RA, Dec in degrees) to Horizontal coordinates (Azimuth, Altitude in degrees).
     * @param lastDeg Local Apparent Sidereal Time in degrees.
     * @param latitudeDeg Observer latitude in degrees (+North).
     */
    fun equatorialToHorizontal(
        equatorial: Equatorial,
        lastDeg: Double,
        latitudeDeg: Double
    ): Horizontal {
        // Hour Angle (HA) in degrees = LAST - RA
        var haDeg = lastDeg - equatorial.raDeg
        haDeg %= 360.0
        if (haDeg < 0) haDeg += 360.0

        val haRad = Math.toRadians(haDeg)
        val decRad = Math.toRadians(equatorial.decDeg)
        val latRad = Math.toRadians(latitudeDeg)

        // Altitude
        val sinAlt = sin(decRad) * sin(latRad) + cos(decRad) * cos(latRad) * cos(haRad)
        val altRad = asin(sinAlt.coerceIn(-1.0, 1.0))
        var altDeg = Math.toDegrees(altRad)

        // Azimuth (measured from North = 0, East = 90)
        val cosAz = (sin(decRad) - sin(latRad) * sin(altRad)) / (cos(latRad) * cos(altRad)).coerceAtLeast(1e-6)
        val sinAz = -cos(decRad) * sin(haRad) / cos(altRad).coerceAtLeast(1e-6)

        var azRad = atan2(sinAz, cosAz)
        var azDeg = Math.toDegrees(azRad)
        if (azDeg < 0) azDeg += 360.0

        // Apply Bennett formula for atmospheric refraction if above horizon
        if (altDeg > -1.0) {
            val R = 1.02 / tan(Math.toRadians(altDeg + 10.3 / (altDeg + 5.11))) // Bennett formula in arcminutes
            altDeg += (R / 60.0)
        }

        return Horizontal(azimuthDeg = azDeg, altitudeDeg = altDeg)
    }

    /**
     * Converts Horizontal (Azimuth, Altitude) back to Equatorial (RA, Dec).
     */
    fun horizontalToEquatorial(
        horizontal: Horizontal,
        lastDeg: Double,
        latitudeDeg: Double
    ): Equatorial {
        val azRad = Math.toRadians(horizontal.azimuthDeg)
        val altRad = Math.toRadians(horizontal.altitudeDeg)
        val latRad = Math.toRadians(latitudeDeg)

        val sinDec = sin(altRad) * sin(latRad) + cos(altRad) * cos(latRad) * cos(azRad)
        val decRad = asin(sinDec.coerceIn(-1.0, 1.0))
        val decDeg = Math.toDegrees(decRad)

        val sinHA = -sin(azRad) * cos(altRad) / cos(decRad).coerceAtLeast(1e-6)
        val cosHA = (sin(altRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad)).coerceAtLeast(1e-6)

        val haRad = atan2(sinHA, cosHA)
        var haDeg = Math.toDegrees(haRad)
        if (haDeg < 0) haDeg += 360.0

        var raDeg = lastDeg - haDeg
        raDeg %= 360.0
        if (raDeg < 0) raDeg += 360.0

        return Equatorial(raDeg = raDeg, decDeg = decDeg)
    }

    /**
     * Formats RA in Hours:Minutes:Seconds.
     */
    fun formatRA(raDeg: Double): String {
        val raHours = raDeg / 15.0
        val h = floor(raHours).toInt()
        val mFull = (raHours - h) * 60.0
        val m = floor(mFull).toInt()
        val s = ((mFull - m) * 60.0).toInt()
        return String.format("%02dh %02dm %02ds", h, m, s)
    }

    /**
     * Formats Dec in Degrees:Minutes:Seconds.
     */
    fun formatDec(decDeg: Double): String {
        val sign = if (decDeg >= 0) "+" else "-"
        val absD = abs(decDeg)
        val d = floor(absD).toInt()
        val mFull = (absD - d) * 60.0
        val m = floor(mFull).toInt()
        val s = ((mFull - m) * 60.0).toInt()
        return String.format("%s%02d° %02d′ %02d″", sign, d, m, s)
    }
}
