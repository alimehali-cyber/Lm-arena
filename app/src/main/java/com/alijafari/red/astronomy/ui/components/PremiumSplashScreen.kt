package com.alijafari.red.astronomy.ui.components

import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Shader
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
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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
import kotlin.math.sin
import kotlin.random.Random

// Font Families
val Jersey25FontFamily = FontFamily(
    Font(R.font.jersey25_regular, FontWeight.Normal)
)

val MontserratLightFontFamily = FontFamily(
    Font(R.font.montserrat_light, FontWeight.W300)
)

private data class SplashStar(
    val xFrac: Float,
    val yFrac: Float,
    val isNear: Boolean,
    val coreRadiusDp: Float,
    val color: Color,
    val cycleSeconds: Float,
    val phaseRad: Float
)

/**
 * Redesigned Splash Screen for "RED".
 *
 * Requirements:
 * 1. Pure black background.
 * 2. Star field (~90 stars: ~64 faint far, ~26 bright near).
 *    Soft radial-gradient glow around bright core (2.5x radius far, 4.0x radius near).
 *    Smooth sine-wave flicker on 2-4s staggered cycles. Fade in over 1.5s.
 * 3. Title: "RED" centered, Jersey 25 font, ~150sp, scale 0.92->1 + fade over ~1.1s.
 *    Filled ONLY inside letter shapes with drifting nebula ShaderBrush (or fallback gradient).
 *    No glow, drop-shadow, blur, or sheen on/around text.
 * 4. Footer: Centered at bottom with staggered fade-up entrance:
 *    - "DESIGNED AND DEVELOPED BY" (Montserrat 300, 9sp, uppercase, 5sp tracking, 55% white)
 *    - 30x1dp hairline divider (32% white, fading ends)
 *    - "ALI JAFARI" (Montserrat 300, 24sp, uppercase, 9sp tracking, metallic-silver vertical gradient)
 */
