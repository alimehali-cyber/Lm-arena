package com.alijafari.red.astronomy.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.alijafari.red.astronomy.astro_engine.EclipseEngine
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.MoonEngine
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.astro_engine.WhatsUpTonightEngine
import com.alijafari.red.astronomy.data.catalog.AstronomyCatalog
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.CalendarSystem
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.TimeMachineMode
import com.alijafari.red.astronomy.ui.MainUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

// ------------------------------------------------------------------------------------------------
// Home's heavy astronomy, computed once per input and shared.
//
// Every Home variant (Default, Cyber, Spatial) derives the same four expensive results: the whole
// catalog with positions, the Moon, the eclipse series and What's Up Tonight. They used to be
// computed inside `remember` on the main thread, during the very first composition of the app,
// which is what kept the launch screen blank. They now go through [HomeAstroMemo]:
//
//  * A key is a data class holding EVERY input of its computation, and `compute()` is the exact
//    call Home made before. Equal keys therefore give bit-identical results, so serving a stored
//    result is indistinguishable from recomputing it (AstroBitIdentityTest pins the engines).
//  * The start-up warm-up (MainViewModel.startStartupWarmup) fills the memo off the main thread
//    while the splash plays, so Home's first composition finds everything ready.
//  * In live mode, [rememberReadyTimeMs] lets a refresh bucket advance only once that bucket's
//    results are computed (off the main thread). Until then Home keeps showing the previous
//    bucket's complete, self-consistent snapshot, exactly as it already does for up to a whole
//    bucket by design. Location, language and calendar changes, and simulation time, stay
//    synchronous exactly as before.
// ------------------------------------------------------------------------------------------------

/** One of Home's expensive computations, identified by all of its inputs. */
internal sealed interface HomeAstroKey<out T : Any> {
    fun compute(): T
}

/** The whole catalog with positions at [jd]. Home filters satellites out before using it. */
internal data class CatalogObjectsKey(val jd: Double) : HomeAstroKey<List<CelestialObject>> {
    override fun compute(): List<CelestialObject> = AstronomyCatalog.getAllObjects(jd)
}

/** The Moon (position, phase, rise and set) at [jd] for an observer. */
internal data class MoonKey(
    val jd: Double,
    val latitude: Double,
    val longitude: Double
) : HomeAstroKey<MoonEngine.MoonData> {
    override fun compute(): MoonEngine.MoonData = MoonEngine.calculateMoon(jd, latitude, longitude)
}

/** The next eclipses, exactly as Home asks for them. */
internal data class EclipseSeriesKey(
    val nowMs: Long,
    val latitude: Double,
    val longitude: Double,
    val elevationM: Double,
    val timezoneId: String,
    val calendarSystem: CalendarSystem,
    val count: Int
) : HomeAstroKey<Pair<List<EclipseEngine.EclipseResult>, List<EclipseEngine.EclipseResult>>> {
    override fun compute(): Pair<List<EclipseEngine.EclipseResult>, List<EclipseEngine.EclipseResult>> =
        EclipseEngine.getNextEclipseSeries(
            nowMs = nowMs,
            userLatDeg = latitude,
            userLonDeg = longitude,
            elevationM = elevationM,
            timezoneId = timezoneId,
            calendarSystem = calendarSystem,
            count = count
        )
}

/** What's Up Tonight. [issTle] is part of the key because tonight's ISS pass depends on it. */
internal data class TonightEventsKey(
    val jd: Double,
    val latitude: Double,
    val longitude: Double,
    val isFa: Boolean,
    val issTle: ISSEngine.TLEData
) : HomeAstroKey<List<WhatsUpTonightEngine.TonightEvent>> {
    override fun compute(): List<WhatsUpTonightEngine.TonightEvent> =
        WhatsUpTonightEngine.calculateTonightEvents(
            jd = jd,
            userLatDeg = latitude,
            userLonDeg = longitude,
            isFa = isFa,
            issTle = issTle
        )
}

/**
 * A bounded, single-flight memo table: a key being computed on one thread is awaited, never computed
 * a second time, by any other thread asking for it. A computation that throws is forgotten (the next
 * request retries) and its exception is rethrown unchanged. Least recently used entries are evicted.
 */
internal class SingleFlightTable<K : Any>(private val maxEntries: Int) {

    init {
        require(maxEntries > 0) { "maxEntries must be positive" }
    }

    private val tasks = object : LinkedHashMap<K, FutureTask<Any>>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, FutureTask<Any>>?): Boolean =
            size > maxEntries
    }

    fun <V : Any> get(key: K, compute: () -> V): V {
        var owner = false
        val task = synchronized(tasks) {
            tasks[key] ?: FutureTask<Any> { compute() }.also {
                tasks[key] = it
                owner = true
            }
        }
        if (owner) task.run()
        try {
            @Suppress("UNCHECKED_CAST")
            return task.get() as V
        } catch (e: ExecutionException) {
            synchronized(tasks) {
                if (tasks[key] === task) tasks.remove(key)
            }
            throw e.cause ?: e
        }
    }

    /** Whether [key]'s result is stored and finished, so [get] would return without computing. */
    fun isReady(key: K): Boolean {
        val task = synchronized(tasks) { tasks[key] } ?: return false
        if (!task.isDone || task.isCancelled) return false
        return try {
            task.get()
            true
        } catch (e: ExecutionException) {
            false
        }
    }

    val size: Int
        get() = synchronized(tasks) { tasks.size }

    fun clear() {
        synchronized(tasks) { tasks.clear() }
    }
}

/** The process-wide store behind Home: one small [SingleFlightTable] per kind of result. */
internal object HomeAstroMemo {

