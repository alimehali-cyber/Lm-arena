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

        // Schedule periodic 6-hour sync and trigger initial refresh
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

            val sunAltitudeDeg = remember(uiState.userLocation) {
                com.alijafari.red.astronomy.astro_engine.SunEngine.getSunAltAz(
                    com.alijafari.red.astronomy.astro_engine.TimeEngine.getJulianDate(),
                    uiState.userLocation.latitude,
                    uiState.userLocation.longitude
                ).altitudeDeg
            }

            REDTheme(themeMode = uiState.themeMode, sunAltitudeDeg = sunAltitudeDeg) {
                CompositionLocalProvider(
                    LocalContext provides localizedContext,
                    LocalLayoutDirection provides layoutDirection,
                    LocalActivityResultRegistryOwner provides this@MainActivity
                ) {
                    var showSplashScreen by remember { mutableStateOf(true) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("main_scaffold"),
                            containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            val isFa = uiState.language == com.alijafari.red.astronomy.domain.AppLanguage.PERSIAN
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .height(64.dp)
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (uiState.selectedTab == 4) {
                                    // Home Screen Top Bar
                                    // Far RIGHT (Start in RTL): RED logo with pulsing glow
                                    val infiniteTransition = rememberInfiniteTransition(label = "RedLogoGlow")
                                    val glowOpacity by infiniteTransition.animateFloat(
                                        initialValue = 0.0f,
                                        targetValue = 0.15f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(durationMillis = 1500, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "GlowOpacity"
                                    )

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.radialGradient(
                                                        colors = listOf(
                                                            com.alijafari.red.astronomy.ui.theme.AccentPrimary.copy(alpha = glowOpacity),
                                                            Color.Transparent
                                                        )
                                                    )
                                                )
                                        )
                                        SafeAppLogo(
                                            modifier = Modifier.size(32.dp),
                                            cornerRadius = 8.dp
                                        )
                                    }

                                    // Far LEFT (End in RTL): Signature text
                                    Text(
                                        text = "Developed by Ali Jafari",
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontFamily = com.alijafari.red.astronomy.ui.theme.IranSans,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Light,
                                            fontSize = 10.sp,
                                            letterSpacing = 0.5.sp,
                                            color = Color(0xFF3A3A44),
                                            textDirection = androidx.compose.ui.text.style.TextDirection.Ltr
                                        )
                                    )
                                } else {
                                    // Other Tabs Top Bar (e.g. Moon screen)
                                    IconButton(
                                        onClick = { viewModel.selectTab(4) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Back",
                                            tint = Color(0xFF9CA3AF),
                                            modifier = Modifier.size(24.dp)
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
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontFamily = com.alijafari.red.astronomy.ui.theme.IranSans,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    )

                                    Spacer(modifier = Modifier.size(32.dp))
                                }
                            }
                        },
                        bottomBar = {
                            com.alijafari.red.astronomy.ui.components.FloatingBottomBar(
                                selectedTab = uiState.selectedTab,
                                onTabSelected = { viewModel.selectTab(it) }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
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
                    }
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
