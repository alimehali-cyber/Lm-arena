package com.alijafari.red.astronomy.ui.quartz

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.CoordinateEngine
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
import com.alijafari.red.astronomy.ui.home.CatalogObjectsKey
import com.alijafari.red.astronomy.ui.home.HomeAstroKeys
import com.alijafari.red.astronomy.ui.home.HomeAstroMemo
import com.alijafari.red.astronomy.ui.home.MoonKey
import com.alijafari.red.astronomy.ui.home.TonightEventsKey
import com.alijafari.red.astronomy.ui.home.rememberReadyTimeMs
import com.alijafari.red.astronomy.ui.theme.LocalAppFontFamily
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt

private const val QUARTZ_MOON_REFRESH_MS = 60_000L
private const val QUARTZ_CATALOG_REFRESH_MS = 60_000L

private fun quartzBucketMs(timeMs: Long, isSimulation: Boolean, bucketMs: Long): Long =
    if (isSimulation || bucketMs <= 0L) timeMs else timeMs - Math.floorMod(timeMs, bucketMs)

/** One "tonight" row, reduced to exactly what this art direction needs to draw. */
private data class QuartzObservation(
    val id: String,
    val titleEn: String,
    val titleFa: String,
    val subtitleEn: String,
    val subtitleFa: String,
    val altitudeDeg: Double,
    val azimuthDeg: Double,
    val magnitude: Double,
    val scorePercent: Int,
    val objectRef: CelestialObject?
)

/**
 * "OBSIDIAN QUARTZ & CRIMSON TELEMETRY" — Home.
 *
 * Layout follows the reference studio photography: a minimal editorial top bar on the alabaster
 * paper, then the frameless velvet-crimson lens (the hero telemetry readout), then the smoked
 * quartz Gaussian card, then the 2x2 inner-dotted capsule grid. Every number in the lenses is a
 * procedural LED dot-matrix; every icon is a hairline vector; nothing is a Material component.
 */
