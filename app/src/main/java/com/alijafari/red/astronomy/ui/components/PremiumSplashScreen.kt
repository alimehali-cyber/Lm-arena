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
import androidx.compose.ui.geometry.Rect
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
 * World-Class Ultra-Premium Animated Splash Screen for "RED".
 * Designed with 60FPS hardware-accelerated Compose Canvas particle motion,
 * multi-ring gyroscopic orbital physics, and elegant typography.
 *
 * Developer Credit:
 * Designed and Developed by
 * Ali Jafari
 */
@Composable
fun PremiumSplashScreen(
    onSplashComplete: () -> Unit
) {
    val bgGlowAlpha = remember { Animatable(0f) }
    val starsAlpha = remember { Animatable(0f) }
    val planetScale = remember { Animatable(0.2f) }
    val planetAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(24f) }
    val lineScaleX = remember { Animatable(0f) }
    val creditAlpha = remember { Animatable(0f) }
    val creditOffsetY = remember { Animatable(18f) }
    val splashContainerAlpha = remember { Animatable(1f) }

    // Multi-axis orbital rotation transitions
    val infiniteTransition = rememberInfiniteTransition(label = "SplashOrbits")
    val orbitAngle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitAngle1"
    )

    val orbitAngle2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitAngle2"
    )

    val starTwinkle by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StarTwinkle"
    )

    // Pre-generated deterministic star positions & sizes
    val starPositions = remember {
        val rand = Random(42)
        List(65) {
            Triple(
                rand.nextFloat(), // x %
                rand.nextFloat(), // y %
                rand.nextFloat() * 1.8f + 0.4f // radius
            )
        }
    }

    // Pre-generated ambient cosmic dust particles
    val particlePositions = remember {
        val rand = Random(108)
        List(18) {
            Triple(
                rand.nextFloat(),
                rand.nextFloat(),
                rand.nextFloat() * 2.2f + 0.8f
            )
        }
    }

    LaunchedEffect(Unit) {
        // 0.0s - 0.3s: Background deep glow fades in
        launch {
            bgGlowAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing)
            )
        }

        // 0.2s - 0.6s: Star field fades in
        delay(200)
        launch {
            starsAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }

        // 0.3s - 1.0s: Central Crimson Celestial Core scales smoothly
        launch {
            planetAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
            )
            planetScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(
                    durationMillis = 700,
                    easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
                )
            )
        }

        // 0.6s - 1.2s: "RED" title slides up cleanly
        delay(300)
        launch {
            titleAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 450, easing = LinearOutSlowInEasing)
            )
            titleOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 600,
                    easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
                )
            )
        }

        // 0.9s - 1.4s: Golden accent line expands horizontally
        delay(300)
        launch {
            lineScaleX.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(
                    durationMillis = 500,
                    easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
                )
            )
        }

        // 1.2s - 1.8s: Developer Credit fades in with elegant typography
        delay(300)
        launch {
            creditAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
            creditOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 550,
                    easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
                )
            )
        }

        // 2.0s - 2.45s: Smooth container fade out exit
        delay(800)
        splashContainerAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 450,
                easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
            )
        )

        onSplashComplete()
    }

    if (splashContainerAlpha.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = splashContainerAlpha.value }
                .background(Color(0xFF03050B))
                .testTag("premium_splash_screen"),
            contentAlignment = Alignment.Center
        ) {
            // Background Cosmic Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cX = size.width / 2f
                val cY = size.height / 2f

                // Deep Crimson Cosmic Nebula Radial Glow
                if (bgGlowAlpha.value > 0f) {
                    val glowRadius = size.minDimension * 0.70f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFDC2626).copy(alpha = 0.25f * bgGlowAlpha.value),
                                Color(0xFF991B1B).copy(alpha = 0.12f * bgGlowAlpha.value),
                                Color(0xFF380E18).copy(alpha = 0.05f * bgGlowAlpha.value),
                                Color.Transparent
                            ),
                            center = Offset(cX, cY),
                            radius = glowRadius
                        ),
                        center = Offset(cX, cY),
                        radius = glowRadius
                    )
                }

                // Sparkling Starfield
                if (starsAlpha.value > 0f) {
                    starPositions.forEachIndexed { idx, (xFrac, yFrac, starRadius) ->
                        val px = xFrac * size.width
                        val py = yFrac * size.height
                        val currentTwinkle = if (idx % 2 == 0) starTwinkle else (1.3f - starTwinkle)
                        val alpha = (0.20f + 0.70f * currentTwinkle).coerceIn(0f, 1f) * starsAlpha.value

                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = starRadius.dp.toPx(),
                            center = Offset(px, py)
                        )
                    }

                    // Ambient Floating Stardust
                    particlePositions.forEach { (xFrac, yFrac, pSize) ->
                        val px = xFrac * size.width
                        val py = yFrac * size.height
                        drawCircle(
                            color = Color(0xFFFCA5A5).copy(alpha = 0.25f * starsAlpha.value),
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
                    .padding(horizontal = 28.dp)
            ) {
                // Central Celestial Gyroscopic Icon
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
                        val coreRadius = 38.dp.toPx()

                        // Multi-layer Corona Flare
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFEF4444).copy(alpha = 0.40f),
                                    Color(0xFFB91C1C).copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                center = Offset(cX, cY),
                                radius = coreRadius * 2.2f
                            ),
                            center = Offset(cX, cY),
                            radius = coreRadius * 2.2f
                        )

                        // Central Crimson Planet/Sphere with Realistic Lighting Specular
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFCA5A5), // Highlight
                                    Color(0xFFDC2626), // Main Crimson Body
                                    Color(0xFF7F1D1D), // Shadow
                                    Color(0xFF1E070B)  // Dark Limb
                                ),
                                center = Offset(cX - coreRadius * 0.35f, cY - coreRadius * 0.35f),
                                radius = coreRadius * 1.35f
                            ),
                            center = Offset(cX, cY),
                            radius = coreRadius
                        )

                        // Outer Dual Gyroscopic Rings
                        val ringRx = coreRadius * 1.85f
                        val ringRy = coreRadius * 0.52f

                        // Ring 1 (-25° inclination)
                        rotate(degrees = -25f, pivot = Offset(cX, cY)) {
                            drawOval(
                                color = Color(0xFFFCA5A5).copy(alpha = 0.65f),
                                topLeft = Offset(cX - ringRx, cY - ringRy),
                                size = androidx.compose.ui.geometry.Size(ringRx * 2f, ringRy * 2f),
                                style = Stroke(width = 1.8.dp.toPx())
                            )

                            // Orbiting Satellite 1
                            val rad1 = Math.toRadians(orbitAngle1.toDouble())
                            val dot1X = cX + ringRx * cos(rad1).toFloat()
                            val dot1Y = cY + ringRy * sin(rad1).toFloat()

                            drawCircle(
                                color = Color(0xFFEF4444).copy(alpha = 0.5f),
                                radius = 6.dp.toPx(),
                                center = Offset(dot1X, dot1Y)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.dp.toPx(),
                                center = Offset(dot1X, dot1Y)
                            )
                        }

                        // Ring 2 (+35° inclination counter-rotating)
                        rotate(degrees = 35f, pivot = Offset(cX, cY)) {
                            drawOval(
                                color = Color(0xFF38BDF8).copy(alpha = 0.45f),
                                topLeft = Offset(cX - ringRx * 0.85f, cY - ringRy * 0.85f),
                                size = androidx.compose.ui.geometry.Size(ringRx * 1.7f, ringRy * 1.7f),
                                style = Stroke(width = 1.2.dp.toPx())
                            )

                            // Orbiting Satellite 2
                            val rad2 = Math.toRadians(orbitAngle2.toDouble())
                            val dot2X = cX + ringRx * 0.85f * cos(rad2).toFloat()
                            val dot2Y = cY + ringRy * 0.85f * sin(rad2).toFloat()

                            drawCircle(
                                color = Color(0xFF38BDF8),
                                radius = 2.5.dp.toPx(),
                                center = Offset(dot2X, dot2Y)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // App Title "RED ASTRONOMY"
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.graphicsLayer {
                        alpha = titleAlpha.value
                        translationY = titleOffsetY.value
                    }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // "RED"
                        Text(
                            text = "RED",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 46.sp,
                                letterSpacing = 20.sp,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        // "A S T R O N O M Y"
                        Text(
                            text = "A S T R O N O M Y",
                            style = TextStyle(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = 6.sp,
                                color = Color(0xFFEF4444)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Thin Horizontal Accent Divider
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(1.5.dp)
                        .graphicsLayer {
                            scaleX = lineScaleX.value
                            alpha = lineScaleX.value * 0.85f
                        }
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFEF4444),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Developer Information (Formatted Exactly as Requested)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.graphicsLayer {
                        alpha = creditAlpha.value
                        translationY = creditOffsetY.value
                    }
                ) {
                    Text(
                        text = "Designed and Developed by",
                        style = TextStyle(
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            letterSpacing = 2.0.sp,
                            color = Color(0xFF9CA3AF),
                            textAlign = TextAlign.Center
                        )
                    )
                    Text(
                        text = "Ali Jafari",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 1.5.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}
