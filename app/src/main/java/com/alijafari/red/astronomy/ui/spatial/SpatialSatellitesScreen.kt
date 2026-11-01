package com.alijafari.red.astronomy.ui.spatial

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.astro_engine.SatelliteCategory
import com.alijafari.red.astronomy.astro_engine.SatelliteEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteItem
import com.alijafari.red.astronomy.astro_engine.SatelliteLiveState
import com.alijafari.red.astronomy.astro_engine.StarlinkTrainManager
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.astro_engine.WorldGeographyData
import com.alijafari.red.astronomy.data.repository.TleRepository
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.screens.SatelliteDetailScreen
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * VisionOS Spatial Glassmorphism Orbital Satellite Tracker Screen.
 *
 * Combines a beveled squircle SGP4 ground-track radar window, a circular radial dot elevation
 * meter, pink-to-orange & blue-to-purple mesh telemetry pills with 5x7 LED dot-matrix readouts,
 * an orbital time-warp glow slider, and a modular 2-column satellite fleet Bento grid.
 */
@Composable
fun SpatialSatellitesScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val palette = SpatialTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN
    val context = LocalContext.current

    var realTimeNowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var timeOffsetHours by remember { mutableFloatStateOf(0f) }
    val currentSimulationMs = remember(realTimeNowMs, timeOffsetHours) {
        realTimeNowMs + (timeOffsetHours * 3_600_000L).toLong()
    }

    LaunchedEffect(timeOffsetHours) {
        while (abs(timeOffsetHours) < 0.01f) {
            delay(1000L)
            realTimeNowMs = System.currentTimeMillis()
        }
    }

    val tleRepo = remember { TleRepository.getInstance(context) }
    var detectedTrain by remember { mutableStateOf<SatelliteItem?>(null) }

    LaunchedEffect(Unit) {
        val initialTrain = StarlinkTrainManager.detectTrain(tleRepo.getStarlinkTles())
        detectedTrain = initialTrain
        withContext(Dispatchers.IO) {
            val updated = tleRepo.refreshTles()
            if (updated || initialTrain == null) {
                val refreshed = StarlinkTrainManager.detectTrain(tleRepo.getStarlinkTles())
                withContext(Dispatchers.Main) {
                    detectedTrain = refreshed
                }
            }
        }
    }

    var selectedCategory by remember { mutableStateOf(SatelliteCategory.ALL) }
    var selectedSatelliteId by remember { mutableStateOf("iss_zarya") }
    var activeDetailSatellite by remember { mutableStateOf<SatelliteItem?>(null) }

    val visibleSatellites = remember(detectedTrain) {
        SatelliteCatalog.getVisibleList(detectedTrain)
    }

    val filteredSatellites = remember(selectedCategory, visibleSatellites) {
        visibleSatellites.filter { sat ->
            when (selectedCategory) {
                SatelliteCategory.ALL -> true
                SatelliteCategory.ISS -> sat.category == SatelliteCategory.ISS
                SatelliteCategory.STARLINK -> sat.category == SatelliteCategory.STARLINK || sat.isTrain
                SatelliteCategory.HUBBLE -> sat.category == SatelliteCategory.HUBBLE
                SatelliteCategory.VISIBLE -> sat.isNakedEyeCandidate
            }
        }
    }

    val liveSatelliteStates = remember(filteredSatellites, currentSimulationMs, uiState.userLocation) {
        filteredSatellites.map { sat ->
            SatelliteEngine.calculateSatelliteState(
                satellite = sat,
                timestampMs = currentSimulationMs,
                userLatDeg = uiState.userLocation.latitude,
                userLonDeg = uiState.userLocation.longitude
            )
        }
    }

    val activeLiveState = remember(liveSatelliteStates, selectedSatelliteId, visibleSatellites, currentSimulationMs, uiState.userLocation) {
        liveSatelliteStates.find { it.satellite.id == selectedSatelliteId }
            ?: visibleSatellites.firstOrNull()?.let { fallback ->
                SatelliteEngine.calculateSatelliteState(
                    satellite = fallback,
                    timestampMs = currentSimulationMs,
                    userLatDeg = uiState.userLocation.latitude,
                    userLonDeg = uiState.userLocation.longitude
                )
            }
    }

    if (activeDetailSatellite != null) {
        SatelliteDetailScreen(
            satelliteItem = activeDetailSatellite!!,
            userLocation = uiState.userLocation,
            language = uiState.language,
            onBack = { activeDetailSatellite = null },
            simulationTimestampMs = currentSimulationMs
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 112.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFa) "پایش مداری فضایی" else "Spatial Orbit Deck",
                        style = RedTypographyTokens.sectionHeading.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = palette.textPrimary
                    )
                    Text(
                        text = if (isFa) "ردیابی زنده SGP4 و گذرهای مرئی" else "Real-time SGP4 propagation & pass radar",
                        style = RedTypographyTokens.caption.copy(fontSize = 12.sp),
                        color = palette.textSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpatialGlassCapsule(
                        isActive = abs(timeOffsetHours) < 0.05f,
                        activeGlowColor = SpatialNeonRose,
                        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                        elevation = 6.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (abs(timeOffsetHours) < 0.05f) SpatialMintGlow else SpatialWarmPeach)
                        )
                        Text(
                            text = if (abs(timeOffsetHours) < 0.05f) {
                                if (isFa) "زنده SGP4" else "LIVE SGP4"
                            } else {
                                if (isFa) "شبیه‌سازی" else "WARP"
                            },
                            style = RedTypographyTokens.badge.copy(fontSize = 10.5.sp),
                            color = palette.accentInk
                        )
                    }

                    SpatialGlowIconButton(
                        icon = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings),
                        onClick = { viewModel.setShowSettingsDialog(true) }
                    )
                }
            }
        }

        // 2. Spatial Ground-Track Viewport Window
        item {
            SpatialGroundTrackWindow(
                liveSatelliteStates = liveSatelliteStates,
                selectedSatellite = activeLiveState?.satellite,
                currentSimulationMs = currentSimulationMs,
                userLat = uiState.userLocation.latitude,
                userLon = uiState.userLocation.longitude,
                isFa = isFa,
                onSelectSatellite = { sat -> selectedSatelliteId = sat.id },
                onOpenDetail = { sat -> activeDetailSatellite = sat }
            )
        }

        // 3. Asymmetric Telemetry Bento Grid: Radial Elevation Dot Meter + Velocity & Altitude Mesh Pills
        if (activeLiveState != null) {
            item {
                SpatialTelemetryBentoRow(
                    liveState = activeLiveState,
                    isFa = isFa,
                    onOpenDetail = { activeDetailSatellite = activeLiveState.satellite }
                )
            }
        }

        // 4. Orbital Time-Warp Scrubber Bento Card
        item {
            val isLive = abs(timeOffsetHours) < 0.05f
            val offsetRaw = if (isLive) "0.0H" else String.format(
                Locale.US,
                "%s%.1fH",
                if (timeOffsetHours > 0f) "+" else "-",
                abs(timeOffsetHours)
            )
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
                            text = if (isFa) "اسکرابر زمان مدار" else "Orbital Time-Warp",
                            style = RedTypographyTokens.bodyPrimary.copy(fontSize = 14.sp),
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (isFa) "پیش‌بینی مسیر زیرماهواره‌ای (±۱۲ ساعت)" else "Propagate ground track ±12 hours",
                            style = RedTypographyTokens.caption.copy(fontSize = 11.5.sp),
                            color = palette.textSecondary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DotMatrixText(
                            text = if (isFa) TimeEngine.formatPersianNumbers(offsetRaw) else offsetRaw,
                            dotSize = 2.0.dp,
                            activeColor = palette.dotMatrixInk,
                            glowColor = SpatialNeonRose
                        )
                        SpatialGlowToggle(
                            checked = isLive,
                            onCheckedChange = { wantLive ->
                                timeOffsetHours = if (wantLive) 0f else 1.5f
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                SpatialGlowSlider(
                    value = timeOffsetHours,
                    onValueChange = { timeOffsetHours = it },
                    valueRange = -12f..12f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 5. Constellation Filter Capsules
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SpatialSectionHeader(
                    title = if (isFa) "ناوگان ماهواره‌ای" else "Active Constellation",
                    subtitle = if (isFa) "برای ردیابی مسیر روی کارت ضربه بزنید" else "Tap a card to lock ground track or inspect passes",
                    badgeText = if (isFa) TimeEngine.formatPersianNumbers("${liveSatelliteStates.size}") else "${liveSatelliteStates.size}"
                )

                val catOptions = listOf(
                    SatelliteCategory.ALL to (if (isFa) "همه" else "All Fleet"),
                    SatelliteCategory.ISS to (if (isFa) "ایستگاه فضایی" else "Space Stations"),
                    SatelliteCategory.STARLINK to (if (isFa) "استارلینک" else "Starlink"),
                    SatelliteCategory.HUBBLE to (if (isFa) "تلسکوپ‌ها" else "Telescopes"),
                    SatelliteCategory.VISIBLE to (if (isFa) "چشم غیرمسلح" else "Naked-Eye")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(catOptions) { (cat, title) ->
                        SpatialGlassCapsule(
                            isActive = selectedCategory == cat,
                            activeGlowColor = SpatialNeonRose,
                            elevation = 6.dp,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            onClick = { selectedCategory = cat }
                        ) {
                            Text(
                                text = title,
                                style = RedTypographyTokens.caption.copy(
                                    fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = if (selectedCategory == cat) palette.accentInk else palette.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // 6. 2-Column Satellite Fleet Bento Grid
        val rows = liveSatelliteStates.chunked(2)
        items(rows) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { state ->
                    SpatialSatelliteFleetCard(
                        state = state,
                        isSelected = state.satellite.id == activeLiveState?.satellite?.id,
                        isFa = isFa,
                        onSelect = { selectedSatelliteId = state.satellite.id },
                        onOpenDetail = { activeDetailSatellite = state.satellite },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SpatialGroundTrackWindow(
    liveSatelliteStates: List<SatelliteLiveState>,
    selectedSatellite: SatelliteItem?,
    currentSimulationMs: Long,
    userLat: Double,
    userLon: Double,
    isFa: Boolean,
    onSelectSatellite: (SatelliteItem) -> Unit,
    onOpenDetail: (SatelliteItem) -> Unit
) {
    val palette = SpatialTheme.palette
    val density = LocalDensity.current
    val windowShape = squircleShape(32.dp)

    val subSolarPoint = remember(currentSimulationMs / 30_000L) {
        SatelliteEngine.calculateSubSolarPoint(currentSimulationMs)
    }

    val trackPoints = remember(selectedSatellite, currentSimulationMs / 10_000L) {
        selectedSatellite?.let {
            SatelliteEngine.calculateGroundTrack(
                satellite = it,
                currentTimestampMs = currentSimulationMs
            )
        } ?: emptyList()
    }

    val rimBrush = remember(palette) {
        Brush.verticalGradient(
            listOf(palette.rimTopHighlight, palette.rimSideBorder, palette.rimBottomReflection)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(216.dp)
            .shadow(18.dp, windowShape, ambientColor = palette.ambientDropShadow, spotColor = palette.ambientDropShadow)
            .clip(windowShape)
            .drawWithContent {
                drawContent()
                val bevel = 2.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                        startY = 0f,
                        endY = bevel * 2.2f
                    ),
                    size = Size(size.width, bevel * 2.2f)
                )
            }
            .border(BorderStroke(1.dp, rimBrush), windowShape)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(liveSatelliteStates) {
                    detectTapGestures { tap ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val pxPerLon = w / 360f
                        val pxPerLat = h / 180f
                        var closest: SatelliteItem? = null
                        var bestDist = with(density) { 30.dp.toPx() }
                        for (st in liveSatelliteStates) {
                            val sx = (w / 2f) + st.topocentric.subLonDeg.toFloat() * pxPerLon
                            val sy = (h / 2f) - st.topocentric.subLatDeg.toFloat() * pxPerLat
                            val d = hypot(tap.x - sx, tap.y - sy)
                            if (d < bestDist) {
                                bestDist = d
                                closest = st.satellite
                            }
                        }
                        if (closest != null) onSelectSatellite(closest)
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val oceanBg = if (palette.isDark) Color(0xFF140E22) else Color(0xFFEBD7C9)
            val landFill = if (palette.isDark) Color(0xFF281D3D) else Color(0xFFF9EFE8)
            val landBorder = if (palette.isDark) Color(0x47FFB0D0) else Color(0x407A4B3A)
            val gridLine = if (palette.isDark) Color(0x18FFFFFF) else Color(0x183A2218)

            drawRect(color = oceanBg)

            val pxPerLon = w / 360f
            val pxPerLat = h / 180f
            fun mapX(lon: Double): Float = (w / 2f) + lon.toFloat() * pxPerLon
            fun mapY(lat: Double): Float = (h / 2f) - lat.toFloat() * pxPerLat

            for (lon in -120..120 step 60) {
                drawLine(gridLine, Offset(mapX(lon.toDouble()), 0f), Offset(mapX(lon.toDouble()), h), 1f)
            }
            for (lat in -60..60 step 30) {
                drawLine(gridLine, Offset(0f, mapY(lat.toDouble())), Offset(w, mapY(lat.toDouble())), 1f)
            }

            // World Continents
            drawContext.canvas.save()
            drawContext.canvas.scale(w, h)
            val strokeW = (1.0f / w).coerceIn(0.0005f, 0.02f)
            for (unitPath in WorldGeographyData.normalizedPaths) {
                drawPath(path = unitPath, color = landFill, style = Fill)
                drawPath(path = unitPath, color = landBorder, style = Stroke(width = strokeW))
            }
            drawContext.canvas.restore()

            // Astronomical Solar Terminator Shade
            val subLatRad = Math.toRadians(subSolarPoint.latDeg)
            val tanSubLat = tan(subLatRad)
            val nightPath = Path()
            if (subSolarPoint.latDeg >= 0) {
                nightPath.moveTo(mapX(-180.0), mapY(-90.0))
                nightPath.lineTo(mapX(180.0), mapY(-90.0))
                for (lon in 180 downTo -180 step 5) {
                    val dLonRad = Math.toRadians(lon - subSolarPoint.lonDeg)
                    val termLatDeg = Math.toDegrees(atan(-cos(dLonRad) / if (abs(tanSubLat) < 1e-4) 1e-4 else tanSubLat))
                    nightPath.lineTo(mapX(lon.toDouble()), mapY(termLatDeg))
                }
            } else {
                nightPath.moveTo(mapX(-180.0), mapY(90.0))
                nightPath.lineTo(mapX(180.0), mapY(90.0))
                for (lon in 180 downTo -180 step 5) {
                    val dLonRad = Math.toRadians(lon - subSolarPoint.lonDeg)
                    val termLatDeg = Math.toDegrees(atan(-cos(dLonRad) / if (abs(tanSubLat) < 1e-4) 1e-4 else tanSubLat))
                    nightPath.lineTo(mapX(lon.toDouble()), mapY(termLatDeg))
                }
            }
            nightPath.close()
            drawPath(
                path = nightPath,
                color = if (palette.isDark) Color(0x8807040F) else Color(0x302A1832)
            )

            // Selected Satellite Neon Ground-Track Curve
            if (trackPoints.isNotEmpty()) {
                val trackPath = Path()
                var first = true
                var prevLon: Double? = null
                for ((tLat, tLon) in trackPoints) {
                    if (prevLon != null && abs(tLon - prevLon) > 180.0) first = true
                    val tx = mapX(tLon)
                    val ty = mapY(tLat)
                    if (first) {
                        trackPath.moveTo(tx, ty)
                        first = false
                    } else {
                        trackPath.lineTo(tx, ty)
                    }
                    prevLon = tLon
                }
                // Outer neon rose aura
                drawPath(
                    path = trackPath,
                    color = SpatialNeonRose.copy(alpha = 0.28f),
                    style = Stroke(width = 7f)
                )
                drawPath(
                    path = trackPath,
                    color = SpatialNeonRose,
                    style = Stroke(
                        width = 2.4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )
                )
            }

            // Observer Pin
            val ux = mapX(userLon)
            val uy = mapY(userLat)
            drawCircle(SpatialMintGlow.copy(alpha = 0.32f), radius = 10.dp.toPx(), center = Offset(ux, uy))
            drawCircle(SpatialMintGlow, radius = 3.8.dp.toPx(), center = Offset(ux, uy))

            // Satellite Beacons
            for (st in liveSatelliteStates) {
                val sx = mapX(st.topocentric.subLonDeg)
                val sy = mapY(st.topocentric.subLatDeg)
                val isSel = st.satellite.id == selectedSatellite?.id
                if (isSel) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(SpatialNeonRose.copy(alpha = 0.65f), Color.Transparent),
                            center = Offset(sx, sy),
                            radius = 18.dp.toPx()
                        ),
                        radius = 18.dp.toPx(),
                        center = Offset(sx, sy)
                    )
                    drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(sx, sy))
                    drawCircle(SpatialNeonRoseVibrant, radius = 3.2.dp.toPx(), center = Offset(sx, sy))
                } else {
                    drawCircle(SpatialWarmPeach.copy(alpha = 0.85f), radius = 2.8.dp.toPx(), center = Offset(sx, sy))
                }
            }
        }

        // Floating Active Target Pill in Bottom Corner
        if (selectedSatellite != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomBetweenCenterCompat)
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpatialGlassCapsule(
                    isActive = true,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    onClick = { onOpenDetail(selectedSatellite) }
                ) {
                    Icon(
                        imageVector = Icons.Default.SatelliteAlt,
                        contentDescription = null,
                        tint = palette.accentInk,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isFa) selectedSatellite.nameFa else selectedSatellite.nameEn,
                        style = RedTypographyTokens.caption.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = palette.accentInk,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

private val Alignment.Companion.BottomBetweenCenterCompat: Alignment
    get() = BottomCenter

@Composable
private fun SpatialTelemetryBentoRow(
    liveState: SatelliteLiveState,
    isFa: Boolean,
    onOpenDetail: () -> Unit
) {
    val palette = SpatialTheme.palette
    val elevDeg = liveState.topocentric.elevationDeg
    val elevInt = elevDeg.roundToInt()
    val elevStr = run {
        val raw = "${if (elevInt >= 0) "+" else ""}${elevInt}°"
        if (isFa) TimeEngine.formatPersianNumbers(raw) else raw
    }
    val elevNorm = ((elevDeg + 90.0) / 180.0).toFloat().coerceIn(0f, 1f)

    val velRaw = String.format(Locale.US, "%.2f", liveState.topocentric.velocityKmS)
    val velDisplay = if (isFa) TimeEngine.formatPersianNumbers(velRaw) else velRaw

    val altRaw = "${liveState.topocentric.satAltKm.roundToInt()}KM"
    val altDisplay = if (isFa) TimeEngine.formatPersianNumbers(altRaw) else altRaw

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left Squircle Card: Horizon Elevation Circular Radial Dot Meter
        SpatialGlassCard(
            modifier = Modifier
                .weight(1.08f)
                .height(232.dp),
            cornerRadius = 32.dp,
            contentPadding = PaddingValues(15.dp),
            elevation = 14.dp,
            glowAccent = if (elevDeg > 0) SpatialMintGlow else SpatialNeonRose,
            onClick = onOpenDetail
        ) {
            Text(
                text = if (isFa) "ارتفاع افقی" else "ELEVATION RADAR",
                style = RedTypographyTokens.caption.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = palette.textTertiary
            )
            Text(
                text = if (elevDeg > 0.0) {
                    if (isFa) "بالای افق شما" else "Above Horizon"
                } else {
                    if (isFa) "زیر افق محلی" else "Below Horizon"
                },
                style = RedTypographyTokens.bodyPrimary.copy(fontSize = 13.5.sp),
                color = if (elevDeg > 0.0) palette.successInk else palette.textPrimary
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                SpatialRadialDotMeter(
                    progress = elevNorm,
                    centerValueText = elevStr,
                    subValueText = if (isFa) "آزیموت ${TimeEngine.formatPersianNumbers("${liveState.topocentric.azimuthDeg.roundToInt()}°")}"
                    else "AZ ${liveState.topocentric.azimuthDeg.roundToInt()}°",
                    statusLabel = if (isFa) "جزئیات گذرهای بعدی" else "Tap for Pass Predictions",
                    diameter = 112.dp,
                    activeColor = if (elevDeg > 0.0) SpatialMintGlow else SpatialNeonRose,
                    secondaryActiveColor = SpatialWarmPeach
                )
            }
        }

        // Right Column: Stacked Mesh Gradient Telemetry Pills
        Column(
            modifier = Modifier
                .weight(0.92f)
                .height(232.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Orbital Velocity Mesh Pill (Rose-to-Orange)
            SpatialMeshPill(
                preset = SpatialMeshPreset.ROSE_TO_ORANGE,
                cornerRadius = 28.dp,
                contentPadding = PaddingValues(13.dp),
                onClick = onOpenDetail,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
                        Text(
                            text = if (isFa) "سرعت مداری" else "VELOCITY",
                            style = RedTypographyTokens.caption.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xB823111C)
                        )
                        Icon(
                            imageVector = if (liveState.topocentric.isSunlit) Icons.Default.WbSunny else Icons.Outlined.NightsStay,
                            contentDescription = null,
                            tint = Color(0xFF23111C),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    DotMatrixText(
                        text = velDisplay,
                        dotSize = 2.4.dp,
                        dotGap = 0.9.dp,
                        charGap = 2.5.dp,
                        activeColor = Color(0xFF23111C),
                        glowColor = Color.White
                    )

                    Text(
                        text = if (isFa) "کیلومتر بر ثانیه" else "km/s orbital speed",
                        style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                        color = Color(0xCC23111C)
                    )
                }
            }

            // Orbital Altitude Mesh Pill (Blue-to-Purple)
            SpatialMeshPill(
                preset = SpatialMeshPreset.BLUE_TO_PURPLE,
                cornerRadius = 28.dp,
                contentPadding = PaddingValues(13.dp),
                onClick = onOpenDetail,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isFa) "ارتفاع مداری" else "ORBIT ALTITUDE",
                        style = RedTypographyTokens.caption.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White.copy(alpha = 0.80f)
                    )

                    DotMatrixText(
                        text = altDisplay,
                        dotSize = 2.3.dp,
                        dotGap = 0.9.dp,
                        charGap = 2.4.dp,
                        activeColor = Color.White,
                        glowColor = SpatialSkyBlue
                    )

                    val rangeKm = liveState.topocentric.rangeKm.roundToInt()
                    Text(
                        text = if (isFa) {
                            "فاصله خط دید: ${TimeEngine.formatPersianNumbers("$rangeKm")} km"
                        } else {
                            "Range $rangeKm km"
                        },
                        style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SpatialSatelliteFleetCard(
    state: SatelliteLiveState,
    isSelected: Boolean,
    isFa: Boolean,
    onSelect: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = SpatialTheme.palette
    val elevInt = state.topocentric.elevationDeg.roundToInt()
    val elevText = run {
        val raw = "${if (elevInt >= 0) "+" else ""}${elevInt}°"
        if (isFa) TimeEngine.formatPersianNumbers(raw) else raw
    }

    SpatialGlassCard(
        modifier = modifier,
        cornerRadius = 28.dp,
        contentPadding = PaddingValues(14.dp),
        elevation = if (isSelected) 14.dp else 8.dp,
        elevated = isSelected,
        glowAccent = if (isSelected) SpatialNeonRose else null,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DotMatrixText(
                text = elevText,
                dotSize = 1.9.dp,
                dotGap = 0.75.dp,
                charGap = 2.0.dp,
                activeColor = palette.dotMatrixInk,
                glowColor = if (elevInt > 0) SpatialMintGlow else SpatialNeonRose
            )

            SpatialGlowIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                onClick = onOpenDetail,
                size = 28.dp,
                iconSize = 14.dp,
                isActive = isSelected
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (isFa) state.satellite.nameFa else state.satellite.nameEn,
            style = RedTypographyTokens.bodyPrimary.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
            ),
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "NORAD ${if (isFa) TimeEngine.formatPersianNumbers(state.satellite.noradId.toString()) else state.satellite.noradId}",
            style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
            color = palette.textSecondary
        )
    }
}
