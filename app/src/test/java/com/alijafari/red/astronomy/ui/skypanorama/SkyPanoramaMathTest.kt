package com.alijafari.red.astronomy.ui.skypanorama

import com.alijafari.red.astronomy.astro_engine.CoordinateEngine
import com.alijafari.red.astronomy.astro_engine.GalacticEngine
import com.alijafari.red.astronomy.ui.rendering.HeroSkyProjection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.acos

/**
 * Orientation, screen-geometry, wrap, pole, observer and time checks for the sky panorama.
 *
 * These are pure JVM tests. The panorama's screen geometry must agree with the hero's own
 * projection ([HeroSkyProjection]) and with the hero's own horizon transform
 * ([CoordinateEngine]), so the main correspondence checks compare against those engines directly
 * rather than against this file's own formulas.
 */
class SkyPanoramaMathTest {

    private val tol = 1e-9

    /** Canvas used by the hero, in pixels. Both a phone-like and a tall tablet-like size. */
    private val canvases = listOf(400.0 to 800.0, 1080.0 to 2340.0)

    private val observerLatitudes = listOf(-60.0, -33.87, 0.0, 30.0, 52.37, 80.0)
    private val lsts = listOf(0.0, 77.3, 200.0, 301.9)

    private fun uv(raDeg: Double, decDeg: Double): DoubleArray {
        val d = SkyPanoramaMath.directionFromRaDec(raDeg, decDeg)
        return SkyPanoramaMath.uvForDirection(d[0], d[1], d[2])
    }

    /** Wrap-aware distance on the horizontal (u) axis. */
    private fun wrapDistance(a: Double, b: Double): Double {
        val d = abs(a - b) % 1.0
        return minOf(d, 1.0 - d)
    }

    /** Angle between two unit vectors, in degrees. */
    private fun angleDeg(a: DoubleArray, b: DoubleArray): Double =
        Math.toDegrees(acos(SkyPanoramaMath.dot(a, b).coerceIn(-1.0, 1.0)))

    /** Screen position of a sky direction as the panorama draws it. */
    private fun panoramaPixel(dir: DoubleArray, w: Double, h: Double, lat: Double, lst: Double): DoubleArray {
        val basis = SkyPanoramaMath.horizonBasis(lst, lat)
        return SkyPanoramaMath.pixelForDirection(dir, w, h, lat, basis)
    }

    @Test
    fun horizonFrameAtZeroLstOnEquatorIsCanonical() {
        val b = SkyPanoramaMath.horizonBasis(0.0, 0.0)
        assertVec(doubleArrayOf(1.0, 0.0, 0.0), b.zenith)   // zenith is RA 0h, Dec 0
        assertVec(doubleArrayOf(0.0, 1.0, 0.0), b.east)     // east is the direction of increasing RA
        assertVec(doubleArrayOf(0.0, 0.0, 1.0), b.north)    // north is toward the celestial pole
    }

    @Test
    fun horizonFrameIsOrthonormalAndRightHandedForArbitraryObservers() {
        val samples = listOf(-60.0 to 0.0, 0.0 to 30.1141, 123.4 to -33.8688, 250.0 to 64.0, 359.9 to 89.0,
            12.0 to -90.0, 200.0 to 90.0)
        for ((lst, lat) in samples) {
            val b = SkyPanoramaMath.horizonBasis(lst, lat)
            assertEquals(1.0, SkyPanoramaMath.dot(b.east, b.east), tol)
            assertEquals(1.0, SkyPanoramaMath.dot(b.north, b.north), tol)
            assertEquals(1.0, SkyPanoramaMath.dot(b.zenith, b.zenith), tol)
            assertEquals(0.0, SkyPanoramaMath.dot(b.east, b.north), tol)
            assertEquals(0.0, SkyPanoramaMath.dot(b.east, b.zenith), tol)
            assertEquals(0.0, SkyPanoramaMath.dot(b.north, b.zenith), tol)
            // east x north must equal zenith (right-handed East-North-Up frame).
            assertVec(b.zenith, cross(b.east, b.north))
        }
    }

    @Test
    fun zenithDeclinationEqualsLatitudeAndNorthTangentPointsToThePoleInTheNorth() {
        val lat = 30.0
        val b = SkyPanoramaMath.horizonBasis(123.0, lat)
        assertEquals(lat, Math.toDegrees(Math.asin(b.zenith[2])), 1e-9)
        assertTrue("north tangent should have a positive z component", b.north[2] > 0.0)
    }

