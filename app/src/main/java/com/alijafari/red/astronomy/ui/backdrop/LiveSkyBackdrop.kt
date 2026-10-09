package com.alijafari.red.astronomy.ui.backdrop

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import com.alijafari.red.astronomy.ui.rendering.SkySceneFrame
import com.alijafari.red.astronomy.ui.rendering.SkySceneModel
import com.alijafari.red.astronomy.ui.rendering.SkySceneRenderer
import com.alijafari.red.astronomy.ui.rendering.SkyTimeModel
import com.alijafari.red.astronomy.ui.skypanorama.SkyPanoramaFeature
import com.alijafari.red.astronomy.ui.skypanorama.SkyPanoramaLayer
import com.alijafari.red.astronomy.ui.skypanorama.SkyPanoramaState
import com.alijafari.red.astronomy.ui.skypanorama.SkyPanoramaStatus
import com.zig.gravity.ui.ImmersiveScreenState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

private const val LIVE_SKY_TAG = "LiveSkyBackdrop"

/** Test hook on the drawn backdrop node. It is not a visible control. */
const val LIVE_SKY_BACKDROP_TAG = "live_sky_backdrop"

/**
 * Non-interactive full-screen sky behind the app UI. It shows the same sky as the Home hero: the same effective instant,
 * the same observer, the same photographic panorama and the same [SkySceneRenderer] overlays.
 *
 * Time and location:
 *  - The effective instant is [SkyTimeModel.baseTimeMs] (simulation or live clock) plus the Home drag offset, read from
 *    [dragOffsetHours]. The drag offset includes its animated return to zero.
 *  - The live clock is the shared [liveTimeMs] from MainViewModel. This composable adds no clock of its own.
 *
 * Costs and guarantees:
 *  - When [visible] is false, this composable composes nothing: no clock subscription, no computation and no GL host.
 *  - Astronomy runs on [Dispatchers.Default] through [latestFrames]. Requests are throttled to about 30 per second, a
 *    superseded computation is cancelled, and the previous frame stays on screen until the next one is ready.
 *  - The panorama is the same [SkyPanoramaLayer] Home uses: the same asset, renderer and shaders, with its own GL
 *    context for this surface. It appears only when [SkyPanoramaFeature.isPresented] says it is ready, and otherwise the
 *    procedural sky stays on screen as the fallback.
 *  - If computing or drawing fails, the backdrop draws nothing and the plain app background shows through. Nothing crashes.
 *
 * Interaction is intentionally absent. The backdrop has no pointer input and no semantics, so taps reach the content above it.
 * The Home hero keeps its own interactive canvas.
 */
@Composable
fun LiveSkyBackdrop(
    enabled: Boolean,
    selectedTab: Int,
    isSimulation: Boolean,
    simulationTimeMs: Long,
    latitudeDeg: Double,
    longitudeDeg: Double,
    elevationM: Double,
    theme: SkyCanvasTheme,
    liveTimeMs: StateFlow<Long>,
    dragOffsetHours: StateFlow<Float>,
    modifier: Modifier = Modifier
) {
    // Visibility is decided here, so only this composable recomposes when a tab or an immersive Lab feature changes.
    val visible = LiveSkyPolicy.isBackdropVisible(
        enabled = enabled,
        selectedTab = selectedTab,
        immersiveActive = ImmersiveScreenState.active
    )
    // Nothing below is composed while hidden. Its clock subscription, its frame producer and its GL host
    // are all removed, so Live Sky OFF or an excluded screen leaves no work behind.
    if (!visible) return

    LiveSkyActiveBackdrop(
        isSimulation = isSimulation,
        simulationTimeMs = simulationTimeMs,
        latitudeDeg = latitudeDeg,
        longitudeDeg = longitudeDeg,
        elevationM = elevationM,
        theme = theme,
        liveTimeMs = liveTimeMs,
        dragOffsetHours = dragOffsetHours,
        modifier = modifier
    )
}

