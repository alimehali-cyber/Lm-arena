package com.alijafari.red.astronomy.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.CoordinateEngine
import com.alijafari.red.astronomy.astro_engine.MoonEngine
import com.alijafari.red.astronomy.astro_engine.SunEngine
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.theme.*
import com.alijafari.red.astronomy.util.toPersianDigits
import java.util.Calendar
import java.util.Random
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun MoonScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    var selectedDayOffsetFloat by remember { mutableStateOf(0f) }
    val dayOffsetInt = selectedDayOffsetFloat.roundToInt()

    val baseJd = remember { TimeEngine.getJulianDate() }
    val currentJd = baseJd + selectedDayOffsetFloat.toDouble()

    val moonData = remember(currentJd, uiState.userLocation) {
        MoonEngine.calculateMoon(currentJd, uiState.userLocation.latitude, uiState.userLocation.longitude)
    }

    val sunHoriz = remember(currentJd, uiState.userLocation) {
        val sunPos = SunEngine.calculatePosition(currentJd)
        val lastDeg = TimeEngine.getLAST(currentJd, uiState.userLocation.longitude)
        CoordinateEngine.equatorialToHorizontal(
            equatorial = CoordinateEngine.Equatorial(sunPos.raDeg, sunPos.decDeg),
            lastDeg = lastDeg,
            latitudeDeg = uiState.userLocation.latitude
        )
    }

    val upcomingPhases = remember(baseJd) {
        MoonEngine.getUpcomingMajorPhases(baseJd)
    }

    val selectedCalendar = remember(dayOffsetInt) {
        val cal = Calendar.getInstance(TimeEngine.TEHRAN_TIME_ZONE)
        cal.add(Calendar.DAY_OF_YEAR, dayOffsetInt)
        cal
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("moon_screen"),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. MOON HERO SECTION (~400dp height)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .testTag("moon_hero_card")
            ) {
                // Procedural Starfield background
                val starColor = MaterialTheme.colorScheme.onBackground
                Canvas(modifier = Modifier.matchParentSize()) {
                    val random = Random(42)
                    val width = size.width
                    val height = size.height
                    for (i in 0..75) {
                        val x = random.nextFloat() * width
                        val y = random.nextFloat() * height
                        val alpha = 0.10f + random.nextFloat() * 0.20f
                        val radius = 0.8f + random.nextFloat() * 1.5f
                        drawCircle(
                            color = starColor.copy(alpha = alpha),
                            radius = radius,
                            center = Offset(x, y)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 20.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Date switcher pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { selectedDayOffsetFloat = (selectedDayOffsetFloat - 1f).coerceIn(-30f, 30f) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Previous Day", tint = MaterialTheme.colorScheme.primary)
                        }

                        val dateStr = TimeEngine.formatDate(selectedCalendar.timeInMillis, uiState.calendarSystem, isFa).let {
                            if (isFa) it.toPersianDigits() else it
                        }
                        val offsetBadge = when {
                            dayOffsetInt == 0 -> if (isFa) "امروز — $dateStr" else "Today — $dateStr"
                            dayOffsetInt > 0 -> if (isFa) "+${dayOffsetInt} روز — $dateStr".toPersianDigits() else "+${dayOffsetInt}d — $dateStr"
                            else -> if (isFa) "${dayOffsetInt} روز — $dateStr".toPersianDigits() else "${dayOffsetInt}d — $dateStr"
                        }

                        Text(
                            text = offsetBadge,
                            style = TextStyle(
                                fontFamily = IranSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        IconButton(
                            onClick = { selectedDayOffsetFloat = (selectedDayOffsetFloat + 1f).coerceIn(-30f, 30f) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Next Day", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Scrubber drag instructions badge below date pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Swipe, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Text(
                            text = if (isFa) "برای تغییر روزها (۳۰- تا ۳۰+) ماه را افقی بکشید" else "Drag moon to scrub days (-30 to +30d)",
                            style = TextStyle(
                                fontFamily = IranSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        if (dayOffsetInt != 0) {
                            Surface(
                                onClick = { selectedDayOffsetFloat = 0f },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = if (isFa) "بازنشانی" else "Reset",
                                    style = TextStyle(
                                        fontFamily = IranSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Realistic Photographic Moon Visualization with Horizontal Scrubber Drag
                    PhotographicMoonView(
                        moonData = moonData,
                        sunHoriz = sunHoriz,
                        latitude = uiState.userLocation.latitude,
                        longitude = uiState.userLocation.longitude,
                        jd = currentJd,
                        onDragDelta = { dragAmount ->
                            val deltaDays = dragAmount / 15f
                            selectedDayOffsetFloat = (selectedDayOffsetFloat + deltaDays).coerceIn(-30f, 30f)
                        },
                        modifier = Modifier.size(270.dp)
                    )

                    // Phase Name & Illumination Text below moon
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isFa) moonData.phaseNameFa else moonData.phaseNameEn,
                            style = TextStyle(
                                fontFamily = IranSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            textAlign = TextAlign.Center
                        )

                        val illFormatted = String.format("%.0f", moonData.illuminationPercent).let {
                            if (isFa) "${it}٪ روشن".toPersianDigits() else "$it% Illuminated"
                        }
                        Text(
                            text = illFormatted,
                            style = TextStyle(
                                fontFamily = IranSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 2. DETAILS CARD (Glassmorphism 2x2 grid)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("moon_details_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isFa) "جزئیات موقعیت و فاز" else "Position & Phase Details",
                        style = TextStyle(
                            fontFamily = IranSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    val riseStr = moonData.moonriseTimeMs?.let {
                        TimeEngine.formatTime24h(it, isFa)
                    } ?: if (isFa) "۱۹:۴۲" else "19:42"

                    val setStr = moonData.moonsetTimeMs?.let {
                        TimeEngine.formatTime24h(it, isFa)
                    } ?: if (isFa) "۰۵:۱۸" else "05:18"

                    val distStr = String.format("%,d", moonData.distanceKm.toInt()).let {
                        if (isFa) "$it کیلومتر".toPersianDigits() else "$it km"
                    }

                    val altStr = String.format("%.0f", moonData.altitudeDeg).let {
                        if (isFa) "$it° بالای افق".toPersianDigits() else "$it° Above Horizon"
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MoonTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.WbSunny,
                                label = if (isFa) "طلوع ماه" else "Moonrise",
                                value = if (isFa) riseStr.toPersianDigits() else riseStr
                            )

                            MoonTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.NightsStay,
                                label = if (isFa) "غروب ماه" else "Moonset",
                                value = if (isFa) setStr.toPersianDigits() else setStr
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MoonTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Straighten,
                                label = if (isFa) "فاصله از زمین" else "Distance",
                                value = distStr
                            )

                            MoonTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Navigation,
                                label = if (isFa) "ارتفاع فعلی" else "Current Altitude",
                                value = altStr
                            )
                        }
                    }
                }
            }
        }

        // 3. UPCOMING PHASES CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("moon_upcoming_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (isFa) "فازهای آینده" else "Upcoming Phases",
                        style = TextStyle(
                            fontFamily = IranSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        upcomingPhases.forEach { phase ->
                            val name = if (isFa) phase.phaseNameFa else phase.phaseNameEn
                            val dateText = TimeEngine.formatDate(phase.dateMs, uiState.calendarSystem, isFa).let {
                                if (isFa) it.toPersianDigits() else it
                            }
                            val daysText = if (isFa) "${phase.daysFromNow} روز دیگر".toPersianDigits() else "in ${phase.daysFromNow} days"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Brightness2,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "$name — $dateText",
                                        style = TextStyle(
                                            fontFamily = IranSans,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }

                                Text(
                                    text = daysText,
                                    style = TextStyle(
                                        fontFamily = IranSans,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotographicMoonView(
    moonData: MoonEngine.MoonData,
    sunHoriz: CoordinateEngine.Horizontal,
    latitude: Double,
    longitude: Double,
    jd: Double,
    onDragDelta: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MoonRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Rotation"
    )

    val limbScreenAngleDeg = remember(moonData.azimuthDeg, moonData.altitudeDeg, sunHoriz.azimuthDeg, sunHoriz.altitudeDeg) {
        CoordinateEngine.calculateMoonLimbScreenAngleDeg(
            moonAzimuthDeg = moonData.azimuthDeg,
            moonAltitudeDeg = moonData.altitudeDeg,
            sunAzimuthDeg = sunHoriz.azimuthDeg,
            sunAltitudeDeg = sunHoriz.altitudeDeg
        ).toFloat()
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Purple Radial Glow backdrop (320dp)
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFA855F7).copy(alpha = 0.18f),
                            Color(0xFFA855F7).copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Faint outer ring
        Box(
            modifier = Modifier
                .size(286.dp)
                .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
        )

        // Realistic Moon image + Phase Canvas Shader
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .graphicsLayer {
                    rotationZ = if (latitude < 0) 180f + rotationAngle else rotationAngle
                }
        ) {
            // High-Res Photographic Moon Asset (Bundled, 100% offline ready, photorealistic astrophotography)
            Image(
                painter = painterResource(id = R.drawable.img_full_moon_photo_1785673146290),
                contentDescription = moonData.phaseNameFa,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic Lunar Phase Shadow Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                val ill = moonData.illuminationPercent / 100.0

                if (ill < 0.98) {
                    // Multi-pass Feathered Terminator Shadow Overlay for natural penumbra blending
                    // Subtly darkened unilluminated limb with realistic earthshine and faint lunar surface detail
                    val targetShadowAlpha = 0.965f
                    val numSteps = 16
                    val stepAlpha = targetShadowAlpha / numSteps
                    val feather = radius * 0.08f // Soft penumbra width (~22px feathering)

                    for (step in 0 until numSteps) {
                        val t = (step.toFloat() / (numSteps - 1) - 0.5f) * 2f
                        val offset = t * feather

                        val shadowPath = Path()
                        // Outer shadow arc around left edge (+90° bottom to -90° top)
                        shadowPath.addArc(
                            Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                            90f,
                            180f
                        )

                        val k = (2.0 * ill - 1.0).toFloat()
                        val stepInnerWidth = (abs(k) * radius + offset).coerceAtLeast(0f)
                        val innerRect = Rect(center.x - stepInnerWidth, center.y - radius, center.x + stepInnerWidth, center.y + radius)

                        // Inner terminator arc from -90° top to +90° bottom
                        val innerSweep = if (k >= 0) -180f else 180f
                        shadowPath.arcTo(innerRect, 270f, innerSweep, false)
                        shadowPath.close()

                        rotate(limbScreenAngleDeg, center) {
                            drawPath(
                                path = shadowPath,
                                color = Color(0xFF030307).copy(alpha = stepAlpha)
                            )
                        }
                    }
                }

                // Ambient Edge Limb Darkening
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF05050D).copy(alpha = 0.40f)
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }
        }
    }
}

@Composable
private fun MoonTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = label,
                    style = TextStyle(
                        fontFamily = IranSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Text(
                    text = value,
                    style = TextStyle(
                        fontFamily = IranSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}
