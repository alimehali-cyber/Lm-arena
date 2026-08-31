package com.alijafari.red.astronomy.sandbox.render.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.sandbox.physics.AstroPhysicsConstants
import com.alijafari.red.astronomy.sandbox.physics.GravitySandboxEngine
import com.alijafari.red.astronomy.sandbox.presets.SandboxPreset
import com.alijafari.red.astronomy.sandbox.presets.SandboxPresetCatalog
import com.alijafari.red.astronomy.sandbox.render.diagnostics.RenderDiagnosticsSnapshot
import com.alijafari.red.astronomy.sandbox.render.gl.QualityLevel
import com.alijafari.red.astronomy.sandbox.render.renderer.RenderTheme
import com.alijafari.red.astronomy.sandbox.render.scale.ScaleMode
import com.alijafari.red.astronomy.sandbox.snapshot.BodyRenderState
import com.alijafari.red.astronomy.sandbox.snapshot.DoubleBufferSnapshotManager
import com.alijafari.red.astronomy.sandbox.snapshot.SandboxRenderFrame
import com.alijafari.red.astronomy.sandbox.worker.SandboxPhysicsWorker
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.sqrt

/**
 * Gravity Sandbox 3D Viewport Screen (Phase 4).
 *
 * Immersive scientific workspace providing:
 * 1. Authoritative 3D OpenGL viewport with touch manipulation & raycasting.
 * 2. Visual Mode Toggles: Orbital Trails, Trajectory Prediction, Velocity & Accel Vectors, Barycenter.
 * 3. Body Inspector Card: Live velocity, acceleration, mass, radius, and distance to barycenter.
 * 4. Barycenter Telemetry: System center-of-mass coordinates and total active mass.
 * 5. Simulation & Time Step Controls: Real-time pause, resume, speed multipliers, and preset selection.
 */
