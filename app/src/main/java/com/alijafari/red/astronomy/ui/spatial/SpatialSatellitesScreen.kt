package com.alijafari.red.astronomy.ui.spatial

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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import com.alijafari.red.astronomy.ui.theme.RedCornerRadius
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape
import java.text.SimpleDateFormat
import java.util.Date
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

private fun isSpatialNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/**
 * VisionOS Spatial Glassmorphism Satellites Screen — Exact Default [ISSScreen] Grid Layout.
 *
 * Preserves the exact 5-section vertical stack of Default UI (`ISSScreen`):
 * 0. Status/Action Row (Live SGP4 badge + Alerts & Help buttons)
 * 1. 2:1 Scientific 2D Earth Map (`aspectRatio(2.0f)`) with zoom/pan, anchored satellite popover, and reset FAB
 * 2. Category Filter Chips + Auto/Dark/Light Map Theme Selector Row
 * 3. Compact 12-Hour Time Scrubber Card (`-12h..+12h`)
 * 4. Upcoming Visible Passes (Next 3 Days) chronological card list
 * 5. Tracked Satellites single-column card list
 */
@Composable
fun SpatialSatellitesScreen(
    uiState: MainUiState,
    @Suppress("UNUSED_PARAMETER") viewModel: MainViewModel
) {
    val palette = SpatialTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN
    val canvasFontFamily = LocalAppFontFamily.current
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    val isOnline = remember(context) { isSpatialNetworkAvailable(context) }
    val density = LocalDensity.current

    val prefs = remember { context.getSharedPreferences("astro_app_prefs", Context.MODE_PRIVATE) }
    var isAutoAlertEnabled by remember {
        mutableStateOf(prefs.getBoolean("auto_satellite_alerts_enabled", prefs.getBoolean("iss_auto_alerts_enabled", false)))
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

    val mapShape = squircleShape(RedCornerRadius.lg)
    val mapRimBrush = remember(palette) {
        Brush.verticalGradient(
            listOf(palette.rimTopHighlight, palette.rimSideBorder, palette.rimBottomReflection)
        )
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
            verticalArrangement = Arrangement.spacedBy(RedSpacing.lg)
        ) {
            // 0. Status/Action Row (Exact Default Layout)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                ) {
                    if (timeOffsetHours == 0f) {
                        SpatialMeshPill(
                            preset = SpatialMeshPreset.ROSE_TO_ORANGE,
                            isCapsule = true,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (isFa) "پایش زنده SGP4" else "Live SGP4 Orbit",
                                style = RedTypographyTokens.badge.copy(fontSize = 11.sp),
                                color = Color(0xFF23111C)
                            )
                        }
                    } else {
                        SpatialMeshPill(
                            preset = SpatialMeshPreset.BLUE_TO_PURPLE,
                            isCapsule = true,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (isFa) "شبیه‌سازی زمانی" else "Time Simulation",
                                style = RedTypographyTokens.badge.copy(fontSize = 11.sp),
                                color = Color.White
                            )
                        }
                    }

                    if (!isOnline) {
                        SpatialGlassCapsule(
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            elevation = 4.dp
                        ) {
                            Text(
                                text = if (isFa) "آفلاین" else "Offline",
                                style = RedTypographyTokens.badge.copy(fontSize = 10.5.sp),
                                color = palette.textSecondary
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                ) {
                    SpatialGlowIconButton(
                        icon = if (isAutoAlertEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                        contentDescription = "Alerts",
                        onClick = { showLeadTimeSelectionDialog = true },
                        size = 36.dp,
                        iconSize = 18.dp,
                        isActive = isAutoAlertEnabled,
                        modifier = Modifier.testTag("satellite_alerts_toggle")
                    )

                    SpatialGlowIconButton(
                        icon = Icons.Default.HelpOutline,
                        contentDescription = "Help",
                        onClick = { showTutorialDialog = true },
                        size = 36.dp,
                        iconSize = 18.dp,
                        modifier = Modifier.testTag("satellite_help_button")
                    )
                }
            }

            // 1. SCIENTIFICALLY ACCURATE 2D EARTH MAP (Exact 2:1 Aspect Ratio)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.0f)
                    .shadow(16.dp, mapShape, ambientColor = palette.ambientDropShadow, spotColor = palette.ambientDropShadow)
                    .clip(mapShape)
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
                    .border(BorderStroke(1.dp, mapRimBrush), mapShape)
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

                    val oceanColor = if (activeMapDark) Color(0xFF130D21) else Color(0xFFEBD9CB)
                    val landColor = if (activeMapDark) Color(0xFF261B3A) else Color(0xFFFAF1EA)
                    val landStroke = if (activeMapDark) Color(0x47FFB0D0) else Color(0x407A4B3A)
                    val gridColor = if (activeMapDark) Color(0x18FFFFFF) else Color(0x183A2218)
                    val equatorColor = if (activeMapDark) SpatialSkyBlue.copy(alpha = 0.45f) else SpatialNeonRoseDeep.copy(alpha = 0.35f)
                    val nightShadowColor = if (activeMapDark) Color(0xAD07040F) else Color(0x332A1832)
                    val cityLightColor = SpatialWarmPeach

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
                        drawLine(equatorColor, Offset(0f, eqY), Offset(w, eqY), 1.5f)
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
                                    color = cityLightColor.copy(alpha = 0.85f * alpha),
                                    radius = city.sizeDp * zoomScale * 0.75f,
                                    center = Offset(cx, cy)
                                )
                            }
                        }
                    }

                    val userX = mapX(uiState.userLocation.longitude)
                    val userY = mapY(uiState.userLocation.latitude)
                    if (userX in 0f..w && userY in 0f..h) {
                        drawCircle(SpatialMintGlow.copy(alpha = 0.32f), radius = 12.dp.toPx(), center = Offset(userX, userY))
                        drawCircle(SpatialMintGlow, radius = 4.dp.toPx(), center = Offset(userX, userY))

                        val rawCity = if (isFa) uiState.userLocation.cityNameFa else uiState.userLocation.cityNameEn
                        val cityLabel = if (isFa) TimeEngine.formatPersianNumbers(rawCity) else rawCity
                        val textLayoutResult = textMeasurer.measure(
                            text = cityLabel,
                            style = TextStyle(
                                fontFamily = canvasFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeMapDark) Color.White else Color(0xFF1F1525)
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
                        drawPath(path = trackPath, color = SpatialNeonRose.copy(alpha = 0.30f), style = Stroke(width = 7f))
                        drawPath(
                            path = trackPath,
                            color = SpatialNeonRose,
                            style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
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
                                        colors = listOf(SpatialNeonRose.copy(alpha = 0.65f), Color.Transparent),
                                        center = Offset(sx, sy),
                                        radius = 16.dp.toPx()
                                    ),
                                    radius = 16.dp.toPx(),
                                    center = Offset(sx, sy)
                                )
                            }
                            drawCircle(
                                color = if (isSelected) SpatialNeonRoseVibrant else SpatialWarmPeach,
                                radius = if (isSelected) 5.5.dp.toPx() else 4.dp.toPx(),
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
                                .width(150.dp)
                                .clickable { activeDetailSatellite = satState.satellite }
                                .testTag("anchored_sat_label"),
                            shape = squircleShape(14.dp),
                            color = Color(0xEB1A1228),
                            border = BorderStroke(1.dp, SpatialNeonRose)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SatelliteAlt,
                                    contentDescription = null,
                                    tint = SpatialNeonRose,
                                    modifier = Modifier.size(14.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isFa) satState.satellite.nameFa else satState.satellite.nameEn,
                                        style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val altStr = String.format(Locale.US, "%.0f km", satState.topocentric.satAltKm)
                                    Text(
                                        text = if (isFa) TimeEngine.formatPersianNumbers("ارتفاع $altStr") else "Alt $altStr",
                                        style = RedTypographyTokens.caption.copy(fontSize = 9.5.sp),
                                        color = Color.White.copy(alpha = 0.78f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Detail",
                                    tint = SpatialNeonRose,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. MAP THEME & CATEGORY CONTROLS AREA (Exact Default Row Outside Map)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                ) {
                    items(SatelliteCategory.entries) { cat ->
                        val isSelected = cat == selectedCategory
                        if (isSelected) {
                            SpatialMeshPill(
                                preset = SpatialMeshPreset.ROSE_TO_ORANGE,
                                isCapsule = true,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                onClick = { selectedCategory = cat },
                                modifier = Modifier.testTag("filter_${cat.name}")
                            ) {
                                Text(
                                    text = if (isFa) cat.labelFa else cat.labelEn,
                                    style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                    color = Color(0xFF23111C)
                                )
                            }
                        } else {
                            SpatialGlassCapsule(
                                isActive = false,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                elevation = 4.dp,
                                onClick = { selectedCategory = cat },
                                modifier = Modifier.testTag("filter_${cat.name}")
                            ) {
                                Text(
                                    text = if (isFa) cat.labelFa else cat.labelEn,
                                    style = RedTypographyTokens.caption.copy(fontSize = 12.sp),
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(RedSpacing.sm))

                SpatialGlassCapsule(
                    contentPadding = PaddingValues(3.dp),
                    elevation = 4.dp,
                    modifier = Modifier.testTag("map_theme_selector")
                ) {
                    listOf(
                        Triple(MapTheme.AUTO, if (isFa) "خودکار" else "Auto", "map_theme_auto"),
                        Triple(MapTheme.DARK, if (isFa) "تاریک" else "Dark", "map_theme_dark"),
                        Triple(MapTheme.LIGHT, if (isFa) "روشن" else "Light", "map_theme_light")
                    ).forEach { (theme, label, _) ->
                        val isSelected = mapTheme == theme
                        Box(
                            modifier = Modifier
                                .clip(RedShapes.capsule)
                                .background(
                                    if (isSelected) SpatialNeonRose.copy(alpha = if (palette.isDark) 0.34f else 0.25f)
                                    else Color.Transparent
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
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) palette.accentInk else palette.textSecondary
                            )
                        }
                    }
                }
            }

            // 3. COMPACT TIME SCRUBBER (-12h to +12h, Exact Default Card Slot)
            SpatialGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("satellite_time_scrubber_card"),
                cornerRadius = RedCornerRadius.lg,
                contentPadding = PaddingValues(RedSpacing.lg),
                elevation = 12.dp,
                glowAccent = SpatialNeonRose
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = palette.accentInk,
                            modifier = Modifier.size(16.dp)
                        )

                        val isPersianCalendar = isFa || uiState.calendarSystem == CalendarSystem.SOLAR_HIJRI
                        val timeStr = remember(currentSimulationMs, isPersianCalendar, isFa) {
                            if (isPersianCalendar) {
                                val sh = TimeEngine.toSolarHijri(currentSimulationMs, TimeEngine.TEHRAN_TIME_ZONE)
                                val timePart = SimpleDateFormat("HH:mm", Locale.US).apply {
                                    timeZone = TimeEngine.TEHRAN_TIME_ZONE
                                }.format(Date(currentSimulationMs))
                                val raw = "${sh.day} ${sh.monthNameFa}، $timePart"
                                if (isFa) TimeEngine.formatPersianNumbers(raw) else raw
                            } else {
                                SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date(currentSimulationMs))
                            }
                        }

                        Text(
                            text = timeStr,
                            style = RedTypographyTokens.bodySecondary.copy(fontWeight = FontWeight.SemiBold),
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val rawOff = if (timeOffsetHours == 0f) "0.0H" else String.format(
                            Locale.US,
                            "%s%.1fH",
                            if (timeOffsetHours > 0f) "+" else "",
                            timeOffsetHours
                        )
                        DotMatrixText(
                            text = if (isFa) TimeEngine.formatPersianNumbers(rawOff) else rawOff,
                            dotSize = 1.9.dp,
                            activeColor = palette.dotMatrixInk,
                            glowColor = SpatialNeonRose
                        )
                        SpatialGlowToggle(
                            checked = timeOffsetHours == 0f,
                            onCheckedChange = { live ->
                                timeOffsetHours = if (live) 0f else 1.0f
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(RedSpacing.xs))

                SpatialGlowSlider(
                    value = timeOffsetHours,
                    onValueChange = { raw ->
                        val rounded = (raw * 60f).roundToInt() / 60f
                        timeOffsetHours = rounded
                    },
                    valueRange = -12f..12f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("time_scrubber_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isFa) "۱۲ ساعت قبل" else "12h ago",
                        style = RedTypographyTokens.caption,
                        color = palette.textSecondary
                    )
                    Text(
                        text = if (isFa) "زنده" else "Live",
                        style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold),
                        color = palette.accentInk
                    )
                    Text(
                        text = if (isFa) "۱۲ ساعت بعد" else "12h ahead",
                        style = RedTypographyTokens.caption,
                        color = palette.textSecondary
                    )
                }
            }

            // 4. UPCOMING VISIBLE PASSES PREDICTIONS (Next 3 Days — Exact Default Single-Column Stack)
            val roundedStartMs = remember(currentSimulationMs) {
                (currentSimulationMs / 60_000L) * 60_000L
            }

            var allUpcomingPasses by remember {
                mutableStateOf<List<Pair<SatelliteItem, ISSEngine.ISSPass>>?>(null)
            }

            SpatialSectionHeader(
                title = if (isFa) "گذرهای قابل مشاهده بعدی (۳ روز آینده)" else "Upcoming Visible Passes (Next 3 Days)",
                badgeText = if (allUpcomingPasses?.isNotEmpty() == true) {
                    val c = allUpcomingPasses!!.size.toString()
                    if (isFa) TimeEngine.formatPersianNumbers(c) else c
                } else null
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

            val currentAllPasses = allUpcomingPasses
            if (currentAllPasses == null) {
                SpatialGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = RedCornerRadius.lg,
                    contentPadding = PaddingValues(RedSpacing.lg)
                ) {
                    Text(
                        text = if (isFa) "در حال محاسبه گذرهای ماهواره‌ها..." else "Calculating satellite passes...",
                        style = RedTypographyTokens.bodySecondary,
                        color = palette.textSecondary
                    )
                }
            } else if (currentAllPasses.isEmpty()) {
                SpatialGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = RedCornerRadius.lg,
                    contentPadding = PaddingValues(RedSpacing.lg)
                ) {
                    Text(
                        text = if (isFa) "هیچ گذر قابل مشهودی مطابق با معیار علمی در ۳ روز آینده یافت نشد."
                        else "No visible passes meeting scientific criteria predicted in next 3 days.",
                        style = RedTypographyTokens.bodySecondary,
                        color = palette.textSecondary
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)) {
                    currentAllPasses.forEach { (sat, pass) ->
                        SpatialVisiblePassCard(
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

            // 5. TRACKED SATELLITES LIST (Exact Default Single-Column Card List)
            SpatialSectionHeader(
                title = if (isFa) "فهرست ماهواره‌ها" else "Tracked Satellites",
                badgeText = if (isFa) TimeEngine.formatPersianNumbers("${filteredSatellites.size}") else "${filteredSatellites.size}"
            )

            Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)) {
                liveSatelliteStates.forEach { state ->
                    val sat = state.satellite
                    val isSelected = sat.id == selectedSatelliteId

                    SpatialGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sat_card_${sat.id}"),
                        cornerRadius = RedCornerRadius.xl,
                        contentPadding = PaddingValues(RedSpacing.md),
                        elevation = if (isSelected) 14.dp else 8.dp,
                        elevated = isSelected,
                        glowAccent = if (isSelected) SpatialNeonRose else null,
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
                                horizontalArrangement = Arrangement.spacedBy(RedSpacing.md),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) SpatialNeonRose.copy(alpha = 0.28f)
                                            else palette.glassFill
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) SpatialNeonRose else palette.rimSideBorder,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SatelliteAlt,
                                        contentDescription = null,
                                        tint = if (isSelected) palette.accentInk else palette.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = if (isFa) sat.nameFa else sat.nameEn,
                                        style = RedTypographyTokens.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val altStr = String.format(Locale.US, "%.0fKM", state.topocentric.satAltKm)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        DotMatrixText(
                                            text = if (isFa) TimeEngine.formatPersianNumbers(altStr) else altStr,
                                            dotSize = 1.65.dp,
                                            activeColor = palette.dotMatrixInk,
                                            glowColor = SpatialNeonRose
                                        )
                                        val magStr = String.format(Locale.US, "Mag %+.1f", state.apparentMagnitude)
                                        Text(
                                            text = if (isFa) TimeEngine.formatPersianNumbers(magStr) else magStr,
                                            style = RedTypographyTokens.caption,
                                            color = palette.textSecondary
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                            ) {
                                if (state.isNakedEyeVisible) {
                                    SpatialMeshPill(
                                        preset = SpatialMeshPreset.ROSE_TO_ORANGE,
                                        isCapsule = true,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isFa) "با چشم" else "Naked Eye",
                                            style = RedTypographyTokens.badge.copy(fontSize = 10.sp),
                                            color = Color(0xFF23111C)
                                        )
                                    }
                                } else {
                                    SpatialGlassCapsule(
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        elevation = 2.dp
                                    ) {
                                        Text(
                                            text = if (isFa) "تلسکوپی" else "Telescopic",
                                            style = RedTypographyTokens.badge.copy(fontSize = 10.sp),
                                            color = palette.textTertiary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { activeDetailSatellite = sat },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("open_sat_detail_${sat.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Details",
                                        tint = palette.accentInk,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(96.dp))
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
                    style = RedTypographyTokens.sectionTitle.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (isFa) "زمان یادآوری قبل از آغاز گذر:" else "Notification timing before pass:",
                        style = RedTypographyTokens.bodyPrimary.copy(fontWeight = FontWeight.Bold),
                        color = palette.accentInk
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
                            Text(text = label, style = RedTypographyTokens.bodySecondary)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Text(
                        text = if (isFa) "ماهواره‌های تحت پایش خودکار:" else "Monitored satellites:",
                        style = RedTypographyTokens.bodyPrimary.copy(fontWeight = FontWeight.Bold),
                        color = palette.accentInk
                    )

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

                            Toast.makeText(
                                context,
                                if (isFa) "سیستم پایش خودکار ۷ روزه برای ماهواره‌های انتخاب شده فعال شد!"
                                else "7-day automated pass schedule enabled for selected satellites!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpatialNeonRoseDeep)
                ) {
                    Text(text = if (isFa) "فعال‌سازی سیستم هشدار" else "Enable Monitoring", color = Color.White)
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
                                Toast.makeText(
                                    context,
                                    if (isFa) "هشدارهای خودکار ماهواره غیرفعال شدند."
                                    else "Automated satellite alerts disabled.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Text(
                                text = if (isFa) "غیرفعال‌سازی" else "Disable Alerts",
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
                    text = if (isFa) "راهنمای مرکز ردیابی ماهواره‌ها" else "Satellite Tracking Guide",
                    style = RedTypographyTokens.sectionTitle.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    SpatialTutorialItem(
                        title = if (isFa) "نقشه دقیق زمین:" else "Accurate Earth Map:",
                        desc = if (isFa) "نمایش قاره‌ها با پروژکسیون دقیق جغرافیایی، خط مرز روز و شب واقعی و روشنایی شهرهای شب."
                        else "Displays accurate geographic continents, real astronomical terminator boundary, and night-side city lights."
                    )
                    SpatialTutorialItem(
                        title = if (isFa) "تعامل با ماهواره:" else "Satellite Interaction:",
                        desc = if (isFa) "با لمس هر ماهواره، برچسب شناور نام و ارتفاع ظاهر می‌شود. لمس مجدد برچسب، صفحه جزئیات کامل را باز می‌کند."
                        else "Tap any satellite to show an anchored label. Tap the label to open full satellite details and timeline."
                    )
                    SpatialTutorialItem(
                        title = if (isFa) "محور زمان ۱۲ ساعته:" else "12-Hour Time Scrubber:",
                        desc = if (isFa) "بررسی و شبیه‌سازی موقعیت ماهواره‌ها از ۱۲ ساعت قبل تا ۱۲ ساعت بعد."
                        else "Scrub from -12h in the past to +12h into the future to simulate satellite motion and day/night illumination."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTutorialDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SpatialNeonRoseDeep)
                ) {
                    Text(text = if (isFa) "متوجه شدم" else "Got It", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun SpatialTutorialItem(title: String, desc: String) {
    val palette = SpatialTheme.palette
    Column {
        Text(text = title, style = RedTypographyTokens.bodyPrimary.copy(fontWeight = FontWeight.Bold), color = palette.accentInk)
        Text(text = desc, style = RedTypographyTokens.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SpatialVisiblePassCard(
    satName: String,
    pass: ISSEngine.ISSPass,
    isFa: Boolean,
    onSchedulePassReminder: (leadMinutes: Int) -> Unit
) {
    val palette = SpatialTheme.palette
    var showMenu by remember { mutableStateOf(false) }

    val calendarSystem = if (isFa) CalendarSystem.SOLAR_HIJRI else CalendarSystem.GREGORIAN
    val dateStr = TimeEngine.formatDate(pass.startTimeMs, calendarSystem, isFa)
    val riseStr = TimeEngine.formatTime24h(pass.startTimeMs, isFa)
    val maxStr = TimeEngine.formatTime24h(pass.maxTimeMs, isFa)
    val setStr = TimeEngine.formatTime24h(pass.endTimeMs, isFa)

    val maxElevRaw = "${pass.maxElevationDeg.roundToInt()}°"
    val maxElevStr = if (isFa) TimeEngine.formatPersianNumbers(maxElevRaw) else maxElevRaw
    val durationMin = ((pass.endTimeMs - pass.startTimeMs) / 60_000L).coerceAtLeast(1)

    SpatialGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = RedCornerRadius.window,
        contentPadding = PaddingValues(16.dp),
        elevation = 12.dp,
        glowAccent = SpatialNeonRose
    ) {
        // Header Row: Satellite Name + Compact Radial Visibility Meter + Classification Pill + Alert Bell
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.SatelliteAlt,
                    contentDescription = null,
                    tint = palette.accentInk,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = satName,
                    style = RedTypographyTokens.bodyPrimary.copy(fontWeight = FontWeight.Bold),
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val scoreNorm = (pass.visibilityScore / 100f).coerceIn(0.15f, 1f)
                val scoreText = if (isFa) TimeEngine.formatPersianNumbers("${pass.visibilityScore}") else "${pass.visibilityScore}"
                SpatialCompactRadialMeter(
                    progress = scoreNorm,
                    centerValueText = scoreText,
                    diameter = 38.dp,
                    activeColor = SpatialMintGlow,
                    secondaryActiveColor = SpatialNeonRose
                )

                SpatialMeshPill(
                    preset = SpatialMeshPreset.BLUE_TO_PURPLE,
                    isCapsule = true,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isFa) pass.classification.labelFa else pass.classification.labelEn,
                        style = RedTypographyTokens.badge.copy(fontSize = 10.sp),
                        color = Color.White,
                        maxLines = 1
                    )
                }

                Box {
                    SpatialGlowIconButton(
                        icon = Icons.Default.NotificationsNone,
                        contentDescription = null,
                        onClick = { showMenu = true },
                        size = 30.dp,
                        iconSize = 15.dp
                    )
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        listOf(
                            10 to (if (isFa) "۱۰ دقیقه قبل" else "10 mins before"),
                            30 to (if (isFa) "۳۰ دقیقه قبل" else "30 mins before"),
                            1440 to (if (isFa) "۱ روز قبل" else "1 day before")
                        ).forEach { (mins, label) ->
                            DropdownMenuItem(
                                text = { Text(label, style = RedTypographyTokens.bodySecondary) },
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

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Event, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(14.dp))
                Text(text = dateStr, style = RedTypographyTokens.caption, color = palette.textSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(14.dp))
                Text(
                    text = if (isFa) "مدت: ${TimeEngine.formatPersianNumbers("$durationMin")} دقیقه" else "Duration: $durationMin min",
                    style = RedTypographyTokens.caption,
                    color = palette.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.75.dp)
                .background(palette.rimSideBorder.copy(alpha = 0.55f))
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Rise / Max (LED Dot-Matrix) / Set Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = if (isFa) "طلوع (آغاز)" else "Rise", style = RedTypographyTokens.caption, color = palette.textSecondary)
                DotMatrixText(
                    text = riseStr,
                    dotSize = 1.85.dp,
                    activeColor = palette.dotMatrixInk,
                    glowColor = null
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isFa) "اوج ($maxElevStr)" else "Max ($maxElevStr)",
                    style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.Bold),
                    color = palette.accentInk
                )
                DotMatrixText(
                    text = maxStr,
                    dotSize = 1.95.dp,
                    activeColor = palette.dotMatrixInk,
                    glowColor = SpatialNeonRose
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = if (isFa) "غروب (پایان)" else "Set", style = RedTypographyTokens.caption, color = palette.textSecondary)
                DotMatrixText(
                    text = setStr,
                    dotSize = 1.85.dp,
                    activeColor = palette.dotMatrixInk,
                    glowColor = null
                )
            }
        }
    }
}
