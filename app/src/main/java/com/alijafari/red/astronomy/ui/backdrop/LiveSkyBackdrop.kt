package com.alijafari.red.astronomy.ui.backdrop

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import com.alijafari.red.astronomy.ui.rendering.SkySceneFrame
import com.alijafari.red.astronomy.ui.rendering.SkySceneModel
import com.alijafari.red.astronomy.ui.rendering.SkySceneRenderer
import com.zig.gravity.ui.ImmersiveScreenState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val LIVE_SKY_TAG = "LiveSkyBackdrop"

/** Test hook on the drawn backdrop node. It is not a visible control. */
const val LIVE_SKY_BACKDROP_TAG = "live_sky_backdrop"

/** Wall-clock tick while the backdrop is visible and the host is resumed. Drives twinkle and pulse phases only. */
private const val WALL_CLOCK_TICK_MS = 1_000L

/**
 * Non-interactive full-screen sky behind the app UI. It draws the same [SkySceneRenderer] pipeline as the Home
 * hero, through a [SkyBackdropViewport] that fills the window.
 *
 * Costs and guarantees:
 *  - Astronomy is recomputed only when the quantised minute changes, on [Dispatchers.Default]. The previous frame stays
 *    on screen until the new one is ready.
 *  - The 1 Hz clock runs only while [visible] is true and the host is RESUMED. Its value is read in the draw phase, so a
 *    tick redraws the backdrop but does not recompose it.
 *  - When [visible] is false, nothing is emitted, no clock runs and no computation runs.
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
    modifier: Modifier = Modifier
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Visibility is decided here, so only this composable recomposes when a tab or an immersive Lab feature changes.
    val visible = LiveSkyPolicy.isBackdropVisible(
        enabled = enabled,
        selectedTab = selectedTab,
        immersiveActive = ImmersiveScreenState.active
    )

    var wallClockMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lifecycle, visible) {
        if (!visible) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                wallClockMs = System.currentTimeMillis()
                delay(WALL_CLOCK_TICK_MS)
            }
        }
    }

    // Recomposes only when the quantised minute changes, not on every clock tick.
    val minuteState = remember(isSimulation, simulationTimeMs) {
        derivedStateOf {
            LiveSkyFrameRequest.resolveMinuteMs(isSimulation, simulationTimeMs, wallClockMs)
        }
    }
    val minuteMs: Long = minuteState.value
    // Equal requests do not restart the producer, so clock ticks inside one minute do no astronomy work.
    val request = LiveSkyFrameRequest(
        visible = visible,
        minuteMs = minuteMs,
        latitudeDeg = latitudeDeg,
        longitudeDeg = longitudeDeg,
        elevationM = elevationM
    )

    val frameState = produceState<SkySceneFrame?>(initialValue = null, request) {
        if (!request.needsCompute) {
            value = null
            return@produceState
        }
        try {
            value = withContext(Dispatchers.Default) {
                SkySceneModel.compute(
                    jd = TimeEngine.getJulianDate(request.minuteMs),
                    latitudeDeg = request.latitudeDeg,
                    longitudeDeg = request.longitudeDeg,
                    elevationM = request.elevationM
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Keep the previous frame. If there is none, the plain background shows.
            Log.w(LIVE_SKY_TAG, "Live Sky frame computation failed", e)
        }
    }
    val frame = frameState.value

    // Non-state flag. Writing it from the draw phase must not trigger recomposition.
    val renderHealth = remember { SkyBackdropRenderHealth() }

    if (visible && frame != null && !renderHealth.failed) {
        BoxWithConstraints(
            modifier = modifier
                .clipToBounds()
                .clearAndSetSemantics { }
                .testTag(LIVE_SKY_BACKDROP_TAG)
        ) {
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
                            frame = frame,
                            theme = theme,
                            frameTimeMs = wallClockMs,
                            panoramaActive = false
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

/** Per-composition draw-failure flag. It resets when the backdrop leaves and re-enters composition. */
private class SkyBackdropRenderHealth {
    var failed: Boolean = false
}
