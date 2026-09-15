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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.zig.museum.core.engine.FilamentView
import com.zig.museum.core.engine.InspectorEngine
import com.zig.museum.core.engine.SunState
import com.zig.museum.core.model.ObjectRegistry
import kotlinx.coroutines.delay

@Composable
fun SpaceMuseumViewerScreen(
    objectId: String,
    onBack: () -> Unit,
    onCredits: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    isFa: Boolean = false
) {
    DisposableEffect(Unit) {
        try {
            val clazz = Class.forName("com.zig.gravity.ui.ImmersiveScreenState")
            val instance = clazz.getField("INSTANCE").get(null)
            val activeField = instance.javaClass.getDeclaredField("active")
            activeField.isAccessible = true
            activeField.set(instance, true)
        } catch (e: Exception) {}
        onDispose {
            try {
                val clazz = Class.forName("com.zig.gravity.ui.ImmersiveScreenState")
                val instance = clazz.getField("INSTANCE").get(null)
                val activeField = instance.javaClass.getDeclaredField("active")
                activeField.isAccessible = true
                activeField.set(instance, false)
            } catch (e: Exception) {}
        }
    }

    val spec = ObjectRegistry.byId(objectId)
    var sunState by remember { mutableStateOf(SunState()) }
    var tier by remember { mutableStateOf(0) }

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
    var hasRenderable by remember { mutableStateOf(engine.hasActiveRenderable()) }
    var hasMaterial by remember { mutableStateOf(engine.hasMaterial()) }
    var lastError by remember { mutableStateOf(engine.getLastError()) }

    LaunchedEffect(objectId) {
        // Poll for renderable becoming available after FilamentView loads object
        repeat(20) {
            hasRenderable = engine.hasActiveRenderable()
            hasMaterial = engine.hasMaterial()
            lastError = engine.getLastError()
            if (hasRenderable) return@repeat
            delay(250)
        }
    }

    // Continuous polling every 500ms to keep debug overlay fresh
    LaunchedEffect(objectId) {
        while (true) {
            delay(500)
            hasRenderable = engine.hasActiveRenderable()
            hasMaterial = engine.hasMaterial()
            lastError = engine.getLastError()
        }
    }

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
                    if (zoom != 1f) engine.cameraRig.zoom(1f / zoom)
                    engine.updateCameraFromRig()
                    cameraState = engine.cameraRig.state
                    hasRenderable = engine.hasActiveRenderable()
                    hasMaterial = engine.hasMaterial()
                    lastError = engine.getLastError()
                }
            }
    ) {
        FilamentView(
            objectId = objectId,
            modifier = Modifier.fillMaxSize(),
            tier = tier,
            sunState = sunState,
            onCameraChange = { newState ->
                cameraState = newState
                hasRenderable = engine.hasActiveRenderable()
                hasMaterial = engine.hasMaterial()
                lastError = engine.getLastError()
            }
        )

        // Sophisticated fallback Canvas — shows when Filament fails, but now with better 3D illusion
        // When hasRenderable=true, only subtle glow behind real 3D
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(
                x = size.width / 2f + cameraState.yawDeg * 2.5f,
                y = size.height / 2f + cameraState.pitchDeg * 2.5f
            )
            val baseRadius = minOf(size.width, size.height) * 0.38f
            val radius = (baseRadius / cameraState.radius.coerceIn(1f, 3f)) * 1.5f
            val oblateness = spec?.oblateness?.toFloat() ?: 0f
            val radiusY = radius * (1f - oblateness)

            // Subtle glow behind - very subtle when hasRenderable true to avoid blue screen, strong when fallback
            if (hasRenderable) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(fallbackColor.copy(alpha = 0.08f), Color.Transparent),
                        center = center,
                        radius = radius * 1.5f
                    ),
                    radius = radius * 1.5f,
                    center = center
                )
            } else {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(fallbackColor.copy(alpha = 0.9f), fallbackColor.copy(alpha = 0.3f), Color.Transparent),
                        center = center,
                        radius = radius * 2.2f
                    ),
                    radius = radius * 2.2f,
                    center = center
                )
            }

            if (!hasRenderable) {
                // Improved 3D illusion fallback — not just simple circle
                // Multiple layers for depth
                // Outer shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.4f),
                    radius = radius * 1.05f,
                    center = Offset(center.x + radius * 0.08f, center.y + radius * 0.08f)
                )

                if (oblateness > 0.01f) {
                    // Oblate spheroid with 3D shading
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.5f), fallbackColor, fallbackColor.copy(alpha = 0.4f), Color.Black.copy(alpha = 0.6f)),
                            center = Offset(center.x - radius * 0.35f, center.y - radiusY * 0.35f),
                            radius = radius * 1.2f
                        ),
                        topLeft = Offset(center.x - radius, center.y - radiusY),
                        size = Size(radius * 2f, radiusY * 2f)
                    )
                    // Equatorial band for gas giants
                    if (objectId == "jupiter" || objectId == "saturn") {
                        drawOval(
                            brush = Brush.linearGradient(
                                colors = listOf(fallbackColor.copy(alpha = 0.3f), Color.White.copy(alpha = 0.2f), fallbackColor.copy(alpha = 0.3f)),
                                start = Offset(center.x - radius, center.y - radiusY * 0.2f),
                                end = Offset(center.x + radius, center.y + radiusY * 0.2f)
                            ),
                            topLeft = Offset(center.x - radius * 0.9f, center.y - radiusY * 0.25f),
                            size = Size(radius * 1.8f, radiusY * 0.5f)
                        )
                    }
                } else {
                    // Sphere with proper Phong-like shading
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.6f), fallbackColor, fallbackColor.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.7f)),
                            center = Offset(center.x - radius * 0.4f, center.y - radius * 0.4f),
                            radius = radius * 1.1f
                        ),
                        radius = radius,
                        center = center
                    )
                    // Terminator shadow for moon/earth
                    if (objectId == "moon" || objectId == "earth" || objectId == "mars") {
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.25f),
                            radius = radius * 0.9f,
                            center = Offset(center.x + radius * 0.3f, center.y + radius * 0.1f)
                        )
                    }
                }

                // Highlight
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = radius * 0.18f,
                    center = Offset(center.x - radius * 0.32f, center.y - radiusY * 0.32f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = radius * 0.08f,
                    center = Offset(center.x - radius * 0.38f, center.y - radiusY * 0.38f)
                )

                // Saturn rings with gap
                if (objectId == "saturn") {
                    drawOval(
                        color = Color.White.copy(alpha = 0.25f),
                        topLeft = Offset(center.x - radius * 2.0f, center.y - radius * 0.22f),
                        size = Size(radius * 4.0f, radius * 0.44f),
                        style = Stroke(width = radius * 0.08f)
                    )
                    drawOval(
                        color = Color(0xFF090A0F).copy(alpha = 0.7f),
                        topLeft = Offset(center.x - radius * 1.0f, center.y - radius * 0.09f),
                        size = Size(radius * 2.0f, radius * 0.18f)
                    )
                    drawOval(
                        color = Color.White.copy(alpha = 0.15f),
                        topLeft = Offset(center.x - radius * 2.0f, center.y - radius * 0.22f),
                        size = Size(radius * 4.0f, radius * 0.44f),
                        style = Stroke(width = radius * 0.02f)
                    )
                }

                // Earth continents hint
                if (objectId == "earth") {
                    drawCircle(
                        color = Color(0xFF2E7D32).copy(alpha = 0.4f),
                        radius = radius * 0.25f,
                        center = Offset(center.x + radius * 0.1f, center.y - radius * 0.1f)
                    )
                    drawCircle(
                        color = Color(0xFF2E7D32).copy(alpha = 0.3f),
                        radius = radius * 0.18f,
                        center = Offset(center.x - radius * 0.2f, center.y + radius * 0.15f)
                    )
                }

                // Black hole lensing hint
                if (objectId == "black_hole") {
                    drawCircle(
                        color = Color.Black,
                        radius = radius * 0.5f,
                        center = center
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFF9800).copy(alpha = 0.8f), Color(0xFFFF5722).copy(alpha = 0.3f), Color.Transparent),
                            center = center,
                            radius = radius * 1.3f
                        ),
                        radius = radius * 1.3f,
                        center = center,
                        style = Stroke(width = radius * 0.15f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
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
                IconButton(onClick = { onCredits?.invoke(objectId) }, modifier = Modifier.testTag("viewer_credits_button")) {
                    Icon(Icons.Filled.Info, contentDescription = "Credits", tint = Color.White)
                }
            }

            Spacer(Modifier.weight(1f))

            val instrumentation = remember { InspectorEngine.getInstance().instrumentation }
            val debugData = instrumentation.toDebugOverlay()
            Column(
                modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.7f)).padding(8.dp).testTag("debug_overlay")
            ) {
                Text("fps: ${"%.1f".format(debugData.fps)} p50: ${"%.1f".format(debugData.p50Ms)}ms p95: ${"%.1f".format(debugData.p95Ms)}ms tier: ${debugData.tier} camR: ${"%.2f".format(cameraState.radius)}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                // Foundational Rebuild Phase 0.1: this used to unconditionally print a hardcoded
                // "tiles: 1 bytes: 4194304 uploads: 0 evict: 0" literal regardless of whether any
                // tile/asset system was involved (see docs/audit/MILESTONE_AUDIT.md, M3, and
                // InspectorEngine.loadEllipsoidObject()'s removed hardcoded-counter line). No tile
                // store is wired into the render path yet, so this must say so honestly instead of
                // showing a plausible-looking but fake number.
                Text(
                    if (debugData.tileStoreActive) "tiles: ${debugData.residentTiles} bytes: ${debugData.residentBytes} uploads: ${debugData.uploadsPerFrame} evict: ${debugData.evictions}"
                    else "tile store: not active",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
                // Foundational Rebuild Phase 0.1: real render-loop honesty counters, replacing the
                // previous fps-only signal that could not distinguish "beginFrame() succeeded and a
                // real frame was presented" from "beginFrame() returned false / threw, nothing was
                // drawn, and doFrame() just returned quickly" -- see
                // docs/audit/MILESTONE_AUDIT.md M11 for the 899-1156fps evidence this conflation
                // produced. rl.summary() reports doFrame()/beginFrame()/render()/endFrame() call
                // counts with their real true/false/threw breakdown, plus the SurfaceView's actual
                // measured viewport size at last setViewport() call.
                Text(
                    "render loop: ${debugData.renderLoop.summary()}",
                    color = if (debugData.renderLoop.hasPresentedAtLeastOneFrame()) Color.White else Color.Red,
                    style = MaterialTheme.typography.labelSmall
                )

                Text(
                    "Filament: ${if (hasRenderable) "YES 3D" else "NO fallback"} material:${if (hasMaterial) "YES" else "NO"} id:$objectId | ${spec?.displayNameEn ?: objectId}",
                    color = if (hasRenderable && hasMaterial) Color.Green else Color.Yellow,
                    style = MaterialTheme.typography.labelSmall
                )
                if (lastError.isNotEmpty()) {
                    Text("err: $lastError", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                }
                Text(
                    if (isFa) "بکشید برای چرخش، نیشگون برای زوم" else "Drag orbit, pinch zoom — ${if (hasRenderable) "Filament 3D lit sphere" else "Canvas fallback"} yaw:${"%.0f".format(cameraState.yawDeg)} pitch:${"%.0f".format(cameraState.pitchDeg)}",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f))
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
                    .testTag("viewer_controls_placeholder"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    if (isFa) "نور خورشید: آزیموت ${sunState.azimuthDeg} ارتفاع ${sunState.elevationDeg} EV ${sunState.exposureEV}" else "Sun: az ${sunState.azimuthDeg} el ${sunState.elevationDeg} EV ${sunState.exposureEV}",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(spec?.let { if (isFa) it.dataCeilingTextFa else it.dataCeilingTextEn } ?: "", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