@Composable
private fun LiveSkyActiveBackdrop(
    isSimulation: Boolean,
    simulationTimeMs: Long,
    latitudeDeg: Double,
    longitudeDeg: Double,
    elevationM: Double,
    theme: SkyCanvasTheme,
    liveTimeMs: StateFlow<Long>,
    dragOffsetHours: StateFlow<Float>,
    modifier: Modifier
) {
    // Cheap state only. Neither value triggers astronomy here: the producer below reads the request.
    val liveTime by liveTimeMs.collectAsState()
    val dragOffset by dragOffsetHours.collectAsState()

    val request = LiveSkyFrameRequest(
        visible = true,
        baseTimeMs = SkyTimeModel.baseTimeMs(isSimulation, simulationTimeMs, liveTime),
        dragOffsetHours = dragOffset,
        latitudeDeg = latitudeDeg,
        longitudeDeg = longitudeDeg,
        elevationM = elevationM
    )
    val latestRequest = rememberUpdatedState(request)

    // One producer per subtree. It reads the latest request, coalesces requests, and cancels superseded work.
    var frame by remember { mutableStateOf<SkySceneFrame?>(null) }
    LaunchedEffect(Unit) {
        snapshotFlow { latestRequest.value }
            .latestFrames { req -> computeFrame(req) }
            .collect { computed -> frame = computed }
    }

    // Non-state flag. Writing it from the draw phase must not trigger recomposition.
    val renderHealth = remember { SkyBackdropRenderHealth() }

    val currentFrame = frame
    val panoramaEnabled = SkyPanoramaFeature.isEnabledFor(theme)
    var panoramaReady by remember { mutableStateOf(false) }
    val panoramaActive = SkyPanoramaFeature.isPresented(theme, panoramaReady)
    // Derived from the same frame that Home derives its panorama state from.
    val panoramaState = remember(currentFrame) { currentFrame?.let { SkyPanoramaState.fromSceneFrame(it) } }

    if (currentFrame != null && !renderHealth.failed) {
        Box(
            modifier = modifier
                .clipToBounds()
                .clearAndSetSemantics { }
                .testTag(LIVE_SKY_BACKDROP_TAG)
        ) {
            // The panorama fills the window and sits beneath the overlays, the same stacking order as the Home hero.
            if (panoramaEnabled && panoramaState != null) {
                SkyPanoramaLayer(
                    state = panoramaState,
                    modifier = Modifier.fillMaxSize(),
                    onStatusChanged = { status -> panoramaReady = status == SkyPanoramaStatus.READY }
                )
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val viewport = SkyBackdropViewport.fit(
                    windowWidthPx = constraints.maxWidth.toFloat(),
                    windowHeightPx = constraints.maxHeight.toFloat()
                )
                if (viewport.isDrawable) {
                    val canvasWidth = with(density) { viewport.virtualWidthPx.toDp() }
                    val canvasOffset = with(density) { viewport.offsetXPx.toDp() }
                    Canvas(
                        modifier = Modifier
                            .requiredSize(width = canvasWidth, height = maxHeight)
                            .offset(x = canvasOffset, y = 0.dp)
                    ) {
                        try {
                            SkySceneRenderer.drawSky(
                                drawScope = this,
                                frame = currentFrame,
                                theme = theme,
                                frameTimeMs = liveTime,
                                panoramaActive = panoramaActive
                            )
                        } catch (e: Exception) {
                            renderHealth.failed = true
                            Log.w(LIVE_SKY_TAG, "Live Sky draw failed; backdrop disabled until re-entered", e)
                        }
                    }
                }
            }
        }
    }
}

/**
 * One astronomy computation on [Dispatchers.Default]. A failure returns null, so the previous frame stays on screen.
 * Cancellation is always rethrown, so a superseded computation never reports a result.
 */
private suspend fun computeFrame(req: LiveSkyFrameRequest): SkySceneFrame? = try {
    withContext(Dispatchers.Default) {
        SkySceneModel.compute(
            jd = req.julianDate,
            latitudeDeg = req.latitudeDeg,
            longitudeDeg = req.longitudeDeg,
            elevationM = req.elevationM
        )
    }
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Log.w(LIVE_SKY_TAG, "Live Sky frame computation failed", e)
    null
}

/** Per-composition draw-failure flag. It resets when the backdrop leaves and re-enters composition. */
private class SkyBackdropRenderHealth {
    var failed: Boolean = false
}
