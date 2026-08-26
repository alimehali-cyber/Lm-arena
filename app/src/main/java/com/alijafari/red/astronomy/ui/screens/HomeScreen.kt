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
import androidx.compose.ui.text.TextStyle
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
            .testTag("home_screen_column"),
        contentPadding = PaddingValues(horizontal = RedSpacing.lg, vertical = RedSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(RedSpacing.lg)
    ) {
        // 0. HOME HEADER — Centered RED Title, Contextual Location Pill & Essential Actions
        item {
            val locationName = if (isFa) uiState.userLocation.cityNameFa else uiState.userLocation.cityNameEn
            val coordsText = String.format(java.util.Locale.US, "%.2f°N, %.2f°E", uiState.userLocation.latitude, uiState.userLocation.longitude)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = RedSpacing.md, vertical = RedSpacing.xs)
                    .testTag("home_red_header"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RedSpacing.xs)
            ) {
                    // Top Action Bar with Centered "RED"
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Favorites bookmark action (Start)
                        IconButton(
                            onClick = { viewModel.setShowFavoritesDialog(true) },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(36.dp)
                                .clip(CircleShape)
                                .testTag("top_favorites_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BookmarkBorder,
                                contentDescription = stringResource(R.string.favorites_and_history),
                                tint = RedTheme.colors.textPrimary,
                                modifier = Modifier.size(RedIconSize.sm)
                            )
                        }

                        // Centered "RED" brand title
                        Text(
                            text = "RED",
                            style = TextStyle(
                                fontFamily = IranSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                letterSpacing = 3.sp
                            ),
                            color = RedTheme.colors.textPrimary,
                            modifier = Modifier.testTag("home_red_title")
                        )

                        // Settings action (End)
                        IconButton(
                            onClick = { viewModel.setShowSettingsDialog(true) },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(36.dp)
                                .clip(CircleShape)
                                .testTag("top_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.settings),
                                tint = RedTheme.colors.textPrimary,
                                modifier = Modifier.size(RedIconSize.sm)
                            )
                        }
                    }

                    // Understated, quiet location selector beneath RED
                    Surface(
                        onClick = { viewModel.setShowLocationSelector(true) },
                        shape = RoundedCornerShape(RedCornerRadius.pill),
                        color = RedTheme.colors.surfaceElevated.copy(alpha = 0.7f),
                        border = BorderStroke(0.75.dp, RedTheme.colors.border.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("home_location_selector_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = RedSpacing.md, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = "Location",
                                tint = RedTheme.colors.accentRed,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = locationName,
                                style = RedTypographyTokens.bodySecondary.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = RedTheme.colors.textPrimary
                            )
                            Text(
                                text = "•",
                                style = RedTypographyTokens.caption,
                                color = RedTheme.colors.textTertiary
                            )
                            Text(
                                text = coordsText,
                                style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                                color = RedTheme.colors.textSecondary
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Change Location",
                                tint = RedTheme.colors.textTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        // 1. HERO SKY CANVAS — Level 1: Hero Content
        item {
            HeroSkyCanvas(
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier.testTag("home_header_card")
            )
        }

        // 2. WHAT'S UP TONIGHT? — Level 2: Contextual Astronomical Information
        item {
            val tonightEvents = remember(uiState.userLocation, jd, isFa) {
                WhatsUpTonightEngine.calculateTonightEvents(
                    jd = jd,
                    userLatDeg = uiState.userLocation.latitude,
                    userLonDeg = uiState.userLocation.longitude,
                    isFa = isFa
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)
            ) {
                RedSectionHeader(
                    title = if (isFa) "امشب در آسمان" else "What's Up Tonight",
                    subtitle = if (isFa) "رویدادهای شاخص نجومی برای مختصات شما"
                    else "Key celestial events for your coordinates"
                )

                if (tonightEvents.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(RedCornerRadius.lg),
                        color = RedTheme.colors.surfaceElevated,
                        border = BorderStroke(0.75.dp, RedTheme.colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isFa) "امشب رویداد شاخصی ثبت نشده است." else "No major celestial events tonight.",
                            style = RedTypographyTokens.bodySecondary,
                            color = RedTheme.colors.textSecondary,
                            modifier = Modifier.padding(RedSpacing.lg)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(RedCornerRadius.xl),
                        color = RedTheme.colors.surfaceElevated,
                        border = BorderStroke(0.75.dp, RedTheme.colors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_highlights_card")
                    ) {
                        Column(modifier = Modifier.padding(vertical = RedSpacing.xs)) {
                            tonightEvents.take(5).forEachIndexed { idx, event ->
                                TonightEventRow(
                                    event = event,
                                    isTopRanked = idx == 0,
                                    isFa = isFa,
                                    onClick = {
                                        event.targetObject?.let { obj ->
                                            viewModel.openObjectDetail(obj)
                                        }
                                    }
                                )
                                if (idx < minOf(4, tonightEvents.size - 1)) {
                                    RedHairlineDivider(modifier = Modifier.padding(horizontal = RedSpacing.md))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. UPCOMING ECLIPSES SECTION — Level 3: Supporting Astronomical Events
        item {
            val (solarEclipse, lunarEclipse) = remember(uiState.userLocation) {
                EclipseEngine.getNextEclipses(
                    nowMs = System.currentTimeMillis(),
                    userLatDeg = uiState.userLocation.latitude,
                    userLonDeg = uiState.userLocation.longitude
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)
            ) {
                RedSectionHeader(
                    title = if (isFa) "گرفتگی‌های نجومی" else "Upcoming Eclipses",
                    subtitle = if (isFa) "محاسبه دقیق کسوف و خسوف بر اساس موقعیت شما"
                    else "Calculated precisely for your location"
                )

                Surface(
                    shape = RoundedCornerShape(RedCornerRadius.xl),
                    color = RedTheme.colors.surfaceElevated,
                    border = BorderStroke(0.75.dp, RedTheme.colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_next_eclipses_card")
                ) {
                    Column(modifier = Modifier.padding(vertical = RedSpacing.xs)) {
                        // Nearest / Primary Eclipse
                        EclipseItemRow(
                            isSolar = true,
                            isPrimary = true,
                            title = if (isFa) solarEclipse.event.nameFa else solarEclipse.event.nameEn,
                            dateStr = if (isFa) solarEclipse.formattedDateFa else solarEclipse.formattedDateEn,
                            visibilityInfo = if (isFa) solarEclipse.localVisibilityTextFa else solarEclipse.localVisibilityTextEn,
                            isLocallyVisible = solarEclipse.isLocallyVisible,
                            isFa = isFa,
                            onClick = { selectedEclipseResult = solarEclipse }
                        )

                        RedHairlineDivider(modifier = Modifier.padding(horizontal = RedSpacing.md))

                        // Secondary Eclipse
                        EclipseItemRow(
                            isSolar = false,
                            isPrimary = false,
                            title = if (isFa) lunarEclipse.event.nameFa else lunarEclipse.event.nameEn,
                            dateStr = if (isFa) lunarEclipse.formattedDateFa else lunarEclipse.formattedDateEn,
                            visibilityInfo = if (isFa) lunarEclipse.localVisibilityTextFa else lunarEclipse.localVisibilityTextEn,
                            isLocallyVisible = lunarEclipse.isLocallyVisible,
                            isFa = isFa,
                            onClick = { selectedEclipseResult = lunarEclipse }
                        )
                    }
                }
            }
        }

        // 4. INTERACTIVE ASTRONOMY SEARCH — Level 3: Search & Exploration
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)
            ) {
                RedSectionHeader(
                    title = if (isFa) "جستجوی اجرام آسمان" else "Interactive Sky Search",
                    subtitle = if (isFa) "کاوش سیارات، ستارگان، صور فلکی و سحابی‌ها"
                    else "Search planets, stars, constellations & deep sky"
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_astronomy_search_input"),
                    placeholder = {
                        Text(
                            text = if (isFa) "نام سیاره، ستاره، سحابی..." else "Search planets, stars, nebulae...",
                            style = RedTypographyTokens.bodySecondary,
                            color = RedTheme.colors.textTertiary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
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
                    shape = RoundedCornerShape(RedCornerRadius.lg),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedTheme.colors.accentRed,
                        unfocusedBorderColor = RedTheme.colors.border,
                        focusedContainerColor = RedTheme.colors.surfaceElevated,
                        unfocusedContainerColor = RedTheme.colors.surfaceElevated
                    )
                )

                val displayedPredictiveItems = remember(filteredSearchResults) {
                    filteredSearchResults.take(6)
                }

                if (displayedPredictiveItems.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(RedCornerRadius.lg),
                        color = RedTheme.colors.surfaceElevated,
                        border = BorderStroke(0.75.dp, RedTheme.colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = RedSpacing.lg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isFa) "هیچ جرم متناسبی پیدا نشد." else "No matching celestial object found.",
                                style = RedTypographyTokens.bodySecondary,
                                color = RedTheme.colors.textTertiary
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(RedCornerRadius.xl),
                        color = RedTheme.colors.surfaceElevated,
                        border = BorderStroke(0.75.dp, RedTheme.colors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_astronomy_search_card")
                    ) {
                        Column(modifier = Modifier.padding(vertical = RedSpacing.xs)) {
                            displayedPredictiveItems.forEachIndexed { idx, (obj, horiz, obs) ->
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

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.openObjectDetail(obj) }
                                        .padding(horizontal = RedSpacing.md, vertical = RedSpacing.sm)
                                        .testTag("predictive_search_item_${obj.id}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(RedSpacing.md),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        CelestialObjectIcon(
                                            type = obj.type,
                                            tint = RedTheme.colors.accentRed,
                                            modifier = Modifier.size(20.dp)
                                        )
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
                                            backgroundColor = RedTheme.colors.surfaceGrouped,
                                            textColor = RedTheme.colors.textTertiary,
                                            borderColor = RedTheme.colors.border
                                        )
                                    }
                                }

                                if (idx < displayedPredictiveItems.size - 1) {
                                    RedHairlineDivider(modifier = Modifier.padding(horizontal = RedSpacing.md))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom spacing to completely clear the floating bottom navigation bar
        item {
            Spacer(modifier = Modifier.height(112.dp))
        }
    }
}

@Composable
private fun CelestialObjectIcon(
    type: ObjectType?,
    modifier: Modifier = Modifier,
    tint: Color = RedTheme.colors.accentRed
) {
    val icon = when (type) {
        ObjectType.PLANET, ObjectType.DWARF_PLANET -> Icons.Outlined.Public
        ObjectType.MOON -> Icons.Outlined.NightsStay
        ObjectType.SUN -> Icons.Outlined.WbSunny
        ObjectType.STAR, ObjectType.ASTERISM -> Icons.Outlined.Star
        ObjectType.DEEP_SKY, ObjectType.GALAXY, ObjectType.NEBULA,
        ObjectType.STAR_CLUSTER, ObjectType.GLOBULAR_CLUSTER, ObjectType.BLACK_HOLE -> Icons.Outlined.AutoAwesome
        ObjectType.SATELLITE -> Icons.Outlined.Sensors
        ObjectType.METEOR_SHOWER -> Icons.Outlined.FlashOn
        ObjectType.CONSTELLATION -> Icons.Outlined.Grain
        ObjectType.REFERENCE_POINT, null -> Icons.Outlined.Place
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier
    )
}

@Composable
private fun EclipseItemRow(
    isSolar: Boolean,
    isPrimary: Boolean,
    title: String,
    dateStr: String,
    visibilityInfo: String,
    isLocallyVisible: Boolean,
    isFa: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = RedSpacing.md, vertical = RedSpacing.sm)
            .testTag("eclipse_item_row_${title.lowercase().replace(' ', '_')}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RedSpacing.md),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isSolar) Icons.Outlined.WbSunny else Icons.Outlined.NightsStay,
                contentDescription = null,
                tint = if (isPrimary) RedTheme.colors.accentRed else RedTheme.colors.textSecondary,
                modifier = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = RedTypographyTokens.bodyPrimary.copy(
                        fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = RedTheme.colors.textPrimary
                )
                Text(
                    text = dateStr,
                    style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Medium),
                    color = if (isPrimary) RedTheme.colors.accentRed else RedTheme.colors.textSecondary
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
                backgroundColor = RedTheme.colors.surfaceGrouped,
                textColor = RedTheme.colors.textTertiary,
                borderColor = RedTheme.colors.border
            )
        }
    }
}

@Composable
private fun TonightEventRow(
    event: WhatsUpTonightEngine.TonightEvent,
    isTopRanked: Boolean,
    isFa: Boolean,
    onClick: () -> Unit
) {
    val eventIcon = when {
        event.targetObject != null -> when (event.targetObject.type) {
            ObjectType.PLANET, ObjectType.DWARF_PLANET -> Icons.Outlined.Public
            ObjectType.MOON -> Icons.Outlined.NightsStay
            ObjectType.SUN -> Icons.Outlined.WbSunny
            ObjectType.STAR, ObjectType.ASTERISM -> Icons.Outlined.Star
            ObjectType.DEEP_SKY, ObjectType.GALAXY, ObjectType.NEBULA,
            ObjectType.STAR_CLUSTER, ObjectType.GLOBULAR_CLUSTER, ObjectType.BLACK_HOLE -> Icons.Outlined.AutoAwesome
            ObjectType.SATELLITE -> Icons.Outlined.Sensors
            ObjectType.METEOR_SHOWER -> Icons.Outlined.FlashOn
            ObjectType.CONSTELLATION -> Icons.Outlined.Grain
            ObjectType.REFERENCE_POINT -> Icons.Outlined.Place
        }
        event.id.contains("solar") -> Icons.Outlined.WbSunny
        event.id.contains("lunar") -> Icons.Outlined.NightsStay
        event.id.contains("meteor") -> Icons.Outlined.FlashOn
        event.id.contains("iss") -> Icons.Outlined.Sensors
        else -> Icons.Outlined.AutoAwesome
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = RedSpacing.md, vertical = RedSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RedSpacing.md),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = eventIcon,
                contentDescription = null,
                tint = if (isTopRanked) RedTheme.colors.accentRed else RedTheme.colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = if (isFa) event.titleFa else event.titleEn,
                    style = RedTypographyTokens.bodyPrimary.copy(
                        fontWeight = if (isTopRanked) FontWeight.SemiBold else FontWeight.Medium
                    ),
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
                    backgroundColor = RedTheme.colors.surfaceGrouped,
                    textColor = RedTheme.colors.textTertiary,
                    borderColor = RedTheme.colors.border
                )
            }
        }
    }
}
