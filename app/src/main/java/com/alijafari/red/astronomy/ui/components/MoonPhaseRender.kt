package com.alijafari.red.astronomy.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.CoordinateEngine
import com.alijafari.red.astronomy.astro_engine.MoonEngine
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** The colour the night side dims toward — the same near-black space tone the limb gradient uses. */
private val MoonNightColor = Color(0xFF05050D)

/**
 * The plate frames the Moon with black starfield: the lunar disc itself covers only the central ~82% of
 * the 1024px square. The zoom is the reciprocal of that, plus a hair of overshoot so the very limb — the
 * darkest part of the photograph — is what gets clipped, never the starfield behind it.
 */
private const val MoonPlateDiscFraction = 0.82f
private const val MoonPlateZoom = 1.03f / MoonPlateDiscFraction

/**
 * How bright the Moon's night side stays. Earthshine really is that faint, and keeping a tenth of the
 * plate's texture (rather than none) is what stops the unlit half from turning into either a black disc
 * or a white hole in the card.
 */
private const val MoonNightSideBrightness = 0.11f

/** Sixteen overlapping passes reach [MoonNightSideBrightness] where they all overlap: 1 - b^(1/16). */
private const val MoonNightStepAlpha = 0.129f
/**
 * ------------------------------------------------------------------------------------------------
 * THE 2.5D SHADING SET
 * ------------------------------------------------------------------------------------------------
 *
 * The plate is one flat photograph. What makes a sphere read as a sphere is not more texture, it is the
 * *light*: a bright sub-solar point falling off toward the limb, a limb that goes dark before it reaches
 * the edge, a cold earthshine on the unlit side, and a specular catch on the sunward rim. All four are
 * drawn here as gradients over the same asset, on the Sun's actual screen direction for this hour, so the
 * disc turns with the sky instead of being a sticker with a phase cut out of it.
 *
 * Every layer uses `BlendMode.SrcAtop`, so the shading lives on the Moon and nowhere else: the plate's
 * starfield behind the limb is never touched.
 */
/** Strength of the sphere's own highlight at the sub-solar point. */
private const val MoonSphereHighlightAlpha = 0.20f
private const val MoonLimbAlpha = 0.46f
/** Earthshine's colour: the cold blue of a full Earth seen from the Moon, not a neutral grey. */
private val MoonEarthshineTint = Color(0xFF7FA6FF)
private const val MoonEarthshineAlpha = 0.10f
private const val MoonSpecularAlpha = 0.13f
private const val MoonParallaxMaxFraction = 0.014f

