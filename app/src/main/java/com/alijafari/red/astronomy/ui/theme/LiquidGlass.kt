package com.alijafari.red.astronomy.ui.theme

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * RED Liquid Glass Optical Specification & Tokens
 *
 * Modeled on physical optical principles:
 * - High optical transmission (transparency with refractive presence)
 * - Index of Refraction (IOR ~ 1.15) perimeter bounce and specular highlights
 * - Sub-pixel chromatic dispersion / grazing edge highlight
 * - Vertical slab transmission gradient (attenuation and internal reflection)
 * - Dynamic backdrop blur with graceful degradation
 * - Living astronomical light orientation when in Celestial/Dynamic Silk mode
 */
@Immutable
data class LiquidGlassStyle(
    val transmission: Float = 0.92f,          // 0.0 (opaque) to 1.0 (pure optical pass-through)
    val ior: Float = 1.15f,                   // Index of refraction ratio
    val blurRadius: Dp = 20.dp,               // Backdrop blur intensity
    val thickness: Dp = 8.dp,                 // Optical slab depth
    val chromaticAberration: Float = 0.08f,    // Restrained prismatic dispersion at grazing edges
    val surfaceRoughness: Float = 0.0f,       // Optical smoothness
    val baseTint: Color? = null,              // Optional explicit tint override
    val elevation: Dp = RedElevation.floatingNav
)

object LiquidGlassDefaults {
    val NavigationBar = LiquidGlassStyle(
        transmission = 0.88f,
        ior = 1.15f,
        blurRadius = 24.dp,
        thickness = 10.dp,
        chromaticAberration = 0.06f,
        surfaceRoughness = 0.0f,
        elevation = 6.dp
    )

    val Card = LiquidGlassStyle(
        transmission = 0.90f,
        ior = 1.12f,
        blurRadius = 16.dp,
        thickness = 6.dp,
        chromaticAberration = 0.04f,
        surfaceRoughness = 0.0f,
        elevation = 2.dp
    )

    val Modal = LiquidGlassStyle(
        transmission = 0.85f,
        ior = 1.18f,
        blurRadius = 28.dp,
        thickness = 12.dp,
        chromaticAberration = 0.09f,
        surfaceRoughness = 0.0f,
        elevation = 8.dp
    )
}

/**
 * Reusable Liquid Glass Modifier.
 *
 * Applies multi-layer optical glass rendering:
 * 1. Native Backdrop Blur (RenderEffect on Android 12+, with graceful fallback).
 * 2. Translucent Optical Slab Gradient (dynamic light transmission based on theme).
 * 3. Dual-Rim Specular Boundary (grazing highlight + IOR refractive edge).
 * 4. Micro Chromatic Dispersion along top specular perimeter.
 * 5. Living Celestial Alignment (adapts specular angle to Sun/Moon when available).
 */
