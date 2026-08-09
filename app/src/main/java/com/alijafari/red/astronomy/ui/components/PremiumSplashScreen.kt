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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Font Families
val Jersey25FontFamily = FontFamily(
    Font(R.font.jersey25_regular, FontWeight.Normal)
)

val MontserratLightFontFamily = FontFamily(
    Font(R.font.montserrat_light, FontWeight.W300)
)

/**
 * Splash Screen for "RED".
 * Solid black background with glowing red "RED" text in the middle.
 */
@Composable
fun PremiumSplashScreen(
    onSplashComplete: () -> Unit
) {
    // Animations
    val titleAlpha = remember { Animatable(0f) }
    val titleScale = remember { Animatable(0.90f) }
    val footerAlpha = remember { Animatable(0f) }
    val footerOffsetY = remember { Animatable(18f) }
    val splashContainerAlpha = remember { Animatable(1f) }

    // Pulsing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "RedGlowPulse")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    LaunchedEffect(Unit) {
        // 1. Title "RED" entrance animation
        launch {
            titleAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
            )
            titleScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(
                    durationMillis = 1000,
                    easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
                )
            )
        }

        // 2. Footer entrance
        delay(300)
        launch {
            footerAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
            footerOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 800,
                    easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
                )
            )
        }

        // Hold and exit
        delay(2200)
        splashContainerAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
        )

        onSplashComplete()
    }

    if (splashContainerAlpha.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = splashContainerAlpha.value }
                .background(Color.Black) // Solid black background
                .testTag("premium_splash_screen"),
            contentAlignment = Alignment.Center
        ) {
            // Ambient glowing red aura behind "RED" text
            Canvas(modifier = Modifier.fillMaxSize()) {
                val currentAlpha = titleAlpha.value
                if (currentAlpha > 0f) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val baseRadius = size.width.coerceAtMost(size.height) * 0.45f
                    val currentGlowRadius = baseRadius * glowPulse

                    // Outer soft red halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF0033).copy(alpha = 0.45f * currentAlpha),
                                Color(0xFFFF1A40).copy(alpha = 0.22f * currentAlpha),
                                Color(0xFFCC0029).copy(alpha = 0.08f * currentAlpha),
                                Color.Transparent
                            ),
                            center = Offset(centerX, centerY),
                            radius = currentGlowRadius
                        ),
                        center = Offset(centerX, centerY),
                        radius = currentGlowRadius
                    )

                    // Concentrated inner red core glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF3355).copy(alpha = 0.60f * currentAlpha),
                                Color(0xFFFF0033).copy(alpha = 0.25f * currentAlpha),
                                Color.Transparent
                            ),
                            center = Offset(centerX, centerY),
                            radius = currentGlowRadius * 0.55f
                        ),
                        center = Offset(centerX, centerY),
                        radius = currentGlowRadius * 0.55f
                    )
                }
            }

            // Glowing "RED" Text in middle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        alpha = titleAlpha.value
                        scaleX = titleScale.value
                        scaleY = titleScale.value
                    }
            ) {
                // Layer 1: Wide diffused text glow
                Text(
                    text = "RED",
                    style = TextStyle(
                        fontFamily = Jersey25FontFamily,
                        fontSize = 150.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFFFF0033).copy(alpha = 0.5f * glowPulse)
                    ),
                    modifier = Modifier.graphicsLayer {
                        scaleX = 1.08f
                        scaleY = 1.08f
                        alpha = 0.6f
                    }
                )

                // Layer 2: Medium vibrant red glow
                Text(
                    text = "RED",
                    style = TextStyle(
                        fontFamily = Jersey25FontFamily,
                        fontSize = 150.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFFFF2E4D).copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.graphicsLayer {
                        scaleX = 1.03f
                        scaleY = 1.03f
                        alpha = 0.85f
                    }
                )

                // Layer 3: Main vivid glowing red title
                val redGradientBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF6677),
                        Color(0xFFFF0033),
                        Color(0xFFE60026)
                    )
                )

                Text(
                    text = "RED",
                    style = TextStyle(
                        fontFamily = Jersey25FontFamily,
                        fontSize = 150.sp,
                        textAlign = TextAlign.Center,
                        brush = redGradientBrush
                    )
                )
            }

            // Footer: Centered at bottom
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 36.dp)
                    .graphicsLayer {
                        alpha = footerAlpha.value
                        translationY = footerOffsetY.value.dp.toPx()
                    }
            ) {
                Text(
                    text = "DESIGNED AND DEVELOPED BY",
                    style = TextStyle(
                        fontFamily = MontserratLightFontFamily,
                        fontWeight = FontWeight.W300,
                        fontSize = 9.sp,
                        letterSpacing = 5.sp,
                        color = Color.White.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center
                    )
                )

                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.32f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                val metallicSilverBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFDDE1E8),
                        Color(0xFFA7AEBC)
                    )
                )

                Text(
                    text = "ALI JAFARI",
                    style = TextStyle(
                        fontFamily = MontserratLightFontFamily,
                        fontWeight = FontWeight.W300,
                        fontSize = 24.sp,
                        letterSpacing = 9.sp,
                        brush = metallicSilverBrush,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}
