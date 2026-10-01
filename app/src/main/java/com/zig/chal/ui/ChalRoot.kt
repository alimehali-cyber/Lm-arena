package com.zig.chal.ui

import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import android.provider.Settings
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alijafari.red.astronomy.ui.theme.RedControlHeight
import com.alijafari.red.astronomy.ui.theme.RedCornerRadius
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedTheme
import com.zig.chal.config.ChalCameraConfig
import com.zig.chal.config.ChalFeatures
import com.zig.chal.config.ChalFormatting
import com.zig.chal.config.ChalPresetName
import com.zig.chal.config.ChalSimulationParams
import com.zig.chal.config.ChalXapkScenario
import com.zig.chal.config.XapkSimulationSettingsStore
import com.zig.chal.render.ChalBenchmark
import com.zig.chal.render.ChalCamera
import com.zig.chal.render.ChalFallbackReason
import com.zig.chal.render.ChalSurfaceHost
import com.zig.gravity.ui.ImmersiveScreenState
import kotlinx.coroutines.delay

/** How long the chrome stays up after the last touch before it fades out and hands back the screen. */
private const val CHROME_IDLE_TIMEOUT_MS = 20_000L

/**
 * Root screen for the Chal Kerr black-hole laboratory.
 *
 * Hosts the XAPK's native Vulkan SurfaceView on capable ARM64 devices and retains the Chal GLES
 * renderer as a capability-gated fallback. The Lab UI maps the XAPK's physical presets, parameter
 * block, camera gestures, and telemetry while preserving the app's bilingual chrome.
 *
 * Full-screen behaviour: the screen claims [ImmersiveScreenState] so the host shell drops its own
 * top bar and navigation while a simulator is open — without that, the app's "Lab" header sat above
 * the simulator's own header and the status-bar inset was applied twice.
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
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var reducedMotionEnabled by remember(context) { mutableStateOf(isSystemMotionReduced(context)) }

    BackHandler(enabled = true, onBack = onBack)

    // A full-screen simulator owns the display: hide the app's floating navigation bar and header
    // exactly the way the app's other immersive simulators do (app-level state, not Chal code).
    DisposableEffect(Unit) {
        ImmersiveScreenState.enter()
        onDispose { ImmersiveScreenState.exit() }
    }

    // Start from the XAPK's persisted `bh.params` state and hardware-based quality default.
    val settingsStore = remember(context.applicationContext) {
        XapkSimulationSettingsStore(context.applicationContext)
    }
    val initialParams = remember(settingsStore) { settingsStore.load() }

    val surfaceView = remember(context.applicationContext, initialParams) {
        ChalSurfaceHost(context, initialParams)
    }

    // The active backend can be replaced in place when a failed native load is retried, so it is
    // held as state rather than captured once.
    var renderer by remember { mutableStateOf(surfaceView.renderer) }
    var usingNative by remember { mutableStateOf(surfaceView.isUsingXapkRenderer) }
    var fallbackReason by remember { mutableStateOf(surfaceView.fallbackReason) }
    var fallbackDetail by remember { mutableStateOf(surfaceView.fallbackDetail) }

    var params by remember { mutableStateOf(initialParams) }
    var snapshot by remember { mutableStateOf(renderer.snapshot()) }
    var showUi by remember { mutableStateOf(true) }
    var panelExpanded by remember { mutableStateOf(true) }
    var bannerDismissed by remember { mutableStateOf(false) }
    var errorDismissed by remember { mutableStateOf(false) }
    var hintVisible by remember { mutableStateOf(true) }
    var lastInteractionAt by remember { mutableStateOf(SystemClock.elapsedRealtime()) }

    /** Any deliberate touch pushes the auto-hide timer back. */
    fun noteInteraction() {
        lastInteractionAt = SystemClock.elapsedRealtime()
    }

    // Push UI-side parameter edits to the render thread. Keep the live orientation when a physical
    // slider/preset changes so a slightly stale 5 Hz HUD snapshot cannot snap the camera backwards.
    fun applyParams(updated: ChalSimulationParams, preserveCamera: Boolean = true) {
        noteInteraction()
        val applied = if (preserveCamera) {
            val camera = surfaceView.captureCamera()
            val distance = if (updated.zoom != params.zoom) updated.zoom else camera.distance.toDouble()
            updated.withCamera(camera.yaw.toDouble(), camera.pitch.toDouble(), distance)
        } else {
            updated
        }
        params = applied
        renderer.updateParams(applied)
    }

    fun startCinematic(mode: ChalCamera.CinematicMode) {
        val systemReducedMotion = isSystemMotionReduced(context)
        reducedMotionEnabled = systemReducedMotion
        surfaceView.startCinematic(mode, reducedMotion = systemReducedMotion)
    }

    val latestParams = rememberUpdatedState(params)

    fun capturedSessionParams(): ChalSimulationParams {
        val camera = surfaceView.captureCamera()
        return latestParams.value.withCamera(
            camera.yaw.toDouble(),
            camera.pitch.toDouble(),
            camera.distance.toDouble()
        )
    }

    // One write per physical-control interaction burst; camera yaw/pitch are captured on pause/exit
    // and are intentionally excluded from this debounce key because auto-spin can change them forever.
    LaunchedEffect(params.copy(cameraYaw = 0.0, cameraPitch = 0.0)) {
        delay(750)
        settingsStore.save(latestParams.value)
    }
    DisposableEffect(Unit) {
        onDispose { settingsStore.save(capturedSessionParams()) }
    }

    // Both the XAPK SurfaceView and the GLES fallback follow the host lifecycle. The Lifecycle
    // registry replays the current state to a new observer, so the observer alone drives resume —
    // calling onResume() here as well used to start the native engine twice.
    DisposableEffect(lifecycleOwner) {
        var surfaceResumed = false
        fun resumeSurface() {
            if (!surfaceResumed) {
                surfaceResumed = true
                surfaceView.onResume()
            }
        }

        val observer = LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val systemReducedMotion = isSystemMotionReduced(context)
                    reducedMotionEnabled = systemReducedMotion
                    resumeSurface()
                    if (systemReducedMotion) surfaceView.stopCinematic()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    surfaceResumed = false
                    settingsStore.save(capturedSessionParams())
                    surfaceView.onPause()
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) resumeSurface()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            surfaceView.onPause()
            surfaceView.release()
        }
    }

    // Telemetry + cinematic state poll (5 Hz, same cadence the reference throttles its HUD to).
    LaunchedEffect(Unit) {
        while (true) {
            val live = renderer.snapshot()
            snapshot = live
            // While the renderer owns the parameter block (benchmark sweep or a cinematic), mirroring
            // it back into the panel would drag the controls around under the user's finger.
            val rendererOwnsParameters = live.benchmarkState == ChalBenchmark.State.RUNNING || live.isCinematic
            if (!rendererOwnsParameters) {
                val engineParams = renderer.params
                val camera = surfaceView.captureCamera()
                val liveParams = engineParams.withCamera(
                    camera.yaw.toDouble(),
                    camera.pitch.toDouble(),
                    camera.distance.toDouble()
                )
                if (liveParams != params) params = liveParams
            }
            delay(200)
        }
    }

    // A tap anywhere on the scene toggles the chrome on both backends; the hint retires on first use.
    SideEffect {
        surfaceView.onTap = {
            hintVisible = false
            showUi = !showUi
            if (showUi) noteInteraction()
        }
        surfaceView.onBackendChanged = {
            renderer = surfaceView.renderer
            usingNative = surfaceView.isUsingXapkRenderer
            fallbackReason = surfaceView.fallbackReason
            fallbackDetail = surfaceView.fallbackDetail
            snapshot = surfaceView.renderer.snapshot()
        }
    }

    // Auto-hide only when the simulator is running idle, never over a paused frame or a director
    // sequence, and never while the benchmark is measuring.
    LaunchedEffect(showUi, params.paused, snapshot.isCinematic, snapshot.benchmarkState) {
        if (!showUi || params.paused || snapshot.isCinematic) return@LaunchedEffect
        if (snapshot.benchmarkState == ChalBenchmark.State.RUNNING) return@LaunchedEffect
        while (true) {
            delay(1_000)
            if (SystemClock.elapsedRealtime() - lastInteractionAt >= CHROME_IDLE_TIMEOUT_MS) {
                showUi = false
                break
            }
        }
    }

    val fallbackNotice = fallbackReason.takeIf { it != ChalFallbackReason.NONE }
    val hardError = surfaceView.errorMessage
    val nativeRetryable = usingNative.not() && surfaceView.isNativeLoadRetryable

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
                // Legibility scrims: the controls keep their contrast over a bright accretion disk
                // without dimming the middle of the frame where the black hole is.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (panelExpanded) 260.dp else 110.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(WindowInsets.systemBars.asPaddingValues())
                        .padding(horizontal = RedSpacing.md, vertical = RedSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                ) {
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
                                text = if (isPersian) "آزمایشگاه نسبیتی سیاه‌چاله کر" else "Kerr Black Hole Laboratory",
                                color = Color.White.copy(alpha = 0.60f),
                                fontSize = 8.sp
                            )
                            Text(
                                text = ChalFormatting.backendLabel(isPersian, usingNative),
                                color = if (usingNative) {
                                    RedTheme.colors.statusSuccess.copy(alpha = 0.85f)
                                } else {
                                    RedTheme.colors.statusWarning.copy(alpha = 0.85f)
                                },
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(WindowInsets.systemBars.asPaddingValues())
                        .padding(horizontal = RedSpacing.md, vertical = RedSpacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                ) {
                    if (panelExpanded) {
                        ChalControlPanel(
                            params = params,
                            isPersian = isPersian,
                            isCinematic = snapshot.isCinematic,
                            isXapkRenderer = usingNative,
                            reducedMotionEnabled = reducedMotionEnabled,
                            onScenarioSelected = { scenario ->
                                surfaceView.stopCinematic()
                                applyParams(scenario.applyTo(params), preserveCamera = false)
                                surfaceView.resetScenarioPitch()
                            },
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
                                applyParams(
                                    params.copy(
                                        features = params.features.copy(rayTracingQuality = quality),
                                        performancePreset = ChalFeatures.matchesPreset(
                                            params.features.copy(rayTracingQuality = quality)
                                        )
                                    )
                                )
                            },
                            onStartBenchmark = {
                                noteInteraction()
                                surfaceView.startBenchmark()
                            },
                            onCancelBenchmark = {
                                noteInteraction()
                                surfaceView.cancelBenchmark()
                            },
                            onCommit = { noteInteraction() },
                            isBenchmarkRunning = snapshot.benchmarkState == ChalBenchmark.State.RUNNING,
                            benchmarkPreset = snapshot.benchmarkPreset?.let { preset ->
                                preset.label(isPersian)
                            },
                            benchmarkProgress = snapshot.benchmarkProgress,
                            benchmarkResults = snapshot.benchmarkResults,
                            benchmarkRecommendation = snapshot.benchmarkRecommendation,
                            maxContentHeight = if (isLandscape) 130.dp else 268.dp,
                            onStartCinematic = { tool ->
                                noteInteraction()
                                when (tool) {
                                    ChalCinematicTool.ORBIT -> startCinematic(ChalCamera.CinematicMode.ORBIT)
                                    ChalCinematicTool.DIVE -> startCinematic(ChalCamera.CinematicMode.DIVE)
                                }
                            },
                            modifier = Modifier.widthIn(max = 460.dp)
                        )
                    }

                    ChalCommandRail(
                        isPersian = isPersian,
                        isCinematic = snapshot.isCinematic,
                        isPaused = params.paused,
                        panelExpanded = panelExpanded,
                        onTogglePanel = {
                            noteInteraction()
                            panelExpanded = !panelExpanded
                        },
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
                        onTogglePause = { applyParams(params.copy(paused = !params.paused)) },
                        modifier = Modifier.widthIn(max = 460.dp)
                    )
                }
            } else {
                ChalChromeButton(
                    onClick = {
                        showUi = true
                        noteInteraction()
                    },
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

            if (hintVisible && showUi) {
                Text(
                    text = if (isPersian) {
                        "برای گردش بکشید · با دو انگشت بزرگ‌نمایی کنید · برای پنهان‌کردن رابط ضربه بزنید"
                    } else {
                        "Drag to orbit · Pinch to zoom · Tap to hide the interface"
                    },
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 9.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(RedCornerRadius.pill))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = RedSpacing.md, vertical = RedSpacing.sm)
                        .testTag("chal_gesture_hint")
                )
                LaunchedEffect(Unit) {
                    delay(6_000)
                    hintVisible = false
                }
            }

            // Backend notices. Both are dismissible and bilingual; the old build showed a red,
            // undismissable English string and offered no way to retry a transient load failure.
            when {
                hardError != null && !errorDismissed -> ChalNoticeCard(
                    isPersian = isPersian,
                    isError = true,
                    title = if (isPersian) "خطای موتور رندر" else "Renderer error",
                    body = hardError,
                    detail = null,
                    onDismiss = { errorDismissed = true },
                    onRetry = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(RedSpacing.lg)
                )

                fallbackNotice != null && !bannerDismissed -> ChalNoticeCard(
                    isPersian = isPersian,
                    isError = false,
                    title = if (isPersian) "موتور بومی فعال نیست" else "Native renderer unavailable",
                    body = fallbackReason.explanation(isPersian),
                    detail = fallbackDetail,
                    onDismiss = { bannerDismissed = true },
                    onRetry = if (nativeRetryable) {
                        {
                            noteInteraction()
                            if (surfaceView.retryNativeRenderer(params)) {
                                renderer = surfaceView.renderer
                                usingNative = surfaceView.isUsingXapkRenderer
                                fallbackReason = surfaceView.fallbackReason
                                fallbackDetail = surfaceView.fallbackDetail
                                bannerDismissed = false
                                snapshot = renderer.snapshot()
                            } else {
                                fallbackReason = surfaceView.fallbackReason
                                fallbackDetail = surfaceView.fallbackDetail
                            }
                        }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(WindowInsets.systemBars.asPaddingValues())
                        .padding(top = if (isLandscape) 56.dp else 120.dp)
                        .padding(horizontal = RedSpacing.lg)
                        .testTag("chal_fallback_banner")
                )
            }
        }
    }
}