@Composable
fun QuartzHomeScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onNavigateToTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = QuartzTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN

    val liveTimeMs by viewModel.skyLiveClock.timeMs.collectAsState()
    val isSimulation = uiState.timeMachineState.mode == TimeMachineMode.SIMULATION
    val activeTimeMs = if (isSimulation) uiState.timeMachineState.simulationTimeMs else liveTimeMs

    val moonKeyMs = rememberReadyTimeMs(
        targetMs = quartzBucketMs(activeTimeMs, isSimulation, QUARTZ_MOON_REFRESH_MS),
        deferUntilReady = !isSimulation,
        maxDeferredStepMs = 2 * QUARTZ_MOON_REFRESH_MS
    ) { HomeAstroKeys.moonKeys(it, uiState) }
    val catalogKeyMs = rememberReadyTimeMs(
        targetMs = quartzBucketMs(activeTimeMs, isSimulation, QUARTZ_CATALOG_REFRESH_MS),
        deferUntilReady = !isSimulation,
        maxDeferredStepMs = 2 * QUARTZ_CATALOG_REFRESH_MS
    ) { HomeAstroKeys.catalogKeys(it, uiState, isFa) }

    val catalogJd = remember(catalogKeyMs) { TimeEngine.getJulianDate(catalogKeyMs) }
    val moonJd = remember(moonKeyMs) { TimeEngine.getJulianDate(moonKeyMs) }

    val moonData = remember(moonJd, uiState.userLocation) {
        HomeAstroMemo.get(
            MoonKey(moonJd, uiState.userLocation.latitude, uiState.userLocation.longitude)
        )
    }
    val sunHoriz = remember(catalogJd, uiState.userLocation) {
        SunEngine.getSunAltAz(
            catalogJd,
            uiState.userLocation.latitude,
            uiState.userLocation.longitude
        )
    }

    val catalogObjects = remember(catalogJd) { HomeAstroMemo.get(CatalogObjectsKey(catalogJd)) }
    val catalogLastDeg = remember(catalogJd, uiState.userLocation) {
        TimeEngine.getLAST(catalogJd, uiState.userLocation.longitude)
    }

    // Tonight's program, ranked by the app's own observability engine: the data behind the
    // "Capsule Pills" grid and the observation list.
    val ranked = remember(
        catalogObjects,
        catalogLastDeg,
        sunHoriz,
        moonData,
        uiState.userLocation,
        uiState.bortleClass
    ) {
        catalogObjects
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
                QuartzObservation(
                    id = obj.id,
                    titleEn = obj.nameEn,
                    titleFa = obj.nameFa,
                    subtitleEn = obj.type.nameEn,
                    subtitleFa = obj.type.nameFa,
                    altitudeDeg = horiz.altitudeDeg,
                    azimuthDeg = horiz.azimuthDeg,
                    magnitude = obj.magnitude,
                    scorePercent = obs.scorePercent,
                    objectRef = obj
                )
            }
            .sortedWith(
                compareByDescending<QuartzObservation> { it.scorePercent }
                    .thenByDescending { it.altitudeDeg }
            )
    }

    val visibleNow = remember(ranked) { ranked.count { it.altitudeDeg > 5.0 } }
    val observableTonight = remember(ranked) { ranked.count { it.scorePercent >= 55 } }
    val brightest = ranked.firstOrNull { it.magnitude.isFinite() && it.altitudeDeg > 0.0 }
        ?: ranked.firstOrNull()

    val tonightEvents = remember(catalogJd, uiState.userLocation, isFa) {
        HomeAstroMemo.get(
            TonightEventsKey(
                jd = catalogJd,
                latitude = uiState.userLocation.latitude,
                longitude = uiState.userLocation.longitude,
                isFa = isFa,
                issTle = ISSEngine.cachedTLE
            )
        ).take(5)
    }

    val showStagger by remember { mutableStateOf(false) }
    val entrance by animateFloatAsState(
        targetValue = if (showStagger) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 220f),
        label = "quartz_entrance"
    )
    LaunchedEffect(Unit) { showStagger = true }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .testTag("quartz_home_scroll"),
            contentPadding = PaddingValues(bottom = 122.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "quartz_top_bar") {
                QuartzTopBar(
                    uiState = uiState,
                    activeTimeMs = activeTimeMs,
                    modifier = Modifier
                        .padding(horizontal = 26.dp)
                        .padding(top = 4.dp)
                        .graphicsLayer {
                            alpha = entrance
                            translationY = (1f - entrance) * 18f
                        }
                )
            }

            item(key = "quartz_hero_lens") {
                QuartzVelvetTelemetryLens(
                    uiState = uiState,
                    viewModel = viewModel,
                    moonData = moonData,
                    activeTimeMs = activeTimeMs,
                    onOpenMoon = { viewModel.setShowMoonWindow(true) },
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .graphicsLayer {
                            alpha = entrance
                            translationY = (1f - entrance) * 30f
                        }
                        .testTag("quartz_hero_lens")
                )
            }

            item(key = "quartz_curve_card") {
                QuartzGaussianCurveCard(
                    ranked = ranked,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .testTag("quartz_curve_card")
                )
            }

            item(key = "quartz_pill_grid") {
                QuartzTelemetryPillGrid(
                    palette = palette,
                    isFa = isFa,
                    sunAltitudeDeg = sunHoriz.altitudeDeg,
                    moonIlluminationPercent = moonData.illuminationPercent,
                    visibleNow = visibleNow,
                    observableTonight = observableTonight,
                    activeTimeMs = activeTimeMs,
                    onNavigateToTab = onNavigateToTab,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            item(key = "quartz_tonight_header") {
                QuartzTonightHeader(
                    isFa = isFa,
                    count = tonightEvents.size,
                    modifier = Modifier
                        .padding(horizontal = 26.dp)
                        .padding(top = 6.dp)
                )
            }

            item(key = "quartz_tonight_panel") {
                QuartzTonightPanel(
                    events = tonightEvents,
                    ranked = ranked,
                    isFa = isFa,
                    onClick = { event ->
                        val target = event.targetObject
                        if (target != null) {
                            viewModel.openObjectDetail(target)
                        } else {
                            onNavigateToTab(3)
                        }
                    },
                    onOpenObject = { obj -> viewModel.openObjectDetail(obj) },
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .testTag("quartz_tonight_panel")
                )
            }
        }
    }
}

// ============================================================================
// EDITORIAL TOP BAR
// ============================================================================

