package com.alijafari.red.astronomy.ui.screens

import android.content.Context
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

enum class MapTheme {
    REALISTIC,
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
    val density = LocalDensity.current

    // Notification & Map Prefs
    val prefs = remember { context.getSharedPreferences("astro_prefs", Context.MODE_PRIVATE) }
    var isAutoAlertEnabled by remember {
        mutableStateOf(prefs.getBoolean("iss_auto_alerts_enabled", false))
    }
    var selectedLeadMinutes by remember {
        mutableStateOf(prefs.getInt("iss_alert_lead_minutes", 10))
    }
    var showLeadTimeSelectionDialog by remember { mutableStateOf(false) }

    // Map Theme State (Persisted)
    var mapTheme by remember {
        val saved = prefs.getString("satellite_map_theme", MapTheme.REALISTIC.name)
        mutableStateOf(if (saved == MapTheme.LIGHT.name) MapTheme.LIGHT else MapTheme.REALISTIC)
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

    // Time Control & Scrubber State (-24h to +24h)
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

    // Filters
    var selectedCategory by remember { mutableStateOf(SatelliteCategory.ALL) }

    // Selected Satellite & Detail Navigation
    var selectedSatelliteId by remember { mutableStateOf("iss_zarya") }
    var activeDetailSatellite by remember { mutableStateOf<SatelliteItem?>(null) }
    
    // Temporary anchored label state (shown when tapping a satellite)
    var anchoredSatLabelState by remember { mutableStateOf<SatelliteLiveState?>(null) }

    // Map Camera Gestures State
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0.0f) }
    var panOffsetY by remember { mutableFloatStateOf(0.0f) }
    var isFollowSatelliteMode by remember { mutableStateOf(false) }

    // Dialog Modals
    var showTutorialDialog by remember { mutableStateOf(false) }

    // Filtered Satellites
    val filteredSatellites = remember(selectedCategory) {
        SatelliteCatalog.satellites.filter { sat ->
            when (selectedCategory) {
                SatelliteCategory.ALL -> true
                SatelliteCategory.ISS -> sat.category == SatelliteCategory.ISS
                SatelliteCategory.STARLINK -> sat.category == SatelliteCategory.STARLINK
                SatelliteCategory.HUBBLE -> sat.category == SatelliteCategory.HUBBLE
                SatelliteCategory.JWST -> sat.category == SatelliteCategory.JWST
                SatelliteCategory.VISIBLE -> sat.isNakedEyeCandidate
            }
        }
    }

    val selectedSatItem = remember(selectedSatelliteId) {
        SatelliteCatalog.getById(selectedSatelliteId)
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

    val selectedSatState = liveSatelliteStates.find { it.satellite.id == selectedSatelliteId }
        ?: liveSatelliteStates.firstOrNull()

    // Follow Satellite Camera Effect
    LaunchedEffect(isFollowSatelliteMode, selectedSatState, zoomScale) {
        if (isFollowSatelliteMode && selectedSatState != null && zoomScale > 1.0f) {
            val subLat = selectedSatState.topocentric.subLatDeg
            val subLon = selectedSatState.topocentric.subLonDeg
            panOffsetX = -((subLon / 180.0) * 180.0 * zoomScale.toDouble()).toFloat()
            panOffsetY = ((subLat / 90.0) * 90.0 * zoomScale.toDouble()).toFloat()
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
                                text = if (isFa) "مرکز ردیابی ماهواره‌ها" else "Satellite Tracking Center",
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
                        // Alerts Toggle Button
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

                        // Tutorial / Help Button
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
                    .background(if (mapTheme == MapTheme.REALISTIC) Color(0xFF080C19) else Color(0xFFE0F2FE))
                    .border(
                        1.dp,
                        if (mapTheme == MapTheme.REALISTIC) Color.White.copy(alpha = 0.15f) else Color(0xFF0284C7).copy(alpha = 0.3f),
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
                    val isRealistic = mapTheme == MapTheme.REALISTIC
                    val landColor = if (isRealistic) Color(0xFF1B263B) else Color(0xFFA7F3D0)
                    val landStroke = if (isRealistic) Color(0xFF2E3E5D) else Color(0xFF059669)
                    val gridColor = if (isRealistic) Color(0x18FFFFFF) else Color(0x3094A3B8)
                    val equatorColor = if (isRealistic) Color(0x4000F0FF) else Color(0x600284C7)
                    val nightShadowColor = if (isRealistic) Color(0xCD040711) else Color(0x330F172A)
                    val cityLightColor = if (isRealistic) Color(0xFFFFC107) else Color(0xFFD97706)

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
                            style = Stroke(width = (if (isRealistic) 1.2f else 1.0f) / scaleX)
                        )
                    }
                    drawContext.canvas.restore()

                    // 3. Astronomical Day/Night Terminator & Night Shade Region
                    val subLatRad = Math.toRadians(subSolarPoint.latDeg)
                    val tanSubLat = tan(subLatRad)

                    val nightPath = Path()
                    nightPath.moveTo(mapX(-180.0), mapY(90.0))
                    nightPath.lineTo(mapX(180.0), mapY(90.0))

                    for (lon in 180 downTo -180 step 4) {
                        val dLonRad = Math.toRadians(lon - subSolarPoint.lonDeg)
                        val termLatRad = atan(-cos(dLonRad) / if (abs(tanSubLat) < 1e-4) 1e-4 else tanSubLat)
                        val termLatDeg = Math.toDegrees(termLatRad)

                        val tx = mapX(lon.toDouble())
                        val ty = mapY(termLatDeg)
                        nightPath.lineTo(tx, ty)
                    }
                    nightPath.close()

                    drawPath(path = nightPath, color = nightShadowColor)

                    // 4. Night-Side City Lights (fade near terminator)
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

                    // 5. User Location Pin
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
                    }

                    // 6. Selected Satellite Ground Track
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
                            color = AccentPrimary.copy(alpha = 0.75f),
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

                // Compact Map Theme Switcher Toggle (Top Left)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .testTag("map_theme_toggle"),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (mapTheme == MapTheme.REALISTIC) AccentPrimary else Color.Transparent)
                                .clickable {
                                    mapTheme = MapTheme.REALISTIC
                                    prefs.edit().putString("satellite_map_theme", MapTheme.REALISTIC.name).apply()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isFa) "واقع‌گرایانه" else "Realistic",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
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
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isFa) "روشن" else "Light",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }

                // Floating Map Camera Action Buttons (Top Right)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            zoomScale = 1.0f
                            panOffsetX = 0f
                            panOffsetY = 0f
                            isFollowSatelliteMode = false
                        },
                        containerColor = Color.Black.copy(alpha = 0.65f),
                        contentColor = Color.White,
                        modifier = Modifier.size(30.dp).testTag("map_reset_view")
                    ) {
                        Icon(Icons.Default.CenterFocusWeak, contentDescription = "Reset", modifier = Modifier.size(15.dp))
                    }

                    SmallFloatingActionButton(
                        onClick = { isFollowSatelliteMode = !isFollowSatelliteMode },
                        containerColor = if (isFollowSatelliteMode) AccentPrimary else Color.Black.copy(alpha = 0.65f),
                        contentColor = Color.White,
                        modifier = Modifier.size(30.dp).testTag("map_follow_toggle")
                    ) {
                        Icon(Icons.Default.GpsFixed, contentDescription = "Follow", modifier = Modifier.size(15.dp))
                    }

                    SmallFloatingActionButton(
                        onClick = { zoomScale = (zoomScale + 0.8f).coerceAtMost(6.0f) },
                        containerColor = Color.Black.copy(alpha = 0.65f),
                        contentColor = Color.White,
                        modifier = Modifier.size(30.dp).testTag("map_zoom_in")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(15.dp))
                    }

                    SmallFloatingActionButton(
                        onClick = {
                            zoomScale = (zoomScale - 0.8f).coerceAtLeast(1.0f)
                            if (zoomScale == 1.0f) {
                                panOffsetX = 0f
                                panOffsetY = 0f
                            }
                        },
                        containerColor = Color.Black.copy(alpha = 0.65f),
                        contentColor = Color.White,
                        modifier = Modifier.size(30.dp).testTag("map_zoom_out")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(15.dp))
                    }
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

            // 2. COMPACT SATELLITE FILTERS BAR
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
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

            // 3. COMPACT 24-HOUR TIME SCRUBBER (-24h to +24h)
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

                    // Time Slider (-24h to +24h)
                    Slider(
                        value = timeOffsetHours,
                        onValueChange = { timeOffsetHours = it },
                        valueRange = -24f..24f,
                        steps = 95,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentPrimary,
                            activeTrackColor = AccentPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("time_scrubber_slider")
                    )
                }
            }

            // 4. UPCOMING VISIBLE PASSES PREDICTIONS
            Text(
                text = if (isFa) "گذرهای قابل مشاهده بعدی" else "Upcoming Visible Passes",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            val upcomingPasses = remember(currentSimulationMs, uiState.userLocation, selectedSatelliteId) {
                ISSEngine.predictPasses(
                    userLatDeg = uiState.userLocation.latitude,
                    userLonDeg = uiState.userLocation.longitude,
                    startTimestampMs = currentSimulationMs,
                    tle = selectedSatItem.defaultTle,
                    scanDays = 2,
                    visibleOnly = true
                ).take(3)
            }

            if (upcomingPasses.isEmpty()) {
                Text(
                    text = if (isFa) "هیچ گذر قابل مشهودی در ۲۴ ساعت آینده یافت نشد." else "No visible passes predicted in next 24 hours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                val sdfPass = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(upcomingPasses) { pass ->
                        Surface(
                            modifier = Modifier
                                .width(220.dp)
                                .clickable { activeDetailSatellite = selectedSatItem }
                                .testTag("pass_card_${pass.startTimeMs}"),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Color(pass.classification.colorHex).copy(alpha = 0.4f))
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
                                    Text(
                                        text = if (isFa) pass.classification.labelFa else pass.classification.labelEn,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(pass.classification.colorHex)
                                    )
                                    Text(
                                        text = if (isFa) "${pass.passDurationSec / 60} دقیقه".toPersianDigits() else "${pass.passDurationSec / 60} min",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                val riseStr = sdfPass.format(Date(pass.startTimeMs))
                                val setStr = sdfPass.format(Date(pass.endTimeMs))
                                Text(
                                    text = if (isFa) "شروع: $riseStr • پایان: $setStr".toPersianDigits() else "Rise: $riseStr • Set: $setStr",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = if (isFa) "حداکثر ارتفاع: ${String.format(Locale.US, "%.0f°", pass.maxElevationDeg)}".toPersianDigits()
                                    else "Max Elevation: ${String.format(Locale.US, "%.0f°", pass.maxElevationDeg)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
                                selectedSatelliteId = sat.id
                                anchoredSatLabelState = state
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
                                Text(
                                    text = if (state.isNakedEyeVisible) (if (isFa) "با چشم" else "Naked Eye") else (if (isFa) "غیرمسلح نیست" else "Not Visible"),
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

    // Lead Time Selection Dialog
    if (showLeadTimeSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showLeadTimeSelectionDialog = false },
            title = {
                Text(
                    text = if (isFa) "تنظیم هشدار گذرهای ماهواره" else "Configure Satellite Pass Alerts",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isFa) "چند دقیقه قبل از گذرهای قابل مشاهده برای شما یادآوری ارسال شود؟"
                        else "How many minutes before visible passes would you like a notification?",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    listOf(5, 10, 15, 30).forEach { mins ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLeadMinutes = mins }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLeadMinutes == mins,
                                onClick = { selectedLeadMinutes = mins }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFa) "$mins دقیقه قبل" else "$mins minutes prior",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeadTimeSelectionDialog = false
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Text(text = if (isFa) "فعال‌سازی" else "Enable")
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
                        desc = if (isFa) "امکان تغییر بین حالت واقع‌گرایانه (تاریک) و روشن با حفظ نسبت‌های جغرافیایی."
                        else "Switch between Realistic (dark) and Light map themes while maintaining exact 2:1 geographic proportions."
                    )
                    TutorialBullet(
                        title = if (isFa) "تعامل با ماهواره:" else "Satellite Interaction:",
                        desc = if (isFa) "با لمس هر ماهواره، برچسب موقت شناور با نام و ارتفاع ظاهر می‌شود. لمس مجدد برچسب، صفحه جزئیات کامل را باز می‌کند."
                        else "Tap any satellite to show an anchored label with its name & altitude. Tap the label to open full satellite details."
                    )
                    TutorialBullet(
                        title = if (isFa) "محور زمان ۲۴ ساعته:" else "24-Hour Time Scrubber:",
                        desc = if (isFa) "بررسی موقعیت مداری ماهواره‌ها در ۲۴ ساعت گذشته یا آینده."
                        else "Scrub from -24h in the past to +24h into the future to simulate orbit geometry."
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
