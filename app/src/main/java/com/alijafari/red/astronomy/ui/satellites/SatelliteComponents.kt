package com.alijafari.red.astronomy.ui.satellites

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteCategory
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedTheme
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape
import java.util.TimeZone

// ================================================================================================
// 1. HEADER ROW — one status chip (start) · alerts bell · guide (end)
// ================================================================================================

/**
 * One capsule replaces the three badges. Offline and TLE staleness become inline glyphs inside it,
 * and anything that does not fit collapses into a `+N` counter — text is never truncated mid-word.
 */
@Composable
fun SatStatusChip(
    data: SatStatusData,
    tokens: ZigSatTokens,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    skin: SatSkin = SatSkin.DEFAULT,
) {
    val isFa = data.isFa
    val glyphCount = 2 + (if (!data.isOnline) 1 else 0) +
        (if ((data.tleAgeDays ?: 0.0) > 2.0) 1 else 0) +
        (if (data.usingCachedTle) 1 else 0)

    val stateColor = if (data.isLive) tokens.live else tokens.sim
    val stateText = when {
        data.isLive -> if (isFa) "زنده · SGP4" else "Live · SGP4"
        else -> if (isFa) {
            "شبیه‌سازی ${SatFmt.offsetMinutes(data.offsetMinutes, true)}"
        } else {
            "Simulation ${SatFmt.offsetMinutes(data.offsetMinutes, false)}"
        }
    }
    val meshLive = skin == SatSkin.SPATIAL && data.isLive
    val fill = when {
        meshLive -> Color(0xFFB4462A)
        skin == SatSkin.AETHER -> tokens.card.copy(alpha = 0.55f)
        else -> tokens.cardAlt
    }

    Box(
        modifier = modifier
            .testTag("satellite_status_chip")
            .height(SatGrid.minTouchTarget)
            .clip(RedShapes.capsule)
            .background(fill)
            .border(1.dp, if (meshLive) Color(0x66FF9E7A) else tokens.border, RedShapes.capsule)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ZigStatusDot(color = if (meshLive) Color.White else stateColor, pulse = data.isLive)
            Text(
                text = stateText,
                style = RedTypographyTokens.badge.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold).tabular(),
                color = if (meshLive) Color.White else tokens.inkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (!data.isOnline) {
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = if (isFa) "آفلاین" else "Offline",
                    tint = RedTheme.colors.statusError,
                    modifier = Modifier.size(14.dp),
                )
            }
            val age = data.tleAgeDays
            if (age != null && age > 2.0) {
                val tint = when {
                    age > 7.0 -> RedTheme.colors.statusError
                    tokens.skin == SatSkin.DEFAULT -> Color(0xFF8A6A1F)
                    else -> tokens.sim
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = if (isFa) "عمر داده" else "TLE age",
                        tint = tint,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = SatFmt.digits("${age.toInt()}d", isFa),
                        style = RedTypographyTokens.badge.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                        color = tint,
                        maxLines = 1,
                    )
                }
            }
            if (glyphCount > 3) {
                Text(
                    text = SatFmt.digits("+${glyphCount - 3}", isFa),
                    style = RedTypographyTokens.badge.copy(fontSize = 10.sp),
                    color = tokens.inkTertiary,
                    maxLines = 1,
                )
            }
        }
    }
}

// ================================================================================================
// 2. GLOBE STAGE — the untouched globe plus four non-competing HUD zones
// ================================================================================================

/**
 * Wraps the globe with the control column (Reset · Map Theme · Legend) inside the end margin, keeps
 * the outer scroll-passthrough margins free, and paints the simulation vignette when scrubbing.
 */
@Composable
fun SatGlobeStage(
    tokens: ZigSatTokens,
    mapThemeLabel: String,
    mapThemeIcon: ImageVector,
    isSimulated: Boolean,
    onReset: () -> Unit,
    onCycleMapTheme: () -> Unit,
    legendExpanded: Boolean,
    onToggleLegend: () -> Unit,
    isFa: Boolean,
    modifier: Modifier = Modifier,
    controlColumnExtra: (@Composable () -> Unit)? = null,
    globe: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.16f)
            .testTag("satellite_globe_stage"),
    ) {
        globe()

        if (isSimulated) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, tokens.sim.copy(alpha = 0.35f)),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .width(SatGrid.minTouchTarget),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SatGlobeControl(
                icon = Icons.Filled.CenterFocusWeak,
                label = if (isFa) "مرکز" else "Reset",
                tokens = tokens,
                onClick = onReset,
                testTag = "map_reset_view_stage",
            )
            SatGlobeControl(
                icon = mapThemeIcon,
                label = mapThemeLabel,
                tokens = tokens,
                onClick = onCycleMapTheme,
                testTag = "map_theme_selector",
            )
            SatGlobeControl(
                icon = Icons.Filled.Layers,
                label = if (isFa) "راهنما" else "Legend",
                tokens = tokens,
                active = legendExpanded,
                onClick = onToggleLegend,
                testTag = "globe_legend_toggle",
            )
            controlColumnExtra?.invoke()
        }

        AnimatedVisibility(
            visible = legendExpanded,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 12.dp),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            SatLegendPanel(tokens = tokens, isFa = isFa)
        }
    }
}

