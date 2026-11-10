package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.EclipseEngine
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.MoonEngine
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.astro_engine.WhatsUpTonightEngine
import com.alijafari.red.astronomy.data.catalog.AstronomyCatalog
import com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.CalendarSystem
import com.alijafari.red.astronomy.domain.ObjectType
import com.alijafari.red.astronomy.domain.TimeMachineMode
import com.alijafari.red.astronomy.domain.TimeMachineState
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.home.CatalogObjectsKey
import com.alijafari.red.astronomy.ui.home.EclipseSeriesKey
import com.alijafari.red.astronomy.ui.home.HomeAstroKeys
import com.alijafari.red.astronomy.ui.home.HomeAstroMemo
import com.alijafari.red.astronomy.ui.home.HomeWarmupDeadline
import com.alijafari.red.astronomy.ui.home.MoonKey
import com.alijafari.red.astronomy.ui.home.shouldGateLaunchForHomeWarmup
import com.alijafari.red.astronomy.ui.home.SingleFlightTable
import com.alijafari.red.astronomy.ui.home.TonightEventsKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * The start-up performance work: shared results must be exactly the results the engines give, the
 * memo must compute each input once, and the app must be wired to the warm-up and the splash hold.
 * (Bit-identity of the engines themselves against the pre-change code is AstroBitIdentityTest.)
 */
class StartupPerformanceTest {

    private val tehranLat = 35.6892
    private val tehranLon = 51.3890
    private val nowMs = 1_793_793_600_000L // 2026-11-04T12:00:00Z

    // ---- single-flight memo --------------------------------------------------------------------

    @Test
    fun concurrentRequestsForOneKeyComputeItOnce() {
        val table = SingleFlightTable<String>(maxEntries = 4)
        val computations = AtomicInteger()
        val release = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(8)
        val results = (0 until 8).map {
            pool.submit<Any> {
                table.get("k") {
                    computations.incrementAndGet()
                    release.await(5, TimeUnit.SECONDS)
                    Any()
                }
            }
        }
        Thread.sleep(200)
        assertFalse("not ready while computing", table.isReady("k"))
        release.countDown()
        val values = results.map { it.get(5, TimeUnit.SECONDS) }
        pool.shutdown()
        assertEquals("computed exactly once", 1, computations.get())
        values.forEach { assertSame("every caller gets the one result", values.first(), it) }
        assertTrue(table.isReady("k"))
    }

    @Test
    fun aFailedComputationIsRethrownUnchangedAndRetried() {
        val table = SingleFlightTable<String>(maxEntries = 4)
        val boom = IllegalStateException("boom")
        try {
            table.get<Any>("k") { throw boom }
            fail("expected the computation's exception")
        } catch (e: IllegalStateException) {
            assertSame("the original exception, not a wrapper", boom, e)
        }
        assertFalse(table.isReady("k"))
        assertEquals("retried after a failure", "ok", table.get("k") { "ok" })
    }

    @Test
    fun theTableIsBounded() {
        val table = SingleFlightTable<Int>(maxEntries = 3)
        repeat(10) { i -> table.get(i) { "v$i" } }
        assertEquals(3, table.size)
        assertTrue("the newest stays", table.isReady(9))
        assertFalse("the oldest is evicted", table.isReady(0))
    }

    // ---- shared results are the engines' results -------------------------------------------------

    @Test
    fun homeMemoReturnsExactlyWhatTheEnginesReturn() {
        HomeAstroMemo.clearForTest()
        val jd = TimeEngine.getJulianDate(nowMs)

        val moonKey = MoonKey(jd, tehranLat, tehranLon)
        val moon = HomeAstroMemo.get(moonKey)
        assertEquals(MoonEngine.calculateMoon(jd, tehranLat, tehranLon).toString(), moon.toString())
        assertSame("a stored result is served, not recomputed", moon, HomeAstroMemo.get(moonKey))

        val eclipseKey = EclipseSeriesKey(nowMs, tehranLat, tehranLon, 1190.0, "Asia/Tehran", CalendarSystem.SOLAR_HIJRI, 3)
        val direct = EclipseEngine.getNextEclipseSeries(
            nowMs = nowMs,
            userLatDeg = tehranLat,
            userLonDeg = tehranLon,
            elevationM = 1190.0,
            timezoneId = "Asia/Tehran",
            calendarSystem = CalendarSystem.SOLAR_HIJRI,
            count = 3
        )
        assertEquals(direct.toString(), HomeAstroMemo.get(eclipseKey).toString())

        val tonightKey = TonightEventsKey(jd, tehranLat, tehranLon, true, ISSEngine.cachedTLE)
        val tonightDirect = WhatsUpTonightEngine.calculateTonightEvents(jd = jd, userLatDeg = tehranLat, userLonDeg = tehranLon, isFa = true)
        assertEquals(tonightDirect.toString(), HomeAstroMemo.get(tonightKey).toString())

        // Satellites carry the wall clock (TLE age), so compare what Home actually uses.
        fun homeView(list: List<com.alijafari.red.astronomy.domain.CelestialObject>) =
            list.filter { it.type != ObjectType.SATELLITE && it.id != "sat_iss" && it.id != "planet_earth" }.toString()
        assertEquals(homeView(AstronomyCatalog.getAllObjects(jd)), homeView(HomeAstroMemo.get(CatalogObjectsKey(jd))))
    }

