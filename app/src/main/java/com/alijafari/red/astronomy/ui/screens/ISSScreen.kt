package com.alijafari.red.astronomy.ui.screens

import androidx.compose.runtime.Composable
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.satellites.SatSkin
import com.alijafari.red.astronomy.ui.satellites.SatellitesStage

@Composable
fun ISSScreen(uiState: MainUiState, viewModel: MainViewModel) {
    SatellitesStage(uiState = uiState, viewModel = viewModel, skin = SatSkin.DEFAULT)
}
