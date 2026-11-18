package com.alijafari.red.astronomy.ui.satellites

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteCategory
import com.alijafari.red.astronomy.astro_engine.SatelliteItem
import com.alijafari.red.astronomy.astro_engine.SatelliteLiveState
import com.alijafari.red.astronomy.ui.aether.AetherDotMatrixText
import com.alijafari.red.astronomy.ui.spatial.DotMatrixText
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun SatText(text: String, color: Color, modifier: Modifier = Modifier, size: Int = 12, strong: Boolean = false) {
    Text(text, modifier = modifier, color = color, style = RedTypographyTokens.badge.copy(
        fontSize = size.sp, lineHeight = (size * 1.25f).sp,
        fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Medium,
    ).tabular())
}

@Composable
internal fun SatStatusPill(data: SatStatusData, tokens: ZigSatTokens, onClick: () -> Unit, onLive: () -> Unit) {
    val offline = !data.isOnline
    val stale = (data.tleAgeDays ?: 0.0) > 2.0
    val collapse = offline && stale && (LocalConfiguration.current.screenWidthDp < 360 || LocalDensity.current.fontScale >= 1.3f)
    val statusColor = if (data.isLive) tokens.live else tokens.sim
    Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
        Row(Modifier.heightIn(min = 36.dp).satMaterial(tokens, SatSlot.StatusPill, CircleShape, tokens.hudFill,
            if (!data.isLive || tokens.glassy) statusColor else null), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).testTag("satellite_status_chip")
                .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ZigStatusDot(statusColor, data.isLive)
                Text(if (data.isLive) stringResource(R.string.sat_live) else SatFmt.offsetMinutes(data.offsetMinutes, data.isFa),
                    color = tokens.hudInk, softWrap = false,
                    style = RedTypographyTokens.badge.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold).tabular())
                if (offline) Icon(Icons.Filled.WifiOff, stringResource(R.string.sat_offline), Modifier.size(14.dp), tint = tokens.hudInk)
                if (stale && !collapse) Icon(Icons.Filled.Schedule, stringResource(R.string.sat_stale_tle), Modifier.size(14.dp),
                    tint = if ((data.tleAgeDays ?: 0.0) > 7.0) tokens.accentInk else tokens.sim)
                if (collapse) SatText(SatFmt.signed(1.0, 0, isFa = data.isFa), tokens.hudInk,
                    Modifier.semantics { contentDescription = if (data.isFa) "دادهٔ مداری قدیمی" else "Aged orbital data" })
            }
            if (!data.isLive) Box(Modifier.size(48.dp).clickable(role = Role.Button, onClick = onLive), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Replay, stringResource(R.string.sat_return_live), Modifier.size(18.dp), tint = tokens.hudInk)
            }
        }
    }
}

