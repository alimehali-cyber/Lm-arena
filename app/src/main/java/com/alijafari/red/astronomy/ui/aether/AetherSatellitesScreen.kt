package com.alijafari.red.astronomy.ui.aether

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private fun isAetherNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

private fun String.aetherSatDigits(isFa: Boolean): String =
    if (isFa) TimeEngine.formatPersianNumbers(this) else this

@Composable
fun AetherSatellitesScreen(
    uiState: MainUiState,
    @Suppress("UNUSED_PARAMETER") viewModel: MainViewModel
) {
    val context = LocalContext.current
    val palette = AetherTheme.palette
    val appFont = LocalAppFontFamily.current
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val isFa = uiState.language == AppLanguage.PERSIAN
    val isOnline = remember(context) { isAetherNetworkAvailable(context) }

    val statusTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

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

    val selectedSatItem = remember(selectedSatelliteId, detectedTrain, visibleSatellites) {
        selectedSatelliteId?.let { id -> SatelliteCatalog.getById(id, detectedTrain) }
            ?: visibleSatellites.firstOrNull()
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

    if (activeDetailSatellite != null) {
        BackHandler { activeDetailSatellite = null }
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
        modifier = Modifier
            .fillMaxSize()
            .testTag("satellites_screen"),
        contentPadding = PaddingValues(
            top = (statusTopPadding + 14.dp).coerceAtLeast(24.dp),
            bottom = navBottomPadding + 106.dp,
            start = 16.dp,
            end = 16.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. FLOATING TOP STATUS CAPSULE + MICRO-ICON DISCS
        item(key = "aether_sat_header") {
            val altHeadline = selectedLiveState?.topocentric?.satAltKm?.roundToInt() ?: 418
            val modeWord = if (abs(timeOffsetHours) < 0.05f) {
                if (isFa) "زنده" else "Live"
            } else {
                if (isFa) "شبیه‌سازی" else "Scrub"
            }
            val netSuffix = if (!isOnline) (if (isFa) " • آفلاین" else " • Offline") else ""
            val capsuleText = if (isFa) {
                "SGP4 • ${altHeadline.toString().aetherSatDigits(true)} km • $modeWord$netSuffix"
            } else {
                "SGP4 • $altHeadline km • $modeWord$netSuffix"
            }

            Row(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AetherFloatingStatusCapsule(
                    statusText = capsuleText,
                    onClick = { timeOffsetHours = 0f },
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AetherMicroIconDisc(
                        icon = if (isAutoAlertEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                        contentDescription = "Alerts",
                        isActive = isAutoAlertEnabled,
                        onClick = { showLeadTimeSelectionDialog = true },
                        modifier = Modifier.testTag("satellite_alerts_toggle")
                    )

                    AetherMicroIconDisc(
                        icon = Icons.Default.HelpOutline,
                        contentDescription = "Help",
                        onClick = { showTutorialDialog = true },
                        modifier = Modifier.testTag("satellite_help_button")
                    )
                }
            }
        }

        // 2. HERO FRAMELESS SAPPHIRE JEWEL ORBITAL VIEWPORT CARD (36.dp)
        item(key = "aether_sat_hero_card") {
            val satName = selectedSatItem?.let { if (isFa) it.nameFa else it.nameEn }
                ?: (if (isFa) "ایستگاه فضایی" else "ISS (Zarya)")
            val altKmVal = selectedLiveState?.topocentric?.satAltKm ?: 418.0
            val altStr = altKmVal.roundToInt().toString().aetherSatDigits(isFa)
            val isVisibleNow = selectedLiveState?.isNakedEyeVisible == true

            AetherJewelTelemetryCard(
                preset = AetherJewelPreset.SAPPHIRE,
                cornerRadius = AetherCornerGeometry.heroCard,
                contentPadding = PaddingValues(20.dp),
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = satName,
                            style = RedTypographyTokens.sectionTitle.copy(
                                fontFamily = appFont,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = if (isFa) 0.sp else (-0.3).sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            AetherRollingDotMatrixCounter(
                                text = altStr,
                                dotSize = 3.8.dp,
                                dotGap = 1.25.dp,
                                charGap = 3.8.dp,
                                activeColor = Color.White,
                                glowColor = Color(0xFF4CA8E0)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "km",
                                style = RedTypographyTokens.caption.copy(
                                    fontFamily = appFont,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Light
                                ),
                                color = Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        AetherLegendDotItem(
                            label = if (isFa) "مرئی" else "Visible",
                            filled = isVisibleNow
                        )
                        AetherLegendDotItem(
                            label = if (isFa) "تاریک/پنهان" else "Occluded",
                            filled = !isVisibleNow
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2:1 Edge-Lit Rim-Bordered Earth Ground-Track Canvas
                val viewportShape = squircleShape(22.dp)
                val subSolarPoint = remember(currentSimulationMs) {
                    SatelliteEngine.calculateSubSolarPoint(currentSimulationMs)
                }
                var labelPosOnScreen by remember { mutableStateOf<Offset?>(null) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2.0f)
                        .clip(viewportShape)
                        .specularRimBorder(
                            shape = viewportShape,
                            strokeWidth = 1.dp,
                            highlightStart = Color.White.copy(alpha = 0.42f),
                            highlightMid = Color.White.copy(alpha = 0.08f),
                            highlightCenter = Color.White.copy(alpha = 0.03f),
                            highlightBounce = Color.White.copy(alpha = 0.20f)
                        )
                ) {
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
                            activeMapWine -> Color(0xFF14030A)
                            activeMapDark -> Color(0xFF060913)
                            else -> Color(0xFFE4ECF7)
                        }
                        val landColor = when {
                            activeMapWine -> Color(0xFF2B0816)
                            activeMapDark -> Color(0xFF0F1B30)
                            else -> Color(0xFFF8FAFC)
                        }
                        val landStroke = when {
                            activeMapWine -> Color(0x4DFF8CA6)
                            activeMapDark -> Color(0x424CA8E0)
                            else -> Color(0x330F172A)
                        }
                        val gridColor = if (activeMapDark) Color(0x14FFFFFF) else Color(0x140F172A)
                        val equatorColor = Color.White.copy(alpha = 0.25f)
                        val nightShadowColor = if (activeMapDark) Color(0xAD020308) else Color(0x2E0F172A)

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
                            drawLine(equatorColor, Offset(0f, eqY), Offset(w, eqY), 1.2f)
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
                                        color = Color(0xFFE07A42).copy(alpha = 0.78f * alpha),
                                        radius = city.sizeDp * zoomScale * 0.70f,
                                        center = Offset(cx, cy)
                                    )
                                }
                            }
                        }

                        val userX = mapX(uiState.userLocation.longitude)
                        val userY = mapY(uiState.userLocation.latitude)
                        if (userX in 0f..w && userY in 0f..h) {
                            drawCircle(Color(0xFF4ADE80).copy(alpha = 0.30f), radius = 10.dp.toPx(), center = Offset(userX, userY))
                            drawCircle(Color(0xFF4ADE80), radius = 3.6.dp.toPx(), center = Offset(userX, userY))

                            val rawCity = if (isFa) uiState.userLocation.cityNameFa else uiState.userLocation.cityNameEn
                            val cityLabel = rawCity.aetherSatDigits(isFa)
                            val textLayoutResult = textMeasurer.measure(
                                text = cityLabel,
                                style = TextStyle(
                                    fontFamily = appFont,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (activeMapDark) Color.White else Color(0xFF0F172A)
                                )
                            )
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(userX - textLayoutResult.size.width / 2f, userY - 17.dp.toPx())
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
                            drawPath(path = trackPath, color = Color.White.copy(alpha = 0.18f), style = Stroke(width = 5f))
                            drawPath(
                                path = trackPath,
                                color = Color.White.copy(alpha = 0.78f),
                                style = Stroke(width = 1.8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f)))
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
                                            colors = listOf(AetherCrimsonCoreEdge.copy(alpha = 0.75f), Color.Transparent),
                                            center = Offset(sx, sy),
                                            radius = 15.dp.toPx()
                                        ),
                                        radius = 15.dp.toPx(),
                                        center = Offset(sx, sy)
                                    )
                                }
                                drawCircle(
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.58f),
                                    radius = if (isSelected) 4.8.dp.toPx() else 3.2.dp.toPx(),
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
                        }
                    }

                    if (anchoredSatLabelState != null && labelPosOnScreen != null) {
                        val pos = labelPosOnScreen!!
                        val sat = anchoredSatLabelState!!.satellite
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = (pos.x - 46f).roundToInt().coerceAtLeast(6),
                                        y = (pos.y - 36f).roundToInt().coerceAtLeast(6)
                                    )
                                }
                                .testTag("anchored_sat_label")
                        ) {
                            AetherMinimalPillBadge(
                                text = if (isFa) sat.nameFa else sat.nameEn,
                                fillColor = Color(0xC80B0C14),
                                onClick = { activeDetailSatellite = sat }
                            )
                        }
                    }

                    if (zoomScale > 1.03f) {
                        AetherMicroIconDisc(
                            icon = Icons.Default.CenterFocusWeak,
                            contentDescription = "Reset Map",
                            onClick = {
                                zoomScale = 1f
                                panOffsetX = 0f
                                panOffsetY = 0f
                            },
                            size = 30.dp,
                            iconSize = 15.dp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .testTag("map_reset_view")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val scrubMetric = if (abs(timeOffsetHours) >= 0.05f) {
                    val sign = if (timeOffsetHours > 0f) "+" else "−"
                    val hrs = String.format(Locale.US, "%.1f", abs(timeOffsetHours)).aetherSatDigits(isFa)
                    if (isFa) "$sign$hrs ساعت" else "$sign$hrs hr"
                } else {
                    val elVal = String.format(
                        Locale.US,
                        "%+.1f°",
                        selectedLiveState?.topocentric?.elevationDeg ?: 0.0
                    ).aetherSatDigits(isFa)
                    if (isFa) "ارتفاع $elVal" else "$elVal Elev"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AetherRollingMetricText(
                        text = scrubMetric,
                        isPersian = isFa,
                        fontSize = 26.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AetherMicroStepper(
                            onDecrement = { timeOffsetHours = (timeOffsetHours - 0.5f).coerceIn(-12f, 12f) },
                            onIncrement = { timeOffsetHours = (timeOffsetHours + 0.5f).coerceIn(-12f, 12f) },
                            onReset = { timeOffsetHours = 0f }
                        )

                        if (selectedSatItem != null) {
                            AetherMinimalPillBadge(
                                text = if (isFa) "جزئیات ←" else "Detail →",
                                onClick = { activeDetailSatellite = selectedSatItem }
                            )
                        }
                    }
                }
            }
        }

        // 3. SPATIAL BENTO ROW: 60-TICK ELEVATION DIAL + GLOWING MESH PILLS
        item(key = "aether_sat_bento") {
            val elevDeg = selectedLiveState?.topocentric?.elevationDeg ?: 0.0
            val altKm = selectedLiveState?.topocentric?.satAltKm ?: 418.0
            // Circular LEO orbital velocity v = sqrt(mu / (Re + h)), mu = 398600.4418 km^3/s^2, Re = 6371 km
            val orbitalVelKmS = sqrt(398600.4418 / (6371.0 + altKm.coerceAtLeast(120.0)))
            val dialFraction = ((elevDeg + 90.0) / 180.0).toFloat().coerceIn(0.05f, 1f)
            val elevStatus = if (elevDeg > 0.0) {
                if (isFa) "بالای افق" else "Above Horz"
            } else {
                if (isFa) "زیر افق" else "Below Horz"
            }

            Row(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AetherRadialDialCard(
                    scoreText = "${elevDeg.roundToInt()}°".aetherSatDigits(isFa),
                    statusLabel = elevStatus,
                    captionTop = if (isFa) "زاویه ناظر" else "Elevation",
                    highlightFraction = dialFraction,
                    onClick = { selectedSatItem?.let { activeDetailSatellite = it } },
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    val speedStr = String.format(Locale.US, "%.2f", orbitalVelKmS).aetherSatDigits(isFa)
                    AetherGlowingMeshPillCard(
                        title = if (isFa) "سرعت مداری" else "Orbital Vel",
                        value = speedStr,
                        unit = "km/s",
                        badge = "LEO",
                        preset = AetherMeshPillPreset.PINK_ROSE,
                        onClick = { selectedSatItem?.let { activeDetailSatellite = it } }
                    )

                    val passCount = allUpcomingPasses?.size ?: 0
                    val passCountStr = String.format(Locale.US, "%02d", passCount).aetherSatDigits(isFa)
                    val noradBadge = selectedSatItem?.let { "N${it.noradId}" } ?: "SGP4"
                    AetherGlowingMeshPillCard(
                        title = if (isFa) "گذرهای ۳ روزه" else "3-Day Passes",
                        value = passCountStr,
                        unit = if (isFa) "گذر" else "vis",
                        badge = noradBadge.aetherSatDigits(isFa),
                        preset = AetherMeshPillPreset.AMBER_COPPER,
                        onClick = { selectedSatItem?.let { activeDetailSatellite = it } }
                    )
                }
            }
        }

        // 4. RIM-LIT CATEGORY & MAP THEME CAPSULES
        item(key = "aether_sat_filters") {
            Row(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SatelliteCategory.entries.forEach { cat ->
                    val active = selectedCategory == cat
                    val label = if (isFa) cat.labelFa else cat.labelEn
                    AetherRimCapsule(
                        isActive = active,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                        onClick = { selectedCategory = cat },
                        modifier = Modifier.testTag("filter_${cat.name}")
                    ) {
                        Text(
                            text = label,
                            style = RedTypographyTokens.caption.copy(
                                fontFamily = appFont,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Medium else FontWeight.Light
                            ),
                            color = if (active) palette.textPrimary else palette.textMicro
                        )
                    }
                }

                val mapLabel = when (mapTheme) {
                    MapTheme.AUTO -> if (isFa) "نقشه خودکار" else "Auto Map"
                    MapTheme.DARK -> if (isFa) "نقشه تاریک" else "Dark Map"
                    MapTheme.LIGHT -> if (isFa) "نقشه روشن" else "Light Map"
                }
                AetherRimCapsule(
                    isActive = false,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                    onClick = {
                        val entries = MapTheme.entries
                        val next = entries[(entries.indexOf(mapTheme) + 1) % entries.size]
                        mapTheme = next
                        prefs.edit().putString("satellite_map_theme", next.name).apply()
                    },
                    modifier = Modifier.testTag("map_theme_selector")
                ) {
                    Text(
                        text = mapLabel,
                        style = RedTypographyTokens.caption.copy(
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = palette.sapphireAccent
                    )
                }
            }
        }

        // 5. FRAMELESS ORBITAL TIME SCRUBBER (±12h) WITH SMOOTH SPLINE CURVE
        item(key = "aether_sat_scrubber") {
            val splineSamples = remember(selectedSatItem, currentSimulationMs) {
                val pts = selectedSatItem?.let {
                    SatelliteEngine.calculateGroundTrack(it, currentSimulationMs)
                }.orEmpty()
                if (pts.size >= 6) {
                    val step = (pts.size / 6).coerceAtLeast(1)
                    (0 until 6).map { idx ->
                        val pt = pts[(idx * step).coerceIn(0, pts.lastIndex)]
                        ((pt.first + 90.0) / 180.0).toFloat().coerceIn(0.12f, 0.88f)
                    }
                } else {
                    listOf(0.28f, 0.62f, 0.84f, 0.52f, 0.24f, 0.68f)
                }
            }
            val scrubFraction = ((timeOffsetHours + 12f) / 24f).coerceIn(0f, 1f)

            AetherFrostedSpatialCard(
                cornerRadius = AetherCornerGeometry.heroCard,
                contentPadding = PaddingValues(20.dp),
                ambientTint = palette.sapphireAccent,
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .testTag("satellite_time_scrubber_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFa) "پیمایش زمانی مدار (±۱۲ ساعت)" else "Orbital Time Scrubber (±12h)",
                        style = RedTypographyTokens.sectionTitle.copy(
                            fontFamily = appFont,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = if (isFa) 0.sp else (-0.3).sp
                        ),
                        color = palette.textPrimary
                    )
                    if (abs(timeOffsetHours) > 0.05f) {
                        AetherMinimalPillBadge(
                            text = if (isFa) "بازگشت به زنده" else "Reset Live",
                            textColor = palette.primaryAccent,
                            borderColor = palette.primaryAccent.copy(alpha = 0.5f),
                            onClick = { timeOffsetHours = 0f }
                        )
                    } else {
                        AetherLegendDotItem(
                            label = if (isFa) "زنده" else "Live",
                            filled = true,
                            dotColor = palette.glowWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                AetherSplineTelemetryCanvas(
                    samples = splineSamples,
                    currentFraction = scrubFraction,
                    lineColor = palette.glowWhite.copy(alpha = 0.55f),
                    orbColor = palette.glowWhite,
                    glowColor = palette.primaryAccent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                AetherTelemetrySlider(
                    value = timeOffsetHours,
                    onValueChange = { timeOffsetHours = it },
                    valueRange = -12f..12f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("time_scrubber_slider")
                )
            }
        }

        // 6. UPCOMING 3-DAY VISIBLE PASSES
        item(key = "aether_sat_passes") {
            val currentPasses = allUpcomingPasses
            AetherFrostedSpatialCard(
                cornerRadius = AetherCornerGeometry.heroCard,
                contentPadding = PaddingValues(20.dp),
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFa) "گذرهای مرئی (۳ روز آینده)" else "Upcoming Visible Passes",
                        style = RedTypographyTokens.sectionTitle.copy(
                            fontFamily = appFont,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = if (isFa) 0.sp else (-0.3).sp
                        ),
                        color = palette.textPrimary
                    )
                    AetherDotMatrixText(
                        text = String.format(Locale.US, "%02d", currentPasses?.size ?: 0).aetherSatDigits(isFa),
                        dotSize = 2.2.dp,
                        dotGap = 0.8.dp,
                        charGap = 2.2.dp,
                        activeColor = palette.glowWhite,
                        glowColor = palette.primaryAccent
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (currentPasses == null) {
                    Text(
                        text = if (isFa) "در حال محاسبه گذرهای مداری..." else "Propagating SGP4 visible passes...",
                        style = RedTypographyTokens.bodySecondary.copy(fontFamily = appFont),
                        color = palette.textMicro
                    )
                } else if (currentPasses.isEmpty()) {
                    Text(
                        text = if (isFa) "هیچ گذر مرئی در ۳ روز آینده پیش‌بینی نشد."
                        else "No naked-eye sunlit passes predicted in the next 72 hours.",
                        style = RedTypographyTokens.bodySecondary.copy(fontFamily = appFont),
                        color = palette.textMicro
                    )
                } else {
                    val calSys = if (isFa) CalendarSystem.SOLAR_HIJRI else CalendarSystem.GREGORIAN
                    val shown = currentPasses.take(5)
                    shown.forEachIndexed { index, (sat, pass) ->
                        var showPassAlertMenu by remember { mutableStateOf(false) }
                        val dateStr = TimeEngine.formatDate(pass.startTimeMs, calSys, isFa)
                        val riseStr = TimeEngine.formatTime24h(pass.startTimeMs, isFa)
                        val endStr = TimeEngine.formatTime24h(pass.endTimeMs, isFa)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isFa) sat.nameFa else sat.nameEn,
                                    style = RedTypographyTokens.bodyPrimary.copy(
                                        fontFamily = appFont,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "$dateStr  •  $riseStr – $endStr",
                                    style = RedTypographyTokens.caption.copy(
                                        fontFamily = appFont,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Light
                                    ),
                                    color = palette.textMicro
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AetherDotMatrixText(
                                    text = "${pass.maxElevationDeg.roundToInt()}°".aetherSatDigits(isFa),
                                    dotSize = 2.0.dp,
                                    dotGap = 0.7.dp,
                                    charGap = 2.0.dp,
                                    activeColor = palette.glowWhite,
                                    glowColor = palette.primaryAccent
                                )

                                Box {
                                    AetherMicroIconDisc(
                                        icon = Icons.Default.NotificationsNone,
                                        contentDescription = "Schedule Pass Reminder",
                                        onClick = { showPassAlertMenu = true },
                                        size = 30.dp,
                                        iconSize = 15.dp
                                    )
                                    DropdownMenu(
                                        expanded = showPassAlertMenu,
                                        onDismissRequest = { showPassAlertMenu = false }
                                    ) {
                                        listOf(
                                            10 to (if (isFa) "۱۰ دقیقه قبل" else "10m before"),
                                            30 to (if (isFa) "۳۰ دقیقه قبل" else "30m before"),
                                            1440 to (if (isFa) "۱ روز قبل" else "24h before")
                                        ).forEach { (mins, lbl) ->
                                            DropdownMenuItem(
                                                text = { Text(text = lbl) },
                                                onClick = {
                                                    showPassAlertMenu = false
                                                    executeWithNotificationPermission {
                                                        AstroNotificationManager.scheduleSpecificPassAlarm(
                                                            context = context,
                                                            satellite = sat,
                                                            pass = pass,
                                                            userLocation = uiState.userLocation,
                                                            leadMinutes = mins
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (index < shown.lastIndex) {
                            HorizontalDivider(
                                color = palette.mutedTelemetry.copy(alpha = 0.16f),
                                thickness = 0.75.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // 7. TRACKED SATELLITES DECK
        item(key = "aether_sat_deck") {
            AetherFrostedSpatialCard(
                cornerRadius = AetherCornerGeometry.heroCard,
                contentPadding = PaddingValues(20.dp),
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = if (isFa) "ناوگان ماهواره‌های ردیابی‌شده" else "Tracked Constellation",
                    style = RedTypographyTokens.sectionTitle.copy(
                        fontFamily = appFont,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = if (isFa) 0.sp else (-0.3).sp
                    ),
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    liveSatelliteStates.forEach { state ->
                        val sat = state.satellite
                        val isSelected = sat.id == selectedSatelliteId
                        val name = if (isFa) sat.nameFa else sat.nameEn

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(squircleShape(20.dp))
                                .background(
                                    if (isSelected) palette.primaryAccent.copy(alpha = if (palette.isDark) 0.22f else 0.12f)
                                    else palette.frostedGlassFill
                                )
                                .specularRimBorder(
                                    cornerRadius = 20.dp,
                                    strokeWidth = 1.dp
                                )
                                .clickable {
                                    selectedSatelliteId = sat.id
                                    anchoredSatLabelState = state
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("sat_card_${sat.id}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (state.isNakedEyeVisible) palette.glowWhite else Color.Transparent
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) palette.primaryAccent else palette.mutedTelemetry,
                                            CircleShape
                                        )
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = RedTypographyTokens.bodyPrimary.copy(
                                            fontFamily = appFont,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val altInfo = "NORAD ${sat.noradId} • ${state.topocentric.satAltKm.roundToInt()} km"
                                        .aetherSatDigits(isFa)
                                    Text(
                                        text = altInfo,
                                        style = RedTypographyTokens.caption.copy(
                                            fontFamily = appFont,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Light
                                        ),
                                        color = palette.textMicro
                                    )
                                }
                            }

                            AetherMinimalPillBadge(
                                text = if (isFa) "باز ←" else "Inspect →",
                                textColor = palette.textPrimary,
                                borderColor = palette.specularRimStart,
                                onClick = { activeDetailSatellite = sat },
                                modifier = Modifier.testTag("open_sat_detail_${sat.id}")
                            )
                        }
                    }
                }
            }
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
                TextButton(
                    onClick = {
                        showLeadTimeSelectionDialog = false
                        executeWithNotificationPermission {
                            val nextEnabled = !isAutoAlertEnabled
                            isAutoAlertEnabled = nextEnabled
                            prefs.edit()
                                .putBoolean("auto_satellite_alerts_enabled", nextEnabled)
                                .putInt("iss_alert_lead_minutes", selectedLeadMinutes)
                                .putStringSet("monitored_satellite_ids", selectedSatIdsForAlerts)
                                .apply()
                        }
                    }
                ) {
                    Text(text = if (isFa) "ذخیره" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeadTimeSelectionDialog = false }) {
                    Text(text = if (isFa) "انصراف" else "Cancel")
                }
            }
        )
    }

    if (showTutorialDialog) {
        Dialog(onDismissRequest = { showTutorialDialog = false }) {
            AetherFrostedSpatialCard(
                cornerRadius = AetherCornerGeometry.heroCard,
                contentPadding = PaddingValues(22.dp),
                enableHazeBlur = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFa) "راهنمای رصد ماهواره‌ها" else "Orbital Tracking Guide",
                        style = RedTypographyTokens.sectionTitle.copy(
                            fontFamily = appFont,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = if (isFa) 0.sp else (-0.3).sp
                        ),
                        color = palette.textPrimary
                    )
                    AetherMicroIconDisc(
                        icon = Icons.Default.Close,
                        contentDescription = "Close",
                        onClick = { showTutorialDialog = false },
                        size = 32.dp,
                        iconSize = 16.dp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isFa) {
                        "گذرهای مرئی زمانی رخ می‌دهند که رصدگر در تاریکی شب یا گرگ‌ومیش باشد اما ماهواره در ارتفاع مداری هنوز نور خورشید را بازتاب دهد."
                    } else {
                        "Visible passes occur when your observing site is in twilight or darkness while the orbiting spacecraft remains sunlit high overhead."
                    },
                    style = RedTypographyTokens.bodySecondary.copy(
                        fontFamily = appFont,
                        fontWeight = FontWeight.Light
                    ),
                    color = palette.textSecondary
                )
            }
        }
    }
}