@Composable
private fun SatGlobeControl(
    icon: ImageVector,
    label: String,
    tokens: ZigSatTokens,
    onClick: () -> Unit,
    testTag: String,
    active: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(SatGrid.minTouchTarget)
            .testTag(testTag)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    when {
                        active -> tokens.live.copy(alpha = 0.22f)
                        tokens.skin == SatSkin.AETHER -> Color(0x66000000)
                        tokens.skin == SatSkin.SPATIAL -> tokens.card.copy(alpha = 0.55f)
                        else -> tokens.cardAlt
                    }
                )
                .border(
                    1.dp,
                    if (tokens.skin == SatSkin.DEFAULT) tokens.border else tokens.inkPrimary.copy(alpha = 0.22f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) tokens.live else tokens.inkPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Six marker swatches in a 172 dp panel, bottom-start, auto-hidden by the screen on globe drag. */
@Composable
fun SatLegendPanel(tokens: ZigSatTokens, isFa: Boolean) {
    val rows: List<Triple<String, Color, Boolean>> = listOf(
        Triple(if (isFa) "انتخاب‌شده + مسیر" else "Selected + track", tokens.live, false),
        Triple(if (isFa) "با چشم غیرمسلح" else "Naked-eye", tokens.nakedEye, false),
        Triple(if (isFa) "تلسکوپی" else "Telescopic", tokens.telescopic, true),
        Triple(if (isFa) "استارلینک (آفتاب)" else "Starlink sunlit", tokens.starlink, false),
        Triple(if (isFa) "قطار مروارید" else "Train \u25CF\u2500\u25CF\u2500\u25CF", tokens.starlink, false),
        Triple(if (isFa) "موقعیت شما" else "You", tokens.inkPrimary, false),
    )
    Column(
        modifier = Modifier
            .width(172.dp)
            .clip(squircleShape(SatRadius.popover))
            .background(if (tokens.glassy) tokens.popoverFill.copy(alpha = 0.86f) else tokens.cardAlt)
            .border(1.dp, tokens.border, squircleShape(SatRadius.popover))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("globe_legend_panel"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.forEach { (label, color, hollow) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (hollow) Color.Transparent else color.copy(alpha = 0.9f))
                        .border(1.dp, color, CircleShape),
                )
                Text(
                    text = label,
                    style = RedTypographyTokens.badge.copy(fontSize = 11.sp),
                    color = tokens.inkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ================================================================================================
// 3. FOCUS DOCK — the satellite the globe is centred on (one fact, one place)
// ================================================================================================

@Composable
fun SatFocusDock(
    data: SatDockData,
    tokens: ZigSatTokens,
    compact: Boolean,
    onDetails: () -> Unit,
    onAr: () -> Unit,
    onShowOnGlobe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFa = data.isFa
    ZigSatCard(
        modifier = modifier.testTag("satellite_focus_dock"),
        tokens = tokens,
        shape = squircleShape(SatRadius.card),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(tokens.live.copy(alpha = 0.16f))
                        .border(1.dp, tokens.live.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.SatelliteAlt,
                        contentDescription = null,
                        tint = tokens.live,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.fullName,
                        style = RedTypographyTokens.sectionTitle.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = tokens.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("dock_sat_name"),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = if (data.isSunlit) "\u2600" else "\u263E",
                            style = RedTypographyTokens.badge.copy(fontSize = 11.sp),
                            color = if (data.isSunlit) Color(0xFFE0A020) else tokens.umbra,
                        )
                        Text(
                            text = (if (data.isSunlit) (if (isFa) "آفتاب‌خورده" else "Sunlit")
                            else (if (isFa) "در سایه زمین" else "Earth shadow")) + " · " +
                                (if (data.isAboveHorizon) (if (isFa) "بالای افق" else "Above horizon")
                                else (if (isFa) "زیر افق" else "Below horizon")),
                            style = RedTypographyTokens.badge.copy(fontSize = 11.sp),
                            color = tokens.inkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                ZigSatIconButton(
                    icon = Icons.Filled.Visibility,
                    contentDescription = if (isFa) "نمایش در واقعیت افزوده" else "Spot in AR",
                    tokens = tokens,
                    onClick = onAr,
                    testTag = "dock_ar",
                )
                ZigSatIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = if (isFa) "جزئیات" else "Details",
                    tokens = tokens,
                    onClick = onDetails,
                    active = true,
                    testTag = "dock_details",
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val speedLabel = if (isFa) "سرعت" else "SPEED"
            val elevLabel = if (isFa) "زاویه" else "ELEV"
            val altLabel = if (isFa) "ارتفاع" else "ALT"
            val rangeLabel = if (isFa) "فاصله" else "RANGE"
            val metrics = if (compact) {
                listOf(
                    altLabel to "${SatFmt.kilometers(data.altitudeKm, isFa)} km",
                    elevLabel to SatFmt.signed(data.elevationDeg, 0, "°", isFa),
                    speedLabel to "${SatFmt.speed(data.speedKmS, isFa)} km/s",
                )
            } else {
                listOf(
                    altLabel to "${SatFmt.kilometers(data.altitudeKm, isFa)} km",
                    speedLabel to "${SatFmt.speed(data.speedKmS, isFa)} km/s",
                    elevLabel to SatFmt.signed(data.elevationDeg, 0, "°", isFa),
                    rangeLabel to "${SatFmt.kilometers(data.rangeKm, isFa)} km",
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                metrics.forEachIndexed { index, (label, value) ->
                    Column(modifier = Modifier.weight(1f)) {
                        ZigMicroCaps(text = label, tokens = tokens, isFa = isFa)
                        Text(
                            text = value,
                            style = RedTypographyTokens.badge.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            ).tabular(),
                            color = if (label == elevLabel && !data.isAboveHorizon) tokens.inkTertiary else tokens.inkPrimary,
                            maxLines = 1,
                        )
                    }
                    if (index < metrics.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(tokens.border),
                        )
                    }
                }
            }

            if (data.isCached) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isFa) "کَش‌شده" else "cached",
                    style = RedTypographyTokens.badge.copy(fontSize = 10.sp),
                    color = tokens.inkTertiary,
                )
            }
        }
    }
}

// ================================================================================================
// 4. TIME STRIP — the only time control on the page
// ================================================================================================

