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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.*
import com.alijafari.red.astronomy.ui.components.SSATutorialCategory
import com.alijafari.red.astronomy.ui.components.SSATutorialModal
import com.alijafari.red.astronomy.ui.theme.*
import kotlin.math.*

@Composable
fun ScaleWalkView(
    activeTimeMs: Long,
    userLatDeg: Double,
    userLonDeg: Double,
    isFa: Boolean,
    onOpenObjectDetail: ((PlanetEngine.PlanetType) -> Unit)? = null
) {
    val textMeasurer = rememberTextMeasurer()

    // Walking Scale State
    var selectedScale by remember { mutableStateOf(SolarSystem3DEngine.WalkingScale.TEN_MILLION_KM) }
    var radiusExaggerationFactor by remember { mutableFloatStateOf(15.0f) } // Default 15x radius exaggeration for easy visibility

    // User Virtual AR Camera Position in Meters relative to Sun at (0,0,0)
    var userCamX by remember { mutableDoubleStateOf(0.0) }
    var userCamY by remember { mutableDoubleStateOf(0.0) }
    var userCamZ by remember { mutableDoubleStateOf(0.0) }

    // User Walking Step Counter
    var userStepsTaken by remember { mutableIntStateOf(0) }

    // Camera Pitch & Yaw Drag Angles
    var camPitchDeg by remember { mutableFloatStateOf(15.0f) }
    var camYawDeg by remember { mutableFloatStateOf(0.0f) }

    // Selected Planet for Floating Detail Modal
    var selectedPlanetForModal by remember { mutableStateOf<SolarSystem3DEngine.Planet3DData?>(null) }
    var showTutorialModal by remember { mutableStateOf(false) }

    val jd = remember(activeTimeMs) { TimeEngine.getJulianDate(activeTimeMs) }

    // Calculate 3D Solar System Dataset
    val planetDataset = remember(jd, selectedScale, radiusExaggerationFactor) {
        SolarSystem3DEngine.calculateSolarSystem3D(
            jd = jd,
            scale = selectedScale,
            stepMeters = 0.8,
            radiusExaggerationFactor = radiusExaggerationFactor.toDouble()
        )
    }

    // Calculate Jovian Moons Dataset
    val jupiterPlanetData = planetDataset.find { it.planet == PlanetEngine.PlanetType.JUPITER }
    val jovianMoons = remember(jd, jupiterPlanetData, selectedScale) {
        jupiterPlanetData?.let { jup ->
            SolarSystem3DEngine.calculateJovianMoons3D(
                jd = jd,
                jupiterPosMeters = jup.scaledPositionMeters,
                scale = selectedScale,
                stepMeters = 0.8
            )
        } ?: emptyList()
    }

    // Reset Origin Handler
    val handleResetOrigin = {
        userCamX = 0.0
        userCamY = 0.0
        userCamZ = 0.0
        userStepsTaken = 0
        camPitchDeg = 15.0f
        camYawDeg = 0.0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    camYawDeg = (camYawDeg - dragAmount.x * 0.3f) % 360f
                    if (camYawDeg < 0) camYawDeg += 360f
                    camPitchDeg = (camPitchDeg + dragAmount.y * 0.3f).coerceIn(-85f, 85f)
                }
            }
            .testTag("scale_walk_view")
    ) {
        // LAYER 1: Interactive 3D Solar System Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f

            val fovPixels = canvasWidth * 0.9f

            val yawRad = Math.toRadians(camYawDeg.toDouble())
            val pitchRad = Math.toRadians(camPitchDeg.toDouble())

            val cosYaw = cos(yawRad)
            val sinYaw = sin(yawRad)
            val cosPitch = cos(pitchRad)
            val sinPitch = sin(pitchRad)

            // Draw Orbit Ring Lines
            for (pData in planetDataset) {
                val orbitRadiusMeters = pData.scaledPositionMeters.length()
                if (orbitRadiusMeters > 0.1) {
                    val path = Path()
                    var firstPoint = true

                    for (angleStep in 0..360 step 6) {
                        val rad = Math.toRadians(angleStep.toDouble())
                        val ox = orbitRadiusMeters * cos(rad)
                        val oy = orbitRadiusMeters * sin(rad)
                        val oz = 0.0

                        // Rel to user camera
                        val rx = ox - userCamX
                        val ry = oy - userCamY
                        val rz = oz - userCamZ

                        // Yaw rotation
                        val x1 = rx * cosYaw - ry * sinYaw
                        val y1 = rx * sinYaw + ry * cosYaw
                        val z1 = rz

                        // Pitch rotation
                        val x2 = x1
                        val y2 = y1 * cosPitch - z1 * sinPitch
                        val z2 = y1 * sinPitch + z1 * cosPitch

                        if (y2 > 0.1) {
                            val px = centerX + (x2 / y2 * fovPixels).toFloat()
                            val py = centerY - (z2 / y2 * fovPixels).toFloat()

                            if (firstPoint) {
                                path.moveTo(px, py)
                                firstPoint = false
                            } else {
                                path.lineTo(px, py)
                            }
                        }
                    }

                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.18f),
                        style = Stroke(width = 1.2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f)))
                    )
                }
            }

            // Project Sun Origin (0,0,0)
            val sunRx = 0.0 - userCamX
            val sunRy = 0.0 - userCamY
            val sunRz = 0.0 - userCamZ

            val sunX1 = sunRx * cosYaw - sunRy * sinYaw
            val sunY1 = sunRx * sinYaw + sunRy * cosYaw
            val sunZ1 = sunRz

            val sunX2 = sunX1
            val sunY2 = sunY1 * cosPitch - sunZ1 * sinPitch
            val sunZ2 = sunY1 * sinPitch + sunZ1 * cosPitch

            if (sunY2 > 0.1) {
                val sunPx = centerX + (sunX2 / sunY2 * fovPixels).toFloat()
                val sunPy = centerY - (sunZ2 / sunY2 * fovPixels).toFloat()

                // Render Sun Coronal Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF066), Color(0xFFFF8800).copy(alpha = 0.4f), Color.Transparent),
                        center = Offset(sunPx, sunPy),
                        radius = 80f * density
                    ),
                    radius = 80f * density,
                    center = Offset(sunPx, sunPy)
                )

                drawCircle(color = Color.White, radius = 22f * density, center = Offset(sunPx, sunPy))
            }

            // Project and Render Planets (sorted from furthest to nearest relative to user camera)
            val sortedPlanets = planetDataset.sortedByDescending {
                val dx = it.scaledPositionMeters.x - userCamX
                val dy = it.scaledPositionMeters.y - userCamY
                val dz = it.scaledPositionMeters.z - userCamZ
                sqrt(dx * dx + dy * dy + dz * dz)
            }

            for (pData in sortedPlanets) {
                val rx = pData.scaledPositionMeters.x - userCamX
                val ry = pData.scaledPositionMeters.y - userCamY
                val rz = pData.scaledPositionMeters.z - userCamZ

                val x1 = rx * cosYaw - ry * sinYaw
                val y1 = rx * sinYaw + ry * cosYaw
                val z1 = rz

                val x2 = x1
                val y2 = y1 * cosPitch - z1 * sinPitch
                val z2 = y1 * sinPitch + z1 * cosPitch

                if (y2 > 0.1) {
                    val px = centerX + (x2 / y2 * fovPixels).toFloat()
                    val py = centerY - (z2 / y2 * fovPixels).toFloat()

                    if (px in -100f..(canvasWidth + 100f) && py in -100f..(canvasHeight + 100f)) {
                        val pDistMeters = sqrt(rx * rx + ry * ry + rz * rz)
                        val pDistKm = pDistMeters * (selectedScale.kmPerStep / 0.8)

                        val radiusPx = (pData.displayRadiusMeters / y2 * fovPixels * 0.1).toFloat().coerceIn(8f, 75f)

                        val planetColor = when (pData.planet) {
                            PlanetEngine.PlanetType.MERCURY -> Color(0xFFA3A3A3)
                            PlanetEngine.PlanetType.VENUS -> Color(0xFFFFD166)
                            PlanetEngine.PlanetType.MARS -> Color(0xFFEF4444)
                            PlanetEngine.PlanetType.JUPITER -> Color(0xFFF97316)
                            PlanetEngine.PlanetType.SATURN -> Color(0xFFEAB308)
                            PlanetEngine.PlanetType.URANUS -> Color(0xFF38BDF8)
                            PlanetEngine.PlanetType.NEPTUNE -> Color(0xFF3B82F6)
                            PlanetEngine.PlanetType.PLUTO -> Color(0xFFCBD5E1)
                        }

                        // Special Rendering: Earth Day/Night + Moon
                        if (pData.planet == PlanetEngine.PlanetType.MERCURY /* planet type marker placeholder */) {
                            // Render standard planet disc
                            drawCircle(color = planetColor.copy(alpha = 0.35f), radius = radiusPx * 1.6f, center = Offset(px, py))
                            drawCircle(color = planetColor, radius = radiusPx, center = Offset(px, py))
                        } else if (pData.planet == PlanetEngine.PlanetType.SATURN) {
                            // Render Saturn Rings
                            drawOval(
                                color = Color(0xFFFEF08A).copy(alpha = 0.7f),
                                topLeft = Offset(px - radiusPx * 2.5f, py - radiusPx * 0.8f),
                                size = Size(radiusPx * 5f, radiusPx * 1.6f),
                                style = Stroke(width = 4f * density)
                            )
                            drawCircle(color = planetColor, radius = radiusPx, center = Offset(px, py))
                        } else {
                            drawCircle(color = planetColor.copy(alpha = 0.35f), radius = radiusPx * 1.5f, center = Offset(px, py))
                            drawCircle(color = planetColor, radius = radiusPx, center = Offset(px, py))
                        }

                        // Floating Name & Telemetry Label
                        val labelText = if (isFa) pData.planet.nameFa else pData.planet.nameEn
                        val distText = if (pDistKm > 1_000_000) String.format("%.1fM km", pDistKm / 1_000_000.0) else String.format("%.0fk km", pDistKm / 1_000.0)

                        val layoutResult = textMeasurer.measure(
                            text = "$labelText ($distText)",
                            style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )

                        val labelX = px - layoutResult.size.width / 2f
                        val labelY = py + radiusPx + 8f

                        drawRoundRect(
                            color = BackgroundCard.copy(alpha = 0.88f),
                            topLeft = Offset(labelX - 8f, labelY - 4f),
                            size = Size(layoutResult.size.width + 16f, layoutResult.size.height + 8f),
                            cornerRadius = CornerRadius(10f, 10f)
                        )

                        drawText(
                            textLayoutResult = layoutResult,
                            topLeft = Offset(labelX, labelY)
                        )
                    }
                }
            }

            // Render Jovian Moons around Jupiter
            for (jMoon in jovianMoons) {
                val rx = jMoon.scaledPositionMeters.x - userCamX
                val ry = jMoon.scaledPositionMeters.y - userCamY
                val rz = jMoon.scaledPositionMeters.z - userCamZ

                val x1 = rx * cosYaw - ry * sinYaw
                val y1 = rx * sinYaw + ry * cosYaw
                val z1 = rz

                val x2 = x1
                val y2 = y1 * cosPitch - z1 * sinPitch
                val z2 = y1 * sinPitch + z1 * cosPitch

                if (y2 > 0.1) {
                    val mx = centerX + (x2 / y2 * fovPixels).toFloat()
                    val my = centerY - (z2 / y2 * fovPixels).toFloat()

                    if (mx in -50f..(canvasWidth + 50f) && my in -50f..(canvasHeight + 50f)) {
                        val moonColor = when (jMoon.moon) {
                            JupiterMoonsEngine.GalileanMoon.IO -> Color(0xFFFACC15)
                            JupiterMoonsEngine.GalileanMoon.EUROPA -> Color(0xFF38BDF8)
                            JupiterMoonsEngine.GalileanMoon.GANYMEDE -> Color(0xFFA855F7)
                            JupiterMoonsEngine.GalileanMoon.CALLISTO -> Color(0xFF64748B)
                            else -> Color.White
                        }
                        drawCircle(color = moonColor, radius = 4f * density, center = Offset(mx, my))
                    }
                }
            }
        }

        // LAYER 2: TOP CONTROL BAR (Scale Selector & Tutorial)
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
                            Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (isFa) "پیمایش مقیاس‌دار منظومه شمسی" else "Solar System Scale Walk",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        IconButton(onClick = { showTutorialModal = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Scale Picker Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SolarSystem3DEngine.WalkingScale.values().forEach { scaleOption ->
                            FilterChip(
                                selected = selectedScale == scaleOption,
                                onClick = { selectedScale = scaleOption },
                                label = { Text(if (isFa) scaleOption.nameFa else scaleOption.nameEn, fontSize = 11.sp) },
                                shape = CircleShape
                            )
                        }
                    }

                    // Radius Exaggeration Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isFa) "بزرگ‌نمایی شعاع:" else "Radius Scale:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Slider(
                            value = radiusExaggerationFactor,
                            onValueChange = { radiusExaggerationFactor = it },
                            valueRange = 1.0f..100.0f,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format("%.0fx", radiusExaggerationFactor),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentSecondary
                        )
                    }
                }
            }
        }

        // LAYER 3: BOTTOM AR VIRTUAL STEPPING CONTROLS
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Planet Quick Selector Cards Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                planetDataset.forEach { pData ->
                    Surface(
                        onClick = {
                            selectedPlanetForModal = pData
                            onOpenObjectDetail?.invoke(pData.planet)
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = BackgroundCard.copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(14.dp))
                            Text(
                                text = if (isFa) pData.planet.nameFa else pData.planet.nameEn,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Virtual Step Controls & Reset Origin
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
                            text = if (isFa) "گام‌های رصدگر: $userStepsTaken" else "Steps Taken: $userStepsTaken",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isFa) selectedScale.labelFa else selectedScale.labelEn,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Walk Backward Step Button
                        IconButton(
                            onClick = {
                                userStepsTaken--
                                val stepDistMeters = 0.8
                                val yawRad = Math.toRadians(camYawDeg.toDouble())
                                userCamX -= sin(yawRad) * stepDistMeters
                                userCamY -= cos(yawRad) * stepDistMeters
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(NavyBackground, CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Step Back", tint = TextPrimary)
                        }

                        // Walk Forward Step Button
                        Button(
                            onClick = {
                                userStepsTaken++
                                val stepDistMeters = 0.8
                                val yawRad = Math.toRadians(camYawDeg.toDouble())
                                userCamX += sin(yawRad) * stepDistMeters
                                userCamY += cos(yawRad) * stepDistMeters
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                            shape = CircleShape
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.DirectionsWalk, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(if (isFa) "قدم به جلو" else "Step Forward", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        // Reset Origin Button
                        OutlinedButton(
                            onClick = handleResetOrigin,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset Origin", tint = AccentSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // LAYER 4: Planet Detail Dialog
        selectedPlanetForModal?.let { pData ->
            AlertDialog(
                onDismissRequest = { selectedPlanetForModal = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = AccentPrimary)
                        Text(if (isFa) pData.planet.nameFa else pData.planet.nameEn, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (isFa) pData.planet.descriptionFa else pData.planet.descriptionEn, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Divider(color = CardBorder)
                        Text(String.format(if (isFa) "• فاصله تا خورشید: %.2f AU" else "• Distance to Sun: %.2f AU", pData.heliocentricDistanceAU), style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                        Text(String.format(if (isFa) "• سرعت مداری: %.1f km/s" else "• Orbital Speed: %.1f km/s", pData.orbitalSpeedKmS), style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                        Text(String.format(if (isFa) "• قطر استوایی: %.0f km" else "• Equatorial Diameter: %.0f km", pData.radiusKm * 2.0), style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                        Text(String.format(if (isFa) "• فاصله تا زمین: %.2f AU" else "• Distance to Earth: %.2f AU", pData.geocentricDistanceAU), style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    }
                },
                confirmButton = {
                    Button(onClick = { selectedPlanetForModal = null }, colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)) {
                        Text(if (isFa) "بستن" else "Close", color = Color.White)
                    }
                },
                containerColor = BackgroundCard,
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Tutorial Modal
        if (showTutorialModal) {
            SSATutorialModal(
                category = SSATutorialCategory.SCALE_WALK,
                isFa = isFa,
                onDismiss = { showTutorialModal = false }
            )
        }
    }
}
