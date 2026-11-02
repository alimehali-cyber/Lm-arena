package com.alijafari.red.astronomy.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * ============================================================================
 * THE AMBIENT WORLD
 * ============================================================================
 *
 * The soft, slowly breathing mesh the Spatial Glass interface floats over: a night-coloured base, five
 * wide colour fields orbiting at different speeds, a faint dot lattice, and a vignette that keeps the
 * edges of the screen quiet.
 *
 * This is the layer that makes the panels read as glass. A translucent sheet over a *flat* colour is
 * indistinguishable from a slightly lighter sheet; a translucent sheet over a moving mesh picks up a
 * different colour at every edge and changes as the mesh drifts — which is exactly the ambient,
 * ethereal quality the style is built on. Nothing here samples anything: the world is drawn first, the
 * cards are drawn over it, and the glass composites them on the GPU for free.
 *
 * ## Cost
 *
 * Everything that depends on the surface size — the base gradient, the dot lattice's point list — is
 * built once in [drawWithCache] and reused for every frame. The only per-frame work is one animated
 * float and six draw calls: five radial gradients for the drifting lights and one vignette. The
 * animation invalidates the *draw phase only*; composition and layout are never touched, and the
 * transition is linear and endless, so nothing allocates per frame.
 *
 * [animated] is false in previews and in the settings thumbnails, which read a single still frame.
 */
@Composable
fun SpatialAmbientBackdrop(
    spec: SpatialGlassSpec,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    showDots: Boolean = true
) {
    if (!spec.enabled) return

    val transition = rememberInfiniteTransition(label = "SpatialAmbientDrift")

    // Both channels are always created (an infinite transition costs nothing until it is read); the
    // still one is what a preview or a thumbnail reads, so only one of them is ever observed.
    val stillPhase = remember { mutableFloatStateOf(0.12f) }
    val driftingPhase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpatialAmbientPhase"
    )
    val phaseState = if (animated) driftingPhase else stillPhase

    Spacer(
        modifier = modifier.drawWithCache {
            val base = Brush.verticalGradient(
                colors = listOf(spec.ambientBase, spec.ambientBaseBottom),
                startY = 0f,
                endY = size.height
            )

            // The lattice, built once per size: one flat list of points, drawn in a single call.
            val lattice = if (showDots) {
                val step = 30.dp.toPx()
                val points = ArrayList<Offset>()
                var y = step * 0.5f
                while (y < size.height) {
                    var x = step * 0.5f
                    while (x < size.width) {
                        points.add(Offset(x, y))
                        x += step
                    }
                    y += step
                }
                points
            } else {
                emptyList()
            }

            val dotColor = spec.dot.copy(alpha = 0.14f)
            val dotWidth = 1.6.dp.toPx()
            val longestSide = size.maxDimension.coerceAtLeast(1f)
            val vignetteCenter = Offset(size.width * 0.5f, size.height * 0.42f)
            val vignetteRadius = longestSide * 0.82f
            val width = size.width
            val height = size.height

            onDrawBehind {
                // Read inside the draw phase on purpose: a new phase invalidates the drawing of this
                // layer and nothing else.
                val phase = phaseState.value

                drawRect(base)

                // The colour fields. Screen blending is what turns overlapping translucent circles
                // into light rather than paint — the difference between "glass over a gradient" and
                // "glass over a lamp".
                spec.blobs.forEach { blob ->
                    val angle = 2.0 * PI * (phase + blob.phase)
                    val center = Offset(
                        x = width * (blob.centerX + (sin(angle) * blob.driftX).toFloat()),
                        y = height * (blob.centerY + (cos(angle) * blob.driftY).toFloat())
                    )
                    val radius = longestSide * blob.radius
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                blob.color.copy(alpha = 0.62f),
                                blob.color.copy(alpha = 0.20f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center,
                        blendMode = BlendMode.Screen
                    )
                }

                if (lattice.isNotEmpty()) {
                    drawPoints(
                        points = lattice,
                        pointMode = PointMode.Points,
                        color = dotColor,
                        strokeWidth = dotWidth,
                        cap = StrokeCap.Round
                    )
                }

                // The vignette, so text at the screen's edges always has a quieter field behind it.
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.52f)),
                        center = vignetteCenter,
                        radius = vignetteRadius
                    )
                )
            }
        }
    )
}