@Composable
fun GravitySandboxScreen(
    uiState: MainUiState,
    onBackToLab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isFa = uiState.language == AppLanguage.PERSIAN

    // 1. Instantiate Physics & Snapshot Pipeline
    val snapshotManager = remember { DoubleBufferSnapshotManager() }
    val engine = remember { GravitySandboxEngine() }
    val physicsWorker = remember {
        SandboxPhysicsWorker(
            engine = engine,
            snapshotManager = snapshotManager
        ).apply {
            loadPreset(SandboxPresetCatalog.SUN_EARTH)
        }
    }

    var surfaceViewRef by remember { mutableStateOf<GravitySandboxSurfaceView?>(null) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var selectedPresetName by remember { mutableStateOf("Sun & Earth") }
    var activePreset by remember { mutableStateOf<SandboxPreset>(SandboxPresetCatalog.SUN_EARTH) }
    var selectedBodyId by remember { mutableStateOf<String?>(null) }
    var currentScaleMode by remember { mutableStateOf(ScaleMode.SOLAR_SYSTEM_COMPRESSED) }
    var diagnosticsSnapshot by remember { mutableStateOf(RenderDiagnosticsSnapshot()) }
    var latestSnapshot by remember { mutableStateOf<SandboxRenderFrame?>(null) }

    // Phase 4 Visual Feature Toggle States
    var isTrailsEnabled by remember { mutableStateOf(true) }
    var isPredictionEnabled by remember { mutableStateOf(true) }
    var isVelocityVectorEnabled by remember { mutableStateOf(true) }
    var isAccelVectorEnabled by remember { mutableStateOf(true) }
    var isBarycenterEnabled by remember { mutableStateOf(true) }
    var isGridEnabled by remember { mutableStateOf(true) }
    var isSimulationPaused by remember { mutableStateOf(false) }
    var currentSpeedMultiplier by remember { mutableStateOf(1.0) }

    // Start physics worker
    LaunchedEffect(Unit) {
        physicsWorker.start()
    }

    // Diagnostics & Snapshot polling loop
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            surfaceViewRef?.renderer?.diagnostics?.getLatestSnapshot()?.let {
                diagnosticsSnapshot = it
            }
            snapshotManager.getLatestSnapshot()?.let {
                latestSnapshot = it
            }
        }
    }

    // Handle Android Lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    surfaceViewRef?.onResume()
                    physicsWorker.start()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    surfaceViewRef?.onPause()
                    physicsWorker.stop()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    physicsWorker.stop()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            physicsWorker.stop()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("gravity_sandbox_screen")
    ) {
        // 1. OpenGL ES 3.0 Surface View Bridge
        AndroidView(
            factory = { ctx ->
                GravitySandboxSurfaceView(
                    context = ctx,
                    snapshotManager = snapshotManager,
                    qualityLevel = QualityLevel.HIGH
                ).also { view ->
                    surfaceViewRef = view
                    view.renderer.theme = RenderTheme.DARK
                    view.renderer.trailManager.isEnabled = isTrailsEnabled
                    view.renderer.trajectoryPredictor.isEnabled = isPredictionEnabled
                    view.renderer.vectorOverlayRenderer.showVelocityVectors = isVelocityVectorEnabled
                    view.renderer.vectorOverlayRenderer.showAccelerationVectors = isAccelVectorEnabled
                    view.renderer.barycenterRenderer.isEnabled = isBarycenterEnabled
                    view.renderer.isGridVisible = isGridEnabled
                    view.renderer.onBodySelectedListener = { bodyId ->
                        selectedBodyId = bodyId
                        view.renderer.selectedBodyId = bodyId
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("sandbox_gl_surface")
        )

        // 2. Top Navigation Bar (Liquid Glass)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = RedSpacing.md, vertical = RedSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBackToLab)
                    .testTag("sandbox_back_button"),
                shape = CircleShape,
                color = RedTheme.colors.surfaceElevated.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, RedTheme.colors.border)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Lab",
                        tint = RedTheme.colors.textPrimary
                    )
                }
            }

            // Title & Active Preset Pill
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(RedCornerRadius.full))
                    .testTag("sandbox_preset_pill"),
                shape = RoundedCornerShape(RedCornerRadius.full),
                color = RedTheme.colors.surfaceElevated.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, RedTheme.colors.border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = RedSpacing.md, vertical = RedSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RedSpacing.xs)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(RedTheme.colors.accentRed)
                    )
                    Text(
                        text = if (isFa) "سندباکس گرانش: $selectedPresetName" else "Gravity Sandbox: $selectedPresetName",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = RedTheme.colors.textPrimary
                    )
                }
            }

            // Diagnostic Toggle Button
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { showDiagnostics = !showDiagnostics }
                    .testTag("sandbox_diag_toggle"),
                shape = CircleShape,
                color = RedTheme.colors.surfaceElevated.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, RedTheme.colors.border)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (showDiagnostics) Icons.Default.BugReport else Icons.Outlined.BugReport,
                        contentDescription = "Toggle Diagnostics",
                        tint = if (showDiagnostics) RedTheme.colors.accentRed else RedTheme.colors.textSecondary
                    )
                }
            }
        }

        // 3. Top Visual Layers Bar (Horizontal Scrollable Filter Chips)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 56.dp, start = RedSpacing.md, end = RedSpacing.md)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(RedSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Toggle Orbital Trails
            FilterChip(
                selected = isTrailsEnabled,
                onClick = {
                    isTrailsEnabled = !isTrailsEnabled
                    surfaceViewRef?.renderer?.trailManager?.isEnabled = isTrailsEnabled
                },
                label = { Text(if (isFa) "مدارها" else "Trails", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.testTag("chip_toggle_trails")
            )

            // Toggle Trajectory Prediction
            FilterChip(
                selected = isPredictionEnabled,
                onClick = {
                    isPredictionEnabled = !isPredictionEnabled
                    surfaceViewRef?.renderer?.trajectoryPredictor?.isEnabled = isPredictionEnabled
                },
                label = { Text(if (isFa) "پیش‌بینی مسیر" else "Trajectory", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.testTag("chip_toggle_prediction")
            )

            // Toggle Velocity Vectors
            FilterChip(
                selected = isVelocityVectorEnabled,
                onClick = {
                    isVelocityVectorEnabled = !isVelocityVectorEnabled
                    surfaceViewRef?.renderer?.vectorOverlayRenderer?.showVelocityVectors = isVelocityVectorEnabled
                },
                label = { Text(if (isFa) "بردار سرعت" else "Velocity Vector", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.testTag("chip_toggle_velocity")
            )

            // Toggle Gravitational Acceleration Vectors
            FilterChip(
                selected = isAccelVectorEnabled,
                onClick = {
                    isAccelVectorEnabled = !isAccelVectorEnabled
                    surfaceViewRef?.renderer?.vectorOverlayRenderer?.showAccelerationVectors = isAccelVectorEnabled
                },
                label = { Text(if (isFa) "بردار شتاب" else "Accel Vector", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.testTag("chip_toggle_accel")
            )

            // Toggle Barycenter Marker
            FilterChip(
                selected = isBarycenterEnabled,
                onClick = {
                    isBarycenterEnabled = !isBarycenterEnabled
                    surfaceViewRef?.renderer?.barycenterRenderer?.isEnabled = isBarycenterEnabled
                },
                label = { Text(if (isFa) "مرکز جرم" else "Barycenter", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(Icons.Default.Adjust, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.testTag("chip_toggle_barycenter")
            )

            // Toggle Reference Grid
            FilterChip(
                selected = isGridEnabled,
                onClick = {
                    isGridEnabled = !isGridEnabled
                    surfaceViewRef?.renderer?.isGridVisible = isGridEnabled
                },
                label = { Text(if (isFa) "شبکه مداری" else "Grid", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.testTag("chip_toggle_grid")
            )
        }

        // 4. Floating Diagnostic Overlay
        AnimatedVisibility(
            visible = showDiagnostics,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 104.dp, start = RedSpacing.md)
        ) {
            Surface(
                shape = RoundedCornerShape(RedCornerRadius.md),
                color = Color.Black.copy(alpha = 0.78f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier
                    .widthIn(max = 270.dp)
                    .testTag("sandbox_diagnostics_overlay")
            ) {
                Column(
                    modifier = Modifier.padding(RedSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "GL ES 3.0 DIAGNOSTICS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = RedTheme.colors.accentRed
                    )
                    Text(
                        text = "FPS: %.1f | Frame: %.2f ms".format(diagnosticsSnapshot.fps, diagnosticsSnapshot.frameTimeMs),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                        color = Color.White
                    )
                    Text(
                        text = "Bodies: ${diagnosticsSnapshot.activeBodyCount} | Draws: ${diagnosticsSnapshot.drawCallCount}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                        color = Color.White
                    )
                    Text(
                        text = "Snap Age: ${diagnosticsSnapshot.snapshotAgeMs} ms (Seq #${diagnosticsSnapshot.snapshotSequence})",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Cam Dist: %.1f | Target: ${diagnosticsSnapshot.cameraTarget}".format(diagnosticsSnapshot.cameraDistance),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Scale: ${diagnosticsSnapshot.scaleMode}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                        color = RedTheme.colors.accentGold
                    )
                    if (selectedBodyId != null) {
                        Text(
                            text = "Focused: $selectedBodyId",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            color = RedTheme.colors.accentBlue
                        )
                    }
                }
            }
        }

        // 5. Selected Body Inspector & Scientific HUD
        val currentSelectedBody = latestSnapshot?.bodies?.firstOrNull { it.id == selectedBodyId }
        val barycenterInfo = surfaceViewRef?.renderer?.barycenterRenderer?.latestBarycenterInfo

        AnimatedVisibility(
            visible = currentSelectedBody != null,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 104.dp, end = RedSpacing.md)
        ) {
            currentSelectedBody?.let { body ->
                SelectedBodyInspectorCard(
                    body = body,
                    barycenterDistance = barycenterInfo?.let { b ->
                        val dx = body.posX - b.positionPhysicsMeters.x
                        val dy = body.posY - b.positionPhysicsMeters.y
                        val dz = body.posZ - b.positionPhysicsMeters.z
                        sqrt(dx * dx + dy * dy + dz * dz)
                    } ?: 0.0,
                    isFa = isFa,
                    onFocusCamera = {
                        surfaceViewRef?.renderer?.camera?.focusedBodyId = body.id
                    },
                    onClearTrail = {
                        val idx = latestSnapshot?.bodies?.indexOfFirst { it.id == body.id } ?: -1
                        if (idx >= 0) surfaceViewRef?.renderer?.trailManager?.clearBody(idx)
                    },
                    onDeselect = {
                        selectedBodyId = null
                        surfaceViewRef?.renderer?.selectedBodyId = null
                    }
                )
            }
        }

        // 6. Bottom Controls Deck (Camera Framing, Presets, Simulation Flow)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(RedSpacing.md),
            verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)
        ) {
            // Simulation Controls & Speed Multipliers Row
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RedCornerRadius.lg))
                    .testTag("sandbox_time_controls"),
                shape = RoundedCornerShape(RedCornerRadius.lg),
                color = RedTheme.colors.surfaceElevated.copy(alpha = 0.90f),
                border = androidx.compose.foundation.BorderStroke(1.dp, RedTheme.colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RedSpacing.sm, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play / Pause Button
                    IconButton(
                        onClick = {
                            isSimulationPaused = !isSimulationPaused
                            if (isSimulationPaused) {
                                physicsWorker.pause()
                            } else {
                                physicsWorker.resume()
                            }
                        },
                        modifier = Modifier.testTag("btn_pause_resume")
                    ) {
                        Icon(
                            imageVector = if (isSimulationPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Play/Pause",
                            tint = if (isSimulationPaused) RedTheme.colors.accentGold else RedTheme.colors.accentRed
                        )
                    }

                    // Speed Multiplier Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(1.0, 10.0, 100.0, 1000.0).forEach { speed ->
                            FilterChip(
                                selected = currentSpeedMultiplier == speed,
                                onClick = {
                                    currentSpeedMultiplier = speed
                                    physicsWorker.setTimeMultiplier(speed)
                                },
                                label = {
                                    Text("${speed.toInt()}×", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                                },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }

                    // Reset Time / Preset
                    IconButton(
                        onClick = {
                            physicsWorker.loadPreset(activePreset, currentSpeedMultiplier)
                            surfaceViewRef?.renderer?.clearTrails()
                            surfaceViewRef?.renderer?.fitAllBodies()
                        },
                        modifier = Modifier.testTag("btn_reset_simulation")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Simulation",
                            tint = RedTheme.colors.textSecondary
                        )
                    }
                }
            }

            // Quick Camera & Visual Tools Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RedSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fit All Bodies
                ElevatedButton(
                    onClick = { surfaceViewRef?.renderer?.fitAllBodies() },
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = RedTheme.colors.surfaceElevated.copy(alpha = 0.88f)
                    ),
                    contentPadding = PaddingValues(horizontal = RedSpacing.sm, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_fit_all")
                ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isFa) "نمای کلی" else "Fit All", style = MaterialTheme.typography.labelSmall)
                }

                // Reset Camera
                ElevatedButton(
                    onClick = { surfaceViewRef?.renderer?.resetCamera() },
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = RedTheme.colors.surfaceElevated.copy(alpha = 0.88f)
                    ),
                    contentPadding = PaddingValues(horizontal = RedSpacing.sm, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_reset_cam")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isFa) "تنظیم مجدد" else "Reset", style = MaterialTheme.typography.labelSmall)
                }

                // Clear Trails
                ElevatedButton(
                    onClick = { surfaceViewRef?.renderer?.clearTrails() },
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = RedTheme.colors.surfaceElevated.copy(alpha = 0.88f)
                    ),
                    contentPadding = PaddingValues(horizontal = RedSpacing.sm, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_clear_trails")
                ) {
                    Icon(Icons.Default.LinearScale, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isFa) "پاکسازی رد" else "Clear Trails", style = MaterialTheme.typography.labelSmall)
                }

                // Scale Mode Switcher
                ElevatedButton(
                    onClick = {
                        val nextMode = when (currentScaleMode) {
                            ScaleMode.SOLAR_SYSTEM_COMPRESSED -> ScaleMode.LINEAR
                            ScaleMode.LINEAR -> ScaleMode.PLANETARY_SYSTEM
                            ScaleMode.PLANETARY_SYSTEM -> ScaleMode.SOLAR_SYSTEM_COMPRESSED
                            ScaleMode.INSPECTION -> ScaleMode.SOLAR_SYSTEM_COMPRESSED
                        }
                        currentScaleMode = nextMode
                        surfaceViewRef?.renderer?.scaleManager?.scaleMode = nextMode
                    },
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = RedTheme.colors.surfaceElevated.copy(alpha = 0.88f)
                    ),
                    contentPadding = PaddingValues(horizontal = RedSpacing.sm, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_switch_scale")
                ) {
                    Icon(Icons.Default.ZoomOutMap, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(currentScaleMode.name.take(6), style = MaterialTheme.typography.labelSmall)
                }
            }

            // Quick Preset Selection Deck
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RedCornerRadius.xl))
                    .testTag("sandbox_presets_bar"),
                shape = RoundedCornerShape(RedCornerRadius.xl),
                color = RedTheme.colors.surfaceElevated.copy(alpha = 0.90f),
                border = androidx.compose.foundation.BorderStroke(1.dp, RedTheme.colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RedSpacing.sm, vertical = RedSpacing.xs)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(RedSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Preset: Kerr Black Hole
                    FilterChip(
                        selected = selectedPresetName == "Kerr Black Hole",
                        onClick = {
                            selectedPresetName = "Kerr Black Hole"
                            activePreset = SandboxPresetCatalog.KERR_ROTATING_BLACK_HOLE
                            physicsWorker.loadPreset(SandboxPresetCatalog.KERR_ROTATING_BLACK_HOLE, currentSpeedMultiplier)
                            surfaceViewRef?.renderer?.clearTrails()
                            surfaceViewRef?.renderer?.fitAllBodies()
                        },
                        label = { Text(if (isFa) "سیاه‌چاله کِر" else "Kerr Black Hole", style = MaterialTheme.typography.labelSmall) }
                    )

                    // Preset: Black Hole Infall
                    FilterChip(
                        selected = selectedPresetName == "Black Hole Infall",
                        onClick = {
                            selectedPresetName = "Black Hole Infall"
                            activePreset = SandboxPresetCatalog.BLACK_HOLE_ACCRETION_DEMO
                            physicsWorker.loadPreset(SandboxPresetCatalog.BLACK_HOLE_ACCRETION_DEMO, currentSpeedMultiplier)
                            surfaceViewRef?.renderer?.clearTrails()
                            surfaceViewRef?.renderer?.fitAllBodies()
                        },
                        label = { Text(if (isFa) "فروریزش سیاه‌چاله" else "Black Hole Infall", style = MaterialTheme.typography.labelSmall) }
                    )

                    // Preset: Wormhole
                    FilterChip(
                        selected = selectedPresetName == "Wormhole Bridge",
                        onClick = {
                            selectedPresetName = "Wormhole Bridge"
                            activePreset = SandboxPresetCatalog.THEORETICAL_WORMHOLE_SYSTEM
                            physicsWorker.loadPreset(SandboxPresetCatalog.THEORETICAL_WORMHOLE_SYSTEM, currentSpeedMultiplier)
                            surfaceViewRef?.renderer?.clearTrails()
                            surfaceViewRef?.renderer?.fitAllBodies()
                        },
                        label = { Text(if (isFa) "کرم‌چاله فرضی" else "Wormhole Bridge", style = MaterialTheme.typography.labelSmall) }
                    )

                    // Preset: Sun & Earth
                    FilterChip(
                        selected = selectedPresetName == "Sun & Earth",
                        onClick = {
                            selectedPresetName = "Sun & Earth"
                            activePreset = SandboxPresetCatalog.SUN_EARTH
                            physicsWorker.loadPreset(SandboxPresetCatalog.SUN_EARTH, currentSpeedMultiplier)
                            surfaceViewRef?.renderer?.clearTrails()
                            surfaceViewRef?.renderer?.fitAllBodies()
                        },
                        label = { Text(if (isFa) "خورشید و زمین" else "Sun-Earth", style = MaterialTheme.typography.labelSmall) }
                    )

                    // Preset: Earth-Moon
                    FilterChip(
                        selected = selectedPresetName == "Earth-Moon",
                        onClick = {
                            selectedPresetName = "Earth-Moon"
                            activePreset = SandboxPresetCatalog.EARTH_MOON
                            physicsWorker.loadPreset(SandboxPresetCatalog.EARTH_MOON, currentSpeedMultiplier)
                            surfaceViewRef?.renderer?.clearTrails()
                            surfaceViewRef?.renderer?.fitAllBodies()
                        },
                        label = { Text(if (isFa) "زمین و ماه" else "Earth-Moon", style = MaterialTheme.typography.labelSmall) }
                    )

                    // Preset: Full Solar System
                    FilterChip(
                        selected = selectedPresetName == "Solar System",
                        onClick = {
                            selectedPresetName = "Solar System"
                            activePreset = SandboxPresetCatalog.FULL_SOLAR_SYSTEM
                            physicsWorker.loadPreset(SandboxPresetCatalog.FULL_SOLAR_SYSTEM, currentSpeedMultiplier)
                            surfaceViewRef?.renderer?.clearTrails()
                            surfaceViewRef?.renderer?.fitAllBodies()
                        },
                        label = { Text(if (isFa) "منظومه شمسی" else "Full Solar", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    }
}

/**
 * Live Scientific Telemetry & Inspector Card for the focused celestial body.
 */
@Composable
fun SelectedBodyInspectorCard(
    body: BodyRenderState,
    barycenterDistance: Double,
    isFa: Boolean,
    onFocusCamera: () -> Unit,
    onClearTrail: () -> Unit,
    onDeselect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speedMag = sqrt(body.velX * body.velX + body.velY * body.velY + body.velZ * body.velZ)
    val accMag = sqrt(body.accX * body.accX + body.accY * body.accY + body.accZ * body.accZ)
    val speedKmS = speedMag / 1000.0
    val baryDistAU = barycenterDistance / AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS

    val isBlackHole = body.type == com.alijafari.red.astronomy.sandbox.model.SandboxBodyType.BLACK_HOLE
    val isWormhole = body.type == com.alijafari.red.astronomy.sandbox.model.SandboxBodyType.THEORETICAL_WORMHOLE

    // Schwarzschild radius: Rs = 2GM/c^2
    val rsMeters = if (body.massKg > 0.0) {
        (2.0 * AstroPhysicsConstants.G * body.massKg) /
                (AstroPhysicsConstants.SPEED_OF_LIGHT * AstroPhysicsConstants.SPEED_OF_LIGHT)
    } else 0.0
    val rsKm = rsMeters / 1000.0
    val rShadowKm = rsKm * 2.598
    val rIscoKm = rsKm * 3.0

    Surface(
        shape = RoundedCornerShape(RedCornerRadius.lg),
        color = Color.Black.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isBlackHole) RedTheme.colors.accentRed.copy(alpha = 0.6f)
            else if (isWormhole) RedTheme.colors.accentBlue.copy(alpha = 0.6f)
            else RedTheme.colors.accentBlue.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .widthIn(max = 300.dp)
            .testTag("body_inspector_card")
    ) {
        Column(
            modifier = Modifier.padding(RedSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isBlackHole) RedTheme.colors.accentRed
                                else if (isWormhole) RedTheme.colors.accentBlue
                                else RedTheme.colors.accentGold
                            )
                    )
                    Text(
                        text = if (isFa) body.nameFa else body.nameEn,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                IconButton(onClick = onDeselect, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

            // Standard Kinematic Telemetry
            Text(
                text = if (isFa) "سرعت مداری: %.2f km/s".format(speedKmS) else "Velocity: %.2f km/s".format(speedKmS),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = RedTheme.colors.accentBlue
            )
            Text(
                text = if (isFa) "شتاب گرانشی: %.4f m/s²".format(accMag) else "Acceleration: %.4f m/s²".format(accMag),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = RedTheme.colors.accentGold
            )
            Text(
                text = if (isFa) "فاصله از مرکز جرم: %.3f AU".format(baryDistAU) else "Dist to Barycenter: %.3f AU".format(baryDistAU),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                text = if (isFa) "جرم فیزیکی: %.2e kg".format(body.massKg) else "Mass: %.2e kg".format(body.massKg),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = Color.White.copy(alpha = 0.7f)
            )

            // Relativistic Specialized Telemetry
            if (isBlackHole) {
                HorizontalDivider(color = RedTheme.colors.accentRed.copy(alpha = 0.3f))
                Text(
                    text = if (isFa) "افق رویداد (Rs): %.2f km".format(rsKm) else "Event Horizon (Rs): %.2f km".format(rsKm),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
                    color = RedTheme.colors.accentRed
                )
                Text(
                    text = if (isFa) "سایه گرانشی: %.2f km".format(rShadowKm) else "Shadow Radius: %.2f km".format(rShadowKm),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = RedTheme.colors.accentGold
                )
                Text(
                    text = if (isFa) "مدار پایدار دایروی (ISCO): %.2f km".format(rIscoKm) else "ISCO Radius: %.2f km".format(rIscoKm),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.White.copy(alpha = 0.85f)
                )
            } else if (isWormhole) {
                HorizontalDivider(color = RedTheme.colors.accentBlue.copy(alpha = 0.3f))
                Text(
                    text = if (isFa) "متریک: موریس-تورن بازپیمایش‌پذیر" else "Metric: Morris-Thorne Traversible",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
                    color = RedTheme.colors.accentBlue
                )
                Text(
                    text = if (isFa) "توپولوژی: پل فضا-زمان دو سویه" else "Topology: Inter-Universal Bridge",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = RedTheme.colors.accentGold
                )
            }

            Spacer(Modifier.height(4.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = onFocusCamera,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(if (isFa) "تمرکز" else "Focus", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                }

                OutlinedButton(
                    onClick = onClearTrail,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(if (isFa) "حذف رد" else "Clear", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                }
            }
        }
    }
}
