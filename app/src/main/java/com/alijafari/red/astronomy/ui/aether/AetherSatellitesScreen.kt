package com.alijafari.red.astronomy.ui.aether

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
 * Satellites — Aether skin (jewel + dial + oscilloscope).
 *
 * One stage, three skins: Aether shares the anatomy, the state and the rules of the other two skins
 * (see [SatellitesStage]) and changes only its surfaces, numerals and motion. The duplicate-ledger
 * rules are therefore structural, not per-skin:
 *
 * - altitude lives once, in the focus dock's hero counter (plus the contextual popover and detail);
 * - time is controlled once, by the time-strip card, with the steppers integrated at its ends;
 * - the "N above horizon" LED meter moved to the fleet rail header;
 * - fleet tokens focus first, exactly like Default and Spatial, and a second tap opens detail.
 *
 * In Wine mode the jewel surfaces use the Ruby tone; in Light the jewel stays sapphire with a navy
 * rim and keeps luminous digits, because the stage's tokens carry that decision.
 */
@Composable
fun AetherSatellitesScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val globeModifier = remember(screenWidthDp) {
        Modifier
            .offset(x = (-20).dp)
            .requiredWidth((screenWidthDp.dp - 40.dp).coerceAtLeast(1.dp))
    }
    SatellitesStage(
        uiState = uiState,
        viewModel = viewModel,
        skin = SatSkin.AETHER,
        globeModifier = globeModifier,
    )
}
