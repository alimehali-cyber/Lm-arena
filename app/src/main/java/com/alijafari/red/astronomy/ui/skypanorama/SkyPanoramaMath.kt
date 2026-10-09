package com.alijafari.red.astronomy.ui.skypanorama

import com.alijafari.red.astronomy.ui.rendering.HeroSkyProjection
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure (Android-free) orientation, screen-geometry and texture-coordinate math for the sky panorama.
 *
 * Conventions (must stay in sync with [SkyPanoramaShaders.FRAGMENT_SHADER]):
 *  - Directions are unit vectors in the ICRF/J2000 equatorial frame: +x toward RA 0h / Dec 0,
 *    +y toward RA 6h / Dec 0, +z toward the north celestial pole. The hero's catalogue stars and
 *    galactic-plane line pass J2000 RA/Dec straight to CoordinateEngine with the same apparent
 *    sidereal time (no precession step), so the panorama uses that frame to match them. The Sun and
 *    planets come from their own engines, whose frame has not been verified here.
 *  - The NASA "celestial" star map is centred on RA 0h, RA increases to the LEFT, and north
 *    (Dec +90) is the TOP row. Texture coordinates are therefore:
 *        u = fract(0.5 - RA / 2pi)     (u = 0.5 at RA 0h, u decreases as RA increases)
 *        v = 0.5 - Dec / pi            (v = 0 at the north pole, the first image row)
 *    The first row of the decoded bitmap is uploaded as t = 0, so no vertical flip is applied.
 *    This was checked against the JPEG itself: the Galactic centre is bright at this mapping and
 *    dim under the mirrored or flipped alternatives.
 *  - Screen geometry is NOT a perspective camera. It is the inverse of [HeroSkyProjection], so the
 *    panorama and the hero's own stars, planets and galactic-plane line share one screen layout:
 *    horizontal = azimuth across the full width (centred on south in the northern hemisphere,
 *    north in the southern), vertical = altitude, linear from the top margin (zenith) down past
 *    the horizon line. Date and time enter only through the local sidereal time, and longitude only
 *    through that same LST. Latitude enters through the observer frame.
 */
object SkyPanoramaMath {

    /**
     * Observer's horizon frame expressed in equatorial (ICRF/J2000) coordinates.
     * `east`, `north` and `zenith` form a right-handed orthonormal set: east x north = zenith.
     */
    class HorizonBasis(
        val east: DoubleArray,
        val north: DoubleArray,
        val zenith: DoubleArray
    )

    /** Quantization step for local sidereal time, in degrees (0.05 deg is about 12 s of time). */
    const val LST_QUANTUM_DEG = 0.05

    /** Equatorial unit vector for right ascension and declination in degrees. */
    fun directionFromRaDec(raDeg: Double, decDeg: Double): DoubleArray {
        val ra = Math.toRadians(raDeg)
        val dec = Math.toRadians(decDeg)
        val cd = cos(dec)
        return doubleArrayOf(cd * cos(ra), cd * sin(ra), sin(dec))
    }

    /**
     * Horizon frame for a local sidereal time (degrees) and latitude (degrees).
     *  - zenith = (RA = LST, Dec = latitude)
     *  - north  = d(zenith)/d(latitude): the tangent toward the north celestial pole
     *  - east   = d(zenith)/d(LST): the direction of increasing RA on the horizon, i.e. the
     *             direction of the eastern horizon
     * All three are continuous in latitude, including at the poles, so no clamping is needed.
     */
    fun horizonBasis(lstDeg: Double, latitudeDeg: Double): HorizonBasis {
        val lst = Math.toRadians(lstDeg)
        val lat = Math.toRadians(latitudeDeg)
        val zenith = doubleArrayOf(cos(lat) * cos(lst), cos(lat) * sin(lst), sin(lat))
        val north = doubleArrayOf(-sin(lat) * cos(lst), -sin(lat) * sin(lst), cos(lat))
        val east = doubleArrayOf(-sin(lst), cos(lst), 0.0)
        return HorizonBasis(east = east, north = north, zenith = zenith)
    }

    /** Screen y of the horizon, in pixels from the top, exactly as [HeroSkyProjection] computes it. */
    fun horizonYPx(heightPx: Double): Double = heightPx * HeroSkyProjection.HORIZON_FRACTION

    /** Pixels per degree of altitude, exactly as [HeroSkyProjection] computes its vertical scale. */
    fun pixelsPerDegree(heightPx: Double): Double =
        (horizonYPx(heightPx) - HeroSkyProjection.TOP_MARGIN_PX) / 90.0

    /**
     * Azimuth at the centre of the screen: 180 deg (south) in the northern hemisphere, 0 deg (north)
     * in the southern. Equivalent to [HeroSkyProjection.project]:
     * relAz = signed(az - offset), x = (0.5 + relAz / 360) W.
     */
    fun azimuthOffsetDeg(latitudeDeg: Double): Double = if (latitudeDeg >= 0.0) 180.0 else 0.0

