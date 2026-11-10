package com.alijafari.red.astronomy.ui.aurora

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.ui.theme.squircleShape

// ============================================================================
// AURORA COMPONENTS
// ============================================================================
//
// Every control the Aurora screens share. Two rules run through all of them:
//
//  - Control rhythm DR1/DR4 — mixed controls in one row share a baseline and an
//    apparent height, and a compact silhouette still keeps a 48 dp operable
//    target. [AURORA_MIN_TARGET] is the floor and nothing opts out of it.
//  - System states DR1/DR2 — a busy or unavailable control keeps the geometry it
//    had when it was available, and status is never carried by colour alone:
//    every [AuroraStatus] ships its own glyph and expects a label beside it.

/** The smallest operable target Aurora allows itself (control rhythm DR4). */
val AURORA_MIN_TARGET = 48.dp

/** The shared control height, so a button and a field in one row agree. */
val AURORA_CONTROL_HEIGHT = 48.dp

// ----------------------------------------------------------------------------
// Status
// ----------------------------------------------------------------------------

/**
 * The five semantic states a system can report.
 *
 * System-states DR2 is the reason this is an enum of *meanings* and not of
 * colours: a status is a category, and the colour is only one of the three cues
 * that carry it.
 */
enum class AuroraStatus {
    NEUTRAL,
    GOOD,
    WARN,
    BAD,
    INFO
}

@Composable
private fun auroraStatusColor(status: AuroraStatus): Color {
    val palette = AuroraTheme.palette
    return when (status) {
        AuroraStatus.NEUTRAL -> palette.textSecondary
        AuroraStatus.GOOD -> palette.good
        AuroraStatus.WARN -> palette.warn
        AuroraStatus.BAD -> palette.bad
        AuroraStatus.INFO -> palette.info
    }
}

private fun auroraStatusIcon(status: AuroraStatus): ImageVector = when (status) {
    AuroraStatus.NEUTRAL -> Icons.Default.Info
    AuroraStatus.GOOD -> Icons.Default.CheckCircle
    AuroraStatus.WARN -> Icons.Default.ErrorOutline
    AuroraStatus.BAD -> Icons.Default.ErrorOutline
    AuroraStatus.INFO -> Icons.Default.Info
}

/**
 * A status badge: colour, glyph and label together, never one of them alone.
 *
 * The container is a wash of the status hue rather than a fill of it, so the
 * label keeps a full contrast ratio against the surface it sits on; the glyph
 * and the words are what actually say which status this is.
 */
@Composable
fun AuroraStatusPill(
    label: String,
    status: AuroraStatus,
    modifier: Modifier = Modifier,
    icon: ImageVector = auroraStatusIcon(status)
) {
    val palette = AuroraTheme.palette
    val tone = auroraStatusColor(status)
    Row(
        modifier = modifier
            .heightIn(min = 28.dp)
            .clip(squircleShape(AuroraRadius.chip))
            .background(tone.copy(alpha = if (palette.isDark) 0.16f else 0.12f))
            .border(
                width = 1.dp,
                color = tone.copy(alpha = 0.34f),
                shape = squircleShape(AuroraRadius.chip)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AuroraSpace.local)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tone,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = AuroraType.caption,
            color = tone
        )
    }
}

/** A 6 dp signal light. Always paired with a label by its caller. */
@Composable
fun AuroraStatusDot(
    status: AuroraStatus,
    modifier: Modifier = Modifier,
    lit: Boolean = true
) {
    val tone = auroraStatusColor(status)
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(if (lit) tone else tone.copy(alpha = 0.22f))
    )
}

// ----------------------------------------------------------------------------
// Section structure
// ----------------------------------------------------------------------------

/**
 * A section header: an eyebrow, a title, and at most one trailing action.
 *
 * Typography-hierarchy DR2 — the eyebrow is 11 sp at 1.1 sp tracking and the
 * title is 17 sp semibold, so the two are never mistakable for each other or
 * for the body copy beneath them.
 */
