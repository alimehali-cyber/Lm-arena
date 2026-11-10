package com.zig.antar.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.domain.AppUiStyle
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.cyber.CyberTheme
import com.alijafari.red.astronomy.ui.spatial.SpatialTheme
import com.alijafari.red.astronomy.ui.theme.RedCornerRadius
import com.alijafari.red.astronomy.ui.theme.RedElevation
import com.alijafari.red.astronomy.ui.theme.RedSurface
import com.alijafari.red.astronomy.ui.theme.RedTheme
import com.alijafari.red.astronomy.ui.theme.RedTypography
import com.alijafari.red.astronomy.ui.theme.squircleShape
import com.zig.antar.config.AntarCameraMode
import com.zig.antar.config.AntarMassPreset
import com.zig.antar.config.AntarObjectMode
import com.zig.antar.config.AntarPreset
import com.zig.antar.config.AntarQualityTier
import com.zig.antar.config.AntarSimulationSettings
import com.zig.antar.config.AntarTelemetry
import com.zig.antar.physics.AntarKerrMetrics
import com.zig.antar.physics.AntarNumberFormat
import kotlin.math.pow
import kotlin.math.roundToInt

@Immutable
internal data class AntarChromeTokens(
    val sheetShape: Shape,
    val cardShape: Shape,
    val chipShape: Shape,
    val surfaceColor: Color,
    val borderColor: Color,
    val accentColor: Color,
    val accentSubtleColor: Color,
    val signalDotColor: Color
)

@Composable
internal fun rememberAntarChromeTokens(
    uiStyle: AppUiStyle,
    themeMode: ThemeMode
): AntarChromeTokens {
    val redColors = RedTheme.colors
    val spatial = SpatialTheme.palette
    val cyber = CyberTheme.palette
    return remember(uiStyle, themeMode, redColors, spatial, cyber) {
        when (uiStyle) {
            AppUiStyle.CYBER -> AntarChromeTokens(
                sheetShape = CutCornerShape(14.dp),
                cardShape = CutCornerShape(10.dp),
                chipShape = CutCornerShape(8.dp),
                surfaceColor = redColors.surfaceElevated,
                borderColor = cyber.cutoutViewportBorder,
                accentColor = cyber.primaryAccent,
                accentSubtleColor = cyber.primaryAccent.copy(alpha = if (cyber.isDark) 0.22f else 0.14f),
                signalDotColor = cyber.signalDot
            )
            AppUiStyle.SPATIAL -> AntarChromeTokens(
                sheetShape = squircleShape(28.dp),
                cardShape = squircleShape(18.dp),
                chipShape = squircleShape(RedCornerRadius.pill),
                surfaceColor = redColors.surfaceElevated,
                borderColor = spatial.rimSideBorder,
                accentColor = spatial.accentInk,
                accentSubtleColor = spatial.neonGlow.copy(alpha = if (spatial.isDark) 0.24f else 0.16f),
                signalDotColor = spatial.neonGlow
            )
            AppUiStyle.QUARTZ -> AntarChromeTokens(
                sheetShape = squircleShape(32.dp),
                cardShape = squircleShape(16.dp),
                chipShape = squircleShape(RedCornerRadius.pill),
                surfaceColor = redColors.surfaceElevated,
                borderColor = redColors.border,
                accentColor = redColors.accentRed,
                accentSubtleColor = redColors.accentRedSubtle,
                signalDotColor = redColors.accentGold
            )
            AppUiStyle.DEFAULT -> AntarChromeTokens(
                sheetShape = squircleShape(28.dp),
                cardShape = squircleShape(16.dp),
                chipShape = squircleShape(RedCornerRadius.pill),
                surfaceColor = redColors.surfaceElevated,
                borderColor = redColors.border,
                accentColor = redColors.accentRed,
                accentSubtleColor = redColors.accentRedSubtle,
                signalDotColor = redColors.accentGold
            )
        }
    }
}

/**
 * Compact floating live telemetry pill & minimalist HUD visibility toggle.
 * Intentionally avoids any top navigation header or back button (system back handles exit).
 */
