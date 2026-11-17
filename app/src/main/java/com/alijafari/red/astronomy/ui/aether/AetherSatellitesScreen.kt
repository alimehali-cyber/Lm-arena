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
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.alijafari.red.astronomy.data.repository.TleRepository
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.CalendarSystem
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.notification.AstroNotificationManager
import com.alijafari.red.astronomy.notification.NotificationPermissionHelper
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.components.EarthFocusTarget
import com.alijafari.red.astronomy.ui.components.SATELLITE_EARTH_ASPECT_RATIO
import com.alijafari.red.astronomy.ui.components.SatelliteEarth3D
import com.alijafari.red.astronomy.ui.components.SatelliteEarthOverlayStyle
import com.alijafari.red.astronomy.ui.screens.MapTheme
import com.alijafari.red.astronomy.ui.screens.SatelliteDetailScreen
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape
import com.alijafari.red.astronomy.util.formatTleStalenessBadge
import com.alijafari.red.astronomy.util.rememberIsNetworkOnline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

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
    viewModel: MainViewModel
) {
    val palette = AetherTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN
    val context = LocalContext.current
    val isOnline by rememberIsNetworkOnline(context)
    val coroutineScope = rememberCoroutineScope()

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
    val starlinkCacheVersion by tleRepo.starlinkVersion.collectAsState()
    var activeTrains by remember { mutableStateOf<List<StarlinkTrainManager.StarlinkTrain>>(emptyList()) }
    var overheadStarlinks by remember { mutableStateOf<List<SatelliteItem>>(emptyList()) }
    var topPassStarlinks by remember { mutableStateOf<List<SatelliteItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            tleRepo.ensureStarlinkDiskCacheLoaded()
            tleRepo.refreshTles()
        }
    }

    var wasOnline by remember { mutableStateOf(isOnline) }
    LaunchedEffect(isOnline) {
        if (!wasOnline && isOnline) {
            withContext(Dispatchers.IO) {
                tleRepo.refreshTles()
            }
        }
        wasOnline = isOnline
    }

    // Refresh real-time overhead Starlink shortlist + active physical trains every 12s on Dispatchers.Default
    val realtimeBucketKey = currentSimulationMs / StarlinkTrainManager.REALTIME_SNAPSHOT_TTL_MS
    LaunchedEffect(
        starlinkCacheVersion,
        uiState.userLocation.latitude,
        uiState.userLocation.longitude,
        realtimeBucketKey
    ) {
        val snapshot = withContext(Dispatchers.Default) {
            StarlinkTrainManager.selectRealtimeOverheadStarlinks(
                starlinkTles = tleRepo.getStarlinkTles(),
                userLatDeg = uiState.userLocation.latitude,
                userLonDeg = uiState.userLocation.longitude,
                nowMs = currentSimulationMs,
                maxOverhead = StarlinkTrainManager.DEFAULT_REALTIME_OVERHEAD_LIMIT
            )
        }
        activeTrains = snapshot.activeTrains
        overheadStarlinks = snapshot.overheadStarlinks
    }

    val detectedTrain = remember(activeTrains) {
        activeTrains.firstOrNull()?.leaderItem
    }

    var selectedCategory by remember { mutableStateOf(SatelliteCategory.ALL) }
    var selectedSatelliteId by remember { mutableStateOf<String?>("iss_zarya") }
    var activeDetailSatellite by remember { mutableStateOf<SatelliteItem?>(null) }
    var anchoredSatLabelState by remember { mutableStateOf<SatelliteLiveState?>(null) }

    var earthFocusTarget by remember { mutableStateOf<EarthFocusTarget?>(null) }
    var focusSequence by remember { mutableLongStateOf(0L) }

    // Curated fleet list: 4 static catalog satellites + active Starlink trains + top overhead/visible Starlinks
    val visibleSatellites = remember(activeTrains, overheadStarlinks, topPassStarlinks, selectedCategory) {
        val trainLeaders = activeTrains.map { it.leaderItem }
        val individualLimit = if (selectedCategory == SatelliteCategory.STARLINK) 16 else 8
        val combinedIndividuals = (overheadStarlinks + topPassStarlinks)
            .distinctBy { it.noradId }
            .take(individualLimit)
        SatelliteCatalog.satellites + trainLeaders + combinedIndividuals
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

    val showStarlinkConstellationOnMap = selectedCategory == SatelliteCategory.ALL ||
        selectedCategory == SatelliteCategory.STARLINK ||
        selectedCategory == SatelliteCategory.VISIBLE

    val selectedSatItem = remember(selectedSatelliteId, detectedTrain, activeTrains, overheadStarlinks, topPassStarlinks) {
        selectedSatelliteId?.let { id ->
            activeTrains.firstOrNull { it.leaderItem.id == id }?.leaderItem
                ?: activeTrains.asSequence().flatMap { it.memberItems.asSequence() }.firstOrNull { it.id == id }
                ?: overheadStarlinks.firstOrNull { it.id == id }
                ?: topPassStarlinks.firstOrNull { it.id == id }
                ?: SatelliteCatalog.getById(id, detectedTrain)
        }
    }

    // Smooth SGP4 propagation of the curated fleet list (static + train leaders + top overhead)
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

    // Smooth SGP4 propagation for every pearl in active Starlink trains (for string-of-pearls map rendering)
    val trainPearlStatesByTrain = remember(activeTrains, showStarlinkConstellationOnMap, currentSimulationMs, uiState.userLocation) {
        if (!showStarlinkConstellationOnMap || activeTrains.isEmpty()) {
            emptyList()
        } else {
            activeTrains.map { train ->
                train to train.memberItems.map { pearl ->
                    SatelliteEngine.calculateSatelliteState(
                        satellite = pearl,
                        timestampMs = currentSimulationMs,
                        userLatDeg = uiState.userLocation.latitude,
                        userLonDeg = uiState.userLocation.longitude
                    )
                }
            }
        }
    }

    // Smooth SGP4 propagation for all overhead individual Starlinks on the Live Orbital Map
    val mapOverheadStarlinkStates = remember(overheadStarlinks, showStarlinkConstellationOnMap, currentSimulationMs, uiState.userLocation) {
        if (!showStarlinkConstellationOnMap || overheadStarlinks.isEmpty()) {
            emptyList()
        } else {
            overheadStarlinks.map { sat ->
                SatelliteEngine.calculateSatelliteState(
                    satellite = sat,
                    timestampMs = currentSimulationMs,
                    userLatDeg = uiState.userLocation.latitude,
                    userLonDeg = uiState.userLocation.longitude
                )
            }
        }
    }

    val allInteractiveMapStates = remember(liveSatelliteStates, trainPearlStatesByTrain, mapOverheadStarlinkStates) {
        val byId = LinkedHashMap<String, SatelliteLiveState>()
        for (st in liveSatelliteStates) byId[st.satellite.id] = st
        for ((_, pearls) in trainPearlStatesByTrain) {
            for (p in pearls) byId.putIfAbsent(p.satellite.id, p)
        }
        for (st in mapOverheadStarlinkStates) byId.putIfAbsent(st.satellite.id, st)
        byId.values.toList()
    }

    val selectedLiveState = remember(allInteractiveMapStates, liveSatelliteStates, selectedSatelliteId) {
        allInteractiveMapStates.firstOrNull { it.satellite.id == selectedSatelliteId }
            ?: liveSatelliteStates.firstOrNull()
    }

    LaunchedEffect(uiState.selectedSatelliteId) {
        val targetId = uiState.selectedSatelliteId ?: return@LaunchedEffect
        selectedSatelliteId = targetId
        val match = allInteractiveMapStates.firstOrNull {
            it.satellite.id == targetId || "sat_${it.satellite.noradId}" == targetId
        }
        if (match != null) {
            selectedSatelliteId = match.satellite.id
            anchoredSatLabelState = match
            focusSequence += 1L
            earthFocusTarget = EarthFocusTarget(
                latDeg = match.topocentric.subLatDeg,
                lonDeg = match.topocentric.subLonDeg,
                requestId = focusSequence,
            )
        }
        viewModel.selectSatelliteById(null)
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

    val isViewingActiveTrain = remember(selectedLiveState, selectedSatelliteId, selectedCategory, activeTrains) {
        selectedLiveState?.satellite?.isTrain == true ||
            activeTrains.any { t ->
                t.leaderItem.id == selectedSatelliteId || t.memberItems.any { m -> m.id == selectedSatelliteId }
            } ||
            (selectedCategory == SatelliteCategory.STARLINK && activeTrains.isNotEmpty())
    }

    val starlinkCacheAgeDays = remember(starlinkCacheVersion, realTimeNowMs) {
        tleRepo.getStarlinkCacheAgeDays(realTimeNowMs)
    }

    val staleBadgeText = remember(isViewingActiveTrain, selectedLiveState?.tleMetadata, activeTrains, starlinkCacheAgeDays, isFa) {
        val effectiveMeta = if (isViewingActiveTrain && selectedLiveState?.satellite?.isTrain != true) {
            activeTrains.firstOrNull()?.leaderItem?.let { SatelliteEngine.getEffectiveTleInfo(it) }
                ?: selectedLiveState?.tleMetadata
        } else {
            selectedLiveState?.tleMetadata
        }
        formatTleStalenessBadge(
            isViewingActiveTrain = isViewingActiveTrain,
            tleMetadata = effectiveMeta,
            starlinkCacheAgeDays = starlinkCacheAgeDays,
            isFa = isFa
        )
    }

    val statusCapsuleText = remember(altInt, timeOffsetHours, isOnline, staleBadgeText, isFa) {
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
        val staleSuffix = if (staleBadgeText != null) " • $staleBadgeText" else ""
        "$orbitMode  •  $altLabel$netSuffix$staleSuffix"
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
                        val nextEnabled = !isAutoAlertEnabled
                        isAutoAlertEnabled = nextEnabled
                        prefs.edit().putBoolean("auto_satellite_alerts_enabled", nextEnabled).apply()
                        coroutineScope.launch(Dispatchers.Default) {
                            if (nextEnabled) {
                                val astroPrefs = context.getSharedPreferences("astro_prefs", Context.MODE_PRIVATE)
                                val satIds = astroPrefs.getStringSet("monitored_satellite_ids", null)
                                    ?: setOf("iss_zarya", "tiangong", "starlink_train", "hubble")
                                AstroNotificationManager.scheduleRollingSatellitePasses(
                                    context = context,
                                    userLocation = uiState.userLocation,
                                    selectedSatIds = satIds,
                                    leadMinutes = 10,
                                    scanDays = 7
                                )
                            } else {
                                AstroNotificationManager.cancelAllPassNotifications(context)
                            }
                        }
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
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = selectedLiveState != null) {
                            selectedLiveState?.let { target ->
                                focusSequence += 1L
                                earthFocusTarget = EarthFocusTarget(
                                    latDeg = target.topocentric.subLatDeg,
                                    lonDeg = target.topocentric.subLonDeg,
                                    requestId = focusSequence,
                                )
                            }
                        }
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

            // Interactive 3D Earth Globe (GLES 2.0 + SGP4 Orbital Telemetry Overlay)
            val earthOverlayStyle = remember(activeMapDark, activeMapWine) {
                SatelliteEarthOverlayStyle(
                    gridColor = if (activeMapDark) Color(0x18FFFFFF) else Color(0x1C0F172A),
                    equatorColor = if (activeMapWine) {
                        Color(0x66FF94B2)
                    } else {
                        Color.White.copy(alpha = 0.28f)
                    },
                    limbRimColor = when {
                        activeMapWine -> Color(0x3DFF94B2)
                        activeMapDark -> Color(0x3368C5F8)
                        else -> Color(0x2E0F172A)
                    },
                    cityLightAccent = AetherAmberMeshStart.copy(alpha = 0.42f),
                    userPinColor = AetherGlowWhite,
                    userLabelColor = Color.White,
                    orbitTrackColor = AetherGlowWhite,
                    orbitGlowColor = AetherGlowWhite.copy(alpha = 0.26f),
                    selectedMarkerColor = AetherGlowWhite,
                    selectedGlowColor = AetherGlowWhite.copy(alpha = 0.72f),
                    starlinkColor = Color(0xFF60A5FA),
                    trainChainColor = Color(0xFF7DD3FC).copy(alpha = 0.50f),
                    trainLeaderColor = AetherGlowWhite,
                    trainMemberColor = Color(0xFFBAE6FD),
                    nakedEyeMarkerColor = AetherPinkMeshStart,
                    defaultMarkerColor = AetherPinkMeshStart,
                    resetButtonContainerColor = Color.Black.copy(alpha = 0.52f),
                    popoverContainerColor = Color(0xEB0B0D18),
                    popoverBorderColor = Color.White.copy(alpha = 0.35f),
                    popoverAccentColor = Color.White,
                    popoverShape = RedShapes.capsule,
                    showPopoverLeadingIcon = false,
                    altPrefixEn = "",
                    altPrefixFa = "",
                )
            }

            SatelliteEarth3D(
                currentSimulationMs = currentSimulationMs,
                userLocation = uiState.userLocation,
                isFa = isFa,
                mapTheme = mapTheme,
                appThemeMode = uiState.themeMode,
                selectedSatelliteId = selectedSatelliteId,
                selectedSatItem = selectedSatItem,
                liveSatelliteStates = liveSatelliteStates,
                mapOverheadStarlinkStates = mapOverheadStarlinkStates,
                trainPearlStatesByTrain = trainPearlStatesByTrain,
                allInteractiveMapStates = allInteractiveMapStates,
                anchoredSatLabelState = anchoredSatLabelState,
                overlayStyle = earthOverlayStyle,
                focusTarget = earthFocusTarget,
                onSelectSatellite = { satState ->
                    selectedSatelliteId = satState.satellite.id
                    anchoredSatLabelState = satState
                },
                onDismissAnchoredLabel = {
                    anchoredSatLabelState = null
                },
                onOpenSatelliteDetail = { sat ->
                    activeDetailSatellite = sat
                },
                modifier = Modifier
                    .offset(x = (-22).dp)
                    .requiredWidth((LocalConfiguration.current.screenWidthDp.dp - RedSpacing.lg * 2).coerceAtLeast(1.dp))
                    .aspectRatio(SATELLITE_EARTH_ASPECT_RATIO),
            )

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
                    .specularRimBorder(shape = RedShapes.capsule)
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
        val passWindowKey = remember(currentSimulationMs, timeOffsetHours) {
            if (abs(timeOffsetHours) < 0.02f) {
                (currentSimulationMs / 900_000L) * 900_000L // 15m stability bucket in live mode
            } else {
                (currentSimulationMs / 60_000L) * 60_000L
            }
        }
        var allUpcomingPasses by remember {
            mutableStateOf<List<Pair<SatelliteItem, ISSEngine.ISSPass>>?>(null)
        }

        LaunchedEffect(
            uiState.userLocation.latitude,
            uiState.userLocation.longitude,
            passWindowKey,
            starlinkCacheVersion
        ) {
            withContext(Dispatchers.Default) {
                val starlinkTles = tleRepo.getStarlinkTles()
                val detectedTrainsNow = StarlinkTrainManager.detectActiveTrains(
                    starlinkTles = starlinkTles,
                    nowMs = passWindowKey
                )
                val shortlistedStarlinks = StarlinkTrainManager.selectTopVisibleStarlinks(
                    starlinkTles = starlinkTles,
                    activeTrains = detectedTrainsNow,
                    userLatDeg = uiState.userLocation.latitude,
                    userLonDeg = uiState.userLocation.longitude,
                    startTimeMs = passWindowKey,
                    maxCount = StarlinkTrainManager.DEFAULT_TOP_VISIBLE_PASS_LIMIT
                )

                val combined = mutableListOf<Pair<SatelliteItem, ISSEngine.ISSPass>>()

                // 1. Static catalog + active Starlink Trains (3-day visible scan)
                val primaryTargets = SatelliteCatalog.satellites + detectedTrainsNow.map { it.leaderItem }
                for (sat in primaryTargets) {
                    val passes = ISSEngine.predictPasses(
                        userLatDeg = uiState.userLocation.latitude,
                        userLonDeg = uiState.userLocation.longitude,
                        startTimestampMs = passWindowKey,
                        tle = SatelliteEngine.getEffectiveTle(sat),
                        scanDays = 3,
                        visibleOnly = true,
                        standardMag = sat.standardMagnitude
                    )
                    for (p in passes) {
                        combined.add(sat to p)
                    }
                }

                // 2. Top 18 fast-preselected high-elevation individual Starlinks (48-hour visible scan)
                for (sat in shortlistedStarlinks) {
                    val passes = ISSEngine.predictPasses(
                        userLatDeg = uiState.userLocation.latitude,
                        userLonDeg = uiState.userLocation.longitude,
                        startTimestampMs = passWindowKey,
                        tle = sat.defaultTle,
                        scanDays = 2,
                        visibleOnly = true,
                        standardMag = sat.standardMagnitude
                    )
                    for (p in passes) {
                        combined.add(sat to p)
                    }
                }

                val sorted = combined.sortedBy { it.second.startTimeMs }
                withContext(Dispatchers.Main) {
                    topPassStarlinks = shortlistedStarlinks
                    allUpcomingPasses = sorted
                }
            }
        }

        val displayedPasses = remember(allUpcomingPasses, selectedCategory) {
            allUpcomingPasses?.filter { (sat, _) ->
                when (selectedCategory) {
                    SatelliteCategory.ALL -> true
                    SatelliteCategory.ISS -> sat.category == SatelliteCategory.ISS
                    SatelliteCategory.STARLINK -> sat.category == SatelliteCategory.STARLINK || sat.isTrain
                    SatelliteCategory.HUBBLE -> sat.category == SatelliteCategory.HUBBLE
                    SatelliteCategory.VISIBLE -> sat.isNakedEyeCandidate
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
            displayedPasses?.size?.let { count ->
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

        val currentPasses = displayedPasses
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
            val maxPassCards = if (selectedCategory == SatelliteCategory.STARLINK) 12 else 10
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                currentPasses.take(maxPassCards).forEach { (sat, pass) ->
                    AetherVisiblePassCard(
                        satName = if (isFa) sat.nameFa else sat.nameEn,
                        isTrainPass = sat.isTrain,
                        trainSatelliteCount = sat.trainCount,
                        pass = pass,
                        calendarSystem = uiState.calendarSystem,
                        isFa = isFa,
                        onCardClick = {
                            selectedSatelliteId = sat.id
                            activeDetailSatellite = sat
                        },
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
                        focusSequence += 1L
                        earthFocusTarget = EarthFocusTarget(
                            latDeg = state.topocentric.subLatDeg,
                            lonDeg = state.topocentric.subLonDeg,
                            requestId = focusSequence,
                        )
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
    isTrainPass: Boolean = false,
    trainSatelliteCount: Int = 1,
    pass: ISSEngine.ISSPass,
    calendarSystem: CalendarSystem,
    isFa: Boolean,
    onCardClick: (() -> Unit)? = null,
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
        accentGlow = if (isTrainPass) Color(0xFF7DD3FC) else palette.sapphireGlow,
        onClick = onCardClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (isTrainPass) {
                    val countText = if (isFa) {
                        "قطار استارلینک • ${TimeEngine.formatPersianNumbers(trainSatelliteCount.toString())} ماهواره"
                    } else {
                        "STRING OF PEARLS • $trainSatelliteCount SATS"
                    }
                    AetherPillBadge(
                        text = countText,
                        selected = true,
                        tint = Color(0xFF7DD3FC)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
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
