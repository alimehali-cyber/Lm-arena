package com.alijafari.red.astronomy.ui.spatial

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.satellites.SatSkin
import com.alijafari.red.astronomy.ui.satellites.SatellitesStage

/**
 * Satellites — Spatial skin (glass + mesh + dot-matrix).
 *
 * Same anatomy as the Default skin (see [SatellitesStage]); only the surfaces, the status chip's mesh,
 * the numerals and the motion change. Spatial uses the same Live/Simulation capsule behaviour as the
 * other skins, and its fleet tokens focus on the globe before they open detail.
 */
@Composable
fun SpatialSatellitesScreen(
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
        skin = SatSkin.SPATIAL,
        globeModifier = globeModifier,
    )
}