@Composable
internal fun SatAvatar(item: SatelliteItem, tokens: ZigSatTokens, size: Dp = 40.dp) {
    Box(Modifier.size(size).background((if (item.isTrain) tokens.starlink else tokens.live).copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center) {
        if (item.isTrain) PearlGlyph(tokens.starlink, Modifier.size(24.dp, 12.dp))
        else Icon(satIcon(item), null, Modifier.size(size * 0.48f), tint = tokens.accentInk)
    }
}

internal fun satIcon(item: SatelliteItem): ImageVector = when (item.category) {
    SatelliteCategory.ISS -> Icons.Filled.SatelliteAlt
    SatelliteCategory.HUBBLE -> Icons.Filled.Visibility
    else -> Icons.Filled.Satellite
}

@Composable
internal fun PearlGlyph(color: Color, modifier: Modifier = Modifier.size(16.dp)) {
    Canvas(modifier) {
        val middle = size.height / 2
        drawLine(color.copy(alpha = .6f), Offset(size.width * .12f, middle), Offset(size.width * .88f, middle), 1.dp.toPx())
        repeat(3) { index -> drawCircle(color, (if (index == 2) 2.5.dp else 2.dp).toPx(), Offset(size.width * (.16f + index * .34f), middle)) }
    }
}

@Composable
internal fun SatFocusCard(
    focus: SatelliteLiveState,
    tokens: ZigSatTokens,
    isFa: Boolean,
    compact: Boolean,
    wide: Boolean,
    simulated: Boolean,
    offline: Boolean,
    nextPass: SatPassUi?,
    nowMs: Long,
    span: String,
    hints: Boolean,
    onFleet: () -> Unit,
    onDetails: () -> Unit,
    onPasses: () -> Unit,
    onCycle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val cycle by rememberUpdatedState(onCycle)
    val passes by rememberUpdatedState(onPasses)
    val previousLabel = stringResource(R.string.sat_prev_sat)
    val nextLabel = stringResource(R.string.sat_next_sat)
    val state = focus.topocentric
    Column(modifier.fillMaxWidth().testTag("sat_focus_card").satMaterial(tokens, SatSlot.FocusCard, squircleShape(SatRadius.card))
        .semantics {
            customActions = listOf(CustomAccessibilityAction(previousLabel) { cycle(-1); true }, CustomAccessibilityAction(nextLabel) { cycle(1); true })
        }
        .pointerInput(rtl) {
            var travel = Offset.Zero
            detectDragGestures(onDragStart = { travel = Offset.Zero }, onDragEnd = {
                if (travel.y < -32.dp.toPx() && abs(travel.y) > abs(travel.x)) passes()
                else if (abs(travel.x) > 32.dp.toPx() && abs(travel.x) > abs(travel.y)) {
                    val towardsStart = if (rtl) travel.x > 0 else travel.x < 0
                    cycle(if (towardsStart) 1 else -1)
                }
            }) { change, drag -> change.consume(); travel += drag }
        }.padding(horizontal = 12.dp, vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onFleet),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SatAvatar(focus.satellite, tokens)
                Column(Modifier.weight(1f)) {
                    Text(shortNameOf(focus.satellite, isFa), color = tokens.inkPrimary,
                        style = RedTypographyTokens.sectionTitle.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (offline) SatText(stringResource(R.string.sat_cached), tokens.inkSecondary, size = 10)
                }
            }
            if (hints) {
                Icon((if (LocalLayoutDirection.current == LayoutDirection.Rtl) Icons.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowLeft), null, Modifier.size(14.dp), tint = tokens.inkTertiary.copy(alpha = .58f))
                Icon((if (LocalLayoutDirection.current == LayoutDirection.Rtl) Icons.Filled.KeyboardArrowLeft else Icons.Filled.KeyboardArrowRight), null, Modifier.size(14.dp), tint = tokens.inkTertiary.copy(alpha = .58f))
            }
            ZigSatIconButton(Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.sat_details), tokens, onDetails,
                active = true, visualSize = 44.dp, testTag = "dock_details")
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 20.dp).then(if (!simulated) Modifier else Modifier.drawBehind {
            drawLine(tokens.sim, Offset(0f, size.height - 1.dp.toPx()), Offset(size.width, size.height - 1.dp.toPx()), 1.dp.toPx())
        }).padding(horizontal = 2.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (state.isSunlit) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                stringResource(if (state.isSunlit) R.string.sat_sunlit else R.string.sat_shadow), Modifier.size(16.dp),
                tint = if (state.isSunlit) tokens.sim else tokens.umbra)
            val alt = SatFmt.kilometers(state.satAltKm, isFa)
            SatMetric(alt, stringResource(R.string.sat_kilometers), stringResource(R.string.sat_altitude) + " $alt " +
                stringResource(R.string.sat_kilometers_a11y), tokens, Modifier.weight(if (isFa) 1.25f else 1f), roll = true)
            SatMetric(SatFmt.degrees(state.elevationDeg, isFa, signed = true),
                if (state.elevationDeg < 0.0) stringResource(R.string.sat_below) else "",
                stringResource(R.string.sat_elevation), tokens, Modifier.weight(1f), muted = state.elevationDeg < 0.0)
            if (!compact) SatMetric(SatFmt.speed(state.velocityKmS, isFa), stringResource(R.string.sat_speed_unit),
                stringResource(R.string.sat_speed), tokens, Modifier.weight(if (isFa) 1.65f else 1.15f))
            if (wide) SatMetric(SatFmt.magnitude(focus.apparentMagnitude, isFa), stringResource(R.string.sat_mag_short),
                stringResource(R.string.sat_magnitude), tokens, Modifier.weight(.85f))
        }
        val pass = nextPass?.pass
        val title = when {
            pass == null -> stringResource(R.string.sat_no_visible, span)
            pass.startTimeMs <= nowMs -> stringResource(R.string.sat_in_progress, SatFmt.countdown(pass.endTimeMs - nowMs, isFa))
            else -> stringResource(R.string.sat_next_in, SatFmt.countdown(pass.startTimeMs - nowMs, isFa))
        }
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("sat_focus_passes")
            .clickable(role = Role.Button, onClick = onPasses), contentAlignment = Alignment.Center) {
            Row(Modifier.fillMaxWidth().heightIn(min = 36.dp).background(tokens.cardAlt, CircleShape)
                .border(1.dp, tokens.border, CircleShape).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ZigStatusDot(nextPass?.quality?.hue ?: tokens.telescopic, false, 6.dp)
                val label = nextPass?.quality?.let { if (compact || LocalDensity.current.fontScale >= 1.3f) it.shortLabel(isFa) else it.label(isFa) }
                SatText(if (label != null) "$title · $label" else title, tokens.inkPrimary, Modifier.weight(1f), strong = true)
                Icon(Icons.Filled.KeyboardArrowUp, null, Modifier.size(16.dp), tint = tokens.inkSecondary)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SatMetric(value: String, unit: String, label: String, tokens: ZigSatTokens, modifier: Modifier = Modifier,
    muted: Boolean = false, roll: Boolean = false) {
    val ink = if (muted) tokens.inkSecondary else tokens.inkPrimary
    val reduce = LocalSatReduceMotion.current
    val fontScale = LocalDensity.current.fontScale
    FlowRow(modifier.clearAndSetSemantics { contentDescription = "$label $value $unit" },
        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        val matrix = tokens.glassy && fontScale < 1.3f && value.any(Char::isDigit)
        val slotWidth = if (matrix) (if (roll) 32.dp else 30.dp) else Dp.Unspecified
        Crossfade(targetState = value, modifier = if (matrix) Modifier.widthIn(min = slotWidth) else Modifier,
            animationSpec = tween(120), label = "sat-number") { digits ->
            val glyphs = digits.filter { it.isDigit() || it in "+−.٫٬,°" }
            when {
                !matrix -> SatText(digits, ink, size = 12, strong = true)
                tokens.skin == SatSkin.SPATIAL -> DotMatrixText(glyphs, dotSize = 0.87.dp, dotGap = 0.57.dp,
                    charGap = 1.dp, activeColor = ink, glowColor = if (reduce) null else tokens.timeAccent)
                else -> AetherDotMatrixText(glyphs, dotSize = 0.87.dp, dotGap = 0.57.dp,
                    charGap = 1.dp, color = ink, glowColor = if (reduce) null else tokens.timeAccent)
            }
        }
        if (unit.isNotEmpty()) SatText(unit, tokens.inkSecondary, size = 10)
    }
}

@Composable
internal fun SatLegendPanel(tokens: ZigSatTokens, isFa: Boolean, modifier: Modifier = Modifier) {
    val labels = listOf(R.string.sat_legend_selected, R.string.sat_legend_naked, R.string.sat_legend_scope,
        R.string.sat_legend_starlink, R.string.sat_legend_train, R.string.sat_legend_you)
    Column(modifier.testTag("globe_legend_panel").satMaterial(tokens, SatSlot.Popover, squircleShape(SatRadius.popover))
        .padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        labels.forEachIndexed { index, label ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Canvas(Modifier.size(16.dp)) {
                    when (index) {
                        0 -> { drawLine(tokens.live, Offset(0f, center.y), Offset(size.width, center.y), 1.dp.toPx()); drawCircle(tokens.live, 4.dp.toPx()); drawCircle(tokens.hudInk, 1.4.dp.toPx()) }
                        1 -> drawCircle(tokens.nakedEye, 4.dp.toPx())
                        2 -> drawCircle(tokens.telescopic, 3.dp.toPx())
                        3 -> { drawCircle(tokens.starlink, 2.5.dp.toPx(), Offset(3.dp.toPx(), center.y)); drawCircle(tokens.starlink.copy(alpha = .6f), 2.5.dp.toPx(), Offset(12.dp.toPx(), center.y), style = Stroke(1.dp.toPx())) }
                        4 -> { drawLine(tokens.starlink, Offset(0f, center.y), Offset(size.width, center.y)); repeat(3) { drawCircle(tokens.starlink, 2.dp.toPx(), Offset(size.width * (.125f + .375f * it), center.y)) } }
                        5 -> { drawCircle(tokens.starlink, 5.dp.toPx(), style = Stroke(1.dp.toPx())); drawCircle(tokens.starlink, 2.dp.toPx()) }
                    }
                }
                SatText(stringResource(label), tokens.inkPrimary, size = 12)
            }
        }
    }
}

@Composable
internal fun SatTimeTray(
    tokens: ZigSatTokens, offset: Int, realNow: Long, tz: TimeZone, markers: List<SatTimeMarker>, isFa: Boolean,
    compactHeight: Boolean, actions: SatTimeActions, onClose: () -> Unit, modifier: Modifier = Modifier,
) {
    val clock = SatFmt.dateTime(realNow + offset * 60_000L, isFa, tz)
    val available = nextSatelliteTime(markers, offset) != null
    Column(modifier.fillMaxWidth().testTag("satellite_time_scrubber_card")
        .satMaterial(tokens, SatSlot.TimeTray, squircleShape(SatRadius.card), accent = tokens.timeAccent)
        .padding(horizontal = 10.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SatText(clock, tokens.inkPrimary, Modifier.weight(1f).padding(start = 6.dp), size = 13, strong = true)
            Box(Modifier.widthIn(min = 92.dp).heightIn(min = 48.dp).then(if (offset == 0) Modifier else
                Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.sat_return_live), onClick = actions.onLive)),
                contentAlignment = Alignment.Center) {
                Row(Modifier.background(tokens.hudFill, CircleShape).border(1.dp, tokens.border, CircleShape).padding(horizontal = 7.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    ZigStatusDot(if (offset == 0) tokens.live else tokens.sim, offset == 0, 6.dp)
                    SatText(if (offset == 0) stringResource(R.string.sat_live) else SatFmt.offsetMinutes(offset, isFa), tokens.inkPrimary, strong = true)
                    if (offset != 0) Icon(Icons.Filled.Replay, null, Modifier.size(12.dp), tint = tokens.inkPrimary)
                }
            }
            ZigSatIconButton(Icons.Filled.Close, stringResource(R.string.sat_close), tokens, onClose)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            SatStepButton((if (LocalLayoutDirection.current == LayoutDirection.Rtl) Icons.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowLeft), stringResource(R.string.sat_step_prev),
                stringResource(R.string.sat_hour_prev), tokens, { actions.onStep(-15) }, { actions.onStep(-60) })
            SatScrubber(tokens, offset, realNow, tz, markers, isFa, actions, Modifier.weight(1f))
            SatStepButton((if (LocalLayoutDirection.current == LayoutDirection.Rtl) Icons.Filled.KeyboardArrowLeft else Icons.Filled.KeyboardArrowRight), stringResource(R.string.sat_step_next),
                stringResource(R.string.sat_hour_next), tokens, { actions.onStep(15) }, { actions.onStep(60) })
            ZigSatIconButton(if (LocalLayoutDirection.current == LayoutDirection.Rtl) Icons.Filled.SkipPrevious else Icons.Filled.SkipNext,
                stringResource(if (available) R.string.sat_next_pass else R.string.sat_no_pass_range), tokens, actions.onNextPass, enabled = available)
        }
        if (!compactHeight) Row(Modifier.fillMaxWidth().padding(start = 48.dp, end = 96.dp, bottom = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(-12, -6, 0, 6, 12).forEach { hour ->
                SatText(if (hour == 0) stringResource(R.string.sat_live) else SatFmt.signed(hour.toDouble(), 0, if (isFa) "س" else "h", isFa),
                    if (hour == 0) tokens.accentInk else tokens.inkSecondary, size = 10)
            }
        }
    }
}

@Composable
private fun SatStepButton(icon: ImageVector, description: String, longDescription: String, tokens: ZigSatTokens,
    onTap: () -> Unit, onRepeat: () -> Unit) {
    val tap by rememberUpdatedState(onTap)
    val step by rememberUpdatedState(onRepeat)
    Box(Modifier.size(48.dp).semantics {
        role = Role.Button
        onClick(description) { tap(); true }
        onLongClick(longDescription) { step(); true }
    }.pointerInput(Unit) {
        detectTapGestures(onTap = { tap() }, onPress = {
            coroutineScope {
                val repeatJob = launch {
                    delay(viewConfiguration.longPressTimeoutMillis)
                    while (true) { step(); delay(250) }
                }
                tryAwaitRelease()
                repeatJob.cancel()
            }
        }, onLongPress = {} )
    }, contentAlignment = Alignment.Center) {
        Icon(icon, description, Modifier.size(22.dp), tint = tokens.inkPrimary)
    }
}

@Composable
private fun SatScrubber(tokens: ZigSatTokens, offset: Int, realNow: Long, tz: TimeZone, markers: List<SatTimeMarker>,
    isFa: Boolean, actions: SatTimeActions, modifier: Modifier = Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var dragging by remember { mutableStateOf(false) }
    val latestMarkers by rememberUpdatedState(markers)
    val latestActions by rememberUpdatedState(actions)
    val androidView = LocalView.current
    val simClock = SatFmt.clock(realNow + offset * 60_000L, isFa, tz)
    val spoken = if (offset == 0) stringResource(R.string.sat_live) else stringResource(
        if (offset > 0) R.string.sat_spoken_plus else R.string.sat_spoken_minus, SatFmt.duration(abs(offset), isFa), simClock)
    val sliderLabel = stringResource(R.string.sat_slider)
    val prev = stringResource(R.string.sat_step_prev)
    val next = stringResource(R.string.sat_step_next)
    val prevHour = stringResource(R.string.sat_hour_prev)
    val nextHour = stringResource(R.string.sat_hour_next)
    val live = stringResource(R.string.sat_return_live)
    val nextPass = stringResource(R.string.sat_next_pass)
    BoxWithConstraints(modifier.height(48.dp).testTag("time_scrubber_slider").semantics {
        stateDescription = spoken
        contentDescription = sliderLabel
        progressBarRangeInfo = ProgressBarRangeInfo(offset.toFloat(), -720f..720f, 1439)
        setProgress { actions.onOffsetChange(it.roundToInt()); true }
        customActions = listOf(
            CustomAccessibilityAction(prev) { actions.onStep(-15); true },
            CustomAccessibilityAction(next) { actions.onStep(15); true },
            CustomAccessibilityAction(prevHour) { actions.onStep(-60); true },
            CustomAccessibilityAction(nextHour) { actions.onStep(60); true },
            CustomAccessibilityAction(live) { actions.onLive(); true },
        ) + if (nextSatelliteTime(markers, offset) == null) emptyList() else listOf(CustomAccessibilityAction(nextPass) { actions.onNextPass(); true })
    }.pointerInput(rtl) {
        awaitEachGesture {
            val down = awaitFirstDown()
            val start = 12.dp.toPx()
            val span = (size.width - 2 * start).coerceAtLeast(1f)
            var last: Int? = null
            fun update(x: Float) {
                val fraction = ((x - start) / span).coerceIn(0f, 1f)
                val raw = ((if (rtl) 1f - fraction else fraction) - 0.5f) * 1440f
                val snapped = snapSatelliteTime(raw, latestMarkers)
                if (snapped != last) {
                    val feedback = when {
                        snapped == 0 -> HapticFeedbackConstants.CLOCK_TICK
                        latestMarkers.any { it.minute == snapped } -> HapticFeedbackConstants.CONFIRM
                        last != null && snapped / 60 != last!! / 60 -> HapticFeedbackConstants.KEYBOARD_TAP
                        else -> null
                    }
                    if (feedback != null) androidView.performHapticFeedback(feedback)
                    latestActions.onOffsetChange(snapped)
                    last = snapped
                }
            }
            down.consume()
            update(down.position.x)
            try {
                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (change.pressed) { dragging = true; update(change.position.x) }
                    change.consume()
                } while (change.pressed)
            } finally { dragging = false }
        }
    }) {
        Canvas(Modifier.fillMaxSize().testTag("time_scrubber_track")) {
            val inset = 12.dp.toPx()
            val span = (size.width - 2 * inset).coerceAtLeast(1f)
            fun x(m: Int): Float {
                val f = m / 1440f + .5f
                return inset + span * if (rtl) 1f - f else f
            }
            val y = center.y
            drawRoundRect(tokens.inkTertiary.copy(alpha = .40f), Offset(inset, y - 2.dp.toPx()), Size(span, 4.dp.toPx()), CornerRadius(2.dp.toPx()))
            for (hour in -12..12) {
                val at = x(hour * 60)
                val half = (if (hour == 0) 6.dp else if (hour % 6 == 0) 4.dp else 2.dp).toPx()
                drawLine(if (hour == 0) tokens.timeAccent else tokens.inkSecondary, Offset(at, y - half), Offset(at, y + half), 1.dp.toPx())
            }
            markers.forEach { mark ->
                val at = x(mark.minute)
                drawLine(mark.quality.hue, Offset(at, y - 14.dp.toPx()), Offset(at, y - 8.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
            }
            val thumb = Offset(x(offset), y)
            if (tokens.glassy) drawCircle(tokens.timeAccent.copy(alpha = .17f), (if (dragging) 19.dp else 13.dp).toPx(), thumb)
            drawCircle(tokens.timeAccent, (if (dragging) 16.dp else 10.dp).toPx(), thumb)
            drawCircle(Color.White, 3.dp.toPx(), thumb)
        }
        if (dragging) {
            val f = offset / 1440f + .5f
            val x = maxWidth * (if (rtl) 1f - f else f) - 42.dp
            SatText("$simClock · ${SatFmt.offsetMinutes(offset, isFa)}", tokens.inkPrimary,
                Modifier.absoluteOffset(x.coerceIn(0.dp, (maxWidth - 84.dp).coerceAtLeast(0.dp)), (-17).dp)
                    .background(tokens.hudFill, CircleShape).padding(horizontal = 5.dp, vertical = 3.dp), size = 10)
        }
    }
}

@Composable
fun SatScopeRow(selected: SatelliteCategory, tokens: ZigSatTokens, isFa: Boolean, onSelect: (SatelliteCategory) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SatelliteCategory.entries.forEach { category ->
            ZigSatChip(SatFmt.categoryLabel(category, isFa), selected == category, tokens, { onSelect(category) })
        }
    }
}

internal fun androidx.compose.ui.layout.LayoutCoordinates.anchorInWindow(): IntRect {
    val b = boundsInWindow()
    return IntRect(b.left.roundToInt(), b.top.roundToInt(), b.right.roundToInt(), b.bottom.roundToInt())
}

@Composable
fun SatSpotlightCard(
    data: SatSpotlightUi,
    tokens: ZigSatTokens,
    isFa: Boolean,
    actions: SatPassActions,
    modifier: Modifier = Modifier,
    arcHeight: Dp = 112.dp,
    showActions: Boolean = true,
    nowMs: Long = System.currentTimeMillis(),
    realNowMs: Long = nowMs,
    tz: TimeZone = TimeZone.getDefault(),
) {
    val pass = data.pass.pass
    val accent = if (data.pass.isTrain) tokens.starlink else tokens.timeAccent
    var bellAnchor by remember { mutableStateOf(IntRect.Zero) }
    ZigSatCard(
        modifier = modifier.testTag("next_pass_spotlight"),
        tokens = tokens,
        accent = accent,
        shape = squircleShape(SatRadius.sheet),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ZigMicroCaps(
                    text = stringResource(R.string.sat_next_pass),
                    tokens = tokens,
                    isFa = isFa,
                    color = tokens.accentInk,
                )
                Spacer(modifier = Modifier.weight(1f))
                ZigQualityChip(
                    quality = data.pass.quality,
                    score = pass.visibilityScore,
                    tokens = tokens,
                    isFa = isFa,
                )
            }

            Text(
                text = data.pass.satName,
                style = RedTypographyTokens.sectionTitle.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                color = tokens.inkPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (data.pass.isTrain) {
                val short = LocalConfiguration.current.screenWidthDp < 360 || LocalDensity.current.fontScale >= 1.3f
                SatText(stringResource(if (short) R.string.sat_train_short else R.string.sat_train_long,
                    SatFmt.int(data.pass.sat.trainCount, isFa)), tokens.inkPrimary,
                    Modifier.background(tokens.starlink.copy(alpha = .15f), CircleShape).padding(horizontal = 8.dp, vertical = 4.dp), size = 11)
            }
            if (data.isInProgress) {
                SatText(stringResource(R.string.sat_in_progress, SatFmt.countdown(pass.endTimeMs - nowMs, isFa)),
                    tokens.inkPrimary, size = 14, strong = true)
                SatPassProgressBar(pass, tokens, nowMs)
            } else {
                SatText(stringResource(R.string.sat_starts_in), tokens.inkSecondary)
                Text(SatFmt.countdown(pass.startTimeMs - nowMs, isFa),
                    style = RedTypographyTokens.sectionHeading.copy(fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold).tabular(),
                    color = tokens.inkPrimary, modifier = Modifier.testTag("spotlight_countdown"))
            }
            if (data.simulatedInsidePass) SatText(stringResource(R.string.sat_sim_inside), tokens.inkSecondary)

            SatSkyArc(pass = pass, tokens = tokens, isFa = isFa, height = arcHeight, nowMs = nowMs,
                showCompassMarks = arcHeight >= 160.dp, train = data.pass.isTrain)

            Row(modifier = Modifier.fillMaxWidth()) {
                SpotlightStat(
                    label = stringResource(R.string.sat_rise),
                    value = SatFmt.clock(pass.startTimeMs, isFa, tz),
                    sub = SatFmt.compass(pass.startAzimuthDeg, isFa),
                    tokens = tokens,
                    isFa = isFa,
                    modifier = Modifier.weight(1f),
                )
                SpotlightStat(
                    label = stringResource(R.string.sat_max),
                    value = SatFmt.clock(pass.maxTimeMs, isFa, tz),
                    sub = SatFmt.degrees(pass.maxElevationDeg, isFa),
                    tokens = tokens,
                    isFa = isFa,
                    modifier = Modifier.weight(1f),
                    accent = tokens.accentInk,
                )
                SpotlightStat(
                    label = stringResource(R.string.sat_set),
                    value = SatFmt.clock(pass.endTimeMs, isFa, tz),
                    sub = SatFmt.compass(pass.endAzimuthDeg, isFa),
                    tokens = tokens,
                    isFa = isFa,
                    modifier = Modifier.weight(1f),
                )
            }

            SatMetaLine(pass = pass, tokens = tokens, isFa = isFa)

            if (showActions) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    SatDialogButton("${stringResource(R.string.sat_remind)} ▾", tokens, { actions.onRemind(bellAnchor) },
                        enabled = actions.isReminderEnabled && (pass.startTimeMs - 10 * 60_000L > realNowMs ||
                            data.pass.reminderLeadMinutes != null && pass.startTimeMs > realNowMs),
                        modifier = Modifier.weight(1f).onGloballyPositioned { bellAnchor = it.anchorInWindow() })
                    SatDialogButton(stringResource(R.string.sat_ar), tokens, actions.onAr, modifier = Modifier.weight(1.2f))
                    ZigSatIconButton(Icons.Filled.CenterFocusWeak, stringResource(R.string.sat_show_on_globe), tokens, actions.onShowOnGlobe)
                }
            }
        }
    }
}