    @Test
    fun sharedEclipseScansGiveTheSameResultsAsFreshOnes() {
        EclipseEngine.clearScanCachesForTest()
        fun series(elevation: Double, calendar: CalendarSystem, tz: String) = EclipseEngine.getNextEclipseSeries(
            nowMs = nowMs,
            userLatDeg = 48.2082,
            userLonDeg = 16.3738,
            elevationM = elevation,
            timezoneId = tz,
            calendarSystem = calendar,
            count = 3
        ).toString()
        val fresh = series(190.0, CalendarSystem.GREGORIAN, "Europe/Vienna")
        // Same eclipses and place: the scans are now served from the cache, the formatting is redone.
        val fromCache = series(190.0, CalendarSystem.GREGORIAN, "Europe/Vienna")
        assertEquals(fresh, fromCache)
        val otherCalendar = series(190.0, CalendarSystem.SOLAR_HIJRI, "Asia/Tehran")
        EclipseEngine.clearScanCachesForTest()
        assertEquals("cached scans + other formatting == fresh scans + other formatting",
            series(190.0, CalendarSystem.SOLAR_HIJRI, "Asia/Tehran"), otherCalendar)
        assertEquals(fresh, series(190.0, CalendarSystem.GREGORIAN, "Europe/Vienna"))
    }

    @Test
    fun indexedCatalogLookupReturnsTheSameInstanceAsALinearScan() {
        val all = CanonicalAstroCatalog.getAllCanonicalObjects()
        val inputs = LinkedHashSet<String>()
        all.forEach { inputs += it.canonicalId; inputs += it.legacyIds; inputs += it.searchAliasesEn; inputs += it.searchAliasesFa }
        inputs += listOf("M 31", "ngc-224", "nothing_like_this", "")
        for (input in inputs) {
            val resolved = CanonicalAstroCatalog.resolveCanonicalId(input)
            val linear = all.find { it.canonicalId == resolved }
            assertSame("lookup of '$input'", linear, CanonicalAstroCatalog.getCanonicalObject(input))
        }
    }

    // ---- the warm-up asks for exactly what Home asks for ------------------------------------------

    @Test
    fun onlyHomeLaunchesWaitForHomeWarmup() {
        val home = MainUiState()
        assertTrue("ordinary Home launch is gated for its warm-up", shouldGateLaunchForHomeWarmup(home))
        assertFalse("satellite notification route must not wait for Home", shouldGateLaunchForHomeWarmup(home.copy(selectedTab = 1)))
        assertFalse("fullscreen sky route must not wait for Home", shouldGateLaunchForHomeWarmup(home.copy(showSkyViewer = true)))
        assertFalse("eclipse route must not wait for Home", shouldGateLaunchForHomeWarmup(home.copy(showEclipseWindow = true)))
        assertFalse(
            "object notification route must not wait for Home",
            shouldGateLaunchForHomeWarmup(home.copy(selectedObjectForDetail = AstronomyCatalog.SUN))
        )
    }

    @Test
    fun warmupFallbackUsesAnElapsedRealtimeDeadlineNotSplashFrames() {
        val startElapsedMs = 12_345L
        val deadlineElapsedMs = startElapsedMs + HomeWarmupDeadline.MAX_WAIT_MS
        assertEquals(HomeWarmupDeadline.MAX_WAIT_MS, HomeWarmupDeadline.remainingMs(deadlineElapsedMs, startElapsedMs))
        assertEquals(1L, HomeWarmupDeadline.remainingMs(deadlineElapsedMs, deadlineElapsedMs - 1L))
        assertEquals(0L, HomeWarmupDeadline.remainingMs(deadlineElapsedMs, deadlineElapsedMs))
        assertEquals(0L, HomeWarmupDeadline.remainingMs(deadlineElapsedMs, deadlineElapsedMs + 5_000L))
    }

