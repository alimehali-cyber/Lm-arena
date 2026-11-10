package com.alijafari.red.astronomy.ui.obsidian

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asFrameworkPaint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.CoordinateEngine
import com.alijafari.red.astronomy.astro_engine.SunEngine
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.CalendarSystem
import com.alijafari.red.astronomy.domain.TimeMachineMode
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.home.HomeAstroKeys
import com.alijafari.red.astronomy.ui.home.HomeAstroMemo
import com.alijafari.red.astronomy.ui.home.MoonKey
import com.alijafari.red.astronomy.ui.home.rememberReadyTimeMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Obsidian Quartz & Crimson Telemetry — Home.
 *
 * The art-directed showcase of the language, drawn entirely on Canvas:
 *  - an editorial top bar with a LIVE/SYNCED signal dot,
 *  - a frameless **Velvet Crimson lens** carrying the Moon's illumination as a procedural
 *    5x7 LED dot-matrix readout, a poured/planned phase legend, a synthesizer pulse waveform
 *    whose pulses track the lunar age, and a vector micro-stepper that scrubs the lunar day,
 *  - a **Smoked Frosted Quartz** card with an interactive Gaussian altitude curve and a 7-day
 *    selector (drag the curve or tap a day; both are haptically indexed),
 *  - a 2x2 bento of **Refracting Capsule Pills** with 6.dp inset dashed inner tracks.
 *
 * Real telemetry only: the numbers come from `HomeAstroMemo` (ELP2000 Moon + Sun), not placeholders.
 */
