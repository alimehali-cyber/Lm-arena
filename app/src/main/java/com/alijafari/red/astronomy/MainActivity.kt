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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.alijafari.red.astronomy.ui.components.FavoritesHistoryDialog
import com.alijafari.red.astronomy.ui.components.ObjectDetailModal
import com.alijafari.red.astronomy.ui.components.SafeAppLogo
import com.alijafari.red.astronomy.ui.components.SettingsDialog
import com.alijafari.red.astronomy.ui.screens.*
import com.alijafari.red.astronomy.ui.theme.REDTheme
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
            viewModel.selectTab(2) // Moon tab
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
                timestampMs = currentEffectiveTimeMs
            ) {
                CompositionLocalProvider(
                    LocalContext provides localizedContext,
                    LocalLayoutDirection provides layoutDirection,
                    LocalActivityResultRegistryOwner provides this@MainActivity
                ) {
                    var showSplashScreen by remember { mutableStateOf(true) }

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
                            topBar = {
                                if (uiState.selectedTab != 4) {
                                    val isFa = uiState.language == com.alijafari.red.astronomy.domain.AppLanguage.PERSIAN
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .statusBarsPadding()
                                            .height(56.dp)
                                            .padding(horizontal = com.alijafari.red.astronomy.ui.theme.RedSpacing.lg),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.selectTab(4) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowForward,
                                                contentDescription = "Back",
                                                tint = com.alijafari.red.astronomy.ui.theme.RedTheme.colors.textSecondary,
                                                modifier = Modifier.size(com.alijafari.red.astronomy.ui.theme.RedIconSize.md)
                                            )
                                        }

                                        val pageTitle = when (uiState.selectedTab) {
                                            0 -> if (isFa) "آزمایشگاه" else "Lab"
                                            1 -> if (isFa) "ماهواره‌ها" else "Satellites"
                                            2 -> if (isFa) "ماه" else "Moon"
                                            3 -> if (isFa) "آسمان AR" else "AR Sky"
                                            else -> ""
                                        }

                                        Text(
                                            text = pageTitle,
                                            style = com.alijafari.red.astronomy.ui.theme.RedTypographyTokens.sectionHeading,
                                            color = com.alijafari.red.astronomy.ui.theme.RedTheme.colors.textPrimary
                                        )

                                        Spacer(modifier = Modifier.size(36.dp))
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = innerPadding.calculateTopPadding())
                            ) {
                                Crossfade(
                                    targetState = uiState.selectedTab,
                                    label = "TabSwitch"
                                ) { tab ->
                                    when (tab) {
                                        0 -> LabScreen(
                                            uiState = uiState,
                                            viewModel = viewModel
                                        )
                                        1 -> ISSScreen(
                                            uiState = uiState,
                                            viewModel = viewModel
                                        )
                                        2 -> MoonScreen(
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

                        // Floating Navigation Bar overlaying content directly
                        com.alijafari.red.astronomy.ui.components.FloatingBottomBar(
                            selectedTab = uiState.selectedTab,
                            onTabSelected = { viewModel.selectTab(it) },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        // Dialogs and Modals
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
