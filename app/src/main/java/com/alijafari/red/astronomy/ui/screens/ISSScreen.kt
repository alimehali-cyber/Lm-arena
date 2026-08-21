package com.alijafari.red.astronomy.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
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
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.astro_engine.*
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.notification.AstroNotificationManager
import com.alijafari.red.astronomy.ui.theme.AccentPrimary
import com.alijafari.red.astronomy.util.toPersianDigits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

private fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

enum class MapTheme {
    DARK,
    LIGHT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ISSScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    val context = LocalContext.current
    val isOnline = remember(context) { isNetworkAvailable(context) }
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // Notification & Map Prefs
    val prefs = remember { context.getSharedPreferences("astro_prefs", Context.MODE_PRIVATE) }
    var isAutoAlertEnabled by remember {
        mutableStateOf(prefs.getBoolean("iss_auto_alerts_enabled", false))
    }
    var selectedLeadMinutes by remember {
        mutableStateOf(prefs.getInt("iss_alert_lead_minutes", 10))
    }
    var showLeadTimeSelectionDialog by remember { mutableStateOf(false) }

    // Map Theme State (Persisted: "DARK" or "LIGHT")
    var mapTheme by remember {
        val saved = prefs.getString("satellite_map_theme", MapTheme.DARK.name)
        mutableStateOf(if (saved == MapTheme.LIGHT.name) MapTheme.LIGHT else MapTheme.DARK)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        prefs.edit()
            .putBoolean("iss_auto_alerts_enabled", true)
            .putInt("iss_alert_lead_minutes", selectedLeadMinutes)
            .apply()

        isAutoAlertEnabled = true
        AstroNotificationManager.scheduleUpcomingIssPasses(
            context = context,
            userLocation = uiState.userLocation,
            leadMinutes = selectedLeadMinutes
        )
        Toast.makeText(
            context,
            if (isFa) "هشدار خودکار $selectedLeadMinutes دقیقه قبل تنظیم شد!" else "Auto notification set for $selectedLeadMinutes mins prior!",
            Toast.LENGTH_SHORT
        ).show()
    }

    // Time Control & Scrubber State (-12h to +12h)
    var realTimeNowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var timeOffsetHours by remember { mutableFloatStateOf(0f) } // 0 = Now
    val currentSimulationMs = remember(realTimeNowMs, timeOffsetHours) {
        realTimeNowMs + (timeOffsetHours * 3600 * 1000L).toLong()
    }

    // Real-Time Clock Ticker (updates realTimeNowMs every second when in live mode)
    LaunchedEffect(timeOffsetHours) {
        while (timeOffsetHours == 0f) {
            delay(1000L)
            realTimeNowMs = System.currentTimeMillis()
        }
    }

    val tleRepo = remember { com.alijafari.red.astronomy.data.repository.TleRepository.getInstance(context) }
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

    // Filters
    var selectedCategory by remember { mutableStateOf(SatelliteCategory.ALL) }

    // Selected Satellite (Nullable: null = no satellite selected / no orbit line)
    var selectedSatelliteId by remember { mutableStateOf<String?>("iss_zarya") }
    var activeDetailSatellite by remember { mutableStateOf<SatelliteItem?>(null) }

    // Temporary anchored label state (shown when tapping a satellite)
    var anchoredSatLabelState by remember { mutableStateOf<SatelliteLiveState?>(null) }

