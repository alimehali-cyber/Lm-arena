package com.alijafari.red.astronomy.ui.quartz

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.astro_engine.SatelliteEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteItem
import com.alijafari.red.astronomy.astro_engine.SatelliteLiveState
import com.alijafari.red.astronomy.astro_engine.WorldGeographyData
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.TimeMachineMode
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

private const val QUARTZ_SAT_REFRESH_MS = 10_000L

/**
 * Live state of every catalogue satellite at one instant, computed once per instant.
 */
private data class QuartzSatelliteSample(
    val states: List<SatelliteLiveState>,
    val subLonDeg: Double,
    val subLatDeg: Double
)

private fun sampleSatellites(atMs: Long, uiState: MainUiState): QuartzSatelliteSample {
    val states = SatelliteCatalog.getVisibleList().map { sat ->
        SatelliteEngine.calculateSatelliteState(
            satellite = sat,
            timestampMs = atMs,
            userLatDeg = uiState.userLocation.latitude,
            userLonDeg = uiState.userLocation.longitude,
            userAltMeters = uiState.userLocation.elevationMeters
        )
    }
    val sub = SatelliteEngine.calculateSubSolarPoint(atMs)
    return QuartzSatelliteSample(states, sub.lonDeg, sub.latDeg)
}

/**
 * "OBSIDIAN QUARTZ & CRIMSON TELEMETRY" — Orbital tracking.
 *
 * A smoked-quartz chart plate carries a hand-plotted equirectangular Earth (the app's own coastline
 * dataset), the live sub-solar terminator, the selected satellite's past/future ground track and the
 * observer's meridian. Under it: the orbital readouts as dot-matrix capsules, and the catalogue as a
 * horizontally scrubbed row of hairline chips.
 */
