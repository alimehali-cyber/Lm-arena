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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.*
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.CalendarSystem
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.notification.AstroNotificationManager
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.components.EarthFocusTarget
import com.alijafari.red.astronomy.ui.components.SATELLITE_EARTH_ASPECT_RATIO
import com.alijafari.red.astronomy.ui.components.SatelliteEarth3D
import com.alijafari.red.astronomy.ui.components.SatelliteEarthOverlayStyle
import com.alijafari.red.astronomy.ui.theme.*
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedSurface
import com.alijafari.red.astronomy.ui.theme.squircleShape
import com.alijafari.red.astronomy.util.formatTleStalenessBadge
import com.alijafari.red.astronomy.util.rememberIsNetworkOnline
import com.alijafari.red.astronomy.util.toPersianDigits
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MapTheme {
    AUTO,
    DARK,
    LIGHT
}

data class SatelliteMapPalette(
    val oceanColor: Color,
    val landColor: Color,
    val landStroke: Color,
    val gridColor: Color,
    val equatorColor: Color,
    val nightShadowColor: Color,
    val cityLightColor: Color,
    val orbitTrackColor: Color,
    val userPinColor: Color,
    val userTextColor: Color,
    val isDarkBackground: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ISSScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    val context = LocalContext.current
    val isOnline by rememberIsNetworkOnline(context)

    // Notification & Map Prefs
    val prefs = remember { context.getSharedPreferences("astro_prefs", Context.MODE_PRIVATE) }
    var isAutoAlertEnabled by remember {
        mutableStateOf(prefs.getBoolean("auto_satellite_alerts_enabled", prefs.getBoolean("iss_auto_alerts_enabled", false)))
    }
    var selectedLeadMinutes by remember {
        mutableStateOf(prefs.getInt("iss_alert_lead_minutes", 10))
    }
    var showLeadTimeSelectionDialog by remember { mutableStateOf(false) }

    // Map Theme State (Persisted: "AUTO", "DARK" or "LIGHT")
    val celestialLighting = LocalCelestialLighting.current
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

    val activeThemeMode = when (mapTheme) {
        MapTheme.DARK -> ThemeMode.DARK
        MapTheme.LIGHT -> ThemeMode.LIGHT
        MapTheme.AUTO -> uiState.themeMode
    }

    val mapPalette = remember(activeThemeMode, celestialLighting) {
        when (activeThemeMode) {
            ThemeMode.LIGHT -> {
                SatelliteMapPalette(
                    oceanColor = SoftCreamVariant,
                    landColor = SoftCreamSurface,
                    landStroke = SoftCreamCardBorder,
                    gridColor = Color(0x22182238),
                    equatorColor = OldMoneyChampagneGold,
                    nightShadowColor = Color(0x35111118),
                    cityLightColor = OldMoneyBronze,
                    orbitTrackColor = OldMoneyBurgundy,
                    userPinColor = OldMoneyNavy,
                    userTextColor = OldMoneySlate,
                    isDarkBackground = false
                )
            }
            ThemeMode.DARK -> {
                SatelliteMapPalette(
                    oceanColor = Color(0xFF000000),
                    landColor = Color(0xFF101014),
                    landStroke = Color(0xFF27272A),
                    gridColor = Color(0x18FFFFFF),
                    equatorColor = Color(0xFF38BDF8),
                    nightShadowColor = Color(0xF0000000),
                    cityLightColor = Color(0xFFFFC107),
                    orbitTrackColor = AccentPrimary,
                    userPinColor = Color(0xFF00F0FF),
                    userTextColor = Color(0xFFFFFFFF),
                    isDarkBackground = true
                )
            }
            ThemeMode.WINE -> {
                SatelliteMapPalette(
                    oceanColor = WineBackground,
                    landColor = WineSurfaceElevated,
                    landStroke = WineOutline,
                    gridColor = Color(0x22FCECEF),
                    equatorColor = WineCognacGold,
                    nightShadowColor = Color(0xEB0B0205),
                    cityLightColor = WineCognacGold,
                    orbitTrackColor = WineAccentCrimson,
                    userPinColor = WineVintageRose,
                    userTextColor = WineTextPrimary,
                    isDarkBackground = true
                )
            }
            else -> { // DARK, GLASS, etc.
                SatelliteMapPalette(
                    oceanColor = Color(0xFF1E293B),
                    landColor = Color(0xFF334155),
                    landStroke = Color(0xFF64748B),
                    gridColor = Color(0x18FFFFFF),
                    equatorColor = Color(0x4000F0FF),
                    nightShadowColor = Color(0xCC030712),
                    cityLightColor = Color(0xFFFFC107),
                    orbitTrackColor = AccentPrimary,
                    userPinColor = Color(0xFF00F0FF),
                    userTextColor = Color(0xFF00F0FF),
                    isDarkBackground = true
                )
            }
        }
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
        if (com.alijafari.red.astronomy.notification.NotificationPermissionHelper.hasPostNotificationPermission(context)) {
            action()
        } else {
            pendingNotificationAction = action
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
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

    // Filters
    var selectedCategory by remember { mutableStateOf(SatelliteCategory.ALL) }

    // Selected Satellite (Nullable: null = no satellite selected / no orbit line)
    var selectedSatelliteId by remember { mutableStateOf<String?>("iss_zarya") }
    var activeDetailSatellite by remember { mutableStateOf<SatelliteItem?>(null) }

    // Temporary anchored label state (shown when tapping a satellite)
    var anchoredSatLabelState by remember { mutableStateOf<SatelliteLiveState?>(null) }

    // 3D Earth camera focus target (smoothly rotates the globe when choosing a satellite)
    var earthFocusTarget by remember { mutableStateOf<EarthFocusTarget?>(null) }
    var focusSequence by remember { mutableLongStateOf(0L) }

    // Dialog Modals
    var showTutorialDialog by remember { mutableStateOf(false) }

    // Curated fleet list: 4 static catalog satellites + active Starlink trains + top overhead/visible Starlinks
    val visibleSatellites = remember(activeTrains, overheadStarlinks, topPassStarlinks, selectedCategory) {
        val trainLeaders = activeTrains.map { it.leaderItem }
        val individualLimit = if (selectedCategory == SatelliteCategory.STARLINK) 16 else 8
        val combinedIndividuals = (overheadStarlinks + topPassStarlinks)
            .distinctBy { it.noradId }
            .take(individualLimit)
        SatelliteCatalog.getVisibleList(null) + trainLeaders + combinedIndividuals
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

    // Compute live positions for curated filtered fleet satellites
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

    // Smooth SGP4 propagation for every pearl in active Starlink trains
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

    // Smooth SGP4 propagation for real-time overhead individual Starlinks on the Live Map
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            // Owns its status-bar inset: the shared top band that used to provide it is gone (Part 6,
            // item 8), so this screen must not slide under the clock.
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RedSpacing.lg, vertical = RedSpacing.md),
            verticalArrangement = Arrangement.spacedBy(RedSpacing.lg)
        ) {
            // Status/Action Row (Alerts, Guide, Status)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                ) {
                    RedBadge(
                        text = if (timeOffsetHours == 0f) (if (isFa) "پایش زنده SGP4" else "Live SGP4 Orbit") else (if (isFa) "شبیه‌سازی زمانی" else "Time Simulation"),
                        isAccent = timeOffsetHours == 0f
                    )
                    if (!isOnline) {
                        RedBadge(
                            text = if (isFa) "آفلاین" else "Offline",
                            isAccent = false
                        )
                    }
                    if (staleBadgeText != null) {
                        RedBadge(
                            text = staleBadgeText,
                            isAccent = false
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                ) {
                    // Notification Alert Toggle
                    IconButton(
                        onClick = { showLeadTimeSelectionDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isAutoAlertEnabled) RedTheme.colors.accentRed.copy(alpha = 0.15f)
                                else RedTheme.colors.surfaceElevated
                            )
                            .border(
                                1.dp,
                                if (isAutoAlertEnabled) RedTheme.colors.accentRed.copy(alpha = 0.5f)
                                else RedTheme.colors.border,
                                CircleShape
                            )
                            .testTag("satellite_alerts_toggle")
                    ) {
                        Icon(
                            imageVector = if (isAutoAlertEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                            contentDescription = "Alerts",
                            tint = if (isAutoAlertEnabled) RedTheme.colors.accentRed else RedTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Help / Guide Button
                    IconButton(
                        onClick = { showTutorialDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(RedTheme.colors.surfaceElevated)
                            .border(1.dp, RedTheme.colors.border, CircleShape)
                            .testTag("satellite_help_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Help",
                            tint = RedTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 1. MAIN FEATURE: INTERACTIVE 3D EARTH GLOBE (GLES 2.0 + SGP4 ORBITAL OVERLAY)
            val earthOverlayStyle = remember(mapPalette, activeThemeMode) {
                val selectedAccent = when (activeThemeMode) {
                    ThemeMode.WINE -> WineAccentCrimson
                    else -> AccentPrimary
                }
                SatelliteEarthOverlayStyle(
                    gridColor = mapPalette.gridColor,
                    equatorColor = mapPalette.equatorColor.copy(alpha = 0.42f),
                    limbRimColor = if (mapPalette.isDarkBackground) {
                        Color.White.copy(alpha = 0.14f)
                    } else {
                        OldMoneyNavy.copy(alpha = 0.22f)
                    },
                    cityLightAccent = mapPalette.cityLightColor.copy(alpha = 0.42f),
                    userPinColor = mapPalette.userPinColor,
                    userLabelColor = Color.White,
                    orbitTrackColor = mapPalette.orbitTrackColor.copy(alpha = 0.90f),
                    orbitGlowColor = mapPalette.orbitTrackColor.copy(alpha = 0.24f),
                    selectedMarkerColor = selectedAccent,
                    selectedGlowColor = selectedAccent.copy(alpha = 0.55f),
                    starlinkColor = Color(0xFF38BDF8),
                    trainChainColor = Color(0xFF38BDF8).copy(alpha = 0.54f),
                    trainLeaderColor = Color.White,
                    trainMemberColor = Color(0xFFBAE6FD),
                    nakedEyeMarkerColor = Color(0xFF2DC653),
                    defaultMarkerColor = Color(0xFFFFB703),
                    resetButtonContainerColor = Color.Black.copy(alpha = 0.65f),
                    popoverContainerColor = Color(0xF20F172A),
                    popoverBorderColor = selectedAccent,
                    popoverAccentColor = selectedAccent,
                    popoverShape = squircleShape(12.dp),
                    showPopoverLeadingIcon = true,
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
                    .offset(x = -RedSpacing.lg)
                    .requiredWidth(LocalConfiguration.current.screenWidthDp.dp)
                    .aspectRatio(SATELLITE_EARTH_ASPECT_RATIO),
            )

            // 2. MAP THEME & CATEGORY CONTROLS AREA (Outside Map Canvas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Filter Chips
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                ) {
                    items(SatelliteCategory.entries) { cat ->
                        val isSelected = cat == selectedCategory
                        Surface(
                            shape = RedShapes.capsule,
                            color = if (isSelected) RedTheme.colors.accentRed else RedTheme.colors.surfaceElevated,
                            border = BorderStroke(1.dp, if (isSelected) RedTheme.colors.accentRed else RedTheme.colors.border),
                            modifier = Modifier
                                .clickable { selectedCategory = cat }
                                .testTag("filter_${cat.name}")
                        ) {
                            Text(
                                text = if (isFa) cat.labelFa else cat.labelEn,
                                style = RedTypography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSelected) Color.White else RedTheme.colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(RedSpacing.sm))

                // Compact Map Theme Selector Outside Map
                Surface(
                    shape = RedShapes.capsule,
                    color = RedTheme.colors.surfaceElevated,
                    border = BorderStroke(1.dp, RedTheme.colors.border),
                    modifier = Modifier.testTag("map_theme_selector")
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                                    .background(if (isSelected) RedTheme.colors.accentRed else Color.Transparent)
                                    .clickable {
                                        mapTheme = theme
                                        prefs.edit().putString("satellite_map_theme", theme.name).apply()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = RedTypography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                                    color = if (isSelected) Color.White else RedTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 3. COMPACT TIME SCRUBBER (-12h to +12h)
            RedSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("satellite_time_scrubber_card"),
                shape = squircleShape(RedCornerRadius.lg)
            ) {
                Column(
                    modifier = Modifier.padding(RedSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = RedTheme.colors.accentRed,
                                modifier = Modifier.size(16.dp)
                            )

                            val isPersianCalendar = isFa || uiState.calendarSystem == CalendarSystem.SOLAR_HIJRI
                            val timeStr = remember(currentSimulationMs, isPersianCalendar, isFa) {
                                if (isPersianCalendar) {
                                    val sh = TimeEngine.toSolarHijri(currentSimulationMs, TimeEngine.TEHRAN_TIME_ZONE)
                                    val timePart = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = TimeEngine.TEHRAN_TIME_ZONE }.format(Date(currentSimulationMs))
                                    val raw = "${sh.day} ${sh.monthNameFa}، $timePart"
                                    if (isFa) raw.toPersianDigits() else raw
                                } else {
                                    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.US)
                                    sdf.format(Date(currentSimulationMs))
                                }
                            }
                            val offsetStr = if (timeOffsetHours == 0f) (if (isFa) "زنده" else "Live")
                            else if (timeOffsetHours > 0) {
                                val raw = "+${String.format(Locale.US, "%.1fh", timeOffsetHours)}"
                                if (isFa) raw.toPersianDigits() else raw
                            } else {
                                val raw = String.format(Locale.US, "%.1fh", timeOffsetHours)
                                if (isFa) raw.toPersianDigits() else raw
                            }

                            Text(
                                text = "$timeStr ($offsetStr)",
                                style = RedTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = RedTheme.colors.textPrimary
                            )
                        }

                        if (timeOffsetHours != 0f) {
                            Surface(
                                shape = RedShapes.capsule,
                                color = RedTheme.colors.accentRed.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, RedTheme.colors.accentRed.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clickable { timeOffsetHours = 0f }
                                    .testTag("time_reset_now")
                            ) {
                                Text(
                                    text = if (isFa) "زنده" else "Live",
                                    style = RedTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = RedTheme.colors.accentRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Time Slider (-12h to +12h, 1-minute steps)
                    RedSlider(
                        value = timeOffsetHours,
                        onValueChange = { raw ->
                            val stepHours = 1.0 / 60.0 // 1 minute step
                            val rounded = (round(raw / stepHours) * stepHours).toFloat()
                            timeOffsetHours = rounded
                        },
                        valueRange = -12f..12f,
                        steps = 1439,
                        modifier = Modifier.fillMaxWidth().testTag("time_scrubber_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isFa) "۱۲ ساعت قبل" else "12h ago",
                            style = RedTypography.labelSmall,
                            color = RedTheme.colors.textSecondary
                        )
                        Text(
                            text = if (isFa) "زنده" else "Live",
                            style = RedTypography.labelSmall,
                            color = RedTheme.colors.accentRed
                        )
                        Text(
                            text = if (isFa) "۱۲ ساعت بعد" else "12h ahead",
                            style = RedTypography.labelSmall,
                            color = RedTheme.colors.textSecondary
                        )
                    }
                }
            }

            // 4. UPCOMING VISIBLE PASSES PREDICTIONS (ALL + Starlink Trains + Top Visible Starlinks)
            val passWindowKey = remember(currentSimulationMs, timeOffsetHours) {
                if (abs(timeOffsetHours) < 0.02f) {
                    (currentSimulationMs / 900_000L) * 900_000L
                } else {
                    (currentSimulationMs / 60_000L) * 60_000L
                }
            }

            var allUpcomingPasses by remember {
                mutableStateOf<List<Pair<com.alijafari.red.astronomy.astro_engine.SatelliteItem, ISSEngine.ISSPass>>?>(null)
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

                    val combined = mutableListOf<Pair<com.alijafari.red.astronomy.astro_engine.SatelliteItem, ISSEngine.ISSPass>>()

                    val primaryTargets = com.alijafari.red.astronomy.astro_engine.SatelliteCatalog.satellites +
                        detectedTrainsNow.map { it.leaderItem }
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

            RedSectionHeader(
                title = if (isFa) "گذرهای قابل مشاهده بعدی (۳ روز آینده)" else "Upcoming Visible Passes (Next 3 Days)",
                actionText = if (displayedPasses?.isNotEmpty() == true) "${displayedPasses!!.size}" else null
            )

            val currentAllPasses = displayedPasses
            if (currentAllPasses == null) {
                RedElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isFa) "در حال محاسبه گذرهای ماهواره‌ها..." else "Calculating satellite passes...",
                        style = RedTypography.bodyMedium,
                        color = RedTheme.colors.textSecondary,
                        modifier = Modifier.padding(RedSpacing.lg)
                    )
                }
            } else if (currentAllPasses.isEmpty()) {
                RedElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isFa) "هیچ گذر قابل مشهودی مطابق با معیار علمی در ۳ روز آینده یافت نشد."
                        else "No visible passes meeting scientific criteria predicted in next 3 days.",
                        style = RedTypography.bodyMedium,
                        color = RedTheme.colors.textSecondary,
                        modifier = Modifier.padding(RedSpacing.lg)
                    )
                }
            } else {
                val maxPassCards = if (selectedCategory == SatelliteCategory.STARLINK) 12 else 10
                Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)) {
                    currentAllPasses.take(maxPassCards).forEach { (sat, pass) ->
                        DetailedVisiblePassCard(
                            satName = if (isFa) sat.nameFa else sat.nameEn,
                            pass = pass,
                            cityName = uiState.userLocation.cityNameFa,
                            isFa = isFa,
                            isTrainPass = sat.isTrain,
                            trainSatelliteCount = sat.trainCount,
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
                                    val labelStr = if (leadMins == 1440) (if (isFa) "۱ روز" else "1 day") else (if (isFa) "$leadMins دقیقه" else "$leadMins mins")
                                    Toast.makeText(
                                        context,
                                        if (isFa) "هشدار گذر $labelStr قبل از شروع تنظیم شد!" else "Alert set $labelStr prior to pass!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                }
            }

            // 5. SATELLITE LIST / CARDS SECTION
            RedSectionHeader(
                title = if (isFa) "فهرست ماهواره‌ها" else "Tracked Satellites",
                actionText = "${filteredSatellites.size}"
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)
            ) {
                liveSatelliteStates.forEach { state ->
                    val sat = state.satellite
                    val isSelected = sat.id == selectedSatelliteId

                    RedElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) {
                                    selectedSatelliteId = null
                                    anchoredSatLabelState = null
                                } else {
                                    selectedSatelliteId = sat.id
                                    anchoredSatLabelState = state
                                    focusSequence += 1L
                                    earthFocusTarget = EarthFocusTarget(
                                        latDeg = state.topocentric.subLatDeg,
                                        lonDeg = state.topocentric.subLonDeg,
                                        requestId = focusSequence,
                                    )
                                }
                            }
                            .testTag("sat_card_${sat.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(RedSpacing.md),
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
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) RedTheme.colors.accentRed else RedTheme.colors.surfaceElevated)
                                        .border(1.dp, if (isSelected) RedTheme.colors.accentRed else RedTheme.colors.border, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SatelliteAlt,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else RedTheme.colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = if (isFa) sat.nameFa else sat.nameEn,
                                        style = RedTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = RedTheme.colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val altStr = String.format(Locale.US, "%.0f km", state.topocentric.satAltKm)
                                    val magStr = String.format(Locale.US, "%+.1f", state.apparentMagnitude)
                                    Text(
                                        text = if (isFa) "ارتفاع $altStr • قدر $magStr".toPersianDigits() else "Alt $altStr • Mag $magStr",
                                        style = RedTypography.labelSmall,
                                        color = RedTheme.colors.textSecondary
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                            ) {
                                RedBadge(
                                    text = if (state.isNakedEyeVisible) (if (isFa) "با چشم" else "Naked Eye") else (if (isFa) "تلسکوپی" else "Telescopic"),
                                    isAccent = state.isNakedEyeVisible
                                )

                                IconButton(
                                    onClick = { activeDetailSatellite = sat },
                                    modifier = Modifier.size(28.dp).testTag("open_sat_detail_${sat.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Details",
                                        tint = RedTheme.colors.accentRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom spacing for floating navigation bar
            Spacer(modifier = Modifier.height(96.dp))
        }
    }

    // Multi-Satellite Notification State
    var selectedSatIdsForAlerts by remember {
        val savedSet = prefs.getStringSet("monitored_satellite_ids", null)
        mutableStateOf(savedSet ?: setOf("iss_zarya", "tiangong", "starlink_train", "hubble"))
    }

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
                        val satAlertId = if (sat.isTrain) "starlink_train" else sat.id
                        val isChecked = satAlertId in selectedSatIdsForAlerts || sat.id in selectedSatIdsForAlerts
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSatIdsForAlerts = if (isChecked) {
                                        selectedSatIdsForAlerts - satAlertId - sat.id
                                    } else {
                                        (selectedSatIdsForAlerts - sat.id) + satAlertId
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
                                            (selectedSatIdsForAlerts - sat.id) + satAlertId
                                        } else {
                                            selectedSatIdsForAlerts - satAlertId - sat.id
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
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Text(text = if (isFa) "فعال‌سازی سیستم هشدار" else "Enable Monitoring")
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
                        title = if (isFa) "کره سه‌بعدی زمین:" else "Interactive 3D Earth:",
                        desc = if (isFa) "چرخش با لمس و بزرگ‌نمایی دو انگشتی روی کره سه‌بعدی با مرز واقعی روز و شب، ابرها، پستی‌وبلندی و چراغ شهرهای شب."
                        else "Drag to rotate and pinch or double-tap to zoom the 3D Earth with real-time solar terminator, clouds, terrain relief, and night-side city lights."
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
