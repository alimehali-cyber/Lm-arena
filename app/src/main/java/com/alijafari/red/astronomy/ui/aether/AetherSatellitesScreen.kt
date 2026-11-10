package com.alijafari.red.astronomy.ui.aether

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.ISSEngine
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
import com.alijafari.red.astronomy.domain.CalendarSystem
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.notification.AstroNotificationManager
import com.alijafari.red.astronomy.notification.NotificationPermissionHelper
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.screens.MapTheme
import com.alijafari.red.astronomy.ui.screens.SatelliteDetailScreen
import com.alijafari.red.astronomy.ui.theme.LocalAppFontFamily
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

private fun isAetherNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/**
 * Aether Telemetry UI — Satellites & ISS Orbital Telemetry Screen:
 *
 * - Floating Status Capsule (`✦  SGP4 Live Orbit  •  ISS 418 km  ⚡`)
 * - Frameless Sapphire Jewel Orbital Map Hero Card (`36.dp` squircle, zero outer bezel,
 *   1.dp `specularRimBorder`, rolling Dot-Matrix ISS altitude counter, `● Visible / ○ Below`
 *   LED legend, interactive 2:1 Earth ground-track & solar terminator lens, and `← ━ →`
 *   micro-stepper scrubbing `±15m` orbital time with haptics)
 * - Asymmetric Bento Row: 60-Tick `AetherRadialDialCard` (`34.dp`) + stacked Pink & Amber
 *   `AetherGlowingMeshPillCard`s (`RoundedCornerShape(50)`)
 * - Frameless Frosted Orbital Time Scrubber (`±12h`, `satellite_time_scrubber_card`,
 *   `time_scrubber_slider`), 3-Day Visible Passes with `AetherOscilloscopeWaveformCanvas`,
 *   and Tracked Satellites Deck.
 */
