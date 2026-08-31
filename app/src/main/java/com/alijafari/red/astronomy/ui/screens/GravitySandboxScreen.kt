package com.alijafari.red.astronomy.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.*
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.theme.*
import com.alijafari.red.astronomy.util.toPersianDigits
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GravitySandboxScreen(
    uiState: MainUiState,
    onBackToLab: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        onBackToLab()
    }

    val isFa = uiState.language == AppLanguage.PERSIAN

    // Selected Preset Scenario
    var currentScenario by remember { mutableStateOf(PresetScenario.SOLAR_SYSTEM) }

    // Simulation Body List
    val bodies = remember {
        mutableStateListOf<CelestialBody>().apply {
            addAll(GravitySandboxEngine.getPresetBodies(currentScenario).map { it.deepCopy() })
        }
    }

    // Playback & Physics Controls
    var isPlaying by remember { mutableStateOf(true) }
    var speedMultiplier by remember { mutableFloatStateOf(1.0f) }
    var enableCollisions by remember { mutableStateOf(true) }
    var showTrails by remember { mutableStateOf(true) }
    var showVelocityVectors by remember { mutableStateOf(true) }
    var showForceVectors by remember { mutableStateOf(false) }
    var showCenterOfMass by remember { mutableStateOf(true) }
    var trackCenterOfMass by remember { mutableStateOf(false) }
    var showDiagnosticsHUD by remember { mutableStateOf(true) }

    // --- TEACHING LAYER STATE ---
    var isTeachingModeOn by remember { mutableStateOf(true) }
    val teachingObserver = remember { GravityTeachingObserver() }
    var activeTeachingMoment by remember { mutableStateOf<TeachingMoment?>(null) }
    var selectedMomentForWhy by remember { mutableStateOf<TeachingMoment?>(null) }

    // Discovery Mode & Interactive Prediction Experiments
    var showDiscoveryModal by remember { mutableStateOf(false) }
    var selectedExperimentForPrediction by remember { mutableStateOf<InteractiveExperiment?>(null) }
    var activeGuidedLesson by remember { mutableStateOf<PhysicsLesson?>(null) }
    var showAcademyDialog by remember { mutableStateOf(false) }

    // Interaction Modes: VIEW, SPAWN_DRAG
    var isSpawnModeActive by remember { mutableStateOf(false) }
    var dragStartPos by remember { mutableStateOf<Offset?>(null) }
    var dragCurrentPos by remember { mutableStateOf<Offset?>(null) }

    // Selected Body for Editing / Details
    var selectedBody by remember { mutableStateOf<CelestialBody?>(null) }
    var showAddBodyDialog by remember { mutableStateOf(false) }

    // Canvas Transformation State (Pan & Zoom)
    var scale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Diagnostic Stats
    var diagnostics by remember {
        mutableStateOf(GravitySandboxEngine.calculateDiagnostics(bodies))
    }

    // Auto Physics Tick & Event-Driven Teaching Observer Loop
    LaunchedEffect(isPlaying, speedMultiplier, enableCollisions, currentScenario, isTeachingModeOn) {
        val baseDt = when (currentScenario) {
            PresetScenario.SOLAR_SYSTEM, PresetScenario.LAGRANGE_POINTS -> 3600.0 * 4.0 // 4 hours
            PresetScenario.BINARY_STAR, PresetScenario.FIGURE_EIGHT, PresetScenario.CHAOTIC_FOUR_BODY -> 3600.0 * 1.5
            PresetScenario.BLACK_HOLE_SLINGSHOT -> 3600.0 * 0.5
            PresetScenario.EMPTY_CANVAS -> 3600.0 * 2.0
        }

        while (true) {
            val now = System.currentTimeMillis()
            if (isPlaying && bodies.isNotEmpty()) {
                val effectiveDt = baseDt * speedMultiplier.toDouble()
                GravitySandboxEngine.stepSimulation(
                    bodies = bodies,
                    dt = effectiveDt,
                    enableCollisions = enableCollisions,
                    substepCount = 10
                )
                diagnostics = GravitySandboxEngine.calculateDiagnostics(bodies)

                // Event-driven quiet teaching observer
                val triggeredMoment = teachingObserver.observeSimulation(
                    bodies = bodies,
                    diagnostics = diagnostics,
                    currentTimeMs = now,
                    isTeachingModeOn = isTeachingModeOn
                )
                if (triggeredMoment != null) {
                    activeTeachingMoment = triggeredMoment
                }

                if (trackCenterOfMass && diagnostics.bodyCount > 0) {
                    val scaleDist = getSpatialScale(currentScenario)
                    panOffset = Offset(
                        x = (-diagnostics.comPosX / scaleDist).toFloat(),
                        y = (diagnostics.comPosY / scaleDist).toFloat()
                    )
                }
            }
            delay(16) // ~60 FPS
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("gravity_sandbox_screen"),
        containerColor = Color(0xFF070B14),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isFa) "شبیه‌ساز گرانش و مکانیک سماوی" else "Gravity Sandbox",
                            style = RedTypographyTokens.sectionHeading.copy(fontSize = 18.sp),
                            color = RedTheme.colors.textPrimary
                        )
                        Text(
                            text = if (isFa) "شبیه‌سازی N-جرم گرانشی نیوتنی" else "N-Body Newton-Kepler Simulator",
                            style = RedTypographyTokens.caption,
                            color = RedTheme.colors.textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToLab,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(RedTheme.colors.surfaceElevated.copy(alpha = 0.8f))
                            .testTag("sandbox_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Lab",
                            tint = RedTheme.colors.textPrimary
                        )
                    }
                },
                actions = {
                    // Teaching Mode Toggle ("آموزش / Learn")
                    FilterChip(
                        selected = isTeachingModeOn,
                        onClick = {
                            isTeachingModeOn = !isTeachingModeOn
                            if (!isTeachingModeOn) activeTeachingMoment = null
                        },
                        label = {
                            Text(
                                text = if (isFa) "آموزش" else "Learn",
                                style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold),
                                color = if (isTeachingModeOn) Color.White else RedTheme.colors.textSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = if (isTeachingModeOn) Color.White else RedTheme.colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RedTheme.colors.accentRed,
                            containerColor = RedTheme.colors.surfaceElevated.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("toggle_teaching_mode_button")
                    )

                    // Discovery Mode ("کشف کن / Explore")
                    IconButton(
                        onClick = { showDiscoveryModal = true },
                        modifier = Modifier.testTag("open_discovery_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Explore Experiments",
                            tint = Color(0xFFFFAB40)
                        )
                    }

                    // Physics Academy
                    IconButton(
                        onClick = { showAcademyDialog = true },
                        modifier = Modifier.testTag("open_academy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Physics Academy",
                            tint = RedTheme.colors.accentRed
                        )
                    }

                    // HUD Toggle
                    IconButton(
                        onClick = { showDiagnosticsHUD = !showDiagnosticsHUD },
                        modifier = Modifier.testTag("toggle_hud_button")
                    ) {
                        Icon(
                            imageVector = if (showDiagnosticsHUD) Icons.Default.Analytics else Icons.Outlined.Analytics,
                            contentDescription = "Toggle HUD",
                            tint = if (showDiagnosticsHUD) RedTheme.colors.accentRed else RedTheme.colors.textSecondary
                        )
                    }

                    // Reset
                    IconButton(
                        onClick = {
                            bodies.clear()
                            bodies.addAll(GravitySandboxEngine.getPresetBodies(currentScenario).map { it.deepCopy() })
                            diagnostics = GravitySandboxEngine.calculateDiagnostics(bodies)
                            teachingObserver.reset()
                            activeTeachingMoment = null
                        },
                        modifier = Modifier.testTag("reset_sandbox_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Simulation",
                            tint = RedTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF090E1A).copy(alpha = 0.9f)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Interactive Physics Canvas
            val spatialScaleMeters = remember(currentScenario) { getSpatialScale(currentScenario) }

            // Read in Composable scope: RedTheme.colors is a @Composable accessor and
            // cannot be used inside the non-composable Canvas draw lambda below.
            val selectionRingColor = RedTheme.colors.accentRed

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("gravity_sandbox_canvas")
                    .pointerInput(isSpawnModeActive) {
                        if (isSpawnModeActive) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    dragStartPos = offset
                                    dragCurrentPos = offset
                                },
                                onDrag = { change, _ ->
                                    dragCurrentPos = change.position
                                },
                                onDragEnd = {
                                    val start = dragStartPos
                                    val end = dragCurrentPos
                                    if (start != null && end != null) {
                                        val canvasWidth = size.width
                                        val canvasHeight = size.height

                                        val screenX = (start.x - canvasWidth / 2f) / scale - panOffset.x
                                        val screenY = (start.y - canvasHeight / 2f) / scale - panOffset.y

                                        val posX = screenX.toDouble() * spatialScaleMeters
                                        val posY = -screenY.toDouble() * spatialScaleMeters

                                        val dragDx = (end.x - start.x) / scale
                                        val dragDy = (end.y - start.y) / scale

                                        val velX = dragDx.toDouble() * 150.0
                                        val velY = -dragDy.toDouble() * 150.0

                                        val newBody = CelestialBody(
                                            id = UUID.randomUUID().toString(),
                                            nameEn = "Launched Body",
                                            nameFa = "جرم پرتاب شده",
                                            mass = 1.0e24,
                                            radius = 6.0e6,
                                            posX = posX,
                                            posY = posY,
                                            velX = velX,
                                            velY = velY,
                                            colorHex = 0xFFFFAB40,
                                            bodyType = BodyType.TERRESTRIAL_PLANET
                                        )
                                        bodies.add(newBody)
                                        diagnostics = GravitySandboxEngine.calculateDiagnostics(bodies)
                                    }
                                    dragStartPos = null
                                    dragCurrentPos = null
                                    isSpawnModeActive = false
                                }
                            )
                        } else {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.1f, 30.0f)
                                panOffset += pan
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            val canvasWidth = size.width
                            val canvasHeight = size.height

                            val hit = bodies.firstOrNull { body ->
                                val bodyScreenX = (canvasWidth / 2f) + (body.posX / spatialScaleMeters).toFloat() * scale + panOffset.x * scale
                                val bodyScreenY = (canvasHeight / 2f) - (body.posY / spatialScaleMeters).toFloat() * scale + panOffset.y * scale
                                val dist = sqrt((tapOffset.x - bodyScreenX).pow(2) + (tapOffset.y - bodyScreenY).pow(2))
                                dist < 32.dp.toPx()
                            }
                            selectedBody = hit
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val center = Offset(canvasWidth / 2f, canvasHeight / 2f)

                // 1. Draw Grid Background
                val gridSpacing = 80.dp.toPx() * scale
                val gridAlpha = (0.15f * scale).coerceIn(0.05f, 0.25f)

                var x = (panOffset.x * scale) % gridSpacing
                while (x < canvasWidth) {
                    drawLine(
                        color = Color(0xFF1E293B).copy(alpha = gridAlpha),
                        start = Offset(x, 0f),
                        end = Offset(x, canvasHeight),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += gridSpacing
                }

                var y = (panOffset.y * scale) % gridSpacing
                while (y < canvasHeight) {
                    drawLine(
                        color = Color(0xFF1E293B).copy(alpha = gridAlpha),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += gridSpacing
                }

                // Coordinate Crosshair
                val originX = center.x + panOffset.x * scale
                val originY = center.y + panOffset.y * scale
                drawLine(
                    color = Color(0xFF334155).copy(alpha = 0.4f),
                    start = Offset(0f, originY),
                    end = Offset(canvasWidth, originY),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color(0xFF334155).copy(alpha = 0.4f),
                    start = Offset(originX, 0f),
                    end = Offset(originX, canvasHeight),
                    strokeWidth = 1.dp.toPx()
                )

                // 2. Draw Motion Trails
                if (showTrails) {
                    for (body in bodies) {
                        if (body.trailPoints.size > 1) {
                            val path = Path()
                            val bodyColor = Color(body.colorHex)

                            for (i in body.trailPoints.indices) {
                                val pt = body.trailPoints[i]
                                val sx = center.x + (pt.first / spatialScaleMeters).toFloat() * scale + panOffset.x * scale
                                val sy = center.y - (pt.second / spatialScaleMeters).toFloat() * scale + panOffset.y * scale

                                if (i == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
                            }

                            drawPath(
                                path = path,
                                color = bodyColor.copy(alpha = 0.45f),
                                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }

                // 3. Draw Celestial Bodies & Overlay Vectors
                for (body in bodies) {
                    val sx = center.x + (body.posX / spatialScaleMeters).toFloat() * scale + panOffset.x * scale
                    val sy = center.y - (body.posY / spatialScaleMeters).toFloat() * scale + panOffset.y * scale

                    val bodyColor = Color(body.colorHex)
                    val visualRadius = (8.dp.toPx() * (log10(body.mass / 1e20).toFloat().coerceIn(0.6f, 2.5f))) * scale.coerceIn(0.5f, 2.0f)

                    if (body.bodyType == BodyType.STAR) {
                        drawCircle(
                            color = bodyColor.copy(alpha = 0.25f),
                            radius = visualRadius * 2.2f,
                            center = Offset(sx, sy)
                        )
                    } else if (body.bodyType == BodyType.BLACK_HOLE) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.2f),
                            radius = visualRadius * 1.8f,
                            center = Offset(sx, sy)
                        )
                    }

                    drawCircle(
                        color = if (body.bodyType == BodyType.BLACK_HOLE) Color.Black else bodyColor,
                        radius = visualRadius,
                        center = Offset(sx, sy)
                    )

                    drawCircle(
                        color = if (selectedBody?.id == body.id) selectionRingColor else bodyColor.copy(alpha = 0.8f),
                        radius = visualRadius + (if (selectedBody?.id == body.id) 3.dp.toPx() else 0.dp.toPx()),
                        center = Offset(sx, sy),
                        style = Stroke(width = if (selectedBody?.id == body.id) 2.dp.toPx() else 1.dp.toPx())
                    )

                    if (showVelocityVectors) {
                        val vScale = 0.00008f * scale
                        val endX = sx + (body.velX * vScale).toFloat()
                        val endY = sy - (body.velY * vScale).toFloat()

                        drawLine(
                            color = Color(0xFF64B5F6),
                            start = Offset(sx, sy),
                            end = Offset(endX, endY),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawCircle(
                            color = Color(0xFF64B5F6),
                            radius = 3.dp.toPx(),
                            center = Offset(endX, endY)
                        )
                    }

                    if (showForceVectors) {
                        val fScale = 0.005f * scale
                        val fEndX = sx + (body.accX * fScale).toFloat()
                        val fEndY = sy - (body.accY * fScale).toFloat()

                        drawLine(
                            color = Color(0xFFFF7043),
                            start = Offset(sx, sy),
                            end = Offset(fEndX, fEndY),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }

                // 4. Draw Center of Mass
                if (showCenterOfMass && diagnostics.bodyCount > 0) {
                    val comSx = center.x + (diagnostics.comPosX / spatialScaleMeters).toFloat() * scale + panOffset.x * scale
                    val comSy = center.y - (diagnostics.comPosY / spatialScaleMeters).toFloat() * scale + panOffset.y * scale

                    drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = 6.dp.toPx(),
                        center = Offset(comSx, comSy),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawLine(
                        color = Color(0xFFFFD54F),
                        start = Offset(comSx - 8.dp.toPx(), comSy),
                        end = Offset(comSx + 8.dp.toPx(), comSy),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFFFFD54F),
                        start = Offset(comSx, comSy - 8.dp.toPx()),
                        end = Offset(comSx, comSy + 8.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                // 5. Draw Launch Drag Arrow
                val startP = dragStartPos
                val currentP = dragCurrentPos
                if (startP != null && currentP != null) {
                    drawLine(
                        color = Color(0xFFFFAB40),
                        start = startP,
                        end = currentP,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = Color(0xFFFFAB40),
                        radius = 8.dp.toPx(),
                        center = startP
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = currentP
                    )
                }
            }

            // Top Scenario Preset Selector Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = RedSpacing.md, vertical = RedSpacing.sm),
                horizontalArrangement = Arrangement.Center
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm),
                    contentPadding = PaddingValues(horizontal = RedSpacing.sm)
                ) {
                    items(PresetScenario.entries) { scenario ->
                        val isSelected = scenario == currentScenario
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                currentScenario = scenario
                                bodies.clear()
                                bodies.addAll(GravitySandboxEngine.getPresetBodies(scenario).map { it.deepCopy() })
                                diagnostics = GravitySandboxEngine.calculateDiagnostics(bodies)
                                teachingObserver.reset()
                                activeTeachingMoment = null
                                scale = 1.0f
                                panOffset = Offset.Zero
                            },
                            label = {
                                Text(
                                    text = if (isFa) scenario.titleFa else scenario.titleEn,
                                    style = RedTypographyTokens.caption.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                    color = if (isSelected) Color.White else RedTheme.colors.textSecondary
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RedTheme.colors.accentRed,
                                containerColor = RedTheme.colors.surfaceElevated.copy(alpha = 0.85f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) RedTheme.colors.accentRed else RedTheme.colors.border,
                                enabled = true,
                                selected = isSelected
                            ),
                            modifier = Modifier.testTag("preset_chip_${scenario.name.lowercase()}")
                        )
                    }
                }
            }

            // --- FLOATING TEACHING MOMENT CARD (Event-Driven quietly when isTeachingModeOn) ---
            val moment = activeTeachingMoment
            if (isTeachingModeOn && moment != null) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 52.dp, start = RedSpacing.md, end = RedSpacing.md)
                        .fillMaxWidth(0.92f)
                ) {
                    RedElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("teaching_moment_card"),
                        shape = RoundedCornerShape(RedCornerRadius.lg),
                        backgroundColor = Color(0xFF0F172A).copy(alpha = 0.94f),
                        borderColor = RedTheme.colors.accentRed,
                        contentPadding = PaddingValues(RedSpacing.md)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(RedTheme.colors.accentRed.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Psychology,
                                            contentDescription = null,
                                            tint = RedTheme.colors.accentRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = if (isFa) "یک نکته آموزشی" else "Physics Insight",
                                        style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold),
                                        color = RedTheme.colors.accentRed
                                    )
                                }

                                IconButton(
                                    onClick = { activeTeachingMoment = null },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = RedTheme.colors.textSecondary
                                    )
                                }
                            }

                            Text(
                                text = if (isFa) moment.titleFa else moment.titleEn,
                                style = RedTypographyTokens.sectionHeading.copy(fontSize = 15.sp),
                                color = RedTheme.colors.textPrimary
                            )

                            val reaction = if (isFa) moment.personalityReactionFa else moment.personalityReactionEn
                            if (reaction != null) {
                                Text(
                                    text = reaction,
                                    style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFFFFD54F)
                                )
                            }

                            Text(
                                text = if (isFa) moment.shortNoteFa else moment.shortNoteEn,
                                style = RedTypographyTokens.bodySecondary.copy(fontSize = 12.sp),
                                color = RedTheme.colors.textSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { selectedMomentForWhy = moment },
                                    colors = ButtonDefaults.buttonColors(containerColor = RedTheme.colors.accentRed),
                                    shape = RoundedCornerShape(RedCornerRadius.md),
                                    contentPadding = PaddingValues(horizontal = RedSpacing.md, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isFa) "چرا؟" else "Why?",
                                        style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Real-Time Physics Diagnostics HUD Panel
            AnimatedVisibility(
                visible = showDiagnosticsHUD && activeTeachingMoment == null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 56.dp, start = RedSpacing.md)
            ) {
                RedElevatedCard(
                    modifier = Modifier
                        .width(220.dp)
                        .testTag("diagnostics_hud_card"),
                    shape = RoundedCornerShape(RedCornerRadius.md),
                    backgroundColor = Color(0xFF090E1A).copy(alpha = 0.88f),
                    borderColor = RedTheme.colors.border.copy(alpha = 0.6f),
                    contentPadding = PaddingValues(RedSpacing.md)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isFa) "تشخیص‌های گرانش" else "Physics HUD",
                            style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold),
                            color = RedTheme.colors.accentRed
                        )
                        HorizontalDivider(
                            color = RedTheme.colors.border.copy(alpha = 0.4f),
                            thickness = 0.5.dp
                        )
                        DiagnosticRow(
                            label = if (isFa) "اجرام:" else "Bodies:",
                            value = if (isFa) diagnostics.bodyCount.toString().toPersianDigits() else "${diagnostics.bodyCount}"
                        )
                        DiagnosticRow(
                            label = if (isFa) "انرژی جنبشی (K):" else "Kinetic (K):",
                            value = formatEnergy(diagnostics.kineticEnergy, isFa)
                        )
                        DiagnosticRow(
                            label = if (isFa) "انرژی پتانسیل (U):" else "Potential (U):",
                            value = formatEnergy(diagnostics.potentialEnergy, isFa)
                        )
                        DiagnosticRow(
                            label = if (isFa) "انرژی کل (E):" else "Total (E):",
                            value = formatEnergy(diagnostics.totalEnergy, isFa),
                            isHighlight = true
                        )
                        DiagnosticRow(
                            label = if (isFa) "برخوردها:" else "Collisions:",
                            value = if (enableCollisions) (if (isFa) "فعال" else "ON") else (if (isFa) "غیرفعال" else "OFF")
                        )
                    }
                }
            }

            // Floating Controls Bar (Bottom Center)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, start = RedSpacing.md, end = RedSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)
            ) {
                // Secondary Visual Toggles Bar
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(RedCornerRadius.lg))
                        .background(Color(0xFF0D1322).copy(alpha = 0.9f))
                        .border(1.dp, RedTheme.colors.border.copy(alpha = 0.5f), RoundedCornerShape(RedCornerRadius.lg))
                        .padding(horizontal = RedSpacing.sm, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showTrails = !showTrails },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toggle_trails_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Trails",
                            tint = if (showTrails) RedTheme.colors.accentRed else RedTheme.colors.textSecondary
                        )
                    }
                    IconButton(
                        onClick = { showVelocityVectors = !showVelocityVectors },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toggle_velocity_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Velocity Vectors",
                            tint = if (showVelocityVectors) RedTheme.colors.accentRed else RedTheme.colors.textSecondary
                        )
                    }
                    IconButton(
                        onClick = { showForceVectors = !showForceVectors },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toggle_force_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compress,
                            contentDescription = "Force Vectors",
                            tint = if (showForceVectors) RedTheme.colors.accentRed else RedTheme.colors.textSecondary
                        )
                    }
                    IconButton(
                        onClick = { showCenterOfMass = !showCenterOfMass },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toggle_com_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Adjust,
                            contentDescription = "Center of Mass",
                            tint = if (showCenterOfMass) RedTheme.colors.accentRed else RedTheme.colors.textSecondary
                        )
                    }
                    IconButton(
                        onClick = { enableCollisions = !enableCollisions },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toggle_collisions_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MergeType,
                            contentDescription = "Collisions",
                            tint = if (enableCollisions) RedTheme.colors.accentRed else RedTheme.colors.textSecondary
                        )
                    }
                    IconButton(
                        onClick = { isSpawnModeActive = !isSpawnModeActive },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toggle_spawn_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Spawn Launch Mode",
                            tint = if (isSpawnModeActive) Color(0xFFFFAB40) else RedTheme.colors.textSecondary
                        )
                    }
                }

                // Primary Playback Navigation Dock
                RedElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playback_controls_dock"),
                    shape = RoundedCornerShape(RedCornerRadius.xl),
                    backgroundColor = Color(0xFF090E1A).copy(alpha = 0.95f),
                    borderColor = RedTheme.colors.border,
                    contentPadding = PaddingValues(horizontal = RedSpacing.lg, vertical = RedSpacing.sm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(RedTheme.colors.accentRed)
                                .testTag("play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = RedSpacing.md),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isFa)
                                    "سرعت شبیه‌سازی: ${(speedMultiplier.toDouble()).toString().take(4).toPersianDigits()}x"
                                else
                                    "Speed: ${String.format("%.1f", speedMultiplier)}x",
                                style = RedTypographyTokens.caption,
                                color = RedTheme.colors.textSecondary
                            )
                            Slider(
                                value = speedMultiplier,
                                onValueChange = { speedMultiplier = it },
                                valueRange = 0.1f..10.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = RedTheme.colors.accentRed,
                                    activeTrackColor = RedTheme.colors.accentRed,
                                    inactiveTrackColor = RedTheme.colors.border
                                ),
                                modifier = Modifier.testTag("speed_slider")
                            )
                        }

                        Button(
                            onClick = { showAddBodyDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedTheme.colors.surfaceElevated
                            ),
                            border = BorderStroke(1.dp, RedTheme.colors.border),
                            shape = RoundedCornerShape(RedCornerRadius.lg),
                            contentPadding = PaddingValues(horizontal = RedSpacing.md, vertical = 8.dp),
                            modifier = Modifier.testTag("add_body_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Body",
                                tint = RedTheme.colors.accentRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFa) "افزودن" else "Add",
                                style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold),
                                color = RedTheme.colors.textPrimary
                            )
                        }
                    }
                }
            }

            // Body Detail / Edit Inspector Dialog
            val activeBody = selectedBody
            if (activeBody != null) {
                AlertDialog(
                    onDismissRequest = { selectedBody = null },
                    confirmButton = {
                        TextButton(onClick = { selectedBody = null }) {
                            Text(if (isFa) "بستن" else "Close", color = RedTheme.colors.accentRed)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                bodies.remove(activeBody)
                                diagnostics = GravitySandboxEngine.calculateDiagnostics(bodies)
                                selectedBody = null
                            }
                        ) {
                            Text(if (isFa) "حذف جرم" else "Delete Body", color = Color(0xFFFF5252))
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(activeBody.colorHex))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFa) activeBody.nameFa else activeBody.nameEn,
                                style = RedTypographyTokens.sectionHeading
                            )
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DiagnosticRow(
                                label = if (isFa) "نوع جرم:" else "Type:",
                                value = if (isFa) activeBody.bodyType.titleFa else activeBody.bodyType.titleEn
                            )
                            DiagnosticRow(
                                label = if (isFa) "جرم (kg):" else "Mass (kg):",
                                value = String.format("%.3e", activeBody.mass)
                            )
                            DiagnosticRow(
                                label = if (isFa) "سرعت (m/s):" else "Speed (m/s):",
                                value = String.format("%.2f", sqrt(activeBody.velX * activeBody.velX + activeBody.velY * activeBody.velY))
                            )
                            DiagnosticRow(
                                label = if (isFa) "موقعیت X (m):" else "Pos X (m):",
                                value = String.format("%.2e", activeBody.posX)
                            )
                            DiagnosticRow(
                                label = if (isFa) "موقعیت Y (m):" else "Pos Y (m):",
                                value = String.format("%.2e", activeBody.posY)
                            )
                        }
                    },
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color.White,
                    textContentColor = Color.White
                )
            }

            // Modal for Creating Custom Celestial Body
            if (showAddBodyDialog) {
                AddCelestialBodyModal(
                    isFa = isFa,
                    onDismiss = { showAddBodyDialog = false },
                    onAddBody = { newBody ->
                        bodies.add(newBody)
                        diagnostics = GravitySandboxEngine.calculateDiagnostics(bodies)
                        showAddBodyDialog = false
                    }
                )
            }

            // Modal for Physics Academy Guided Lessons
            if (showAcademyDialog) {
                PhysicsAcademyModal(
                    isFa = isFa,
                    lessons = GravitySandboxEngine.physicsLessons,
                    onDismiss = { showAcademyDialog = false },
                    onSelectLesson = { lessonItem ->
                        currentScenario = lessonItem.presetScenario
                        bodies.clear()
                        bodies.addAll(GravitySandboxEngine.getPresetBodies(lessonItem.presetScenario).map { it.deepCopy() })
                        diagnostics = GravitySandboxEngine.calculateDiagnostics(bodies)
                        activeGuidedLesson = lessonItem
                        scale = 1.0f
                        panOffset = Offset.Zero
                        showAcademyDialog = false
                    }
                )
            }

            // Modal for "کشف کن / Explore" Discovery Mode
            if (showDiscoveryModal) {
                DiscoveryModeModal(
                    isFa = isFa,
                    experiments = GravityTeachingCatalog.experiments,
                    onDismiss = { showDiscoveryModal = false },
                    onSelectExperiment = { exp ->
                        selectedExperimentForPrediction = exp
                        showDiscoveryModal = false
                    }
                )
            }

            // Modal for "Why?" 3-Level Explanation
            val whyMoment = selectedMomentForWhy
            if (whyMoment != null) {
                ThreeLevelExplanationModal(
                    isFa = isFa,
                    moment = whyMoment,
                    onDismiss = { selectedMomentForWhy = null }
                )
            }

            // Modal for Prediction Prompt Before Experiment
            val expForPrediction = selectedExperimentForPrediction
            if (expForPrediction != null) {
                PredictionPromptModal(
                    isFa = isFa,
                    experiment = expForPrediction,
                    onDismiss = { selectedExperimentForPrediction = null },
                    onLaunchExperiment = {
                        currentScenario = expForPrediction.scenario
                        bodies.clear()
                        bodies.addAll(GravitySandboxEngine.getPresetBodies(expForPrediction.scenario).map { it.deepCopy() })
                        diagnostics = GravitySandboxEngine.calculateDiagnostics(bodies)
                        activeTeachingMoment = expForPrediction.explanationMoment
                        selectedExperimentForPrediction = null
                    }
                )
            }
        }
    }
}

