package com.alijafari.red.astronomy.ui.obsidian

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.screens.ISSScreen

/**
 * Obsidian Quartz & Crimson Telemetry — Satellites.
 *
 * The orbital pass table, world-track canvas and alert controls are the app's one satellite
 * implementation ([ISSScreen]); the Obsidian identity comes from the theme layer:
 * [ObsidianThemeProvider] remaps `LocalRedColors` (surfaces, hairlines, nav tint, accents) to
 * the Alabaster / Obsidian / Velvet Crimson palette on this branch of the UI selector, and the
 * `EditorialSilhouetteBackdrop` is drawn behind it by `MainActivity`.
 *
 * Kept as its own entry point so the satellites tab has a stable Obsidian seam of its own —
 * the same shape as `CyberSatellitesScreen` / `SpatialSatellitesScreen`.
 */
@Composable
fun ObsidianSatellitesScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    ISSScreen(uiState = uiState, viewModel = viewModel)
}
