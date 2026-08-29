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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    var selectedDayOffsetFloat by remember { mutableFloatStateOf(0f) }
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
        modifier = modifier
            .fillMaxSize()
            .testTag("moon_screen"),
        contentPadding = PaddingValues(horizontal = RedSpacing.lg, vertical = RedSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(RedSpacing.xl)
    ) {
        // 1. MOON HERO SECTION (Clean, unboxed & breathable)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = RedSpacing.xs)
                    .testTag("moon_hero_card"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RedSpacing.md)
            ) {
                // Date Navigator Pill
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(RedTheme.colors.surfaceGrouped)
                        .border(0.75.dp, RedTheme.colors.border, CircleShape)
                        .padding(horizontal = RedSpacing.xs, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RedSpacing.xs)
                ) {
                    IconButton(
                        onClick = { selectedDayOffsetFloat = (selectedDayOffsetFloat - 1f).coerceIn(-30f, 30f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFa) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Day",
                            tint = RedTheme.colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
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
                        style = RedTypographyTokens.caption.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = RedTheme.colors.textPrimary
                        ),
                        modifier = Modifier.padding(horizontal = RedSpacing.xs)
                    )

                    IconButton(
                        onClick = { selectedDayOffsetFloat = (selectedDayOffsetFloat + 1f).coerceIn(-30f, 30f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFa) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Day",
                            tint = RedTheme.colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Interactive Scrubber Drag Pill
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(RedTheme.colors.accentRed.copy(alpha = 0.08f))
                        .border(0.75.dp, RedTheme.colors.accentRed.copy(alpha = 0.2f), CircleShape)
                        .padding(horizontal = RedSpacing.md, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RedSpacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Swipe,
                        contentDescription = null,
                        tint = RedTheme.colors.accentRed,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (isFa) "برای جابجایی روزها ماه را افقی بکشید" else "Swipe horizontally to scrub days",
                        style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                        color = RedTheme.colors.textPrimary
                    )
                    if (dayOffsetInt != 0) {
                        Surface(
                            onClick = { selectedDayOffsetFloat = 0f },
                            shape = RoundedCornerShape(RedCornerRadius.xs),
                            color = RedTheme.colors.accentRed.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (isFa) "امروز" else "Reset",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = RedTheme.colors.accentRed
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
                        val deltaDays = dragAmount / 16f
                        selectedDayOffsetFloat = (selectedDayOffsetFloat + deltaDays).coerceIn(-30f, 30f)
                    },
                    modifier = Modifier.size(260.dp)
                )

                // Phase Name & Illumination Text below moon
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (isFa) moonData.phaseNameFa else moonData.phaseNameEn,
                        style = RedTypographyTokens.sectionHeading.copy(fontSize = 22.sp),
                        color = RedTheme.colors.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    val illFormatted = String.format("%.0f", moonData.illuminationPercent).let {
                        if (isFa) "${it}٪ روشن".toPersianDigits() else "$it% Illuminated"
                    }
                    Text(
                        text = illFormatted,
                        style = RedTypographyTokens.bodyPrimary.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = RedTheme.colors.accentRed
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 2. ORBITAL & TIMING DETAILS SECTION (Clean & unboxed)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("moon_details_card"),
                verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)
            ) {
                RedSectionHeader(
                    title = if (isFa) "مشخصات مداری و زمان‌بندی" else "Orbital & Timing Parameters",
                    subtitle = if (isFa) "طلوع، غروب، فاصله و مشخصات فیزیکی" else "Rise, set, distance & physical ephemeris"
                )

                val riseStr = moonData.moonriseTimeMs?.let {
                    TimeEngine.formatTime24h(it, isFa)
                } ?: if (isFa) "--:--" else "--:--"

                val setStr = moonData.moonsetTimeMs?.let {
                    TimeEngine.formatTime24h(it, isFa)
                } ?: if (isFa) "--:--" else "--:--"

                val distStr = String.format("%,d", moonData.distanceKm.toInt()).let {
                    if (isFa) "$it کیلومتر".toPersianDigits() else "$it km"
                }

                val altStr = String.format("%.1f", moonData.altitudeDeg).let {
                    if (isFa) "$it°".toPersianDigits() else "$it°"
                }

                val azStr = String.format("%.1f", moonData.azimuthDeg).let {
                    if (isFa) "$it°".toPersianDigits() else "$it°"
                }

                val ageStr = String.format("%.1f", moonData.ageDays).let {
                    if (isFa) "$it روز".toPersianDigits() else "$it days"
                }

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = RedSpacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MoonMetricItem(
                            icon = Icons.Outlined.WbSunny,
                            label = if (isFa) "طلوع ماه" else "Moonrise",
                            value = if (isFa) riseStr.toPersianDigits() else riseStr,
                            modifier = Modifier.weight(1f)
                        )
                        MoonMetricItem(
                            icon = Icons.Outlined.NightsStay,
                            label = if (isFa) "غروب ماه" else "Moonset",
                            value = if (isFa) setStr.toPersianDigits() else setStr,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    RedHairlineDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = RedSpacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MoonMetricItem(
                            icon = Icons.Outlined.Straighten,
                            label = if (isFa) "فاصله از زمین" else "Distance",
                            value = distStr,
                            modifier = Modifier.weight(1f)
                        )
                        MoonMetricItem(
                            icon = Icons.Outlined.Schedule,
                            label = if (isFa) "سن ماه" else "Lunar Age",
                            value = ageStr,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    RedHairlineDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = RedSpacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MoonMetricItem(
                            icon = Icons.Outlined.Navigation,
                            label = if (isFa) "ارتفاع مداری" else "Altitude",
                            value = altStr,
                            modifier = Modifier.weight(1f)
                        )
                        MoonMetricItem(
                            icon = Icons.Outlined.Explore,
                            label = if (isFa) "سمت (زاویه افقی)" else "Azimuth",
                            value = azStr,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. PHYSICAL & EPHEMERIS PROPERTIES SECTION (Clean & unboxed)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("moon_ephemeris_card"),
                verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)
            ) {
                RedSectionHeader(
                    title = if (isFa) "پارامترهای فیزیکی و رصدی" else "Physical & Observational Data",
                    subtitle = if (isFa) "قطر ظاهری، رخ‌گردی و درخشش زمین‌تاب" else "Angular diameter, libration & earthshine"
                )

                val angDiamStr = String.format("%.1f′", moonData.angularDiameterArcmin).let {
                    if (isFa) it.toPersianDigits() else it
                }

                val libLonStr = String.format("%+.1f°", moonData.librationLonDeg).let {
                    if (isFa) it.toPersianDigits() else it
                }

                val libLatStr = String.format("%+.1f°", moonData.librationLatDeg).let {
                    if (isFa) it.toPersianDigits() else it
                }

                val earthshineStr = String.format("%.0f%%", moonData.earthshinePercent * 100.0).let {
                    if (isFa) it.toPersianDigits() else it
                }

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = RedSpacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MoonMetricItem(
                            icon = Icons.Outlined.FitScreen,
                            label = if (isFa) "قطر زاویه‌ای" else "Angular Diameter",
                            value = angDiamStr,
                            modifier = Modifier.weight(1f)
                        )
                        MoonMetricItem(
                            icon = Icons.Outlined.LightMode,
                            label = if (isFa) "زمین‌تاب" else "Earthshine",
                            value = earthshineStr,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    RedHairlineDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = RedSpacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MoonMetricItem(
                            icon = Icons.Outlined.Rotate90DegreesCcw,
                            label = if (isFa) "رخ‌گردی طولی" else "Libration (Lon)",
                            value = libLonStr,
                            modifier = Modifier.weight(1f)
                        )
                        MoonMetricItem(
                            icon = Icons.Outlined.Rotate90DegreesCw,
                            label = if (isFa) "رخ‌گردی عرضی" else "Libration (Lat)",
                            value = libLatStr,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. UPCOMING PHASES SECTION (Clean & unboxed)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("moon_upcoming_card"),
                verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)
            ) {
                RedSectionHeader(
                    title = if (isFa) "فازهای آینده" else "Upcoming Phases",
                    subtitle = if (isFa) "زمان‌بندی فازهای بعدی ماه" else "Next lunar quarters & full moon schedule"
                )

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    upcomingPhases.forEachIndexed { idx, phase ->
                        val name = if (isFa) phase.phaseNameFa else phase.phaseNameEn
                        val dateText = TimeEngine.formatDate(phase.dateMs, uiState.calendarSystem, isFa).let {
                            if (isFa) it.toPersianDigits() else it
                        }
                        val daysText = if (isFa) "${phase.daysFromNow} روز دیگر".toPersianDigits() else "in ${phase.daysFromNow} days"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = RedSpacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(RedSpacing.md)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(RedTheme.colors.accentRed.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Brightness2,
                                        contentDescription = null,
                                        tint = RedTheme.colors.accentRed,
                                        modifier = Modifier.size(RedIconSize.sm)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = name,
                                        style = RedTypographyTokens.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                                        color = RedTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = dateText,
                                        style = RedTypographyTokens.caption,
                                        color = RedTheme.colors.textSecondary
                                    )
                                }
                            }

                            RedBadge(
                                text = daysText,
                                backgroundColor = RedTheme.colors.surfaceGrouped,
                                textColor = RedTheme.colors.textSecondary,
                                borderColor = RedTheme.colors.border
                            )
                        }

                        if (idx < upcomingPhases.size - 1) {
                            RedHairlineDivider()
                        }
                    }
                }
            }
        }

        // Bottom spacing for floating navigation bar
        item {
            Spacer(modifier = Modifier.height(112.dp))
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
        // Purple Radial Glow backdrop
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFA855F7).copy(alpha = 0.16f),
                            Color(0xFFA855F7).copy(alpha = 0.03f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Faint outer orbital ring
        Box(
            modifier = Modifier
                .size(274.dp)
                .border(0.75.dp, Color.White.copy(alpha = 0.06f), CircleShape)
        )

        // Realistic Moon image + Phase Canvas Shader
        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(CircleShape)
                .graphicsLayer {
                    rotationZ = if (latitude < 0) 180f + rotationAngle else rotationAngle
                }
        ) {
            // High-Res Photographic Moon Asset
            Image(
                painter = painterResource(id = R.drawable.img_full_moon_photo_1785673146290),
                contentDescription = moonData.phaseNameFa,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic Lunar Phase Shadow Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (size.width <= 0f || size.height <= 0f) return@Canvas
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                val ill = moonData.illuminationPercent / 100.0

                if (ill < 0.98) {
                    val targetShadowAlpha = 0.965f
                    val numSteps = 16
                    val stepAlpha = targetShadowAlpha / numSteps
                    val feather = radius * 0.08f

                    for (step in 0 until numSteps) {
                        val t = (step.toFloat() / (numSteps - 1) - 0.5f) * 2f
                        val offset = t * feather

                        val shadowPath = Path()
                        shadowPath.addArc(
                            Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                            90f,
                            180f
                        )

                        val k = (2.0 * ill - 1.0).toFloat()
                        val stepInnerWidth = (abs(k) * radius + offset).coerceAtLeast(0f)
                        val innerRect = Rect(center.x - stepInnerWidth, center.y - radius, center.x + stepInnerWidth, center.y + radius)

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
                            Color(0xFF05050D).copy(alpha = 0.38f)
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
private fun MoonMetricItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RedSpacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(RedTheme.colors.accentRed.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = RedTheme.colors.accentRed,
                modifier = Modifier.size(RedIconSize.sm)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = RedTypographyTokens.caption,
                color = RedTheme.colors.textSecondary
            )
            Text(
                text = value,
                style = RedTypographyTokens.bodyPrimary.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = RedTheme.colors.textPrimary
            )
        }
    }
}
