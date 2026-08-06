package com.alijafari.red.astronomy.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.alijafari.red.astronomy.astro_engine.MoonEngine
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.theme.*
import com.alijafari.red.astronomy.util.toPersianDigits
import java.util.Calendar
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class CelestialMode {
    MOON,
    EARTH
}

@Composable
fun MoonScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    var selectedMode by remember { mutableStateOf(CelestialMode.MOON) }

    var selectedDayOffsetFloat by remember { mutableStateOf(0f) }
    val dayOffsetInt = selectedDayOffsetFloat.roundToInt()

    val baseJd = remember { TimeEngine.getJulianDate() }
    val currentJd = baseJd + selectedDayOffsetFloat.toDouble()

    // Observer location on Moon (default: Sea of Tranquility / Apollo 11: 0.67°N, 23.47°E)
    var observerLunarLat by remember { mutableStateOf(0.67) }
    var observerLunarLon by remember { mutableStateOf(23.47) }

    val moonData = remember(currentJd, uiState.userLocation) {
        MoonEngine.calculateMoon(currentJd, uiState.userLocation.latitude, uiState.userLocation.longitude)
    }

    val earthData = remember(currentJd, observerLunarLat, observerLunarLon) {
        MoonEngine.calculateEarthFromMoon(currentJd, observerLunarLat, observerLunarLon)
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
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // TOP SEGMENTED SWITCHER BAR
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 20.dp, end = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(4.dp)
                            .wrapContentWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Moon Option
                        Surface(
                            onClick = { selectedMode = CelestialMode.MOON },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selectedMode == CelestialMode.MOON) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🌔",
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (isFa) "دید از زمین (ماه)" else "Moon View",
                                    style = TextStyle(
                                        fontFamily = IranSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (selectedMode == CelestialMode.MOON) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        // Earth Option
                        Surface(
                            onClick = { selectedMode = CelestialMode.EARTH },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selectedMode == CelestialMode.EARTH) Color(0xFF0284C7) else Color.Transparent,
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🌍",
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (isFa) "دید از ماه (زمین)" else "Earth from Moon",
                                    style = TextStyle(
                                        fontFamily = IranSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (selectedMode == CelestialMode.EARTH) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Animated Content Switcher between Moon and Earth Mode
        item {
            AnimatedContent(
                targetState = selectedMode,
                label = "CelestialModeTransition"
            ) { mode ->
                when (mode) {
                    CelestialMode.MOON -> {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            // 1. MOON HERO SECTION
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
                                            color = Color.White.copy(alpha = alpha),
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
                                    DateSwitcherPill(
                                        selectedCalendar = selectedCalendar,
                                        dayOffsetInt = dayOffsetInt,
                                        isFa = isFa,
                                        calendarSystem = uiState.calendarSystem,
                                        onOffsetChange = { selectedDayOffsetFloat = it }
                                    )

                                    // Instruction badge
                                    InstructionBadge(
                                        isFa = isFa,
                                        dayOffsetInt = dayOffsetInt,
                                        onReset = { selectedDayOffsetFloat = 0f },
                                        textFa = "برای تغییر روزها (۳۰- تا ۳۰+) ماه را افقی بکشید",
                                        textEn = "Drag moon to scrub days (-30 to +30d)"
                                    )

                                    // Photographic Moon View
                                    PhotographicMoonView(
                                        moonData = moonData,
                                        latitude = uiState.userLocation.latitude,
                                        onDragDelta = { dragAmount ->
                                            val deltaDays = dragAmount / 15f
                                            selectedDayOffsetFloat = (selectedDayOffsetFloat + deltaDays).coerceIn(-30f, 30f)
                                        },
                                        modifier = Modifier.size(270.dp)
                                    )

                                    // Phase Name & Illumination Text
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

                            // 2. MOON DETAILS CARD
                            MoonDetailsCard(
                                moonData = moonData,
                                isFa = isFa
                            )

                            // 3. UPCOMING PHASES CARD
                            UpcomingPhasesCard(
                                upcomingPhases = upcomingPhases,
                                calendarSystem = uiState.calendarSystem,
                                isFa = isFa
                            )
                        }
                    }

                    CelestialMode.EARTH -> {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            // 1. EARTH HERO SECTION
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF031024),
                                                MaterialTheme.colorScheme.background
                                            )
                                        )
                                    )
                                    .testTag("earth_hero_card")
                            ) {
                                // Starfield Canvas
                                Canvas(modifier = Modifier.matchParentSize()) {
                                    val random = Random(88)
                                    val width = size.width
                                    val height = size.height
                                    for (i in 0..85) {
                                        val x = random.nextFloat() * width
                                        val y = random.nextFloat() * height
                                        val alpha = 0.15f + random.nextFloat() * 0.25f
                                        val radius = 0.8f + random.nextFloat() * 1.6f
                                        drawCircle(
                                            color = Color.White.copy(alpha = alpha),
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
                                    DateSwitcherPill(
                                        selectedCalendar = selectedCalendar,
                                        dayOffsetInt = dayOffsetInt,
                                        isFa = isFa,
                                        calendarSystem = uiState.calendarSystem,
                                        onOffsetChange = { selectedDayOffsetFloat = it }
                                    )

                                    // Instruction badge
                                    InstructionBadge(
                                        isFa = isFa,
                                        dayOffsetInt = dayOffsetInt,
                                        onReset = { selectedDayOffsetFloat = 0f },
                                        textFa = "برای چرخش زمین یا تغییر زمان، کره زمین را بکشید",
                                        textEn = "Drag Earth to rotate globe & scrub time"
                                    )

                                    // Interactive Photographic Earth View
                                    PhotographicEarthView(
                                        earthData = earthData,
                                        onDragDelta = { dragAmount ->
                                            val deltaDays = dragAmount / 15f
                                            selectedDayOffsetFloat = (selectedDayOffsetFloat + deltaDays).coerceIn(-30f, 30f)
                                        },
                                        modifier = Modifier.size(270.dp)
                                    )

                                    // Phase Name & Illumination Text
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (isFa) earthData.phaseNameFa else earthData.phaseNameEn,
                                            style = TextStyle(
                                                fontFamily = IranSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 22.sp,
                                                color = Color.White
                                            ),
                                            textAlign = TextAlign.Center
                                        )

                                        val illFormatted = String.format("%.0f", earthData.illuminationPercent).let {
                                            if (isFa) "زمین‌تاب: ${it}٪ روشن (دید از روی ماه)".toPersianDigits() else "Earth Phase: $it% Illuminated (from Moon)"
                                        }
                                        Text(
                                            text = illFormatted,
                                            style = TextStyle(
                                                fontFamily = IranSans,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF38BDF8)
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Earthrise / Earthset Visibility Status Box
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (earthData.isVisibleFromObserver) Color(0xFF0284C7).copy(alpha = 0.18f) else Color(0xFFEF4444).copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, if (earthData.isVisibleFromObserver) Color(0xFF38BDF8).copy(alpha = 0.4f) else Color(0xFFEF4444).copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (earthData.isVisibleFromObserver) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                tint = if (earthData.isVisibleFromObserver) Color(0xFF38BDF8) else Color(0xFFEF4444),
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Column {
                                                val statusHeader = if (earthData.isVisibleFromObserver) {
                                                    if (isFa) "زمین در آسمان معلق است (موقعیت: سمت پیدای ماه)" else "Earth suspended in Sky (Near Side Location)"
                                                } else {
                                                    if (isFa) "زمین از این موقعیت ماه هرگز دیده نمی‌شود" else "Earth is never visible from this lunar location"
                                                }
                                                val statusSub = if (earthData.isVisibleFromObserver) {
                                                    val alt = String.format("%.1f°", earthData.earthAltitudeInLunarSky).let { if (isFa) it.toPersianDigits() else it }
                                                    val az = String.format("%.1f°", earthData.earthAzimuthInLunarSky).let { if (isFa) it.toPersianDigits() else it }
                                                    if (isFa) "ارتفاع زمین: $alt • سمت: $az" else "Earth Alt: $alt • Azimuth: $az"
                                                } else {
                                                    if (isFa) "پشت ماه (Far Side) — پوسته ماه مانع دیدن زمین است" else "Far Side of Moon — Lunar globe blocks line of sight"
                                                }
                                                Text(
                                                    text = statusHeader,
                                                    style = TextStyle(
                                                        fontFamily = IranSans,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = Color.White
                                                    )
                                                )
                                                Text(
                                                    text = statusSub,
                                                    style = TextStyle(
                                                        fontFamily = IranSans,
                                                        fontSize = 11.sp,
                                                        color = Color.White.copy(alpha = 0.7f)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. EARTH TIMELINE JUMP PRESETS
                            EarthTimelinePresets(
                                isFa = isFa,
                                onJumpToOffset = { offset -> selectedDayOffsetFloat = offset }
                            )

                            // 3. EARTH DETAILS CARD
                            EarthDetailsCard(
                                earthData = earthData,
                                observerLat = observerLunarLat,
                                observerLon = observerLunarLon,
                                isFa = isFa,
                                onObserverLocationChanged = { lat, lon ->
                                    observerLunarLat = lat
                                    observerLunarLon = lon
                                }
                            )

                            // 4. EDUCATIONAL EXPANDABLE CARDS FOR EARTH
                            EarthEducationalCards(isFa = isFa)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateSwitcherPill(
    selectedCalendar: Calendar,
    dayOffsetInt: Int,
    isFa: Boolean,
    calendarSystem: com.alijafari.red.astronomy.domain.CalendarSystem,
    onOffsetChange: (Float) -> Unit
) {
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
            onClick = { onOffsetChange((dayOffsetInt - 1).toFloat().coerceIn(-30f, 30f)) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Previous Day", tint = MaterialTheme.colorScheme.primary)
        }

        val dateStr = TimeEngine.formatDate(selectedCalendar.timeInMillis, calendarSystem, isFa).let {
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
            onClick = { onOffsetChange((dayOffsetInt + 1).toFloat().coerceIn(-30f, 30f)) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Next Day", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun InstructionBadge(
    isFa: Boolean,
    dayOffsetInt: Int,
    onReset: () -> Unit,
    textFa: String,
    textEn: String
) {
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
            text = if (isFa) textFa else textEn,
            style = TextStyle(
                fontFamily = IranSans,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        if (dayOffsetInt != 0) {
            Surface(
                onClick = onReset,
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
}

@Composable
private fun PhotographicEarthView(
    earthData: MoonEngine.EarthFromMoonData,
    onDragDelta: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var cloudRotationAngle by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(100L)
            cloudRotationAngle = (cloudRotationAngle + 0.15f) % 360f
        }
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
        // Cyan Atmospheric Halo Glow
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.22f),
                            Color(0xFF0284C7).copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Outer Ring
        Box(
            modifier = Modifier
                .size(286.dp)
                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.2f), CircleShape)
        )

        // Earth Sphere Canvas
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .graphicsLayer {
                    rotationZ = 23.4f // Real Earth axial tilt relative to ecliptic
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // Ocean Blue Base Sphere
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1E3A8A),
                            Color(0xFF0F172A)
                        ),
                        center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
                        radius = radius * 1.4f
                    ),
                    radius = radius,
                    center = center
                )

                // Simplified Continents Polygons Rendering on Earth Globe
                val subLon = earthData.subsolarLongitude
                val continentColor = Color(0xFF22C55E).copy(alpha = 0.85f)

                // Draw continents based on rotation
                val continents = listOf(
                    // Eurasia
                    listOf(Pair(60.0, 10.0), Pair(70.0, 60.0), Pair(50.0, 100.0), Pair(30.0, 120.0), Pair(10.0, 80.0), Pair(30.0, 40.0)),
                    // Americas
                    listOf(Pair(60.0, -120.0), Pair(40.0, -80.0), Pair(10.0, -80.0), Pair(-30.0, -60.0), Pair(-50.0, -70.0), Pair(20.0, -100.0)),
                    // Africa
                    listOf(Pair(30.0, 30.0), Pair(10.0, 40.0), Pair(-30.0, 30.0), Pair(-30.0, 15.0), Pair(0.0, 10.0), Pair(15.0, -15.0))
                )

                for (cont in continents) {
                    val path = Path()
                    var first = true
                    for ((lat, lon) in cont) {
                        val relLon = (lon - subLon + 540.0) % 360.0 - 180.0
                        if (relLon in -90.0..90.0) {
                            val x = center.x + (radius * cos(Math.toRadians(lat)) * sin(Math.toRadians(relLon))).toFloat()
                            val y = center.y - (radius * sin(Math.toRadians(lat))).toFloat()
                            if (first) {
                                path.moveTo(x, y)
                                first = false
                            } else {
                                path.lineTo(x, y)
                            }
                        }
                    }
                    if (!first) {
                        path.close()
                        drawPath(path = path, color = continentColor)
                    }
                }

                // Procedural Cloud Swirl Overlay
                val cloudPath = Path()
                val cloudAngle = Math.toRadians(cloudRotationAngle.toDouble())
                for (i in 0..360 step 30) {
                    val rad = Math.toRadians(i.toDouble())
                    val cx = center.x + (radius * 0.75f * cos(rad + cloudAngle)).toFloat()
                    val cy = center.y + (radius * 0.5f * sin(rad * 2 + cloudAngle)).toFloat()
                    drawCircle(
                        color = Color.White.copy(alpha = 0.25f),
                        radius = radius * 0.25f,
                        center = Offset(cx, cy)
                    )
                }

                // Earth Day/Night Terminator Shadow Overlay according to Earth Phase
                val ill = earthData.illuminationPercent / 100.0
                if (ill < 0.98) {
                    val targetShadowAlpha = 0.94f
                    val numSteps = 16
                    val stepAlpha = targetShadowAlpha / numSteps
                    val feather = radius * 0.08f

                    for (step in 0 until numSteps) {
                        val t = (step.toFloat() / (numSteps - 1) - 0.5f) * 2f
                        val offset = t * feather

                        val shadowPath = Path()
                        val sweepAngle = 180f
                        val startAngle = -90f

                        shadowPath.addArc(
                            Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                            startAngle,
                            sweepAngle
                        )

                        val k = (2.0 * ill - 1.0).toFloat()
                        val stepInnerWidth = (abs(k) * radius + offset).coerceAtLeast(0f)
                        val innerRect = Rect(center.x - stepInnerWidth, center.y - radius, center.x + stepInnerWidth, center.y + radius)

                        val innerSweep = if (k >= 0) -180f else 180f
                        shadowPath.arcTo(innerRect, 90f, innerSweep, false)
                        shadowPath.close()

                        drawPath(
                            path = shadowPath,
                            color = Color(0xFF020617).copy(alpha = stepAlpha)
                        )
                    }
                }

                // Atmosphere Limb Edge Reflection
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF38BDF8).copy(alpha = 0.45f)
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
private fun EarthTimelinePresets(
    isFa: Boolean,
    onJumpToOffset: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isFa) "پرش‌های زمانی خط زمانی زمین" else "Earth Timeline Historical Jumps",
                style = TextStyle(
                    fontFamily = IranSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Apollo 11
                Surface(
                    onClick = { onJumpToOffset(-20820f) }, // July 1969
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFB703).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFFFFB703).copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🚀", fontSize = 16.sp)
                        Text(
                            text = if (isFa) "آپولو ۱۱ (۱۹۶۹)" else "Apollo 11",
                            style = TextStyle(fontFamily = IranSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFFFB703))
                        )
                    }
                }

                // Apollo 8 Earthrise
                Surface(
                    onClick = { onJumpToOffset(-21040f) }, // Dec 1968
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📸", fontSize = 16.sp)
                        Text(
                            text = if (isFa) "طلوع زمین (۱۹۶۸)" else "Earthrise '68",
                            style = TextStyle(fontFamily = IranSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF38BDF8))
                        )
                    }
                }

                // Today
                Surface(
                    onClick = { onJumpToOffset(0f) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📍", fontSize = 16.sp)
                        Text(
                            text = if (isFa) "زمان زنده (امروز)" else "Live Today",
                            style = TextStyle(fontFamily = IranSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EarthDetailsCard(
    earthData: MoonEngine.EarthFromMoonData,
    observerLat: Double,
    observerLon: Double,
    isFa: Boolean,
    onObserverLocationChanged: (Double, Double) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isFa) "مشخصات نجومی زمین از دید ماه" else "Earth Telemetry as Seen from Moon",
                style = TextStyle(
                    fontFamily = IranSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            // Observer Location Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isFa) "موقعیت ناظر روی سطح ماه:" else "Lunar Observer Site:",
                        style = TextStyle(fontFamily = IranSans, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = if (observerLon > -90 && observerLon < 90)
                            if (isFa) "دریای آرامش (Sea of Tranquility 0.67°N, 23.47°E)" else "Sea of Tranquility (0.67°N, 23.47°E)"
                        else
                            if (isFa) "دهانه جکسون / سمت پنهان ماه (Jackson Crater 22.4°N, 163.1°W)" else "Jackson Crater (Far Side)",
                        style = TextStyle(fontFamily = IranSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    )
                }

                TextButton(
                    onClick = {
                        if (observerLon > 0) {
                            onObserverLocationChanged(22.4, -163.1) // Switch to Far Side
                        } else {
                            onObserverLocationChanged(0.67, 23.47) // Switch to Near Side
                        }
                    }
                ) {
                    Text(
                        text = if (isFa) "تغییر موقعیت" else "Switch Site",
                        style = TextStyle(fontFamily = IranSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            // Grid Tiles
            val distStr = String.format("%,d", earthData.distanceKm.toInt()).let { if (isFa) "$it کیلومتر".toPersianDigits() else "$it km" }
            val angSizeStr = String.format("%.1f'", earthData.angularDiameterArcmin).let { if (isFa) "$it دقیقه قوسی (~۱.۹°)".toPersianDigits() else "$it arcmin (~1.9°)" }
            val subsolarStr = String.format("%.1f°, %.1f°", earthData.subsolarLatitude, earthData.subsolarLongitude).let { if (isFa) it.toPersianDigits() else it }
            val raDecStr = String.format("%.1f°, %.1f°", earthData.earthRaDeg, earthData.earthDecDeg).let { if (isFa) it.toPersianDigits() else it }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MoonTile(modifier = Modifier.weight(1f), icon = Icons.Default.Straighten, label = if (isFa) "فاصله تا ماه" else "Distance", value = distStr)
                    MoonTile(modifier = Modifier.weight(1f), icon = Icons.Default.Fullscreen, label = if (isFa) "قطر ظاهری (دید از ماه)" else "Angular Diameter", value = angSizeStr)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MoonTile(modifier = Modifier.weight(1f), icon = Icons.Default.WbSunny, label = if (isFa) "نقطه زیرخورشیدی زمین" else "Subsolar Point", value = subsolarStr)
                    MoonTile(modifier = Modifier.weight(1f), icon = Icons.Default.Explore, label = if (isFa) "بعد و میل استوایی (RA/Dec)" else "Earth RA / Dec", value = raDecStr)
                }
            }
        }
    }
}

@Composable
private fun EarthEducationalCards(isFa: Boolean) {
    val cards = listOf(
        Pair(
            if (isFa) "چرا زمین از روی ماه دارای فازها و حالت‌ها است؟" else "Why Earth has phases from the Moon",
            if (isFa)
                "زمین exactly همانند ماه فاز دارد زیرا نصف کره زمین همواره توسط خورشید روشن می‌شود. چون فاز زمین و ماه دقیقاً مکمل یکدیگرند، هنگامی که ناظر روی زمین «ماه نو» می‌بیند، ناظر روی ماه «زمین کامل (Full Earth)» می‌بیند!"
            else
                "Earth exhibits phases viewed from the Moon for the exact same geometric reason the Moon has phases from Earth. Earth's phase is always the precise mathematical inverse of the Moon's phase seen from Earth."
        ),
        Pair(
            if (isFa) "چرا زمین در آسمان ماه تقریباً ثابت و معلق می‌ماند؟" else "Why Earth hangs nearly motionless in lunar sky",
            if (isFa)
                "زیرا ماه به دلیل قفل جزرومدی (Tidal Locking) همواره یک روی خود را به سمت زمین نگه می‌دارد. بنابراین اگر ناظر روی سمت پیدای ماه بایستد، زمین همواره در یک جای ثابت در آسمان معلق خواهد بود!"
            else
                "Because the Moon is tidally locked to Earth, completing one full rotation in the exact time it orbits Earth. Consequently, for an observer on the near side of the Moon, Earth remains permanently fixed at nearly the exact same spot in the lunar sky."
        ),
        Pair(
            if (isFa) "تاثیر رخ‌گردی (Libration) و نوسان آونگی زمین" else "How Libration causes Earth to wobble slightly",
            if (isFa)
                "به دلیل بیضوی بودن مدار ماه و انحراف مداری، زمین در آسمان ماه نوسان ظریف آونگی حدود ۶ تا ۸ درجه انجام می‌دهد. این نوسان باعث طلوع و غروب آهسته زمین در مناطق مرزی (Limb) ماه می‌شود."
            else
                "Due to the Moon's elliptical orbit and axial tilt, Earth slowly sways back and forth by ~6 to 8 degrees in the lunar sky in a phenomenon known as libration."
        ),
        Pair(
            if (isFa) "طلوع زمین (Earthrise) به روایت فضانوردان آپولو" else "How astronauts witnessed 'Earthrise'",
            if (isFa)
                "فضانوردان آپولو ۸ و ۱۱ طلوع شکوهمند زمین را هنگامی دیدند که فضاپیمای آن‌ها در مدار ماه می‌چرخید. عکس تاریخی Earthrise که توسط ویلیام اندرس گرفته شد، یکی از تاثیرگذارترین تصاویر تاریخ بشر است."
            else
                "Apollo astronauts experienced the famous 'Earthrise' not by standing still on the lunar surface, but while orbiting around the Moon in their spacecraft."
        ),
        Pair(
            if (isFa) "چرا زمین ۴ برابر بزرگتر از ماه به نظر می‌رسد؟" else "Why Earth looks 4x larger than the Moon",
            if (isFa)
                "قطر زمین حدود ۱۲٬۷۴۲ کیلومتر است که تقریباً ۳.۷ برابر قطر ماه (۳٬۴۷۴ کیلومتر) می‌باشد. بنابراین زمین در آسمان ماه مساحتی حدود ۱۴ برابر بزرگتر از ماه در آسمان زمین اشغال می‌کند!"
            else
                "Earth's diameter (12,742 km) is ~3.7 times larger than the Moon's (3,474 km). In the lunar sky, Earth spans nearly 1.9 degrees, covering ~14 times the surface area of the Full Moon."
        ),
        Pair(
            if (isFa) "چرا درخشندگی زمین‌تاب بسیار شدیدتر از ماه‌تاب است؟" else "Why Earth shines 40-50x brighter than Full Moon",
            if (isFa)
                "زمین نه‌تنها ۴ برابر بزرگتر است، بلکه به دلیل وجود ابرها، یخ‌ها و اقیانوس‌ها آلبدو (ضریب بازتاب) بسیار بالاتری (~۳۷٪) در مقایسه با سنگ‌های تاریک ماه (~۱۲٪) دارد."
            else
                "Earth's high reflectivity (albedo ~37% from clouds, oceans, ice) combined with its larger size means 'Earthshine' illuminates the lunar night with 40 to 50 times the brightness of the Full Moon!"
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isFa) "🎓 کارت‌های آموزشی نجوم زمین و ماه" else "🎓 Earth & Moon Educational Insights",
                style = TextStyle(
                    fontFamily = IranSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            cards.forEach { (title, desc) ->
                var expanded by remember { mutableStateOf(false) }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                style = TextStyle(fontFamily = IranSans, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (expanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = desc,
                                style = TextStyle(fontFamily = IranSans, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoonDetailsCard(
    moonData: MoonEngine.MoonData,
    isFa: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("moon_details_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isFa) "جزئیات موقعیت و فاز ماه" else "Position & Phase Details",
                style = TextStyle(
                    fontFamily = IranSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            val riseStr = moonData.moonriseTimeMs?.let { TimeEngine.formatTime24h(it, isFa) } ?: if (isFa) "۱۹:۴۲" else "19:42"
            val setStr = moonData.moonsetTimeMs?.let { TimeEngine.formatTime24h(it, isFa) } ?: if (isFa) "۰۵:۱۸" else "05:18"
            val distStr = String.format("%,d", moonData.distanceKm.toInt()).let { if (isFa) "$it کیلومتر".toPersianDigits() else "$it km" }
            val altStr = String.format("%.0f", moonData.altitudeDeg).let { if (isFa) "$it° بالای افق".toPersianDigits() else "$it° Above Horizon" }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    MoonTile(modifier = Modifier.weight(1f), icon = Icons.Default.WbSunny, label = if (isFa) "طلوع ماه" else "Moonrise", value = if (isFa) riseStr.toPersianDigits() else riseStr)
                    MoonTile(modifier = Modifier.weight(1f), icon = Icons.Default.NightsStay, label = if (isFa) "غروب ماه" else "Moonset", value = if (isFa) setStr.toPersianDigits() else setStr)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    MoonTile(modifier = Modifier.weight(1f), icon = Icons.Default.Straighten, label = if (isFa) "فاصله از زمین" else "Distance", value = distStr)
                    MoonTile(modifier = Modifier.weight(1f), icon = Icons.Default.Navigation, label = if (isFa) "ارتفاع فعلی" else "Current Altitude", value = altStr)
                }
            }
        }
    }
}

@Composable
private fun UpcomingPhasesCard(
    upcomingPhases: List<MoonEngine.UpcomingPhaseInfo>,
    calendarSystem: com.alijafari.red.astronomy.domain.CalendarSystem,
    isFa: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("moon_upcoming_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isFa) "فازهای آینده" else "Upcoming Phases",
                style = TextStyle(fontFamily = IranSans, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                upcomingPhases.forEach { phase ->
                    val name = if (isFa) phase.phaseNameFa else phase.phaseNameEn
                    val dateText = TimeEngine.formatDate(phase.dateMs, calendarSystem, isFa).let { if (isFa) it.toPersianDigits() else it }
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(imageVector = Icons.Default.Brightness2, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(text = "$name — $dateText", style = TextStyle(fontFamily = IranSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface))
                        }
                        Text(text = daysText, style = TextStyle(fontFamily = IranSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotographicMoonView(
    moonData: MoonEngine.MoonData,
    latitude: Double,
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
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFA855F7).copy(alpha = 0.18f), Color(0xFFA855F7).copy(alpha = 0.04f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(286.dp)
                .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
        )

        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .graphicsLayer { rotationZ = if (latitude < 0) 180f + rotationAngle else rotationAngle }
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_full_moon_photo_1785673146290),
                contentDescription = moonData.phaseNameFa,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                val ill = moonData.illuminationPercent / 100.0
                val age = moonData.ageDays
                val isWaxing = age < 14.765

                if (ill < 0.98) {
                    val targetShadowAlpha = 0.92f
                    val numSteps = 16
                    val stepAlpha = targetShadowAlpha / numSteps
                    val feather = radius * 0.08f

                    for (step in 0 until numSteps) {
                        val t = (step.toFloat() / (numSteps - 1) - 0.5f) * 2f
                        val offset = t * feather

                        val shadowPath = Path()
                        val sweepAngle = 180f
                        val startAngle = if (isWaxing) 90f else -90f

                        shadowPath.addArc(
                            Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                            startAngle,
                            sweepAngle
                        )

                        val k = (2.0 * ill - 1.0).toFloat()
                        val stepInnerWidth = (abs(k) * radius + offset).coerceAtLeast(0f)
                        val innerRect = Rect(center.x - stepInnerWidth, center.y - radius, center.x + stepInnerWidth, center.y + radius)

                        if (isWaxing) {
                            val innerSweep = if (k >= 0) -180f else 180f
                            shadowPath.arcTo(innerRect, 270f, innerSweep, false)
                        } else {
                            val innerSweep = if (k >= 0) -180f else 180f
                            shadowPath.arcTo(innerRect, 90f, innerSweep, false)
                        }
                        shadowPath.close()

                        drawPath(path = shadowPath, color = Color(0xFF07070F).copy(alpha = stepAlpha))
                    }
                }

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0xFF0A0A12).copy(alpha = 0.35f)),
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
                Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }

            Column {
                Text(text = label, style = TextStyle(fontFamily = IranSans, fontWeight = FontWeight.Normal, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                Text(text = value, style = TextStyle(fontFamily = IranSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface))
            }
        }
    }
}
