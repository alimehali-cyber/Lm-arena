package com.alijafari.red.astronomy.sandbox.render.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.alijafari.red.astronomy.sandbox.physics.GravitySandboxEngine
import com.alijafari.red.astronomy.sandbox.presets.SandboxPresetCatalog
import com.alijafari.red.astronomy.sandbox.render.diagnostics.RenderDiagnosticsSnapshot
import com.alijafari.red.astronomy.sandbox.render.gl.QualityLevel
import com.alijafari.red.astronomy.sandbox.render.renderer.RenderTheme
import com.alijafari.red.astronomy.sandbox.render.scale.ScaleMode
import com.alijafari.red.astronomy.sandbox.snapshot.DoubleBufferSnapshotManager
import com.alijafari.red.astronomy.sandbox.worker.SandboxPhysicsWorker
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Gravity Sandbox 3D Viewport Screen integrating the OpenGL ES 3.0 renderer with Jetpack Compose.
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
    var showDiagnostics by remember { mutableStateOf(true) }
    var selectedPresetName by remember { mutableStateOf("Sun & Earth") }
    var selectedBodyId by remember { mutableStateOf<String?>(null) }
    var currentScaleMode by remember { mutableStateOf(ScaleMode.SOLAR_SYSTEM_COMPRESSED) }
    var diagnosticsSnapshot by remember { mutableStateOf(RenderDiagnosticsSnapshot()) }

    // Start physics worker
    LaunchedEffect(Unit) {
        physicsWorker.start()
    }

    // Diagnostics periodic polling loop (isolated from render thread)
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            surfaceViewRef?.renderer?.diagnostics?.getLatestSnapshot()?.let {
                diagnosticsSnapshot = it
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
                    view.renderer.onBodySelectedListener = { bodyId ->
                        selectedBodyId = bodyId
                        view.renderer.camera.focusedBodyId = bodyId
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
                        style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold),
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

        // 3. Floating Diagnostic Overlay
        AnimatedVisibility(
            visible = showDiagnostics,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 80.dp, start = RedSpacing.md)
        ) {
            Surface(
                shape = RoundedCornerShape(RedCornerRadius.md),
                color = Color.Black.copy(alpha = 0.75f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .testTag("sandbox_diagnostics_overlay")
            ) {
                Column(
                    modifier = Modifier.padding(RedSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "GL ES 3.0 DIAGNOSTICS",
                        style = RedTypographyTokens.caption.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = RedTheme.colors.accentRed
                    )
                    Text(
                        text = "FPS: %.1f | Frame: %.2f ms".format(diagnosticsSnapshot.fps, diagnosticsSnapshot.frameTimeMs),
                        style = RedTypographyTokens.caption.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                        color = Color.White
                    )
                    Text(
                        text = "Bodies: ${diagnosticsSnapshot.activeBodyCount} | Draws: ${diagnosticsSnapshot.drawCallCount}",
                        style = RedTypographyTokens.caption.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                        color = Color.White
                    )
                    Text(
                        text = "Snap Age: ${diagnosticsSnapshot.snapshotAgeMs} ms (Seq #${diagnosticsSnapshot.snapshotSequence})",
                        style = RedTypographyTokens.caption.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Cam Dist: %.1f | Target: ${diagnosticsSnapshot.cameraTarget}".format(diagnosticsSnapshot.cameraDistance),
                        style = RedTypographyTokens.caption.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Scale: ${diagnosticsSnapshot.scaleMode}",
                        style = RedTypographyTokens.caption.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                        color = RedTheme.colors.accentGold
                    )
                    if (selectedBodyId != null) {
                        Text(
                            text = "Focused: $selectedBodyId",
                            style = RedTypographyTokens.caption.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            color = RedTheme.colors.accentBlue
                        )
                    }
                }
            }
        }

        // 4. Bottom Controls Deck (Camera Framing, Presets, Simulation Flow)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(RedSpacing.md),
            verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)
        ) {
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
                    Text(if (isFa) "نمای کلی" else "Fit All", style = RedTypographyTokens.caption)
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
                    Text(if (isFa) "تنظیم مجدد" else "Reset", style = RedTypographyTokens.caption)
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
                    Text(if (isFa) "پاکسازی رد" else "Trails", style = RedTypographyTokens.caption)
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
                    Text(currentScaleMode.name.take(6), style = RedTypographyTokens.caption)
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
                        .padding(horizontal = RedSpacing.sm, vertical = RedSpacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Preset 1: Sun & Earth
                    FilterChip(
                        selected = selectedPresetName == "Sun & Earth",
                        onClick = {
                            selectedPresetName = "Sun & Earth"
                            physicsWorker.loadPreset(SandboxPresetCatalog.SUN_EARTH)
                            surfaceViewRef?.renderer?.clearTrails()
                            surfaceViewRef?.renderer?.fitAllBodies()
                        },
                        label = { Text("Sun-Earth", style = RedTypographyTokens.caption) }
                    )

                    // Preset 2: Earth-Moon
                    FilterChip(
                        selected = selectedPresetName == "Earth-Moon",
                        onClick = {
                            selectedPresetName = "Earth-Moon"
                            physicsWorker.loadPreset(SandboxPresetCatalog.EARTH_MOON)
                            surfaceViewRef?.renderer?.clearTrails()
                            surfaceViewRef?.renderer?.fitAllBodies()
                        },
                        label = { Text("Earth-Moon", style = RedTypographyTokens.caption) }
                    )

                    // Preset 3: Figure-8 3-Body
                    FilterChip(
                        selected = selectedPresetName == "Figure-8",
                        onClick = {
                            selectedPresetName = "Figure-8"
                            physicsWorker.loadPreset(SandboxPresetCatalog.FIGURE_EIGHT_THREE_BODY)
                            surfaceViewRef?.renderer?.clearTrails()
                            surfaceViewRef?.renderer?.fitAllBodies()
                        },
                        label = { Text("Figure-8", style = RedTypographyTokens.caption) }
                    )

                    // Preset 4: Full Solar System
                    FilterChip(
                        selected = selectedPresetName == "Solar System",
                        onClick = {
                            selectedPresetName = "Solar System"
                            physicsWorker.loadPreset(SandboxPresetCatalog.FULL_SOLAR_SYSTEM)
                            surfaceViewRef?.renderer?.clearTrails()
                            surfaceViewRef?.renderer?.fitAllBodies()
                        },
                        label = { Text("Full Solar", style = RedTypographyTokens.caption) }
                    )
                }
            }
        }
    }
}