    @Test
    fun polarObserverGivesAFiniteFrameWithThePoleAtZenith() {
        val b = SkyPanoramaMath.horizonBasis(0.0, 90.0)
        for (vec in listOf(b.east, b.north, b.zenith)) {
            assertTrue(vec.all { !it.isNaN() })
        }
        assertEquals(1.0, b.zenith[2], 1e-9)           // the celestial pole is overhead
        assertEquals(0.0, b.north[2], 1e-9)            // north is horizontal at the pole
    }

    @Test
    fun raDecReferencePointsMapToNasaCelestialTexture() {
        // Centre column is RA 0h, the equator is the middle row.
        assertUv(0.5, 0.5, uv(0.0, 0.0))
        // RA increases to the LEFT: 6h sits at u = 0.25, 18h at u = 0.75.
        assertUv(0.25, 0.5, uv(90.0, 0.0))
        assertUv(0.75, 0.5, uv(270.0, 0.0))
        // Declination +90 is the top row (v = 0), -90 is the bottom row (v = 1).
        assertEquals(0.0, uv(0.0, 90.0)[1], 1e-9)
        assertEquals(1.0, uv(0.0, -90.0)[1], 1e-9)
    }

    @Test
    fun galacticCentreLandsInTheLowerRightQuadrantOfTheCelestialMap() {
        // Sgr A* is near RA 266.405 deg, Dec -28.936 deg. With RA increasing to the left, it is
        // 94 deg to the right of the 0h centre column and below the equator.
        val gc = uv(266.405, -28.936)
        assertEquals(0.760, gc[0], 0.001)
        assertEquals(0.661, gc[1], 0.001)
    }

    @Test
    fun horizontalWrapIsContinuousAcrossTheRaBranchCut() {
        // RA +179.9 and -179.9 are neighbours on the sphere, and must be neighbours on the texture.
        val east = uv(179.9, 10.0)[0]
        val west = uv(-179.9, 10.0)[0]
        assertTrue("seam distance ${wrapDistance(east, west)}", wrapDistance(east, west) < 0.002)
        // Both sides must remain inside the texture's [0, 1) range after fract().
        assertTrue(east >= 0.0 && east < 1.0)
        assertTrue(west >= 0.0 && west < 1.0)
    }

    @Test
    fun screenEdgesAreTheSameSkyDirectionSoTheAzimuthWrapIsSeamless() {
        for ((w, h) in canvases) {
            for (lat in observerLatitudes) {
                val basis = SkyPanoramaMath.horizonBasis(77.3, lat)
                val left = SkyPanoramaMath.pixelToDirection(0.0, h * 0.5, w, h, lat, basis)
                val right = SkyPanoramaMath.pixelToDirection(w, h * 0.5, w, h, lat, basis)
                assertTrue("edge directions differ for lat $lat", angleDeg(left, right) < 1e-6)
                // A one-pixel step across the seam must be as small as a one-pixel step anywhere else.
                // Near the poles RA is stretched by 1/cos(Dec), so compare against an interior step
                // at the same row instead of a fixed texel count.
                val seamStep = angleDeg(
                    SkyPanoramaMath.pixelToDirection(w - 0.5, h * 0.5, w, h, lat, basis),
                    SkyPanoramaMath.pixelToDirection(0.5, h * 0.5, w, h, lat, basis)
                )
                val interiorStep = angleDeg(
                    SkyPanoramaMath.pixelToDirection(0.5, h * 0.5, w, h, lat, basis),
                    SkyPanoramaMath.pixelToDirection(1.5, h * 0.5, w, h, lat, basis)
                )
                assertTrue("seam step $seamStep deg vs interior $interiorStep deg for lat $lat",
                    abs(seamStep - interiorStep) < 0.02 * interiorStep + 1e-9)
            }
        }
    }

