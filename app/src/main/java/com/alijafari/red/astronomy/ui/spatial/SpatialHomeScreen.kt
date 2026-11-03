package com.alijafari.red.astronomy.ui.spatial

import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.ui.home.CatalogObjectsKey
import com.alijafari.red.astronomy.ui.home.EclipseSeriesKey
import com.alijafari.red.astronomy.ui.home.HomeAstroKeys
import com.alijafari.red.astronomy.ui.home.HomeAstroMemo
import com.alijafari.red.astronomy.ui.home.MoonKey
import com.alijafari.red.astronomy.ui.home.TonightEventsKey
import com.alijafari.red.astronomy.ui.home.rememberReadyTimeMs
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
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.alijafari.red.astronomy.ui.screens.HomeTextShift
import com.alijafari.red.astronomy.ui.theme.LocalAppFontFamily
import com.alijafari.red.astronomy.ui.theme.LocalBackdropLuminance
import com.alijafari.red.astronomy.ui.theme.RedCornerRadius
import com.alijafari.red.astronomy.ui.theme.RedIconSize
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.backdropInkFor
import com.alijafari.red.astronomy.ui.theme.solidSurface
import com.alijafari.red.astronomy.ui.theme.squircleShape
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val SPATIAL_MOON_REFRESH_MS = 60_000L
private const val SPATIAL_CATALOG_REFRESH_MS = 60_000L
private const val SPATIAL_ECLIPSE_REFRESH_MS = 3_600_000L

private fun spatialBucketMs(timeMs: Long, isSimulation: Boolean, bucketMs: Long): Long =
    if (isSimulation || bucketMs <= 0L) timeMs else timeMs - java.lang.Math.floorMod(timeMs, bucketMs)