/**
 * Header (date/time + Live capsule) · control row (‹ slider › ⏭) · scale row. ±720 min in one-minute
 * steps with a magnetic Live detent and pass markers above the track.
 */
@Composable
fun SatTimeStrip(
    tokens: ZigSatTokens,
    offsetMinutes: Int,
    clockLabel: String,
    passMarkers: List<Int>,
    nextPassAvailable: Boolean,
    actions: SatTimeActions,
    isFa: Boolean,
    modifier: Modifier = Modifier,
) {
    ZigSatCard(
        modifier = modifier.testTag("satellite_time_scrubber_card"),
        tokens = tokens,
        shape = squircleShape(SatRadius.card),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = tokens.live,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = clockLabel,
                    style = RedTypographyTokens.badge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold).tabular(),
                    color = tokens.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                SatLiveCapsule(
                    offsetMinutes = offsetMinutes,
                    tokens = tokens,
                    isFa = isFa,
                    onClick = actions.onLive,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SatStepButton(
                    label = "\u2039",
                    tokens = tokens,
                    onClick = { actions.onStep(-15) },
                    onLongClick = { actions.onStep(-60) },
                )
                SatScrubber(
                    offsetMinutes = offsetMinutes,
                    tokens = tokens,
                    passMarkers = passMarkers,
                    onOffsetChange = actions.onOffsetChange,
                    isFa = isFa,
                    modifier = Modifier.weight(1f),
                )
                SatStepButton(
                    label = "\u203A",
                    tokens = tokens,
                    onClick = { actions.onStep(15) },
                    onLongClick = { actions.onStep(60) },
                )
                SatStepButton(
                    label = null,
                    icon = Icons.Filled.SkipNext,
                    tokens = tokens,
                    enabled = nextPassAvailable,
                    onClick = actions.onNextPass,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf(
                    if (isFa) "\u2212۱۲س" else "\u221212h",
                    if (isFa) "\u2212۶س" else "\u22126h",
                    if (isFa) "زنده" else "Live",
                    if (isFa) "+۶س" else "+6h",
                    if (isFa) "+۱۲س" else "+12h",
                ).forEach { label ->
                    Text(
                        text = label,
                        style = RedTypographyTokens.badge.copy(fontSize = 10.sp).tabular(),
                        color = if (label == (if (isFa) "زنده" else "Live")) tokens.live else tokens.inkTertiary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SatLiveCapsule(
    offsetMinutes: Int,
    tokens: ZigSatTokens,
    isFa: Boolean,
    onClick: () -> Unit,
) {
    val isLive = offsetMinutes == 0
    Row(
        modifier = Modifier
            .clip(RedShapes.capsule)
            .background(if (isLive) tokens.live.copy(alpha = 0.14f) else tokens.sim.copy(alpha = 0.18f))
            .border(
                1.dp,
                if (isLive) tokens.live.copy(alpha = 0.45f) else tokens.sim.copy(alpha = 0.55f),
                RedShapes.capsule,
            )
            .then(if (isLive) Modifier else Modifier.clickable(onClick = onClick))
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("time_live_capsule"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ZigStatusDot(color = if (isLive) tokens.live else tokens.sim, pulse = isLive, size = 7.dp)
        Text(
            text = if (isLive) (if (isFa) "زنده" else "Live") else SatFmt.offsetMinutes(offsetMinutes, isFa) + " \u21BA",
            style = RedTypographyTokens.badge.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold).tabular(),
            color = if (isLive) tokens.live else tokens.sim,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SatStepButton(
    label: String?,
    tokens: ZigSatTokens,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onLongClick: () -> Unit = {},
) {
    // A tap steps 15 minutes, a long press a whole hour — the spec's two-step scrub affordance.
    Box(
        modifier = Modifier
            .size(SatGrid.minTouchTarget)
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = if (enabled) {
                    "Next pass"
                } else {
                    "No pass in ±12h"
                },
                tint = if (enabled) tokens.inkPrimary else tokens.inkTertiary.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp),
            )
        } else {
            Text(
                text = label.orEmpty(),
                style = RedTypographyTokens.sectionTitle.copy(fontSize = 20.sp),
                color = tokens.inkPrimary,
            )
        }
    }
}

/**
 * 4 dp track, 20 dp thumb (32 while dragging), hour ticks, pass markers and the Live detent. The
 * track is drawn in the ambient layout direction, so past sits at the end edge in Persian.
 */
@Composable
private fun SatScrubber(
    offsetMinutes: Int,
    tokens: ZigSatTokens,
    passMarkers: List<Int>,
    onOffsetChange: (Int) -> Unit,
    isFa: Boolean,
    modifier: Modifier = Modifier,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var dragging by remember { mutableStateOf(false) }
    var dragMinutes by remember { mutableStateOf(offsetMinutes.toFloat()) }
    val shown = if (dragging) dragMinutes.toInt() else offsetMinutes

    LaunchedEffect(offsetMinutes) {
        if (!dragging) dragMinutes = offsetMinutes.toFloat()
    }
    val haptics = rememberSatHaptics()

    // Physical fraction (0 at the physical start edge) → signed minutes, 1-minute steps.
    fun minutesFor(fraction: Float): Int {
        val logical = if (isRtl) 1f - fraction else fraction
        val raw = (logical - 0.5f) * 1440f
        val snapped = if (kotlin.math.abs(raw) <= 4f) 0f else raw
        return snapped.toInt().coerceIn(-720, 720)
    }

    // Screen readers get the same state the bubble shows, so the slider is usable without sight.
    val stateText = when {
        offsetMinutes == 0 -> if (isFa) "زنده" else "Live"
        else -> SatFmt.offsetMinutes(offsetMinutes, isFa)
    }
    Column(
        modifier = modifier
            .testTag("time_scrubber_slider")
            .semantics {
                stateDescription = stateText
                contentDescription = if (isFa) {
                    "خط زمان ماهواره، ±۱۲ ساعت"
                } else {
                    "Satellite time, ±12 hours"
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .padding(horizontal = 10.dp),
        ) {
            if (dragging) {
                Text(
                    text = SatFmt.offsetMinutes(shown, isFa),
                    style = RedTypographyTokens.badge.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold).tabular(),
                    color = tokens.inkPrimary,
                    modifier = Modifier
                        .align(if (shown >= 0) Alignment.CenterEnd else Alignment.CenterStart)
                        .clip(RedShapes.capsule)
                        .background(tokens.cardAlt)
                        .border(1.dp, tokens.border, RedShapes.capsule)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(horizontal = 10.dp)
                .testTag("time_scrubber_track")
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val minutes = minutesFor((offset.x / size.width).coerceIn(0f, 1f))
                        dragMinutes = minutes.toFloat()
                        onOffsetChange(minutes)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragging = true
                            dragMinutes = minutesFor((offset.x / size.width).coerceIn(0f, 1f)).toFloat()
                        },
                        onDragEnd = {
                            dragging = false
                            onOffsetChange(dragMinutes.toInt())
                        },
                        onDragCancel = {
                            dragging = false
                            onOffsetChange(dragMinutes.toInt())
                        },
                    ) { change, _ ->
                        val minutes = minutesFor((change.position.x / size.width).coerceIn(0f, 1f))
                        if (minutes != dragMinutes.toInt()) {
                            if (minutes == 0 || kotlin.math.abs(minutes) % 60 == 0) haptics()
                            dragMinutes = minutes.toFloat()
                            onOffsetChange(minutes)
                        }
                    }
                },
        ) {
            val centerY = size.height * 0.6f
            val trackH = 4.dp.toPx()
            val inset = 2.dp.toPx()
            val usable = (size.width - inset * 2f).coerceAtLeast(1f)
            fun xFor(minutes: Int): Float {
                val logical = (minutes / 1440f + 0.5f).coerceIn(0f, 1f)
                val fraction = if (isRtl) 1f - logical else logical
                return inset + usable * fraction
            }

            drawRoundRect(
                color = tokens.border,
                topLeft = Offset(inset, centerY - trackH / 2f),
                size = Size(usable, trackH),
                cornerRadius = CornerRadius(trackH / 2f, trackH / 2f),
            )

            for (hour in -12..12) {
                val x = xFor(hour * 60)
                val major = hour % 6 == 0
                val half = if (major) 7.dp.toPx() else 6.dp.toPx()
                drawLine(
                    color = if (major) tokens.inkTertiary else tokens.border,
                    start = Offset(x, centerY - half),
                    end = Offset(x, centerY + half),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            passMarkers.forEach { marker ->
                val x = xFor(marker)
                drawLine(
                    color = tokens.starlink,
                    start = Offset(x, centerY - 12.dp.toPx()),
                    end = Offset(x, centerY - 8.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            val liveX = xFor(0)
            drawLine(
                color = tokens.live,
                start = Offset(liveX, centerY - 8.dp.toPx()),
                end = Offset(liveX, centerY + 8.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )

            val thumbX = xFor(shown)
            val thumbRadius = (if (dragging) 15.dp else 10.dp).toPx()
            drawCircle(tokens.live.copy(alpha = 0.22f), thumbRadius * 1.5f, Offset(thumbX, centerY))
            drawCircle(tokens.live, thumbRadius, Offset(thumbX, centerY))
            drawCircle(Color.White, thumbRadius * 0.36f, Offset(thumbX, centerY))
        }
    }
}

// ================================================================================================
// 5. SCOPE CHIPS + FLEET RAIL
// ================================================================================================

@Composable
fun SatScopeRow(
    selected: SatelliteCategory,
    tokens: ZigSatTokens,
    isFa: Boolean,
    onSelect: (SatelliteCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("satellite_scope_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
    ) {
        items(SatelliteCategory.entries) { cat ->
            ZigSatChip(
                label = SatFmt.categoryLabel(cat, isFa),
                selected = cat == selected,
                tokens = tokens,
                onClick = { onSelect(cat) },
                testTag = "filter_${cat.name}",
            )
        }
    }
}

/**
 * A horizontally scrolling rail directly under the globe: one tap focuses, a second tap on the same
 * token opens details. The rail also carries the generalised "N up" LED meter.
 */
@Composable
fun SatFleetRail(
    tokens: ZigSatTokens,
    headerLabel: String,
    count: Int,
    aboveHorizonCount: Int,
    fleet: List<SatTokenUi>,
    isFa: Boolean,
    onTokenTap: (SatTokenUi) -> Unit,
    onSeeAll: () -> Unit,
    tokenWidth: Dp,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ZigMicroCapsWithCount(caps = headerLabel, count = count, tokens = tokens, isFa = isFa)
            Spacer(modifier = Modifier.weight(1f))
            ZigAboveHorizonMeter(tokens = tokens, lit = aboveHorizonCount, total = fleet.size, isFa = isFa)
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .height(SatGrid.minTouchTarget)
                    .clickable(onClick = onSeeAll)
                    .testTag("fleet_see_all"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isFa) "همه" else "See all",
                    style = RedTypographyTokens.badge.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    color = tokens.live,
                )
            }
        }

        Row(
            modifier = Modifier.padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendDot(tokens.nakedEye, if (isFa) "با چشم" else "Naked-eye", tokens, hollow = false)
            LegendDot(tokens.telescopic, if (isFa) "تلسکوپی" else "Telescopic", tokens, hollow = true)
        }

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("satellite_fleet_rail"),
        ) {
            items(fleet, key = { it.id }) { tokenUi ->
                SatFleetToken(
                    data = tokenUi,
                    tokens = tokens,
                    width = tokenWidth,
                    isFa = isFa,
                    onClick = { onTokenTap(tokenUi) },
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String, tokens: ZigSatTokens, hollow: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (hollow) Color.Transparent else color)
                .border(1.dp, color, CircleShape),
        )
        Text(
            text = label,
            style = RedTypographyTokens.badge.copy(fontSize = 10.sp),
            color = tokens.inkTertiary,
            maxLines = 1,
        )
    }
}

/** The generalised Aether five-dot "N above horizon" meter. */
@Composable
fun ZigAboveHorizonMeter(tokens: ZigSatTokens, lit: Int, total: Int, isFa: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            for (i in 0 until 5) {
                val on = i < lit.coerceIn(0, 5)
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (on) tokens.live else tokens.border),
                )
            }
        }
        Text(
            text = if (isFa) "${SatFmt.int(lit, true)} بالا" else "$lit up",
            style = RedTypographyTokens.badge.copy(fontSize = 10.sp),
            color = tokens.inkTertiary,
            maxLines = 1,
        )
    }
}

@Composable
fun SatFleetToken(
    data: SatTokenUi,
    tokens: ZigSatTokens,
    width: Dp,
    isFa: Boolean,
    onClick: () -> Unit,
) {
    val below = data.elevationDeg <= 0.0
    Column(
        modifier = Modifier
            .width(width)
            .height(96.dp)
            .clip(squircleShape(SatRadius.tile))
            .background(if (data.isSelected) tokens.selectedFill else tokens.cardAlt)
            .border(
                if (data.isSelected) 2.dp else 1.dp,
                if (data.isSelected) tokens.selectedRing else tokens.border,
                squircleShape(SatRadius.tile),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag("sat_token_${data.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.SatelliteAlt,
                contentDescription = null,
                tint = if (data.isSelected) tokens.selected else tokens.inkSecondary,
                modifier = Modifier.size(26.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (data.isNakedEye) tokens.nakedEye else Color.Transparent)
                    .border(1.dp, if (data.isNakedEye) tokens.nakedEye else tokens.telescopic, CircleShape),
            )
        }
        Text(
            text = if (data.isTrain) {
                if (isFa) "${data.shortName} ${SatFmt.int(data.trainCount, true)}" else "${data.shortName} ${data.trainCount}"
            } else {
                data.shortName
            },
            style = RedTypographyTokens.badge.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
            color = tokens.inkPrimary,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = SatFmt.signed(data.elevationDeg, 0, "°", isFa),
            style = RedTypographyTokens.badge.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold).tabular(),
            color = if (below) tokens.inkTertiary else tokens.inkPrimary,
            maxLines = 1,
        )
    }
}

// ================================================================================================
// 6. NEXT PASS SPOTLIGHT + sky arc
// ================================================================================================

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
) {
    val pass = data.pass.pass
    val accent = if (data.pass.isTrain) tokens.starlink else tokens.live
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
                    text = if (isFa) "گذر بعدی" else "Next pass",
                    tokens = tokens,
                    isFa = isFa,
                    color = accent,
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
                Row(
                    modifier = Modifier
                        .clip(RedShapes.capsule)
                        .background(tokens.starlink.copy(alpha = 0.16f))
                        .border(1.dp, tokens.starlink.copy(alpha = 0.5f), RedShapes.capsule)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (isFa) "رشته مروارید" else "STRING OF PEARLS",
                        style = RedTypographyTokens.badge.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = tokens.inkPrimary,
                    )
                    Text(
                        text = "· ${SatFmt.int(data.pass.sat.trainCount, isFa)}",
                        style = RedTypographyTokens.badge.copy(fontSize = 10.sp).tabular(),
                        color = tokens.inkSecondary,
                    )
                }
            }

            if (data.isInProgress) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = if (isFa) "در جریان" else "In progress",
                        style = RedTypographyTokens.badge.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        color = tokens.inkSecondary,
                    )
                    Text(
                        text = "· " + if (isFa) {
                            "${SatFmt.int(data.remainingMinutes, true)} دقیقه مانده"
                        } else {
                            "${data.remainingMinutes} min left"
                        },
                        style = RedTypographyTokens.badge.copy(fontSize = 12.sp).tabular(),
                        color = tokens.inkPrimary,
                    )
                }
                SatPassProgressBar(pass = pass, tokens = tokens, nowMs = nowMs)
            } else {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isFa) "شروع در" else "starts in",
                        style = RedTypographyTokens.badge.copy(fontSize = 12.sp),
                        color = tokens.inkSecondary,
                    )
                    Text(
                        text = SatFmt.countdown(pass.startTimeMs - nowMs, isFa),
                        style = RedTypographyTokens.sectionHeading.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.SemiBold,
                        ).tabular(),
                        color = tokens.inkPrimary,
                        maxLines = 1,
                        modifier = Modifier.testTag("spotlight_countdown"),
                    )
                }
            }

            SatSkyArc(pass = pass, tokens = tokens, isFa = isFa, height = arcHeight, nowMs = nowMs)

            Row(modifier = Modifier.fillMaxWidth()) {
                SpotlightStat(
                    label = if (isFa) "طلوع" else "Rise",
                    value = SatFmt.clock(pass.startTimeMs, isFa, TimeZone.getDefault()),
                    sub = SatFmt.compass(pass.startAzimuthDeg, isFa),
                    tokens = tokens,
                    isFa = isFa,
                    modifier = Modifier.weight(1f),
                )
                SpotlightStat(
                    label = if (isFa) "اوج" else "Max",
                    value = SatFmt.clock(pass.maxTimeMs, isFa, TimeZone.getDefault()),
                    sub = SatFmt.degrees(pass.maxElevationDeg, isFa),
                    tokens = tokens,
                    isFa = isFa,
                    modifier = Modifier.weight(1f),
                    accent = tokens.live,
                )
                SpotlightStat(
                    label = if (isFa) "غروب" else "Set",
                    value = SatFmt.clock(pass.endTimeMs, isFa, TimeZone.getDefault()),
                    sub = SatFmt.compass(pass.endAzimuthDeg, isFa),
                    tokens = tokens,
                    isFa = isFa,
                    modifier = Modifier.weight(1f),
                )
            }

            SatMetaLine(pass = pass, tokens = tokens, isFa = isFa)

            if (showActions) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ZigPrimaryAction(
                        label = if (isFa) "یادآوری" else "Remind",
                        tokens = tokens,
                        onClick = { actions.onRemind(10) },
                        modifier = Modifier.weight(1f),
                    )
                    ZigSecondaryAction(
                        label = if (isFa) "نمایش در AR" else "Spot in AR",
                        tokens = tokens,
                        onClick = actions.onAr,
                        modifier = Modifier.weight(1f),
                    )
                    ZigSatIconButton(
                        icon = Icons.Filled.CenterFocusWeak,
                        contentDescription = if (isFa) "نمایش روی کره" else "Show on globe",
                        tokens = tokens,
                        onClick = actions.onShowOnGlobe,
                    )
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
            text = "mag ${SatFmt.magnitude(pass.estimatedMagnitude, isFa)}",
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
fun SatSkyArc(
    pass: ISSEngine.ISSPass,
    tokens: ZigSatTokens,
    isFa: Boolean,
    height: Dp = 112.dp,
    nowMs: Long = System.currentTimeMillis(),
    showCompassMarks: Boolean = false,
) {
    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .testTag("next_pass_sky_arc"),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val horizonY = h - 14.dp.toPx()
                val left = 6.dp.toPx()
                val right = (w - 6.dp.toPx()).coerceAtLeast(left + 1f)
                val span = right - left

                drawLine(
                    color = tokens.border,
                    start = Offset(left, horizonY),
                    end = Offset(right, horizonY),
                    strokeWidth = 1.dp.toPx(),
                )

                val peakFraction = (pass.maxElevationDeg / 90.0).coerceIn(0.06, 1.0).toFloat()
                val peakHeight = (horizonY - 12.dp.toPx()) * peakFraction
                val peakX = left + span * 0.5f
                val c1 = Offset(left + span * 0.18f, horizonY - peakHeight * 1.35f)
                val c2 = Offset(right - span * 0.18f, horizonY - peakHeight * 1.35f)

                fun pointAt(t: Float): Offset {
                    val u = 1f - t
                    return Offset(
                        u * u * u * left + 3 * u * u * t * c1.x + 3 * u * t * t * c2.x + t * t * t * right,
                        u * u * u * horizonY + 3 * u * u * t * c1.y + 3 * u * t * t * c2.y + t * t * t * horizonY,
                    )
                }

                val dome = Path().apply {
                    moveTo(left, horizonY)
                    cubicTo(c1.x, c1.y, c2.x, c2.y, right, horizonY)
                }
                drawPath(
                    path = dome,
                    color = tokens.inkTertiary.copy(alpha = 0.55f),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
                )

                drawLine(
                    color = tokens.border,
                    start = Offset(peakX, horizonY),
                    end = Offset(peakX, horizonY - peakHeight),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())),
                )

                val passSpan = (pass.endTimeMs - pass.startTimeMs).coerceAtLeast(1L).toFloat()
                listOfNotNull(pass.shadowEntryMs, pass.shadowExitMs).forEach { shadowMs ->
                    val t = ((shadowMs - pass.startTimeMs) / passSpan).coerceIn(0f, 1f)
                    drawCircle(tokens.umbra, 3.dp.toPx(), pointAt(t))
                }

                drawCircle(tokens.inkTertiary, 2.dp.toPx(), Offset(left, horizonY))
                drawCircle(tokens.inkTertiary, 2.dp.toPx(), Offset(right, horizonY))

                drawCircle(tokens.live.copy(alpha = 0.24f), 9.dp.toPx(), Offset(peakX, horizonY - peakHeight))
                drawCircle(tokens.live, 4.dp.toPx(), Offset(peakX, horizonY - peakHeight))

                if (nowMs in pass.startTimeMs..pass.endTimeMs) {
                    val t = ((nowMs - pass.startTimeMs) / passSpan).coerceIn(0f, 1f)
                    drawCircle(Color.White, 3.5.dp.toPx(), pointAt(t))
                }

                if (showCompassMarks) {
                    for (i in 0 until 4) {
                        drawCircle(
                            color = tokens.inkTertiary.copy(alpha = 0.4f),
                            radius = 1.5.dp.toPx(),
                            center = Offset(left + span * (i / 3f), horizonY + 7.dp.toPx()),
                        )
                    }
                }
            }

            Text(
                text = SatFmt.degrees(pass.maxElevationDeg, isFa),
                style = RedTypographyTokens.badge.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold).tabular(),
                color = tokens.live,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = SatFmt.compass(pass.startAzimuthDeg, isFa),
                    style = RedTypographyTokens.badge.copy(fontSize = 10.sp),
                    color = tokens.inkTertiary,
                )
                Text(
                    text = SatFmt.compass(pass.endAzimuthDeg, isFa),
                    style = RedTypographyTokens.badge.copy(fontSize = 10.sp),
                    color = tokens.inkTertiary,
                )
            }
        }
    }
}

