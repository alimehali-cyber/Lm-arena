package com.alijafari.red.astronomy

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.alijafari.red.astronomy.astro_engine.ISSEngine.TLEData
import com.alijafari.red.astronomy.astro_engine.SGP4Propagator
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.astro_engine.SatelliteEngine
import com.alijafari.red.astronomy.astro_engine.StarlinkTrainManager
import com.alijafari.red.astronomy.data.repository.STARLINK_GP_URL
import com.alijafari.red.astronomy.data.repository.STARLINK_LAST_30_DAYS_URL
import com.alijafari.red.astronomy.data.repository.STARLINK_SUP_GP_URL
import com.alijafari.red.astronomy.data.repository.TleRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.GZIPOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StarlinkPipelineTest {

    // Nov 13, 2026 12:00:00 UTC -> JD 2461358.0 (dayOfYear = 317.5 in 2026)
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
        meanMotion: Double = 15.88000000
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
            meanMotion
        )
        val line1 = withChecksum(l1Raw)
        val line2 = withChecksum(l2Raw)
        assertTrue("Synthetic line1 checksum invalid: $line1", TleRepository.validateChecksum(line1))
        assertTrue("Synthetic line2 checksum invalid: $line2", TleRepository.validateChecksum(line2))
        return TLEData(name = name, line1 = line1, line2 = line2)
    }

    private fun gzipBytes(text: String): ByteArray {
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).bufferedWriter(Charsets.UTF_8).use { it.write(text) }
        return baos.toByteArray()
    }

    @Test
    fun `1 epochToJulian matches exact astronomical reference values`() {
        assertEquals(2461041.5, StarlinkTrainManager.epochToJulian(2026, 1.0), 1e-9)
        assertEquals(2461358.0, StarlinkTrainManager.epochToJulian(2026, 317.5), 1e-9)
        assertEquals(2461358.0, StarlinkTrainManager.epochToJulian(26, 317.5), 1e-9)

        val sgp4 = SGP4Propagator()
        assertEquals(
            sgp4.tleEpochToJulianDate(1998, 324.5),
            StarlinkTrainManager.epochToJulian(98, 324.5),
            1e-9
        )
    }

    @Test
    fun `2 detectActiveTrains accepts tight string of pearls and rejects dispersed 360deg DirectToCell shell`() {
        val tles = ArrayList<TLEData>()

        // Group A: Genuine 20-satellite launch train (COSPAR 26042) tightly clustered over ~9.5° + 2 straggler outliers
        for (i in 0 until 20) {
            val piece = ('A'.code + i).toChar().toString()
            tles.add(
                buildSyntheticTle(
                    name = "STARLINK-${31000 + i}",
                    noradToken5 = (61000 + i).toString(),
                    cospar8 = "26042$piece",
                    epochDay = 317.20,
                    incDeg = 53.16,
                    raanDeg = 124.20,
                    argPerigeeDeg = 80.0,
                    meanAnomalyDeg = 40.0 + i * 0.5, // 40.0° .. 49.5° -> 9.5° tight arc!
                    meanMotion = 15.88
                )
            )
        }
        // Add 2 distant outlier debris/test sats in same COSPAR (within 15% trim tolerance: floor(22 * 0.85) = 18)
        for (k in 0..1) {
            tles.add(
                buildSyntheticTle(
                    name = "STARLINK-OUTLIER-$k",
                    noradToken5 = (61050 + k).toString(),
                    cospar8 = "26042Z$k",
                    epochDay = 317.20,
                    incDeg = 53.16,
                    raanDeg = 124.25,
                    argPerigeeDeg = 80.0,
                    meanAnomalyDeg = 220.0 + k * 25.0,
                    meanMotion = 15.88
                )
            )
        }

        // Group B: Dispersed Direct-to-Cell operational shell (24 sats, fresh epoch today, meanMotion 15.72,
        // same COSPAR 24199 & RAAN, but spread evenly every 15° around all 360° of the orbit!)
        for (i in 0 until 24) {
            tles.add(
                buildSyntheticTle(
                    name = "STARLINK-DTC-${4000 + i}",
                    noradToken5 = (59500 + i).toString(),
                    cospar8 = "24199A",
                    epochDay = 317.30,
                    incDeg = 53.05,
                    raanDeg = 210.0,
                    argPerigeeDeg = 45.0,
                    meanAnomalyDeg = (i * 15.0) % 360.0, // Full 360° operational ring
                    meanMotion = 15.72
                )
            )
        }

        val trains = StarlinkTrainManager.detectActiveTrains(tles, nowMs = nov132026NoonUtcMs)
        assertEquals("Only the genuine tight train should pass; dispersed D2C ring must be rejected", 1, trains.size)

        val train = trains.first()
        assertEquals("2026-042", train.groupKey)
        assertTrue("Trimmed train span (${train.angularSpanDeg}°) must be <= 90° and tight (< 12°)", train.angularSpanDeg < 12.0)
        assertEquals(20, train.satelliteCount)

        // Verify Leader -> Trailer ordering: Leader (#0) had largest initial meanAnomaly in the core cluster
        val leaderNorad = StarlinkTrainManager.extractNoradId(train.memberTles.first().line1)
        val trailerNorad = StarlinkTrainManager.extractNoradId(train.memberTles.last().line1)
        assertEquals(61019, leaderNorad)
        assertEquals(61000, trailerNorad)
    }

    @Test
    fun `2b detectActiveTrains supports up to 3 trains sorted by tightest span and Alpha5 NORAD IDs`() {
        val tles = ArrayList<TLEData>()
        val trainSpecs = listOf(
            Triple("26051", 2.0, "A01"), // 10 sats * 2.0° = 18° span, Alpha-5 NORADs A0100..A0109
            Triple("26052", 0.6, "A02"), // 10 sats * 0.6° = 5.4° span (tightest!)
            Triple("     ", 3.5, "A03"), // Supplemental blank COSPAR -> SUP-... fallback, 31.5° span
            Triple("26054", 6.0, "A04")  // 54° span (4th train -> capped out by max 3)
        )

        trainSpecs.forEachIndexed { idx, (cospar5, stepDeg, alphaPrefix) ->
            for (j in 0 until 10) {
                tles.add(
                    buildSyntheticTle(
                        name = "STARLINK-${idx * 100 + j}",
                        noradToken5 = "${alphaPrefix}0$j",
                        cospar8 = if (cospar5.isBlank()) "        " else "${cospar5}A",
                        epochDay = 317.40,
                        incDeg = 43.0 + idx * 3.0,
                        raanDeg = 30.0 + idx * 40.0,
                        argPerigeeDeg = 10.0,
                        meanAnomalyDeg = (355.0 + j * stepDeg) % 360.0, // Exercises 359° -> 0° circular wraparound!
                        meanMotion = 15.92
                    )
                )
            }
        }

        val trains = StarlinkTrainManager.detectActiveTrains(tles, nowMs = nov132026NoonUtcMs)
        assertEquals(3, trains.size)
        assertEquals("2026-052", trains[0].groupKey)
        assertEquals("2026-051", trains[1].groupKey)
        assertTrue(trains[2].groupKey.startsWith("SUP-"))
        assertTrue(trains[0].angularSpanDeg < trains[1].angularSpanDeg)
        assertTrue(trains[1].angularSpanDeg < trains[2].angularSpanDeg)
        assertEquals(100209, trains[0].leaderItem.noradId)
    }

    @Test
    fun `3 tleRepository enforces 2h disk cache cooldown deduplicates newest epoch and survives partial failure`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(TleRepository.STARLINK_PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        java.io.File(context.filesDir, TleRepository.STARLINK_CACHE_FILE_NAME).delete()

        val starlinkHttpCalls = AtomicInteger(0)
        var failBulkGp = false
        var fakeNowMs = nov132026NoonUtcMs

        val supTleNewest = buildSyntheticTle(
            name = "STARLINK-5001",
            noradToken5 = "62001",
            cospar8 = "26060A",
            epochDay = 317.48, // Newer epoch in SUP_GP
            incDeg = 53.16,
            raanDeg = 100.0
        )
        val last30OlderDuplicate = buildSyntheticTle(
            name = "STARLINK-5001",
            noradToken5 = "62001",
            cospar8 = "26060A",
            epochDay = 316.90, // Older epoch -> must lose deduplication!
            incDeg = 53.16,
            raanDeg = 100.0
        )
        val last30UncatalogedObject = buildSyntheticTle(
            name = "OBJECT B",
            noradToken5 = "62002",
            cospar8 = "26060B",
            epochDay = 317.40,
            incDeg = 53.15, // Matches 53° Starlink shell
            raanDeg = 100.1,
            meanMotion = 15.85
        )
        val last30NonStarlinkGeo = buildSyntheticTle(
            name = "INTELSAT 40E",
            noradToken5 = "62003",
            cospar8 = "26061A",
            epochDay = 317.40,
            incDeg = 0.05, // GEO non-Starlink -> must be filtered out!
            raanDeg = 10.0,
            meanMotion = 1.0027
        )
        val bulkCatalogSat = buildSyntheticTle(
            name = "STARLINK-3999",
            noradToken5 = "58999",
            cospar8 = "24088A",
            epochDay = 317.10,
            incDeg = 53.05,
            raanDeg = 200.0,
            meanMotion = 15.06
        )

        val mockClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val req = chain.request()
                val url = req.url.toString()
                starlinkHttpCalls.incrementAndGet()

                when (url) {
                    STARLINK_SUP_GP_URL -> {
                        val txt = "${supTleNewest.name}\n${supTleNewest.line1}\n${supTleNewest.line2}\n"
                        Response.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(txt.toResponseBody("text/plain".toMediaType()))
                            .build()
                    }
                    STARLINK_LAST_30_DAYS_URL -> {
                        val txt = buildString {
                            append("${last30OlderDuplicate.name}\n${last30OlderDuplicate.line1}\n${last30OlderDuplicate.line2}\n")
                            append("${last30UncatalogedObject.name}\n${last30UncatalogedObject.line1}\n${last30UncatalogedObject.line2}\n")
                            append("${last30NonStarlinkGeo.name}\n${last30NonStarlinkGeo.line1}\n${last30NonStarlinkGeo.line2}\n")
                        }
                        Response.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(txt.toResponseBody("text/plain".toMediaType()))
                            .build()
                    }
                    STARLINK_GP_URL -> {
                        assertEquals("gzip", req.header("Accept-Encoding"))
                        if (failBulkGp) {
                            Response.Builder()
                                .request(req)
                                .protocol(Protocol.HTTP_1_1)
                                .code(403)
                                .message("Forbidden")
                                .body("Rate limited".toResponseBody("text/plain".toMediaType()))
                                .build()
                        } else {
                            val gz = gzipBytes("${bulkCatalogSat.name}\n${bulkCatalogSat.line1}\n${bulkCatalogSat.line2}\n")
                            Response.Builder()
                                .request(req)
                                .protocol(Protocol.HTTP_1_1)
                                .code(200)
                                .message("OK")
                                .header("Content-Encoding", "gzip")
                                .body(gz.toResponseBody("application/gzip".toMediaType()))
                                .build()
                        }
                    }
                    else -> {
                        Response.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body("".toResponseBody("text/plain".toMediaType()))
                            .build()
                    }
                }
            })
            .build()

        val repo = TleRepository(
            context = context,
            okHttpClientOverride = mockClient,
            clockMs = { fakeNowMs },
            autoLoadDiskAsync = false
        )

        // 1. Initial sync (no disk cache yet) -> hits all 3 Starlink feeds
        assertTrue(repo.refreshTles(refreshStaticCatalog = false))
        assertEquals(3, starlinkHttpCalls.get())
        assertTrue(repo.getStarlinkCacheFile().exists())

        val byNorad = repo.getStarlinkTles().associateBy { StarlinkTrainManager.extractNoradId(it.line1) }
        assertEquals("Should contain 62001, 62002 (OBJECT at 53°), and 58999; INTELSAT excluded", 3, byNorad.size)
        assertEquals("Newest epoch (317.48) must win deduplication", 317.48, byNorad[62001]!!.epochDay, 1e-6)
        assertFalse("Non-Starlink GEO satellite must be filtered out", byNorad.containsKey(62003))

        // 2. Call refreshTles again 30 minutes later (< 2-hour cooldown) -> ZERO network calls!
        fakeNowMs += 30 * 60 * 1000L
        assertFalse(repo.shouldFetchStarlinkFromNetwork(fakeNowMs))
        repo.refreshTles(refreshStaticCatalog = false)
        assertEquals("No Starlink HTTP requests allowed within 2-hour cooldown", 3, starlinkHttpCalls.get())

        // 3. Cold-start new repository instance offline -> loads immediately from disk cache!
        val coldStartRepo = TleRepository(
            context = context,
            okHttpClientOverride = mockClient,
            clockMs = { fakeNowMs },
            autoLoadDiskAsync = false
        )
        assertTrue(coldStartRepo.ensureStarlinkDiskCacheLoaded())
        assertEquals(3, coldStartRepo.getStarlinkTles().size)
        assertEquals("Cold-start disk load must not trigger network", 3, starlinkHttpCalls.get())

        // 4. Advance past 2-hour cooldown (+2h 5m) and simulate HTTP 403 on bulk STARLINK_GP_URL
        fakeNowMs += (100 * 60 * 1000L) // total +130 minutes > 120 minutes
        failBulkGp = true
        assertTrue(coldStartRepo.shouldFetchStarlinkFromNetwork(fakeNowMs))
        assertTrue(coldStartRepo.refreshTles(refreshStaticCatalog = false))
        assertEquals(6, starlinkHttpCalls.get())
        // Bulk 58999 from prior disk cache + SUP_GP 62001 + LAST_30_DAYS 62002 all preserved!
        assertEquals(3, coldStartRepo.getStarlinkTles().size)
    }

    @Test
    fun `4 static catalog satellites have zero regressions and dynamic Starlinks resolve cleanly`() {
        val staticNorads = SatelliteCatalog.satellites.map { it.noradId }
        assertEquals(listOf(25544, 48274, 20580, 27386), staticNorads)

        // Register a dynamic Starlink and verify static catalog + dynamic Starlink resolution
        val dynTle = buildSyntheticTle(
            name = "STARLINK-7777",
            noradToken5 = "63777",
            cospar8 = "26070A",
            epochDay = 317.4,
            incDeg = 53.16,
            raanDeg = 140.0
        )
        val dynSnap = StarlinkTrainManager.selectRealtimeOverheadStarlinks(
            starlinkTles = listOf(dynTle),
            userLatDeg = 35.6892,
            userLonDeg = 51.3890,
            nowMs = nov132026NoonUtcMs
        )
        assertNotNull(dynSnap)

        for (expectedNorad in staticNorads) {
            val resolved = SatelliteEngine.resolveSatelliteItem("sat_$expectedNorad")
            assertNotNull(resolved)
            assertEquals(expectedNorad, resolved!!.noradId)
        }
    }

    @Test
    fun `5 all four UI theme Satellites screens share full multi-train and individual Starlink parity`() {
        fun readScreenSource(relative: String): String {
            val candidates = listOf("src/main/java/$relative", "app/src/main/java/$relative")
            return candidates.map(::File).firstOrNull { it.exists() }?.readText()
                ?: error("Source not found: $relative")
        }

        val screens = mapOf(
            "AetherSatellitesScreen" to readScreenSource("com/alijafari/red/astronomy/ui/aether/AetherSatellitesScreen.kt"),
            "ISSScreen" to readScreenSource("com/alijafari/red/astronomy/ui/screens/ISSScreen.kt"),
            "SpatialSatellitesScreen" to readScreenSource("com/alijafari/red/astronomy/ui/spatial/SpatialSatellitesScreen.kt"),
            "CyberSatellitesScreen" to readScreenSource("com/alijafari/red/astronomy/ui/cyber/CyberSatellitesScreen.kt")
        )

        val requiredTokens = listOf(
            "tleRepo.starlinkVersion.collectAsState()",
            "StarlinkTrainManager.selectRealtimeOverheadStarlinks(",
            "StarlinkTrainManager.detectActiveTrains(",
            "StarlinkTrainManager.selectTopVisibleStarlinks(",
            "trainPearlStatesByTrain",
            "mapOverheadStarlinkStates",
            "allInteractiveMapStates",
            "isTrainPass = sat.isTrain"
        )

        for ((screenName, src) in screens) {
            for (token in requiredTokens) {
                assertTrue(
                    "Expected $screenName to contain '$token' for full Starlink parity",
                    src.contains(token)
                )
            }
            // None of the 4 screens should rely on the legacy single-train detectTrain() call anymore
            assertFalse(
                "$screenName should use detectActiveTrains() instead of legacy single detectTrain()",
                src.contains("StarlinkTrainManager.detectTrain(")
            )
        }

        // Verify all 4 pass card implementations carry the STRING OF PEARLS train badge
        val detailSrc = readScreenSource("com/alijafari/red/astronomy/ui/screens/SatelliteDetailScreen.kt")
        assertTrue(detailSrc.contains("STRING OF PEARLS"))
        assertTrue(screens.getValue("AetherSatellitesScreen").contains("STRING OF PEARLS"))
        assertTrue(screens.getValue("SpatialSatellitesScreen").contains("STRING OF PEARLS"))
        assertTrue(screens.getValue("CyberSatellitesScreen").contains("STRING OF PEARLS"))
    }
}