/** `mag −3.4 · 6 min · SW → NE` — signs are bidi-isolated, never `+-`. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SatMetaLine(pass: ISSEngine.ISSPass, tokens: ZigSatTokens, isFa: Boolean) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "${stringResource(R.string.sat_mag_short)} ${SatFmt.magnitude(pass.estimatedMagnitude, isFa)}",
            style = RedTypographyTokens.badge.copy(fontSize = 11.sp).tabular(),
            color = tokens.inkSecondary,
        )
        Text(
            text = SatFmt.duration(((pass.endTimeMs - pass.startTimeMs) / 60_000L).toInt(), isFa),
            style = RedTypographyTokens.badge.copy(fontSize = 11.sp).tabular(),
            color = tokens.inkSecondary,
        )
        Text(
            text = SatFmt.direction(pass.startAzimuthDeg, pass.endAzimuthDeg, isFa, compact = true),
            style = RedTypographyTokens.badge.copy(fontSize = 11.sp),
            color = tokens.inkSecondary,
        )
    }
}

@Composable
private fun SpotlightStat(
    label: String,
    value: String,
    sub: String,
    tokens: ZigSatTokens,
    isFa: Boolean,
    modifier: Modifier = Modifier,
    accent: Color? = null,
) {
    Column(modifier = modifier) {
        ZigMicroCaps(text = label, tokens = tokens, isFa = isFa)
        Text(
            text = value,
            style = RedTypographyTokens.badge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold).tabular(),
            color = accent ?: tokens.inkPrimary,
            maxLines = 1,
        )
        Text(
            text = sub,
            style = RedTypographyTokens.badge.copy(fontSize = 11.sp),
            color = tokens.inkTertiary,
            maxLines = 1,
        )
    }
}

/**
 * The sky dome. Geography never mirrors, so the canvas is pinned to LTR: rise stays at the geometric
 * left, the dome runs to set, the peak sits at the max elevation, and shadow entry/exit are dashed
 * in `umbra`. During a pass a live dot travels the arc.
 */