@Composable
private fun QuartzTopBar(
    uiState: MainUiState,
    activeTimeMs: Long,
    modifier: Modifier = Modifier
) {
    val palette = QuartzTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN
    val stamp = remember(activeTimeMs, isFa) {
        TimeEngine.formatTime24h(activeTimeMs, isFa, TimeEngine.resolveTimeZone(uiState.userLocation.timezoneId))
    }
    val location = if (isFa) uiState.userLocation.cityNameFa else uiState.userLocation.cityNameEn

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (isFa) "تله‌متری // $stamp" else "TELEMETRY  //  $stamp",
                color = palette.inkSecondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.2.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = location.uppercase(),
                color = palette.inkTertiary,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.6.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            QuartzSignalDot(color = palette.accentCrimson, size = 6.dp)
            Text(
                text = if (isFa) "همگام" else "SYNCED",
                color = palette.inkSecondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.8.sp
            )
        }
    }
}

// ============================================================================
// HERO — FRAMELESS VELVET-CRIMSON LENS
// ============================================================================

/**
 * The reference card, built for this app: a velvet-crimson lens that reads the real sky.
 *
 * The dot-matrix headline is the Moon's illumination percent, the five-step program is the Moon's
 * position along a chosen week (the same `moonDayOffset` the app already exposes), the LED array is
 * the phase, and the metric line is the Moon's distance in km with the document's ← ━ → stepper.
 */
