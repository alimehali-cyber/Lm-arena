package com.alijafari.red.astronomy.ui.cyber

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SatelliteAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.CoordinateEngine
import com.alijafari.red.astronomy.astro_engine.CoordinateEngineLegacy
import com.alijafari.red.astronomy.astro_engine.EclipseEngine
import com.alijafari.red.astronomy.astro_engine.MoonEngine
import com.alijafari.red.astronomy.astro_engine.ObservabilityEngine
import com.alijafari.red.astronomy.astro_engine.SunEngine
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.astro_engine.WhatsUpTonightEngine
import com.alijafari.red.astronomy.data.catalog.AstronomyCatalog
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.ObjectType
import com.alijafari.red.astronomy.domain.TimeMachineMode
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.components.EclipseDetailModal
import com.alijafari.red.astronomy.ui.components.EclipseRender
import com.alijafari.red.astronomy.ui.components.EclipseWindow
import com.alijafari.red.astronomy.ui.components.HeroSkyCanvas
import com.alijafari.red.astronomy.ui.components.MoonPhaseRender
import com.alijafari.red.astronomy.ui.components.SkyCanvasPresentation
import com.alijafari.red.astronomy.ui.rendering.SkyTimeModel
import com.alijafari.red.astronomy.ui.theme.LocalAppFontFamily
import com.alijafari.red.astronomy.ui.theme.LocalBackdropLuminance
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.backdropInkFor
import com.alijafari.red.astronomy.ui.theme.solidSurface
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private const val CYBER_MOON_REFRESH_MS = 60_000L
private const val CYBER_CATALOG_REFRESH_MS = 60_000L
private const val CYBER_ECLIPSE_REFRESH_MS = 3_600_000L

private fun cyberBucketMs(timeMs: Long, isSimulation: Boolean, bucketMs: Long): Long =
    if (isSimulation || bucketMs <= 0L) timeMs else timeMs - java.lang.Math.floorMod(timeMs, bucketMs)

private enum class CyberTonightFilter {
    ALL, PLANETS, DEEP_SKY
}