@Composable
private fun ZigPrimaryAction(
    label: String,
    tokens: ZigSatTokens,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(SatGrid.minTouchTarget)
            .clip(RedShapes.capsule)
            .background(tokens.live.copy(alpha = 0.16f))
            .border(1.dp, tokens.live.copy(alpha = 0.55f), RedShapes.capsule)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = RedTypographyTokens.badge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = tokens.live,
            maxLines = 1,
        )
    }
}

@Composable
private fun ZigSecondaryAction(
    label: String,
    tokens: ZigSatTokens,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(SatGrid.minTouchTarget)
            .clip(RedShapes.capsule)
            .background(tokens.cardAlt)
            .border(1.dp, tokens.border, RedShapes.capsule)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = RedTypographyTokens.badge.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
            color = tokens.inkSecondary,
            maxLines = 1,
        )
    }
}

/** 6 dp linear progress with a Max tick, shown while a pass is in progress. */
@Composable
fun SatPassProgressBar(pass: ISSEngine.ISSPass, tokens: ZigSatTokens, nowMs: Long) {
    val span = (pass.endTimeMs - pass.startTimeMs).coerceAtLeast(1L)
    val progress = ((nowMs - pass.startTimeMs).toFloat() / span).coerceIn(0f, 1f)
    val maxFraction = ((pass.maxTimeMs - pass.startTimeMs).toFloat() / span).coerceIn(0f, 1f)
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
            size = Size(size.width * progress, h),
            cornerRadius = CornerRadius(h / 2f, h / 2f),
        )
        drawLine(
            color = tokens.inkTertiary,
            start = Offset(size.width * maxFraction, -2.dp.toPx()),
            end = Offset(size.width * maxFraction, h + 2.dp.toPx()),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

// ================================================================================================
// 7. COMPACT PASS CARD + passes by night
// ================================================================================================

/**
 * Compact pass card: the whole card opens detail, the bell is its own 48 dp target, and the date is
 * supplied by the night group header so it is not repeated per card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SatCompactPassCard(
    data: SatPassUi,
    tokens: ZigSatTokens,
    isFa: Boolean,
    actions: SatPassActions,
    modifier: Modifier = Modifier,
) {
    val pass = data.pass
    val accent = if (data.isTrain) tokens.starlink else null
    ZigSatCard(
        modifier = modifier.testTag("pass_card_${data.key}"),
        tokens = tokens,
        accent = accent,
        shape = squircleShape(SatRadius.compactCard),
        onClick = actions.onOpenDetail,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.SatelliteAlt,
                    contentDescription = null,
                    tint = if (data.isTrain) tokens.starlink else tokens.inkSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = data.satName,
                    style = RedTypographyTokens.badge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    color = tokens.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (data.isTrain) {
                    Text(
                        text = if (isFa) {
                            "قطار · ${SatFmt.int(data.sat.trainCount, true)}"
                        } else {
                            "Train · ${data.sat.trainCount}"
                        },
                        style = RedTypographyTokens.badge.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                        color = tokens.inkSecondary,
                        maxLines = 1,
                    )
                }
                ZigQualityChip(
                    quality = data.quality,
                    score = pass.visibilityScore,
                    tokens = tokens,
                    isFa = isFa,
                    full = false,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = SatFmt.clock(pass.startTimeMs, isFa, TimeZone.getDefault()) +
                        " \u2013 " + SatFmt.clock(pass.endTimeMs, isFa, TimeZone.getDefault()),
                    style = RedTypographyTokens.badge.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold).tabular(),
                    color = tokens.inkPrimary,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Max ${SatFmt.clock(pass.maxTimeMs, isFa, TimeZone.getDefault())} · " +
                        SatFmt.degrees(pass.maxElevationDeg, isFa),
                    style = RedTypographyTokens.badge.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium).tabular(),
                    color = tokens.live,
                    maxLines = 1,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PassMeta(
                        text = SatFmt.direction(pass.startAzimuthDeg, pass.endAzimuthDeg, isFa),
                        tokens = tokens,
                    )
                    PassMeta(
                        text = "mag ${SatFmt.magnitude(pass.estimatedMagnitude, isFa)}",
                        tokens = tokens,
                    )
                    PassMeta(
                        text = SatFmt.duration(((pass.endTimeMs - pass.startTimeMs) / 60_000L).toInt(), isFa),
                        tokens = tokens,
                    )
                }
                ZigBellButton(
                    leadMinutes = data.reminderLeadMinutes,
                    isAutoCovered = data.isAutoCovered,
                    isPast = data.isFinished,
                    tokens = tokens,
                    isFa = isFa,
                    onClick = { actions.onRemind(data.reminderLeadMinutes ?: 10) },
                )
            }
        }
    }
}

@Composable
private fun PassMeta(text: String, tokens: ZigSatTokens) {
    Text(
        text = text,
        style = RedTypographyTokens.badge.copy(fontSize = 11.sp).tabular(),
        color = tokens.inkTertiary,
        maxLines = 1,
    )
}

/** Section header: title, tabular count capsule, optional scope echo, subtitle. */
@Composable
fun SatSectionHeader(
    title: String,
    tokens: ZigSatTokens,
    isFa: Boolean,
    count: Int? = null,
    subtitle: String? = null,
    scopeEcho: String? = null,
    onClearScope: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    style = RedTypographyTokens.sectionTitle.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                    color = tokens.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (count != null) {
                    Text(
                        text = SatFmt.intPadded(count, 2, isFa),
                        style = RedTypographyTokens.badge.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium).tabular(),
                        color = tokens.inkSecondary,
                        modifier = Modifier
                            .clip(RedShapes.capsule)
                            .background(tokens.cardAlt)
                            .border(1.dp, tokens.border, RedShapes.capsule)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                if (scopeEcho != null) {
                    Row(
                        modifier = Modifier
                            .clip(RedShapes.capsule)
                            .background(tokens.live.copy(alpha = 0.12f))
                            .border(1.dp, tokens.live.copy(alpha = 0.4f), RedShapes.capsule),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = scopeEcho,
                            style = RedTypographyTokens.badge.copy(fontSize = 10.sp),
                            color = tokens.live,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                        )
                        if (onClearScope != null) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable(onClick = onClearScope),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "\u00D7",
                                    style = RedTypographyTokens.badge.copy(fontSize = 12.sp),
                                    color = tokens.live,
                                )
                            }
                        }
                    }
                }
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = RedTypographyTokens.badge.copy(fontSize = 12.sp),
                    color = tokens.inkTertiary,
                    maxLines = 1,
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * Passes grouped by observer night. The first night is open with up to three cards and an inline
 * "Show one more"; later nights are compact summary rows that expand in place.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SatPassesByNight(
    groups: List<SatNightGroup>,
    tokens: ZigSatTokens,
    isFa: Boolean,
    actionsFor: (SatPassUi) -> SatPassActions,
    onToggleNight: (Long) -> Unit,
    onShowMore: (Long) -> Unit,
    expandedNights: Set<Long>,
    shownCounts: Map<Long, Int> = emptyMap(),
    modifier: Modifier = Modifier,
    maxCardsPerNight: Int = 10,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        groups.forEachIndexed { index, group ->
            val expanded = index == 0 || group.nightKey in expandedNights
            val shown = (shownCounts[group.nightKey] ?: 3).coerceAtMost(maxCardsPerNight)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (expanded) SatGrid.minTouchTarget else 56.dp)
                        .clip(squircleShape(SatRadius.tile))
                        .background(if (expanded) Color.Transparent else tokens.cardAlt)
                        .then(if (expanded) Modifier else Modifier.border(1.dp, tokens.border, squircleShape(SatRadius.tile)))
                        .clickable(enabled = index != 0) { onToggleNight(group.nightKey) }
                        .padding(horizontal = if (expanded) 2.dp else 14.dp)
                        .testTag("night_group_${group.nightKey}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = group.title,
                        style = RedTypographyTokens.badge.copy(
                            fontSize = if (expanded) 14.sp else 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = tokens.inkPrimary,
                        maxLines = 1,
                    )
                    if (group.subtitle != null) {
                        Text(
                            text = group.subtitle,
                            style = RedTypographyTokens.badge.copy(fontSize = 10.sp),
                            color = tokens.inkTertiary,
                            maxLines = 1,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (isFa) {
                            "${SatFmt.int(group.passes.size, true)} گذر"
                        } else {
                            "${group.passes.size} pass" + if (group.passes.size == 1) "" else "es"
                        },
                        style = RedTypographyTokens.badge.copy(fontSize = 11.sp).tabular(),
                        color = tokens.inkSecondary,
                        maxLines = 1,
                    )
                    group.best?.let { best ->
                        Text(text = "\u2605", style = RedTypographyTokens.badge.copy(fontSize = 11.sp), color = best.quality.hue)
                        Text(
                            text = SatFmt.degrees(best.pass.maxElevationDeg, isFa),
                            style = RedTypographyTokens.badge.copy(fontSize = 10.sp).tabular(),
                            color = tokens.inkTertiary,
                            maxLines = 1,
                        )
                    }
                    if (index != 0) {
                        Text(
                            text = if (expanded) "\u25B4" else "\u25BE",
                            style = RedTypographyTokens.badge.copy(fontSize = 11.sp),
                            color = tokens.inkTertiary,
                        )
                    }
                }

                if (expanded) {
                    val visible = group.passes.take(shown)
                    visible.forEach { passUi ->
                        SatCompactPassCard(
                            data = passUi,
                            tokens = tokens,
                            isFa = isFa,
                            actions = actionsFor(passUi),
                        )
                    }
                    val remaining = group.passes.size - visible.size
                    if (remaining > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(SatGrid.minTouchTarget)
                                .clickable { onShowMore(group.nightKey) }
                                .testTag("night_show_more_${group.nightKey}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (isFa) {
                                    "نمایش ${SatFmt.int(remaining, true)} گذر دیگر"
                                } else {
                                    "Show $remaining more"
                                },
                                style = RedTypographyTokens.badge.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                color = tokens.live,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Loading skeleton: three pulsing cards, no shimmer sweep (Default never blurs). */
@Composable
fun SatPassSkeletons(tokens: ZigSatTokens, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "SatSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1200), repeatMode = RepeatMode.Reverse),
        label = "SatSkeletonAlpha",
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .clip(squircleShape(SatRadius.compactCard))
                    .background(tokens.cardAlt.copy(alpha = alpha * 0.8f))
                    .border(1.dp, tokens.border, squircleShape(SatRadius.compactCard)),
            )
        }
    }
}

/** Empty state: an explanation plus the two ways out of it. */
@Composable
fun SatPassEmpty(
    tokens: ZigSatTokens,
    isFa: Boolean,
    scopeLabel: String?,
    onShowAll: () -> Unit,
    onOpenSevenDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ZigSatCard(modifier = modifier, tokens = tokens, shape = squircleShape(SatRadius.compactCard)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = null,
                tint = tokens.inkTertiary,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = if (isFa) {
                    "هیچ گذر مرئی‌ای در ۳ روز آینده" + (scopeLabel?.let { " برای $it" } ?: "") + " یافت نشد."
                } else {
                    "No visible passes in the next 3 days" + (scopeLabel?.let { " for $it" } ?: "") + "."
                },
                style = RedTypographyTokens.bodySecondary,
                color = tokens.inkSecondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZigSecondaryAction(
                    label = if (isFa) "همه ماهواره‌ها" else "Show all satellites",
                    tokens = tokens,
                    onClick = onShowAll,
                    modifier = Modifier.weight(1f),
                )
                ZigSecondaryAction(
                    label = if (isFa) "نمای ۷ روزه" else "7-day view",
                    tokens = tokens,
                    onClick = onOpenSevenDay,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