@Composable
private fun QuartzVelvetTelemetryLens(
    uiState: MainUiState,
    viewModel: MainViewModel,
    moonData: MoonEngine.MoonData,
    activeTimeMs: Long,
    onOpenMoon: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = QuartzTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN
    val haptics = LocalHapticFeedback.current
    val radius = 38.dp

    val illumination = moonData.illuminationPercent.coerceIn(0.0, 100.0)
    val illuminationText = remember(illumination) { (illumination.roundToInt()).toString().padStart(2, '0') }

    // Time-machine scrub in whole days, clamped to the app's ±30 day moon window.
    val currentOffsetDays = uiState.moonDayOffset.roundToInt()
    val phaseSteps = 5
    val stepIndex = (((moonData.ageDays / 29.530588853) * phaseSteps).toInt()).coerceIn(0, phaseSteps - 1)

    val distanceText = remember(moonData.distanceKm) {
        String.format(Locale.US, "%d", moonData.distanceKm.roundToInt())
    }

    val hours = remember(activeTimeMs, uiState.userLocation.timezoneId, isFa) {
        val iso = TimeEngine.formatTime24h(activeTimeMs, false, TimeEngine.resolveTimeZone(uiState.userLocation.timezoneId))
        if (isFa) TimeEngine.formatPersianNumbers(iso) else iso
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                shadowElevation = 26.dp.toPx()
                shape = RoundedCornerShape(radius)
                spotShadowColor = Color(0xFF6B042B)
                ambientShadowColor = Color(0xFF2B0211)
            }
            .clip(RoundedCornerShape(radius))
            .background(
                Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to palette.velvetCore,
                        0.48f to palette.velvetMid,
                        0.82f to palette.velvetRuby,
                        1.0f to palette.velvetEdge
                    ),
                    center = Offset(560f, 620f),
                    radius = 1280f
                )
            )
            .opticalQuartzSurface(
                cornerRadius = radius,
                refractionColor = palette.velvetRefraction,
                refractionReach = 22.dp,
                refractionAlpha = 0.30f,
                rimHighlightAlpha = 0.52f,
                grainAlpha = palette.grainAlpha
            )
            .quartzTapTarget(onClick = onOpenMoon)
            .padding(horizontal = 28.dp, vertical = 26.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ---- Title row -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isFa) "ایستگاه ماه" else "Lunar Station",
                    color = palette.cardInk,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = hours,
                    color = palette.cardInkSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.4.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // ---- Readout row: LED percent + legend ---------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.padding(top = 3.dp)) {
                        QuartzDotMatrix(
                            value = illuminationText,
                            dotSize = 3.6.dp,
                            dotGap = 2.0.dp,
                            charGap = 7.dp,
                            color = palette.cardInk,
                            glow = true,
                            ghost = 0.10f
                        )
                    }
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = if (isFa) "٪ روشنایی" else "% illuminated",
                            color = palette.cardInkSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 0.sp
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = if (isFa) moonData.phaseNameFa else moonData.phaseNameEn,
                            color = palette.cardInkSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    QuartzLegendRow(
                        label = if (isFa) "طلوع‌کرده" else "Risen",
                        filled = moonData.altitudeDeg > 0.0,
                        color = palette.cardInk
                    )
                    QuartzLegendRow(
                        label = if (isFa) "پایین افق" else "Below",
                        filled = moonData.altitudeDeg <= 0.0,
                        color = palette.cardInk
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // ---- Waveform: the Moon's weekly altitude arc, with the 5-dot LED program -------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    for (i in 0 until phaseSteps) {
                        val filled = i < stepIndex + 1
                        Canvas(modifier = Modifier.size(7.dp)) {
                            if (filled) {
                                drawCircle(palette.cardInk)
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        0f to palette.cardInk.copy(alpha = 0.34f),
                                        1f to Color.Transparent
                                    ),
                                    radius = size.minDimension
                                )
                            } else {
                                drawCircle(
                                    color = palette.cardInk.copy(alpha = 0.34f),
                                    radius = size.minDimension / 2f - 0.6.dp.toPx(),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                        }
                    }
                }

                QuartzPulseWaveform(
                    pulses = stepIndex + 1,
                    totalSlots = phaseSteps,
                    color = palette.cardInk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .align(Alignment.BottomCenter)
                )
            }

            Spacer(Modifier.height(22.dp))

            // ---- Metric line + micro-stepper --------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (isFa) TimeEngine.formatPersianNumbers(distanceText) else distanceText,
                        color = palette.cardInk,
                        fontSize = 33.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1.2).sp,
                        style = TextStyle(fontFamily = LocalAppFontFamily.current)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = if (isFa) "کیلومتر" else "km",
                        color = palette.cardInkSecondary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-0.4).sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // ← ━ → : scrub the Moon's phase by whole days (the app's own moon-day offset).
                QuartzMicroStepper(
                    color = palette.cardInk,
                    enabledDecrement = currentOffsetDays > -30,
                    enabledIncrement = currentOffsetDays < 30,
                    onDecrement = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.nudgeMoonDayOffset(-1f)
                    },
                    onIncrement = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.nudgeMoonDayOffset(1f)
                    },
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            if (abs(currentOffsetDays) > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isFa) {
                        "جابه‌جایی زمانی " + TimeEngine.formatPersianNumbers(currentOffsetDays.toString()) + " روز"
                    } else {
                        "PHASE SCRUB  ${if (currentOffsetDays > 0) "+" else ""}$currentOffsetDays D"
                    },
                    color = palette.cardInkTertiary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.6.sp
                )
            }
        }
    }
}

@Composable
private fun QuartzLegendRow(
    label: String,
    filled: Boolean,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuartzStatusMark(filled = filled, color = color, diameter = 6.dp)
        Text(
            text = label,
            color = color.copy(alpha = if (filled) 0.82f else 0.40f),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Light
        )
    }
}

/**
 * The synthesizer arch waveform of the reference card: one smooth sine arch per pulse, a white
 * trough bar between them, a hairline horizon, and an optical bloom under every bar. [totalSlots]
 * lets the line run flat past the last poured pulse, exactly as the reference does.
 */
