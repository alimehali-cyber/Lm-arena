package com.alijafari.red.astronomy.ui.aether

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
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
import com.alijafari.red.astronomy.astro_engine.EclipseEngine
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.MoonEngine
import com.alijafari.red.astronomy.astro_engine.ObservabilityEngine
import com.alijafari.red.astronomy.astro_engine.SunEngine
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.astro_engine.WhatsUpTonightEngine
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
import com.alijafari.red.astronomy.ui.home.CatalogObjectsKey
import com.alijafari.red.astronomy.ui.home.EclipseSeriesKey
import com.alijafari.red.astronomy.ui.home.HomeAstroKeys
import com.alijafari.red.astronomy.ui.home.HomeAstroMemo
import com.alijafari.red.astronomy.ui.home.MoonKey
import com.alijafari.red.astronomy.ui.home.TonightEventsKey
import com.alijafari.red.astronomy.ui.home.rememberReadyTimeMs
import com.alijafari.red.astronomy.ui.theme.LocalAppFontFamily
import com.alijafari.red.astronomy.ui.theme.LocalBackdropLuminance
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.backdropInkFor
import com.alijafari.red.astronomy.ui.theme.solidSurface
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val AETHER_MOON_REFRESH_MS = 60_000L
private const val AETHER_CATALOG_REFRESH_MS = 60_000L
private const val AETHER_ECLIPSE_REFRESH_MS = 3_600_000L

private fun aetherBucketMs(timeMs: Long, isSimulation: Boolean, bucketMs: Long): Long =
    if (isSimulation || bucketMs <= 0L) timeMs else timeMs - java.lang.Math.floorMod(timeMs, bucketMs)

private enum class AetherTonightFilter {
    ALL, SOLAR_SYSTEM, DEEP_SKY
}

