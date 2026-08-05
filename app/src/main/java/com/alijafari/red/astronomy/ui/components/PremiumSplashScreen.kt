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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ultra-Premium, Minimalist Apple-Grade Animated Splash Screen
 * Runs smoothly for a maximum of 2.2 seconds (total timeline <= 2.2s).
 * Features developer attribution:
 * "Designed and Developed by:
 * Ali Jafari"
 */
@Composable
fun PremiumSplashScreen(
    onSplashComplete: () -> Unit
) {
    // Animatable states for hardware-accelerated GPU animation
    val logoScale = remember { Animatable(0.85f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(14f) }
    val lineScale = remember { Animatable(0f) }
    val containerAlpha = remember { Animatable(1.0f) }

    LaunchedEffect(Unit) {
        // Step 1: Smooth Apple ease-out reveal for logo emblem (0 to 600ms)
        launch {
            logoScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(
                    durationMillis = 700,
                    easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
                )
            )
        }
        launch {
            logoAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(
                    durationMillis = 650,
                    easing = FastOutSlowInEasing
                )
            )
        }

        // Step 2: Line reveal & Typography slide-up (250ms to 850ms)
        delay(250)
        launch {
            lineScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(
                    durationMillis = 600,
                    easing = FastOutSlowInEasing
                )
            )
        }
        launch {
            textOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 600,
                    easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
                )
            )
        }
        launch {
            textAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(
                    durationMillis = 550,
                    easing = LinearOutSlowInEasing
                )
            )
        }

        // Step 3: Hold steady phase (850ms to 1700ms)
        delay(1250)

        // Step 4: Ultra-smooth cross-dissolve exit to app (1700ms to 2150ms)
        containerAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 450,
                easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
            )
        )

        onSplashComplete()
    }

    if (containerAlpha.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = containerAlpha.value
                }
                .background(Color(0xFF090A0F))
                .testTag("premium_splash_screen"),
            contentAlignment = Alignment.Center
        ) {
            // Ambient soft background glow (Apple-style subtle radial halo)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val glowRadius = size.minDimension * 0.55f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE11D48).copy(alpha = 0.12f * logoAlpha.value),
                            Color(0xFF8B5CF6).copy(alpha = 0.06f * logoAlpha.value),
                            Color.Transparent
                        ),
                        center = center,
                        radius = glowRadius
                    ),
                    center = center,
                    radius = glowRadius
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // Apple-Inspired Minimalist Vector Celestial Emblem
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            alpha = logoAlpha.value
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cX = size.width / 2f
                        val cY = size.height / 2f
                        val baseR = size.width * 0.38f

                        // Outer thin orbital ring
                        drawCircle(
                            color = Color.White.copy(alpha = 0.15f),
                            radius = baseR,
                            center = Offset(cX, cY),
                            style = Stroke(width = 1.2.dp.toPx())
                        )

                        // Elliptical inclined orbit ring (Apple minimalist aesthetic)
                        val orbitPath = Path()
                        val rx = baseR * 1.18f
                        val ry = baseR * 0.42f
                        orbitPath.addOval(
                            androidx.compose.ui.geometry.Rect(
                                cX - rx, cY - ry,
                                cX + rx, cY + ry
                            )
                        )
                        drawPath(
                            path = orbitPath,
                            color = Color(0xFFF43F5E).copy(alpha = 0.65f),
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // Inner core glowing point
                        drawCircle(
                            color = Color(0xFFFB7185).copy(alpha = 0.35f),
                            radius = 16.dp.toPx(),
                            center = Offset(cX, cY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = Offset(cX, cY)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // App Title "RED" with luxurious tracking
                Text(
                    text = "RED",
                    style = TextStyle(
                        fontWeight = FontWeight.Light,
                        fontSize = 32.sp,
                        letterSpacing = 8.sp,
                        color = Color.White
                    ),
                    modifier = Modifier.graphicsLayer {
                        alpha = logoAlpha.value
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Delicate Accent Line
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(1.dp)
                        .graphicsLayer {
                            scaleX = lineScale.value
                            alpha = lineScale.value * 0.5f
                        }
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xFFFB7185),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Spacer(modifier = Modifier.weight(1f))

                // Developer's Information (Mandatory verbatim text block)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .padding(bottom = 56.dp)
                        .graphicsLayer {
                            alpha = textAlpha.value
                            translationY = textOffsetY.value
                        }
                ) {
                    Text(
                        text = "Designed and Developed by:",
                        style = TextStyle(
                            fontWeight = FontWeight.Light,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp,
                            color = Color(0xFF9CA3AF),
                            textAlign = TextAlign.Center
                        )
                    )
                    Text(
                        text = "Ali Jafari",
                        style = TextStyle(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFFF3F4F6),
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}
