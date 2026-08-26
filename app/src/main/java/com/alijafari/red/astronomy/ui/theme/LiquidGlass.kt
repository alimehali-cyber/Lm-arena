package com.alijafari.red.astronomy.ui.theme

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

/**
 * RED Liquid Glass Optical Specification
 *
 * Real Liquid Glass material specification based on Kyant0 Backdrop architecture:
 * - Real-time background pixel sampling & optical refraction
 * - Spatial vibrancy enhancement
 * - Configurable Gaussian blur diffusion
 * - Convex / concave lens distortion with optional chromatic dispersion
 * - Ambient highlight, external ambient shadow, and internal bevel shadow
 */
@Immutable
data class LiquidGlassStyle(
    val blurRadius: Dp = 8.dp,
    val refractionHeight: Dp = 24.dp,
    val refractionAmount: Dp = 24.dp,
    val chromaticAberration: Boolean = false,
    val hasHighlight: Boolean = true,
    val shadowRadius: Dp = 12.dp,
    val innerShadowRadius: Dp = 2.dp
)

object LiquidGlassDefaults {
    val NavigationBar = LiquidGlassStyle(
        blurRadius = 8.dp,
        refractionHeight = 24.dp,
        refractionAmount = 24.dp,
        chromaticAberration = false,
        hasHighlight = true,
        shadowRadius = 12.dp,
        innerShadowRadius = 2.dp
    )

    val ActiveTab = LiquidGlassStyle(
        blurRadius = 0.dp,
        refractionHeight = 10.dp,
        refractionAmount = 14.dp,
        chromaticAberration = true,
        hasHighlight = true,
        shadowRadius = 0.dp,
        innerShadowRadius = 1.5.dp
    )

    val Card = LiquidGlassStyle(
        blurRadius = 6.dp,
        refractionHeight = 16.dp,
        refractionAmount = 16.dp,
        chromaticAberration = false,
        hasHighlight = true,
        shadowRadius = 8.dp,
        innerShadowRadius = 1.5.dp
    )

    val Modal = LiquidGlassStyle(
        blurRadius = 12.dp,
        refractionHeight = 28.dp,
        refractionAmount = 28.dp,
        chromaticAberration = false,
        hasHighlight = true,
        shadowRadius = 16.dp,
        innerShadowRadius = 3.dp
    )
}

/**
 * Reusable Liquid Glass Modifier.
 *
 * Applies real-time backdrop sampling, lens refraction, vibrancy, blur,
 * highlights, and shadows to any Compose component.
 */
fun Modifier.liquidGlass(
    backdrop: Backdrop,
    shape: Shape = RoundedCornerShape(32.dp),
    style: LiquidGlassStyle = LiquidGlassDefaults.NavigationBar
): Modifier = this
    .drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            if (style.blurRadius > 0.dp) {
                blur(style.blurRadius.toPx())
            }
            if (style.refractionHeight > 0.dp || style.refractionAmount > 0.dp) {
                lens(
                    refractionHeight = style.refractionHeight.toPx(),
                    refractionAmount = style.refractionAmount.toPx(),
                    chromaticAberration = style.chromaticAberration
                )
            }
        },
        highlight = if (style.hasHighlight) { { Highlight.Ambient } } else null,
        shadow = if (style.shadowRadius > 0.dp) { { Shadow(radius = style.shadowRadius) } } else null,
        innerShadow = if (style.innerShadowRadius > 0.dp) { { InnerShadow(radius = style.innerShadowRadius) } } else null
    )
    .clip(shape)

/**
 * Reusable Native Liquid Glass Surface Composable.
 */
@Composable
fun LiquidGlassSurface(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    style: LiquidGlassStyle = LiquidGlassDefaults.NavigationBar,
    shape: Shape = RoundedCornerShape(32.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "LiquidGlassPress"
    )

    Box(
        modifier = modifier
            .scale(pressScale)
            .liquidGlass(
                backdrop = backdrop,
                shape = shape,
                style = style
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