@Composable
fun AetherSatellitesScreen(
    uiState: MainUiState,
    @Suppress("UNUSED_PARAMETER") viewModel: MainViewModel
) {
    val palette = AetherTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN
    val canvasFontFamily = LocalAppFontFamily.current
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    val isOnline = remember(context) { isAetherNetworkAvailable(context) }
    val density = LocalDensity.current

    val prefs = remember { context.getSharedPreferences("astro_app_prefs", Context.MODE_PRIVATE) }
    var isAutoAlertEnabled by remember {
        mutableStateOf(
            prefs.getBoolean(
                "auto_satellite_alerts_enabled",
                prefs.getBoolean("iss_auto_alerts_enabled", false)
            )
        )
    }

    var mapTheme by remember {
        val saved = prefs.getString("satellite_map_theme", MapTheme.AUTO.name)
        mutableStateOf(
            when (saved) {
                MapTheme.DARK.name -> MapTheme.DARK
                MapTheme.LIGHT.name -> MapTheme.LIGHT
                else -> MapTheme.AUTO
            }
        )
    }

    val activeMapDark = when (mapTheme) {
        MapTheme.DARK -> true
        MapTheme.LIGHT -> false
        MapTheme.AUTO -> uiState.themeMode != ThemeMode.LIGHT
    }
    val activeMapWine = mapTheme == MapTheme.AUTO && uiState.themeMode == ThemeMode.WINE

    var pendingNotificationAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingNotificationAction?.invoke()
            pendingNotificationAction = null
        } else {
            pendingNotificationAction = null
            Toast.makeText(
                context,
                if (isFa) "برای دریافت هشدارهای گذر ماهواره، لطفاً مجوز اعلان را فعال کنید."
                else "Notification permission is required to receive satellite pass alerts.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val executeWithNotificationPermission: (() -> Unit) -> Unit = { action ->
        if (NotificationPermissionHelper.hasPostNotificationPermission(context)) {
            action()
        } else {
            pendingNotificationAction = action
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var realTimeNowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var timeOffsetHours by remember { mutableFloatStateOf(0f) }
    val currentSimulationMs = remember(realTimeNowMs, timeOffsetHours) {
        realTimeNowMs + (timeOffsetHours * 3_600_000L).toLong()
    }

    LaunchedEffect(timeOffsetHours) {
        while (timeOffsetHours == 0f) {
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
                val newTrain = StarlinkTrainManager.detectTrain(tleRepo.getStarlinkTles())
                withContext(Dispatchers.Main) {
                    detectedTrain = newTrain
                }
            }
        }
    }

    var selectedCategory by remember { mutableStateOf(SatelliteCategory.ALL) }
    var selectedSatelliteId by remember { mutableStateOf<String?>("iss_zarya") }
    var activeDetailSatellite by remember { mutableStateOf<SatelliteItem?>(null) }
    var anchoredSatLabelState by remember { mutableStateOf<SatelliteLiveState?>(null) }

    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0.0f) }
    var panOffsetY by remember { mutableFloatStateOf(0.0f) }

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

    val selectedSatItem = remember(selectedSatelliteId, detectedTrain) {
        selectedSatelliteId?.let { id -> SatelliteCatalog.getById(id, detectedTrain) }
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

    val selectedLiveState = remember(liveSatelliteStates, selectedSatelliteId) {
        liveSatelliteStates.firstOrNull { it.satellite.id == selectedSatelliteId }
            ?: liveSatelliteStates.firstOrNull()
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

    val altInt = selectedLiveState?.topocentric?.satAltKm?.roundToInt() ?: 418
    val elevDeg = selectedLiveState?.topocentric?.elevationDeg ?: 0.0
    val azDeg = selectedLiveState?.topocentric?.azimuthDeg ?: 0.0
    val isAboveHorizon = elevDeg > 0.0

    val statusCapsuleText = remember(altInt, timeOffsetHours, isOnline, isFa) {
        val orbitMode = if (abs(timeOffsetHours) < 0.02f) {
            if (isFa) "مدار زنده SGP4" else "SGP4 Live Orbit"
        } else {
            val raw = String.format(Locale.US, "%+.1fh", timeOffsetHours)
            if (isFa) "اسکراب ${TimeEngine.formatPersianNumbers(raw)}" else "Scrub $raw"
        }
        val altLabel = if (isFa) {
            "${TimeEngine.formatPersianNumbers(altInt.toString())} km"
        } else {
            "$altInt km"
        }
        val netSuffix = if (!isOnline) (if (isFa) " • آفلاین" else " • Offline") else ""
        "$orbitMode  •  $altLabel$netSuffix"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RedSpacing.lg, vertical = RedSpacing.md)
            .testTag("satellite_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Floating Top Status Capsule + Alert Lens
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AetherFloatingStatusCapsule(
                text = statusCapsuleText,
                onClick = { timeOffsetHours = 0f }
            )

            AetherOpticalIconLens(
                icon = if (isAutoAlertEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                contentDescription = "Alerts",
                isActive = isAutoAlertEnabled,
                onClick = {
                    executeWithNotificationPermission {
                        isAutoAlertEnabled = !isAutoAlertEnabled
                        prefs.edit().putBoolean("auto_satellite_alerts_enabled", isAutoAlertEnabled).apply()
                    }
                },
                modifier = Modifier.testTag("satellite_alerts_toggle")
            )
        }

        // 2. Frameless Sapphire Jewel Orbital Map Hero Card (`36.dp` squircle, 1.dp specular rim)
        val altDigits = remember(altInt, isFa) {
            val raw = altInt.coerceIn(0, 9999).toString().padStart(3, '0')
            if (isFa) TimeEngine.formatPersianNumbers(raw) else raw
        }

        AetherJewelCard(
            modifier = Modifier.fillMaxWidth(),
            jewelTone = if (palette.isWine) AetherJewelTone.WINE_RUBY else AetherJewelTone.SAPPHIRE,
            cornerRadius = 36.dp,
            contentPadding = PaddingValues(22.dp),
            elevation = 28.dp
        ) {
            // Card Title + Visible / Below LED Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedLiveState?.let {
                        if (isFa) it.satellite.nameFa else it.satellite.nameEn
                    } ?: if (isFa) "ماهواره‌ها و مدار زنده" else "Satellites & Live Orbit",
                    style = RedTypographyTokens.sectionTitle.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.3).sp
                    ),
                    color = Color.White.copy(alpha = 0.96f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AetherLegendDotItem(
                        label = if (isFa) "مرئی" else "Visible",
                        filled = isAboveHorizon
                    )
                    AetherLegendDotItem(
                        label = if (isFa) "زیر افق" else "Below",
                        filled = !isAboveHorizon
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dot-Matrix Altitude Counter + Sub-Satellite Coordinates + 5 LED Dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    AetherRollingDotMatrixCounter(
                        valueText = altDigits,
                        dotSize = 2.65.dp,
                        dotGap = 1.0.dp,
                        charGap = 3.5.dp,
                        color = AetherGlowWhite,
                        glowColor = AetherSapphireCoreEdge
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.padding(top = 2.dp)) {
                        Text(
                            text = if (isFa) "ارتفاع مداری (کیلومتر)" else "Orbital Altitude (km)",
                            style = RedTypographyTokens.caption.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = Color.White.copy(alpha = 0.82f)
                        )
                        val subCoordsRaw = String.format(
                            Locale.US,
                            "%.1f°, %.1f°",
                            selectedLiveState?.topocentric?.subLatDeg ?: 0.0,
                            selectedLiveState?.topocentric?.subLonDeg ?: 0.0
                        )
                        Text(
                            text = if (isFa) TimeEngine.formatPersianNumbers(subCoordsRaw) else subCoordsRaw,
                            style = RedTypographyTokens.caption.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Light
                            ),
                            color = Color.White.copy(alpha = 0.58f)
                        )
                    }
                }

                val aboveCount = liveSatelliteStates.count { it.topocentric.elevationDeg > 0.0 }.coerceIn(1, 5)
                AetherLedDotArray(
                    totalDots = 5,
                    litCount = aboveCount
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive 2:1 Earth Ground-Track & Solar Terminator Viewport
            val viewportShape = RoundedCornerShape(24.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.0f)
                    .clip(viewportShape)
                    .specularRimBorder(cornerRadius = 24.dp)
            ) {
                val subSolarPoint = remember(currentSimulationMs) {
                    SatelliteEngine.calculateSubSolarPoint(currentSimulationMs)
                }
                var labelPosOnScreen by remember { mutableStateOf<Offset?>(null) }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                zoomScale = (zoomScale * zoom).coerceIn(1.0f, 6.0f)
                                if (zoomScale > 1.0f) {
                                    panOffsetX = (panOffsetX + pan.x).coerceIn(-300f * zoomScale, 300f * zoomScale)
                                    panOffsetY = (panOffsetY + pan.y).coerceIn(-150f * zoomScale, 150f * zoomScale)
                                } else {
                                    panOffsetX = 0f
                                    panOffsetY = 0f
                                }
                            }
                        }
                        .pointerInput(liveSatelliteStates, zoomScale, panOffsetX, panOffsetY) {
                            detectTapGestures { tapOffset ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                val pxPerDeg = w / 360.0f
                                val centerX = w / 2f
                                val centerY = h / 2f

                                fun getMapX(lon: Double): Float =
                                    centerX + (lon.toFloat() * pxPerDeg) * zoomScale + panOffsetX
                                fun getMapY(lat: Double): Float =
                                    centerY - (lat.toFloat() * pxPerDeg) * zoomScale + panOffsetY

                                var hitSat: SatelliteLiveState? = null
                                var bestDist = 42f
                                for (st in liveSatelliteStates) {
                                    val sx = getMapX(st.topocentric.subLonDeg)
                                    val sy = getMapY(st.topocentric.subLatDeg)
                                    val d = hypot(tapOffset.x - sx, tapOffset.y - sy)
                                    if (d < bestDist) {
                                        bestDist = d
                                        hitSat = st
                                    }
                                }
                                if (hitSat != null) {
                                    selectedSatelliteId = hitSat.satellite.id
                                    anchoredSatLabelState = hitSat
                                } else {
                                    anchoredSatLabelState = null
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val oceanColor = when {
                        activeMapWine -> Color(0xFF140209)
                        activeMapDark -> Color(0xFF03101D)
                        else -> Color(0xFFE4ECF7)
                    }
                    val landColor = when {
                        activeMapWine -> Color(0xFF36081A)
                        activeMapDark -> Color(0xFF0C2B47)
                        else -> Color(0xFFF8FAFC)
                    }
                    val landStroke = when {
                        activeMapWine -> Color(0x52FF94B2)
                        activeMapDark -> Color(0x4768C5F8)
                        else -> Color(0x330F172A)
                    }
                    val gridColor = if (activeMapDark) Color(0x16FFFFFF) else Color(0x140F172A)
                    val nightShadowColor = if (activeMapDark) Color(0xAD010308) else Color(0x2E0F172A)

                    drawRect(color = oceanColor)

                    val pxPerDeg = w / 360.0f
                    val centerX = w / 2f
                    val centerY = h / 2f

                    fun mapX(lon: Double): Float =
                        centerX + (lon.toFloat() * pxPerDeg) * zoomScale + panOffsetX
                    fun mapY(lat: Double): Float =
                        centerY - (lat.toFloat() * pxPerDeg) * zoomScale + panOffsetY

                    for (gLon in -150..150 step 30) {
                        val gx = mapX(gLon.toDouble())
                        if (gx in 0f..w) drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), 1f)
                    }
                    for (gLat in -60..60 step 30) {
                        val gy = mapY(gLat.toDouble())
                        if (gy in 0f..h) drawLine(gridColor, Offset(0f, gy), Offset(w, gy), 1f)
                    }
                    val eqY = mapY(0.0)
                    if (eqY in 0f..h) {
                        drawLine(Color.White.copy(alpha = 0.24f), Offset(0f, eqY), Offset(w, eqY), 1.3f)
                    }

                    val scaleX = (w * zoomScale).coerceAtLeast(0.001f)
                    val scaleY = (h * zoomScale).coerceAtLeast(0.001f)
                    val translateX = centerX * (1f - zoomScale) + panOffsetX
                    val translateY = centerY * (1f - zoomScale) + panOffsetY

                    drawContext.canvas.save()
                    drawContext.canvas.translate(translateX, translateY)
                    drawContext.canvas.scale(scaleX, scaleY)
                    val strokeW = ((if (activeMapDark) 1.2f else 1.0f) / scaleX).coerceIn(0.001f, 5.0f)
                    for (unitPath in WorldGeographyData.normalizedPaths) {
                        drawPath(path = unitPath, color = landColor, style = Fill)
                        drawPath(path = unitPath, color = landStroke, style = Stroke(width = strokeW))
                    }
                    drawContext.canvas.restore()

                    val subLatRad = Math.toRadians(subSolarPoint.latDeg)
                    val tanSubLat = tan(subLatRad)
                    val nightPath = Path()
                    val poleLat = if (subSolarPoint.latDeg >= 0) -90.0 else 90.0
                    nightPath.moveTo(mapX(-180.0), mapY(poleLat))
                    nightPath.lineTo(mapX(180.0), mapY(poleLat))
                    for (lon in 180 downTo -180 step 4) {
                        val dLonRad = Math.toRadians(lon - subSolarPoint.lonDeg)
                        val termLatDeg = Math.toDegrees(
                            atan(-cos(dLonRad) / if (abs(tanSubLat) < 1e-4) 1e-4 else tanSubLat)
                        )
                        nightPath.lineTo(mapX(lon.toDouble()), mapY(termLatDeg))
                    }
                    nightPath.close()
                    drawPath(path = nightPath, color = nightShadowColor)

                    for (city in SatelliteEngine.majorCityLights) {
                        val cLatRad = Math.toRadians(city.lat)
                        val dLonRad = Math.toRadians(city.lon - subSolarPoint.lonDeg)
                        val cosPsi = sin(cLatRad) * sin(subLatRad) + cos(cLatRad) * cos(subLatRad) * cos(dLonRad)
                        if (cosPsi < 0.0) {
                            val alpha = (abs(cosPsi) / 0.12f).coerceAtMost(1.0).toFloat()
                            val cx = mapX(city.lon)
                            val cy = mapY(city.lat)
                            if (cx in 0f..w && cy in 0f..h) {
                                drawCircle(
                                    color = AetherAmberMeshStart.copy(alpha = 0.78f * alpha),
                                    radius = city.sizeDp * zoomScale * 0.72f,
                                    center = Offset(cx, cy)
                                )
                            }
                        }
                    }

                    val userX = mapX(uiState.userLocation.longitude)
                    val userY = mapY(uiState.userLocation.latitude)
                    if (userX in 0f..w && userY in 0f..h) {
                        drawCircle(AetherGlowWhite.copy(alpha = 0.28f), radius = 10.dp.toPx(), center = Offset(userX, userY))
                        drawCircle(AetherGlowWhite, radius = 3.6.dp.toPx(), center = Offset(userX, userY))

                        val rawCity = if (isFa) uiState.userLocation.cityNameFa else uiState.userLocation.cityNameEn
                        val cityLabel = if (isFa) TimeEngine.formatPersianNumbers(rawCity) else rawCity
                        val textLayoutResult = textMeasurer.measure(
                            text = cityLabel,
                            style = TextStyle(
                                fontFamily = canvasFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (activeMapDark) Color.White else Color(0xFF0F172A)
                            )
                        )
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(userX - textLayoutResult.size.width / 2f, userY - 18.dp.toPx())
                        )
                    }

                    if (selectedSatItem != null) {
                        val trackPoints = SatelliteEngine.calculateGroundTrack(
                            satellite = selectedSatItem,
                            currentTimestampMs = currentSimulationMs
                        )
                        val trackPath = Path()
                        var isFirst = true
                        var prevLon: Double? = null
                        for ((tLat, tLon) in trackPoints) {
                            if (prevLon != null && abs(tLon - prevLon) > 180.0) isFirst = true
                            val tx = mapX(tLon)
                            val ty = mapY(tLat)
                            if (isFirst) {
                                trackPath.moveTo(tx, ty)
                                isFirst = false
                            } else {
                                trackPath.lineTo(tx, ty)
                            }
                            prevLon = tLon
                        }
                        drawPath(
                            path = trackPath,
                            color = AetherGlowWhite.copy(alpha = 0.24f),
                            style = Stroke(width = 5.5f)
                        )
                        drawPath(
                            path = trackPath,
                            color = AetherGlowWhite,
                            style = Stroke(
                                width = 2.0f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f))
                            )
                        )
                    }

                    for (liveState in liveSatelliteStates) {
                        val sat = liveState.satellite
                        val isSelected = sat.id == selectedSatelliteId
                        val sx = mapX(liveState.topocentric.subLonDeg)
                        val sy = mapY(liveState.topocentric.subLatDeg)
                        if (sx in -20f..(w + 20f) && sy in -20f..(h + 20f)) {
                            if (isSelected) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(AetherGlowWhite.copy(alpha = 0.72f), Color.Transparent),
                                        center = Offset(sx, sy),
                                        radius = 15.dp.toPx()
                                    ),
                                    radius = 15.dp.toPx(),
                                    center = Offset(sx, sy)
                                )
                            }
                            drawCircle(
                                color = if (isSelected) AetherGlowWhite else AetherPinkMeshStart,
                                radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx(),
                                center = Offset(sx, sy)
                            )
                        }
                    }

                    if (anchoredSatLabelState != null) {
                        val satState = anchoredSatLabelState!!
                        labelPosOnScreen = Offset(
                            mapX(satState.topocentric.subLonDeg),
                            mapY(satState.topocentric.subLatDeg)
                        )
                    } else {
                        labelPosOnScreen = null
                    }
                }

                SmallFloatingActionButton(
                    onClick = {
                        zoomScale = 1.0f
                        panOffsetX = 0f
                        panOffsetY = 0f
                    },
                    containerColor = Color.Black.copy(alpha = 0.52f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(32.dp)
                        .testTag("map_reset_view")
                ) {
                    Icon(Icons.Default.CenterFocusWeak, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                }

                if (anchoredSatLabelState != null && labelPosOnScreen != null) {
                    val satState = anchoredSatLabelState!!
                    val lPos = labelPosOnScreen!!
                    Box(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            modifier = Modifier
                                .offset(
                                    x = with(density) { (lPos.x - 75f).toDp() },
                                    y = with(density) { (lPos.y - 48f).toDp() }
                                )
                                .width(154.dp)
                                .clickable { activeDetailSatellite = satState.satellite }
                                .testTag("anchored_sat_label"),
                            shape = RedShapes.capsule,
                            color = Color(0xEB0B0D18),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isFa) satState.satellite.nameFa else satState.satellite.nameEn,
                                        style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Medium),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val altStr = String.format(Locale.US, "%.0f km", satState.topocentric.satAltKm)
                                    Text(
                                        text = if (isFa) TimeEngine.formatPersianNumbers(altStr) else altStr,
                                        style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Light, fontSize = 9.5.sp),
                                        color = Color.White.copy(alpha = 0.78f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Detail",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer: Simulation Clock + Micro-Stepper (`← ━ →`, ±15m)
            val simTimeLabel = remember(currentSimulationMs, uiState.calendarSystem, isFa) {
                val datePart = TimeEngine.formatDate(currentSimulationMs, uiState.calendarSystem, isFa)
                val timePart = TimeEngine.formatTime24h(currentSimulationMs, isFa)
                "$datePart  •  $timePart"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = simTimeLabel,
                    style = RedTypographyTokens.caption.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light
                    ),
                    color = Color.White.copy(alpha = 0.80f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                AetherMicroStepper(
                    onDecrement = { timeOffsetHours = (timeOffsetHours - 0.25f).coerceIn(-12f, 12f) },
                    onIncrement = { timeOffsetHours = (timeOffsetHours + 0.25f).coerceIn(-12f, 12f) }
                )
            }
        }

        // 3. Asymmetric Bento Row: 60-Tick Radial Dial Card + Stacked Pink/Amber Mesh Pill Cards
        val elevScoreText = remember(elevDeg, isFa) {
            val raw = String.format(Locale.US, "%+.0f°", elevDeg)
            if (isFa) TimeEngine.formatPersianNumbers(raw) else raw
        }
        val dialFraction = ((elevDeg.toFloat() + 90f) / 180f).coerceIn(0.08f, 1f)
        val azValueText = remember(azDeg, isFa) {
            val raw = String.format(Locale.US, "%.0f", azDeg)
            if (isFa) TimeEngine.formatPersianNumbers(raw) else raw
        }
        val altPillValue = remember(altInt, isFa) {
            val raw = altInt.toString()
            if (isFa) TimeEngine.formatPersianNumbers(raw) else raw
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AetherRadialDialCard(
                scoreText = elevScoreText,
                status = if (isAboveHorizon) {
                    if (isFa) "بالای افق" else "Above Horizon"
                } else {
                    if (isFa) "زیر افق" else "Below Horizon"
                },
                highlightFraction = dialFraction,
                title = if (isFa) "زاویه ارتفاع" else "Elevation",
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
                onClick = {
                    selectedLiveState?.satellite?.let { activeDetailSatellite = it }
                }
            )

            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AetherGlowingMeshPillCard(
                    title = if (isFa) "ارتفاع مداری" else "Altitude",
                    value = altPillValue,
                    unit = "km",
                    badge = "LEO",
                    preset = AetherMeshPillPreset.PINK_MESH,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        selectedLiveState?.satellite?.let { activeDetailSatellite = it }
                    }
                )

                AetherGlowingMeshPillCard(
                    title = if (isFa) "سمت رصدی" else "Azimuth",
                    value = azValueText,
                    unit = "deg",
                    badge = "SGP4",
                    preset = AetherMeshPillPreset.AMBER_MESH,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        selectedLiveState?.satellite?.let { activeDetailSatellite = it }
                    }
                )
            }
        }

        // 4. Category Pills & Map Theme Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SatelliteCategory.entries) { cat ->
                    AetherPillBadge(
                        text = if (isFa) cat.labelFa else cat.labelEn,
                        selected = cat == selectedCategory,
                        tint = palette.textPrimary,
                        onClick = { selectedCategory = cat },
                        modifier = Modifier.testTag("filter_${cat.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                modifier = Modifier
                    .clip(RedShapes.capsule)
                    .background(palette.frostedGlassFill)
                    .specularRimBorder(cornerRadius = 100.dp)
                    .padding(horizontal = 4.dp, vertical = 3.dp)
                    .testTag("map_theme_selector"),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf(
                    MapTheme.AUTO to (if (isFa) "خودکار" else "Auto"),
                    MapTheme.DARK to (if (isFa) "تاریک" else "Dark"),
                    MapTheme.LIGHT to (if (isFa) "روشن" else "Light")
                ).forEach { (theme, label) ->
                    val selected = mapTheme == theme
                    Box(
                        modifier = Modifier
                            .clip(RedShapes.capsule)
                            .background(
                                if (selected) palette.crimsonGlow.copy(alpha = 0.28f) else Color.Transparent
                            )
                            .clickable {
                                mapTheme = theme
                                prefs.edit().putString("satellite_map_theme", theme.name).apply()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            style = RedTypographyTokens.caption.copy(
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Light,
                                fontSize = 10.5.sp
                            ),
                            color = if (selected) palette.textPrimary else palette.textMicro
                        )
                    }
                }
            }
        }

        // 5. Frameless Frosted Orbital Time Scrubber (`-12h..+12h`)
        AetherFrostedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("satellite_time_scrubber_card"),
            cornerRadius = 32.dp,
            contentPadding = PaddingValues(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AetherLegendDotItem(
                    label = if (isFa) "محور زمان مداری (±۱۲ ساعت)" else "Orbital Time Scrubber (±12h)",
                    filled = abs(timeOffsetHours) < 0.05f,
                    dotColor = palette.textPrimary
                )

                AetherPillBadge(
                    text = if (abs(timeOffsetHours) < 0.05f) {
                        if (isFa) "زنده" else "LIVE"
                    } else {
                        val raw = String.format(Locale.US, "%+.1fh", timeOffsetHours)
                        if (isFa) TimeEngine.formatPersianNumbers(raw) else raw
                    },
                    selected = abs(timeOffsetHours) < 0.05f,
                    tint = palette.textPrimary,
                    onClick = { timeOffsetHours = 0f }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            AetherTelemetrySlider(
                value = timeOffsetHours,
                onValueChange = { timeOffsetHours = it },
                valueRange = -12f..12f,
                accentColor = palette.sapphireGlow,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("time_scrubber_slider")
            )
        }

        // 6. Upcoming Visible Passes (Next 3 Days) with Oscilloscope Waveforms
        val roundedStartMs = remember(currentSimulationMs) {
            (currentSimulationMs / 60_000L) * 60_000L
        }
        var allUpcomingPasses by remember {
            mutableStateOf<List<Pair<SatelliteItem, ISSEngine.ISSPass>>?>(null)
        }

        LaunchedEffect(uiState.userLocation, roundedStartMs, detectedTrain) {
            withContext(Dispatchers.Default) {
                val allSats = SatelliteCatalog.getVisibleList(detectedTrain)
                val combined = mutableListOf<Pair<SatelliteItem, ISSEngine.ISSPass>>()
                for (sat in allSats) {
                    val passes = ISSEngine.predictPasses(
                        userLatDeg = uiState.userLocation.latitude,
                        userLonDeg = uiState.userLocation.longitude,
                        startTimestampMs = roundedStartMs,
                        tle = SatelliteEngine.getEffectiveTle(sat),
                        scanDays = 3,
                        visibleOnly = true,
                        standardMag = sat.standardMagnitude
                    )
                    for (p in passes) {
                        combined.add(Pair(sat, p))
                    }
                }
                val sorted = combined.sortedBy { it.second.startTimeMs }
                withContext(Dispatchers.Main) {
                    allUpcomingPasses = sorted
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isFa) "گذرهای قابل رویت (۳ روز آینده)" else "Upcoming Visible Passes",
                style = RedTypographyTokens.sectionTitle.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.3).sp
                ),
                color = palette.textPrimary
            )
            allUpcomingPasses?.size?.let { count ->
                AetherPillBadge(
                    text = if (isFa) {
                        TimeEngine.formatPersianNumbers(count.toString())
                    } else {
                        String.format(Locale.US, "%02d", count)
                    },
                    tint = palette.textPrimary
                )
            }
        }

        val currentPasses = allUpcomingPasses
        if (currentPasses == null) {
            AetherFrostedCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 28.dp,
                contentPadding = PaddingValues(18.dp)
            ) {
                Text(
                    text = if (isFa) "در حال محاسبه گذرهای مداری..." else "Propagating SGP4 visible passes...",
                    style = RedTypographyTokens.bodySecondary.copy(fontWeight = FontWeight.Light),
                    color = palette.textSecondary
                )
            }
        } else if (currentPasses.isEmpty()) {
            AetherFrostedCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 28.dp,
                contentPadding = PaddingValues(18.dp)
            ) {
                Text(
                    text = if (isFa) "هیچ گذر مرئی در ۳ روز آینده پیش‌بینی نشد."
                    else "No naked-eye sunlit passes predicted in the next 72 hours.",
                    style = RedTypographyTokens.bodySecondary.copy(fontWeight = FontWeight.Light),
                    color = palette.textSecondary
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                currentPasses.take(8).forEach { (sat, pass) ->
                    AetherVisiblePassCard(
                        satName = if (isFa) sat.nameFa else sat.nameEn,
                        pass = pass,
                        calendarSystem = uiState.calendarSystem,
                        isFa = isFa,
                        onSchedulePassReminder = { leadMins ->
                            executeWithNotificationPermission {
                                AstroNotificationManager.scheduleSpecificPassAlarm(
                                    context = context,
                                    satellite = sat,
                                    pass = pass,
                                    userLocation = uiState.userLocation,
                                    leadMinutes = leadMins
                                )
                                Toast.makeText(
                                    context,
                                    if (isFa) "هشدار گذر تنظیم شد!" else "Pass alert scheduled!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            }
        }

        // 7. Tracked Satellites Telemetry Deck
        Text(
            text = if (isFa) "فهرست ماهواره‌های تحت پایش" else "Tracked Satellites Fleet",
            style = RedTypographyTokens.sectionTitle.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.3).sp
            ),
            color = palette.textPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            liveSatelliteStates.forEach { state ->
                val sat = state.satellite
                val isSelected = sat.id == selectedSatelliteId
                val elevRaw = String.format(Locale.US, "%+.0f°", state.topocentric.elevationDeg)
                val altRaw = String.format(Locale.US, "%.0f km", state.topocentric.satAltKm)

                AetherFrostedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sat_card_${sat.id}"),
                    cornerRadius = 28.dp,
                    contentPadding = PaddingValues(16.dp),
                    accentGlow = if (isSelected) palette.sapphireGlow else null,
                    onClick = {
                        selectedSatelliteId = sat.id
                        activeDetailSatellite = sat
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            AetherOpticalIconLens(
                                icon = Icons.Default.SatelliteAlt,
                                contentDescription = null,
                                isActive = isSelected,
                                onClick = {
                                    selectedSatelliteId = sat.id
                                    activeDetailSatellite = sat
                                }
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isFa) sat.nameFa else sat.nameEn,
                                    style = RedTypographyTokens.bodyPrimary.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isFa) TimeEngine.formatPersianNumbers(altRaw) else altRaw,
                                    style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Light),
                                    color = palette.textSecondary
                                )
                            }
                        }

                        AetherPillBadge(
                            text = if (isFa) TimeEngine.formatPersianNumbers(elevRaw) else elevRaw,
                            selected = state.topocentric.elevationDeg > 0.0,
                            tint = palette.textPrimary,
                            onClick = { activeDetailSatellite = sat }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(112.dp))
    }
}

