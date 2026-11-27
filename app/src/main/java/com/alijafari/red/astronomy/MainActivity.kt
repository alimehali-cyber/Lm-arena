package com.alijafari.red.astronomy

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.alijafari.red.astronomy.ui.StartupTrace
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.AppUiStyle
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.home.HomeWarmupDeadline
import com.alijafari.red.astronomy.ui.home.shouldGateLaunchForHomeWarmup
import com.alijafari.red.astronomy.ui.aether.AetherAmbientBackground
import com.alijafari.red.astronomy.ui.aether.AetherFloatingNavBar
import com.alijafari.red.astronomy.ui.aether.AetherSettingsBubble
import com.alijafari.red.astronomy.ui.aether.AetherHomeScreen
import com.alijafari.red.astronomy.ui.aether.AetherLabScreen
import com.alijafari.red.astronomy.ui.aether.AetherSatellitesScreen
import com.alijafari.red.astronomy.ui.aether.AetherSettingsModal
import com.alijafari.red.astronomy.ui.aether.AetherThemeProvider
import com.alijafari.red.astronomy.ui.spatial.SpatialAtmosphereBackground
import com.alijafari.red.astronomy.ui.spatial.SpatialFloatingNavBar
import com.alijafari.red.astronomy.ui.spatial.SpatialSettingsBubble
import com.alijafari.red.astronomy.ui.spatial.SpatialHomeScreen
import com.alijafari.red.astronomy.ui.spatial.SpatialLabScreen
import com.alijafari.red.astronomy.ui.spatial.SpatialSatellitesScreen
import com.alijafari.red.astronomy.ui.spatial.SpatialSettingsModal
import com.alijafari.red.astronomy.ui.spatial.SpatialThemeProvider
import com.alijafari.red.astronomy.ui.backdrop.AppBackdropMode
import com.alijafari.red.astronomy.ui.backdrop.LiveSkyBackdrop
import com.alijafari.red.astronomy.ui.backdrop.PhotoBackdrop
import com.alijafari.red.astronomy.ui.components.LocationSelectorDialog
import com.alijafari.red.astronomy.ui.components.MoonDetailWindow
import com.alijafari.red.astronomy.ui.components.ObjectDetailModal
import com.alijafari.red.astronomy.ui.components.SafeAppLogo
import com.alijafari.red.astronomy.ui.components.SettingsDialog
import com.alijafari.red.astronomy.ui.components.PremiumSplashScreen
import com.alijafari.red.astronomy.ui.components.SplashRevealRole
import com.alijafari.red.astronomy.ui.components.rememberPremiumSplashState
import com.alijafari.red.astronomy.ui.components.splashReveal
import com.alijafari.red.astronomy.ui.monuments.MonumentId
import com.alijafari.red.astronomy.ui.monuments.MonumentScreen
import com.alijafari.red.astronomy.ui.splash.SplashArtLoader
import com.alijafari.red.astronomy.ui.screens.*
import com.alijafari.red.astronomy.ui.theme.REDTheme
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.util.LocaleHelper