/**
 * Aether Telemetry UI — Spatial Telemetry Dashboard Home Screen:
 *
 * - Full-bleed edge-to-edge [HeroSkyCanvas] (`SkyCanvasPresentation.SCENE`) topped with the
 *   floating `100.dp` [AetherFloatingStatusCapsule] (`✦ ZIG • 89% • Bortle 3 ⚡`).
 * - Hero Frameless Jewel-Tone Sky Telemetry Card (`36.dp` crimson radial optical lens `#1C020D ->
 *   #5E052A -> #9E0B45`, 1.dp specular rim, rolling Rubik-Dots counter, `● Risen` / `○ Planned`
 *   legend, 5 LED dots, cubic-bezier `AetherOscilloscopeWaveformCanvas` with glowing white bottom
 *   troughs, rolling `31.sp` light secondary readout, and haptic `← ━ →` micro-stepper).
 * - Bento Telemetry Row: Frosted 60-Tick `AetherRadialDialCard` (`34.dp`) + stacked Pink & Amber
 *   `AetherGlowingMeshPillCard`s (`RoundedCornerShape(50)`).
 * - Frameless Sapphire & Crimson Optical Lenses for the interactive 2.5D Moon sphere and Eclipse plate.
 * - Frameless Frosted Spatial "What's Up Tonight" telemetry deck with spline visibility curves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AetherHomeScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val palette = AetherTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN

    val liveTimeMs by viewModel.skyLiveClock.timeMs.collectAsState()
    val isSimulation = uiState.timeMachineState.mode == TimeMachineMode.SIMULATION
    val activeTimeMs = if (isSimulation) uiState.timeMachineState.simulationTimeMs else liveTimeMs

    val moonKeyMs = rememberReadyTimeMs(
        targetMs = aetherBucketMs(activeTimeMs, isSimulation, AETHER_MOON_REFRESH_MS),
        deferUntilReady = !isSimulation,
        maxDeferredStepMs = 2 * AETHER_MOON_REFRESH_MS
    ) { HomeAstroKeys.moonKeys(it, uiState) }
    val catalogKeyMs = rememberReadyTimeMs(
        targetMs = aetherBucketMs(activeTimeMs, isSimulation, AETHER_CATALOG_REFRESH_MS),
        deferUntilReady = !isSimulation,
        maxDeferredStepMs = 2 * AETHER_CATALOG_REFRESH_MS
    ) { HomeAstroKeys.catalogKeys(it, uiState, isFa) }
    val eclipseKeyMs = rememberReadyTimeMs(
        targetMs = aetherBucketMs(activeTimeMs, isSimulation, AETHER_ECLIPSE_REFRESH_MS),
        deferUntilReady = !isSimulation,
        maxDeferredStepMs = 2 * AETHER_ECLIPSE_REFRESH_MS
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

    val scrubbedMoonMs = remember(moonKeyMs, uiState.moonDayOffset) {
        moonKeyMs + (uiState.moonDayOffset * 86_400_000f).toLong()
    }
    val cardMoonData = remember(scrubbedMoonMs, uiState.userLocation) {
        val jd = TimeEngine.getJulianDate(scrubbedMoonMs)
        HomeAstroMemo.get(MoonKey(jd, uiState.userLocation.latitude, uiState.userLocation.longitude))
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
        HomeAstroMemo.get(
            TonightEventsKey(
                jd = catalogJd,
                latitude = uiState.userLocation.latitude,
                longitude = uiState.userLocation.longitude,
                isFa = isFa,
                issTle = ISSEngine.cachedTLE
            )
        ).take(8)
    }

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

    val nextEclipse = remember(nextSolarEclipses, nextLunarEclipses) {
        listOfNotNull(nextSolarEclipses.firstOrNull(), nextLunarEclipses.firstOrNull())
            .minByOrNull { it.localPeakTimeMs }
    }

    var selectedEclipseForDetail by remember { mutableStateOf<EclipseEngine.EclipseResult?>(null) }
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var tonightFilter by rememberSaveable { mutableStateOf(AetherTonightFilter.ALL) }

    val filteredHighlights = remember(tonightHighlights, tonightFilter) {
        when (tonightFilter) {
            AetherTonightFilter.ALL -> tonightHighlights.take(6)
            AetherTonightFilter.SOLAR_SYSTEM -> tonightHighlights.filter {
                val t = it.targetObject?.type
                t == ObjectType.PLANET || t == ObjectType.MOON || t == ObjectType.SUN
            }.ifEmpty { tonightHighlights.take(4) }
            AetherTonightFilter.DEEP_SKY -> tonightHighlights.filter {
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
        val sceneHeight = (maxHeight * 0.49f).coerceIn(320.dp, 460.dp)
        val controlsBottom = 56.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("aether_home_scroll"),
            contentPadding = PaddingValues(bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. UNTOUCHED SKY SCENE + FLOATING TOP STATUS CAPSULE
            item(key = "aether_sky_hero") {
                AetherSkyHeroSection(
                    uiState = uiState,
                    viewModel = viewModel,
                    isFa = isFa,
                    moonIllumPercent = moonData.illuminationPercent.roundToInt(),
                    sceneHeight = sceneHeight,
                    controlsBottom = controlsBottom,
                    onOpenSearch = { isSearchOpen = true }
                )
            }

            // 2. HERO COMPONENT: FRAMELESS JEWEL-TONE CELESTIAL INTAKE / TELEMETRY CARD
            item(key = "aether_hero_jewel_card") {
                AetherFramelessSkyTelemetryCard(
                    isFa = isFa,
                    sortedObjectsWithObs = sortedObjectsWithObs,
                    bortleClass = uiState.bortleClass,
                    sunAltitudeDeg = sunHoriz.altitudeDeg,
                    modifier = Modifier.padding(horizontal = RedSpacing.lg)
                )
            }

            // 3. BENTO ROW: FROSTED 60-TICK RADIAL DIAL CARD + STACKED PINK & AMBER MESH PILLS
            item(key = "aether_bento_dial_and_pills") {
                AetherBentoDialAndPillsRow(
                    uiState = uiState,
                    isFa = isFa,
                    sunAltitudeDeg = sunHoriz.altitudeDeg,
                    moonIllumPercent = moonData.illuminationPercent,
                    sortedObjectsWithObs = sortedObjectsWithObs,
                    onOpenBortleSettings = { viewModel.setShowSettingsDialog(true) },
                    onOpenArSky = { onNavigateToTab(3) },
                    onOpenSatellites = { onNavigateToTab(1) },
                    modifier = Modifier.padding(horizontal = RedSpacing.lg)
                )
            }

            // 4. FRAMELESS LUNAR & ECLIPSE OPTICAL LENSES (2.5D Moon + Eclipse Plate)
            item(key = "aether_moon_eclipse_lenses") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RedSpacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AetherLunarOpticalCard(
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

                    AetherEclipseOpticalCard(
                        eclipse = nextEclipse,
                        isFa = isFa,
                        onClick = { viewModel.setShowEclipseWindow(true) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_eclipse_card")
                    )
                }
            }

            // 5. TONIGHT'S TELEMETRY HEADER + MINIMAL 1.DP PILL FILTERS
            item(key = "aether_tonight_header") {
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
                            Text(
                                text = if (isFa) "امشب در آسمان" else "What's Up Tonight",
                                style = RedTypographyTokens.sectionTitle.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = (-0.3).sp,
                                    shadow = Shadow(
                                        color = backdropInk.halo,
                                        offset = Offset.Zero,
                                        blurRadius = 7f
                                    )
                                ),
                                color = palette.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isFa) "رویدادهای شاخص نجومی برای مختصات رصدی شما"
                                else "Live meridian transits & naked-eye highlights",
                                style = RedTypographyTokens.caption.copy(
                                    fontSize = 12.sp,
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

                        val countDisplay = if (isFa) {
                            TimeEngine.formatPersianNumbers(filteredHighlights.size.toString().padStart(2, '0'))
                        } else {
                            filteredHighlights.size.toString().padStart(2, '0')
                        }
                        AetherRollingDotMatrixCounter(
                            valueText = countDisplay,
                            dotSize = 1.95.dp,
                            dotGap = 0.72.dp,
                            charGap = 2.4.dp,
                            color = palette.textPrimary,
                            glowColor = palette.crimsonGlow
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val filters = listOf(
                            AetherTonightFilter.ALL to (if (isFa) "همه اجرام" else "All Telemetry"),
                            AetherTonightFilter.SOLAR_SYSTEM to (if (isFa) "منظومه شمسی" else "Solar System"),
                            AetherTonightFilter.DEEP_SKY to (if (isFa) "اعماق آسمان" else "Deep Sky")
                        )
                        items(filters, key = { it.first.name }) { (filter, label) ->
                            val selected = tonightFilter == filter
                            AetherPillBadge(
                                text = label,
                                selected = selected,
                                tint = palette.textPrimary,
                                onClick = { tonightFilter = filter }
                            )
                        }
                    }
                }
            }

            // 6. FRAMELESS FROSTED SPATIAL TONIGHT CARD
            item(key = "aether_tonight_card") {
                AetherFrostedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RedSpacing.lg)
                        .testTag("home_highlights_card"),
                    cornerRadius = 36.dp,
                    contentPadding = PaddingValues(22.dp),
                    accentGlow = palette.crimsonGlow
                ) {
                    filteredHighlights.forEachIndexed { index, event ->
                        AetherTonightEventRow(
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
                                modifier = Modifier.padding(vertical = 14.dp),
                                thickness = 0.5.dp,
                                color = palette.mutedTelemetry.copy(alpha = 0.25f)
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
            AetherSkySearchOverlay(
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
        AetherFullscreenSkyViewer(
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
private fun AetherSkyHeroSection(
    uiState: MainUiState,
    viewModel: MainViewModel,
    isFa: Boolean,
    moonIllumPercent: Int,
    sceneHeight: Dp,
    controlsBottom: Dp,
    onOpenSearch: () -> Unit
) {
    val capsuleText = remember(isFa, moonIllumPercent, uiState.bortleClass) {
        if (isFa) {
            val m = TimeEngine.formatPersianNumbers(moonIllumPercent.toString())
            val b = TimeEngine.formatPersianNumbers(uiState.bortleClass.toString())
            "زیگ  •  ماه $m٪  •  بورتل $b"
        } else {
            "ZIG  •  $moonIllumPercent% Moon  •  Bortle ${uiState.bortleClass}"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(sceneHeight + controlsBottom)
            .testTag("home_sky_scene")
    ) {
        HeroSkyCanvas(
            uiState = uiState,
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .height(sceneHeight),
            presentation = SkyCanvasPresentation.SCENE,
            bottomTailHeight = controlsBottom
        )

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

        // FLOATING TOP STATUS CAPSULE + OPTICAL ICON LENSES
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = RedSpacing.lg, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AetherOpticalIconLens(
                icon = Icons.Outlined.BookmarkBorder,
                contentDescription = stringResource(R.string.favorites_and_history),
                onClick = { viewModel.setShowFavoritesDialog(true) },
                modifier = Modifier.testTag("top_favorites_button")
            )

            AetherFloatingStatusCapsule(
                text = capsuleText,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(horizontal = 8.dp)
                    .testTag("home_red_title")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AetherOpticalIconLens(
                    icon = Icons.Outlined.Search,
                    contentDescription = if (isFa) "جستجوی آسمان" else "Search the sky",
                    onClick = onOpenSearch,
                    modifier = Modifier.testTag("top_search_button")
                )
                AetherOpticalIconLens(
                    icon = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.settings),
                    onClick = { viewModel.setShowSettingsDialog(true) },
                    modifier = Modifier.testTag("top_settings_button")
                )
            }
        }

        // BOTTOM FLOATING RIM-LIT LOCATION & 360° SKY CAPSULES
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = RedSpacing.lg, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0x5408080C))
                    .background(Color.White.copy(alpha = 0.08f))
                    .specularRimBorder(cornerRadius = 100.dp)
                    .clickable { viewModel.setShowLocationSelector(true) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("home_location_selector_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isLiveGpsLocation) Icons.Default.MyLocation else Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = AetherLightningTint,
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

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0x5408080C))
                    .background(Color.White.copy(alpha = 0.08f))
                    .specularRimBorder(cornerRadius = 100.dp)
                    .clickable { viewModel.setShowSkyViewer(true) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("home_sky_expand_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FitScreen,
                    contentDescription = if (isFa) "نمایش تمام‌صفحه آسمان" else "Open fullscreen sky",
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
 * HERO COMPONENT: Frameless Jewel-Tone Celestial Telemetry Card (`RoundedCornerShape(36.dp)`).
 *
 * Faithful realization of the Aether Telemetry Hero Card:
 * - Deep crimson radial optical jewel (`#1C020D` center -> `#5E052A` mid -> `#9E0B45` edge)
 * - 2-digit Rolling Dot-Matrix primary readout + `● Risen` / `○ Planned` LED legend
 * - 5 LED status array + Animated Cubic-Bezier `AetherOscilloscopeWaveformCanvas` with
 *   glowing `BlurMaskFilter` neon-white bottom troughs
 * - Rolling `30.sp` `FontWeight.Light` (`-0.8.sp`) secondary metric + haptic `← ━ →` micro-stepper
 */