@Composable
fun QuartzSatellitesScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = QuartzTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN
    val satellites = remember { SatelliteCatalog.getVisibleList() }
    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    val selected = satellites.getOrNull(selectedIndex) ?: satellites.first()

    val liveTimeMs by viewModel.skyLiveClock.timeMs.collectAsState()
    val isSimulation = uiState.timeMachineState.mode == TimeMachineMode.SIMULATION
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isSimulation) {
        while (!isSimulation) {
            tick = System.currentTimeMillis()
            kotlinx.coroutines.delay(QUARTZ_SAT_REFRESH_MS)
        }
    }
    val instantMs = if (isSimulation) uiState.timeMachineState.simulationTimeMs else maxOf(tick, liveTimeMs)

    // SGP4 sampling is real numerical work (every catalogue satellite, plus a 46-point ground track),
    // so each instant is computed once on a background thread and reused until the clock advances.
    // Until the first snapshot lands the plate draws its empty state rather than blocking a frame.
    val sample by produceState<QuartzSatelliteSample?>(initialValue = null, instantMs, uiState.userLocation) {
        value = withContext(Dispatchers.Default) { sampleSatellites(instantMs, uiState) }
    }
    val groundTrack by produceState<List<Pair<Double, Double>>>(initialValue = emptyList(), instantMs, selected) {
        value = withContext(Dispatchers.Default) {
            SatelliteEngine.calculateGroundTrack(
                satellite = selected,
                currentTimestampMs = instantMs,
                tle = SatelliteEngine.getEffectiveTle(selected)
            )
        }
    }
    val nowIndex = (45 / 3).coerceIn(0, (groundTrack.size - 1).coerceAtLeast(0))
    val selectedState = sample?.states?.firstOrNull { it.satellite.id == selected.id }
        ?: sample?.states?.firstOrNull()
    val isSunlit = selectedState?.topocentric?.isSunlit == true

    // Local altitude arc for the next pass window (the "waveform" of this screen), sampled off the
    // main thread with the rest of the SGP4 work.
    val altitudeArc by produceState<List<Float>>(
        initialValue = emptyList(),
        instantMs,
        selected,
        uiState.userLocation
    ) {
        value = withContext(Dispatchers.Default) {
            val (_, future) = SatelliteEngine.generateOrbitTrajectory(
                satellite = selected,
                currentTimestampMs = instantMs,
                userLatDeg = uiState.userLocation.latitude,
                userLonDeg = uiState.userLocation.longitude,
                userAltMeters = uiState.userLocation.elevationMeters,
                pastMinutes = 0,
                futureMinutes = 100,
                stepSeconds = 200,
                tle = SatelliteEngine.getEffectiveTle(selected)
            )
            future.map { it.altitudeDeg.toFloat() }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .testTag("quartz_satellites_scroll"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- Editorial header ------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = if (isFa) "ردیابی مدار" else "Orbital Tracking",
                    color = palette.inkPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-0.4).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isFa) {
                        "گذرهای زنده، زمین‌مسیر و ترمیناتور"
                    } else {
                        "Live passes · ground track · terminator"
                    },
                    color = palette.inkTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                QuartzSignalDot(
                    color = if (isSunlit) palette.accentAmber else palette.inkTertiary,
                    size = 6.dp,
                    glowing = isSunlit
                )
                Text(
                    text = if (isSunlit) {
                        if (isFa) "آفتاب‌خورده" else "SUNLIT"
                    } else {
                        if (isFa) "در سایه" else "ECLIPSED"
                    },
                    color = palette.inkSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.4.sp
                )
            }
        }

        // ---- Chart plate -----------------------------------------------------------
        QuartzOrbitChartPlate(
            sample = sample,
            selectedSatellite = selected,
            groundTrack = groundTrack,
            nowIndex = nowIndex,
            observerLat = uiState.userLocation.latitude,
            observerLon = uiState.userLocation.longitude,
            palette = palette,
            modifier = Modifier.testTag("quartz_orbit_chart")
        )

        // ---- Orbital telemetry capsules --------------------------------------------
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuartzReadoutCapsule(
                    title = if (isFa) "ارتفاع" else "Altitude",
                    value = String.format(Locale.US, "%d", (selectedState?.topocentric?.satAltKm ?: 0.0).roundToInt()),
                    unit = "km",
                    accent = palette.accentCobalt,
                    modifier = Modifier.weight(1f)
                )
                QuartzReadoutCapsule(
                    title = if (isFa) "سرعت" else "Velocity",
                    value = String.format(Locale.US, "%.1f", selectedState?.topocentric?.velocityKmS ?: 0.0),
                    unit = "km/s",
                    accent = palette.accentRose,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuartzReadoutCapsule(
                    title = if (isFa) "فاصله" else "Range",
                    value = String.format(Locale.US, "%d", (selectedState?.topocentric?.rangeKm ?: 0.0).roundToInt()),
                    unit = "km",
                    accent = palette.accentAmber,
                    modifier = Modifier.weight(1f)
                )
                QuartzReadoutCapsule(
                    title = if (isFa) "قدر" else "Magnitude",
                    value = String.format(Locale.US, O_MAG_FORMAT, abs(selectedState?.apparentMagnitude ?: 0.0)),
                    unit = if ((selectedState?.apparentMagnitude ?: 0.0) < 0) "-m" else "+m",
                    accent = palette.accentCobalt,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ---- Altitude arc: the next pass, as a hairline waveform -------------------
        QuartzAltitudeArcCard(
            arc = altitudeArc,
            state = selectedState,
            isFa = isFa,
            palette = palette
        )

        // ---- Catalogue scrubber ----------------------------------------------------
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            itemsIndexed(satellites) { index, sat ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.14f) else Color.Transparent
                        )
                        .drawWithCache {
                            onDrawBehind {
                                drawRoundRect(
                                    color = Color.White.copy(alpha = if (isSelected) 0.55f else 0.22f),
                                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                                    style = Stroke(width = 0.8.dp.toPx())
                                )
                            }
                        }
                        .quartzTapTarget(onClick = { selectedIndex = index })
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        if (isSelected) {
                            QuartzSignalDot(color = Color.White, size = 5.dp, glowing = false)
                        }
                        Text(
                            text = (if (isFa) sat.nameFa else sat.nameEn).uppercase(Locale.ROOT),
                            color = Color.White.copy(alpha = if (isSelected) 0.94f else 0.52f),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Light,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(96.dp))
    }
}

private const val O_MAG_FORMAT = "%.1f"

// ============================================================================
// CHART PLATE — smoked quartz, hairlines, one real ground track
// ============================================================================