@Composable
fun AuroraSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    supporting: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    val palette = AuroraTheme.palette
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f, fill = true)) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow.uppercase(),
                    style = AuroraType.eyebrow,
                    color = palette.textTertiary
                )
                Spacer(modifier = Modifier.height(AuroraSpace.local))
            }
            Text(
                text = title,
                style = AuroraType.section,
                color = palette.textPrimary
            )
            if (supporting != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = supporting,
                    style = AuroraType.caption,
                    color = palette.textTertiary
                )
            }
        }
        if (action != null) {
            Spacer(modifier = Modifier.width(AuroraSpace.group))
            action()
        }
    }
}

/** A hairline. Quieter than a gap alone, and never used where a gap would do. */
@Composable
fun AuroraDivider(modifier: Modifier = Modifier) {
    val palette = AuroraTheme.palette
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(palette.rim.copy(alpha = palette.rim.alpha * 0.5f))
    )
}

// ----------------------------------------------------------------------------
// Chips and rails
// ----------------------------------------------------------------------------

/** A filter chip. Selected chips keep their raised surface for the whole gesture. */
@Composable
fun AuroraChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null
) {
    val palette = AuroraTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    AuroraLiquidSurface(
        modifier = modifier.heightIn(min = 36.dp),
        shape = squircleShape(AuroraRadius.chip),
        tier = if (selected) AuroraSurfaceTier.RAISED else AuroraSurfaceTier.CARD,
        selected = selected,
        onClick = onClick,
        interactionSource = interactionSource,
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AuroraSpace.local)
        ) {
            if (leading != null) {
                Icon(
                    imageVector = leading,
                    contentDescription = null,
                    tint = if (selected) palette.accent else palette.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = label,
                style = AuroraType.label,
                color = if (selected) palette.textPrimary else palette.textSecondary
            )
        }
    }
}

/**
 * A segmented rail: one choice out of a small, fixed set.
 *
 * The whole rail is one lens and the segments are drawn inside it, so the
 * selected thumb reads as a part moving *within* one surface rather than as a
 * second card laid on top of the first (shape-relationships DR2: the inner
 * radius is the outer radius minus the gap).
 */
@Composable
fun AuroraSegmentedRail(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AuroraTheme.palette
    val reduceMotion = rememberAuroraReducedMotion()
    AuroraLiquidSurface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AURORA_CONTROL_HEIGHT),
        shape = squircleShape(AuroraRadius.control),
        tier = AuroraSurfaceTier.CARD
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val weight by animateFloatAsState(
                    targetValue = if (selected) 1.12f else 1f,
                    animationSpec = tween(
                        durationMillis = if (reduceMotion) 0 else AuroraMotion.LOCAL_MS,
                        easing = AuroraMotion.settle
                    ),
                    label = "AuroraRailSegment$index"
                )
                Row(
                    modifier = Modifier
                        .weight(weight)
                        .heightIn(min = AURORA_MIN_TARGET - 8.dp)
                        .clip(squircleShape(AuroraRadius.inner(AuroraRadius.control, 4.dp)))
                        .then(
                            if (selected) {
                                Modifier.background(palette.selectedWash)
                            } else {
                                Modifier
                            }
                        )
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(index) }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = label,
                        style = AuroraType.label,
                        color = if (selected) palette.textPrimary else palette.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Actions
// ----------------------------------------------------------------------------

/**
 * The single primary action of a group.
 *
 * Colour-emphasis DR3 is enforced by construction: this component is the only
 * one that wears the spectral gradient, so a group that uses it has named its
 * one emphasised action and every peer is a [AuroraGhostAction] or an
 * [AuroraIconAction].
 *
 * System-states DR5 — [loading] swaps the label for a spinner *in place*. The
 * button keeps the width it had, so the row it sits in never reflows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuroraPrimaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val palette = AuroraTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    AuroraLiquidSurface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AURORA_CONTROL_HEIGHT),
        shape = squircleShape(AuroraRadius.control),
        tier = AuroraSurfaceTier.RAISED,
        enabled = enabled && !loading,
        onClick = if (loading) null else onClick,
        interactionSource = interactionSource,
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = palette.accent,
                strokeWidth = 2.4.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AuroraSpace.local),
                modifier = Modifier.padding(horizontal = AuroraSpace.group)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = label,
                    style = AuroraType.label,
                    color = if (enabled) palette.textPrimary else palette.textDisabled
                )
            }
        }
    }
}

/**
 * Every action that is not the primary one. Same height, same geometry, no
 * spectral gradient — so the emphasis stays where the group put it.
 */