    @Test
    fun rightSideOfScreenIsWestAndSamplesMoveRightInTheTexture() {
        // Northern observer, LST 0: the screen centre faces south. Right of centre is west (RA < 0).
        val w = 400.0
        val h = 800.0
        val basis = SkyPanoramaMath.horizonBasis(0.0, 0.0)
        val y = SkyPanoramaMath.horizonYPx(h) - 30.0 * SkyPanoramaMath.pixelsPerDegree(h)  // altitude 30
        val right = SkyPanoramaMath.pixelToDirection(0.75 * w, y, w, h, 0.0, basis)
        val left = SkyPanoramaMath.pixelToDirection(0.25 * w, y, w, h, 0.0, basis)
        val uvRight = SkyPanoramaMath.uvForDirection(right[0], right[1], right[2])
        val uvLeft = SkyPanoramaMath.uvForDirection(left[0], left[1], left[2])
        // Right of the screen means decreasing RA, which is larger u in this map.
        assertTrue("right pixel should sample larger u", uvRight[0] > 0.5 && uvLeft[0] < 0.5)
    }

    @Test
    fun altitudeIncreasesUpTheScreen() {
        val w = 400.0
        val h = 800.0
        val lat = 0.0
        val basis = SkyPanoramaMath.horizonBasis(0.0, lat)
        val high = SkyPanoramaMath.pixelToDirection(w / 2, 100.0, w, h, lat, basis)
        val low = SkyPanoramaMath.pixelToDirection(w / 2, 500.0, w, h, lat, basis)
        // Higher on screen means higher in the sky, i.e. closer to the zenith.
        assertTrue(SkyPanoramaMath.dot(high, basis.zenith) > SkyPanoramaMath.dot(low, basis.zenith))
    }

    @Test
    fun topOfHeroSkyIsTheObserverZenithForEveryLatitude() {
        for ((w, h) in canvases) {
            for (lat in observerLatitudes) {
                val basis = SkyPanoramaMath.horizonBasis(123.4, lat)
                val top = SkyPanoramaMath.pixelToDirection(w / 2, HeroSkyProjection.TOP_MARGIN_PX.toDouble(), w, h, lat, basis)
                assertVec(basis.zenith, top)
                // The panorama's zenith is at Dec = latitude, matching the hero's sky.
                assertEquals(lat, Math.toDegrees(Math.asin(top[2])), 1e-6)
            }
        }
    }

