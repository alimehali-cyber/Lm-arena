package com.alijafari.red.astronomy.ui.satellites

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.rotate
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
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
    ).tabular().satOptical())
}

@Composable
internal fun SatStatusPill(data: SatStatusData, tokens: ZigSatTokens, onClick: () -> Unit, onLive: () -> Unit) {
    val offline = !data.isOnline
    val stale = (data.tleAgeDays ?: 0.0) > 2.0
    val collapse = offline && stale && (LocalConfiguration.current.screenWidthDp < 360 || LocalDensity.current.fontScale >= 1.3f)
    val statusColor = if (data.isLive) tokens.live else tokens.sim
    val staleDescription = stringResource(R.string.sat_stale_tle)
    Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.matchParentSize().padding(vertical = 6.dp)
            .satMaterial(tokens, SatSlot.StatusPill, CircleShape, tokens.hudFill,
                if (!data.isLive || tokens.glassy) statusColor else null))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).testTag("satellite_status_chip")
                .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ZigStatusDot(statusColor, data.isLive)
                Text(if (data.isLive) stringResource(R.string.sat_live) else stringResource(R.string.sat_preview_label),
                    color = tokens.hudInk, softWrap = false,
                    style = RedTypographyTokens.badge.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold).tabular().satOptical())
                if (offline) Icon(Icons.Filled.WifiOff, stringResource(R.string.sat_offline), Modifier.size(14.dp), tint = tokens.hudInk)
                if (stale && !collapse) Icon(Icons.Filled.Schedule, stringResource(R.string.sat_stale_tle), Modifier.size(14.dp),
                    tint = if ((data.tleAgeDays ?: 0.0) > 7.0) tokens.accentInk else tokens.sim)
                if (collapse) SatText(SatFmt.signed(1.0, 0, isFa = data.isFa), tokens.hudInk,
                    Modifier.semantics { contentDescription = staleDescription })
            }
            if (!data.isLive) {
                val returnLabel = stringResource(R.string.sat_return_now)
                Box(Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .clickable(role = Role.Button, onClick = onLive).semantics { contentDescription = returnLabel }
                    .padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                    SatText(stringResource(R.string.sat_now_button), tokens.hudInk, strong = true)
                }
            }
        }
    }
}

@Composable
internal fun SatAvatar(item: SatelliteItem, tokens: ZigSatTokens, size: Dp = 40.dp) {
    Box(Modifier.size(size).background((if (item.isTrain || item.category == SatelliteCategory.STARLINK)
        tokens.starlink else tokens.live).copy(alpha = 0.12f), squircleShape(size * .34f)),
        contentAlignment = Alignment.Center) {
        if (item.isTrain) PearlGlyph(tokens.starlink, Modifier.size(size * .70f, size * .34f))
        else SatelliteSilhouette(item, tokens.accentInk, Modifier.size(size * .72f))
    }
}

