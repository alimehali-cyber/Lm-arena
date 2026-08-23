package com.alijafari.red.astronomy.ui.screens

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.*
import com.alijafari.red.astronomy.data.catalog.AstronomyCatalog
import com.alijafari.red.astronomy.domain.*
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.components.HeroSkyCanvas
import com.alijafari.red.astronomy.ui.components.EclipseDetailModal
import com.alijafari.red.astronomy.ui.theme.*
import com.alijafari.red.astronomy.util.toPersianDigits
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

    // Moon data
    val moonData = remember(jd, uiState.userLocation) {
        MoonEngine.calculateMoon(jd, uiState.userLocation.latitude, uiState.userLocation.longitude)
    }

    val allObjects = remember(jd) { AstronomyCatalog.getAllObjects(jd) }

    val sortedObjectsWithObs = remember(allObjects, lastDeg, sunHoriz, moonData, uiState.userLocation, uiState.bortleClass) {
        allObjects
            .filter { it.id != "planet_earth" && it.id != "sat_iss" && it.type != ObjectType.SATELLITE }
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

    // Astronomy Predictive Search State
    var searchQuery by remember { mutableStateOf("") }
    var selectedEclipseResult by remember { mutableStateOf<EclipseEngine.EclipseResult?>(null) }

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

    selectedEclipseResult?.let { eclipseRes ->
        val detailedInfo = remember(eclipseRes, uiState.userLocation) {
            EclipseEngine.computeDetailedInfo(
                result = eclipseRes,
                userLatDeg = uiState.userLocation.latitude,
                userLonDeg = uiState.userLocation.longitude
            )
        }
        EclipseDetailModal(
            detailedInfo = detailedInfo,
            language = uiState.language,
            onDismiss = { selectedEclipseResult = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen_column"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. TOP APP BAR — Header Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "RED",
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = if (isFa) "• آسمان زنده" else "• Hero Sky",
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = IranSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }

                    val locationName = if (isFa) uiState.userLocation.cityNameFa else uiState.userLocation.cityNameEn
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = locationName,
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = IranSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                            .testTag("top_favorites_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = stringResource(R.string.favorites_and_history),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setShowSettingsDialog(true) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                            .testTag("top_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

        // 2. WHAT'S UP TONIGHT? CARD — Intelligently ranked events tonight
        item {
            val tonightEvents = remember(uiState.userLocation, jd, isFa) {
                WhatsUpTonightEngine.calculateTonightEvents(
                    jd = jd,
                    userLatDeg = uiState.userLocation.latitude,
                    userLonDeg = uiState.userLocation.longitude,
                    isFa = isFa
                )
            }

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
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "✨", fontSize = 18.sp)
                            Text(
                                text = if (isFa) "امشب در آسمان چی داریم؟" else "What's Up Tonight?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (isFa) "مهم‌ترین رویدادهای نجومی امشب بر اساس اولویت رصدی در موقعیت شما"
                            else "Top ranked astronomical events occurring tonight for your location",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    if (tonightEvents.isEmpty()) {
                        Text(
                            text = if (isFa) "امشب رویداد نجومی ویژه‌ای ثبت نشده است." else "No special astronomical events tonight.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            tonightEvents.take(5).forEach { event ->
                                TonightEventRow(
                                    event = event,
                                    isFa = isFa,
                                    onClick = {
                                        event.targetObject?.let { obj ->
                                            viewModel.openObjectDetail(obj)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. NEXT ECLIPSES CARD — Compact scientific eclipse predictions for user location
        item {
            val (solarEclipse, lunarEclipse) = remember(uiState.userLocation) {
                EclipseEngine.getNextEclipses(
                    nowMs = System.currentTimeMillis(),
                    userLatDeg = uiState.userLocation.latitude,
                    userLonDeg = uiState.userLocation.longitude
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_next_eclipses_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🌘", fontSize = 18.sp)
                            Text(
                                text = if (isFa) "رویدادهای گرفتگی بعدی (کسوف و خسوف)" else "Next Eclipses",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (isFa) "محاسبه بر اساس موقعیت جغرافیایی دقیق شما" else "Calculated for your exact location",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Solar Eclipse Item
                    EclipseItemRow(
                        icon = "☀️",
                        title = if (isFa) solarEclipse.event.nameFa else solarEclipse.event.nameEn,
                        dateStr = if (isFa) solarEclipse.formattedDateFa else solarEclipse.formattedDateEn,
                        visibilityInfo = if (isFa) solarEclipse.localVisibilityTextFa else solarEclipse.localVisibilityTextEn,
                        isLocallyVisible = solarEclipse.isLocallyVisible,
                        isFa = isFa,
                        onClick = { selectedEclipseResult = solarEclipse }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Lunar Eclipse Item
                    EclipseItemRow(
                        icon = "🌕",
                        title = if (isFa) lunarEclipse.event.nameFa else lunarEclipse.event.nameEn,
                        dateStr = if (isFa) lunarEclipse.formattedDateFa else lunarEclipse.formattedDateEn,
                        visibilityInfo = if (isFa) lunarEclipse.localVisibilityTextFa else lunarEclipse.localVisibilityTextEn,
                        isLocallyVisible = lunarEclipse.isLocallyVisible,
                        isFa = isFa,
                        onClick = { selectedEclipseResult = lunarEclipse }
                    )

                    Text(
                        text = if (isFa) "برای مشاهده زمان دقیق، فازها و راه‌نمای رصد روی هر رویداد ضربه بزنید ➔" else "Tap an eclipse for exact local timing, phases & safety guide ➔",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = AccentPrimary
                    )
                }
            }
        }

        // 4. INTERACTIVE ASTRONOMY SEARCH BAR WITH PREDICTIVE OPTIONS
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
                                    ObjectType.PLANET, ObjectType.DWARF_PLANET -> "🪐"
                                    ObjectType.MOON -> "🌙"
                                    ObjectType.DEEP_SKY, ObjectType.GALAXY, ObjectType.NEBULA, ObjectType.STAR_CLUSTER, ObjectType.GLOBULAR_CLUSTER, ObjectType.BLACK_HOLE -> "🌌"
                                    ObjectType.STAR, ObjectType.ASTERISM -> "⭐"
                                    ObjectType.SATELLITE -> "🛰️"
                                    ObjectType.SUN -> "☀️"
                                    ObjectType.METEOR_SHOWER -> "☄️"
                                    ObjectType.CONSTELLATION -> "✨"
                                    ObjectType.REFERENCE_POINT -> "📍"
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

        // Bottom spacing to clear floating nav bar
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun EclipseItemRow(
    icon: String,
    title: String,
    dateStr: String,
    visibilityInfo: String,
    isLocallyVisible: Boolean,
    isFa: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("eclipse_item_row_${title.lowercase().replace(' ', '_')}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = icon, fontSize = 22.sp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentPrimary
                    )
                    Text(
                        text = visibilityInfo,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isLocallyVisible) StatusExcellent.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (isLocallyVisible) (if (isFa) "قابل رصد" else "Locally Visible") else (if (isFa) "غیرقابل رصد" else "Not Visible"),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = if (isLocallyVisible) StatusExcellent else Color.Gray
                )
            }
        }
    }
}

@Composable
private fun TonightEventRow(
    event: WhatsUpTonightEngine.TonightEvent,
    isFa: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
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
                Text(text = event.icon, fontSize = 20.sp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isFa) event.titleFa else event.titleEn,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isFa) event.explanationFa else event.explanationEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFa) event.timeOrDateStrFa else event.timeOrDateStrEn,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = AccentPrimary
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isFa) event.visibilityTextFa else event.visibilityTextEn,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val (badgeText, badgeColor) = when (event.visibilityStatus) {
                WhatsUpTonightEngine.EventVisibilityStatus.OPTIMAL -> Pair(if (isFa) "عالی" else "Optimal", StatusExcellent)
                WhatsUpTonightEngine.EventVisibilityStatus.GOOD -> Pair(if (isFa) "خوب" else "Good", StatusGood)
                WhatsUpTonightEngine.EventVisibilityStatus.MARGINAL -> Pair(if (isFa) "متوسط" else "Marginal", Color(0xFFFFB703))
                WhatsUpTonightEngine.EventVisibilityStatus.NOT_VISIBLE -> Pair(if (isFa) "غیرقابل رصد" else "Not Visible", Color.Gray)
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = badgeColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = badgeText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = badgeColor
                )
            }
        }
    }
}
