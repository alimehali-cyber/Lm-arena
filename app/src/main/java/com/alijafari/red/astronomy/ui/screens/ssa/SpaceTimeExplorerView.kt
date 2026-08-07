package com.alijafari.red.astronomy.ui.screens.ssa

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.*
import com.alijafari.red.astronomy.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun SpaceTimeExplorerView(
    initialTimeMs: Long,
    isFa: Boolean
) {
    val textMeasurer = rememberTextMeasurer()

    // Time State
    var simulatedTimeMs by remember { mutableLongStateOf(initialTimeMs) }
    var speedMultiplier by remember { mutableDoubleStateOf(100.0) } // Default 100x speed
    var isPaused by remember { mutableStateOf(false) }

    // Selected Planet for Educational Overlay
    var selectedPlanet by remember { mutableStateOf<PlanetEngine.PlanetType>(PlanetEngine.PlanetType.MARS) }

    // Camera Drag Rotation
    var camPitchDeg by remember { mutableFloatStateOf(45.0f) }
    var camYawDeg by remember { mutableFloatStateOf(0.0f) }

    // Continuous Smooth Time Advancement Coroutine
    LaunchedEffect(isPaused, speedMultiplier) {
        var lastFrameTimeMs = System.currentTimeMillis()
        while (!isPaused) {
            delay(16) // ~60 FPS
            val now = System.currentTimeMillis()
            val dtRealSec = (now - lastFrameTimeMs) / 1000.0
            lastFrameTimeMs = now

            val dtSimulatedMs = (dtRealSec * speedMultiplier * 1000.0).toLong()
            simulatedTimeMs += dtSimulatedMs
        }
    }

    val jd = remember(simulatedTimeMs) { TimeEngine.getJulianDate(simulatedTimeMs) }

    // Calculate All Planet Positions & Trails
    val planetPositions = remember(jd) {
        PlanetEngine.PlanetType.values().map { planet ->
            val posKm = SolarSystem3DEngine.calculateHeliocentricPositionKm(planet, jd)
            val speed = SolarSystem3DEngine.calculateOrbitalSpeedKmS(planet, posKm.length())
            Pair(planet, Pair(posKm, speed))
        }.toMap()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    camYawDeg = (camYawDeg - dragAmount.x * 0.4f) % 360f
                    if (camYawDeg < 0) camYawDeg += 360f
                    camPitchDeg = (camPitchDeg + dragAmount.y * 0.4f).coerceIn(10f, 85f)
                }
            }
            .testTag("space_time_explorer_view")
    ) {
        // LAYER 1: Interactive 3D Solar System & Orbital Trails Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f

            val pitchRad = Math.toRadians(camPitchDeg.toDouble())
            val yawRad = Math.toRadians(camYawDeg.toDouble())

            val cosYaw = cos(yawRad)
            val sinYaw = sin(yawRad)
            val cosPitch = cos(pitchRad)
            val sinPitch = sin(pitchRad)

            val maxAU = 32.0 // Neptune orbit boundary
            val maxPixels = canvasWidth * 0.45f

            // Render Sun at Center
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFD166), Color(0xFFFF8800).copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(centerX, centerY),
                    radius = 35f * density
                ),
                radius = 35f * density,
                center = Offset(centerX, centerY)
            )
            drawCircle(color = Color.White, radius = 10f * density, center = Offset(centerX, centerY))

            // Render Planet Orbits and Positions
            for (planet in PlanetEngine.PlanetType.values()) {
                val pColor = when (planet) {
                    PlanetEngine.PlanetType.MERCURY -> Color(0xFFA3A3A3)
                    PlanetEngine.PlanetType.VENUS -> Color(0xFFFFD166)
                    PlanetEngine.PlanetType.MARS -> Color(0xFFEF4444)
                    PlanetEngine.PlanetType.JUPITER -> Color(0xFFF97316)
                    PlanetEngine.PlanetType.SATURN -> Color(0xFFEAB308)
                    PlanetEngine.PlanetType.URANUS -> Color(0xFF06B6D4)
                    PlanetEngine.PlanetType.NEPTUNE -> Color(0xFF3B82F6)
                    PlanetEngine.PlanetType.PLUTO -> Color(0xFFCBD5E1)
                    else -> Color(0xFF38BDF8)
                }

                // Render Orbit Trail Ellipse
                val trailPath = Path()
                var firstPoint = true

                for (angleStep in 0..360 step 5) {
                    val aRad = Math.toRadians(angleStep.toDouble())
                    val aAU = planet.semiMajorAxisAU
                    val xAU = aAU * cos(aRad)
                    val yAU = aAU * sin(aRad)
                    val zAU = 0.0

                    // Yaw & Pitch projection
                    val x1 = xAU * cosYaw - yAU * sinYaw
                    val y1 = xAU * sinYaw + yAU * cosYaw
                    val z1 = zAU

                    val x2 = x1
                    val y2 = y1 * cosPitch - z1 * sinPitch
                    val z2 = y1 * sinPitch + z1 * cosPitch

                    val px = (centerX + (x2 / maxAU * maxPixels)).toFloat()
                    val py = (centerY - (z2 / maxAU * maxPixels)).toFloat()

                    if (firstPoint) {
                        trailPath.moveTo(px, py)
                        firstPoint = false
                    } else {
                        trailPath.lineTo(px, py)
                    }
                }

                val isSelected = planet == selectedPlanet
                drawPath(
                    path = trailPath,
                    color = if (isSelected) pColor else pColor.copy(alpha = 0.25f),
                    style = Stroke(width = if (isSelected) 3.5f * density else 1.2f * density)
                )

                // Render Planet Current Position
                val (posKm, _) = planetPositions[planet] ?: continue
                val xAU = posKm.x / 149_597_870.7
                val yAU = posKm.y / 149_597_870.7
                val zAU = posKm.z / 149_597_870.7

                val x1 = xAU * cosYaw - yAU * sinYaw
                val y1 = xAU * sinYaw + yAU * cosYaw
                val z1 = zAU

                val x2 = x1
                val y2 = y1 * cosPitch - z1 * sinPitch
                val z2 = y1 * sinPitch + z1 * cosPitch

                val px = (centerX + (x2 / maxAU * maxPixels)).toFloat()
                val py = (centerY - (z2 / maxAU * maxPixels)).toFloat()

                val pRadiusPx = if (isSelected) 12f * density else 7f * density
                drawCircle(color = pColor.copy(alpha = 0.35f), radius = pRadiusPx * 1.8f, center = Offset(px, py))
                drawCircle(color = pColor, radius = pRadiusPx, center = Offset(px, py))

                if (isSelected) {
                    drawCircle(color = Color.White, radius = pRadiusPx + 4f, center = Offset(px, py), style = Stroke(width = 2f))
                }
            }
        }

        // LAYER 2: TOP TIME CONTROL BAR & PRESETS
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
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Timelapse, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
                            Text(
                                text = if (isFa) "کاوشگر زمان و فضا" else "Space-Time Explorer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        // Play / Pause Toggle Button
                        IconButton(onClick = { isPaused = !isPaused }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                tint = AccentPrimary
                            )
                        }
                    }

                    // Formatted Date Display
                    val utcDateStr = remember(simulatedTimeMs) { TimeEngine.formatDate(simulatedTimeMs, com.alijafari.red.astronomy.domain.CalendarSystem.GREGORIAN, isFa) }
                    Text(
                        text = if (isFa) "تاریخ رصد: $utcDateStr" else "Simulated Date: $utcDateStr",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentSecondary
                    )

                    // Speed Multiplier Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1.0, 10.0, 100.0, 1000.0, 10000.0).forEach { speed ->
                            FilterChip(
                                selected = speedMultiplier == speed,
                                onClick = { speedMultiplier = speed },
                                label = { Text(String.format("%.0fx", speed), fontSize = 11.sp) },
                                shape = CircleShape
                            )
                        }
                    }
                }
            }
        }

        // LAYER 3: BOTTOM EDUCATIONAL OVERLAY CARD
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Planet Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlanetEngine.PlanetType.values().forEach { planet ->
                    FilterChip(
                        selected = selectedPlanet == planet,
                        onClick = { selectedPlanet = planet },
                        label = { Text(if (isFa) planet.nameFa else planet.nameEn, fontSize = 12.sp) },
                        shape = CircleShape
                    )
                }
            }

            // Educational Parameters Card
            val (currentPosKm, currentSpeed) = planetPositions[selectedPlanet] ?: Pair(SolarSystem3DEngine.Vector3D(0.0, 0.0, 0.0), 0.0)
            val helioDistAU = currentPosKm.length() / 149_597_870.7

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = BackgroundCard.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, CardBorder),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFa) "اطلاعات مداری: ${selectedPlanet.nameFa}" else "Orbital Physics: ${selectedPlanet.nameEn}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = String.format("%.2f AU", helioDistAU),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentPrimary
                        )
                    }

                    Divider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(if (isFa) "دوره تناوب مداری:" else "Orbital Period:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(String.format(if (isFa) "%.2f سال" else "%.2f years", selectedPlanet.orbitalPeriodYears), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        Column {
                            Text(if (isFa) "سرعت مداری لحظه‌ای:" else "Orbital Velocity:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(String.format("%.1f km/s", currentSpeed), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AccentSecondary)
                        }

                        Column {
                            Text(if (isFa) "برخروج از مرکز:" else "Eccentricity:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(String.format("%.4f", selectedPlanet.eccentricity), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        Column {
                            Text(if (isFa) "انحراف مداری:" else "Inclination:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(String.format("%.2f°", selectedPlanet.inclinationDeg), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}