@Composable
fun SatSkyArc(pass: ISSEngine.ISSPass, tokens: ZigSatTokens, isFa: Boolean, height: Dp = 112.dp,
    nowMs: Long = System.currentTimeMillis(), showCompassMarks: Boolean = false, train: Boolean = false) {
    val color = if (train) tokens.starlink else if (tokens.skin == SatSkin.AETHER) tokens.timeAccent else tokens.accentInk
    val span = (pass.endTimeMs - pass.startTimeMs).coerceAtLeast(1L).toFloat()
    fun fraction(ms: Long) = ((ms - pass.startTimeMs) / span).coerceIn(0f, 1f)
    val peak = fraction(pass.maxTimeMs).coerceIn(.06f, .94f)
    val elevation = (pass.maxElevationDeg / 90.0).toFloat().coerceIn(.06f, 1f)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(height).testTag("next_pass_sky_arc")) {
            Canvas(Modifier.matchParentSize()) {
                val left = 6.dp.toPx()
                val right = size.width - left
                val floor = size.height - 22.dp.toPx()
                val rise = (floor - 18.dp.toPx()) * elevation
                fun point(t: Float): Offset {
                    val argument = if (t <= peak) t / peak else (1f - t) / (1f - peak)
                    return Offset(left + (right - left) * t, floor - rise * kotlin.math.sin(argument * (kotlin.math.PI / 2)).toFloat())
                }
                drawLine(tokens.inkTertiary.copy(alpha = .5f), Offset(left, floor), Offset(right, floor), 1.dp.toPx())
                val apex = point(peak)
                drawLine(tokens.inkTertiary.copy(alpha = .55f), apex, Offset(apex.x, floor), 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())))
                val entry = pass.shadowEntryMs?.let(::fraction)
                val exit = pass.shadowExitMs?.let(::fraction)
                val cuts = (listOf(0f, 1f) + listOfNotNull(entry, exit)).distinct().sorted()
                cuts.zipWithNext().forEach { (start, end) ->
                    val mid = (start + end) * .5f
                    val shadow = when {
                        entry != null && exit != null -> if (entry < exit) mid in entry..exit else mid < exit || mid > entry
                        entry != null -> mid >= entry
                        exit != null -> mid <= exit
                        else -> !pass.isIssSunlitAtMax
                    }
                    val path = Path().apply {
                        val first = point(start)
                        moveTo(first.x, first.y)
                        repeat(48) { i -> val p = point(start + (end - start) * (i + 1) / 48f); lineTo(p.x, p.y) }
                    }
                    if (!shadow && tokens.skin == SatSkin.AETHER) drawPath(path, color.copy(alpha = .15f), style = Stroke(7.dp.toPx()))
                    val dash = if (shadow) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())) else null
                    drawPath(path, if (shadow) tokens.umbra else color, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, pathEffect = dash))
                }
                listOfNotNull(entry, exit).filter { it > 0f && it < 1f }.forEach { drawCircle(tokens.umbra, 3.dp.toPx(), point(it)) }
                drawCircle(color, 3.8.dp.toPx(), apex)
                drawCircle(tokens.inkSecondary, 2.dp.toPx(), point(0f)); drawCircle(tokens.inkSecondary, 2.dp.toPx(), point(1f))
                if (train) for (i in 1..7) drawCircle(color, 2.dp.toPx(), point(i / 8f))
                if (nowMs in pass.startTimeMs..pass.endTimeMs) {
                    drawCircle(tokens.inkPrimary, 4.5.dp.toPx(), point(fraction(nowMs)))
                    drawCircle(color, 2.5.dp.toPx(), point(fraction(nowMs)))
                }
            }
            SatText(SatFmt.degrees(pass.maxElevationDeg, isFa), tokens.inkPrimary,
                Modifier.absoluteOffset((maxWidth * peak - 12.dp).coerceIn(0.dp, (maxWidth - 30.dp).coerceAtLeast(0.dp)),
                    ((height - 40.dp) * (1f - elevation)).coerceAtLeast(0.dp)), size = 11, strong = true)
            Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                val points = if (showCompassMarks) listOf(0.0, 90.0, 180.0, 270.0) else listOf(pass.startAzimuthDeg, pass.endAzimuthDeg)
                points.forEach { az -> SatText(SatFmt.compass(az, isFa), tokens.inkSecondary, size = 10) }
            }
        }
    }
}