@Composable
fun AuroraGhostAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val palette = AuroraTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    AuroraLiquidSurface(
        modifier = modifier.heightIn(min = AURORA_CONTROL_HEIGHT),
        shape = squircleShape(AuroraRadius.control),
        tier = AuroraSurfaceTier.CARD,
        enabled = enabled,
        onClick = onClick,
        interactionSource = interactionSource,
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AuroraSpace.local),
            modifier = Modifier.padding(horizontal = AuroraSpace.group)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) palette.textSecondary else palette.textDisabled,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                style = AuroraType.label,
                color = if (enabled) palette.textSecondary else palette.textDisabled
            )
        }
    }
}

/**
 * A compact icon control: a 40 dp silhouette with a 48 dp target.
 *
 * Control rhythm DR4 is the whole reason for the padding — the visible shape
 * stays small enough to sit in a toolbar without the row growing to fit it.
 */
@Composable
fun AuroraIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    badge: (@Composable () -> Unit)? = null
) {
    val palette = AuroraTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    Box(modifier = modifier.size(AURORA_MIN_TARGET), contentAlignment = Alignment.Center) {
        AuroraLiquidSurface(
            modifier = Modifier.size(40.dp),
            shape = squircleShape(AuroraRadius.chip),
            tier = if (selected) AuroraSurfaceTier.RAISED else AuroraSurfaceTier.CARD,
            selected = selected,
            enabled = enabled,
            onClick = onClick,
            interactionSource = interactionSource,
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = when {
                    !enabled -> palette.textDisabled
                    selected -> palette.accent
                    else -> palette.textSecondary
                },
                modifier = Modifier.size(20.dp)
            )
        }
        if (badge != null) {
            Box(modifier = Modifier.align(Alignment.TopEnd)) { badge() }
        }
    }
}

// ----------------------------------------------------------------------------
// Readouts
// ----------------------------------------------------------------------------

/**
 * A label, a value, and an optional supporting line.
 *
 * Spatial hierarchy DR1 — the label sits 6 dp from its value and the whole tile
 * sits 14 dp from its peers, so what belongs together is closer than what does
 * not, with no card drawn around either pair.
 */
@Composable
fun AuroraMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    status: AuroraStatus = AuroraStatus.NEUTRAL
) {
    val palette = AuroraTheme.palette
    val tone = if (status == AuroraStatus.NEUTRAL) palette.textSecondary else auroraStatusColor(status)
    AuroraCard(modifier = modifier, contentPadding = AuroraSpace.group) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AuroraSpace.local)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tone,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = label.uppercase(),
                style = AuroraType.eyebrow,
                color = palette.textTertiary
            )
        }
        Spacer(modifier = Modifier.height(AuroraSpace.local))
        Text(
            text = value,
            style = AuroraType.data,
            color = palette.textPrimary
        )
        if (supporting != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = supporting,
                style = AuroraType.caption,
                color = palette.textTertiary
            )
        }
    }
}

/** One row of a key/value readout. The local gap is the smallest in the system. */
@Composable
fun AuroraReadoutRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    status: AuroraStatus = AuroraStatus.NEUTRAL
) {
    val palette = AuroraTheme.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = AuroraType.label,
            color = palette.textSecondary,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(AuroraSpace.group))
        Text(
            text = value,
            style = AuroraType.data,
            color = if (status == AuroraStatus.NEUTRAL) palette.textPrimary else auroraStatusColor(status)
        )
    }
}

/**
 * A bounded progress bar.
 *
 * The track and the fill are drawn from the same lens geometry, and the value
 * is always announced by a label beside it: a bare bar is colour and length
 * only, which is exactly what system-states DR2 says not to rely on.
 */