@Composable
private fun QuartzOrbitChartPlate(
    sample: QuartzSatelliteSample?,
    selectedSatellite: SatelliteItem,
    groundTrack: List<Pair<Double, Double>>,
    nowIndex: Int,
    observerLat: Double,
    observerLon: Double,
    palette: QuartzPalette,
    modifier: Modifier = Modifier
) {
    val radius = 32.dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        palette.quartzTop.copy(alpha = 0.97f),
                        palette.quartzMid.copy(alpha = 0.93f),
                        Color(0xFF101820).copy(alpha = 0.94f)
                    )
                )
            )
            .opticalQuartzSurface(
                cornerRadius = radius,
                refractionColor = palette.quartzRefraction,
                refractionReach = 20.dp,
                refractionAlpha = 0.26f,
                rimHighlightAlpha = 0.46f,
                grainAlpha = palette.grainAlpha
            )
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (groundTrack.isEmpty()) "GROUND TRACK" else "GROUND TRACK // $nowIndex",
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuartzVectorGlyph(
                        glyph = QuartzGlyph.Plus,
                        color = Color.White.copy(alpha = 0.34f),
                        modifier = Modifier.size(9.dp),
                        strokeWidth = 0.9.dp
                    )
                    QuartzVectorGlyph(
                        glyph = QuartzGlyph.Crosshair,
                        color = Color.White.copy(alpha = 0.34f),
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 0.9.dp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.05f)
                    .testTag("quartz_earth_chart")
            ) {
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f) return@Canvas

                fun mapX(lon: Double): Float = (((lon + 180.0) / 360.0) * w).toFloat()
                fun mapY(lat: Double): Float = (((90.0 - lat) / 180.0) * h).toFloat()

                // 1. Ocean plate: a deep obsidian slab inside the card.
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0B1116), Color(0xFF0A0E13))
                    ),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                )

                // 2. Night side: the half of the globe turned away from the sun.
                val subLon = sample?.subLonDeg ?: 0.0
                val subLat = sample?.subLatDeg ?: 0.0
                val terminator = Path().apply {
                    moveTo(mapX(subLon + 90.0), 0f)
                    lineTo(w, 0f)
                    lineTo(w, h)
                    lineTo(mapX(subLon - 90.0), h)
                    close()
                }
                drawPath(terminator, Color.Black.copy(alpha = 0.42f))

                // 3. Graticule: 30° meridians and parallels as hairlines.
                for (lon in -150..150 step 30) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.06f),
                        start = Offset(mapX(lon.toDouble()), 0f),
                        end = Offset(mapX(lon.toDouble()), h),
                        strokeWidth = 0.6.dp.toPx()
                    )
                }
                for (lat in -60..60 step 30) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.06f),
                        start = Offset(0f, mapY(lat.toDouble())),
                        end = Offset(w, mapY(lat.toDouble())),
                        strokeWidth = 0.6.dp.toPx()
                    )
                }
                // Equator, brighter.
                drawLine(
                    color = Color.White.copy(alpha = 0.14f),
                    start = Offset(0f, h / 2f),
                    end = Offset(w, h / 2f),
                    strokeWidth = 0.75.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 6f))
                )

                // 4. Landmasses: filled with a graphite wash, outlined with a 0.8.dp hairline.
                // WorldGeographyData stores unit paths (0..1); one matrix scales them to the plate.
                val unitToPlate = Matrix().apply { scale(w, h) }
                for (unitPath in WorldGeographyData.normalizedPaths) {
                    val land = Path().apply {
                        addPath(unitPath)
                        transform(unitToPlate)
                    }
                    drawPath(land, Color(0xFF25303A).copy(alpha = 0.92f))
                    drawPath(
                        land,
                        Color.White.copy(alpha = 0.20f),
                        style = Stroke(width = 0.8.dp.toPx())
                    )
                }

                // 5. Ground track: past solid, future dotted, with the live node as a loupe.
                if (groundTrack.size > 1) {
                    val past = Path()
                    val future = Path()
                    groundTrack.forEachIndexed { index, (lat, lon) ->
                        val x = mapX(lon)
                        val y = mapY(lat)
                        when {
                            index == 0 -> past.moveTo(x, y)
                            index <= nowIndex -> past.lineTo(x, y)
                            index == nowIndex + 1 -> {
                                future.moveTo(mapX(groundTrack[nowIndex].second), mapY(groundTrack[nowIndex].first))
                                future.lineTo(x, y)
                            }
                            else -> future.lineTo(x, y)
                        }
                    }
                    drawPath(
                        future,
                        Color.White.copy(alpha = 0.55f),
                        style = Stroke(
                            width = 1.1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 6f)),
                            cap = StrokeCap.Round
                        )
                    )
                    drawPath(
                        past,
                        Color.White.copy(alpha = 0.22f),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawPath(
                        past,
                        Color.White,
                        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    val node = groundTrack.getOrNull(nowIndex)
                    if (node != null) {
                        val nx = mapX(node.second)
                        val ny = mapY(node.first)
                        drawCircle(Color.White.copy(alpha = 0.18f), radius = 10.dp.toPx(), center = Offset(nx, ny))
                        drawCircle(
                            Color.White.copy(alpha = 0.5f),
                            radius = 10.dp.toPx(),
                            center = Offset(nx, ny),
                            style = Stroke(width = 0.8.dp.toPx())
                        )
                        drawCircle(Color.White, radius = 2.6.dp.toPx(), center = Offset(nx, ny))
                    }
                }

                // 6. Observer meridian: a crimson hairline with a crosshair.
                val ox = mapX(observerLon)
                val oy = mapY(observerLat)
                drawLine(
                    color = palette.accentCrimson.copy(alpha = 0.55f),
                    start = Offset(ox, 0f),
                    end = Offset(ox, h),
                    strokeWidth = 0.8.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 5f))
                )
                drawCircle(
                    color = palette.accentCrimson,
                    radius = 3.dp.toPx(),
                    center = Offset(ox, oy)
                )
                drawCircle(
                    color = palette.accentCrimson.copy(alpha = 0.5f),
                    radius = 7.dp.toPx(),
                    center = Offset(ox, oy),
                    style = Stroke(width = 0.8.dp.toPx())
                )

                // 7. Sub-solar point: a small warm ring (the sun's footprint).
                if (sample != null) {
                    drawCircle(
                        color = palette.accentAmber.copy(alpha = 0.9f),
                        radius = 2.4.dp.toPx(),
                        center = Offset(mapX(subLon), mapY(subLat))
                    )
                    drawCircle(
                        color = palette.accentAmber.copy(alpha = 0.35f),
                        radius = 9.dp.toPx(),
                        center = Offset(mapX(subLon), mapY(subLat)),
                        style = Stroke(width = 0.7.dp.toPx())
                    )
                }

                // 8. Plate hairline border + corner calibration marks.
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.14f),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                    style = Stroke(width = 0.75.dp.toPx())
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "90°W     0°     90°E",
                    color = Color.White.copy(alpha = 0.30f),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "TLE AGE " + String.format(
                        Locale.US,
                        "%.1f",
                        SatelliteEngine.getEffectiveTleInfo(selectedSatellite).epochAgeDays
                    ) + "d",
                    color = Color.White.copy(alpha = 0.30f),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}

