package com.alijafari.red.astronomy.ui.satellites

import android.content.Context
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.EarthViewportProjection
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.*
import com.alijafari.red.astronomy.data.repository.TleRepository
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.notification.AstroNotificationManager
import com.alijafari.red.astronomy.notification.AstroNotificationStore
import com.alijafari.red.astronomy.rememberEarthCameraController
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.components.EarthFocusTarget
import com.alijafari.red.astronomy.ui.components.SATELLITE_EARTH_DEFAULT_DISTANCE
import com.alijafari.red.astronomy.ui.components.SatelliteEarth3D
import com.alijafari.red.astronomy.ui.components.SatelliteEarthOverlayStyle
import com.alijafari.red.astronomy.ui.components.satelliteViewport
import com.alijafari.red.astronomy.ui.screens.MapTheme
import com.alijafari.red.astronomy.ui.screens.SatelliteDetailScreen
import com.alijafari.red.astronomy.ui.screens.ar.ArScreenSupport
import com.alijafari.red.astronomy.ui.theme.squircleShape
import com.alijafari.red.astronomy.util.formatTleStalenessBadge
import com.alijafari.red.astronomy.util.rememberIsNetworkOnline
import java.util.TimeZone
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MINUTE = 60_000L
private const val DAY = 24 * 60 * MINUTE

private data class SatDetailEntry(val satellite: SatelliteItem, val pass: ISSEngine.ISSPass?)
private data class SatFeedback(val label: String, val undo: (() -> Unit)? = null)