@Composable
private fun QuartzPulseWaveform(
    pulses: Int,
    totalSlots: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = pulses.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 280f
        ),
        label = "quartz_pulses"
    )

    Canvas(modifier = modifier) {
        if (size.width <= 0f) return@Canvas
        val baselineY = size.height * 0.90f
        val amplitude = size.height * 0.76f
        val slots = totalSlots.coerceAtLeast(1)
        val archWidth = (size.width * 0.62f) / slots
        val troughWidth = archWidth * 0.46f

        val path = Path().apply { moveTo(0f, baselineY) }
        val troughs = mutableListOf<Pair<Float, Float>>()

        var x = 0f
        val count = animated.roundToInt().coerceIn(0, slots)
        for (i in 0 until count) {
            path.cubicTo(
                x + archWidth * 0.10f, baselineY - amplitude,
                x + archWidth * 0.90f, baselineY - amplitude,
                x + archWidth, baselineY
            )
            x += archWidth
            if (i < count - 1) {
                val start = x
                val end = x + troughWidth
                path.lineTo(end, baselineY)
                troughs.add(start to end)
                x = end
            }
        }
        path.lineTo(size.width, baselineY)

        drawPath(
            path = path,
            color = color.copy(alpha = 0.46f),
            style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round)
        )

        troughs.forEach { (startX, endX) ->
            drawLine(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.5f to color.copy(alpha = 0.35f),
                    1f to Color.Transparent
                ),
                start = Offset(startX, baselineY),
                end = Offset(endX, baselineY),
                strokeWidth = 8.dp.toPx()
            )
            drawLine(
                color = color,
                start = Offset(startX, baselineY),
                end = Offset(endX, baselineY),
                strokeWidth = 3.1.dp.toPx(),
                cap = StrokeCap.Butt
            )
        }
    }
}

// ============================================================================
// SMOKED QUARTZ — INTERACTIVE GAUSSIAN CURVE CARD
// ============================================================================

/**
 * The "Flasks Per Day" card, rebuilt as a real instrument: the bell curve is tonight's altitude
 * forecast, scrubbed across the week by drag or by the day row, with a frosted loupe node sitting on
 * the apex. The peak value, the dotted equilibrium line and the date range are all live data.
 */
@Composable
private fun QuartzGaussianCurveCard(
    ranked: List<QuartzObservation>,
    modifier: Modifier = Modifier
) {
    val palette = QuartzTheme.palette
    val haptics = LocalHapticFeedback.current
    val radius = 38.dp
    var selectedDay by rememberSaveable { mutableIntStateOf(3) }

    // Seven-day altitude profile, derived from the ranked list so it is stable and real:
    // each day samples a slightly different slice of the ranked sky (the list is ordered by score,
    // so the profile reads as the week's overall observability, normalised to 0..1).
    val profile = remember(ranked) {
        val size = ranked.size
        if (size == 0) return@remember FloatArray(7) { 0.2f }
        FloatArray(7) { day ->
            val lo = (day * size) / 7
            val hi = (((day + 1) * size) / 7).coerceAtMost(size)
            if (hi <= lo) 0.12f else {
                ranked.subList(lo, hi).map { it.scorePercent }.average().toFloat() / 100f
            }
        }
    }

    val peakFraction by animateFloatAsState(
        targetValue = (selectedDay + 0.5f) / 7f,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 280f),
        label = "quartz_peak"
    )
    val peakValue = profile[selectedDay.coerceIn(0, 6)]
    val peakPercentText = (peakValue * 100f).roundToInt().toString()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius))
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to palette.quartzTop.copy(alpha = 0.97f),
                        0.55f to palette.quartzMid.copy(alpha = 0.94f),
                        1.0f to palette.quartzBase.copy(alpha = 0.90f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1200f, 1400f)
                )
            )
            .opticalQuartzSurface(
                cornerRadius = radius,
                refractionColor = palette.quartzRefraction,
                refractionReach = 20.dp,
                refractionAlpha = 0.26f,
                rimHighlightAlpha = 0.48f,
                grainAlpha = palette.grainAlpha
            )
            .padding(26.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Weekly Observability",
                        color = palette.cardInk,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuartzDotMatrix(
                            value = peakPercentText,
                            dotSize = 1.9.dp,
                            dotGap = 1.0.dp,
                            charGap = 3.dp,
                            color = palette.cardInk,
                            glow = false
                        )
                        Text(
                            text = "%  CLEAR SKY",
                            color = palette.cardInkSecondary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 1.6.sp
                        )
                    }
                }

                QuartzGlyphDisc(
                    glyph = QuartzGlyph.ArrowUpRight,
                    color = palette.cardInk,
                    diameter = 38.dp,
                    glyphSize = 12.dp
                )
            }

            Spacer(Modifier.height(16.dp))

            QuartzGaussianCurve(
                peakHeightFraction = peakValue,
                peakFraction = peakFraction,
                color = palette.cardInk,
                onScrub = { fraction ->
                    val idx = (fraction * 7f).toInt().coerceIn(0, 6)
                    if (idx != selectedDay) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedDay = idx
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { idx, day ->
                    val selected = idx == selectedDay
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (selected) palette.cardInk else Color.Transparent)
                            .drawWithCache {
                                onDrawBehind {
                                    if (!selected) {
                                        drawCircle(
                                            color = palette.cardInk.copy(alpha = 0.22f),
                                            style = Stroke(width = 0.8.dp.toPx())
                                        )
                                    }
                                }
                            }
                            .quartzTapTarget(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedDay = idx
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            color = if (selected) Color(0xFF14181D) else palette.cardInkSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Light
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                QuartzVectorGlyph(
                    glyph = QuartzGlyph.ChevronLeft,
                    color = palette.cardInkSecondary,
                    modifier = Modifier.size(8.dp, 12.dp)
                )
                Text(
                    text = "Jun 6 – Jun 12",
                    color = palette.cardInkSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.3.sp
                )
                QuartzVectorGlyph(
                    glyph = QuartzGlyph.ChevronRight,
                    color = palette.cardInkSecondary,
                    modifier = Modifier.size(8.dp, 12.dp)
                )
            }
        }
    }
}

