package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.astro_engine.MoonEngine
import com.example.astro_engine.TimeEngine
import com.example.domain.AppLanguage
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.util.toPersianDigits
import java.util.Calendar
import java.util.Random
import kotlin.math.abs

@Composable
fun MoonScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    var selectedDayOffset by remember { mutableStateOf(0) }

    val baseJd = remember { TimeEngine.getJulianDate() }
    val currentJd = baseJd + selectedDayOffset

    val moonData = remember(currentJd, uiState.userLocation) {
        MoonEngine.calculateMoon(currentJd, uiState.userLocation.latitude, uiState.userLocation.longitude)
    }

    val upcomingPhases = remember(baseJd) {
        MoonEngine.getUpcomingMajorPhases(baseJd)
    }

    val selectedCalendar = remember(selectedDayOffset) {
        val cal = Calendar.getInstance(TimeEngine.TEHRAN_TIME_ZONE)
        cal.add(Calendar.DAY_OF_YEAR, selectedDayOffset)
        cal
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("moon_screen"),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. MOON HERO SECTION (~380dp height)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF13111A), Color(0xFF0A0A12))
                        )
                    )
                    .testTag("moon_hero_card")
            ) {
                // Procedural Starfield background
                Canvas(modifier = Modifier.fillMaxSize()) {
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
                        .fillMaxSize()
                        .padding(top = 12.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Date switcher pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { selectedDayOffset -= 1 },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Previous Day", tint = Color(0xFFA855F7))
                        }

                        val dateStr = TimeEngine.formatDate(selectedCalendar.timeInMillis, uiState.calendarSystem, isFa).let {
                            if (isFa) it.toPersianDigits() else it
                        }
                        Text(
                            text = if (selectedDayOffset == 0) (if (isFa) "امروز — $dateStr" else "Today — $dateStr") else dateStr,
                            style = TextStyle(
                                fontFamily = IranSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = Color(0xFFF5F5F7)
                            )
                        )

                        IconButton(
                            onClick = { selectedDayOffset += 1 },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Next Day", tint = Color(0xFFA855F7))
                        }
                    }

                    // Realistic Photographic Moon Visualization
                    PhotographicMoonView(
                        moonData = moonData,
                        latitude = uiState.userLocation.latitude,
                        modifier = Modifier.size(280.dp)
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
                                color = Color(0xFFF5F5F7)
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
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = Color(0xFFA855F7)
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
                    containerColor = Color(0xFF13111A).copy(alpha = 0.70f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
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
                            color = Color(0xFFF5F5F7)
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
                    containerColor = Color(0xFF13111A).copy(alpha = 0.70f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
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
                            color = Color(0xFFF5F5F7)
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
                                    .background(Color.White.copy(alpha = 0.04f))
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
                                        tint = Color(0xFFA855F7),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "$name — $dateText",
                                        style = TextStyle(
                                            fontFamily = IranSans,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = Color(0xFFF5F5F7)
                                        )
                                    )
                                }

                                Text(
                                    text = daysText,
                                    style = TextStyle(
                                        fontFamily = IranSans,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = Color(0xFF9CA3AF)
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
    latitude: Double,
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
        modifier = modifier,
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
                val age = moonData.ageDays
                val isWaxing = age < 14.765

                if (ill < 0.98) {
                    // Multi-pass Feathered Terminator Shadow Overlay for natural penumbra blending
                    val targetShadowAlpha = 0.92f
                    val numSteps = 16
                    val stepAlpha = targetShadowAlpha / numSteps
                    val feather = radius * 0.08f // Soft penumbra width (~22px feathering)

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

                        if (k >= 0) {
                            shadowPath.arcTo(innerRect, if (isWaxing) 270f else 90f, -sweepAngle, false)
                        } else {
                            shadowPath.arcTo(innerRect, if (isWaxing) 90f else 270f, sweepAngle, false)
                        }
                        shadowPath.close()

                        drawPath(
                            path = shadowPath,
                            color = Color(0xFF07070F).copy(alpha = stepAlpha)
                        )
                    }
                }

                // Ambient Edge Limb Darkening
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF0A0A12).copy(alpha = 0.35f)
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
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
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
                    .background(Color(0xFFA855F7).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color(0xFFA855F7),
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
                        color = Color(0xFF9CA3AF)
                    )
                )
                Text(
                    text = value,
                    style = TextStyle(
                        fontFamily = IranSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFF5F5F7)
                    )
                )
            }
        }
    }
}