import com.alijafari.red.astronomy.data.repository.TleRepository
import com.alijafari.red.astronomy.data.worker.IssTleWorker
import com.alijafari.red.astronomy.data.worker.TleSyncWorker
import com.alijafari.red.astronomy.astro_engine.SatelliteEngine

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TARGET_OBJECT_ID = "extra_target_object_id"
        const val EXTRA_TARGET_TYPE = "extra_target_type"
        const val EXTRA_TARGET_ROUTE = "extra_target_route"

        /** Longest the system splash is held waiting for the splash art to decode. */
        private const val SPLASH_ART_MAX_HOLD_MS = 800L

        /** Background sync is scheduled after the first frame, or after this long at the latest. */
        private const val BACKGROUND_SYNC_FALLBACK_MS = 3_000L
    }

    private val viewModel: MainViewModel by viewModels()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var backgroundSyncScheduled = false

    /** Schedules the TLE sync work (periodic 6-hour sync plus an initial refresh), once per onCreate. */
    private fun scheduleBackgroundSyncOnce() {
        if (backgroundSyncScheduled) return
        backgroundSyncScheduled = true
        val appContext = applicationContext
        IssTleWorker.schedulePeriodicSync(appContext)
        IssTleWorker.enqueueImmediateSync(appContext)
        TleSyncWorker.schedulePeriodicSync(appContext)
        TleSyncWorker.enqueueImmediateSync(appContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val targetObjId = intent?.getStringExtra(EXTRA_TARGET_OBJECT_ID) ?: return
        val targetType = intent.getStringExtra(EXTRA_TARGET_TYPE) ?: ""

        if (targetType == "SATELLITE" || targetObjId.startsWith("iss") || targetObjId.startsWith("starlink") || targetObjId.startsWith("hubble") || targetObjId.startsWith("tiangong") || targetObjId.startsWith("sat_")) {
            viewModel.selectTab(1) // Satellites tab
            viewModel.selectSatelliteById(targetObjId)
        } else if (targetObjId == "moon" || targetObjId == "planet_moon") {
            // There is no Moon tab any more: the Moon lives on Home as a card, so a lunar notification
            // lands on Home and opens the Moon window from there.
            viewModel.selectTab(4) // Home tab
            viewModel.setShowMoonWindow(true)
        } else {
            val celObj = com.alijafari.red.astronomy.data.catalog.AstronomyCatalog.getById(targetObjId)
            if (celObj != null) {
                viewModel.openObjectDetail(celObj)
            } else {
                viewModel.selectTab(4) // Home tab
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        StartupTrace.mark("activity created")
        // Start decoding the launch splash art before anything else, on its own thread.
        SplashArtLoader.preload(applicationContext)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The phone's status bar stays hidden across the app; a swipe from the top edge still
        // reveals it transiently. Every dialog window re-hides it too (see HideStatusBar).
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        // Android 12+: the system splash is a plain field of the velvet's darkest tone (see
        // values-v31/themes.xml) and the animated splash's first frame is that exact colour, so the
        // system splash can go the instant we draw - no fade overlapping the animation.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { it.remove() }
        }
        handleNotificationIntent(intent)

        // Tie the satellite engine to the TLE repository. The repository opens the database and loads
        // the cached elements when first used, so it is fetched lazily: the start-up warm-up creates it
        // off the main thread instead of here, before the first frame. Same repository, same answers.
        val appContext = applicationContext
        SatelliteEngine.customTleResolver = { noradId -> TleRepository.getInstance(appContext).getTle(noradId) }
        SatelliteEngine.customTleMetadataResolver = { noradId ->
            TleRepository.getInstance(appContext).getTleWithMetadata(noradId)
        }

        // Warm Home before its first reveal only when Home is the launch destination. A notification
        // or deep link to another screen must not wait for, or compete with, unrelated Home work.
        if (shouldGateLaunchForHomeWarmup(viewModel.uiState.value)) {
            viewModel.startStartupWarmup()
        }

        // Schedule the periodic 6-hour sync and the initial refresh once the first frame is up (and
        // at the latest after BACKGROUND_SYNC_FALLBACK_MS), so WorkManager's setup stays out of the
        // launch path. Same work, same policies, a moment later.
        mainHandler.postDelayed(::scheduleBackgroundSyncOnce, BACKGROUND_SYNC_FALLBACK_MS)

        // Keep the system splash (window background) up until the splash art is decoded and its
        // textures are uploading, so the animation's first frame is never a decode stall. Bounded
        // by SPLASH_ART_MAX_HOLD_MS; the splash degrades to a plain velvet colour if art is late.
        val content: View = findViewById(android.R.id.content)
        val holdStart = SystemClock.uptimeMillis()
        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                val ready = SplashArtLoader.isSettled ||
                    SystemClock.uptimeMillis() - holdStart > SPLASH_ART_MAX_HOLD_MS
                if (ready) {
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                    // Runs after this first frame has been drawn.
                    mainHandler.post(::scheduleBackgroundSyncOnce)
                }
                return ready
            }
        })

        setContent {
            // The status bar is hidden for good. The system re-shows it with a bright scrim while
            // the keyboard is up (a white band where the status bar used to be), so tuck it away
            // again whenever the IME becomes visible. Every dialog window re-hides it too.
            com.alijafari.red.astronomy.ui.components.HideStatusBar()
            val baseContext = LocalContext.current
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(uiState.selectedTab) {
                if (uiState.selectedTab != 3) viewModel.satelliteNavigation.returnFromAr = false
            }
            BackHandler(uiState.selectedTab == 3 && viewModel.satelliteNavigation.returnFromAr) {
                viewModel.satelliteNavigation.returnFromAr = false
                viewModel.selectTab(1)
            }
            val showcaseActive = com.alijafari.red.astronomy.ui.demo.Showcase.active
            val showcaseScene = com.alijafari.red.astronomy.ui.demo.Showcase.scene
            BackHandler(showcaseActive) { com.alijafari.red.astronomy.ui.demo.Showcase.stop() }
            LaunchedEffect(showcaseActive) {
                if (showcaseActive) {
                    val original = viewModel.uiState.value
                    try {
                        viewModel.setShowcaseLanguage(AppLanguage.PERSIAN)
                        viewModel.selectTab(4)
                        delay(6000)
                        viewModel.selectTab(0)
                        com.alijafari.red.astronomy.ui.demo.Showcase.scene = com.alijafari.red.astronomy.ui.demo.ShowcaseScene.BLACK_HOLE
                        delay(7000)
                        com.alijafari.red.astronomy.ui.demo.Showcase.scene = com.alijafari.red.astronomy.ui.demo.ShowcaseScene.GRAVITY
                        delay(7000)
                        viewModel.selectTab(1)
                        com.alijafari.red.astronomy.ui.demo.Showcase.scene = com.alijafari.red.astronomy.ui.demo.ShowcaseScene.SATELLITES
                        delay(8000)
                        viewModel.selectTab(3)
                        com.alijafari.red.astronomy.ui.demo.Showcase.scene = com.alijafari.red.astronomy.ui.demo.ShowcaseScene.AR
                        delay(8000)
                        com.alijafari.red.astronomy.ui.demo.Showcase.scene = com.alijafari.red.astronomy.ui.demo.ShowcaseScene.TIME
                        viewModel.setSimulatedTime(1735689600000L)
                        viewModel.setTimeMachineSpeed(com.alijafari.red.astronomy.domain.TimeSimulationSpeed.SPEED_6H)
                        viewModel.setTimeMachinePlaying(true)
                        delay(8000)
                        viewModel.setTimeMachinePlaying(false)
                        viewModel.selectTab(4)
                        com.alijafari.red.astronomy.ui.demo.Showcase.scene = com.alijafari.red.astronomy.ui.demo.ShowcaseScene.END
                        delay(6000)
                    } finally {
                        viewModel.restoreShowcaseState(original.language, original.timeMachineState, original.selectedTab)
                        com.alijafari.red.astronomy.ui.demo.Showcase.stop()
                    }
                }
            }
            val langCode = LocaleHelper.getLanguageCode(uiState.language)
            val localizedContext = remember(langCode) {
                LocaleHelper.setLocale(baseContext, langCode)
            }
            val isFa = uiState.language == AppLanguage.PERSIAN
            val layoutDirection = if (isFa) LayoutDirection.Rtl else LayoutDirection.Ltr
            // The monument page is full-screen window content, so its transition is owned here, next
            // to the state that opens it. The last page is remembered so the exit animation still has
            // something to draw after the open flag has already become null.
            var lastMonument by remember { mutableStateOf<MonumentId?>(null) }
            LaunchedEffect(uiState.monumentDetail) {
                if (uiState.monumentDetail != null) lastMonument = uiState.monumentDetail
            }

            val currentEffectiveTimeMs = if (uiState.timeMachineState.mode == com.alijafari.red.astronomy.domain.TimeMachineMode.SIMULATION) {
                uiState.timeMachineState.simulationTimeMs
            } else {
                System.currentTimeMillis()
            }

            val sunAltitudeDeg = remember(uiState.userLocation, currentEffectiveTimeMs) {
                com.alijafari.red.astronomy.astro_engine.SunEngine.getSunAltAz(
                    com.alijafari.red.astronomy.astro_engine.TimeEngine.getJulianDate(currentEffectiveTimeMs),
                    uiState.userLocation.latitude,
                    uiState.userLocation.longitude
                ).altitudeDeg
            }

            REDTheme(
                themeMode = uiState.themeMode,
                sunAltitudeDeg = sunAltitudeDeg,
                userLatitude = uiState.userLocation.latitude,
                userLongitude = uiState.userLocation.longitude,
                timestampMs = currentEffectiveTimeMs,
                isPersian = isFa
            ) {
                CompositionLocalProvider(
                    LocalContext provides localizedContext,
                    LocalLayoutDirection provides layoutDirection,
                    LocalActivityResultRegistryOwner provides this@MainActivity,
                    // The selected interface design language, published for the screens that are not
                    // one of the three style-native pages. The AR sky and the Gravity Sandbox read it
                    // through `chromeTokens()` so their floating chrome is geometrically in the same
                    // family as the page that launched them (see ui/theme/ImmersiveChrome.kt).
                    com.alijafari.red.astronomy.ui.theme.LocalAppUiStyle provides uiState.uiStyle
                ) {
                    // Orientation changes (including the sky viewer's landscape transition) recreate the
                    // Activity. Save this one-shot state across that recreation so the intro never reappears.
                    var showSplashScreen by rememberSaveable { mutableStateOf(true) }
                    // The splash clock, shared with the UI so the UI slides in on the same timeline as
                    // the splash leaves. Null once the splash is gone (every reveal is then a no-op).
                    val splashState = if (showSplashScreen) rememberPremiumSplashState(waitForContent = true) else null
                    // On a Home launch, the app is composed under the splash once Home's astronomy
                    // is ready (the warm-up) and the Z has landed. Other launch routes don't wait for
                    // unrelated Home work. The splash holds its exit until the app has drawn (below).
                    // With no splash (a recreation after hand-off), the app is composed at once.
                    val warmupDone by viewModel.startupWarmupDone.collectAsStateWithLifecycle()
                    val homeWarmupRequired = shouldGateLaunchForHomeWarmup(uiState)

                    // This is a wall-clock deadline, deliberately separate from the splash animation
                    // clock. The animation caps large frame deltas to stay smooth, so it must not be
                    // used as a timeout (at low frame rates it can advance much more slowly than real
                    // time). Save the deadline across Activity recreation so rotation cannot restart it.
                    var warmupDeadlineElapsedMs by rememberSaveable {
                        mutableStateOf(0L)
                    }
                    LaunchedEffect(splashState, homeWarmupRequired, warmupDone) {
                        if (splashState == null || !homeWarmupRequired || warmupDone) {
                            return@LaunchedEffect
                        }
                        if (warmupDeadlineElapsedMs == 0L) {
                            warmupDeadlineElapsedMs =
                                SystemClock.elapsedRealtime() + HomeWarmupDeadline.MAX_WAIT_MS
                        }
                        while (true) {
                            val remainingMs = HomeWarmupDeadline.remainingMs(
                                deadlineElapsedRealtimeMs = warmupDeadlineElapsedMs,
                                nowElapsedRealtimeMs = SystemClock.elapsedRealtime()
                            )
                            if (remainingMs == 0L) break
                            // Recheck elapsedRealtime periodically: unlike the animation clock, it
                            // also accounts for long pauses between rendered frames and device sleep.
                            delay(minOf(remainingMs, 250L))
                        }
                        StartupTrace.mark("Home warm-up deadline reached; revealing app without splash")
                        // If even the animation clock is stalled, merely allowing composition behind
                        // the splash would not help. Drop the splash at the wall-clock deadline so a
                        // slow/failed warm-up cannot leave the launch permanently covered.
                        showSplashScreen = false
                    }

                    val composeApp = if (splashState == null) {
                        true
                    } else {
                        val splashAllows by remember(splashState) {
                            derivedStateOf { splashState.allowsAppComposition }
                        }
                        val warmupMayBeSkipped = !homeWarmupRequired
                        splashAllows && (warmupDone || warmupMayBeSkipped)
                    }
                    // If launch went to a different destination, begin warming Home only after the
                    // splash hand-off. This preserves the later Home benefit without spending worker
                    // time on the destination's first visible frames.
                    LaunchedEffect(showSplashScreen, warmupDone) {
                        if (!showSplashScreen && !warmupDone) viewModel.startStartupWarmup()
                    }
                    // How bright the thing behind the app actually is, resolved from what the backdrop
                    // *is* rather than from the theme (Part 6, item 1). Text with no surface under it —
                    // the Home feed titles — asks this for its ink.
                    val themeBackgroundColor = com.alijafari.red.astronomy.ui.theme.RedTheme.colors.background
                    val backdropLuminance = remember(
                        uiState.appBackdropMode,
                        uiState.skyCanvasTheme,
                        themeBackgroundColor,
                        sunAltitudeDeg
                    ) {
                        com.alijafari.red.astronomy.ui.theme.appBackdropLuminance(
                            mode = uiState.appBackdropMode,
                            themeBackground = themeBackgroundColor,
                            sunAltitudeDeg = sunAltitudeDeg,
                            skyTheme = uiState.skyCanvasTheme
                        )
                    }

                    // The backdrop's measured brightness is still resolved and provided: the Home feed
                    // titles are written straight onto it and pick their ink from it (see BackdropInk).
                    val isSpatialUi = uiState.uiStyle == AppUiStyle.SPATIAL
                    val isAetherUi = uiState.uiStyle == AppUiStyle.AETHER
                    SpatialThemeProvider(
                        enabled = isSpatialUi,
                        themeMode = uiState.themeMode
                    ) {
                    AetherThemeProvider(
                        enabled = isAetherUi,
                        themeMode = uiState.themeMode
                    ) {
                    CompositionLocalProvider(
                        com.alijafari.red.astronomy.ui.theme.LocalBackdropLuminance provides backdropLuminance
                    ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(com.alijafari.red.astronomy.ui.theme.RedTheme.colors.background)
                    ) {
                    if (composeApp) {
                    // Once the app's first frames are drawn, release the splash and report the launch
                    // as fully drawn (the platform's time-to-full-display metric).
                    LaunchedEffect(Unit) {
                        StartupTrace.mark("app composed")
                        withFrameNanos { }
                        withFrameNanos { }
                        splashState?.markContentReady()
                        StartupTrace.mark("app drawn")
                        reportFullyDrawn()
                    }
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .splashReveal(splashState)
                            .testTag("main_scaffold"),
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),

                    ) { innerPadding ->
                        // The scaffold has no top band, so its inner padding is zero: each screen owns its
                        // own status-bar inset. The backdrop still spans the whole window, which is what
                        // lets the sky run under the status bar.
                        Box(modifier = Modifier.fillMaxSize()) {
                            // The app backdrop: the live sky and the photograph, over the theme's own surface
                            // painted by the Box above. Drawn once, behind every screen.

                            // Always composed. It emits nothing unless Live Sky is eligible for the current screen.
                            LiveSkyBackdrop(
                                enabled = uiState.appBackdropMode == AppBackdropMode.LIVE_SKY,
                                selectedTab = uiState.selectedTab,
                                isSimulation = uiState.timeMachineState.mode == com.alijafari.red.astronomy.domain.TimeMachineMode.SIMULATION,
                                simulationTimeMs = uiState.timeMachineState.simulationTimeMs,
                                latitudeDeg = uiState.userLocation.latitude,
                                longitudeDeg = uiState.userLocation.longitude,
                                elevationM = uiState.userLocation.elevationMeters,
                                theme = uiState.skyCanvasTheme,
                                liveTimeMs = viewModel.skyLiveClock.timeMs,
                                dragOffsetHours = viewModel.skyDragOffsetHours,
                                modifier = Modifier.fillMaxSize(),
                                bortleClass = uiState.bortleClass
                            )
                            PhotoBackdrop(
                                mode = uiState.appBackdropMode,
                                selectedTab = uiState.selectedTab,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isSpatialUi && uiState.selectedTab != 3) {
                                SpatialAtmosphereBackground(
                                    themeMode = uiState.themeMode,
                                    isOpaqueDreamscape = uiState.appBackdropMode == AppBackdropMode.NONE,
                                    sunAltitudeDeg = sunAltitudeDeg,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            if (isAetherUi && uiState.selectedTab != 3) {
                                AetherAmbientBackground(
                                    themeMode = uiState.themeMode,
                                    isOpaqueCanvas = uiState.appBackdropMode == AppBackdropMode.NONE,
                                    sunAltitudeDeg = sunAltitudeDeg,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // Preserve the satellite sheet and detail scroll positions across the AR tab.
                            val satelliteTabState = rememberSaveableStateHolder()
                            // The screens, drawn over the backdrop.
                            Crossfade(
                                targetState = Pair(uiState.selectedTab, uiState.uiStyle),
                                label = "TabSwitch"
                            ) { (tab, activeStyle) ->
                                when (tab) {
                                    // Root tab 2 is retired: the Moon screen became a card on Home.
                                    0 -> if (showcaseScene == com.alijafari.red.astronomy.ui.demo.ShowcaseScene.BLACK_HOLE) {
                                        com.zig.antar.ui.AntarRoot(onBack = { com.alijafari.red.astronomy.ui.demo.Showcase.stop() }, language = AppLanguage.PERSIAN, uiStyle = activeStyle, themeMode = uiState.themeMode, showcase = true)
                                    } else if (showcaseScene == com.alijafari.red.astronomy.ui.demo.ShowcaseScene.GRAVITY) {
                                        com.zig.gravity.ui.GravitySandboxRoot(onBack = { com.alijafari.red.astronomy.ui.demo.Showcase.stop() }, startInPersian = true, startInDarkTheme = uiState.themeMode != com.alijafari.red.astronomy.domain.ThemeMode.LIGHT, showcase = true)
                                    } else when (activeStyle) {
                                        AppUiStyle.AETHER -> AetherLabScreen(
                                            uiState = uiState,
                                            viewModel = viewModel
                                        )
                                        AppUiStyle.SPATIAL -> SpatialLabScreen(
                                            uiState = uiState,
                                            viewModel = viewModel
                                        )
                                        AppUiStyle.DEFAULT -> LabScreen(
                                            uiState = uiState,
                                            viewModel = viewModel
                                        )
                                    }
                                    1 -> satelliteTabState.SaveableStateProvider("satellites-$activeStyle") {
                                        when (activeStyle) {
                                        AppUiStyle.AETHER -> AetherSatellitesScreen(
                                            uiState = uiState,
                                            viewModel = viewModel
                                        )
                                        AppUiStyle.SPATIAL -> SpatialSatellitesScreen(
                                            uiState = uiState,
                                            viewModel = viewModel
                                        )
                                        AppUiStyle.DEFAULT -> ISSScreen(
                                            uiState = uiState,
                                            viewModel = viewModel
                                        )
                                        }
                                    }
                                    3 -> CompassARScreen(
                                        uiState = uiState,
                                        viewModel = viewModel
                                    )
                                    4 -> when (activeStyle) {
                                        AppUiStyle.AETHER -> AetherHomeScreen(
                                            uiState = uiState,
                                            viewModel = viewModel,
                                            onNavigateToTab = { viewModel.selectTab(it) }
                                        )
                                        AppUiStyle.SPATIAL -> SpatialHomeScreen(
                                            uiState = uiState,
                                            viewModel = viewModel,
                                            onNavigateToTab = { viewModel.selectTab(it) }
                                        )
                                        AppUiStyle.DEFAULT -> HomeScreen(
                                            uiState = uiState,
                                            viewModel = viewModel,
                                            onNavigateToTab = { viewModel.selectTab(it) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showcaseScene == com.alijafari.red.astronomy.ui.demo.ShowcaseScene.END) {
                        Box(Modifier.fillMaxSize().background(Color(0xFF080C1A)), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("زیگ", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                                Text("نسخهٔ بتا، به‌زودی", color = Color.White, fontSize = 20.sp)
                            }
                        }
                    }

                    // Floating Navigation Bar.
                    // Hidden while an immersive screen (the Gravity Sandbox) owns the display.
                    // Where the settings button sits on screen: the anchor the settings menu grows
                    // out of when it opens and returns into when it closes.
                    var settingsButtonCenter by remember { mutableStateOf<Offset?>(null) }
                    val satellitesChromeOccluded by viewModel.satellitesChromeOccluded.collectAsStateWithLifecycle()
                    if (showcaseScene != com.alijafari.red.astronomy.ui.demo.ShowcaseScene.END && !com.zig.gravity.ui.ImmersiveScreenState.active && !(uiState.selectedTab == 1 && satellitesChromeOccluded)) {
                        // The pill dock and the circular settings button form one cluster: close
                        // together, centred at the bottom of the screen. They always travel together.
                        val chromeModifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = RedSpacing.lg, vertical = RedSpacing.sm)
                            .splashReveal(splashState, SplashRevealRole.NavigationBar)
                        Row(
                            modifier = chromeModifier,
                            horizontalArrangement = Arrangement.spacedBy(
                                com.alijafari.red.astronomy.ui.components.BOTTOM_NAV_GAP,
                                Alignment.CenterHorizontally
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val navModifier = Modifier.fillMaxWidth(
                                com.alijafari.red.astronomy.ui.components.BOTTOM_NAV_WIDTH_FRACTION
                            )
                            when (uiState.uiStyle) {
                                AppUiStyle.AETHER -> AetherFloatingNavBar(
                                    selectedTab = uiState.selectedTab,
                                    onTabSelected = { viewModel.selectTab(it) },
                                    modifier = navModifier
                                )
                                AppUiStyle.SPATIAL -> SpatialFloatingNavBar(
                                    selectedTab = uiState.selectedTab,
                                    onTabSelected = { viewModel.selectTab(it) },
                                    modifier = navModifier
                                )
                                AppUiStyle.DEFAULT -> com.alijafari.red.astronomy.ui.components.FloatingBottomBar(
                                    selectedTab = uiState.selectedTab,
                                    onTabSelected = { viewModel.selectTab(it) },
                                    modifier = navModifier
                                )
                            }
                            val openSettings = { viewModel.setShowSettingsDialog(true) }
                            Box(
                                modifier = Modifier.onGloballyPositioned { coords ->
                                    settingsButtonCenter = coords.boundsInRoot().center
                                }
                            ) {
                                when (uiState.uiStyle) {
                                    AppUiStyle.AETHER -> AetherSettingsBubble(onClick = openSettings)
                                    AppUiStyle.SPATIAL -> SpatialSettingsBubble(onClick = openSettings)
                                    AppUiStyle.DEFAULT -> com.alijafari.red.astronomy.ui.components
                                        .FloatingSettingsBubble(onClick = openSettings)
                                }
                            }
                        }
                    }

                    // Dialogs and Modals, drawn as siblings above the content
                    if (uiState.showLocationSelector) {
                        LocationSelectorDialog(
                            uiState = uiState,
                            onDismiss = { viewModel.setShowLocationSelector(false) },
                            onSelectLocation = { city ->
                                viewModel.setLocation(
                                    cityEn = city.nameEn,
                                    cityFa = city.nameFa,
                                    lat = city.latitude,
                                    lon = city.longitude,
                                    elevationMeters = city.elevationMeters,
                                    timezoneId = city.timezoneId,
                                    countryCode = if (city.isIran) "IR" else "GLOBAL",
                                    provinceEn = city.provinceEn,
                                    provinceFa = city.provinceFa
                                )
                            },
                            onSelectCoordinates = { lat, lon, elev, nameEn, nameFa ->
                                viewModel.setLocation(
                                    cityEn = nameEn,
                                    cityFa = nameFa,
                                    lat = lat,
                                    lon = lon,
                                    elevationMeters = elev
                                )
                            },
                            onGpsSelected = { lat, lon, alt ->
                                viewModel.setGpsLocation(lat, lon, alt)
                            },
                            onToggleFavorite = { city ->
                                viewModel.toggleFavoriteLocation(city)
                            },
                            onRemoveFavorite = { id ->
                                viewModel.removeFavoriteLocation(id)
                            }
                        )
                    }

                      if (uiState.selectedObjectForDetail != null) {
                          ObjectDetailModal(
                              obj = uiState.selectedObjectForDetail!!,
                              uiState = uiState,
                              viewModel = viewModel,
                              onDismiss = { viewModel.closeObjectDetail() }
                          )
                      }

                      if (uiState.showMoonWindow) {
                          MoonDetailWindow(
                                    baseTimeMs = currentEffectiveTimeMs,
                              uiState = uiState,
                              viewModel = viewModel,
                              onDismiss = { viewModel.setShowMoonWindow(false) }
                          )
                      }

                      if (uiState.showSettingsDialog) {
                          when (uiState.uiStyle) {
                              AppUiStyle.AETHER -> AetherSettingsModal(
                                  uiState = uiState,
                                  viewModel = viewModel,
                                  onDismiss = { viewModel.setShowSettingsDialog(false) },
                                  settingsButtonCenter = settingsButtonCenter
                              )
                              AppUiStyle.SPATIAL -> SpatialSettingsModal(
                                  uiState = uiState,
                                  viewModel = viewModel,
                                  onDismiss = { viewModel.setShowSettingsDialog(false) },
                                  settingsButtonCenter = settingsButtonCenter
                              )
                              AppUiStyle.DEFAULT -> SettingsDialog(
                                  uiState = uiState,
                                  viewModel = viewModel,
                                  onDismiss = { viewModel.setShowSettingsDialog(false) },
                                  settingsButtonCenter = settingsButtonCenter
                              )
                          }
                      }

                    // The monument pages. Drawn last, above the screens, the floating navigation bar
                    // and every dialog, and opening off the canvas with a short lift: a 0.92 scale and
                    // a 1/12 slide, both on the same 420 ms curve, so the scene appears to recede
                    // rather than be covered. Closing is deliberately faster than opening.
                    AnimatedVisibility(
                        visible = uiState.monumentDetail != null,
                        enter = fadeIn(tween(220)) +
                            scaleIn(
                                animationSpec = tween(420, easing = FastOutSlowInEasing),
                                initialScale = 0.92f
                            ) +
                            slideInVertically(tween(420, easing = FastOutSlowInEasing)) { it / 12 },
                        exit = fadeOut(tween(180)) +
                            scaleOut(
                                animationSpec = tween(260, easing = FastOutSlowInEasing),
                                targetScale = 0.96f
                            ) +
                            slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it / 16 }
                    ) {
                        val openMonument = lastMonument
                        if (openMonument != null) {
                            MonumentScreen(
                                monument = openMonument,
                                isPersian = isFa,
                                onClose = { viewModel.closeMonument() },
                                modifier = Modifier.testTag("monument_screen_root")
                            )
                        }
                    }

                    } // composeApp

                      if (showSplashScreen && splashState != null) {
                          PremiumSplashScreen(
                              onSplashComplete = {
                                  showSplashScreen = false
                              },
                              state = splashState
                          )
                      }
                  }
                }
                }
                }
            }
        }
    }
}
}
