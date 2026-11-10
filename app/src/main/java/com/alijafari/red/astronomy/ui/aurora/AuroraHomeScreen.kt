package com.alijafari.red.astronomy.ui.aurora

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.align
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.alijafari.red.astronomy.ui.theme.squircleShape
import java.util.Locale

// ============================================================================
// AURORA HOME
// ============================================================================
//
// Home is where a design language either holds together or does not, because it
// is the only screen that has to carry a full-bleed image, six kinds of reading,
// and a floating dock at the same time.
//
// The composition follows template-composition DR1-DR3 exactly:
//   page context .... the sky itself, edge to edge, and the place and instant
//                     written under it — never a card repeating them;
//   primary content . the Moon viewport and tonight's events;
//   primary action .. one per region, and never two competing fills;
//   supporting ....... metrics, the eclipse countdown, the object rail.
//
// The expensive astronomy is not recomputed here. It comes through the shared
// [HomeAstroMemo], keyed on every input, exactly as the other three Home
// variants take it, so the four cannot drift apart in what they report.

private const val AURORA_MOON_REFRESH_MS = 60_000L
private const val AURORA_CATALOG_REFRESH_MS = 60_000L
private const val AURORA_ECLIPSE_REFRESH_MS = 3_600_000L

private enum class AuroraTonightFilter {
    ALL,
    PLANETS,
    DEEP_SKY
}