@Composable
fun ObsidianHomeScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    val haptics = LocalHapticFeedback.current

    val liveTimeMs by viewModel.skyLiveClock.timeMs.collectAsState()
    val isSimulation = uiState.timeMachineState.mode == TimeMachineMode.SIMULATION
    val activeTimeMs = if (isSimulation) uiState.timeMachineState.simulationTimeMs else liveTimeMs

    // Same bucketing as the other Home variants: the Moon solve runs at most once a minute.
    val moonKeyMs = rememberReadyTimeMs(
        targetMs = HomeAstroKeys.bucketMs(activeTimeMs, isSimulation, HomeAstroKeys.MOON_REFRESH_MS),
        deferUntilReady = !isSimulation,
        maxDeferredStepMs = 2 * HomeAstroKeys.MOON_REFRESH_MS
    ) { HomeAstroKeys.moonKeys(it, uiState) }

    val jd = remember(moonKeyMs) { TimeEngine.getJulianDate(moonKeyMs) }
    val moonData = remember(jd, uiState.userLocation) {
        HomeAstroMemo.get(
            MoonKey(jd, uiState.userLocation.latitude, uiState.userLocation.longitude)
        )
    }
    val sunHoriz = remember(jd, uiState.userLocation) {
        val sunPos = SunEngine.calculatePosition(jd)
        val lastDeg = TimeEngine.getLAST(jd, uiState.userLocation.longitude)
        CoordinateEngine.equatorialToHorizontal(
            equatorial = CoordinateEngine.Equatorial(sunPos.raDeg, sunPos.decDeg),
            lastDeg = lastDeg,
            latitudeDeg = uiState.userLocation.latitude
        )
    }

    // The seven daily Moon altitudes behind the Gaussian curve. Each one is a full ELP2000 solve,
    // so they are computed once per bucket off the main thread — the curve fills in when ready
    // instead of stalling the first frame.
    val moonLocation = uiState.userLocation
    val weekAltitudes by produceState(
        initialValue = List(7) { 0.0 },
        moonLocation,
        jd
    ) {
        value = withContext(Dispatchers.Default) {
            List(7) { dayOffset ->
                HomeAstroMemo.get(
                    MoonKey(
                        jd + dayOffset,
                        moonLocation.latitude,
                        moonLocation.longitude
                    )
                ).altitudeDeg
            }
        }
    }

    // Staggered entrance choreography on a critically damped spring (never bouncy).
    val enterTransition = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enterTransition.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.86f, stiffness = 220f)
        )
    }

    val illumination = moonData.illuminationPercent.roundToInt().coerceIn(0, 100)
    val phaseLabel = if (isFa) moonData.phaseNameFa else moonData.phaseNameEn
    val isWaxing = moonData.ageDays <= 14.77

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("obsidian_home_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = enterTransition.value
                    translationY = (1f - enterTransition.value) * 36f
                }
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Minimal editorial top bar.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TELEMETRY  //  " + TimeEngine.formatDate(
                        timestampMs = activeTimeMs,
                        calendarSystem = uiState.calendarSystem,
                        isFa = false
                    ).uppercase(),
                    color = ObsidianTheme.palette.textPrimary.copy(alpha = 0.55f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.8.sp,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { viewModel.setShowSettingsDialog(true) }
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ObsidianTheme.palette.signalDot)
                    )
                    Text(
                        text = if (isSimulation) "SIMULATED" else "SYNCED",
                        color = ObsidianTheme.palette.textPrimary.copy(alpha = 0.65f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.6.sp
                    )
                }
            }

            // Engine legend: proves the screen is instrumentation, not a feed.
            ObsidianTelemetryRow(
                icon = ObsidianVectorIcon.CROSSHAIR,
                label = if (isFa) "موتور قمری ELP2000" else "LUNAR ENGINE  //  ELP2000",
                value = (if (isFa) "بورتل " else "BORTLE ") + uiState.bortleClass,
                tint = ObsidianTheme.palette.textPrimary.copy(alpha = 0.42f),
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            // Location line — the hairline micro-icon replaces a Material pin.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { viewModel.setShowLocationSelector(true) }
                    )
                    .testTag("obsidian_location_pill")
            ) {
                ObsidianVectorIconGlyph(
                    icon = ObsidianVectorIcon.CROSSHAIR,
                    modifier = Modifier.size(11.dp),
                    tint = ObsidianTheme.palette.textPrimary.copy(alpha = 0.5f),
                    strokeWidth = 1.1.dp
                )
                Text(
                    text = (if (isFa) uiState.userLocation.cityNameFa else uiState.userLocation.cityNameEn) +
                        "  ·  " + TimeEngine.formatTime24h(
                            timestampMs = activeTimeMs,
                            isFa = isFa,
                            timeZone = TimeEngine.resolveTimeZone(uiState.userLocation.timezoneId)
                        ),
                    color = ObsidianTheme.palette.textPrimary.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.9.sp,
                    maxLines = 1
                )
            }

            // 2. Hero lens: Velvet Crimson lunar intake.
            VelvetCrimsonLunarCard(
                illuminationPercent = illumination,
                phaseLabel = phaseLabel,
                isWaxing = isWaxing,
                ageDays = moonData.ageDays,
                distanceKm = moonData.distanceKm,
                dayOffset = uiState.moonDayOffset,
                isFa = isFa,
                onDayOffsetChange = { offset ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.setMoonDayOffset(offset)
                }
            )

            // 3. Secondary hero: smoked quartz altitude curve.
            SmokedQuartzAltitudeCard(
                isFa = isFa,
                weekAltitudes = weekAltitudes
            )

            // 4. 2x2 bento of refracting capsule pills.
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RefractingTelemetryPill(
                        title = if (isFa) "ارتفاع ماه" else "Moon Alt.",
                        value = moonData.altitudeDeg.roundToInt().toString(),
                        unit = "°",
                        delta = if (moonData.altitudeDeg >= 0) "+" else "-",
                        accentColor = ObsidianRose,
                        baseColor = Color(0xFF281924),
                        modifier = Modifier.weight(1f)
                    )
                    RefractingTelemetryPill(
                        title = if (isFa) "فاصله ماه" else "Distance",
                        value = (moonData.distanceKm / 1000.0).roundToInt().toString(),
                        unit = "Mm",
                        delta = "0",
                        accentColor = ObsidianCobalt,
                        baseColor = Color(0xFF192030),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RefractingTelemetryPill(
                        title = if (isFa) "خورشید" else "Sun Alt.",
                        value = sunHoriz.altitudeDeg.roundToInt().toString(),
                        unit = "°",
                        delta = if (sunHoriz.altitudeDeg >= 0) "+" else "-",
                        accentColor = ObsidianAmber,
                        baseColor = Color(0xFF2D1912),
                        modifier = Modifier.weight(1f)
                    )
                    RefractingActionPill(
                        onClick = { viewModel.setShowMoonWindow(true) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick-access micro-capsules: Lab / Satellites / AR Sky, all vector-drawn.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ObsidianMicroActionCapsule(
                    icon = ObsidianVectorIcon.LAB,
                    label = if (isFa) "آزمایشگاه" else "Lab",
                    onClick = { onNavigateToTab(0) },
                    modifier = Modifier.weight(1f),
                    testTag = "obsidian_quick_lab"
                )
                ObsidianMicroActionCapsule(
                    icon = ObsidianVectorIcon.SATELLITE,
                    label = if (isFa) "ماهواره‌ها" else "Satellites",
                    onClick = { onNavigateToTab(1) },
                    modifier = Modifier.weight(1f),
                    testTag = "obsidian_quick_satellites"
                )
                ObsidianMicroActionCapsule(
                    icon = ObsidianVectorIcon.AR,
                    label = if (isFa) "آسمان AR" else "AR Sky",
                    onClick = { onNavigateToTab(3) },
                    modifier = Modifier.weight(1f),
                    testTag = "obsidian_quick_ar"
                )
            }

            Spacer(modifier = Modifier.height(104.dp))
        }
    }
}

