package com.alijafari.red.astronomy.ui.aether

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.components.SettingsDialog

/** Aether settings use the shared, localized settings experience with the Aether theme provider active. */
@Composable
fun AetherSettingsModal(
    uiState: MainUiState,
    viewModel: MainViewModel,
    sourceBounds: Rect?,
    onDismiss: () -> Unit,
) {
    SettingsDialog(
        uiState = uiState,
        viewModel = viewModel,
        sourceBounds = sourceBounds,
        onDismiss = onDismiss,
    )
}