@Composable
fun MoonPhaseRender(
    moonData: MoonEngine.MoonData,
    sunHoriz: CoordinateEngine.Horizontal,
    latitude: Double,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MoonRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Rotation"
    )

    val limbScreenAngleDeg = remember(moonData.azimuthDeg, moonData.altitudeDeg, sunHoriz.azimuthDeg, sunHoriz.altitudeDeg) {
        CoordinateEngine.calculateMoonLimbScreenAngleDeg(
            moonAzimuthDeg = moonData.azimuthDeg,
            moonAltitudeDeg = moonData.altitudeDeg,
            sunAzimuthDeg = sunHoriz.azimuthDeg,
            sunAltitudeDeg = sunHoriz.altitudeDeg
        ).toFloat()
    }

    // The layer is turned over for a southern observer, so anything drawn in screen angles has to be
    // compensated for that turn: a Sun that is to the right of the Moon here is to the left of it there.
    val layerRotationDeg = if (latitude < 0) 180f else 0f
    val renderAngleDeg = limbScreenAngleDeg - layerRotationDeg

    // Libration is small (±8°) but it is the one thing that makes the disc feel like a globe rather than
    // a printed circle: the surface shifts by a hair as we see a little more of one limb. Kept under 1.5 %
    // of the plate so it can never pull the clipped limb inwards.
    val parallaxX =
        ((moonData.librationLonDeg / 8.0).coerceIn(-1.0, 1.0) * MoonParallaxMaxFraction).toFloat()
    val parallaxY =
        ((moonData.librationLatDeg / 8.0).coerceIn(-1.0, 1.0) * MoonParallaxMaxFraction).toFloat()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // One offscreen layer for plate + shadow, so BlendMode.Clear erases the Moon and nothing behind it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    rotationZ = layerRotationDeg + rotationAngle
                }
        ) {
            // High-Res Photographic Moon Asset
            Image(
                painter = painterResource(id = R.drawable.img_full_moon_photo_1785673146290),
                contentDescription = moonData.phaseNameFa,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(MoonPlateZoom)
                    .graphicsLayer {
                        translationX = parallaxX * size.width
                        translationY = parallaxY * size.height
                    }
            )

            // Dynamic Lunar Phase Cut-Out
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (size.width <= 0f || size.height <= 0f) return@Canvas
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                val ill = moonData.illuminationPercent / 100.0

                if (ill < 0.98) {
                    // Sixteen overlapping partial dims: the core of the night side settles at
                    // MoonNightSideBrightness, feathered across the terminator.
                    val numSteps = 16
                    val stepAlpha = MoonNightStepAlpha
                    val feather = radius * 0.08f

                    for (step in 0 until numSteps) {
                        val t = (step.toFloat() / (numSteps - 1) - 0.5f) * 2f
                        val offset = t * feather

                        val shadowPath = Path()
                        shadowPath.addArc(
                            Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                            90f,
                            180f
                        )

                        val k = (2.0 * ill - 1.0).toFloat()
                        val stepInnerWidth = (abs(k) * radius + offset).coerceAtLeast(0f)
                        val innerRect = Rect(center.x - stepInnerWidth, center.y - radius, center.x + stepInnerWidth, center.y + radius)

                        val innerSweep = if (k >= 0) -180f else 180f
                        shadowPath.arcTo(innerRect, 270f, innerSweep, false)
                        shadowPath.close()

                        rotate(renderAngleDeg, center) {
                            drawPath(
                                path = shadowPath,
                                color = MoonNightColor,
                                alpha = stepAlpha,
                                blendMode = BlendMode.SrcAtop
                            )
                        }
                    }
                }

                // ---------------------------------------------------------------------------------
                // 2.5D shading, applied in this order: the sphere's own light, then the earthshine on
                // its unlit side, then the limb, then the specular catch on the sunward rim.
                // ---------------------------------------------------------------------------------
                val sunRad = Math.toRadians(renderAngleDeg.toDouble())
                val sunDir = Offset(cos(sunRad).toFloat(), sin(sunRad).toFloat())
                val subSolar = Offset(
                    center.x + sunDir.x * radius * 0.44f,
                    center.y + sunDir.y * radius * 0.44f
                )

                // 1. The sphere. A radial gradient centred on the sub-solar point: brightest where the
                //    Sun is overhead on the surface, falling through transparent mid-disc and into a dark
                //    rim — which is the curvature cue a flat plate cannot fake with its own tone.
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color.White.copy(alpha = MoonSphereHighlightAlpha),
                            0.30f to Color.White.copy(alpha = MoonSphereHighlightAlpha * 0.28f),
                            0.58f to Color.Transparent,
                            0.84f to Color.Black.copy(alpha = 0.16f),
                            1.00f to Color.Black.copy(alpha = 0.34f)
                        ),
                        center = subSolar,
                        radius = radius * 1.42f
                    ),
                    radius = radius,
                    center = center,
                    blendMode = BlendMode.SrcAtop
                )

                // 2. Earthshine is not grey. The unlit side is lit by a blue planet, so it gets a cold
                //    cast on the side away from the Sun rather than a neutral darkening.
                if (ill < 0.98) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.00f to MoonEarthshineTint.copy(alpha = MoonEarthshineAlpha * 1.6f),
                                0.55f to MoonEarthshineTint.copy(alpha = MoonEarthshineAlpha * 0.7f),
                                1.00f to Color.Transparent
                            ),
                            center = Offset(
                                center.x - sunDir.x * radius * 0.40f,
                                center.y - sunDir.y * radius * 0.40f
                            ),
                            radius = radius * 1.15f
                        ),
                        radius = radius,
                        center = center,
                        blendMode = BlendMode.SrcAtop
                    )
                }

                // 3. The limb. Dark *before* the edge, not at it, so the disc reads as a ball turning away
                //    from us instead of as a circle with a border painted on.
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.80f to Color.Transparent,
                            0.93f to MoonNightColor.copy(alpha = 0.26f),
                            1.00f to MoonNightColor.copy(alpha = MoonLimbAlpha)
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center,
                    blendMode = BlendMode.SrcAtop
                )

                // 4. The specular catch: three concentric arcs hugging the sunward rim, fading outward —
                //    the highlight that says the surface is polished rock, not paper.
                if (ill > 0.02) {
                    val arcSweep = 96f + 74f * ill.toFloat()
                    val arcStart = renderAngleDeg - arcSweep / 2f
                    for (i in 0 until 3) {
                        val inset = radius * (0.030f + 0.020f * i)
                        val r = radius - inset
                        if (r <= 0f) continue
                        drawArc(
                            color = Color.White.copy(alpha = MoonSpecularAlpha * (1f - 0.28f * i)),
                            startAngle = arcStart,
                            sweepAngle = arcSweep,
                            useCenter = false,
                            topLeft = Offset(center.x - r, center.y - r),
                            size = Size(r * 2f, r * 2f),
                            style = Stroke(width = radius * (0.012f + 0.014f * i)),
                            blendMode = BlendMode.SrcAtop
                        )
                    }
                }
            }
        }
    }
}