@Composable
private fun ThreeLevelExplanationModal(
    isFa: Boolean,
    moment: TeachingMoment,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableStateOf(ExplanationLevel.LEVEL_1_SIMPLE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isFa) "متوجه شدم" else "Got It", color = RedTheme.colors.accentRed)
            }
        },
        title = {
            Column {
                Text(
                    text = if (isFa) "چرا این اتفاق افتاد؟" else "Why did this happen?",
                    style = RedTypographyTokens.sectionHeading
                )
                Text(
                    text = if (isFa) moment.titleFa else moment.titleEn,
                    style = RedTypographyTokens.caption,
                    color = RedTheme.colors.accentRed
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.md)) {
                // Tab Switcher for 3 Levels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RedCornerRadius.md))
                        .background(Color(0xFF1E293B))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FilterChip(
                        selected = activeTab == ExplanationLevel.LEVEL_1_SIMPLE,
                        onClick = { activeTab = ExplanationLevel.LEVEL_1_SIMPLE },
                        label = { Text(if (isFa) "سطح ۱: ساده" else "Level 1: Simple") }
                    )
                    FilterChip(
                        selected = activeTab == ExplanationLevel.LEVEL_2_MECHANISM,
                        onClick = { activeTab = ExplanationLevel.LEVEL_2_MECHANISM },
                        label = { Text(if (isFa) "سطح ۲: مکانیزم" else "Level 2: Mechanism") }
                    )
                    FilterChip(
                        selected = activeTab == ExplanationLevel.LEVEL_3_SCIENTIFIC,
                        onClick = { activeTab = ExplanationLevel.LEVEL_3_SCIENTIFIC },
                        label = { Text(if (isFa) "سطح ۳: علمی" else "Level 3: Scientific") }
                    )
                }

                // Level Explanation Content
                RedElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RedCornerRadius.md),
                    backgroundColor = Color(0xFF0F172A),
                    borderColor = RedTheme.colors.border,
                    contentPadding = PaddingValues(RedSpacing.md)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val textContent = when (activeTab) {
                            ExplanationLevel.LEVEL_1_SIMPLE -> if (isFa) moment.level1Fa else moment.level1En
                            ExplanationLevel.LEVEL_2_MECHANISM -> if (isFa) moment.level2Fa else moment.level2En
                            ExplanationLevel.LEVEL_3_SCIENTIFIC -> if (isFa) moment.level3Fa else moment.level3En
                        }

                        Text(
                            text = textContent,
                            style = RedTypographyTokens.bodySecondary,
                            color = RedTheme.colors.textPrimary
                        )

                        if (activeTab == ExplanationLevel.LEVEL_3_SCIENTIFIC && moment.formulaSymbol != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "فرمول علمی: ${moment.formulaSymbol}",
                                style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFFD54F)
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
private fun DiscoveryModeModal(
    isFa: Boolean,
    experiments: List<InteractiveExperiment>,
    onDismiss: () -> Unit,
    onSelectExperiment: (InteractiveExperiment) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isFa) "بستن" else "Close", color = RedTheme.colors.accentRed)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = Color(0xFFFFAB40),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isFa) "کشف کن — پیشنهاد آزمایش‌ها" else "Explore Experiments",
                    style = RedTypographyTokens.sectionHeading
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(RedSpacing.md)
            ) {
                items(experiments) { exp ->
                    RedElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectExperiment(exp) },
                        shape = RoundedCornerShape(RedCornerRadius.md),
                        backgroundColor = Color(0xFF1E293B),
                        borderColor = RedTheme.colors.border,
                        contentPadding = PaddingValues(RedSpacing.md)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isFa) exp.titleFa else exp.titleEn,
                                style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                color = Color.White
                            )
                            Text(
                                text = if (isFa) exp.instructionFa else exp.instructionEn,
                                style = RedTypographyTokens.bodySecondary.copy(fontSize = 11.sp),
                                color = RedTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
private fun PredictionPromptModal(
    isFa: Boolean,
    experiment: InteractiveExperiment,
    onDismiss: () -> Unit,
    onLaunchExperiment: () -> Unit
) {
    var selectedOptionId by remember { mutableStateOf<String?>(null) }
    var hasPredicted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (hasPredicted) {
                        onLaunchExperiment()
                    } else {
                        hasPredicted = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RedTheme.colors.accentRed)
            ) {
                Text(
                    text = if (hasPredicted) (if (isFa) "شروع آزمایش" else "Run Experiment") else (if (isFa) "ثبت حدس" else "Predict"),
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isFa) "انصراف" else "Cancel", color = RedTheme.colors.textSecondary)
            }
        },
        title = {
            Text(
                text = if (isFa) experiment.titleFa else experiment.titleEn,
                style = RedTypographyTokens.sectionHeading
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.md)) {
                Text(
                    text = if (isFa) experiment.questionFa else experiment.questionEn,
                    style = RedTypographyTokens.bodySecondary,
                    color = RedTheme.colors.textPrimary
                )

                if (!hasPredicted) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        experiment.predictionOptions.forEach { option ->
                            val isSelected = option.id == selectedOptionId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(RedCornerRadius.md))
                                    .clickable { selectedOptionId = option.id },
                                color = if (isSelected) RedTheme.colors.accentRed.copy(alpha = 0.2f) else Color(0xFF1E293B),
                                border = BorderStroke(1.dp, if (isSelected) RedTheme.colors.accentRed else RedTheme.colors.border)
                            ) {
                                Row(
                                    modifier = Modifier.padding(RedSpacing.md),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedOptionId = option.id },
                                        colors = RadioButtonDefaults.colors(selectedColor = RedTheme.colors.accentRed)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isFa) option.textFa else option.textEn,
                                        style = RedTypographyTokens.caption,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                } else {
                    RedElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(RedCornerRadius.md),
                        backgroundColor = Color(0xFF0F172A),
                        borderColor = RedTheme.colors.accentRed,
                        contentPadding = PaddingValues(RedSpacing.md)
                    ) {
                        Text(
                            text = if (isFa)
                                "حدس شما ثبت شد! هیچ اشکالی ندارد اگر پاسخ متفاوتی انتخاب کرده باشید؛ بیایید با هم شبیه‌سازی را اجرا کنیم و نتیجه را به چشم ببینیم."
                            else
                                "Your prediction is locked! Let's launch the experiment and observe the actual physical result.",
                            style = RedTypographyTokens.bodySecondary,
                            color = Color(0xFFFFD54F)
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
private fun PhysicsAcademyModal(
    isFa: Boolean,
    lessons: List<PhysicsLesson>,
    onDismiss: () -> Unit,
    onSelectLesson: (PhysicsLesson) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isFa) "بستن" else "Close", color = RedTheme.colors.accentRed)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = RedTheme.colors.accentRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isFa) "آکادمی اخترفیزیک و آزمایش‌های گرانش" else "Physics Academy & Experiments",
                    style = RedTypographyTokens.sectionHeading
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(RedSpacing.md)
            ) {
                items(lessons) { lesson ->
                    RedElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectLesson(lesson) },
                        shape = RoundedCornerShape(RedCornerRadius.md),
                        backgroundColor = Color(0xFF1E293B),
                        borderColor = RedTheme.colors.border,
                        contentPadding = PaddingValues(RedSpacing.md)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isFa) lesson.titleFa else lesson.titleEn,
                                    style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                    color = RedTheme.colors.textPrimary
                                )
                                Text(
                                    text = lesson.formulaSymbol,
                                    style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFFFD54F)
                                )
                            }
                            Text(
                                text = if (isFa) lesson.subtitleFa else lesson.subtitleEn,
                                style = RedTypographyTokens.caption,
                                color = RedTheme.colors.accentRed
                            )
                            Text(
                                text = if (isFa) lesson.conceptFa else lesson.conceptEn,
                                style = RedTypographyTokens.bodySecondary.copy(fontSize = 11.sp),
                                color = RedTheme.colors.textSecondary,
                                maxLines = 3
                            )
                            Button(
                                onClick = { onSelectLesson(lesson) },
                                colors = ButtonDefaults.buttonColors(containerColor = RedTheme.colors.accentRed),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = if (isFa) "شروع آزمایش" else "Start Experiment",
                                    style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = RedTypographyTokens.caption,
            color = RedTheme.colors.textSecondary
        )
        Text(
            text = value,
            style = RedTypographyTokens.caption.copy(
                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
                fontSize = if (isHighlight) 13.sp else 12.sp
            ),
            color = if (isHighlight) RedTheme.colors.accentRed else RedTheme.colors.textPrimary
        )
    }
}