// ============================================================================
// HERO: FRAMELESS VELVET-CRIMSON LENS
// ============================================================================

@Composable
private fun VelvetCrimsonLunarCard(
    illuminationPercent: Int,
    phaseLabel: String,
    isWaxing: Boolean,
    ageDays: Double,
    distanceKm: Double,
    dayOffset: Float,
    isFa: Boolean,
    onDayOffsetChange: (Float) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val pulses = ageDays.roundToInt().coerceIn(1, 29)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "obsidianHeroScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(RoundedCornerShape(38.dp))
            .background(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF170109),
                        0.52f to Color(0xFF4B0420),
                        0.84f to Color(0xFF8C093B),
                        1.0f to Color(0xFFB81450)
                    ),
                    center = Offset(420f, 380f),
                    radius = 740f
                )
            )
            .opticalQuartzSurface(
                cornerRadius = 38.dp,
                innerRefractionColor = Color(0xFFFF7AA8).copy(alpha = 0.25f),
                rimHighlightAlpha = 0.52f
            )
            .clickable(interactionSource = interactionSource, indication = null) { }
            .padding(horizontal = 26.dp, vertical = 24.dp)
            .testTag("obsidian_hero_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isFa) "درخشندگی ماه" else "Lunar Illumination",
                    color = Color.White.copy(alpha = 0.94f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = phaseLabel,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.4.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DotMatrixNumber(
                        value = illuminationPercent.toString().padStart(2, '0'),
                        dotSize = 3.5.dp,
                        dotSpacing = 2.0.dp,
                        charSpacing = 7.dp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = if (isFa) "درصد روشنایی" else "percent lit",
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-0.1).sp
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    StatusLegendRow(
                        label = if (isFa) "فزاینده" else "Waxing",
                        isFilled = isWaxing
                    )
                    StatusLegendRow(
                        label = if (isFa) "کاهنده" else "Waning",
                        isFilled = !isWaxing
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                // Lunar-age LED array (one dot per lunar day, capped visually at 6).
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    for (i in 1..5) {
                        val filled = i <= (pulses / 6).coerceIn(1, 5)
                        Canvas(modifier = Modifier.size(7.dp)) {
                            if (filled) {
                                drawCircle(color = Color.White)
                            } else {
                                drawCircle(
                                    color = Color.White.copy(alpha = if (i == 5) 0.45f else 0.25f),
                                    style = Stroke(width = 1.1.dp.toPx())
                                )
                            }
                        }
                    }
                }

                SynthesizerPulseWaveform(
                    pulses = pulses,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .align(Alignment.BottomCenter)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = (distanceKm / 1000.0).roundToInt().toString(),
                        color = Color.White.copy(alpha = 0.96f),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1.2).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFa) "هزار کیلومتر" else "thousand km",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-0.2).sp
                    )
                }

                VectorMicroStepper(
                    onDecrement = {
                        if (dayOffset > -30f) onDayOffsetChange((dayOffset - 1f).coerceAtLeast(-30f))
                    },
                    onIncrement = {
                        if (dayOffset < 30f) onDayOffsetChange((dayOffset + 1f).coerceAtMost(30f))
                    },
                    onLongPressReset = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDayOffsetChange(0f)
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusLegendRow(label: String, isFilled: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(modifier = Modifier.size(6.dp)) {
            if (isFilled) {
                drawCircle(color = Color.White)
            } else {
                drawCircle(
                    color = Color.White.copy(alpha = 0.4f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = if (isFilled) 0.82f else 0.42f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Light
        )
    }
}

/** Precision synthesizer pulse train: smooth sine arches joined by thick lit troughs. */
@Composable
private fun SynthesizerPulseWaveform(
    pulses: Int,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    val animatedPulses by animateFloatAsState(
        targetValue = pulses.toFloat(),
        animationSpec = spring(
            dampingRatio = OBSIDIAN_SPRING_DAMPING,
            stiffness = OBSIDIAN_SPRING_STIFFNESS
        ),
        label = "obsidianPulses"
    )

    Canvas(modifier = modifier) {
        val baselineY = size.height * 0.88f
        val amplitude = size.height * 0.78f
        val archWidth = size.width * 0.066f
        val troughWidth = size.width * 0.052f

        val wavePath = Path().apply { moveTo(0f, baselineY) }
        val troughs = mutableListOf<Pair<Float, Float>>()

        var x = 0f
        val count = animatedPulses.toInt().coerceAtLeast(1)

        for (i in 0 until count) {
            wavePath.cubicTo(
                x + archWidth * 0.12f, baselineY - amplitude,
                x + archWidth * 0.88f, baselineY - amplitude,
                x + archWidth, baselineY
            )
            x += archWidth

            if (i < count - 1) {
                val startTrough = x
                val endTrough = x + troughWidth
                wavePath.lineTo(endTrough, baselineY)
                troughs.add(startTrough to endTrough)
                x = endTrough
            }
        }

        wavePath.lineTo(size.width, baselineY)

        // 1. Hairline continuous wave.
        drawPath(
            path = wavePath,
            color = tint.copy(alpha = 0.48f),
            style = Stroke(width = 1.15.dp.toPx(), cap = StrokeCap.Round)
        )

        // 2. Optical bloom + thick lit trough bars.
        val bloomPaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            alpha = 140
            strokeWidth = 4.dp.toPx()
            style = android.graphics.Paint.Style.STROKE
            maskFilter = android.graphics.BlurMaskFilter(
                8.dp.toPx(),
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }

        troughs.forEach { (startX, endX) ->
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawLine(startX, baselineY, endX, baselineY, bloomPaint)
            }
            drawLine(
                color = tint,
                start = Offset(startX, baselineY),
                end = Offset(endX, baselineY),
                strokeWidth = 3.2.dp.toPx(),
                cap = StrokeCap.Butt
            )
        }
    }
}

/** Custom vector micro-stepper (`←  ━  →`), haptic on every click. */
@Composable
private fun VectorMicroStepper(
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onLongPressReset: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDecrement()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            ObsidianVectorIconGlyph(
                icon = ObsidianVectorIcon.ARROW_LEFT,
                modifier = Modifier.size(13.dp, 8.dp),
                tint = Color.White.copy(alpha = 0.85f)
            )
        }

        Box(
            modifier = Modifier
                .width(13.dp)
                .height(2.2.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onLongPressReset
                )
        )

        Box(
            modifier = Modifier
                .size(26.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onIncrement()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            ObsidianVectorIconGlyph(
                icon = ObsidianVectorIcon.ARROW_RIGHT,
                modifier = Modifier.size(13.dp, 8.dp),
                tint = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

// ============================================================================
// SECONDARY HERO: SMOKED FROSTED QUARTZ GAUSSIAN ALTITUDE CURVE
// ============================================================================

@Composable
private fun SmokedQuartzAltitudeCard(
    isFa: Boolean,
    weekAltitudes: List<Double>
) {
    val haptics = LocalHapticFeedback.current
    val dayLabels = remember(isFa) {
        if (isFa) listOf("ی", "د", "س", "چ", "پ", "ج", "ش")
        else listOf("S", "M", "T", "W", "T", "F", "S")
    }
    var selectedDayIndex by remember { mutableIntStateOf(3) }

    val peakFraction by animateFloatAsState(
        targetValue = (selectedDayIndex + 0.5f) / 7f,
        animationSpec = spring(
            dampingRatio = OBSIDIAN_SPRING_DAMPING,
            stiffness = OBSIDIAN_SPRING_STIFFNESS
        ),
        label = "obsidianPeakShift"
    )
    val selectedAltitude = weekAltitudes.getOrElse(selectedDayIndex) { 0.0 }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(38.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ObsidianSmokedQuartz.copy(alpha = 0.86f),
                        Color(0xFF283845).copy(alpha = 0.82f),
                        ObsidianSlateRim.copy(alpha = 0.76f)
                    )
                )
            )
            .opticalQuartzSurface(
                cornerRadius = 38.dp,
                innerRefractionColor = Color.White.copy(alpha = 0.28f),
                rimHighlightAlpha = 0.5f
            )
            .padding(24.dp)
            .testTag("obsidian_curve_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isFa) "قوس ارتفاع ماه" else "Lunar Altitude Arc",
                        color = Color.White.copy(alpha = 0.94f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DotMatrixNumber(
                            value = selectedAltitude.roundToInt().toString(),
                            dotSize = 2.1.dp,
                            dotSpacing = 1.1.dp,
                            charSpacing = 3.5.dp
                        )
                        Text(
                            text = if (isFa) "درجه در روز انتخابی" else "deg on selected day",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }

                // Hairline circle with a vector diagonal arrow.
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .drawWithCache {
                            onDrawBehind {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.25f),
                                    style = Stroke(width = 0.8.dp.toPx())
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    ObsidianVectorIconGlyph(
                        icon = ObsidianVectorIcon.ARROW_DIAGONAL,
                        modifier = Modifier.size(10.dp),
                        tint = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GaussianTelemetryCurveCanvas(
                peakCenterFraction = peakFraction,
                onScrubDay = { newIdx ->
                    if (newIdx != selectedDayIndex) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedDayIndex = newIdx
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayLabels.forEachIndexed { idx, day ->
                    val isSelected = idx == selectedDayIndex
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .drawWithCache {
                                onDrawBehind {
                                    if (!isSelected) {
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.22f),
                                            style = Stroke(width = 0.8.dp.toPx())
                                        )
                                    }
                                }
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedDayIndex = idx
                            }
                            .testTag("obsidian_day_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            color = if (isSelected) Color(0xFF14181D) else Color.White.copy(alpha = 0.55f),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Light
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ObsidianVectorIconGlyph(
                    icon = ObsidianVectorIcon.ARROW_LEFT,
                    modifier = Modifier.size(9.dp, 6.dp),
                    tint = Color.White.copy(alpha = 0.5f),
                    strokeWidth = 1.1.dp
                )
                Text(
                    text = TimeEngine.formatDate(
                        timestampMs = System.currentTimeMillis(),
                        calendarSystem = CalendarSystem.SOLAR_HIJRI,
                        isFa = isFa
                    ),
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.2.sp
                )
                ObsidianVectorIconGlyph(
                    icon = ObsidianVectorIcon.ARROW_RIGHT,
                    modifier = Modifier.size(9.dp, 6.dp),
                    tint = Color.White.copy(alpha = 0.5f),
                    strokeWidth = 1.1.dp
                )
            }
        }
    }
}

@Composable
private fun GaussianTelemetryCurveCanvas(
    peakCenterFraction: Float,
    onScrubDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectHorizontalDragGestures { change, _ ->
                val fraction = (change.position.x / size.width).coerceIn(0f, 0.99f)
                onScrubDay((fraction * 7).toInt())
            }
        }
    ) {
        val w = size.width
        val h = size.height
        val baseLineY = h * 0.88f
        val midDottedY = h * 0.58f
        val peakAmplitude = h * 0.68f
        val sigma = w * 0.14f
        val mu = w * peakCenterFraction

        fun gaussianY(x: Float): Float {
            val exponent = -((x - mu).pow(2)) / (2f * sigma.pow(2))
            return baseLineY - peakAmplitude * exp(exponent)
        }

        // 1. Dotted equilibrium line.
        drawLine(
            color = Color.White.copy(alpha = 0.22f),
            start = Offset(0f, midDottedY),
            end = Offset(w, midDottedY),
            strokeWidth = 0.8.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
        )

        // 2. Gaussian spline: dotted tails, solid centre arch.
        val fullCurvePath = Path()
        val solidCenterPath = Path()
        val steps = 120
        val solidMinX = mu - sigma * 1.45f
        val solidMaxX = mu + sigma * 1.45f
        var solidStarted = false

        for (i in 0..steps) {
            val x = (i.toFloat() / steps) * w
            val y = gaussianY(x)
            if (i == 0) fullCurvePath.moveTo(x, y) else fullCurvePath.lineTo(x, y)

            if (x in solidMinX..solidMaxX) {
                if (!solidStarted) {
                    solidCenterPath.moveTo(x, y)
                    solidStarted = true
                } else {
                    solidCenterPath.lineTo(x, y)
                }
            }
        }

        drawPath(
            path = fullCurvePath,
            color = Color.White.copy(alpha = 0.38f),
            style = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 5f), 0f)
            )
        )
        drawPath(
            path = solidCenterPath,
            color = Color.White.copy(alpha = 0.95f),
            style = Stroke(width = 1.35.dp.toPx(), cap = StrokeCap.Round)
        )

        // 3. Peak node: frosted loupe halo + dotted drop line.
        val apexY = gaussianY(mu)
        drawLine(
            color = Color.White.copy(alpha = 0.55f),
            start = Offset(mu, apexY),
            end = Offset(mu, midDottedY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 4f), 0f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.16f),
            radius = 12.dp.toPx(),
            center = Offset(mu, apexY)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.45f),
            radius = 12.dp.toPx(),
            center = Offset(mu, apexY),
            style = Stroke(width = 0.8.dp.toPx())
        )
        drawCircle(
            color = Color.White,
            radius = 2.6.dp.toPx(),
            center = Offset(mu, apexY)
        )
    }
}

