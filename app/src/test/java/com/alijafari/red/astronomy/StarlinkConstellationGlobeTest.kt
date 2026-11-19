package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.ISSEngine.TLEData
import com.alijafari.red.astronomy.astro_engine.StarlinkTrainManager
import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The globe must paint the whole Starlink shell, not only the satellites above the observer's
 * horizon: these tests lock the bulk sub-satellite sweep, its memoization, and the renderer wiring.
 */
class StarlinkConstellationGlobeTest {

    // Nov 13, 2026 12:00:00 UTC -> JD 2461358.0
    private val nov132026NoonUtcMs = ((2461358.0 - 2440587.5) * 86_400_000.0).toLong()

    @Before
    fun setUp() {
        StarlinkTrainManager.invalidateCaches()
    }

    private fun withChecksum(lineWithoutChecksum: String): String {
        val body = lineWithoutChecksum.padEnd(68, ' ').substring(0, 68)
        var sum = 0
        for (c in body) {
            when {
                c.isDigit() -> sum += (c - '0')
                c == '-' -> sum += 1
            }
        }
        return body + (sum % 10).toString()
    }

    private fun buildSyntheticTle(
        name: String,
        noradToken5: String,
        cospar8: String,
        epochYear2: Int = 26,
        epochDay: Double = 317.25000000,
        incDeg: Double = 53.1600,
        raanDeg: Double = 124.2000,
        ecc7: String = "0001400",
        argPerigeeDeg: Double = 90.0000,
        meanAnomalyDeg: Double = 10.0000,
        meanMotion: Double = 15.88000000,
    ): TLEData {
        val norad5 = noradToken5.padStart(5, ' ').substring(0, 5)
        val cosparPadded = cospar8.padEnd(8, ' ').substring(0, 8)
        val epochField = String.format(Locale.US, "%02d%012.8f", epochYear2, epochDay)
        val l1Raw = "1 ${norad5}U $cosparPadded $epochField  .00025000  00000+0  15000-3 0  999"
        val l2Raw = String.format(
            Locale.US,
            "2 %s %8.4f %8.4f %s %8.4f %8.4f %11.8f00001",
            norad5,
            incDeg,
            raanDeg,
            ecc7,
            argPerigeeDeg,
            meanAnomalyDeg,
            meanMotion,
        )
        return TLEData(name = name, line1 = withChecksum(l1Raw), line2 = withChecksum(l2Raw))
    }

    private val fixtureNorads = (0 until 12).map { 61000 + it }

    /** A full-shell fixture: 12 planes spread over 360° of RAAN, as the real constellation is. */
    private fun constellationFixture(): List<TLEData> = fixtureNorads.mapIndexed { plane, norad ->
        buildSyntheticTle(
            name = "STARLINK-$norad",
            noradToken5 = norad.toString(),
            cospar8 = "26001A",
            raanDeg = plane * 30.0,
            meanAnomalyDeg = (plane * 37) % 360,
        )
    }

    @Test
    fun everyStarlinkIsProjectedRegardlessOfTheObserverHorizon() {
        val tles = constellationFixture()
        val points = StarlinkTrainManager.computeConstellationPoints(tles, nov132026NoonUtcMs)

        assertEquals("the sweep must return the whole shell, not an overhead shortlist", tles.size, points.size)
        assertEquals(fixtureNorads.toSet(), points.map { it.noradId }.toSet())
        for (point in points) {
            assertTrue("sub-satellite latitude ${point.subLatDeg}", point.subLatDeg in -90.0..90.0)
            assertTrue("sub-satellite longitude ${point.subLonDeg}", point.subLonDeg in -180.0..180.0)
            assertTrue("plausible LEO altitude ${point.altitudeKm}", point.altitudeKm in 150.0..900.0)
        }
        // The planes are spread over the whole globe, so the fixes cannot be an overhead-only column.
        val spread = points.maxOf { it.subLonDeg } - points.minOf { it.subLonDeg }
        assertTrue("expected a globally spread constellation, got ${spread}° of longitude", spread > 120.0)
    }

