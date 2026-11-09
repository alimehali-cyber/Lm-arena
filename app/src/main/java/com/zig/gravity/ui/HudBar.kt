package com.zig.gravity.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.ui.theme.ChromeIconDisc
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.chromeTokens
import com.zig.gravity.physics.EngineConstants
import com.zig.gravity.util.PersianDigits

/**
 * §3.11 HUD — exactly the locked control set and nothing else persistent:
 * play/pause · the speed ladder · reset · trails · teaching · table colour · camera · add.
 *
 * The old dark/light theme toggle is gone: the two themes it switched between live on as the
 * `charcoal` and `paper` table surfaces, and the palette button now opens the surface picker (§6).
 *
 * ## Dress
 *
 * The bar used to be a teal instrument with its own glass fill, its own icon discs and its own
 * 11 sp labels — a small, complete design system of its own, sitting inside an app that has one.
 * It now wears that app's chrome (`ui/theme/ImmersiveChrome.kt`): the theme's elevated surface and
 * hairline for the platter, `RedTypographyTokens.badge` for the speed ladder, and the shared
 * [ChromeIconDisc] for every control, so a press springs the same way here as it does on the AR sky.
 *
 * The accent comes from the theme as well — no teal, no literal — which means the sandbox's own
 * `AppUiStyle` (DEFAULT / CYBER / SPATIAL) and `ThemeMode` (dark / light / wine) are respected by the
 * one control bar the screen has.
 */
@Composable
fun HudBar(
    paused: Boolean,
    speedIndex: Int,
    trailsVisible: Boolean,
    teachingEnabled: Boolean,
    persian: Boolean,
    onTogglePlay: () -> Unit,
    onSpeed: (Int) -> Unit,
    onReset: () -> Unit,
    onToggleTrails: () -> Unit,
    onToggleTeaching: () -> Unit,
    onOpenTableSurface: () -> Unit,
    cameraPanelOpen: Boolean = false,
    onToggleCameraPanel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val chrome = chromeTokens()
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(chrome.fill, CircleShape)
            .border(BorderStroke(1.dp, chrome.stroke), CircleShape)
            .padding(horizontal = 6.dp, vertical = 5.dp)
            .testTag("gravity_hud"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ChromeIconDisc(
            icon = if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
            contentDescription = if (persian) "پخش یا مکث" else "Play or pause",
            onClick = onTogglePlay,
            active = !paused,
            testTag = "hud_play_pause"
        )

        SpeedLadder(
            speedIndex = speedIndex,
            persian = persian,
            onSpeed = onSpeed
        )

        ChromeIconDisc(
            icon = Icons.Filled.Refresh,
            contentDescription = if (persian) "شروع دوباره" else "Reset",
            onClick = onReset,
            testTag = "hud_reset"
        )
        ChromeIconDisc(
            icon = Icons.Filled.Timeline,
            contentDescription = if (persian) "رد حرکت" else "Trails",
            onClick = onToggleTrails,
            active = trailsVisible,
            testTag = "hud_trails"
        )
        ChromeIconDisc(
            icon = Icons.Filled.School,
            contentDescription = if (persian) "آموزش" else "Teaching",
            onClick = onToggleTeaching,
            active = teachingEnabled,
            testTag = "hud_teaching"
        )
        ChromeIconDisc(
            icon = Icons.Filled.Palette,
            contentDescription = if (persian) "رنگ میز" else "Table colour",
            onClick = onOpenTableSurface,
            testTag = "hud_btn_table"
        )
        ChromeIconDisc(
            icon = Icons.Filled.CenterFocusStrong,
            contentDescription = if (persian) "دوربین" else "Camera",
            onClick = onToggleCameraPanel,
            active = cameraPanelOpen,
            testTag = "hud_camera"
        )
    }
}

/** §3.6b: speeds are labelled honestly — they multiply simulated time, never the timestep. */
private fun speedLabel(index: Int, persian: Boolean): String {
    val raw = EngineConstants.SPEED_LABELS[index]
    return if (persian) PersianDigits.convert(raw.replace("x", "×")) else raw
}

/**
 * The speed ladder, as one segmented capsule.
 *
 * The selected step is a solid accent tablet with the ink that reads on it; the rest are the panel's
 * own ink at the secondary weight. Selection cross-fades in 180 ms and the tablet springs, so holding
 * the ladder down and walking through 1× → 10× → 69× → 100× reads as one control changing state
 * instead of four separate chips being repainted.
 */
@Composable
private fun SpeedLadder(
    speedIndex: Int,
    persian: Boolean,
    onSpeed: (Int) -> Unit
) {
    val chrome = chromeTokens()
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(chrome.fillQuiet, CircleShape)
            .border(BorderStroke(1.dp, chrome.stroke.copy(alpha = 0.6f)), CircleShape)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in EngineConstants.SPEEDS.indices) {
            SpeedStep(
                label = speedLabel(i, persian),
                selected = i == speedIndex,
                onClick = { onSpeed(i) },
                tag = "hud_speed_$i"
            )
        }
    }
}

@Composable
private fun SpeedStep(label: String, selected: Boolean, onClick: () -> Unit, tag: String) {
    val chrome = chromeTokens()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 480f),
        label = "SpeedStepPress"
    )
    val container by animateColorAsState(
        targetValue = if (selected) chrome.accent else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "SpeedStepFill"
    )
    val ink by animateColorAsState(
        targetValue = if (selected) chrome.onAccent else chrome.textSecondary,
        animationSpec = tween(durationMillis = 180),
        label = "SpeedStepInk"
    )
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 34.dp, minHeight = 32.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(container, CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 5.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = RedTypographyTokens.badge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = ink,
            maxLines = 1
        )
    }
}

/** Kept for the panels that still need a plain, non-interactive glyph in the theme's dim ink. */
@Composable
internal fun HudGlyph(icon: ImageVector, description: String, tint: Color) {
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = tint,
        modifier = Modifier.padding(2.dp)
    )
}
