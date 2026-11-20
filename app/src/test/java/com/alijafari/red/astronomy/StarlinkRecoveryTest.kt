package com.alijafari.red.astronomy

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.alijafari.red.astronomy.astro_engine.ISSEngine.TLEData
import com.alijafari.red.astronomy.astro_engine.StarlinkTrainManager
import com.alijafari.red.astronomy.data.repository.STARLINK_GP_URL
import com.alijafari.red.astronomy.data.repository.STARLINK_LAST_30_DAYS_URL
import com.alijafari.red.astronomy.data.repository.STARLINK_SUP_GP_URL
import com.alijafari.red.astronomy.data.repository.StarlinkSyncStatus
import com.alijafari.red.astronomy.data.repository.TleRepository
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A fresh install — or app data cleared — has no Starlink catalogue at all and must download one.
 * That single download can be refused (CelesTrak 403), and the app used to answer that with silence:
 * an empty globe, no retry for six hours, and no word about why. These checks pin the recovery.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StarlinkRecoveryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var now = 1_800_000_000_000L

    @Before
    fun freshDevice() {
        File(context.filesDir, TleRepository.STARLINK_CACHE_FILE_NAME).delete()
        context.getSharedPreferences(TleRepository.STARLINK_PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
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

    private fun starlinkTle(name: String, norad: String): TLEData {
        val norad5 = norad.padStart(5, ' ').substring(0, 5)
        val l1 = withChecksum("1 $norad5" + "U 26060A   26317.25000000  .00025000  00000+0  15000-3 0  999")
        val l2 = withChecksum(
            String.format(Locale.US, "2 %s %8.4f %8.4f 0001400 %8.4f %8.4f %11.8f00001",
                norad5, 53.16, 124.20, 90.0, 10.0, 15.88000000),
        )
        assertTrue(TleRepository.validateChecksum(l1))
        assertTrue(TleRepository.validateChecksum(l2))
        return TLEData(name = name, line1 = l1, line2 = l2)
    }

    private val feedBodies = mapOf(
        STARLINK_SUP_GP_URL to starlinkTle("STARLINK-7001", "61001"),
        STARLINK_LAST_30_DAYS_URL to starlinkTle("STARLINK-7002", "61002"),
        STARLINK_GP_URL to starlinkTle("STARLINK-7003", "61003"),
    )

    private fun client(calls: AtomicInteger, refuse: () -> Boolean): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val req = chain.request()
                calls.incrementAndGet()
                if (refuse()) {
                    Response.Builder().request(req).protocol(Protocol.HTTP_1_1)
                        .code(403).message("Forbidden")
                        .body("Rate limited".toResponseBody("text/plain".toMediaType()))
                        .build()
                } else {
                    val tle = feedBodies[req.url.toString()] ?: starlinkTle("STARLINK-7004", "61004")
                    Response.Builder().request(req).protocol(Protocol.HTTP_1_1)
                        .code(200).message("OK")
                        .body("${tle.name}\n${tle.line1}\n${tle.line2}\n".toResponseBody("text/plain".toMediaType()))
                        .build()
                }
            })
            .build()

    private fun repo(calls: AtomicInteger, refuse: () -> Boolean) = TleRepository(
        context = context,
        okHttpClientOverride = client(calls, refuse),
        clockMs = { now },
        autoLoadDiskAsync = false,
    )

    @Test
    fun aRefusedCatalogueIsNamedAndTheNextAttemptIsSpaced() = runBlocking {
        val calls = AtomicInteger()
        val repository = repo(calls) { true }

        assertFalse("nothing arrived, so nothing was updated", repository.refreshTles(refreshStaticCatalog = false))
        assertTrue(repository.isStarlinkCatalogMissing())
        assertEquals(StarlinkSyncStatus.FAILED, repository.starlinkSync.value.status)
        assertEquals("the screen can say what refused", "HTTP 403", repository.starlinkSync.value.failureReason)
        assertEquals(3, calls.get())

        // Relaunching the app, or reopening the tab, used to fire the same three bulk requests again
        // within seconds — the burst that keeps CelesTrak's ban alive.
        now += 60_000L
        assertFalse(repository.shouldFetchStarlinkFromNetwork(now))
        repository.refreshTles(refreshStaticCatalog = false)
        assertEquals("a second caller one minute later stays local", 3, calls.get())

        // The first retry is five minutes out, then the gap doubles per failure.
        assertEquals(5 * 60_000L, repository.starlinkRetryGapMs())
        now += 4 * 60_000L
        assertTrue(repository.shouldFetchStarlinkFromNetwork(now))
        assertFalse(repository.refreshTles(refreshStaticCatalog = false))
        assertEquals(6, calls.get())
        assertEquals(10 * 60_000L, repository.starlinkRetryGapMs())
        assertEquals(StarlinkSyncStatus.FAILED, repository.starlinkSync.value.status)
    }

    @Test
    fun aFailedDownloadNeverBlocksTheUserTappingTryAgain() = runBlocking {
        val calls = AtomicInteger()
        var refuse = true
        val repository = repo(calls) { refuse }

        repository.refreshTles(refreshStaticCatalog = false)
        assertEquals(3, calls.get())

        // Inside the retry gap the timer says no...
        now += 60_000L
        assertFalse(repository.shouldFetchStarlinkFromNetwork(now))
        // ...but an explicit tap must still be honoured.
        refuse = false
        assertTrue(repository.forceRefreshStarlink())
        assertEquals(6, calls.get())
        assertEquals(StarlinkSyncStatus.READY, repository.starlinkSync.value.status)
        assertEquals(3, repository.getStarlinkTles().size)
        assertNull(repository.starlinkSync.value.failureReason)
        assertFalse(repository.isStarlinkCatalogMissing())
    }

    @Test
    fun theLaunchBurstCollapsesIntoOneDownload() = runBlocking {
        val calls = AtomicInteger()
        val repository = repo(calls) { false }

        // The two launch workers and the satellites tab all ask at the same moment.
        coroutineScope {
            (1..3).map { async(Dispatchers.IO) { repository.refreshTles(refreshStaticCatalog = false) } }.awaitAll()
        }
        assertEquals("one sync, three feeds — not nine requests", 3, calls.get())
        assertEquals(StarlinkSyncStatus.READY, repository.starlinkSync.value.status)
        assertEquals(3, repository.getStarlinkTles().size)
    }

    @Test
    fun aSuccessfulDownloadRestoresTheTwoHourCooldown() = runBlocking {
        val calls = AtomicInteger()
        var refuse = true
        val repository = repo(calls) { refuse }
        repository.refreshTles(refreshStaticCatalog = false)

        now += 5 * 60_000L
        refuse = false
        assertTrue(repository.refreshTles(refreshStaticCatalog = false))
        assertEquals("the failure streak is forgotten", 5 * 60_000L, repository.starlinkRetryGapMs())

        now += 30 * 60_000L
        assertFalse("back on the normal two-hour cooldown", repository.shouldFetchStarlinkFromNetwork(now))
        now += 100 * 60_000L
        assertTrue(repository.shouldFetchStarlinkFromNetwork(now))
    }

    @Test
    fun theScreenExplainsAndRetriesWhileTheCatalogueIsMissing() {
        fun readSource(relative: String): String {
            val candidates = listOf(File("src/main/java/$relative"), File("app/src/main/java/$relative"))
            return candidates.firstOrNull { it.isFile }?.readText() ?: error("Source not found: $relative")
        }

        fun readRes(relative: String): String {
            val candidates = listOf(File("src/main/res/$relative"), File("app/src/main/res/$relative"))
            return candidates.firstOrNull { it.isFile }?.readText() ?: error("Resource not found: $relative")
        }

        val stage = readSource("com/alijafari/red/astronomy/ui/satellites/SatellitesStage.kt")
        val components = readSource("com/alijafari/red/astronomy/ui/satellites/SatelliteComponents.kt")
        val worker = readSource("com/alijafari/red/astronomy/data/worker/TleSyncWorker.kt")

        assertTrue("the screen keeps asking while the sky is empty", stage.contains("tleRepo.refreshTles(refreshStaticCatalog = false)"))
        assertTrue(stage.contains("delay(60_000L)"))
        assertTrue("a tap retries immediately", stage.contains("tleRepo.forceRefreshStarlink()"))
        assertTrue("and the reason reaches the globe", stage.contains("starlinkSync.failureReason"))
        assertTrue(components.contains("fun SatStarlinkNotice("))
        assertTrue(components.contains("testTag(\"sat_starlink_notice\")"))
        assertTrue("the worker retries instead of pretending success", worker.contains("isStarlinkCatalogMissing()"))
        assertTrue(worker.contains("Result.retry()"))

        val english = readRes("values/satellites.xml")
        val persian = readRes("values-fa/satellites.xml")
        for (key in listOf("sat_starlink_downloading", "sat_starlink_failed", "sat_starlink_offline", "sat_starlink_retry")) {
            assertTrue("missing English $key", english.contains("name=\"$key\""))
            assertTrue("missing Persian $key", persian.contains("name=\"$key\""))
        }
        assertTrue("the technical reason keeps its placeholder",
            Regex("<string name=\"sat_starlink_failed\">[^<]*%1\\\$s").containsMatchIn(persian))
    }
}