@Composable
fun PremiumSplashScreen(
    onSplashComplete: () -> Unit
) {
    val context = LocalContext.current

    // Animations
    val starFieldAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleScale = remember { Animatable(0.92f) }
    val footerAlpha = remember { Animatable(0f) }
    val footerOffsetY = remember { Animatable(18f) }
    val splashContainerAlpha = remember { Animatable(1f) }

    // Time loop for star flicker and nebula drift
    val infiniteTransition = rememberInfiniteTransition(label = "SplashInfinite")
    val timeMillis by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "TimeMillis"
    )

    // Nebula translation 14s loop
    val nebulaDriftFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "NebulaDrift"
    )

    // Generate ~90 stars (64 far, 26 near) deterministically
    val starList = remember {
        val rand = Random(2026)
        val list = mutableListOf<SplashStar>()

        // 64 faint far stars (0.3–0.7 dp)
        repeat(64) {
            val radius = rand.nextFloat() * (0.7f - 0.3f) + 0.3f
            list.add(
                SplashStar(
                    xFrac = rand.nextFloat(),
                    yFrac = rand.nextFloat(),
                    isNear = false,
                    coreRadiusDp = radius,
                    color = Color.White,
                    cycleSeconds = rand.nextFloat() * (4.0f - 2.0f) + 2.0f,
                    phaseRad = (rand.nextFloat() * 2.0 * Math.PI).toFloat()
                )
            )
        }

        // 26 brighter near stars (0.8–1.7 dp, ~30% tinted soft blue/violet)
        repeat(26) {
            val radius = rand.nextFloat() * (1.7f - 0.8f) + 0.8f
            val isTinted = rand.nextFloat() < 0.30f
            val color = if (isTinted) {
                if (rand.nextBoolean()) Color(0xFFA5F3FC) else Color(0xFFD8B4FE)
            } else {
                Color.White
            }
            list.add(
                SplashStar(
                    xFrac = rand.nextFloat(),
                    yFrac = rand.nextFloat(),
                    isNear = true,
                    coreRadiusDp = radius,
                    color = color,
                    cycleSeconds = rand.nextFloat() * (4.0f - 2.0f) + 2.0f,
                    phaseRad = (rand.nextFloat() * 2.0 * Math.PI).toFloat()
                )
            )
        }
        list
    }

    // Load nebula bitmap texture from res/drawable
    val bitmap = remember(context) {
        try {
            BitmapFactory.decodeResource(context.resources, R.drawable.nebula_texture)
        } catch (e: Exception) {
            null
        }
    }

    // ShaderBrush for title letters fill
    val matrix = remember { Matrix() }
    val nebulaShaderBrush = remember(bitmap, nebulaDriftFraction) {
        if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
            val shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            matrix.reset()
            val translateX = nebulaDriftFraction * bitmap.width.toFloat()
            val translateY = nebulaDriftFraction * bitmap.height.toFloat() * 0.5f
            matrix.postTranslate(translateX, translateY)
            shader.setLocalMatrix(matrix)
            ShaderBrush(shader)
        } else {
            null
        }
    }

    // Fallback slow-drifting purple->blue->orange gradient brush
    val fallbackGradientBrush = remember(nebulaDriftFraction) {
        val startX = nebulaDriftFraction * 1000f
        val startY = nebulaDriftFraction * 500f
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF9333EA), // vivid purple
                Color(0xFF2563EB), // vivid blue
                Color(0xFFEA580C), // vivid orange
                Color(0xFF7C3AED)  // vivid purple
            ),
            start = Offset(startX, startY),
            end = Offset(startX + 1000f, startY + 600f)
        )
    }

    val activeLetterBrush = nebulaShaderBrush ?: fallbackGradientBrush

    // Entrance and exit animations
    LaunchedEffect(Unit) {
        // 1. Star field fades in over ~1.5 s
        launch {
            starFieldAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing)
            )
        }

        // 2. Title "RED" entrance animation (scale 0.92->1 + fade over ~1.1s) starting at 200ms
        delay(200)
        launch {
            titleAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 1100, easing = LinearOutSlowInEasing)
            )
            titleScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(
                    durationMillis = 1100,
                    easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
                )
            )
        }

        // 3. Footer staggered fade-up entrance starting at 600ms
        delay(400)
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

        // Display hold time, then exit fade
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
                .background(Color.Black) // Pure black background
                .testTag("premium_splash_screen"),
            contentAlignment = Alignment.Center
        ) {
            // Star field Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val currentStarAlpha = starFieldAlpha.value
                if (currentStarAlpha > 0f) {
                    val timeSec = timeMillis / 1000f

                    starList.forEach { star ->
                        val px = star.xFrac * size.width
                        val py = star.yFrac * size.height
                        val corePx = star.coreRadiusDp.dp.toPx()
                        val glowMultiplier = if (star.isNear) 4.0f else 2.5f
                        val glowPx = corePx * glowMultiplier

                        // Smooth sine wave flicker factor (varies 0.2..1.0)
                        val flicker = (0.6f + 0.4f * sin(2.0 * Math.PI * timeSec / star.cycleSeconds + star.phaseRad)).toFloat().coerceIn(0.1f, 1.0f)
                        val starAlpha = currentStarAlpha * flicker

                        // 1. Soft radial gradient glow around the core
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    star.color.copy(alpha = 0.65f * starAlpha),
                                    star.color.copy(alpha = 0.20f * starAlpha),
                                    Color.Transparent
                                ),
                                center = Offset(px, py),
                                radius = glowPx
                            ),
                            center = Offset(px, py),
                            radius = glowPx
                        )

                        // 2. Bright core
                        drawCircle(
                            color = star.color.copy(alpha = starAlpha),
                            radius = corePx,
                            center = Offset(px, py)
                        )
                    }
                }
            }

            // Title: "RED" centered
            Text(
                text = "RED",
                style = TextStyle(
                    fontFamily = Jersey25FontFamily,
                    fontSize = 150.sp,
                    textAlign = TextAlign.Center,
                    brush = activeLetterBrush
                ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        alpha = titleAlpha.value
                        scaleX = titleScale.value
                        scaleY = titleScale.value
                    }
            )

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
                // "DESIGNED AND DEVELOPED BY"
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

                // 30x1 dp hairline divider
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

                // "ALI JAFARI"
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
