package com.alijafari.red.astronomy.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlin.math.sqrt

/**
 * Full-window settings host. The dialog panel starts at the settings button's window bounds, uses a
 * spring to expand into place, and waits for the reverse spring before it is removed from composition.
 */
@Composable
internal fun SettingsDialogMotion(
    sourceBounds: Rect?,
    onDismiss: () -> Unit,
    content: @Composable (panelModifier: Modifier, dismiss: () -> Unit) -> Unit,
) {
    var panelSize by remember { mutableStateOf(IntSize.Zero) }
    var safeWindowBounds by remember { mutableStateOf<Rect?>(null) }
    var hasStarted by remember { mutableStateOf(false) }
    var closeRequested by remember { mutableStateOf(false) }
    var isOpen by remember { mutableStateOf(false) }
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val transition = updateTransition(targetState = isOpen, label = "SettingsButtonLaunch")

    val windowCenter = safeWindowBounds?.center ?: Offset.Zero
    val panelWidth = panelSize.width.toFloat().coerceAtLeast(1f)
    val panelHeight = panelSize.height.toFloat().coerceAtLeast(1f)
    val startScale = remember(sourceBounds, panelSize) {
        if (sourceBounds == null || panelSize.width == 0 || panelSize.height == 0) {
            0.14f
        } else {
            val areaRatio = (sourceBounds.width / panelWidth) * (sourceBounds.height / panelHeight)
            sqrt(areaRatio).coerceIn(0.08f, 0.30f)
        }
    }
    val startTranslation = sourceBounds?.let { it.center - windowCenter } ?: Offset.Zero

    val scale by transition.animateFloat(
        transitionSpec = {
            if (targetState) spring(dampingRatio = 0.82f, stiffness = 360f)
            else spring(dampingRatio = 0.9f, stiffness = 500f)
        },
        label = "SettingsPanelScale",
    ) { open -> if (open) 1f else startScale }
    val translationX by transition.animateFloat(
        transitionSpec = {
            if (targetState) spring(dampingRatio = 0.82f, stiffness = 360f)
            else spring(dampingRatio = 0.9f, stiffness = 500f)
        },
        label = "SettingsPanelX",
    ) { open -> if (open) 0f else startTranslation.x }
    val translationY by transition.animateFloat(
        transitionSpec = {
            if (targetState) spring(dampingRatio = 0.82f, stiffness = 360f)
            else spring(dampingRatio = 0.9f, stiffness = 500f)
        },
        label = "SettingsPanelY",
    ) { open -> if (open) 0f else startTranslation.y }
    val scrimAlpha by transition.animateFloat(
        transitionSpec = { tween(if (targetState) 250 else 190) },
        label = "SettingsScrim",
    ) { open -> if (open) 0.46f else 0f }

    val requestDismiss: () -> Unit = {
        if (!closeRequested) {
            closeRequested = true
            isOpen = false
        }
    }

    LaunchedEffect(safeWindowBounds, panelSize, closeRequested) {
        if (!hasStarted && safeWindowBounds != null && panelSize != IntSize.Zero) {
            hasStarted = true
            if (!closeRequested) isOpen = true
        }
    }
    LaunchedEffect(closeRequested, transition.isRunning) {
        if (closeRequested && !transition.isRunning) latestOnDismiss()
    }

    val dialogView = LocalView.current
    val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
    SideEffect { dialogWindow?.setDimAmount(0f) }

    Dialog(
        onDismissRequest = requestDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = requestDismiss,
                    )
                    .testTag("settings_dialog_scrim"),
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .onGloballyPositioned { safeWindowBounds = it.boundsInWindow() },
                contentAlignment = Alignment.Center,
            ) {
                val safeWidth = (maxWidth - 32.dp).coerceAtLeast(0.dp).coerceAtMost(760.dp)
                val safeHeight = (maxHeight - 32.dp).coerceAtLeast(0.dp)
                val panelModifier = Modifier
                    .widthIn(max = safeWidth)
                    .fillMaxWidth()
                    .heightIn(max = safeHeight)
                    .fillMaxHeight()
                    .onSizeChanged { panelSize = it }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.translationX = translationX
                        this.translationY = translationY
                        alpha = if (hasStarted) 1f else 0f
                    }
                content(panelModifier, requestDismiss)
            }
        }
    }
}