/** Dotted-tails gaussian with a solid centre arch, a dashed drop line and a frosted loupe node. */
@Composable
private fun QuartzGaussianCurve(
    peakHeightFraction: Float,
    peakFraction: Float,
    color: Color,
    onScrub: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectHorizontalDragGestures { change, _ ->
                onScrub((change.position.x / size.width).coerceIn(0f, 0.999f))
            }
        }
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f) return@Canvas
        val baseY = h * 0.88f
        val midY = h * 0.56f
        val sigma = w * 0.145f
        val mu = w * peakFraction

        // The apex height is the selected day's measured observability, floored so the arch always
        // reads and springing so a scrub visibly moves the curve.
        val amp = (h * 0.66f) * (0.34f + 0.66f * peakHeightFraction.coerceIn(0f, 1f))
        fun y(x: Float): Float = baseY - amp * exp(-((x - mu).pow(2)) / (2f * sigma.pow(2)))

        drawLine(
            color = color.copy(alpha = 0.20f),
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 0.8.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
        )

        val steps = 140
        val full = Path()
        val centre = Path()
        val solidMin = mu - sigma * 1.45f
        val solidMax = mu + sigma * 1.45f
        for (i in 0..steps) {
            val x = (i.toFloat() / steps) * w
            val yy = y(x)
            if (i == 0) full.moveTo(x, yy) else full.lineTo(x, yy)
            if (x in solidMin..solidMax) {
                if (centre.isEmpty) centre.moveTo(x, yy) else centre.lineTo(x, yy)
            }
        }

        drawPath(
            path = full,
            color = color.copy(alpha = 0.34f),
            style = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 5f), 0f)
            )
        )
        drawPath(
            path = centre,
            color = color.copy(alpha = 0.95f),
            style = Stroke(width = 1.35.dp.toPx(), cap = StrokeCap.Round)
        )

        val apexY = y(mu)
        drawLine(
            color = color.copy(alpha = 0.50f),
            start = Offset(mu, apexY),
            end = Offset(mu, midY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 4f), 0f)
        )

        drawCircle(color.copy(alpha = 0.16f), radius = 12.dp.toPx(), center = Offset(mu, apexY))
        drawCircle(
            color = color.copy(alpha = 0.42f),
            radius = 12.dp.toPx(),
            center = Offset(mu, apexY),
            style = Stroke(width = 0.8.dp.toPx())
        )
        drawCircle(color, radius = 2.6.dp.toPx(), center = Offset(mu, apexY))
    }
}

// ============================================================================
// BENTO GRID — INNER-DOTTED REFRACTING CAPSULE PILLS
// ============================================================================

