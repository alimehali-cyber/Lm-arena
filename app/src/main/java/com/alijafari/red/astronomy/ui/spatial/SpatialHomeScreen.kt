package com.alijafari.red.astronomy.ui.spatial

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.CoordinateEngine
import com.alijafari.red.astronomy.astro_engine.EclipseEngine
import com.alijafari.red.astronomy.astro_engine.MoonEngine
import com.alijafari.red.astronomy.astro_engine.ObservabilityEngine
import com.alijafari.red.astronomy.astro_engine.SunEngine
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.data.catalog.AstronomyCatalog
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.ObjectType
import com.alijafari.red.astronomy.domain.TimeMachineMode
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.components.EclipseRender
import com.alijafari.red.astronomy.ui.components.EclipseWindow
import com.alijafari.red.astronomy.ui.components.HeroSkyCanvas
import com.alijafari.red.astronomy.ui.components.MoonPhaseRender
import com.alijafari.red.astronomy.ui.components.SkyCanvasPresentation
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

private const val SPATIAL_MOON_REFRESH_MS = 60_000L
private const val SPATIAL_CATALOG_REFRESH_MS = 60_000L
private const val SPATIAL_ECLIPSE_REFRESH_MS = 3_600_000L

private fun spatialBucketMs(timeMs: Long, isSimulation: Boolean, bucketMs: Long): Long =
    if (isSimulation || bucketMs <= 0L) timeMs else timeMs - java.lang.Math.floorMod(timeMs, bucketMs)

/**
 * VisionOS Spatial Glassmorphism & Bento Grid Home Screen.
 *
 * Presents ZIG's live sky ephemeris, lunar phase, upcoming eclipse, time-drift scrubber, and
 * tonight's celestial catalog as modular floating G2 squircles, mesh gradient pills, glowing
 * neon toggles, LED dot-matrix telemetry, and a circular radial dot meter.
 */
