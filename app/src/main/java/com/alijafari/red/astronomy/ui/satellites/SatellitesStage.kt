package com.alijafari.red.astronomy.ui.satellites

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.notification.AstroNotificationManager
import com.alijafari.red.astronomy.notification.NotificationPermissionHelper
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.components.EarthFocusTarget
import com.alijafari.red.astronomy.ui.components.SatelliteEarth3D
import com.alijafari.red.astronomy.ui.components.SatelliteEarthOverlayStyle
import com.alijafari.red.astronomy.ui.screens.MapTheme
import com.alijafari.red.astronomy.ui.screens.SatelliteDetailScreen
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedTheme
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape
import com.alijafari.red.astronomy.ui.satellites.SatSnackbar
import com.alijafari.red.astronomy.util.formatTleStalenessBadge
import com.alijafari.red.astronomy.util.rememberIsNetworkOnline
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "One stage, three skins".
 *
 * The three Satellites screens are the same anatomy — header → globe stage → focus dock → time strip →
 * scope chips → fleet rail → next-pass spotlight → passes by night — rendered from one state object.
 * A [SatSkin] changes surfaces, numerals, shape language and motion only.
 */
@Composable
fun SatellitesStage(
    uiState: MainUiState,
    viewModel: MainViewModel,
    skin: SatSkin,
    globeModifier: Modifier,
    reduceMotion: Boolean = false,
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    val context = LocalContext.current
    val isOnline by rememberIsNetworkOnline(context)
    val tokens = remember(skin, uiState.themeMode) { zigSatTokens(skin, uiState.themeMode) }
    val tz = remember { TimeZone.getDefault() }
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val widthClass = satWidthClass(configuration.screenWidthDp)
    val compactDock = widthClass == SatWidthClass.COMPACT || configuration.screenHeightDp < 700

    // ---- preferences -------------------------------------------------------------------------
    val prefs = remember {
        context.getSharedPreferences("astro_app_prefs", Context.MODE_PRIVATE)
    }
    val legacyPrefs = remember { context.getSharedPreferences("astro_prefs", Context.MODE_PRIVATE) }

    // Both prefs files are still in the wild: the Default/Spatial screens historically wrote
    // `astro_app_prefs`, the older Default screen `astro_prefs`. Read through both, write to both.
    fun prefBoolean(key: String, legacyKey: String?, default: Boolean = false): Boolean {
        // Newest store wins, then either key in the legacy file, then the default.
        val legacy = legacyPrefs.getBoolean(legacyKey ?: key, legacyPrefs.getBoolean(key, default))
        return prefs.getBoolean(key, prefs.getBoolean(legacyKey ?: key, legacy))
    }

    fun prefInt(key: String, legacyKey: String?, default: Int): Int {
        val legacy = legacyPrefs.getInt(legacyKey ?: key, legacyPrefs.getInt(key, default))
        return prefs.getInt(key, prefs.getInt(legacyKey ?: key, legacy))
    }

    var isAutoAlertEnabled by remember {
        mutableStateOf(prefBoolean("auto_satellite_alerts_enabled", "iss_auto_alerts_enabled"))
    }
    var selectedLeadMinutes by remember { mutableIntStateOf(prefInt("satellite_alert_lead_minutes", "iss_alert_lead_minutes", 10)) }
    var monitoredIds by remember {
        mutableStateOf(
            prefs.getStringSet("monitored_satellite_ids", null)
                ?: legacyPrefs.getStringSet("monitored_satellite_ids", null)
                ?: setOf("iss_zarya", "tiangong", "starlink_train", "hubble")
        )
    }

    var mapTheme by remember {
        val saved = prefs.getString("satellite_map_theme", legacyPrefs.getString("satellite_map_theme", MapTheme.AUTO.name))
        mutableStateOf(
            when (saved) {
                MapTheme.DARK.name -> MapTheme.DARK
                MapTheme.LIGHT.name -> MapTheme.LIGHT
                else -> MapTheme.AUTO
            }
        )
    }

    fun persistMapTheme(theme: MapTheme) {
        mapTheme = theme
        prefs.edit().putString("satellite_map_theme", theme.name).apply()
        legacyPrefs.edit().putString("satellite_map_theme", theme.name).apply()
    }

    // ---- notification permission -------------------------------------------------------------
    var pendingNotificationAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pendingNotificationAction?.invoke()
        } else {
            Toast.makeText(
                context,
                if (isFa) "برای دریافت هشدارهای گذر ماهواره، لطفاً مجوز اعلان را فعال کنید."
                else "Notification permission is required to receive satellite pass alerts.",
                Toast.LENGTH_LONG,
            ).show()
        }
        pendingNotificationAction = null
    }
    val withNotificationPermission: (() -> Unit) -> Unit = { action ->
        if (NotificationPermissionHelper.hasPostNotificationPermission(context)) {
            action()
        } else {
            pendingNotificationAction = action
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ---- time ---------------------------------------------------------------------------------
    var realTimeNowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var timeOffsetMinutes by remember { mutableIntStateOf(0) }
    val currentSimulationMs = remember(realTimeNowMs, timeOffsetMinutes) {
        realTimeNowMs + timeOffsetMinutes * 60_000L
    }
    LaunchedEffect(timeOffsetMinutes) {
        while (timeOffsetMinutes == 0) {
            delay(1000L)
            realTimeNowMs = System.currentTimeMillis()
        }
    }

    // ---- TLE + Starlink snapshot -------------------------------------------------------------
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
            withContext(Dispatchers.IO) { tleRepo.refreshTles() }
        }
        wasOnline = isOnline
    }

    val realtimeBucketKey = currentSimulationMs / StarlinkTrainManager.REALTIME_SNAPSHOT_TTL_MS
    LaunchedEffect(starlinkCacheVersion, uiState.userLocation.latitude, uiState.userLocation.longitude, realtimeBucketKey) {
        val snapshot = withContext(Dispatchers.Default) {
            StarlinkTrainManager.selectRealtimeOverheadStarlinks(
                starlinkTles = tleRepo.getStarlinkTles(),
                userLatDeg = uiState.userLocation.latitude,
                userLonDeg = uiState.userLocation.longitude,
                nowMs = currentSimulationMs,
                maxOverhead = StarlinkTrainManager.DEFAULT_REALTIME_OVERHEAD_LIMIT,
            )
        }
        activeTrains = snapshot.activeTrains
        overheadStarlinks = snapshot.overheadStarlinks
    }
    val detectedTrain = remember(activeTrains) { activeTrains.firstOrNull()?.leaderItem }

    // ---- selection, scope, detail ------------------------------------------------------------
    var selectedCategory by remember { mutableStateOf(SatelliteCategory.ALL) }
    var selectedSatelliteId by remember { mutableStateOf<String?>("iss_zarya") }
    var activeDetailSatellite by remember { mutableStateOf<SatelliteItem?>(null) }
    var detailPreselectPassMs by remember { mutableStateOf<Long?>(null) }
    var anchoredSatLabelState by remember { mutableStateOf<SatelliteLiveState?>(null) }
    var earthFocusTarget by remember { mutableStateOf<EarthFocusTarget?>(null) }
    var focusSequence by remember { mutableLongStateOf(0L) }

    // ---- sheets & popovers -------------------------------------------------------------------
    var showDataStatus by remember { mutableStateOf(false) }
    var showAlertsSheet by remember { mutableStateOf(false) }
    var showGuideSheet by remember { mutableStateOf(false) }
    var showFleetSheet by remember { mutableStateOf(false) }
    var legendExpanded by remember { mutableStateOf(false) }
    var reminderTarget by remember { mutableStateOf<SatPassUi?>(null) }
    var snackMessage by remember { mutableStateOf<String?>(null) }
    var expandedNights by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var shownCounts by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    val reminders = remember { mutableStateMapOf<String, Int>() }
    val listState = rememberLazyListState()

    // ---- fleet --------------------------------------------------------------------------------
    val visibleSatellites = remember(activeTrains, overheadStarlinks, topPassStarlinks, selectedCategory) {
        val trainLeaders = activeTrains.map { it.leaderItem }
        val individualLimit = if (selectedCategory == SatelliteCategory.STARLINK) 16 else 8
        val individuals = (overheadStarlinks + topPassStarlinks).distinctBy { it.noradId }.take(individualLimit)
        SatelliteCatalog.getVisibleList(null) + trainLeaders + individuals
    }
    val filteredSatellites = remember(selectedCategory, visibleSatellites) {
        visibleSatellites.filter { sat -> matchesScope(sat, selectedCategory) }
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
                userLonDeg = uiState.userLocation.longitude,
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
                        userLonDeg = uiState.userLocation.longitude,
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
                    userLonDeg = uiState.userLocation.longitude,
                )
            }
        }
    }

    val allInteractiveMapStates = remember(liveSatelliteStates, trainPearlStatesByTrain, mapOverheadStarlinkStates) {
        val byId = LinkedHashMap<String, SatelliteLiveState>()
        for (st in liveSatelliteStates) byId[st.satellite.id] = st
        for ((_, pearls) in trainPearlStatesByTrain) {
            for (pearl in pearls) byId.putIfAbsent(pearl.satellite.id, pearl)
        }
        for (st in mapOverheadStarlinkStates) byId.putIfAbsent(st.satellite.id, st)
        byId.values.toList()
    }

    val selectedLiveState = remember(allInteractiveMapStates, liveSatelliteStates, selectedSatelliteId) {
        allInteractiveMapStates.firstOrNull { it.satellite.id == selectedSatelliteId }
            ?: liveSatelliteStates.firstOrNull()
    }

    // Deep link from a notification or another screen.
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
            activeTrains.any { train ->
                train.leaderItem.id == selectedSatelliteId || train.memberItems.any { it.id == selectedSatelliteId }
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
            isFa = isFa,
        )
    }

    // ---- passes -------------------------------------------------------------------------------
    val passWindowKey = remember(currentSimulationMs, timeOffsetMinutes) {
        if (timeOffsetMinutes == 0) (currentSimulationMs / 900_000L) * 900_000L
        else (currentSimulationMs / 60_000L) * 60_000L
    }
    var allUpcomingPasses by remember {
        mutableStateOf<List<Pair<SatelliteItem, ISSEngine.ISSPass>>?>(null)
    }

    LaunchedEffect(uiState.userLocation.latitude, uiState.userLocation.longitude, passWindowKey, starlinkCacheVersion) {
        withContext(Dispatchers.Default) {
            val starlinkTles = tleRepo.getStarlinkTles()
            val detectedTrainsNow = StarlinkTrainManager.detectActiveTrains(
                starlinkTles = starlinkTles,
                nowMs = passWindowKey,
            )
            val shortlistedStarlinks = StarlinkTrainManager.selectTopVisibleStarlinks(
                starlinkTles = starlinkTles,
                activeTrains = detectedTrainsNow,
                userLatDeg = uiState.userLocation.latitude,
                userLonDeg = uiState.userLocation.longitude,
                startTimeMs = passWindowKey,
                maxCount = StarlinkTrainManager.DEFAULT_TOP_VISIBLE_PASS_LIMIT,
            )

            val combined = mutableListOf<Pair<SatelliteItem, ISSEngine.ISSPass>>()
            val primaryTargets = SatelliteCatalog.satellites + detectedTrainsNow.map { it.leaderItem }
            for (sat in primaryTargets) {
                ISSEngine.predictPasses(
                    userLatDeg = uiState.userLocation.latitude,
                    userLonDeg = uiState.userLocation.longitude,
                    startTimestampMs = passWindowKey,
                    tle = SatelliteEngine.getEffectiveTle(sat),
                    scanDays = 3,
                    visibleOnly = true,
                    standardMag = sat.standardMagnitude,
                ).forEach { combined.add(sat to it) }
            }
            for (sat in shortlistedStarlinks) {
                ISSEngine.predictPasses(
                    userLatDeg = uiState.userLocation.latitude,
                    userLonDeg = uiState.userLocation.longitude,
                    startTimestampMs = passWindowKey,
                    tle = sat.defaultTle,
                    scanDays = 2,
                    visibleOnly = true,
                    standardMag = sat.standardMagnitude,
                ).forEach { combined.add(sat to it) }
            }
            val sorted = combined.sortedBy { it.second.startTimeMs }
            withContext(Dispatchers.Main) {
                topPassStarlinks = shortlistedStarlinks
                allUpcomingPasses = sorted
            }
        }
    }

    fun autoCovered(sat: SatelliteItem): Boolean =
        isAutoAlertEnabled && (monitoredIds.contains(sat.id) || (sat.isTrain && monitoredIds.contains("starlink_train")))

    fun toPassUi(pair: Pair<SatelliteItem, ISSEngine.ISSPass>): SatPassUi {
        val (sat, pass) = pair
        val key = passKey(sat, pass)
        return SatPassUi(
            key = key,
            sat = sat,
            satName = if (isFa) sat.nameFa else sat.nameEn,
            pass = pass,
            nightKey = SatNights.nightKey(pass.startTimeMs, tz),
            reminderLeadMinutes = reminders[key],
            isAutoCovered = autoCovered(sat),
        )
    }

    val scopedPasses: List<SatPassUi>? = remember(allUpcomingPasses, selectedCategory, isFa, reminders.toMap(), isAutoAlertEnabled, monitoredIds) {
        allUpcomingPasses?.let { list ->
            val cap = if (selectedCategory == SatelliteCategory.STARLINK) 12 else 10
            list.filter { (sat, _) -> matchesScope(sat, selectedCategory) }
                .map { toPassUi(it) }
                .sortedBy { it.pass.startTimeMs }
                // One page of nights: the per-night cap does the rest of the limiting.
                .take(cap * 4)
        }
    }

    val nightGroups = remember(scopedPasses, realTimeNowMs, isFa) {
        scopedPasses?.let { SatNights.group(it, realTimeNowMs, tz, isFa) } ?: emptyList()
    }
    val spotlight = remember(scopedPasses, currentSimulationMs, isFa) {
        scopedPasses?.let { SatNights.spotlight(it, currentSimulationMs, tz, isFa) }
    }

    // ---- actions ------------------------------------------------------------------------------
    fun focusSatellite(state: SatelliteLiveState) {
        selectedSatelliteId = state.satellite.id
        anchoredSatLabelState = state
        focusSequence += 1L
        earthFocusTarget = EarthFocusTarget(
            latDeg = state.topocentric.subLatDeg,
            lonDeg = state.topocentric.subLonDeg,
            requestId = focusSequence,
        )
    }

    fun focusSatelliteById(id: String) {
        val state = allInteractiveMapStates.firstOrNull { it.satellite.id == id }
        if (state != null) {
            focusSatellite(state)
        } else {
            selectedSatelliteId = id
        }
        scope.launch { listState.animateScrollToItem(0) }
    }

    fun openDetail(sat: SatelliteItem, passStartMs: Long? = null) {
        selectedSatelliteId = sat.id
        detailPreselectPassMs = passStartMs
        activeDetailSatellite = sat
    }

    fun showOnAr(sat: SatelliteItem) {
        viewModel.selectSatelliteById(sat.id)
        viewModel.selectTab(3)
    }

    fun setReminder(passUi: SatPassUi, leadMinutes: Int) {
        withNotificationPermission {
            AstroNotificationManager.scheduleSpecificPassAlarm(
                context = context,
                satellite = passUi.sat,
                pass = passUi.pass,
                userLocation = uiState.userLocation,
                leadMinutes = leadMinutes,
            )
            reminders[passUi.key] = leadMinutes
            val at = SatFmt.clock(passUi.pass.startTimeMs - leadMinutes * 60_000L, isFa, tz)
            snackMessage = if (isFa) "یادآوری برای $at تنظیم شد" else "Reminder set for $at"
        }
    }

    val nextPassInRange = remember(scopedPasses, currentSimulationMs) {
        scopedPasses?.any { it.pass.maxTimeMs in currentSimulationMs..(currentSimulationMs + 12L * 60L * 60L * 1000L) } == true
    }
    val passMarkers = remember(scopedPasses, realTimeNowMs) {
        scopedPasses.orEmpty()
            .map { ((it.pass.maxTimeMs - realTimeNowMs) / 60_000L).toInt() }
            .filter { abs(it) <= 720 }
            .distinct()
    }

    // ---- skin slots ---------------------------------------------------------------------------
    val slots = SatSkinSlots(
        statusChip = { modifier, data, onClick -> SatStatusChip(data, tokens, onClick, modifier, skin) },
        dock = { data, onDetails, onAr, onShowOnGlobe ->
            SatFocusDock(
                data = data,
                tokens = tokens,
                compact = compactDock,
                onDetails = onDetails,
                onAr = onAr,
                onShowOnGlobe = onShowOnGlobe,
            )
        },
        passCard = { modifier, passUi, actions ->
            SatCompactPassCard(passUi, tokens, isFa, actions, modifier)
        },
        spotlight = { modifier, data, actions ->
            SatSpotlightCard(data, tokens, isFa, actions, modifier)
        },
    )

    val dockData = selectedLiveState?.let { state ->
        SatDockData(
            shortName = shortNameOf(state.satellite, isFa),
            fullName = if (isFa) state.satellite.nameFa else state.satellite.nameEn,
            categoryLabel = SatFmt.categoryLabel(state.satellite.category, isFa),
            isSunlit = state.topocentric.isSunlit,
            isAboveHorizon = state.topocentric.elevationDeg > 0.0,
            altitudeKm = state.topocentric.satAltKm,
            speedKmS = state.topocentric.velocityKmS,
            elevationDeg = state.topocentric.elevationDeg,
            rangeKm = state.topocentric.rangeKm,
            azimuthDeg = state.topocentric.azimuthDeg,
            magnitude = state.apparentMagnitude,
            subLatDeg = state.topocentric.subLatDeg,
            subLonDeg = state.topocentric.subLonDeg,
            isSimulated = timeOffsetMinutes != 0,
            isCached = !isOnline,
            isFa = isFa,
        )
    }

    val fleetTokens = remember(liveSatelliteStates, selectedSatelliteId, isFa) {
        liveSatelliteStates.map { state ->
            SatTokenUi(
                id = state.satellite.id,
                shortName = shortNameOf(state.satellite, isFa),
                fullName = if (isFa) state.satellite.nameFa else state.satellite.nameEn,
                isTrain = state.satellite.isTrain,
                trainCount = state.satellite.trainCount,
                elevationDeg = state.topocentric.elevationDeg,
                isNakedEye = state.isNakedEyeVisible,
                isSelected = state.satellite.id == selectedSatelliteId,
            )
        }
    }

    val statusData = SatStatusData(
        isLive = timeOffsetMinutes == 0,
        offsetMinutes = timeOffsetMinutes,
        isOnline = isOnline,
        tleAgeDays = selectedLiveState?.tleMetadata?.epochAgeDays,
        usingCachedTle = !isOnline,
        sourceLabel = selectedLiveState?.tleMetadata?.source?.name ?: (if (isFa) "TLE داخلی" else "Bundled TLE"),
        isFa = isFa,
    )

    // ---- detail screen ------------------------------------------------------------------------
    if (activeDetailSatellite != null) {
        SatelliteDetailScreen(
            satelliteItem = activeDetailSatellite!!,
            userLocation = uiState.userLocation,
            language = uiState.language,
            onBack = {
                activeDetailSatellite = null
                detailPreselectPassMs = null
            },
            simulationTimestampMs = currentSimulationMs,
            skin = skin,
            preselectedPassStartMs = detailPreselectPassMs,
            reminders = reminders.toMap(),
            onRemoveReminder = { sat, pass -> reminders.remove(passKey(sat, pass)) },
            onSetReminder = { sat, pass, lead ->
                withNotificationPermission {
                    AstroNotificationManager.scheduleSpecificPassAlarm(
                        context = context,
                        satellite = sat,
                        pass = pass,
                        userLocation = uiState.userLocation,
                        leadMinutes = lead,
                    )
                    reminders[passKey(sat, pass)] = lead
                }
            },
            onShowOnGlobe = { sat -> activeDetailSatellite = null; focusSatelliteById(sat.id) },
            onSpotInAr = { sat -> activeDetailSatellite = null; showOnAr(sat) },
            themeMode = uiState.themeMode,
        )
        return
    }

    val horizontalPadding = if (widthClass == SatWidthClass.LARGE) 20.dp else 16.dp
    val tokenWidth = when (widthClass) {
        SatWidthClass.COMPACT -> 72.dp
        SatWidthClass.MEDIUM -> 80.dp
        SatWidthClass.LARGE -> 88.dp
    }

    val miniDockVisible by remember { derivedStateOf { listState.firstVisibleItemIndex > 1 } }

    CompositionLocalProvider(
        LocalSatSkin provides skin,
        LocalZigSatTokens provides tokens,
        LocalSatReduceMotion provides reduceMotion,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .testTag("satellite_screen"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    bottom = SatGrid.bottomClearance,
                ),
                verticalArrangement = Arrangement.spacedBy(SatGrid.gapInGroup),
            ) {
                // A. Header row
                item(key = "header") {
                    SatHeaderRow(
                        tokens = tokens,
                        data = statusData,
                        alertsActive = isAutoAlertEnabled,
                        onStatusClick = { showDataStatus = true },
                        onAlertsClick = { showAlertsSheet = true },
                        onGuideClick = { showGuideSheet = true },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                // B. Globe stage with its HUD zones
                item(key = "globe") {
                    val mapThemeIcon: ImageVector = when (mapTheme) {
                        MapTheme.AUTO -> Icons.Filled.BrightnessAuto
                        MapTheme.DARK -> Icons.Filled.DarkMode
                        MapTheme.LIGHT -> Icons.Filled.LightMode
                    }
                    SatGlobeStage(
                        tokens = tokens,
                        mapThemeLabel = when (mapTheme) {
                            MapTheme.AUTO -> if (isFa) "خودکار" else "Auto"
                            MapTheme.DARK -> if (isFa) "تاریک" else "Dark"
                            MapTheme.LIGHT -> if (isFa) "روشن" else "Light"
                        },
                        mapThemeIcon = mapThemeIcon,
                        isSimulated = timeOffsetMinutes != 0,
                        onReset = {
                            focusSequence += 1L
                            earthFocusTarget = EarthFocusTarget(
                                latDeg = uiState.userLocation.latitude,
                                lonDeg = uiState.userLocation.longitude,
                                requestId = focusSequence,
                            )
                        },
                        onCycleMapTheme = {
                            persistMapTheme(
                                when (mapTheme) {
                                    MapTheme.AUTO -> MapTheme.DARK
                                    MapTheme.DARK -> MapTheme.LIGHT
                                    MapTheme.LIGHT -> MapTheme.AUTO
                                }
                            )
                        },
                        legendExpanded = legendExpanded,
                        onToggleLegend = { legendExpanded = !legendExpanded },
                        isFa = isFa,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
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
                            overlayStyle = earthOverlayStyle(tokens, uiState.themeMode),
                            focusTarget = earthFocusTarget,
                            onSelectSatellite = { state ->
                                // First tap focuses on the globe, second tap on the same marker opens detail.
                                if (state.satellite.id == selectedSatelliteId) {
                                    openDetail(state.satellite)
                                } else {
                                    focusSatellite(state)
                                }
                            },
                            onDismissAnchoredLabel = { anchoredSatLabelState = null },
                            onOpenSatelliteDetail = { sat -> openDetail(sat) },
                            modifier = globeModifier,
                        )
                    }
                }

                // C. Focus dock — rides up over the globe's lower margin so the globe stays the
                // largest element on the screen.
                item(key = "dock") {
                    if (dockData != null) {
                        Box(modifier = Modifier.offset(y = (-16).dp)) {
                            slots.dock(
                                dockData,
                                { selectedLiveState?.satellite?.let { openDetail(it) } },
                                { selectedLiveState?.satellite?.let { showOnAr(it) } },
                                { selectedLiveState?.let { focusSatellite(it) } },
                            )
                        }
                    }
                }

                // D. Time strip
                item(key = "timestrip") {
                    SatTimeStrip(
                        tokens = tokens,
                        offsetMinutes = timeOffsetMinutes,
                        clockLabel = SatFmt.dateTime(currentSimulationMs, isFa, tz),
                        passMarkers = passMarkers,
                        nextPassAvailable = nextPassInRange,
                        actions = SatTimeActions(
                            onOffsetChange = { timeOffsetMinutes = it },
                            onLive = { timeOffsetMinutes = 0 },
                            onNextPass = {
                                val next = scopedPasses.orEmpty()
                                    .map { it.pass.maxTimeMs }
                                    .filter { it > currentSimulationMs }
                                    .minOrNull()
                                if (next != null) {
                                    timeOffsetMinutes = (((next - realTimeNowMs) / 60_000L).toInt()).coerceIn(-720, 720)
                                }
                            },
                            onStep = { delta -> timeOffsetMinutes = (timeOffsetMinutes + delta).coerceIn(-720, 720) },
                        ),
                        isFa = isFa,
                    )
                }

                // E. Scope chips
                item(key = "scope") {
                    SatScopeRow(
                        selected = selectedCategory,
                        tokens = tokens,
                        isFa = isFa,
                        onSelect = { selectedCategory = it },
                    )
                }

                // F. Fleet rail
                item(key = "fleet") {
                    SatFleetRail(
                        tokens = tokens,
                        headerLabel = if (isFa) "تحت پایش" else "Tracked",
                        count = fleetTokens.size,
                        aboveHorizonCount = fleetTokens.count { it.elevationDeg > 0.0 },
                        fleet = fleetTokens,
                        isFa = isFa,
                        onTokenTap = { tokenUi ->
                            if (tokenUi.id == selectedSatelliteId) {
                                val sat = liveSatelliteStates.firstOrNull { it.satellite.id == tokenUi.id }?.satellite
                                if (sat != null) openDetail(sat)
                            } else {
                                focusSatelliteById(tokenUi.id)
                            }
                        },
                        onSeeAll = { showFleetSheet = true },
                        tokenWidth = tokenWidth,
                    )
                }

                // G. Next pass spotlight
                if (spotlight != null) {
                    item(key = "spotlight") {
                        slots.spotlight(
                            Modifier,
                            spotlight,
                            SatPassActions(
                                onOpenDetail = { openDetail(spotlight.pass.sat, spotlight.pass.pass.startTimeMs) },
                                onShowOnGlobe = { focusSatelliteById(spotlight.pass.sat.id) },
                                onAr = { showOnAr(spotlight.pass.sat) },
                                onRemind = { reminderTarget = spotlight.pass },
                                onRemoveReminder = {
                                    reminders.remove(spotlight.pass.key)
                                    snackMessage = if (isFa) "یادآوری حذف شد" else "Reminder removed"
                                },
                                isReminderEnabled = true,
                            ),
                        )
                    }
                }

                // H. Passes by night
                item(key = "passes_header") {
                    SatSectionHeader(
                        title = if (isFa) "گذرهای پیشِ رو" else "Upcoming passes",
                        tokens = tokens,
                        isFa = isFa,
                        count = scopedPasses?.size,
                        subtitle = if (isFa) "۳ روز آینده" else "Next 3 days",
                        scopeEcho = if (selectedCategory != SatelliteCategory.ALL) {
                            SatFmt.categoryLabel(selectedCategory, isFa)
                        } else {
                            null
                        },
                        onClearScope = { selectedCategory = SatelliteCategory.ALL },
                    )
                }

                item(key = "passes") {
                    val passes = scopedPasses
                    when {
                        passes == null -> SatPassSkeletons(tokens)
                        passes.isEmpty() -> SatPassEmpty(
                            tokens = tokens,
                            isFa = isFa,
                            scopeLabel = if (selectedCategory == SatelliteCategory.ALL) null else SatFmt.categoryLabel(selectedCategory, isFa),
                            onShowAll = { selectedCategory = SatelliteCategory.ALL },
                            onOpenSevenDay = {
                                selectedSatelliteId?.let { id ->
                                    SatelliteCatalog.getById(id, detectedTrain)?.let { openDetail(it) }
                                }
                            },
                        )
                        else -> SatPassesByNight(
                            groups = nightGroups,
                            tokens = tokens,
                            isFa = isFa,
                            actionsFor = { passUi ->
                                SatPassActions(
                                    onOpenDetail = { openDetail(passUi.sat, passUi.pass.startTimeMs) },
                                    onShowOnGlobe = { focusSatelliteById(passUi.sat.id) },
                                    onAr = { showOnAr(passUi.sat) },
                                    onRemind = { reminderTarget = passUi },
                                    onRemoveReminder = { reminders.remove(passUi.key) },
                                    isReminderEnabled = true,
                                )
                            },
                            onToggleNight = { key ->
                                expandedNights = if (key in expandedNights) expandedNights - key else expandedNights + key
                            },
                            onShowMore = { key ->
                                shownCounts = shownCounts + (key to ((shownCounts[key] ?: 3) + 1))
                            },
                            expandedNights = expandedNights,
                            shownCounts = shownCounts,
                            maxCardsPerNight = if (selectedCategory == SatelliteCategory.STARLINK) 12 else 10,
                        )
                    }
                }
            }

            // Sticky mini-dock: appears once the globe has left the viewport.
            AnimatedVisibility(
                visible = miniDockVisible,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = horizontalPadding, vertical = 6.dp),
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
            ) {
                if (dockData != null) {
                    SatMiniDock(
                        data = dockData,
                        tokens = tokens,
                        isSimulated = timeOffsetMinutes != 0,
                        onReturnToLive = { timeOffsetMinutes = 0 },
                        onGlobe = { scope.launch { listState.animateScrollToItem(0) } },
                    )
                }
            }

            // Confirmation snackbar, with Undo.
            AnimatedVisibility(
                visible = snackMessage != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = horizontalPadding, vertical = 24.dp),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                SatSnackbar(
                    message = snackMessage.orEmpty(),
                    actionLabel = if (isFa) "بازگردانی" else "Undo",
                    tokens = tokens,
                    onAction = { snackMessage = null },
                )
            }
        }
    }

    LaunchedEffect(snackMessage) {
        if (snackMessage != null) {
            delay(4000L)
            snackMessage = null
        }
    }

    // ---- sheets --------------------------------------------------------------------------------
    if (showDataStatus) {
        SatDataStatusSheet(
            data = statusData,
            tokens = tokens,
            onDismiss = { showDataStatus = false },
            onReturnToLive = {
                timeOffsetMinutes = 0
                showDataStatus = false
            },
        )
    }

    if (showAlertsSheet) {
        SatAlertsSheet(
            tokens = tokens,
            isFa = isFa,
            enabled = isAutoAlertEnabled,
            leadMinutes = selectedLeadMinutes,
            monitoredIds = monitoredIds,
            nextAlertSummary = scopedPasses?.firstOrNull()?.let { passUi ->
                "${passUi.satName} · ${SatNights.title(passUi.nightKey, realTimeNowMs, tz, isFa)} " +
                    SatFmt.clock(passUi.pass.startTimeMs - selectedLeadMinutes * 60_000L, isFa, tz)
            },
            onDismiss = { showAlertsSheet = false },
            onSave = { enabled, lead, ids ->
                isAutoAlertEnabled = enabled
                selectedLeadMinutes = lead
                monitoredIds = ids
                legacyPrefs.edit()
                    .putBoolean("auto_satellite_alerts_enabled", enabled)
                    .putBoolean("iss_auto_alerts_enabled", enabled)
                    .putStringSet("monitored_satellite_ids", ids)
                    .putInt("satellite_alert_lead_minutes", lead)
                    .putInt("iss_alert_lead_minutes", lead)
                    .apply()
                prefs.edit()
                    .putBoolean("auto_satellite_alerts_enabled", enabled)
                    .putBoolean("iss_auto_alerts_enabled", enabled)
                    .putStringSet("monitored_satellite_ids", ids)
                    .putInt("satellite_alert_lead_minutes", lead)
                    .putInt("iss_alert_lead_minutes", lead)
                    .putString("user_city_name_fa", uiState.userLocation.cityNameFa)
                    .putString("user_city_name_en", uiState.userLocation.cityNameEn)
                    .putFloat("user_lat", uiState.userLocation.latitude.toFloat())
                    .putFloat("user_lon", uiState.userLocation.longitude.toFloat())
                    .apply()
                if (enabled) {
                    withNotificationPermission {
                        scope.launch(Dispatchers.Default) {
                            AstroNotificationManager.scheduleRollingSatellitePasses(
                                context = context,
                                userLocation = uiState.userLocation,
                                selectedSatIds = ids,
                                leadMinutes = lead,
                                scanDays = 7,
                            )
                        }
                    }
                } else {
                    AstroNotificationManager.cancelAllPassNotifications(context)
                }
                showAlertsSheet = false
            },
        )
    }

    if (showGuideSheet) {
        SatGuideSheet(tokens = tokens, isFa = isFa, onDismiss = { showGuideSheet = false })
    }

    if (showFleetSheet) {
        SatFleetSheet(
            tokens = tokens,
            isFa = isFa,
            fleet = fleetTokens,
            onPick = { tokenUi ->
                showFleetSheet = false
                focusSatelliteById(tokenUi.id)
            },
            onDismiss = { showFleetSheet = false },
        )
    }

    val reminder = reminderTarget
    if (reminder != null) {
        SatSheetScaffold(
            tokens = tokens,
            onDismiss = { reminderTarget = null },
            testTag = "reminder_popover",
        ) {
            SatSheetTitle(
                title = if (isFa) "یادآوری گذر" else "Pass reminder",
                subtitle = reminder.satName,
                tokens = tokens,
            )
            SatReminderOptions(
                tokens = tokens,
                isFa = isFa,
                passStartMs = reminder.pass.startTimeMs,
                selectedLeadMinutes = reminders[reminder.key],
                onChoose = { lead ->
                    setReminder(reminder, lead)
                    reminderTarget = null
                },
                onRemove = if (reminders.containsKey(reminder.key)) {
                    {
                        reminders.remove(reminder.key)
                        snackMessage = if (isFa) "یادآوری حذف شد" else "Reminder removed"
                        reminderTarget = null
                    }
                } else {
                    null
                },
            )
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Header and mini-dock
// ------------------------------------------------------------------------------------------------

@Composable
private fun SatHeaderRow(
    tokens: ZigSatTokens,
    data: SatStatusData,
    alertsActive: Boolean,
    onStatusClick: () -> Unit,
    onAlertsClick: () -> Unit,
    onGuideClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SatStatusChip(
            data = data,
            tokens = tokens,
            onClick = onStatusClick,
            modifier = Modifier.weight(1f),
            skin = LocalSatSkin.current,
        )
        ZigSatIconButton(
            icon = if (alertsActive) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
            contentDescription = if (data.isFa) "هشدارها" else "Alerts",
            tokens = tokens,
            onClick = onAlertsClick,
            active = alertsActive,
            testTag = "satellite_alerts_toggle",
        )
        ZigSatIconButton(
            icon = Icons.Filled.HelpOutline,
            contentDescription = if (data.isFa) "راهنما" else "Guide",
            tokens = tokens,
            onClick = onGuideClick,
            testTag = "satellite_help_button",
        )
    }
}

/** 52 dp capsule under the status inset once the globe has scrolled away. */
@Composable
private fun SatMiniDock(
    data: SatDockData,
    tokens: ZigSatTokens,
    isSimulated: Boolean,
    onReturnToLive: () -> Unit,
    onGlobe: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RedShapes.capsule)
            .background(tokens.cardAlt)
            .border(1.dp, tokens.border, RedShapes.capsule)
            .padding(horizontal = 14.dp)
            .testTag("satellite_mini_dock"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.SatelliteAlt,
            contentDescription = null,
            tint = tokens.live,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = data.shortName,
            style = RedTypographyTokens.badge.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            color = tokens.inkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${SatFmt.kilometers(data.altitudeKm, data.isFa)} km",
            style = RedTypographyTokens.badge.copy(fontSize = 11.sp).tabular(),
            color = tokens.inkSecondary,
            maxLines = 1,
        )
        ZigStatusDot(color = if (isSimulated) tokens.sim else tokens.live, pulse = !isSimulated, size = 6.dp)
        Spacer(modifier = Modifier.weight(1f))
        if (isSimulated) {
            Box(
                modifier = Modifier
                    .height(SatGrid.minTouchTarget)
                    .clickable(onClick = onReturnToLive),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (data.isFa) "\u21BA زنده" else "\u21BA Live",
                    style = RedTypographyTokens.badge.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = tokens.sim,
                )
            }
        }
        Box(
            modifier = Modifier
                .height(SatGrid.minTouchTarget)
                .clickable(onClick = onGlobe),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "\u2191 " + if (data.isFa) "کره" else "Globe",
                style = RedTypographyTokens.badge.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = tokens.live,
            )
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Helpers
// ------------------------------------------------------------------------------------------------

