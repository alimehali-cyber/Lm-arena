package com.alijafari.red.astronomy.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ============================================================================
 * THE CARD SURFACE
 * ============================================================================
 *
 * Every panel in this app — a tool card, a dialog body, the floating bar, the two Home hero cards — is
 * drawn by this one composable: the theme's own fill, a hairline border, an optional elevation, the
 * call site's corner geometry, and the press feedback every tappable surface shares.
 *
 * It is deliberately a *flat* surface. The material is the theme's: fills taken from the token set,
 * which is the same token set the app's text colours were contrast-repaired against. Nothing here
 * samples the pixels behind the app, so a card is cheap to draw, its ink is known before it is
 * painted, and it looks the same on every device regardless of what is behind it.
 *
 * ## Why the ink still gets resolved here
 *
 * A card is allowed to hand in its own [color] and [tint] — a scrim over a photograph for the sky
 * button, a darker sheet for a modal. Those are not theme tokens, so the theme cannot have repaired the
 * ink for them. Resolving the fill once, here, and handing the result down through [LocalRedColors]
 * means every child that reads `RedTheme.colors.textPrimary` inherits an ink chosen against the surface
 * it is actually drawn on, instead of against the screen background it happens to sit over.
 */
@Immutable
data class SurfaceInk(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

/**
 * Relative luminance of a colour, from the same weights the ink decisions have always used.
 *
 * Public because the AR and composited surfaces make the same call for their own fixed chrome.
 */
fun inkedLuminance(color: Color): Float =
    0.299f * color.red + 0.587f * color.green + 0.114f * color.blue

/** The readable ink for a given background — near-black or near-white, never a mid grey that dies. */
fun readableInkOn(background: Color): Color =
    if (inkedLuminance(background) > 0.55f) Color(0xFF080A12) else Color(0xFFF8FAFF)

/**
 * The fill a surface ends up with: [base], with [tint] laid over it when a call site asked for one.
 *
 * Alphas are composited rather than averaged, so a translucent scrim over a translucent sheet resolves
 * to the colour the eye will see, which is the colour the ink decision below is made against.
 */
internal fun resolvedFill(base: Color, tint: Color): Color {
    if (tint.alpha <= 0.001f) return base.copy(alpha = 1f)
    val outAlpha = tint.alpha + base.alpha * (1f - tint.alpha)
    if (outAlpha <= 0.001f) return Color.Transparent
    fun channel(t: Float, b: Float) = (t * tint.alpha + b * base.alpha * (1f - tint.alpha)) / outAlpha
    return Color(
        red = channel(tint.red, base.red),
        green = channel(tint.green, base.green),
        blue = channel(tint.blue, base.blue),
        alpha = outAlpha
    )
}

/** The three text inks for a surface filled with [base] (and optionally [tint]). */
fun surfaceInkFor(base: Color, tint: Color = Color.Transparent): SurfaceInk {
    val ink = readableInkOn(resolvedFill(base, tint))
    return SurfaceInk(
        primary = ink,
        secondary = ink.copy(alpha = 0.74f),
        tertiary = ink.copy(alpha = 0.56f)
    )
}

/**
 * A card: fill, border, elevation, corner geometry, press feedback — and the ink its children should use.
 *
 * [color] is the surface itself. [tint] is an optional translucent layer over it, for the few places
 * that want a scrim on top of a base colour (the sky buttons do this to stay readable over a photograph).
 * Both are drawn in that order, under the content.
 */
@Composable
fun RedSurface(
    modifier: Modifier = Modifier,
    shape: Shape = squircleShape(RedCornerRadius.window),
    color: Color = RedTheme.colors.surfaceElevated,
    border: BorderStroke? = BorderStroke(1.dp, RedTheme.colors.border),
    elevation: Dp = RedElevation.floating,
    tint: Color = Color.Transparent,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentColor: Color = Color.Unspecified,
    content: @Composable () -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "RedSurfacePress"
    )

    val ink = surfaceInkFor(color, tint)
    val surfaceColors = RedTheme.colors.copy(
        textPrimary = ink.primary,
        textSecondary = ink.secondary,
        textTertiary = ink.tertiary
    )

    // propagateMinConstraints is what keeps a card that is given a width passing that width down, so a
    // Row with SpaceBetween inside it still spreads instead of collapsing to the left.
    Box(
        propagateMinConstraints = true,
        modifier = modifier
            .scale(pressScale)
            .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape) else Modifier)
            .background(color, shape)
            .then(if (tint.alpha > 0.001f) Modifier.background(tint, shape) else Modifier)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clip(shape)
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
        CompositionLocalProvider(
            LocalRedColors provides surfaceColors,
            LocalContentColor provides if (contentColor == Color.Unspecified) {
                ink.primary
            } else {
                contentColor
            }
        ) {
            content()
        }
    }
}

/**
 * The fill-only form of [RedSurface], for the handful of small chrome elements that carry their own
 * layout and only need the material: same fill, same border, same clip.
 */
fun Modifier.solidSurface(
    shape: Shape,
    color: Color,
    border: BorderStroke? = null
): Modifier = this
    .background(color, shape)
    .then(if (border != null) Modifier.border(border, shape) else Modifier)
    .clip(shape)