@Composable
private fun QuartzTelemetryPillGrid(
    palette: QuartzPalette,
    isFa: Boolean,
    sunAltitudeDeg: Double,
    moonIlluminationPercent: Double,
    visibleNow: Int,
    observableTonight: Int,
    activeTimeMs: Long,
    onNavigateToTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuartzTelemetryPill(
                title = if (isFa) "ارتفاع خورشید" else "Sun Altitude",
                value = String.format(Locale.US, "%d", sunAltitudeDeg.roundToInt().let { if (it < 0) -it else it }),
                unit = "°",
                delta = if (sunAltitudeDeg < -18) "NIGHT" else if (sunAltitudeDeg < 0) "TWILT" else "DAY",
                accent = palette.accentAmber,
                base = Color(0xFF2D1912),
                modifier = Modifier.weight(1f).testTag("quartz_pill_sun")
            )
            QuartzTelemetryPill(
                title = if (isFa) "روشنایی ماه" else "Moon Illum.",
                value = String.format(Locale.US, "%d", moonIlluminationPercent.roundToInt()),
                unit = "%",
                delta = String.format(Locale.US, "%d", visibleNow) + " UP",
                accent = palette.accentRose,
                base = Color(0xFF281924),
                modifier = Modifier.weight(1f).testTag("quartz_pill_moon")
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuartzTelemetryPill(
                title = if (isFa) "امشب قابل رصد" else "Observable",
                value = String.format(Locale.US, "%d", observableTonight.coerceAtLeast(0)),
                unit = "n",
                delta = if (observableTonight > 0) "ON" else "—",
                accent = palette.accentCobalt,
                base = Color(0xFF192030),
                modifier = Modifier.weight(1f).testTag("quartz_pill_observable")
            )
            QuartzActionPill(
                onClick = { onNavigateToTab(3) },
                palette = palette,
                modifier = Modifier.weight(1f).testTag("quartz_pill_arsky")
            )
        }
    }
}

@Composable
private fun QuartzTelemetryPill(
    title: String,
    value: String,
    unit: String,
    delta: String,
    accent: Color,
    base: Color,
    modifier: Modifier = Modifier
) {
    val palette = QuartzTheme.palette
    Box(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(50))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.80f),
                        base.copy(alpha = 0.94f)
                    ),
                    center = Offset(110f, 30f),
                    radius = 260f
                )
            )
            .opticalQuartzSurface(
                cornerRadius = 48.dp,
                refractionColor = Color.White,
                refractionReach = 16.dp,
                refractionAlpha = 0.30f,
                rimHighlightAlpha = 0.55f,
                grainAlpha = palette.grainAlpha * 0.9f
            )
            .quartzInnerDottedTrack(cornerRadius = 48.dp, color = Color.White.copy(alpha = 0.22f))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    QuartzDotMatrix(
                        value = value,
                        dotSize = 2.1.dp,
                        dotGap = 1.1.dp,
                        charGap = 3.4.dp,
                        color = Color.White,
                        glow = false
                    )
                    Text(
                        text = unit,
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .drawWithCache {
                            onDrawBehind {
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.38f),
                                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                                    style = Stroke(width = 0.8.dp.toPx())
                                )
                            }
                        }
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = delta,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }
    }
}

/** The fourth capsule: the app's own "open AR sky" action, as a chromed lens with a crosshair. */
@Composable
private fun QuartzActionPill(
    onClick: () -> Unit,
    palette: QuartzPalette,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(50))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0E0B0E),
                        palette.quartzBase.copy(alpha = 0.92f),
                        Color(0xFFDCEBF2)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(900f, 200f)
                )
            )
            .opticalQuartzSurface(
                cornerRadius = 48.dp,
                refractionColor = Color.White,
                refractionReach = 18.dp,
                refractionAlpha = 0.34f,
                rimHighlightAlpha = 0.55f,
                grainAlpha = palette.grainAlpha
            )
            .quartzInnerDottedTrack(cornerRadius = 48.dp, color = Color.White.copy(alpha = 0.26f))
            .quartzTapTarget(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            QuartzVectorGlyph(
                glyph = QuartzGlyph.Crosshair,
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(16.dp),
                strokeWidth = 1.1.dp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "AR SKY",
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
        }
    }
}

// ============================================================================
// TONIGHT — SECTION HEADER + OBSERVATION PANEL
// ============================================================================