    // Two of each is enough for a bucket change (old + new); a little more absorbs the Moon card's
    // scrubbing and the live/start-up pair. The catalog lists are the largest (~100 KB each).
    private val catalog = SingleFlightTable<HomeAstroKey<*>>(maxEntries = 3)
    private val moon = SingleFlightTable<HomeAstroKey<*>>(maxEntries = 6)
    private val eclipses = SingleFlightTable<HomeAstroKey<*>>(maxEntries = 3)
    private val tonight = SingleFlightTable<HomeAstroKey<*>>(maxEntries = 3)

    private fun tableFor(key: HomeAstroKey<*>): SingleFlightTable<HomeAstroKey<*>> = when (key) {
        is CatalogObjectsKey -> catalog
        is MoonKey -> moon
        is EclipseSeriesKey -> eclipses
        is TonightEventsKey -> tonight
    }

    /** The result for [key]: stored, awaited if another thread is computing it, or computed now. */
    fun <T : Any> get(key: HomeAstroKey<T>): T = tableFor(key).get(key) { key.compute() }

    /** Whether [key]'s result is ready, so [get] would return without computing. */
    fun isReady(key: HomeAstroKey<*>): Boolean = tableFor(key).isReady(key)

    internal fun clearForTest() {
        catalog.clear()
        moon.clear()
        eclipses.clear()
        tonight.clear()
    }
}

/** Mutable holder for [rememberReadyTimeMs]; only touched on the main thread. */
private class ReadyTimeHolder {
    var shownMs: Long? = null
    val version = mutableIntStateOf(0)
}

/**
 * The refresh-bucket time Home should show.
 *
 * With [deferUntilReady] off (simulation mode), this is simply [targetMs], as before. With it on (live
 * mode), the clock stepping FORWARD into the next bucket (by at most [maxDeferredStepMs]) is adopted
 * the moment every result that bucket needs ([keysAt]) is ready; until then the previous bucket is kept
 * and the new one is computed on [Dispatchers.Default]. Anything else (the first bucket, a jump back, a
 * big jump such as leaving the time machine) is adopted at once and computed synchronously when not
 * already stored, exactly as before.
 */
@Composable
internal fun rememberReadyTimeMs(
    targetMs: Long,
    deferUntilReady: Boolean,
    maxDeferredStepMs: Long,
    keysAt: (Long) -> List<HomeAstroKey<*>>
): Long {
    val holder = remember { ReadyTimeHolder() }
    val currentKeysAt by rememberUpdatedState(keysAt)
    holder.version.intValue // Recompose when a deferred bucket becomes ready.
    val shown = holder.shownMs
    val adoptNow = shown == null ||
        !deferUntilReady ||
        targetMs <= shown ||
        targetMs - shown > maxDeferredStepMs ||
        keysAt(targetMs).all { HomeAstroMemo.isReady(it) }
    return if (adoptNow) {
        holder.shownMs = targetMs
        targetMs
    } else {
        LaunchedEffect(targetMs) {
            val keys = currentKeysAt(targetMs)
            withContext(Dispatchers.Default) {
                keys.forEach { HomeAstroMemo.get(it) }
            }
            holder.shownMs = targetMs
            holder.version.intValue++
        }
        shown!!
    }
}

/** Mirrors the identical bucket helpers of the three Home variants. */
internal object HomeAstroKeys {
    const val MOON_REFRESH_MS = 60_000L
    const val CATALOG_REFRESH_MS = 60_000L
    const val ECLIPSE_REFRESH_MS = 3_600_000L
    const val ECLIPSE_COUNT = 3

    fun bucketMs(timeMs: Long, isSimulation: Boolean, bucketMs: Long): Long =
        if (isSimulation || bucketMs <= 0L) timeMs else timeMs - java.lang.Math.floorMod(timeMs, bucketMs)

    fun catalogKeys(catalogKeyMs: Long, state: MainUiState, isFa: Boolean): List<HomeAstroKey<*>> {
        val jd = TimeEngine.getJulianDate(catalogKeyMs)
        val location = state.userLocation
        return listOf(
            CatalogObjectsKey(jd),
            TonightEventsKey(jd, location.latitude, location.longitude, isFa, ISSEngine.cachedTLE)
        )
    }

    fun moonKeys(moonKeyMs: Long, state: MainUiState): List<HomeAstroKey<*>> =
        listOf(MoonKey(TimeEngine.getJulianDate(moonKeyMs), state.userLocation.latitude, state.userLocation.longitude))

    fun eclipseKeys(eclipseKeyMs: Long, state: MainUiState): List<HomeAstroKey<*>> {
        val location = state.userLocation
        return listOf(
            EclipseSeriesKey(
                nowMs = eclipseKeyMs,
                latitude = location.latitude,
                longitude = location.longitude,
                elevationM = location.elevationMeters,
                timezoneId = location.timezoneId,
                calendarSystem = state.calendarSystem,
                count = ECLIPSE_COUNT
            )
        )
    }

    /**
     * Every key Home's first composition asks for, in the order the warm-up should compute them
     * (biggest first). [liveTimeMs] is the live clock's current value, which is what Home reads first.
     */
    fun forHome(state: MainUiState, liveTimeMs: Long): List<HomeAstroKey<*>> {
        val isSimulation = state.timeMachineState.mode == TimeMachineMode.SIMULATION
        val activeTimeMs = if (isSimulation) state.timeMachineState.simulationTimeMs else liveTimeMs
        val isFa = state.language == AppLanguage.PERSIAN
        return eclipseKeys(bucketMs(activeTimeMs, isSimulation, ECLIPSE_REFRESH_MS), state) +
            catalogKeys(bucketMs(activeTimeMs, isSimulation, CATALOG_REFRESH_MS), state, isFa) +
            moonKeys(bucketMs(activeTimeMs, isSimulation, MOON_REFRESH_MS), state)
    }
}
