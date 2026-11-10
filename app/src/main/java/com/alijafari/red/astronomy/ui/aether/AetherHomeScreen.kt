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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val AETHER_MOON_REFRESH_MS = 60_000L
private const val AETHER_CATALOG_REFRESH_MS = 60_000L
private const val AETHER_ECLIPSE_REFRESH_MS = 3_600_000L

private fun aetherBucketMs(timeMs: Long, isSimulation: Boolean, bucketMs: Long): Long =
    if (isSimulation || bucketMs <= 0L) timeMs else timeMs - Math.floorMod(timeMs, bucketMs)

private fun String.aetherFormatDigits(isFa: Boolean): String =
    if (isFa) TimeEngine.formatPersianNumbers(this) else this

private enum class AetherTonightFilter {
    ALL, PLANETS, DEEP_SKY
}

/**
 * Aether Telemetry UI Home Screen:
 * - Preserves the full-bleed unblurred [HeroSkyCanvas] (`SkyCanvasPresentation.SCENE`) at the top,
 *   crowned by the Floating Status Capsule (`✦ ZIG • Bortle X • HH:MM ⚡`) and frameless rim-lit discs.
 * - Features the signature `36.dp` Frameless Crimson Jewel Sky Telemetry Card with rolling Dot-Matrix
 *   readout, `● Optimal` / `○ Rising` legend, 5-LED status array, cubic-bezier Oscilloscope Canvas
 *   with breathing neon trough glow, and `← ━ →` haptic time-machine micro-stepper.
 * - Spatial Bento Row pairing the `34.dp` 60-Tick Radial Observability Dial with two stacked
 *   Glowing Mesh Pill Cards (`PINK_ROSE` Solar Altitude & `AMBER_COPPER` Lunar Distance).
 * - Twin Frameless Sapphire & Emerald Jewel Lens Cards (`home_moon_card` & `home_eclipse_card`).
 * - Haze-blurred Frosted Spatial Tonight's Telemetry Stream (`home_highlights_card`).
 */
