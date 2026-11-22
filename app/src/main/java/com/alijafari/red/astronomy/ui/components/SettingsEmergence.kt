package com.alijafari.red.astronomy.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

/**
 * The settings menu's emergence.
 *
 * The menu grows out of the circular settings button when tapped open — scaling up from a sliver
 * the size of the button itself at the button's exact position — and collapses back into the same
 * button when closed. Both directions ride spring physics with a whisper of overshoot on the way
 * out and a clean, quick settle on the way back in, over an animated scrim.
 *
 * [origin] is the settings button's centre in root coordinates; when it is unknown the menu
 * still emerges from the lower-trailing corner where the button lives on average.
 */
@Composable
fun SettingsEmergenceDialog(
    origin: Offset?,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }
    var rootBounds by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(Unit) {
        // Emergence: a gentle overshoot, like the menu springing out of the button.
        progress.animateTo(
            1f,
            spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
        )
    }

    fun requestClose() {
        if (!closing) {
            closing = true
            scope.launch {
                // Return: no overshoot, quick and clean, back into the button.
                progress.animateTo(
                    0f,
                    spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)
                )
                onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = { requestClose() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        HideStatusBar()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { rootBounds = it.boundsInRoot() }
        ) {
            // Scrim, lit with the same progress so it breathes with the menu.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.62f * progress.value))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { requestClose() }
            )

            // The menu itself: scaled around the settings button's exact pivot so the first
            // sliver of the panel sits precisely on the button, then unfolds from there.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = progress.value
                        scaleX = lerp(0.10f, 1f, p)
                        scaleY = lerp(0.10f, 1f, p)
                        alpha = p
                        val root = rootBounds
                        transformOrigin =
                            if (root != null && root.width > 0f && root.height > 0f && origin != null) {
                                TransformOrigin(
                                    ((origin.x - root.left) / root.width).coerceIn(0f, 1f),
                                    ((origin.y - root.top) / root.height).coerceIn(0f, 1f)
                                )
                            } else {
                                // Fallback pivot: just above the trailing end of the dock.
                                TransformOrigin(0.62f, 0.94f)
                            }
                    },
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}
