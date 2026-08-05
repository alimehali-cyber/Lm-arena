package com.example

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
import com.example.domain.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.components.FavoritesHistoryDialog
import com.example.ui.components.ObjectDetailModal
import com.example.ui.components.SafeAppLogo
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.*
import com.example.ui.theme.REDTheme
import com.example.util.LocaleHelper

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val baseContext = LocalContext.current
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val langCode = LocaleHelper.getLanguageCode(uiState.language)
            val localizedContext = remember(langCode) {
                LocaleHelper.setLocale(baseContext, langCode)
            }
            val isFa = uiState.language == AppLanguage.PERSIAN
            val layoutDirection = if (isFa) LayoutDirection.Rtl else LayoutDirection.Ltr

            REDTheme(themeMode = uiState.themeMode) {
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
                            val isFa = uiState.language == com.example.domain.AppLanguage.PERSIAN
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .height(64.dp)
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (uiState.selectedTab == 0) {
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
                                                            com.example.ui.theme.AccentPrimary.copy(alpha = glowOpacity),
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
                                            fontFamily = com.example.ui.theme.IranSans,
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
                                        onClick = { viewModel.selectTab(0) },
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
                                        1 -> if (isFa) "قطب‌نما AR" else "AR Compass"
                                        2 -> if (isFa) "ماه" else "Moon"
                                        3 -> if (isFa) "ایستگاه فضایی" else "ISS Tracker"
                                        else -> ""
                                    }

                                    Text(
                                        text = pageTitle,
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontFamily = com.example.ui.theme.IranSans,
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
                            com.example.ui.components.FloatingBottomBar(
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
                                    0 -> HomeScreen(
                                        uiState = uiState,
                                        viewModel = viewModel,
                                        onNavigateToTab = { viewModel.selectTab(it) }
                                    )
                                    1 -> CompassARScreen(
                                        uiState = uiState,
                                        viewModel = viewModel
                                    )
                                    2 -> MoonScreen(
                                        uiState = uiState,
                                        viewModel = viewModel
                                    )
                                    3 -> ISSScreen(
                                        uiState = uiState,
                                        viewModel = viewModel
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

                        if (showSplashScreen) {
                            com.example.ui.components.PremiumSplashScreen(
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