@Composable
private fun AetherFramelessSkyTelemetryCard(
    isFa: Boolean,
    sortedObjectsWithObs: List<Triple<CelestialObject, CoordinateEngine.Horizontal, ObservabilityEngine.ObservabilityResult>>,
    bortleClass: Int,
    sunAltitudeDeg: Double,
    modifier: Modifier = Modifier
) {
    // 5 interactive aperture/magnitude telemetry tiers (1..5)
    var apertureTier by rememberSaveable { mutableIntStateOf(4) }

    val baseLimitingMag = remember(bortleClass, sunAltitudeDeg) {
        val bortleLimit = (7.6 - bortleClass * 0.45).coerceIn(3.2, 7.2)
        if (sunAltitudeDeg > -6.0) 2.5 else bortleLimit
    }

    val tierMagLimit = remember(baseLimitingMag, apertureTier) {
        baseLimitingMag + (apertureTier - 3) * 0.85
    }

    val risenTargetsCount = remember(sortedObjectsWithObs, tierMagLimit) {
        sortedObjectsWithObs.count { (_, horiz, _) ->
            horiz.altitudeDeg > 8.0
        }.let { totalAbove ->
            val filtered = sortedObjectsWithObs.count { (obj, horiz, _) ->
                horiz.altitudeDeg > 8.0 && obj.magnitude <= tierMagLimit
            }
            filtered.coerceIn(1, totalAbove.coerceAtLeast(1)).coerceAtMost(99)
        }
    }

    val primaryDigits = remember(risenTargetsCount, isFa) {
        val padded = risenTargetsCount.toString().padStart(2, '0')
        if (isFa) TimeEngine.formatPersianNumbers(padded) else padded
    }

    val secondaryValue = remember(tierMagLimit, isFa) {
        val raw = String.format(Locale.US, "%.1f mag", tierMagLimit)
        if (isFa) {
            "${TimeEngine.formatPersianNumbers(String.format(Locale.US, "%.1f", tierMagLimit))} قدر"
        } else {
            raw
        }
    }

    AetherJewelCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("aether_hero_telemetry_card"),
        jewelTone = AetherJewelTone.CRIMSON,
        cornerRadius = 36.dp,
        contentPadding = PaddingValues(26.dp),
        elevation = 28.dp
    ) {
        // Card Title (17.sp, FontWeight.Medium, letterSpacing = -0.3.sp)
        Text(
            text = if (isFa) "تله‌متری رصد امشب" else "Sky Telemetry",
            style = RedTypographyTokens.sectionTitle.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.3).sp
            ),
            color = Color.White.copy(alpha = 0.95f)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Top Row: Rolling Dot-Matrix Readout + Risen/Planned Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(verticalAlignment = Alignment.Top) {
                AetherRollingDotMatrixCounter(
                    valueText = primaryDigits,
                    dotSize = 3.25.dp,
                    dotGap = 1.15.dp,
                    charGap = 4.5.dp,
                    color = AetherGlowWhite,
                    glowColor = AetherCrimsonCoreEdge,
                    showGhostGrid = false
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isFa) "جرم قابل رصد" else "targets tonight",
                    style = RedTypographyTokens.caption.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                AetherLegendDotItem(
                    label = if (isFa) "طلوع‌کرده" else "Risen",
                    filled = true
                )
                AetherLegendDotItem(
                    label = if (isFa) "در انتظار" else "Planned",
                    filled = false
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Middle Row: Cubic-Bezier Oscilloscope Waveform + 5 LED Status Dots
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            AetherLedDotArray(
                totalDots = 5,
                litCount = apertureTier,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            AetherOscilloscopeWaveformCanvas(
                activeWaves = apertureTier,
                progress = apertureTier / 5f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .align(Alignment.BottomCenter)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Bottom Row: Secondary Value (30.sp Light, -0.8.sp) + Micro Stepper (← ━ →)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            AetherRollingSecondaryMetric(
                valueText = secondaryValue,
                fontSizeSp = 30f
            )

            AetherMicroStepper(
                onDecrement = { if (apertureTier > 1) apertureTier-- },
                onIncrement = { if (apertureTier < 5) apertureTier++ },
                canDecrement = apertureTier > 1,
                canIncrement = apertureTier < 5,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

/**
 * BENTO TELEMETRY ROW:
 * Left: Square `34.dp` Frosted 60-Tick Radial Gauge (`AetherRadialDialCard`)
 * Right: Stacked `RoundedCornerShape(50)` Pink & Amber Glowing Mesh Pill Cards
 */
@Composable
private fun AetherBentoDialAndPillsRow(
    uiState: MainUiState,
    isFa: Boolean,
    sunAltitudeDeg: Double,
    moonIllumPercent: Double,
    sortedObjectsWithObs: List<Triple<CelestialObject, CoordinateEngine.Horizontal, ObservabilityEngine.ObservabilityResult>>,
    onOpenBortleSettings: () -> Unit,
    onOpenArSky: () -> Unit,
    onOpenSatellites: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skyScore = remember(uiState.bortleClass, sunAltitudeDeg, moonIllumPercent, sortedObjectsWithObs) {
        val darknessBonus = ((10 - uiState.bortleClass) * 7).coerceIn(7, 63)
        val sunBonus = when {
            sunAltitudeDeg <= -18.0 -> 28
            sunAltitudeDeg <= -6.0 -> 16
            else -> 4
        }
        val moonPenalty = (moonIllumPercent * 0.08).roundToInt()
        (darknessBonus + sunBonus - moonPenalty + 8).coerceIn(12, 99)
    }

    val scoreText = if (isFa) {
        TimeEngine.formatPersianNumbers(skyScore.toString())
    } else {
        skyScore.toString()
    }
    val statusWord = when {
        skyScore >= 75 -> if (isFa) "ایده‌آل" else "Optimal"
        skyScore >= 50 -> if (isFa) "مطلوب" else "Clear"
        else -> if (isFa) "روشن" else "Daylight"
    }

    val sunAbsStr = String.format(Locale.US, "%.1f", abs(sunAltitudeDeg))
    val sunDisplay = if (isFa) TimeEngine.formatPersianNumbers(sunAbsStr) else sunAbsStr
    val sunBadge = when {
        sunAltitudeDeg <= -18.0 -> if (isFa) "↓ شب" else "↓ Night"
        sunAltitudeDeg <= 0.0 -> if (isFa) "↓ شفق" else "↓ Dusk"
        else -> if (isFa) "↑ روز" else "↑ Day"
    }

    val bortleValue = if (isFa) {
        TimeEngine.formatPersianNumbers(uiState.bortleClass.toString().padStart(2, '0'))
    } else {
        uiState.bortleClass.toString().padStart(2, '0')
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AetherRadialDialCard(
            scoreText = scoreText,
            status = statusWord,
            highlightFraction = skyScore / 100f,
            title = if (isFa) "شاخص آسمان" else "Sky Index",
            onClick = onOpenBortleSettings,
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AetherGlowingMeshPillCard(
                title = if (isFa) "افق خورشیدی (AR)" else "Solar Horizon (AR)",
                value = sunDisplay,
                unit = "deg",
                badge = sunBadge,
                preset = AetherMeshPillPreset.PINK_MESH,
                onClick = onOpenArSky
            )

            AetherGlowingMeshPillCard(
                title = if (isFa) "بورتل و مدارها" else "Bortle & Orbits",
                value = bortleValue,
                unit = if (isFa) "کلاس" else "cls",
                badge = if (isFa) "مدار ←" else "SGP4 →",
                preset = AetherMeshPillPreset.AMBER_MESH,
                onClick = onOpenSatellites
            )
        }
    }
}

@Composable
private fun AetherLunarOpticalCard(
    uiState: MainUiState,
    viewModel: MainViewModel,
    isFa: Boolean,
    moonData: MoonEngine.MoonData,
    sunHoriz: CoordinateEngine.Horizontal,
    onScrubDays: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val illumInt = moonData.illuminationPercent.roundToInt()
    val illumText = if (isFa) {
        TimeEngine.formatPersianNumbers(illumInt.toString().padStart(2, '0'))
    } else {
        illumInt.toString().padStart(2, '0')
    }
    val phaseName = if (isFa) moonData.phaseNameFa else moonData.phaseNameEn

    AetherJewelCard(
        modifier = modifier,
        jewelTone = AetherJewelTone.SAPPHIRE,
        cornerRadius = 36.dp,
        contentPadding = PaddingValues(18.dp),
        elevation = 20.dp,
        onClick = { viewModel.setShowMoonWindow(true) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isFa) "ماه" else "Lunar Lens",
                style = RedTypographyTokens.caption.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White.copy(alpha = 0.85f)
            )
            AetherPillBadge(text = "%")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .size(92.dp)
                .align(Alignment.CenterHorizontally)
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

        Spacer(modifier = Modifier.height(12.dp))

        AetherRollingDotMatrixCounter(
            valueText = illumText,
            dotSize = 2.15.dp,
            dotGap = 0.82.dp,
            charGap = 2.8.dp,
            color = Color.White,
            glowColor = AetherSapphireCoreEdge
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = phaseName,
            style = RedTypographyTokens.caption.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Light
            ),
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(10.dp))

        AetherMicroStepper(
            onDecrement = { onScrubDays(-1f) },
            onIncrement = { onScrubDays(1f) },
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
private fun AetherEclipseOpticalCard(
    eclipse: EclipseEngine.EclipseResult?,
    isFa: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eclipseType = eclipse?.localType ?: EclipseEngine.EclipseType.TOTAL_SOLAR
    val daysUntil = eclipse?.daysRemaining?.coerceAtLeast(0) ?: 0
    val daysText = remember(daysUntil, isFa) {
        val raw = daysUntil.coerceAtMost(999).toString().padStart(2, '0')
        if (isFa) TimeEngine.formatPersianNumbers(raw) else raw
    }
    val titleText = remember(eclipse, isFa) {
        if (eclipse == null) {
            if (isFa) "گرفت بعدی" else "Next Eclipse"
        } else {
            if (isFa) eclipse.localNameFa else eclipse.localNameEn
        }
    }
    val dateSubtitle = remember(eclipse, isFa) {
        if (eclipse == null) {
            if (isFa) "در حال محاسبه" else "Calculating"
        } else {
            if (isFa) eclipse.formattedDateFa else eclipse.formattedDateEn
        }
    }

    AetherJewelCard(
        modifier = modifier,
        jewelTone = AetherJewelTone.EMERALD,
        cornerRadius = 36.dp,
        contentPadding = PaddingValues(18.dp),
        elevation = 20.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isFa) "گرفت" else "Eclipse",
                style = RedTypographyTokens.caption.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White.copy(alpha = 0.85f)
            )
            AetherPillBadge(text = if (isFa) "روز" else "DAYS")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .size(92.dp)
                .align(Alignment.CenterHorizontally)
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

        Spacer(modifier = Modifier.height(12.dp))

        AetherRollingDotMatrixCounter(
            valueText = daysText,
            dotSize = 2.15.dp,
            dotGap = 0.82.dp,
            charGap = 2.8.dp,
            color = Color.White,
            glowColor = AetherEmeraldCoreEdge
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = titleText,
            style = RedTypographyTokens.caption.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Light
            ),
            color = Color.White.copy(alpha = 0.78f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = dateSubtitle,
            style = RedTypographyTokens.caption.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Light
            ),
            color = Color.White.copy(alpha = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AetherTonightEventRow(
    event: WhatsUpTonightEngine.TonightEvent,
    isFa: Boolean,
    onClick: () -> Unit
) {
    val palette = AetherTheme.palette
    val obj = event.targetObject
    val scoreFraction = when (event.visibilityStatus) {
        WhatsUpTonightEngine.EventVisibilityStatus.OPTIMAL -> 0.92f
        WhatsUpTonightEngine.EventVisibilityStatus.GOOD -> 0.76f
        WhatsUpTonightEngine.EventVisibilityStatus.MARGINAL -> 0.52f
        WhatsUpTonightEngine.EventVisibilityStatus.NOT_VISIBLE -> 0.24f
    }
    val readout = remember(obj, scoreFraction, isFa) {
        val m = obj?.magnitude
        if (m != null && m < 25.0) {
            val raw = String.format(Locale.US, "%.1f", m)
            if (isFa) TimeEngine.formatPersianNumbers(raw) else raw
        } else {
            val pct = (scoreFraction * 99f).roundToInt().toString()
            if (isFa) TimeEngine.formatPersianNumbers(pct) else pct
        }
    }

    val splineSample = remember(scoreFraction) {
        listOf(0.18f, scoreFraction * 0.65f, scoreFraction, scoreFraction * 0.78f, 0.28f)
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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(
                    if (scoreFraction >= 0.7f) palette.glowWhite else palette.mutedTelemetry
                )
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isFa) event.titleFa else event.titleEn,
                style = RedTypographyTokens.bodyPrimary.copy(
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.2).sp
                ),
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isFa) event.explanationFa else event.explanationEn,
                style = RedTypographyTokens.caption.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Light
                ),
                color = palette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AetherDotMatrixText(
                text = readout,
                dotSize = 1.65.dp,
                dotGap = 0.62.dp,
                charGap = 2.0.dp,
                color = palette.textPrimary,
                glowColor = palette.crimsonGlow
            )
            AetherSplineCurveCanvas(
                points = splineSample,
                activeFraction = 0.52f,
                curveColor = palette.mutedTelemetry,
                glowColor = palette.crimsonGlow,
                modifier = Modifier
                    .width(52.dp)
                    .height(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AetherSkySearchOverlay(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    viewModel: MainViewModel,
    isFa: Boolean,
    results: List<Triple<CelestialObject, CoordinateEngine.Horizontal, ObservabilityEngine.ObservabilityResult>>,
    onClose: () -> Unit
) {
    val palette = AetherTheme.palette
    val focusRequester = remember { FocusRequester() }
    val appFont = LocalAppFontFamily.current

    BackHandler(onBack = onClose)
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvasVoid.copy(alpha = 0.92f))
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = RedSpacing.lg, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AetherFrostedCard(
                cornerRadius = 36.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                accentGlow = palette.crimsonGlow
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = palette.textPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onQueryChange,
                        placeholder = {
                            Text(
                                text = if (isFa) "جستجوی ستاره، سیاره، سحابی..." else "Search stars, planets, nebulae...",
                                style = RedTypographyTokens.bodySecondary.copy(fontWeight = FontWeight.Light),
                                color = palette.mutedTelemetry
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
                            cursorColor = palette.crimsonGlow
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .testTag("sky_search_input")
                    )
                    AetherOpticalIconLens(
                        icon = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        onClick = onClose,
                        size = 34.dp,
                        iconSize = 16.dp
                    )
                }
            }

            if (results.isNotEmpty()) {
                AetherFrostedCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 36.dp,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                AetherDotMatrixText(
                                    text = if (isFa) TimeEngine.formatPersianNumbers(altStr) else altStr,
                                    dotSize = 1.75.dp,
                                    dotGap = 0.65.dp,
                                    charGap = 2.0.dp,
                                    color = if (horiz.altitudeDeg > 0) palette.textPrimary else palette.mutedTelemetry,
                                    glowColor = if (horiz.altitudeDeg > 0) palette.crimsonGlow else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findAetherActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findAetherActivity()
    else -> null
}

@Composable
private fun AetherFullscreenSkyViewer(
    uiState: MainUiState,
    viewModel: MainViewModel,
    isFa: Boolean,
    onDismiss: () -> Unit
) {
    val hostView = LocalView.current
    val activity = remember(hostView) { hostView.context.findAetherActivity() }

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
        AetherFullscreenWindowSideEffect(dialogWindow, dialogView)

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
private fun AetherFullscreenWindowSideEffect(
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