    @Test
    fun pixelDirectionIsTheExactInverseOfTheHeroProjection() {
        // Round trip: hero pixel -> (az, alt) -> direction must equal the engine's own
        // horizontal-to-equatorial conversion of the same (az, alt). Exact, so no tolerance for refraction.
        for ((w, h) in canvases) {
            for (lat in observerLatitudes) {
                for (lst in lsts) {
                    val basis = SkyPanoramaMath.horizonBasis(lst, lat)
                    for (az in listOf(0.0, 45.0, 135.0, 180.0, 250.0, 359.0)) {
                        for (alt in listOf(-15.0, 0.0, 12.0, 45.0, 85.0)) {
                            val p = HeroSkyProjection.project(az, alt, w.toFloat(), h.toFloat(), lat)
                            val dir = SkyPanoramaMath.pixelToDirection(p.x.toDouble(), p.y.toDouble(), w, h, lat, basis)
                            val eq = CoordinateEngine.horizontalToEquatorial(
                                CoordinateEngine.Horizontal(az, alt), lst, lat
                            )
                            val expected = SkyPanoramaMath.directionFromRaDec(eq.raDeg, eq.decDeg)
                            assertTrue(
                                "az=$az alt=$alt lat=$lat lst=$lst canvas=${w}x$h: ${angleDeg(dir, expected)} deg off",
                                angleDeg(dir, expected) < 1e-4
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun starsAppearAtTheSamePixelAsTheHeroDrawsThem() {
        // The hero draws a star from CoordinateEngine.equatorialToHorizontal (which adds atmospheric
        // refraction) through HeroSkyProjection. The panorama must place the same direction at the
        // same pixel. Refraction is the only expected difference, at most ~0.1 deg above 10 deg.
        val stars = listOf(0.0 to 0.0, 88.79 to 7.41, 152.09 to 11.97, 266.40 to -28.94, 310.36 to 45.28)
        for ((w, h) in canvases) {
            for (lat in observerLatitudes) {
                for (lst in lsts) {
                    for ((ra, dec) in stars) {
                        val hz = CoordinateEngine.equatorialToHorizontal(
                            CoordinateEngine.Equatorial(ra, dec), lst, lat
                        )
                        if (hz.altitudeDeg < 10.0) continue
                        val hero = HeroSkyProjection.project(hz.azimuthDeg, hz.altitudeDeg, w.toFloat(), h.toFloat(), lat)
                        val pan = panoramaPixel(SkyPanoramaMath.directionFromRaDec(ra, dec), w, h, lat, lst)
                        val tolPx = 0.1 * SkyPanoramaMath.pixelsPerDegree(h) + 0.5
                        assertTrue(
                            "star ($ra, $dec) lat=$lat lst=$lst canvas=${w}x$h: hero=(${hero.x}, ${hero.y}) panorama=(${pan[0]}, ${pan[1]})",
                            abs(pan[0] - hero.x) <= tolPx && abs(pan[1] - hero.y) <= tolPx
                        )
                    }
                }
            }
        }
    }

    @Test
    fun milkyWayPlaneInThePanoramaCoincidesWithTheHeroGalacticLine() {
        // The hero draws the galactic equator from GalacticEngine (RA/Dec from galacticToEquatorial,
        // then the horizon transform). The panorama's Milky Way must lie on the same screen path.
        val w = 400.0
        val h = 800.0
        for (lat in listOf(52.37, -33.87)) {
            for (lst in listOf(65.15, 200.0, 310.5)) {
                val points = GalacticEngine.calculateGalacticPlanePointsWithLast(lst, lat, 0.0)
                for (p in points.filter { it.altitudeDeg > 10.0 }) {
                    val hero = HeroSkyProjection.project(p.azimuthDeg, p.altitudeDeg, w.toFloat(), h.toFloat(), lat)
                    val pan = panoramaPixel(SkyPanoramaMath.directionFromRaDec(p.raDeg, p.decDeg), w, h, lat, lst)
                    val tolPx = 0.1 * SkyPanoramaMath.pixelsPerDegree(h) + 0.5
                    assertTrue(
                        "galactic l=${p.galLongitudeDeg} lat=$lat lst=$lst: hero=(${hero.x}, ${hero.y}) panorama=(${pan[0]}, ${pan[1]})",
                        abs(pan[0] - hero.x) <= tolPx && abs(pan[1] - hero.y) <= tolPx
                    )
                }
            }
        }
    }

    @Test
    fun galacticCentreIsOnTheMeridianAtTheExpectedAltitudeAndColumn() {
        // Sgr A* (RA 266.405, Dec -28.936) transits the meridian when LST = RA. Its altitude is
        // 90 - |lat - Dec| and it sits in the centre column (south) for a northern observer.
        val w = 400.0
        val h = 800.0
        val lat = 51.5
        val lst = 266.405
        val gc = GalacticEngine.calculateGalacticPlanePointsWithLast(lst, lat, 0.0)
            .firstOrNull { it.galLongitudeDeg == 0.0 }
        assertTrue("l=0 point must exist", gc != null)
        // The galactic-plane point at l = 0 is the Galactic centre (b = 0).
        assertEquals(266.405, gc!!.raDeg, 0.01)
        assertEquals(-28.936, gc.decDeg, 0.01)

        val expectedAlt = 90.0 - (lat + 28.936)
        val pan = panoramaPixel(SkyPanoramaMath.directionFromRaDec(266.405, -28.936), w, h, lat, lst)
        assertEquals(w / 2, pan[0], 0.05)
        assertEquals(SkyPanoramaMath.horizonYPx(h) - expectedAlt * SkyPanoramaMath.pixelsPerDegree(h), pan[1], 0.05)
    }

    @Test
    fun siderealRotationMovesStarsWestwardAcrossTheScreen() {
        // RA 100, Dec 20 is in the east half of the sky at LST 40 and climbs toward the south.
        // One sidereal hour later (LST + 15) it must be further right on screen (west).
        val w = 400.0
        val h = 800.0
        val lat = 52.0
        val ra = 100.0
        val dec = 20.0
        val before = panoramaPixel(SkyPanoramaMath.directionFromRaDec(ra, dec), w, h, lat, 40.0)
        val after = panoramaPixel(SkyPanoramaMath.directionFromRaDec(ra, dec), w, h, lat, 55.0)
        assertTrue("star should move right (west): before=${before[0]} after=${after[0]}", after[0] > before[0])
    }

    @Test
    fun fixedScreenPixelSeesRightAscensionIncreaseWithSiderealTime() {
        // The sky turns westward, so the direction seen at a fixed pixel moves EASTWARD in RA:
        // one hour of LST later the same pixel shows RA larger by 15 deg.
        val w = 400.0
        val h = 800.0
        val lat = 40.0
        val px = 0.3 * w
        val py = 0.4 * h
        for (lst in listOf(0.0, 123.0, 300.0)) {
            val d0 = SkyPanoramaMath.pixelToDirection(px, py, w, h, lat, SkyPanoramaMath.horizonBasis(lst, lat))
            val d1 = SkyPanoramaMath.pixelToDirection(px, py, w, h, lat, SkyPanoramaMath.horizonBasis(lst + 15.0, lat))
            val ra0 = Math.toDegrees(Math.atan2(d0[1], d0[0])).let { SkyPanoramaMath.wrap360(it) }
            val ra1 = Math.toDegrees(Math.atan2(d1[1], d1[0])).let { SkyPanoramaMath.wrap360(it) }
            val delta = SkyPanoramaMath.wrap360(ra1 - ra0)
            assertEquals("RA shift at LST $lst", 15.0, delta, 1e-6)
        }
    }

    @Test
    fun observerLatitudeSetsTheZenithDeclinationAndTheNorthPoleAltitude() {
        val w = 400.0
        val h = 800.0
        for (lat in observerLatitudes) {
            // The north celestial pole sits at altitude = latitude, at the screen edge (north).
            val ncp = panoramaPixel(SkyPanoramaMath.directionFromRaDec(0.0, 90.0), w, h, lat, 0.0)
            assertEquals(
                SkyPanoramaMath.horizonYPx(h) - lat * SkyPanoramaMath.pixelsPerDegree(h),
                ncp[1],
                0.05
            )
        }
    }

    @Test
    fun northCelestialPoleIsOnTheScreenEdgeAndTheSouthPoleBelowTheHorizon() {
        // Declination edges: Dec +90 is the top row of the map (v = 0) and sits on the north seam.
        val w = 400.0
        val h = 800.0
        val lat = 40.0
        val ncp = panoramaPixel(SkyPanoramaMath.directionFromRaDec(0.0, 90.0), w, h, lat, 0.0)
        assertTrue("north pole x=${ncp[0]} should be on the seam", ncp[0] < 0.5 || ncp[0] > w - 0.5)
        assertEquals(0.0, uv(0.0, 90.0)[1], 1e-9)
        // Dec -90 is the bottom row and sits below the horizon for a northern observer.
        val scp = panoramaPixel(SkyPanoramaMath.directionFromRaDec(0.0, -90.0), w, h, lat, 0.0)
        assertTrue("south pole should be below the horizon", scp[1] > SkyPanoramaMath.horizonYPx(h))
        assertEquals(1.0, uv(0.0, -90.0)[1], 1e-9)
    }

    @Test
    fun texelLodIsZeroForNearOneToOneSamplingAndGrowsForSmallerViewports() {
        // The whole 360 deg fits the width, so a 4096-texel map on a 4096-pixel-wide screen is 1:1.
        assertEquals(0f, SkyPanoramaMath.texelLod(4096, 2048, 4096, 8000), 1e-6f)
        assertTrue(SkyPanoramaMath.texelLod(4096, 2048, 400, 800) > 1.0f)
        assertTrue(SkyPanoramaMath.texelLod(4096, 2048, 200, 800) > SkyPanoramaMath.texelLod(4096, 2048, 400, 800))
        assertEquals(0f, SkyPanoramaMath.texelLod(0, 2048, 400, 800), 0f)
    }

    @Test
    fun quantizeLstWrapsAndSnapsToTheQuantum() {
        assertEquals(0.0, SkyPanoramaMath.quantizeLst(359.99), 1e-9)
        assertEquals(10.05, SkyPanoramaMath.quantizeLst(10.04), 1e-9)
        assertEquals(12.0, SkyPanoramaMath.quantizeLst(12.0), 1e-9)
        assertEquals(12.0, SkyPanoramaMath.quantizeLst(-348.0), 1e-9)
    }

    private fun cross(a: DoubleArray, b: DoubleArray): DoubleArray = doubleArrayOf(
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0]
    )

    private fun assertVec(expected: DoubleArray, actual: DoubleArray) {
        for (i in 0..2) assertEquals("component $i", expected[i], actual[i], 1e-9)
    }

    private fun assertUv(u: Double, v: Double, actual: DoubleArray) {
        assertEquals("u", u, actual[0], 1e-6)
        assertEquals("v", v, actual[1], 1e-6)
    }
}