@Composable
fun AuroraProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    status: AuroraStatus = AuroraStatus.INFO,
    label: String? = null
) {
    val palette = AuroraTheme.palette
    val tone = if (status == AuroraStatus.NEUTRAL) palette.accent else auroraStatusColor(status)
    val clamped = progress.coerceIn(0f, 1f)
    val fraction by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = AuroraMotion.LOCAL_MS, easing = AuroraMotion.settle),
        label = "AuroraProgress"
    )
    Column(modifier = modifier) {
        if (label != null) {
            Text(text = label, style = AuroraType.caption, color = palette.textTertiary)
            Spacer(modifier = Modifier.height(AuroraSpace.local))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(squircleShape(AuroraRadius.chip))
                .background(palette.innerShadow)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(squircleShape(AuroraRadius.chip))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                tone.copy(alpha = tone.alpha * 0.55f),
                                tone
                            )
                        )
                    )
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Selection rows
// ----------------------------------------------------------------------------

/**
 * A settings row: a title, an optional subtitle, and a check that says which one
 * is current.
 *
 * Both the check and the row's raised surface mark the selection, so the state
 * survives a theme that happens to make the wash hard to see.
 */
@Composable
fun AuroraOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    val palette = AuroraTheme.palette
    AuroraLiquidSurface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AURORA_MIN_TARGET),
        shape = squircleShape(AuroraRadius.inner(AuroraRadius.card, 8.dp)),
        tier = if (selected) AuroraSurfaceTier.RAISED else AuroraSurfaceTier.CARD,
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AuroraSpace.group, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = true)) {
                Text(
                    text = title,
                    style = AuroraType.label,
                    color = if (enabled) palette.textPrimary else palette.textDisabled
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = AuroraType.caption,
                        color = palette.textTertiary
                    )
                }
            }
            if (selected) {
                Spacer(modifier = Modifier.width(AuroraSpace.local))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/** A row that carries a switch. The whole row is the target, not just the switch. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuroraSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    val palette = AuroraTheme.palette
    AuroraLiquidSurface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AURORA_MIN_TARGET),
        shape = squircleShape(AuroraRadius.inner(AuroraRadius.card, 8.dp)),
        tier = AuroraSurfaceTier.CARD,
        enabled = enabled,
        onClick = { if (enabled) onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AuroraSpace.group, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = true)) {
                Text(
                    text = title,
                    style = AuroraType.label,
                    color = if (enabled) palette.textPrimary else palette.textDisabled
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = AuroraType.caption,
                        color = palette.textTertiary
                    )
                }
            }
            Spacer(modifier = Modifier.width(AuroraSpace.group))
            Switch(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled,
                colors = auroraSwitchColors()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun auroraSwitchColors(): SwitchColors {
    val palette = AuroraTheme.palette
    return SwitchDefaults.colors(
        checkedThumbColor = palette.onAccent,
        checkedTrackColor = palette.accent,
        checkedBorderColor = Color.Transparent,
        uncheckedThumbColor = palette.textTertiary,
        uncheckedTrackColor = palette.innerShadow,
        uncheckedBorderColor = Color.Transparent,
        disabledCheckedThumbColor = palette.textDisabled,
        disabledCheckedTrackColor = palette.innerShadow,
        disabledUncheckedThumbColor = palette.textDisabled,
        disabledUncheckedTrackColor = palette.innerShadow
    )
}

/** A labelled slider. The value is printed beside the label, not just felt. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuroraSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    valueText: String? = null,
    enabled: Boolean = true
) {
    val palette = AuroraTheme.palette
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = AuroraType.label, color = palette.textSecondary)
            if (valueText != null) {
                Text(text = valueText, style = AuroraType.data, color = palette.textPrimary)
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = palette.innerShadow,
                disabledThumbColor = palette.textDisabled,
                disabledActiveTrackColor = palette.textDisabled,
                disabledInactiveTrackColor = palette.innerShadow
            )
        )
    }
}

// ----------------------------------------------------------------------------
// Disclosure
// ----------------------------------------------------------------------------

/**
 * One collapsible group of settings.
 *
 * Motion DR2 — the expansion is a 220 ms size change, an order of magnitude
 * quicker than a screen entrance, because the thing moving is small and the
 * person already asked for it. Under reduced motion it is a straight swap.
 */
@Composable
fun AuroraExpandableSection(
    title: String,
    currentValueLabel: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = AuroraTheme.palette
    val reduceMotion = rememberAuroraReducedMotion()
    val enterDuration = if (reduceMotion) 0 else AuroraMotion.LOCAL_MS
    val chevron by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = enterDuration, easing = AuroraMotion.settle),
        label = "AuroraDisclosureChevron"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        AuroraLiquidSurface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AURORA_MIN_TARGET)
                .then(if (testTag != null) Modifier.auroraTestTag(testTag) else Modifier),
            shape = squircleShape(AuroraRadius.card),
            tier = AuroraSurfaceTier.CARD,
            onClick = onToggle,
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.padding(horizontal = AuroraSpace.group, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(AuroraSpace.group))
                Column(modifier = Modifier.weight(1f, fill = true)) {
                    Text(
                        text = title,
                        style = AuroraType.label,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = currentValueLabel,
                        style = AuroraType.caption,
                        color = palette.textTertiary
                    )
                }
                Spacer(modifier = Modifier.width(AuroraSpace.local))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(enterDuration)) + expandVertically(tween(enterDuration)),
            exit = fadeOut(tween(if (reduceMotion) 0 else AuroraMotion.MICRO_MS)) +
                shrinkVertically(tween(if (reduceMotion) 0 else AuroraMotion.MICRO_MS))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = AuroraSpace.group, top = AuroraSpace.group),
                verticalArrangement = Arrangement.spacedBy(AuroraSpace.local),
                content = content
            )
        }
    }
}