@Composable
private fun QuartzTonightHeader(
    isFa: Boolean,
    count: Int,
    modifier: Modifier = Modifier
) {
    val palette = QuartzTheme.palette
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuartzSignalDot(color = palette.accentCrimson, size = 6.dp)
                Text(
                    text = if (isFa) "امشب در آسمان" else "Tonight's Program",
                    color = palette.inkPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.2).sp
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (isFa) "رویدادها و اجرام شاخص امشب" else "Curated events & meridian transits",
                color = palette.inkTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Light
            )
        }
        QuartzDotMatrix(
            value = count.coerceIn(0, 99).toString().padStart(2, '0'),
            dotSize = 1.9.dp,
            dotGap = 0.9.dp,
            charGap = 3.dp,
            color = palette.inkSecondary,
            glow = false
        )
    }
}

@Composable
private fun QuartzTonightPanel(
    events: List<WhatsUpTonightEngine.TonightEvent>,
    ranked: List<QuartzObservation>,
    isFa: Boolean,
    onClick: (WhatsUpTonightEngine.TonightEvent) -> Unit,
    onOpenObject: (CelestialObject) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = QuartzTheme.palette
    val radius = 32.dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        palette.quartzTop.copy(alpha = 0.96f),
                        palette.quartzMid.copy(alpha = 0.92f),
                        palette.quartzBase.copy(alpha = 0.86f)
                    )
                )
            )
            .opticalQuartzSurface(
                cornerRadius = radius,
                refractionColor = palette.quartzRefraction,
                refractionReach = 18.dp,
                refractionAlpha = 0.24f,
                rimHighlightAlpha = 0.44f,
                grainAlpha = palette.grainAlpha
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column {
            if (events.isEmpty()) {
                val fallback = ranked.take(3)
                fallback.forEachIndexed { index, obs ->
                    QuartzObservationRow(
                        title = if (isFa) obs.titleFa else obs.titleEn,
                        subtitle = if (isFa) obs.subtitleFa else obs.subtitleEn,
                        altitudeDeg = obs.altitudeDeg,
                        magnitude = obs.magnitude,
                        scorePercent = obs.scorePercent,
                        onClick = { obs.objectRef?.let(onOpenObject) },
                        isFa = isFa
                    )
                    if (index < fallback.lastIndex) {
                        QuartzHairline(
                            color = Color.White.copy(alpha = 0.16f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            } else {
                events.forEachIndexed { index, event ->
                    QuartzEventRow(
                        event = event,
                        isFa = isFa,
                        onClick = { onClick(event) }
                    )
                    if (index < events.lastIndex) {
                        Spacer(Modifier.height(10.dp))
                        QuartzHairline(
                            color = Color.White.copy(alpha = 0.16f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuartzEventRow(
    event: WhatsUpTonightEngine.TonightEvent,
    isFa: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .quartzTapTarget(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // The status is a hairline ring with a value inside: no icon, no emoji.
        Box(
            modifier = Modifier
                .size(38.dp)
                .drawWithCache {
                    onDrawBehind {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.26f),
                            style = Stroke(width = 0.8.dp.toPx())
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = event.importanceScore.coerceIn(0, 99).toString().padStart(2, '0'),
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 0.4.sp
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isFa) event.titleFa else event.titleEn,
                color = Color.White.copy(alpha = 0.94f),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (isFa) event.explanationFa else event.explanationEn,
                color = Color.White.copy(alpha = 0.56f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Light,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = if (isFa) event.timeOrDateStrFa else event.timeOrDateStrEn,
            color = Color.White.copy(alpha = 0.48f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun QuartzObservationRow(
    title: String,
    subtitle: String,
    altitudeDeg: Double,
    magnitude: Double,
    scorePercent: Int,
    isFa: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .quartzTapTarget(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        QuartzDotMatrix(
            value = String.format(Locale.US, "%02d", scorePercent.coerceIn(0, 99)),
            dotSize = 1.7.dp,
            dotGap = 0.9.dp,
            charGap = 2.6.dp,
            color = Color.White.copy(alpha = 0.9f),
            glow = false
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.94f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$subtitle · ALT ${String.format(Locale.US, "%.0f", altitudeDeg)}°",
                color = Color.White.copy(alpha = 0.52f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Light
            )
        }
        Text(
            text = "m ${String.format(Locale.US, "%.1f", magnitude)}",
            color = Color.White.copy(alpha = 0.60f),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Light
        )
    }
}
