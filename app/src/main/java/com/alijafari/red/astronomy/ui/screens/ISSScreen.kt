package com.alijafari.red.astronomy.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.satellites.SatSkin
import com.alijafari.red.astronomy.ui.satellites.SatellitesStage

/**
 * Satellites — Default skin ("flat editorial").
 *
 * The screen is the one shared stage (see [SatellitesStage]); Default is the reference layout with
 * opaque surfaces, a 1 dp border, and zero blur and zero shadow.
 */
@Composable
fun ISSScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val globeModifier = remember(screenWidthDp) {
        Modifier
            .offset(x = (-16).dp)
            .requiredWidth(screenWidthDp.dp)
    }
    SatellitesStage(
        uiState = uiState,
        viewModel = viewModel,
        skin = SatSkin.DEFAULT,
        globeModifier = globeModifier,
    )
}