private fun isSystemMotionReduced(context: Context): Boolean = try {
    val resolver = context.contentResolver
    listOf(
        Settings.Global.ANIMATOR_DURATION_SCALE,
        Settings.Global.TRANSITION_ANIMATION_SCALE,
        Settings.Global.WINDOW_ANIMATION_SCALE
    ).any { key -> Settings.Global.getFloat(resolver, key, 1f) <= 0f }
} catch (_: Exception) {
    false
}

/**
 * Recompute `performancePreset` from the feature matrix after a parameter edit, mirroring the
 * reference's `matchesPreset` badge behaviour.
 */
private fun derivePreset(params: ChalSimulationParams): ChalSimulationParams =
    params.copy(performancePreset = ChalFeatures.matchesPreset(params.features))

/** User-facing explanation of why the GLES fallback is presenting frames. */
private fun ChalFallbackReason.explanation(isPersian: Boolean): String = when (this) {
    ChalFallbackReason.NONE -> ""
    ChalFallbackReason.API_TOO_OLD -> if (isPersian) {
        "موتور بومی به اندروید ۱۰ یا جدیدتر نیاز دارد؛ نسخهٔ تقریبی GLES فعال است."
    } else {
        "The native engine needs Android 10 or newer, so the approximate GLES renderer is active."
    }

    ChalFallbackReason.NOT_ARM64 -> if (isPersian) {
        "موتور بومی فقط برای arm64-v8a ساخته شده است؛ نسخهٔ تقریبی GLES فعال است."
    } else {
        "The native engine is built for arm64-v8a only, so the approximate GLES renderer is active."
    }

    ChalFallbackReason.NO_VULKAN -> if (isPersian) {
        "این دستگاه ولکان ۱٫۱ را گزارش نمی‌کند؛ نسخهٔ تقریبی GLES فعال است."
    } else {
        "This device does not report Vulkan 1.1, so the approximate GLES renderer is active."
    }

    ChalFallbackReason.LIBRARY_LOAD_FAILED -> if (isPersian) {
        "بارگذاری کتابخانهٔ بومی ناموفق بود؛ نسخهٔ تقریبی GLES فعال است."
    } else {
        "The native library could not be loaded, so the approximate GLES renderer is active."
    }

    ChalFallbackReason.RUNTIME_FAILURE -> if (isPersian) {
        "موتور بومی هنگام اجرا با خطا روبه‌رو شد؛ نسخهٔ تقریبی GLES جایگزین شد."
    } else {
        "The native renderer failed at runtime; the approximate GLES fallback took over."
    }
}

