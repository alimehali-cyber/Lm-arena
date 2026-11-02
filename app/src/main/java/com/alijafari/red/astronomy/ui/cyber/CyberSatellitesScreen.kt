package com.alijafari.red.astronomy.ui.cyber

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun isCyberNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/**
 * Cyber Glass UI Satellites & Orbital Tracking Screen:
 * - Spatial Floating Capsule Bar telemetry header
 * - Thick-cut `36.dp` Cutout Viewport Window Card framing the crisp 2:1 interactive 2D Earth
 *   Ground-Track & Terminator Map above Nothing Dot-Matrix orbital counters & a Mesh Gradient Pill
 * - Spatial Floating Capsule category & map-theme segmented bars (supporting Dark, Light & Wine)
 * - 12-Hour orbital time-scrubber widget with Dot-Matrix offset counter
 * - 3-Day Upcoming Visible Passes stack with `CyberDotRingGauge` & Mesh Gradient pills
 * - Tracked Satellites Matrix Deck
 */
@Composable
fun CyberSatellitesScreen(
    uiState: MainUiState,
    @Suppress("UNUSED_PARAMETER") viewModel: MainViewModel
) {
    val palette = CyberTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN
    val canvasFontFamily = LocalAppFontFamily.current
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    val isOnline = remember(context) { isCyberNetworkAvailable(context) }
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
    var selectedLeadMinutes by remember {
        mutableIntStateOf(prefs.getInt("iss_alert_lead_minutes", 10))
    }
    var showLeadTimeSelectionDialog by remember { mutableStateOf(false) }

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

    val coroutineScope = rememberCoroutineScope()
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
        realTimeNowMs + (timeOffsetHours * 3600 * 1000L).toLong()
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
    var showTutorialDialog by remember { mutableStateOf(false) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RedSpacing.lg, vertical = RedSpacing.md),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 0. SPATIAL FLOATING CAPSULE TELEMETRY HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CyberFloatingCapsuleBar(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                    elevation = 8.dp,
                    isActive = timeOffsetHours == 0f
                ) {
                    CyberSignalDot(
                        color = if (timeOffsetHours == 0f) palette.signalDot else palette.statusWarn,
                        size = 6.dp
                    )
                    Text(
                        text = if (timeOffsetHours == 0f) {
                            if (isFa) "تله‌متری زنده SGP4" else "SGP4 LIVE ORBIT"
                        } else {
                            if (isFa) "شبیه‌سازی زمانی" else "ORBIT SCRUB"
                        },
                        style = RedTypographyTokens.caption.copy(
                            fontWeight = FontWeight.Light,
                            fontSize = 11.5.sp,
                            letterSpacing = if (isFa) 0.sp else 1.0.sp
                        ),
                        color = palette.textPrimary
                    )
                    if (!isOnline) {
                        Text(
                            text = if (isFa) "• آفلاین" else "• OFFLINE",
                            style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Light, fontSize = 10.sp),
                            color = palette.textMuted
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CyberMicroIconButton(
                        icon = if (isAutoAlertEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                        contentDescription = "Alerts",
                        onClick = { showLeadTimeSelectionDialog = true },
                        isActive = isAutoAlertEnabled,
                        modifier = Modifier.testTag("satellite_alerts_toggle")
                    )
                    CyberMicroIconButton(
                        icon = Icons.Default.HelpOutline,
                        contentDescription = "Help",
                        onClick = { showTutorialDialog = true },
                        modifier = Modifier.testTag("satellite_help_button")
                    )
                }
            }

            // 1. CUTOUT VIEWPORT WINDOW ORBITAL MAP CARD (`36.dp` Spatial Glass 2.0 + 2:1 Crisp Cutout)
            CyberGlassWidget(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 36.dp,
                contentPadding = PaddingValues(16.dp),
                ambientTint = palette.primaryAccent
            ) {
                val cutoutShape = squircleShape(24.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2.0f)
                        .clip(cutoutShape)
                        .border(1.dp, palette.cutoutViewportBorder, cutoutShape)
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
                        if (w <= 0f || h <= 0f) return@Canvas

                        val oceanColor = when {
                            activeMapWine -> Color(0xFF120308)
                            activeMapDark -> Color(0xFF060A14)
                            else -> Color(0xFFE4ECF7)
                        }
                        val landColor = when {
                            activeMapWine -> Color(0xFF280914)
                            activeMapDark -> Color(0xFF111A2E)
                            else -> Color(0xFFF8FAFC)
                        }
                        val landStroke = when {
                            activeMapWine -> Color(0x47FF8CA6)
                            activeMapDark -> Color(0x3D3CE0F5)
                            else -> Color(0x330F172A)
                        }
                        val gridColor = if (activeMapDark) Color(0x16FFFFFF) else Color(0x140F172A)
                        val equatorColor = palette.primaryAccent.copy(alpha = 0.40f)
                        val nightShadowColor = if (activeMapDark) Color(0xAD020308) else Color(0x2E0F172A)
                        val cityLightColor = palette.secondaryAccent

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
                            drawLine(equatorColor, Offset(0f, eqY), Offset(w, eqY), 1.4f)
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
                        if (subSolarPoint.latDeg >= 0) {
                            nightPath.moveTo(mapX(-180.0), mapY(-90.0))
                            nightPath.lineTo(mapX(180.0), mapY(-90.0))
                            for (lon in 180 downTo -180 step 4) {
                                val dLonRad = Math.toRadians(lon - subSolarPoint.lonDeg)
                                val termLatDeg = Math.toDegrees(atan(-cos(dLonRad) / if (abs(tanSubLat) < 1e-4) 1e-4 else tanSubLat))
                                nightPath.lineTo(mapX(lon.toDouble()), mapY(termLatDeg))
                            }
                        } else {
                            nightPath.moveTo(mapX(-180.0), mapY(90.0))
                            nightPath.lineTo(mapX(180.0), mapY(90.0))
                            for (lon in 180 downTo -180 step 4) {
                                val dLonRad = Math.toRadians(lon - subSolarPoint.lonDeg)
                                val termLatDeg = Math.toDegrees(atan(-cos(dLonRad) / if (abs(tanSubLat) < 1e-4) 1e-4 else tanSubLat))
                                nightPath.lineTo(mapX(lon.toDouble()), mapY(termLatDeg))
                            }
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
                                        color = cityLightColor.copy(alpha = 0.80f * alpha),
                                        radius = city.sizeDp * zoomScale * 0.72f,
                                        center = Offset(cx, cy)
                                    )
                                }
                            }
                        }

                        val userX = mapX(uiState.userLocation.longitude)
                        val userY = mapY(uiState.userLocation.latitude)
                        if (userX in 0f..w && userY in 0f..h) {
                            drawCircle(palette.statusGood.copy(alpha = 0.32f), radius = 11.dp.toPx(), center = Offset(userX, userY))
                            drawCircle(palette.statusGood, radius = 3.8.dp.toPx(), center = Offset(userX, userY))

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
                            drawPath(path = trackPath, color = palette.primaryAccent.copy(alpha = 0.25f), style = Stroke(width = 6f))
                            drawPath(
                                path = trackPath,
                                color = palette.primaryAccent,
                                style = Stroke(width = 2.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 9f)))
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
                                            colors = listOf(palette.signalDot.copy(alpha = 0.65f), Color.Transparent),
                                            center = Offset(sx, sy),
                                            radius = 15.dp.toPx()
                                        ),
                                        radius = 15.dp.toPx(),
                                        center = Offset(sx, sy)
                                    )
                                }
                                drawCircle(
                                    color = if (isSelected) palette.signalDot else palette.primaryAccent,
                                    radius = if (isSelected) 5.2.dp.toPx() else 3.8.dp.toPx(),
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
                        containerColor = Color.Black.copy(alpha = 0.55f),
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
                                color = Color(0xEB0E1320),
                                border = BorderStroke(1.dp, palette.primaryAccent)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CyberSignalDot(size = 5.5.dp, glowing = false)
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
                                        tint = palette.primaryAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Frosted Glass Telemetry Footer Below the Cutout Orbital Viewport
                if (selectedLiveState != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    val altKmRaw = "${selectedLiveState.topocentric.satAltKm.roundToInt()}"
                    val elevRaw = String.format(Locale.US, "%+.0f°", selectedLiveState.topocentric.elevationDeg)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFa) selectedLiveState.satellite.nameFa else selectedLiveState.satellite.nameEn,
                                style = RedTypographyTokens.bodyPrimary.copy(
                                    fontWeight = FontWeight.Light,
                                    fontSize = 15.sp
                                ),
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                CyberStatCounter(
                                    valueText = if (isFa) TimeEngine.formatPersianNumbers(altKmRaw) else altKmRaw,
                                    statusLabel = if (isFa) "کیلومتر" else "KM",
                                    dotSize = 1.9.dp,
                                    accentColor = palette.primaryAccent,
                                    showSignalDot = false
                                )
                                CyberStatCounter(
                                    valueText = if (isFa) TimeEngine.formatPersianNumbers(elevRaw) else elevRaw,
                                    statusLabel = if (isFa) "ارتفاع" else "EL",
                                    dotSize = 1.9.dp,
                                    accentColor = if (selectedLiveState.topocentric.elevationDeg > 0) palette.statusGood else palette.secondaryAccent,
                                    showSignalDot = false
                                )
                            }
                        }

                        CyberMeshPillCard(
                            preset = CyberMeshPreset.CYAN_VIOLET,
                            isCapsule = true,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            onClick = { activeDetailSatellite = selectedLiveState.satellite }
                        ) {
                            Text(
                                text = if (isFa) "تله‌متری" else "Telemetry",
                                style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Medium, fontSize = 11.5.sp),
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // 2. SPATIAL FLOATING CAPSULE CATEGORY & MAP-THEME BAR
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
                        val isSelected = cat == selectedCategory
                        if (isSelected) {
                            CyberMeshPillCard(
                                preset = CyberMeshPreset.CORAL_PEACH,
                                isCapsule = true,
                                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 7.dp),
                                onClick = { selectedCategory = cat },
                                modifier = Modifier.testTag("filter_${cat.name}")
                            ) {
                                Text(
                                    text = if (isFa) cat.labelFa else cat.labelEn,
                                    style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
                                    color = Color.White
                                )
                            }
                        } else {
                            CyberFloatingCapsuleBar(
                                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 7.dp),
                                elevation = 4.dp,
                                onClick = { selectedCategory = cat },
                                modifier = Modifier.testTag("filter_${cat.name}")
                            ) {
                                Text(
                                    text = if (isFa) cat.labelFa else cat.labelEn,
                                    style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Light, fontSize = 12.sp),
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                CyberFloatingCapsuleBar(
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    elevation = 4.dp,
                    modifier = Modifier.testTag("map_theme_selector")
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
                                    if (selected) palette.primaryAccent.copy(alpha = 0.24f) else Color.Transparent
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
                                color = if (selected) palette.textPrimary else palette.textMuted
                            )
                        }
                    }
                }
            }

            // 3. ORBITAL TIME-SCRUBBER WIDGET (`-12h..+12h`)
            CyberGlassWidget(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("satellite_time_scrubber_card"),
                cornerRadius = 32.dp,
                contentPadding = PaddingValues(18.dp),
                ambientTint = palette.secondaryAccent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CyberSignalDot(
                            color = if (timeOffsetHours == 0f) palette.statusGood else palette.statusWarn,
                            size = 6.dp
                        )
                        Text(
                            text = if (isFa) "محور زمان مداری (۱۲± ساعت)" else "ORBITAL TIME SCRUBBER (±12H)",
                            style = RedTypographyTokens.caption.copy(
                                fontWeight = FontWeight.Light,
                                fontSize = 11.sp
                            ),
                            color = palette.textSecondary
                        )
                    }

                    val offsetText = if (abs(timeOffsetHours) < 0.05f) {
                        "LIVE"
                    } else {
                        val raw = String.format(Locale.US, "%+.1fH", timeOffsetHours)
                        if (isFa) TimeEngine.formatPersianNumbers(raw) else raw
                    }
                    CyberMatrixText(
                        text = offsetText,
                        dotSize = 1.9.dp,
                        dotGap = 0.75.dp,
                        charGap = 2.2.dp,
                        activeColor = palette.matrixLit,
                        glowColor = palette.primaryAccent
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                CyberGlassSlider(
                    value = timeOffsetHours,
                    onValueChange = { timeOffsetHours = it },
                    valueRange = -12f..12f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("time_scrubber_slider")
                )

                if (abs(timeOffsetHours) >= 0.05f) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        CyberFloatingCapsuleBar(
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            elevation = 2.dp,
                            onClick = { timeOffsetHours = 0f }
                        ) {
                            Text(
                                text = if (isFa) "بازگشت به زمان زنده" else "Reset to Live",
                                style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Light, fontSize = 10.5.sp),
                                color = palette.primaryAccent
                            )
                        }
                    }
                }
            }

            // 4. UPCOMING VISIBLE PASSES (Next 3 Days)
            val roundedStartMs = remember(currentSimulationMs) {
                (currentSimulationMs / 60_000L) * 60_000L
            }
            var allUpcomingPasses by remember {
                mutableStateOf<List<Pair<SatelliteItem, ISSEngine.ISSPass>>?>(null)
            }

            CyberSectionHeader(
                title = if (isFa) "گذرهای قابل رویت (۳ روز آینده)" else "Upcoming Visible Passes",
                subtitle = if (isFa) "گذرهای روشن در آسمان تاریک رصدگر" else "Sunlit orbital transits over your twilight/night horizon",
                tagText = allUpcomingPasses?.size?.let { count ->
                    if (isFa) TimeEngine.formatPersianNumbers(count.toString()) else String.format(Locale.US, "%02d", count)
                }
            )

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

            val currentPasses = allUpcomingPasses
            if (currentPasses == null) {
                CyberGlassWidget(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 30.dp,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Text(
                        text = if (isFa) "در حال محاسبه گذرهای مداری..." else "Propagating SGP4 visible passes...",
                        style = RedTypographyTokens.bodySecondary.copy(fontWeight = FontWeight.Light),
                        color = palette.textSecondary
                    )
                }
            } else if (currentPasses.isEmpty()) {
                CyberGlassWidget(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 30.dp,
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
                    currentPasses.forEach { (sat, pass) ->
                        CyberVisiblePassCard(
                            satName = if (isFa) sat.nameFa else sat.nameEn,
                            pass = pass,
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
                                    val labelStr = if (leadMins == 1440) (if (isFa) "۱ روز" else "1 day")
                                    else (if (isFa) "$leadMins دقیقه" else "$leadMins mins")
                                    Toast.makeText(
                                        context,
                                        if (isFa) "هشدار گذر $labelStr قبل از شروع تنظیم شد!"
                                        else "Alert set $labelStr prior to pass!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                }
            }

            // 5. TRACKED SATELLITES MATRIX DECK
            CyberSectionHeader(
                title = if (isFa) "فهرست ماهواره‌های تحت پایش" else "Tracked Satellites Matrix",
                tagText = if (isFa) {
                    TimeEngine.formatPersianNumbers(filteredSatellites.size.toString())
                } else {
                    String.format(Locale.US, "%02d", filteredSatellites.size)
                }
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                liveSatelliteStates.forEach { state ->
                    val sat = state.satellite
                    val isSelected = sat.id == selectedSatelliteId
                    val elevRaw = String.format(Locale.US, "%+.0f°", state.topocentric.elevationDeg)

                    CyberGlassWidget(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sat_card_${sat.id}"),
                        cornerRadius = 30.dp,
                        contentPadding = PaddingValues(16.dp),
                        elevated = isSelected,
                        ambientTint = if (isSelected) palette.primaryAccent else null,
                        onClick = {
                            selectedSatelliteId = if (isSelected) null else sat.id
                            anchoredSatLabelState = if (isSelected) null else state
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
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) palette.primaryAccent.copy(alpha = 0.22f)
                                            else palette.glassFill
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) palette.primaryAccent else palette.glassBorder,
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SatelliteAlt,
                                        contentDescription = null,
                                        tint = if (isSelected) palette.primaryAccent else palette.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isFa) sat.nameFa else sat.nameEn,
                                        style = RedTypographyTokens.bodyPrimary.copy(
                                            fontWeight = FontWeight.Light,
                                            fontSize = 15.sp
                                        ),
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val altText = String.format(Locale.US, "%.0f km", state.topocentric.satAltKm)
                                    Text(
                                        text = if (isFa) TimeEngine.formatPersianNumbers("مدار $altText") else "Orbit $altText",
                                        style = RedTypographyTokens.caption.copy(
                                            fontWeight = FontWeight.Light,
                                            fontSize = 11.5.sp
                                        ),
                                        color = palette.textSecondary
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CyberMatrixText(
                                    text = if (isFa) TimeEngine.formatPersianNumbers(elevRaw) else elevRaw,
                                    dotSize = 1.85.dp,
                                    dotGap = 0.7.dp,
                                    charGap = 2.0.dp,
                                    activeColor = if (state.topocentric.elevationDeg > 0) palette.statusGood else palette.matrixLit,
                                    glowColor = if (state.topocentric.elevationDeg > 0) palette.statusGood else palette.primaryAccent
                                )

                                IconButton(
                                    onClick = { activeDetailSatellite = sat },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("open_sat_detail_${sat.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Details",
                                        tint = palette.primaryAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(104.dp))
        }
    }

    var selectedSatIdsForAlerts by remember {
        val savedSet = prefs.getStringSet("monitored_satellite_ids", null)
        mutableStateOf(savedSet ?: setOf("iss_zarya", "tiangong", "starlink_train", "hubble"))
    }

    if (showLeadTimeSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showLeadTimeSelectionDialog = false },
            title = {
                Text(
                    text = if (isFa) "سیستم هشدار خودکار ماهواره‌ها" else "Automated Satellite Pass Alerts",
                    style = RedTypographyTokens.sectionTitle.copy(fontWeight = FontWeight.Medium)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (isFa) "زمان یادآوری قبل از آغاز گذر:" else "Notification timing before pass:",
                        style = RedTypographyTokens.bodyPrimary.copy(fontWeight = FontWeight.Medium),
                        color = palette.primaryAccent
                    )

                    listOf(
                        10 to (if (isFa) "۱۰ دقیقه قبل" else "10 minutes before"),
                        30 to (if (isFa) "۳۰ دقیقه قبل" else "30 minutes before"),
                        1440 to (if (isFa) "۱ روز قبل (۲۴ ساعت)" else "1 day before (24 hours)")
                    ).forEach { (mins, label) ->
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
                            Text(text = label, style = RedTypographyTokens.bodySecondary)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    SatelliteCatalog.getVisibleList(detectedTrain).forEach { sat ->
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
                            verticalAlignment = Alignment.CenterVertically
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFa) sat.nameFa else sat.nameEn,
                                style = RedTypographyTokens.bodySecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeadTimeSelectionDialog = false
                        executeWithNotificationPermission {
                            isAutoAlertEnabled = true
                            prefs.edit()
                                .putBoolean("auto_satellite_alerts_enabled", true)
                                .putBoolean("iss_auto_alerts_enabled", true)
                                .putStringSet("monitored_satellite_ids", selectedSatIdsForAlerts)
                                .putInt("satellite_alert_lead_minutes", selectedLeadMinutes)
                                .putInt("iss_alert_lead_minutes", selectedLeadMinutes)
                                .putString("user_city_name_fa", uiState.userLocation.cityNameFa)
                                .putString("user_city_name_en", uiState.userLocation.cityNameEn)
                                .putFloat("user_lat", uiState.userLocation.latitude.toFloat())
                                .putFloat("user_lon", uiState.userLocation.longitude.toFloat())
                                .apply()

                            coroutineScope.launch(Dispatchers.Default) {
                                AstroNotificationManager.scheduleRollingSatellitePasses(
                                    context = context,
                                    userLocation = uiState.userLocation,
                                    selectedSatIds = selectedSatIdsForAlerts,
                                    leadMinutes = selectedLeadMinutes,
                                    scanDays = 7
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primaryAccent)
                ) {
                    Text(text = if (isFa) "فعال‌سازی" else "Enable", color = Color.White)
                }
            },
            dismissButton = {
                Row {
                    if (isAutoAlertEnabled) {
                        TextButton(
                            onClick = {
                                showLeadTimeSelectionDialog = false
                                isAutoAlertEnabled = false
                                prefs.edit()
                                    .putBoolean("auto_satellite_alerts_enabled", false)
                                    .putBoolean("iss_auto_alerts_enabled", false)
                                    .apply()
                                AstroNotificationManager.cancelAllPassNotifications(context)
                            }
                        ) {
                            Text(
                                text = if (isFa) "غیرفعال‌سازی" else "Disable",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    TextButton(onClick = { showLeadTimeSelectionDialog = false }) {
                        Text(text = if (isFa) "انصراف" else "Cancel")
                    }
                }
            }
        )
    }

    if (showTutorialDialog) {
        AlertDialog(
            onDismissRequest = { showTutorialDialog = false },
            title = {
                Text(
                    text = if (isFa) "راهنمای مرکز ردیابی ماهواره‌ها" else "Orbital Tracking Guide",
                    style = RedTypographyTokens.sectionTitle.copy(fontWeight = FontWeight.Medium)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isFa) "پنجره برش‌خورده نقشه زمین قاره‌ها، مرز واقعی شب و روز و مسیر حرکت مداری ماهواره انتخاب‌شده را نمایش می‌دهد."
                        else "The crisp cutout Earth viewport displays continents, real solar terminator, night-side city lights, and live SGP4 ground tracks.",
                        style = RedTypographyTokens.bodySecondary
                    )
                    Text(
                        text = if (isFa) "با محور زمان ۱۲± ساعت می‌توانید موقعیت گذشته و آینده ماهواره‌ها را شبیه‌سازی کنید."
                        else "Use the ±12h orbital time scrubber to inspect past and upcoming orbital positions.",
                        style = RedTypographyTokens.bodySecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTutorialDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primaryAccent)
                ) {
                    Text(text = if (isFa) "متوجه شدم" else "Got It", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun CyberVisiblePassCard(
    satName: String,
    pass: ISSEngine.ISSPass,
    isFa: Boolean,
    onSchedulePassReminder: (leadMinutes: Int) -> Unit
) {
    val palette = CyberTheme.palette
    var showMenu by remember { mutableStateOf(false) }

    val calendarSystem = if (isFa) CalendarSystem.SOLAR_HIJRI else CalendarSystem.GREGORIAN
    val dateStr = TimeEngine.formatDate(pass.startTimeMs, calendarSystem, isFa)
    val riseStr = TimeEngine.formatTime24h(pass.startTimeMs, isFa)
    val maxStr = TimeEngine.formatTime24h(pass.maxTimeMs, isFa)
    val setStr = TimeEngine.formatTime24h(pass.endTimeMs, isFa)

    val maxElevRaw = "${pass.maxElevationDeg.roundToInt()}°"
    val maxElevStr = if (isFa) TimeEngine.formatPersianNumbers(maxElevRaw) else maxElevRaw
    val durationMin = ((pass.endTimeMs - pass.startTimeMs) / 60_000L).coerceAtLeast(1)
    val durText = if (isFa) "${TimeEngine.formatPersianNumbers(durationMin.toString())} دقیقه" else "${durationMin}m"

    CyberGlassWidget(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 32.dp,
        contentPadding = PaddingValues(18.dp),
        ambientTint = palette.primaryAccent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    CyberSignalDot(size = 5.5.dp, color = palette.statusGood)
                    Text(
                        text = satName,
                        style = RedTypographyTokens.bodyPrimary.copy(
                            fontWeight = FontWeight.Light,
                            fontSize = 15.5.sp
                        ),
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$dateStr  •  $durText",
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
                CyberDotRingGauge(
                    progress = (pass.maxElevationDeg.toFloat() / 90f).coerceIn(0.08f, 1f),
                    centerText = maxElevStr,
                    diameter = 46.dp,
                    accentColor = palette.primaryAccent,
                    secondaryColor = palette.secondaryAccent
                )

                Box {
                    CyberMicroIconButton(
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

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(thickness = 0.5.dp, color = palette.glassBorder.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CyberPassTimelineStop(
                label = if (isFa) "طلوع" else "RISE",
                time = riseStr,
                accent = palette.textSecondary
            )
            CyberPassTimelineStop(
                label = if (isFa) "اوج" else "MAX",
                time = maxStr,
                accent = palette.primaryAccent
            )
            CyberPassTimelineStop(
                label = if (isFa) "غروب" else "SET",
                time = setStr,
                accent = palette.textSecondary
            )
        }
    }
}

@Composable
private fun CyberPassTimelineStop(
    label: String,
    time: String,
    accent: Color
) {
    val palette = CyberTheme.palette
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = RedTypographyTokens.caption.copy(
                fontWeight = FontWeight.Light,
                fontSize = 10.sp
            ),
            color = palette.textMuted
        )
        Spacer(modifier = Modifier.height(4.dp))
        CyberMatrixText(
            text = time,
            dotSize = 1.65.dp,
            dotGap = 0.65.dp,
            charGap = 1.8.dp,
            activeColor = palette.matrixLit,
            glowColor = accent
        )
    }
}
