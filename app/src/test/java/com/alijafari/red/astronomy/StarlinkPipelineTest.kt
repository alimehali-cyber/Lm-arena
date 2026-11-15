package com.alijafari.red.astronomy

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.ISSEngine.TLEData
import com.alijafari.red.astronomy.astro_engine.SGP4Propagator
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.astro_engine.SatelliteEngine
import com.alijafari.red.astronomy.astro_engine.StarlinkTrainManager
import com.alijafari.red.astronomy.astro_engine.TleDataSource
import com.alijafari.red.astronomy.astro_engine.TleMetadata
import com.alijafari.red.astronomy.data.repository.STARLINK_GP_URL
import com.alijafari.red.astronomy.data.repository.STARLINK_LAST_30_DAYS_URL
import com.alijafari.red.astronomy.data.repository.STARLINK_SUP_GP_URL
import com.alijafari.red.astronomy.data.repository.TleRepository
import com.alijafari.red.astronomy.domain.UserLocation
import com.alijafari.red.astronomy.notification.AstroNotificationManager
import com.alijafari.red.astronomy.notification.AstroNotificationStore
import com.alijafari.red.astronomy.util.formatTleStalenessBadge
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
    fun `2b detectActiveTrains supports up to 3 trains sorted by newest launch then tightest span and Alpha5 NORAD IDs`() {
        val tles = ArrayList<TLEData>()
        val trainSpecs = listOf(
            Triple("26051", 2.0, "A01"), // 10 sats * 2.0° = 18.0° span, launchKey 2026051
            Triple("26052", 0.6, "A02"), // 10 sats * 0.6° = 5.4° span, launchKey 2026052 (newer than 26051!)
            Triple("     ", 1.5, "A03"), // Supplemental blank COSPAR -> SUP-... launch-day priority #1, 13.5° span
            Triple("26049", 1.8, "A04"), // Older 4th train (launchKey 2026049 -> capped out by max 3)
            Triple("26053", 10.0, "A05") // Dispersing sector with 10.0° median gap (> 8.0° limit) -> MUST be rejected!
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
        // Newest launch batch first: uncataloged SUP launch-day batch -> 2026-052 -> 2026-051 (while 2026-053 was rejected for 4.0° > 2.5° spacing)
        assertTrue(trains[0].groupKey.startsWith("SUP-"))
        assertEquals("2026-052", trains[1].groupKey)
        assertEquals("2026-051", trains[2].groupKey)
        assertEquals(100309, trains[0].leaderItem.noradId)
        assertEquals(100209, trains[1].leaderItem.noradId)
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

    @Test
    fun `6 detectActiveTrains rejects 2025-034 DirectToCell sector and prioritizes newest 2026 launch and unsplit SUP cluster`() {
        val tles = ArrayList<TLEData>()

        // 1. Old February 2025 Direct-to-Cell batch (2025-034A..N) at ~355 km (meanMotion = 15.70)
        //    with 13 satellites packed inside a < 20° arc (1.2° spacing!) and fresh TODAY 2026 epoch!
        for (i in 0 until 13) {
            val piece = ('A'.code + i).toChar().toString()
            tles.add(
                buildSyntheticTle(
                    name = "STARLINK-11350-DTC-$i",
                    noradToken5 = (63100 + i).toString(),
                    cospar8 = "25034$piece",
                    epochYear2 = 26,
                    epochDay = 317.45, // Fresh today TLE epoch!
                    incDeg = 53.16,
                    raanDeg = 88.5,
                    argPerigeeDeg = 75.0,
                    meanAnomalyDeg = 120.0 + i * 1.2, // 14.4° compact Direct-to-Cell sector (< 45°)!
                    meanMotion = 15.70
                )
            )
        }

        // 2. Recent 2026 Starlink launch train (2026-068) from this week at initial orbit-raising (meanMotion = 16.35)
        for (i in 0 until 14) {
            val piece = ('A'.code + i).toChar().toString()
            tles.add(
                buildSyntheticTle(
                    name = "STARLINK-36200-$i",
                    noradToken5 = (68200 + i).toString(),
                    cospar8 = "26068$piece",
                    epochYear2 = 26,
                    epochDay = 317.42,
                    incDeg = 53.16,
                    raanDeg = 162.4,
                    argPerigeeDeg = 90.0,
                    meanAnomalyDeg = 200.0 + i * 1.5, // 19.5° arc (wider than 2025-034, yet MUST win!)
                    meanMotion = 16.35
                )
            )
        }

        val trainsCatalogedOnly = StarlinkTrainManager.detectActiveTrains(tles, nowMs = nov132026NoonUtcMs)
        assertEquals(
            "Old 2025-034 Direct-to-Cell sector MUST be 100% rejected; only 2026-068 may be returned",
            1,
            trainsCatalogedOnly.size
        )
        assertEquals("2026-068", trainsCatalogedOnly.first().groupKey)
        assertFalse(trainsCatalogedOnly.any { it.groupKey.contains("2025-034") })

        // 3. Also verify uncataloged launch-day SUP train straddling RAAN 123.9°..124.1° and inc 52.95°..53.05°
        //    with temporary supplemental NORAD IDs 70001..70012 is NOT split across integer bucket boundaries!
        StarlinkTrainManager.invalidateCaches()
        for (i in 0 until 12) {
            tles.add(
                buildSyntheticTle(
                    name = "OBJECT ${('A'.code + i).toChar()}",
                    noradToken5 = (70001 + i).toString(),
                    cospar8 = "UNKNOWN ",
                    epochYear2 = 26,
                    epochDay = 317.48,
                    incDeg = 52.95 + i * 0.01,   // Crosses 53.00° boundary
                    raanDeg = 123.90 + i * 0.02, // Crosses 124.00° boundary
                    argPerigeeDeg = 60.0,
                    meanAnomalyDeg = 15.0 + i * 0.4,
                    meanMotion = 16.48
                )
            )
        }

        val trainsWithSup = StarlinkTrainManager.detectActiveTrains(tles, nowMs = nov132026NoonUtcMs)
        assertEquals(2, trainsWithSup.size)
        // Uncataloged launch-day SUP cluster comes first with all 12 pearls intact, followed by 2026-068!
        assertTrue(trainsWithSup[0].groupKey.startsWith("SUP-"))
        assertEquals("Boundary-straddling SUP train must retain all 12 satellites without splitting", 12, trainsWithSup[0].satelliteCount)
        assertEquals("2026-068", trainsWithSup[1].groupKey)
        assertFalse(trainsWithSup.any { it.groupKey.contains("2025-034") })

        // 4. Verify Top-8 COSPAR Launch Recency Gate within 2026: when 8 newer 2026 batches exist
        //    (26061..26068), an older 9th batch (26012) is hard-rejected even if compact!
        val nineBatches2026 = ArrayList<TLEData>()
        val batchCodes = listOf("26012", "26061", "26062", "26063", "26064", "26065", "26066", "26067", "26068")
        batchCodes.forEachIndexed { bIdx, code ->
            // Only 26012 (oldest, 9th) and 26068 (newest, 1st) are compact; 26061..26067 are already dispersed operational rings
            val step = if (code == "26012" || code == "26068") 0.8 else 36.0
            for (k in 0 until 10) {
                nineBatches2026.add(
                    buildSyntheticTle(
                        name = "STARLINK-$code-$k",
                        noradToken5 = (80000 + bIdx * 100 + k).toString(),
                        cospar8 = "${code}A",
                        epochYear2 = 26,
                        epochDay = 317.40,
                        incDeg = 53.16,
                        raanDeg = (20.0 + bIdx * 30.0) % 360.0,
                        argPerigeeDeg = 45.0,
                        meanAnomalyDeg = (k * step) % 360.0,
                        meanMotion = 15.80
                    )
                )
            }
        }
        StarlinkTrainManager.invalidateCaches()
        val gated2026Trains = StarlinkTrainManager.detectActiveTrains(nineBatches2026, nowMs = nov132026NoonUtcMs)
        assertEquals("Older 9th batch 2026-012 outside top-8 recent launches MUST be rejected", 1, gated2026Trains.size)
        assertEquals("2026-068", gated2026Trains.first().groupKey)
    }

    @Test
    fun `7 Starship Flight 15 26-Starlink Starbase deployment 500-series COSPAR schema v4 invalidation and fallback`() {
        // 7a. CelesTrak 500-series synthetic COSPAR numbers (26500..26999) MUST return null from parseCosparLaunchKey
        assertNull(StarlinkTrainManager.parseCosparLaunchKey("26500", "26500A"))
        assertNull(StarlinkTrainManager.parseCosparLaunchKey("26501", "26501B"))
        assertNull(StarlinkTrainManager.parseCosparLaunchKey("26999", "26999Z"))
        assertEquals(2026099, StarlinkTrainManager.parseCosparLaunchKey("26099", "26099A"))

        // 7b. Starship Flight 15 (Ship 42) deploys 26 Starlinks out of Starbase Boca Chica (~26.4° inclination)
        //     listed in GROUP=last-30-days as OBJECT A..OBJECT Z + STARSHIP R/B + STARSHIP DEB
        val rawLast30Feed = ArrayList<TLEData>()
        for (i in 0 until 26) {
            val piece = ('A'.code + i).toChar().toString()
            rawLast30Feed.add(
                buildSyntheticTle(
                    name = "OBJECT $piece",
                    noradToken5 = (69100 + i).toString(),
                    cospar8 = "26099$piece",
                    epochYear2 = 26,
                    epochDay = 317.35,
                    incDeg = 26.42, // Starbase Boca Chica inclination (outside narrow 4-band Falcon 9 shells!)
                    raanDeg = 148.6,
                    argPerigeeDeg = 70.0,
                    meanAnomalyDeg = 10.0 + i * 3.2, // 80.0° spread 3 days after deployment (3.2° spacing)
                    meanMotion = 16.58               // ~215 km Starship deployment altitude
                )
            )
        }
        // Add R/B and DEB on same launch -> MUST be excluded!
        rawLast30Feed.add(
            buildSyntheticTle(
                name = "STARSHIP R/B",
                noradToken5 = "69150",
                cospar8 = "26099AA",
                epochYear2 = 26,
                epochDay = 317.35,
                incDeg = 26.42,
                raanDeg = 148.6,
                meanMotion = 16.58
            )
        )
        rawLast30Feed.add(
            buildSyntheticTle(
                name = "OBJECT DEB",
                noradToken5 = "69151",
                cospar8 = "26099AB",
                epochYear2 = 26,
                epochDay = 317.35,
                incDeg = 26.42,
                raanDeg = 148.6,
                meanMotion = 16.58
            )
        )

        val retained = TleRepository.filterLast30DaysStarlinkClusterEntries(rawLast30Feed)
        assertEquals("All 26 Starbase OBJECT payloads must be retained while R/B and DEB are excluded", 26, retained.size)

        // Even if 9 stale CelesTrak 500-series synthetic COSPAR batches (26501..26509, > 5d old) exist in catalog,
        // they must NOT consume Top-8 COSPAR slots or block 2026-099!
        val catalogWithSynthetic500 = ArrayList<TLEData>(retained)
        for (sIdx in 1..9) {
            for (k in 0 until 6) {
                catalogWithSynthetic500.add(
                    buildSyntheticTle(
                        name = "STARLINK-SUP-$sIdx-$k",
                        noradToken5 = (75000 + sIdx * 10 + k).toString(),
                        cospar8 = "2650${sIdx}A",
                        epochYear2 = 26,
                        epochDay = 310.0, // > 5.0 days old synthetic sup-gp
                        incDeg = 53.15,
                        raanDeg = 10.0 * sIdx,
                        meanAnomalyDeg = k * 1.0,
                        meanMotion = 15.90
                    )
                )
            }
        }

        val starshipTrains = StarlinkTrainManager.detectActiveTrains(catalogWithSynthetic500, nowMs = nov132026NoonUtcMs)
        assertEquals(1, starshipTrains.size)
        assertEquals("2026-099", starshipTrains.first().groupKey)
        assertEquals(26, starshipTrains.first().satelliteCount)

        // 7c. Guaranteed Newest-Launch Fallback: when 2026-099 spreads to 145° (> 110°) during late raising,
        //     fallback still selects 2026-099 (and still rejects 2025-034)!
        val lateDispersing = ArrayList<TLEData>()
        for (i in 0 until 12) {
            lateDispersing.add(
                buildSyntheticTle(
                    name = "STARLINK-DTC-2025-$i",
                    noradToken5 = (63200 + i).toString(),
                    cospar8 = "25034A",
                    epochYear2 = 26,
                    epochDay = 317.40,
                    incDeg = 53.16,
                    raanDeg = 88.5,
                    meanAnomalyDeg = 50.0 + i * 1.0,
                    meanMotion = 15.70
                )
            )
        }
        for (i in 0 until 15) {
            lateDispersing.add(
                buildSyntheticTle(
                    name = "STARLINK-V3-$i",
                    noradToken5 = (69300 + i).toString(),
                    cospar8 = "26099A",
                    epochYear2 = 26,
                    epochDay = 317.40,
                    incDeg = 26.42,
                    raanDeg = 148.6,
                    meanAnomalyDeg = 20.0 + i * 10.0, // 140° arc (> 110° primary gate, <= 180° fallback arc!)
                    meanMotion = 15.25
                )
            )
        }
        StarlinkTrainManager.invalidateCaches()
        val fallbackTrains = StarlinkTrainManager.detectActiveTrains(lateDispersing, nowMs = nov132026NoonUtcMs)
        assertEquals(1, fallbackTrains.size)
        assertEquals("2026-099", fallbackTrains.first().groupKey)
        assertEquals(15, fallbackTrains.first().satelliteCount)

        // 7d. Non-destructive Schema v4 upgrade: pre-existing v0 cache file MUST be preserved & loaded
        //     into memory for offline resilience while bypassing the 2-hour cooldown until online sync succeeds!
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cacheFile = File(context.filesDir, TleRepository.STARLINK_CACHE_FILE_NAME)
        val staleTle = retained.first()
        cacheFile.writeText("${staleTle.name}\n${staleTle.line1}\n${staleTle.line2}\n")
        context.getSharedPreferences(TleRepository.STARLINK_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(TleRepository.KEY_LAST_STARLINK_SYNC_MS, nov132026NoonUtcMs)
            .remove(TleRepository.KEY_STARLINK_CACHE_SCHEMA_VERSION) // Simulates upgrade from pre-v4 APK!
            .commit()

        val upgradedRepo = TleRepository(
            context = context,
            clockMs = { nov132026NoonUtcMs + 60_000L }, // Only 1 minute after old APK's sync!
            autoLoadDiskAsync = false
        )
        assertTrue("Pre-v4 cache must be loaded non-destructively on ensureStarlinkDiskCacheLoaded()", upgradedRepo.ensureStarlinkDiskCacheLoaded())
        assertTrue("Existing cache file must NOT be wiped before online sync succeeds", cacheFile.exists())
        assertEquals("Offline upgraded user must retain cached Starlink TLE immediately", 1, upgradedRepo.getStarlinkTles().size)
        assertEquals(0, upgradedRepo.getStoredStarlinkSchemaVersion())
        assertTrue("Upgraded APK must immediately bypass 2h cooldown to fetch fresh feeds", upgradedRepo.shouldFetchStarlinkFromNetwork(nov132026NoonUtcMs + 60_000L))
    }

    @Test
    fun `8 Non-destructive offline schema migration unknown NORAD null meanMotion 16_70 alerts 48h cap and staleness UI`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cacheFile = File(context.filesDir, TleRepository.STARLINK_CACHE_FILE_NAME)

        // 8a. Non-destructive schema migration when offline vs online
        val cachedPreUpgradeTle = buildSyntheticTle(
            name = "STARLINK-PRE-V4",
            noradToken5 = "68801",
            cospar8 = "26088A",
            epochYear2 = 26,
            epochDay = 317.10,
            incDeg = 53.16,
            raanDeg = 120.0,
            meanAnomalyDeg = 45.0,
            meanMotion = 15.90
        )
        cacheFile.writeText("${cachedPreUpgradeTle.name}\n${cachedPreUpgradeTle.line1}\n${cachedPreUpgradeTle.line2}\n")
        context.getSharedPreferences(TleRepository.STARLINK_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(TleRepository.KEY_LAST_STARLINK_SYNC_MS, nov132026NoonUtcMs)
            .putInt(TleRepository.KEY_STARLINK_CACHE_SCHEMA_VERSION, 2) // Older schema version
            .commit()

        var shouldFailNetwork = true
        val freshNetworkTle = buildSyntheticTle(
            name = "STARLINK-FRESH-V4",
            noradToken5 = "69901",
            cospar8 = "26099A",
            epochYear2 = 26,
            epochDay = 317.48,
            incDeg = 26.42,
            raanDeg = 148.6,
            meanAnomalyDeg = 90.0,
            meanMotion = 16.70
        )

        val switchingClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val req = chain.request()
                if (shouldFailNetwork) {
                    Response.Builder()
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .code(503)
                        .message("Offline / Service Unavailable")
                        .body("".toResponseBody("text/plain".toMediaType()))
                        .build()
                } else {
                    val body = when (req.url.toString()) {
                        STARLINK_SUP_GP_URL -> "${freshNetworkTle.name}\n${freshNetworkTle.line1}\n${freshNetworkTle.line2}\n"
                        else -> ""
                    }
                    Response.Builder()
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(body.toResponseBody("text/plain".toMediaType()))
                        .build()
                }
            })
            .build()

        val migrationRepo = TleRepository(
            context = context,
            okHttpClientOverride = switchingClient,
            clockMs = { nov132026NoonUtcMs + 120_000L },
            autoLoadDiskAsync = false
        )

        // Attempt refresh while offline during schema migration (v2 -> v4)
        migrationRepo.refreshTles(refreshStaticCatalog = false)
        assertTrue("Offline refresh during schema upgrade MUST preserve starlink_merged_cache.tle", cacheFile.exists())
        assertEquals("Offline user must keep pre-upgrade cached Starlink TLE", 1, migrationRepo.getStarlinkTles().size)
        assertEquals(68801, StarlinkTrainManager.extractNoradId(migrationRepo.getStarlinkTles().first().line1))
        assertEquals("Schema version must NOT advance while network sync fails", 2, migrationRepo.getStoredStarlinkSchemaVersion())
        assertTrue("Cooldown must remain bypassed until schema v4 network sync succeeds", migrationRepo.shouldFetchStarlinkFromNetwork(nov132026NoonUtcMs + 180_000L))

        // Reconnect online -> schema v4 sync succeeds and stamps version = 4
        shouldFailNetwork = false
        assertTrue("Online refresh must succeed and persist merged cache", migrationRepo.refreshTles(refreshStaticCatalog = false))
        assertEquals("Schema version must update to 4 after successful online sync", 4, migrationRepo.getStoredStarlinkSchemaVersion())
        assertEquals(2, migrationRepo.getStarlinkTles().size)
        assertFalse("2h cooldown must engage once schema v4 sync has succeeded", migrationRepo.shouldFetchStarlinkFromNetwork(nov132026NoonUtcMs + 180_000L))

        // 8b. Unknown NORAD returns null instead of ISS TLE; 4 static satellites have refreshed 26317 checksummed TLEs
        assertNull("Unknown NORAD ID must return null from getTle() instead of ISS TLE", migrationRepo.getTle(987654))
        assertNull("Unknown NORAD ID must return null from getTleWithMetadata()", migrationRepo.getTleWithMetadata(987654))

        val expectedStaticNorads = setOf(25544, 48274, 20580, 27386)
        assertEquals(expectedStaticNorads, SatelliteCatalog.satellites.map { it.noradId }.toSet())
        for (sat in SatelliteCatalog.satellites) {
            assertTrue("Line 1 checksum must be valid for ${sat.id}", TleRepository.validateChecksum(sat.defaultTle.line1))
            assertTrue("Line 2 checksum must be valid for ${sat.id}", TleRepository.validateChecksum(sat.defaultTle.line2))
            assertTrue("Bundled fallback TLE for ${sat.id} must be mid-November 2026 (26317)", sat.defaultTle.line1.contains("26317.50000000"))
        }
        assertTrue(ISSEngine.TLEData().line1.contains("26317.50000000"))
        assertTrue(TleRepository.validateChecksum(ISSEngine.TLEData().line1))
        assertTrue(TleRepository.validateChecksum(ISSEngine.TLEData().line2))

        // Verify SatelliteEngine.getEffectiveTle(dynamicStarlinkItem) preserves the Starlink's own TLE when resolver returns null
        val prevResolver = SatelliteEngine.customTleResolver
        val prevMetaResolver = SatelliteEngine.customTleMetadataResolver
        try {
            SatelliteEngine.customTleResolver = { id -> migrationRepo.getTle(id) }
            SatelliteEngine.customTleMetadataResolver = { id -> migrationRepo.getTleWithMetadata(id) }
            val prunedDynamicItem = StarlinkTrainManager.selectRealtimeOverheadStarlinks(
                starlinkTles = listOf(freshNetworkTle),
                userLatDeg = 0.0,
                userLonDeg = 0.0,
                nowMs = nov132026NoonUtcMs,
                maxOverhead = 10
            ).let {
                // Build a dynamic item with an uncached NORAD 77777
                val uncachedTle = buildSyntheticTle(
                    name = "STARLINK-UNCACHED",
                    noradToken5 = "77777",
                    cospar8 = "26099Z",
                    epochYear2 = 26,
                    epochDay = 317.45,
                    incDeg = 53.16,
                    raanDeg = 40.0,
                    meanAnomalyDeg = 10.0,
                    meanMotion = 16.70
                )
                SatelliteCatalog.satellites.first().copy(
                    id = "starlink_77777",
                    noradId = 77777,
                    nameEn = "Starlink-77777",
                    defaultTle = uncachedTle
                )
            }
            val effective = SatelliteEngine.getEffectiveTle(prunedDynamicItem)
            assertEquals("Uncached dynamic Starlink must keep its own TLE, never ISS 25544", prunedDynamicItem.defaultTle.line1, effective.line1)
        } finally {
            SatelliteEngine.customTleResolver = prevResolver
            SatelliteEngine.customTleMetadataResolver = prevMetaResolver
        }

        // 8c. meanMotion = 16.70 rev/day Starlinks accepted by BOTH selectRealtimeOverheadStarlinks() and selectTopVisibleStarlinks()
        val lowInsertionStarlinks = ArrayList<TLEData>()
        var idx = 0
        for (raan in listOf(0.0, 60.0, 120.0, 180.0, 240.0, 300.0)) {
            for (ma in listOf(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0)) {
                idx++
                lowInsertionStarlinks.add(
                    buildSyntheticTle(
                        name = "STARLINK-LOW-$idx",
                        noradToken5 = (81000 + idx).toString(),
                        cospar8 = "25040A", // Individual non-train Starlink (excludedNorads empty) at meanMotion = 16.70
                        epochYear2 = 26,
                        epochDay = 317.45,
                        incDeg = 53.16,
                        raanDeg = raan,
                        argPerigeeDeg = 90.0,
                        meanAnomalyDeg = ma,
                        meanMotion = 16.70
                    )
                )
            }
        }
        StarlinkTrainManager.invalidateCaches()
        val sampleSubPos = ISSEngine().calculateTopocentricPos(
            timestampMs = nov132026NoonUtcMs,
            userLatDeg = 0.0,
            userLonDeg = 0.0,
            userAltMeters = 0.0,
            tle = lowInsertionStarlinks.first()
        )
        val overheadSnapshot = StarlinkTrainManager.selectRealtimeOverheadStarlinks(
            starlinkTles = lowInsertionStarlinks,
            userLatDeg = sampleSubPos.subLatDeg,
            userLonDeg = sampleSubPos.subLonDeg,
            nowMs = nov132026NoonUtcMs,
            maxOverhead = 20
        )
        assertTrue(
            "meanMotion = 16.70 Starlinks must be accepted by selectRealtimeOverheadStarlinks()",
            overheadSnapshot.overheadStarlinks.isNotEmpty()
        )

        val topVisible = StarlinkTrainManager.selectTopVisibleStarlinks(
            starlinkTles = lowInsertionStarlinks,
            activeTrains = emptyList(),
            userLatDeg = 35.6892,
            userLonDeg = 51.3890,
            startTimeMs = nov132026NoonUtcMs,
            maxCount = 20
        )
        assertTrue(
            "meanMotion = 16.70 Starlinks must be accepted by selectTopVisibleStarlinks()",
            topVisible.isNotEmpty()
        )

        // 8d. Automated "starlink_train" alerts in AstroNotificationManager cap lookahead to 48 hours (2 days)
        val alertTrainTles = ArrayList<TLEData>()
        for (i in 0 until 12) {
            alertTrainTles.add(
                buildSyntheticTle(
                    name = "STARLINK-ALERT-$i",
                    noradToken5 = (82000 + i).toString(),
                    cospar8 = "26099${('A'.code + i).toChar()}",
                    epochYear2 = 26,
                    epochDay = 317.45,
                    incDeg = 53.16,
                    raanDeg = 289.0, // Passes directly over Tehran during evening twilight!
                    argPerigeeDeg = 0.0,
                    meanAnomalyDeg = 46.8 + i * 1.2,
                    meanMotion = 15.60
                )
            )
        }
        AstroNotificationManager.cancelAllPassNotifications(context)
        val observerLoc = UserLocation(
            cityNameFa = "تهران",
            cityNameEn = "Tehran",
            latitude = 35.6892,
            longitude = 51.3890
        )
        AstroNotificationManager.scheduleRollingSatellitePasses(
            context = context,
            userLocation = observerLoc,
            selectedSatIds = setOf("iss_zarya", "starlink_train"),
            leadMinutes = 10,
            scanDays = 7,
            nowMs = nov132026NoonUtcMs,
            starlinkTlesOverride = alertTrainTles
        )
        val registeredTrain = SatelliteCatalog.getById("starlink_train")
        assertTrue("Primary Starlink train must be registered under 'starlink_train'", registeredTrain.isTrain)
        val raw7DayTrainPasses = ISSEngine.predictPasses(
            userLatDeg = observerLoc.latitude,
            userLonDeg = observerLoc.longitude,
            startTimestampMs = nov132026NoonUtcMs,
            tle = registeredTrain.defaultTle,
            scanDays = 7,
            visibleOnly = true,
            standardMag = registeredTrain.standardMagnitude
        )
        assertTrue(
            "7-day un-capped train prediction must contain passes beyond +48h",
            raw7DayTrainPasses.any { it.startTimeMs > nov132026NoonUtcMs + 48L * 3_600_000L }
        )
        val scheduledTrainPasses = AstroNotificationStore.getAll(context).filter { it.objectId == "starlink_train" }
        assertTrue("Starlink train must schedule upcoming visible passes within 48h", scheduledTrainPasses.isNotEmpty())
        for (item in scheduledTrainPasses) {
            assertTrue(
                "Scheduled starlink_train pass must be capped within +48 hours",
                item.eventTimeMs <= nov132026NoonUtcMs + 48L * 3_600_000L
            )
        }
        val savedMonitored = context.getSharedPreferences("astro_prefs", Context.MODE_PRIVATE)
            .getStringSet("monitored_satellite_ids", emptySet()).orEmpty()
        assertTrue("monitored_satellite_ids must retain 'starlink_train'", "starlink_train" in savedMonitored)

        // 8e. TLE staleness / train drift badge thresholds & reactive network observer wiring
        val dummyTle = ISSEngine.TLEData()
        val freshTrainMeta = TleMetadata(69901, TleDataSource.LIVE_NETWORK, 1.4, nov132026NoonUtcMs, false, dummyTle)
        val driftingTrainMeta = TleMetadata(69901, TleDataSource.PERSISTED_CACHE, 2.6, nov132026NoonUtcMs, false, dummyTle)
        val fourDayStaticMeta = TleMetadata(25544, TleDataSource.PERSISTED_CACHE, 4.0, nov132026NoonUtcMs, false, dummyTle)
        val eightDayStaticMeta = TleMetadata(25544, TleDataSource.PERSISTED_CACHE, 8.3, nov132026NoonUtcMs, false, dummyTle)

        assertNull(formatTleStalenessBadge(isViewingActiveTrain = true, tleMetadata = freshTrainMeta, isFa = false))
        assertEquals("TLE 2.6d • Drift", formatTleStalenessBadge(isViewingActiveTrain = true, tleMetadata = driftingTrainMeta, isFa = false))
        assertNull(formatTleStalenessBadge(isViewingActiveTrain = false, tleMetadata = fourDayStaticMeta, isFa = false))
        assertEquals("TLE: 8.3d old", formatTleStalenessBadge(isViewingActiveTrain = false, tleMetadata = eightDayStaticMeta, isFa = false))

        fun readSrc(rel: String): String {
            return listOf("src/main/java/$rel", "app/src/main/java/$rel")
                .map(::File)
                .first { it.exists() }
                .readText()
        }
        for (relPath in listOf(
            "com/alijafari/red/astronomy/ui/screens/ISSScreen.kt",
            "com/alijafari/red/astronomy/ui/aether/AetherSatellitesScreen.kt",
            "com/alijafari/red/astronomy/ui/spatial/SpatialSatellitesScreen.kt",
            "com/alijafari/red/astronomy/ui/cyber/CyberSatellitesScreen.kt",
            "com/alijafari/red/astronomy/ui/screens/SatelliteDetailScreen.kt"
        )) {
            val src = readSrc(relPath)
            assertTrue("Expected $relPath to use reactive rememberIsNetworkOnline(context)", src.contains("rememberIsNetworkOnline(context)"))
            assertTrue("Expected $relPath to use formatTleStalenessBadge(", src.contains("formatTleStalenessBadge("))
        }
    }
}