@Composable
internal fun AntarFloatingStatusOverlay(
    settings: AntarSimulationSettings,
    telemetry: AntarTelemetry,
    showUi: Boolean,
    isPersian: Boolean,
    chrome: AntarChromeTokens,
    onToggleShowUi: () -> Unit,
    onExitPlunge: () -> Unit,
    onStopTour: () -> Unit,
    onRestartBinary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        if (showUi && settings.showTelemetryHud) {
            RedSurface(
                shape = chrome.chipShape,
                color = chrome.surfaceColor,
                border = BorderStroke(1.dp, chrome.borderColor),
                elevation = RedElevation.popover,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .testTag("antar_telemetry_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    telemetry.insideHorizon -> RedTheme.colors.statusError
                                    settings.paused -> RedTheme.colors.statusWarning
                                    else -> chrome.signalDotColor
                                }
                            )
                    )
                    val rReadout = if (telemetry.plungeActive) telemetry.plungeRadiusM else telemetry.cameraDistanceM
                    val summary = buildString {
                        append(AntarStrings.objectModeLabel(settings.objectMode, isPersian))
                        append("  ·  ")
                        if (settings.objectMode == AntarObjectMode.BINARY_MERGER) {
                            append("d=")
                            append(AntarNumberFormat.decimal(telemetry.binarySeparationM, 1, isPersian))
                            append("M")
                        } else {
                            append("a=")
                            append(AntarNumberFormat.decimal(settings.spin, 2, isPersian))
                            append("  ·  r=")
                            append(AntarNumberFormat.decimal(rReadout, 1, isPersian))
                            append("M")
                        }
                        append("  ·  ")
                        append(AntarNumberFormat.integer(telemetry.fps, isPersian))
                        append(" fps")
                    }
                    Text(
                        text = summary,
                        style = RedTypography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = RedTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (telemetry.plungeActive) {
                        AntarInlineBadgeButton(
                            label = if (telemetry.insideHorizon) {
                                AntarStrings.insideHorizonBadge(isPersian)
                            } else {
                                AntarStrings.exitPlunge(isPersian)
                            },
                            chrome = chrome,
                            onClick = onExitPlunge
                        )
                    } else if (telemetry.tourActive) {
                        AntarInlineBadgeButton(
                            label = AntarStrings.stopTour(isPersian),
                            chrome = chrome,
                            onClick = onStopTour
                        )
                    } else if (settings.objectMode == AntarObjectMode.BINARY_MERGER && telemetry.binaryMerged) {
                        AntarInlineBadgeButton(
                            label = AntarStrings.restartInspiral(isPersian),
                            chrome = chrome,
                            onClick = onRestartBinary
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Minimalist HUD toggle button (44dp touch target)
        val eyeDesc = if (showUi) AntarStrings.hideHud(isPersian) else AntarStrings.showHud(isPersian)
        RedSurface(
            shape = chrome.chipShape,
            color = chrome.surfaceColor.copy(alpha = if (showUi) 0.96f else 0.72f),
            border = BorderStroke(1.dp, chrome.borderColor),
            elevation = RedElevation.popover,
            onClick = onToggleShowUi,
            modifier = Modifier
                .size(44.dp)
                .semantics { contentDescription = eyeDesc }
                .testTag("antar_toggle_hud")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (showUi) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    tint = RedTheme.colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AntarInlineBadgeButton(
    label: String,
    chrome: AntarChromeTokens,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(chrome.chipShape)
            .background(chrome.accentSubtleColor)
            .border(1.dp, chrome.accentColor.copy(alpha = 0.55f), chrome.chipShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = RedTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = chrome.accentColor,
            maxLines = 1
        )
    }
}

/**
 * Floating thumb-reach dock shown at the bottom of the screen when the full control sheet is
 * collapsed. Gives one-tap access to presets, camera mode, play/pause, recenter, and settings.
 */
@Composable
internal fun AntarQuickDockBar(
    settings: AntarSimulationSettings,
    isPersian: Boolean,
    chrome: AntarChromeTokens,
    onOpenPresets: () -> Unit,
    onCycleCameraMode: () -> Unit,
    onTogglePause: () -> Unit,
    onResetView: () -> Unit,
    onOpenControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    RedSurface(
        shape = chrome.sheetShape,
        color = chrome.surfaceColor,
        border = BorderStroke(1.dp, chrome.borderColor),
        elevation = RedElevation.floating,
        modifier = modifier.testTag("antar_quick_dock")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Active preset / system pill
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(chrome.chipShape)
                    .background(chrome.accentSubtleColor)
                    .border(1.dp, chrome.accentColor.copy(alpha = 0.45f), chrome.chipShape)
                    .clickable(onClick = onOpenPresets)
                    .padding(horizontal = 12.dp)
                    .testTag("antar_quick_presets"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = chrome.accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    val activeLabel = settings.activePreset?.let { AntarStrings.presetTitle(it, isPersian) }
                        ?: AntarStrings.objectModeLabel(settings.objectMode, isPersian)
                    Text(
                        text = activeLabel,
                        style = RedTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = RedTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Camera trajectory chip
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(chrome.chipShape)
                    .background(RedTheme.colors.textPrimary.copy(alpha = 0.07f))
                    .border(1.dp, chrome.borderColor, chrome.chipShape)
                    .clickable(onClick = onCycleCameraMode)
                    .padding(horizontal = 12.dp)
                    .testTag("antar_quick_camera_mode"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = AntarStrings.cameraModeLabel(settings.cameraMode, isPersian),
                    style = RedTypography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = RedTheme.colors.textPrimary,
                    maxLines = 1
                )
            }

            // Play / Pause
            AntarIconButton(
                icon = if (settings.paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDesc = if (settings.paused) AntarStrings.play(isPersian) else AntarStrings.pause(isPersian),
                chrome = chrome,
                active = settings.paused,
                onClick = onTogglePause,
                tag = "antar_quick_pause"
            )

            // Re-center view
            AntarIconButton(
                icon = Icons.Default.CenterFocusStrong,
                contentDesc = AntarStrings.resetView(isPersian),
                chrome = chrome,
                active = false,
                onClick = onResetView,
                tag = "antar_quick_recenter"
            )

            // Expand full settings sheet
            AntarIconButton(
                icon = Icons.Default.Tune,
                contentDesc = AntarStrings.controls(isPersian),
                chrome = chrome,
                active = true,
                onClick = onOpenControls,
                tag = "antar_open_controls"
            )
        }
    }
}

@Composable
private fun AntarIconButton(
    icon: ImageVector,
    contentDesc: String,
    chrome: AntarChromeTokens,
    active: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(chrome.chipShape)
            .background(
                if (active) chrome.accentSubtleColor
                else RedTheme.colors.textPrimary.copy(alpha = 0.07f)
            )
            .border(
                1.dp,
                if (active) chrome.accentColor.copy(alpha = 0.5f) else chrome.borderColor,
                chrome.chipShape
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = contentDesc }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) chrome.accentColor else RedTheme.colors.textPrimary,
            modifier = Modifier.size(19.dp)
        )
    }
}

/**
 * Full 4-tab simulation control sheet (`Scene`, `Physics`, `Disk & Sky`, `View & Touch`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AntarControlPanel(
    settings: AntarSimulationSettings,
    telemetry: AntarTelemetry,
    selectedTab: AntarControlTab,
    onSelectTab: (AntarControlTab) -> Unit,
    onUpdateSettings: (AntarSimulationSettings) -> Unit,
    onApplyPreset: (AntarPreset) -> Unit,
    onResetView: () -> Unit,
    onResetAll: () -> Unit,
    onRestartBinary: () -> Unit,
    onClosePanel: () -> Unit,
    isPersian: Boolean,
    chrome: AntarChromeTokens,
    maxHeight: Dp,
    modifier: Modifier = Modifier
) {
    RedSurface(
        shape = chrome.sheetShape,
        color = chrome.surfaceColor,
        border = BorderStroke(1.dp, chrome.borderColor),
        elevation = RedElevation.modal,
        modifier = modifier
            .heightIn(max = maxHeight)
            .testTag("antar_control_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Sheet drag-handle + title + collapse action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AntarStrings.title(isPersian),
                        style = RedTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = RedTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = AntarStrings.subtitle(isPersian),
                        style = RedTypography.labelSmall,
                        color = RedTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AntarIconButton(
                        icon = if (settings.paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDesc = if (settings.paused) AntarStrings.play(isPersian) else AntarStrings.pause(isPersian),
                        chrome = chrome,
                        active = settings.paused,
                        onClick = {
                            onUpdateSettings(settings.copy(paused = !settings.paused))
                        },
                        tag = "antar_panel_pause"
                    )
                    AntarIconButton(
                        icon = Icons.Default.ExpandMore,
                        contentDesc = AntarStrings.closeControls(isPersian),
                        chrome = chrome,
                        active = false,
                        onClick = onClosePanel,
                        tag = "antar_close_controls"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4-Tab Segmented Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(chrome.cardShape)
                    .background(RedTheme.colors.textPrimary.copy(alpha = 0.06f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AntarControlTab.entries.forEach { tab ->
                    val selected = tab == selectedTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(chrome.chipShape)
                            .background(if (selected) chrome.accentSubtleColor else Color.Transparent)
                            .then(
                                if (selected) {
                                    Modifier.border(1.dp, chrome.accentColor.copy(alpha = 0.55f), chrome.chipShape)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onSelectTab(tab) }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .testTag("antar_tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = AntarStrings.tabLabel(tab, isPersian),
                            style = RedTypography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) chrome.accentColor else RedTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable Tab Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    AntarControlTab.SCENE -> SceneTabContent(
                        settings = settings,
                        isPersian = isPersian,
                        chrome = chrome,
                        onUpdateSettings = onUpdateSettings,
                        onApplyPreset = onApplyPreset
                    )
                    AntarControlTab.SPACETIME -> SpacetimeTabContent(
                        settings = settings,
                        telemetry = telemetry,
                        isPersian = isPersian,
                        chrome = chrome,
                        onUpdateSettings = onUpdateSettings,
                        onRestartBinary = onRestartBinary
                    )
                    AntarControlTab.DISK_SKY -> DiskSkyTabContent(
                        settings = settings,
                        isPersian = isPersian,
                        chrome = chrome,
                        onUpdateSettings = onUpdateSettings
                    )
                    AntarControlTab.VIEW_TOUCH -> ViewTouchTabContent(
                        settings = settings,
                        isPersian = isPersian,
                        chrome = chrome,
                        onUpdateSettings = onUpdateSettings,
                        onResetView = onResetView,
                        onResetAll = onResetAll
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SceneTabContent(
    settings: AntarSimulationSettings,
    isPersian: Boolean,
    chrome: AntarChromeTokens,
    onUpdateSettings: (AntarSimulationSettings) -> Unit,
    onApplyPreset: (AntarPreset) -> Unit
) {
    AntarSectionHeader(AntarStrings.sectionObjectType(isPersian))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AntarObjectMode.entries.forEach { mode ->
            val selected = settings.objectMode == mode
            AntarChoiceChip(
                label = AntarStrings.objectModeLabel(mode, isPersian),
                selected = selected,
                chrome = chrome,
                modifier = Modifier.weight(1f),
                onClick = {
                    val presetForObject = when (mode) {
                        AntarObjectMode.KERR -> AntarPreset.PHYSICAL
                        AntarObjectMode.TON_618 -> AntarPreset.TON_618_QUASAR
                        AntarObjectMode.BINARY_MERGER -> AntarPreset.BINARY_INSPIRAL
                    }
                    if (!selected) onApplyPreset(presetForObject)
                }
            )
        }
    }

    AntarSectionHeader(AntarStrings.sectionCameraMode(isPersian))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AntarCameraMode.entries.forEach { mode ->
            val enabled = !(mode == AntarCameraMode.PLUNGE && settings.objectMode == AntarObjectMode.BINARY_MERGER)
            if (enabled) {
                AntarChoiceChip(
                    label = AntarStrings.cameraModeLabel(mode, isPersian),
                    selected = settings.cameraMode == mode,
                    chrome = chrome,
                    onClick = {
                        onUpdateSettings(settings.copy(cameraMode = mode, activePreset = null).normalize())
                    }
                )
            }
        }
    }

    AntarSectionHeader(AntarStrings.sectionPresets(isPersian))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AntarPreset.entries.forEach { preset ->
            val selected = settings.activePreset == preset
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(chrome.cardShape)
                    .background(
                        if (selected) chrome.accentSubtleColor
                        else RedTheme.colors.textPrimary.copy(alpha = 0.05f)
                    )
                    .border(
                        1.dp,
                        if (selected) chrome.accentColor.copy(alpha = 0.6f) else chrome.borderColor,
                        chrome.cardShape
                    )
                    .clickable { onApplyPreset(preset) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("antar_preset_${preset.jsKey}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AntarStrings.presetTitle(preset, isPersian),
                            style = RedTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (selected) chrome.accentColor else RedTheme.colors.textPrimary
                        )
                        Text(
                            text = AntarStrings.presetSubtitle(preset, isPersian),
                            style = RedTypography.labelSmall,
                            color = RedTheme.colors.textSecondary
                        )
                    }
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(chrome.accentColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpacetimeTabContent(
    settings: AntarSimulationSettings,
    telemetry: AntarTelemetry,
    isPersian: Boolean,
    chrome: AntarChromeTokens,
    onUpdateSettings: (AntarSimulationSettings) -> Unit,
    onRestartBinary: () -> Unit
) {
    AntarSectionHeader(AntarStrings.sectionObservables(isPersian))
    val observerR = if (telemetry.plungeActive) telemetry.plungeRadiusM else settings.distanceM
    val observables = remember(settings.spin, observerR) {
        AntarKerrMetrics.computeObservables(settings.spin, observerR)
    }

    if (settings.objectMode == AntarObjectMode.BINARY_MERGER) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AntarObservableCard(
                label = AntarStrings.obsBinarySep(isPersian),
                value = "${AntarNumberFormat.decimal(telemetry.binarySeparationM, 2, isPersian)} M",
                chrome = chrome,
                modifier = Modifier.weight(1f)
            )
            AntarObservableCard(
                label = AntarStrings.obsOrbitalSpeed(isPersian),
                value = "${AntarNumberFormat.decimal(telemetry.binaryVelocityC, 3, isPersian)} c",
                chrome = chrome,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val mhz = telemetry.binaryGwFreqHz * 1e6
            AntarObservableCard(
                label = AntarStrings.obsGwFreq(isPersian),
                value = "${AntarNumberFormat.decimal(mhz, 2, isPersian)} μHz",
                chrome = chrome,
                modifier = Modifier.weight(1f)
            )
            AntarObservableCard(
                label = AntarStrings.obsHorizon(isPersian),
                value = "${AntarNumberFormat.decimal(observables.horizonRadiusM, 3, isPersian)} M",
                chrome = chrome,
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AntarObservableCard(
                label = AntarStrings.obsHorizon(isPersian),
                value = "${AntarNumberFormat.decimal(observables.horizonRadiusM, 3, isPersian)} M",
                chrome = chrome,
                modifier = Modifier.weight(1f)
            )
            AntarObservableCard(
                label = AntarStrings.obsIsco(isPersian),
                value = "${AntarNumberFormat.decimal(observables.iscoProgradeM, 2, isPersian)} M",
                chrome = chrome,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AntarObservableCard(
                label = AntarStrings.obsPhotonRing(isPersian),
                value = "${AntarNumberFormat.decimal(observables.photonOrbitProgradeM, 2, isPersian)} M",
                chrome = chrome,
                modifier = Modifier.weight(1f)
            )
            AntarObservableCard(
                label = AntarStrings.obsTimeDilation(isPersian),
                value = if (observables.gravitationalTimeDilation > 0.0) {
                    "${AntarNumberFormat.decimal(observables.gravitationalTimeDilation * 100.0, 1, isPersian)}%"
                } else {
                    "—"
                },
                chrome = chrome,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (settings.objectMode == AntarObjectMode.BINARY_MERGER) {
        AntarSectionHeader(AntarStrings.sectionBinary(isPersian))
        AntarSliderRow(
            label = AntarStrings.paramBinaryQ(isPersian),
            valueText = AntarNumberFormat.decimal(settings.binaryMassRatio, 2, isPersian),
            value = settings.binaryMassRatio.toFloat(),
            range = 0.1f..1.0f,
            chrome = chrome,
            onValueChange = {
                onUpdateSettings(settings.copy(binaryMassRatio = it.toDouble(), activePreset = null).normalize())
            }
        )
        AntarSliderRow(
            label = AntarStrings.paramBinarySpin1(isPersian),
            valueText = AntarNumberFormat.decimal(settings.binarySpin1, 2, isPersian),
            value = settings.binarySpin1.toFloat(),
            range = 0.0f..0.99f,
            chrome = chrome,
            onValueChange = {
                onUpdateSettings(settings.copy(binarySpin1 = it.toDouble(), activePreset = null).normalize())
            }
        )
        AntarSliderRow(
            label = AntarStrings.paramBinarySpin2(isPersian),
            valueText = AntarNumberFormat.decimal(settings.binarySpin2, 2, isPersian),
            value = settings.binarySpin2.toFloat(),
            range = 0.0f..0.99f,
            chrome = chrome,
            onValueChange = {
                onUpdateSettings(settings.copy(binarySpin2 = it.toDouble(), activePreset = null).normalize())
            }
        )
        AntarSliderRow(
            label = AntarStrings.paramBinarySep(isPersian),
            valueText = "${AntarNumberFormat.decimal(settings.binarySeparationM, 1, isPersian)} M",
            value = settings.binarySeparationM.toFloat(),
            range = 8f..26f,
            chrome = chrome,
            onValueChange = {
                onUpdateSettings(settings.copy(binarySeparationM = it.toDouble(), activePreset = null).normalize())
            }
        )
        AntarSliderRow(
            label = AntarStrings.paramBinaryRate(isPersian),
            valueText = "${AntarNumberFormat.decimal(settings.binaryPlaybackSpeed, 2, isPersian)}×",
            value = settings.binaryPlaybackSpeed.toFloat(),
            range = 0.25f..4.0f,
            chrome = chrome,
            onValueChange = {
                onUpdateSettings(settings.copy(binaryPlaybackSpeed = it.toDouble(), activePreset = null).normalize())
            }
        )
        AntarActionRowButton(
            label = AntarStrings.restartInspiral(isPersian),
            icon = Icons.Default.Refresh,
            chrome = chrome,
            onClick = onRestartBinary
        )
    } else {
        AntarSectionHeader(AntarStrings.sectionKerrMetric(isPersian))
        AntarSliderRow(
            label = AntarStrings.paramSpin(isPersian),
            valueText = AntarNumberFormat.decimal(settings.spin, 3, isPersian),
            value = settings.spin.toFloat(),
            range = 0.0f..0.998f,
            chrome = chrome,
            onValueChange = {
                onUpdateSettings(settings.copy(spin = it.toDouble(), activePreset = null).normalize())
            }
        )

        Text(
            text = AntarStrings.paramMass(isPersian),
            style = RedTypography.labelMedium,
            color = RedTheme.colors.textSecondary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AntarMassPreset.entries.forEach { mass ->
                AntarChoiceChip(
                    label = AntarStrings.massLabel(mass, isPersian),
                    selected = settings.massPreset == mass,
                    chrome = chrome,
                    onClick = {
                        onUpdateSettings(settings.copy(massPreset = mass, activePreset = null))
                    }
                )
            }
        }

        AntarSliderRow(
            label = AntarStrings.paramBeaming(isPersian),
            valueText = "${AntarNumberFormat.integer((settings.beamingStrength * 100).roundToInt(), isPersian)}%",
            value = settings.beamingStrength.toFloat(),
            range = 0.0f..1.6f,
            chrome = chrome,
            onValueChange = {
                onUpdateSettings(settings.copy(beamingStrength = it.toDouble(), activePreset = null).normalize())
            }
        )
        AntarSliderRow(
            label = AntarStrings.paramTimeScale(isPersian),
            valueText = "${AntarNumberFormat.decimal(settings.timeScale, 2, isPersian)}×",
            value = settings.timeScale.toFloat(),
            range = 0.1f..3.0f,
            chrome = chrome,
            onValueChange = {
                onUpdateSettings(settings.copy(timeScale = it.toDouble()).normalize())
            }
        )
        if (settings.cameraMode == AntarCameraMode.PLUNGE) {
            AntarSliderRow(
                label = AntarStrings.paramPlungeSpeed(isPersian),
                valueText = "${AntarNumberFormat.decimal(settings.plungeSpeed, 2, isPersian)}×",
                value = settings.plungeSpeed.toFloat(),
                range = 0.25f..3.0f,
                chrome = chrome,
                onValueChange = {
                    onUpdateSettings(settings.copy(plungeSpeed = it.toDouble()).normalize())
                }
            )
        }
        AntarSwitchRow(
            label = AntarStrings.paramBolometric(isPersian),
            checked = settings.bolometricLocked,
            chrome = chrome,
            onCheckedChange = {
                onUpdateSettings(settings.copy(bolometricLocked = it, activePreset = null))
            }
        )
    }
}

@Composable
private fun DiskSkyTabContent(
    settings: AntarSimulationSettings,
    isPersian: Boolean,
    chrome: AntarChromeTokens,
    onUpdateSettings: (AntarSimulationSettings) -> Unit
) {
    AntarSectionHeader(AntarStrings.sectionDisk(isPersian))
    AntarSwitchRow(
        label = AntarStrings.paramDiskEnabled(isPersian),
        checked = settings.diskEnabled,
        chrome = chrome,
        onCheckedChange = { onUpdateSettings(settings.copy(diskEnabled = it, activePreset = null)) }
    )
    if (settings.diskEnabled) {
        AntarSwitchRow(
            label = AntarStrings.paramThickDisk(isPersian),
            checked = settings.thickDiskMode,
            chrome = chrome,
            onCheckedChange = { onUpdateSettings(settings.copy(thickDiskMode = it, activePreset = null)) }
        )
        AntarSwitchRow(
            label = AntarStrings.paramPhysicalTemp(isPersian),
            checked = settings.physicalDiskTemp,
            chrome = chrome,
            onCheckedChange = { onUpdateSettings(settings.copy(physicalDiskTemp = it, activePreset = null)) }
        )

        if (settings.physicalDiskTemp) {
            // Logarithmic slider for Eddington ratio [10^-3 .. 2.0]
            val logVal = kotlin.math.log10(settings.eddingtonRatio.coerceIn(1e-3, 2.0)).toFloat()
            AntarSliderRow(
                label = AntarStrings.paramEddington(isPersian),
                valueText = AntarNumberFormat.decimal(settings.eddingtonRatio, 3, isPersian),
                value = logVal,
                range = -3.0f..0.3f,
                chrome = chrome,
                onValueChange = { lv ->
                    val edd = 10.0.pow(lv.toDouble())
                    onUpdateSettings(settings.copy(eddingtonRatio = edd, activePreset = null).normalize())
                }
            )
        } else {
            AntarSliderRow(
                label = AntarStrings.paramPeakTemp(isPersian),
                valueText = "${AntarNumberFormat.integer(settings.peakTemperatureK.roundToInt(), isPersian)} K",
                value = settings.peakTemperatureK.toFloat(),
                range = 2200f..36000f,
                chrome = chrome,
                onValueChange = {
                    onUpdateSettings(settings.copy(peakTemperatureK = it.toDouble(), activePreset = null).normalize())
                }
            )
        }

        AntarSliderRow(
            label = AntarStrings.paramOuterRadius(isPersian),
            valueText = "${AntarNumberFormat.decimal(settings.outerRadiusM, 1, isPersian)} M",
            value = settings.outerRadiusM.toFloat(),
            range = 7f..48f,
            chrome = chrome,
            onValueChange = {
                onUpdateSettings(settings.copy(outerRadiusM = it.toDouble(), activePreset = null).normalize())
            }
        )
        AntarSliderRow(
            label = AntarStrings.paramDensity(isPersian),
            valueText = AntarNumberFormat.decimal(settings.opticalDensity, 2, isPersian),
            value = settings.opticalDensity.toFloat(),
            range = 0.05f..3.0f,
            chrome = chrome,
            onValueChange = {
                onUpdateSettings(settings.copy(opticalDensity = it.toDouble(), activePreset = null).normalize())
            }
        )
        AntarSliderRow(
            label = AntarStrings.paramTurbulence(isPersian),
            valueText = "${AntarNumberFormat.integer((settings.turbulence * 100).roundToInt(), isPersian)}%",
            value = settings.turbulence.toFloat(),
            range = 0.0f..1.5f,
            chrome = chrome,
            onValueChange = {
                onUpdateSettings(settings.copy(turbulence = it.toDouble(), activePreset = null).normalize())
            }
        )
        AntarSliderRow(
            label = AntarStrings.paramHaze(isPersian),
            valueText = "${AntarNumberFormat.integer((settings.coronalHaze * 100).roundToInt(), isPersian)}%",
            value = settings.coronalHaze.toFloat(),
            range = 0.0f..1.5f,
            chrome = chrome,
            onValueChange = {
                onUpdateSettings(settings.copy(coronalHaze = it.toDouble(), activePreset = null).normalize())
            }
        )
        AntarSliderRow(
            label = AntarStrings.paramDiskBright(isPersian),
            valueText = "${AntarNumberFormat.decimal(settings.diskBrightness, 2, isPersian)}×",
            value = settings.diskBrightness.toFloat(),
            range = 0.1f..3.0f,
            chrome = chrome,
            onValueChange = {
                onUpdateSettings(settings.copy(diskBrightness = it.toDouble(), activePreset = null).normalize())
            }
        )

        if (settings.objectMode == AntarObjectMode.TON_618) {
            AntarSectionHeader(AntarStrings.sectionQuasar(isPersian))
            AntarSliderRow(
                label = AntarStrings.paramCorona(isPersian),
                valueText = AntarNumberFormat.decimal(settings.xrayCorona, 2, isPersian),
                value = settings.xrayCorona.toFloat(),
                range = 0.0f..2.0f,
                chrome = chrome,
                onValueChange = {
                    onUpdateSettings(settings.copy(xrayCorona = it.toDouble(), activePreset = null).normalize())
                }
            )
            AntarSliderRow(
                label = AntarStrings.paramWind(isPersian),
                valueText = AntarNumberFormat.decimal(settings.uvDiskWind, 2, isPersian),
                value = settings.uvDiskWind.toFloat(),
                range = 0.0f..2.0f,
                chrome = chrome,
                onValueChange = {
                    onUpdateSettings(settings.copy(uvDiskWind = it.toDouble(), activePreset = null).normalize())
                }
            )
            AntarSliderRow(
                label = AntarStrings.paramBlr(isPersian),
                valueText = AntarNumberFormat.decimal(settings.broadLineRegion, 2, isPersian),
                value = settings.broadLineRegion.toFloat(),
                range = 0.0f..2.0f,
                chrome = chrome,
                onValueChange = {
                    onUpdateSettings(settings.copy(broadLineRegion = it.toDouble(), activePreset = null).normalize())
                }
            )
        }
    }

    AntarSectionHeader(AntarStrings.sectionSky(isPersian))
    AntarSwitchRow(
        label = AntarStrings.paramStars(isPersian),
        checked = settings.starsEnabled,
        chrome = chrome,
        onCheckedChange = { onUpdateSettings(settings.copy(starsEnabled = it, activePreset = null)) }
    )
    if (settings.starsEnabled) {
        AntarSliderRow(
            label = AntarStrings.paramStarBright(isPersian),
            valueText = AntarNumberFormat.decimal(settings.starBrightness, 2, isPersian),
            value = settings.starBrightness.toFloat(),
            range = 0.0f..2.0f,
            chrome = chrome,
            onValueChange = {
                onUpdateSettings(settings.copy(starBrightness = it.toDouble()).normalize())
            }
        )
        AntarSliderRow(
            label = AntarStrings.paramSkyBright(isPersian),
            valueText = AntarNumberFormat.decimal(settings.skyBrightness, 2, isPersian),
            value = settings.skyBrightness.toFloat(),
            range = 0.0f..1.5f,
            chrome = chrome,
            onValueChange = {
                onUpdateSettings(settings.copy(skyBrightness = it.toDouble()).normalize())
            }
        )
    }
    AntarSwitchRow(
        label = AntarStrings.paramJet(isPersian),
        checked = settings.relativisticJetEnabled,
        chrome = chrome,
        onCheckedChange = { onUpdateSettings(settings.copy(relativisticJetEnabled = it, activePreset = null)) }
    )
    AntarSwitchRow(
        label = AntarStrings.paramGrid(isPersian),
        checked = settings.lensGridEnabled,
        chrome = chrome,
        onCheckedChange = { onUpdateSettings(settings.copy(lensGridEnabled = it, activePreset = null)) }
    )
    AntarSwitchRow(
        label = AntarStrings.paramEht(isPersian),
        checked = settings.ehtFilterEnabled,
        chrome = chrome,
        onCheckedChange = { onUpdateSettings(settings.copy(ehtFilterEnabled = it, activePreset = null)) }
    )
}

@Composable
private fun ViewTouchTabContent(
    settings: AntarSimulationSettings,
    isPersian: Boolean,
    chrome: AntarChromeTokens,
    onUpdateSettings: (AntarSimulationSettings) -> Unit,
    onResetView: () -> Unit,
    onResetAll: () -> Unit
) {
    AntarSectionHeader(AntarStrings.sectionOptics(isPersian))
    val isMerger = settings.objectMode == AntarObjectMode.BINARY_MERGER
    val minDist = AntarKerrMetrics.minOrbitDistanceM(settings.spin, isMerger, settings.binarySeparationM).toFloat()
    AntarSliderRow(
        label = AntarStrings.paramDistance(isPersian),
        valueText = "${AntarNumberFormat.decimal(settings.distanceM, 1, isPersian)} M",
        value = settings.distanceM.toFloat().coerceIn(minDist, 120f),
        range = minDist..120f,
        chrome = chrome,
        onValueChange = {
            onUpdateSettings(settings.copy(distanceM = it.toDouble(), activePreset = null).normalize())
        }
    )
    AntarSliderRow(
        label = AntarStrings.paramInclination(isPersian),
        valueText = "${AntarNumberFormat.decimal(settings.inclinationDeg, 1, isPersian)}°",
        value = settings.inclinationDeg.toFloat(),
        range = 2f..178f,
        chrome = chrome,
        onValueChange = {
            onUpdateSettings(settings.copy(inclinationDeg = it.toDouble(), activePreset = null).normalize())
        }
    )
    AntarSliderRow(
        label = AntarStrings.paramRoll(isPersian),
        valueText = "${AntarNumberFormat.decimal(settings.rollDeg, 1, isPersian)}°",
        value = settings.rollDeg.toFloat(),
        range = -180f..180f,
        chrome = chrome,
        onValueChange = {
            onUpdateSettings(settings.copy(rollDeg = it.toDouble(), activePreset = null).normalize())
        }
    )
    AntarSliderRow(
        label = AntarStrings.paramFov(isPersian),
        valueText = "${AntarNumberFormat.integer(settings.fovDeg.roundToInt(), isPersian)}°",
        value = settings.fovDeg.toFloat(),
        range = 20f..100f,
        chrome = chrome,
        onValueChange = {
            onUpdateSettings(settings.copy(fovDeg = it.toDouble(), activePreset = null).normalize())
        }
    )
    AntarSliderRow(
        label = AntarStrings.paramExposure(isPersian),
        valueText = AntarNumberFormat.decimal(settings.exposure, 2, isPersian),
        value = settings.exposure.toFloat(),
        range = 0.25f..3.2f,
        chrome = chrome,
        onValueChange = {
            onUpdateSettings(settings.copy(exposure = it.toDouble()).normalize())
        }
    )
    AntarSliderRow(
        label = AntarStrings.paramBloom(isPersian),
        valueText = "${AntarNumberFormat.integer((settings.bloomStrength * 100).roundToInt(), isPersian)}%",
        value = settings.bloomStrength.toFloat(),
        range = 0.0f..1.2f,
        chrome = chrome,
        onValueChange = {
            onUpdateSettings(settings.copy(bloomStrength = it.toDouble()).normalize())
        }
    )

    Text(
        text = AntarStrings.paramQuality(isPersian),
        style = RedTypography.labelMedium,
        color = RedTheme.colors.textSecondary
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AntarQualityTier.entries.forEach { q ->
            AntarChoiceChip(
                label = AntarStrings.qualityLabel(q, isPersian),
                selected = settings.quality == q,
                chrome = chrome,
                modifier = Modifier.weight(1f),
                onClick = { onUpdateSettings(settings.copy(quality = q)) }
            )
        }
    }
    AntarSwitchRow(
        label = AntarStrings.paramAutoRes(isPersian),
        checked = settings.adaptiveResolution,
        chrome = chrome,
        onCheckedChange = { onUpdateSettings(settings.copy(adaptiveResolution = it)) }
    )

    AntarSectionHeader(AntarStrings.sectionTouch(isPersian))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(chrome.cardShape)
            .background(RedTheme.colors.textPrimary.copy(alpha = 0.05f))
            .border(1.dp, chrome.borderColor, chrome.cardShape)
            .padding(12.dp)
    ) {
        Text(
            text = AntarStrings.gestureSummary(isPersian),
            style = RedTypography.labelSmall,
            color = RedTheme.colors.textSecondary
        )
    }

    AntarSliderRow(
        label = AntarStrings.paramSensitivity(isPersian),
        valueText = "${AntarNumberFormat.decimal(settings.gestureSensitivity, 2, isPersian)}×",
        value = settings.gestureSensitivity.toFloat(),
        range = 0.45f..2.0f,
        chrome = chrome,
        onValueChange = {
            onUpdateSettings(settings.copy(gestureSensitivity = it.toDouble()).normalize())
        }
    )
    AntarSwitchRow(
        label = AntarStrings.paramInertia(isPersian),
        checked = settings.inertialGlide,
        chrome = chrome,
        onCheckedChange = { onUpdateSettings(settings.copy(inertialGlide = it)) }
    )
    AntarSwitchRow(
        label = AntarStrings.paramTwoFingerRoll(isPersian),
        checked = settings.twoFingerRollEnabled,
        chrome = chrome,
        onCheckedChange = { onUpdateSettings(settings.copy(twoFingerRollEnabled = it)) }
    )
    AntarSwitchRow(
        label = AntarStrings.paramShowHudBadge(isPersian),
        checked = settings.showTelemetryHud,
        chrome = chrome,
        onCheckedChange = { onUpdateSettings(settings.copy(showTelemetryHud = it)) }
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AntarActionRowButton(
            label = AntarStrings.resetView(isPersian),
            icon = Icons.Default.CenterFocusStrong,
            chrome = chrome,
            onClick = onResetView,
            modifier = Modifier.weight(1f)
        )
        AntarActionRowButton(
            label = AntarStrings.resetAll(isPersian),
            icon = Icons.Default.Refresh,
            chrome = chrome,
            onClick = onResetAll,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AntarSectionHeader(title: String) {
    Text(
        text = title,
        style = RedTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = RedTheme.colors.textPrimary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun AntarObservableCard(
    label: String,
    value: String,
    chrome: AntarChromeTokens,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(chrome.cardShape)
            .background(RedTheme.colors.textPrimary.copy(alpha = 0.05f))
            .border(1.dp, chrome.borderColor, chrome.cardShape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = RedTypography.labelSmall,
                color = RedTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = RedTypography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = chrome.accentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AntarChoiceChip(
    label: String,
    selected: Boolean,
    chrome: AntarChromeTokens,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(chrome.chipShape)
            .background(
                if (selected) chrome.accentSubtleColor
                else RedTheme.colors.textPrimary.copy(alpha = 0.05f)
            )
            .border(
                1.dp,
                if (selected) chrome.accentColor.copy(alpha = 0.6f) else chrome.borderColor,
                chrome.chipShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = RedTypography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) chrome.accentColor else RedTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AntarSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    chrome: AntarChromeTokens,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = RedTypography.bodySmall,
                color = RedTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = valueText,
                style = RedTypography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = chrome.accentColor
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = chrome.accentColor,
                activeTrackColor = chrome.accentColor,
                inactiveTrackColor = RedTheme.colors.textPrimary.copy(alpha = 0.16f)
            )
        )
    }
}

@Composable
private fun AntarSwitchRow(
    label: String,
    checked: Boolean,
    chrome: AntarChromeTokens,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(chrome.cardShape)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = RedTypography.bodySmall,
            color = RedTheme.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = chrome.accentColor,
                uncheckedThumbColor = RedTheme.colors.textSecondary,
                uncheckedTrackColor = RedTheme.colors.textPrimary.copy(alpha = 0.14f)
            )
        )
    }
}

@Composable
private fun AntarActionRowButton(
    label: String,
    icon: ImageVector,
    chrome: AntarChromeTokens,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(chrome.cardShape)
            .background(chrome.accentSubtleColor)
            .border(1.dp, chrome.accentColor.copy(alpha = 0.5f), chrome.cardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = chrome.accentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = RedTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = chrome.accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
