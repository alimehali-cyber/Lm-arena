package com.alijafari.red.astronomy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ultra-Premium Apple/Linear/Vercel-Grade Animated Full-Screen Splash Screen for "RED".
 * Total duration: MAXIMUM 2.5 seconds (2450ms).
 *
 * Developer Credit (Mandatory):
 * Designed and Developed by:
 * Ali Jafari
 */
@Composable
fun PremiumSplashScreen(
    onSplashComplete: () -> Unit
) {
    // Animatable properties for 60fps GPU acceleration
    val bgGlowAlpha = remember { Animatable(0f) }
    val starsAlpha = remember { Animatable(0f) }
    val planetScale = remember { Animatable(0.3f) }
    val planetAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(20f) }
    val lineScaleX = remember { Animatable(0f) }
    val creditAlpha = remember { Animatable(0f) }
    val creditOffsetY = remember { Animatable(16f) }
    val splashContainerAlpha = remember { Animatable(1f) }

    // Satellite orbital phase animation
    val infiniteTransition = rememberInfiniteTransition(label = "OrbitalDot")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitAngle"
    )

    // Twinkle animation for stars
    val starTwinkle by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StarTwinkle"
    )

    // Pre-generated deterministic star positions & sizes
    val starPositions = remember {
        val rand = Random(42)
        List(50) {
            Triple(
                rand.nextFloat(), // x % (0..1)
                rand.nextFloat(), // y % (0..1)
                rand.nextFloat() * 1.5f + 0.5f // size in px
            )
        }
    }

    // Pre-generated floating ambient particles
    val particlePositions = remember {
        val rand = Random(108)
        List(12) {
            Triple(
                rand.nextFloat(), // x %
                rand.nextFloat(), // y %
                rand.nextFloat() * 2.5f + 1.0f // size
            )
        }
    }

    // Strict Timeline Choreography (Total <= 2.5 seconds)
    LaunchedEffect(Unit) {
        // 0.0s - 0.2s: Background gradient and glow fade in
        launch {
            bgGlowAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing)
            )
        }

        // 0.3s - 0.5s: Star field fades in
        delay(300)
        launch {
            starsAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }

        // 0.3s - 1.1s: Central Mars planet scales up from 0.3 -> 1.0 with Apple cubic-bezier
        launch {
            planetAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
            )
            planetScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(
                    durationMillis = 750,
                    easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
                )
            )
        }

        // 0.6s - 1.3s: "RED" text slides up from 20dp below with fade-in
        delay(300) // (600ms total)
        launch {
            titleAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            titleOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 650,
                    easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
                )
            )
        }

        // 1.0s - 1.5s: Accent line expands from 0 to 60dp width
        delay(400) // (1000ms total)
        launch {
            lineScaleX.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(
                    durationMillis = 500,
                    easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
                )
            )
        }

        // 1.3s - 1.9s: Developer credit fades in from below
        delay(300) // (1300ms total)
        launch {
            creditAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
        launch {
            creditOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 550,
                    easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
                )
            )
        }

        // 2.0s - 2.5s: Entire splash screen fades out with smooth opacity exit
        delay(700) // (2000ms total)
        splashContainerAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 450,
                easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
            )
        )

        // 2.45s: Complete transition to main app UI
        onSplashComplete()
    }

    if (splashContainerAlpha.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = splashContainerAlpha.value
                }
                .background(Color(0xFF000000))
                .testTag("premium_splash_screen"),
            contentAlignment = Alignment.Center
        ) {
            // Background Layer: Deep Crimson Mars Radial Glow & Star Field
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cX = size.width / 2f
                val cY = size.height / 2f

                // Central Deep Crimson/Mars-Red Glow
                if (bgGlowAlpha.value > 0f) {
                    val glowRadius = size.minDimension * 0.65f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFB41E14).copy(alpha = 0.22f * bgGlowAlpha.value),
                                Color(0xFFE11D48).copy(alpha = 0.10f * bgGlowAlpha.value),
                                Color(0xFF1E0A10).copy(alpha = 0.04f * bgGlowAlpha.value),
                                Color.Transparent
                            ),
                            center = Offset(cX, cY),
                            radius = glowRadius
                        ),
                        center = Offset(cX, cY),
                        radius = glowRadius
                    )
                }

                // Faint Star Field with Twinkle Effect
                if (starsAlpha.value > 0f) {
                    starPositions.forEachIndexed { idx, (xFrac, yFrac, starSize) ->
                        val px = xFrac * size.width
                        val py = yFrac * size.height
                        val currentTwinkle = if (idx % 2 == 0) starTwinkle else (1.4f - starTwinkle)
                        val alpha = (0.25f + 0.65f * currentTwinkle).coerceIn(0f, 1f) * starsAlpha.value

                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = starSize.dp.toPx(),
                            center = Offset(px, py)
                        )
                    }

                    // Ambient Slow Floating Particles
                    particlePositions.forEach { (xFrac, yFrac, pSize) ->
                        val px = xFrac * size.width
                        val py = yFrac * size.height
                        drawCircle(
                            color = Color(0xFFFB7185).copy(alpha = 0.20f * starsAlpha.value),
                            radius = pSize.dp.toPx(),
                            center = Offset(px, py)
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // Central Hero Visual: Mars Planet with Radial Shading + Tilted Ring + Orbiting Satellite
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer {
                            scaleX = planetScale.value
                            scaleY = planetScale.value
                            alpha = planetAlpha.value
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cX = size.width / 2f
                        val cY = size.height / 2f
                        val planetRadius = 40.dp.toPx() // 80dp diameter

                        // Outer Soft Red Corona Halo
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFF43F5E).copy(alpha = 0.35f),
                                    Color(0xFF9F1239).copy(alpha = 0.12f),
                                    Color.Transparent
                                ),
                                center = Offset(cX, cY),
                                radius = planetRadius * 1.8f
                            ),
                            center = Offset(cX, cY),
                            radius = planetRadius * 1.8f
                        )

                        // Mars-like Planet Sphere with Realistic Gradient (Highlight Top-Left, Shadow Bottom-Right)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFDA4AF), // Top-left specular highlight
                                    Color(0xFFE11D48), // Main rich crimson body
                                    Color(0xFF881337), // Shadow side
                                    Color(0xFF1F040A)  // Deep limb darkness
                                ),
                                center = Offset(cX - planetRadius * 0.35f, cY - planetRadius * 0.35f),
                                radius = planetRadius * 1.35f
                            ),
                            center = Offset(cX, cY),
                            radius = planetRadius
                        )

                        // Thin Orbital Ring (Tilted at 75°)
                        val ringRx = planetRadius * 1.75f
                        val ringRy = planetRadius * 0.48f

                        rotate(degrees = -20f, pivot = Offset(cX, cY)) {
                            // Ring Path
                            val ringPath = Path().apply {
                                addOval(
                                    androidx.compose.ui.geometry.Rect(
                                        cX - ringRx, cY - ringRy,
                                        cX + ringRx, cY + ringRy
                                    )
                                )
                            }
                            drawPath(
                                path = ringPath,
                                color = Color(0xFFFB7185).copy(alpha = 0.60f),
                                style = Stroke(width = 1.6.dp.toPx())
                            )

                            // Orbiting Satellite Glowing Dot along the Elliptic Ring
                            val rad = Math.toRadians(orbitAngle.toDouble())
                            val dotX = cX + ringRx * cos(rad).toFloat()
                            val dotY = cY + ringRy * sin(rad).toFloat()

                            // Satellite Outer Glow
                            drawCircle(
                                color = Color(0xFFFB7185).copy(alpha = 0.45f),
                                radius = 6.dp.toPx(),
                                center = Offset(dotX, dotY)
                            )
                            // Satellite Core Dot
                            drawCircle(
                                color = Color.White,
                                radius = 2.8.dp.toPx(),
                                center = Offset(dotX, dotY)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // App Title "RED" (Cinematic Space Grotesk / High-Tracking Typography)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.graphicsLayer {
                        alpha = titleAlpha.value
                        translationY = titleOffsetY.value
                    }
                ) {
                    // Glow blur behind "RED"
                    Text(
                        text = "RED",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 50.sp,
                            letterSpacing = 20.sp,
                            color = Color(0xFFE11D48).copy(alpha = 0.4f)
                        )
                    )
                    // Crisp pure white "RED"
                    Text(
                        text = "RED",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 50.sp,
                            letterSpacing = 20.sp,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Thin 60dp Accent Line (Expanding horizontally from 0 to 60dp)
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(1.5.dp)
                        .graphicsLayer {
                            scaleX = lineScaleX.value
                            alpha = lineScaleX.value * 0.8f
                        }
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFF43F5E),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Spacer(modifier = Modifier.weight(1f))

                // Developer Information (MANDATORY EXACT SPECIFICATION)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .padding(bottom = 64.dp) // At least 60dp from bottom
                        .graphicsLayer {
                            alpha = creditAlpha.value
                            translationY = creditOffsetY.value
                        }
                ) {
                    Text(
                        text = "DESIGNED AND DEVELOPED BY:",
                        style = TextStyle(
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            letterSpacing = 2.2.sp,
                            color = Color(0xFF9CA3AF),
                            textAlign = TextAlign.Center
                        )
                    )
                    Text(
                        text = "Ali Jafari",
                        style = TextStyle(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            letterSpacing = 1.2.sp,
                            color = Color(0xFFFFFFFF),
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}
