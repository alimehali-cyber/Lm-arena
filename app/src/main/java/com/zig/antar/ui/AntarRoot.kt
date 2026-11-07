package com.zig.antar.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.AppUiStyle
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.theme.RedElevation
import com.alijafari.red.astronomy.ui.theme.RedSurface
import com.alijafari.red.astronomy.ui.theme.RedTheme
import com.alijafari.red.astronomy.ui.theme.RedTypography
import com.zig.antar.config.AntarCameraMode
import com.zig.antar.config.AntarObjectMode
import com.zig.antar.config.AntarPreset
import com.zig.antar.config.AntarSettingsStore
import com.zig.antar.config.AntarSimulationSettings
import com.zig.antar.config.AntarTelemetry
import com.zig.antar.gesture.AntarGestureController
import com.zig.antar.render.AntarEngineHandle
import com.zig.antar.render.AntarWebEngineView
import com.zig.gravity.ui.ImmersiveScreenState
import kotlinx.coroutines.delay

/**
 * Full-bleed immersive laboratory screen for **ZIG's Blackhole** (`سیاه‌چاله ZIG`).
 *
 * - Owns the full screen via [ImmersiveScreenState] (no top header bar, no back button; system back
 *   collapses the control sheet first, then returns to the Lab tab).
 * - Adapts chrome geometry, surfaces, and contrast-safe inks across [AppUiStyle.DEFAULT],
 *   [AppUiStyle.SPATIAL], and [AppUiStyle.CYBER], as well as [ThemeMode.LIGHT], [ThemeMode.DARK],
 *   and [ThemeMode.WINE], in both English and Persian (`Vazirmatn` + RTL + `tnum` numerals).
 */
