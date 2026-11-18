package com.alijafari.red.astronomy.ui.satellites

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.SatelliteLiveState
import kotlin.math.abs

@Composable
internal fun Hairline(t: ZigSatTokens) =
    Spacer(Modifier.fillMaxWidth().height(with(LocalDensity.current) { 1f.toDp() }).background(t.border))

@Composable
internal fun Glyph(icon: ImageVector, role: LayoutRole, tint: Color, size: Dp = 16.dp, description: String? = null) =
    Icon(icon, description, Modifier.size(size).layoutRole(role), tint = tint)

@Composable
internal fun Disclosure(t: ZigSatTokens) =
    Glyph(Icons.AutoMirrored.Rounded.KeyboardArrowRight, LayoutRole.Mirrors, t.inkTertiary)

@Composable
internal fun HudToolbar(panel: SatPanel, sim: Boolean, t: ZigSatTokens, open: (SatPanel) -> Unit) {
    Row(
        Modifier.height(48.dp).satMaterial(t, SatSlot.Toolbar, SatShapes.capsule),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
                Triple(SatPanel.Fleet, Icons.Rounded.SatelliteAlt, R.string.sat_fleet),
                Triple(SatPanel.Time, Icons.Rounded.Schedule, R.string.sat_time),
                Triple(SatPanel.Layers, Icons.Rounded.Layers, R.string.sat_map),
            )
            .forEachIndexed { i, (dest, icon, label) ->
                if (i != 0)
                    Spacer(Modifier.height(16.dp).width(with(LocalDensity.current) { 1f.toDp() }).background(t.border))
                Box(
                    Modifier.size(48.dp).testTag(listOf("sat_fleet", "sat_time", "sat_layers")[i]).clickable(
                        role = Role.Button,
                    ) {
                        open(dest)
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        stringResource(label),
                        Modifier.size(21.dp),
                        tint = if (panel == dest) t.accentInk else t.inkPrimary,
                    )
                    if (sim && dest == SatPanel.Time)
                        Spacer(
                            Modifier.align(Alignment.TopEnd)
                                .padding(top = 10.dp, end = 10.dp)
                                .size(5.dp)
                                .background(t.sim, SatShapes.capsule),
                        )
                }
            }
    }
}

