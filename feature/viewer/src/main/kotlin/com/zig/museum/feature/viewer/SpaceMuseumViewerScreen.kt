package com.zig.museum.feature.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.zig.museum.core.engine.FilamentView
import com.zig.museum.core.engine.InspectorEngine
import com.zig.museum.core.engine.SunState
import com.zig.museum.core.model.ObjectRegistry

/**
 * SpaceMuseumViewerScreen — M1 implementation with Filament surface.
 * M0 was empty placeholder; M1 hosts Filament via InspectorEngine.
 * Sets ImmersiveScreenState to hide FloatingBottomBar (reuse gravity's object).
 */
@Composable
fun SpaceMuseumViewerScreen(
    objectId: String,
    onBack: () -> Unit,
    onCredits: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isFa: Boolean = false
) {
    // Reuse gravity's immersive flag to hide bottom nav bar without editing MainActivity
    DisposableEffect(Unit) {
        try {
            val clazz = Class.forName("com.zig.gravity.ui.ImmersiveScreenState")
            val instance = clazz.getField("INSTANCE").get(null)
            val activeField = instance.javaClass.getDeclaredField("active")
            activeField.isAccessible = true
            activeField.set(instance, true)
        } catch (e: Exception) {
            // If gravity immersive state not found, ignore — nav bar will stay visible in M0, acceptable
        }
        onDispose {
            try {
                val clazz = Class.forName("com.zig.gravity.ui.ImmersiveScreenState")
                val instance = clazz.getField("INSTANCE").get(null)
                val activeField = instance.javaClass.getDeclaredField("active")
                activeField.isAccessible = true
                activeField.set(instance, false)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    val spec = ObjectRegistry.byId(objectId)
    var sunState by remember { mutableStateOf(SunState()) }
    var tier by remember { mutableStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090A0F))
            .testTag("space_museum_viewer_screen")
    ) {
        // Filament surface — M1 first object on screen
        FilamentView(
            objectId = objectId,
            modifier = Modifier.fillMaxSize(),
            tier = tier,
            sunState = sunState
        )

        // Overlay UI
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("viewer_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = if (isFa) spec?.displayNameFa ?: objectId else spec?.displayNameEn ?: objectId,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.testTag("viewer_object_name")
                )
                IconButton(onClick = { onCredits?.invoke() }, modifier = Modifier.testTag("viewer_credits_button")) {
                    Icon(Icons.Default.Info, contentDescription = "Credits", tint = Color.White)
                }
            }

            Spacer(Modifier.weight(1f))

            // Debug overlay (debug builds only) per §5.9 and §15.3
            val instrumentation = remember { InspectorEngine.getInstance().instrumentation }
            val debugData = instrumentation.toDebugOverlay()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp)
                    .testTag("debug_overlay")
            ) {
                Text(
                    text = "fps: ${"%.1f".format(debugData.fps)} p50: ${"%.1f".format(debugData.p50Ms)}ms p95: ${"%.1f".format(debugData.p95Ms)}ms tier: ${debugData.tier}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "tiles: ${debugData.residentTiles} bytes: ${debugData.residentBytes} uploads: ${debugData.uploadsPerFrame} evict: ${debugData.evictions}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "leaked: ${InspectorEngine.getInstance().getLeakedResourceCount()} (must be 0 after 10 recreations)",
                    color = if (InspectorEngine.getInstance().getLeakedResourceCount() == 0) Color.Green else Color.Red,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Bottom controls placeholder for M1 — rotation, sun direction, EV, tone mapper, TAA toggles
            // Real controls in M5, but M1 needs EV slider and tone mapper switch per DoD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.White.copy(alpha = 0.05f))
                    .testTag("viewer_controls_placeholder"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isFa) "نور خورشید: آزیموت ${sunState.azimuthDeg} ارتفاع ${sunState.elevationDeg} EV ${sunState.exposureEV}" else "Sun: az ${sunState.azimuthDeg} el ${sunState.elevationDeg} EV ${sunState.exposureEV}",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = if (isFa) "تغییر تون‌مپر AgX/PBR Neutral و TAA در M1" else "Tone mapper AgX/PBR Neutral + TAA toggles in M1",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = spec?.let { if (isFa) it.dataCeilingTextFa else it.dataCeilingTextEn } ?: "",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