@Composable
fun AntarRoot(
    onBack: () -> Unit,
    language: AppLanguage = AppLanguage.ENGLISH,
    uiStyle: AppUiStyle = AppUiStyle.DEFAULT,
    themeMode: ThemeMode = ThemeMode.DARK,
    modifier: Modifier = Modifier
) {
    val isPersian = language == AppLanguage.PERSIAN
    val context = LocalContext.current
    val density = LocalDensity.current
    val viewConfig = LocalViewConfiguration.current
    val reducedMotion = remember(context) { isAnimatorReducedMotion(context) }

    // Register immersive mode so the floating bottom nav hides and background sky renderers sleep.
    DisposableEffect(Unit) {
        ImmersiveScreenState.enter()
        onDispose { ImmersiveScreenState.exit() }
    }

    // Keep system status/navigation bar icons light over the dark relativistic cosmos; restore on exit.
    val activity = remember(context) { context.findHostActivity() }
    DisposableEffect(activity) {
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        val prevLightStatus = controller?.isAppearanceLightStatusBars
        val prevLightNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (prevLightStatus != null) controller.isAppearanceLightStatusBars = prevLightStatus
            if (prevLightNav != null) controller.isAppearanceLightNavigationBars = prevLightNav
        }
    }

    val appContext = context.applicationContext
    var settings by remember { mutableStateOf(AntarSettingsStore.load(appContext)) }
    var telemetry by remember { mutableStateOf(AntarTelemetry()) }
    var showUi by rememberSaveable { mutableStateOf(true) }
    var panelOpen by rememberSaveable { mutableStateOf(false) }
    var selectedTabOrdinal by rememberSaveable { mutableIntStateOf(AntarControlTab.SCENE.ordinal) }
    val selectedTab = AntarControlTab.entries.getOrElse(selectedTabOrdinal) { AntarControlTab.SCENE }
    var loadingTimedOut by remember { mutableStateOf(false) }

    val engineHandle = remember { AntarEngineHandle() }
    val gestureController = remember { AntarGestureController() }
    var inertiaEpoch by remember { mutableIntStateOf(0) }

    // System back closes the expanded sheet first, then exits ZIG's Blackhole to Lab.
    BackHandler(enabled = true) {
        if (panelOpen) {
            panelOpen = false
        } else {
            AntarSettingsStore.save(appContext, settings)
            onBack()
        }
    }

    // Safety fallback so loading indicator never lingers past 2.8 seconds.
    LaunchedEffect(Unit) {
        delay(2800L)
        loadingTimedOut = true
    }

    // Persist settings whenever user changes controls (debounced slightly to avoid disk churn on sliders).
    LaunchedEffect(settings) {
        delay(350L)
        AntarSettingsStore.save(appContext, settings)
    }

    val layoutDir = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr
    val chrome = rememberAntarChromeTokens(uiStyle = uiStyle, themeMode = themeMode)

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(rememberCosmicBackdropColor(themeMode))
                .testTag("antar_root")
        ) {
            val containerWidth = maxWidth
            val containerHeight = maxHeight
            val viewportWidthDp = containerWidth.value
            val viewportHeightDp = containerHeight.value
            val isWideLandscape = containerWidth >= 680.dp && containerWidth > containerHeight
            val sideDockWidthDp = 356f

            // Smooth optical framing offset when the control sheet opens so the photon ring stays centered
            // in the unobstructed portion of the screen.
            val targetDockShiftXDp = if (showUi && panelOpen && isWideLandscape) {
                if (isPersian) sideDockWidthDp * 0.42f else -sideDockWidthDp * 0.42f
            } else {
                0f
            }
            val targetDockShiftYDp = if (showUi && panelOpen && !isWideLandscape) {
                -viewportHeightDp * 0.16f
            } else {
                0f
            }

            val animatedDockShiftXDp by animateFloatAsState(
                targetValue = targetDockShiftXDp,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "AntarDockShiftX"
            )
            val animatedDockShiftYDp by animateFloatAsState(
                targetValue = targetDockShiftYDp,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "AntarDockShiftY"
            )

            // Sync parameter changes to the WebGL2 engine.
            LaunchedEffect(settings) {
                engineHandle.syncSettings(
                    settings = settings,
                    panXDp = gestureController.panOffsetXDp,
                    panYDp = gestureController.panOffsetYDp + animatedDockShiftYDp.toDouble(),
                    dockShiftDp = animatedDockShiftXDp.toDouble(),
                    yawRad = gestureController.lookYawRad,
                    pitchRad = gestureController.lookPitchRad
                )
            }

            // Sync smooth dock framing animation without touching simulation parameters.
            LaunchedEffect(animatedDockShiftXDp, animatedDockShiftYDp) {
                engineHandle.syncCameraFast(
                    settings = settings,
                    panXDp = gestureController.panOffsetXDp,
                    panYDp = gestureController.panOffsetYDp + animatedDockShiftYDp.toDouble(),
                    dockShiftDp = animatedDockShiftXDp.toDouble(),
                    yawRad = gestureController.lookYawRad,
                    pitchRad = gestureController.lookPitchRad
                )
            }

            // Frame-synchronized inertial glide loop; sleeps completely when velocity reaches zero.
            val latestDockX by rememberUpdatedState(animatedDockShiftXDp)
            val latestDockY by rememberUpdatedState(animatedDockShiftYDp)
            LaunchedEffect(inertiaEpoch, reducedMotion, settings.inertialGlide) {
                if (reducedMotion || !settings.inertialGlide) return@LaunchedEffect
                var lastFrameNs = 0L
                while (gestureController.hasActiveInertia) {
                    val frameNs = withFrameNanos { it }
                    if (lastFrameNs != 0L) {
                        val dtSec = ((frameNs - lastFrameNs) * 1e-9).coerceIn(1.0 / 240.0, 0.08)
                        val next = gestureController.tickInertia(
                            settings = settings,
                            dtSec = dtSec,
                            plungeActive = telemetry.plungeActive || settings.cameraMode == AntarCameraMode.PLUNGE,
                            reducedMotion = reducedMotion
                        )
                        settings = next
                        engineHandle.syncCameraFast(
                            settings = next,
                            panXDp = gestureController.panOffsetXDp,
                            panYDp = gestureController.panOffsetYDp + latestDockY.toDouble(),
                            dockShiftDp = latestDockX.toDouble(),
                            yawRad = gestureController.lookYawRad,
                            pitchRad = gestureController.lookPitchRad
                        )
                    }
                    lastFrameNs = frameNs
                }
            }

            // 1. Hardware-accelerated WebGL2 Kerr-Schild Ray Tracer
            AntarWebEngineView(
                initialSettings = settings,
                handle = engineHandle,
                onTelemetryUpdate = { snap -> telemetry = snap },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Native Multi-Touch Gesture Surface (1-finger orbit, 2-finger pinch/pan/roll, double-tap)
            val latestSettings by rememberUpdatedState(settings)
            val latestTelemetry by rememberUpdatedState(telemetry)
            val viewportWidthPx = with(density) { containerWidth.toPx() }
            val viewportHeightPx = with(density) { containerHeight.toPx() }
            val edgeGuardPx = with(density) { 20.dp.toPx() }
            val touchSlopPx = viewConfig.touchSlop
            val doubleTapRadiusPx = with(density) { 42.dp.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("antar_gesture_surface")
                    .pointerInput(viewportWidthPx, viewportHeightPx, density.density) {
                        var lastTapUpMs = 0L
                        var lastTapPos = Offset.Zero

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (gestureController.shouldIgnoreEdgeTouch(
                                    startXPx = down.position.x,
                                    viewportWidthPx = viewportWidthPx,
                                    edgeGuardPx = edgeGuardPx
                                )
                            ) {
                                return@awaitEachGesture
                            }

                            val downTimeMs = down.uptimeMillis
                            val downPos = down.position
                            var lastUpTimeMs = downTimeMs
                            var maxPointers = 1
                            var singleFingerDistancePx = 0f
                            var lastEventTimeMs = downTimeMs

                            val handoff = gestureController.onGestureStart(
                                settings = latestSettings,
                                liveAzimuthDeg = latestTelemetry.cameraAzimuthDeg,
                                liveInclinationDeg = latestTelemetry.cameraInclinationDeg,
                                liveDistanceM = latestTelemetry.cameraDistanceM
                            )
                            if (handoff != latestSettings) {
                                settings = handoff
                                engineHandle.syncCameraFast(
                                    settings = handoff,
                                    panXDp = gestureController.panOffsetXDp,
                                    panYDp = gestureController.panOffsetYDp + latestDockY.toDouble(),
                                    dockShiftDp = latestDockX.toDouble(),
                                    yawRad = gestureController.lookYawRad,
                                    pitchRad = gestureController.lookPitchRad
                                )
                            }

                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) {
                                    event.changes.firstOrNull()?.let { lastUpTimeMs = it.uptimeMillis }
                                    break
                                }

                                gestureController.onPointerCountChanged(pressed.size)
                                if (pressed.size > maxPointers) maxPointers = pressed.size

                                val nowMs = pressed.first().uptimeMillis
                                val dtSec = ((nowMs - lastEventTimeMs).coerceAtLeast(4L)) / 1000.0
                                lastEventTimeMs = nowMs
                                val plungeNow = latestTelemetry.plungeActive ||
                                    latestSettings.cameraMode == AntarCameraMode.PLUNGE

                                if (pressed.size == 1) {
                                    val change: PointerInputChange = pressed[0]
                                    val delta = change.positionChange()
                                    singleFingerDistancePx += delta.getDistance()
                                    if (singleFingerDistancePx > touchSlopPx * 0.42f) {
                                        val updated = gestureController.onSingleFingerDrag(
                                            settings = latestSettings,
                                            dxPx = delta.x,
                                            dyPx = delta.y,
                                            dtSec = dtSec,
                                            viewportWidthPx = viewportWidthPx,
                                            viewportHeightPx = viewportHeightPx,
                                            plungeActive = plungeNow
                                        )
                                        settings = updated
                                        engineHandle.syncCameraFast(
                                            settings = updated,
                                            panXDp = gestureController.panOffsetXDp,
                                            panYDp = gestureController.panOffsetYDp + latestDockY.toDouble(),
                                            dockShiftDp = latestDockX.toDouble(),
                                            yawRad = gestureController.lookYawRad,
                                            pitchRad = gestureController.lookPitchRad
                                        )
                                        change.consume()
                                    }
                                } else {
                                    val zoom = event.calculateZoom()
                                    val panPx = event.calculatePan()
                                    val rotDeg = event.calculateRotation()
                                    val panDxDp = panPx.x / density.density
                                    val panDyDp = panPx.y / density.density
                                    val updated = gestureController.onTwoFingerTransform(
                                        settings = latestSettings,
                                        zoomChange = zoom,
                                        panDxDp = panDxDp,
                                        panDyDp = panDyDp,
                                        rotationDeg = rotDeg,
                                        viewportWidthDp = viewportWidthDp,
                                        viewportHeightDp = viewportHeightDp,
                                        plungeActive = plungeNow
                                    )
                                    settings = updated
                                    engineHandle.syncCameraFast(
                                        settings = updated,
                                        panXDp = gestureController.panOffsetXDp,
                                        panYDp = gestureController.panOffsetYDp + latestDockY.toDouble(),
                                        dockShiftDp = latestDockX.toDouble(),
                                        yawRad = gestureController.lookYawRad,
                                        pitchRad = gestureController.lookPitchRad
                                    )
                                    event.changes.forEach { ch ->
                                        if (ch.positionChange() != Offset.Zero) ch.consume()
                                    }
                                }
                            }

                            val isCleanSingleTap = maxPointers == 1 &&
                                (lastUpTimeMs - downTimeMs) < 260L &&
                                singleFingerDistancePx < touchSlopPx * 1.6f

                            if (isCleanSingleTap) {
                                val dtTap = lastUpTimeMs - lastTapUpMs
                                if (dtTap in 35L..330L && (downPos - lastTapPos).getDistance() < doubleTapRadiusPx) {
                                    lastTapUpMs = 0L
                                    val recentered = gestureController.onDoubleTap(latestSettings)
                                    settings = recentered
                                    engineHandle.syncSettings(
                                        settings = recentered,
                                        panXDp = gestureController.panOffsetXDp,
                                        panYDp = gestureController.panOffsetYDp + latestDockY.toDouble(),
                                        dockShiftDp = latestDockX.toDouble(),
                                        yawRad = gestureController.lookYawRad,
                                        pitchRad = gestureController.lookPitchRad
                                    )
                                } else {
                                    lastTapUpMs = lastUpTimeMs
                                    lastTapPos = downPos
                                    gestureController.stopInertia()
                                }
                            } else {
                                gestureController.onGestureEnd(
                                    settings = latestSettings,
                                    reducedMotion = reducedMotion
                                )
                                if (gestureController.hasActiveInertia) {
                                    inertiaEpoch++
                                }
                            }
                        }
                    }
            )

            // 3. Safe-area inset floating HUD + Quick Dock + 4-Tab Control Panel
            val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(safeInsets)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                AntarFloatingStatusOverlay(
                    settings = settings,
                    telemetry = telemetry,
                    showUi = showUi,
                    isPersian = isPersian,
                    chrome = chrome,
                    onToggleShowUi = { showUi = !showUi },
                    onExitPlunge = {
                        gestureController.clearTransientOffsets()
                        settings = settings.copy(cameraMode = AntarCameraMode.ORBIT).normalize()
                    },
                    onStopTour = {
                        settings = settings.copy(cameraMode = AntarCameraMode.ORBIT).normalize()
                    },
                    onRestartBinary = {
                        settings = settings.copy(paused = false)
                        engineHandle.restartBinary()
                    },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Shader compilation subtle progress veil
                AnimatedVisibility(
                    visible = !telemetry.engineReady && telemetry.errorMessage == null && !loadingTimedOut,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    RedSurface(
                        shape = chrome.cardShape,
                        color = chrome.surfaceColor,
                        border = BorderStroke(1.dp, chrome.borderColor),
                        elevation = RedElevation.popover
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = chrome.accentColor,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = AntarStrings.loadingShader(isPersian),
                                style = RedTypography.labelMedium,
                                color = RedTheme.colors.textPrimary
                            )
                        }
                    }
                }

                // WebGL2 error fallback card
                if (telemetry.errorMessage != null) {
                    RedSurface(
                        shape = chrome.cardShape,
                        color = chrome.surfaceColor,
                        border = BorderStroke(1.dp, RedTheme.colors.statusError.copy(alpha = 0.6f)),
                        elevation = RedElevation.modal,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(340.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = RedTheme.colors.statusError,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = AntarStrings.webglErrorTitle(isPersian),
                                style = RedTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = RedTheme.colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = telemetry.errorMessage.orEmpty(),
                                style = RedTypography.bodySmall,
                                color = RedTheme.colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Quick Bottom Dock (when UI visible and full sheet collapsed)
                AnimatedVisibility(
                    visible = showUi && !panelOpen,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    AntarQuickDockBar(
                        settings = settings,
                        isPersian = isPersian,
                        chrome = chrome,
                        onOpenPresets = {
                            selectedTabOrdinal = AntarControlTab.SCENE.ordinal
                            panelOpen = true
                        },
                        onCycleCameraMode = {
                            val allowed = if (settings.objectMode == AntarObjectMode.BINARY_MERGER) {
                                listOf(AntarCameraMode.ORBIT, AntarCameraMode.CINEMATIC, AntarCameraMode.TOUR)
                            } else {
                                AntarCameraMode.entries
                            }
                            val nextIdx = (allowed.indexOf(settings.cameraMode) + 1) % allowed.size
                            gestureController.clearTransientOffsets()
                            settings = settings.copy(cameraMode = allowed[nextIdx], activePreset = null).normalize()
                        },
                        onTogglePause = {
                            settings = settings.copy(paused = !settings.paused)
                        },
                        onResetView = {
                            gestureController.clearTransientOffsets()
                            val reset = settings.resetCameraView()
                            settings = reset
                            engineHandle.syncSettings(
                                settings = reset,
                                panXDp = 0.0,
                                panYDp = animatedDockShiftYDp.toDouble(),
                                dockShiftDp = animatedDockShiftXDp.toDouble(),
                                yawRad = 0.0,
                                pitchRad = 0.0
                            )
                        },
                        onOpenControls = { panelOpen = true }
                    )
                }

                // Full 4-Tab Control Sheet (bottom sheet in portrait, side sheet in wide landscape)
                AnimatedVisibility(
                    visible = showUi && panelOpen,
                    enter = fadeIn() + slideInVertically { it / 3 },
                    exit = fadeOut() + slideOutVertically { it / 3 },
                    modifier = Modifier.align(
                        if (isWideLandscape) Alignment.CenterEnd else Alignment.BottomCenter
                    )
                ) {
                    AntarControlPanel(
                        settings = settings,
                        telemetry = telemetry,
                        selectedTab = selectedTab,
                        onSelectTab = { selectedTabOrdinal = it.ordinal },
                        onUpdateSettings = { next -> settings = next },
                        onApplyPreset = { preset: AntarPreset ->
                            gestureController.clearTransientOffsets()
                            settings = preset.applyTo(settings)
                        },
                        onResetView = {
                            gestureController.clearTransientOffsets()
                            settings = settings.resetCameraView()
                        },
                        onResetAll = {
                            gestureController.clearTransientOffsets()
                            AntarSettingsStore.clear(appContext)
                            settings = AntarSimulationSettings.DEFAULT
                        },
                        onRestartBinary = {
                            settings = settings.copy(paused = false)
                            engineHandle.restartBinary()
                        },
                        onClosePanel = { panelOpen = false },
                        isPersian = isPersian,
                        chrome = chrome,
                        maxHeight = if (isWideLandscape) (containerHeight - 28.dp) else (containerHeight * 0.50f),
                        modifier = if (isWideLandscape) {
                            Modifier.width(sideDockWidthDp.dp)
                        } else {
                            Modifier.fillMaxWidth()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberCosmicBackdropColor(themeMode: ThemeMode): Color = remember(themeMode) {
    // The scene WebView can briefly expose transparent pixels while its WebGL surface is starting.
    // Keep that fallback uniformly black; a radial gradient reads as a blurry orb and can be
    // mistaken for the black hole itself.
    when (themeMode) {
        ThemeMode.WINE -> Color(0xFF030102)
        ThemeMode.LIGHT -> Color(0xFF02040A)
        ThemeMode.DARK -> Color(0xFF010205)
    }
}

private fun Context.findHostActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun isAnimatorReducedMotion(context: Context): Boolean = runCatching {
    val scale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    )
    scale <= 0.01f
}.getOrDefault(false)
