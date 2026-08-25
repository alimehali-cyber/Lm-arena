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
import com.alijafari.red.astronomy.ui.components.LocationSelectorDialog
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

    if (uiState.showLocationSelector) {
        LocationSelectorDialog(
            uiState = uiState,
            onDismiss = { viewModel.setShowLocationSelector(false) },
            onSelectLocation = { city ->
                viewModel.setLocation(
                    cityEn = city.nameEn,
                    cityFa = city.nameFa,
                    lat = city.latitude,
                    lon = city.longitude,
                    elevationMeters = city.elevationMeters,
                    timezoneId = city.timezoneId,
                    countryCode = if (city.isIran) "IR" else "GLOBAL",
                    provinceEn = city.provinceEn,
                    provinceFa = city.provinceFa
                )
            },
            onSelectCoordinates = { lat, lon, elev, nameEn, nameFa ->
                viewModel.setLocation(
                    cityEn = nameEn,
                    cityFa = nameFa,
                    lat = lat,
                    lon = lon,
                    elevationMeters = elev
                )
            },
            onGpsSelected = { lat, lon, alt ->
                viewModel.setGpsLocation(lat, lon, alt)
            },
            onToggleFavorite = { city ->
                viewModel.toggleFavoriteLocation(city)
            },
            onRemoveFavorite = { id ->
                viewModel.removeFavoriteLocation(id)
            }
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
                    .padding(vertical = RedSpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                ) {
                    Text(
                        text = "RED",
                        style = RedTypographyTokens.heroDisplay.copy(
                            color = RedTheme.colors.accentRed,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = if (isFa) "• آسمان زنده" else "• Live Sky",
                        style = RedTypographyTokens.bodyPrimary.copy(
                            fontWeight = FontWeight.Medium,
                            color = RedTheme.colors.textSecondary
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.setShowFavoritesDialog(true) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(RedTheme.colors.surfaceElevated)
                            .border(1.dp, RedTheme.colors.border, CircleShape)
                            .testTag("top_favorites_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = stringResource(R.string.favorites_and_history),
                            tint = RedTheme.colors.accentRed,
                            modifier = Modifier.size(RedIconSize.sm)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setShowSettingsDialog(true) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(RedTheme.colors.surfaceElevated)
                            .border(1.dp, RedTheme.colors.border, CircleShape)
                            .testTag("top_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = RedTheme.colors.textSecondary,
                            modifier = Modifier.size(RedIconSize.sm)
                        )
                    }
                }
            }
        }

        // 0.5. LOCATION SELECTION BAR — Refined Primary Control framing the Sky
        item {
            val locationName = if (isFa) uiState.userLocation.cityNameFa else uiState.userLocation.cityNameEn
            val coordsText = String.format(java.util.Locale.US, "%.2f°N, %.2f°E", uiState.userLocation.latitude, uiState.userLocation.longitude)
            val elevText = if (uiState.userLocation.elevationMeters > 0) {
                if (isFa) " • ${uiState.userLocation.elevationMeters.toInt()} متر" else " • ${uiState.userLocation.elevationMeters.toInt()}m"
            } else ""

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RedCornerRadius.lg))
                    .border(
                        width = 1.dp,
                        color = RedTheme.colors.border,
                        shape = RoundedCornerShape(RedCornerRadius.lg)
                    )
                    .clickable {
                        viewModel.setShowLocationSelector(true)
                    }
                    .testTag("home_location_selector_button"),
                color = RedTheme.colors.surfaceElevated,
                shadowElevation = RedElevation.subtle
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RedSpacing.md, vertical = RedSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RedSpacing.md),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(RedTheme.colors.accentRed.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = RedTheme.colors.accentRed,
                                modifier = Modifier.size(RedIconSize.sm)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(RedSpacing.xs)
                            ) {
                                Text(
                                    text = locationName,
                                    style = RedTypographyTokens.bodyPrimary.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    ),
                                    color = RedTheme.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(RedCornerRadius.xs))
                                        .background(RedTheme.colors.accentRed.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isFa) "تغییر" else "Change",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = RedTheme.colors.accentRed,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                            Text(
                                text = "$coordsText$elevText",
                                style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                                color = RedTheme.colors.textSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand Location Picker",
                        tint = RedTheme.colors.textTertiary,
                        modifier = Modifier.size(RedIconSize.sm)
                    )
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

        // 2. WHAT'S UP TONIGHT? — Coherent Section Grouping
        item {
            val tonightEvents = remember(uiState.userLocation, jd, isFa) {
                WhatsUpTonightEngine.calculateTonightEvents(
                    jd = jd,
                    userLatDeg = uiState.userLocation.latitude,
                    userLonDeg = uiState.userLocation.longitude,
                    isFa = isFa
                )
            }

            RedElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_highlights_card"),
                shape = RoundedCornerShape(RedCornerRadius.xl),
                backgroundColor = RedTheme.colors.surfaceElevated,
                borderColor = RedTheme.colors.border,
                contentPadding = PaddingValues(RedSpacing.lg)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.md)) {
                    RedSectionHeader(
                        title = if (isFa) "امشب در آسمان" else "What's Up Tonight",
                        subtitle = if (isFa) "مهم‌ترین رویدادهای نجومی بر اساس موقعیت رصدی شما"
                        else "Top ranked astronomical events for your location"
                    )

                    if (tonightEvents.isEmpty()) {
                        Text(
                            text = if (isFa) "امشب رویداد نجومی ویژه‌ای ثبت نشده است." else "No special astronomical events tonight.",
                            style = RedTypographyTokens.bodySecondary,
                            color = RedTheme.colors.textSecondary,
                            modifier = Modifier.padding(vertical = RedSpacing.sm)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)) {
                            tonightEvents.take(5).forEachIndexed { idx, event ->
                                TonightEventRow(
                                    event = event,
                                    isFa = isFa,
                                    onClick = {
                                        event.targetObject?.let { obj ->
                                            viewModel.openObjectDetail(obj)
                                        }
                                    }
                                )
                                if (idx < minOf(4, tonightEvents.size - 1)) {
                                    RedHairlineDivider(modifier = Modifier.padding(horizontal = RedSpacing.xs))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. NEXT ECLIPSES CARD — Compact scientific eclipse predictions
        item {
            val (solarEclipse, lunarEclipse) = remember(uiState.userLocation) {
                EclipseEngine.getNextEclipses(
                    nowMs = System.currentTimeMillis(),
                    userLatDeg = uiState.userLocation.latitude,
                    userLonDeg = uiState.userLocation.longitude
                )
            }

            RedElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_next_eclipses_card"),
                shape = RoundedCornerShape(RedCornerRadius.xl),
                backgroundColor = RedTheme.colors.surfaceElevated,
                borderColor = RedTheme.colors.border,
                contentPadding = PaddingValues(RedSpacing.lg)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.md)) {
                    RedSectionHeader(
                        title = if (isFa) "گرفتگی‌های بعدی" else "Next Eclipses",
                        subtitle = if (isFa) "محاسبه دقیق کسوف و خسوف برای موقعیت شما"
                        else "Calculated precisely for your coordinates"
                    )

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

                    RedHairlineDivider()

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
                        text = if (isFa) "برای مشاهده زمان دقیق، فازها و راه‌نما ضربه بزنید ➔" else "Tap an eclipse for local timing, phases & guide ➔",
                        style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                        color = RedTheme.colors.accentRed,
                        modifier = Modifier.padding(top = RedSpacing.xs)
                    )
                }
            }
        }

        // 4. INTERACTIVE ASTRONOMY SEARCH BAR WITH PREDICTIVE OPTIONS
        item {
            RedElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_astronomy_search_card"),
                shape = RoundedCornerShape(RedCornerRadius.xl),
                backgroundColor = RedTheme.colors.surfaceElevated,
                borderColor = RedTheme.colors.border,
                contentPadding = PaddingValues(RedSpacing.lg)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.md)) {
                    RedSectionHeader(
                        title = if (isFa) "جستجوی هوشمند اجرام" else "Interactive Sky Search",
                        subtitle = if (isFa) "کاوش در بانک داده جامع اجرام آسمان" else "Search planets, stars, constellations & deep sky"
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_astronomy_search_input"),
                        placeholder = {
                            Text(
                                text = if (isFa) "نام سیاره، ستاره یا سحابی..." else "Search planets, stars, nebulae...",
                                style = RedTypographyTokens.bodySecondary,
                                color = RedTheme.colors.textTertiary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = RedTheme.colors.accentRed,
                                modifier = Modifier.size(RedIconSize.sm)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = RedTheme.colors.textSecondary,
                                        modifier = Modifier.size(RedIconSize.sm)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(RedCornerRadius.md),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedTheme.colors.accentRed,
                            unfocusedBorderColor = RedTheme.colors.border,
                            focusedContainerColor = RedTheme.colors.surfaceGrouped,
                            unfocusedContainerColor = RedTheme.colors.surfaceGrouped
                        )
                    )

                    val displayedPredictiveItems = remember(filteredSearchResults) {
                        filteredSearchResults.take(6)
                    }

                    if (displayedPredictiveItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = RedSpacing.md),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isFa) "هیچ جرم متناسبی پیدا نشد." else "No matching celestial object found.",
                                style = RedTypographyTokens.bodySecondary,
                                color = RedTheme.colors.textTertiary
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.xs)) {
                            displayedPredictiveItems.forEachIndexed { idx, (obj, horiz, obs) ->
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
                                    shape = RoundedCornerShape(RedCornerRadius.sm),
                                    color = RedTheme.colors.surfaceGrouped
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = RedSpacing.md, vertical = RedSpacing.sm),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(RedSpacing.md),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(text = icon, fontSize = 18.sp)
                                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                                Text(
                                                    text = if (isFa) obj.nameFa else obj.nameEn,
                                                    style = RedTypographyTokens.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                                                    color = RedTheme.colors.textPrimary
                                                )
                                                Text(
                                                    text = riseSetStr,
                                                    style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                                                    color = RedTheme.colors.accentRed
                                                )
                                            }
                                        }

                                        val altInt = horiz.altitudeDeg.toInt()
                                        val statusBadge = if (horiz.altitudeDeg > 0.0) {
                                            if (isFa) "ارتفاع ${altInt}°".toPersianDigits() else "Alt ${altInt}°"
                                        } else {
                                            if (isFa) "زیر افق" else "Below Horizon"
                                        }

                                        if (horiz.altitudeDeg > 0.0) {
                                            RedStatusBadge(
                                                text = statusBadge,
                                                statusColor = RedTheme.colors.statusSuccess,
                                                containerColor = RedTheme.colors.statusSuccessContainer
                                            )
                                        } else {
                                            RedBadge(
                                                text = statusBadge,
                                                backgroundColor = RedTheme.colors.surface,
                                                textColor = RedTheme.colors.textTertiary,
                                                borderColor = RedTheme.colors.border
                                            )
                                        }
                                    }
                                }

                                if (idx < displayedPredictiveItems.size - 1) {
                                    RedHairlineDivider()
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
        shape = RoundedCornerShape(RedCornerRadius.md),
        color = RedTheme.colors.surfaceGrouped,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("eclipse_item_row_${title.lowercase().replace(' ', '_')}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RedSpacing.md, vertical = RedSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RedSpacing.md),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = icon, fontSize = 20.sp)
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = title,
                        style = RedTypographyTokens.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                        color = RedTheme.colors.textPrimary
                    )
                    Text(
                        text = dateStr,
                        style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Medium),
                        color = RedTheme.colors.accentRed
                    )
                    Text(
                        text = visibilityInfo,
                        style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                        color = RedTheme.colors.textSecondary
                    )
                }
            }

            if (isLocallyVisible) {
                RedStatusBadge(
                    text = if (isFa) "قابل رصد" else "Locally Visible",
                    statusColor = RedTheme.colors.statusSuccess,
                    containerColor = RedTheme.colors.statusSuccessContainer
                )
            } else {
                RedBadge(
                    text = if (isFa) "غیرقابل رصد" else "Not Visible",
                    backgroundColor = RedTheme.colors.surface,
                    textColor = RedTheme.colors.textTertiary,
                    borderColor = RedTheme.colors.border
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
        shape = RoundedCornerShape(RedCornerRadius.md),
        color = RedTheme.colors.surfaceGrouped
    ) {
        Row(
            modifier = Modifier.padding(horizontal = RedSpacing.md, vertical = RedSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RedSpacing.md),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = event.icon, fontSize = 18.sp)
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = if (isFa) event.titleFa else event.titleEn,
                        style = RedTypographyTokens.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                        color = RedTheme.colors.textPrimary
                    )
                    Text(
                        text = if (isFa) event.explanationFa else event.explanationEn,
                        style = RedTypographyTokens.caption,
                        color = RedTheme.colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(RedSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFa) event.timeOrDateStrFa else event.timeOrDateStrEn,
                            style = RedTypographyTokens.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                            color = RedTheme.colors.accentRed
                        )
                        Text(
                            text = "•",
                            style = RedTypographyTokens.caption,
                            color = RedTheme.colors.textTertiary
                        )
                        Text(
                            text = if (isFa) event.visibilityTextFa else event.visibilityTextEn,
                            style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                            color = RedTheme.colors.textSecondary
                        )
                    }
                }
            }

            when (event.visibilityStatus) {
                WhatsUpTonightEngine.EventVisibilityStatus.OPTIMAL -> {
                    RedStatusBadge(
                        text = if (isFa) "عالی" else "Optimal",
                        statusColor = RedTheme.colors.statusSuccess,
                        containerColor = RedTheme.colors.statusSuccessContainer
                    )
                }
                WhatsUpTonightEngine.EventVisibilityStatus.GOOD -> {
                    RedStatusBadge(
                        text = if (isFa) "خوب" else "Good",
                        statusColor = RedTheme.colors.accentRed,
                        containerColor = RedTheme.colors.accentRed.copy(alpha = 0.12f)
                    )
                }
                WhatsUpTonightEngine.EventVisibilityStatus.MARGINAL -> {
                    RedStatusBadge(
                        text = if (isFa) "متوسط" else "Marginal",
                        statusColor = RedTheme.colors.statusWarning,
                        containerColor = RedTheme.colors.statusWarningContainer
                    )
                }
                WhatsUpTonightEngine.EventVisibilityStatus.NOT_VISIBLE -> {
                    RedBadge(
                        text = if (isFa) "غیرقابل رصد" else "Not Visible",
                        backgroundColor = RedTheme.colors.surface,
                        textColor = RedTheme.colors.textTertiary,
                        borderColor = RedTheme.colors.border
                    )
                }
            }
        }
    }
}