    /**
     * Horizontal coordinates (azimuth in [0, 360) from north through east, altitude in degrees) of a
     * screen pixel. This is the exact inverse of [HeroSkyProjection.project]. Pixel coordinates use
     * the top-left origin, as in Compose and the hero.
     */
    fun azAltForPixel(
        xPx: Double,
        yPx: Double,
        widthPx: Double,
        heightPx: Double,
        latitudeDeg: Double
    ): Pair<Double, Double> {
        val relAz = (xPx / widthPx - 0.5) * 360.0
        val azDeg = wrap360(relAz + azimuthOffsetDeg(latitudeDeg))
        val altDeg = (horizonYPx(heightPx) - yPx) / pixelsPerDegree(heightPx)
        return Pair(azDeg, altDeg)
    }

    /**
     * Equatorial unit vector seen at a screen pixel:
     *   d = sin(az) cos(alt) East + cos(az) cos(alt) North + sin(alt) Zenith.
     * This is what the fragment shader computes per pixel.
     */
    fun pixelToDirection(
        xPx: Double,
        yPx: Double,
        widthPx: Double,
        heightPx: Double,
        latitudeDeg: Double,
        basis: HorizonBasis
    ): DoubleArray {
        val (azDeg, altDeg) = azAltForPixel(xPx, yPx, widthPx, heightPx, latitudeDeg)
        val az = Math.toRadians(azDeg)
        val alt = Math.toRadians(altDeg)
        val ca = cos(alt)
        val e = basis.east
        val n = basis.north
        val z = basis.zenith
        return normalize(
            doubleArrayOf(
                sin(az) * ca * e[0] + cos(az) * ca * n[0] + sin(alt) * z[0],
                sin(az) * ca * e[1] + cos(az) * ca * n[1] + sin(alt) * z[1],
                sin(az) * ca * e[2] + cos(az) * ca * n[2] + sin(alt) * z[2]
            )
        )
    }

    /**
     * Screen position (x, y) in pixels where an equatorial direction appears in the panorama. This is
     * the forward mapping, in the same geometry as [HeroSkyProjection.project] (without refraction).
     */
    fun pixelForDirection(
        direction: DoubleArray,
        widthPx: Double,
        heightPx: Double,
        latitudeDeg: Double,
        basis: HorizonBasis
    ): DoubleArray {
        val d = direction
        val altDeg = Math.toDegrees(asin(dot(d, basis.zenith).coerceIn(-1.0, 1.0)))
        val azDeg = wrap360(Math.toDegrees(atan2(dot(d, basis.east), dot(d, basis.north))))
        val relAz = HeroSkyProjection.normalizeSignedAngle(azDeg - azimuthOffsetDeg(latitudeDeg))
        val x = (0.5 + relAz / 360.0) * widthPx
        val y = horizonYPx(heightPx) - altDeg * pixelsPerDegree(heightPx)
        return doubleArrayOf(x, y)
    }

    /**
     * Texture coordinates (u, v) for an equatorial direction. This mirrors the fragment shader
     * exactly, so unit tests can check the shader's mapping on the CPU.
     */
    fun uvForDirection(x: Double, y: Double, z: Double): DoubleArray {
        val ra = atan2(y, x)                       // -pi..pi, eastward positive
        val dec = asin(z.coerceIn(-1.0, 1.0))
        val u = fract(0.5 - ra / (2.0 * PI))
        val v = 0.5 - dec / PI
        return doubleArrayOf(u, v)
    }

    /**
     * Mip level for a screen-uniform LOD. The panorama is sampled with an explicit LOD instead of
     * implicit derivatives, which avoids a seam where the RA branch cut at +/-pi would otherwise
     * produce a discontinuous derivative.
     *
     * The full 360 deg of azimuth spans the width and the 180 deg of altitude spans the height of
     * the scaled hero projection, so the texel footprint per pixel is the larger of the two axes.
     */
    fun texelLod(textureWidth: Int, textureHeight: Int, widthPx: Int, heightPx: Int): Float {
        if (textureWidth <= 0 || textureHeight <= 0 || widthPx <= 0 || heightPx <= 0) return 0f
        val pxPerDeg = pixelsPerDegree(heightPx.toDouble())
        if (pxPerDeg <= 0.0) return 0f
        val texelsPerPixelX = textureWidth.toDouble() / widthPx
        val texelsPerPixelY = textureHeight / (180.0 * pxPerDeg)
        return max(0.0, log2(max(texelsPerPixelX, texelsPerPixelY))).toFloat()
    }

    /** Wraps a sidereal time into [0, 360) and snaps it to [LST_QUANTUM_DEG]. */
    fun quantizeLst(lstDeg: Double): Double {
        val wrapped = ((lstDeg % 360.0) + 360.0) % 360.0
        val snapped = Math.round(wrapped / LST_QUANTUM_DEG) * LST_QUANTUM_DEG
        return if (snapped >= 360.0) snapped - 360.0 else snapped
    }

    fun wrap360(deg: Double): Double = ((deg % 360.0) + 360.0) % 360.0

    fun fract(x: Double): Double = x - Math.floor(x)

    fun dot(a: DoubleArray, b: DoubleArray): Double = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]

    fun normalize(v: DoubleArray): DoubleArray {
        val len = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        require(len > 1e-12) { "Cannot normalize a zero-length vector" }
        return doubleArrayOf(v[0] / len, v[1] / len, v[2] / len)
    }
}