@Composable
fun AetherHomeScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onNavigateToTab: (Int) -> Unit = { viewModel.selectTab(it) }
) {
    val palette = AetherTheme.palette
    val appFont = LocalAppFontFamily.current
    val isFa = uiState.language == AppLanguage.PERSIAN
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

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

    val sunAltDeg = sunHoriz.altitudeDeg
    val skyQualityScore = remember(sunAltDeg, uiState.bortleClass, moonData.illuminationPercent, moonData.altitudeDeg) {
        val darknessPts = when {
            sunAltDeg <= -18.0 -> 58.0
            sunAltDeg <= -12.0 -> 46.0
            sunAltDeg <= -6.0 -> 30.0
            sunAltDeg <= 0.0 -> 16.0
            else -> 6.0
        }
        val bortlePts = ((9 - uiState.bortleClass.coerceIn(1, 9)) / 8.0) * 28.0
        val moonPenalty = if (moonData.altitudeDeg > 0.0) (moonData.illuminationPercent / 100.0) * 12.0 else 0.0
        (darknessPts + bortlePts + 14.0 - moonPenalty).roundToInt().coerceIn(8, 99)
    }

    val skyQualityLabel = when {
        sunAltDeg > 0.0 -> if (isFa) "روز روشن" else "Daylight"
        skyQualityScore >= 78 -> if (isFa) "بهینه" else "Optimal"
        skyQualityScore >= 52 -> if (isFa) "مطلوب" else "Good"
        else -> if (isFa) "گرگ‌ومیش" else "Twilight"
    }

    val optimalHighlightsCount = remember(tonightHighlights) {
        tonightHighlights.count {
            it.visibilityStatus == WhatsUpTonightEngine.EventVisibilityStatus.OPTIMAL ||
                it.visibilityStatus == WhatsUpTonightEngine.EventVisibilityStatus.GOOD
        }
    }
    val marginalHighlightsCount = (tonightHighlights.size - optimalHighlightsCount).coerceAtLeast(0)

    var selectedEclipseForDetail by remember { mutableStateOf<EclipseEngine.EclipseResult?>(null) }
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var tonightFilter by rememberSaveable { mutableStateOf(AetherTonightFilter.ALL) }

    val filteredHighlights = remember(tonightHighlights, tonightFilter) {
        when (tonightFilter) {
            AetherTonightFilter.ALL -> tonightHighlights.take(6)
            AetherTonightFilter.PLANETS -> tonightHighlights.filter {
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
            sortedObjectsWithObs.take(16)
        } else {
            val q = trimmedQuery.lowercase(Locale.ROOT)
            sortedObjectsWithObs.filter { (obj, _, _) ->
                obj.nameEn.lowercase(Locale.ROOT).contains(q) ||
                    obj.nameFa.lowercase(Locale.ROOT).contains(q) ||
                    obj.constellationEn.lowercase(Locale.ROOT).contains(q) ||
                    obj.constellationFa.lowercase(Locale.ROOT).contains(q) ||
                    obj.category.lowercase(Locale.ROOT).contains(q) ||
                    obj.id.lowercase(Locale.ROOT).contains(q)
            }.take(20)
        }
    }

    val liveClockLabel = remember(activeTimeMs, uiState.userLocation.timezoneId, isFa) {
        val tz = TimeEngine.resolveTimeZone(uiState.userLocation.timezoneId)
        val fmt = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = tz }
        fmt.format(Date(activeTimeMs)).aetherFormatDigits(isFa)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sceneHeight = (maxHeight * 0.50f).coerceIn(330.dp, 470.dp)
        val controlsBottom = 56.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_scroll_container"),
            contentPadding = PaddingValues(bottom = navBottomPadding + 108.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. UNTOUCHED FULL-BLEED SKY CANVAS + FLOATING AETHER STATUS CAPSULE & RIM DISCS
            item(key = "aether_sky_hero") {
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
                            .height(sceneHeight * 0.28f)
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Black.copy(alpha = 0.34f),
                                    0.65f to Color.Black.copy(alpha = 0.10f),
                                    1.0f to Color.Transparent
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = (statusTopPadding + 10.dp).coerceAtLeast(18.dp),
                                start = 16.dp,
                                end = 16.dp
                            )
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val capsuleStatus = if (isFa) {
                            "ZIG • بورتل ${uiState.bortleClass.toString().aetherFormatDigits(true)} • $liveClockLabel"
                        } else {
                            "ZIG • Bortle ${uiState.bortleClass} • $liveClockLabel"
                        }

                        AetherFloatingStatusCapsule(
                            statusText = capsuleStatus,
                            onClick = { viewModel.returnToLiveTime() },
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .testTag("home_red_title")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AetherMicroIconDisc(
                                icon = Icons.Outlined.BookmarkBorder,
                                contentDescription = stringResource(R.string.favorites_and_history),
                                onClick = { viewModel.setShowFavoritesDialog(true) },
                                modifier = Modifier.testTag("top_favorites_button")
                            )
                            AetherMicroIconDisc(
                                icon = Icons.Outlined.Search,
                                contentDescription = if (isFa) "جستجو" else "Search",
                                onClick = { isSearchOpen = true },
                                modifier = Modifier.testTag("top_search_button")
                            )
                            AetherMicroIconDisc(
                                icon = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.settings),
                                onClick = { viewModel.setShowSettingsDialog(true) },
                                modifier = Modifier.testTag("top_settings_button")
                            )
                        }
                    }

                    val cityLabel = if (uiState.isLiveGpsLocation) {
                        if (isFa) "موقعیت زنده GPS" else "Live GPS"
                    } else if (isFa) {
                        uiState.userLocation.cityNameFa
                    } else {
                        uiState.userLocation.cityNameEn
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AetherRimCapsule(
                            onClick = { viewModel.setShowLocationSelector(true) },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("home_location_selector_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = palette.primaryAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = cityLabel,
                                style = RedTypographyTokens.bodySecondary.copy(
                                    fontFamily = appFont,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(palette.statusOptimal)
                            )
                        }

                        AetherMicroIconDisc(
                            icon = Icons.Outlined.FitScreen,
                            contentDescription = if (isFa) "نمای تمام‌صفحه" else "Fullscreen Sky",
                            onClick = { viewModel.setShowSkyViewer(true) },
                            modifier = Modifier.testTag("home_sky_expand_button")
                        )
                    }
                }
            }

            // 2. HERO FRAMELESS JEWEL-TONE SKY TELEMETRY CARD (36.dp CRIMSON LENS)
            item(key = "aether_hero_jewel_telemetry") {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AetherJewelTelemetryCard(
                        preset = AetherJewelPreset.CRIMSON,
                        cornerRadius = AetherCornerGeometry.heroCard,
                        contentPadding = PaddingValues(24.dp),
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = if (isFa) "تله‌متری آسمان امشب" else "Sky Telemetry",
                                    style = RedTypographyTokens.sectionTitle.copy(
                                        fontFamily = appFont,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = if (isFa) 0.sp else (-0.3).sp
                                    ),
                                    color = Color.White.copy(alpha = 0.90f)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                val heroReadout = String.format(Locale.US, "%02d", tonightHighlights.size.coerceIn(0, 99))
                                    .aetherFormatDigits(isFa)
                                AetherRollingDotMatrixCounter(
                                    text = heroReadout,
                                    dotSize = 4.6.dp,
                                    dotGap = 1.45.dp,
                                    charGap = 4.8.dp,
                                    activeColor = Color.White,
                                    glowColor = AetherCrimsonCoreEdge
                                )
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                AetherLegendDotItem(
                                    label = if (isFa) {
                                        "بهینه (${optimalHighlightsCount.toString().aetherFormatDigits(true)})"
                                    } else {
                                        "Optimal ($optimalHighlightsCount)"
                                    },
                                    filled = true
                                )
                                AetherLegendDotItem(
                                    label = if (isFa) {
                                        "مرزی (${marginalHighlightsCount.toString().aetherFormatDigits(true)})"
                                    } else {
                                        "Rising ($marginalHighlightsCount)"
                                    },
                                    filled = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        val activeLeds = ((skyQualityScore / 20f).roundToInt()).coerceIn(1, 5)
                        AetherLedStatusArray(
                            litCount = activeLeds,
                            totalDots = 5
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        AetherOscilloscopeCanvas(
                            activeWaves = activeLeds,
                            maxWaves = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        val bottomMetricText = if (isSimulation) {
                            val deltaHrs = (activeTimeMs - liveTimeMs) / 3_600_000.0
                            val sign = if (deltaHrs >= 0.0) "+" else "−"
                            val hrsStr = String.format(Locale.US, "%.1f", abs(deltaHrs)).aetherFormatDigits(isFa)
                            if (isFa) "$sign$hrsStr ساعت" else "$sign$hrsStr hr"
                        } else {
                            val sunStr = String.format(Locale.US, "%.1f°", sunAltDeg).aetherFormatDigits(isFa)
                            if (isFa) "خورشید $sunStr" else "$sunStr Sun"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            AetherRollingMetricText(
                                text = bottomMetricText,
                                isPersian = isFa,
                                fontSize = 30.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            AetherMicroStepper(
                                onDecrement = {
                                    viewModel.setSimulatedTime(activeTimeMs - 3_600_000L)
                                },
                                onIncrement = {
                                    viewModel.setSimulatedTime(activeTimeMs + 3_600_000L)
                                },
                                onReset = {
                                    viewModel.returnToLiveTime()
                                }
                            )
                        }
                    }
                }
            }

            // 3. SPATIAL BENTO ROW: 60-TICK RADIAL DIAL + STACKED GLOWING MESH PILLS
            item(key = "aether_bento_row") {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AetherRadialDialCard(
                            scoreText = skyQualityScore.toString().aetherFormatDigits(isFa),
                            statusLabel = skyQualityLabel,
                            captionTop = if (isFa) "شاخص رصد" else "Sky Index",
                            highlightFraction = skyQualityScore / 100f,
                            onClick = { onNavigateToTab(3) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            val sunValStr = String.format(Locale.US, "%.1f", sunAltDeg).aetherFormatDigits(isFa)
                            val bortleBadge = if (isFa) {
                                "B${uiState.bortleClass.toString().aetherFormatDigits(true)}"
                            } else {
                                "Bortle ${uiState.bortleClass}"
                            }

                            AetherGlowingMeshPillCard(
                                title = if (isFa) "ارتفاع خورشید" else "Solar Alt",
                                value = sunValStr,
                                unit = "deg",
                                badge = bortleBadge,
                                preset = AetherMeshPillPreset.PINK_ROSE,
                                onClick = { viewModel.setShowLocationSelector(true) }
                            )

                            val distK = (moonData.distanceKm / 1000.0).roundToInt().toString().aetherFormatDigits(isFa)
                            AetherGlowingMeshPillCard(
                                title = if (isFa) "فاصله ماه" else "Lunar Range",
                                value = distK,
                                unit = if (isFa) "هزار km" else "k km",
                                badge = if (isFa) "مدار ↑" else "SGP4 ↑",
                                preset = AetherMeshPillPreset.AMBER_COPPER,
                                onClick = { onNavigateToTab(1) }
                            )
                        }
                    }
                }
            }

            // 4. TWIN FRAMELESS JEWEL LENS CARDS: LUNAR PHASE & NEXT ECLIPSE
            item(key = "aether_moon_eclipse_pair") {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AetherMoonLensCard(
                            moonData = cardMoonData,
                            sunHoriz = cardSunHoriz,
                            latitude = uiState.userLocation.latitude,
                            moonDayOffset = uiState.moonDayOffset,
                            isFa = isFa,
                            onNudgeOffsetDays = { viewModel.nudgeMoonDayOffset(it) },
                            onResetOffsetDays = { viewModel.setMoonDayOffset(0f) },
                            onOpenMoonWindow = { viewModel.setShowMoonWindow(true) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )

                        AetherEclipseLensCard(
                            eclipse = nextEclipse,
                            isFa = isFa,
                            onOpenEclipseWindow = { viewModel.setShowEclipseWindow(true) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }

            // 5. TONIGHT'S TELEMETRY STREAM (FRAMELESS HAZE FROSTED SPATIAL CARD)
            item(key = "aether_tonight_telemetry") {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AetherTonightTelemetryCard(
                        events = filteredHighlights,
                        activeFilter = tonightFilter,
                        onSelectFilter = { tonightFilter = it },
                        isFa = isFa,
                        onEventClick = { ev ->
                            when {
                                ev.id.startsWith("solar_") -> {
                                    selectedEclipseForDetail = nextSolarEclipses.firstOrNull()
                                }
                                ev.id.startsWith("lunar_") -> {
                                    selectedEclipseForDetail = nextLunarEclipses.firstOrNull()
                                }
                                else -> ev.targetObject?.let { obj ->
                                    viewModel.openObjectDetail(obj)
                                }
                            }
                        },
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("home_highlights_card")
                    )
                }
            }
        }

        // SEARCH OVERLAY MODAL
        AnimatedVisibility(
            visible = isSearchOpen,
            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { -it / 5 },
            exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { -it / 5 }
        ) {
            AetherSearchOverlay(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                results = filteredResults,
                isFa = isFa,
                onSelectObject = { obj ->
                    searchQuery = ""
                    isSearchOpen = false
                    viewModel.openObjectDetail(obj)
                },
                onDismiss = {
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
private fun AetherMoonLensCard(
    moonData: MoonEngine.MoonData,
    sunHoriz: CoordinateEngine.Horizontal,
    latitude: Double,
    moonDayOffset: Float,
    isFa: Boolean,
    onNudgeOffsetDays: (Float) -> Unit,
    onResetOffsetDays: () -> Unit,
    onOpenMoonWindow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appFont = LocalAppFontFamily.current
    val density = LocalDensity.current
    val currentOnNudge by rememberUpdatedState(onNudgeOffsetDays)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val illumPct = moonData.illuminationPercent.roundToInt().coerceIn(0, 100)
    val phaseLabel = if (isFa) moonData.phaseNameFa else moonData.phaseNameEn

    AetherJewelTelemetryCard(
        preset = AetherJewelPreset.SAPPHIRE,
        cornerRadius = AetherCornerGeometry.heroCard,
        contentPadding = PaddingValues(18.dp),
        elevation = 20.dp,
        onClick = onOpenMoonWindow,
        modifier = modifier.testTag("home_moon_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isFa) "ماه" else "Moon",
                style = RedTypographyTokens.sectionTitle.copy(
                    fontFamily = appFont,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = if (isFa) 0.sp else (-0.3).sp
                ),
                color = Color.White.copy(alpha = 0.90f),
                maxLines = 1
            )
            AetherRollingDotMatrixCounter(
                text = "$illumPct%".aetherFormatDigits(isFa),
                dotSize = 2.0.dp,
                dotGap = 0.7.dp,
                charGap = 1.9.dp,
                activeColor = Color.White,
                glowColor = Color(0xFF4CA8E0)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .pointerInput(isRtl) {
                    val pxPerDay = with(density) { 14.dp.toPx() }.coerceAtLeast(1f)
                    val dirSign = if (isRtl) -1f else 1f
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        currentOnNudge((dragAmount / pxPerDay) * dirSign)
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .testTag("home_moon_card_render"),
                contentAlignment = Alignment.Center
            ) {
                MoonPhaseRender(
                    moonData = moonData,
                    sunHoriz = sunHoriz,
                    latitude = latitude,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = phaseLabel,
            style = RedTypographyTokens.bodySecondary.copy(
                fontFamily = appFont,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val offsetLabel = if (abs(moonDayOffset) >= 0.08f) {
                val sign = if (moonDayOffset > 0f) "+" else "−"
                val d = String.format(Locale.US, "%.1f", abs(moonDayOffset)).aetherFormatDigits(isFa)
                if (isFa) "$sign$d روز" else "${sign}${d}d"
            } else {
                if (isFa) "زنده" else "Live"
            }
            Text(
                text = offsetLabel,
                style = RedTypographyTokens.caption.copy(
                    fontFamily = appFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light
                ),
                color = Color.White.copy(alpha = 0.65f)
            )
            AetherMicroStepper(
                onDecrement = { onNudgeOffsetDays(-1f) },
                onIncrement = { onNudgeOffsetDays(1f) },
                onReset = onResetOffsetDays
            )
        }
    }
}

@Composable
private fun AetherEclipseLensCard(
    eclipse: EclipseEngine.EclipseResult?,
    isFa: Boolean,
    onOpenEclipseWindow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appFont = LocalAppFontFamily.current
    val eclipseType = eclipse?.localType ?: EclipseEngine.EclipseType.TOTAL_SOLAR
    val daysAway = eclipse?.daysRemaining?.coerceAtLeast(0) ?: 0
    val titleText = if (eclipse == null) {
        if (isFa) "گرفت بعدی" else "Next Eclipse"
    } else if (isFa) {
        eclipse.localNameFa
    } else {
        eclipse.localNameEn
    }
    val dateSubtitle = if (eclipse == null) {
        if (isFa) "در حال محاسبه" else "Calculating..."
    } else if (isFa) {
        eclipse.formattedDateFa
    } else {
        eclipse.formattedDateEn
    }

    AetherJewelTelemetryCard(
        preset = AetherJewelPreset.EMERALD,
        cornerRadius = AetherCornerGeometry.heroCard,
        contentPadding = PaddingValues(18.dp),
        elevation = 20.dp,
        onClick = onOpenEclipseWindow,
        modifier = modifier.testTag("home_eclipse_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isFa) "گرفت‌ها" else "Eclipses",
                style = RedTypographyTokens.sectionTitle.copy(
                    fontFamily = appFont,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = if (isFa) 0.sp else (-0.3).sp
                ),
                color = Color.White.copy(alpha = 0.90f),
                maxLines = 1
            )
            AetherRollingDotMatrixCounter(
                text = "${daysAway}D".aetherFormatDigits(isFa),
                dotSize = 2.0.dp,
                dotGap = 0.7.dp,
                charGap = 1.9.dp,
                activeColor = Color.White,
                glowColor = Color(0xFF34B894)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
        ) {
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

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = titleText,
            style = RedTypographyTokens.bodySecondary.copy(
                fontFamily = appFont,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateSubtitle,
                style = RedTypographyTokens.caption.copy(
                    fontFamily = appFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light
                ),
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            AetherMinimalPillBadge(
                text = if (isFa) "کامل ←" else "Open →"
            )
        }
    }
}

@Composable
private fun AetherTonightTelemetryCard(
    events: List<WhatsUpTonightEngine.TonightEvent>,
    activeFilter: AetherTonightFilter,
    onSelectFilter: (AetherTonightFilter) -> Unit,
    isFa: Boolean,
    onEventClick: (WhatsUpTonightEngine.TonightEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AetherTheme.palette
    val appFont = LocalAppFontFamily.current

    AetherFrostedSpatialCard(
        cornerRadius = AetherCornerGeometry.heroCard,
        contentPadding = PaddingValues(20.dp),
        ambientTint = palette.primaryAccent,
        modifier = modifier
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
                        fontFamily = appFont,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = if (isFa) 0.sp else (-0.3).sp
                    ),
                    color = palette.textPrimary
                )
                Text(
                    text = if (isFa) "جریان تله‌متری اجرام قابل رصد" else "Live observable object telemetry",
                    style = RedTypographyTokens.caption.copy(
                        fontFamily = appFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light
                    ),
                    color = palette.textMicro
                )
            }
            AetherRollingDotMatrixCounter(
                text = String.format(Locale.US, "%02d", events.size.coerceIn(0, 99)).aetherFormatDigits(isFa),
                dotSize = 2.4.dp,
                dotGap = 0.85.dp,
                charGap = 2.3.dp,
                activeColor = palette.glowWhite,
                glowColor = palette.primaryAccent
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                AetherTonightFilter.ALL to (if (isFa) "همه" else "All"),
                AetherTonightFilter.PLANETS to (if (isFa) "سیارات و ماه" else "Planets"),
                AetherTonightFilter.DEEP_SKY to (if (isFa) "اعماق آسمان" else "Deep Sky")
            )
            filters.forEach { (filter, label) ->
                val active = activeFilter == filter
                AetherRimCapsule(
                    isActive = active,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    onClick = { onSelectFilter(filter) }
                ) {
                    Text(
                        text = label,
                        style = RedTypographyTokens.caption.copy(
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Medium else FontWeight.Light
                        ),
                        color = if (active) palette.textPrimary else palette.textMicro
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (events.isEmpty()) {
            Text(
                text = if (isFa) "رویدادی برای نمایش وجود ندارد." else "No events visible right now.",
                style = RedTypographyTokens.bodySecondary.copy(fontFamily = appFont),
                color = palette.textMicro,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            events.forEachIndexed { idx, ev ->
                AetherTelemetryObjectRow(
                    event = ev,
                    isFa = isFa,
                    onClick = { onEventClick(ev) }
                )
                if (idx < events.lastIndex) {
                    HorizontalDivider(
                        color = palette.mutedTelemetry.copy(alpha = 0.16f),
                        thickness = 0.75.dp,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AetherTelemetryObjectRow(
    event: WhatsUpTonightEngine.TonightEvent,
    isFa: Boolean,
    onClick: () -> Unit
) {
    val palette = AetherTheme.palette
    val appFont = LocalAppFontFamily.current
    val isPrime = event.visibilityStatus == WhatsUpTonightEngine.EventVisibilityStatus.OPTIMAL ||
        event.visibilityStatus == WhatsUpTonightEngine.EventVisibilityStatus.GOOD

    val scorePct = when (event.visibilityStatus) {
        WhatsUpTonightEngine.EventVisibilityStatus.OPTIMAL -> 95
        WhatsUpTonightEngine.EventVisibilityStatus.GOOD -> 78
        WhatsUpTonightEngine.EventVisibilityStatus.MARGINAL -> 54
        WhatsUpTonightEngine.EventVisibilityStatus.NOT_VISIBLE -> 24
    }
    val metricBadge = remember(event.targetObject, scorePct, isFa) {
        val m = event.targetObject?.magnitude
        if (m != null && m < 25.0) {
            String.format(Locale.US, "%.1f", m).aetherFormatDigits(isFa)
        } else {
            "$scorePct%".aetherFormatDigits(isFa)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(squircleShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isPrime) palette.glowWhite else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (isPrime) palette.glowWhite else palette.mutedTelemetry,
                    shape = CircleShape
                )
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isFa) event.titleFa else event.titleEn,
                style = RedTypographyTokens.bodyPrimary.copy(
                    fontFamily = appFont,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isFa) event.explanationFa else event.explanationEn,
                style = RedTypographyTokens.caption.copy(
                    fontFamily = appFont,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Light
                ),
                color = palette.textMicro,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            AetherDotMatrixText(
                text = metricBadge,
                dotSize = 1.85.dp,
                dotGap = 0.65.dp,
                charGap = 1.8.dp,
                activeColor = palette.glowWhite,
                glowColor = if (isPrime) palette.primaryAccent else null
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (event.targetObject?.magnitude != null && event.targetObject.magnitude < 25.0) {
                    if (isFa) "قدر" else "MAG"
                } else {
                    if (isFa) "کیفیت" else "VIS"
                },
                style = RedTypographyTokens.caption.copy(
                    fontFamily = appFont,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Light
                ),
                color = palette.textMicro
            )
        }
    }
}

@Composable
private fun AetherSearchOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<Triple<CelestialObject, CoordinateEngine.Horizontal, ObservabilityEngine.ObservabilityResult>>,
    isFa: Boolean,
    onSelectObject: (CelestialObject) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = AetherTheme.palette
    val appFont = LocalAppFontFamily.current
    val statusTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .padding(
                top = (statusTopPadding + 16.dp).coerceAtLeast(28.dp),
                start = 16.dp,
                end = 16.dp,
                bottom = 24.dp
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        AetherFrostedSpatialCard(
            cornerRadius = AetherCornerGeometry.heroCard,
            contentPadding = PaddingValues(20.dp),
            ambientTint = palette.primaryAccent,
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isFa) "جستجوی اجرام آسمانی" else "Celestial Search",
                    style = RedTypographyTokens.sectionTitle.copy(
                        fontFamily = appFont,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = if (isFa) 0.sp else (-0.3).sp
                    ),
                    color = palette.textPrimary
                )
                AetherMicroIconDisc(
                    icon = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
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
                textStyle = RedTypographyTokens.bodyPrimary.copy(
                    fontFamily = appFont,
                    color = palette.textPrimary
                ),
                placeholder = {
                    Text(
                        text = if (isFa) "نام سیاره، ستاره یا سحابی..." else "Planet, star, nebula...",
                        style = RedTypographyTokens.bodySecondary.copy(fontFamily = appFont),
                        color = palette.textMicro
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = palette.primaryAccent
                    )
                },
                shape = RedShapes.capsule,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.primaryAccent,
                    unfocusedBorderColor = palette.specularRimStart,
                    cursorColor = palette.primaryAccent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sky_search_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                results.take(7).forEach { (obj, horiz, _) ->
                    val name = if (isFa) obj.nameFa else obj.nameEn
                    val constName = if (isFa) {
                        obj.constellationFa.ifEmpty { obj.category }
                    } else {
                        obj.constellationEn.ifEmpty { obj.category }
                    }
                    val altStr = String.format(Locale.US, "%+.0f°", horiz.altitudeDeg).aetherFormatDigits(isFa)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(squircleShape(18.dp))
                            .background(palette.frostedGlassFill)
                            .specularRimBorder(cornerRadius = 18.dp)
                            .clickable { onSelectObject(obj) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                style = RedTypographyTokens.bodyPrimary.copy(
                                    fontFamily = appFont,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = constName,
                                style = RedTypographyTokens.caption.copy(
                                    fontFamily = appFont,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Light
                                ),
                                color = palette.textMicro,
                                maxLines = 1
                            )
                        }
                        AetherDotMatrixText(
                            text = altStr,
                            dotSize = 1.85.dp,
                            dotGap = 0.65.dp,
                            charGap = 1.8.dp,
                            activeColor = palette.glowWhite,
                            glowColor = if (horiz.altitudeDeg > 0.0) palette.primaryAccent else null
                        )
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

                AetherMicroIconDisc(
                    icon = Icons.Default.Close,
                    contentDescription = if (isFa) "بستن نمای تمام‌صفحه" else "Close fullscreen sky",
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(RedSpacing.md)
                        .testTag("sky_canvas_fullscreen_close")
                )
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
