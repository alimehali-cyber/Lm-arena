package com.alijafari.red.astronomy.ui.aether

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import com.alijafari.red.astronomy.domain.ThemeMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ambient Breathing Background Mesh for Aether Telemetry UI:
 * - Paints `CanvasVoid` (`#08080C` in Dark, `#0A0206` in Wine, `#F3F3F8` in Light) when opaque.
 * - Uses `InfiniteTransition` (`tween(durationMillis = 9000, easing = LinearEasing)`) to drift
 *   two large radial color blobs along a smooth figure-8 Lissajous path so frosted optical
 *   cards subtly shift color refraction in real time.
 * - Enhances Android 13+ (`API 33+`) devices with a lightweight GPU-accelerated AGSL
 *   `RuntimeShader` atmospheric refraction veil, falling back seamlessly to Compose `Canvas`
 *   gradients on API 24–32.
 */
@Composable
fun AetherAmbientBackground(
    themeMode: ThemeMode,
    isOpaqueCanvas: Boolean,
    sunAltitudeDeg: Double = -18.0,
    modifier: Modifier = Modifier
) {
    val palette = AetherTheme.palette
    val infiniteTransition = rememberInfiniteTransition(label = "aetherAmbientMesh")
    val ambientPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aetherFigure8Drift"
    )

    val twilightWarmth = ((sunAltitudeDeg + 18.0) / 24.0).coerceIn(0.0, 1.0).toFloat()
    val agslHolder = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AetherAgslMeshHelper.createOrNull()
        } else {
            null
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        if (isOpaqueCanvas) {
            drawRect(color = palette.canvasVoid)
        }

        val alphaScale = if (isOpaqueCanvas) 1f else 0.55f
        val theta = (ambientPhase * 2.0 * PI).toFloat()

        // Smooth figure-8 Lissajous trajectories: x = cos(t), y = sin(2t) / 2
        val fig8X1 = cos(theta)
        val fig8Y1 = sin(2f * theta) * 0.5f

        val fig8X2 = -cos(theta + 0.8f)
        val fig8Y2 = -sin(2f * (theta + 0.8f)) * 0.5f

        val primaryAlpha = when (themeMode) {
            ThemeMode.WINE -> 0.50f
            ThemeMode.DARK -> 0.45f
            ThemeMode.LIGHT -> 0.26f
        } * alphaScale

        val secondaryAlpha = when (themeMode) {
            ThemeMode.WINE -> 0.42f
            ThemeMode.DARK -> 0.40f
            ThemeMode.LIGHT -> 0.24f
        } * alphaScale

        // Blob 1: Jewel Crimson-Plum figure-8 drift
        val center1 = Offset(
            x = w * (0.48f + 0.24f * fig8X1),
            y = h * (0.28f + 0.14f * fig8Y1)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.ambientBlobPrimary.copy(alpha = primaryAlpha),
                    Color.Transparent
                ),
                center = center1,
                radius = w * 0.78f
            ),
            radius = w * 0.78f,
            center = center1
        )

        // Blob 2: Deep Cerulean / Wine Merlot counter-drifting figure-8
        val center2 = Offset(
            x = w * (0.54f + 0.22f * fig8X2),
            y = h * (0.68f + 0.14f * fig8Y2)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.ambientBlobSecondary.copy(alpha = secondaryAlpha),
                    Color.Transparent
                ),
                center = center2,
                radius = w * 0.72f
            ),
            radius = w * 0.72f,
            center = center2
        )

        // Subtle horizon warmth responsive to solar depression
        if (twilightWarmth > 0.02f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AetherAmberMeshStart.copy(alpha = 0.14f * twilightWarmth * alphaScale),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.30f, h * 0.86f),
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = Offset(w * 0.30f, h * 0.86f)
            )
        }

        // GPU-accelerated AGSL subtle optical wave & grain veil on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && agslHolder != null) {
            val brush = AetherAgslMeshHelper.updateBrush(
                holder = agslHolder,
                width = w,
                height = h,
                timeRad = theta,
                intensity = if (palette.isDark) 0.06f * alphaScale else 0.03f * alphaScale
            )
            if (brush != null) {
                drawRect(brush = brush)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object AetherAgslMeshHelper {
    private const val AGSL_SOURCE = """
        uniform float2 resolution;
        uniform float time;
        uniform float intensity;

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / max(resolution, float2(1.0));
            float wave = sin(uv.x * 5.2 + time) * cos(uv.y * 4.1 - time * 0.8);
            float grain = fract(sin(dot(fragCoord * 0.35, float2(12.9898, 78.233))) * 43758.5453);
            float lum = clamp(0.5 + 0.35 * wave + 0.15 * (grain - 0.5), 0.0, 1.0);
            half3 tint = mix(half3(0.62, 0.04, 0.27), half3(0.18, 0.45, 0.72), half(uv.y));
            return half4(tint * half(lum * intensity), half(intensity * 0.65));
        }
    """

    class Holder(val shader: RuntimeShader, val brush: ShaderBrush)

    fun createOrNull(): Holder? = runCatching {
        val rs = RuntimeShader(AGSL_SOURCE)
        Holder(rs, ShaderBrush(rs))
    }.getOrNull()

    fun updateBrush(
        holder: Holder,
        width: Float,
        height: Float,
        timeRad: Float,
        intensity: Float
    ): ShaderBrush? = runCatching {
        holder.shader.setFloatUniform("resolution", width, height)
        holder.shader.setFloatUniform("time", timeRad)
        holder.shader.setFloatUniform("intensity", intensity)
        holder.brush
    }.getOrNull()
}