/** Simple, intentionally literal silhouettes: each instrument/station has a distinct physical outline. */
@Composable
internal fun SatelliteSilhouette(item: SatelliteItem, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        fun p(x: Float, y: Float) = Offset(size.width * x, size.height * y)
        fun rect(left: Float, top: Float, right: Float, bottom: Float) =
            androidx.compose.ui.geometry.Offset(size.width * left, size.height * top) to
                androidx.compose.ui.geometry.Size(size.width * (right - left), size.height * (bottom - top))
        fun line(a: Pair<Float, Float>, b: Pair<Float, Float>, width: Float = .055f) =
            drawLine(color, p(a.first, a.second), p(b.first, b.second), size.minDimension * width, StrokeCap.Round)
        fun panel(left: Float, top: Float, right: Float, bottom: Float, rows: Int = 2) {
            val (offset, bounds) = rect(left, top, right, bottom)
            drawRect(color.copy(alpha = .72f), offset, bounds)
            drawRect(color.copy(alpha = .92f), offset, bounds, style = Stroke(size.minDimension * .028f))
            for (i in 1 until rows) {
                val y = top + (bottom - top) * i / rows
                line(left to y, right to y, .018f)
            }
            val x = (left + right) / 2f
            line(x to top, x to bottom, .018f)
        }
        val iss = item.category == SatelliteCategory.ISS || item.id == "iss_zarya"
        val hubble = item.category == SatelliteCategory.HUBBLE || item.id.contains("hubble", true)
        val tiangong = item.id.contains("tiangong", true)
        when {
            iss -> {
                // ISS: long integrated truss with four broad solar-array wings.
                line(.12f to .50f, .88f to .50f, .065f)
                panel(.04f, .17f, .32f, .39f, 3); panel(.04f, .61f, .32f, .83f, 3)
                panel(.68f, .17f, .96f, .39f, 3); panel(.68f, .61f, .96f, .83f, 3)
                val (o, b) = rect(.40f, .35f, .60f, .65f)
                drawRoundRect(color, o, b, androidx.compose.ui.geometry.CornerRadius(size.minDimension * .07f))
                line(.50f to .30f, .50f to .70f, .025f)
            }
            hubble -> {
                // Hubble: open aperture, long cylindrical telescope bus and two side arrays.
                panel(.02f, .34f, .23f, .66f, 2); panel(.77f, .34f, .98f, .66f, 2)
                line(.22f to .50f, .77f to .50f, .075f)
                val (o, b) = rect(.37f, .31f, .64f, .69f)
                drawRoundRect(color, o, b, androidx.compose.ui.geometry.CornerRadius(size.minDimension * .09f))
                drawCircle(color.copy(alpha = .56f), size.minDimension * .10f, p(.70f, .50f))
                drawCircle(color, size.minDimension * .06f, p(.70f, .50f))
            }
            tiangong -> {
                // Tiangong's T-shaped module stack and deployed solar wings.
                panel(.02f, .30f, .28f, .47f, 2); panel(.72f, .30f, .98f, .47f, 2)
                line(.24f to .39f, .76f to .39f, .05f)
                val (core, coreSize) = rect(.42f, .30f, .58f, .78f)
                drawRoundRect(color, core, coreSize, androidx.compose.ui.geometry.CornerRadius(size.minDimension * .06f))
                val (lab, labSize) = rect(.28f, .47f, .72f, .61f)
                drawRoundRect(color, lab, labSize, androidx.compose.ui.geometry.CornerRadius(size.minDimension * .06f))
            }
            item.category == SatelliteCategory.EARTH_OBSERVATION || item.id.contains("envisat", true) -> {
                // Envisat: compact instrument bus and one long, offset solar-array wing.
                panel(.57f, .20f, .98f, .80f, 3)
                line(.48f to .50f, .62f to .50f, .045f)
                val (o, b) = rect(.16f, .32f, .50f, .68f)
                drawRoundRect(color, o, b, androidx.compose.ui.geometry.CornerRadius(size.minDimension * .04f))
                line(.24f to .32f, .24f to .14f, .025f)
            }
            else -> {
                // Starlink: compact flatbus with one broad deployable solar wing.
                panel(.03f, .22f, .58f, .78f, 3)
                line(.56f to .50f, .68f to .50f, .045f)
                val (o, b) = rect(.64f, .34f, .94f, .66f)
                drawRoundRect(color, o, b, androidx.compose.ui.geometry.CornerRadius(size.minDimension * .04f))
            }
        }
    }
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
    passesLoading: Boolean = false,
    onNextPass: () -> Unit = onPasses,
    pinnedOutsideScope: Boolean = false,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val cycle by rememberUpdatedState(onCycle)
    val passes by rememberUpdatedState(onPasses)
    val previousLabel = stringResource(R.string.sat_prev_sat)
    val nextLabel = stringResource(R.string.sat_next_sat)
    val state = focus.topocentric
    Column(modifier.fillMaxWidth().testTag("sat_focus_card").satMaterial(tokens, SatSlot.FocusCard, squircleShape(SatRadius.card))
        .semantics {
            isTraversalGroup = true
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
            Row(Modifier.weight(1f).heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onDetails),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SatAvatar(focus.satellite, tokens)
                Column(Modifier.weight(1f)) {
                    Text(shortNameOf(focus.satellite, isFa), color = tokens.inkPrimary,
                        style = RedTypographyTokens.sectionTitle.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold).satOptical(),
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    SatText(stringResource(if (state.elevationDeg >= 0.0) R.string.sat_above_your_horizon else R.string.sat_below_your_horizon), tokens.inkSecondary)
                    if (pinnedOutsideScope) SatText(stringResource(R.string.sat_pinned_scope), tokens.inkSecondary, size = 11)
                    if (offline) SatText(stringResource(R.string.sat_cached), tokens.inkSecondary, size = 10)
                }
            }
            SatDialogButton(stringResource(R.string.sat_change_satellite), tokens, onFleet)
        }
        // This satellite's own next pass. The card above the globe is where a satellite is picked,
        // so the pass it carries is the one for the satellite currently under the lens.
        val pass = nextPass?.pass
        val title = when {
            passesLoading -> stringResource(R.string.sat_checking_passes)
            pass == null -> stringResource(R.string.sat_no_visible, span)
            pass.startTimeMs <= nowMs -> stringResource(R.string.sat_in_progress, SatFmt.countdown(pass.endTimeMs - nowMs, isFa))
            else -> stringResource(R.string.sat_next_in, SatFmt.countdown(pass.startTimeMs - nowMs, isFa))
        }
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("sat_focus_passes")
            .clickable(role = Role.Button, onClick = if (pass != null) onNextPass else onPasses), contentAlignment = Alignment.Center) {
            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).background(tokens.cardAlt, CircleShape)
                .border(1.dp, tokens.border, CircleShape).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ZigStatusDot(nextPass?.quality?.graphic(tokens) ?: tokens.telescopic, false, 6.dp)
                val label = nextPass?.quality?.let { if (compact || LocalDensity.current.fontScale >= 1.3f) it.shortLabel(isFa) else it.label(isFa) }
                SatText(if (label != null) "$title · $label" else title, tokens.inkPrimary, Modifier.weight(1f), strong = true)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(16.dp), tint = tokens.inkSecondary)
            }
        }
    }
}

/**
 * The dedicated passes card: the soonest pass over the observer, whoever is flying it, plus the
 * satellite scope that narrows it and the door to the seven-day agenda. It never shows the selected
 * satellite's pass as such — that belongs to [SatFocusCard] — so the two readouts stay separate.
 */
