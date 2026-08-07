package com.alijafari.red.astronomy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.alijafari.red.astronomy.R
import kotlinx.coroutines.delay

/**
 * Premium Splash Screen for "RED".
 * Displays the exact space art visual featuring dark cosmic eclipse, RED logo, celestial orbital paths,
 * developer credit "Designed and Developed by Ali Jafari", and silhouette landscape.
 */
@Composable
fun PremiumSplashScreen(
    onSplashComplete: () -> Unit
) {
    val splashAlpha = remember { Animatable(0f) }
    val splashScale = remember { Animatable(1.03f) }

    LaunchedEffect(Unit) {
        // Fade in and gently settle scale
        splashAlpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)
        )
        splashScale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 2200, easing = LinearOutSlowInEasing)
        )

        // Hold splash screen for complete viewing experience
        delay(1200)

        // Smooth fade out exit
        splashAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )

        onSplashComplete()
    }

    if (splashAlpha.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF03050B))
                .graphicsLayer {
                    alpha = splashAlpha.value
                    scaleX = splashScale.value
                    scaleY = splashScale.value
                }
                .testTag("premium_splash_screen"),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_splash_bg),
                contentDescription = "RED Splash Screen - Designed and Developed by Ali Jafari",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