    @Test
    fun sweepIsMemoizedPerBucketAndTracksTime() {
        val tles = constellationFixture()
        val bucket = StarlinkTrainManager.CONSTELLATION_POSITION_TTL_MS
        val aligned = nov132026NoonUtcMs / bucket * bucket
        val first = StarlinkTrainManager.computeConstellationPoints(tles, aligned)
        val recheck = StarlinkTrainManager.computeConstellationPoints(tles, aligned + bucket - 1L)
        assertSame("a repeat call inside the same bucket must reuse the sweep", first, recheck)

        val later = StarlinkTrainManager.computeConstellationPoints(tles, aligned + bucket * 3)
        assertNotEquals(
            "a new bucket must re-propagate the shell",
            first.first().subLonDeg,
            later.first().subLonDeg,
        )
    }

    @Test
    fun decayedOrUnparseableEntriesNeverReachTheGlobe() {
        val healthy = constellationFixture()
        val decayed = buildSyntheticTle(
            name = "STARLINK-DECAYED",
            noradToken5 = "61999",
            cospar8 = "26001A",
            meanMotion = 11.5,
        )
        val points = StarlinkTrainManager.computeConstellationPoints(healthy + decayed, nov132026NoonUtcMs)
        assertEquals(healthy.size, points.size)
        assertTrue(points.none { it.noradId == 61999 })
    }

    @Test
    fun denseProjectionMatchesTheAllocatingProjection() {
        val camera = EarthCameraSnapshot(
            yawRad = 0.8f,
            pitchRad = -0.35f,
            distance = 3.22f,
        )
        val viewportWidth = 390f
        val viewportHeight = 844f
        val buffer = FloatArray(6)
        for (lat in -80..80 step 20) {
            for (lon in -180..180 step 45) {
                for (radius in listOf(1f, 1.087f)) {
                    val expected = Earth3DMath.projectLatLon(lat.toDouble(), lon.toDouble(), radius,
                        camera, viewportWidth, viewportHeight)
                    val model = Earth3DMath.latLonToModel(lat.toDouble(), lon.toDouble(), radius)
                    val visible = Earth3DMath.projectModelPointInto(
                        mx = model[0],
                        my = model[1],
                        mz = model[2],
                        radius = radius,
                        camera = camera,
                        viewportWidth = viewportWidth,
                        viewportHeight = viewportHeight,
                        out = buffer,
                    )
                    assertEquals("visible flag at $lat/$lon/$radius", expected.visible, visible)
                    assertEquals("worldX at $lat/$lon/$radius", expected.worldX, buffer[2], 1e-4f)
                    assertEquals("worldY at $lat/$lon/$radius", expected.worldY, buffer[3], 1e-4f)
                    assertEquals("worldZ at $lat/$lon/$radius", expected.worldZ, buffer[4], 1e-4f)
                    if (expected.visible) {
                        assertEquals("screenX at $lat/$lon/$radius", expected.screenX, buffer[0], 1e-3f)
                        assertEquals("screenY at $lat/$lon/$radius", expected.screenY, buffer[1], 1e-3f)
                    }
                }
            }
        }
    }

    @Test
    fun globeAndStageWireTheWholeConstellation() {
        fun readSource(relative: String): String {
            val candidates = listOf(File("src/main/java/$relative"), File("app/src/main/java/$relative"))
            return candidates.firstOrNull { it.isFile }?.readText()
                ?: error("Source not found: $relative")
        }

        val stage = readSource("com/alijafari/red/astronomy/ui/satellites/SatellitesStage.kt")
        assertTrue("the stage sweeps the whole shell",
            stage.contains("StarlinkTrainManager.computeConstellationPoints("))
        assertTrue("and feeds it to the globe", stage.contains("constellationPoints = constellationPoints"))

        val overlay = readSource("com/alijafari/red/astronomy/ui/components/SatelliteEarth3D.kt")
        assertTrue("the globe accepts the constellation",
            overlay.contains("constellationPoints: List<StarlinkTrainManager.StarlinkConstellationPoint>"))
        assertTrue("and batches it into point clouds", overlay.contains("drawPoints("))
        assertTrue(overlay.contains("PointMode.Points"))
    }
}