@Composable
fun SpatialHomeScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val palette = SpatialTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN

    val liveTimeMs by viewModel.skyLiveClock.timeMs.collectAsState()
    val skyDragOffsetHours by viewModel.skyDragOffsetHours.collectAsState()
    val isSimulation = uiState.timeMachineState.mode == TimeMachineMode.SIMULATION
    val baseTimeMs = if (isSimulation) {
        uiState.timeMachineState.simulationTimeMs
    } else {
        liveTimeMs
    }
    val activeTimeMs = baseTimeMs + (skyDragOffsetHours * 3_600_000f).toLong()

    val moonKeyMs = spatialBucketMs(activeTimeMs, isSimulation || skyDragOffsetHours != 0f, SPATIAL_MOON_REFRESH_MS)
    val catalogKeyMs = spatialBucketMs(activeTimeMs, isSimulation || skyDragOffsetHours != 0f, SPATIAL_CATALOG_REFRESH_MS)
    val eclipseKeyMs = spatialBucketMs(baseTimeMs, isSimulation, SPATIAL_ECLIPSE_REFRESH_MS)

    val catalogJd = remember(catalogKeyMs) { TimeEngine.getJulianDate(catalogKeyMs) }
    val allObjects = remember(catalogJd) { AstronomyCatalog.getAllObjects(catalogJd) }
    val catalogLastDeg = remember(catalogJd, uiState.userLocation) {
        TimeEngine.getLAST(catalogJd, uiState.userLocation.longitude)
    }

    val sunPos = remember(catalogJd) { SunEngine.calculatePosition(catalogJd) }
    val sunHoriz = remember(sunPos, catalogLastDeg, uiState.userLocation) {
        CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(sunPos.raDeg, sunPos.decDeg),
            catalogLastDeg,
            uiState.userLocation.latitude
        )
    }

    val moonJd = remember(moonKeyMs, uiState.moonDayOffset) {
        TimeEngine.getJulianDate(moonKeyMs + (uiState.moonDayOffset.toDouble() * 86_400_000.0).toLong())
    }
    val moonData = remember(moonJd, uiState.userLocation) {
        MoonEngine.calculateMoon(moonJd, uiState.userLocation.latitude, uiState.userLocation.longitude)
    }
    val moonSunHoriz = remember(moonJd, uiState.userLocation) {
        val sPos = SunEngine.calculatePosition(moonJd)
        val sLast = TimeEngine.getLAST(moonJd, uiState.userLocation.longitude)
        CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(sPos.raDeg, sPos.decDeg),
            sLast,
            uiState.userLocation.latitude
        )
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.setMoonDayOffset(0f) }
    }

    val sortedObjectsWithObs = remember(
        allObjects,
        catalogLastDeg,
        sunHoriz,
        moonData,
        uiState.userLocation,
        uiState.bortleClass
    ) {
        allObjects
            .filter { it.id != "planet_earth" && it.id != "sat_iss" && it.type != ObjectType.SATELLITE }
            .map { obj ->
                val eq = if (obj.id == "moon") {
                    CoordinateEngine.geocentricToTopocentric(
                        geocentric = CoordinateEngine.Equatorial(moonData.raDeg, moonData.decDeg),
                        geocentricDistanceKm = moonData.distanceKm,
                        lastDeg = catalogLastDeg,
                        latitudeDeg = uiState.userLocation.latitude,
                        elevationM = uiState.userLocation.elevationMeters
                    )
                } else {
                    CoordinateEngine.Equatorial(obj.raDeg, obj.decDeg)
                }
                val horiz = CoordinateEngine.equatorialToHorizontal(
                    eq,
                    catalogLastDeg,
                    uiState.userLocation.latitude,
                    uiState.userLocation.elevationMeters
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
            .sortedWith(
                compareByDescending<Triple<CelestialObject, CoordinateEngine.Horizontal, ObservabilityEngine.ObservabilityResult>> {
                    it.third.scorePercent
                }.thenByDescending { it.second.altitudeDeg }
            )
    }

    val (nextSolarEclipses, nextLunarEclipses) = remember(
        uiState.userLocation,
        eclipseKeyMs,
        uiState.calendarSystem
    ) {
        EclipseEngine.getNextEclipseSeries(
            nowMs = eclipseKeyMs,
            userLatDeg = uiState.userLocation.latitude,
            userLonDeg = uiState.userLocation.longitude,
            elevationM = uiState.userLocation.elevationMeters,
            timezoneId = uiState.userLocation.timezoneId,
            calendarSystem = uiState.calendarSystem,
            count = 3
        )
    }
    val heroEclipse = remember(nextSolarEclipses, nextLunarEclipses) {
        listOfNotNull(nextSolarEclipses.firstOrNull(), nextLunarEclipses.firstOrNull())
            .minByOrNull { it.localPeakTimeMs }
    }

    // Category filter for Tonight's Bento Grid: 0=All, 1=Planets, 2=Stars, 3=Deep Sky
    var selectedCategory by remember { mutableIntStateOf(0) }
    val tonightFiltered = remember(sortedObjectsWithObs, selectedCategory) {
        sortedObjectsWithObs
            .filter { (obj, horiz, _) ->
                val aboveHorizon = horiz.altitudeDeg > -2.0
                val catMatch = when (selectedCategory) {
                    1 -> obj.type == ObjectType.PLANET || obj.id == "moon" || obj.id == "sun"
                    2 -> obj.type == ObjectType.STAR
                    3 -> obj.type == ObjectType.DEEP_SKY
                    else -> true
                }
                aboveHorizon && catMatch
            }
            .take(18)
    }

    // Predictive Search Overlay State
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    BackHandler(enabled = isSearchOpen) {
        isSearchOpen = false
        searchQuery = ""
    }

    val liveClockText = remember(activeTimeMs / 30_000L, uiState.userLocation.timezoneId, isFa) {
        val sdf = SimpleDateFormat("HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone(uiState.userLocation.timezoneId)
        }
        val raw = sdf.format(Date(activeTimeMs))
        if (isFa) TimeEngine.formatPersianNumbers(raw) else raw
    }

    val formattedDate = remember(activeTimeMs / 60_000L, uiState.calendarSystem, isFa, uiState.userLocation.timezoneId) {
        TimeEngine.formatDate(
            timestampMs = activeTimeMs,
            calendarSystem = uiState.calendarSystem,
            isFa = isFa,
            timeZone = TimeEngine.resolveTimeZone(uiState.userLocation.timezoneId)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_column"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 112.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Floating Spatial Top Header Bar
        item {
            SpatialTopHeaderBar(
                isFa = isFa,
                liveClockText = liveClockText,
                onOpenFavorites = { viewModel.setShowFavoritesDialog(true) },
                onOpenSearch = { isSearchOpen = true },
                onOpenSettings = { viewModel.setShowSettingsDialog(true) }
            )
        }

        // 2. Bento Hero Window: VisionOS Squircle Sky Observatory Viewport
        item {
            SpatialSkyObservatoryWindow(
                uiState = uiState,
                viewModel = viewModel,
                isFa = isFa,
                formattedDate = formattedDate,
                onOpenLocation = { viewModel.setShowLocationSelector(true) },
                onExpandSkyViewer = { viewModel.setShowSkyViewer(true) }
            )
        }

        // 3. Asymmetric Bento Row: Lunar Radial Dot Meter + Mesh Gradient Eclipse & AR Pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Squircle Card: Lunar Phase + Circular Radial Dot Meter
                SpatialLunarBentoCard(
                    moonData = moonData,
                    sunHoriz = moonSunHoriz,
                    latitude = uiState.userLocation.latitude,
                    isFa = isFa,
                    onClick = { viewModel.setShowMoonWindow(true) },
                    modifier = Modifier
                        .weight(1.10f)
                        .height(238.dp)
                )

                // Right Column: Stacked Mesh Gradient Pills (Pink-to-Orange Eclipse + Blue-to-Purple AR/Bortle)
                Column(
                    modifier = Modifier
                        .weight(0.90f)
                        .height(238.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SpatialEclipseMeshPill(
                        eclipse = heroEclipse,
                        isFa = isFa,
                        onClick = { viewModel.setShowEclipseWindow(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    SpatialSkyQualityMeshPill(
                        bortleClass = uiState.bortleClass,
                        sunAltitudeDeg = sunHoriz.altitudeDeg,
                        isFa = isFa,
                        onOpenArSky = { onNavigateToTab(3) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }

        // 4. Spatial Time-Drift Scrubber Bento Card (Glow Toggle + LED Offset + Glow Slider)
        item {
            SpatialTimeDriftBentoCard(
                skyDragOffsetHours = skyDragOffsetHours,
                isFa = isFa,
                onOffsetChange = { viewModel.setSkyDragOffsetHours(it) },
                onResetLive = { viewModel.setSkyDragOffsetHours(0f) }
            )
        }

        // 5. What's Up Tonight Section Header + Glowing Category Capsules
        item {
            val countLabel = if (isFa) {
                "${TimeEngine.formatPersianNumbers(tonightFiltered.size.toString())} جرم فعال"
            } else {
                "${tonightFiltered.size} Visible"
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SpatialSectionHeader(
                    title = if (isFa) "امشب در آسمان" else "Tonight's Sky Bento",
                    subtitle = if (isFa) "مرتب‌شده بر اساس شاخص رصدپذیری زنده" else "Ranked by live local observability score",
                    badgeText = countLabel
                )

                val categories = listOf(
                    0 to (if (isFa) "همه اجرام" else "All Sky"),
                    1 to (if (isFa) "منظومه شمسی" else "Solar System"),
                    2 to (if (isFa) "ستارگان درخشان" else "Bright Stars"),
                    3 to (if (isFa) "اعماق آسمان" else "Deep Sky")
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { (idx, label) ->
                        SpatialGlassCapsule(
                            isActive = selectedCategory == idx,
                            activeGlowColor = SpatialNeonRose,
                            elevation = 6.dp,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            onClick = { selectedCategory = idx }
                        ) {
                            Text(
                                text = label,
                                style = RedTypographyTokens.caption.copy(
                                    fontWeight = if (selectedCategory == idx) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = if (selectedCategory == idx) palette.accentInk else palette.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // 6. Modular 2-Column Bento Grid of Celestial Objects
        val pairedObjects = tonightFiltered.chunked(2)
        items(pairedObjects) { rowPair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowPair.forEach { (obj, horiz, obs) ->
                    SpatialCelestialBentoTile(
                        obj = obj,
                        horiz = horiz,
                        obs = obs,
                        isFa = isFa,
                        onClick = { viewModel.openObjectDetail(obj, activeTimeMs) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowPair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    if (isSearchOpen) {
        SpatialSkySearchDialog(
            isFa = isFa,
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            allObjectsWithObs = sortedObjectsWithObs,
            onSelectObject = { obj ->
                isSearchOpen = false
                searchQuery = ""
                viewModel.openObjectDetail(obj, activeTimeMs)
            },
            onDismiss = {
                isSearchOpen = false
                searchQuery = ""
            }
        )
    }

    if (uiState.showSkyViewer) {
        Dialog(
            onDismissRequest = { viewModel.setShowSkyViewer(false) },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                HeroSkyCanvas(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    presentation = SkyCanvasPresentation.VIEWER,
                    bottomTailHeight = 0.dp
                )
                SpatialGlowIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = stringResource(R.string.ok),
                    onClick = { viewModel.setShowSkyViewer(false) },
                    isActive = true,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                )
            }
        }
    }

    if (uiState.showEclipseWindow) {
        EclipseWindow(
            solarEclipses = nextSolarEclipses,
            lunarEclipses = nextLunarEclipses,
            language = uiState.language,
            userLocation = uiState.userLocation,
            initialFamily = heroEclipse?.event?.type,
            onDismiss = { viewModel.setShowEclipseWindow(false) }
        )
    }
}

@Composable
private fun SpatialTopHeaderBar(
    isFa: Boolean,
    liveClockText: String,
    onOpenFavorites: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val palette = SpatialTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SpatialGlowIconButton(
                icon = Icons.Outlined.BookmarkBorder,
                contentDescription = stringResource(R.string.favorites_and_history),
                onClick = onOpenFavorites,
                modifier = Modifier.testTag("top_favorites_button")
            )
            Column {
                Text(
                    text = if (isFa) "زیگ فضایی" else "ZIG SPATIAL",
                    style = RedTypographyTokens.sectionTitle.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        letterSpacing = if (isFa) 0.sp else 1.8.sp
                    ),
                    color = palette.textPrimary,
                    modifier = Modifier.testTag("home_red_title")
                )
                Text(
                    text = if (isFa) "رصدخانه شیشه‌ای" else "Spatial Observatory",
                    style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                    color = palette.textSecondary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Live Dot-Matrix Clock Capsule
            SpatialGlassCapsule(
                contentPadding = PaddingValues(horizontal = 11.dp, vertical = 8.dp),
                isActive = false,
                elevation = 6.dp
            ) {
                DotMatrixText(
                    text = liveClockText,
                    dotSize = 2.0.dp,
                    dotGap = 0.85.dp,
                    charGap = 2.2.dp,
                    activeColor = palette.dotMatrixInk,
                    glowColor = SpatialNeonRose
                )
            }

            SpatialGlowIconButton(
                icon = Icons.Outlined.Search,
                contentDescription = if (isFa) "جستجوی اجرام آسمان" else "Search the sky",
                onClick = onOpenSearch,
                modifier = Modifier.testTag("top_search_button")
            )

            SpatialGlowIconButton(
                icon = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.settings),
                onClick = onOpenSettings,
                isActive = true,
                modifier = Modifier.testTag("top_settings_button")
            )
        }
    }
}

@Composable
private fun SpatialSkyObservatoryWindow(
    uiState: MainUiState,
    viewModel: MainViewModel,
    isFa: Boolean,
    formattedDate: String,
    onOpenLocation: () -> Unit,
    onExpandSkyViewer: () -> Unit
) {
    val palette = SpatialTheme.palette
    val windowShape = squircleShape(34.dp)
    val rimBrush = remember(palette) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.78f),
                palette.rimSideBorder,
                palette.rimBottomReflection
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(276.dp)
            .shadow(
                elevation = 20.dp,
                shape = windowShape,
                ambientColor = palette.ambientDropShadow,
                spotColor = palette.ambientDropShadow
            )
            .clip(windowShape)
            .drawWithContent {
                drawContent()
                // Inset top specular rim highlight over the sky viewport
                val bevel = 2.2.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.62f), Color.Transparent),
                        startY = 0f,
                        endY = bevel * 2.2f
                    ),
                    topLeft = Offset.Zero,
                    size = Size(size.width, bevel * 2.2f)
                )
            }
            .border(BorderStroke(1.2.dp, rimBrush), windowShape)
            .testTag("home_sky_scene")
    ) {
        HeroSkyCanvas(
            uiState = uiState,
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize(),
            presentation = SkyCanvasPresentation.SCENE,
            bottomTailHeight = 0.dp
        )

        // Subtle bottom vignette for floating capsule legibility
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(84.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.44f))
                    )
                )
        )

        // Top-Start Date Capsule floating inside viewport
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(squircleShape(16.dp))
                .background(Color.Black.copy(alpha = 0.34f))
                .border(0.8.dp, Color.White.copy(alpha = 0.32f), squircleShape(16.dp))
                .padding(horizontal = 11.dp, vertical = 5.dp)
        ) {
            Text(
                text = formattedDate,
                style = RedTypographyTokens.caption.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.5.sp
                ),
                color = Color.White
            )
        }

        // Bottom Controls Row: Location Selector Pill + Glowing Expand Sky Pill
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val cityText = if (uiState.isLiveGpsLocation) {
                if (isFa) "GPS زنده" else "Live GPS"
            } else {
                if (isFa) uiState.userLocation.cityNameFa else uiState.userLocation.cityNameEn
            }

            Row(
                modifier = Modifier
                    .clip(squircleShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.42f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.38f), squircleShape(20.dp))
                    .clickable(onClick = onOpenLocation)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("home_location_selector_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isLiveGpsLocation) Icons.Default.MyLocation else Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = SpatialNeonRose,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = cityText,
                    style = RedTypographyTokens.caption.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier
                    .clip(squircleShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                SpatialNeonRose.copy(alpha = 0.44f),
                                SpatialWarmPeach.copy(alpha = 0.38f)
                            )
                        )
                    )
                    .border(0.8.dp, Color.White.copy(alpha = 0.65f), squircleShape(20.dp))
                    .clickable(onClick = onExpandSkyViewer)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("home_sky_expand_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = if (isFa) "نمای کامل" else "Sky View",
                    style = RedTypographyTokens.caption.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SpatialLunarBentoCard(
    moonData: MoonEngine.MoonData,
    sunHoriz: CoordinateEngine.Horizontal,
    latitude: Double,
    isFa: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = SpatialTheme.palette
    val illumInt = moonData.illuminationPercent.roundToInt().coerceIn(0, 100)
    val illumText = if (isFa) TimeEngine.formatPersianNumbers("$illumInt%") else "$illumInt%"
    val ageStr = String.format(Locale.US, "%.1fd", moonData.ageDays)
    val ageDisplay = if (isFa) {
        "${TimeEngine.formatPersianNumbers(String.format(Locale.US, "%.1f", moonData.ageDays))} روز"
    } else {
        "Age $ageStr"
    }

    SpatialGlassCard(
        modifier = modifier.testTag("home_moon_card"),
        cornerRadius = 32.dp,
        contentPadding = PaddingValues(15.dp),
        elevation = 14.dp,
        glowAccent = SpatialNeonRose,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isFa) "فاز ماه" else "LUNAR PHASE",
                    style = RedTypographyTokens.caption.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = if (isFa) 0.sp else 0.8.sp
                    ),
                    color = palette.textTertiary
                )
                Text(
                    text = if (isFa) moonData.phaseNameFa else moonData.phaseNameEn,
                    style = RedTypographyTokens.bodyPrimary.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    ),
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            ) {
                MoonPhaseRender(
                    moonData = moonData,
                    sunHoriz = sunHoriz,
                    latitude = latitude,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            SpatialRadialDotMeter(
                progress = (moonData.illuminationPercent / 100.0).toFloat(),
                centerValueText = illumText,
                subValueText = ageDisplay,
                statusLabel = if (isFa) "روشنایی سطح ماه" else "Lunar Illumination",
                diameter = 114.dp,
                tickCount = 32,
                activeColor = SpatialNeonRose,
                secondaryActiveColor = SpatialWarmPeach,
                dotSize = 2.2.dp
            )
        }
    }
}

@Composable
private fun SpatialEclipseMeshPill(
    eclipse: EclipseEngine.EclipseResult?,
    isFa: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val type = eclipse?.localType ?: EclipseEngine.EclipseType.TOTAL_SOLAR
    val name = when {
        eclipse != null -> if (isFa) eclipse.localNameFa else eclipse.localNameEn
        isFa -> "خورشیدگرفتگی"
        else -> "Solar Eclipse"
    }
    val days = eclipse?.daysRemaining?.coerceAtLeast(0) ?: 0
    val daysMatrix = if (isFa) {
        TimeEngine.formatPersianNumbers("${days}D")
    } else {
        "${days}D"
    }

    SpatialMeshPill(
        preset = SpatialMeshPreset.ROSE_TO_ORANGE,
        cornerRadius = 28.dp,
        contentPadding = PaddingValues(13.dp),
        onClick = if (eclipse != null) onClick else null,
        modifier = modifier.testTag("home_eclipse_card")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(34.dp)) {
                    EclipseRender(
                        type = type,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                DotMatrixText(
                    text = daysMatrix,
                    dotSize = 2.0.dp,
                    dotGap = 0.8.dp,
                    charGap = 2.2.dp,
                    activeColor = Color(0xFF23111C),
                    glowColor = Color.White
                )
            }

            Column {
                Text(
                    text = if (isFa) "گرفت بعدی" else "NEXT ECLIPSE",
                    style = RedTypographyTokens.caption.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color(0xB823111C),
                    maxLines = 1
                )
                Text(
                    text = name,
                    style = RedTypographyTokens.bodyPrimary.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    ),
                    color = Color(0xFF23111C),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SpatialSkyQualityMeshPill(
    bortleClass: Int,
    sunAltitudeDeg: Double,
    isFa: Boolean,
    onOpenArSky: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skyStateText = when {
        sunAltitudeDeg > 0.0 -> if (isFa) "آسمان روز" else "Daylight Sky"
        sunAltitudeDeg > -6.0 -> if (isFa) "گرگ‌ومیش مدنی" else "Civil Twilight"
        sunAltitudeDeg > -12.0 -> if (isFa) "گرگ‌ومیش دریایی" else "Nautical Dusk"
        sunAltitudeDeg > -18.0 -> if (isFa) "گرگ‌ومیش نجومی" else "Astro Twilight"
        else -> if (isFa) "شب تاریک نجومی" else "Astronomical Night"
    }
    val bortleCode = if (isFa) TimeEngine.formatPersianNumbers("B$bortleClass") else "B$bortleClass"

    SpatialMeshPill(
        preset = SpatialMeshPreset.BLUE_TO_PURPLE,
        cornerRadius = 28.dp,
        contentPadding = PaddingValues(13.dp),
        onClick = onOpenArSky,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }

                DotMatrixText(
                    text = bortleCode,
                    dotSize = 2.1.dp,
                    dotGap = 0.85.dp,
                    charGap = 2.4.dp,
                    activeColor = Color.White,
                    glowColor = SpatialSkyBlue
                )
            }

            Column {
                Text(
                    text = skyStateText,
                    style = RedTypographyTokens.caption.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isFa) "قطب‌نمای آسمان AR" else "Launch AR Sky",
                    style = RedTypographyTokens.bodyPrimary.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SpatialTimeDriftBentoCard(
    skyDragOffsetHours: Float,
    isFa: Boolean,
    onOffsetChange: (Float) -> Unit,
    onResetLive: () -> Unit
) {
    val palette = SpatialTheme.palette
    val isLive = abs(skyDragOffsetHours) < 0.05f
    val rawOffsetStr = if (isLive) {
        "0.0H"
    } else {
        val sign = if (skyDragOffsetHours > 0f) "+" else "-"
        String.format(Locale.US, "%s%.1fH", sign, abs(skyDragOffsetHours))
    }
    val offsetDisplay = if (isFa) TimeEngine.formatPersianNumbers(rawOffsetStr) else rawOffsetStr

    SpatialGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 30.dp,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        elevation = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isFa) "ماشین زمان آسمان" else "Spatial Sky Time-Drift",
                    style = RedTypographyTokens.bodyPrimary.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = palette.textPrimary
                )
                Text(
                    text = if (isLive) {
                        if (isFa) "همگام با زمان زنده رصدخانه" else "Synced to live observatory clock"
                    } else {
                        if (isFa) "جابجایی ساعتی ستارگان و ماه" else "Scrubbing hourly sky & lunar motion"
                    },
                    style = RedTypographyTokens.caption.copy(fontSize = 11.5.sp),
                    color = palette.textSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DotMatrixText(
                    text = offsetDisplay,
                    dotSize = 2.0.dp,
                    dotGap = 0.8.dp,
                    charGap = 2.2.dp,
                    activeColor = palette.dotMatrixInk,
                    glowColor = if (isLive) SpatialMintGlow else SpatialNeonRose
                )
                SpatialGlowToggle(
                    checked = isLive,
                    onCheckedChange = { wantLive ->
                        if (wantLive) onResetLive() else onOffsetChange(2.0f)
                    },
                    glowColor = SpatialNeonRose
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        SpatialGlowSlider(
            value = skyDragOffsetHours,
            onValueChange = onOffsetChange,
            valueRange = -12f..12f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SpatialCelestialBentoTile(
    obj: CelestialObject,
    horiz: CoordinateEngine.Horizontal,
    obs: ObservabilityEngine.ObservabilityResult,
    isFa: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = SpatialTheme.palette
    val altInt = horiz.altitudeDeg.roundToInt()
    val altMatrix = run {
        val raw = "${if (altInt >= 0) "+" else ""}${altInt}°"
        if (isFa) TimeEngine.formatPersianNumbers(raw) else raw
    }
    val scoreText = run {
        val raw = "${obs.scorePercent}%"
        if (isFa) TimeEngine.formatPersianNumbers(raw) else raw
    }

    val accentGlow = when (obj.type) {
        ObjectType.PLANET -> SpatialWarmPeach
        ObjectType.STAR -> SpatialSkyBlue
        ObjectType.DEEP_SKY -> SpatialCosmicIris
        else -> SpatialNeonRose
    }

    SpatialGlassCard(
        modifier = modifier,
        cornerRadius = 28.dp,
        contentPadding = PaddingValues(14.dp),
        elevation = 10.dp,
        glowAccent = accentGlow,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(squircleShape(10.dp))
                    .background(accentGlow.copy(alpha = if (palette.isDark) 0.22f else 0.18f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = scoreText,
                    style = RedTypographyTokens.badge.copy(fontSize = 10.sp),
                    color = if (palette.isDark) accentGlow else palette.accentInk
                )
            }

            DotMatrixText(
                text = altMatrix,
                dotSize = 1.9.dp,
                dotGap = 0.75.dp,
                charGap = 2.0.dp,
                activeColor = palette.dotMatrixInk,
                glowColor = accentGlow
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isFa) obj.nameFa else obj.nameEn,
            style = RedTypographyTokens.bodyPrimary.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        val subLabel = if (isFa) {
            obj.constellationFa.ifBlank { obj.category }
        } else {
            obj.constellationEn.ifBlank { obj.category }
        }
        Text(
            text = subLabel,
            style = RedTypographyTokens.caption.copy(fontSize = 11.5.sp),
            color = palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SpatialSkySearchDialog(
    isFa: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    allObjectsWithObs: List<Triple<CelestialObject, CoordinateEngine.Horizontal, ObservabilityEngine.ObservabilityResult>>,
    onSelectObject: (CelestialObject) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = SpatialTheme.palette
    val filtered = remember(query, allObjectsWithObs) {
        val q = query.trim().lowercase()
        allObjectsWithObs.filter { (obj, _, _) ->
            if (q.isEmpty()) true else {
                obj.nameFa.lowercase().contains(q) ||
                    obj.nameEn.lowercase().contains(q) ||
                    obj.constellationFa.lowercase().contains(q) ||
                    obj.constellationEn.lowercase().contains(q) ||
                    obj.category.lowercase().contains(q)
            }
        }.take(24)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 28.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            SpatialGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
                cornerRadius = 34.dp,
                contentPadding = PaddingValues(18.dp),
                elevation = 24.dp,
                elevated = true,
                glowAccent = SpatialNeonRose
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFa) "جستجوی اجرام آسمانی" else "Celestial Search",
                        style = RedTypographyTokens.sectionTitle.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary
                    )
                    SpatialGlowIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = stringResource(R.string.ok),
                        onClick = onDismiss,
                        size = 34.dp,
                        iconSize = 16.dp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = RedTypographyTokens.bodyPrimary.copy(color = palette.textPrimary),
                    placeholder = {
                        Text(
                            text = if (isFa) "نام ستاره، سیاره یا سحابی..." else "Planet, star, nebula, galaxy...",
                            style = RedTypographyTokens.bodyPrimary,
                            color = palette.textTertiary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = palette.accentInk
                        )
                    },
                    shape = squircleShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary,
                        focusedBorderColor = palette.accentInk,
                        unfocusedBorderColor = palette.rimSideBorder,
                        cursorColor = palette.accentInk
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { (obj, horiz, _) ->
                        val altStr = run {
                            val v = horiz.altitudeDeg.roundToInt()
                            val raw = "${if (v >= 0) "+" else ""}${v}°"
                            if (isFa) TimeEngine.formatPersianNumbers(raw) else raw
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(squircleShape(18.dp))
                                .background(palette.glassFill)
                                .clickable { onSelectObject(obj) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isFa) obj.nameFa else obj.nameEn,
                                    style = RedTypographyTokens.bodyPrimary.copy(fontSize = 14.sp),
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = if (isFa) obj.constellationFa else obj.constellationEn,
                                    style = RedTypographyTokens.caption,
                                    color = palette.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            DotMatrixText(
                                text = altStr,
                                dotSize = 1.9.dp,
                                activeColor = palette.dotMatrixInk,
                                glowColor = SpatialNeonRose
                            )
                        }
                    }
                }
            }
        }
    }
}