@Composable
fun SatPassProgressBar(pass: ISSEngine.ISSPass, tokens: ZigSatTokens, nowMs: Long) {
    val span = (pass.endTimeMs - pass.startTimeMs).coerceAtLeast(1L)
    val progress = ((nowMs - pass.startTimeMs).toFloat() / span).coerceIn(0f, 1f)
    val maxFraction = ((pass.maxTimeMs - pass.startTimeMs).toFloat() / span).coerceIn(0f, 1f)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp),
    ) {
        val h = size.height
        drawRoundRect(
            color = tokens.border,
            size = Size(size.width, h),
            cornerRadius = CornerRadius(h / 2f, h / 2f),
        )
        drawRoundRect(
            color = tokens.live,
            topLeft = Offset(if (rtl) size.width * (1f - progress) else 0f, 0f),
            size = Size(size.width * progress, h),
            cornerRadius = CornerRadius(h / 2f, h / 2f),
        )
        listOf(.004f, maxFraction, .996f).forEach { fraction ->
            val x = size.width * if (rtl) 1f - fraction else fraction
            drawLine(tokens.inkTertiary, Offset(x, -2.dp.toPx()), Offset(x, h + 2.dp.toPx()), 1.dp.toPx())
        }
    }
}

// ================================================================================================
// 7. COMPACT PASS CARD + passes by night
// ================================================================================================