@Composable
private fun ChalNoticeCard(
    isPersian: Boolean,
    isError: Boolean,
    title: String,
    body: String,
    detail: String?,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val accent = if (isError) RedTheme.colors.statusError else RedTheme.colors.statusWarning
    Column(
        modifier = modifier
            .widthIn(max = 420.dp)
            .clip(RoundedCornerShape(RedCornerRadius.lg))
            .background(Color.Black.copy(alpha = 0.88f))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(RedCornerRadius.lg))
            .padding(RedSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "  $title",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Box(
                modifier = Modifier
                    .size(RedControlHeight.compact)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onDismiss)
                    .testTag("chal_notice_dismiss"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = if (isPersian) "بستن" else "Dismiss",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Text(text = body, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)

        if (detail != null) {
            Text(
                text = detail,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        if (onRetry != null) {
            Row(
                modifier = Modifier
                    .heightIn(min = RedControlHeight.regular)
                    .clip(RoundedCornerShape(RedCornerRadius.sm))
                    .background(RedTheme.colors.accentRed.copy(alpha = 0.22f))
                    .border(1.dp, RedTheme.colors.accentRed.copy(alpha = 0.55f), RoundedCornerShape(RedCornerRadius.sm))
                    .clickable(role = Role.Button, onClick = onRetry)
                    .padding(horizontal = RedSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isPersian) "  تلاش دوباره برای موتور بومی" else "  Retry native renderer",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun ChalChromeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(RedControlHeight.regular)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Docked command rail: panel visibility, view reset (abort while a director sequence runs) and pause.
 *
 * Pause is offered on both backends: the native engine exposes `nativeOnPause`, so hiding the
 * control on the XAPK path (as the first port did) made the simulator look unpausable.
 */
@Composable
private fun ChalCommandRail(
    isPersian: Boolean,
    isCinematic: Boolean,
    isPaused: Boolean,
    panelExpanded: Boolean,
    onTogglePanel: () -> Unit,
    onReset: () -> Unit,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chal_command_rail"),
        horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
    ) {
        ChalRailButton(
            label = if (panelExpanded) {
                if (isPersian) "پنهان‌کردن کنترل‌ها" else "Hide controls"
            } else {
                if (isPersian) "نمایش کنترل‌ها" else "Show controls"
            },
            highlight = false,
            onClick = onTogglePanel,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (panelExpanded) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(16.dp)
            )
        }

        ChalRailButton(
            label = if (isCinematic) {
                if (isPersian) "توقف توالی" else "ABORT SEQ"
            } else {
                if (isPersian) "بازنشانی نما" else "Reset view"
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
            highlight = isPaused,
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
            .heightIn(min = RedControlHeight.regular)
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
            .clickable(role = Role.Button, onClick = onClick)
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