fun Modifier.liquidGlass(
    style: LiquidGlassStyle = LiquidGlassDefaults.NavigationBar,
    shape: Shape = RoundedCornerShape(RedCornerRadius.xxl),
    isDark: Boolean = true,
    celestialLighting: CelestialLighting? = null
): Modifier = this
    .then(
        if (style.elevation > 0.dp) {
            Modifier.shadow(
                elevation = style.elevation,
                shape = shape,
                clip = false,
                ambientColor = if (isDark) Color(0x66000000) else Color(0x1F000000),
                spotColor = if (isDark) Color(0x80000000) else Color(0x29000000)
            )
        } else {
            Modifier
        }
    )
    .clip(shape)
    .then(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && style.blurRadius > 0.dp) {
            Modifier.blur(radius = style.blurRadius)
        } else {
            Modifier
        }
    )
    .drawWithCache {
        val w = size.width
        val h = size.height

        // 1. Compute Theme-Aware Optical Slab Colors
        val (slabTopColor, slabBottomColor, rimHighlightColor, rimShadowColor) = if (isDark) {
            val base = style.baseTint ?: Color(0xFF10121A)
            val topAlpha = (1f - style.transmission) * 0.75f + 0.12f
            val bottomAlpha = (1f - style.transmission) * 1.10f + 0.22f
            Quadruple(
                base.copy(alpha = topAlpha),
                base.copy(alpha = bottomAlpha),
                Color(0x52FFFFFF), // 32% crisp white specular top edge
                Color(0x14000000)  // Subtle base ambient rim
            )
        } else {
            val base = style.baseTint ?: Color(0xFFFFFFFF)
            val topAlpha = (1f - style.transmission) * 0.65f + 0.55f
            val bottomAlpha = (1f - style.transmission) * 0.95f + 0.72f
            Quadruple(
                base.copy(alpha = topAlpha),
                base.copy(alpha = bottomAlpha),
                Color(0x99FFFFFF), // Crisp pure white reflection in light mode
                Color(0x1F000000)  // 12% dark shadow rim
            )
        }

        // 2. Optical Slab Transmission Brush
        val slabBrush = Brush.verticalGradient(
            0.0f to slabTopColor,
            0.6f to slabTopColor.copy(alpha = (slabTopColor.alpha + slabBottomColor.alpha) * 0.5f),
            1.0f to slabBottomColor
        )

        // 3. Specular Direction (Driven by living astronomical light if present, otherwise top-angled)
        val (lightStartX, lightStartY, lightEndX, lightEndY) = if (celestialLighting != null && celestialLighting.sheenIntensity > 0.01f) {
            val dirX = celestialLighting.sheenDirection.x
            val dirY = celestialLighting.sheenDirection.y
            Quadruple(
                (0.5f - dirX * 0.5f) * w,
                (0.5f - dirY * 0.5f) * h,
                (0.5f + dirX * 0.5f) * w,
                (0.5f + dirY * 0.5f) * h
            )
        } else {
            Quadruple(0f, 0f, w * 0.4f, h)
        }

        // 4. Refraction Rim Gradient (Outer Boundary)
        val outerRimBrush = Brush.linearGradient(
            0.0f to rimHighlightColor,
            0.45f to rimHighlightColor.copy(alpha = rimHighlightColor.alpha * 0.35f),
            0.85f to Color.Transparent,
            1.0f to rimShadowColor,
            start = Offset(lightStartX, lightStartY),
            end = Offset(lightEndX, lightEndY)
        )

        // 5. Chromatic Dispersion Highlights (Restrained sub-pixel optical prismatic hint)
        val chromaticBrush = if (style.chromaticAberration > 0.01f) {
            val dispersionAlpha = (style.chromaticAberration * 0.6f).coerceIn(0.02f, 0.15f)
            Brush.horizontalGradient(
                0.0f to Color.Transparent,
                0.2f to Color(0xFF38BDF8).copy(alpha = dispersionAlpha),      // Cyan / Ice Starlight
                0.5f to Color(0xFFFFFFFF).copy(alpha = dispersionAlpha * 1.5f),// Core Specular
                0.8f to Color(0xFFF43F5E).copy(alpha = dispersionAlpha * 0.7f),// Subtle Rose / Vermilion fringe
                1.0f to Color.Transparent
            )
        } else {
            null
        }

        val strokeWidthPx = 1.dp.toPx()
        val innerStrokePx = 0.75.dp.toPx()

        onDrawBehind {
            // A. Base Optical Glass Slab Fill
            drawRect(brush = slabBrush)

            // B. Refractive Surface Specular Shimmer (Internal reflection)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        rimHighlightColor.copy(alpha = rimHighlightColor.alpha * 0.25f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, 0f),
                    radius = w * 0.65f
                )
            )

            // C. Outer Refraction Rim Stroke
            drawRect(
                brush = outerRimBrush,
                style = Stroke(width = strokeWidthPx)
            )

            // D. Chromatic Dispersion Grazing Edge (Top specular line)
            if (chromaticBrush != null) {
                drawLine(
                    brush = chromaticBrush,
                    start = Offset(w * 0.1f, strokeWidthPx * 0.5f),
                    end = Offset(w * 0.9f, strokeWidthPx * 0.5f),
                    strokeWidth = innerStrokePx
                )
            }
        }
    }

/**
 * Native Reusable Liquid Glass Container.
 *
 * Provides optical glass physics, interactive tactile response, and theme awareness.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    style: LiquidGlassStyle = LiquidGlassDefaults.NavigationBar,
    shape: Shape = RoundedCornerShape(RedCornerRadius.xxl),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDark = RedTheme.colors.isDark
    val celestialLighting = LocalCelestialLighting.current

    // Tactile compression and optical flare on touch
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.982f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "LiquidGlassPress"
    )

    Box(
        modifier = modifier
            .scale(pressScale)
            .liquidGlass(
                style = style,
                shape = shape,
                isDark = isDark,
                celestialLighting = celestialLighting
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        content()
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
