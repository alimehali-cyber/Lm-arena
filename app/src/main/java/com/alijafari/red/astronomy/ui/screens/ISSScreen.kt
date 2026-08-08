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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ISSScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    val context = LocalContext.current

    // Notification Prefs
    val prefs = remember { context.getSharedPreferences("astro_prefs", Context.MODE_PRIVATE) }
    var isAutoAlertEnabled by remember {
        mutableStateOf(prefs.getBoolean("iss_auto_alerts_enabled", false))
    }
    var selectedLeadMinutes by remember {
        mutableStateOf(prefs.getInt("iss_alert_lead_minutes", 10))
    }
    var showLeadTimeSelectionDialog by remember { mutableStateOf(false) }

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
    var isFloatingPillVisible by remember { mutableStateOf(true) }

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
            panOffsetX = -((subLon / 180.0) * 200.0 * zoomScale.toDouble()).toFloat()
            panOffsetY = ((subLat / 90.0) * 100.0 * zoomScale.toDouble()).toFloat()
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

            // 1. MAIN FEATURE: INTERACTIVE 2D EARTH MAP
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0B0E1B))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomScale = (zoomScale * zoom).coerceIn(1.0f, 6.0f)
                            if (zoomScale > 1.0f) {
                                panOffsetX = (panOffsetX + pan.x).coerceIn(-400f * zoomScale, 400f * zoomScale)
                                panOffsetY = (panOffsetY + pan.y).coerceIn(-200f * zoomScale, 200f * zoomScale)
                            } else {
                                panOffsetX = 0f
                                panOffsetY = 0f
                            }
                        }
                    }
            ) {
                // Earth Map Canvas
                val subSolarPoint = remember(currentSimulationMs) {
                    SatelliteEngine.calculateSubSolarPoint(currentSimulationMs)
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    fun mapX(lon: Double): Float {
                        val baseX = ((lon + 180.0) / 360.0 * w).toFloat()
                        val centerX = w / 2f
                        return centerX + (baseX - centerX) * zoomScale + panOffsetX
                    }

                    fun mapY(lat: Double): Float {
                        val baseY = ((90.0 - lat) / 180.0 * h).toFloat()
                        val centerY = h / 2f
                        return centerY + (baseY - centerY) * zoomScale + panOffsetY
                    }

                    // 1. Geographic Lat/Lon Grid
                    val gridColor = Color.White.copy(alpha = 0.04f)
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
                            color = AccentPrimary.copy(alpha = 0.20f),
                            start = Offset(0f, eqY),
                            end = Offset(w, eqY),
                            strokeWidth = 1.5f
                        )
                    }

                    // 2. Draw World Continents Landmasses
                    drawWorldContinentsScaled(this, w, h, zoomScale, panOffsetX, panOffsetY, Color(0xFF1E2640))

                    // 3. Draw Dynamic Day/Night Terminator & Night Shade Region
                    drawDayNightTerminator(this, w, h, zoomScale, panOffsetX, panOffsetY, subSolarPoint)

                    // 4. Draw Dynamic Night-Side City Lights
                    drawNightCityLights(this, w, h, zoomScale, panOffsetX, panOffsetY, subSolarPoint)

                    // 5. Draw User Location Pin
                    val userX = mapX(uiState.userLocation.longitude)
                    val userY = mapY(uiState.userLocation.latitude)
                    if (userX in 0f..w && userY in 0f..h) {
                        drawCircle(
                            color = Color(0xFF00F0FF).copy(alpha = 0.3f),
                            radius = 12.dp.toPx(),
                            center = Offset(userX, userY)
                        )
                        drawCircle(
                            color = Color(0xFF00F0FF),
                            radius = 4.dp.toPx(),
                            center = Offset(userX, userY)
                        )
                    }

                    // 6. Draw Selected Satellite Ground Track
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
                            color = AccentPrimary.copy(alpha = 0.6f),
                            style = Stroke(
                                width = 2.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                            )
                        )
                    }

                    // 7. Draw All Filtered Satellite Markers
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
                                    radius = 16.dp.toPx(),
                                    center = Offset(sx, sy)
                                )
                            }

                            drawCircle(
                                color = markerColor,
                                radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                                center = Offset(sx, sy)
                            )
                        }
                    }
                }

                // Floating Action Buttons on Map
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Reset View Button
                    SmallFloatingActionButton(
                        onClick = {
                            zoomScale = 1.0f
                            panOffsetX = 0f
                            panOffsetY = 0f
                            isFollowSatelliteMode = false
                        },
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White,
                        modifier = Modifier.size(32.dp).testTag("map_reset_view")
                    ) {
                        Icon(Icons.Default.CenterFocusWeak, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                    }

                    // Follow Satellite Mode Toggle
                    SmallFloatingActionButton(
                        onClick = { isFollowSatelliteMode = !isFollowSatelliteMode },
                        containerColor = if (isFollowSatelliteMode) AccentPrimary else Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White,
                        modifier = Modifier.size(32.dp).testTag("map_follow_toggle")
                    ) {
                        Icon(Icons.Default.GpsFixed, contentDescription = "Follow", modifier = Modifier.size(16.dp))
                    }

                    // Zoom In
                    SmallFloatingActionButton(
                        onClick = { zoomScale = (zoomScale + 0.8f).coerceAtMost(6.0f) },
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White,
                        modifier = Modifier.size(32.dp).testTag("map_zoom_in")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                    }

                    // Zoom Out
                    SmallFloatingActionButton(
                        onClick = {
                            zoomScale = (zoomScale - 0.8f).coerceAtLeast(1.0f)
                            if (zoomScale == 1.0f) {
                                panOffsetX = 0f
                                panOffsetY = 0f
                            }
                        },
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White,
                        modifier = Modifier.size(32.dp).testTag("map_zoom_out")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
                    }
                }

                // 1st Tap Floating Info Pill near Map Bottom
                if (selectedSatState != null && isFloatingPillVisible) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp)
                            .clickable { activeDetailSatellite = selectedSatState.satellite }
                            .testTag("map_floating_info_pill"),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xF10D1120),
                        border = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(AccentPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SatelliteAlt, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(16.dp))
                            }

                            Column {
                                Text(
                                    text = if (isFa) selectedSatState.satellite.nameFa else selectedSatState.satellite.nameEn,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                val altStr = String.format(Locale.US, "%.0f km", selectedSatState.topocentric.satAltKm)
                                val spdStr = String.format(Locale.US, "%.1f km/s", selectedSatState.topocentric.velocityKmS)
                                Text(
                                    text = if (isFa) "ارتفاع: $altStr • سرعت: $spdStr".toPersianDigits() else "Alt: $altStr • Vel: $spdStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Detail",
                                tint = AccentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
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

            // 3. COMPACT 24-HOUR TIME SCRUBBER
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
                                isFloatingPillVisible = true
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
                        title = if (isFa) "نقشه زنده زمین:" else "Live Earth Map:",
                        desc = if (isFa) "نمایش خط جدایش روز و شب (Terminator) به همراه روشنایی شهرهای بزرگ در شب."
                        else "Displays the dynamic day/night terminator and real-time night-side city lights."
                    )
                    TutorialBullet(
                        title = if (isFa) "کنترل نقشه:" else "Map Controls:",
                        desc = if (isFa) "امکان زوم با دو انگشت، جابه‌جایی، بازنشانی نمای زمین و حالت دنبال‌کردن ماهواره."
                        else "Supports pinch-to-zoom, panning, reset-view, and follow-satellite mode."
                    )
                    TutorialBullet(
                        title = if (isFa) "محور زمان ۲۴ ساعته:" else "24-Hour Time Scrubber:",
                        desc = if (isFa) "بررسی موقعیت مداری ماهواره‌ها در ۲۴ ساعت گذشته یا آینده."
                        else "Scrub from -24h in the past to +24h into the future to simulate orbit geometry."
                    )
                    TutorialBullet(
                        title = if (isFa) "رؤیت با چشم غیرمسلح:" else "Naked-Eye Visibility:",
                        desc = if (isFa) "محاسبه بر اساس تاریکی آسمان، تابش خورشید روی ماهواره و زاویه ارتفاع بالای افق."
                        else "Scientifically calculated based on dark sky twilight, satellite solar illumination, and elevation."
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

// Canvas Helper: Scaled World Continents Rendering
private fun drawWorldContinentsScaled(
    drawScope: DrawScope,
    w: Float,
    h: Float,
    zoom: Float,
    panX: Float,
    panY: Float,
    landColor: Color
) {
    fun mapX(lon: Double): Float {
        val baseX = ((lon + 180.0) / 360.0 * w).toFloat()
        val centerX = w / 2f
        return centerX + (baseX - centerX) * zoom + panX
    }

    fun mapY(lat: Double): Float {
        val baseY = ((90.0 - lat) / 180.0 * h).toFloat()
        val centerY = h / 2f
        return centerY + (baseY - centerY) * zoom + panY
    }

    val continents = listOf(
        // North America
        listOf(
            70.0 to -165.0, 72.0 to -125.0, 60.0 to -105.0, 50.0 to -65.0,
            25.0 to -80.0, 15.0 to -90.0, 8.0 to -78.0, 15.0 to -105.0,
            30.0 to -115.0, 55.0 to -165.0
        ),
        // Greenland
        listOf(75.0 to -45.0, 82.0 to -30.0, 65.0 to -38.0, 60.0 to -45.0),
        // South America
        listOf(
            10.0 to -75.0, 5.0 to -50.0, -10.0 to -35.0, -23.0 to -42.0,
            -55.0 to -68.0, -45.0 to -75.0, -18.0 to -70.0, 5.0 to -78.0
        ),
        // Europe
        listOf(
            36.0 to -9.0, 43.0 to -9.0, 48.0 to -4.0, 54.0 to 5.0,
            58.0 to 6.0, 68.0 to 14.0, 71.0 to 28.0, 60.0 to 30.0,
            45.0 to 35.0, 38.0 to 24.0, 36.0 to -9.0
        ),
        // Africa
        listOf(
            36.0 to -5.0, 37.0 to 10.0, 32.0 to 32.0, 12.0 to 43.0,
            10.0 to 51.0, -12.0 to 40.0, -34.0 to 20.0, -34.0 to 18.0,
            -18.0 to 12.0, 4.0 to 9.0, 15.0 to -17.0, 35.0 to -6.0
        ),
        // Asia
        listOf(
            75.0 to 60.0, 75.0 to 170.0, 60.0 to 170.0, 40.0 to 140.0,
            22.0 to 120.0, 10.0 to 105.0, 1.0 to 104.0, 8.0 to 77.0,
            25.0 to 65.0, 12.0 to 44.0, 30.0 to 35.0, 40.0 to 30.0,
            55.0 to 60.0
        ),
        // Australia
        listOf(
            -12.0 to 130.0, -12.0 to 142.0, -25.0 to 153.0,
            -38.0 to 148.0, -35.0 to 117.0, -20.0 to 114.0
        ),
        // Antarctica
        listOf(-65.0 to -180.0, -65.0 to 180.0, -90.0 to 180.0, -90.0 to -180.0)
    )

    for (polygon in continents) {
        val path = Path()
        polygon.forEachIndexed { index, (lat, lon) ->
            val px = mapX(lon)
            val py = mapY(lat)
            if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        drawScope.drawPath(path = path, color = landColor)
    }
}

// Canvas Helper: Dynamic Day/Night Terminator Shade
private fun drawDayNightTerminator(
    drawScope: DrawScope,
    w: Float,
    h: Float,
    zoom: Float,
    panX: Float,
    panY: Float,
    subSolarPoint: SubSolarPoint
) {
    fun mapX(lon: Double): Float {
        val baseX = ((lon + 180.0) / 360.0 * w).toFloat()
        val centerX = w / 2f
        return centerX + (baseX - centerX) * zoom + panX
    }

    fun mapY(lat: Double): Float {
        val baseY = ((90.0 - lat) / 180.0 * h).toFloat()
        val centerY = h / 2f
        return centerY + (baseY - centerY) * zoom + panY
    }

    val subLatRad = Math.toRadians(subSolarPoint.latDeg)
    val tanSubLat = tan(subLatRad)

    val nightPath = Path()

    // Top boundary across map
    nightPath.moveTo(mapX(-180.0), mapY(90.0))
    nightPath.lineTo(mapX(180.0), mapY(90.0))

    // Terminator curve from +180 to -180
    for (lon in 180 downTo -180 step 5) {
        val dLonRad = Math.toRadians(lon - subSolarPoint.lonDeg)
        val termLatRad = atan(-cos(dLonRad) / if (abs(tanSubLat) < 1e-4) 1e-4 else tanSubLat)
        val termLatDeg = Math.toDegrees(termLatRad)

        val tx = mapX(lon.toDouble())
        val ty = mapY(termLatDeg)
        nightPath.lineTo(tx, ty)
    }

    nightPath.close()

    drawScope.drawPath(
        path = nightPath,
        color = Color(0xBB0B1021)
    )
}

// Canvas Helper: Night-Side City Lights
private fun drawNightCityLights(
    drawScope: DrawScope,
    w: Float,
    h: Float,
    zoom: Float,
    panX: Float,
    panY: Float,
    subSolarPoint: SubSolarPoint
) {
    fun mapX(lon: Double): Float {
        val baseX = ((lon + 180.0) / 360.0 * w).toFloat()
        val centerX = w / 2f
        return centerX + (baseX - centerX) * zoom + panX
    }

    fun mapY(lat: Double): Float {
        val baseY = ((90.0 - lat) / 180.0 * h).toFloat()
        val centerY = h / 2f
        return centerY + (baseY - centerY) * zoom + panY
    }

    val subLatRad = Math.toRadians(subSolarPoint.latDeg)

    for (city in SatelliteEngine.majorCityLights) {
        val cLatRad = Math.toRadians(city.lat)
        val dLonRad = Math.toRadians(city.lon - subSolarPoint.lonDeg)

        // Spherical zenith angle cosine cosPsi
        val cosPsi = sin(cLatRad) * sin(subLatRad) + cos(cLatRad) * cos(subLatRad) * cos(dLonRad)

        if (cosPsi < 0.0) { // On night side
            val alpha = min(1.0f, (abs(cosPsi) / 0.15f).toFloat())
            val cx = mapX(city.lon)
            val cy = mapY(city.lat)

            if (cx in 0f..w && cy in 0f..h) {
                drawScope.drawCircle(
                    color = Color(0xFFFFC107).copy(alpha = 0.8f * alpha),
                    radius = city.sizeDp * zoom * 0.8f,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}