/**
 * VisionOS Spatial Glassmorphism Home Screen — Exact Default Grid Layout & Untouched Sky Canvas.
 *
 * Preserves 100% of the Default HomeScreen's spatial hierarchy and layout:
 * - Full-bleed edge-to-edge [HeroSkyCanvas] (`SkyCanvasPresentation.SCENE`) starting at `y = 0`
 *   behind the status bar, with the exact top scrim, header actions, location/viewer pill row,
 *   and dissolved ground tail (`bottomTailHeight = controlsBottom`).
 * - Twin full-width stacked hero cards (`MoonCard` + `EclipseCard`) computed by [SpatialHeroCardMetrics]
 *   with [HomeTextShift] offsets, upgraded to VisionOS frosted translucent glass, specular top-edge
 *   rim lighting, inset glass bevels, and 5x7 LED dot-matrix clock/obscuration telemetry.
 * - Single-column "What's Up Tonight" grouped card (`home_highlights_card`) with top 5 tonight events,
 *   mesh-gradient status pills, and compact circular radial dot meters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpatialHomeScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    @Suppress("UNUSED_PARAMETER") onNavigateToTab: (Int) -> Unit
) {
    val isFa = uiState.language == AppLanguage.PERSIAN

    val liveTimeMs by viewModel.skyLiveClock.timeMs.collectAsState()
    val isSimulation = uiState.timeMachineState.mode == TimeMachineMode.SIMULATION
    val activeTimeMs = if (isSimulation) {
        uiState.timeMachineState.simulationTimeMs
    } else {
        liveTimeMs
    }

    // In live mode each bucket advances once its results are ready (computed off the main thread),
    // so a refresh never stalls a frame; see rememberReadyTimeMs. Simulation time is unchanged.
    val moonKeyMs = rememberReadyTimeMs(
        targetMs = spatialBucketMs(activeTimeMs, isSimulation, SPATIAL_MOON_REFRESH_MS),
        deferUntilReady = !isSimulation,
        maxDeferredStepMs = 2 * SPATIAL_MOON_REFRESH_MS
    ) { HomeAstroKeys.moonKeys(it, uiState) }
    val catalogKeyMs = rememberReadyTimeMs(
        targetMs = spatialBucketMs(activeTimeMs, isSimulation, SPATIAL_CATALOG_REFRESH_MS),
        deferUntilReady = !isSimulation,
        maxDeferredStepMs = 2 * SPATIAL_CATALOG_REFRESH_MS
    ) { HomeAstroKeys.catalogKeys(it, uiState, isFa) }
    val eclipseKeyMs = rememberReadyTimeMs(
        targetMs = spatialBucketMs(activeTimeMs, isSimulation, SPATIAL_ECLIPSE_REFRESH_MS),
        deferUntilReady = !isSimulation,
        maxDeferredStepMs = 2 * SPATIAL_ECLIPSE_REFRESH_MS
    ) { HomeAstroKeys.eclipseKeys(it, uiState) }

    val catalogJd = remember(catalogKeyMs) { TimeEngine.getJulianDate(catalogKeyMs) }
    val allObjects = remember(catalogJd) { HomeAstroMemo.get(CatalogObjectsKey(catalogJd)) }
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
        HomeAstroMemo.get(MoonKey(moonJd, uiState.userLocation.latitude, uiState.userLocation.longitude))
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

    val moonCardJd = TimeEngine.getJulianDate(
        moonKeyMs +
            (uiState.moonDayOffset.toDouble() * 86_400_000.0).toLong()
    )
    val moonCardData = remember(moonCardJd, uiState.userLocation) {
        HomeAstroMemo.get(MoonKey(moonCardJd, uiState.userLocation.latitude, uiState.userLocation.longitude))
    }
    val moonCardSunHoriz = remember(moonCardJd, uiState.userLocation) {
        val scrubSun = SunEngine.calculatePosition(moonCardJd)
        val scrubLast = TimeEngine.getLAST(moonCardJd, uiState.userLocation.longitude)
        CoordinateEngine.equatorialToHorizontal(
            equatorial = CoordinateEngine.Equatorial(scrubSun.raDeg, scrubSun.decDeg),
            lastDeg = scrubLast,
            latitudeDeg = uiState.userLocation.latitude
        )
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.setMoonDayOffset(0f) }
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }
    val closeSkySearch = {
        isSearchOpen = false
        searchQuery = ""
    }
    BackHandler(enabled = isSearchOpen) { closeSkySearch() }
    var selectedEclipseResult by remember { mutableStateOf<EclipseEngine.EclipseResult?>(null) }

    val (nextSolarEclipses, nextLunarEclipses) = remember(
        uiState.userLocation,
        eclipseKeyMs,
        uiState.calendarSystem
    ) {
        HomeAstroMemo.get(
            EclipseSeriesKey(
                nowMs = eclipseKeyMs,
                latitude = uiState.userLocation.latitude,
                longitude = uiState.userLocation.longitude,
                elevationM = uiState.userLocation.elevationMeters,
                timezoneId = uiState.userLocation.timezoneId,
                calendarSystem = uiState.calendarSystem,
                count = HomeAstroKeys.ECLIPSE_COUNT
            )
        )
    }

    val heroEclipse = remember(nextSolarEclipses, nextLunarEclipses) {
        listOfNotNull(nextSolarEclipses.firstOrNull(), nextLunarEclipses.firstOrNull())
            .minByOrNull { it.localPeakTimeMs }
    }

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
                userLonDeg = uiState.userLocation.longitude,
                timezoneId = uiState.userLocation.timezoneId
            )
        }
        EclipseDetailModal(
            detailedInfo = detailedInfo,
            language = uiState.language,
            onDismiss = { selectedEclipseResult = null }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sceneHeight = (maxHeight * 0.52f).coerceIn(312.dp, 560.dp)
        val heroCard = remember(maxWidth) { SpatialHeroCardMetrics.forWidth(maxWidth) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_column"),
            contentPadding = PaddingValues(bottom = RedSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(RedSpacing.lg)
        ) {
            // 0. THE SKY — exact full-bleed HeroSkyCanvas + exact stacked Moon & Eclipse cards
            item {
                SpatialHomeSkyHero(
                    uiState = uiState,
                    viewModel = viewModel,
                    isFa = isFa,
                    sceneHeight = sceneHeight,
                    card = heroCard,
                    instantMs = activeTimeMs,
                    moonData = moonCardData,
                    sunHoriz = moonCardSunHoriz,
                    nextEclipse = heroEclipse,
                    onScrubDays = { delta -> viewModel.nudgeMoonDayOffset(delta) },
                    onOpenEclipses = { viewModel.setShowEclipseWindow(true) },
                    onOpenSearch = { isSearchOpen = true }
                )
            }

            // 1. WHAT'S UP TONIGHT? — exact single-column grouped card layout with Spatial Glassmorphism
            item {
                val tonightEvents = remember(uiState.userLocation, catalogJd, isFa) {
                    HomeAstroMemo.get(
                        TonightEventsKey(
                            jd = catalogJd,
                            latitude = uiState.userLocation.latitude,
                            longitude = uiState.userLocation.longitude,
                            isFa = isFa,
                            issTle = ISSEngine.cachedTLE
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RedSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                ) {
                    SpatialHomeSectionHeader(
                        title = if (isFa) "امشب در آسمان" else "What's Up Tonight",
                        subtitle = if (isFa) "رویدادهای شاخص نجومی برای مختصات شما"
                        else "Key celestial events for your coordinates"
                    )

                    if (tonightEvents.isEmpty()) {
                        SpatialGlassCard(
                            cornerRadius = RedCornerRadius.lg,
                            contentPadding = PaddingValues(RedSpacing.lg),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isFa) "امشب رویداد شاخصی ثبت نشده است." else "No major celestial events tonight.",
                                style = RedTypographyTokens.bodySecondary,
                                color = SpatialTheme.palette.textSecondary
                            )
                        }
                    } else {
                        SpatialGlassCard(
                            cornerRadius = RedCornerRadius.xl,
                            contentPadding = PaddingValues(vertical = RedSpacing.xs),
                            glowAccent = SpatialNeonRose,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_highlights_card")
                        ) {
                            tonightEvents.take(5).forEachIndexed { idx, event ->
                                SpatialTonightEventRow(
                                    event = event,
                                    isTopRanked = idx == 0,
                                    isFa = isFa,
                                    onClick = {
                                        when {
                                            event.id.startsWith("solar_") -> {
                                                selectedEclipseResult = nextSolarEclipses.firstOrNull()
                                            }
                                            event.id.startsWith("lunar_") -> {
                                                selectedEclipseResult = nextLunarEclipses.firstOrNull()
                                            }
                                            else -> event.targetObject?.let { obj ->
                                                viewModel.openObjectDetail(obj)
                                            }
                                        }
                                    }
                                )
                                if (idx < minOf(4, tonightEvents.size - 1)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = RedSpacing.md)
                                            .height(0.75.dp)
                                            .background(SpatialTheme.palette.rimSideBorder.copy(alpha = 0.55f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(112.dp))
            }
        }

        // SKY SEARCH OVERLAY — exact top-aligned predictive search overlay in Spatial Glass
        AnimatedVisibility(
            visible = isSearchOpen,
            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { -it / 6 },
            exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { -it / 6 }
        ) {
            SpatialSkySearchOverlay(
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it },
                results = filteredSearchResults,
                isFa = isFa,
                uiState = uiState,
                viewModel = viewModel,
                moonData = moonData,
                catalogJd = catalogJd,
                onDismiss = { closeSkySearch() }
            )
        }

        // FULLSCREEN SKY VIEWER — untouched landscape edge-to-edge HeroSkyCanvas
        if (uiState.showSkyViewer) {
            SpatialFullscreenSkyViewer(
                uiState = uiState,
                viewModel = viewModel,
                onDismiss = { viewModel.setShowSkyViewer(false) }
            )
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
}

private data class SpatialHeroCardMetrics(
    val width: Dp,
    val cardHeight: Dp
) {
    val moonCardHeight: Dp get() = cardHeight
    val eclipseCardHeight: Dp get() = cardHeight
    val renderSquare: Dp get() = cardHeight

    companion object {
        val gap: Dp = RedSpacing.md

        fun forWidth(available: Dp): SpatialHeroCardMetrics {
            val square = ((available - RedSpacing.lg * 2 - gap) * 0.5f).coerceIn(132.dp, 210.dp)
            val width = (square * 2 + gap).coerceAtMost((available - RedSpacing.lg * 2).coerceAtLeast(square))
            val height = (square * 0.75f).coerceIn(112.dp, 152.dp).coerceAtMost(width)
            return SpatialHeroCardMetrics(width = width, cardHeight = height)
        }
    }
}

@Composable
private fun SpatialHomeSectionHeader(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier
) {
    val palette = SpatialTheme.palette
    val ink = backdropInkFor(LocalBackdropLuminance.current)
    val halo = if (ink.halo.alpha <= 0.01f) {
        null
    } else {
        Shadow(color = ink.halo, offset = Offset.Zero, blurRadius = 7f)
    }

    Column(
        modifier = modifier.padding(horizontal = RedSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = title,
            style = RedTypographyTokens.sectionTitle.copy(shadow = halo),
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = RedTypographyTokens.caption.copy(shadow = halo),
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Exact replica of Default UI's [HomeSkyHero] geometry and untouched [HeroSkyCanvas],
 * pairing the full-bleed sky scene with Spatial Glassmorphic Moon & Eclipse hero cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpatialHomeSkyHero(
    uiState: MainUiState,
    viewModel: MainViewModel,
    isFa: Boolean,
    sceneHeight: Dp,
    card: SpatialHeroCardMetrics,
    instantMs: Long,
    moonData: MoonEngine.MoonData,
    sunHoriz: CoordinateEngine.Horizontal,
    nextEclipse: EclipseEngine.EclipseResult?,
    onScrubDays: (Float) -> Unit,
    onOpenEclipses: () -> Unit,
    onOpenSearch: () -> Unit
) {
    val scrim = Color.Black
    val pillRowHeight = 38.dp
    val eclipseCardBottom = SpatialHeroCardMetrics.gap
    val moonCardBottom = eclipseCardBottom + card.eclipseCardHeight + SpatialHeroCardMetrics.gap
    val pillRowBottom = moonCardBottom + card.moonCardHeight + SpatialHeroCardMetrics.gap
    val controlsBottom = pillRowBottom + pillRowHeight

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(sceneHeight + controlsBottom)
            .testTag("home_sky_scene")
    ) {
        // THE SKY CANVAS — left 100% untouched, full-bleed edge-to-edge from y = 0
        HeroSkyCanvas(
            uiState = uiState,
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .height(sceneHeight),
            presentation = SkyCanvasPresentation.SCENE,
            bottomTailHeight = controlsBottom
        )

        // Top scrim — exact same height & gradient as HomeScreen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sceneHeight * 0.34f)
                .background(
                    Brush.verticalGradient(
                        0.0f to scrim.copy(alpha = 0.38f),
                        0.55f to scrim.copy(alpha = 0.14f),
                        1.0f to Color.Transparent
                    )
                )
        )

        // HEADER: brand and actions — exact same placement over the sky canvas
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = RedSpacing.lg, vertical = RedSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
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
                        tint = Color.White,
                        modifier = Modifier.size(RedIconSize.sm)
                    )
                }

                Text(
                    text = if (isFa) "زیگ" else "ZIG",
                    style = TextStyle(
                        fontFamily = LocalAppFontFamily.current,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = if (isFa) 0.sp else 3.sp,
                        color = Color.White,
                        shadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 1f), 8f)
                    ),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("home_red_title")
                )

                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .testTag("top_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = if (isFa) "جستجوی اجرام آسمان" else "Search the sky",
                            tint = Color.White,
                            modifier = Modifier.size(RedIconSize.sm)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setShowSettingsDialog(true) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .testTag("top_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = Color.White,
                            modifier = Modifier.size(RedIconSize.sm)
                        )
                    }
                }
            }
        }

        // PILL ROW JUST ABOVE THE MOON CARD: Location capsule + Fullscreen Sky Viewer button
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = RedSpacing.lg,
                    end = RedSpacing.lg,
                    bottom = pillRowBottom
                )
                .height(pillRowHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpatialHomeLocationPill(
                isFa = isFa,
                isLiveGps = uiState.isLiveGpsLocation,
                cityName = if (isFa) uiState.userLocation.cityNameFa else uiState.userLocation.cityNameEn,
                onClick = { viewModel.setShowLocationSelector(true) },
                modifier = Modifier.weight(1f, fill = false)
            )

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .solidSurface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.38f),
                        border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.45f))
                    )
                    .clickable { viewModel.setShowSkyViewer(true) }
                    .testTag("home_sky_expand_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FitScreen,
                    contentDescription = if (isFa) "نمایش تمام‌صفحه آسمان" else "Open the sky fullscreen",
                    tint = Color(0xFFF8FAFF),
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        // CARD 1: the Moon, with the Spatial LED Dot-Matrix & Condition Readout beside it
        SpatialMoonCard(
            uiState = uiState,
            viewModel = viewModel,
            isFa = isFa,
            instantMs = instantMs,
            renderSquare = card.renderSquare,
            moonData = moonData,
            sunHoriz = sunHoriz,
            onScrubDays = onScrubDays,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = moonCardBottom)
                .width(card.width)
                .height(card.moonCardHeight)
        )

        // CARD 2: the next eclipse, directly beneath the Moon card
        SpatialEclipseCard(
            eclipse = nextEclipse,
            isFa = isFa,
            timezoneId = uiState.userLocation.timezoneId,
            onClick = onOpenEclipses,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = eclipseCardBottom)
                .width(card.width)
                .height(card.eclipseCardHeight)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpatialHomeLocationPill(
    isFa: Boolean,
    isLiveGps: Boolean,
    cityName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RedShapes.capsule,
        color = Color.Black.copy(alpha = 0.34f),
        border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.45f)),
        modifier = modifier.testTag("home_location_selector_button")
    ) {
        Row(
            modifier = Modifier.padding(start = 11.dp, end = 13.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isLiveGps) Icons.Default.MyLocation else Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = SpatialNeonRose,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (isLiveGps) {
                    if (isFa) "GPS زنده" else "Live GPS"
                } else {
                    cityName
                },
                style = RedTypographyTokens.bodySecondary.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SpatialMoonCard(
    uiState: MainUiState,
    viewModel: MainViewModel,
    isFa: Boolean,
    instantMs: Long,
    renderSquare: Dp,
    moonData: MoonEngine.MoonData,
    sunHoriz: CoordinateEngine.Horizontal,
    onScrubDays: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    SpatialGlassCard(
        modifier = modifier.testTag("home_moon_card"),
        cornerRadius = RedCornerRadius.window,
        contentPadding = PaddingValues(0.dp),
        elevation = 16.dp,
        elevated = true,
        glowAccent = SpatialNeonRose
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(SpatialHeroCardMetrics.gap)
        ) {
            Box(
                modifier = Modifier
                    .width(renderSquare)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { viewModel.setShowMoonWindow(true) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(RedSpacing.sm)
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

            SpatialMoonCardReadout(
                uiState = uiState,
                viewModel = viewModel,
                isFa = isFa,
                instantMs = instantMs,
                moonData = moonData,
                sunHoriz = sunHoriz,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun SpatialMoonCardReadout(
    uiState: MainUiState,
    viewModel: MainViewModel,
    isFa: Boolean,
    instantMs: Long,
    moonData: MoonEngine.MoonData,
    sunHoriz: CoordinateEngine.Horizontal,
    modifier: Modifier = Modifier
) {
    val palette = SpatialTheme.palette
    val dragOffsetHours by viewModel.skyDragOffsetHours.collectAsState()
    val userTimeZone = remember(uiState.userLocation.timezoneId) {
        TimeEngine.resolveTimeZone(uiState.userLocation.timezoneId)
    }
    val shownMs = remember(instantMs, dragOffsetHours) {
        TimeEngine.getTimestampFromJulianDate(SkyTimeModel.effectiveJd(instantMs, dragOffsetHours))
    }
    val timeText = remember(shownMs, isFa, userTimeZone) {
        TimeEngine.formatTime24h(shownMs, isFa, userTimeZone)
    }
    val dateText = remember(shownMs, isFa, userTimeZone, uiState.calendarSystem) {
        TimeEngine.formatDate(shownMs, uiState.calendarSystem, isFa, userTimeZone)
    }
    val conditionText = spatialSkyConditionLabel(sunHoriz.altitudeDeg, isFa)
    val moonText = remember(moonData, isFa) {
        val percent = moonData.illuminationPercent.roundToInt()
        val pctFormatted = if (isFa) TimeEngine.formatPersianNumbers(percent.toString()) else percent.toString()
        if (moonData.altitudeDeg > 0.0) {
            if (isFa) "ماه $pctFormatted٪ بالای افق" else "Moon $pctFormatted% up"
        } else {
            if (isFa) "ماه زیر افق" else "Moon below horizon"
        }
    }
    val isReadingSimulatedInstant = abs(dragOffsetHours) > 0.05f

    Column(
        modifier = modifier
            .offset(x = HomeTextShift.moonCard)
            .padding(horizontal = RedSpacing.md)
            .testTag("home_moon_card_readout"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        // 5x7 LED Dot-Matrix Clock Display inside the exact clock slot
        DotMatrixText(
            text = timeText,
            dotSize = 2.55.dp,
            dotGap = 1.0.dp,
            charGap = 2.8.dp,
            activeColor = palette.dotMatrixInk,
            glowColor = SpatialNeonRose
        )

        Spacer(modifier = Modifier.height(RedSpacing.sm))
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(1.5.dp)
                .background(
                    Brush.horizontalGradient(listOf(SpatialNeonRose, SpatialWarmPeach))
                )
        )
        Spacer(modifier = Modifier.height(RedSpacing.sm))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (isReadingSimulatedInstant) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(SpatialNeonRose)
                )
            }
            Text(
                text = conditionText,
                style = RedTypographyTokens.bodySecondary.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                ),
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = dateText,
            style = RedTypographyTokens.caption,
            color = palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = moonText,
            style = RedTypographyTokens.caption,
            color = palette.accentInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SpatialEclipseCard(
    eclipse: EclipseEngine.EclipseResult?,
    isFa: Boolean,
    timezoneId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val type = eclipse?.localType ?: EclipseEngine.EclipseType.TOTAL_SOLAR
    val name = when {
        eclipse != null -> if (isFa) eclipse.localNameFa else eclipse.localNameEn
        isFa -> "خورشیدگرفتگی"
        else -> "Solar eclipse"
    }

    SpatialGlassCard(
        modifier = modifier.testTag("home_eclipse_card"),
        cornerRadius = RedCornerRadius.window,
        contentPadding = PaddingValues(0.dp),
        elevation = 16.dp,
        elevated = true,
        glowAccent = SpatialWarmPeach,
        onClick = if (eclipse != null) onClick else null
    ) {
        val appDirection = LocalLayoutDirection.current
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(modifier = Modifier.fillMaxSize()) {
                val detailsWeight = Modifier.weight(1f)

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .padding(RedSpacing.sm)
                        .testTag("home_eclipse_card_render"),
                    contentAlignment = Alignment.Center
                ) {
                    EclipseRender(
                        type = type,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                CompositionLocalProvider(LocalLayoutDirection provides appDirection) {
                    SpatialEclipseCardDetails(
                        eclipse = eclipse,
                        name = name,
                        isFa = isFa,
                        timezoneId = timezoneId,
                        modifier = detailsWeight
                            .fillMaxHeight()
                            .offset(x = HomeTextShift.eclipseCard)
                            .padding(start = RedSpacing.xxs, end = RedSpacing.sm)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpatialEclipseCardDetails(
    eclipse: EclipseEngine.EclipseResult?,
    name: String,
    isFa: Boolean,
    timezoneId: String,
    modifier: Modifier = Modifier
) {
    val palette = SpatialTheme.palette
    if (eclipse == null) {
        Column(
            modifier = modifier.testTag("home_eclipse_card_details"),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = name,
                style = RedTypographyTokens.bodySecondary.copy(fontWeight = FontWeight.SemiBold),
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isFa) "گرفتگی بعدی به‌زودی" else "The next eclipse is being computed",
                style = RedTypographyTokens.caption,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        return
    }

    val userTimeZone = remember(timezoneId) { TimeEngine.resolveTimeZone(timezoneId) }
    val dateText = if (isFa) eclipse.formattedDateFa else eclipse.formattedDateEn
    val days = eclipse.daysRemaining
    val countdown = when {
        days <= 0 -> if (isFa) "امروز" else "Today"
        days == 1 -> if (isFa) "فردا" else "Tomorrow"
        isFa -> "${TimeEngine.formatPersianNumbers(days.toString())} روز دیگر"
        else -> "in $days days"
    }
    val peakTime = remember(eclipse.localPeakTimeMs, isFa, userTimeZone) {
        TimeEngine.formatTime24h(eclipse.localPeakTimeMs, isFa, userTimeZone)
    }
    val magnitudeText = remember(eclipse, isFa) {
        if (eclipse.event.isSolar) {
            val pct = eclipse.localObscurationPercent
            if (isFa) "${TimeEngine.formatPersianNumbers(pct.toString())}٪ پوشش" else "$pct% covered"
        } else {
            val mag = String.format(Locale.US, "%.2f", eclipse.localMagnitude)
            if (isFa) "بزرگی ${TimeEngine.formatPersianNumbers(mag)}" else "magnitude $mag"
        }
    }
    val grade = if (isFa) eclipse.visibility.grade.labelFa else eclipse.visibility.grade.labelEn
    val accent = if (eclipse.visibility.isObservable) palette.successInk else palette.warningInk

    Column(
        modifier = modifier.testTag("home_eclipse_card_details"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = name,
            style = RedTypographyTokens.bodySecondary.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                lineHeight = 16.sp
            ),
            color = palette.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "$dateText · $countdown",
            style = RedTypographyTokens.caption,
            color = palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = if (isFa) {
                "$magnitudeText · اوج ${TimeEngine.formatPersianNumbers(peakTime)} · $grade"
            } else {
                "$magnitudeText · peak $peakTime · $grade"
            },
            style = RedTypographyTokens.caption.copy(fontSize = 10.5.sp),
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SpatialTonightEventRow(
    event: WhatsUpTonightEngine.TonightEvent,
    isTopRanked: Boolean,
    isFa: Boolean,
    onClick: () -> Unit
) {
    val palette = SpatialTheme.palette
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
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (isTopRanked) SpatialNeonRose.copy(alpha = if (palette.isDark) 0.24f else 0.18f)
                        else palette.glassFill
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = eventIcon,
                    contentDescription = null,
                    tint = if (isTopRanked) palette.accentInk else palette.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = if (isFa) event.titleFa else event.titleEn,
                    style = RedTypographyTokens.bodyPrimary.copy(
                        fontWeight = if (isTopRanked) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = palette.textPrimary
                )
                Text(
                    text = if (isFa) event.explanationFa else event.explanationEn,
                    style = RedTypographyTokens.caption,
                    color = palette.textSecondary,
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
                        color = palette.accentInk
                    )
                    Text(
                        text = "•",
                        style = RedTypographyTokens.caption,
                        color = palette.textTertiary
                    )
                    Text(
                        text = if (isFa) event.visibilityTextFa else event.visibilityTextEn,
                        style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                        color = palette.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Trailing badge slot: Compact Radial Dot Meter + Mesh Gradient Pill
        when (event.visibilityStatus) {
            WhatsUpTonightEngine.EventVisibilityStatus.OPTIMAL -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SpatialCompactRadialMeter(
                        progress = 0.94f,
                        centerValueText = if (isFa) TimeEngine.formatPersianNumbers("95") else "95",
                        diameter = 38.dp,
                        activeColor = SpatialMintGlow,
                        secondaryActiveColor = SpatialSkyBlue
                    )
                    SpatialMeshPill(
                        preset = SpatialMeshPreset.BLUE_TO_PURPLE,
                        isCapsule = true,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isFa) "عالی" else "Optimal",
                            style = RedTypographyTokens.badge.copy(fontSize = 10.5.sp),
                            color = Color.White
                        )
                    }
                }
            }
            WhatsUpTonightEngine.EventVisibilityStatus.GOOD -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SpatialCompactRadialMeter(
                        progress = 0.78f,
                        centerValueText = if (isFa) TimeEngine.formatPersianNumbers("78") else "78",
                        diameter = 38.dp,
                        activeColor = SpatialNeonRose,
                        secondaryActiveColor = SpatialWarmPeach
                    )
                    SpatialMeshPill(
                        preset = SpatialMeshPreset.ROSE_TO_ORANGE,
                        isCapsule = true,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isFa) "خوب" else "Good",
                            style = RedTypographyTokens.badge.copy(fontSize = 10.5.sp),
                            color = Color(0xFF23111C)
                        )
                    }
                }
            }
            WhatsUpTonightEngine.EventVisibilityStatus.MARGINAL -> {
                SpatialGlassCapsule(
                    isActive = true,
                    activeGlowColor = SpatialWarmPeach,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    elevation = 2.dp
                ) {
                    Text(
                        text = if (isFa) "متوسط" else "Marginal",
                        style = RedTypographyTokens.badge.copy(fontSize = 10.5.sp),
                        color = palette.warningInk
                    )
                }
            }
            WhatsUpTonightEngine.EventVisibilityStatus.NOT_VISIBLE -> {
                SpatialGlassCapsule(
                    isActive = false,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    elevation = 0.dp
                ) {
                    Text(
                        text = if (isFa) "غیرقابل رصد" else "Not Visible",
                        style = RedTypographyTokens.badge.copy(fontSize = 10.5.sp),
                        color = palette.textTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun SpatialSkySearchOverlay(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    results: List<Triple<CelestialObject, CoordinateEngine.Horizontal, ObservabilityEngine.ObservabilityResult>>,
    isFa: Boolean,
    uiState: MainUiState,
    viewModel: MainViewModel,
    moonData: MoonEngine.MoonData,
    catalogJd: Double,
    onDismiss: () -> Unit
) {
    val palette = SpatialTheme.palette
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val displayedPredictiveItems = remember(results) { results.take(6) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_astronomy_search_overlay")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .testTag("home_astronomy_search_scrim")
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            SpatialGlassCard(
                cornerRadius = RedCornerRadius.window,
                contentPadding = PaddingValues(vertical = RedSpacing.xs),
                elevation = 22.dp,
                elevated = true,
                glowAccent = SpatialNeonRose,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(RedSpacing.md)
                    .fillMaxWidth()
                    .testTag("home_astronomy_search_card")
            ) {
                OutlinedTextField(
                    textStyle = TextStyle(
                        fontFamily = LocalAppFontFamily.current,
                        color = palette.textPrimary
                    ),
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RedSpacing.sm)
                        .focusRequester(focusRequester)
                        .testTag("home_astronomy_search_input"),
                    placeholder = {
                        Text(
                            text = if (isFa) "نام سیاره، ستاره، سحابی..." else "Search planets, stars, nebulae...",
                            style = RedTypographyTokens.bodySecondary,
                            color = palette.textTertiary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = palette.accentInk,
                            modifier = Modifier.size(RedIconSize.sm)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = if (isFa) "پاک کردن" else "Clear",
                                        tint = palette.textSecondary,
                                        modifier = Modifier.size(RedIconSize.sm)
                                    )
                                }
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = if (isFa) "بستن جستجو" else "Close search",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(RedIconSize.md)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = squircleShape(RedCornerRadius.lg),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        errorBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = palette.accentInk
                    )
                )

                if (displayedPredictiveItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = RedSpacing.lg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFa) "هیچ جرم متناسبی پیدا نشد." else "No matching celestial object found.",
                            style = RedTypographyTokens.bodySecondary,
                            color = palette.textTertiary
                        )
                    }
                } else {
                    Column(modifier = Modifier.padding(top = RedSpacing.xs)) {
                        displayedPredictiveItems.forEachIndexed { idx, (obj, horiz, _) ->
                            val riseSetStr = remember(obj, moonData, uiState.userLocation, catalogJd, isFa) {
                                if (obj.id == "moon") {
                                    val tz = TimeEngine.resolveTimeZone(uiState.userLocation.timezoneId)
                                    val rStr = TimeEngine.formatTime24h(moonData.moonriseTimeMs ?: 0L, isFa, tz)
                                    val sStr = TimeEngine.formatTime24h(moonData.moonsetTimeMs ?: 0L, isFa, tz)
                                    if (isFa) "طلوع: $rStr | غروب: $sStr" else "Rise: $rStr | Set: $sStr"
                                } else {
                                    val rst = CoordinateEngine.calculateRiseSetTransit(
                                        raDeg = obj.raDeg,
                                        decDeg = obj.decDeg,
                                        latDeg = uiState.userLocation.latitude,
                                        lonDeg = uiState.userLocation.longitude,
                                        jd = catalogJd,
                                        isFa = isFa,
                                        timezoneId = uiState.userLocation.timezoneId,
                                        horizonAltDeg = CoordinateEngineLegacy.horizonAltDegFor(obj.id)
                                    )
                                    if (isFa) "طلوع: ${rst.riseTimeStr} | غروب: ${rst.setTimeStr}" else "Rise: ${rst.riseTimeStr} | Set: ${rst.setTimeStr}"
                                }
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
                                    SpatialCelestialObjectIcon(
                                        type = obj.type,
                                        tint = palette.accentInk,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        Text(
                                            text = if (isFa) obj.nameFa else obj.nameEn,
                                            style = RedTypographyTokens.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                                            color = palette.textPrimary
                                        )
                                        Text(
                                            text = riseSetStr,
                                            style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                                            color = palette.accentInk
                                        )
                                    }
                                }

                                val altInt = horiz.altitudeDeg.toInt()
                                val altRaw = "${if (altInt >= 0) "+" else ""}${altInt}°"
                                SpatialGlassCapsule(
                                    isActive = horiz.altitudeDeg > 0.0,
                                    activeGlowColor = SpatialMintGlow,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                                    elevation = 2.dp
                                ) {
                                    DotMatrixText(
                                        text = if (isFa) TimeEngine.formatPersianNumbers(altRaw) else altRaw,
                                        dotSize = 1.75.dp,
                                        activeColor = palette.dotMatrixInk,
                                        glowColor = if (horiz.altitudeDeg > 0.0) SpatialMintGlow else null
                                    )
                                }
                            }

                            if (idx < displayedPredictiveItems.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = RedSpacing.md)
                                        .height(0.75.dp)
                                        .background(palette.rimSideBorder.copy(alpha = 0.55f))
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
private fun SpatialCelestialObjectIcon(
    type: ObjectType?,
    modifier: Modifier = Modifier,
    tint: Color
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

private fun spatialSkyConditionLabel(sunAltitudeDeg: Double, isFa: Boolean): String = when {
    sunAltitudeDeg > 6.0 -> if (isFa) "روز" else "Daylight"
    sunAltitudeDeg > 0.0 -> if (isFa) "ساعت طلایی" else "Golden hour"
    sunAltitudeDeg > -6.0 -> if (isFa) "شفق مدنی" else "Civil twilight"
    sunAltitudeDeg > -12.0 -> if (isFa) "شفق دریایی" else "Nautical twilight"
    sunAltitudeDeg > -18.0 -> if (isFa) "شفق نجومی" else "Astronomical twilight"
    else -> if (isFa) "شب نجومی" else "Astronomical night"
}

/**
 * Exact replica of Default UI's [FullscreenSkyViewer] so opening the sky fullscreen in Spatial UI
 * enters edge-to-edge immersive landscape with the untouched [HeroSkyCanvas].
 */
@Composable
private fun SpatialFullscreenSkyViewer(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    val hostView = LocalView.current
    val activity = remember(hostView) { hostView.context.findActivitySpatial() }

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
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val dialogView = LocalView.current
        DisposableEffect(dialogView) {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            if (window != null) {
                window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK))
                window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                WindowCompat.setDecorFitsSystemWindows(window, false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.attributes = window.attributes.apply {
                        layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
                val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
            onDispose {
                if (activity?.isChangingConfigurations != true) {
                    window?.let { WindowInsetsControllerCompat(it, it.decorView).show(WindowInsetsCompat.Type.systemBars()) }
                }
            }
        }

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
                    imageVector = Icons.Outlined.Close,
                    contentDescription = if (isFa) "بستن نمای تمام‌صفحه" else "Close fullscreen sky",
                    tint = Color(0xFFF8FAFF),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun Context.findActivitySpatial(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
