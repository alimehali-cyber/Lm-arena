package com.alijafari.red.astronomy.ui.screens.ssa

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.GravitySandboxEngine
import com.alijafari.red.astronomy.ui.components.SSATutorialCategory
import com.alijafari.red.astronomy.ui.components.SSATutorialModal
import com.alijafari.red.astronomy.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun GravitySandboxView(
    isFa: Boolean
) {
    // Sandbox State
    val bodies = remember { mutableStateListOf<GravitySandboxEngine.SandboxBody>() }
    var selectedCollisionMode by remember { mutableStateOf(GravitySandboxEngine.CollisionMode.MERGE) }
    var simulationSpeedMultiplier by remember { mutableDoubleStateOf(1.0) }
    var isPaused by remember { mutableStateOf(false) }

    // Add Object Palette & Inspector State
    var activeObjectTypeToAdd by remember { mutableStateOf<GravitySandboxEngine.ObjectType?>(null) }
    var selectedBody by remember { mutableStateOf<GravitySandboxEngine.SandboxBody?>(null) }

    var showTutorialModal by remember { mutableStateOf(false) }
    var currentPresetId by remember { mutableStateOf("SOLAR_SYSTEM") }

    var canvasSizeWidth by remember { mutableFloatStateOf(1000f) }
    var canvasSizeHeight by remember { mutableFloatStateOf(1000f) }

    // Velocity Drag Arrow Creation State
    var dragStartOffset by remember { mutableStateOf<Offset?>(null) }
    var dragCurrentOffset by remember { mutableStateOf<Offset?>(null) }

    // Load initial preset
    LaunchedEffect(currentPresetId, canvasSizeWidth, canvasSizeHeight) {
        if (canvasSizeWidth > 100f && canvasSizeHeight > 100f) {
            bodies.clear()
            bodies.addAll(GravitySandboxEngine.getPresetScenario(currentPresetId, canvasSizeWidth, canvasSizeHeight))
        }
    }

    // Continuous Physics Coroutine Step Loop
    LaunchedEffect(isPaused, simulationSpeedMultiplier, selectedCollisionMode) {
        var lastFrameTimeMs = System.currentTimeMillis()
        while (true) {
            delay(16) // ~60 FPS
            val now = System.currentTimeMillis()
            val realDtSec = (now - lastFrameTimeMs) / 1000.0
            lastFrameTimeMs = now

            if (!isPaused && bodies.isNotEmpty()) {
                val dtEffective = (realDtSec * simulationSpeedMultiplier * 0.08).coerceIn(0.001, 0.2)
                GravitySandboxEngine.stepPhysics(
                    bodies = bodies,
                    dtSeconds = dtEffective,
                    collisionMode = selectedCollisionMode,
                    maxTrailPoints = 100
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
            .testTag("gravity_sandbox_view")
    ) {
        // LAYER 1: Physics Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeObjectTypeToAdd) {
                    detectTapGestures { tapOffset ->
                        val tappedBody = bodies.find { b ->
                            val dx = b.pos.x - tapOffset.x
                            val dy = b.pos.y - tapOffset.y
                            kotlin.math.sqrt(dx * dx + dy * dy) <= b.radius + 15f
                        }

                        if (tappedBody != null) {
                            selectedBody = tappedBody
                        } else if (activeObjectTypeToAdd != null) {
                            // Spawn new body at tap position
                            val type = activeObjectTypeToAdd!!
                            val newBody = GravitySandboxEngine.SandboxBody(
                                id = java.util.UUID.randomUUID().toString(),
                                name = if (isFa) type.labelFa else type.labelEn,
                                type = type,
                                mass = when (type) {
                                    GravitySandboxEngine.ObjectType.SUN -> 2000.0
                                    GravitySandboxEngine.ObjectType.PLANET -> 50.0
                                    GravitySandboxEngine.ObjectType.MOON -> 2.0
                                    GravitySandboxEngine.ObjectType.BLACK_HOLE -> 8000.0
                                    else -> 10.0
                                },
                                radius = when (type) {
                                    GravitySandboxEngine.ObjectType.SUN -> 24f
                                    GravitySandboxEngine.ObjectType.PLANET -> 12f
                                    GravitySandboxEngine.ObjectType.BLACK_HOLE -> 20f
                                    else -> 8f
                                },
                                pos = GravitySandboxEngine.Vector2D(tapOffset.x.toDouble(), tapOffset.y.toDouble()),
                                vel = GravitySandboxEngine.Vector2D(0.0, 0.0),
                                color = type.defaultColor
                            )
                            bodies.add(newBody)
                            selectedBody = newBody
                            activeObjectTypeToAdd = null
                        } else {
                            selectedBody = null
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragStartOffset = offset
                            dragCurrentOffset = offset
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragCurrentOffset = change.position
                        },
                        onDragEnd = {
                            if (dragStartOffset != null && dragCurrentOffset != null && selectedBody != null) {
                                val dx = (dragCurrentOffset!!.x - dragStartOffset!!.x) * 0.8
                                val dy = (dragCurrentOffset!!.y - dragStartOffset!!.y) * 0.8
                                selectedBody!!.vel = GravitySandboxEngine.Vector2D(dx.toDouble(), dy.toDouble())
                            }
                            dragStartOffset = null
                            dragCurrentOffset = null
                        }
                    )
                }
        ) {
            canvasSizeWidth = size.width
            canvasSizeHeight = size.height

            // Render Fading Trails
            for (body in bodies) {
                if (body.trail.size > 1) {
                    val trailPath = Path()
                    trailPath.moveTo(body.trail.first().x.toFloat(), body.trail.first().y.toFloat())
                    for (pt in body.trail.drop(1)) {
                        trailPath.lineTo(pt.x.toFloat(), pt.y.toFloat())
                    }
                    drawPath(
                        path = trailPath,
                        color = body.color.copy(alpha = 0.45f),
                        style = Stroke(width = 2.5f * density)
                    )
                }
            }

            // Render Bodies
            for (body in bodies) {
                val bx = body.pos.x.toFloat()
                val by = body.pos.y.toFloat()
                val isSelected = selectedBody?.id == body.id

                if (body.type == GravitySandboxEngine.ObjectType.BLACK_HOLE) {
                    // Black Hole Lensing Accretion Glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color(0xFFA855F7).copy(alpha = 0.5f), Color(0xFF1E1B4B), Color.Black),
                            center = Offset(bx, by),
                            radius = body.radius * 3.5f
                        ),
                        radius = body.radius * 3.5f,
                        center = Offset(bx, by)
                    )
                    drawCircle(color = Color.Black, radius = body.radius, center = Offset(bx, by))
                    drawCircle(color = Color(0xFFA855F7), radius = body.radius + 2f, center = Offset(bx, by), style = Stroke(width = 2f))
                } else {
                    // Soft Radial Glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(body.color, body.color.copy(alpha = 0.3f), Color.Transparent),
                            center = Offset(bx, by),
                            radius = body.radius * 2.2f
                        ),
                        radius = body.radius * 2.2f,
                        center = Offset(bx, by)
                    )

                    drawCircle(color = body.color, radius = body.radius, center = Offset(bx, by))
                }

                if (isSelected) {
                    drawCircle(color = Color.White, radius = body.radius + 6f, center = Offset(bx, by), style = Stroke(width = 2f * density))
                }
            }

            // Render Swipe Velocity Vector Arrow
            if (dragStartOffset != null && dragCurrentOffset != null) {
                drawLine(
                    color = AccentPrimary,
                    start = dragStartOffset!!,
                    end = dragCurrentOffset!!,
                    strokeWidth = 3f * density
                )
                drawCircle(color = AccentPrimary, radius = 6f * density, center = dragCurrentOffset!!)
            }
        }

        // LAYER 2: TOP CONTROL BAR (Presets & Tutorial)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp, start = 12.dp, end = 12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = BackgroundCard.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, CardBorder),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
                            Text(
                                text = if (isFa) "آزمایشگاه گرانش نیوتونی" else "Gravity Sandbox",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { isPaused = !isPaused }, modifier = Modifier.size(32.dp)) {
                                Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null, tint = AccentPrimary)
                            }
                            IconButton(onClick = { showTutorialModal = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = AccentPrimary)
                            }
                        }
                    }

                    // Presets Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "SOLAR_SYSTEM" to (if (isFa) "منظومه شمسی" else "Solar System"),
                            "EARTH_MOON" to (if (isFa) "زمین و ماه" else "Earth-Moon"),
                            "JUPITER_MOONS" to (if (isFa) "اقمار مشتری" else "Jupiter Moons"),
                            "BINARY_STARS" to (if (isFa) "ستارگان دوگانه" else "Binary Stars"),
                            "LAGRANGE_POINTS" to (if (isFa) "نقاط لاگرانژی" else "Lagrange Points"),
                            "THREE_BODY_CHAOS" to (if (isFa) "آشوب سه جسمی" else "3-Body Chaos"),
                            "SLINGSHOT" to (if (isFa) "مانور قلاب‌سنگی" else "Slingshot")
                        ).forEach { (presetKey, presetName) ->
                            FilterChip(
                                selected = currentPresetId == presetKey,
                                onClick = { currentPresetId = presetKey },
                                label = { Text(presetName, fontSize = 11.sp) },
                                shape = CircleShape
                            )
                        }
                    }

                    // Speed Multipliers Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1.0, 10.0, 100.0, 1000.0, 10000.0, 100000.0).forEach { speed ->
                            FilterChip(
                                selected = simulationSpeedMultiplier == speed,
                                onClick = { simulationSpeedMultiplier = speed },
                                label = { Text(if (speed >= 1000) "${(speed / 1000).toInt()}k×" else "${speed.toInt()}×", fontSize = 11.sp) },
                                shape = CircleShape
                            )
                        }
                    }
                }
            }
        }

        // LAYER 3: BOTTOM TOOLBAR & ADD OBJECT PALETTE
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Add Object Palette Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GravitySandboxEngine.ObjectType.values().forEach { objType ->
                    FilterChip(
                        selected = activeObjectTypeToAdd == objType,
                        onClick = { activeObjectTypeToAdd = if (activeObjectTypeToAdd == objType) null else objType },
                        label = { Text("+ ${if (isFa) objType.labelFa else objType.labelEn}", fontSize = 12.sp) },
                        shape = CircleShape
                    )
                }
            }

            // Controls & Inspector Bar
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = BackgroundCard.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, CardBorder),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isFa) "اجرام فعال: ${bodies.size}" else "Active Bodies: ${bodies.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isFa) selectedCollisionMode.labelFa else selectedCollisionMode.labelEn,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Collision Mode Toggle Button
                        OutlinedButton(
                            onClick = {
                                val modes = GravitySandboxEngine.CollisionMode.values()
                                val nextIdx = (selectedCollisionMode.ordinal + 1) % modes.size
                                selectedCollisionMode = modes[nextIdx]
                            },
                            shape = CircleShape,
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Icon(Icons.Default.BlurCircular, contentDescription = "Collision Mode", tint = AccentSecondary, modifier = Modifier.size(16.dp))
                        }

                        // Clear All Button
                        Button(
                            onClick = { bodies.clear(); selectedBody = null },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Tutorial Modal
        if (showTutorialModal) {
            SSATutorialModal(
                category = SSATutorialCategory.GRAVITY_SANDBOX,
                isFa = isFa,
                onDismiss = { showTutorialModal = false }
            )
        }
    }
}