@Composable
private fun AddCelestialBodyModal(
    isFa: Boolean,
    onDismiss: () -> Unit,
    onAddBody: (CelestialBody) -> Unit
) {
    var nameEn by remember { mutableStateOf("New Planet") }
    var nameFa by remember { mutableStateOf("سیاره جدید") }
    var selectedType by remember { mutableStateOf(BodyType.TERRESTRIAL_PLANET) }
    var massMultiplier by remember { mutableFloatStateOf(1.0f) }

    val colorHex = when (selectedType) {
        BodyType.STAR -> 0xFFFFD700
        BodyType.BLACK_HOLE -> 0xFF000000
        BodyType.GAS_GIANT -> 0xFFFF8A65
        BodyType.TERRESTRIAL_PLANET -> 0xFF4FC3F7
        BodyType.MOON -> 0xFFE0E0E0
        BodyType.COMET -> 0xFF80DEEA
        BodyType.ASTEROID -> 0xFFA1887F
    }

    val baseMass = when (selectedType) {
        BodyType.STAR -> 1.989e30
        BodyType.BLACK_HOLE -> 1.0e32
        BodyType.GAS_GIANT -> 1.898e27
        BodyType.TERRESTRIAL_PLANET -> 5.972e24
        BodyType.MOON -> 7.342e22
        BodyType.COMET -> 1.0e15
        BodyType.ASTEROID -> 1.0e18
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val body = CelestialBody(
                        id = UUID.randomUUID().toString(),
                        nameEn = nameEn,
                        nameFa = nameFa,
                        mass = baseMass * massMultiplier.toDouble(),
                        radius = 6.0e6,
                        posX = 0.0,
                        posY = 1.0e11,
                        velX = -25000.0,
                        velY = 0.0,
                        colorHex = colorHex,
                        bodyType = selectedType
                    )
                    onAddBody(body)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RedTheme.colors.accentRed)
            ) {
                Text(if (isFa) "افزودن جرم" else "Spawn Body", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isFa) "انصراف" else "Cancel", color = RedTheme.colors.textSecondary)
            }
        },
        title = {
            Text(
                text = if (isFa) "افزودن جرم جدید به شبیه‌ساز" else "Add Celestial Body",
                style = RedTypographyTokens.sectionHeading
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.md)) {
                OutlinedTextField(
                    value = if (isFa) nameFa else nameEn,
                    onValueChange = { if (isFa) nameFa = it else nameEn = it },
                    label = { Text(if (isFa) "نام جرم" else "Body Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (isFa) "نوع جرم:" else "Body Type:",
                    style = RedTypographyTokens.caption,
                    color = RedTheme.colors.textSecondary
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BodyType.entries) { type ->
                        FilterChip(
                            selected = type == selectedType,
                            onClick = { selectedType = type },
                            label = { Text(if (isFa) type.titleFa else type.titleEn) }
                        )
                    }
                }

                Text(
                    text = if (isFa) "ضریب جرم: ${massMultiplier}x" else "Mass Multiplier: ${massMultiplier}x",
                    style = RedTypographyTokens.caption,
                    color = RedTheme.colors.textSecondary
                )
                Slider(
                    value = massMultiplier,
                    onValueChange = { massMultiplier = it },
                    valueRange = 0.1f..10.0f
                )
            }
        },
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

private fun getSpatialScale(scenario: PresetScenario): Double {
    return when (scenario) {
        PresetScenario.SOLAR_SYSTEM, PresetScenario.LAGRANGE_POINTS -> 8.0e8 // 800,000 km per screen dp
        PresetScenario.BINARY_STAR, PresetScenario.FIGURE_EIGHT, PresetScenario.CHAOTIC_FOUR_BODY -> 1.5e9
        PresetScenario.BLACK_HOLE_SLINGSHOT -> 8.0e9
        PresetScenario.EMPTY_CANVAS -> 1.0e9
    }
}

private fun formatEnergy(joules: Double, isFa: Boolean): String {
    val exponent = floor(log10(abs(joules).coerceAtLeast(1.0)))
    val mantissa = joules / 10.0.pow(exponent)
    val str = String.format("%.2fe%.0f J", mantissa, exponent)
    return if (isFa) str.toPersianDigits() else str
}