@Composable
internal fun SatNextPassCard(
    next: SatPassUi?,
    scopeLabel: String,
    scopeChoices: List<Pair<String, String>>,
    scopeIds: Set<String>,
    scopeOpen: Boolean,
    loading: Boolean,
    nowMs: Long,
    tz: TimeZone,
    isFa: Boolean,
    compact: Boolean,
    tokens: ZigSatTokens,
    onToggleScopeOpen: () -> Unit,
    onToggleScope: (String) -> Unit,
    onOpenPass: () -> Unit,
    onMorePasses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pass = next?.pass
    val countdown = pass?.let {
        SatFmt.countdown((if (it.startTimeMs <= nowMs) it.endTimeMs else it.startTimeMs) - nowMs, isFa)
    }
    val summary = when {
        loading -> stringResource(R.string.sat_checking_passes)
        pass == null -> stringResource(R.string.sat_pass_card_empty)
        pass.startTimeMs <= nowMs -> stringResource(R.string.sat_in_progress, countdown.orEmpty())
        else -> "${SatFmt.clock(pass.startTimeMs, isFa, tz)} · " + stringResource(R.string.sat_pass_in, countdown.orEmpty())
    }
    Column(modifier.fillMaxWidth().testTag("sat_pass_card")
        .satMaterial(tokens, SatSlot.FocusCard, squircleShape(SatRadius.card))
        .padding(horizontal = 12.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { ZigMicroCaps(stringResource(R.string.sat_pass_card_title), tokens, isFa = isFa) }
            ZigSatChip(scopeLabel, scopeOpen, tokens, onToggleScopeOpen, testTag = "sat_pass_scope_chip",
                trailing = { Icon(Icons.Filled.ExpandMore, null, Modifier.size(14.dp), tint = tokens.inkPrimary) })
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f).heightIn(min = 48.dp)
                .clip(squircleShape(14.dp)).clickable(role = Role.Button, enabled = pass != null, onClick = onOpenPass)
                .testTag("sat_pass_card_open").padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZigStatusDot(next?.quality?.graphic(tokens) ?: tokens.telescopic, false, 7.dp)
                Column(Modifier.weight(1f)) {
                    Text(next?.let { SatFmt.digits(it.satName, isFa) } ?: stringResource(R.string.sat_next_label),
                        color = if (pass == null) tokens.inkSecondary else tokens.inkPrimary,
                        style = RedTypographyTokens.badge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold).satOptical(),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    SatText(summary, tokens.inkSecondary, size = 11, strong = pass != null)
                }
                val quality = next?.quality?.let { if (compact) it.shortLabel(isFa) else it.label(isFa) }
                if (quality != null) SatText(quality, tokens.accentInk, size = 11, strong = true)
                if (pass != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(16.dp), tint = tokens.inkSecondary)
            }
            SatDialogButton(stringResource(R.string.sat_more_passes_button), tokens, onMorePasses, testTag = "sat_all_passes")
        }
        if (scopeOpen) {
            SatPassScopePicker(scopeChoices, scopeIds, tokens, onToggleScope, Modifier.padding(bottom = 4.dp))
        }
    }
}