/** The data/interaction owner for all three entry points. Renderers do not own navigation state. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SatellitesStage(uiState: MainUiState, viewModel: MainViewModel, skin: SatSkin) {
    val context = LocalContext.current
    val view = LocalView.current
    val location = uiState.userLocation
    val isFa = uiState.language == AppLanguage.PERSIAN
    val isOnline by rememberIsNetworkOnline(context)
    val tokens = remember(skin, uiState.themeMode) { zigSatTokens(skin, uiState.themeMode) }
    val tz = remember(location.timezoneId) { TimeEngine.resolveTimeZone(location.timezoneId) }
    val nights = remember(location.latitude, location.longitude, tz) { SatObserverNights(location.latitude, location.longitude, tz) }
    val reduce = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { context.getSharedPreferences("astro_app_prefs", Context.MODE_PRIVATE) }
    val legacy = remember(context) { context.getSharedPreferences("astro_prefs", Context.MODE_PRIVATE) }
    val screen = rememberSaveable(saver = SatellitesState.Saver) {
        val name = prefs.getString("satellite_map_theme", legacy.getString("satellite_map_theme", null))
        val savedScope = prefs.getString("satellite_scope", null)
        SatellitesState(SatellitesUiState(
            scope = SatelliteCategory.entries.firstOrNull { it.name == savedScope } ?: SatelliteCategory.ALL,
            mapTheme = MapTheme.entries.firstOrNull { it.name == name } ?: MapTheme.AUTO,
        ))
    }
    val state = screen.ui
    val mapTheme = state.mapTheme
    val selectedCategory = state.scope
    val selectedSatelliteId = state.focusId
    val timeOffsetMinutes = state.timeOffsetMin
    fun setSatelliteScope(category: SatelliteCategory) {
        screen.scope(category)
        prefs.edit().putString("satellite_scope", category.name).apply()
    }
    var config by remember(prefs, legacy) {
        val defaults = SatAlertsConfig()
        mutableStateOf(SatAlertsConfig(
            enabled = prefs.getBoolean("auto_satellite_alerts_enabled", prefs.getBoolean("iss_auto_alerts_enabled",
                legacy.getBoolean("auto_satellite_alerts_enabled", legacy.getBoolean("iss_auto_alerts_enabled", false)))),
            leadMinutes = prefs.getInt("satellite_alert_lead_minutes", prefs.getInt("iss_alert_lead_minutes",
                legacy.getInt("satellite_alert_lead_minutes", legacy.getInt("iss_alert_lead_minutes", 10)))),
            satelliteIds = prefs.getStringSet("monitored_satellite_ids", null)?.toSet()
                ?: legacy.getStringSet("monitored_satellite_ids", null)?.toSet() ?: defaults.satelliteIds,
        ))
    }
    var firstRun by remember(prefs) { mutableStateOf(!prefs.getBoolean("satellite_globe_hud_seen", false)) }
    LaunchedEffect(Unit) {
        if (firstRun) {
            prefs.edit().putBoolean("satellite_globe_hud_seen", true).apply()
            delay(8000)
            firstRun = false
        }
    }
    val permissionMessage = stringResource(R.string.sat_permission)
    var pendingPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pendingPermission?.invoke()
        else Toast.makeText(context, permissionMessage, Toast.LENGTH_LONG).show()
        pendingPermission = null
    }
    fun withNotificationPermission(action: () -> Unit) {
        if (com.alijafari.red.astronomy.notification.NotificationPermissionHelper.hasPostNotificationPermission(context)) action()
        else {
            pendingPermission = action
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var realTimeNowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L - System.currentTimeMillis() % 1000L)
            realTimeNowMs = System.currentTimeMillis()
        }
    }
    val currentSimulationMs = realTimeNowMs + timeOffsetMinutes * MINUTE
    var timeAnimation by remember { mutableStateOf<Job?>(null) }
    fun setTime(minutes: Int, animate: Boolean = false) {
        timeAnimation?.cancel()
        if (!animate || reduce) screen.offset(minutes)
        else timeAnimation = scope.launch {
            Animatable(screen.ui.timeOffsetMin.toFloat()).animateTo(minutes.toFloat(), tween(400)) { screen.offset(value.roundToInt()) }
        }
    }

    // The TLE and train authorities are unchanged; the UI only projects their snapshots.
    val tleRepo = remember { TleRepository.getInstance(context) }
    val starlinkCacheVersion by tleRepo.starlinkVersion.collectAsState()
    var tleRefreshTick by remember { mutableIntStateOf(0) }
    var wasOnline by remember { mutableStateOf(false) }
    LaunchedEffect(isOnline) {
        withContext(Dispatchers.IO) { tleRepo.ensureStarlinkDiskCacheLoaded() }
        if (!wasOnline && isOnline && withContext(Dispatchers.IO) { tleRepo.refreshTles() }) tleRefreshTick++
        wasOnline = isOnline
    }
    var activeTrains by remember { mutableStateOf<List<StarlinkTrainManager.StarlinkTrain>>(emptyList()) }
    var overheadStarlinks by remember { mutableStateOf<List<SatelliteItem>>(emptyList()) }
    var topPassStarlinks by remember { mutableStateOf<List<SatelliteItem>>(emptyList()) }
    val realtimeBucketKey = currentSimulationMs / StarlinkTrainManager.REALTIME_SNAPSHOT_TTL_MS
    LaunchedEffect(starlinkCacheVersion, location, realtimeBucketKey) {
        if (timeOffsetMinutes != 0) delay(140)
        val snapshot = withContext(Dispatchers.Default) {
            StarlinkTrainManager.selectRealtimeOverheadStarlinks(tleRepo.getStarlinkTles(), location.latitude,
                location.longitude, location.elevationMeters, nowMs = currentSimulationMs, maxOverhead = StarlinkTrainManager.DEFAULT_REALTIME_OVERHEAD_LIMIT)
        }
        activeTrains = snapshot.activeTrains
        overheadStarlinks = snapshot.overheadStarlinks
    }
    val detectedTrain = activeTrains.firstOrNull()?.leaderItem
    val visibleSatellites = remember(activeTrains, overheadStarlinks, topPassStarlinks, selectedCategory) {
        val individuals = (overheadStarlinks + topPassStarlinks).distinctBy { it.noradId }
            .take(if (selectedCategory == SatelliteCategory.STARLINK) 16 else 8)
        SatelliteCatalog.getVisibleList(null) + activeTrains.map { it.leaderItem } + individuals
    }
    val filteredSatellites = remember(visibleSatellites, selectedCategory) { visibleSatellites.filter { matchesScope(it, selectedCategory) } }
    val showStarlinks = selectedCategory == SatelliteCategory.ALL || selectedCategory == SatelliteCategory.STARLINK || selectedCategory == SatelliteCategory.VISIBLE
    var retainedFocus by remember { mutableStateOf<SatelliteItem?>(null) }
    val selectedSatItem = remember(selectedSatelliteId, visibleSatellites, activeTrains, retainedFocus) {
        visibleSatellites.firstOrNull { it.id == selectedSatelliteId }
            ?: activeTrains.asSequence().flatMap { it.memberItems.asSequence() }.firstOrNull { it.id == selectedSatelliteId }
            ?: retainedFocus?.takeIf { it.id == selectedSatelliteId }
            ?: SatelliteCatalog.getById(selectedSatelliteId, detectedTrain)
    }
    fun propagate(sat: SatelliteItem) = SatelliteEngine.calculateSatelliteState(
        satellite = sat, timestampMs = currentSimulationMs, userLatDeg = location.latitude, userLonDeg = location.longitude,
        userAltMeters = location.elevationMeters,
    )
    val liveSatelliteStates = remember(filteredSatellites, currentSimulationMs, location, tleRefreshTick) { filteredSatellites.map(::propagate) }
    val trainPearlStatesByTrain = remember(activeTrains, showStarlinks, currentSimulationMs, location) {
        if (showStarlinks) activeTrains.map { train -> train to train.memberItems.map(::propagate) } else emptyList()
    }
    val mapOverheadStarlinkStates = remember(overheadStarlinks, showStarlinks, currentSimulationMs, location) {
        if (showStarlinks) overheadStarlinks.map(::propagate) else emptyList()
    }
    val selectedLiveState = remember(selectedSatItem, liveSatelliteStates, currentSimulationMs, location, tleRefreshTick) {
        liveSatelliteStates.firstOrNull { it.satellite.id == selectedSatelliteId } ?: selectedSatItem?.let(::propagate)
            ?: liveSatelliteStates.firstOrNull()
    }
    val globeFleet = remember(liveSatelliteStates, selectedLiveState) { (liveSatelliteStates + listOfNotNull(selectedLiveState)).distinctBy { it.satellite.id } }
    val allInteractiveMapStates = remember(globeFleet, trainPearlStatesByTrain, mapOverheadStarlinkStates) {
        (globeFleet + trainPearlStatesByTrain.flatMap { it.second } + mapOverheadStarlinkStates).distinctBy { it.satellite.id }
    }
    var anchorId by remember { mutableStateOf<String?>(null) }
    var focusSequence by remember { mutableLongStateOf(0L) }
    var earthFocusTarget by remember { mutableStateOf<EarthFocusTarget?>(null) }
    var centered by remember { mutableStateOf(true) }
    val camera = rememberEarthCameraController(location.latitude, location.longitude, SATELLITE_EARTH_DEFAULT_DISTANCE)
    fun focusSatellite(live: SatelliteLiveState) {
        screen.focus(live.satellite.id)
        retainedFocus = live.satellite
        anchorId = live.satellite.id
        earthFocusTarget = EarthFocusTarget(live.topocentric.subLatDeg, live.topocentric.subLonDeg,
            SATELLITE_EARTH_DEFAULT_DISTANCE * .974f, ++focusSequence)
        firstRun = false
    }
    fun focusItem(satellite: SatelliteItem) = focusSatellite(allInteractiveMapStates.firstOrNull { it.satellite.id == satellite.id } ?: propagate(satellite))
    fun cycle(delta: Int) {
        if (liveSatelliteStates.isEmpty()) return
        val index = liveSatelliteStates.indexOfFirst { it.satellite.id == screen.ui.focusId }
        val next = if (index < 0) (if (delta > 0) 0 else liveSatelliteStates.lastIndex)
            else (index + delta + liveSatelliteStates.size) % liveSatelliteStates.size
        focusSatellite(liveSatelliteStates[next])
    }
    LaunchedEffect(uiState.selectedSatelliteId, uiState.selectedTab, starlinkCacheVersion) {
        if (uiState.selectedTab != 1) return@LaunchedEffect
        val id = uiState.selectedSatelliteId ?: return@LaunchedEffect
        val known = allInteractiveMapStates.firstOrNull { it.satellite.id == id || "sat_${it.satellite.noradId}" == id }?.satellite
            ?: SatelliteCatalog.getById(id, detectedTrain)
        if (known.id == id || "sat_${known.noradId}" == id || id == "starlink_train") {
            focusItem(known)
            viewModel.selectSatelliteById(null)
        }
    }

    val meta = selectedLiveState?.tleMetadata
    val tleAge = meta?.epochAgeDays
    val cachedAge = remember(starlinkCacheVersion, realTimeNowMs / MINUTE) { tleRepo.getStarlinkCacheAgeDays(realTimeNowMs) }
    val trainSelected = selectedLiveState?.satellite?.isTrain == true
    // Keep train drift transparency in addition to the standard age bucket.
    val trainDrift = trainSelected && formatTleStalenessBadge(trainSelected, meta, cachedAge, isFa) != null
    val statusAge = if (trainDrift) maxOf(tleAge ?: 0.0, cachedAge ?: 0.0) else tleAge
    val status = SatStatusData(timeOffsetMinutes == 0, timeOffsetMinutes, isOnline, statusAge,
        tleAge?.let { realTimeNowMs - (it * DAY).toLong() }, meta?.source, isFa)

    // Cache a superset of the ±12h scrub window and the three-day feed. Scrubbing only changes
    // the filtering and live propagation, never launches a fresh multi-day search per slider tick.
    val passWindowKey = realTimeNowMs / (15 * MINUTE) * (15 * MINUTE) - 12 * 60 * MINUTE
    var allUpcomingPasses by remember(location) { mutableStateOf<List<Pair<SatelliteItem, ISSEngine.ISSPass>>?>(null) }
    LaunchedEffect(location, passWindowKey, starlinkCacheVersion, tleRefreshTick) {
        val (shortlist, sorted) = withContext(Dispatchers.Default) {
            val tles = tleRepo.getStarlinkTles()
            val trains = StarlinkTrainManager.detectActiveTrains(tles, realTimeNowMs)
            val individuals = StarlinkTrainManager.selectTopVisibleStarlinks(tles, trains, location.latitude, location.longitude,
                realTimeNowMs, maxCount = StarlinkTrainManager.DEFAULT_TOP_VISIBLE_PASS_LIMIT)
            val targets = SatelliteCatalog.satellites + trains.map { it.leaderItem } + individuals
            val passes = targets.distinctBy { it.id }.flatMap { sat ->
                ensureActive()
                ISSEngine.predictPasses(location.latitude, location.longitude, passWindowKey,
                    tle = SatelliteEngine.getEffectiveTle(sat), scanDays = if (sat.isTrain) 3 else 5,
                    visibleOnly = true, standardMag = sat.standardMagnitude).map { sat to it }
            }.sortedBy { it.second.startTimeMs }
            individuals to passes
        }
        topPassStarlinks = shortlist
        allUpcomingPasses = sorted
    }

    var alarms by remember { mutableStateOf(AstroNotificationStore.getAll(context).filter { it.objectType == "SATELLITE" }) }
    DisposableEffect(context) {
        val unsubscribe = AstroNotificationStore.observe(context) {
            alarms = AstroNotificationStore.getAll(context).filter { it.objectType == "SATELLITE" }
        }
        onDispose { unsubscribe() }
    }
    fun saveAlerts(new: SatAlertsConfig) {
        val persist = {
            AstroNotificationManager.configureSatelliteMonitoring(context, new.enabled, location, new.satelliteIds, new.leadMinutes)
            config = new
            screen.dismiss()
        }
        if (new.enabled) withNotificationPermission(persist) else persist()
    }
    var alertPredictions by remember(location) { mutableStateOf<List<Pair<SatelliteItem, ISSEngine.ISSPass>>?>(null) }
    val showAlerts = state.activePanel == SatPanel.Alerts
    LaunchedEffect(showAlerts, location, tleRefreshTick, starlinkCacheVersion) {
        if (showAlerts) {
            alertPredictions = withContext(Dispatchers.Default) {
                val ids = (SatelliteCatalog.satellites.map { it.id } + "starlink_train").toSet()
                AstroNotificationManager.predictRollingPasses(context, location, ids, realTimeNowMs)
            }
        }
    }
    val basePasses = remember(allUpcomingPasses, alarms, config, isFa, nights) {
        allUpcomingPasses?.map { (sat, pass) ->
            val matching = alarms.filter { it.triggerTimeMs > realTimeNowMs && it.coversSatellitePass(sat, pass.startTimeMs) }
            SatPassUi(passKey(sat, pass), sat, shortNameOf(sat, isFa), pass, nights.key(pass.maxTimeMs),
                matching.firstOrNull { it.manualPass }?.leadMinutes, config.enabled && matching.any { !it.manualPass })
        }
    }
    val windowPasses = remember(basePasses, currentSimulationMs) { basePasses?.filter { row ->
        row.pass.endTimeMs > currentSimulationMs && row.pass.startTimeMs <= currentSimulationMs + (if (row.isTrain) 2L else 3L) * DAY
    } }
    val scopedPasses = remember(windowPasses, selectedCategory) { windowPasses?.filter { matchesScope(it.sat, selectedCategory) } }
    val currentNight = remember(nights, currentSimulationMs / MINUTE) { nights.key(currentSimulationMs) }
    val passesByNight = remember(scopedPasses, currentNight, isFa, tz) {
        SatNights.group(scopedPasses.orEmpty(), currentSimulationMs, tz, isFa, referenceNight = currentNight)
    }
    val spotlight = remember(scopedPasses, currentSimulationMs, isFa, tz, timeOffsetMinutes) {
        SatNights.spotlight(scopedPasses.orEmpty(), currentSimulationMs, tz, isFa, timeOffsetMinutes != 0)
    }
    val markers = remember(basePasses, selectedCategory, realTimeNowMs / MINUTE) { basePasses.orEmpty().asSequence()
        .filter { matchesScope(it.sat, selectedCategory) }
        .map { SatTimeMarker(((it.pass.maxTimeMs - realTimeNowMs) / 60_000.0).roundToInt(), it.quality) }
        .filter { it.minute in -720..720 }.distinctBy { it.minute }.toList() }
    val fleet = remember(liveSatelliteStates, selectedSatelliteId, isFa) { liveSatelliteStates.map { live ->
        SatTokenUi(live.satellite.id, shortNameOf(live.satellite, isFa), fullNameOf(live.satellite, isFa), live.satellite.isTrain,
            live.satellite.trainCount, live.topocentric.elevationDeg, live.satellite.isNakedEyeCandidate,
            live.satellite.id == selectedSatelliteId, live.satellite)
    } }
    val nextFocused = windowPasses?.firstOrNull { it.sat.id == selectedLiveState?.satellite?.id }

    var activeDetail by remember { mutableStateOf<SatDetailEntry?>(null) }
    var detailSource by remember { mutableStateOf("focus") }
    var reminderTarget by remember { mutableStateOf<SatReminderTarget?>(null) }
    var feedback by remember { mutableStateOf<SatFeedback?>(null) }
    LaunchedEffect(state.activePanel) { if (state.activePanel != SatPanel.Passes) reminderTarget = null }
    LaunchedEffect(feedback) { if (feedback != null) { delay(4500); feedback = null } }
    val removedMessage = stringResource(R.string.sat_reminder_removed)
    val scheduledMessage = stringResource(R.string.sat_reminder_set, "%s")
    fun removeReminder(sat: SatelliteItem, pass: ISSEngine.ISSPass) {
        val removed = alarms.firstOrNull { it.manualPass && it.coversSatellitePass(sat, pass.startTimeMs) } ?: return
        AstroNotificationManager.cancelSpecificPassAlarm(context, sat, pass.startTimeMs)
        if (config.enabled) AstroNotificationManager.requestRollingPassRefresh(context)
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        feedback = SatFeedback(removedMessage) {
            AstroNotificationManager.scheduleSpecificPassAlarm(context, sat, pass, location, removed.leadMinutes)
            feedback = null
        }
    }
    fun setReminder(sat: SatelliteItem, pass: ISSEngine.ISSPass, lead: Int) {
        withNotificationPermission {
            val prior = alarms.firstOrNull { it.manualPass && it.coversSatellitePass(sat, pass.startTimeMs) }
            if (AstroNotificationManager.scheduleSpecificPassAlarm(context, sat, pass, location, lead)) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                val at = SatFmt.clock(pass.startTimeMs - lead * MINUTE, isFa, tz)
                feedback = SatFeedback(scheduledMessage.replace("%s", at)) {
                    AstroNotificationManager.cancelSpecificPassAlarm(context, sat, pass.startTimeMs)
                    if (prior != null) AstroNotificationManager.scheduleSpecificPassAlarm(context, sat, pass, location, prior.leadMinutes)
                    if (prior == null) AstroNotificationManager.requestRollingPassRefresh(context)
                    feedback = null
                }
            }
        }
    }
    fun detail(sat: SatelliteItem, pass: ISSEngine.ISSPass? = null, source: String = "focus") {
        detailSource = source
        activeDetail = SatDetailEntry(sat, pass)
    }
    fun showOnAr(sat: SatelliteItem) {
        activeDetail = null
        screen.dismiss()
        viewModel.locateObjectInAR(ArScreenSupport.targetForSatellite(sat))
    }
    fun passActions(pass: SatPassUi) = SatPassActions(
        onOpenDetail = { detail(pass.sat, pass.pass, pass.key) },
        onShowOnGlobe = { focusItem(pass.sat) },
        onAr = { showOnAr(pass.sat) },
        onRemind = { bounds -> reminderTarget = SatReminderTarget(pass, bounds) },
    )
    fun closePanel() {
        reminderTarget = null
        anchorId = null
        screen.dismiss()
    }
    val activeModal = state.activePanel in setOf(SatPanel.Fleet, SatPanel.Passes, SatPanel.Alerts, SatPanel.Guide)
    SideEffect { viewModel.setSatellitesChromeOccluded(uiState.selectedTab == 1 && (activeModal || activeDetail != null)) }
    DisposableEffect(viewModel) { onDispose { viewModel.setSatellitesChromeOccluded(false) } }
    BackHandler(uiState.selectedTab == 1 && activeDetail == null && (state.activePanel != SatPanel.None || anchorId != null)) { closePanel() }
    val returnLive = { setTime(0, animate = true) }
    val timeActions = SatTimeActions({ setTime(it) }, returnLive,
        { nextSatelliteTime(markers, screen.ui.timeOffsetMin)?.let { setTime(it, animate = true) } },
        { delta -> setTime(screen.ui.timeOffsetMin + delta) })

    CompositionLocalProvider(LocalSatSkin provides skin, LocalZigSatTokens provides tokens, LocalSatReduceMotion provides reduce) {
        Box(Modifier.fillMaxSize().background(tokens.canvas)) {
            SharedTransitionLayout(Modifier.fillMaxSize()) {
                val shared = this
                val duration = if (reduce) 120 else 320
                AnimatedContent(targetState = activeDetail, modifier = Modifier.fillMaxSize(),
                    transitionSpec = { fadeIn(tween(duration)) togetherWith fadeOut(tween(duration)) }, label = "satellite-detail") { entry ->
                    val visibility = this
                    val sourceModifier: @Composable (String) -> Modifier = { key ->
                        if (reduce || key != detailSource) Modifier else with(shared) {
                            Modifier.sharedBounds(rememberSharedContentState("sat-detail-$key"), visibility,
                                boundsTransform = { _, _ -> tween(320) })
                        }
                    }
                    if (entry != null) {
                        SatelliteDetailScreen(
                            satelliteItem = entry.satellite, userLocation = location, language = uiState.language,
                            onBack = { activeDetail = null }, simulationTimestampMs = currentSimulationMs,
                            skin = skin, themeMode = uiState.themeMode, preselectedPass = entry.pass,
                            reminderAlarms = alarms, isSimulated = timeOffsetMinutes != 0,
                            onRemoveReminder = ::removeReminder, onSetReminder = ::setReminder,
                            onShowOnGlobe = { satellite -> activeDetail = null; focusItem(satellite) },
                            onSpotInAr = ::showOnAr,
                            modifier = sourceModifier(detailSource),
                        )
                    } else {
                        SatelliteHud(
                            state = state, status = status, tokens = tokens, config = config, focused = selectedLiveState,
                            nextFocused = nextFocused, now = realTimeNowMs, tz = tz, markers = markers, centered = centered,
                            firstRun = firstRun, timeActions = timeActions, onPanel = { panel ->
                                reminderTarget = null
                                anchorId = null
                                screen.toggle(panel)
                                if (panel == SatPanel.Guide) firstRun = false
                            }, onClose = ::closePanel, onLive = returnLive,
                            onLocate = {
                                closePanel()
                                earthFocusTarget = EarthFocusTarget(location.latitude, location.longitude,
                                    SATELLITE_EARTH_DEFAULT_DISTANCE, ++focusSequence, locate = true)
                            }, onCycle = ::cycle, onDetail = { selectedLiveState?.satellite?.let { detail(it) } },
                            onLighting = { theme ->
                                screen.lighting(theme)
                                prefs.edit().putString("satellite_map_theme", theme.name).apply()
                                legacy.edit().putString("satellite_map_theme", theme.name).apply()
                            }, onLegend = { screen.legend(it); screen.dismiss() },
                            focusModifier = sourceModifier("focus"),
                            globe = { framing, top, bottom ->
                                SatelliteEarth3D(
                                    currentSimulationMs = currentSimulationMs, userLocation = location, isFa = isFa,
                                    mapTheme = mapTheme, appThemeMode = uiState.themeMode, selectedSatelliteId = selectedSatelliteId,
                                    selectedSatItem = selectedSatItem, liveSatelliteStates = globeFleet,
                                    mapOverheadStarlinkStates = mapOverheadStarlinkStates, trainPearlStatesByTrain = trainPearlStatesByTrain,
                                    allInteractiveMapStates = allInteractiveMapStates,
                                    anchoredSatLabelState = allInteractiveMapStates.firstOrNull { it.satellite.id == anchorId },
                                    overlayStyle = earthOverlayStyle(tokens, uiState.themeMode), focusTarget = earthFocusTarget,
                                    onFocusConsumed = { earthFocusTarget = null }, onSelectSatellite = ::focusSatellite,
                                    onDismissAnchoredLabel = ::closePanel,
                                    onOpenSatelliteDetail = { detail(it, source = "marker") }, modifier = Modifier.fillMaxSize(),
                                    framing = framing, hudTop = top, hudBottom = bottom, controller = camera, tokens = tokens,
                                    reduceMotion = reduce, onGlobeGesture = { screen.legend(false) }, onCenteredChanged = { centered = it },
                                    popoverModifier = sourceModifier("marker"),
                                )
                            }, sheet = { panel, side ->
                                when (panel) {
                                    SatPanel.Fleet -> SatFleetSheet(fleet, selectedCategory, tokens, isFa, ::setSatelliteScope,
                                        { focusItem(it.satellite) }, ::closePanel)
                                    SatPanel.Passes -> SatPassesSheet(tokens, isFa, selectedCategory, scopedPasses?.size, passesByNight,
                                        spotlight, currentSimulationMs, realTimeNowMs, side, ::closePanel, { setSatelliteScope(SatelliteCategory.ALL) },
                                        { selectedLiveState?.satellite?.let { detail(it) } }, ::passActions, sourceModifier, tz, isOnline)
                                    SatPanel.Alerts -> SatAlertsSheet(tokens, isFa, config, { ids, lead ->
                                        alertPredictions.orEmpty().firstOrNull { (sat, pass) -> monitored(sat, ids) &&
                                            pass.startTimeMs - lead * MINUTE > realTimeNowMs &&
                                            alarms.none { it.manualPass && it.coversSatellitePass(sat, pass.startTimeMs) } }
                                            ?.let { (sat, pass) -> "${shortNameOf(sat, isFa)} · " + SatFmt.dateTime(pass.startTimeMs - lead * MINUTE, isFa, tz) }
                                    }, ::closePanel, ::saveAlerts)
                                    SatPanel.Guide -> SatGuideSheet(tokens, isFa, ::closePanel)
                                    else -> Unit
                                }
                            },
                        )
                    }
                }
            }
            val pending = reminderTarget
            if (pending != null && activeDetail == null) {
                val manual = alarms.firstOrNull { it.manualPass && it.coversSatellitePass(pending.pass.sat, pending.pass.pass.startTimeMs) }
                SatReminderPopover(tokens, isFa, pending, manual?.leadMinutes, realTimeNowMs, tz,
                    onChoose = { lead -> setReminder(pending.pass.sat, pending.pass.pass, lead); reminderTarget = null },
                    onRemove = manual?.let { { removeReminder(pending.pass.sat, pending.pass.pass); reminderTarget = null } },
                    onClose = { reminderTarget = null })
            }
            AnimatedVisibility(feedback != null, Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = if (activeModal || activeDetail != null) 12.dp else 88.dp),
                enter = fadeIn(tween(120)), exit = fadeOut(tween(120))) {
                feedback?.let { SatSnackbar(it.label, tokens, it.undo) }
            }
        }
    }
}

/** Reusable, non-scrolling UI. Previews substitute only the native globe viewport and live inputs. */
@Composable
internal fun SatelliteHud(
    state: SatellitesUiState, status: SatStatusData, tokens: ZigSatTokens, config: SatAlertsConfig,
    focused: SatelliteLiveState?, nextFocused: SatPassUi?, now: Long, tz: TimeZone, markers: List<SatTimeMarker>,
    centered: Boolean, firstRun: Boolean, timeActions: SatTimeActions, onPanel: (SatPanel) -> Unit, onClose: () -> Unit,
    onLive: () -> Unit, onLocate: () -> Unit, onCycle: (Int) -> Unit, onDetail: () -> Unit,
    onLighting: (MapTheme) -> Unit, onLegend: (Boolean) -> Unit, focusModifier: Modifier = Modifier,
    globe: @Composable (EarthViewportProjection, Dp, Dp) -> Unit, sheet: @Composable (SatPanel, Boolean) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds().testTag("satellite_screen")) {
        val reduce = LocalSatReduceMotion.current
        val density = LocalDensity.current
        val width = maxWidth
        val height = maxHeight
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val top = topInset + if (state.legend) 292.dp else 118.dp
        val navClearance = bottomInset + 84.dp
        val compact = width < 360.dp || height - topInset - bottomInset < 700.dp
        val padding = if (width >= 412.dp) 20.dp else 16.dp
        var focusHeight by remember { mutableStateOf(126.dp) }
        val bottom = navClearance + focusHeight + 3.dp
        val framing = with(density) { satelliteViewport(width.toPx(), height.toPx(), top.toPx(), (height - bottom).toPx()) }
        val isFa = status.isFa
        val panel = state.activePanel
        val simulated = state.timeOffsetMin != 0
        val modal = panel == SatPanel.Fleet || panel == SatPanel.Passes || panel == SatPanel.Alerts || panel == SatPanel.Guide
        // Sheets block the globe and its HUD for touch *and* for accessibility. Time and Layers don't.
        Box(Modifier.matchParentSize().then(if (modal) Modifier.clearAndSetSemantics {} else Modifier)) {
            SatelliteStarfield(tokens)
            globe(framing, top, bottom)
            if (simulated) Box(Modifier.matchParentSize().border(2.dp, tokens.sim.copy(alpha = 0.35f)))

            Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(start = padding, end = padding, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                SatStatusPill(status, tokens, { onPanel(SatPanel.DataStatus) }, onLive)
                Spacer(Modifier.weight(1f))
                val alerts = if (config.enabled) stringResource(R.string.sat_alerts_tooltip, satLeadLabel(config.leadMinutes, isFa),
                    SatFmt.int(config.satelliteIds.size, isFa)) else stringResource(R.string.sat_alerts)
                ZigSatIconButton(Icons.Filled.NotificationsNone, alerts, tokens, { onPanel(SatPanel.Alerts) },
                    dot = if (config.enabled) tokens.live else null, hud = true, tooltip = alerts, testTag = "sat_alerts")
                ZigSatIconButton(Icons.Filled.HelpOutline, stringResource(R.string.sat_help), tokens, { onPanel(SatPanel.Guide) },
                    hud = true, dot = if (firstRun) tokens.live else null, pulseDot = firstRun, testTag = "sat_guide")
            }
            Row(Modifier.align(Alignment.TopEnd).padding(top = topInset + 64.dp, end = padding),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                ZigSatIconButton(Icons.Filled.SatelliteAlt, stringResource(R.string.sat_fleet), tokens, { onPanel(SatPanel.Fleet) },
                    visualSize = 44.dp, hud = true, testTag = "sat_fleet")
                ZigSatIconButton(Icons.Filled.Schedule, stringResource(R.string.sat_time), tokens, { onPanel(SatPanel.Time) },
                    visualSize = 44.dp, dot = if (simulated) tokens.sim else null, hud = true, active = panel == SatPanel.Time, testTag = "sat_time")
                ZigSatIconButton(Icons.Filled.Layers, stringResource(R.string.sat_layers), tokens, { onPanel(SatPanel.Layers) },
                    visualSize = 44.dp, hud = true, active = panel == SatPanel.Layers, testTag = "sat_layers")
                ZigSatIconButton(Icons.Filled.CenterFocusWeak, stringResource(R.string.sat_locate), tokens, onLocate,
                    visualSize = 44.dp, hud = true, enabled = !centered, testTag = "map_reset_view")
            }
            Box(Modifier.align(Alignment.BottomCenter).padding(start = padding,
                end = padding, bottom = navClearance)) {
                AnimatedContent(targetState = panel == SatPanel.Time, transitionSpec = {
                    if (reduce) fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                    else if (targetState) (slideInVertically(tween(240)) { it / 4 } + fadeIn(tween(210))) togetherWith fadeOut(tween(160))
                    else fadeIn(tween(160)) togetherWith (slideOutVertically(tween(230)) { it / 4 } + fadeOut(tween(180)))
                }, label = "hud-bottom") { showTime ->
                    if (showTime) SatTimeTray(tokens, state.timeOffsetMin, now, tz, markers, isFa, compact, timeActions, onClose)
                    else if (focused != null) {
                        val span = stringResource(if (focused.satellite.isTrain) R.string.sat_two_days else R.string.sat_three_days)
                        Crossfade(focused.satellite.id, focusModifier, animationSpec = tween(if (reduce) 120 else 160), label = "satellite-focus") { id ->
                            var presented by remember(id) { mutableStateOf(focused) }
                            SideEffect { if (id == focused.satellite.id) presented = focused }
                            SatFocusCard(presented, tokens, isFa, compact, width >= 412.dp, simulated, !status.isOnline, nextFocused,
                                now + state.timeOffsetMin * MINUTE, span, firstRun, { onPanel(SatPanel.Fleet) }, onDetail,
                                { onPanel(SatPanel.Passes) }, onCycle,
                                Modifier.onSizeChanged { focusHeight = with(density) { it.height.toDp() } })
                        }
                    }
                }
            }
            if (state.legend && panel == SatPanel.None) SatLegendPanel(tokens, isFa,
                Modifier.align(Alignment.TopStart).padding(start = padding, top = topInset + 112.dp).width(172.dp))

            when (panel) {
                SatPanel.DataStatus -> SatDataStatusPopover(status, tokens, tz, onClose, { onLive(); onClose() },
                    Modifier.align(Alignment.TopStart).padding(start = padding, end = padding, top = top))
                SatPanel.Layers -> SatLayersPopover(tokens, state.mapTheme, state.legend, onLighting, onLegend,
                    Modifier.align(Alignment.TopEnd).padding(end = padding, top = top.coerceAtMost((height - bottom - 106.dp).coerceAtLeast(top))))
                else -> Unit
            }
        }
        if (modal) sheet(panel, width >= 600.dp)
    }
}