@Composable
fun AuroraHomeScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    val liveTimeMs by viewModel.skyLiveClock.timeMs.collectAsState()
    val isSimulation = uiState.timeMachineState.mode == TimeMachineMode.SIMULATION
    val activeTimeMs = if (isSimulation) uiState.timeMachineState.simulationTimeMs else liveTimeMs

    // Each bucket advances once its results are ready, computed off the main
    // thread, so a refresh never costs a frame. Simulation time is immediate.
    val moonKeyMs = rememberReadyTimeMs(
        targetMs = HomeAstroKeys.bucketMs(activeTimeMs, isSimulation, AURORA_MOON_REFRESH_MS),
        deferUntilReady = !isSimulation,
        maxDeferredStepMs = 2 * AURORA_MOON_REFRESH_MS
    ) { HomeAstroKeys.moonKeys(it, uiState) }
    val catalogKeyMs = rememberReadyTimeMs(
        targetMs = HomeAstroKeys.bucketMs(activeTimeMs, isSimulation, AURORA_CATALOG_REFRESH_MS),
        deferUntilReady = !isSimulation,
        maxDeferredStepMs = 2 * AURORA_CATALOG_REFRESH_MS
    ) { HomeAstroKeys.catalogKeys(it, uiState, isFa) }
    val eclipseKeyMs = rememberReadyTimeMs(
        targetMs = HomeAstroKeys.bucketMs(activeTimeMs, isSimulation, AURORA_ECLIPSE_REFRESH_MS),
        deferUntilReady = !isSimulation,
        maxDeferredStepMs = 2 * AURORA_ECLIPSE_REFRESH_MS
    ) { HomeAstroKeys.eclipseKeys(it, uiState) }

    val catalogJd = remember(catalogKeyMs) { TimeEngine.getJulianDate(catalogKeyMs) }
    val allObjects = remember(catalogJd) { HomeAstroMemo.get(CatalogObjectsKey(catalogJd)) }
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
    val twilight = remember(sunHoriz) { SunEngine.getTwilightPhase(sunHoriz.altitudeDeg) }

    val moonJd = remember(moonKeyMs) { TimeEngine.getJulianDate(moonKeyMs) }
    val moonData = remember(moonJd, uiState.userLocation) {
        HomeAstroMemo.get(MoonKey(moonJd, uiState.userLocation.latitude, uiState.userLocation.longitude))
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
        )
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

    // Every object's altitude and observability, ranked. Earth is the observer's
    // own floor and the ISS has its own screen, so neither belongs in this list.
    val rankedObjects = remember(
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
                val equatorial = if (obj.id == "moon") {
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
                val horizontal = CoordinateEngine.equatorialToHorizontal(
                    equatorial,
                    catalogLastDeg,
                    uiState.userLocation.latitude,
                    uiState.userLocation.elevationMeters
                )
                val observability = ObservabilityEngine.calculateObservability(
                    altitudeDeg = horizontal.altitudeDeg,
                    sunAltitudeDeg = sunHoriz.altitudeDeg,
                    moonIlluminationPercent = moonData.illuminationPercent,
                    objectMagnitude = obj.magnitude,
                    bortleClass = uiState.bortleClass,
                    objectType = obj.type,
                    objectId = obj.id
                )
                AuroraRankedObject(obj, horizontal, observability)
            }
            .sortedWith(
                compareByDescending<AuroraRankedObject> { it.observability.scorePercent }
                    .thenByDescending { it.horizontal.altitudeDeg }
            )
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.setMoonDayOffset(0f) }
    }

    var tonightFilter by rememberSaveable { mutableStateOf(AuroraTonightFilter.ALL) }
    val filteredHighlights = remember(tonightHighlights, tonightFilter) {
        when (tonightFilter) {
            AuroraTonightFilter.ALL -> tonightHighlights.take(5)
            AuroraTonightFilter.PLANETS -> tonightHighlights.filter {
                val type = it.targetObject?.type
                type == ObjectType.PLANET || type == ObjectType.MOON || type == ObjectType.SUN
            }.take(5)
            AuroraTonightFilter.DEEP_SKY -> tonightHighlights.filter { it.targetObject?.type == ObjectType.DEEP_SKY }
                .take(5)
        }
    }

    val timezone = remember(uiState.userLocation.timezoneId) {
        TimeEngine.resolveTimeZone(uiState.userLocation.timezoneId)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("aurora_home"),
        contentPadding = PaddingValues(bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(AuroraSpace.section)
    ) {
        item {
            AuroraHeroSky(
                uiState = uiState,
                viewModel = viewModel,
                isFa = isFa,
                timezone = timezone,
                timeMs = activeTimeMs,
                twilightLabel = if (isFa) twilight.nameFa else twilight.nameEn
            )
        }

        item {
            AuroraMoonCard(
                isFa = isFa,
                moonData = moonData,
                sunHoriz = sunHoriz,
                latitude = uiState.userLocation.latitude,
                timezone = timezone,
                onOpenDetail = { viewModel.setShowMoonWindow(true) },
                modifier = Modifier
                    .padding(horizontal = AuroraSpace.screenEdge)
                    .offset(y = -(44).dp)
            )
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = AuroraSpace.screenEdge),
                verticalArrangement = Arrangement.spacedBy(AuroraSpace.group)
            ) {
                AuroraSectionHeader(
                    eyebrow = if (isFa) "امشب" else "Tonight",
                    title = if (isFa) "چه خبر است" else "What's up tonight"
                )
                AuroraSegmentedRail(
                    labels = listOf(
                        if (isFa) "همه" else "All",
                        if (isFa) "سیارات" else "Planets",
                        if (isFa) "اعماق فضا" else "Deep sky"
                    ),
                    selectedIndex = AuroraTonightFilter.entries.indexOf(tonightFilter),
                    onSelect = { tonightFilter = AuroraTonightFilter.entries[it] }
                )
                if (filteredHighlights.isEmpty()) {
                    AuroraEmptyState(
                        title = if (isFa) "موردی برای این دسته نیست" else "Nothing in this group",
                        body = if (isFa) {
                            "امشب رویدادی در این دسته ثبت نشده است. دستهٔ دیگری را برگزینید."
                        } else {
                            "No event in this group is listed for tonight. Try another group."
                        }
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(AuroraSpace.local)) {
                        filteredHighlights.forEach { event ->
                            AuroraTonightRow(
                                event = event,
                                isFa = isFa,
                                onOpen = {
                                    val target = event.targetObject
                                    if (target != null) viewModel.openObjectDetail(target)
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = AuroraSpace.screenEdge),
                verticalArrangement = Arrangement.spacedBy(AuroraSpace.group)
            ) {
                AuroraSectionHeader(
                    eyebrow = if (isFa) "اکنون" else "Right now",
                    title = if (isFa) "درخشان‌ترین‌ها" else "Brightest above you",
                    // The one primary action of this region: point the phone at the sky and
                    // see the ranking in place. It is the region's action, not the page's, so
                    // it sits with the content it acts on (template-composition DR2).
                    action = {
                        AuroraGhostAction(
                            label = if (isFa) "نشانه‌گیری" else "Point",
                            icon = Icons.Default.Explore,
                            onClick = { onNavigateToTab(3) }
                        )
                    }
                )
                if (rankedObjects.isEmpty()) {
                    AuroraSkeleton(modifier = Modifier.fillMaxWidth().height(96.dp))
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(AuroraSpace.group),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(rankedObjects.take(8)) { ranked ->
                            AuroraObjectTile(
                                ranked = ranked,
                                isFa = isFa,
                                onOpen = { viewModel.openObjectDetail(ranked.obj) },
                                modifier = Modifier.width(168.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = AuroraSpace.screenEdge),
                verticalArrangement = Arrangement.spacedBy(AuroraSpace.group)
            ) {
                AuroraSectionHeader(
                    eyebrow = if (isFa) "پدیده" else "Phenomenon",
                    title = if (isFa) "گرفت بعدی" else "Next eclipse"
                )
                if (nextEclipse == null) {
                    AuroraEmptyState(
                        title = if (isFa) "گرفتی در راه نیست" else "No eclipse upcoming",
                        body = if (isFa) {
                            "در بازهٔ محاسبه‌شده گرفتی پیش‌بینی نشده است."
                        } else {
                            "No eclipse falls inside the computed window."
                        },
                        icon = Icons.Default.AutoAwesome
                    )
                } else {
                    AuroraEclipseCard(
                        eclipse = nextEclipse,
                        isFa = isFa,
                        timezone = timezone,
                        nowMs = activeTimeMs,
                        onOpen = { viewModel.setShowEclipseWindow(true) }
                    )
                }
            }
        }
    }
}

private data class AuroraRankedObject(
    val obj: CelestialObject,
    val horizontal: CoordinateEngine.Horizontal,
    val observability: ObservabilityEngine.ObservabilityResult
)

// ----------------------------------------------------------------------------
// The hero
// ----------------------------------------------------------------------------

/**
 * The page context: the sky, edge to edge, with the place and the instant under
 * it.
 *
 * Nothing here is inside a card. Template-composition DR2 — the page context
 * leads the hierarchy without competing with the primary task, and a panel
 * around the sky would put a second surface between the person and the one
 * thing the screen is about.
 */
@Composable
private fun AuroraHeroSky(
    uiState: MainUiState,
    viewModel: MainViewModel,
    isFa: Boolean,
    timezone: java.util.TimeZone,
    timeMs: Long,
    twilightLabel: String
) {
    val palette = AuroraTheme.palette

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(352.dp)
    ) {
        HeroSkyCanvas(
            uiState = uiState,
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .testTag("aurora_hero_sky"),
            presentation = SkyCanvasPresentation.SCENE,
            onExpand = { viewModel.setShowSkyViewer(true) }
        )

        // The floating chrome: where you are, and the way out to settings.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = AuroraSpace.screenEdge, vertical = AuroraSpace.group),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AuroraLiquidSurface(
                modifier = Modifier.heightIn(min = 40.dp),
                shape = squircleShape(AuroraRadius.pill),
                tier = AuroraSurfaceTier.CARD,
                onClick = { viewModel.setShowLocationSelector(true) },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AuroraSpace.local)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isFa) uiState.userLocation.cityNameFa else uiState.userLocation.cityNameEn,
                        style = AuroraType.label,
                        color = palette.textPrimary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(AuroraSpace.local)) {
                AuroraStatusPill(
                    label = twilightLabel,
                    status = AuroraStatus.INFO,
                    icon = if (palette.isDark) Icons.Default.NightsStay else Icons.Outlined.WbSunny
                )
                AuroraIconAction(
                    icon = Icons.Default.Settings,
                    contentDescription = if (isFa) "تنظیمات" else "Settings",
                    onClick = { viewModel.setShowSettingsDialog(true) }
                )
            }
        }

        // The instant, written at the sky's own foot: the one piece of type that
        // is allowed to be this large, because it is what the screen is about.
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = AuroraSpace.screenEdge)
                .padding(bottom = AuroraSpace.group)
        ) {
            Text(
                text = TimeEngine.formatTime24h(timeMs, isFa, timezone),
                style = AuroraType.display,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = TimeEngine.formatDate(timeMs, uiState.calendarSystem, isFa, timezone),
                style = AuroraType.label,
                color = palette.textSecondary
            )
        }
    }
}

