package com.zig.museum.feature.viewer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zig.museum.core.model.*

/**
 * Viewer controls per M5: rotation control, sun-direction control, layer toggles, camera presets, data HUD, procedural indicator
 */

@Composable
fun RotationControlView(
    state: RotationState,
    onSpeedChange: (RotationSpeed) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(8.dp)) {
        Text("Rotation: ${state.speed.labelEn} angle ${"%.1f".format(state.angleDeg)}°", style = MaterialTheme.typography.labelSmall, color = Color.White)
        Row {
            RotationSpeed.entries.forEach { speed ->
                FilterChip(
                    selected = state.speed == speed,
                    onClick = { onSpeedChange(speed) },
                    label = { Text(speed.labelEn) },
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
    }
}

@Composable
fun SunDirectionControlView(
    state: SunState,
    onAzimuthChange: (Float) -> Unit,
    onElevationChange: (Float) -> Unit,
    onExposureChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(8.dp)) {
        Text("Sun: az ${state.azimuthDeg}° el ${state.elevationDeg}° EV ${state.exposureEV}", style = MaterialTheme.typography.labelSmall, color = Color.White)
        Slider(value = state.azimuthDeg, onValueChange = onAzimuthChange, valueRange = 0f..360f, modifier = Modifier.fillMaxWidth())
        Slider(value = state.elevationDeg, onValueChange = onElevationChange, valueRange = -10f..90f, modifier = Modifier.fillMaxWidth())
        Slider(value = state.exposureEV, onValueChange = onExposureChange, valueRange = -5f..5f, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun LayerTogglesView(
    toggles: List<LayerToggle>,
    enabled: Set<String>,
    onToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(8.dp)) {
        Text("Layers", style = MaterialTheme.typography.labelMedium, color = Color.White)
        toggles.forEach { toggle ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(toggle.labelEn, style = MaterialTheme.typography.labelSmall, color = Color.White)
                Switch(checked = toggle.id in enabled, onCheckedChange = { onToggle(toggle.id, it) })
            }
        }
    }
}

@Composable
fun CameraPresetsView(
    presets: List<CameraPreset>,
    onPresetSelected: (CameraPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(8.dp)) {
        Text("Camera Presets", style = MaterialTheme.typography.labelMedium, color = Color.White)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            presets.take(4).forEach { preset ->
                FilterChip(
                    selected = false,
                    onClick = { onPresetSelected(preset) },
                    label = { Text(preset.labelEn, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

@Composable
fun DataHudView(
    state: DataHudState,
    isFa: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(8.dp)) {
        Text(
            text = if (isFa) state.formatFa() else state.formatEn(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
        if (state.proceduralIndicatorVisible()) {
            Text(
                text = if (isFa) "فراتر از داده‌های منتشر شده" else "Beyond published data — procedural detail",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Yellow
            )
        }
    }
}