// ============================================================================
// READOUT CAPSULES & ALTITUDE ARC
// ============================================================================

@Composable
private fun QuartzReadoutCapsule(
    title: String,
    value: String,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val palette = QuartzTheme.palette
    Box(
        modifier = modifier
            .height(94.dp)
            .clip(RoundedCornerShape(50))
            .background(
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.78f), Color(0xFF151A20).copy(alpha = 0.95f)),
                    center = Offset(110f, 28f),
                    radius = 260f
                )
            )
            .opticalQuartzSurface(
                cornerRadius = 47.dp,
                refractionColor = Color.White,
                refractionReach = 16.dp,
                refractionAlpha = 0.30f,
                rimHighlightAlpha = 0.55f,
                grainAlpha = palette.grainAlpha
            )
            .quartzInnerDottedTrack(cornerRadius = 47.dp, color = Color.White.copy(alpha = 0.22f))
            .padding(horizontal = 18.dp, vertical = 13.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                maxLines = 1
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                QuartzDotMatrix(
                    value = value.filter { it.isDigit() || it == '.' },
                    dotSize = 2.05.dp,
                    dotGap = 1.05.dp,
                    charGap = 3.2.dp,
                    color = Color.White,
                    glow = false
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = unit,
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun QuartzAltitudeArcCard(
    arc: List<Float>,
    state: SatelliteLiveState?,
    isFa: Boolean,
    palette: QuartzPalette,
    modifier: Modifier = Modifier
) {
    val radius = 30.dp
    val reveal by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 240f),
        label = "quartz_arc"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF180309).copy(alpha = 0.96f),
                        Color(0xFF3A0518).copy(alpha = 0.94f),
                        Color(0xFF6B0A2D).copy(alpha = 0.92f)
                    )
                )
            )
            .opticalQuartzSurface(
                cornerRadius = radius,
                refractionColor = palette.velvetRefraction,
                refractionReach = 20.dp,
                refractionAlpha = 0.26f,
                rimHighlightAlpha = 0.48f,
                grainAlpha = palette.grainAlpha
            )
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isFa) "قوس گذر بعدی" else "Next Pass Arc",
                        color = Color.White.copy(alpha = 0.94f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = (if (isFa) state?.satellite?.nameFa else state?.satellite?.nameEn)
                            .orEmpty()
                            .uppercase(Locale.ROOT),
                        color = Color.White.copy(alpha = 0.44f),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.6.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    QuartzDotMatrix(
                        value = (state?.topocentric?.elevationDeg ?: 0.0).roundToInt().coerceAtLeast(0).toString(),
                        dotSize = 2.2.dp,
                        dotGap = 1.1.dp,
                        charGap = 3.6.dp,
                        color = Color.White,
                        glow = false
                    )
                    Text(
                        text = "°",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
            ) {
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f || arc.isEmpty()) return@Canvas
                val baseY = h * 0.94f
                // The dome is 90°: 0° at the horizon, 90° at the zenith.
                fun y(altDeg: Float): Float = baseY - (altDeg.coerceIn(0f, 90f) / 90f) * (h * 0.86f)

                drawLine(
                    color = Color.White.copy(alpha = 0.20f),
                    start = Offset(0f, baseY),
                    end = Offset(w, baseY),
                    strokeWidth = 0.8.dp.toPx()
                )
                for (grid in 1..2) {
                    val gy = y(grid * 30f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.10f),
                        start = Offset(0f, gy),
                        end = Offset(w, gy),
                        strokeWidth = 0.6.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f))
                    )
                }

                val path = Path()
                arc.forEachIndexed { index, alt ->
                    val x = (index.toFloat() / (arc.size - 1).coerceAtLeast(1)) * w
                    val yy = y(alt)
                    if (index == 0) path.moveTo(x, yy) else path.lineTo(x, yy)
                }
                drawPath(
                    path,
                    color = Color.White.copy(alpha = 0.16f * reveal),
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path,
                    color = Color.White.copy(alpha = 0.95f * reveal),
                    style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                )

                val peak = arc.maxOrNull() ?: 0f
                val peakIndex = arc.indexOf(peak)
                if (peakIndex >= 0) {
                    val px = (peakIndex.toFloat() / (arc.size - 1).coerceAtLeast(1)) * w
                    val py = y(peak)
                    drawLine(
                        color = Color.White.copy(alpha = 0.4f),
                        start = Offset(px, py),
                        end = Offset(px, baseY),
                        strokeWidth = 0.8.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 4f))
                    )
                    drawCircle(Color.White.copy(alpha = 0.18f), radius = 9.dp.toPx(), center = Offset(px, py))
                    drawCircle(Color.White, radius = 2.4.dp.toPx(), center = Offset(px, py))
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isFa) "اکنون" else "NOW",
                    color = Color.White.copy(alpha = 0.34f),
                    fontSize = 8.5.sp,
                    letterSpacing = 1.4.sp
                )
                Text(
                    text = "+100 MIN",
                    color = Color.White.copy(alpha = 0.34f),
                    fontSize = 8.5.sp,
                    letterSpacing = 1.4.sp
                )
            }
        }
    }
}