// ============================================================================
// BENTO: REFRACTING CAPSULE PILLS WITH INNER DOTTED TRACKS
// ============================================================================

@Composable
fun RefractingTelemetryPill(
    title: String,
    value: String,
    unit: String,
    delta: String,
    accentColor: Color,
    baseColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(94.dp)
            .clip(RoundedCornerShape(50))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.82f),
                        baseColor.copy(alpha = 0.92f)
                    ),
                    center = Offset(110f, 35f),
                    radius = 260f
                )
            )
            .opticalQuartzSurface(
                cornerRadius = 47.dp,
                innerRefractionColor = Color.White.copy(alpha = 0.32f),
                rimHighlightAlpha = 0.55f
            )
            .obsidianInnerDottedTrack(cornerRadius = 47.dp)
            .padding(horizontal = 18.dp, vertical = 13.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.3.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                maxLines = 1
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
                    DotMatrixNumber(
                        value = value,
                        dotSize = 2.1.dp,
                        dotSpacing = 1.1.dp,
                        charSpacing = 3.5.dp
                    )
                    Text(
                        text = unit,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Light
                    )
                }

                // Hairline delta badge.
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .drawWithCache {
                            onDrawBehind {
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.4f),
                                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                                    style = Stroke(width = 0.8.dp.toPx())
                                )
                            }
                        }
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = delta,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
    }
}