@Composable
private fun SatelliteStarfield(tokens: ZigSatTokens) {
    // No ornamental star texture: the globe is the focal surface, not a backdrop for decoration.
    Canvas(Modifier.fillMaxSize()) { drawRect(tokens.canvas) }
}

internal fun matchesScope(sat: SatelliteItem, scope: SatelliteCategory): Boolean = when (scope) {
    SatelliteCategory.ALL -> true
    SatelliteCategory.ISS -> sat.category == SatelliteCategory.ISS
    SatelliteCategory.STARLINK -> sat.category == SatelliteCategory.STARLINK || sat.isTrain
    SatelliteCategory.HUBBLE -> sat.category == SatelliteCategory.HUBBLE
    SatelliteCategory.VISIBLE -> sat.isNakedEyeCandidate
    SatelliteCategory.TIANGONG -> sat.category == SatelliteCategory.TIANGONG
    SatelliteCategory.EARTH_OBSERVATION -> sat.category == SatelliteCategory.EARTH_OBSERVATION
}

internal fun monitored(sat: SatelliteItem, ids: Set<String>): Boolean = sat.id in ids || sat.isTrain && "starlink_train" in ids
internal fun passKey(sat: SatelliteItem, pass: ISSEngine.ISSPass): String = "${sat.id}_${pass.startTimeMs}"
internal fun fullNameOf(sat: SatelliteItem, isFa: Boolean) = SatFmt.digits(if (isFa) sat.nameFa else sat.nameEn, isFa)
internal fun shortNameOf(sat: SatelliteItem, isFa: Boolean): String = when {
    sat.isTrain -> if (isFa) "قطار استارلینک" else "Starlink train"
    sat.id == "iss_zarya" -> if (isFa) "ایستگاه فضایی" else "ISS"
    else -> fullNameOf(sat, isFa).substringBefore(" (").trim()
}