/** One chip per satellite family, with a leading «همه» that clears the whole scope. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SatPassScopePicker(
    choices: List<Pair<String, String>>,
    selected: Set<String>,
    tokens: ZigSatTokens,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allLabel = stringResource(R.string.sat_pass_scope_all)
    FlowRow(modifier.fillMaxWidth().testTag("pass_scope_picker"),
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ZigSatChip(allLabel, selected.isEmpty(), tokens, { onToggle("") }, testTag = "pass_scope_all")
        choices.forEach { (id, label) ->
            ZigSatChip(label, id in selected, tokens, { onToggle(id) }, testTag = "pass_scope_$id")
        }
    }
}

/** The families both the pass card and the agenda list can be narrowed to. */
@Composable
internal fun satPassScopeChoices(isFa: Boolean): List<Pair<String, String>> {
    val starlinks = stringResource(R.string.sat_starlink_family)
    return remember(isFa, starlinks) {
        SatelliteCatalog.satellites.map { it.id to shortNameOf(it, isFa) } + (STARLINK_SCOPE_ID to starlinks)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SatMetric(value: String, unit: String, label: String, tokens: ZigSatTokens, modifier: Modifier = Modifier,
    muted: Boolean = false, roll: Boolean = false, spokenUnit: String = unit) {
    val ink = if (muted) tokens.inkSecondary else tokens.inkPrimary
    val reduce = LocalSatReduceMotion.current
    val fontScale = LocalDensity.current.fontScale
    val matrix = tokens.glassy && fontScale < 1.3f && value.any(Char::isDigit)
    val numeric = if (matrix) value.filter { it.isDigit() || it in "+−.٫٬,°" } else value
    FlowRow(modifier.clearAndSetSemantics { contentDescription = "$label $value $spokenUnit" },
        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        AnimatedContent(numeric, modifier = if (matrix) Modifier.widthIn(min = if (roll) 32.dp else 30.dp) else Modifier,
            transitionSpec = {
                if (roll && tokens.skin == SatSkin.AETHER && matrix && !reduce) {
                    (slideInVertically(tween(120)) { it / 2 } + fadeIn(tween(120))) togetherWith
                        (slideOutVertically(tween(120)) { -it / 2 } + fadeOut(tween(120)))
                } else fadeIn(tween(120)) togetherWith fadeOut(tween(120))
            }, label = "sat-number") { digits ->
            when {
                !matrix -> SatText(digits, ink, size = 12, strong = true)
                tokens.skin == SatSkin.SPATIAL -> DotMatrixText(digits, dotSize = 0.87.dp, dotGap = 0.57.dp,
                    charGap = 1.dp, activeColor = ink, glowColor = if (reduce) null else tokens.timeAccent)
                else -> AetherDotMatrixText(digits, dotSize = 0.87.dp, dotGap = 0.57.dp,
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
    displayTimeMs: Long = realNow + offset * 60_000L, isPreview: Boolean = offset != 0,
    satelliteName: String = "",
) {
    val clock = SatFmt.dateTime(displayTimeMs, isFa, tz)
    val available = nextSatelliteTime(markers, offset) != null
    Column(modifier.fillMaxWidth().testTag("satellite_time_scrubber_card")
        .satMaterial(tokens, SatSlot.TimeTray, squircleShape(SatRadius.card), accent = tokens.timeAccent)
        .padding(horizontal = 10.dp, vertical = 6.dp)) {
        if (satelliteName.isNotBlank()) SatText(satelliteName, tokens.inkSecondary, Modifier.padding(start = 6.dp), strong = true)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SatText(clock, tokens.inkPrimary, Modifier.weight(1f).padding(start = 6.dp), size = 13, strong = true)
            Box(Modifier.widthIn(min = 92.dp).heightIn(min = 48.dp).then(if (!isPreview) Modifier else
                Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.sat_return_live), onClick = actions.onLive)),
                contentAlignment = Alignment.Center) {
                Row(Modifier.background(tokens.hudFill, CircleShape).border(1.dp, tokens.border, CircleShape).padding(horizontal = 7.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    ZigStatusDot(if (!isPreview) tokens.live else tokens.sim, !isPreview, 6.dp)
                    SatText(if (!isPreview) stringResource(R.string.sat_live) else stringResource(R.string.sat_return_now), tokens.inkPrimary, strong = true)
                    if (isPreview) Icon(Icons.Filled.Replay, null, Modifier.size(12.dp), tint = tokens.inkPrimary)
                }
            }
            ZigSatIconButton(Icons.Filled.Close, stringResource(R.string.sat_close), tokens, onClose)
        }
        if (offset !in -720..720) {
            SatText(stringResource(R.string.sat_outside_timeline), tokens.inkSecondary, Modifier.padding(8.dp))
        } else Row(verticalAlignment = Alignment.CenterVertically) {
            SatStepButton((if (LocalLayoutDirection.current == LayoutDirection.Rtl) Icons.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowLeft), stringResource(R.string.sat_step_prev),
                stringResource(R.string.sat_hour_prev), tokens, { actions.onStep(-15) }, { actions.onStep(-60) })
            SatScrubber(tokens, offset, realNow, tz, markers, isFa, actions, Modifier.weight(1f), displayTimeMs, isPreview)
            SatStepButton((if (LocalLayoutDirection.current == LayoutDirection.Rtl) Icons.Filled.KeyboardArrowLeft else Icons.Filled.KeyboardArrowRight), stringResource(R.string.sat_step_next),
                stringResource(R.string.sat_hour_next), tokens, { actions.onStep(15) }, { actions.onStep(60) })
            ZigSatIconButton(if (LocalLayoutDirection.current == LayoutDirection.Rtl) Icons.Filled.SkipPrevious else Icons.Filled.SkipNext,
                stringResource(if (available) R.string.sat_next_pass else R.string.sat_no_pass_range), tokens, actions.onNextPass, enabled = available,
                tooltip = if (available) null else stringResource(R.string.sat_no_pass_range))
        }
        if (!compactHeight && offset in -720..720) Row(Modifier.fillMaxWidth().padding(start = 48.dp, end = 96.dp, bottom = 3.dp),
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
    isFa: Boolean, actions: SatTimeActions, modifier: Modifier = Modifier,
    displayTimeMs: Long = realNow + offset * 60_000L, isPreview: Boolean = offset != 0) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var dragging by remember { mutableStateOf(false) }
    val latestMarkers by rememberUpdatedState(markers)
    val latestActions by rememberUpdatedState(actions)
    val androidView = LocalView.current
    val simClock = SatFmt.clock(displayTimeMs, isFa, tz)
    val spokenMinutes = abs(offset) % 60
    val spokenHours = abs(offset) / 60
    val interval = listOfNotNull(
        if (spokenHours == 0) null else pluralStringResource(R.plurals.sat_spoken_hours, spokenHours, SatFmt.int(spokenHours, isFa)),
        if (spokenMinutes == 0) null else pluralStringResource(R.plurals.sat_spoken_minutes, spokenMinutes, SatFmt.int(spokenMinutes, isFa)),
    ).joinToString(" ")
    val spoken = when {
        !isPreview -> stringResource(R.string.sat_live)
        offset == 0 -> stringResource(R.string.sat_preview_at, SatFmt.dateTime(displayTimeMs, isFa, tz))
        else -> stringResource(if (offset > 0) R.string.sat_spoken_plus else R.string.sat_spoken_minus, interval, simClock)
    }
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
                drawLine(mark.quality.graphic(tokens), Offset(at, y - 14.dp.toPx()), Offset(at, y - 8.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
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
    selected: Boolean = false,
) {
    val pass = data.pass.pass
    val accent = if (data.pass.isTrain) tokens.starlink else tokens.timeAccent
    var bellAnchor by remember { mutableStateOf(IntRect.Zero) }
    val selectedLabel = stringResource(R.string.sat_selected_pass)
    val selectedStyle = if (selected) Modifier.border(1.5.dp, tokens.accentInk, squircleShape(SatRadius.sheet))
        .semantics { stateDescription = selectedLabel } else Modifier
    ZigSatCard(
        modifier = modifier.then(selectedStyle).testTag("next_pass_spotlight"),
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

            if (selected) SatText(selectedLabel, tokens.accentInk, strong = true)
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
                    style = RedTypographyTokens.sectionHeading.copy(fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold).tabular().satOptical(),
                    color = tokens.inkPrimary, modifier = Modifier.testTag("spotlight_countdown"))
            }
            if (data.simulatedInsidePass) SatText(stringResource(R.string.sat_sim_inside), tokens.inkSecondary)

            SatSkyArc(pass = pass, tokens = tokens, isFa = isFa, height = arcHeight, nowMs = nowMs,
                showCompassMarks = arcHeight >= 160.dp, train = data.pass.isTrain)

            Row(modifier = Modifier.fillMaxWidth()) {
                SpotlightStat(
                    label = stringResource(R.string.sat_rise),
                    value = SatFmt.clock(pass.startTimeMs, isFa, tz),
                    sub = "${SatFmt.degrees(pass.startAzimuthDeg, isFa)} · ${SatFmt.compass(pass.startAzimuthDeg, isFa)}",
                    tokens = tokens,
                    isFa = isFa,
                    modifier = Modifier.weight(1f),
                )
                SpotlightStat(
                    label = stringResource(R.string.sat_max),
                    value = SatFmt.clock(pass.maxTimeMs, isFa, tz),
                    sub = "${SatFmt.degrees(pass.maxAzimuthDeg, isFa)} · ${SatFmt.compass(pass.maxAzimuthDeg, isFa)}",
                    tokens = tokens,
                    isFa = isFa,
                    modifier = Modifier.weight(1f),
                    accent = tokens.accentInk,
                )
                SpotlightStat(
                    label = stringResource(R.string.sat_set),
                    value = SatFmt.clock(pass.endTimeMs, isFa, tz),
                    sub = "${SatFmt.degrees(pass.endAzimuthDeg, isFa)} · ${SatFmt.compass(pass.endAzimuthDeg, isFa)}",
                    tokens = tokens,
                    isFa = isFa,
                    modifier = Modifier.weight(1f),
                )
            }

            SatMetaLine(pass = pass, tokens = tokens, isFa = isFa)

            if (showActions) {
                SatText(stringResource(R.string.sat_ar_live_explanation), tokens.inkSecondary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    SatDialogButton("${stringResource(R.string.sat_remind)} ▾", tokens, { actions.onRemind(bellAnchor) },
                        enabled = actions.isReminderEnabled && (pass.startTimeMs - 10 * 60_000L > realNowMs ||
                            data.pass.reminderLeadMinutes != null && pass.startTimeMs > realNowMs),
                        modifier = Modifier.weight(1f).onGloballyPositioned { bellAnchor = it.anchorInWindow() })
                    SatDialogButton(stringResource(R.string.sat_ar), tokens, actions.onAr, modifier = Modifier.weight(1.2f))
                    ZigSatIconButton(Icons.Filled.CenterFocusWeak, stringResource(R.string.sat_orbit_at_pass), tokens, actions.onShowOnGlobe)
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
            style = RedTypographyTokens.badge.copy(fontSize = 11.sp).tabular().satOptical(),
            color = tokens.inkSecondary,
        )
        Text(
            text = SatFmt.duration(((pass.endTimeMs - pass.startTimeMs) / 60_000L).toInt(), isFa),
            style = RedTypographyTokens.badge.copy(fontSize = 11.sp).tabular().satOptical(),
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
            style = RedTypographyTokens.badge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold).tabular().satOptical(),
            color = accent ?: tokens.inkPrimary,
            maxLines = 1,
        )
        Text(
            text = sub,
            style = RedTypographyTokens.badge.copy(fontSize = 11.sp),
            color = tokens.inkTertiary,
        )
    }
}

/** The horizon is physical: north stays at the back and east at right in both locales. */
@Composable
fun SatSkyArc(pass: ISSEngine.ISSPass, tokens: ZigSatTokens, isFa: Boolean, height: Dp = 112.dp,
    nowMs: Long = System.currentTimeMillis(), showCompassMarks: Boolean = false, train: Boolean = false) {
    val color = if (train) tokens.starlink else if (tokens.skin == SatSkin.AETHER) tokens.timeAccent else tokens.accentInk
    val span = (pass.endTimeMs - pass.startTimeMs).coerceAtLeast(1L).toFloat()
    fun fraction(ms: Long) = ((ms - pass.startTimeMs) / span).coerceIn(0f, 1f)
    val peak = fraction(pass.maxTimeMs).coerceIn(.06f, .94f)
    val magElevation = SatFmt.degrees(pass.maxElevationDeg, isFa)
    val description = stringResource(R.string.sat_sky_description,
        SatFmt.compass(pass.startAzimuthDeg, isFa), magElevation, SatFmt.compass(pass.endAzimuthDeg, isFa))
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val measurer = rememberTextMeasurer()
        val labelStyle = RedTypographyTokens.badge.copy(fontSize = 10.sp).satOptical()
        val peakLabel = measurer.measure(magElevation, labelStyle)
        val cardinalLabels = if (showCompassMarks) listOf(0.0, 90.0, 180.0, 270.0).map { az ->
            az to measurer.measure(SatFmt.compass(az, isFa), labelStyle)
        } else emptyList()
        Canvas(Modifier.fillMaxWidth().height(height).testTag("next_pass_sky_arc")
            .semantics { contentDescription = description }) {
            val rimX = (size.width / 2 - (if (showCompassMarks) 31.dp else 14.dp).toPx()).coerceAtLeast(1f)
            val rimY = size.height * .13f
            val horizon = Offset(size.width / 2, size.height * if (showCompassMarks) .72f else .79f)
            val riseHeight = horizon.y - 19.dp.toPx()
            fun skyPoint(azimuth: Double, elevation: Double): Offset {
                val az = Math.toRadians(azimuth)
                val el = Math.toRadians(elevation)
                val cosEl = kotlin.math.cos(el).toFloat()
                return Offset(horizon.x + rimX * kotlin.math.sin(az).toFloat() * cosEl,
                    horizon.y - rimY * kotlin.math.cos(az).toFloat() * cosEl - riseHeight * kotlin.math.sin(el).toFloat())
            }
            fun point(t: Float): Offset {
                val phase = if (t <= peak) t / peak else (1f - t) / (1f - peak)
                val bearing = if (t <= peak) satBearingBetween(pass.startAzimuthDeg, pass.maxAzimuthDeg, phase)
                    else satBearingBetween(pass.maxAzimuthDeg, pass.endAzimuthDeg, 1f - phase)
                val elev = pass.maxElevationDeg * kotlin.math.sin(phase * (kotlin.math.PI / 2))
                return skyPoint(bearing, elev)
            }
            drawOval(tokens.border, horizon - Offset(rimX, rimY), Size(rimX * 2, rimY * 2), style = Stroke(1.dp.toPx()))
            val apex = point(peak)
            val belowPeak = skyPoint(pass.maxAzimuthDeg, 0.0)
            drawLine(tokens.inkTertiary.copy(alpha = .6f), apex, belowPeak, 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())))
            val entry = pass.shadowEntryMs?.let(::fraction)
            val exit = pass.shadowExitMs?.let(::fraction)
            fun inShadow(mid: Float): Boolean = when {
                entry != null && exit != null -> if (entry < exit) mid in entry..exit else mid < exit || mid > entry
                entry != null -> mid >= entry
                exit != null -> mid <= exit
                else -> !pass.isIssSunlitAtMax
            }
            val cuts = (listOf(0f, 1f) + listOfNotNull(peak, entry, exit)).distinct().sorted()
            cuts.zipWithNext().forEach { (start, end) ->
                val shadow = inShadow((start + end) * .5f)
                val path = Path().apply {
                    val first = point(start)
                    moveTo(first.x, first.y)
                    repeat(32) { i -> val p = point(start + (end - start) * (i + 1) / 32f); lineTo(p.x, p.y) }
                }
                if (!shadow && tokens.skin == SatSkin.AETHER) drawPath(path, color.copy(alpha = .15f), style = Stroke(7.dp.toPx()))
                val dash = if (shadow) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())) else null
                drawPath(path, if (shadow) tokens.umbra else color, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, pathEffect = dash))
            }
            listOfNotNull(entry, exit).filter { it > 0f && it < 1f }.forEach { drawCircle(tokens.umbra, 3.dp.toPx(), point(it)) }
            // The path is plotted in physical azimuth space under an LTR drawing context; this
            // arrow therefore shows actual rise-to-set travel in both Persian and English layouts.
            val arrowFraction = if (peak < .56f) .64f else .38f
            val arrowTail = point(arrowFraction - .035f)
            val arrowTip = point(arrowFraction)
            val angle = kotlin.math.atan2(arrowTip.y - arrowTail.y, arrowTip.x - arrowTail.x)
            val head = 7.dp.toPx()
            val wingA = Offset(arrowTip.x - head * kotlin.math.cos(angle - .62f), arrowTip.y - head * kotlin.math.sin(angle - .62f))
            val wingB = Offset(arrowTip.x - head * kotlin.math.cos(angle + .62f), arrowTip.y - head * kotlin.math.sin(angle + .62f))
            drawLine(color, arrowTip, wingA, 1.8.dp.toPx(), StrokeCap.Round)
            drawLine(color, arrowTip, wingB, 1.8.dp.toPx(), StrokeCap.Round)
            drawCircle(color, 3.8.dp.toPx(), apex)
            drawCircle(tokens.inkSecondary, 2.dp.toPx(), point(0f))
            drawCircle(tokens.inkSecondary, 2.dp.toPx(), point(1f))
            if (train) {
                for (i in 1..7) {
                    val f = i / 8f
                    drawCircle(if (inShadow(f)) tokens.umbra else color, 2.dp.toPx(), point(f))
                }
            }
            if (nowMs in pass.startTimeMs..pass.endTimeMs) {
                drawCircle(tokens.inkPrimary, 4.5.dp.toPx(), point(fraction(nowMs)))
                drawCircle(color, 2.5.dp.toPx(), point(fraction(nowMs)))
            }
            drawText(peakLabel, color = tokens.inkPrimary, topLeft = Offset(
                (apex.x - peakLabel.size.width / 2f).coerceIn(0f, (size.width - peakLabel.size.width).coerceAtLeast(0f)),
                (apex.y - peakLabel.size.height - 5.dp.toPx()).coerceAtLeast(0f)))
            cardinalLabels.forEach { (az, label) ->
                val at = skyPoint(az, 0.0)
                val x = when (az) {
                    90.0 -> at.x + 4.dp.toPx()
                    270.0 -> at.x - label.size.width - 4.dp.toPx()
                    else -> at.x - label.size.width / 2f
                }
                val y = if (az == 180.0) at.y + 2.dp.toPx() else at.y - label.size.height * .63f
                drawText(label, color = tokens.inkSecondary, topLeft = Offset(
                    x.coerceIn(0f, (size.width - label.size.width).coerceAtLeast(0f)),
                    y.coerceIn(0f, (size.height - label.size.height).coerceAtLeast(0f))))
            }
        }
    }
}

