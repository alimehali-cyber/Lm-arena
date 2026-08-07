package com.alijafari.red.astronomy.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.*

enum class TidalTimeUnit(val labelEn: String, val labelFa: String, val hoursStep: Double) {
    HOUR("Hour", "ساعت", 1.0),
    DAY("Day", "روز", 24.0),
    WEEK("Week", "هفته", 168.0),
    MONTH("Month", "ماه", 720.0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TidalSimulatorView(
    isFa: Boolean,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Simulation state
    var isPlaying by remember { mutableStateOf(false) }
    var simSpeed by remember { mutableFloatStateOf(1.0f) } // hours per sec
    var selectedTimeUnit by remember { mutableStateOf(TidalTimeUnit.HOUR) }

    // Moon orbital angle relative to Sun direction (0 = New Moon / Spring, 90 = Quarter / Neap, 180 = Full / Spring)
    var moonAngleDeg by remember { mutableFloatStateOf(0.0f) }

    // Earth self-rotation angle (0 to 360 deg)
    var earthRotationDeg by remember { mutableFloatStateOf(0.0f) }

    // Sun angle in simulation (default 0 deg along +X axis)
    var sunAngleDeg by remember { mutableFloatStateOf(0.0f) }

    // Overlays state
    var showGravVectors by remember { mutableStateOf(true) }
    var showTidalForces by remember { mutableStateOf(true) }
    var showOrbitPath by remember { mutableStateOf(true) }
    var showSunVectors by remember { mutableStateOf(true) }

    // Tutorial modal state
    var showTutorial by remember { mutableStateOf(false) }

    // Time advancement loop
    LaunchedEffect(isPlaying, simSpeed) {
        if (!isPlaying) return@LaunchedEffect
        while (isActive && isPlaying) {
            delay(33) // ~30 fps
            val dtHours = (simSpeed * 0.05f).toDouble()
            // Earth rotates 15 deg per hour
            earthRotationDeg = (earthRotationDeg + dtHours * 15.0).toFloat() % 360.0f
            // Moon orbits Earth ~0.55 deg per hour (360 / 27.3 / 24)
            moonAngleDeg = (moonAngleDeg + dtHours * 0.55).toFloat() % 360.0f
        }
    }

    // Calculations for tidal type
    val moonAngleRad = Math.toRadians(moonAngleDeg.toDouble())
    // Phase alignment factor: 1 at 0 and 180 deg (Spring), 0 at 90 and 270 deg (Neap)
    val alignmentFactor = abs(cos(moonAngleRad))
    val isSpringTide = alignmentFactor > 0.7f
    val isNeapTide = alignmentFactor < 0.3f

    val tideTypeFa = when {
        isSpringTide -> "مَدّ کاهنده و افزاینده بهینه (مَدّ اکبر / Spring Tide)"
        isNeapTide -> "مَدّ کهین (Neap Tide - خورشید و ماه در زاویه ۹۰ درجه)"
        else -> "مَدّ معمولی (تلفیق گرانش خورشید و ماه)"
    }
    val tideTypeEn = when {
        isSpringTide -> "Spring Tide (Sun & Moon Aligned - Max Range)"
        isNeapTide -> "Neap Tide (Sun & Moon at 90° - Min Range)"
        else -> "Intermediate Tide (Combined Gravitational Pull)"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
            .testTag("tidal_simulator_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                            }
                        }
                        Column {
                            Text(
                                text = if (isFa) "شبیه‌ساز زنده مَدّ و جزر اقیانوسی" else "Live Tidal & Gravity Simulator",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isFa) tideTypeFa else tideTypeEn,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSpringTide) AccentPrimary else TextSecondary
                            )
                        }
                    }

                    // Tutorial Button
                    IconButton(
                        onClick = { showTutorial = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(AccentPrimary.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Help", tint = AccentPrimary)
                    }
                }
            }

