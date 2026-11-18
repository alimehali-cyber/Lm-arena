package com.alijafari.red.astronomy.ui.aether

import androidx.compose.runtime.Composable
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.satellites.SatSkin
import com.alijafari.red.astronomy.ui.satellites.SatellitesStage

@Composable
fun AetherSatellitesScreen(uiState: MainUiState, viewModel: MainViewModel) {
    SatellitesStage(uiState = uiState, viewModel = viewModel, skin = SatSkin.AETHER)
}