/** A row that opens something else. The chevron is the affordance, not decoration. */
@Composable
fun AuroraDisclosureRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: ImageVector? = null
) {
    val palette = AuroraTheme.palette
    AuroraLiquidSurface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AURORA_MIN_TARGET),
        shape = squircleShape(AuroraRadius.inner(AuroraRadius.card, 8.dp)),
        tier = AuroraSurfaceTier.CARD,
        onClick = onClick,
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AuroraSpace.group, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                Icon(
                    imageVector = leading,
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(AuroraSpace.group))
            }
            Column(modifier = Modifier.weight(1f, fill = true)) {
                Text(text = title, style = AuroraType.label, color = palette.textPrimary)
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = subtitle, style = AuroraType.caption, color = palette.textTertiary)
                }
            }
            Spacer(modifier = Modifier.width(AuroraSpace.local))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = palette.textTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Busy and empty
// ----------------------------------------------------------------------------

/**
 * A structural placeholder.
 *
 * System-states DR5 says the stand-in holds the geometry of what it replaces,
 * so this takes the shape and size of the real content and only the *sheen*
 * animates. Under reduced motion the sheen stops and the block stays: the wait
 * is still legible, which is the whole of DR8.
 */
@Composable
fun AuroraSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = squircleShape(AuroraRadius.card)
) {
    val palette = AuroraTheme.palette
    val reduceMotion = rememberAuroraReducedMotion()
    val transition = rememberInfiniteTransition(label = "AuroraSkeleton")
    val sheen = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AuroraSkeletonSheen"
    )
    val position = if (reduceMotion) 0.5f else sheen.value

    Box(
        modifier = modifier
            .clip(shape)
            .background(palette.skeletonBase)
            .then(
                if (reduceMotion) {
                    Modifier
                } else {
                    Modifier.background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                palette.skeletonSheen,
                                Color.Transparent
                            )
                        ),
                        alpha = 0.5f + position * 0.5f,
                        shape = shape
                    )
                }
            )
    )
}

/**
 * The empty state. It states what would be here and, when there is one, the
 * single action that fills it — never an icon on its own.
 */
@Composable
fun AuroraEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.NightsStay,
    action: (@Composable () -> Unit)? = null
) {
    val palette = AuroraTheme.palette
    AuroraCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.textTertiary,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.height(AuroraSpace.group))
            Text(
                text = title,
                style = AuroraType.section,
                color = palette.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(AuroraSpace.local))
            Text(
                text = body,
                style = AuroraType.body,
                color = palette.textTertiary,
                textAlign = TextAlign.Center
            )
            if (action != null) {
                Spacer(modifier = Modifier.height(AuroraSpace.group))
                action()
            }
        }
    }
}

/** Small type-safe helper so call sites can tag a surface without an import. */
internal fun Modifier.auroraTestTag(tag: String): Modifier = this.testTag(tag)

/** Convenience padding for the screens' shared horizontal gutter. */
val AURORA_SCREEN_PADDING: PaddingValues
    @Composable
    get() = PaddingValues(horizontal = AuroraSpace.screenEdge)
