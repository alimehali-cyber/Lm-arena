package com.alijafari.red.astronomy.ui.aether

import android.graphics.RuntimeShader
import android.os.Build
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * GPU-accelerated AGSL fragment shader (Android 13+ / API 33+) computing a smooth figure-8
 * dual-blob spatial mesh light field plus subtle frosted micro-grain noise at 120fps.
 */
internal const val AETHER_AGSL_MESH_SHADER = """
    uniform float2 uResolution;
    uniform float uPhase;
    uniform float4 uBaseColor;
    uniform float4 uBlob1Color;
    uniform float4 uBlob2Color;
    uniform float uGrainAlpha;

    float aetherHash(float2 p) {
        return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / max(uResolution, float2(1.0, 1.0));
        float angle = uPhase * 6.2831853;

        // Figure-8 Lissajous drift path for Crimson & Deep Sapphire mesh lights
        float2 c1 = float2(
            0.22 + 0.14 * sin(angle),
            0.28 + 0.11 * sin(angle * 2.0)
        );
        float2 c2 = float2(
            0.78 - 0.15 * sin(angle),
            0.66 + 0.12 * cos(angle * 2.0)
        );

        float aspect = uResolution.x / max(uResolution.y, 1.0);
        float2 d1 = (uv - c1) * float2(aspect, 1.0);
        float2 d2 = (uv - c2) * float2(aspect, 1.0);

        float w1 = exp(-dot(d1, d1) * 3.1) * uBlob1Color.a;
        float w2 = exp(-dot(d2, d2) * 3.4) * uBlob2Color.a;

        float3 rgb = uBaseColor.rgb;
        rgb = mix(rgb, uBlob1Color.rgb, clamp(w1, 0.0, 1.0));
        rgb = mix(rgb, uBlob2Color.rgb, clamp(w2, 0.0, 1.0));

        float grain = (aetherHash(fragCoord) - 0.5) * uGrainAlpha;
        rgb = clamp(rgb + float3(grain, grain, grain), 0.0, 1.0);

        return half4(half(rgb.r), half(rgb.g), half(rgb.b), half(uBaseColor.a));
    }
"""

/**
 * Ambient Breathing Spatial Mesh Background (Section 4.1, Section 5 Component #2, Section 7):
 * - Driven by a 9000ms [rememberInfiniteTransition] figure-8 drift path.
 * - Uses GPU-accelerated AGSL [RuntimeShader] on Android 13+ with seamless pure-Compose
 *   [Brush.radialGradient] fallback on API 24-32.
 * - Records into [hazeState] via `Modifier.haze` so frosted spatial cards refract the live mesh.
 */
@Composable
fun AetherAmbientBackground(
    modifier: Modifier = Modifier,
    transparentBase: Boolean = false,
    hazeState: HazeState? = AetherTheme.hazeState
) {
    val palette = AetherTheme.palette
    val infinite = rememberInfiniteTransition(label = "aetherAmbientMesh")
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    val agslPair = remember(transparentBase) {
        if (!transparentBase && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                val shader = RuntimeShader(AETHER_AGSL_MESH_SHADER)
                shader to ShaderBrush(shader)
            }.getOrNull()
        } else {
            null
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (hazeState != null) {
                    Modifier.haze(state = hazeState)
                } else {
                    Modifier
                }
            )
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val blob1Alpha = if (transparentBase) 0.28f else if (palette.isDark) 0.45f else 0.34f
        val blob2Alpha = if (transparentBase) 0.24f else if (palette.isDark) 0.36f else 0.30f

        var drawnByShader = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && agslPair != null) {
            drawnByShader = runCatching {
                val (shader, brush) = agslPair
                shader.setFloatUniform("uResolution", w, h)
                shader.setFloatUniform("uPhase", drift)
                shader.setFloatUniform(
                    "uBaseColor",
                    palette.canvasVoid.red,
                    palette.canvasVoid.green,
                    palette.canvasVoid.blue,
                    1f
                )
                shader.setFloatUniform(
                    "uBlob1Color",
                    palette.ambientBlobPrimary.red,
                    palette.ambientBlobPrimary.green,
                    palette.ambientBlobPrimary.blue,
                    blob1Alpha
                )
                shader.setFloatUniform(
                    "uBlob2Color",
                    palette.ambientBlobSecondary.red,
                    palette.ambientBlobSecondary.green,
                    palette.ambientBlobSecondary.blue,
                    blob2Alpha
                )
                shader.setFloatUniform("uGrainAlpha", if (palette.isDark) 0.024f else 0.015f)
                drawRect(brush = brush)
                true
            }.getOrDefault(false)
        }

        if (!drawnByShader) {
            if (!transparentBase) {
                drawRect(color = palette.canvasVoid)
            } else {
                drawRect(
                    color = palette.canvasVoid.copy(
                        alpha = if (palette.isDark) 0.28f else 0.22f
                    )
                )
            }

            // Figure-8 Lissajous drift path for the two ambient mesh light blobs
            val theta = drift * 2.0 * PI
            val c1 = Offset(
                x = w * (0.20f + 0.14f * sin(theta).toFloat()),
                y = h * (0.28f + 0.10f * sin(theta * 2.0).toFloat())
            )
            val c2 = Offset(
                x = w * (0.80f - 0.15f * sin(theta).toFloat()),
                y = h * (0.65f + 0.12f * cos(theta * 2.0).toFloat())
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.ambientBlobPrimary.copy(alpha = blob1Alpha),
                        Color.Transparent
                    ),
                    center = c1,
                    radius = w * 0.90f
                ),
                radius = w * 0.90f,
                center = c1
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.ambientBlobSecondary.copy(alpha = blob2Alpha),
                        Color.Transparent
                    ),
                    center = c2,
                    radius = w * 0.85f
                ),
                radius = w * 0.85f,
                center = c2
            )
        }
    }
}