@Composable
internal fun SatFocusCard(
    focus: SatelliteLiveState,
    t: ZigSatTokens,
    fa: Boolean,
    compact: Boolean,
    wide: Boolean,
    next: SatPassUi?,
    now: Long,
    rising: Boolean?,
    alerts: Boolean,
    details: () -> Unit,
    passes: () -> Unit,
    cycle: (Int) -> Unit,
    modifier: Modifier = Modifier,
    avatarModifier: Modifier = Modifier,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val onCycle by rememberUpdatedState(cycle)
    val onPasses by rememberUpdatedState(passes)
    val prev = stringResource(R.string.sat_prev_sat)
    val following = stringResource(R.string.sat_next_sat)
    val passLabel = stringResource(R.string.sat_passes)
    val pos = focus.topocentric
    val description =
        listOf(
                shortNameOf(focus.satellite, fa),
                stringResource(focus.satellite.taxonomy.label),
                stringResource(if (pos.isSunlit) R.string.sat_sunlit else R.string.sat_shadow),
                stringResource(R.string.sat_altitude) +
                    " " +
                    SatFmt.kilometers(pos.satAltKm, fa) +
                    " " +
                    stringResource(R.string.sat_kilometers_a11y),
                stringResource(R.string.sat_angle) + " " + SatFmt.degrees(pos.elevationDeg, fa),
                rising?.let { stringResource(if (it) R.string.sat_rising else R.string.sat_setting) },
                if (compact) null
                else
                    stringResource(R.string.sat_speed) +
                        " " +
                        SatFmt.speed(pos.velocityKmS, fa) +
                        " " +
                        stringResource(R.string.sat_speed_spoken),
                next?.let {
                    stringResource(R.string.sat_next_in, SatFmt.countdown(it.pass.startTimeMs - now, fa)) +
                        " · " +
                        it.quality.label(fa)
                },
            )
            .filterNotNull()
            .joinToString(". ")
    val detailLabel = stringResource(R.string.sat_details)
    Column(
        modifier
            .fillMaxWidth()
            .testTag("sat_focus_card")
            .satMaterial(t, SatSlot.FocusCard, SatShapes.card)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick(detailLabel) {
                    details()
                    true
                }
                customActions =
                    listOf(
                        CustomAccessibilityAction(prev) {
                            onCycle(-1)
                            true
                        },
                        CustomAccessibilityAction(following) {
                            onCycle(1)
                            true
                        },
                        CustomAccessibilityAction(passLabel) {
                            onPasses()
                            true
                        },
                    )
            }
            .pointerInput(rtl) {
                var travel = Offset.Zero
                detectDragGestures(
                        onDragStart = { travel = Offset.Zero },
                        onDragEnd = {
                            if (travel.y < -32.dp.toPx() && -travel.y > abs(travel.x)) onPasses()
                            else if (abs(travel.x) > 32.dp.toPx() && abs(travel.x) > abs(travel.y))
                                onCycle(if ((travel.x < 0) != rtl) 1 else -1)
                        },
                    ) { change, delta ->
                        change.consume()
                        travel += delta
                    }
            }
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button, onClick = details),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(avatarModifier) { SatAvatar(focus.satellite, t) }
            Column(Modifier.weight(1f)) {
                SatText(shortNameOf(focus.satellite, fa), t.inkPrimary, size = 16, strong = true)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SatText(stringResource(focus.satellite.taxonomy.label) + " ·", t.inkSecondary, size = 11)
                    Icon(
                        if (pos.isSunlit) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        null,
                        Modifier.size(12.dp),
                        tint = t.inkSecondary,
                    )
                    SatText(
                        stringResource(if (pos.isSunlit) R.string.sat_sunlit else R.string.sat_shadow),
                        t.inkSecondary,
                        size = 11,
                    )
                }
            }
            Disclosure(t)
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = (44 * LocalDensity.current.fontScale).dp).padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FocusMetric(
                SatFmt.kilometers(pos.satAltKm, fa),
                stringResource(R.string.sat_kilometers),
                stringResource(R.string.sat_altitude),
                t,
                Modifier.weight(1f),
            )
            FocusMetric(
                SatFmt.degrees(pos.elevationDeg, fa),
                "",
                stringResource(if (pos.elevationDeg < 0) R.string.sat_below_horizon else R.string.sat_angle),
                t,
                Modifier.weight(1f),
                pos.elevationDeg < 0,
                rising,
            )
            if (!compact)
                FocusMetric(
                    SatFmt.speed(pos.velocityKmS, fa),
                    stringResource(R.string.sat_speed_unit),
                    stringResource(R.string.sat_speed),
                    t,
                    Modifier.weight(1.25f),
                )
            if (wide && !compact)
                FocusMetric(
                    SatFmt.magnitude(focus.apparentMagnitude, fa),
                    "",
                    stringResource(R.string.sat_mag_short),
                    t,
                    Modifier.weight(.8f),
                )
        }
        Hairline(t)
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("sat_focus_passes")
                .clickable(role = Role.Button, onClick = passes),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ZigStatusDot(next?.quality?.graphic(t) ?: t.inkTertiary, false, 6.dp)
            val line =
                when {
                    next == null -> stringResource(R.string.sat_no_pass)
                    next.pass.startTimeMs <= now ->
                        stringResource(R.string.sat_in_progress, SatFmt.countdown(next.pass.endTimeMs - now, fa))
                    else ->
                        stringResource(
                            R.string.sat_next_in,
                            SatFmt.duration(((next.pass.startTimeMs - now) / 60_000).toInt(), fa),
                        )
                }
            SatText(
                line + (next?.let { " · " + it.quality.label(fa) } ?: ""),
                t.inkSecondary,
                Modifier.weight(1f),
                size = 12,
            )
            if (alerts)
                Icon(
                    Icons.Rounded.NotificationsActive,
                    stringResource(R.string.sat_alerts),
                    Modifier.size(14.dp),
                    tint = t.inkSecondary,
                )
            Glyph(Icons.Rounded.ExpandLess, LayoutRole.Neutral, t.inkSecondary)
        }
    }
}

@Composable
private fun FocusMetric(
    value: String,
    unit: String,
    label: String,
    t: ZigSatTokens,
    modifier: Modifier,
    muted: Boolean = false,
    rising: Boolean? = null,
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (rising != null)
                Glyph(
                    if (rising) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                    LayoutRole.Physical,
                    t.inkSecondary,
                    12.dp,
                    stringResource(if (rising) R.string.sat_rising else R.string.sat_setting),
                )
            SatMetric(value, unit, label, t, muted = muted)
        }
        SatText(label, t.inkSecondary, size = 11)
    }
}