private fun earthOverlayStyle(tokens: ZigSatTokens, themeMode: ThemeMode): SatelliteEarthOverlayStyle = SatelliteEarthOverlayStyle(
    gridColor = (if (themeMode != ThemeMode.LIGHT) Color.White else tokens.selected).copy(alpha = .12f),
    equatorColor = tokens.starlink.copy(alpha = .32f), limbRimColor = tokens.stars.copy(alpha = .32f),
    cityLightAccent = tokens.sim.copy(alpha = .52f), userPinColor = tokens.starlink, userLabelColor = Color.White,
    orbitTrackColor = tokens.live.copy(alpha = .92f), orbitGlowColor = tokens.live.copy(alpha = .24f),
    selectedMarkerColor = tokens.selected, selectedGlowColor = tokens.selectedHalo.copy(alpha = .7f), starlinkColor = tokens.starlink,
    trainChainColor = tokens.starlink.copy(alpha = .54f), trainLeaderColor = Color.White, trainMemberColor = tokens.starlink.copy(alpha = .75f),
    nakedEyeMarkerColor = tokens.nakedEye, defaultMarkerColor = tokens.telescopic,
    popoverContainerColor = tokens.popoverFill, popoverBorderColor = tokens.popoverBorder,
    popoverShape = squircleShape(SatRadius.popover),
)
