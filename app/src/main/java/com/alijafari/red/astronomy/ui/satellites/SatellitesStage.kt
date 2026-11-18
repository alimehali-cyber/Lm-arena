package com.alijafari.red.astronomy.ui.satellites

import android.content.Context
import com.alijafari.red.astronomy.ui.satellites.core.*
import kotlinx.coroutines.flow.collect
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
import com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog
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
import com.alijafari.red.astronomy.ui.screens.MapTheme
import com.alijafari.red.astronomy.ui.screens.SatelliteDetailScreen
import com.alijafari.red.astronomy.ui.screens.ar.ArScreenSupport
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
internal fun SatellitesStage(uiState: MainUiState, viewModel: MainViewModel, skin: SatSkin, clock: Clock = Clock.System) {
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
        SatellitesState(SatellitesUiState(mapTheme = MapTheme.entries.firstOrNull { it.name == name } ?: MapTheme.AUTO))
    }
    val state = screen.ui
    val mapTheme = state.mapTheme
    val lib by context.satelliteLibrary.data.collectAsState(initial=Library())
    val update: LibraryUpdate = { change -> scope.launch { context.satelliteLibrary.updateData(change) }; Unit }
    // Permission alone is not a fix, and a manually chosen city needs no GPS permission.
    val hasLocation = (prefs.contains("user_latitude") && prefs.contains("user_longitude")) ||
        (prefs.contains("user_lat") && prefs.contains("user_lon"))
    val selectedSatelliteId = state.focusId
    val timeOffsetMinutes = state.timeOffsetMin
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
    var firstRun by remember(prefs) { mutableStateOf(!prefs.getBoolean("satellite_coach_v2",false)) }
    fun finishCoach() { firstRun=false;prefs.edit().putBoolean("satellite_coach_v2",true).apply() }
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

    var realTimeNowMs by remember { mutableLongStateOf(clock.nowMs()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L - clock.nowMs() % 1000L)
            realTimeNowMs = clock.nowMs()
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
    val trainMemberIds = remember(activeTrains) { activeTrains.map { train -> train.memberItems.map { it.id } } }
    // Build static search keys once per catalogue revision, never per tick or keystroke.
    val starItems by produceState(emptyList<SatelliteItem>(),starlinkCacheVersion) {
        value=withContext(Dispatchers.Default) { StarlinkTrainManager.getParsedOrbitals(tleRepo.getStarlinkTles()).map(StarlinkTrainManager::buildIndividualStarlinkItem) }
    }
    val repo by produceState(remember { CatalogueRepository(SatelliteCatalog.satellites) },starItems,activeTrains) {
        value=withContext(Dispatchers.Default){ CatalogueRepository(SatelliteCatalog.satellites+activeTrains.map{it.leaderItem}+starItems) }
    }
    // Full-catalogue horizon and brightness samples are amortised; only the budgeted camera fleet runs at 1 Hz.
    val snapshot by produceState(emptyMap<String,Pair<Double,Double>>(),repo,location,currentSimulationMs/60_000,tleRefreshTick) {
        val time=currentSimulationMs
        value=withContext(Dispatchers.Default){repo.rows.associate { r->ensureActive()
            val live=SatelliteEngine.calculateSatelliteState(r.item,time,location.latitude,location.longitude,location.elevationMeters)
            r.item.id to (live.topocentric.elevationDeg to live.apparentMagnitude) }}
    }
    val angles=remember(snapshot){snapshot.mapValues{it.value.first}}; val mags=remember(snapshot){snapshot.mapValues{it.value.second}}
    val scoped=remember(repo,lib,angles){repo.rows.filter {lib.accepts(lib.active,it.craft,(angles[it.item.id] ?: -90.0)>0)}}
    val members=remember(scoped,lib.active){ if(lib.active==Library.ALL)null else scoped.mapTo(HashSet()){it.item.id} }
    var retainedFocus by remember { mutableStateOf<SatelliteItem?>(null) }
    val selectedSatItem=remember(selectedSatelliteId,repo,retainedFocus){repo.rows.firstOrNull{it.item.id==selectedSatelliteId}?.item
        ?: retainedFocus?.takeIf{it.id==selectedSatelliteId} ?: SatelliteCatalog.getById(selectedSatelliteId,detectedTrain)}
    fun propagate(sat:SatelliteItem)=SatelliteEngine.calculateSatelliteState(sat,currentSimulationMs,location.latitude,location.longitude,location.elevationMeters)
    val visibleSatellites=remember(repo,members,lib.favorites,selectedSatItem,activeTrains,overheadStarlinks,topPassStarlinks,angles,state.layers){
        val pearls=if(MapLayer.TRAIN in state.layers)activeTrains.flatMap{it.memberItems} else emptyList()
        (listOf(selectedSatItem)+SatelliteCatalog.satellites+activeTrains.map{it.leaderItem}+overheadStarlinks+pearls+topPassStarlinks+repo.rows.map{it.item})
            .distinctBy{it.id}.sortedWith(compareBy<SatelliteItem>{ when {it.id==selectedSatelliteId->0;it.id in lib.favorites || members?.contains(it.id)==true->1;
                it.isNakedEyeCandidate && (angles[it.id] ?: -90.0)>0->2;else->3} }.thenByDescending{angles[it.id] ?: -90.0}).take(60)
    }
    val batch by produceState(0L to emptyList<SatelliteLiveState>(),visibleSatellites,currentSimulationMs,location,tleRefreshTick) {
        val time=currentSimulationMs
        value=time to withContext(Dispatchers.Default){visibleSatellites.map { ensureActive();propagate(it) }}
    }
    val allInteractiveMapStates=batch.second
    val selectedLiveState=allInteractiveMapStates.firstOrNull { it.satellite.id==selectedSatelliteId }
    var activeArc by remember { mutableStateOf<SatPassUi?>(null) }
    var hushJob by remember { mutableStateOf<Job?>(null) }
    var focusSequence by remember { mutableLongStateOf(0L) }
    var earthFocusTarget by remember { mutableStateOf<EarthFocusTarget?>(null) }
    var centered by remember { mutableStateOf(true) }
    var showSelectedLabel by remember { mutableStateOf(true) }
    val camera = rememberEarthCameraController(location.latitude, location.longitude, SATELLITE_EARTH_DEFAULT_DISTANCE)
    fun focusSatellite(live: SatelliteLiveState) {
        showSelectedLabel = true
        screen.focus(live.satellite.id)
        retainedFocus = live.satellite
        if(activeArc?.sat?.id!=live.satellite.id) activeArc=null
        earthFocusTarget = EarthFocusTarget(live.topocentric.subLatDeg, live.topocentric.subLonDeg,
            SATELLITE_EARTH_DEFAULT_DISTANCE * .974f, ++focusSequence)
        if(firstRun)finishCoach()
    }
    fun focusItem(satellite: SatelliteItem) = focusSatellite(allInteractiveMapStates.firstOrNull { it.satellite.id == satellite.id } ?: propagate(satellite))
    fun cycle(delta: Int) {
        if(scoped.isEmpty()) return
        val index=scoped.indexOfFirst{it.item.id==screen.ui.focusId}
        val next=if(index<0) (if(delta>0)0 else scoped.lastIndex) else (index+delta+scoped.size)%scoped.size
        focusItem(scoped[next].item)
    }
    LaunchedEffect(uiState.selectedSatelliteId, uiState.selectedTab, repo, detectedTrain) {
        if (uiState.selectedTab != 1) return@LaunchedEffect
        val id = uiState.selectedSatelliteId ?: return@LaunchedEffect
        // Catalogue.getById intentionally falls back to ISS for unknown IDs. A notification must
        // not silently focus ISS while a dynamic train or satellite has not loaded yet.
        val canonical = CanonicalAstroCatalog.getCanonicalObject(CanonicalAstroCatalog.resolveCanonicalId(id))
        val norad = canonical?.scientificIdentifiers?.noradId ?: id.removePrefix("sat_").toIntOrNull()
        val known = if (id == "starlink_train") detectedTrain else
            repo.rows.firstOrNull { it.item.id == id || (norad != null && it.item.noradId == norad) }?.item
                ?: SatelliteCatalog.getDynamicByIdOrNorad(id)
        if (known != null) {
            focusItem(known)
            viewModel.selectSatelliteById(null)
        }
    }

    val meta = selectedLiveState?.tleMetadata
    val epoch = remember(meta?.tleData) {
        meta?.tleData?.takeIf { it.epochDay >= 1.0 && it.epochDay < 367.0 }
            ?.let { SGP4Propagator().tleEpochToMs(it.epochYear, it.epochDay) }
    }
    val tleAge = epoch?.let { (realTimeNowMs - it) / DAY.toDouble() } ?: meta?.epochAgeDays
    val cachedAge = remember(starlinkCacheVersion, realTimeNowMs / MINUTE) { tleRepo.getStarlinkCacheAgeDays(realTimeNowMs) }
    val trainSelected = selectedLiveState?.satellite?.isTrain == true
    // Keep train drift transparency in addition to the standard age bucket.
    val trainDrift = trainSelected && formatTleStalenessBadge(trainSelected, meta, cachedAge, isFa) != null
    val statusAge = if (trainDrift) maxOf(tleAge ?: 0.0, cachedAge ?: 0.0) else tleAge
    val status = SatStatusData(timeOffsetMinutes == 0, timeOffsetMinutes, isOnline, statusAge,
        epoch, meta?.source, isFa)

    // Cache a superset of the ±12h scrub window and the three-day feed. Scrubbing only changes
    // the filtering and live propagation, never launches a fresh multi-day search per slider tick.
    val passWindowKey = realTimeNowMs / (15 * MINUTE) * (15 * MINUTE) - 12 * 60 * MINUTE
    var allUpcomingPasses by remember(location) { mutableStateOf<List<Pair<SatelliteItem, ISSEngine.ISSPass>>?>(null) }
    LaunchedEffect(location, passWindowKey, starlinkCacheVersion, tleRefreshTick, state.windowDays, selectedSatItem.id) {
        val (shortlist, sorted) = withContext(Dispatchers.Default) {
            val tles = tleRepo.getStarlinkTles()
            val trains = StarlinkTrainManager.detectActiveTrains(tles, realTimeNowMs)
            val individuals = StarlinkTrainManager.selectTopVisibleStarlinks(tles, trains, location.latitude, location.longitude,
                realTimeNowMs, maxCount = StarlinkTrainManager.DEFAULT_TOP_VISIBLE_PASS_LIMIT)
            val targets = SatelliteCatalog.satellites + trains.map { it.leaderItem } + individuals + selectedSatItem
            val passes = targets.distinctBy { it.id }.flatMap { sat ->
                ensureActive()
                ISSEngine.predictPasses(location.latitude, location.longitude, passWindowKey,
                    tle = SatelliteEngine.getEffectiveTle(sat), scanDays = state.windowDays + 2,
                    visibleOnly = false, standardMag = sat.standardMagnitude).map { sat to it }
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
            screen.sub(SatPanel.Passes)
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
    val basePasses = remember(allUpcomingPasses, alarms, config, isFa, nights, realTimeNowMs / MINUTE) {
        allUpcomingPasses?.map { (sat, pass) ->
            val matching = alarms.filter { it.triggerTimeMs > realTimeNowMs && it.coversSatellitePass(sat, pass.startTimeMs) }
            SatPassUi(passKey(sat, pass), sat, shortNameOf(sat, isFa), pass, nights.key(pass.maxTimeMs),
                matching.firstOrNull { it.manualPass }?.leadMinutes, config.enabled && matching.any { !it.manualPass })
        }
    }
    val windowPasses = remember(basePasses, currentSimulationMs, state.windowDays) { basePasses?.filter { row ->
        row.pass.endTimeMs > currentSimulationMs && row.pass.startTimeMs <= currentSimulationMs + state.windowDays * DAY
    } }
    val allScoped=remember(windowPasses,members){windowPasses?.filter {members==null || it.sat.id in members}}
    val scopedPasses=remember(allScoped,state.visibleOnly){allScoped?.filter {
        !state.visibleOnly || (it.pass.isVisible && it.quality.rank<=SatQuality.GOOD.rank)
    }}
    val currentNight = remember(nights, currentSimulationMs / MINUTE) { nights.key(currentSimulationMs) }
    val passesByNight = remember(scopedPasses, currentNight, isFa, tz) {
        SatNights.group(scopedPasses.orEmpty(), currentSimulationMs, tz, isFa, referenceNight = currentNight)
    }
    val spotlight = remember(scopedPasses, currentSimulationMs, isFa, tz, timeOffsetMinutes) {
        SatNights.spotlight(scopedPasses.orEmpty().filter { it.pass.isVisible }, currentSimulationMs, tz, isFa, timeOffsetMinutes != 0)
    }
    val markers = remember(basePasses, members, realTimeNowMs / MINUTE) { basePasses.orEmpty().asSequence()
        .filter { members==null || it.sat.id in members }
        .map { SatTimeMarker(((it.pass.maxTimeMs - realTimeNowMs) / 60_000.0).roundToInt(), it.quality) }
        .filter { it.minute in -720..720 }.distinctBy { it.minute }.toList() }
    val nextFocused = windowPasses?.firstOrNull { it.sat.id == selectedSatelliteId && it.pass.isVisible && it.quality.rank<=SatQuality.GOOD.rank }
    val nextTimes=remember(basePasses,currentSimulationMs/MINUTE){basePasses.orEmpty().filter{it.pass.endTimeMs>currentSimulationMs}.groupBy{it.sat.id}.mapValues{it.value.first().pass.startTimeMs}}
    var debounced by remember {mutableStateOf("")}
    LaunchedEffect(state.query){ if(state.query.isNotEmpty())delay(150);debounced=state.query }
    val results by remember(repo,debounced,lib,angles,nextTimes,isFa,mags){repo.rows(debounced,lib,angles,nextTimes,isFa,mags)}.collectAsState(emptyList())
    val rising=remember(selectedLiveState,batch.first){ selectedLiveState?.let { s->
        val next=ISSEngine.calculateTopocentricPos(batch.first+10_000,location.latitude,location.longitude,location.elevationMeters,tle=SatelliteEngine.getEffectiveTle(s.satellite))
        next.elevationDeg>s.topocentric.elevationDeg } }

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
        onShowOnGlobe = { activeArc=pass;screen.dismiss();focusItem(pass.sat) },
        onAr = { showOnAr(pass.sat) },
        onRemind = { bounds -> reminderTarget = SatReminderTarget(pass, bounds) },
    )
    val activeModal = state.activePanel.sheet
    SideEffect { viewModel.setSatellitesChromeOccluded(uiState.selectedTab == 1 && (activeModal || activeDetail != null)) }
    DisposableEffect(viewModel) { onDispose { viewModel.setSatellitesChromeOccluded(false) } }
    BackHandler(uiState.selectedTab==1 && activeDetail==null && state.activePanel!=SatPanel.None && reminderTarget==null) { screen.back() }
    val removedCollection=stringResource(R.string.sat_collection_removed)
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
                            onShowOnGlobe = { satellite -> activeDetail = null; screen.dismiss();focusItem(satellite) },
                            onSpotInAr = ::showOnAr,
                            modifier = sourceModifier(detailSource),
                        )
                    } else {
                        SatelliteHud(screen,status,tokens,config,selectedLiveState,nextFocused,realTimeNowMs,tz,markers,centered,firstRun,timeActions,
                            onLocate={earthFocusTarget=EarthFocusTarget(location.latitude,location.longitude,SATELLITE_EARTH_DEFAULT_DISTANCE,++focusSequence,locate=true)},
                            onCycle=::cycle,onDetail={detail(selectedSatItem)},onCoachDone=::finishCoach,rising=rising,avatarModifier=sourceModifier("focus"),
                            globe=GlobeSurface { viewport ->
                                SatelliteEarth3D(currentSimulationMs = batch.first.takeIf{it!=0L} ?: currentSimulationMs, userLocation = location, isFa = isFa,
                                    mapTheme = mapTheme, appThemeMode = uiState.themeMode,selectedSatelliteId=selectedSatelliteId,selectedSatItem=selectedSatItem,
                                    candidates=allInteractiveMapStates,members=members,favorites=lib.favorites,layers=state.layers,
                                    trainMembers=trainMemberIds,viewport=viewport,
                                    activePass=activeArc?.takeIf{it.sat.id==selectedSatelliteId}?.pass ?: nextFocused?.pass?.takeIf{currentSimulationMs in it.startTimeMs..it.endTimeMs},
                                    overlayStyle=earthOverlayStyle(tokens,uiState.themeMode),focusTarget = earthFocusTarget,onFocusConsumed={earthFocusTarget=null},
                                    onSelectSatellite=::focusSatellite,onDismissAnchoredLabel={showSelectedLabel=false},onOpenSatelliteDetail={detail(it,source="marker")},
                                    modifier=Modifier.fillMaxSize(),controller=camera,tokens=tokens,reduceMotion=reduce,onCenteredChanged={centered=it},
                                    onGlobeGesture={ down ->hushJob?.cancel();if(down)screen.hush(true) else hushJob=scope.launch{delay(200);screen.hush(false)} },
                                    popoverModifier=sourceModifier("marker"),showSelectedLabel=showSelectedLabel)
                            },sheet={ panel ->when(panel){
                                SatPanel.Fleet -> CataloguePanel(screen,repo,results,lib,update,tokens,isFa,angles.count{it.value>0},::focusItem,{detail(it)},{screen.dismiss();focusItem(it)})
                                SatPanel.Customize -> CustomizePanel(screen,lib,update,tokens){id->val original=lib;val def=lib.custom.firstOrNull{it.id==id}
                                    update{it.delete(id)}; feedback=SatFeedback(removedCollection) {if(def!=null)update{l->l.copy(custom=l.custom+def,
                                        order=l.order.toMutableList().apply {add(original.order.indexOf(id).coerceIn(0,size),id)},hidden=if(id in original.hidden)l.hidden+id else l.hidden)};feedback=null} }
                                SatPanel.Editor -> EditorPanel(screen,repo,lib,update,tokens,isFa)
                                SatPanel.Passes -> SatPassesSheet(screen,lib,tokens,isFa,allScoped?.size,passesByNight,spotlight,currentSimulationMs,realTimeNowMs,tz,::passActions,
                                    {update{it.copy(active=Library.ALL)}},if(hasLocation)null else {{viewModel.setShowLocationSelector(true)}})
                                SatPanel.Alerts -> SatAlertsSheet(tokens,isFa,config,{ids,lead->alertPredictions.orEmpty().firstOrNull{(sat,p)->monitored(sat,ids)&&p.startTimeMs-lead*MINUTE>realTimeNowMs}
                                    ?.let{(sat,p)->shortNameOf(sat,isFa)+" · "+SatFmt.dateTime(p.startTimeMs-lead*MINUTE,isFa,tz)}},screen::back,::saveAlerts)
                                SatPanel.Layers -> MapPanel(screen,tokens){theme->screen.lighting(theme);prefs.edit().putString("satellite_map_theme",theme.name).apply()}
                                SatPanel.Guide -> SatGuideSheet(tokens,isFa,screen::back)
                                else -> Unit
                            }})
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

internal fun matchesScope(sat: SatelliteItem, scope: SatelliteCategory): Boolean = when (scope) {
    SatelliteCategory.ALL -> true
    SatelliteCategory.ISS -> sat.category == SatelliteCategory.ISS
    SatelliteCategory.STARLINK -> sat.category == SatelliteCategory.STARLINK || sat.isTrain
    SatelliteCategory.HUBBLE -> sat.category == SatelliteCategory.HUBBLE
    SatelliteCategory.VISIBLE -> sat.isNakedEyeCandidate
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
    popoverShape = SatShapes.of(SatRadius.popover),
)
