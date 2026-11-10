package com.alijafari.red.astronomy.ui.obsidian

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.alijafari.red.astronomy.domain.ThemeMode

/**
 * Editorial Silhouette Backdrop — the single atmospheric mood every Obsidian screen sits over.
 *
 * A warm Alabaster Mist canvas with a soft-focus, slowly breathing Obsidian & deep Burgundy
 * silhouette aura rising through the centre, plus one subtle slate chromatic-aberration rim on
 * the left edge. Deliberately only three shapes: the reference photographs are human silhouettes
 * behind frosted glass, not a color dashboard.
 *
 * [breath] is optional so callers that already own a breathing animation (the Home canvas) can
 * drive it; when omitted the backdrop runs its own 9 s reverse tween.
 */
@Composable
fun EditorialSilhouetteBackdrop(
    themeMode: ThemeMode,
    isOpaqueCanvas: Boolean,
    modifier: Modifier = Modifier,
    breathOverride: Float? = null
) {
    val breath = if (breathOverride != null) {
        breathOverride
    } else {
        val transition = rememberInfiniteTransition(label = "obsidian_silhouette")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(9000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "obsidian_breath"
        )
        animated
    }

    val isLight = themeMode == ThemeMode.LIGHT
    val canvasColor = when (themeMode) {
        ThemeMode.LIGHT -> ObsidianAlabaster
        ThemeMode.DARK -> Color(0xFF0B0206)
        ThemeMode.WINE -> Color(0xFF140309)
    }

    // The silhouette is obsidian-with-burgundy always; only its weight changes with the mode.
    val inkStrength = if (isLight) 1f else 0.86f
    val auraAlpha = if (isLight) 1f else 0.82f

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // An opaque canvas when the app backdrop is off; a soft veil over the live sky / photograph
        // when it is on, so the Obsidian mood never fights the sky.
        drawRect(
            color = canvasColor.copy(
                alpha = if (isOpaqueCanvas) 1f else if (isLight) 0.72f else 0.55f
            )
        )

        // Upper obsidian head/shoulders silhouette.
        val headCenter = Offset(w * (0.52f + 0.03f * breath), h * 0.25f)
        val headRadius = w * 0.62f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF090407),
                    Color(0xFF1C0913).copy(alpha = 0.85f * auraAlpha),
                    Color(0xFF59122B).copy(alpha = 0.25f * auraAlpha),
                    Color.Transparent
                ),
                center = headCenter,
                radius = headRadius
            ),
            center = headCenter,
            radius = headRadius
        )

        // Lower torso crimson / obsidian aura bleed.
        val torsoCenter = Offset(w * (0.46f - 0.03f * breath), h * 0.68f)
        val torsoRadius = w * 0.85f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF120309),
                    Color(0xFF470820).copy(alpha = 0.78f * auraAlpha),
                    Color(0xFF8C1D46).copy(alpha = 0.22f * auraAlpha),
                    Color.Transparent
                ),
                center = torsoCenter,
                radius = torsoRadius
            ),
            center = torsoCenter,
            radius = torsoRadius
        )

        // Left-edge slate chromatic aberration rim — the only cool note in the palette.
        val rimCenter = Offset(w * 0.18f, h * 0.56f)
        val rimRadius = w * 0.45f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF4E86A3).copy(alpha = 0.28f * inkStrength * (if (isLight) 1f else 0.7f)),
                    Color.Transparent
                ),
                center = rimCenter,
                radius = rimRadius
            ),
            center = rimCenter,
            radius = rimRadius
        )
    }
}