@Composable
fun RefractingActionPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(94.dp)
            .clip(RoundedCornerShape(50))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF0E0B0E),
                        Color(0xFF3E6378),
                        Color(0xFFDCEBF2)
                    )
                )
            )
            .opticalQuartzSurface(
                cornerRadius = 47.dp,
                innerRefractionColor = Color.White.copy(alpha = 0.35f),
                rimHighlightAlpha = 0.55f
            )
            .obsidianInnerDottedTrack(cornerRadius = 47.dp, alpha = 0.2f)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .testTag("obsidian_action_pill"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(14.dp)) {
            val half = size.width / 2f
            drawLine(
                Color.White.copy(alpha = 0.9f),
                Offset(0f, half),
                Offset(size.width, half),
                1.1.dp.toPx()
            )
            drawLine(
                Color.White.copy(alpha = 0.9f),
                Offset(half, 0f),
                Offset(half, size.height),
                1.1.dp.toPx()
            )
        }
    }
}

/** Small hairline capsule used for the Home quick-access row (vector glyph + label only). */
@Composable
fun ObsidianMicroActionCapsule(
    icon: ObsidianVectorIcon,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(ObsidianSmokedQuartz.copy(alpha = 0.82f))
            .opticalQuartzSurface(
                cornerRadius = 22.dp,
                innerRefractionColor = Color.White.copy(alpha = 0.26f),
                rimHighlightAlpha = 0.5f
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ObsidianVectorIconGlyph(
            icon = icon,
            modifier = Modifier.size(13.dp),
            tint = Color.White.copy(alpha = 0.8f),
            strokeWidth = 1.1.dp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Light,
            maxLines = 1
        )
    }
}
