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
import com.example.ui.components.HeroSkyCanvas
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
            .filter { it.id != "planet_earth" && it.type != ObjectType.SUN && it.id != "sun_sol" && it.id != "sat_iss" && it.type != ObjectType.SATELLITE }
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

    // Astronomy Predictive Search State
    var searchQuery by remember { mutableStateOf("") }

    // Predictive search filter logic across all categories simultaneously
    val filteredSearchResults = remember(searchQuery, sortedObjectsWithObs) {
        val q = searchQuery.trim().lowercase()
        sortedObjectsWithObs.filter { (obj, _, _) ->
            if (q.isEmpty()) true else {
                obj.nameFa.lowercase().contains(q) ||
                obj.nameEn.lowercase().contains(q) ||
                obj.constellationFa.lowercase().contains(q) ||
                obj.constellationEn.lowercase().contains(q) ||
                obj.category.lowercase().contains(q)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_column"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. TOP APP BAR — Elevated Header Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isFa) "آسمان زنده" else "Hero Sky",
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = IranSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color.White
                        )
                    )

                    val locationName = if (isFa) uiState.userLocation.cityNameFa else uiState.userLocation.cityNameEn
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = locationName,
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = IranSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.setShowFavoritesDialog(true) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1533))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            .testTag("top_favorites_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = stringResource(R.string.favorites_and_history),
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setShowSettingsDialog(true) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1533))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            .testTag("top_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 1. HERO SKY CANVAS — Interactive Real-time Sky
        item {
            HeroSkyCanvas(
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier.testTag("home_header_card")
            )
        }

        // 2. TONIGHT'S HIGHLIGHTS CARD — "خب امشب چی داریم؟" (What's Tonight?)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_highlights_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (isFa) "پیشنهادهای هوشمند رصدی بر اساس وضعیت آسمان و موقعیت جغرافیایی شما"
                            else "Smart observation recommendations based on sky conditions and location",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Curated dynamic observation items matching real objects
                    val highlightObjects = remember(sortedObjectsWithObs) {
                        sortedObjectsWithObs.take(4)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        highlightObjects.forEach { (obj, horiz, obs) ->
                            val icon = when (obj.type) {
                                ObjectType.PLANET -> "🪐"
                                ObjectType.MOON -> "🌙"
                                ObjectType.DEEP_SKY -> "🌌"
                                ObjectType.STAR -> "⭐"
                                ObjectType.SATELLITE -> "🛰️"
                                ObjectType.SUN -> "☀️"
                            }

                            val magStr = String.format("%.1f", obj.magnitude)
                            val titleText = if (isFa) {
                                "${obj.nameFa} — قدر ${magStr}".toPersianDigits()
                            } else {
                                "${obj.nameEn} — Mag ${magStr}"
                            }

                            val altInt = horiz.altitudeDeg.toInt()
                            val subtitleText = if (isFa) {
                                "در ارتفاع ${altInt}° — ${obs.level.nameFa}".toPersianDigits()
                            } else {
                                "Alt ${altInt}° — ${obs.level.nameEn}"
                            }

                            val badgeText = when {
                                obj.magnitude < 3.0 -> if (isFa) "چشم غیرمسلح" else "Naked Eye"
                                obj.magnitude < 7.0 -> if (isFa) "دوربین دوچشمی" else "Binoculars"
                                else -> if (isFa) "تلسکوپ" else "Telescope"
                            }

                            ObservationItemRow(
                                data = ObservationItemData(
                                    icon = icon,
                                    title = titleText,
                                    subtitle = subtitleText,
                                    badgeText = badgeText
                                ),
                                onClick = {
                                    viewModel.openObjectDetail(obj)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. INTERACTIVE ASTRONOMY SEARCH BAR WITH PREDICTIVE OPTIONS (AT BOTTOM OF HOME SCREEN)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_astronomy_search_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Objects",
                            tint = AccentPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isFa) "جستجوی هوشمند اجرام آسمان" else "Interactive Sky Search",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Modern Predictive Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_astronomy_search_input"),
                        placeholder = {
                            Text(
                                text = if (isFa) "نام سیاره، ستاره یا سحابی (مثلا مشتری، شباهنگ)..." else "Search planets, stars, nebulae...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = AccentPrimary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        )
                    )

                    // Predictive Search Suggestion Options List
                    val displayedPredictiveItems = remember(filteredSearchResults) {
                        filteredSearchResults.take(6)
                    }

                    if (displayedPredictiveItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isFa) "هیچ جرم متناسبی پیدا نشد." else "No matching celestial object found.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            displayedPredictiveItems.forEach { (obj, horiz, obs) ->
                                val icon = when (obj.type) {
                                    ObjectType.PLANET -> "🪐"
                                    ObjectType.MOON -> "🌙"
                                    ObjectType.DEEP_SKY -> "🌌"
                                    ObjectType.STAR -> "⭐"
                                    ObjectType.SATELLITE -> "🛰️"
                                    ObjectType.SUN -> "☀️"
                                }

                                val riseSetStr = remember(obj, uiState.userLocation, jd, isFa) {
                                    val rst = CoordinateEngine.calculateRiseSetTransit(
                                        raDeg = obj.raDeg,
                                        decDeg = obj.decDeg,
                                        latDeg = uiState.userLocation.latitude,
                                        lonDeg = uiState.userLocation.longitude,
                                        jd = jd,
                                        isFa = isFa
                                    )
                                    if (isFa) "طلوع: ${rst.riseTimeStr} | غروب: ${rst.setTimeStr}" else "Rise: ${rst.riseTimeStr} | Set: ${rst.setTimeStr}"
                                }

                                Surface(
                                    onClick = { viewModel.openObjectDetail(obj) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("predictive_search_item_${obj.id}"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(text = icon, fontSize = 20.sp)
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = if (isFa) obj.nameFa else obj.nameEn,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = riseSetStr,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                    color = AccentPrimary
                                                )
                                            }
                                        }

                                        val altInt = horiz.altitudeDeg.toInt()
                                        val statusBadge = if (horiz.altitudeDeg > 0.0) {
                                            if (isFa) "ارتفاع ${altInt}°".toPersianDigits() else "Alt ${altInt}°"
                                        } else {
                                            if (isFa) "زیر افق" else "Below Horizon"
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (horiz.altitudeDeg > 0.0) StatusExcellent.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = statusBadge,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = if (horiz.altitudeDeg > 0.0) StatusExcellent else Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
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