// ----------------------------------------------------------------------------
// The Moon
// ----------------------------------------------------------------------------

/**
 * The Moon, in a recessed viewport.
 *
 * The viewport is an [AuroraInset] rather than a card because the render is
 * light the person looks *into*, and a well reads as deeper than the page while
 * a card reads as higher — elevation-hierarchy DR1, in the one place on Home
 * where the two are easy to confuse.
 */
@Composable
private fun AuroraMoonCard(
    isFa: Boolean,
    moonData: MoonEngine.MoonData,
    sunHoriz: CoordinateEngine.Horizontal,
    latitude: Double,
    timezone: java.util.TimeZone,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AuroraTheme.palette
    val illumination = moonData.illuminationPercent

    AuroraCard(
        modifier = modifier.testTag("aurora_moon_card"),
        onClick = onOpenDetail
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AuroraSpace.group)
        ) {
            AuroraInset(modifier = Modifier.size(96.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    MoonPhaseRender(
                        moonData = moonData,
                        sunHoriz = sunHoriz,
                        latitude = latitude,
                        modifier = Modifier.size(84.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isFa) "ماه" else "The Moon",
                    style = AuroraType.eyebrow,
                    color = palette.textTertiary
                )
                Spacer(modifier = Modifier.height(AuroraSpace.local))
                Text(
                    text = if (isFa) moonData.phaseNameFa else moonData.phaseNameEn,
                    style = AuroraType.title,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(AuroraSpace.group))
                // Illumination as a bar *and* a number: length alone would be a
                // single cue, which system-states DR2 does not allow.
                AuroraProgressBar(
                    progress = (illumination / 100.0).toFloat(),
                    status = AuroraStatus.NEUTRAL,
                    label = String.format(
                        Locale.US,
                        if (isFa) "درخشندگی %s%%" else "%s%% illuminated",
                        "%.0f".format(Locale.US, illumination)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(AuroraSpace.group))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AuroraSpace.group)
        ) {
            AuroraMetricTile(
                label = if (isFa) "طلوع" else "Moonrise",
                value = moonData.moonriseTimeMs?.let { TimeEngine.formatTime24h(it, isFa, timezone) } ?: "—",
                modifier = Modifier.weight(1f),
                icon = Icons.Default.NightsStay
            )
            AuroraMetricTile(
                label = if (isFa) "غروب" else "Moonset",
                value = moonData.moonsetTimeMs?.let { TimeEngine.formatTime24h(it, isFa, timezone) } ?: "—",
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Stars
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Tonight
// ----------------------------------------------------------------------------

@Composable
private fun AuroraTonightRow(
    event: WhatsUpTonightEngine.TonightEvent,
    isFa: Boolean,
    onOpen: () -> Unit
) {
    val palette = AuroraTheme.palette
    val status = when (event.visibilityStatus) {
        WhatsUpTonightEngine.EventVisibilityStatus.OPTIMAL -> AuroraStatus.GOOD
        WhatsUpTonightEngine.EventVisibilityStatus.GOOD -> AuroraStatus.INFO
        WhatsUpTonightEngine.EventVisibilityStatus.MARGINAL -> AuroraStatus.WARN
        WhatsUpTonightEngine.EventVisibilityStatus.NOT_VISIBLE -> AuroraStatus.NEUTRAL
    }

    AuroraLiquidSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = squircleShape(AuroraRadius.inner(AuroraRadius.card, 8.dp)),
        tier = AuroraSurfaceTier.CARD,
        onClick = if (event.targetObject != null) onOpen else null,
        enabled = event.targetObject != null,
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AuroraSpace.group, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuroraStatusDot(status = status)
            Spacer(modifier = Modifier.width(AuroraSpace.group))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isFa) event.titleFa else event.titleEn,
                    style = AuroraType.label,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isFa) event.visibilityTextFa else event.visibilityTextEn,
                    style = AuroraType.caption,
                    color = palette.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(AuroraSpace.local))
            Text(
                text = if (isFa) event.timeOrDateStrFa else event.timeOrDateStrEn,
                style = AuroraType.data,
                color = palette.textSecondary
            )
            if (event.targetObject != null) {
                Spacer(modifier = Modifier.width(AuroraSpace.local))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = palette.textTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/** The status a visibility level maps onto. Never the level's own colour. */
private fun auroraStatusForLevel(level: ObservabilityEngine.VisibilityLevel): AuroraStatus = when (level) {
    ObservabilityEngine.VisibilityLevel.OUTSTANDING,
    ObservabilityEngine.VisibilityLevel.EXCELLENT -> AuroraStatus.GOOD
    ObservabilityEngine.VisibilityLevel.VERY_GOOD,
    ObservabilityEngine.VisibilityLevel.GOOD -> AuroraStatus.INFO
    ObservabilityEngine.VisibilityLevel.MARGINAL -> AuroraStatus.WARN
    else -> AuroraStatus.BAD
}

// ----------------------------------------------------------------------------
// Objects and eclipses
// ----------------------------------------------------------------------------

@Composable
private fun AuroraObjectTile(
    ranked: AuroraRankedObject,
    isFa: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AuroraTheme.palette
    val status = auroraStatusForLevel(ranked.observability.level)

    AuroraCard(
        modifier = modifier.testTag("aurora_object_tile"),
        onClick = onOpen,
        contentPadding = AuroraSpace.group
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AuroraSpace.local)
        ) {
            AuroraStatusDot(status = status)
            Text(
                text = if (isFa) ranked.obj.type.nameFa else ranked.obj.type.nameEn,
                style = AuroraType.eyebrow,
                color = palette.textTertiary
            )
        }
        Spacer(modifier = Modifier.height(AuroraSpace.local))
        Text(
            text = if (isFa) ranked.obj.nameFa else ranked.obj.nameEn,
            style = AuroraType.section,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = String.format(
                Locale.US,
                if (isFa) "قدر %s • ارتفاع %s°" else "mag %s • alt %s°",
                "%.1f".format(Locale.US, ranked.obj.magnitude),
                "%.0f".format(Locale.US, ranked.horizontal.altitudeDeg)
            ),
            style = AuroraType.caption,
            color = palette.textTertiary
        )
        Spacer(modifier = Modifier.height(AuroraSpace.group))
        AuroraProgressBar(
            progress = ranked.observability.scorePercent / 100f,
            status = status
        )
        Spacer(modifier = Modifier.height(AuroraSpace.local))
        Text(
            text = if (isFa) {
                ranked.observability.bestObservationTimeFa
            } else {
                ranked.observability.bestObservationTimeEn
            },
            style = AuroraType.caption,
            color = palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AuroraEclipseCard(
    eclipse: EclipseEngine.EclipseResult,
    isFa: Boolean,
    timezone: java.util.TimeZone,
    nowMs: Long,
    onOpen: () -> Unit
) {
    val palette = AuroraTheme.palette
    val daysAway = ((eclipse.localPeakTimeMs - nowMs) / 86_400_000L).toInt().coerceAtLeast(0)
    val status = if (eclipse.isLocallyVisible) AuroraStatus.GOOD else AuroraStatus.WARN

    AuroraCard(
        modifier = Modifier.testTag("aurora_eclipse_card"),
        onClick = onOpen
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isFa) eclipse.localNameFa else eclipse.localNameEn,
                    style = AuroraType.eyebrow,
                    color = palette.textTertiary
                )
                Spacer(modifier = Modifier.height(AuroraSpace.local))
                Text(
                    text = if (isFa) eclipse.formattedDateFa else eclipse.formattedDateEn,
                    style = AuroraType.section,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(AuroraSpace.group))
                AuroraStatusPill(
                    label = if (eclipse.isLocallyVisible) {
                        if (isFa) "قابل مشاهده از این نقطه" else "Visible from here"
                    } else {
                        if (isFa) "از این نقطه دیده نمی‌شود" else "Not visible from here"
                    },
                    status = status
                )
            }
            Spacer(modifier = Modifier.width(AuroraSpace.group))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = daysAway.toString(),
                    style = AuroraType.display,
                    color = palette.accent
                )
                Text(
                    text = if (isFa) "روز مانده" else "days away",
                    style = AuroraType.caption,
                    color = palette.textTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(AuroraSpace.group))
        AuroraReadoutRow(
            label = if (isFa) "اوج" else "Peak",
            value = TimeEngine.formatTime24h(eclipse.localPeakTimeMs, isFa, timezone)
        )
        AuroraReadoutRow(
            label = if (isFa) "گرفتگی" else "Obscuration",
            value = "${eclipse.localObscurationPercent}%"
        )
    }
}
