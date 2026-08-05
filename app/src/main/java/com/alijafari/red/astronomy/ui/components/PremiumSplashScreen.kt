package com.alijafari.red.astronomy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.alijafari.red.astronomy.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PremiumSplashScreen(
    onSplashComplete: () -> Unit
) {
    // Safe image loading with fallback mechanism
    var currentResIndex by remember { mutableStateOf((0..2).random()) }
    val splashResList = remember {
        listOf(
            R.drawable.img_splash_1,
            R.drawable.img_splash_2,
            R.drawable.img_splash_3
        )
    }

    var painterResult by remember(currentResIndex) {
        mutableStateOf<androidx.compose.ui.graphics.painter.Painter?>(null)
    }

    LaunchedEffect(currentResIndex) {
        try {
            // Attempt to load painter safely
            painterResult = null
        } catch (e: Exception) {
            android.util.Log.e("RED_Splash", "Failed to load splash image index $currentResIndex", e)
            if (currentResIndex < splashResList.size - 1) {
                currentResIndex++
            }
        }
    }

    // Animatable states for performance-optimized GPU layer transformations
    val imageAlpha = remember { Animatable(0f) }
    val imageScale = remember { Animatable(1.0f) }
    val ambientLight = remember { Animatable(0.97f) }
    val splashContainerAlpha = remember { Animatable(1.0f) }

    LaunchedEffect(Unit) {
        // Step 1: Initial 100ms pure black screen for invisible app initialization
        delay(100)

        // Step 2: Living Zoom (1.0f -> 1.028f over 2700ms)
        launch {
            imageScale.animateTo(
                targetValue = 1.028f,
                animationSpec = tween(
                    durationMillis = 2700,
                    easing = FastOutSlowInEasing
                )
            )
        }

        // Step 3: Ambient light breathing (0.97f -> 1.0f -> 0.98f)
        launch {
            ambientLight.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(1200, easing = LinearOutSlowInEasing)
            )
            ambientLight.animateTo(
                targetValue = 0.98f,
                animationSpec = tween(1400, easing = FastOutSlowInEasing)
            )
        }

        // Step 4: Artwork gentle fade in (0f -> 1f over 750ms ease-out)
        imageAlpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(
                durationMillis = 750,
                easing = FastOutSlowInEasing
            )
        )

        // Hold full artwork experience calmly
        delay(1400)

        // Step 5: Seamless cross-dissolve exit to Home screen (500ms)
        splashContainerAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 500,
                easing = FastOutSlowInEasing
            )
        )

        onSplashComplete()
    }

    if (splashContainerAlpha.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = splashContainerAlpha.value
                }
                .background(Color.Black)
        ) {
            val currentRes = splashResList.getOrNull(currentResIndex) ?: R.drawable.img_splash_1
            val painter = painterResource(id = currentRes)

            Image(
                painter = painter,
                contentDescription = "RED Splash Screen Artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = (imageAlpha.value * ambientLight.value).coerceIn(0f, 1f)
                        scaleX = imageScale.value
                        scaleY = imageScale.value
                    }
            )

            // Pure black background overlay during initial fade-in
            if (imageAlpha.value < 1.0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 1.0f - imageAlpha.value
                        }
                        .background(Color.Black)
                )
            }
        }
    }
}
