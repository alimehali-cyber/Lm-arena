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
import androidx.compose.foundation.layout.requiredWidth
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
import androidx.compose.runtime.collectAsState
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
import com.alijafari.red.astronomy.ui.theme.RedCornerRadius
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.alijafari.red.astronomy.util.formatTleStalenessBadge
import com.alijafari.red.astronomy.util.rememberIsNetworkOnline
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
    viewModel: MainViewModel
) {
    val palette = SpatialTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN
    val context = LocalContext.current
    val isOnline by rememberIsNetworkOnline(context)

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

    var selectedCategory by remember { mutableStateOf(SatelliteCategory.ALL) }
    var selectedSatelliteId by remember { mutableStateOf<String?>("iss_zarya") }
    var activeDetailSatellite by remember { mutableStateOf<SatelliteItem?>(null) }
    var anchoredSatLabelState by remember { mutableStateOf<SatelliteLiveState?>(null) }

    var earthFocusTarget by remember { mutableStateOf<EarthFocusTarget?>(null) }
    var focusSequence by remember { mutableLongStateOf(0L) }
    var showTutorialDialog by remember { mutableStateOf(false) }

    val visibleSatellites = remember(activeTrains, overheadStarlinks, topPassStarlinks, selectedCategory) {
        val trainLeaders = activeTrains.map { it.leaderItem }
        val individualLimit = if (selectedCategory == SatelliteCategory.STARLINK) 16 else 8
        val combinedIndividuals = (overheadStarlinks + topPassStarlinks)
            .distinctBy { it.noradId }
            .take(individualLimit)
        SatelliteCatalog.getVisibleList(null) + trainLeaders + combinedIndividuals
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

                    if (staleBadgeText != null) {
                        SpatialGlassCapsule(
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            elevation = 4.dp
                        ) {
                            Text(
                                text = staleBadgeText,
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

            // 1. INTERACTIVE 3D EARTH GLOBE (GLES 2.0 + SPATIAL ORBITAL TELEMETRY OVERLAY)
            val earthOverlayStyle = remember(activeMapDark) {
                SatelliteEarthOverlayStyle(
                    gridColor = if (activeMapDark) Color(0x18FFFFFF) else Color(0x1F3A2218),
                    equatorColor = if (activeMapDark) {
                        SpatialSkyBlue.copy(alpha = 0.44f)
                    } else {
                        SpatialNeonRoseDeep.copy(alpha = 0.38f)
                    },
                    limbRimColor = if (activeMapDark) {
                        SpatialNeonRose.copy(alpha = 0.18f)
                    } else {
                        Color(0x367A4B3A)
                    },
                    cityLightAccent = SpatialWarmPeach.copy(alpha = 0.44f),
                    userPinColor = SpatialMintGlow,
                    userLabelColor = Color.White,
                    orbitTrackColor = SpatialNeonRose,
                    orbitGlowColor = SpatialNeonRose.copy(alpha = 0.30f),
                    selectedMarkerColor = SpatialNeonRoseVibrant,
                    selectedGlowColor = SpatialNeonRose.copy(alpha = 0.65f),
                    starlinkColor = SpatialSkyBlue,
                    trainChainColor = SpatialMintGlow.copy(alpha = 0.54f),
                    trainLeaderColor = Color.White,
                    trainMemberColor = SpatialMintGlow,
                    nakedEyeMarkerColor = SpatialWarmPeach,
                    defaultMarkerColor = SpatialWarmPeach,
                    resetButtonContainerColor = Color.Black.copy(alpha = 0.55f),
                    popoverContainerColor = Color(0xEB1A1228),
                    popoverBorderColor = SpatialNeonRose,
                    popoverAccentColor = SpatialNeonRose,
                    popoverShape = squircleShape(14.dp),
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
            val passWindowKey = remember(currentSimulationMs, timeOffsetHours) {
                if (abs(timeOffsetHours) < 0.02f) {
                    (currentSimulationMs / 900_000L) * 900_000L
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

            SpatialSectionHeader(
                title = if (isFa) "گذرهای قابل مشاهده بعدی (۳ روز آینده)" else "Upcoming Visible Passes (Next 3 Days)",
                badgeText = if (displayedPasses?.isNotEmpty() == true) {
                    val c = displayedPasses!!.size.toString()
                    if (isFa) TimeEngine.formatPersianNumbers(c) else c
                } else null
            )

            val currentAllPasses = displayedPasses
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
                val maxPassCards = if (selectedCategory == SatelliteCategory.STARLINK) 12 else 10
                Column(verticalArrangement = Arrangement.spacedBy(RedSpacing.sm)) {
                    currentAllPasses.take(maxPassCards).forEach { (sat, pass) ->
                        SpatialVisiblePassCard(
                            satName = if (isFa) sat.nameFa else sat.nameEn,
                            pass = pass,
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
                            verticalAlignment = Alignment.CenterVertically
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
                        title = if (isFa) "کره سه‌بعدی زمین:" else "Interactive 3D Earth:",
                        desc = if (isFa) "چرخش با لمس و بزرگ‌نمایی دو انگشتی روی کره سه‌بعدی با مرز واقعی روز و شب، ابرها، پستی‌وبلندی و چراغ شهرهای شب."
                        else "Drag to rotate and pinch or double-tap to zoom the 3D Earth with real-time solar terminator, clouds, terrain relief, and night-side city lights."
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
    isTrainPass: Boolean = false,
    trainSatelliteCount: Int = 1,
    onCardClick: (() -> Unit)? = null,
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
        glowAccent = if (isTrainPass) SpatialMintGlow else SpatialNeonRose,
        onClick = onCardClick
    ) {
        if (isTrainPass) {
            val countLabel = if (isFa) {
                "قطار استارلینک • ${TimeEngine.formatPersianNumbers(trainSatelliteCount.toString())} ماهواره"
            } else {
                "STRING OF PEARLS • $trainSatelliteCount SATS"
            }
            SpatialMeshPill(
                preset = SpatialMeshPreset.BLUE_TO_PURPLE,
                isCapsule = true,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = countLabel,
                    style = RedTypographyTokens.badge.copy(fontSize = 10.sp),
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

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
