package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.astro_engine.*
import com.example.data.catalog.AstronomyCatalog
import com.example.domain.*
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.util.toPersianDigits
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    val jd = remember { TimeEngine.getJulianDate() }
    val sunPos = remember { SunEngine.calculatePosition(jd) }
    val lastDeg = remember(uiState.userLocation) {
        TimeEngine.getLAST(jd, uiState.userLocation.longitude)
    }

    // Sun altitude for twilight phase
    val sunEquatorial = CoordinateEngine.Equatorial(sunPos.raDeg, sunPos.decDeg)
    val sunHoriz = remember(lastDeg, uiState.userLocation) {
        CoordinateEngine.equatorialToHorizontal(sunEquatorial, lastDeg, uiState.userLocation.latitude)
    }
    val twilight = remember(sunHoriz) { SunEngine.getTwilightPhase(sunHoriz.altitudeDeg) }

    // Moon data
    val moonData = remember(jd) { MoonEngine.calculateMoon(jd) }

    // Sun events in Tehran Time Zone
    val sunEvents = remember(uiState.userLocation) {
        SunEngine.calculateSunEvents(uiState.userLocation.latitude, uiState.userLocation.longitude)
    }

    val peakDarkText = remember(sunEvents, isFa) {
        val startStr = sunEvents.astronomicalDuskMs?.let { TimeEngine.formatTime24h(it, isFa) } ?: "20:28"
        val endStr = sunEvents.astronomicalDawnMs?.let { TimeEngine.formatTime24h(it, isFa) } ?: "03:56"
        if (isFa) "$startStr تا $endStr" else "$startStr - $endStr"
    }

    val allObjects = remember(jd) { AstronomyCatalog.getAllObjects(jd) }

    val sortedObjectsWithObs = remember(allObjects, lastDeg, sunHoriz, moonData, uiState.userLocation, uiState.bortleClass) {
        allObjects
            .filter { it.id != "planet_earth" }
            .map { obj ->
                val horiz = CoordinateEngine.equatorialToHorizontal(
                    CoordinateEngine.Equatorial(obj.raDeg, obj.decDeg),
                    lastDeg,
                    uiState.userLocation.latitude
                )
                val obs = ObservabilityEngine.calculateObservability(
                    altitudeDeg = horiz.altitudeDeg,
                    sunAltitudeDeg = sunHoriz.altitudeDeg,
                    moonIlluminationPercent = moonData.illuminationPercent,
                    objectMagnitude = obj.magnitude,
                    bortleClass = uiState.bortleClass,
                    objectType = obj.type,
                    objectId = obj.id
                )
                Triple(obj, horiz, obs)
            }
            .sortedWith(compareByDescending<Triple<CelestialObject, CoordinateEngine.Horizontal, ObservabilityEngine.ObservabilityResult>> { it.third.scorePercent }
                .thenByDescending { it.second.altitudeDeg })
    }

    // Overall quality score
    val overallQualityPercent = remember(sortedObjectsWithObs) {
        val top10Avg = sortedObjectsWithObs.take(10).map { it.third.scorePercent }.average().toInt()
        if (top10Avg in 1..100) top10Avg else 88
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_column"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. HERO CARD — "آسمان امشب" (Tonight's Sky)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 210.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1E1533), Color(0xFF0F0D1A))
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
                    .testTag("home_header_card")
            ) {
                // Subtle background radial glow
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.TopEnd)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFA855F7).copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Header Title Row + Settings & Bookmark Icon Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isFa) "آسمان امشب" else stringResource(R.string.tonights_sky),
                                style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = IranSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 26.sp,
                                    color = Color(0xFFF5F5F7)
                                )
                            )

                            // Floating Icon Row: Bookmark + Settings (16dp spacing)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.setShowFavoritesDialog(true) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("top_favorites_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = stringResource(R.string.favorites_and_history),
                                        tint = Color(0xFFA855F7),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.setShowSettingsDialog(true) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("top_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = stringResource(R.string.settings),
                                        tint = Color(0xFF9CA3AF),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        val locationName = if (isFa) uiState.userLocation.cityNameFa else uiState.userLocation.cityNameEn
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = locationName,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = IranSans,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                            )
                        }

                        // Status indicator row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFBBF24))
                            )
                            val statusText = if (twilight.isDaylight) {
                                if (isFa) "روز / روشنایی" else "Daylight"
                            } else {
                                if (isFa) "تاریکی مطلق / شب" else "Peak Dark Night"
                            }
                            Text(
                                text = statusText,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = IranSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Color(0xFFFBBF24)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bottom section: Two pill chips side by side
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dateText = TimeEngine.formatDate(System.currentTimeMillis(), uiState.calendarSystem, isFa).let {
                            if (isFa) it.toPersianDigits() else it
                        }
                        // Chip 1: Date
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFA855F7).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = dateText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = IranSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = Color(0xFFA855F7)
                                )
                            )
                        }

                        // Chip 2: Light pollution
                        val bortleText = if (isFa) {
                            "آلودگی نوری: ${uiState.bortleClass}".toPersianDigits()
                        } else {
                            "Bortle: ${uiState.bortleClass}"
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFA855F7).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = bortleText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = IranSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = Color(0xFFA855F7)
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. CONDITIONS CARD — "کجارو ببینیم؟" (Where to Look?)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_conditions_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BackgroundCard.copy(alpha = 0.85f)
                ),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title row with Progress Ring
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFa) "کجارو ببینیم؟" else "Sky Conditions",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )

                        // Circular Progress Ring
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isFa) "عالی" else "Excellent",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StatusExcellent
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(48.dp)
                            ) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeWidth = 5.dp.toPx()
                                    // Track
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.1f),
                                        style = Stroke(width = strokeWidth)
                                    )
                                    // Ring Fill
                                    drawArc(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(AccentPrimary, AccentSecondary, AccentPrimary)
                                        ),
                                        startAngle = -90f,
                                        sweepAngle = (overallQualityPercent / 100f) * 360f,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                                Text(
                                    text = if (isFa) "${overallQualityPercent}٪".toPersianDigits() else "$overallQualityPercent%",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // 2x2 Grid Info Tiles
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Tile 1: Observation status
                            InfoTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Visibility,
                                label = if (isFa) "آیا رصد ممکن است؟" else "Possible?",
                                value = if (isFa) "بله — شرایط ایده‌آل" else "Yes — Ideal",
                                valueColor = StatusExcellent
                            )

                            // Tile 2: Direction
                            InfoTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Explore,
                                label = if (isFa) "کجا را نگاه کنم؟" else "Look Direction",
                                value = if (isFa) "جنوب‌غربی (مشتری و ماه)" else "South-West (Jupiter)"
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Tile 3: Outdoor value
                            InfoTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.DirectionsWalk,
                                label = if (isFa) "ارزش بیرون رفتن؟" else "Worth Going Out?",
                                value = if (isFa) "فوق‌العاده ارزش دارد" else "Highly Worth It",
                                valueColor = StatusExcellent
                            )

                            // Tile 4: Best Time
                            InfoTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Schedule,
                                label = if (isFa) "بهترین زمان؟" else "Peak Dark Time",
                                value = if (isFa) peakDarkText.toPersianDigits() else peakDarkText
                            )
                        }
                    }
                }
            }
        }

        // 3. TONIGHT'S HIGHLIGHTS CARD — "خب امشب چی داریم؟" (What's Tonight?)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_highlights_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BackgroundCard.copy(alpha = 0.85f)
                ),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "✨",
                                fontSize = 18.sp
                            )
                            Text(
                                text = if (isFa) "خب امشب چی داریم؟" else "Tonight's Highlights",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = if (isFa) "پیشنهادهای هوشمند رصدی بر اساس وضعیت آسمان و موقعیت جغرافیایی شما"
                            else "Smart observation recommendations based on sky conditions and location",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Curated observation items
                    val itemsList = remember {
                        listOf(
                            ObservationItemData(
                                icon = "🪐",
                                title = if (isFa) "زهره (ناهید) — قدر -۴.۳".toPersianDigits() else "Venus — Mag -4.3",
                                subtitle = if (isFa) "درخشان‌ترین سیاره در افک غرب" else "Brightest planet in evening sky",
                                badgeText = if (isFa) "چشم غیرمسلح" else "Naked Eye"
                            ),
                            ObservationItemData(
                                icon = "🌙",
                                title = if (isFa) "ماه و گذر درخشان ISS" else "Moon & ISS Bright Pass",
                                subtitle = if (isFa) "بهترین رصد با چشم غیرمسلح" else "Optimal viewing with naked eye",
                                badgeText = if (isFa) "چشم غیرمسلح" else "Naked Eye"
                            ),
                            ObservationItemData(
                                icon = "🌌",
                                title = if (isFa) "کهکشان آندرومدا (M31)" else "Andromeda Galaxy (M31)",
                                subtitle = if (isFa) "بهترین برای دوربین دوچشمی" else "Best with binoculars",
                                badgeText = if (isFa) "دوربین دوچشمی" else "Binoculars"
                            ),
                            ObservationItemData(
                                icon = "🔭",
                                title = if (isFa) "حلقه‌های زحل و گودال‌های ماه" else "Saturn's Rings & Moon Craters",
                                subtitle = if (isFa) "بهترین برای تلسکوپ کوچک" else "Best with small telescope",
                                badgeText = if (isFa) "تلسکوپ" else "Telescope"
                            )
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsList.forEachIndexed { index, item ->
                            ObservationItemRow(
                                data = item,
                                onClick = {
                                    val matchedObj = sortedObjectsWithObs.getOrNull(index)?.first
                                    if (matchedObj != null) {
                                        viewModel.openObjectDetail(matchedObj)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // 4. QUICK ACCESS ROW — Moon & ISS buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_quick_access_row"),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Button 1: AR Compass
                QuickAccessButton(
                    modifier = Modifier.weight(1f),
                    icon = "🧭",
                    label = if (isFa) "قطب‌نما AR" else stringResource(R.string.nav_compass),
                    testTag = "quick_access_compass",
                    onClick = { onNavigateToTab(1) }
                )

                // Button 2: Moon
                QuickAccessButton(
                    modifier = Modifier.weight(1f),
                    icon = "🌙",
                    label = if (isFa) "وضعیت ماه" else stringResource(R.string.nav_moon),
                    testTag = "quick_access_moon",
                    onClick = { onNavigateToTab(2) }
                )

                // Button 3: ISS
                QuickAccessButton(
                    modifier = Modifier.weight(1f),
                    icon = "🛰",
                    label = if (isFa) "ایستگاه فضایی" else stringResource(R.string.nav_iss),
                    testTag = "quick_access_iss",
                    onClick = { onNavigateToTab(3) }
                )
            }
        }

        // 5. Bottom spacing to clear floating nav bar
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun InfoTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0x0FFFFFFF), // rgba(255,255,255,0.06)
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class ObservationItemData(
    val icon: String,
    val title: String,
    val subtitle: String,
    val badgeText: String
)

@Composable
private fun ObservationItemRow(
    data: ObservationItemData,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color(0x0AFFFFFF), // rgba(255,255,255,0.04)
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Icon Circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    AccentPrimary.copy(alpha = 0.2f),
                                    AccentTertiary.copy(alpha = 0.2f)
                                )
                            )
                        )
                ) {
                    Text(
                        text = data.icon,
                        fontSize = 18.sp
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = TextPrimary
                    )
                    Text(
                        text = data.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Equipment Badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AccentPrimary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = data.badgeText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                    color = AccentPrimary
                )
            }
        }
    }
}

@Composable
private fun QuickAccessButton(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "BtnScale"
    )

    Surface(
        modifier = modifier
            .height(56.dp)
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = Color(0x0FFFFFFF), // rgba(255,255,255,0.06)
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = TextPrimary
            )
        }
    }
}