    @Test
    fun warmupKeysFollowHomesBucketFormulas() {
        val live = 1_793_797_199_999L // 2026-11-04T12:59:59.999Z: minute bucket 12:59, hour bucket 12:00
        val state = MainUiState(language = AppLanguage.PERSIAN)
        val keys = HomeAstroKeys.forHome(state, live)
        val loc = state.userLocation
        val minute = live - Math.floorMod(live, 60_000L)
        val hour = live - Math.floorMod(live, 3_600_000L)
        val catalogJd = TimeEngine.getJulianDate(minute)
        assertEquals(
            listOf(
                EclipseSeriesKey(hour, loc.latitude, loc.longitude, loc.elevationMeters, loc.timezoneId, state.calendarSystem, 3),
                CatalogObjectsKey(catalogJd),
                TonightEventsKey(catalogJd, loc.latitude, loc.longitude, true, ISSEngine.cachedTLE),
                MoonKey(TimeEngine.getJulianDate(minute), loc.latitude, loc.longitude)
            ),
            keys
        )

        // Simulation time is used as is, never bucketed.
        val sim = MainUiState(timeMachineState = TimeMachineState(mode = TimeMachineMode.SIMULATION, simulationTimeMs = 1_234_567L))
        val simKeys = HomeAstroKeys.forHome(sim, live)
        assertEquals(1_234_567L, (simKeys[0] as EclipseSeriesKey).nowMs)
        assertEquals(CatalogObjectsKey(TimeEngine.getJulianDate(1_234_567L)), simKeys[1])
    }

    private fun source(relative: String): String =
        listOf("src/main/java/$relative", "app/src/main/java/$relative").map(::File).first { it.exists() }.readText()

    @Test
    fun everyHomeVariantUsesTheSharedBucketsAndMemo() {
        val variants = mapOf(
            "com/alijafari/red/astronomy/ui/screens/HomeScreen.kt" to "",
            "com/alijafari/red/astronomy/ui/cyber/CyberHomeScreen.kt" to "CYBER_",
            "com/alijafari/red/astronomy/ui/spatial/SpatialHomeScreen.kt" to "SPATIAL_",
            "com/alijafari/red/astronomy/ui/aether/AetherHomeScreen.kt" to "AETHER_"
        )
        for ((path, prefix) in variants) {
            val src = source(path)
            // The warm-up's constants must be the variant's, or the warm-up would compute the wrong keys.
            assertTrue(path, src.contains("private const val ${prefix}MOON_REFRESH_MS = ${HomeAstroKeys.MOON_REFRESH_MS / 1000}_000L"))
            assertTrue(path, src.contains("private const val ${prefix}CATALOG_REFRESH_MS = ${HomeAstroKeys.CATALOG_REFRESH_MS / 1000}_000L"))
            assertTrue(path, src.contains("private const val ${prefix}ECLIPSE_REFRESH_MS = 3_600_000L"))
            assertTrue(path, src.contains("{ HomeAstroKeys.moonKeys(it, uiState) }"))
            assertTrue(path, src.contains("{ HomeAstroKeys.catalogKeys(it, uiState, isFa) }"))
            assertTrue(path, src.contains("{ HomeAstroKeys.eclipseKeys(it, uiState) }"))
            assertTrue(path, src.contains("HomeAstroMemo.get(CatalogObjectsKey(catalogJd))"))
            assertTrue(path, src.contains("issTle = ISSEngine.cachedTLE"))
            assertFalse("$path must not compute the catalog on the main thread directly", src.contains("AstronomyCatalog.getAllObjects(catalogJd)"))
            assertFalse("$path must not run the eclipse search directly", src.contains("EclipseEngine.getNextEclipseSeries("))
        }
    }

    @Test
    fun mainActivityWiresTheWarmupAndTheSplashHold() {
        val activity = source("com/alijafari/red/astronomy/MainActivity.kt")
        assertTrue(activity.contains("viewModel.startStartupWarmup()"))
        assertTrue("only Home is warmed before its initial reveal", activity.contains("if (shouldGateLaunchForHomeWarmup(viewModel.uiState.value))"))
        assertTrue("other routes defer Home warm-up until after the splash hand-off", activity.contains("if (!showSplashScreen && !warmupDone) viewModel.startStartupWarmup()"))
        assertTrue(activity.contains("rememberPremiumSplashState(waitForContent = true)"))
        assertTrue(activity.contains("if (composeApp) {"))
        assertTrue(activity.contains("splashState?.markContentReady()"))
        assertTrue(activity.contains("reportFullyDrawn()"))
        assertTrue("warm-up fallback uses a saved elapsed-realtime deadline", activity.contains("SystemClock.elapsedRealtime()"))
        assertTrue("warm-up timeout does not depend on animation time", activity.contains("HomeWarmupDeadline.remainingMs("))
        assertTrue("timeout cannot leave a stalled splash covering the app", activity.contains("showSplashScreen = false"))
        assertTrue("non-Home routes bypass the Home warm-up gate", activity.contains("!homeWarmupRequired"))
        assertTrue("sync work is scheduled after the first frame", activity.contains("mainHandler.post(::scheduleBackgroundSyncOnce)"))
        assertFalse("the TLE store is not opened eagerly in onCreate", activity.contains("val tleRepo = TleRepository.getInstance(applicationContext)"))
        val onCreate = activity.substringAfter("override fun onCreate(").substringBefore("setContent {")
        assertFalse("no WorkManager setup before the first frame", onCreate.contains("IssTleWorker.schedulePeriodicSync"))
    }
}