internal fun satBearingBetween(from: Double, to: Double, fraction: Float): Double {
    val delta = ((to - from + 540.0) % 360.0) - 180.0
    return (from + fraction * delta + 360.0) % 360.0
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
    modifier: Modifier = Modifier, realNowMs: Long = System.currentTimeMillis(), tz: TimeZone = TimeZone.getDefault(),
    selected: Boolean = false) {
    val pass = data.pass
    var bellAnchor by remember(data.key) { mutableStateOf(IntRect.Zero) }
    val clickLabel = stringResource(R.string.sat_details)
    val holdLabel = stringResource(R.string.sat_orbit_at_pass)
    val selectedLabel = stringResource(R.string.sat_selected_pass)
    val selectedStyle = if (selected) Modifier.border(1.5.dp, tokens.accentInk, squircleShape(SatRadius.compactCard))
        .semantics { stateDescription = selectedLabel } else Modifier
    ZigSatCard(modifier.then(selectedStyle).testTag("pass_card_${data.key}"), tokens, accent = if (data.isTrain) tokens.starlink else null,
        shape = squircleShape(SatRadius.compactCard)) {
        Column(Modifier.fillMaxWidth().heightIn(min = 92.dp).combinedClickable(role = Role.Button,
            onClickLabel = clickLabel, onLongClickLabel = holdLabel, onClick = actions.onOpenDetail, onLongClick = actions.onShowOnGlobe)
            .padding(start = 12.dp, end = 9.dp, top = 10.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (data.isTrain) PearlGlyph(tokens.starlink) else SatelliteSilhouette(data.sat, tokens.inkSecondary, Modifier.size(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(data.satName, color = tokens.inkPrimary, style = RedTypographyTokens.badge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (selected) SatText(selectedLabel, tokens.accentInk, size = 11, strong = true)
                }
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
    ZigQualityChip(data.quality, score, tokens, isFa, full = tokens.skin == SatSkin.DEFAULT)
}

@Composable
internal fun SatPassesSheet(tokens: ZigSatTokens, isFa: Boolean, scopeIds: Set<String>, scopeChoices: List<Pair<String, String>>,
    onToggleScope: (String) -> Unit, count: Int?,
    groups: List<SatNightGroup>, spotlight: SatSpotlightUi?, now: Long, realNow: Long, side: Boolean, onClose: () -> Unit,
    actionsFor: (SatPassUi) -> SatPassActions,
    sourceModifier: @Composable (String) -> Modifier, tz: TimeZone, isOnline: Boolean = true,
    selectedPassKey: String? = null) {
    val title = stringResource(R.string.sat_passes)
    val days = stringResource(R.string.sat_seven_days)
    val scopeKey = scopeIds.sorted().joinToString(",")
    val scopeLabel = when {
        scopeIds.isEmpty() -> null
        scopeIds.size == 1 -> scopeChoices.firstOrNull { it.first in scopeIds }?.second
        else -> stringResource(R.string.sat_pass_scope_count, SatFmt.int(scopeIds.size, isFa))
    }
    var overrides by rememberSaveable(scopeKey, stateSaver = SatNightOverridesSaver) { mutableStateOf(emptyMap<Long, Boolean>()) }
    var shown by rememberSaveable(scopeKey) { mutableStateOf(emptyList<Long>()) }
    SatBottomSheet(tokens, title, "passes_sheet", onClose, half = true, side = side) {
        SatSheetTitle("$title · $days", null, tokens, trailing = {
            if (count != null) SatText(SatFmt.int(count, isFa), tokens.inkPrimary,
                Modifier.background(tokens.cardAlt, CircleShape).border(1.dp, tokens.border, CircleShape).padding(horizontal = 11.dp, vertical = 6.dp), strong = true)
        })
        if (count != null && count > 0) SatText(stringResource(R.string.sat_score_explanation), tokens.inkSecondary, size = 12)
        ZigMicroCaps(stringResource(R.string.sat_pass_scope_row), tokens, isFa = isFa)
        SatPassScopePicker(scopeChoices, scopeIds, tokens, onToggleScope)
        if (!isOnline && count != null) SatText(stringResource(R.string.sat_passes_cached), tokens.inkSecondary, size = 11)
        val next = groups.asSequence().flatMap { it.passes.asSequence() }
            .filter { it.pass.endTimeMs > now }.minByOrNull { it.pass.startTimeMs }
        if (next != null) SatText(stringResource(R.string.sat_next_agenda,
            next.satName, SatFmt.dateTime(next.pass.startTimeMs, isFa, tz)), tokens.inkPrimary, strong = true)
        val featured = spotlight ?: next?.let { SatSpotlightUi(it, it.pass.startTimeMs <= now,
            ((it.pass.startTimeMs - now) / 60_000L).toInt().coerceAtLeast(0), false) }
        featured?.let {
            ZigMicroCaps(stringResource(if (spotlight == null) R.string.sat_next_label else R.string.sat_recommended), tokens, isFa = isFa)
            SatSpotlightCard(it, tokens, isFa, actionsFor(it.pass), arcHeight = 96.dp, nowMs = now, realNowMs = realNow, tz = tz,
                selected = it.pass.key == selectedPassKey)
            if (groups.isNotEmpty()) ZigMicroCaps(stringResource(R.string.sat_all_upcoming), tokens, isFa = isFa)
        }
        when (count) {
            null -> SatPassSkeletons(tokens)
            0 -> SatPassEmpty(tokens, isFa, scopeLabel, { onToggleScope("") })
            else -> groups.forEachIndexed { i, group -> key(group.nightKey) {
                val selectedIndex = group.passes.indexOfFirst { it.key == selectedPassKey }
                val initiallyOpen = i == 0 || selectedIndex >= 0
                val defaultCap = if (group.passes.all { it.isTrain }) 12 else 10
                val visibleCount = if (group.nightKey in shown) Int.MAX_VALUE else maxOf(3, selectedIndex + 1)
                SatNightSection(group, overrides[group.nightKey] ?: initiallyOpen, visibleCount,
                    maxOf(defaultCap, selectedIndex + 1), tokens, isFa, actionsFor,
                    onToggle = { overrides = overrides + (group.nightKey to !(overrides[group.nightKey] ?: initiallyOpen)) },
                    onMore = { shown = (shown + group.nightKey).distinct() }, realNow = realNow, sourceModifier = sourceModifier, tz = tz,
                    selectedPassKey = selectedPassKey)
            } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SatNightSection(group: SatNightGroup, expanded: Boolean, shown: Int, cap: Int,
    tokens: ZigSatTokens, isFa: Boolean, actionsFor: (SatPassUi) -> SatPassActions, onToggle: () -> Unit,
    onMore: () -> Unit, realNow: Long, sourceModifier: @Composable (String) -> Modifier = { Modifier },
    tz: TimeZone = TimeZone.getDefault(), selectedPassKey: String? = null) {
    val reduce = LocalSatReduceMotion.current
    val expandedLabel = stringResource(R.string.sat_expanded)
    val collapsedLabel = stringResource(R.string.sat_collapsed)
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(if (reduce) 0 else 220), label = "night-chevron")
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("night_group_${group.nightKey}")
        .satMaterial(tokens, SatSlot.PassCard, squircleShape(SatRadius.compactCard), tokens.cardAlt)
        .clickable(role = Role.Button, onClick = onToggle).semantics { stateDescription = if (expanded) expandedLabel else collapsedLabel }.padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            SatText(group.title, tokens.inkPrimary, size = 13, strong = true)
            if (group.subtitle != null) SatText(group.subtitle, tokens.inkTertiary, size = 11)
            if (!expanded && group.passes.any { it.key == selectedPassKey }) {
                SatText(stringResource(R.string.sat_selected_pass), tokens.accentInk, size = 11, strong = true)
            }
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
                SatCompactPassCard(row, tokens, isFa, actionsFor(row), sourceModifier(row.key), realNow, tz,
                    selected = row.key == selectedPassKey)
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
    modifier: Modifier = Modifier, isTrain: Boolean = false) {
    val span = stringResource(if (isTrain) R.string.sat_two_days else R.string.sat_seven_days)
    ZigSatCard(modifier, tokens, shape = squircleShape(SatRadius.compactCard)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Visibility, null, Modifier.size(24.dp), tokens.inkSecondary)
            SatText(if (scopeLabel == null) stringResource(R.string.sat_empty_passes, span) else
                stringResource(R.string.sat_empty_scope, span, scopeLabel), tokens.inkPrimary, size = 14)
            if (scopeLabel != null) SatDialogButton(stringResource(R.string.sat_show_all), tokens, onShowAll)
        }
    }
}
