package com.alijafari.red.astronomy.ui.satellites

import androidx.compose.foundation.interaction.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
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
import androidx.compose.material.icons.automirrored.filled.*
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
import com.alijafari.red.astronomy.astro_engine.SatelliteCategory
import com.alijafari.red.astronomy.astro_engine.SatelliteItem
import com.alijafari.red.astronomy.astro_engine.SatelliteLiveState
import com.alijafari.red.astronomy.ui.aether.AetherDotMatrixText
import com.alijafari.red.astronomy.ui.spatial.DotMatrixText
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
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
    val collapse = offline && stale
    val statusColor = if (data.isLive) tokens.live else tokens.sim
    val staleDescription = stringResource(R.string.sat_stale_tle)
    Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.matchParentSize().padding(vertical = 6.dp)
            .satMaterial(tokens, SatSlot.StatusPill, SatShapes.capsule, tokens.hudFill,
                if (!data.isLive || tokens.glassy) statusColor else null))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.sizeIn(minWidth = 76.dp, minHeight = 48.dp).testTag("satellite_status_chip")
                .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!offline || !data.isLive) ZigStatusDot(statusColor, data.isLive)
                Text(if (!data.isLive) SatFmt.offsetMinutes(data.offsetMinutes,data.isFa) else stringResource(if(offline)R.string.sat_offline else R.string.sat_live),
                    color = tokens.hudInk, softWrap = false,
                    style = RedTypographyTokens.badge.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold).tabular().satOptical())
                if (offline) Icon(Icons.Filled.WifiOff, stringResource(R.string.sat_offline), Modifier.size(14.dp), tint = tokens.hudInk)
                if (stale && !collapse) Icon(Icons.Filled.Schedule, stringResource(R.string.sat_stale_tle), Modifier.size(14.dp),
                    tint = if ((data.tleAgeDays ?: 0.0) > 7.0) tokens.accentInk else tokens.sim)
                if (collapse) SatText(SatFmt.signed(1.0, 0, isFa = data.isFa), tokens.hudInk,
                    Modifier.semantics { contentDescription = staleDescription })
            }
            if (!data.isLive) Box(Modifier.size(48.dp).clickable(role = Role.Button, onClick = onLive), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.sat_return_live), Modifier.size(18.dp).layoutRole(LayoutRole.Mirrors), tint = tokens.hudInk)
            }
        }
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
                !matrix -> SatText(digits, ink, size = 16, strong = true)
                tokens.skin == SatSkin.SPATIAL -> DotMatrixText(digits, dotSize = 0.87.dp, dotGap = 0.57.dp,
                    charGap = 1.dp, activeColor = ink, glowColor = null)
                else -> AetherDotMatrixText(digits, dotSize = 0.87.dp, dotGap = 0.57.dp,
                    charGap = 1.dp, color = ink, glowColor = null)
            }
        }
        if (unit.isNotEmpty()) SatText(unit, tokens.inkSecondary, size = 10)
    }
}

@Composable
internal fun SatLegendPanel(tokens: ZigSatTokens, isFa: Boolean, modifier: Modifier = Modifier) {
    val items = listOf("iss" to R.string.sat_legend_selected,"starlink" to R.string.sat_starlink,"bus" to R.string.sat_sunlit,"bus" to R.string.sat_shadow)
    Column(modifier) { items.chunked(2).forEach { pair -> Row(Modifier.fillMaxWidth()) { pair.forEach { (id,label) ->
        Row(Modifier.weight(1f).padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Icon(SpacecraftArt.vector(id,Lod.M),null,Modifier.size(18.dp),if(id=="starlink") tokens.starlink else tokens.inkPrimary.copy(alpha=if(label==R.string.sat_shadow).55f else 1f))
            SatText(stringResource(label),tokens.inkSecondary,size=11)
        }
    } } } }
}

