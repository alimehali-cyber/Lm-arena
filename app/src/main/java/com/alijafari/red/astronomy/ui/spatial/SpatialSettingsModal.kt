package com.alijafari.red.astronomy.ui.spatial

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.components.SettingsDialog

/** Spatial settings use the shared, localized settings experience with the Spatial theme provider active. */
@Composable
fun SpatialSettingsModal(
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