            // Main Canvas Area (Interactive Orbit & Water Bulge)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Drag anywhere on canvas rotates the Moon around Earth
                            val sensitivity = 0.5f
                            moonAngleDeg = (moonAngleDeg + dragAmount.x * sensitivity) % 360.0f
                            if (moonAngleDeg < 0) moonAngleDeg += 360.0f
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val earthRadiusPx = min(size.width, size.height) * 0.14f
                    val moonOrbitRadiusPx = min(size.width, size.height) * 0.38f
                    val moonRadiusPx = earthRadiusPx * 0.28f

                    // Draw Background Starfield Effect
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF1E293B).copy(alpha = 0.3f), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = moonOrbitRadiusPx * 1.5f
                        ),
                        center = Offset(cx, cy),
                        radius = moonOrbitRadiusPx * 1.5f
                    )

                    // Draw Sun Direction Rays (Light coming from right/left)
                    val sunX = cx + cos(Math.toRadians(sunAngleDeg.toDouble())).toFloat() * (moonOrbitRadiusPx * 1.3f)
                    val sunY = cy + sin(Math.toRadians(sunAngleDeg.toDouble())).toFloat() * (moonOrbitRadiusPx * 1.3f)

                    if (showSunVectors) {
                        drawLine(
                            color = Color(0xFFFDE047).copy(alpha = 0.35f),
                            start = Offset(sunX, sunY),
                            end = Offset(cx, cy),
                            strokeWidth = 3f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                        )
                        drawCircle(
                            color = Color(0xFFFDE047),
                            radius = 16f,
                            center = Offset(sunX, sunY)
                        )
                    }

                    // Draw Moon Orbit Path
                    if (showOrbitPath) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.12f),
                            radius = moonOrbitRadiusPx,
                            center = Offset(cx, cy),
                            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                        )
                    }

                    // Calculate Moon Position
                    val moonRad = Math.toRadians(moonAngleDeg.toDouble())
                    val mx = cx + (moonOrbitRadiusPx * cos(moonRad)).toFloat()
                    val my = cy + (moonOrbitRadiusPx * sin(moonRad)).toFloat()

                    // Draw Ocean Bulge (Deformed Ellipse aligned with combined gravity vector)
                    // Moon pull is primary, Sun pull is secondary (0.46x)
                    val moonVecX = cos(moonRad)
                    val moonVecY = sin(moonRad)
                    val sunRad = Math.toRadians(sunAngleDeg.toDouble())
                    val sunVecX = cos(sunRad) * 0.46
                    val sunVecY = sin(sunRad) * 0.46

                    val totalVecX = moonVecX + sunVecX
                    val totalVecY = moonVecY + sunVecY
                    val tidalAngleRad = atan2(totalVecY, totalVecX)

                    // Bulge amplitude calculation
                    val bulgeScale = 1.0 + 0.25 * (sqrt(totalVecX * totalVecX + totalVecY * totalVecY) / 1.46)
                    val semimajor = (earthRadiusPx * bulgeScale).toFloat()
                    val semiminor = (earthRadiusPx * (2.0 - bulgeScale)).toFloat()

                    rotate(
                        degrees = Math.toDegrees(tidalAngleRad).toFloat(),
                        pivot = Offset(cx, cy)
                    ) {
                        // High tide water bulge
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF0284C7).copy(alpha = 0.7f), Color(0xFF38BDF8).copy(alpha = 0.3f)),
                                center = Offset(cx, cy),
                                radius = semimajor
                            ),
                            topLeft = Offset(cx - semimajor, cy - semiminor),
                            size = Size(semimajor * 2f, semiminor * 2f)
                        )
                        drawOval(
                            color = Color(0xFF38BDF8),
                            topLeft = Offset(cx - semimajor, cy - semiminor),
                            size = Size(semimajor * 2f, semiminor * 2f),
                            style = Stroke(width = 2f)
                        )
                    }

                    // Draw Solid Earth Sphere
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF1E3A8A), Color(0xFF0F172A)),
                            center = Offset(cx, cy),
                            radius = earthRadiusPx
                        ),
                        radius = earthRadiusPx,
                        center = Offset(cx, cy)
                    )

                    // Draw Earth Continents / Rotating Marker
                    rotate(degrees = earthRotationDeg, pivot = Offset(cx, cy)) {
                        // Green Continent Patches
                        drawCircle(
                            color = Color(0xFF15803D).copy(alpha = 0.8f),
                            radius = earthRadiusPx * 0.35f,
                            center = Offset(cx + earthRadiusPx * 0.4f, cy - earthRadiusPx * 0.2f)
                        )
                        drawCircle(
                            color = Color(0xFF166534).copy(alpha = 0.8f),
                            radius = earthRadiusPx * 0.25f,
                            center = Offset(cx - earthRadiusPx * 0.5f, cy + earthRadiusPx * 0.3f)
                        )
                        // Observer Reference Flag (Red Dot)
                        drawCircle(
                            color = Color(0xFFEF4444),
                            radius = 8f,
                            center = Offset(cx + earthRadiusPx * 0.85f, cy)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3f,
                            center = Offset(cx + earthRadiusPx * 0.85f, cy)
                        )
                    }

                    // Draw Moon Sphere & Phase Shadow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFE2E8F0), Color(0xFF64748B)),
                            center = Offset(mx, my),
                            radius = moonRadiusPx
                        ),
                        radius = moonRadiusPx,
                        center = Offset(mx, my)
                    )

                    // Moon Gravitational Vectors
                    if (showGravVectors) {
                        // Vector towards Moon
                        drawLine(
                            color = Color(0xFF38BDF8),
                            start = Offset(cx, cy),
                            end = Offset(mx, my),
                            strokeWidth = 4f
                        )
                        // Vector towards Sun
                        if (showSunVectors) {
                            drawLine(
                                color = Color(0xFFFDE047),
                                start = Offset(cx, cy),
                                end = Offset(sunX, sunY),
                                strokeWidth = 3f
                            )
                        }
                    }

                    // Differential Tidal Forces Overlay (Arrows pointing outwards along bulge axis)
                    if (showTidalForces) {
                        val bulgeAngle = tidalAngleRad
                        val arrowLen = 35f
                        val p1X = cx + semimajor * cos(bulgeAngle).toFloat()
                        val p1Y = cy + semimajor * sin(bulgeAngle).toFloat()
                        drawLine(
                            color = Color(0xFF34D399),
                            start = Offset(p1X, p1Y),
                            end = Offset(p1X + arrowLen * cos(bulgeAngle).toFloat(), p1Y + arrowLen * sin(bulgeAngle).toFloat()),
                            strokeWidth = 4f
                        )

                        val p2X = cx - semimajor * cos(bulgeAngle).toFloat()
                        val p2Y = cy - semimajor * sin(bulgeAngle).toFloat()
                        drawLine(
                            color = Color(0xFF34D399),
                            start = Offset(p2X, p2Y),
                            end = Offset(p2X - arrowLen * cos(bulgeAngle).toFloat(), p2Y - arrowLen * sin(bulgeAngle).toFloat()),
                            strokeWidth = 4f
                        )
                    }
                }

                // Drag Hint Overlay
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = if (isFa) "👆 لمس و کشیدن برای تغییر زاویه مدار ماه" else "👆 Drag anywhere to move Moon orbit",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Controls & Parameters Panel
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Quick Toggle Chips for Overlays
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = showGravVectors,
                            onClick = { showGravVectors = !showGravVectors },
                            label = { Text(if (isFa) "بردارهای گرانش" else "Gravity Vectors", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(14.dp)) }
                        )
                        FilterChip(
                            selected = showTidalForces,
                            onClick = { showTidalForces = !showTidalForces },
                            label = { Text(if (isFa) "نیروهای مَدی" else "Tidal Forces", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Waves, null, modifier = Modifier.size(14.dp)) }
                        )
                        FilterChip(
                            selected = showSunVectors,
                            onClick = { showSunVectors = !showSunVectors },
                            label = { Text(if (isFa) "بردار خورشید" else "Sun Pull", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.WbSunny, null, modifier = Modifier.size(14.dp)) }
                        )
                        FilterChip(
                            selected = showOrbitPath,
                            onClick = { showOrbitPath = !showOrbitPath },
                            label = { Text(if (isFa) "مسیر مدار" else "Orbit Path", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.PanoramaFishEye, null, modifier = Modifier.size(14.dp)) }
                        )
                    }

                    // Playback & Speed Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FloatingActionButton(
                                onClick = { isPlaying = !isPlaying },
                                containerColor = AccentPrimary,
                                contentColor = Color.Black,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause"
                                )
                            }

                            Column {
                                Text(
                                    text = if (isFa) "سرعت شبیه‌سازی: ${simSpeed.toInt()}×" else "Sim Speed: ${simSpeed.toInt()}x",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isPlaying) (if (isFa) "در حال اجرا..." else "Simulating...") else (if (isFa) "متوقف شد" else "Paused"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPlaying) AccentPrimary else TextSecondary
                                )
                            }
                        }

                        // Time Step Preset Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1f, 5f, 12f, 24f).forEach { speed ->
                                FilterChip(
                                    selected = simSpeed == speed,
                                    onClick = { simSpeed = speed },
                                    label = { Text("${speed.toInt()}h/s", fontSize = 11.sp) },
                                    shape = CircleShape
                                )
                            }
                        }
                    }

                    // Tidal Stats Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isFa) "زاویه ماه-خورشید" else "Moon-Sun Angle",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${moonAngleDeg.toInt()}°",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            VerticalDivider(modifier = Modifier.height(30.dp).width(1.dp), color = CardBorder)

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isFa) "ارتفاع نسبی مَدّ" else "Tidal Amplitude",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = String.format("%.2f m", 1.0 + 0.5 * alignmentFactor),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentPrimary
                                )
                            }

                            VerticalDivider(modifier = Modifier.height(30.dp).width(1.dp), color = CardBorder)

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isFa) "تاخیر روزانه" else "Daily Shift",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "~50 min/day",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Educational Tutorial Dialog
        if (showTutorial) {
            AlertDialog(
                onDismissRequest = { showTutorial = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = AccentPrimary)
                        Text(
                            text = if (isFa) "راهنمای علم مَدّ و جزر اقیانوسی" else "Scientific Guide to Tides",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isFa) "چرا مَدّ و جزر اتفاق می‌افتد؟" else "Why Do Tides Occur?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentPrimary
                        )
                        Text(
                            text = if (isFa)
                                "مَدّ و جزر به دلیل نابرابری کشش گرانشی ماه و خورشید در نقاط مختلف کره زمین ایجاد می‌شود. سمت رو به ماه گرانش بیشتری دریافت کرده و آب را به سمت خود می‌کشد."
                            else
                                "Tides are caused by the gravitational gradient exerted by the Moon and Sun across Earth. Ocean water bulges out toward the Moon on the near side.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Text(
                            text = if (isFa) "چرا دو برجستگی آبی وجود دارد؟" else "Why Are There Two Tidal Bulges?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentPrimary
                        )
                        Text(
                            text = if (isFa)
                                "یک برجستگی در سمت رو به ماه (کشش گرانشی) و برجستگی دیگر در سمت مقابل زمین به دلیل گرانش کمتر و نیروی گریز از مرکز مداری تشکیل می‌شود."
                            else
                                "One bulge forms on the side facing the Moon due to stronger gravity, while a second bulge forms on the opposite side due to inertia and weaker gravitational pull.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Text(
                            text = if (isFa) "تفاوت مَدّ اکبر (Spring) و مَدّ کهین (Neap):" else "Spring vs. Neap Tides:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentPrimary
                        )
                        Text(
                            text = if (isFa)
                                "• مَدّ اکبر (Spring Tide): هنگام ماه نو یا ماه کامل که خورشید، زمین و ماه در یک خط قرار دارند و گرانش آن‌ها یکدیگر را تقویت می‌کند.\n• مَدّ کهین (Neap Tide): در تربیع اول و سوم که ماه و خورشید زاویه ۹۰ درجه می‌سازند و گرانش یکدیگر را تضعیف می‌کنند."
                            else
                                "• Spring Tides: Occur during New and Full Moons when Sun and Moon align, reinforcing gravity for highest tides.\n• Neap Tides: Occur during First and Third Quarter Moons when Sun and Moon pull at 90° angles, damping tides.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showTutorial = false },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        Text(if (isFa) "متوجه شدم" else "Got it", color = Color.Black)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary
            )
        }
    }
}
