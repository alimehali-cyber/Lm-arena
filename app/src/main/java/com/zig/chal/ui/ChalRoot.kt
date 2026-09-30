package com.zig.chal.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.provides
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alijafari.red.astronomy.ui.theme.RedCornerRadius
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedTheme
import com.zig.chal.config.ChalCameraConfig
import com.zig.chal.config.ChalFeatures
import com.zig.chal.config.ChalPresetName
import com.zig.chal.config.ChalRayTracingQuality
import com.zig.chal.config.ChalSimulationParams
import com.zig.chal.render.ChalCamera
import com.zig.chal.render.ChalRenderer
import com.zig.chal.render.ChalSurfaceView
import com.zig.gravity.ui.ImmersiveScreenState
import kotlinx.coroutines.delay

/**
 * Root screen for the Chal Kerr black-hole laboratory.
 *
 * The reference engine's page composition is reproduced here: the ray-marched canvas fills the
 * viewport, the telemetry HUD sits over it, and the control panel docks below with the
 * Reset/Pause command rail.
 */
@Composable
fun ChalRoot(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    startInPersian: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isPersian = startInPersian

    BackHandler(enabled = true, onBack = onBack)

    // A full-screen simulator owns the display: hide the app's floating navigation bar exactly the
    // way the app's other immersive simulators do (app-level state, not Gargantua code).
    DisposableEffect(Unit) {
        ImmersiveScreenState.enter()
        onDispose { ImmersiveScreenState.exit() }
    }

    val surfaceView = remember {
        ChalSurfaceView(context).apply {
            renderer.updateParams(ChalSimulationParams.DEFAULT_PARAMS)
        }
    }
    val renderer: ChalRenderer = surfaceView.renderer

    var params by remember { mutableStateOf(ChalSimulationParams.DEFAULT_PARAMS) }
    var snapshot by remember { mutableStateOf(renderer.snapshot()) }
    var showUi by remember { mutableStateOf(true) }

    // Push UI-side parameter edits to the GL thread.
    fun applyParams(updated: ChalSimulationParams) {
        params = updated
        renderer.updateParams(updated)
    }

    // GLSurfaceView follows the host lifecycle.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> surfaceView.onResume()
                Lifecycle.Event.ON_PAUSE -> surfaceView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        surfaceView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            surfaceView.onPause()
            surfaceView.releaseGl()
        }
    }

    // Telemetry + cinematic state poll (5 Hz, same cadence the reference throttles its HUD to).
    LaunchedEffect(Unit) {
        while (true) {
            snapshot = renderer.snapshot()
            // The camera owns zoom/autoSpin/paused while a cinematic runs; mirror it into the UI.
            val live = renderer.params
            if (live != params) params = live
            delay(200)
        }
    }

    SideEffect { surfaceView.onTap = { showUi = !showUi } }

    CompositionLocalProvider(
        LocalLayoutDirection provides if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("chal_root")
        ) {
            AndroidView(
                factory = { surfaceView },
                modifier = Modifier.fillMaxSize()
            )

            if (showUi) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WindowInsets.systemBars.asPaddingValues())
                        .padding(horizontal = RedSpacing.md, vertical = RedSpacing.sm),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // ---- Top chrome: back, title, telemetry, hide ------------------------
                    Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ChalChromeButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = if (isPersian) "بازگشت" else "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "CHAL",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = if (isPersian) "آزمایشگاه نسبیتی سیاه‌چاله کر" else "Kerr Black Hole Ray-Marcher",
                                    color = Color.White.copy(alpha = 0.60f),
                                    fontSize = 8.sp
                                )
                            }

                            ChalChromeButton(onClick = { showUi = false }) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = if (isPersian) "پنهان کردن رابط" else "Hide interface",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        ChalTelemetry(
                            snapshot = snapshot,
                            isPersian = isPersian,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }

                    // ---- Bottom chrome: control panel + command rail ---------------------
                    Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)) {
                        ChalControlPanel(
                            params = params,
                            isPersian = isPersian,
                            isCinematic = snapshot.isCinematic,
                            onParamsChange = { updated ->
                                applyParams(derivePreset(updated))
                            },
                            onPresetSelected = { preset ->
                                applyParams(
                                    params.copy(
                                        features = ChalFeatures.getPreset(preset),
                                        performancePreset = preset
                                    )
                                )
                            },
                            onQualitySelected = { quality ->
                                val features = params.features.copy(rayTracingQuality = quality)
                                applyParams(
                                    params.copy(
                                        features = features,
                                        performancePreset = ChalFeatures.matchesPreset(features)
                                    )
                                )
                            },
                            onStartBenchmark = { surfaceView.startBenchmark() },
                            onCancelBenchmark = { surfaceView.cancelBenchmark() },
                            isBenchmarkRunning = snapshot.benchmarkState == com.zig.chal.render.ChalBenchmark.State.RUNNING,
                            benchmarkPreset = snapshot.benchmarkPreset?.let { preset ->
                                if (isPersian) preset.label else preset.id
                            },
                            benchmarkProgress = snapshot.benchmarkProgress,
                            benchmarkResults = snapshot.benchmarkResults,
                            benchmarkRecommendation = snapshot.benchmarkRecommendation,
                            onStartCinematic = { tool ->
                                when (tool) {
                                    ChalCinematicTool.ORBIT -> surfaceView.startCinematic(
                                        ChalCamera.CinematicMode.ORBIT,
                                        reducedMotion = false
                                    )

                                    ChalCinematicTool.DIVE -> {
                                        applyParams(params.copy(autoSpin = 0.0))
                                        surfaceView.startCinematic(
                                            ChalCamera.CinematicMode.DIVE,
                                            reducedMotion = false
                                        )
                                    }
                                }
                            }
                        )

                        ChalCommandRail(
                            isPersian = isPersian,
                            isCinematic = snapshot.isCinematic,
                            isPaused = params.paused,
                            onReset = {
                                applyParams(
                                    params.copy(
                                        zoom = ChalCameraConfig.DEFAULT_ZOOM,
                                        autoSpin = ChalCameraConfig.DEFAULT_AUTO_SPIN,
                                        paused = false
                                    )
                                )
                                surfaceView.resetCamera()
                            },
                            onTogglePause = { applyParams(params.copy(paused = !params.paused)) }
                        )
                    }
                }
            } else {
                ChalChromeButton(
                    onClick = { showUi = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(WindowInsets.systemBars.asPaddingValues())
                        .padding(RedSpacing.md)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = if (isPersian) "نمایش رابط" else "Show interface",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            renderer.errorMessage?.let { message ->
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(RedSpacing.lg)
                        .clip(RoundedCornerShape(RedCornerRadius.lg))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .border(1.dp, RedTheme.colors.statusError.copy(alpha = 0.5f), RoundedCornerShape(RedCornerRadius.lg))
                        .padding(RedSpacing.lg)
                ) {
                    Text(text = message, color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Recompute `performancePreset` from the feature matrix after a parameter edit, mirroring the
 * reference's `matchesPreset` badge behaviour.
 */
private fun derivePreset(params: ChalSimulationParams): ChalSimulationParams =
    params.copy(performancePreset = ChalFeatures.matchesPreset(params.features))

@Composable
private fun ChalChromeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** Docked navigation rail: Reset (ABORT SEQ while cinematic) and Pause. */
@Composable
private fun ChalCommandRail(
    isPersian: Boolean,
    isCinematic: Boolean,
    isPaused: Boolean,
    onReset: () -> Unit,
    onTogglePause: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chal_command_rail"),
        horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
    ) {
        ChalRailButton(
            label = if (isCinematic) {
                if (isPersian) "توقف توالی" else "ABORT SEQ"
            } else {
                if (isPersian) "بازنشانی" else "Reset"
            },
            highlight = isCinematic,
            onClick = onReset,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = if (isCinematic) RedTheme.colors.accentRed else Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(16.dp)
            )
        }

        ChalRailButton(
            label = if (isPaused) {
                if (isPersian) "ادامه" else "Resume"
            } else {
                if (isPersian) "توقف" else "Pause"
            },
            highlight = false,
            onClick = onTogglePause,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ChalRailButton(
    label: String,
    highlight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(RedCornerRadius.md))
            .background(
                if (highlight) RedTheme.colors.accentRed.copy(alpha = 0.22f)
                else Color.Black.copy(alpha = 0.65f)
            )
            .border(
                1.dp,
                if (highlight) RedTheme.colors.accentRed.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(RedCornerRadius.md)
            )
            .clickable(onClick = onClick)
            .padding(vertical = RedSpacing.sm),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
        Text(
            text = "  $label",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black
        )
    }
}