/**
 * Compact pass card: the whole card opens detail, the bell is its own 48 dp target, and the date is
 * supplied by the night group header so it is not repeated per card.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun SatCompactPassCard(data: SatPassUi, tokens: ZigSatTokens, isFa: Boolean, actions: SatPassActions,
    modifier: Modifier = Modifier, realNowMs: Long = System.currentTimeMillis(), tz: TimeZone = TimeZone.getDefault()) {
    val pass = data.pass
    var bellAnchor by remember(data.key) { mutableStateOf(IntRect.Zero) }
    val clickLabel = stringResource(R.string.sat_details)
    val holdLabel = stringResource(R.string.sat_show_on_globe)
    ZigSatCard(modifier.testTag("pass_card_${data.key}"), tokens, accent = if (data.isTrain) tokens.starlink else null,
        shape = squircleShape(SatRadius.compactCard)) {
        Column(Modifier.fillMaxWidth().heightIn(min = 92.dp).combinedClickable(role = Role.Button,
            onClickLabel = clickLabel, onLongClickLabel = holdLabel, onClick = actions.onOpenDetail, onLongClick = actions.onShowOnGlobe)
            .padding(start = 12.dp, end = 9.dp, top = 10.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (data.isTrain) PearlGlyph(tokens.starlink) else Icon(satIcon(data.sat), null, Modifier.size(16.dp), tokens.inkSecondary)
                Text(data.satName, Modifier.weight(1f), color = tokens.inkPrimary, style = RedTypographyTokens.badge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                PassQuality(data, tokens, isFa)
            }
            if (data.isTrain) SatText(stringResource(R.string.sat_train_short, SatFmt.int(data.sat.trainCount, isFa)), tokens.inkSecondary, size = 10)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val rise = SatFmt.clock(pass.startTimeMs, isFa, tz)
                val set = SatFmt.clock(pass.endTimeMs, isFa, tz)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SatText(rise, tokens.inkPrimary, size = 19, strong = true)
                    SatText("–", tokens.inkSecondary, size = 19)
                    SatText(set, tokens.inkPrimary, size = 19, strong = true)
                }
                val max = "${stringResource(R.string.sat_max)} ${SatFmt.clock(pass.maxTimeMs, isFa, tz)} · ${SatFmt.degrees(pass.maxElevationDeg, isFa)}"
                SatText(max, tokens.accentInk, size = 12, strong = true)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SatText(SatFmt.direction(pass.startAzimuthDeg, pass.endAzimuthDeg, isFa, compact = true), tokens.inkSecondary, size = 11)
                    SatText("${stringResource(R.string.sat_mag_short)} ${SatFmt.magnitude(pass.estimatedMagnitude, isFa)}", tokens.inkSecondary, size = 11)
                    SatText(SatFmt.duration((pass.passDurationSec / 60L).toInt(), isFa), tokens.inkSecondary, size = 11)
                }
                ZigBellButton(data.reminderLeadMinutes, data.isAutoCovered,
                    isPast = !actions.isReminderEnabled || pass.startTimeMs <= realNowMs ||
                        data.reminderLeadMinutes == null && pass.startTimeMs - 10 * 60_000L <= realNowMs,
                    tokens, isFa, onClick = { actions.onRemind(bellAnchor) },
                    modifier = Modifier.onGloballyPositioned { bellAnchor = it.anchorInWindow() })
            }
        }
    }
}

@Composable
private fun PassQuality(data: SatPassUi, tokens: ZigSatTokens, isFa: Boolean) {
    val score = data.pass.visibilityScore
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        if (tokens.skin == SatSkin.SPATIAL && LocalDensity.current.fontScale < 1.3f) {
            Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.matchParentSize().padding(3.dp)) {
                    drawCircle(tokens.border, style = Stroke(2.dp.toPx()))
                    drawArc(data.quality.hue, -90f, score.coerceIn(0, 100) * 3.6f, false, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                }
                SatText(SatFmt.int(score, isFa), tokens.inkPrimary, size = 10, strong = true)
            }
        } else if (tokens.skin == SatSkin.AETHER && LocalDensity.current.fontScale < 1.3f) {
            Canvas(Modifier.size(28.dp, 20.dp)) {
                val path = Path()
                val cycles = (2 + data.pass.maxElevationDeg / 25).coerceIn(2.0, 5.0)
                repeat(41) { i ->
                    val portion = i / 40f
                    val y = center.y + kotlin.math.sin(portion * cycles * 2 * kotlin.math.PI).toFloat() * size.height * .30f
                    if (i == 0) path.moveTo(0f, y) else path.lineTo(size.width * portion, y)
                }
                drawPath(path, tokens.timeAccent, style = Stroke(1.2.dp.toPx()))
            }
        }
        ZigQualityChip(data.quality, score, tokens, isFa, full = tokens.skin == SatSkin.DEFAULT)
    }
}

@Composable
internal fun SatPassesSheet(tokens: ZigSatTokens, isFa: Boolean, scope: SatelliteCategory, count: Int?,
    groups: List<SatNightGroup>, spotlight: SatSpotlightUi?, now: Long, realNow: Long, side: Boolean, onClose: () -> Unit,
    onClearScope: () -> Unit, onOpenSevenDay: () -> Unit, actionsFor: (SatPassUi) -> SatPassActions,
    sourceModifier: @Composable (String) -> Modifier, tz: TimeZone = TimeZone.getDefault()) {
    val title = stringResource(R.string.sat_passes)
    val days = stringResource(if (count != null && groups.flatMap { it.passes }.let { it.isNotEmpty() && it.all { pass -> pass.isTrain } })
        R.string.sat_two_days else R.string.sat_three_days)
    var overrides by remember(scope) { mutableStateOf<Map<Long, Boolean>>(emptyMap()) }
    var shown by remember(scope) { mutableStateOf<Set<Long>>(emptySet()) }
    SatBottomSheet(tokens, title, "passes_sheet", onClose, half = true, side = side) {
        SatSheetTitle("$title · $days", null, tokens, trailing = {
            if (count != null) SatText(SatFmt.int(count, isFa), tokens.inkPrimary,
                Modifier.background(tokens.cardAlt, CircleShape).border(1.dp, tokens.border, CircleShape).padding(horizontal = 11.dp, vertical = 6.dp), strong = true)
        })
        if (scope != SatelliteCategory.ALL) {
            ZigSatChip(SatFmt.categoryLabel(scope, isFa), false, tokens, onClearScope, trailing = {
                Icon(Icons.Filled.Close, stringResource(R.string.sat_show_all), Modifier.size(14.dp), tint = tokens.inkPrimary)
            })
        }
        spotlight?.let { SatSpotlightCard(it, tokens, isFa, actionsFor(it.pass), arcHeight = 96.dp,
            nowMs = now, realNowMs = realNow, tz = tz) }
        when (count) {
            null -> SatPassSkeletons(tokens)
            0 -> SatPassEmpty(tokens, isFa, if (scope == SatelliteCategory.ALL) null else SatFmt.categoryLabel(scope, isFa), onClearScope, onOpenSevenDay)
            else -> groups.forEachIndexed { i, group -> key(group.nightKey) {
                SatNightSection(group, overrides[group.nightKey] ?: (i == 0), if (group.nightKey in shown) Int.MAX_VALUE else 3,
                    if (scope == SatelliteCategory.STARLINK) 12 else 10, tokens, isFa, actionsFor,
                    onToggle = { overrides = overrides + (group.nightKey to !(overrides[group.nightKey] ?: (i == 0))) },
                    onMore = { shown = shown + group.nightKey }, realNow = realNow, sourceModifier = sourceModifier, tz = tz)
            } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SatNightSection(group: SatNightGroup, expanded: Boolean, shown: Int, cap: Int,
    tokens: ZigSatTokens, isFa: Boolean, actionsFor: (SatPassUi) -> SatPassActions, onToggle: () -> Unit,
    onMore: () -> Unit, realNow: Long, sourceModifier: @Composable (String) -> Modifier = { Modifier },
    tz: TimeZone = TimeZone.getDefault()) {
    val reduce = LocalSatReduceMotion.current
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(if (reduce) 0 else 220), label = "night-chevron")
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("night_group_${group.nightKey}")
        .satMaterial(tokens, SatSlot.PassCard, squircleShape(SatRadius.compactCard), tokens.cardAlt)
        .clickable(role = Role.Button, onClick = onToggle).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            SatText(group.title, tokens.inkPrimary, size = 13, strong = true)
            if (group.subtitle != null) SatText(group.subtitle, tokens.inkTertiary, size = 11)
            if (!expanded && group.best != null) {
                SatText(stringResource(R.string.sat_night_best, group.best.quality.label(isFa),
                    SatFmt.degrees(group.best.pass.maxElevationDeg, isFa)), tokens.inkSecondary, size = 11)
            }
        }
        SatText(if (group.passes.size == 1) stringResource(R.string.sat_pass_count_one) else
            stringResource(R.string.sat_pass_count, SatFmt.int(group.passes.size, isFa)), tokens.inkSecondary, size = 11)
        Icon(Icons.Filled.ExpandMore, null, Modifier.size(18.dp).rotate(rotation), tokens.inkSecondary)
    }
    AnimatedVisibility(expanded, enter = if (reduce) fadeIn(tween(120)) else expandVertically(tween(220)) + fadeIn(tween(150)),
        exit = if (reduce) fadeOut(tween(120)) else shrinkVertically(tween(220)) + fadeOut(tween(150))) {
        Column(Modifier.fillMaxWidth().padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val visible = group.passes.take(shown.coerceAtMost(cap))
            visible.forEach { row -> key(row.key) {
                SatCompactPassCard(row, tokens, isFa, actionsFor(row), sourceModifier(row.key), realNow, tz)
            } }
            val more = minOf(cap, group.passes.size) - visible.size
            if (more > 0) SatDialogButton(stringResource(R.string.sat_more_passes, SatFmt.int(more, isFa)), tokens, onMore,
                modifier = Modifier.fillMaxWidth().testTag("night_show_more_${group.nightKey}"))
        }
    }
}

@Composable
fun SatPassSkeletons(tokens: ZigSatTokens, modifier: Modifier = Modifier) {
    val alpha = if (LocalSatReduceMotion.current) .76f else {
        val transition = rememberInfiniteTransition(label = "SatSkeleton")
        val value by transition.animateFloat(.5f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "opacity")
        value
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) {
            Box(Modifier.fillMaxWidth().height(92.dp).clip(squircleShape(20.dp)).background(tokens.cardAlt.copy(alpha = alpha))
                .border(1.dp, tokens.border, squircleShape(20.dp)))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SatPassEmpty(tokens: ZigSatTokens, isFa: Boolean, scopeLabel: String?, onShowAll: () -> Unit,
    onOpenSevenDay: () -> Unit, modifier: Modifier = Modifier, isTrain: Boolean = false) {
    val span = stringResource(if (isTrain) R.string.sat_two_days else R.string.sat_three_days)
    ZigSatCard(modifier, tokens, shape = squircleShape(SatRadius.compactCard)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Visibility, null, Modifier.size(24.dp), tokens.inkSecondary)
            SatText(if (scopeLabel == null) stringResource(R.string.sat_empty_passes, span) else
                stringResource(R.string.sat_empty_scope, span, scopeLabel), tokens.inkPrimary, size = 14)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (scopeLabel != null) SatDialogButton(stringResource(R.string.sat_show_all), tokens, onShowAll)
                SatDialogButton(stringResource(R.string.sat_seven_day), tokens, onOpenSevenDay)
            }
        }
    }
}
