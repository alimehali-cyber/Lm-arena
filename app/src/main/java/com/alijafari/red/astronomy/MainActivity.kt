package com.alijafari.red.astronomy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
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
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.backdrop.AppBackdropMode
import com.alijafari.red.astronomy.ui.backdrop.LiveSkyBackdrop
import com.alijafari.red.astronomy.ui.backdrop.PhotoBackdrop
import com.alijafari.red.astronomy.ui.components.FavoritesHistoryDialog
import com.alijafari.red.astronomy.ui.components.LocationSelectorDialog
import com.alijafari.red.astronomy.ui.components.MoonDetailWindow
import com.alijafari.red.astronomy.ui.components.ObjectDetailModal
import com.alijafari.red.astronomy.ui.components.SafeAppLogo
import com.alijafari.red.astronomy.ui.components.SettingsDialog
import com.alijafari.red.astronomy.ui.screens.*
import com.alijafari.red.astronomy.ui.theme.REDTheme
import com.alijafari.red.astronomy.util.LocaleHelper
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

import com.alijafari.red.astronomy.data.repository.TleRepository
import com.alijafari.red.astronomy.data.worker.IssTleWorker
import com.alijafari.red.astronomy.data.worker.TleSyncWorker
import com.alijafari.red.astronomy.astro_engine.SatelliteEngine

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TARGET_OBJECT_ID = "extra_target_object_id"
        const val EXTRA_TARGET_TYPE = "extra_target_type"
        const val EXTRA_TARGET_ROUTE = "extra_target_route"
    }

    private val viewModel: MainViewModel by viewModels()

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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNotificationIntent(intent)

        // Initialize TLE repository and tie satellite engine resolver
        val tleRepo = TleRepository.getInstance(applicationContext)
        SatelliteEngine.customTleResolver = { noradId -> tleRepo.getTle(noradId) }
        SatelliteEngine.customTleMetadataResolver = { noradId -> tleRepo.getTleWithMetadata(noradId) }

        // Schedule periodic 6-hour sync and trigger initial refresh
        IssTleWorker.schedulePeriodicSync(this)
        IssTleWorker.enqueueImmediateSync(this)
        TleSyncWorker.schedulePeriodicSync(this)
        TleSyncWorker.enqueueImmediateSync(this)

        setContent {
            val baseContext = LocalContext.current
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val langCode = LocaleHelper.getLanguageCode(uiState.language)
            val localizedContext = remember(langCode) {
                LocaleHelper.setLocale(baseContext, langCode)
            }
            val isFa = uiState.language == AppLanguage.PERSIAN
            val layoutDirection = if (isFa) LayoutDirection.Rtl else LayoutDirection.Ltr

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
                    LocalActivityResultRegistryOwner provides this@MainActivity
                ) {
                    // Orientation changes (including the sky viewer's landscape transition) recreate the
                    // Activity. Save this one-shot state across that recreation so the intro never reappears.
                    var showSplashScreen by rememberSaveable { mutableStateOf(true) }
                    val isGlassActive = uiState.liquidGlassConfig.enabled && com.alijafari.red.astronomy.ui.theme.isLiquidGlassSupported()

                    // TWO layers, for two different jobs. Both are only recorded when real glass is on.
                    //
                    //  - `backdrop` records the whole content area. The floating bar and every dialog are
                    //    siblings *outside* that subtree, so they can sample it safely.
                    //  - `glassBackdrop` records only what is genuinely behind the cards: the theme
                    //    background, the live sky and the photo. Content cards live outside it and sample
                    //    it, which is what makes Home, Lab and Satellite cards real glass (Part 6) without
                    //    ever re-entering a recording pass on their own subtree — the fatal
                    //    "Can not perform this action inside of drawBackdrop!" case.
                    //
                    // The sky is nevertheless drawn once: its layer is a child of the content layer, so
                    // recording the content blits the already-recorded sky into it.
                    val backdrop = if (isGlassActive) rememberLayerBackdrop() else null
                    val glassBackdrop = if (isGlassActive && uiState.selectedTab != 3) {
                        rememberLayerBackdrop()
                    } else {
                        null
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

                    CompositionLocalProvider(
                        com.alijafari.red.astronomy.ui.theme.LocalLiquidGlassEnabled provides (uiState.liquidGlassConfig.enabled && com.alijafari.red.astronomy.ui.theme.isLiquidGlassSupported()),
                        com.alijafari.red.astronomy.ui.theme.LocalLiquidGlassConfig provides uiState.liquidGlassConfig,
                        com.alijafari.red.astronomy.ui.theme.LocalLiquidGlassBackdrop provides glassBackdrop,
                        com.alijafari.red.astronomy.ui.theme.LocalBackdropLuminance provides backdropLuminance
                    ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(com.alijafari.red.astronomy.ui.theme.RedTheme.colors.background)
                    ) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("main_scaffold"),
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),

                    ) { innerPadding ->
                        // The scaffold has no top band any more (Part 6, item 8), so its inner padding is
                        // zero: each screen owns its own status-bar inset. The backdrop layer still spans
                        // the whole window, which is what lets the sky run under the status bar.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (backdrop != null && uiState.selectedTab != 3) {
                                        Modifier.layerBackdrop(backdrop)
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            // The app backdrop, recorded on its own layer so the cards can sample it. This
                            // subtree contains nothing but the backdrop — the theme's own surface, the live
                            // sky and the photograph — so a card drawing this layer is never reading a
                            // recording that contains itself.
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(com.alijafari.red.astronomy.ui.theme.RedTheme.colors.background)
                                    .then(
                                        if (glassBackdrop != null) {
                                            Modifier.layerBackdrop(glassBackdrop)
                                        } else {
                                            Modifier
                                        }
                                    )
                            ) {
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
                            }
                            // The screens. Deliberately NOT wrapped in a null-backdrop provider any more:
                            // `LocalLiquidGlassBackdrop` carries the app backdrop resolved above, so every
                            // card that used to fall back to its own surface now samples the real thing.
                            Crossfade(
                                targetState = uiState.selectedTab,
                                label = "TabSwitch"
                            ) { tab ->
                                when (tab) {
                                    // Root tab 2 is retired: the Moon screen became a card on Home.
                                    0 -> LabScreen(
                                        uiState = uiState,
                                        viewModel = viewModel
                                    )
                                    1 -> ISSScreen(
                                        uiState = uiState,
                                        viewModel = viewModel
                                    )
                                    3 -> CompassARScreen(
                                        uiState = uiState,
                                        viewModel = viewModel
                                    )
                                    4 -> HomeScreen(
                                        uiState = uiState,
                                        viewModel = viewModel,
                                        onNavigateToTab = { viewModel.selectTab(it) }
                                    )
                                }
                            }
                        }
                    }

                    // Floating Navigation Bar with Real Kyant0 Liquid Glass Backdrop sampling.
                    // Hidden while an immersive screen (the Gravity Sandbox) owns the display.
                    if (!com.zig.gravity.ui.ImmersiveScreenState.active) {
                    com.alijafari.red.astronomy.ui.components.FloatingBottomBar(
                        backdrop = backdrop,
                        selectedTab = uiState.selectedTab,
                        onTabSelected = { viewModel.selectTab(it) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                    }

                    // Dialogs and Modals (Promoted sibling overlays with Real Kyant0 Liquid Glass Backdrop sampling)
                    if (uiState.showLocationSelector) {
                        LocationSelectorDialog(
                            backdrop = backdrop,
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

                      if (uiState.showFavoritesDialog) {
                          FavoritesHistoryDialog(
                              uiState = uiState,
                              viewModel = viewModel,
                              onDismiss = { viewModel.setShowFavoritesDialog(false) }
                          )
                      }

                      if (uiState.showMoonWindow) {
                          MoonDetailWindow(
                              backdrop = backdrop,
                              baseTimeMs = currentEffectiveTimeMs,
                              uiState = uiState,
                              viewModel = viewModel,
                              onDismiss = { viewModel.setShowMoonWindow(false) }
                          )
                      }

                      if (uiState.showSettingsDialog) {
                          SettingsDialog(
                              uiState = uiState,
                              viewModel = viewModel,
                              onDismiss = { viewModel.setShowSettingsDialog(false) }
                          )
                      }

                      if (showSplashScreen) {
                          com.alijafari.red.astronomy.ui.components.PremiumSplashScreen(
                              onSplashComplete = {
                                  showSplashScreen = false
                              }
                          )
                      }
                  }
                }
            }
        }
    }
}
}
