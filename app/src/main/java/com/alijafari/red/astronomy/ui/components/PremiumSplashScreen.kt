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
import kotlinx.coroutines.launch

/**
 * Splash Screen for "RED".
 * Displays the paper craft / papercut art style splash illustration with layered cut paper aesthetic.
 */
@Composable
fun PremiumSplashScreen(
    onSplashComplete: () -> Unit
) {
    val splashAlpha = remember { Animatable(0f) }
    val splashScale = remember { Animatable(1.02f) }
    val splashContainerAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Entrance animation
        launch {
            splashAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            splashScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
            )
        }

        // Hold display
        delay(2200)

        // Smooth exit fade out
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
                .background(Color(0xFF0F172A))
                .testTag("premium_splash_screen"),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_splash_screen),
                contentDescription = "RED Splash Screen - Designed and Developed by Ali Jafari",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = splashAlpha.value
                        scaleX = splashScale.value
                        scaleY = splashScale.value
                    },
                contentScale = ContentScale.Crop
            )
        }
    }
}