@Composable
private fun AetherVisiblePassCard(
    satName: String,
    pass: ISSEngine.ISSPass,
    calendarSystem: CalendarSystem,
    isFa: Boolean,
    onSchedulePassReminder: (leadMinutes: Int) -> Unit
) {
    val palette = AetherTheme.palette
    var showMenu by remember { mutableStateOf(false) }

    val dateStr = TimeEngine.formatDate(pass.startTimeMs, calendarSystem, isFa)
    val riseStr = TimeEngine.formatTime24h(pass.startTimeMs, isFa)
    val setStr = TimeEngine.formatTime24h(pass.endTimeMs, isFa)
    val maxElevRaw = "${pass.maxElevationDeg.roundToInt()}°"
    val maxElevStr = if (isFa) TimeEngine.formatPersianNumbers(maxElevRaw) else maxElevRaw
    val durationMin = ((pass.endTimeMs - pass.startTimeMs) / 60_000L).coerceAtLeast(1)
    val durText = if (isFa) "${TimeEngine.formatPersianNumbers(durationMin.toString())} دقیقه" else "${durationMin}m"
    val waveCount = (pass.maxElevationDeg / 18.0).roundToInt().coerceIn(2, 5)
    val passProgress = (pass.maxElevationDeg.toFloat() / 90f).coerceIn(0.28f, 1f)

    AetherFrostedCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 30.dp,
        contentPadding = PaddingValues(18.dp),
        accentGlow = palette.sapphireGlow
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = satName,
                    style = RedTypographyTokens.bodyPrimary.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.5.sp
                    ),
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$dateStr  •  $riseStr–$setStr  •  $durText",
                    style = RedTypographyTokens.caption.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 11.5.sp
                    ),
                    color = palette.textSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AetherPillBadge(
                    text = if (isFa) "اوج $maxElevStr" else "Max $maxElevStr",
                    selected = pass.maxElevationDeg >= 45.0,
                    tint = palette.textPrimary
                )

                Box {
                    AetherOpticalIconLens(
                        icon = Icons.Default.NotificationsNone,
                        contentDescription = "Pass Alert",
                        onClick = { showMenu = true },
                        size = 34.dp,
                        iconSize = 16.dp
                    )
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        listOf(
                            10 to (if (isFa) "۱۰ دقیقه قبل" else "10m before"),
                            30 to (if (isFa) "۳۰ دقیقه قبل" else "30m before"),
                            1440 to (if (isFa) "۱ روز قبل" else "24h before")
                        ).forEach { (mins, label) ->
                            DropdownMenuItem(
                                text = { Text(text = label) },
                                onClick = {
                                    showMenu = false
                                    onSchedulePassReminder(mins)
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        AetherOscilloscopeWaveformCanvas(
            activeWaves = waveCount,
            progress = passProgress,
            waveColor = palette.textPrimary.copy(alpha = 0.52f),
            troughGlowColor = palette.glowWhite,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        )
    }
}
