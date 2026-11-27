package com.alijafari.red.astronomy.ui.demo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.ui.theme.solidSurface

/** Ephemeral presentation state, deliberately never persisted in user settings. */
enum class ShowcaseScene { IDLE, HOME, BLACK_HOLE, GRAVITY, SATELLITES, AR, TIME, END }
object Showcase {
    var scene by mutableStateOf(ShowcaseScene.IDLE)
    val active get() = scene != ShowcaseScene.IDLE
    fun start() { if (!active) scene = ShowcaseScene.HOME }
    fun stop() { scene = ShowcaseScene.IDLE }
}

@Composable
fun ShowcaseButton() {
    if (!Showcase.active) {
        Box(
            modifier = Modifier.size(38.dp)
                .solidSurface(shape = CircleShape, color = Color.Black.copy(alpha = 0.38f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.32f)))
                .clickable { Showcase.start() }.testTag("home_showcase_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "نمایش زیگ", tint = Color.White,
                modifier = Modifier.size(19.dp))
        }
    }
}