    // Map Camera Gestures State
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0.0f) }
    var panOffsetY by remember { mutableFloatStateOf(0.0f) }

    // Dialog Modals
    var showTutorialDialog by remember { mutableStateOf(false) }

    // Visible Satellites including dynamically detected Starlink Train
    val visibleSatellites = remember(detectedTrain) {
        SatelliteCatalog.getVisibleList(detectedTrain)
    }

    // Filtered Satellites
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

    // Compute live positions for all filtered satellites
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

    // Handle full-screen Detail Screen navigation
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

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 2.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AccentPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SatelliteAlt,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (isFa) "ماهواره‌ها" else "Satellites",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (timeOffsetHours == 0f) (if (isFa) "نقشه زنده SGP4 • داده مداری فعال" else "Live SGP4 Map • Real-Time Propagation")
                                else (if (isFa) "حالت شبیه‌سازی زمان" else "Time Simulation Mode"),
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentPrimary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isOnline) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WifiOff,
                                        contentDescription = "Offline",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isFa) "آفلاین" else "Offline",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        // Notification Alert Toggle
                        IconButton(
                            onClick = { showLeadTimeSelectionDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isAutoAlertEnabled) AccentPrimary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .testTag("satellite_alerts_toggle")
                        ) {
                            Icon(
                                imageVector = if (isAutoAlertEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                contentDescription = "Alerts",
                                tint = if (isAutoAlertEnabled) AccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Help / Guide Button
                        IconButton(
                            onClick = { showTutorialDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("satellite_help_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Help",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // 1. MAIN FEATURE: SCIENTIFICALLY ACCURATE 2D EARTH MAP
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.0f) // Strictly preserve true geographic 2:1 aspect ratio
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (mapTheme == MapTheme.DARK) Color(0xFF1E293B) else Color(0xFFE0F2FE))
                    .border(
                        1.dp,
                        if (mapTheme == MapTheme.DARK) Color.White.copy(alpha = 0.15f) else Color(0xFF0284C7).copy(alpha = 0.3f),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                // Solar position for astronomical terminator & city illumination
                val subSolarPoint = remember(currentSimulationMs) {
                    SatelliteEngine.calculateSubSolarPoint(currentSimulationMs)
                }

                // Temporary label screen position tracking
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

                                // Check tap on anchored label first
                                if (labelPosOnScreen != null && anchoredSatLabelState != null) {
                                    val lPos = labelPosOnScreen!!
                                    val labelBounds = Rect(
                                        left = lPos.x - 90f,
                                        top = lPos.y - 45f,
                                        right = lPos.x + 90f,
                                        bottom = lPos.y + 10f
                                    )
                                    if (labelBounds.contains(tapOffset)) {
                                        activeDetailSatellite = anchoredSatLabelState!!.satellite
                                        return@detectTapGestures
                                    }
                                }

                                // Check tap on satellite markers
                                var closest: SatelliteLiveState? = null
                                var minDist = with(density) { 32.dp.toPx() }

                                for (satState in liveSatelliteStates) {
                                    val sx = getMapX(satState.topocentric.subLonDeg)
                                    val sy = getMapY(satState.topocentric.subLatDeg)
                                    val dist = hypot(tapOffset.x - sx, tapOffset.y - sy)
                                    if (dist < minDist) {
                                        minDist = dist
                                        closest = satState
                                    }
                                }

                                if (closest != null) {
                                    selectedSatelliteId = closest.satellite.id
                                    anchoredSatLabelState = closest
                                } else {
                                    anchoredSatLabelState = null
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Geographic Coordinate Proportions: 360° longitude, 180° latitude
                    val pxPerDeg = w / 360.0f
                    val centerX = w / 2f
                    val centerY = h / 2f

                    fun mapX(lon: Double): Float =
                        centerX + (lon.toFloat() * pxPerDeg) * zoomScale + panOffsetX

                    fun mapY(lat: Double): Float =
                        centerY - (lat.toFloat() * pxPerDeg) * zoomScale + panOffsetY

                    // Color theme palettes
                    val isDark = mapTheme == MapTheme.DARK
                    val landColor = if (isDark) Color(0xFF334155) else Color(0xFFA7F3D0)
                    val landStroke = if (isDark) Color(0xFF64748B) else Color(0xFF059669)
                    val gridColor = if (isDark) Color(0x18FFFFFF) else Color(0x3094A3B8)
                    val equatorColor = if (isDark) Color(0x4000F0FF) else Color(0x600284C7)
                    val nightShadowColor = if (isDark) Color(0xCC030712) else Color(0x400F172A)
                    val cityLightColor = if (isDark) Color(0xFFFFC107) else Color(0xFFD97706)

                    // 1. Geographic Lat/Lon Grid Lines
                    for (gLon in -150..150 step 30) {
                        val gx = mapX(gLon.toDouble())
                        if (gx in 0f..w) drawLine(gridColor, start = Offset(gx, 0f), end = Offset(gx, h), strokeWidth = 1f)
                    }
                    for (gLat in -60..60 step 30) {
                        val gy = mapY(gLat.toDouble())
                        if (gy in 0f..h) drawLine(gridColor, start = Offset(0f, gy), end = Offset(w, gy), strokeWidth = 1f)
                    }

                    // Equator Line
                    val eqY = mapY(0.0)
                    if (eqY in 0f..h) {
                        drawLine(
                            color = equatorColor,
                            start = Offset(0f, eqY),
                            end = Offset(w, eqY),
                            strokeWidth = 1.5f
                        )
                    }

                    // 2. Draw Accurate Earth Continents & Landmasses
                    val scaleX = w * zoomScale
                    val scaleY = h * zoomScale
                    val translateX = centerX * (1f - zoomScale) + panOffsetX
                    val translateY = centerY * (1f - zoomScale) + panOffsetY

                    drawContext.canvas.save()
                    drawContext.canvas.translate(translateX, translateY)
                    drawContext.canvas.scale(scaleX, scaleY)

                    for (unitPath in WorldGeographyData.normalizedPaths) {
                        drawPath(path = unitPath, color = landColor, style = Fill)
                        drawPath(
                            path = unitPath,
                            color = landStroke,
                            style = Stroke(width = (if (isDark) 1.2f else 1.0f) / scaleX)
                        )
                    }
                    drawContext.canvas.restore()

                    // 3. Astronomical Day/Night Terminator & Night Shade Region
                    val subLatRad = Math.toRadians(subSolarPoint.latDeg)
                    val tanSubLat = tan(subLatRad)

                    val nightPath = Path()
                    if (subSolarPoint.latDeg >= 0) {
                        // Sun in Northern Hemisphere -> South Pole is night
                        nightPath.moveTo(mapX(-180.0), mapY(-90.0))
                        nightPath.lineTo(mapX(180.0), mapY(-90.0))
                        for (lon in 180 downTo -180 step 4) {
                            val dLonRad = Math.toRadians(lon - subSolarPoint.lonDeg)
                            val termLatRad = atan(-cos(dLonRad) / if (abs(tanSubLat) < 1e-4) 1e-4 else tanSubLat)
                            val termLatDeg = Math.toDegrees(termLatRad)
                            nightPath.lineTo(mapX(lon.toDouble()), mapY(termLatDeg))
                        }
                    } else {
                        // Sun in Southern Hemisphere -> North Pole is night
                        nightPath.moveTo(mapX(-180.0), mapY(90.0))
                        nightPath.lineTo(mapX(180.0), mapY(90.0))
                        for (lon in 180 downTo -180 step 4) {
                            val dLonRad = Math.toRadians(lon - subSolarPoint.lonDeg)
                            val termLatRad = atan(-cos(dLonRad) / if (abs(tanSubLat) < 1e-4) 1e-4 else tanSubLat)
                            val termLatDeg = Math.toDegrees(termLatRad)
                            nightPath.lineTo(mapX(lon.toDouble()), mapY(termLatDeg))
                        }
                    }
                    nightPath.close()

                    drawPath(path = nightPath, color = nightShadowColor)

                    // 4. Night-Side City Lights (appear ONLY on night side)
                    for (city in SatelliteEngine.majorCityLights) {
                        val cLatRad = Math.toRadians(city.lat)
                        val dLonRad = Math.toRadians(city.lon - subSolarPoint.lonDeg)
                        val cosPsi = sin(cLatRad) * sin(subLatRad) + cos(cLatRad) * cos(subLatRad) * cos(dLonRad)

                        if (cosPsi < 0.0) { // On night side
                            val alpha = (abs(cosPsi) / 0.12f).coerceAtMost(1.0).toFloat()
                            val cx = mapX(city.lon)
                            val cy = mapY(city.lat)

                            if (cx in 0f..w && cy in 0f..h) {
                                drawCircle(
                                    color = cityLightColor.copy(alpha = 0.85f * alpha),
                                    radius = city.sizeDp * zoomScale * 0.75f,
                                    center = Offset(cx, cy)
                                )
                            }
                        }
                    }

                    // 5. User Location Pin & City Name Label
                    val userX = mapX(uiState.userLocation.longitude)
                    val userY = mapY(uiState.userLocation.latitude)
                    if (userX in 0f..w && userY in 0f..h) {
                        drawCircle(
                            color = Color(0xFF00F0FF).copy(alpha = 0.35f),
                            radius = 12.dp.toPx(),
                            center = Offset(userX, userY)
                        )
                        drawCircle(
                            color = Color(0xFF00F0FF),
                            radius = 4.dp.toPx(),
                            center = Offset(userX, userY)
                        )

                        val cityName = if (isFa) uiState.userLocation.cityNameFa else uiState.userLocation.cityNameEn
                        val textLayoutResult = textMeasurer.measure(
                            text = if (isFa) cityName.toPersianDigits() else cityName,
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF00F0FF) else Color(0xFF0284C7)
                            )
                        )
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(userX - textLayoutResult.size.width / 2f, userY - 18.dp.toPx())
                        )
                    }

                    // 6. Selected Satellite Ground Track (ONLY if a satellite is selected)
                    if (selectedSatItem != null) {
                        val trackPoints = SatelliteEngine.calculateGroundTrack(
                            satellite = selectedSatItem,
                            currentTimestampMs = currentSimulationMs
                        )
                        val trackPath = Path()
                        var isFirst = true
                        var prevLon: Double? = null

                        for ((tLat, tLon) in trackPoints) {
                            if (prevLon != null && abs(tLon - prevLon) > 180.0) {
                                isFirst = true // handle antimeridian wrap
                            }
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
                            color = AccentPrimary.copy(alpha = 0.85f),
                            style = Stroke(
                                width = 2.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                            )
                        )
                    }

                    // 7. Filtered Satellite Markers
                    for (liveState in liveSatelliteStates) {
                        val sat = liveState.satellite
                        val isSelected = sat.id == selectedSatelliteId
                        val sx = mapX(liveState.topocentric.subLonDeg)
                        val sy = mapY(liveState.topocentric.subLatDeg)

                        if (sx in -20f..(w + 20f) && sy in -20f..(h + 20f)) {
                            val markerColor = if (isSelected) AccentPrimary else if (sat.isNakedEyeCandidate) Color(0xFF2DC653) else Color(0xFFFFB703)

                            if (isSelected) {
                                drawCircle(
                                    color = markerColor.copy(alpha = 0.25f),
                                    radius = 14.dp.toPx(),
                                    center = Offset(sx, sy)
                                )
                            }

                            drawCircle(
                                color = markerColor,
                                radius = if (isSelected) 5.5f.dp.toPx() else 4.dp.toPx(),
                                center = Offset(sx, sy)
                            )
                        }
                    }

                    // 8. Track screen position for temporary anchored label if active
                    if (anchoredSatLabelState != null) {
                        val satState = anchoredSatLabelState!!
                        val ax = mapX(satState.topocentric.subLonDeg)
                        val ay = mapY(satState.topocentric.subLatDeg)
                        labelPosOnScreen = Offset(ax, ay)
                    } else {
                        labelPosOnScreen = null
                    }
                }

                // ONLY Reset Button Overlay on Map (Top Right)
                SmallFloatingActionButton(
                    onClick = {
                        zoomScale = 1.0f
                        panOffsetX = 0f
                        panOffsetY = 0f
                    },
                    containerColor = Color.Black.copy(alpha = 0.65f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(32.dp)
                        .testTag("map_reset_view")
                ) {
                    Icon(Icons.Default.CenterFocusWeak, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                }

                // Temporary Anchored Label on Satellite Tap
                if (anchoredSatLabelState != null && labelPosOnScreen != null) {
                    val satState = anchoredSatLabelState!!
                    val lPos = labelPosOnScreen!!

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(0.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .offset(
                                    x = with(density) { (lPos.x - 75f).toDp() },
                                    y = with(density) { (lPos.y - 48f).toDp() }
                                )
                                .width(150.dp)
                                .clickable { activeDetailSatellite = satState.satellite }
                                .testTag("anchored_sat_label"),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xF20F172A),
                            border = BorderStroke(1.dp, AccentPrimary)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SatelliteAlt,
                                    contentDescription = null,
                                    tint = AccentPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isFa) satState.satellite.nameFa else satState.satellite.nameEn,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val altStr = String.format(Locale.US, "%.0f km", satState.topocentric.satAltKm)
                                    Text(
                                        text = if (isFa) "ارتفاع $altStr".toPersianDigits() else "Alt $altStr",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Detail",
                                    tint = AccentPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. MAP THEME & CATEGORY CONTROLS AREA (Outside Map Canvas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Filter Chips
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(SatelliteCategory.entries) { cat ->
                        val isSelected = cat == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    text = if (isFa) cat.labelFa else cat.labelEn,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("filter_${cat.name}")
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Compact Unobtrusive Map Theme Selector Outside Map
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("map_theme_selector")
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (mapTheme == MapTheme.DARK) AccentPrimary else Color.Transparent)
                                .clickable {
                                    mapTheme = MapTheme.DARK
                                    prefs.edit().putString("satellite_map_theme", MapTheme.DARK.name).apply()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isFa) "تاریک" else "Dark",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (mapTheme == MapTheme.DARK) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (mapTheme == MapTheme.LIGHT) AccentPrimary else Color.Transparent)
                                .clickable {
                                    mapTheme = MapTheme.LIGHT
                                    prefs.edit().putString("satellite_map_theme", MapTheme.LIGHT.name).apply()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isFa) "روشن" else "Light",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (mapTheme == MapTheme.LIGHT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. COMPACT TIME SCRUBBER (-12h to +12h)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(16.dp))

                            val sdf = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.US) }
                            val timeStr = sdf.format(Date(currentSimulationMs))
                            val offsetStr = if (timeOffsetHours == 0f) (if (isFa) "اکنون (زمان زنده)" else "Now (Live)")
                            else if (timeOffsetHours > 0) "+${String.format(Locale.US, "%.1fh", timeOffsetHours)}"
                            else String.format(Locale.US, "%.1fh", timeOffsetHours)

                            Text(
                                text = if (isFa) "$timeStr ($offsetStr)".toPersianDigits() else "$timeStr ($offsetStr)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (timeOffsetHours != 0f) {
                            Button(
                                onClick = { timeOffsetHours = 0f },
                                modifier = Modifier.height(28.dp).testTag("time_reset_now"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                            ) {
                                Text(
                                    text = if (isFa) "زمان زنده" else "Now",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    // Time Slider (-12h to +12h, 1-minute steps)
                    Slider(
                        value = timeOffsetHours,
                        onValueChange = { raw ->
                            val stepHours = 1.0 / 60.0 // 1 minute step
                            val rounded = (round(raw / stepHours) * stepHours).toFloat()
                            timeOffsetHours = rounded
                        },
                        valueRange = -12f..12f,
                        steps = 1439,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentPrimary,
                            activeTrackColor = AccentPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("time_scrubber_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isFa) "۱۲ ساعت قبل" else "12h ago",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isFa) "زمان زنده" else "Now",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentPrimary
                        )
                        Text(
                            text = if (isFa) "۱۲ ساعت بعد" else "12h ahead",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 4. UPCOMING VISIBLE PASSES PREDICTIONS (ALL Satellites, Next 3 Days, Chronological)
            Text(
                text = if (isFa) "گذرهای قابل مشاهده بعدی (۳ روز آینده - همه ماهواره‌ها)"
                else "Upcoming Visible Passes (Next 3 Days - All Satellites)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            val roundedStartMs = remember(currentSimulationMs) {
                (currentSimulationMs / 60_000L) * 60_000L
            }

            var allUpcomingPasses by remember {
                mutableStateOf<List<Pair<com.alijafari.red.astronomy.astro_engine.SatelliteItem, ISSEngine.ISSPass>>?>(null)
            }

            LaunchedEffect(uiState.userLocation, roundedStartMs, detectedTrain) {
                withContext(Dispatchers.Default) {
                    val allSats = com.alijafari.red.astronomy.astro_engine.SatelliteCatalog.getVisibleList(detectedTrain)
                    val combined = mutableListOf<Pair<com.alijafari.red.astronomy.astro_engine.SatelliteItem, ISSEngine.ISSPass>>()
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

            val currentAllPasses = allUpcomingPasses
            if (currentAllPasses == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = if (isFa) "در حال محاسبه گذرهای ماهواره‌ها..." else "Calculating satellite passes...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else if (currentAllPasses.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = if (isFa) "هیچ گذر قابل مشهودی مطابق با معیار علمی در ۳ روز آینده یافت نشد."
                        else "No visible passes meeting scientific criteria predicted in next 3 days.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    currentAllPasses.forEach { (sat, pass) ->
                        DetailedVisiblePassCard(
                            satName = if (isFa) sat.nameFa else sat.nameEn,
                            pass = pass,
                            cityName = uiState.userLocation.cityNameFa,
                            isFa = isFa,
                            onSchedulePassReminder = { leadMins ->
                                AstroNotificationManager.scheduleSpecificPassAlarm(
                                    context = context,
                                    satellite = sat,
                                    pass = pass,
                                    userLocation = uiState.userLocation,
                                    leadMinutes = leadMins
                                )
                                val labelStr = if (leadMins == 1440) (if (isFa) "۱ روز" else "1 day") else (if (isFa) "$leadMins دقیقه" else "$leadMins mins")
                                Toast.makeText(
                                    context,
                                    if (isFa) "هشدار گذر $labelStr قبل از شروع تنظیم شد!" else "Alert set $labelStr prior to pass!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }

            // 5. SATELLITE LIST / CARDS SECTION
            Text(
                text = if (isFa) "فهرست ماهواره‌ها (${filteredSatellites.size})" else "Tracked Satellites (${filteredSatellites.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                liveSatelliteStates.forEach { state ->
                    val sat = state.satellite
                    val isSelected = sat.id == selectedSatelliteId

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSatelliteId = if (isSelected) null else sat.id
                                anchoredSatLabelState = if (isSelected) null else state
                            }
                            .testTag("sat_card_${sat.id}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) AccentPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) AccentPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) AccentPrimary else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SatelliteAlt,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = if (isFa) sat.nameFa else sat.nameEn,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val altStr = String.format(Locale.US, "%.0f km", state.topocentric.satAltKm)
                                    val magStr = String.format(Locale.US, "%+.1f", state.apparentMagnitude)
                                    Text(
                                        text = if (isFa) "ارتفاع $altStr • قدر $magStr".toPersianDigits() else "Alt $altStr • Mag $magStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val verdictColor = if (state.isNakedEyeVisible) Color(0xFF2DC653) else Color(0xFF718096)
                                val verdictText = if (state.isNakedEyeVisible) (if (isFa) "با چشم" else "Naked Eye") else (if (isFa) "قابل مشاهده نیست" else "Not Visible")
                                Text(
                                    text = verdictText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = verdictColor
                                )

                                IconButton(
                                    onClick = { activeDetailSatellite = sat },
                                    modifier = Modifier.size(28.dp).testTag("open_sat_detail_${sat.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Details",
                                        tint = AccentPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Multi-Satellite Notification State
    var selectedSatIdsForAlerts by remember { mutableStateOf(setOf("iss_zarya", "tiangong", "starlink_train", "hubble")) }

    // Lead Time & Multi-Satellite Notification Dialog
    if (showLeadTimeSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showLeadTimeSelectionDialog = false },
            title = {
                Text(
                    text = if (isFa) "سیستم هشدار خودکار ماهواره‌ها" else "Automated Satellite Pass Alerts",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (isFa) "زمان یادآوری قبل از آغاز گذر:" else "Notification timing before pass:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = AccentPrimary
                    )

                    val leadOptions = listOf(
                        10 to (if (isFa) "۱۰ دقیقه قبل" else "10 minutes before"),
                        30 to (if (isFa) "۳۰ دقیقه قبل" else "30 minutes before"),
                        1440 to (if (isFa) "۱ روز قبل (۲۴ ساعت)" else "1 day before (24 hours)")
                    )

                    leadOptions.forEach { (mins, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLeadMinutes = mins }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLeadMinutes == mins,
                                onClick = { selectedLeadMinutes = mins }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Text(
                        text = if (isFa) "ماهواره‌های تحت پایش خودکار:" else "Monitored satellites:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = AccentPrimary
                    )

                    com.alijafari.red.astronomy.astro_engine.SatelliteCatalog.getVisibleList(detectedTrain).forEach { sat ->
                        val isChecked = sat.id in selectedSatIdsForAlerts
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSatIdsForAlerts = if (isChecked) {
                                        selectedSatIdsForAlerts - sat.id
                                    } else {
                                        selectedSatIdsForAlerts + sat.id
                                    }
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedSatIdsForAlerts = if (checked) {
                                            selectedSatIdsForAlerts + sat.id
                                        } else {
                                            selectedSatIdsForAlerts - sat.id
                                        }
                                    }
                                )
                                Text(
                                    text = if (isFa) sat.nameFa else sat.nameEn,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeadTimeSelectionDialog = false
                        isAutoAlertEnabled = true
                        prefs.edit().putBoolean("auto_satellite_alerts_enabled", true).apply()
                        AstroNotificationManager.scheduleMultiSatellitePasses(
                            context = context,
                            selectedSatIds = selectedSatIdsForAlerts,
                            userLocation = uiState.userLocation,
                            leadMinutes = selectedLeadMinutes
                        )
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        Toast.makeText(
                            context,
                            if (isFa) "سیستم هشدار خودکار برای ماهواره‌های انتخاب شده فعال شد!"
                            else "Automated pass monitoring enabled for selected satellites!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Text(text = if (isFa) "فعال‌سازی سیستم هشدار" else "Enable Monitoring")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeadTimeSelectionDialog = false }) {
                    Text(text = if (isFa) "انصراف" else "Cancel")
                }
            }
        )
    }

    // Tutorial Dialog
    if (showTutorialDialog) {
        AlertDialog(
            onDismissRequest = { showTutorialDialog = false },
            title = {
                Text(
                    text = if (isFa) "راهنمای مرکز ردیابی ماهواره‌ها" else "Satellite Tracking Guide",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    TutorialBullet(
                        title = if (isFa) "نقشه دقیق زمین:" else "Accurate Earth Map:",
                        desc = if (isFa) "نمایش قاره‌ها با پروژکسیون دقیق جغرافیایی، خط مرز روز و شب واقعی و روشنایی شهرهای شب."
                        else "Displays accurate geographic continents, real astronomical terminator boundary, and night-side city lights."
                    )
                    TutorialBullet(
                        title = if (isFa) "پوسته نقشه:" else "Map Theme:",
                        desc = if (isFa) "امکان تغییر بین حالت تاریک (Dark) و روشن (Light) از کنترل‌های خارج از نقشه."
                        else "Switch between Dark and Light map themes using unobtrusive controls outside the map."
                    )
                    TutorialBullet(
                        title = if (isFa) "تعامل با ماهواره:" else "Satellite Interaction:",
                        desc = if (isFa) "با لمس هر ماهواره، برچسب شناور نام و ارتفاع ظاهر می‌شود. لمس مجدد برچسب، صفحه جزئیات کامل و خط زمانی گذر را باز می‌کند."
                        else "Tap any satellite to show an anchored label. Tap the label to open full satellite details and timeline."
                    )
                    TutorialBullet(
                        title = if (isFa) "محور زمان ۱۲ ساعته:" else "12-Hour Time Scrubber:",
                        desc = if (isFa) "بررسی و شبیه‌سازی موقعیت ماهواره‌ها از ۱۲ ساعت قبل تا ۱۲ ساعت بعد."
                        else "Scrub from -12h in the past to +12h into the future to simulate satellite motion and day/night illumination."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTutorialDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Text(text = if (isFa) "متوجه شدم" else "Got It")
                }
            }
        )
    }
}

@Composable
private fun TutorialBullet(title: String, desc: String) {
    Column {
        Text(text = title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = AccentPrimary)
        Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