@Composable
internal fun SatTimeTray(
    tokens: ZigSatTokens, offset: Int, realNow: Long, tz: TimeZone, markers: List<SatTimeMarker>, isFa: Boolean,
    compactHeight: Boolean, actions: SatTimeActions, onClose: () -> Unit, modifier: Modifier = Modifier,
) {
    val clock = SatFmt.dateTime(realNow + offset * 60_000L, isFa, tz)
    val available = nextSatelliteTime(markers, offset) != null
    Column(modifier.fillMaxWidth().testTag("satellite_time_scrubber_card")
        .satMaterial(tokens, SatSlot.Tray, SatShapes.of(SatRadius.card), accent = tokens.timeAccent)
        .padding(horizontal = 10.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SatText(clock, tokens.inkPrimary, Modifier.weight(1f).padding(start = 6.dp), size = 13, strong = true)
            Box(Modifier.widthIn(min = 92.dp).heightIn(min = 48.dp).then(if (offset == 0) Modifier else
                Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.sat_return_live), onClick = actions.onLive)),
                contentAlignment = Alignment.Center) {
                Row(Modifier.background(tokens.hudFill, SatShapes.capsule).border(1.dp, tokens.border, SatShapes.capsule).padding(horizontal = 7.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    ZigStatusDot(if (offset == 0) tokens.live else tokens.sim, offset == 0, 6.dp)
                    SatText(if (offset == 0) stringResource(R.string.sat_live) else SatFmt.offsetMinutes(offset, isFa), tokens.inkPrimary, strong = true)
                    if (offset != 0) Icon(Icons.AutoMirrored.Filled.Undo, null, Modifier.size(12.dp).layoutRole(LayoutRole.Mirrors), tint = tokens.inkPrimary)
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
                stringResource(if (available) R.string.sat_next_pass else R.string.sat_no_pass_range), tokens, actions.onNextPass, modifier=Modifier.layoutRole(LayoutRole.TimeAxis), enabled = available,
                tooltip = if (available) null else stringResource(R.string.sat_no_pass_range))
        }

    }
}

@Composable
private fun SatStepButton(icon: ImageVector, description: String, longDescription: String, tokens: ZigSatTokens,
    onTap: () -> Unit, onRepeat: () -> Unit) {
    val tap by rememberUpdatedState(onTap)
    val step by rememberUpdatedState(onRepeat)
    Box(Modifier.size(48.dp).layoutRole(LayoutRole.TimeAxis).semantics {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SatScrubber(tokens: ZigSatTokens, offset: Int, realNow: Long, tz: TimeZone, markers: List<SatTimeMarker>,
    isFa: Boolean, actions: SatTimeActions, modifier: Modifier = Modifier) {
    val interaction=remember { MutableInteractionSource() };val dragging by interaction.collectIsDraggedAsState()
    val view=LocalView.current;val clock=SatFmt.clock(realNow+offset*60_000L,isFa,tz)
    val spoken=if(offset==0)stringResource(R.string.sat_live) else stringResource(if(offset<0)R.string.sat_spoken_minus else R.string.sat_spoken_plus,SatFmt.duration(abs(offset),isFa),clock)
    val label=stringResource(R.string.sat_slider)
    BoxWithConstraints(modifier.height(48.dp),contentAlignment=Alignment.Center){
        Slider(offset.toFloat(),onValueChange={raw-> val m=snapSatelliteTime(raw,markers)
            if(m!=offset){ if(m==0)view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) else if(markers.any{it.minute==m})view.performHapticFeedback(HapticFeedbackConstants.CONFIRM);actions.onOffsetChange(m) } },
            modifier=Modifier.fillMaxSize().testTag("time_scrubber_slider").layoutRole(LayoutRole.TimeAxis).semantics{contentDescription=label;stateDescription=spoken},
            valueRange=-720f..720f,steps=1439,interactionSource=interaction,
            thumb={Spacer(Modifier.size(if(dragging)32.dp else 20.dp).background(tokens.sim,SatShapes.capsule))},
            track={BoxWithConstraints(Modifier.fillMaxWidth().height(16.dp).testTag("time_scrubber_track"),contentAlignment=Alignment.CenterStart){
                Spacer(Modifier.fillMaxWidth().height(4.dp).background(tokens.border,SatShapes.capsule));val width=maxWidth
                Spacer(Modifier.offset(x=width*.5f).size(1.dp,12.dp).background(tokens.inkSecondary))
                markers.forEach{m->Spacer(Modifier.offset(x=width*((m.minute+720)/1440f)).size(2.dp,8.dp).background(m.quality.graphic(tokens)))}
            }})
        if(dragging) SatText("$clock · ${SatFmt.offsetMinutes(offset,isFa)}",tokens.inkPrimary,Modifier.align(Alignment.TopCenter).offset(y=(-12).dp).background(tokens.hudFill,SatShapes.capsule).padding(horizontal=6.dp),size=11)
    }
}

internal fun androidx.compose.ui.layout.LayoutCoordinates.anchorInWindow(): IntRect {
    val b = boundsInWindow()
    return IntRect(b.left.roundToInt(), b.top.roundToInt(), b.right.roundToInt(), b.bottom.roundToInt())
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
            Box(Modifier.fillMaxWidth().height(92.dp).clip(SatShapes.of(20.dp)).background(tokens.cardAlt.copy(alpha = alpha))
                .border(1.dp, tokens.border, SatShapes.of(20.dp)))
        }
    }
}