/**
 * Cyber Glass UI Home Screen (Apple visionOS x Nothing OS x Teenage Engineering):
 *
 * - Leaves the full-bleed edge-to-edge [HeroSkyCanvas] (`SkyCanvasPresentation.SCENE`) completely
 *   untouched and unblurred at the top of the screen, overlaid with a Spatial Floating Capsule Bar.
 * - Introduces twin **Cutout Viewport Window Cards** (`CyberCutoutViewportCard`) for the interactive
 *   2.5D Moon sphere and Next Eclipse phenomenon, framing crisp unblurred astronomical viewports above
 *   ultra-thin geometric typography, Nothing Dot-Matrix counters, and sub-frosted Mesh Gradient Pills.
 * - Includes a Cyber Sky Telemetry Widget (`Bortle Quality`, `Solar Depression`, `Local Clock` in
 *   retro-futuristic dot-matrix counters + vibrant inner Mesh Gradient Pill action cards) and a
 *   Spatial Glass 2.0 "What's Up Tonight" matrix feed with segmented floating capsule filters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberHomeScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val palette = CyberTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN

    val liveTimeMs by viewModel.skyLiveClock.timeMs.collectAsState()
    val isSimulation = uiState.timeMachineState.mode == TimeMachineMode.SIMULATION
    val activeTimeMs = if (isSimulation) uiState.timeMachineState.simulationTimeMs else liveTimeMs

    val moonKeyMs = cyberBucketMs(activeTimeMs, isSimulation, CYBER_MOON_REFRESH_MS)
    val catalogKeyMs = cyberBucketMs(activeTimeMs, isSimulation, CYBER_CATALOG_REFRESH_MS)
    val eclipseKeyMs = cyberBucketMs(activeTimeMs, isSimulation, CYBER_ECLIPSE_REFRESH_MS)

    val catalogJd = remember(catalogKeyMs) { TimeEngine.getJulianDate(catalogKeyMs) }
    val allObjects = remember(catalogJd) { AstronomyCatalog.getAllObjects(catalogJd) }
    val catalogLastDeg = remember(catalogJd, uiState.userLocation) {
        TimeEngine.getLAST(catalogJd, uiState.userLocation.longitude)
    }

    val sunPos = remember(catalogJd) { SunEngine.calculatePosition(catalogJd) }
    val sunEquatorial = CoordinateEngine.Equatorial(sunPos.raDeg, sunPos.decDeg)
    val sunHoriz = remember(sunPos, catalogLastDeg, uiState.userLocation) {
        CoordinateEngine.equatorialToHorizontal(sunEquatorial, catalogLastDeg, uiState.userLocation.latitude)
    }

    val moonJd = remember(moonKeyMs) { TimeEngine.getJulianDate(moonKeyMs) }
    val moonData = remember(moonJd, uiState.userLocation) {
        MoonEngine.calculateMoon(moonJd, uiState.userLocation.latitude, uiState.userLocation.longitude)
    }

    val scrubbedMoonMs = remember(moonKeyMs, uiState.moonDayOffset) {
        moonKeyMs + (uiState.moonDayOffset * 86_400_000f).toLong()
    }
    val cardMoonData = remember(scrubbedMoonMs, uiState.userLocation) {
        val jd = TimeEngine.getJulianDate(scrubbedMoonMs)
        MoonEngine.calculateMoon(jd, uiState.userLocation.latitude, uiState.userLocation.longitude)
    }
    val cardSunHoriz = remember(scrubbedMoonMs, uiState.userLocation) {
        val jd = TimeEngine.getJulianDate(scrubbedMoonMs)
        val pos = SunEngine.calculatePosition(jd)
        val last = TimeEngine.getLAST(jd, uiState.userLocation.longitude)
        CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(pos.raDeg, pos.decDeg),
            last,
            uiState.userLocation.latitude
        )
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

    DisposableEffect(Unit) {
        onDispose { viewModel.setMoonDayOffset(0f) }
    }

    val tonightHighlights = remember(catalogJd, uiState.userLocation, isFa) {
        WhatsUpTonightEngine.calculateTonightEvents(
            jd = catalogJd,
            userLatDeg = uiState.userLocation.latitude,
            userLonDeg = uiState.userLocation.longitude,
            isFa = isFa
        ).take(8)
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

    val nextEclipse = remember(nextSolarEclipses, nextLunarEclipses) {
        listOfNotNull(nextSolarEclipses.firstOrNull(), nextLunarEclipses.firstOrNull())
            .minByOrNull { it.localPeakTimeMs }
    }

    var selectedEclipseForDetail by remember { mutableStateOf<EclipseEngine.EclipseResult?>(null) }
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var tonightFilter by rememberSaveable { mutableStateOf(CyberTonightFilter.ALL) }

    val filteredHighlights = remember(tonightHighlights, tonightFilter) {
        when (tonightFilter) {
            CyberTonightFilter.ALL -> tonightHighlights.take(6)
            CyberTonightFilter.PLANETS -> tonightHighlights.filter {
                val t = it.targetObject?.type
                t == ObjectType.PLANET || t == ObjectType.MOON || t == ObjectType.SUN
            }.ifEmpty { tonightHighlights.take(4) }
            CyberTonightFilter.DEEP_SKY -> tonightHighlights.filter {
                val t = it.targetObject?.type
                t != ObjectType.PLANET && t != ObjectType.MOON && t != ObjectType.SUN
            }.ifEmpty { tonightHighlights.take(4) }
        }
    }

    val trimmedQuery = searchQuery.trim()
    val filteredResults = remember(sortedObjectsWithObs, trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            sortedObjectsWithObs.take(20)
        } else {
            val q = trimmedQuery.lowercase(Locale.ROOT)
            sortedObjectsWithObs.filter { (obj, _, _) ->
                obj.nameEn.lowercase(Locale.ROOT).contains(q) ||
                    obj.nameFa.lowercase(Locale.ROOT).contains(q) ||
                    obj.constellationEn.lowercase(Locale.ROOT).contains(q) ||
                    obj.constellationFa.lowercase(Locale.ROOT).contains(q) ||
                    obj.category.lowercase(Locale.ROOT).contains(q) ||
                    obj.id.lowercase(Locale.ROOT).contains(q)
            }
        }
    }

    val backdropInk = backdropInkFor(LocalBackdropLuminance.current)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sceneHeight = (maxHeight * 0.50f).coerceIn(330.dp, 470.dp)
        val controlsBottom = 56.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("cyber_home_scroll"),
            contentPadding = PaddingValues(bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. UNTOUCHED FULL-BLEED SKY SCENE + SPATIAL FLOATING CAPSULE HEADER BAR
            item(key = "cyber_sky_hero") {
                CyberSkyHeroSection(
                    uiState = uiState,
                    viewModel = viewModel,
                    isFa = isFa,
                    sceneHeight = sceneHeight,
                    controlsBottom = controlsBottom,
                    onOpenSearch = { isSearchOpen = true }
                )
            }

            // 2. NOTHING-OS SKY TELEMETRY WIDGET + INNER MESH GRADIENT PILL CARDS
            item(key = "cyber_telemetry_widget") {
                CyberSkyTelemetryWidget(
                    uiState = uiState,
                    viewModel = viewModel,
                    isFa = isFa,
                    instantMs = activeTimeMs,
                    sunAltitudeDeg = sunHoriz.altitudeDeg,
                    onNavigateToTab = onNavigateToTab,
                    modifier = Modifier.padding(horizontal = RedSpacing.lg)
                )
            }

            // 3. TWIN CUTOUT VIEWPORT WINDOW CARDS (MOON CUTOUT + ECLIPSE CUTOUT)
            item(key = "cyber_cutout_viewport_pair") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RedSpacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CyberMoonCutoutCard(
                        uiState = uiState,
                        viewModel = viewModel,
                        isFa = isFa,
                        moonData = cardMoonData,
                        sunHoriz = cardSunHoriz,
                        onScrubDays = { delta -> viewModel.nudgeMoonDayOffset(delta) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_moon_card")
                    )

                    CyberEclipseCutoutCard(
                        eclipse = nextEclipse,
                        isFa = isFa,
                        onClick = { viewModel.setShowEclipseWindow(true) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_eclipse_card")
                    )
                }
            }

            // 4. TONIGHT'S CELESTIAL MATRIX SECTION HEADER + FLOATING CAPSULE FILTER BAR
            item(key = "cyber_tonight_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RedSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CyberSignalDot(size = 6.5.dp)
                                Text(
                                    text = if (isFa) "امشب در آسمان" else "What's Up Tonight",
                                    style = RedTypographyTokens.sectionTitle.copy(
                                        fontWeight = FontWeight.Light,
                                        fontSize = 18.sp,
                                        shadow = Shadow(
                                            color = backdropInk.halo,
                                            offset = Offset.Zero,
                                            blurRadius = 7f
                                        )
                                    ),
                                    color = palette.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isFa) "برترین رویدادها و اجرام قابل رصد امشب" else "Curated celestial events & meridian transits",
                                style = RedTypographyTokens.caption.copy(
                                    fontWeight = FontWeight.Light,
                                    shadow = Shadow(
                                        color = backdropInk.halo,
                                        offset = Offset.Zero,
                                        blurRadius = 6f
                                    )
                                ),
                                color = palette.textSecondary
                            )
                        }

                        CyberMatrixText(
                            text = if (isFa) {
                                TimeEngine.formatPersianNumbers(filteredHighlights.size.toString())
                            } else {
                                String.format(Locale.US, "0%d", filteredHighlights.size)
                            },
                            dotSize = 2.0.dp,
                            dotGap = 0.8.dp,
                            charGap = 2.2.dp,
                            activeColor = palette.matrixLit,
                            glowColor = palette.primaryAccent
                        )
                    }

                    // Spatial Floating Capsule Segmented Filter Bar
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val filters = listOf(
                            CyberTonightFilter.ALL to (if (isFa) "همه اجرام" else "All Matrix"),
                            CyberTonightFilter.PLANETS to (if (isFa) "منظومه شمسی" else "Solar System"),
                            CyberTonightFilter.DEEP_SKY to (if (isFa) "ستارگان و اعماق آسمان" else "Stars & Deep Sky")
                        )
                        items(filters, key = { it.first.name }) { (filter, label) ->
                            val selected = tonightFilter == filter
                            CyberFloatingCapsuleBar(
                                isActive = selected,
                                elevation = if (selected) 8.dp else 4.dp,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                                onClick = { tonightFilter = filter }
                            ) {
                                if (selected) {
                                    CyberSignalDot(size = 5.dp, glowing = false)
                                }
                                Text(
                                    text = label,
                                    style = RedTypographyTokens.caption.copy(
                                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Light,
                                        fontSize = 12.sp
                                    ),
                                    color = if (selected) palette.textPrimary else palette.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 5. TONIGHT'S MATRIX GLASS WIDGET
            item(key = "cyber_tonight_card") {
                CyberGlassWidget(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RedSpacing.lg)
                        .testTag("home_highlights_card"),
                    cornerRadius = 36.dp,
                    contentPadding = PaddingValues(20.dp),
                    ambientTint = palette.tertiaryAccent
                ) {
                    filteredHighlights.forEachIndexed { index, event ->
                        CyberTonightEventRow(
                            event = event,
                            isFa = isFa,
                            onClick = {
                                when {
                                    event.id.startsWith("solar_") -> {
                                        selectedEclipseForDetail = nextSolarEclipses.firstOrNull()
                                    }
                                    event.id.startsWith("lunar_") -> {
                                        selectedEclipseForDetail = nextLunarEclipses.firstOrNull()
                                    }
                                    else -> event.targetObject?.let { obj ->
                                        viewModel.openObjectDetail(obj)
                                    }
                                }
                            }
                        )
                        if (index < filteredHighlights.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                thickness = 0.6.dp,
                                color = palette.glassBorder.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }
        }

        // SEARCH OVERLAY
        AnimatedVisibility(
            visible = isSearchOpen,
            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { -it / 5 },
            exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { -it / 5 }
        ) {
            CyberSkySearchOverlay(
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it },
                viewModel = viewModel,
                isFa = isFa,
                results = filteredResults,
                onClose = {
                    searchQuery = ""
                    isSearchOpen = false
                }
            )
        }
    }

    if (uiState.showSkyViewer) {
        CyberFullscreenSkyViewer(
            uiState = uiState,
            viewModel = viewModel,
            isFa = isFa,
            onDismiss = { viewModel.setShowSkyViewer(false) }
        )
    }

    if (uiState.showEclipseWindow) {
        EclipseWindow(
            solarEclipses = nextSolarEclipses,
            lunarEclipses = nextLunarEclipses,
            language = uiState.language,
            userLocation = uiState.userLocation,
            initialFamily = nextEclipse?.event?.type,
            onDismiss = { viewModel.setShowEclipseWindow(false) }
        )
    }

    selectedEclipseForDetail?.let { eclipseRes ->
        val detailedInfo = remember(eclipseRes, uiState.userLocation) {
            EclipseEngine.computeDetailedInfo(
                result = eclipseRes,
                userLatDeg = uiState.userLocation.latitude,
                userLonDeg = uiState.userLocation.longitude,
                timezoneId = uiState.userLocation.timezoneId
            )
        }
        EclipseDetailModal(
            detailedInfo = detailedInfo,
            language = uiState.language,
            onDismiss = { selectedEclipseForDetail = null }
        )
    }
}

@Composable
private fun CyberSkyHeroSection(
    uiState: MainUiState,
    viewModel: MainViewModel,
    isFa: Boolean,
    sceneHeight: Dp,
    controlsBottom: Dp,
    onOpenSearch: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(sceneHeight + controlsBottom)
            .testTag("home_sky_scene")
    ) {
        // FULL-BLEED UNTOUCHED SKY CANVAS (SkyCanvasPresentation.SCENE)
        HeroSkyCanvas(
            uiState = uiState,
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .height(sceneHeight),
            presentation = SkyCanvasPresentation.SCENE,
            bottomTailHeight = controlsBottom
        )

        // Delicate top legibility veil
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sceneHeight * 0.30f)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.36f),
                        0.60f to Color.Black.copy(alpha = 0.12f),
                        1.0f to Color.Transparent
                    )
                )
        )

        // SPATIAL FLOATING CAPSULE TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = RedSpacing.lg, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CyberMicroIconButton(
                icon = Icons.Outlined.BookmarkBorder,
                contentDescription = stringResource(R.string.favorites_and_history),
                onClick = { viewModel.setShowFavoritesDialog(true) },
                modifier = Modifier.testTag("top_favorites_button")
            )

            // Center Dynamic Telemetry Capsule Brand Pill
            CyberFloatingCapsuleBar(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp),
                elevation = 10.dp
            ) {
                CyberSignalDot(size = 6.dp)
                Text(
                    text = if (isFa) "زیگ" else "ZIG",
                    style = TextStyle(
                        fontFamily = LocalAppFontFamily.current,
                        fontWeight = FontWeight.Light,
                        fontSize = 15.sp,
                        letterSpacing = if (isFa) 0.sp else 4.sp,
                        color = Color.White
                    ),
                    modifier = Modifier.testTag("home_red_title")
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CyberMicroIconButton(
                    icon = Icons.Outlined.Search,
                    contentDescription = if (isFa) "جستجوی اجرام آسمان" else "Search the sky",
                    onClick = onOpenSearch,
                    modifier = Modifier.testTag("top_search_button")
                )
                CyberMicroIconButton(
                    icon = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.settings),
                    onClick = { viewModel.setShowSettingsDialog(true) },
                    modifier = Modifier.testTag("top_settings_button")
                )
            }
        }

        // BOTTOM SKY FLOATING CAPSULE ROW: LOCATION PILL + FULLSCREEN SKY VIEWER PILL
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = RedSpacing.lg, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CyberFloatingCapsuleBar(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                elevation = 10.dp,
                onClick = { viewModel.setShowLocationSelector(true) },
                modifier = Modifier
                    .weight(1f, fill = false)
                    .testTag("home_location_selector_button")
            ) {
                Icon(
                    imageVector = if (uiState.isLiveGpsLocation) Icons.Default.MyLocation else Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = CyberTheme.palette.primaryAccent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (uiState.isLiveGpsLocation) {
                        if (isFa) "GPS زنده" else "Live GPS"
                    } else {
                        if (isFa) uiState.userLocation.cityNameFa else uiState.userLocation.cityNameEn
                    },
                    style = RedTypographyTokens.caption.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 12.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            CyberFloatingCapsuleBar(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                elevation = 10.dp,
                onClick = { viewModel.setShowSkyViewer(true) },
                modifier = Modifier.testTag("home_sky_expand_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.FitScreen,
                    contentDescription = if (isFa) "نمایش تمام‌صفحه آسمان" else "Open the sky fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (isFa) "نمای کامل" else "360° Sky",
                    style = RedTypographyTokens.caption.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 11.5.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Nothing-OS x visionOS Telemetry Widget:
 * Pairs retro-futuristic Dot-Matrix counters (`3 Good`, `-18.4°`, `22:41`) with ultra-light
 * geometric labels and sub-frosted vibrant Mesh Gradient Pill action cards.
 */
