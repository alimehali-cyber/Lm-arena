package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
                    LocalLayoutDirection provides layoutDirection
                ) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("main_scaffold"),
                        topBar = {
                            TopAppBar(
                                title = {
                                    Image(
                                        painter = painterResource(id = R.drawable.red_app_logo),
                                        contentDescription = "RED App Logo",
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                },
                                actions = {
                                    IconButton(
                                        onClick = { viewModel.setShowFavoritesDialog(true) },
                                        modifier = Modifier.testTag("top_favorites_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = stringResource(R.string.favorites_and_history),
                                            tint = Color(0xFFFFB703)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.setShowSettingsDialog(true) },
                                        modifier = Modifier.testTag("top_settings_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = stringResource(R.string.settings),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .testTag("main_bottom_navigation")
                            ) {
                                NavigationBarItem(
                                    selected = uiState.selectedTab == 0,
                                    onClick = { viewModel.selectTab(0) },
                                    icon = { Icon(if (uiState.selectedTab == 0) Icons.Default.Home else Icons.Outlined.Home, contentDescription = stringResource(R.string.nav_home)) },
                                    label = { Text(text = stringResource(R.string.nav_home), fontSize = 11.sp) },
                                    modifier = Modifier.testTag("nav_item_home")
                                )
                                NavigationBarItem(
                                    selected = uiState.selectedTab == 1,
                                    onClick = { viewModel.selectTab(1) },
                                    icon = { Icon(if (uiState.selectedTab == 1) Icons.Default.Explore else Icons.Outlined.Explore, contentDescription = stringResource(R.string.nav_compass)) },
                                    label = { Text(text = stringResource(R.string.nav_compass), fontSize = 11.sp) },
                                    modifier = Modifier.testTag("nav_item_compass")
                                )
                                NavigationBarItem(
                                    selected = uiState.selectedTab == 2,
                                    onClick = { viewModel.selectTab(2) },
                                    icon = { Icon(if (uiState.selectedTab == 2) Icons.Default.Map else Icons.Outlined.Map, contentDescription = stringResource(R.string.nav_skymap)) },
                                    label = { Text(text = stringResource(R.string.nav_skymap), fontSize = 11.sp) },
                                    modifier = Modifier.testTag("nav_item_skymap")
                                )
                                NavigationBarItem(
                                    selected = uiState.selectedTab == 3,
                                    onClick = { viewModel.selectTab(3) },
                                    icon = { Icon(if (uiState.selectedTab == 3) Icons.Default.NightlightRound else Icons.Outlined.Nightlight, contentDescription = stringResource(R.string.nav_moon)) },
                                    label = { Text(text = stringResource(R.string.nav_moon), fontSize = 11.sp) },
                                    modifier = Modifier.testTag("nav_item_moon")
                                )
                                NavigationBarItem(
                                    selected = uiState.selectedTab == 4,
                                    onClick = { viewModel.selectTab(4) },
                                    icon = { Icon(if (uiState.selectedTab == 4) Icons.Default.SatelliteAlt else Icons.Outlined.SatelliteAlt, contentDescription = stringResource(R.string.nav_iss)) },
                                    label = { Text(text = stringResource(R.string.nav_iss), fontSize = 11.sp) },
                                    modifier = Modifier.testTag("nav_item_iss")
                                )
                            }
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
                                    2 -> SkyMapScreen(
                                        uiState = uiState,
                                        viewModel = viewModel
                                    )
                                    3 -> MoonScreen(
                                        uiState = uiState,
                                        viewModel = viewModel
                                    )
                                    4 -> ISSScreen(
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
                    }
                }
            }
        }
    }
}
