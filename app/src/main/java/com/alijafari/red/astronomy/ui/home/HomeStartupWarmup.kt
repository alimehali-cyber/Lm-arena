package com.alijafari.red.astronomy.ui.home

import android.content.Context
import com.alijafari.red.astronomy.data.repository.TleRepository
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.rendering.SkySceneModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Whether the first visible route needs Home's expensive first-frame data before it is revealed.
 * A notification/deep-link destination is allowed to render without waiting for unrelated Home work.
 * Tab 4 is Home, matching MainUiState's initial value and MainActivity's navigation wiring.
 */
internal fun shouldGateLaunchForHomeWarmup(state: MainUiState): Boolean =
    state.selectedTab == 4 &&
        state.selectedObjectForDetail == null &&
        !state.showSkyViewer &&
        !state.showEclipseWindow

/** Wall-clock deadline helpers for the splash's warm-up fallback. */
internal object HomeWarmupDeadline {
    const val MAX_WAIT_MS = 8_000L

    fun remainingMs(deadlineElapsedRealtimeMs: Long, nowElapsedRealtimeMs: Long): Long =
        (deadlineElapsedRealtimeMs - nowElapsedRealtimeMs).coerceAtLeast(0L)
}

/**
 * Computes everything Home's first composition needs, off the main thread, while the launch splash
 * plays, so the first Home frame is a cheap lookup instead of seconds of astronomy on the UI thread.
 *
 * It only ever fills [HomeAstroMemo] (and warms lazy, pure tables); it changes no state Home reads,
 * so Home's results are the same whether the warm-up ran, half ran or failed.
 */
internal object HomeStartupWarmup {

    /**
     * At most two worker threads, so the splash's UI and render threads keep their cores even on
     * a four-core phone.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val workers = Dispatchers.Default.limitedParallelism(2)

    /**
     * Runs the warm-up. [stateNow] and [liveTimeNow] are read again after each pass: if the inputs
     * moved meanwhile (a restored location arriving, say), the keys Home will now ask for are computed
     * too. At most three passes.
     */
    suspend fun run(context: Context, stateNow: () -> MainUiState, liveTimeNow: () -> Long) = coroutineScope {
        // The TLE store opens the database and loads the cached elements; the catalog's satellites
        // read it through SatelliteEngine's resolvers. Start it first, in parallel with the eclipses.
        val tleStore = async(workers) { TleRepository.getInstance(context.applicationContext) }

        var computed: List<HomeAstroKey<*>>? = null
        for (pass in 0 until 3) {
            val state = stateNow()
            val keys = HomeAstroKeys.forHome(state, liveTimeNow())
            if (keys == computed) break
            val jobs = keys.map { key ->
                async(workers) {
                    if (key is CatalogObjectsKey) tleStore.await()
                    HomeAstroMemo.get(key)
                }
            }
            if (computed == null) {
                // The hero sky's star table is a lazy pure table; building it here keeps it off the
                // first Home frame. The frame itself is discarded.
                val location = state.userLocation
                val moonKey = keys.filterIsInstance<MoonKey>().first()
                jobs.plus(async(workers) {
                    SkySceneModel.compute(moonKey.jd, location.latitude, location.longitude, location.elevationMeters)
                }).awaitAll()
            } else {
                jobs.awaitAll()
            }
            computed = keys
        }
        tleStore.await()
    }
}