internal fun matchesScope(sat: SatelliteItem, scope: SatelliteCategory): Boolean = when (scope) {
    SatelliteCategory.ALL -> true
    SatelliteCategory.ISS -> sat.category == SatelliteCategory.ISS
    SatelliteCategory.STARLINK -> sat.category == SatelliteCategory.STARLINK || sat.isTrain
    SatelliteCategory.HUBBLE -> sat.category == SatelliteCategory.HUBBLE
    SatelliteCategory.VISIBLE -> sat.isNakedEyeCandidate
}

internal fun passKey(sat: SatelliteItem, pass: ISSEngine.ISSPass): String = "${sat.id}_${pass.startTimeMs}"

/** Rail/popover name: short enough to survive a token's 72 dp width. */
internal fun shortNameOf(sat: SatelliteItem, isFa: Boolean): String {
    val full = if (isFa) sat.nameFa else sat.nameEn
    if (sat.isTrain) return if (isFa) "قطار" else "Train"
    val base = full.substringBefore(" (").substringBefore(" (")
    return base.take(if (isFa) 14 else 12).trim()
}

/** The globe overlay palette, expressed in the semantic tokens of the active skin. */
internal fun earthOverlayStyle(tokens: ZigSatTokens, themeMode: ThemeMode): SatelliteEarthOverlayStyle {
    val dark = themeMode != ThemeMode.LIGHT
    return SatelliteEarthOverlayStyle(
        gridColor = if (dark) Color(0x18FFFFFF) else Color(0x1C0F172A),
        equatorColor = tokens.starlink.copy(alpha = 0.32f),
        limbRimColor = if (dark) Color.White.copy(alpha = 0.14f) else Color(0xFF182238).copy(alpha = 0.22f),
        cityLightAccent = Color(0xFFFFC107).copy(alpha = 0.42f),
        userPinColor = if (dark) Color(0xFF00F0FF) else Color(0xFF182238),
        userLabelColor = Color.White,
        orbitTrackColor = tokens.live.copy(alpha = 0.90f),
        orbitGlowColor = tokens.live.copy(alpha = 0.24f),
        selectedMarkerColor = tokens.live,
        selectedGlowColor = tokens.live.copy(alpha = 0.55f),
        starlinkColor = tokens.starlink,
        trainChainColor = tokens.starlink.copy(alpha = 0.54f),
        trainLeaderColor = Color.White,
        trainMemberColor = tokens.starlink.copy(alpha = 0.75f),
        nakedEyeMarkerColor = tokens.nakedEye,
        defaultMarkerColor = Color(0xFFFFB703),
        resetButtonContainerColor = Color.Black.copy(alpha = 0.55f),
        popoverContainerColor = tokens.popoverFill,
        popoverBorderColor = tokens.popoverBorder,
        popoverAccentColor = tokens.starlink,
        popoverShape = squircleShape(SatRadius.popover),
        showPopoverLeadingIcon = true,
        altPrefixEn = "",
        altPrefixFa = "",
    )
}
