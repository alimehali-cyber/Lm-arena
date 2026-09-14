package com.zig.museum.feature.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.zig.museum.core.engine.CameraState
import com.zig.museum.core.engine.FilamentView
import com.zig.museum.core.engine.InspectorEngine
import com.zig.museum.core.engine.SunState
import com.zig.museum.core.model.ObjectRegistry

/**
 * SpaceMuseumViewerScreen — with real Filament rendering and fallback, aware of bottom nav bar
 */
@Composable
fun SpaceMuseumViewerScreen(
    objectId: String,
    onBack: () -> Unit,
    onCredits: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    isFa: Boolean = false
) {
    // Ensure immersive hides bottom nav bar for entire museum
    DisposableEffect(Unit) {
        try {
            val clazz = Class.forName("com.zig.gravity.ui.ImmersiveScreenState")
            val instance = clazz.getField("INSTANCE").get(null)
            val activeField = instance.javaClass.getDeclaredField("active")
            activeField.isAccessible = true
            activeField.set(instance, true)
        } catch (e: Exception) {
        }
        onDispose {
            try {
                val clazz = Class.forName("com.zig.gravity.ui.ImmersiveScreenState")
                val instance = clazz.getField("INSTANCE").get(null)
                val activeField = instance.javaClass.getDeclaredField("active")
                activeField.isAccessible = true
                activeField.set(instance, false)
            } catch (e: Exception) {
            }
        }
    }

    val spec = ObjectRegistry.byId(objectId)
    var sunState by remember { mutableStateOf(SunState()) }
    var tier by remember { mutableStateOf(0) }

    // Color for fallback rendering when Filament fails
    val fallbackColor = remember(objectId) {
        when (objectId) {
            "sun" -> Color(0xFFFFD54F)
            "mercury" -> Color(0xFF9E9E9E)
            "venus" -> Color(0xFFE6C9A8)
            "earth" -> Color(0xFF4FC3F7)
            "moon" -> Color(0xFFBDBDBD)
            "mars" -> Color(0xFFE57373)
            "jupiter" -> Color(0xFFD7CCC8)
            "saturn" -> Color(0xFFFFE082)
            "uranus" -> Color(0xFF81D4FA)
            "neptune" -> Color(0xFF5C6BC0)
            "milky_way" -> Color(0xFF1A237E)
            "iss" -> Color(0xFFE0E0E0)
            "black_hole" -> Color(0xFF212121)
            else -> Color(0xFF7986CB)
        }
    }

    val engine = remember { InspectorEngine.getInstance() }
    var cameraState by remember { mutableStateOf(engine.cameraRig.state) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090A0F))
            .testTag("space_museum_viewer_screen")
            .pointerInput(objectId) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val deltaYaw = pan.x * 0.5f
                    val deltaPitch = -pan.y * 0.5f
                    engine.cameraRig.orbit(deltaYaw, deltaPitch)
                    if (zoom != 1f) {
                        engine.cameraRig.zoom(1f / zoom)
                    }
                    engine.updateCameraFromRig()
                    cameraState = engine.cameraRig.state
                }
            }
    ) {
        // Fallback rendering — colored sphere/ellipsoid via Canvas, visible even if Filament fails
        // Uses cameraState for interactive orbit/zoom and spec.oblateness for accurate shape
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(
                x = size.width / 2f + cameraState.yawDeg * 2f,
                y = size.height / 2f + cameraState.pitchDeg * 2f
            )
            val baseRadius = minOf(size.width, size.height) * 0.35f
            val radius = (baseRadius / cameraState.radius.coerceIn(1f, 3f)) * 1.5f
            val oblateness = spec?.oblateness?.toFloat() ?: 0f
            val radiusY = radius * (1f - oblateness)

            // Draw glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(fallbackColor.copy(alpha = 0.8f), fallbackColor.copy(alpha = 0.1f), Color.Transparent),
                    center = center,
                    radius = radius * 1.5f
                ),
                radius = radius * 1.5f,
                center = center
            )
            // Draw main sphere/ellipsoid with gradient for 3D effect
            // For oblate objects like Jupiter/Saturn, use oval
            if (oblateness > 0.01f) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.3f), fallbackColor, fallbackColor.copy(alpha = 0.6f)),
                        center = Offset(center.x - radius * 0.3f, center.y - radiusY * 0.3f),
                        radius = radius
                    ),
                    topLeft = Offset(center.x - radius, center.y - radiusY),
                    size = androidx.compose.ui.geometry.Size(radius * 2f, radiusY * 2f)
                )
            } else {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.3f), fallbackColor, fallbackColor.copy(alpha = 0.6f)),
                        center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }
            // Draw highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = radius * 0.2f,
                center = Offset(center.x - radius * 0.3f, center.y - radiusY * 0.3f)
            )
            // Saturn rings indicator
            if (objectId == "saturn") {
                drawOval(
                    color = Color.White.copy(alpha = 0.15f),
                    topLeft = Offset(center.x - radius * 1.6f, center.y - radius * 0.15f),
                    size = androidx.compose.ui.geometry.Size(radius * 3.2f, radius * 0.3f)
                )
            }
        }

        // Filament surface — real 3D rendering on top of fallback
        FilamentView(
            objectId = objectId,
            modifier = Modifier.fillMaxSize(),
            tier = tier,
            sunState = sunState,
            onCameraChange = { newState -> cameraState = newState }
        )

        // Overlay UI — aware of system bars and bottom nav
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
        ) {
            // Top bar with status bar padding
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
                IconButton(
                    onClick = { onCredits?.invoke(objectId) },
                    modifier = Modifier.testTag("viewer_credits_button")
                ) {
                    Icon(Icons.Filled.Info, contentDescription = "Credits", tint = Color.White)
                }
            }

            Spacer(Modifier.weight(1f))

            // Debug overlay
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
                    text = "leaked: ${InspectorEngine.getInstance().getLeakedResourceCount()} (must be 0 after 10 recreations) | ${spec?.displayNameEn ?: objectId}",
                    color = if (InspectorEngine.getInstance().getLeakedResourceCount() == 0) Color.Green else Color.Red,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Bottom controls — aware of navigation bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                    )
                    .testTag("viewer_controls_placeholder"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isFa) "نور خورشید: آزیموت ${sunState.azimuthDeg} ارتفاع ${sunState.elevationDeg} EV ${sunState.exposureEV}" else "Sun: az ${sunState.azimuthDeg} el ${sunState.elevationDeg} EV ${sunState.exposureEV}",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = if (isFa) "بکشید برای چرخش، نیشگون برای زوم" else "Drag to orbit, pinch to zoom",
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