@Composable
private fun CyberSkyTelemetryWidget(
    uiState: MainUiState,
    viewModel: MainViewModel,
    isFa: Boolean,
    instantMs: Long,
    sunAltitudeDeg: Double,
    onNavigateToTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = CyberTheme.palette
    val dragOffsetHours by viewModel.skyDragOffsetHours.collectAsState()
    val userTimeZone = remember(uiState.userLocation.timezoneId) {
        TimeEngine.resolveTimeZone(uiState.userLocation.timezoneId)
    }
    val effectiveMs = remember(instantMs, dragOffsetHours) {
        TimeEngine.getTimestampFromJulianDate(SkyTimeModel.effectiveJd(instantMs, dragOffsetHours))
    }
    val clockText = remember(effectiveMs, isFa, userTimeZone) {
        TimeEngine.formatTime24h(effectiveMs, isFa, userTimeZone)
    }
    val dateText = remember(effectiveMs, isFa, userTimeZone, uiState.calendarSystem) {
        TimeEngine.formatDate(effectiveMs, uiState.calendarSystem, isFa, userTimeZone)
    }

    val bortleNum = if (isFa) {
        TimeEngine.formatPersianNumbers(uiState.bortleClass.toString())
    } else {
        uiState.bortleClass.toString()
    }
    val bortleStatus = when {
        uiState.bortleClass <= 3 -> if (isFa) "عالی" else "Dark"
        uiState.bortleClass <= 5 -> if (isFa) "خوب" else "Fair"
        else -> if (isFa) "شهری" else "Bad"
    }
    val bortleAccent = when {
        uiState.bortleClass <= 3 -> palette.statusGood
        uiState.bortleClass <= 5 -> palette.statusWarn
        else -> palette.signalDot
    }

    val sunAltRaw = String.format(Locale.US, "%+.1f°", sunAltitudeDeg)
    val sunAltText = if (isFa) TimeEngine.formatPersianNumbers(sunAltRaw) else sunAltRaw
    val skyStateWord = when {
        sunAltitudeDeg > 0.0 -> if (isFa) "روز" else "Day"
        sunAltitudeDeg > -12.0 -> if (isFa) "گرگ‌ومیش" else "Dusk"
        else -> if (isFa) "شب تاریک" else "Night"
    }
    val isScrubbed = abs(dragOffsetHours) > 0.05f

    CyberGlassWidget(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 36.dp,
        contentPadding = PaddingValues(22.dp),
        ambientTint = palette.primaryAccent
    ) {
        // Top Row: Local Dot-Matrix Clock + Date Capsule
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CyberSignalDot(
                        color = if (isScrubbed) palette.statusWarn else palette.signalDot,
                        size = 6.dp
                    )
                    Text(
                        text = if (isScrubbed) {
                            if (isFa) "شبیه‌سازی زمان" else "TIME SCRUB"
                        } else {
                            if (isFa) "تله‌متری زنده رصد" else "LIVE OBSERVATORY"
                        },
                        style = RedTypographyTokens.caption.copy(
                            fontWeight = FontWeight.Light,
                            fontSize = 10.5.sp,
                            letterSpacing = if (isFa) 0.sp else 1.2.sp
                        ),
                        color = palette.textMuted
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                CyberMatrixText(
                    text = clockText,
                    dotSize = 3.0.dp,
                    dotGap = 1.1.dp,
                    charGap = 3.4.dp,
                    showMatrixGrid = true,
                    activeColor = palette.matrixLit,
                    glowColor = palette.primaryAccent
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = dateText,
                    style = RedTypographyTokens.bodySecondary.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 12.5.sp
                    ),
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(
                        Locale.US,
                        "%.2f°N  %.2f°E",
                        uiState.userLocation.latitude,
                        uiState.userLocation.longitude
                    ),
                    style = RedTypographyTokens.caption.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 11.sp
                    ),
                    color = palette.textMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        HorizontalDivider(thickness = 0.6.dp, color = palette.glassBorder.copy(alpha = 0.45f))
        Spacer(modifier = Modifier.height(16.dp))

        // Middle Row: Nothing-OS Dot-Matrix Stat Counters (`3 Dark`, `-18.4° Night`)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            CyberStatCounter(
                valueText = bortleNum,
                statusLabel = bortleStatus,
                captionLabel = if (isFa) "مقیاس بورتل" else "BORTLE INDEX",
                dotSize = 2.6.dp,
                accentColor = bortleAccent,
                modifier = Modifier.weight(1f)
            )

            CyberStatCounter(
                valueText = sunAltText,
                statusLabel = skyStateWord,
                captionLabel = if (isFa) "ارتفاع خورشید" else "SOLAR ALT",
                dotSize = 2.4.dp,
                accentColor = palette.secondaryAccent,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Inner Sub-Frosted Mesh Gradient Pill Action Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CyberMeshPillCard(
                preset = CyberMeshPreset.CYAN_VIOLET,
                isCapsule = true,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                onClick = { onNavigateToTab(3) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Explore,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isFa) "آسمان‌نمای AR" else "AR Sky Finder",
                    style = RedTypographyTokens.caption.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            CyberMeshPillCard(
                preset = CyberMeshPreset.CORAL_PEACH,
                isCapsule = true,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                onClick = { onNavigateToTab(1) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.SatelliteAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isFa) "گذر ماهواره‌ها" else "Orbital Passes",
                    style = RedTypographyTokens.caption.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Cutout Viewport Window Card #1 — Lunar Viewport:
 * Capsule/oval cutout revealing the crisp, unblurred 2.5D interactive Moon render against
 * starlight, while framing ultra-thin lunar typography, a Dot-Matrix illumination counter,
 * and a Mesh Gradient Pill below it in frosted glass.
 */
@Composable
private fun CyberMoonCutoutCard(
    uiState: MainUiState,
    viewModel: MainViewModel,
    isFa: Boolean,
    moonData: MoonEngine.MoonData,
    sunHoriz: CoordinateEngine.Horizontal,
    onScrubDays: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = CyberTheme.palette
    val illumInt = moonData.illuminationPercent.roundToInt()
    val illumFormatted = if (isFa) {
        "${TimeEngine.formatPersianNumbers(illumInt.toString())}٪"
    } else {
        "$illumInt%"
    }
    val phaseName = if (isFa) moonData.phaseNameFa else moonData.phaseNameEn
    val horizonLabel = if (moonData.altitudeDeg > 0.0) {
        if (isFa) "بالای افق" else "Above horizon"
    } else {
        if (isFa) "زیر افق" else "Below horizon"
    }

    CyberCutoutViewportCard(
        modifier = modifier,
        viewportHeight = 122.dp,
        isCapsuleCutout = true,
        ambientTint = palette.primaryAccent,
        onClick = { viewModel.setShowMoonWindow(true) },
        viewportContent = {
            // Crisp starfield pinpoints inside unblurred lunar cutout window
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in 0 until 14) {
                    val fx = (((sin(i * 19.31 + 1.7) * 43758.5) % 1.0) + 1.0) % 1.0
                    val fy = (((sin(i * 47.11 + 3.1) * 19283.7) % 1.0) + 1.0) % 1.0
                    drawCircle(
                        color = Color.White.copy(alpha = 0.22f + (i % 3) * 0.12f),
                        radius = if (i % 4 == 0) 1.4f else 0.95f,
                        center = Offset((fx * size.width).toFloat(), (fy * size.height).toFloat())
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            onScrubDays(dragAmount / 16f)
                        }
                    }
                    .testTag("home_moon_card_render"),
                contentAlignment = Alignment.Center
            ) {
                MoonPhaseRender(
                    moonData = moonData,
                    sunHoriz = sunHoriz,
                    latitude = uiState.userLocation.latitude,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    ) {
        CyberMatrixText(
            text = illumFormatted,
            dotSize = 2.45.dp,
            dotGap = 0.95.dp,
            charGap = 2.8.dp,
            showMatrixGrid = true,
            activeColor = palette.matrixLit,
            glowColor = palette.primaryAccent
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = phaseName,
            style = RedTypographyTokens.bodyPrimary.copy(
                fontWeight = FontWeight.Light,
                fontSize = 14.sp
            ),
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = horizonLabel,
            style = RedTypographyTokens.caption.copy(
                fontWeight = FontWeight.Light,
                fontSize = 11.5.sp
            ),
            color = palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        CyberMeshPillCard(
            preset = CyberMeshPreset.CYAN_VIOLET,
            isCapsule = true,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            onClick = { viewModel.setShowMoonWindow(true) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Outlined.NightsStay,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (isFa) "اطلس ماه" else "Lunar Atlas",
                style = RedTypographyTokens.caption.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Cutout Viewport Window Card #2 — Eclipse Viewport:
 * Capsule/oval cutout revealing the crisp, unblurred Eclipse phenomenon render, while framing
 * ultra-thin eclipse details, a Dot-Matrix countdown counter, and a Coral-Peach / Wine Mesh Pill below.
 */
@Composable
private fun CyberEclipseCutoutCard(
    eclipse: EclipseEngine.EclipseResult?,
    isFa: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = CyberTheme.palette
    val eclipseType = eclipse?.localType ?: EclipseEngine.EclipseType.TOTAL_SOLAR
    val daysUntil = eclipse?.daysRemaining?.coerceAtLeast(0L)?.toInt() ?: 0
    val countdownMatrix = remember(daysUntil, isFa) {
        val raw = "${daysUntil}D"
        if (isFa) TimeEngine.formatPersianNumbers(daysUntil.toString()) else raw
    }
    val titleText = remember(eclipse, isFa) {
        if (eclipse == null) {
            if (isFa) "خورشیدگرفتگی" else "Next Eclipse"
        } else {
            if (isFa) eclipse.localNameFa else eclipse.localNameEn
        }
    }
    val dateSubtitle = remember(eclipse, isFa) {
        if (eclipse == null) {
            if (isFa) "در حال محاسبه" else "Calculating..."
        } else {
            if (isFa) eclipse.formattedDateFa else eclipse.formattedDateEn
        }
    }

    CyberCutoutViewportCard(
        modifier = modifier,
        viewportHeight = 122.dp,
        isCapsuleCutout = true,
        ambientTint = palette.secondaryAccent,
        onClick = onClick,
        viewportContent = {
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in 0 until 12) {
                    val fx = (((sin(i * 31.7 + 0.9) * 25913.3) % 1.0) + 1.0) % 1.0
                    val fy = (((sin(i * 63.2 + 2.4) * 31847.9) % 1.0) + 1.0) % 1.0
                    drawCircle(
                        color = Color.White.copy(alpha = 0.24f),
                        radius = 1.1f,
                        center = Offset((fx * size.width).toFloat(), (fy * size.height).toFloat())
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .testTag("home_eclipse_card_render"),
                contentAlignment = Alignment.Center
            ) {
                EclipseRender(
                    type = eclipseType,
                    contentDescription = titleText,
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f)
                )
            }
        }
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CyberMatrixText(
                text = countdownMatrix,
                dotSize = 2.45.dp,
                dotGap = 0.95.dp,
                charGap = 2.8.dp,
                showMatrixGrid = true,
                activeColor = palette.matrixLit,
                glowColor = palette.secondaryAccent
            )
            if (isFa) {
                Text(
                    text = "روز",
                    style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Light),
                    color = palette.secondaryAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = titleText,
            style = RedTypographyTokens.bodyPrimary.copy(
                fontWeight = FontWeight.Light,
                fontSize = 14.sp
            ),
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = dateSubtitle,
            style = RedTypographyTokens.caption.copy(
                fontWeight = FontWeight.Light,
                fontSize = 11.5.sp
            ),
            color = palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        CyberMeshPillCard(
            preset = CyberMeshPreset.CORAL_PEACH,
            isCapsule = true,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Outlined.WbSunny,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (isFa) "تقویم گرفت‌ها" else "Eclipse Guide",
                style = RedTypographyTokens.caption.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CyberTonightEventRow(
    event: WhatsUpTonightEngine.TonightEvent,
    isFa: Boolean,
    onClick: () -> Unit
) {
    val palette = CyberTheme.palette
    val obj = event.targetObject
    val scoreFraction = when (event.visibilityStatus) {
        WhatsUpTonightEngine.EventVisibilityStatus.OPTIMAL -> 0.94f
        WhatsUpTonightEngine.EventVisibilityStatus.GOOD -> 0.78f
        WhatsUpTonightEngine.EventVisibilityStatus.MARGINAL -> 0.54f
        WhatsUpTonightEngine.EventVisibilityStatus.NOT_VISIBLE -> 0.24f
    }
    val scoreInt = (scoreFraction * 100f).roundToInt()
    val magBadge = remember(obj, scoreInt, isFa) {
        val m = obj?.magnitude
        if (m != null && m < 25.0) {
            val raw = String.format(Locale.US, "%.1f", m)
            if (isFa) TimeEngine.formatPersianNumbers(raw) else raw
        } else {
            if (isFa) TimeEngine.formatPersianNumbers(scoreInt.toString()) else scoreInt.toString()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon Capsule with Subtle Mesh Glow
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(palette.primaryAccent.copy(alpha = 0.14f))
                .border(1.dp, palette.glassBorder, CircleShape)
        ) {
            Icon(
                imageVector = when {
                    obj?.type == ObjectType.PLANET || obj?.type == ObjectType.DWARF_PLANET -> Icons.Outlined.Public
                    obj?.type == ObjectType.MOON || event.id.contains("lunar") -> Icons.Outlined.NightsStay
                    obj?.type == ObjectType.SUN || event.id.contains("solar") -> Icons.Outlined.WbSunny
                    obj?.type == ObjectType.GALAXY || obj?.type == ObjectType.NEBULA || obj?.type == ObjectType.STAR_CLUSTER -> Icons.Outlined.AutoAwesome
                    else -> Icons.Outlined.Star
                },
                contentDescription = null,
                tint = palette.primaryAccent,
                modifier = Modifier.size(19.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isFa) event.titleFa else event.titleEn,
                style = RedTypographyTokens.bodyPrimary.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = 14.5.sp
                ),
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isFa) event.explanationFa else event.explanationEn,
                style = RedTypographyTokens.caption.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = 11.5.sp
                ),
                color = palette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        CyberDotRingGauge(
            progress = scoreFraction,
            centerText = magBadge,
            diameter = 46.dp,
            accentColor = palette.primaryAccent,
            secondaryColor = palette.secondaryAccent
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CyberSkySearchOverlay(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    viewModel: MainViewModel,
    isFa: Boolean,
    results: List<Triple<CelestialObject, CoordinateEngine.Horizontal, ObservabilityEngine.ObservabilityResult>>,
    onClose: () -> Unit
) {
    val palette = CyberTheme.palette
    val focusRequester = remember { FocusRequester() }
    val appFont = LocalAppFontFamily.current

    BackHandler(onBack = onClose)
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvasBg.copy(alpha = 0.90f))
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = RedSpacing.lg, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CyberGlassWidget(
                cornerRadius = 32.dp,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                elevated = true
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = palette.primaryAccent,
                        modifier = Modifier.size(19.dp)
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onQueryChange,
                        placeholder = {
                            Text(
                                text = if (isFa) "جستجوی ستاره، سیاره، سحابی..." else "Search stars, planets, nebulae...",
                                style = RedTypographyTokens.bodySecondary.copy(fontWeight = FontWeight.Light),
                                color = palette.textMuted
                            )
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = appFont,
                            fontWeight = FontWeight.Light,
                            fontSize = 15.sp,
                            color = palette.textPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = palette.primaryAccent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .testTag("sky_search_input")
                    )
                    CyberMicroIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        onClick = onClose,
                        size = 34.dp,
                        iconSize = 16.dp
                    )
                }
            }

            if (results.isNotEmpty()) {
                CyberGlassWidget(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 34.dp,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(results.take(25), key = { it.first.id }) { (obj, horiz, _) ->
                            val altStr = String.format(Locale.US, "%+.0f°", horiz.altitudeDeg)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.openObjectDetail(obj)
                                        onClose()
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isFa) obj.nameFa else obj.nameEn,
                                        style = RedTypographyTokens.bodyPrimary.copy(fontWeight = FontWeight.Light),
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = if (isFa) obj.constellationFa.ifEmpty { obj.category }
                                        else obj.constellationEn.ifEmpty { obj.category },
                                        style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Light),
                                        color = palette.textSecondary
                                    )
                                }
                                CyberMatrixText(
                                    text = if (isFa) TimeEngine.formatPersianNumbers(altStr) else altStr,
                                    dotSize = 1.9.dp,
                                    dotGap = 0.75.dp,
                                    charGap = 2.0.dp,
                                    activeColor = if (horiz.altitudeDeg > 0) palette.statusGood else palette.textMuted,
                                    glowColor = if (horiz.altitudeDeg > 0) palette.statusGood else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findCyberActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findCyberActivity()
    else -> null
}

@Composable
private fun CyberFullscreenSkyViewer(
    uiState: MainUiState,
    viewModel: MainViewModel,
    isFa: Boolean,
    onDismiss: () -> Unit
) {
    val hostView = LocalView.current
    val activity = remember(hostView) { hostView.context.findCyberActivity() }

    val originalOrientation = rememberSaveable {
        mutableIntStateOf(activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
    }
    DisposableEffect(activity) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            if (activity?.isChangingConfigurations != true) {
                activity?.requestedOrientation = originalOrientation.intValue
            }
        }
    }
    BackHandler { onDismiss() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogView = LocalView.current
        val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
        SideEffectForFullscreenWindow(dialogWindow, dialogView)

        CompositionLocalProvider(
            LocalLayoutDirection provides if (isFa) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .testTag("sky_canvas_fullscreen")
            ) {
                HeroSkyCanvas(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(RedSpacing.md)
                        .size(38.dp)
                        .solidSurface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.38f),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.32f))
                        )
                        .clickable(onClick = onDismiss)
                        .testTag("sky_canvas_fullscreen_close"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (isFa) "بستن نمای تمام‌صفحه" else "Close fullscreen sky",
                        tint = Color(0xFFF8FAFF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SideEffectForFullscreenWindow(
    dialogWindow: android.view.Window?,
    dialogView: android.view.View
) {
    DisposableEffect(dialogWindow) {
        if (dialogWindow != null) {
            dialogWindow.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            dialogWindow.setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK))
            dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dialogWindow.attributes = dialogWindow.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            val controller = WindowInsetsControllerCompat(dialogWindow, dialogView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { }
    }
}
